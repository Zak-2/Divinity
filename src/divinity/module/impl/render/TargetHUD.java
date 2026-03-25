package divinity.module.impl.render;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.render.RenderGuiEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.impl.combat.Aura;
import divinity.utils.AnimationUtils;
import divinity.utils.ColorUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import divinity.utils.math.MathUtils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class TargetHUD extends Module {

    public TargetHUD(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    public void onEvent(RenderGuiEvent event) {
        ClientManager.getInstance().getModuleManager().targetHUD.doRenderTargetHud();
    }

    public void doRenderTargetHud() {
        Aura aura = ClientManager.getInstance().getModuleManager().aura;
        if (isState()) {
            ScaledResolution sr = new ScaledResolution(mc);
            for (Entity base : mc.theWorld.loadedEntityList) {
                if (base instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) base;
                    List<EntityLivingBase> targets;

                    targets = new ArrayList<>();

                    if (mc.currentScreen instanceof GuiChat && (aura.target == null || !ClientManager.getInstance().getModuleManager().aura.isState()))
                        targets.add(mc.thePlayer);
                    else targets.add(aura.target);

                    if (targets.contains(base)) {
                        if (player.targetHud == null) {
                            player.targetHud = new THud(player);
                        }
                        player.targetHud.render(sr.getScaledWidth() / 2f + 18, sr.getScaledHeight() / 2f - 17.5f + 35);
                    } else if (player.targetHud != null) {
                        player.targetHud.animation = 0;
                    }
                }
            }
        }
    }

    public static class THud {
        public final EntityPlayer ent;
        public float animation = 0;

        public THud(EntityPlayer player) {
            this.ent = player;
        }

        public void render(float x, float y) {
            GL11.glPushMatrix();
            String playerName = ent.getName();
            String username = null;

            float nameWidth = Fonts.INTER_MEDIUM.get(16).getStringWidth(playerName);
            float usernameWidth = (username != null) ? Fonts.INTER_MEDIUM.get(16).getStringWidth(" (" + username + ")") : 0;
            float totalWidth = nameWidth + usernameWidth;

            float width = Math.max(85, totalWidth + 15);

            GL11.glTranslatef(x, y, 0);
            ShaderUtils.drawRoundRect(0, 0, 30 + width, 30, 1,RenderUtils.reAlpha(0xff000000, 0.6f));

            Fonts.INTER_MEDIUM.get(16).drawString(playerName, 31f, 5f, ColorUtils.Colors.WHITE.c);

            if (username != null) {
                Fonts.INTER_MEDIUM.get(16).drawStringGradient(" (" + username + ")", 31f + nameWidth, 5f, ClientManager.getInstance().getMainColor().getRGB(), ClientManager.getInstance().getSecondaryColor().getRGB());
            }

            String healthStr = Math.round(ent.getHealth() * 10) / 10d + " hp";
            Fonts.INTER_MEDIUM.get(14).drawString(healthStr, 29 + width - Fonts.INTER_MEDIUM.get(14).getStringWidth(healthStr) - 2, 23, 0xffcccccc);

            boolean isNaN = Float.isNaN(ent.getHealth());
            float health = isNaN ? 20 : ent.getHealth();
            float maxHealth = isNaN ? 20 : ent.getMaxHealth();
            float healthPercent = MathUtils.clamp(health / maxHealth, 0f, 1f);
            RenderUtils.drawRectBound(31, 15f, width - 4, 5f, RenderUtils.reAlpha(ColorUtils.Colors.BLACK.c, 0.35f));

            float barWidth = width - 6;
            float drawPercent = (barWidth / 100) * (healthPercent * 100);

            if (this.animation <= 0) {
                this.animation = drawPercent;
            }

            if (ent.hurtTime <= 6) {
                this.animation = AnimationUtils.smoothAnimation(this.animation, drawPercent, 30f, 0.4f);
            }

            int col = ClientManager.getInstance().getMainColor().getRGB();
            RenderUtils.drawRectBound(32, 16f, this.animation, 3f, RenderUtils.reAlpha(RenderUtils.darker(col, 25), 0.95f));
            RenderUtils.drawRectBound(32, 16f, drawPercent, 3f, RenderUtils.reAlpha(col, 0.95f));

            float amorBarWidth = width - 34;

            float f3 = (amorBarWidth / 100f) * (ent.getTotalArmorValue() * 5);
            RenderUtils.drawRectBound(31, 22f, width - 32, 5f, RenderUtils.reAlpha(ColorUtils.Colors.BLACK.c, 0.35f));
            RenderUtils.drawRectBound(32, 23f, f3, 3f, ClientManager.getInstance().getSecondaryColor().getRGB());
            float scale = 0.6f;

            GL11.glColor4f(1, 1, 1, 1);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDepthMask(false);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

            boolean foundSkin = false;
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (mc.theWorld.getPlayerEntityByUUID(info.getGameProfile().getId()) == ent) {
                    mc.getTextureManager().bindTexture(info.getLocationSkin());
                    RenderUtils.drawScaledCustomSizeModalRect(3f, 3f, 8.0f, 8.0f, 8, 8, 24, 24, 64.0f, 64.0f);
                    if (ent.isWearing(EnumPlayerModelParts.HAT)) {
                        RenderUtils.drawScaledCustomSizeModalRect(3f, 3f, 40.0f, 8.0f, 8, 8, 24, 24, 64.0f, 64.0f);
                    }
                    GlStateManager.bindTexture(0);
                    foundSkin = true;
                    break;
                }
            }

            if (!foundSkin) {
                mc.getTextureManager().bindTexture(DefaultPlayerSkin.getDefaultSkin(ent.getUniqueID()));
                RenderUtils.drawScaledCustomSizeModalRect(3f, 3f, 8.0f, 8.0f, 8, 8, 24, 24, 64.0f, 64.0f);
                RenderUtils.drawScaledCustomSizeModalRect(3f, 3f, 40.0f, 8.0f, 8, 8, 24, 24, 64.0f, 64.0f);
                GlStateManager.bindTexture(0);
            }

            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GlStateManager.resetColor();
            GL11.glPopMatrix();
        }
    }
}
