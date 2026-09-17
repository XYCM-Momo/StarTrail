package com.mmap.client.render;

import com.mmap.client.MMmapMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntBiFunction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

public final class PlayerFaceRenderer {
   private static final Map<UUID, ResourceLocation> SKIN_CACHE = new HashMap<>();
   private static final Map<String, ResourceLocation> LOCAL_SKIN_CACHE = new HashMap<>();
   private static final Map<String, DynamicTexture> LOCAL_SKIN_TEXTURES = new HashMap<>();
   private static final Set<String> LOCAL_SKIN_FAILED = new HashSet<>();
   private static boolean LOCAL_SKIN_HINT_PRINTED = false;

   public static void drawFace(GuiGraphics context, Minecraft client, AbstractClientPlayer player, int centerX, int centerY, int size) {
      if (client != null && player != null) {
         ResourceLocation skin = getOrLoadSkin(client, player);
         if (skin != null) {
            int x0 = centerX - size / 2;
            int y0 = centerY - size / 2;
            net.minecraft.client.gui.components.PlayerFaceRenderer.draw(context, skin, x0, y0, size);
         }
      }
   }

   public static void drawFaceById(GuiGraphics context, ResourceLocation skin, int centerX, int centerY, int size) {
      if (context != null && skin != null) {
         int x0 = centerX - size / 2;
         int y0 = centerY - size / 2;
         net.minecraft.client.gui.components.PlayerFaceRenderer.draw(context, skin, x0, y0, size);
      }
   }

   public static void drawAllOtherPlayers(
      GuiGraphics context,
      Minecraft client,
      int size,
      ToIntBiFunction<AbstractClientPlayer, Integer> faceScreenX,
      ToIntBiFunction<AbstractClientPlayer, Integer> faceScreenY
   ) {
      if (client != null && client.level != null) {
         ClientLevel world = client.level;

         for (AbstractClientPlayer entity : world.players()) {
            if (entity instanceof AbstractClientPlayer && entity != client.player) {
               ResourceLocation skin = getOrLoadSkin(client, entity);
               if (skin != null) {
                  int cx = faceScreenX.applyAsInt(entity, size);
                  int cy = faceScreenY.applyAsInt(entity, size);
                  drawFaceById(context, skin, cx, cy, size);
               }
            }
         }
      }
   }

   public static void drawAllPlayers(
      GuiGraphics context,
      Minecraft client,
      int size,
      ToIntBiFunction<AbstractClientPlayer, Integer> faceScreenX,
      ToIntBiFunction<AbstractClientPlayer, Integer> faceScreenY
   ) {
      if (client != null && client.level != null) {
         ClientLevel world = client.level;

         for (AbstractClientPlayer entity : world.players()) {
            if (entity instanceof AbstractClientPlayer) {
               ResourceLocation skin = getOrLoadSkin(client, entity);
               if (skin != null) {
                  int cx = faceScreenX.applyAsInt(entity, size);
                  int cy = faceScreenY.applyAsInt(entity, size);
                  drawFaceById(context, skin, cx, cy, size);
               }
            }
         }
      }
   }

   private static ResourceLocation getOrLoadSkin(Minecraft client, AbstractClientPlayer player) {
      try {
         client.getSkinManager().getInsecureSkinLocation(player.getGameProfile());
      } catch (Throwable var4) {
      }

      ResourceLocation tex = player.getSkinTextureLocation();
      if (tex == null) {
         return null;
      } else {
         if (isDefaultSteveTexture(client, player, tex)) {
            ResourceLocation localSkin = tryLoadLocalSkin(client, player);
            if (localSkin != null) {
               tex = localSkin;
            } else if (!LOCAL_SKIN_HINT_PRINTED) {
               LOCAL_SKIN_HINT_PRINTED = true;
               MMmapMod.LOGGER
                  .warn(
                     "[PlayerFace] \u73a9\u5bb6 {} \u6ca1\u6709\u76ae\u80a4\uff08CustomSkinLoader+LocalSkin \u90fd\u672a\u627e\u5230\uff09\uff0c\u5c06\u663e\u793a\u9ed8\u8ba4 Steve\u3002\n   \u4fee\u590d\u65b9\u6cd5\uff1a\u628a\u76ae\u80a4 PNG \u653e\u5230 .minecraft/CustomSkinLoader/LocalSkin/skins/{}.png\n   \u7136\u540e\u6267\u884c /mmap reload \u6216\u91cd\u542f\u6e38\u620f\u3002",
                     player.getName().getString(),
                     player.getName().getString()
                  );
            }
         }

         return tex;
      }
   }

   private static boolean isDefaultSteveTexture(Minecraft client, AbstractClientPlayer player, ResourceLocation tex) {
      try {
         ResourceLocation defaultTex = DefaultPlayerSkin.getDefaultSkin(player.getUUID());
         return tex.equals(defaultTex);
      } catch (Throwable var4) {
         return false;
      }
   }

   private static ResourceLocation tryLoadLocalSkin(Minecraft client, AbstractClientPlayer player) {
      String name = player.getName().getString();
      if (name != null && !name.isEmpty()) {
         ResourceLocation cached = LOCAL_SKIN_CACHE.get(name);
         if (cached != null) {
            return cached;
         } else if (LOCAL_SKIN_FAILED.contains(name)) {
            return null;
         } else {
            File skinFile = new File(FabricLoader.getInstance().getConfigDir().toFile().getParentFile(), "CustomSkinLoader/LocalSkin/skins/" + name + ".png");
            if (!skinFile.exists()) {
               LOCAL_SKIN_FAILED.add(name);
               return null;
            } else if (!RenderSystem.isOnRenderThread()) {
               return null;
            } else {
               try {
                  ResourceLocation var9;
                  try (InputStream is = new FileInputStream(skinFile)) {
                     NativeImage image = NativeImage.read(is);
                     if (image == null) {
                        LOCAL_SKIN_FAILED.add(name);
                        return null;
                     }

                     DynamicTexture tex = new DynamicTexture(image);
                     tex.upload();
                     ResourceLocation id = new ResourceLocation("mmap", "local_skin/" + name);
                     client.getTextureManager().register(id, tex);
                     LOCAL_SKIN_CACHE.put(name, id);
                     LOCAL_SKIN_TEXTURES.put(name, tex);
                     MMmapMod.LOGGER
                        .info("[PlayerFace] \u5df2\u4ece LocalSkin \u52a0\u8f7d\u73a9\u5bb6 {} \u7684\u76ae\u80a4: {}", name, skinFile.getAbsolutePath());
                     var9 = id;
                  }

                  return var9;
               } catch (Throwable var12) {
                  MMmapMod.LOGGER.warn("[PlayerFace] \u52a0\u8f7d LocalSkin \u5931\u8d25: {} ({})", skinFile.getAbsolutePath(), var12.getMessage());
                  LOCAL_SKIN_FAILED.add(name);
                  return null;
               }
            }
         }
      } else {
         return null;
      }
   }

   public static void clearCache() {
      SKIN_CACHE.clear();
   }

   public static ResourceLocation getCachedSkin(UUID id) {
      return SKIN_CACHE.get(id);
   }

   private PlayerFaceRenderer() {
   }
}
