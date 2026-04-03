package divinity.voice;

import divinity.ClientManager;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoiceClient {

    private static final byte[] MAGIC = new byte[]{'V', 'C', 'H', '1'};
    private static final byte TYPE_HELLO = 1;
    private static final byte TYPE_POS = 2;
    private static final byte TYPE_AUDIO = 3;

    private static final int SAMPLE_RATE = 48000;
    private static final int FRAME_SAMPLES = 480;
    private static final int FRAME_BYTES_MONO = FRAME_SAMPLES * 2;
    private static final int POS_INTERVAL_MS = 200;
    private static final int SPEAK_HANG_MS = 250;
    private static final int VAD_HANG_MS = 260;

    private static final VoiceClient INSTANCE = new VoiceClient();
    private final Map<String, RemoteState> remotes = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean transmitting = new AtomicBoolean(false);
    private volatile String token;
    private volatile int voicePort = -1;
    private volatile InetAddress voiceHost;
    private volatile String microphoneName = "Default";
    private volatile double range = 32.0;
    private volatile double volume = 1.0;
    private volatile boolean openMic = false;
    private volatile boolean localSpeaking = false;
    private volatile long localSpeakingAt = 0L;
    private DatagramSocket udp;
    private Thread udpRxThread;
    private Thread captureThread;
    private Thread mixThread;
    private TargetDataLine micLine;
    private SourceDataLine outLine;
    private long lastPosSent = 0L;
    private volatile boolean havePos = false;
    private volatile double lastX;
    private volatile double lastY;
    private volatile double lastZ;
    private volatile int lastDim;
    private volatile float lastRange = 32f;
    private volatile long lastPosUpdateAt = 0L;
    private long lastHelloSent = 0L;
    private long lastTokenRequestAt = 0L;
    private long lastPosKeepaliveSent = 0L;
    private String lastServerKey = "";

    private VoiceClient() {
    }

    public static VoiceClient getInstance() {
        return INSTANCE;
    }

    private static double computePan(double dx, double dz, float yawDeg) {
        double yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double nx = dx;
        double nz = dz;
        double len = Math.sqrt(nx * nx + nz * nz);
        if (len < 1e-6) return 0.0;
        nx /= len;
        nz /= len;
        double cross = fx * nz - fz * nx;
        return clamp(cross, -1.0, 1.0);
    }

    private static float clamp(float v, float min, float max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0, 1.0);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    public List<String> listMicrophones() {
        List<String> out = new ArrayList<>();
        out.add("Default");
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, fmt);
        for (Mixer.Info info : infos) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                if (mixer.isLineSupported(lineInfo)) {
                    out.add(info.getName());
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    public void setMicrophoneName(String name) {
        if (name == null || name.isEmpty()) name = "Default";
        if (Objects.equals(this.microphoneName, name)) return;
        this.microphoneName = name;
        restartMic();
    }

    public void setOpenMic(boolean openMic) {
        this.openMic = openMic;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public void setPttPressed(boolean pressed) {
        transmitting.set(pressed);
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public boolean isLocalSpeaking() {
        if (localSpeaking) return true;
        return System.currentTimeMillis() - localSpeakingAt <= SPEAK_HANG_MS;
    }

    public boolean isTransmitting() {
        return transmitting.get();
    }

    public boolean isUserSpeaking(String ign) {
        if (ign == null) return false;
        RemoteState st = remotes.get(ign.toLowerCase(Locale.ROOT));
        if (st == null) return false;
        if (st.speaking) return true;
        return System.currentTimeMillis() - st.lastAudioAt <= SPEAK_HANG_MS;
    }

    public void onVoiceToken(String content) {
        if (content == null) return;
        String[] parts = content.split("\\|");
        if (parts.length < 2) return;
        token = parts[0];
        try {
            voicePort = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            voicePort = -1;
        }

        if (running.get()) {
            sendHello();
        }
    }

    public void requestToken() {
        // IRC token request removed
    }

    public void onIrcDisconnected() {
        voiceHost = null;
        token = null;
        voicePort = -1;
        lastHelloSent = 0L;
        lastTokenRequestAt = 0L;
    }

    public void heartbeat() {
        if (!running.get()) return;

        long now = System.currentTimeMillis();
        String serverKey = computeServerKey();
        if (!serverKey.equals(lastServerKey)) {
            lastServerKey = serverKey;
            token = null;
            voicePort = -1;
            lastHelloSent = 0L;
            lastTokenRequestAt = 0L;
        }

        if (token == null) {
            if (now - lastTokenRequestAt >= 3000) {
                lastTokenRequestAt = now;
                requestToken();
            }
            return;
        }

        if (voiceHost == null || voicePort <= 0) return;

        if (now - lastHelloSent >= 5000) {
            lastHelloSent = now;
            sendHello();
        }

        if (now - lastPosKeepaliveSent >= 1000) {
            lastPosKeepaliveSent = now;
            sendPosKeepalive();
        }

        cleanupRemoteSpeaking(now);
    }

    private void cleanupRemoteSpeaking(long now) {
        for (RemoteState st : remotes.values()) {
            if (now - st.lastAudioAt > SPEAK_HANG_MS) {
                st.speaking = false;
            }
        }
    }

    private void sendPosKeepalive() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            sendPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.dimension, (float) range);
            return;
        }
        if (!havePos) return;
        sendPos(lastX, lastY, lastZ, lastDim, lastRange);
    }

    private void sendPos(double x, double y, double z, int dim, float r) {
        if (token == null || voiceHost == null || voicePort <= 0) return;
        byte[] tok = token.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(4 + 1 + 1 + tok.length + 8 * 3 + 4 + 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(MAGIC);
        buf.put(TYPE_POS);
        buf.put((byte) tok.length);
        buf.put(tok);
        buf.putDouble(x);
        buf.putDouble(y);
        buf.putDouble(z);
        buf.putInt(dim);
        buf.putFloat(r);
        sendRaw(buf.array());
    }

    private String computeServerKey() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return "";
        try {
            if (mc.isSingleplayer()) return "SP";
            if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null) {
                return mc.getCurrentServerData().serverIP.trim().toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        token = null;
        voicePort = -1;
        voiceHost = null;
        havePos = false;
        lastHelloSent = 0L;
        lastTokenRequestAt = 0L;
        lastPosKeepaliveSent = 0L;
        lastServerKey = "";
        try {
            udp = new DatagramSocket();
            udp.setReceiveBufferSize(1024 * 1024);
            udp.setSendBufferSize(512 * 1024);
            udp.setTrafficClass(0xB8);
            udp.setSoTimeout(100);
        } catch (Exception e) {
            running.set(false);
            return;
        }

        udpRxThread = new Thread(this::udpReceiveLoop, "Voice-UDP-RX");
        udpRxThread.setDaemon(true);
        udpRxThread.start();

        captureThread = new Thread(this::captureLoop, "Voice-Capture");
        captureThread.setDaemon(true);
        captureThread.start();

        mixThread = new Thread(this::mixLoop, "Voice-Mix");
        mixThread.setDaemon(true);
        mixThread.start();

        requestToken();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        transmitting.set(false);

        try {
            if (micLine != null) micLine.stop();
        } catch (Exception ignored) {
        }
        try {
            if (micLine != null) micLine.close();
        } catch (Exception ignored) {
        }
        micLine = null;

        try {
            if (outLine != null) outLine.stop();
        } catch (Exception ignored) {
        }
        try {
            if (outLine != null) outLine.close();
        } catch (Exception ignored) {
        }
        outLine = null;

        try {
            if (udp != null) udp.close();
        } catch (Exception ignored) {
        }
        udp = null;

        remotes.clear();

        token = null;
        voicePort = -1;
        voiceHost = null;
        havePos = false;
        lastHelloSent = 0L;
        lastTokenRequestAt = 0L;
        lastPosKeepaliveSent = 0L;
        lastServerKey = "";
    }

    public void tickPosition(double x, double y, double z, int dim, double range) {
        if (!running.get()) return;

        long now = System.currentTimeMillis();
        havePos = true;
        lastX = x;
        lastY = y;
        lastZ = z;
        lastDim = dim;
        lastRange = (float) range;
        lastPosUpdateAt = now;

        String serverKey = computeServerKey();
        if (!serverKey.equals(lastServerKey)) {
            lastServerKey = serverKey;
            remotes.clear();
            lastPosSent = 0L;
            lastHelloSent = 0L;
            lastPosKeepaliveSent = 0L;
            lastTokenRequestAt = 0L;
            token = null;
            voicePort = -1;
            requestToken();
        }

        if (token == null || voiceHost == null || voicePort <= 0) return;

        if (now - lastPosSent < POS_INTERVAL_MS) return;
        lastPosSent = now;

        sendPos(x, y, z, dim, (float) range);
    }

    private void sendHello() {
        if (token == null || voiceHost == null || voicePort <= 0) return;
        byte[] tok = token.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(4 + 1 + 1 + tok.length);
        buf.put(MAGIC);
        buf.put(TYPE_HELLO);
        buf.put((byte) tok.length);
        buf.put(tok);
        sendRaw(buf.array());
    }

    private void sendAudioFrame(byte[] pcmMonoLE, double x, double y, double z, int dim) {
        if (token == null || voiceHost == null || voicePort <= 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        String ign = mc.thePlayer.getName();
        byte[] ignBytes = ign.getBytes(StandardCharsets.UTF_8);
        byte[] tok = token.getBytes(StandardCharsets.UTF_8);

        byte[] payload = pcmMonoLE;

        ByteBuffer buf = ByteBuffer.allocate(4 + 1 + 1 + tok.length + 1 + ignBytes.length + 8 * 3 + 4 + 1 + 2 + payload.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(MAGIC);
        buf.put(TYPE_AUDIO);
        buf.put((byte) tok.length);
        buf.put(tok);
        buf.put((byte) ignBytes.length);
        buf.put(ignBytes);
        buf.putDouble(x);
        buf.putDouble(y);
        buf.putDouble(z);
        buf.putInt(dim);
        buf.put((byte) 0);
        buf.putShort((short) payload.length);
        buf.put(payload);

        sendRaw(buf.array());
    }

    private void sendRaw(byte[] data) {
        try {
            DatagramPacket p = new DatagramPacket(data, data.length, voiceHost, voicePort);
            udp.send(p);
        } catch (Exception ignored) {
        }
    }

    private void udpReceiveLoop() {
        byte[] buf = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (running.get()) {
            try {
                udp.receive(packet);
                int len = packet.getLength();
                if (len < 6) continue;
                if (buf[0] != MAGIC[0] || buf[1] != MAGIC[1] || buf[2] != MAGIC[2] || buf[3] != MAGIC[3]) continue;
                byte type = buf[4];
                if (type != TYPE_AUDIO) continue;
                parseAudio(Arrays.copyOf(buf, len));
            } catch (Exception e) {
                if (!running.get()) break;
            }
        }
    }

    private void parseAudio(byte[] data) {
        try {
            ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            b.position(5);
            int ignLen = b.get() & 0xFF;
            if (ignLen <= 0 || ignLen > 32) return;
            byte[] ignBytes = new byte[ignLen];
            b.get(ignBytes);
            String ign = new String(ignBytes, StandardCharsets.UTF_8);
            double sx = b.getDouble();
            double sy = b.getDouble();
            double sz = b.getDouble();
            int dim = b.getInt();
            boolean compressed = (b.get() & 0xFF) == 1;
            int audioLen = b.getShort() & 0xFFFF;
            if (audioLen <= 0 || audioLen > 4096) return;
            byte[] audio = new byte[audioLen];
            b.get(audio);

            byte[] pcm = compressed ? null : audio;
            if (pcm == null || pcm.length != FRAME_BYTES_MONO) return;

            short[] samples = new short[FRAME_SAMPLES];
            ByteBuffer pb = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < FRAME_SAMPLES; i++) {
                samples[i] = pb.getShort();
            }

            String key = ign.toLowerCase(Locale.ROOT);
            RemoteState st = remotes.computeIfAbsent(key, k -> new RemoteState());
            st.x = sx;
            st.y = sy;
            st.z = sz;
            st.dim = dim;
            st.lastAudioAt = System.currentTimeMillis();
            st.speaking = true;

            synchronized (st.frames) {
                if (st.frames.size() > 100) st.frames.pollFirst();
                st.frames.addLast(samples);
            }
        } catch (Exception ignored) {
        }
    }

    private void captureLoop() {
        AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        byte[] buffer = new byte[FRAME_BYTES_MONO];
        byte[] tx = new byte[FRAME_BYTES_MONO];
        CaptureDsp dsp = new CaptureDsp();
        while (running.get()) {
            ensureMic(fmt);
            if (micLine == null) {
                sleep(250);
                continue;
            }

            int read = 0;
            while (read < buffer.length) {
                int r = micLine.read(buffer, read, buffer.length - read);
                if (r < 0) break;
                read += r;
            }
            if (read != buffer.length) continue;

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null) continue;

            boolean allowTx = openMic || transmitting.get();
            boolean speaking = dsp.process(buffer, tx);
            long now = System.currentTimeMillis();

            if (allowTx && speaking) localSpeakingAt = now;
            localSpeaking = allowTx && speaking;

            if (allowTx) {
                int dim = mc.thePlayer.dimension;
                sendAudioFrame(tx, mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, dim);
            }
        }
    }

    private void mixLoop() {
        AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
        byte[] out = new byte[FRAME_SAMPLES * 4];
        while (running.get()) {
            ensureOut(fmt);
            if (outLine == null) {
                sleep(250);
                continue;
            }

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null) {
                sleep(50);
                continue;
            }

            int dim = mc.thePlayer.dimension;
            double lx = mc.thePlayer.posX;
            double ly = mc.thePlayer.posY;
            double lz = mc.thePlayer.posZ;
            float yaw = mc.thePlayer.rotationYaw;

            float[] mixL = new float[FRAME_SAMPLES];
            float[] mixR = new float[FRAME_SAMPLES];

            long now = System.currentTimeMillis();
            for (RemoteState st : remotes.values()) {
                if (st.dim != dim) continue;
                double dx = st.x - lx;
                double dz = st.z - lz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > range) continue;

                double att = 1.0 - (dist / range);
                att = att * att;
                double base = volume * att;

                double pan = computePan(dx, dz, yaw);
                double leftMul = clamp01(1.0 - pan);
                double rightMul = clamp01(1.0 + pan);

                short[] frame = null;
                synchronized (st.frames) {
                    if (st.frames.size() < 20) {
                        continue;
                    }

                    frame = st.frames.pollFirst();
                }

                if (frame == null) {
                    if (st.lastFrame != null) {
                        frame = st.lastFrame;
                    } else {
                        if (now - st.lastAudioAt > SPEAK_HANG_MS) {
                            st.speaking = false;
                            st.warmedUp = false;
                        }
                        continue;
                    }
                } else {
                    st.lastFrame = frame;
                    st.warmedUp = true;
                }

                st.speaking = true;
                st.lastAudioAt = now;

                for (int i = 0; i < FRAME_SAMPLES; i++) {
                    float s = frame[i] / 32768.0f;
                    mixL[i] += (float) (s * base * leftMul);
                    mixR[i] += (float) (s * base * rightMul);
                }
            }

            ByteBuffer bb = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < FRAME_SAMPLES; i++) {
                int sl = (int) (clamp(mixL[i], -1f, 1f) * 32767f);
                int sr = (int) (clamp(mixR[i], -1f, 1f) * 32767f);
                bb.putShort((short) sl);
                bb.putShort((short) sr);
            }

            try {
                outLine.write(out, 0, out.length);
            } catch (Exception ignored) {
            }

            sleep(1);
        }
    }

    private void restartMic() {
        try {
            if (micLine != null) micLine.stop();
        } catch (Exception ignored) {
        }
        try {
            if (micLine != null) micLine.close();
        } catch (Exception ignored) {
        }
        micLine = null;
    }

    private void ensureMic(AudioFormat fmt) {
        if (micLine != null && micLine.isOpen()) return;
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
            TargetDataLine line = null;

            if (!"Default".equalsIgnoreCase(microphoneName)) {
                Mixer.Info[] infos = AudioSystem.getMixerInfo();
                for (Mixer.Info mi : infos) {
                    if (!mi.getName().equals(microphoneName)) continue;
                    Mixer mixer = AudioSystem.getMixer(mi);
                    if (!mixer.isLineSupported(info)) break;
                    line = (TargetDataLine) mixer.getLine(info);
                    break;
                }
            }

            if (line == null) {
                line = (TargetDataLine) AudioSystem.getLine(info);
            }

            int bufferSize = SAMPLE_RATE * 2 / 5;
            line.open(fmt, bufferSize);
            line.start();
            micLine = line;
        } catch (Exception e) {
            micLine = null;
        }
    }

    private void ensureOut(AudioFormat fmt) {
        if (outLine != null && outLine.isOpen()) return;
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

            int bufferSize = SAMPLE_RATE * 2 * 2 / 5;
            line.open(fmt, bufferSize);
            line.start();
            outLine = line;
        } catch (Exception e) {
            outLine = null;
        }
    }

    public static class RemoteState {
        public final ArrayDeque<short[]> frames = new ArrayDeque<>();
        public volatile double x;
        public volatile double y;
        public volatile double z;
        public volatile int dim;
        public volatile long lastAudioAt;
        public volatile boolean speaking;
        public short[] lastFrame;
        public volatile boolean warmedUp = false;
    }

    private static final class CaptureDsp {
        private float noise = 0.01f;
        private boolean speaking;
        private long lastVoiceAt;

        public boolean process(byte[] inPcmMonoLE, byte[] outPcmMonoLE) {
            System.arraycopy(inPcmMonoLE, 0, outPcmMonoLE, 0, inPcmMonoLE.length);

            long sum = 0;
            for (int i = 0; i < inPcmMonoLE.length; i += 2) {
                int lo = inPcmMonoLE[i] & 0xFF;
                int hi = inPcmMonoLE[i + 1] << 8;
                short s = (short) (lo | hi);
                sum += Math.abs(s);
            }

            long avg = sum / FRAME_SAMPLES;
            long now = System.currentTimeMillis();

            if (avg > 500) {
                lastVoiceAt = now;
                speaking = true;
            } else if (now - lastVoiceAt > VAD_HANG_MS) {
                speaking = false;
            }

            return speaking;
        }
    }
}