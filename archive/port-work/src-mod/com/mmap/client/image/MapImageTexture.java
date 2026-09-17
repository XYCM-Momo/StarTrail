package com.mmap.client.image;

import com.mmap.client.MMmapMod;
import com.mmap.client.crypto.ImageCrypto;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import org.lwjgl.opengl.GL11;

public class MapImageTexture {
   private final MapImage image;
   private final File imageFile;
   private class_1011 nativeImage;
   private class_1043 texture;
   private class_2960 textureId;
   private boolean registered = false;
   private boolean loadFailed = false;

   public MapImageTexture(MapImage image, File imageFile) {
      this.image = image;
      this.imageFile = imageFile;
   }

   public boolean ensureLoaded() {
      if (this.registered && this.texture != null) {
         return true;
      } else if (this.loadFailed) {
         return false;
      } else if (!RenderSystem.isOnRenderThread()) {
         MMmapMod.LOGGER.warn("MapImageTexture.ensureLoaded() \u5fc5\u987b\u5728\u6e32\u67d3\u7ebf\u7a0b\u8c03\u7528\uff01");
         return false;
      } else {
         try {
            if (!this.imageFile.exists()) {
               MMmapMod.LOGGER.warn("\u5730\u56fe\u56fe\u7247\u6587\u4ef6\u4e0d\u5b58\u5728: {}", this.imageFile.getAbsolutePath());
               this.loadFailed = true;
               return false;
            } else {
               byte[] raw = Files.readAllBytes(this.imageFile.toPath());
               byte[] pngBytes = ImageCrypto.maybeDecrypt(raw);
               if (pngBytes == null) {
                  MMmapMod.LOGGER.error("\u56fe\u7247\u89e3\u5bc6\u5931\u8d25\u6216\u6587\u4ef6\u88ab\u7be1\u6539: {}", this.imageFile.getAbsolutePath());
                  this.loadFailed = true;
                  return false;
               } else {
                  InputStream is = new ByteArrayInputStream(pngBytes);

                  class_1011 loadedImage;
                  try {
                     loadedImage = class_1011.method_4309(is);
                  } catch (Throwable var9) {
                     try {
                        is.close();
                     } catch (Throwable var7) {
                        var9.addSuppressed(var7);
                     }

                     throw var9;
                  }

                  is.close();
                  if (loadedImage == null) {
                     MMmapMod.LOGGER.error("\u65e0\u6cd5\u89e3\u6790\u56fe\u7247\u6587\u4ef6: {}", this.imageFile.getAbsolutePath());
                     this.loadFailed = true;
                     return false;
                  } else {
                     try {
                        this.texture = new class_1043(loadedImage);
                     } catch (Exception var8) {
                        loadedImage.close();
                        throw var8;
                     }

                     this.nativeImage = loadedImage;
                     this.textureId = new class_2960("mmap", "map_image/" + sanitizeId(this.image.getId()));
                     class_310.method_1551().method_1531().method_4616(this.textureId, this.texture);
                     this.texture.method_4524();
                     int glId = this.texture.method_4624();
                     RenderSystem.bindTexture(glId);
                     GL11.glTexParameteri(3553, 10242, 33071);
                     GL11.glTexParameteri(3553, 10243, 33071);
                     GL11.glTexParameteri(3553, 10241, 9728);
                     GL11.glTexParameteri(3553, 10240, 9728);
                     this.registered = true;
                     MMmapMod.LOGGER
                        .info(
                           "\u5df2\u52a0\u8f7d\u5730\u56fe\u56fe\u7247: {} ({}x{}) -> {} [CLAMP_TO_EDGE] glId={}",
                           new Object[]{this.imageFile.getName(), this.nativeImage.method_4307(), this.nativeImage.method_4323(), this.textureId, glId}
                        );
                     return true;
                  }
               }
            }
         } catch (OutOfMemoryError var10) {
            MMmapMod.LOGGER
               .error(
                  "\u52a0\u8f7d\u5730\u56fe\u56fe\u7247\u5185\u5b58\u4e0d\u8db3: {} (\u56fe\u7247\u53ef\u80fd\u8fc7\u5927)", this.imageFile.getName(), var10
               );
            this.loadFailed = true;
            return false;
         } catch (Exception var11) {
            MMmapMod.LOGGER.error("\u52a0\u8f7d\u5730\u56fe\u56fe\u7247\u5931\u8d25: {}", this.imageFile.getName(), var11);
            this.loadFailed = true;
            return false;
         }
      }
   }

   public class_2960 getTextureId() {
      return this.textureId;
   }

   public boolean isReady() {
      return this.registered && this.texture != null;
   }

   public boolean isLoadFailed() {
      return this.loadFailed;
   }

   public int getImageWidth() {
      return this.nativeImage != null ? this.nativeImage.method_4307() : 0;
   }

   public int getImageHeight() {
      return this.nativeImage != null ? this.nativeImage.method_4323() : 0;
   }

   public MapImage getMapImage() {
      return this.image;
   }

   public int getGlTextureId() {
      return this.texture != null ? this.texture.method_4624() : -1;
   }

   public void close() {
      if (!RenderSystem.isOnRenderThread()) {
         class_310.method_1551().execute(this::close);
      } else {
         try {
            if (this.registered && this.textureId != null) {
               class_310.method_1551().method_1531().method_4615(this.textureId);
            }

            if (this.texture != null) {
               this.texture.close();
               this.texture = null;
            }

            this.nativeImage = null;
         } catch (Exception var2) {
            MMmapMod.LOGGER.warn("\u91ca\u653e\u5730\u56fe\u56fe\u7247\u7eb9\u7406\u5931\u8d25: {}", this.image.getId(), var2);
         }

         this.registered = false;
         this.loadFailed = false;
      }
   }

   private static String sanitizeId(String id) {
      return id.toLowerCase().replaceAll("[^a-z0-9_/.-]", "_");
   }
}
