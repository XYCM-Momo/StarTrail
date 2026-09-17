package com.mmap.client.render;

import com.mmap.client.MMmapMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;

public class TerrainMapTexture {
   private static final int SIZE = 128;
   private static final ResourceLocation TEX_ID = new ResourceLocation("mmap", "minimap_terrain");
   private NativeImage image;
   private DynamicTexture texture;
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
            this.image = new NativeImage(Format.RGBA, 128, 128, true);
            this.fillInitialGreen();
            this.texture = new DynamicTexture(this.image);
            Minecraft.getInstance().getTextureManager().register(TEX_ID, this.texture);
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
               this.image.setPixelRGBA(x, y, 0);
            } else {
               this.image.setPixelRGBA(x, y, -16711936);
            }
         }
      }
   }

   public ResourceLocation getTextureId() {
      this.ensureTexture();
      return TEX_ID;
   }

   public DynamicTexture getTexture() {
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

   public void update(Minecraft client, double playerX, double playerZ, double blocksPerPixel) {
      this.ensureTexture();
      if (this.image != null && client.level != null) {
         Level world = client.level;
         int halfSize = 64;
         int radiusSq = halfSize * halfSize;
         boolean useSupersample = blocksPerPixel < 1.0;

         for (int py = 0; py < 128; py++) {
            int dy = py - halfSize;

            for (int px = 0; px < 128; px++) {
               int dx = px - halfSize;
               int distSq = dx * dx + dy * dy;
               if (distSq > radiusSq) {
                  this.image.setPixelRGBA(px, py, 0);
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

                  this.image.setPixelRGBA(px, py, color);
               }
            }
         }

         try {
            this.texture.upload();
            this.textureUploaded = true;
         } catch (Throwable var37) {
            MMmapMod.LOGGER.error("[TerrainMapTexture] upload failed: {}", var37.toString());
         }

         this.reset(playerX, playerZ);
      }
   }

   private int sampleBlockColor(Level world, int worldX, int worldZ) {
      int chunkX = worldX >> 4;
      int chunkZ = worldZ >> 4;
      ChunkAccess chunk = null;
      LevelChunk var16;
      if (world instanceof ClientLevel clientWorld) {
         try {
            var16 = clientWorld.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
         } catch (Throwable var15) {
            return 0;
         }
      } else {
         if (!world.hasChunk(chunkX, chunkZ)) {
            return 0;
         }

         var16 = world.getChunk(chunkX, chunkZ);
      }

      if (var16 == null) {
         return 0;
      } else {
         MutableBlockPos pos = new MutableBlockPos();
         BlockState state = null;
         int topY = world.getHeight(Types.WORLD_SURFACE, worldX, worldZ);
         int bottomY = world.getMinBuildHeight();
         if (topY > bottomY) {
            pos.set(worldX, topY, worldZ);
            state = var16.getBlockState(pos);
            if (state.isAir() || state.getLightBlock(world, pos) == 0) {
               for (int y = topY - 1; y > bottomY; y--) {
                  pos.setY(y);
                  state = var16.getBlockState(pos);
                  if (!state.isAir() && state.getLightBlock(world, pos) > 0) {
                     break;
                  }
               }
            }
         } else {
            int maxY = bottomY + 256;
            if (maxY > world.getHeight()) {
               maxY = world.getHeight();
            }

            for (int yx = maxY - 1; yx > bottomY; yx--) {
               pos.set(worldX, yx, worldZ);
               state = var16.getBlockState(pos);
               if (!state.isAir() && state.getLightBlock(world, pos) > 0) {
                  break;
               }
            }
         }

         if (state != null && !state.isAir()) {
            Brightness brightness = this.getHeightShade(world, worldX, pos.getY(), worldZ);

            try {
               MapColor mc = state.getMapColor(world, pos);
               if (mc == MapColor.NONE) {
                  return -8355712;
               } else {
                  int color = mc.calculateRGBColor(brightness);
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

   private Brightness getHeightShade(Level world, int x, int y, int z) {
      if (y < 0) {
         return Brightness.HIGH;
      } else if (y < 64) {
         return Brightness.NORMAL;
      } else {
         return y < 128 ? Brightness.LOW : Brightness.LOWEST;
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
