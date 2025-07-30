package com.github.justkayozz.kitty.init;

public class MobESP {

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
    public class ESPMod {

        public static boolean espEnabled = true;
        public static String targetName = ""; // Nom de la cible à tracker

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

            // Obtenir toutes les entités du monde
            List<Entity> entities = mc.theWorld.loadedEntityList;

            // Configuration OpenGL pour le rendu à travers les murs
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST); // Permet de voir à travers les murs
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(2.0F);

            for (Entity entity : entities) {
                if (entity == mc.thePlayer) continue; // Ignorer le joueur lui-même

                // Vérifier si c'est la cible recherchée
                if (shouldRenderEntity(entity)) {
                    renderEntityESP(entity, event.partialTicks);
                }
            }

            // Restaurer l'état OpenGL
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }

        private boolean shouldRenderEntity(Entity entity) {
            // Ignorer les joueurs pour les donjons
            if (entity instanceof EntityPlayer) {
                return false;
            }

            // Vérifier si c'est un starred mob d'Hypixel SkyBlock
            return isStarredMob(entity);
        }

        private boolean isStarredMob(Entity entity) {
            String entityName = entity.getName();
            if (entityName == null) return false;

            // Les starred mobs ont une étoile ✯ dans leur nom sur Hypixel
            if (entityName.contains("✯") || entityName.contains("⭐")) {
                return true;
            }

            // Vérifier les noms typiques des starred mobs dans les donjons
            String lowerName = entityName.toLowerCase();

            // Starred mobs communs dans les donjons SkyBlock
            String[] starredMobNames = {
                    "starred", "⭐", "✯",
                    // Floor specific starred mobs
                    "shadow assassin", "lost adventurer", "diamond guy", "king midas",
                    "frozen adventurer", "angry archeologist", "crystal nucleus",
                    "professor guardian", "sniper skeleton", "super archer",
                    "crypt dreadlord", "watcher summon", "undead",
                    // Noms avec préfixes étoilés
                    "✯ skeleton", "✯ zombie", "✯ spider", "✯ creeper",
                    "⭐ skeleton", "⭐ zombie", "⭐ spider", "⭐ creeper"
            };

            for (String mobName : starredMobNames) {
                if (lowerName.contains(mobName.toLowerCase())) {
                    return true;
                }
            }

            // Vérifier les patterns de nommage d'Hypixel pour les mobs étoilés
            // Format typique: "✯ [Lv100] Mob Name"
            if (lowerName.matches(".*\\[lv\\d+\\].*") && (lowerName.contains("✯") || lowerName.contains("⭐"))) {
                return true;
            }

            // Vérifier si le mob a des caractéristiques visuelles spéciales
            return hasStarredMobCharacteristics(entity);
        }

        private boolean hasStarredMobCharacteristics(Entity entity) {
            // Les starred mobs ont souvent des effets de particules ou des équipements spéciaux
            // Vérifier la santé élevée (starred mobs ont plus de HP)
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase livingEntity = (EntityLivingBase) entity;
                float maxHealth = livingEntity.getMaxHealth();

                // Les starred mobs ont généralement beaucoup plus de vie que les mobs normaux
                if (maxHealth > 100.0F) {
                    return true;
                }

                // Vérifier les effets de potion (starred mobs ont souvent des buffs)
                if (!livingEntity.getActivePotionEffects().isEmpty()) {
                    return true;
                }
            }

            return false;
        }

        private void renderEntityESP(Entity entity, float partialTicks) {
            RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();

            // Calculer la position interpolée de l'entité
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

            // Ajuster par rapport à la position de la caméra
            x -= renderManager.viewerPosX;
            y -= renderManager.viewerPosY;
            z -= renderManager.viewerPosZ;

            // Obtenir la hitbox de l'entité
            AxisAlignedBB boundingBox = entity.getEntityBoundingBox();

            // Calculer les dimensions de la hitbox
            double minX = boundingBox.minX - entity.posX + x;
            double minY = boundingBox.minY - entity.posY + y;
            double minZ = boundingBox.minZ - entity.posZ + z;
            double maxX = boundingBox.maxX - entity.posX + x;
            double maxY = boundingBox.maxY - entity.posY + y;
            double maxZ = boundingBox.maxZ - entity.posZ + z;

            // Couleur spéciale pour les starred mobs
            if (isStarredMob(entity)) {
                GL11.glColor4f(1.0F, 1.0F, 0.0F, 0.8F); // Jaune/or brillant pour les starred mobs
            } else if (entity instanceof EntityPlayer) {
                GL11.glColor4f(1.0F, 0.0F, 0.0F, 0.5F); // Rouge pour les joueurs
            } else {
                GL11.glColor4f(0.0F, 1.0F, 0.0F, 0.5F); // Vert pour les autres entités
            }

            // Dessiner la hitbox
            drawBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

            // Dessiner le nom de l'entité au-dessus
            renderNameTag(entity.getName(), x, maxY + 0.3, z);
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

        private void renderNameTag(String name, double x, double y, double z) {
            // Configuration pour le rendu du texte
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

            // Dessiner le nom avec couleur spéciale pour starred mobs
            int nameColor = isStarredMob(entity) ? 0xFFD700 : 0xFFFFFF; // Or pour starred, blanc pour autres
            Minecraft.getMinecraft().fontRendererObj.drawString(name,
                    -Minecraft.getMinecraft().fontRendererObj.getStringWidth(name) / 2,
                    0, nameColor);

            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDepthMask(true);
            GL11.glPopMatrix();
        }
    }

// ESPCommand.java - Commandes pour contrôler l'ESP
package com.example.espmod;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

    public class ESPCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "esp";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/esp [on/off/toggle]";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            if (args.length == 0) {
                // Toggle silencieux
                ESPMod.espEnabled = !ESPMod.espEnabled;
                return;
            }

            switch (args[0].toLowerCase()) {
                case "on":
                    ESPMod.espEnabled = true;
                    break;

                case "off":
                    ESPMod.espEnabled = false;
                    break;

                case "toggle":
                    ESPMod.espEnabled = !ESPMod.espEnabled;
                    break;
            }
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }

    // mcmod.info - Informations du mod
    {
        "modid": "espmod",
            "name": "ESP Hitbox Mod",
            "description": "Affiche les hitboxes des entités à travers les murs",
            "version": "1.0",
            "mcversion": "1.8.9",
            "url": "",
            "updateUrl": "",
            "authorList": ["Assistant"],
        "credits": "",
            "logoFile": "",
            "screenshots": [],
        "dependencies": []
    }
}
//testing