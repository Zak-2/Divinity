package divinity.module.impl.render;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.render.RenderGuiEvent;
import divinity.event.impl.render.RenderWorldEvent;
import divinity.event.impl.world.LoadWorldEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.impl.render.esp.ESPUtils;
import divinity.module.impl.render.esp.FrustumUtils;
import divinity.module.impl.render.esp.SkeletonUtils;
import divinity.module.property.impl.*;
import divinity.utils.ColorUtils;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;
import divinity.utils.player.PlayerUtils;
import divinity.utils.server.ServerUtils;
import divinity.voice.VoiceClient;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.vecmath.Vector3d;
import net.vecmath.Vector4d;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class ESP extends Module {
    private static final MultiSelectProperty entitiesProperty = new MultiSelectProperty("Targets", new String[]{"LocalPlayer", "Invisible Entities", "NPC", "TabList only"}, new String[]{"LocalPlayer", "TabList Only"});
    private static final MultiSelectProperty otherEspProperty = new MultiSelectProperty("More ESP options", new String[]{"Chest"}, new String[]{"Chest"});
    
    public final BooleanProperty showEspPreview = new BooleanProperty("Show ESP Preview", true);
    public final BooleanProperty enableEspDragging = new BooleanProperty("Enable ESP Dragging", false);
    public final NumberProperty<Float> espPreviewScale = new NumberProperty<>("ESP Preview Scale", 1.0f, 0.5f, 2.0f, 0.1f);

    // ESP Preview Offsets
    public final NumberProperty<Float> healthBarXOffset = new NumberProperty<>("HealthBar X Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> healthBarYOffset = new NumberProperty<>("HealthBar Y Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> armorBarXOffset = new NumberProperty<>("ArmorBar X Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> armorBarYOffset = new NumberProperty<>("ArmorBar Y Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> nametagXOffset = new NumberProperty<>("Nametag X Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> nametagYOffset = new NumberProperty<>("Nametag Y Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> healthTagXOffset = new NumberProperty<>("HealthTag X Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> healthTagYOffset = new NumberProperty<>("HealthTag Y Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> heldItemXOffset = new NumberProperty<>("HeldItem X Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> heldItemYOffset = new NumberProperty<>("HeldItem Y Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> armorItemsXOffset = new NumberProperty<>("ArmorItems X Offset", 0f, -200f, 200f, 1f);
    public final NumberProperty<Float> armorItemsYOffset = new NumberProperty<>("ArmorItems Y Offset", 0f, -200f, 200f, 1f);

    public final BooleanProperty renderBox = new BooleanProperty("Box", false);
    public final BooleanProperty boxOutline = new BooleanProperty("Box Outline", false, renderBox::getValue);
    public final BooleanProperty armorBar = new BooleanProperty("Armor Bar", true);
    public final BooleanProperty skeleton = new BooleanProperty("Skeleton", true);
    private final ModeProperty boxType = new ModeProperty("Box Type", "Corners", renderBox::getValue, "Corners", "Full");
    private final ColorProperty boxColor = new ColorProperty("Box Color", ClientManager.getInstance().getMainColor(), renderBox::getValue);
    private final NumberProperty<Integer> secondsToPersist = new NumberProperty<>("Seconds to persist", 0, 0, 12, 1);
    private final ColorProperty chestColor = new ColorProperty("Chest Color", ClientManager.getInstance().getMainColor(), () -> otherEspProperty.isSelected("Chest"));
    
    private final Map<EntityPlayer, float[][]> entities = new HashMap<>();
    private final Map<EntityPlayer, PlayerData> lastKnownData = new HashMap<>();
    private final IntBuffer viewport = GLAllocation.createDirectIntBuffer(16);
    private final FloatBuffer modelView = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer projection = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer vector = GLAllocation.createDirectFloatBuffer(4);
    ESPUtils espUtils = new ESPUtils();

    public ESP(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(renderBox, boxType, boxColor, boxOutline, armorBar, skeleton, secondsToPersist, entitiesProperty, otherEspProperty, chestColor, showEspPreview, enableEspDragging, espPreviewScale, healthBarXOffset, healthBarYOffset, armorBarXOffset, armorBarYOffset, nametagXOffset, nametagYOffset, healthTagXOffset, healthTagYOffset, heldItemXOffset, heldItemYOffset, armorItemsXOffset, armorItemsYOffset);
    }

    private static List<net.vecmath.Vector3d> getVector3ds(RenderGuiEvent event, EntityLivingBase entity, AxisAlignedBB aabb) {
        return Arrays.asList(new net.vecmath.Vector3d(aabb.minX, aabb.minY, aabb.minZ),
                new net.vecmath.Vector3d(aabb.minX, aabb.maxY, aabb.minZ), new net.vecmath.Vector3d(aabb.maxX, aabb.minY, aabb.minZ),
                new net.vecmath.Vector3d(aabb.maxX, aabb.maxY, aabb.minZ), new net.vecmath.Vector3d(aabb.minX, aabb.minY, aabb.maxZ),
                new net.vecmath.Vector3d(aabb.minX, aabb.maxY, aabb.maxZ), new net.vecmath.Vector3d(aabb.maxX, aabb.minY, aabb.maxZ),
                new net.vecmath.Vector3d(aabb.maxX, aabb.maxY, aabb.maxZ));
    }

    private static AxisAlignedBB getAxisAlignedBB(RenderGuiEvent event, EntityLivingBase entity) {
        double x = RenderUtils.interpolateScale(entity.posX, entity.lastTickPosX, event.getPartialTicks());
        double y = RenderUtils.interpolateScale(entity.posY, entity.lastTickPosY, event.getPartialTicks());
        double z = RenderUtils.interpolateScale(entity.posZ, entity.lastTickPosZ, event.getPartialTicks());
        double width = entity.width / 1.5;
        double height = entity.height + (entity.isSneaking() ? -0.3 : 0.2);
        return new AxisAlignedBB(x - width, y, z - width, x + width, y + height, z + width);
    }

    @EventListener
    public void onEvent(LoadWorldEvent event) {
        lastKnownData.clear();
    }

    @EventListener
    public void onEvent(RenderGuiEvent event) {
        try {
            glPushMatrix();
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (isValid(player)) {
                    AxisAlignedBB aabb = getAxisAlignedBB(event, player);
                    float[][] rotations = entities.get(player);
                    if (rotations != null) {
                        PlayerData data = lastKnownData.get(player);
                        if (data == null) {
                            data = new PlayerData(rotations, aabb, event.getPartialTicks());
                            lastKnownData.put(player, data);
                        } else {
                            data.boundingBox = aabb;
                            data.rotations = rotations;
                            data.partialTicks = event.getPartialTicks();
                            data.deathTime = -1;
                        }
                    }
                }
            }

            long currentTime = System.currentTimeMillis();
            lastKnownData.entrySet().removeIf(entry -> {
                EntityPlayer player = entry.getKey();
                PlayerData data = entry.getValue();

                if (!player.isEntityAlive() && data.deathTime == -1) {
                    data.deathTime = currentTime;
                }

                return !player.isEntityAlive() && data.deathTime != -1 && (currentTime - data.deathTime) >= secondsToPersist.getValue().intValue() * 1000L;
            });

            for (Map.Entry<EntityPlayer, PlayerData> entry : lastKnownData.entrySet()) {
                EntityPlayer player = entry.getKey();
                PlayerData data = entry.getValue();

                if (isValid(player)) {
                    renderESP(event, player, data.boundingBox, data.partialTicks);
                }
            }

        } catch (Exception exception) {
            exception.getSuppressed();
        } finally {
            GL11.glPopMatrix();
            GlStateManager.enableBlend();
            mc.entityRenderer.setupOverlayRendering();
        }
    }

    private void renderESP(RenderGuiEvent event, EntityPlayer entity, AxisAlignedBB aabb, float partialTicks) {
        java.util.List<Vector3d> vectors = getVector3ds(event, entity, aabb);
        mc.entityRenderer.setupCameraTransform(partialTicks, 0);
        Vector4d position = null;
        for (Vector3d vector : vectors) {
            vector = project2D(event.getSr(), vector.x - RenderManager.viewerPosX,
                    vector.y - RenderManager.viewerPosY, vector.z - RenderManager.viewerPosZ);
            if (vector != null && vector.z >= 0.0 && vector.z < 1.0) {
                if (position == null) {
                    position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
                }
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);
                position.w = Math.max(vector.y, position.w);
            }
        }
        mc.entityRenderer.setupOverlayRendering();
        if (position != null) {
            double boxWidth = .5f;
            double posX = position.x;
            double posY = position.y;
            double endPosX = position.z;
            double endPosY = position.w;
            int boxColor = this.boxColor.getValue().getRGB();
            final int black = new Color(0, 0, 0, 255).getRGB();
            if (renderBox.getValue()) {
                switch (boxType.getValue()) {
                    case "Full":
                        // Left
                        RenderUtils.drawRect(posX - .5, posY, posX + boxWidth - .5, endPosY,
                                boxColor);
                        // Bottom
                        RenderUtils.drawRect(posX, endPosY - boxWidth, endPosX, endPosY,
                                boxColor);
                        // Top
                        RenderUtils.drawRect(posX - .5, posY, endPosX, posY + boxWidth,
                                boxColor);
                        // Right
                        RenderUtils.drawRect(endPosX - boxWidth, posY, endPosX, endPosY,
                                boxColor);
                        break;
                    case "Corners":
                        final float perc = 0.4f / 2f;

                        RenderUtils.drawRect(posX - 0.5f, posY, posX, posY + (endPosY - posY) * perc,
                                boxColor);
                        RenderUtils.drawRect(posX - .5, endPosY - (endPosY - posY) * perc, posX, endPosY,
                                boxColor);
                        RenderUtils.drawRect(posX - .5, posY, posX + (endPosX - posX) * perc, posY + .5,
                                boxColor);
                        RenderUtils.drawRect(endPosX - (endPosX - posX) * perc, posY, endPosX, posY + .5,
                                boxColor);
                        RenderUtils.drawRect(endPosX - .5, posY, endPosX, posY + (endPosY - posY) * perc,
                                boxColor);
                        RenderUtils.drawRect(endPosX - .5, endPosY - (endPosY - posY) * perc, endPosX, endPosY,
                                boxColor);
                        RenderUtils.drawRect(posX, endPosY - .5, posX + (endPosX - posX) * perc, endPosY,
                                boxColor);
                        RenderUtils.drawRect(endPosX - (endPosX - posX) * perc, endPosY - .5, endPosX - .5, endPosY,
                                boxColor);
                        break;
                }
            }

            // health
            RenderUtils.drawRect((float) (endPosX - .5 - boxWidth + 3), (float) (endPosY - .5 + (posY - endPosY + .5) * PlayerUtils.healthPercentage(entity)), (float) (endPosX + .5 + 2.5f), (float) (endPosY + .5), ColorUtils.getHealthColor(entity).getRGB());
            RenderUtils.drawRectOutline((float) (endPosX - .5 - boxWidth + 3), (float) (endPosY - .5 + (posY - endPosY + .5)), (float) (endPosX + .5 + 2.5f), (float) (endPosY + .5), 0.5f, black);

            if (armorBar.getValue()) {
                RenderUtils.drawRect((float) posX - 1, (float) (endPosY - boxWidth) + 3, (float) (posX - 1 + (endPosX + 1 - posX) * PlayerUtils.armorPercentage(entity)), (float) endPosY + 3.5f, Color.CYAN.getRGB());
                RenderUtils.drawRectOutline((float) posX - 1, (float) (endPosY - boxWidth) + 3, (float) (posX - 1 + (endPosX + 1 - posX)), (float) endPosY + 3.5f, 0.5f, black);
            }

            String healthText = String.format("%.1f" + EnumChatFormatting.RED + " \u2764", entity.getHealth());
            double healthWidth = Fonts.INTER_MEDIUM.get(10).getStringWidth(healthText) + 3;
            healthWidth = Math.min(healthWidth, 100);

            String accountName = entity.getName();
            double nameWidth1 = Fonts.INTER_MEDIUM.get(10).getStringWidth(accountName) + 3;
            nameWidth1 = Math.min(nameWidth1, 100);

            String ircUsername = null;

            double nameWidth2 = 0;

            if (ircUsername != null) {
                nameWidth2 = Fonts.INTER_MEDIUM.get(10).getStringWidth(ircUsername) + 3;
                nameWidth2 = Math.min(nameWidth2, 100);
            }

            // Check if VC is active
            boolean isVcActive = ClientManager.getInstance().getModuleManager().proximityChat != null
                    && ClientManager.getInstance().getModuleManager().proximityChat.isState()
                    && ClientManager.getInstance().getModuleManager().proximityChat.espSpeaking.getValue()
                    && VoiceClient.getInstance().isUserSpeaking(entity.getName());

            double vcWidth = 0;
            if (isVcActive) {
                String vcText = "[VC]";
                vcWidth = Fonts.INTER_MEDIUM.get(10).getStringWidth(vcText) + 3;
            }

            double gap = 2;

            double combinedWidth = healthWidth + gap + nameWidth1;

            if (isVcActive) combinedWidth += gap + vcWidth;
            if (ircUsername != null) combinedWidth += gap + nameWidth2;

            double centerX = posX + (endPosX - posX) / 2.0;

            double groupLeft = centerX - (combinedWidth / 2.0);

            double rectTop = posY - 10;
            double rectBottom = rectTop + 8;

            // Health rect
            {
                double healthRightX = groupLeft + healthWidth;

                RenderUtils.drawRect(
                        (float) groupLeft,
                        (float) rectTop,
                        (float) healthRightX,
                        (float) rectBottom,
                        new Color(0, 0, 0, 120).getRGB()
                );

                Fonts.INTER_MEDIUM.get(10).drawStringWithShadow(
                        healthText,
                        (float) (groupLeft + 1.5f),
                        (float) (rectTop + 3.5f),
                        0xFFFFFFFF);
            }

            // Name rect
            {
                double nameLeftX = groupLeft + healthWidth + gap;
                double nameRightX = nameLeftX + nameWidth1;

                RenderUtils.drawRect(
                        (float) nameLeftX,
                        (float) rectTop,
                        (float) nameRightX,
                        (float) rectBottom,
                        new Color(0, 0, 0, 120).getRGB()
                );

                String coloredAccount = (ClientManager.getInstance().getFriendManager().isFriend(entity.getName())
                        ? EnumChatFormatting.GREEN
                        : EnumChatFormatting.WHITE)
                        + accountName
                        + EnumChatFormatting.WHITE;

                Fonts.INTER_MEDIUM.get(10).drawStringWithShadow(
                        coloredAccount,
                        (float) (nameLeftX + 1.5f),
                        (float) (rectTop + 3.5f),
                        ColorUtils.getColorRGB(PlayerUtils.getPlayerTeamColor(entity)));
            }

            // VC Indicator rect
            if (isVcActive) {
                double vcLeftX = groupLeft + healthWidth + gap + nameWidth1 + gap;
                String vcText = "[VC]";
                double vcRightX = vcLeftX + vcWidth;

                RenderUtils.drawRect(
                        (float) vcLeftX,
                        (float) rectTop,
                        (float) vcRightX,
                        (float) rectBottom,
                        new Color(0, 0, 0, 120).getRGB()
                );

                Fonts.INTER_MEDIUM.get(10).drawStringWithShadow(
                        EnumChatFormatting.AQUA + vcText,
                        (float) (vcLeftX + 1.5f),
                        (float) (rectTop + 3.5f),
                        0xFFFFFFFF);
            }

            // IRC Username rect
            if (ircUsername != null) {
                double ircLeftX = groupLeft + healthWidth + gap + nameWidth1 + gap;
                if (isVcActive) {
                    ircLeftX += vcWidth + gap;
                }
                double ircRightX = ircLeftX + nameWidth2;

                // Background
                RenderUtils.drawRect(
                        (float) ircLeftX,
                        (float) rectTop,
                        (float) ircRightX,
                        (float) rectBottom,
                        new Color(0, 0, 0, 120).getRGB()
                );

                Fonts.INTER_MEDIUM.get(10).drawStringGradient(
                        ircUsername,
                        (float) (ircLeftX + 1f),
                        (float) (rectTop + 3.5f),
                        ClientManager.getInstance().getMainColor().getRGB(),
                        ClientManager.getInstance().getSecondaryColor().getRGB()
                );
            }

            ItemStack heldItem = entity.getHeldItem();
            List<ItemStack> items = new ArrayList<>();
            if (heldItem != null) {
                items.add(heldItem);
            }

            for (int i = 0; i < 4; i++) {
                ItemStack armor = entity.inventory.armorInventory[i];
                if (armor != null) {
                    items.add(armor);
                }
            }

            if (!items.isEmpty()) {
                float itemX = (float) (endPosX + 5);
                float itemY = (float) posY;
                for (ItemStack item : items) {
                    RenderUtils.renderItemStack(item, (int) itemX, (int) itemY, 1);
                    itemY += 16;
                }
            }
        }
    }

    private Vector3d project2D(ScaledResolution sr, double x, double y, double z) {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        if (GLU.gluProject((float) x, (float) y, (float) z, modelView, projection, viewport, vector)) {
            return new Vector3d(vector.get(0) / sr.getScaleFactor(), (Display.getHeight() - vector.get(1)) / sr.getScaleFactor(), vector.get(2));
        }
        return null;
    }

    private boolean isValid(EntityLivingBase entity) {
        if (entity == mc.thePlayer) return entitiesProperty.isSelected("LocalPlayer");
        if (entity.isInvisible()) return entitiesProperty.isSelected("Invisible Entities");
        if (entity instanceof EntityPlayer && PlayerUtils.isNPC((EntityPlayer) entity)) return entitiesProperty.isSelected("NPC");
        if (entity instanceof EntityPlayer && !ServerUtils.isTabListed((EntityPlayer) entity)) return entitiesProperty.isSelected("TabList only");
        return true;
    }

    private static class PlayerData {
        public float[][] rotations;
        public AxisAlignedBB boundingBox;
        public float partialTicks;
        public long deathTime = -1;

        public PlayerData(float[][] rotations, AxisAlignedBB boundingBox, float partialTicks) {
            this.rotations = rotations;
            this.boundingBox = boundingBox;
            this.partialTicks = partialTicks;
        }
    }
}
