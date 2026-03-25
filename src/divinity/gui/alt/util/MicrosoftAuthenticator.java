package divinity.gui.alt.util;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import divinity.ClientManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class MicrosoftAuthenticator {
    public static final Logger Logger = LogManager.getLogger();
    private static final int PORT = 42441;
    private static final String USER_AGENT = "MinecraftLauncher/2.3.2283 (Windows NT 10.0; Win64; x64)";
    private final String clientID, clientSecret;
    private final Gson gson = new Gson();
    private final Consumer<SavedAltData> authCallback;
    private HttpServer server;


    public MicrosoftAuthenticator(Consumer<SavedAltData> authCallback, String clientID, String clientSecret) {
        this.authCallback = authCallback;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }

    private static String sendHttpReqBearer(String url, String bearer) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

            conn.addRequestProperty("User-Agent", USER_AGENT);
            conn.addRequestProperty("Accept", "application/json");
            conn.addRequestProperty("Authorization", "Bearer " + bearer);

            conn.connect();

            int responseCode = conn.getResponseCode();

            if (responseCode != 200) {
                conn.disconnect();
                Logger.warn("sendHttpGet: got response code: {}", responseCode);
                return null;
            }

            InputStream is = conn.getInputStream();

            if (is == null) {
                conn.disconnect();
                Logger.warn("sendHttpReqBearer: unable to read response");
                return null;
            }

            return IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("sendHttpReqBearer failed");
            return null;
        }
    }

    private static String sendHttpGetXblSisu(String url, Map<String, String> cookies) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.addRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/png,image/svg+xml,*/*;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setInstanceFollowRedirects(false);

            if (cookies != null) {
                StringBuilder cookieSb = new StringBuilder();
                cookies.forEach((key, value) -> {
                    cookieSb.append(key).append("=").append(value).append("; ");
                });
                conn.setRequestProperty("Cookie", cookieSb.toString());
            }

            conn.setRequestMethod("GET");
            conn.setDoOutput(true);

            conn.connect();

            String redirection = conn.getHeaderField("Location");
            if (redirection != null) {
                if (redirection.contains("accessToken=")) {
                    String[] accessTokenSplit = redirection.split("accessToken=", 2);
                    if (accessTokenSplit.length == 2) {
                        String base64AccessToken = accessTokenSplit[1];
                        String accessToken = new String(Base64.getDecoder().decode(base64AccessToken), StandardCharsets.UTF_8).split("\"rp://api.minecraftservices.com/\",")[1];

                        String token = accessToken.split("\"Token\":\"")[1].split("\"")[0];
                        String uhs = accessToken.split(Pattern.quote("{\"DisplayClaims\":{\"xui\":[{\"uhs\":\""))[1].split("\"")[0];
                        return "XBL3.0 x=" + uhs + ";" + token;
                    }
                }

                redirection = redirection.replace(" ", "%20");
                conn.disconnect();
                return sendHttpGetXblSisu(redirection, cookies);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode != 200) {
                conn.disconnect();
                Logger.warn("sendHttpGet: got response code: {}", responseCode);
                return null;
            }

            InputStream respStream = conn.getInputStream();

            if (respStream == null) {
                conn.disconnect();
                Logger.warn("sendHttpGet: unable to read response");
                return null;
            }

            return IOUtils.toString(respStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("sendHttpGet failed");
            return null;
        }
    }

    private static String sendHttpPost(String url, String body, boolean setCTJson) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.addRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bodyBytes.length);

            conn.addRequestProperty("Content-Type", setCTJson ?
                    "application/json" :
                    "application/x-www-form-urlencoded; charset=UTF-8");

            conn.addRequestProperty("Accept", "application/json");
            conn.connect();

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode != 200) {
                Logger.warn("sendHttpPost: got response code: {} {} {}", responseCode, conn.getURL().toString(), body);
                conn.disconnect();
                return null;
            }

            InputStream respStream = conn.getInputStream();

            if (respStream == null) {
                conn.disconnect();
                Logger.warn("sendHttpPost: unable to read response");
                return null;
            }

            return IOUtils.toString(respStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Logger.error("sendHttpPost failed");
            return null;
        }
    }

    private static Map<String, String> extractCookies(List<String> cookie) {
        Map<String, String> cookies = new HashMap<>();

        for (String line : cookie) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String[] split = line.split("\t");

            if (split.length < 2) {
                System.err.println("Skipping malformed cookie line: " + line);
                continue;
            }

            String key = split[split.length - 2];
            String value = split[split.length - 1];
            cookies.put(key, value);
        }

        return cookies;
    }

    private static List<String> cookieToLines(String cookie) {
        return Arrays.asList(cookie.split("\n"));
    }

    public AltAuthResult loginWithCredentials(SavedAltData altData, String email, String password) {
        altData.type = SavedLoginType.CREDENTIALS;
        altData.emailOrRefreshTokenOrCookie = email;
        altData.password = password;
        altData.authStatus = AltAuthStatus.VERIFIED;
        // TODO: EMAIL PASSWORD LOGIN
        return AltAuthResult.SUCCESS;
    }

    public AltAuthResult loginWithCookie(SavedAltData alt, String cookie) {
        return loginWithCookie(alt, cookieToLines(cookie), cookie);
    }

    public AltAuthResult loginWithCookie(SavedAltData alt, List<String> cookie) {
        return loginWithCookie(alt, cookie, null);
    }

    public AltAuthResult loginWithCookie(SavedAltData alt, List<String> cookie, String splitCookie) {
        alt.type = SavedLoginType.COOKIE;
        alt.emailOrRefreshTokenOrCookie = splitCookie != null ? splitCookie : String.join("\n", cookie);

        Map<String, String> cookies = extractCookies(cookie);

        if (cookies.isEmpty()) return AltAuthResult.NO_COOKIES_GIVEN;

        String xbl = sendHttpGetXblSisu("https://sisu.xboxlive.com/connect/XboxLive/?state=login" +
                "&cobrandId=8058f65d-ce06-4c30-9559-473c9275a65d" +
                "&tid=896928775" +
                "&ru=https://www.minecraft.net/en-us/login" +
                "&aid=1142970254", cookies);

        if (xbl != null && xbl.startsWith("XBL3.0 x=")) {
            McResponse mcRes = gson.fromJson(sendHttpPost("https://api.minecraftservices.com/authentication/login_with_xbox",
                            "{\"identityToken\":\"" + xbl + "\"}", true),
                    McResponse.class);
            if (mcRes == null)
                return null;

            ProfileResponse proResp = fetchProfile(mcRes.access_token);
            if (proResp == null)
                return null;

            alt.profileID = proResp.id;
            alt.cachedUsername = proResp.name;
            alt.accessToken = mcRes.access_token;
            alt.authStatus = AltAuthStatus.VERIFIED;
            return AltAuthResult.SUCCESS;
        } else {
            return AltAuthResult.SISU_XBL_LOGIN_FAILED;
        }
    }

    public void startBrowserLogin(boolean copyToClip) {
        startServer();

        String url = "https://login.live.com/oauth20_authorize.srf?client_id=" + clientID + "&client_secret=" + clientSecret +
                "&response_type=code&redirect_uri=http://localhost:" + PORT + "&scope=XboxLive.signin%20offline_access";

        if (copyToClip) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(url), null);
        } else {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                Logger.error(e);
            }
        }
    }

    public AltAuthResult loginWithRefreshToken(SavedAltData alt, String refreshToken) {
        alt.type = SavedLoginType.REFRESH_TOKEN;
        alt.emailOrRefreshTokenOrCookie = refreshToken;

        AuthTokenResponse res = gson.fromJson(sendHttpPost("https://login.live.com/oauth20_token.srf",
                "client_id=" + clientID + "&client_secret=" + clientSecret + "&refresh_token=" + refreshToken + "&grant_type=refresh_token&redirect_uri=http://localhost:" + PORT,
                false), AuthTokenResponse.class);
        if (res == null)
            return AltAuthResult.LIVE_LOGIN_FAILED;

        return loginWithAccessToken(alt, res.access_token);
    }

    public AltAuthResult loginWithAccessToken(SavedAltData alt, String accessToken) {
        XblXstsResponse xblRes = gson.fromJson(sendHttpPost("https://user.auth.xboxlive.com/user/authenticate",
                "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=" + accessToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}",
                true), XblXstsResponse.class);
        if (xblRes == null)
            return AltAuthResult.USER_XBL_AUTH_FAILED;

        XblXstsResponse xstsRes = gson.fromJson(sendHttpPost("https://xsts.auth.xboxlive.com/xsts/authorize",
                "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblRes.Token + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}",
                true), XblXstsResponse.class);
        if (xstsRes == null)
            return AltAuthResult.XSTS_XBL_AUTH_FAILED;

        McResponse mcRes = gson.fromJson(sendHttpPost("https://api.minecraftservices.com/authentication/login_with_xbox",
                        "{\"identityToken\":\"XBL3.0 x=" + xblRes.DisplayClaims.xui[0].uhs + ";" + xstsRes.Token + "\"}", true),
                McResponse.class);
        if (mcRes == null)
            return AltAuthResult.XSTS_XBL_AUTH_FAILED;

        alt.accessToken = mcRes.access_token;

        ProfileResponse proResp = fetchProfile(mcRes.access_token);
        if (proResp == null)
            return AltAuthResult.PROFILE_FETCH_FAILED;

        alt.profileID = proResp.id;
        alt.cachedUsername = proResp.name;
        alt.authStatus = AltAuthStatus.VERIFIED;
        return AltAuthResult.SUCCESS;
    }

    private void startLiveLogin(String code) {
        String body = sendHttpPost("https://login.live.com/oauth20_token.srf",
                "client_id=" + clientID + "&code=" + code + "&client_secret=" + clientSecret + "&grant_type=authorization_code&redirect_uri=http://localhost:" + PORT,
                false);
        AuthTokenResponse resp = gson.fromJson(body, AuthTokenResponse.class);
        if (resp == null)
            return;

        SavedAltData alt = new SavedAltData();
        AltAuthResult result = loginWithRefreshToken(alt, resp.refresh_token);
        if (result == AltAuthResult.SUCCESS) {
            authCallback.accept(alt);
        } else {
            ClientManager.getInstance().getNotificationManager().addNotification("Microsoft auth failed", "Unable to login with microsoft alt", 3000);
        }
    }

    public ProfileResponse fetchProfile(String mcAccessToken) {
        GameOwnershipResponse gameOwnershipRes = gson.fromJson(sendHttpReqBearer("https://api.minecraftservices.com/entitlements/mcstore",
                mcAccessToken), GameOwnershipResponse.class);
        if (gameOwnershipRes == null || !gameOwnershipRes.hasGameOwnership())
            return null;

        return gson.fromJson(sendHttpReqBearer("https://api.minecraftservices.com/minecraft/profile",
                mcAccessToken), ProfileResponse.class);
    }

    private void handleHttpRequest(HttpExchange req) throws IOException {
        if (req.getRequestMethod().equals("GET")) {
            List<NameValuePair> query = URLEncodedUtils.parse(req.getRequestURI(), StandardCharsets.UTF_8.name());

            String resp = "<html>Failed to start authentication.</html>";
            for (NameValuePair pair : query) {
                if (pair.getName().equals("code")) {
                    startLiveLogin(pair.getValue());
                    resp = "<html>Starting authentication. You may close this window.</html>";
                    break;
                }
            }

            req.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            byte[] respBytes = resp.getBytes(StandardCharsets.UTF_8);
            req.sendResponseHeaders(200, respBytes.length);
            req.getResponseBody().write(respBytes);
            req.getResponseBody().flush();
            req.getResponseBody().close();
        }

        stopServer();
    }

    private void startServer() {
        if (server != null) {
            stopServer();
        }

        try {
            server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
            server.createContext("/", this::handleHttpRequest);
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            Logger.error(e);
        }
    }

    private void stopServer() {
        server.stop(0);
    }
}