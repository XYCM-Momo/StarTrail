package com.asterovie.momo.startrail.client.render;

import com.asterovie.momo.startrail.client.StarTrailMod;
import com.asterovie.momo.startrail.client.config.ConfigManager;
import com.asterovie.momo.startrail.client.image.MapImage;
import com.asterovie.momo.startrail.client.image.MapImageManager;
import com.asterovie.momo.startrail.client.image.MapImageTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

public class MinimapImageRenderer {
   private final MapImageManager mapImageManager;
   private long lastDebugLog = 0L;

   public MinimapImageRenderer(MapImageManager mapImageManager) {
      this.mapImageManager = mapImageManager;
   }

   public void render(GuiGraphics drawContext, Minecraft client, int mapX, int mapY, int mapSize, double zoom, float tickDelta, double playerX, double playerZ, int shape) {
      if (client.player != null && client.level != null) {
         Level world = client.level;
         String worldKey = world.dimension().location().toString();
         String currentWorld = ConfigManager.getInstance().getCurrentWorld();
         if (currentWorld != null) {
            MapImage targetImage = null;
            Collection<MapImage> allImages = this.mapImageManager.getAllImages();

            for (MapImage img : allImages) {
               if ((img.getWorld().equals(currentWorld) || currentWorld.startsWith(img.getWorld()) || img.getWorld().startsWith(currentWorld))
                  && img.contains(playerX, playerZ)) {
                  targetImage = img;
                  break;
               }
            }

            if (targetImage == null) {
               long now = System.currentTimeMillis();
               if (now - this.lastDebugLog > 1000L) {
                  this.lastDebugLog = now;
                  StarTrailMod.LOGGER
                     .warn(
                        "[minimap-image] no image for player at ({}, {}) worldKey={} allImages={}", new Object[]{playerX, playerZ, worldKey, allImages.size()}
                     );

                  for (MapImage imgx : allImages) {
                     StarTrailMod.LOGGER
                        .warn(
                           "  - img: {} bounds=({},{})-({},{}) contains={}",
                           new Object[]{imgx, imgx.getMinX(), imgx.getMinZ(), imgx.getMaxX(), imgx.getMaxZ(), imgx.contains(playerX, playerZ)}
                        );
                  }
               }
            } else {
               MapImageTexture tex = this.mapImageManager.getTexture(targetImage);
               if (tex != null && tex.isReady()) {
                  int imgW = tex.getImageWidth();
                  int imgH = tex.getImageHeight();
                  int radius = mapSize / 2;
                  int centerX = mapX + radius;
                  int centerY = mapY + radius;
                  double playerU = (playerX - (double)targetImage.getMinX()) / (double)targetImage.getWorldWidth();
                  double playerV = (playerZ - (double)targetImage.getMinZ()) / (double)targetImage.getWorldDepth();
                  double worldSpanX = (double)mapSize / zoom;
                  double worldSpanZ = (double)mapSize / zoom;
                  double duPerPixel = worldSpanX / (double)targetImage.getWorldWidth() / (double)mapSize;
                  double dvPerPixel = worldSpanZ / (double)targetImage.getWorldDepth() / (double)mapSize;
                  RenderSystem.enableBlend();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.setShaderTexture(0, tex.getTextureId());
                  RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                  float brightness = computeBrightness(world);
                  int brR = Math.max(0, Math.min(255, (int)(brightness * 255.0F)));
                  int brG = brR;
                  int brB = brR;
                  PoseStack matrices = drawContext.pose();
                  matrices.pushPose();
                  Matrix4f matrix = matrices.last().pose();
                  int gridSize = 64;
                  float cellSize = (float)mapSize / (float)gridSize;
                  Tesselator tessellator = Tesselator.getInstance();
                  BufferBuilder buffer = tessellator.getBuilder();
                  buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                  double u0GlobalD = playerU - (double)radius * duPerPixel;
                  double v0GlobalD = playerV - (double)radius * dvPerPixel;
                  boolean circleMask = shape == 0;

                  for (int gy = 0; gy < gridSize; gy++) {
                     for (int gx = 0; gx < gridSize; gx++) {
                        float x0 = (float)mapX + (float)gx * cellSize;
                        float y0 = (float)mapY + (float)gy * cellSize;
                        float x1 = x0 + cellSize;
                        float y1 = y0 + cellSize;
                        float cx = (x0 + x1) * 0.5F;
                        float cy = (y0 + y1) * 0.5F;
                        float dxC = cx - (float)centerX;
                        float dyC = cy - (float)centerY;
                        float distC2 = dxC * dxC + dyC * dyC;
                        float halfDiag = cellSize * 0.7072F;
                        float maxDist = (float)radius + halfDiag;
                        if (circleMask && distC2 > maxDist * maxDist) {
                           continue;
                        }

                        double u0D = u0GlobalD + (double)((float)gx * cellSize) * duPerPixel;
                        double v0D = v0GlobalD + (double)((float)gy * cellSize) * dvPerPixel;
                        float u0 = (float)u0D;
                        float v0 = (float)v0D;
                        float u1 = (float)(u0D + (double)cellSize * duPerPixel);
                        float v1 = (float)(v0D + (double)cellSize * dvPerPixel);
                        boolean u0Out = u0 < 0.0F || u0 > 1.0F;
                        boolean u1Out = u1 < 0.0F || u1 > 1.0F;
                        boolean v0Out = v0 < 0.0F || v0 > 1.0F;
                        boolean v1Out = v1 < 0.0F || v1 > 1.0F;
                        if (!u0Out || !u1Out || !v0Out || !v1Out) {
                           float aTL = circleMask ? alphaAt(x0, y0, centerX, centerY, radius) : 1.0F;
                           float aTR = circleMask ? alphaAt(x1, y0, centerX, centerY, radius) : 1.0F;
                           float aBR = circleMask ? alphaAt(x1, y1, centerX, centerY, radius) : 1.0F;
                           float aBL = circleMask ? alphaAt(x0, y1, centerX, centerY, radius) : 1.0F;
                              if (u0Out || v0Out) {
                                 aTL = 0.0F;
                              }

                              if (u0Out || v1Out) {
                                 aBL = 0.0F;
                              }

                              if (u1Out || v1Out) {
                                 aBR = 0.0F;
                              }

                              if (u1Out || v0Out) {
                                 aTR = 0.0F;
                              }

                              if (!(aTL <= 0.0F) || !(aTR <= 0.0F) || !(aBR <= 0.0F) || !(aBL <= 0.0F)) {
                                 int aTLi = (int)(aTL * 255.0F);
                                 int aTRi = (int)(aTR * 255.0F);
                                 int aBRi = (int)(aBR * 255.0F);
                                 int aBLi = (int)(aBL * 255.0F);
                                 buffer.vertex(matrix, x0, y0, 0.0F).uv(u0, v0).color(brR, brG, brB, aTLi).endVertex();
                                 buffer.vertex(matrix, x0, y1, 0.0F).uv(u0, v1).color(brR, brG, brB, aBLi).endVertex();
                                 buffer.vertex(matrix, x1, y1, 0.0F).uv(u1, v1).color(brR, brG, brB, aBRi).endVertex();
                                 buffer.vertex(matrix, x1, y0, 0.0F).uv(u1, v0).color(brR, brG, brB, aTRi).endVertex();
                              }
                        }
                     }
                  }

                  tessellator.end();
                  RenderSystem.disableBlend();
                  matrices.popPose();
               } else {
                  long now = System.currentTimeMillis();
                  if (now - this.lastDebugLog > 1000L) {
                     this.lastDebugLog = now;
                     StarTrailMod.LOGGER
                        .warn("[minimap-image] texture not ready for {}: tex={} ready={}", new Object[]{targetImage, tex, tex != null && tex.isReady()});
                  }
               }
            }
         }
      }
   }

   private static float alphaAt(float x, float y, int cx, int cy, int r) {
      float dx = x - (float)cx;
      float dy = y - (float)cy;
      float d = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (d <= (float)r - 2.0F) {
         return 1.0F;
      } else {
         return d >= (float)r ? 0.0F : Math.max(0.0F, ((float)r - d) / 2.0F);
      }
   }

   private static float computeBrightness(Level world) {
      long tod = world.getDayTime() % 24000L;
      float phase = (float)tod / 24000.0F;
      float t = (float)Math.cos((Math.PI * 2) * (double)(phase - 0.25F));
      return 0.7F + 0.3F * t;
   }
}
