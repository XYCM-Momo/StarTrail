package com.mmap.client.render;

import com.mmap.client.MMmapMod;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1937;
import net.minecraft.class_2680;
import net.minecraft.class_2791;
import net.minecraft.class_2806;
import net.minecraft.class_2818;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3620;
import net.minecraft.class_638;
import net.minecraft.class_1011.class_1012;
import net.minecraft.class_2338.class_2339;
import net.minecraft.class_2902.class_2903;
import net.minecraft.class_3620.class_6594;

public class TerrainMapTexture {
   private static final int SIZE = 128;
   private static final class_2960 TEX_ID = new class_2960("mmap", "minimap_terrain");
   private class_1011 image;
   private class_1043 texture;
   private boolean textureUploaded = false;
   private double lastPlayerX = Double.MAX_VALUE;
   private double lastPlayerZ = Double.MAX_VALUE;
   private int updateTickCounter = 0;
   private int updateInterval = 5;

   public TerrainMapTexture() {
   }

   private void ensureTexture() {
      if (this.texture == null) {
         try {
            this.image = new class_1011(class_1012.field_4997, 128, 128, true);
            this.fillInitialGreen();
            this.texture = new class_1043(this.image);
            class_310.method_1551().method_1531().method_4616(TEX_ID, this.texture);
            MMmapMod.LOGGER.info("[TerrainMapTexture] created and registered {}x{} texture", 128, 128);
         } catch (Throwable var2) {
            MMmapMod.LOGGER.error("[TerrainMapTexture] ensureTexture failed: {}", var2.toString());
            this.texture = null;
            this.image = null;
         }
      }
   }

   private void fillInitialGreen() {
      int half = 64;
      int r2 = half * half;

      for (int y = 0; y < 128; y++) {
         int dy = y - half;

         for (int x = 0; x < 128; x++) {
            int dx = x - half;
            int distSq = dx * dx + dy * dy;
            if (distSq > r2) {
               this.image.method_4305(x, y, 0);
            } else {
               this.image.method_4305(x, y, -16711936);
            }
         }
      }
   }

   public class_2960 getTextureId() {
      this.ensureTexture();
      return TEX_ID;
   }

   public class_1043 getTexture() {
      this.ensureTexture();
      return this.texture;
   }

   public boolean needsUpdate(double playerX, double playerZ) {
      this.updateTickCounter++;
      if (this.updateTickCounter < this.updateInterval) {
         return false;
      } else {
         this.updateTickCounter = 0;
         double dx = playerX - this.lastPlayerX;
         double dz = playerZ - this.lastPlayerZ;
         return dx * dx + dz * dz > 1.0;
      }
   }

   public void reset(double playerX, double playerZ) {
      this.lastPlayerX = playerX;
      this.lastPlayerZ = playerZ;
      this.updateTickCounter = 0;
   }

   public void update(class_310 client, double playerX, double playerZ, double blocksPerPixel) {
      this.ensureTexture();
      if (this.image != null && client.field_1687 != null) {
         class_1937 world = client.field_1687;
         int halfSize = 64;
         int radiusSq = halfSize * halfSize;
         boolean useSupersample = blocksPerPixel < 1.0;

         for (int py = 0; py < 128; py++) {
            int dy = py - halfSize;

            for (int px = 0; px < 128; px++) {
               int dx = px - halfSize;
               int distSq = dx * dx + dy * dy;
               if (distSq > radiusSq) {
                  this.image.method_4305(px, py, 0);
               } else {
                  int color;
                  if (useSupersample) {
                     double off = blocksPerPixel * 0.35;
                     int r = 0;
                     int g = 0;
                     int b = 0;
                     int validCount = 0;
                     int[][] offsets = new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}};

                     for (int[] o : offsets) {
                        double sx = (double)dx * blocksPerPixel + ((double)o[0] - 0.5) * 2.0 * off;
                        double sz = (double)dy * blocksPerPixel + ((double)o[1] - 0.5) * 2.0 * off;
                        int wx = (int)Math.floor(playerX + sx);
                        int wz = (int)Math.floor(playerZ + sz);
                        int c = this.sampleBlockColor(world, wx, wz);
                        if (c >>> 24 != 0) {
                           r += c >> 16 & 0xFF;
                           g += c >> 8 & 0xFF;
                           b += c & 0xFF;
                           validCount++;
                        }
                     }

                     if (validCount > 0) {
                        r /= validCount;
                        g /= validCount;
                        b /= validCount;
                        color = 0xFF000000 | r << 16 | g << 8 | b;
                     } else {
                        color = 0;
                     }
                  } else {
                     int worldX = (int)Math.floor(playerX + (double)dx * blocksPerPixel);
                     int worldZ = (int)Math.floor(playerZ + (double)dy * blocksPerPixel);
                     color = this.sampleBlockColor(world, worldX, worldZ);
                  }

                  this.image.method_4305(px, py, color);
               }
            }
         }

