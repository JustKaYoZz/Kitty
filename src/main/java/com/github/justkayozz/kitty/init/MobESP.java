package com.github.justkayozz.kitty.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

@Mod(modid = "espmod", version = "1.0")
public class MobESP {

    public static boolean espEnabled = true;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!espEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        List<Entity> entities = mc.theWorld.loadedEntityList;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);

        for (Entity entity : entities) {
            if (entity == mc.thePlayer) continue;

            if (isStarredMob(entity)) {
                renderEntityESP(entity, event.partialTicks);
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private boolean isStarredMob(Entity entity) {
        String entityName = entity.getName();
        if (entityName == null) return false;

        if (entityName.contains("✯") || entityName.contains("⭐")) {
            return true;
        }

        String lowerName = entityName.toLowerCase();

        String[] starredMobNames = {
                "starred", "shadow assassin", "lost adventurer", "diamond guy", "king midas",
                "frozen adventurer", "angry archeologist", "crystal nucleus",
                "professor guardian", "sniper skeleton", "super archer",
                "crypt dreadlord", "watcher summon", "undead",
                "✯ skeleton", "✯ zombie", "✯ spider", "✯ creeper",
                "⭐ skeleton", "⭐ zombie", "⭐ spider", "⭐ creeper"
        };

        for (String mobName : starredMobNames) {
            if (lowerName.contains(mobName)) {
                return true;
            }
        }

        if (lowerName.matches(".*\\[lv\\d+\\].*") && (lowerName.contains("✯") || lowerName.contains("⭐"))) {
            return true;
        }

        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.getMaxHealth() > 100.0F) return true;
            if (!living.getActivePotionEffects().isEmpty()) return true;
        }

        return false;
    }

    private void renderEntityESP(Entity entity, float partialTicks) {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ;

        AxisAlignedBB bb = entity.getEntityBoundingBox();

        GL11.glColor4f(1.0F, 1.0F, 0.0F, 0.8F);

        drawBoundingBox(
                bb.minX - entity.posX + x, bb.minY - entity.posY + y, bb.minZ - entity.posZ + z,
                bb.maxX - entity.posX + x, bb.maxY - entity.posY + y, bb.maxZ - entity.posZ + z
        );

        renderNameTag(entity, x, bb.maxY - entity.posY + y + 0.3, z);
    }

    private void drawBoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        GL11.glBegin(GL11.GL_LINES);

        // Face inférieure
        GL11.glVertex3d(minX, minY, minZ);
        GL11.glVertex3d(maxX, minY, minZ);
        GL11.glVertex3d(maxX, minY, minZ);
        GL11.glVertex3d(maxX, minY, maxZ);
        GL11.glVertex3d(maxX, minY, maxZ);
        GL11.glVertex3d(minX, minY, maxZ);
        GL11.glVertex3d(minX, minY, maxZ);
        GL11.glVertex3d(minX, minY, minZ);

        // Face supérieure
        GL11.glVertex3d(minX, maxY, minZ);
        GL11.glVertex3d(maxX, maxY, minZ);
        GL11.glVertex3d(maxX, maxY, minZ);
        GL11.glVertex3d(maxX, maxY, maxZ);
        GL11.glVertex3d(maxX, maxY, maxZ);
        GL11.glVertex3d(minX, maxY, maxZ);
        GL11.glVertex3d(minX, maxY, maxZ);
        GL11.glVertex3d(minX, maxY, minZ);

        // Arêtes verticales
        GL11.glVertex3d(minX, minY, minZ);
        GL11.glVertex3d(minX, maxY, minZ);
        GL11.glVertex3d(maxX, minY, minZ);
        GL11.glVertex3d(maxX, maxY, minZ);
        GL11.glVertex3d(maxX, minY, maxZ);
        GL11.glVertex3d(maxX, maxY, maxZ);
        GL11.glVertex3d(minX, minY, maxZ);
        GL11.glVertex3d(minX, maxY, maxZ);

        GL11.glEnd();
    }

    private void renderNameTag(Entity entity, double x, double y, double z) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);

        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        GL11.glRotatef(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-0.025F, -0.025F, 0.025F);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Couleur or pour starred mobs
        int nameColor = 0xFFFFD700; // code ARGB or

        Minecraft.getMinecraft().fontRendererObj.drawString(entity.getName(),
                -Minecraft.getMinecraft().fontRendererObj.getStringWidth(entity.getName()) / 2,
                0, nameColor);

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }
}
