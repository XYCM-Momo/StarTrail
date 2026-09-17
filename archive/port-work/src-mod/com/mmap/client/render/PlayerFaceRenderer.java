package com.mmap.client.render;

import com.mmap.client.MMmapMod;
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
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1068;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_638;
import net.minecraft.class_742;
import net.minecraft.class_7532;

public final class PlayerFaceRenderer {
   private static final Map<UUID, class_2960> SKIN_CACHE = new HashMap<>();
   private static final Map<String, class_2960> LOCAL_SKIN_CACHE = new HashMap<>();
   private static final Map<String, class_1043> LOCAL_SKIN_TEXTURES = new HashMap<>();
   private static final Set<String> LOCAL_SKIN_FAILED = new HashSet<>();
   private static boolean LOCAL_SKIN_HINT_PRINTED = false;

   public static void drawFace(class_332 context, class_310 client, class_742 player, int centerX, int centerY, int size) {
      if (client != null && player != null) {
         class_2960 skin = getOrLoadSkin(client, player);
         if (skin != null) {
            int x0 = centerX - size / 2;
            int y0 = centerY - size / 2;
            class_7532.method_44443(context, skin, x0, y0, size);
         }
      }
   }

   public static void drawFaceById(class_332 context, class_2960 skin, int centerX, int centerY, int size) {
      if (context != null && skin != null) {
         int x0 = centerX - size / 2;
         int y0 = centerY - size / 2;
         class_7532.method_44443(context, skin, x0, y0, size);
      }
   }

   public static void drawAllOtherPlayers(
      class_332 context, class_310 client, int size, ToIntBiFunction<class_742, Integer> faceScreenX, ToIntBiFunction<class_742, Integer> faceScreenY
   ) {
      if (client != null && client.field_1687 != null) {
         class_638 world = client.field_1687;

         for (class_742 entity : world.method_18456()) {
            if (entity instanceof class_742 && entity != client.field_1724) {
               class_2960 skin = getOrLoadSkin(client, entity);
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
      class_332 context, class_310 client, int size, ToIntBiFunction<class_742, Integer> faceScreenX, ToIntBiFunction<class_742, Integer> faceScreenY
   ) {
      if (client != null && client.field_1687 != null) {
         class_638 world = client.field_1687;

         for (class_742 entity : world.method_18456()) {
            if (entity instanceof class_742) {
               class_2960 skin = getOrLoadSkin(client, entity);
               if (skin != null) {
                  int cx = faceScreenX.applyAsInt(entity, size);
                  int cy = faceScreenY.applyAsInt(entity, size);
                  drawFaceById(context, skin, cx, cy, size);
               }
            }
         }
      }
   }

   private static class_2960 getOrLoadSkin(class_310 client, class_742 player) {
      try {
         client.method_1582().method_44705(player.method_7334());
      } catch (Throwable var4) {
      }

      class_2960 tex = player.method_3117();
      if (tex == null) {
         return null;
      } else {
         if (isDefaultSteveTexture(client, player, tex)) {
            class_2960 localSkin = tryLoadLocalSkin(client, player);
            if (localSkin != null) {
               tex = localSkin;
            } else if (!LOCAL_SKIN_HINT_PRINTED) {
               LOCAL_SKIN_HINT_PRINTED = true;
               MMmapMod.LOGGER
                  .warn(
                     "[PlayerFace] \u73a9\u5bb6 {} \u6ca1\u6709\u76ae\u80a4\uff08CustomSkinLoader+LocalSkin \u90fd\u672a\u627e\u5230\uff09\uff0c\u5c06\u663e\u793a\u9ed8\u8ba4 Steve\u3002\n   \u4fee\u590d\u65b9\u6cd5\uff1a\u628a\u76ae\u80a4 PNG \u653e\u5230 .minecraft/CustomSkinLoader/LocalSkin/skins/{}.png\n   \u7136\u540e\u6267\u884c /mmap reload \u6216\u91cd\u542f\u6e38\u620f\u3002",
                     player.method_5477().getString(),
                     player.method_5477().getString()
                  );
            }
         }

         return tex;
      }
   }

   private static boolean isDefaultSteveTexture(class_310 client, class_742 player, class_2960 tex) {
      try {
         class_2960 defaultTex = class_1068.method_4648(player.method_5667());
         return tex.equals(defaultTex);
      } catch (Throwable var4) {
         return false;
      }
   }

   private static class_2960 tryLoadLocalSkin(class_310 client, class_742 player) {
      String name = player.method_5477().getString();
      if (name != null && !name.isEmpty()) {
         class_2960 cached = LOCAL_SKIN_CACHE.get(name);
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
                  class_2960 var9;
                  try (InputStream is = new FileInputStream(skinFile)) {
                     class_1011 image = class_1011.method_4309(is);
                     if (image == null) {
                        LOCAL_SKIN_FAILED.add(name);
                        return null;
                     }

                     class_1043 tex = new class_1043(image);
                     tex.method_4524();
                     class_2960 id = new class_2960("mmap", "local_skin/" + name);
                     client.method_1531().method_4616(id, tex);
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

   public static class_2960 getCachedSkin(UUID id) {
      return SKIN_CACHE.get(id);
   }

   private PlayerFaceRenderer() {
   }
}