         try {
            this.texture.method_4524();
            this.textureUploaded = true;
         } catch (Throwable var37) {
            MMmapMod.LOGGER.error("[TerrainMapTexture] upload failed: {}", var37.toString());
         }

         this.reset(playerX, playerZ);
      }
   }

   private int sampleBlockColor(class_1937 world, int worldX, int worldZ) {
      int chunkX = worldX >> 4;
      int chunkZ = worldZ >> 4;
      class_2791 chunk = null;
      class_2818 var16;
      if (world instanceof class_638 clientWorld) {
         try {
            var16 = clientWorld.method_2935().method_2857(chunkX, chunkZ, class_2806.field_12803, false);
         } catch (Throwable var15) {
            return 0;
         }
      } else {
         if (!world.method_8393(chunkX, chunkZ)) {
            return 0;
         }

         var16 = world.method_8497(chunkX, chunkZ);
      }

      if (var16 == null) {
         return 0;
      } else {
         class_2339 pos = new class_2339();
         class_2680 state = null;
         int topY = world.method_8624(class_2903.field_13202, worldX, worldZ);
         int bottomY = world.method_31607();
         if (topY > bottomY) {
            pos.method_10103(worldX, topY, worldZ);
            state = var16.method_8320(pos);
            if (state.method_26215() || state.method_26193(world, pos) == 0) {
               for (int y = topY - 1; y > bottomY; y--) {
                  pos.method_33098(y);
                  state = var16.method_8320(pos);
                  if (!state.method_26215() && state.method_26193(world, pos) > 0) {
                     break;
                  }
               }
            }
         } else {
            int maxY = bottomY + 256;
            if (maxY > world.method_31605()) {
               maxY = world.method_31605();
            }

            for (int yx = maxY - 1; yx > bottomY; yx--) {
               pos.method_10103(worldX, yx, worldZ);
               state = var16.method_8320(pos);
               if (!state.method_26215() && state.method_26193(world, pos) > 0) {
                  break;
               }
            }
         }

         if (state != null && !state.method_26215()) {
            class_6594 brightness = this.getHeightShade(world, worldX, pos.method_10264(), worldZ);

            try {
               class_3620 mc = state.method_26205(world, pos);
               if (mc == class_3620.field_16008) {
                  return -8355712;
               } else {
                  int color = mc.method_15820(brightness);
                  return color | 0xFF000000;
               }
            } catch (Throwable var14) {
               return -8355712;
            }
         } else {
            return -65281;
         }
      }
   }

   private class_6594 getHeightShade(class_1937 world, int x, int y, int z) {
      if (y < 0) {
         return class_6594.field_34761;
      } else if (y < 64) {
         return class_6594.field_34760;
      } else {
         return y < 128 ? class_6594.field_34759 : class_6594.field_34762;
      }
   }

   public void close() {
      if (this.texture != null) {
         try {
            this.texture.close();
         } catch (Throwable var3) {
         }

         this.texture = null;
      }

      if (this.image != null) {
         try {
            this.image.close();
         } catch (Throwable var2) {
         }

         this.image = null;
      }

      this.textureUploaded = false;
   }
}
