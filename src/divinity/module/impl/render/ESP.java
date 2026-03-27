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
    
    // ESP Preview Properties (Only show preview toggle in ClickGUI)
    public final BooleanProperty showEspPreview = new BooleanProperty("Show ESP Preview", true);
    public final NumberProperty<Float> espPreviewScale = new NumberProperty<>("ESP Preview Scale", 1.0f, 0.5f, 2.0f, 0.1f);

    // Internal Offsets (Not added as properties to the ClickGUI to avoid clutter)
    public final NumberProperty<Float> healthBarXOffset = new NumberProperty<>("healthBarXOffset", 0f, -500f, 500f, 1f);
    public final NumberProperty<Float> healthBarYOffset = new NumberProperty<>("healthBarYOffset", 0f, -500f, 500f, 1f);
    public final NumberProperty<Float> armorBarXOffset = new NumberProperty<>("armorBarXOffset", 0f, -500f, 500f, 1f);
    public final NumberProperty<Float> armorBarYOffset = new NumberProperty<>("armorBarYOffset", 0f, -500f, 500f, 1f);
    public final NumberProperty<Float> nametagXOffset = new NumberProperty<>("nametagXOffset", 0f, -500f, 500f, 1f);
    public final NumberProperty<Float> nametagYOffset = new NumberProperty<>("nametagYOffset", 0f, -500f, 500f, 1f);
    public final NumberProperty<Float> heldItemXOffset = new NumberProperty<>("heldItemXOffset", 0f, -500f, 500f, 1f);
    public final NumberProperty<Float> heldItemYOffset = new NumberProperty<>("heldItemYOffset", 0f, -500f, 500f, 1f);
    public final NumberProperty<Float> armorItemsXOffset = new NumberProperty<>("armorItemsXOffset", 0f, -500f, 500f, 1f);
    public final NumberProperty<Float> armorItemsYOffset = new NumberProperty<>("armorItemsYOffset", 0f, -500f, 500f, 1f);

    public final BooleanProperty renderBox = new BooleanProperty("Box", false);
    public final BooleanProperty boxOutline = new BooleanProperty("Box Outline", false, renderBox::getValue);
    public final BooleanProperty armorBar = new BooleanProperty("Armor Bar", true);
    public final BooleanProperty skeleton = new BooleanProperty("Skeleton", true);
    public final ModeProperty boxType = new ModeProperty("Box Type", "Corners", renderBox::getValue, "Corners", "Full");
    public final ColorProperty boxColor = new ColorProperty("Box Color", ClientManager.getInstance().getMainColor(), renderBox::getValue);
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
        // Register properties for persistence but hide offsets from UI
        addProperty(
            renderBox, boxType, boxColor, boxOutline, armorBar, skeleton, secondsToPersist, 
            entitiesProperty, otherEspProperty, chestColor, 
            showEspPreview, espPreviewScale,
            healthBarXOffset, healthBarYOffset, armorBarXOffset, armorBarYOffset, 
            nametagXOffset, nametagYOffset, heldItemXOffset, heldItemYOffset, 
            armorItemsXOffset, armorItemsYOffset
        );
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

            long currentTime = System.currentTimeMillis();
            lastKnownData.entrySet().removeIf(entry -> {
                EntityPlayer player = entry.getKey();
                PlayerData data = entry.getValue();
                if (!player.isEntityAlive() && data.deathTime == -1) data.deathTime = currentTime;
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
            exception.printStackTrace();
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
                if (position == null) position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
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
                if (boxType.getValue().equals("Full")) {
                    RenderUtils.drawRect(posX - .5, posY, posX + boxWidth - .5, endPosY, boxColor);
                    RenderUtils.drawRect(posX, endPosY - boxWidth, endPosX, endPosY, boxColor);
                    RenderUtils.drawRect(posX - .5, posY, endPosX, posY + boxWidth, boxColor);
                    RenderUtils.drawRect(endPosX - boxWidth, posY, endPosX, endPosY, boxColor);
                } else {
                    final float perc = 0.2f;
                    RenderUtils.drawRect(posX - 0.5f, posY, posX, posY + (endPosY - posY) * perc, boxColor);
                    RenderUtils.drawRect(posX - .5, endPosY - (endPosY - posY) * perc, posX, endPosY, boxColor);
                    RenderUtils.drawRect(posX - .5, posY, posX + (endPosX - posX) * perc, posY + .5, boxColor);
                    RenderUtils.drawRect(endPosX - (endPosX - posX) * perc, posY, endPosX, posY + .5, boxColor);
                    RenderUtils.drawRect(endPosX - .5, posY, endPosX, posY + (endPosY - posY) * perc, boxColor);
                    RenderUtils.drawRect(endPosX - .5, endPosY - (endPosY - posY) * perc, endPosX, endPosY, boxColor);
                    RenderUtils.drawRect(posX, endPosY - .5, posX + (endPosX - posX) * perc, endPosY, boxColor);
                    RenderUtils.drawRect(endPosX - (endPosX - posX) * perc, endPosY - .5, endPosX - .5, endPosY, boxColor);
                }
            }

            // Health Bar with Offsets
            float hX = (float) (posX - 3.5f) + healthBarXOffset.getValue();
            float hY = (float) posY + healthBarYOffset.getValue();
            float hXEnd = (float) (posX - 1f) + healthBarXOffset.getValue();
            float hYEnd = (float) endPosY + healthBarYOffset.getValue();
            float healthPerc = PlayerUtils.healthPercentage(entity);
            RenderUtils.drawRect(hX, hYEnd - (hYEnd - hY) * healthPerc, hXEnd, hYEnd, ColorUtils.getHealthColor(entity).getRGB());
            RenderUtils.drawRectOutline(hX, hY, hXEnd, hYEnd, 0.5f, black);

            // Armor Bar with Offsets
            if (armorBar.getValue()) {
                float aX = (float) posX + armorBarXOffset.getValue();
                float aY = (float) endPosY + 1.5f + armorBarYOffset.getValue();
                float aXEnd = (float) endPosX + armorBarXOffset.getValue();
                float aYEnd = (float) endPosY + 3.5f + armorBarYOffset.getValue();
                float armorPerc = PlayerUtils.armorPercentage(entity);
                RenderUtils.drawRect(aX, aY, aX + (aXEnd - aX) * armorPerc, aYEnd, Color.CYAN.getRGB());
                RenderUtils.drawRectOutline(aX, aY, aXEnd, aYEnd, 0.5f, black);
            }

            // Nametag with Offsets
            String nameText = entity.getName();
            String healthText = String.format("%.1f", entity.getHealth());
            FontRenderer font = Fonts.INTER_MEDIUM.get(10);
            float nameW = font.getStringWidth(nameText) + 4;
            float healthW = font.getStringWidth(healthText) + 4;
            float combinedW = nameW + healthW + 2;
            float nX = (float) (posX + (endPosX - posX) / 2f - combinedW / 2f) + nametagXOffset.getValue();
            float nY = (float) posY - 12 + nametagYOffset.getValue();
            RenderUtils.drawRect(nX, nY, nX + combinedW, nY + 10, new Color(0, 0, 0, 150).getRGB());
            font.drawStringWithShadow(nameText, nX + 2, nY + 2, -1);
            font.drawStringWithShadow(healthText, nX + nameW + 2, nY + 2, ColorUtils.getHealthColor(entity).getRGB());

            // Items with Offsets
            ItemStack held = entity.getHeldItem();
            if (held != null) {
                RenderUtils.renderItemStack(held, (int) (endPosX + 2 + heldItemXOffset.getValue()), (int) (posY + heldItemYOffset.getValue()), 1f);
            }

            float aiY = (float) posY + 18 + armorItemsYOffset.getValue();
            for (int i = 0; i < 4; i++) {
                ItemStack armor = entity.inventory.armorInventory[i];
                if (armor != null) {
                    RenderUtils.renderItemStack(armor, (int) (endPosX + 2 + armorItemsXOffset.getValue()), (int) aiY, 1f);
                    aiY += 16;
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
