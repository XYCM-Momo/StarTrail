package com.asterovie.momo.startrail.client.render;

import com.asterovie.momo.startrail.client.region.PolygonRegion;
import com.asterovie.momo.startrail.client.region.PolygonRegionStore;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public final class PolygonOverlay {
   private PolygonOverlay() {
   }

   public static void render(
      String world,
      PolygonRegionStore store,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      GuiGraphics drawContext,
      Font textRenderer,
      float labelScale,
      boolean clipEnabled,
      int clipCx,
      int clipCy,
      int clipR
   ) {
      render(world, store, camX, camZ, originX, originY, scale, drawContext, textRenderer, labelScale, clipEnabled, clipCx, clipCy, clipR, -1, -1, -1, -1);
   }

   public static void render(
      String world,
      PolygonRegionStore store,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      GuiGraphics drawContext,
      Font textRenderer,
      float labelScale,
      boolean clipEnabled,
      int clipCx,
      int clipCy,
      int clipR,
      int panelX0,
      int panelY0,
      int panelX1,
      int panelY1
   ) {
      if (store != null) {
         List<PolygonRegion> regions = store.getForWorld(world);
         Font useRenderer = textRenderer != null ? textRenderer : Minecraft.getInstance().font;

         for (PolygonRegion r : regions) {
            if (r.isFillEnabled()) {
               drawPolygonFill(drawContext, r, camX, camZ, originX, originY, scale, clipEnabled, clipCx, clipCy, clipR);
            }
         }

         drawAllPolygonLines(drawContext, regions, camX, camZ, originX, originY, scale, clipEnabled, clipCx, clipCy, clipR);

         for (PolygonRegion rx : regions) {
            if (rx.getVertices() != null && rx.getVertices().size() >= 3) {
               drawPolygonLabel(
                  drawContext,
                  useRenderer,
                  rx,
                  camX,
                  camZ,
                  originX,
                  originY,
                  scale,
                  labelScale,
                  clipEnabled,
                  clipCx,
                  clipCy,
                  clipR,
                  panelX0,
                  panelY0,
                  panelX1,
                  panelY1
               );
            }
         }
      }
   }

   public static void render(
      String world,
      PolygonRegionStore store,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      GuiGraphics drawContext,
      Font textRenderer,
      float labelScale
   ) {
      render(world, store, camX, camZ, originX, originY, scale, drawContext, textRenderer, labelScale, false, 0, 0, 0);
   }

   private static void drawAllPolygonLines(
      GuiGraphics ctx,
      List<PolygonRegion> regions,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      boolean clipEnabled,
      int clipCx,
      int clipCy,
      int clipR
   ) {
      boolean hasLine = false;

      for (PolygonRegion r : regions) {
         if (r.isShowLine() && r.getVertices() != null && r.getVertices().size() >= 2) {
            hasLine = true;
            break;
         }
      }

      if (hasLine) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         PoseStack matrices = ctx.pose();
         matrices.pushPose();
         Matrix4f matrix = matrices.last().pose();
         Tesselator tessellator = Tesselator.getInstance();
         BufferBuilder buffer = tessellator.getBuilder();
         buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         int r2 = clipR * clipR;

         for (PolygonRegion rx : regions) {
            if (rx.isShowLine()) {
               List<int[]> v = rx.getVertices();
               int n = v.size();
               if (n >= 2) {
                  int color = rx.getLineColor() | 0xFF000000;
                  float a = (float)(color >> 24 & 0xFF) / 255.0F;
                  float cr = (float)(color >> 16 & 0xFF) / 255.0F;
                  float cg = (float)(color >> 8 & 0xFF) / 255.0F;
                  float cb = (float)(color & 0xFF) / 255.0F;

                  for (int i = 0; i < n; i++) {
                     int[] pa = v.get(i);
                     int[] pb = v.get((i + 1) % n);
                     int ax = worldToScreenX((double)pa[0], camX, originX, scale);
                     int ay = worldToScreenY((double)pa[1], camZ, originY, scale);
                     int bx = worldToScreenX((double)pb[0], camX, originX, scale);
                     int by = worldToScreenY((double)pb[1], camZ, originY, scale);
                     bresenhamLine(buffer, matrix, ax, ay, bx, by, cr, cg, cb, a, clipEnabled, clipCx, clipCy, r2);
                  }
               }
            }
         }

         tessellator.end();
         matrices.popPose();
      }
   }

   private static void bresenhamLine(
      BufferBuilder buffer,
      Matrix4f matrix,
      int x1,
      int y1,
      int x2,
      int y2,
      float r,
      float g,
      float b,
      float a,
      boolean clipEnabled,
      int clipCx,
      int clipCy,
      int r2
   ) {
      int dx = Math.abs(x2 - x1);
      int dy = Math.abs(y2 - y1);
      int sx = x1 < x2 ? 1 : -1;
      int sy = y1 < y2 ? 1 : -1;
      int err = dx - dy;
      int x = x1;
      int y = y1;

      while (true) {
         if (!clipEnabled || inCircle(x, y, clipCx, clipCy, r2)) {
            buffer.vertex(matrix, (float)x, (float)y, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, (float)x, (float)(y + 1), 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, (float)(x + 1), (float)(y + 1), 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, (float)(x + 1), (float)y, 0.0F).color(r, g, b, a).endVertex();
         }

         if (x == x2 && y == y2) {
            return;
         }

         int e2 = err * 2;
         if (e2 > -dy) {
            err -= dy;
            x += sx;
         }

         if (e2 < dx) {
            err += dx;
            y += sy;
         }
      }
   }

   private static void drawPolygonLabel(
      GuiGraphics ctx,
      Font textRenderer,
      PolygonRegion r,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      float labelScale,
      boolean clipEnabled,
      int clipCx,
      int clipCy,
      int clipR,
      int panelX0,
      int panelY0,
      int panelX1,
      int panelY1
   ) {
      int sx = worldToScreenX(r.getCenterX(), camX, originX, scale);
      int sy = worldToScreenY(r.getCenterZ(), camZ, originY, scale);
      if (clipEnabled) {
         int dx = sx - clipCx;
         int dy = sy - clipCy;
         if (dx * dx + dy * dy > clipR * clipR) {
            return;
         }
      }

      if (panelX0 < 0 || sx < panelX0 || sx > panelX1 || sy < panelY0 || sy > panelY1) {
         String text = r.getName();
         int w = textRenderer.width(text);
         int textX = sx - w / 2;
         int textY = sy - 4;
         ctx.fill(textX - 1, textY - 1, textX + w + 1, textY + 9, Integer.MIN_VALUE);
         ctx.drawString(textRenderer, text, textX, textY, -1);
      }
   }

   private static int worldToScreenX(double worldX, double camX, int originX, double scale) {
      return (int)Math.round((double)originX + (worldX - camX) * scale);
   }

   private static int worldToScreenY(double worldZ, double camZ, int originY, double scale) {
      return (int)Math.round((double)originY + (worldZ - camZ) * scale);
   }

   private static void drawPolygonFill(
      GuiGraphics ctx,
      PolygonRegion r,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      boolean clipEnabled,
      int clipCx,
      int clipCy,
      int clipR
   ) {
      List<int[]> v = r.getVertices();
      int n = v.size();
      if (n >= 3) {
         int fillColor = r.getFillColor();
         int[] sx = new int[n];
         int[] sy = new int[n];
         int minY = Integer.MAX_VALUE;
         int maxY = Integer.MIN_VALUE;

         for (int i = 0; i < n; i++) {
            sx[i] = worldToScreenX((double)v.get(i)[0], camX, originX, scale);
            sy[i] = worldToScreenY((double)v.get(i)[1], camZ, originY, scale);
            if (sy[i] < minY) {
               minY = sy[i];
            }

            if (sy[i] > maxY) {
               maxY = sy[i];
            }
         }

         int r2 = clipR * clipR;
         double[] clipHalfWidth = clipEnabled ? new double[maxY - minY + 1] : null;
         if (clipEnabled) {
            for (int y = minY; y <= maxY; y++) {
               int dy = y - clipCy;
               int dy2 = dy * dy;
               if (dy2 >= r2) {
                  clipHalfWidth[y - minY] = -1.0;
               } else {
                  clipHalfWidth[y - minY] = Math.sqrt((double)(r2 - dy2));
               }
            }
         }

         double[] xs = new double[n];

         for (int yx = minY; yx <= maxY; yx++) {
            int count = 0;

            for (int i = 0; i < n; i++) {
               int j = (i + 1) % n;
               int yi = sy[i];
               int yj = sy[j];
               if (yi <= yx && yj > yx || yj <= yx && yi > yx) {
                  double t = (double)(yx - yi) / (double)(yj - yi);
                  xs[count++] = (double)sx[i] + t * (double)(sx[j] - sx[i]);
               }
            }

            if (count >= 2) {
               Arrays.sort(xs, 0, count);

               for (int k = 0; k + 1 < count; k += 2) {
                  int xStart = (int)Math.ceil(xs[k]);
                  int xEnd = (int)Math.floor(xs[k + 1]);
                  if (xEnd >= xStart) {
                     if (clipEnabled) {
                        double halfW = clipHalfWidth[yx - minY];
                        if (halfW < 0.0) {
                           continue;
                        }

                        int cxMin = (int)Math.ceil((double)clipCx - halfW);
                        int cxMax = (int)Math.floor((double)clipCx + halfW);
                        if (xStart < cxMin) {
                           xStart = cxMin;
                        }

                        if (xEnd > cxMax) {
                           xEnd = cxMax;
                        }

                        if (xEnd < xStart) {
                           continue;
                        }
                     }

                     ctx.fill(xStart, yx, xEnd + 1, yx + 1, fillColor);
                  }
               }
            }
         }
      }
   }

   private static void drawLine(GuiGraphics ctx, int x1, int y1, int x2, int y2, int color, boolean clipEnabled, int clipCx, int clipCy, int clipR) {
      int dx = Math.abs(x2 - x1);
      int dy = Math.abs(y2 - y1);
      int sx = x1 < x2 ? 1 : -1;
      int sy = y1 < y2 ? 1 : -1;
      int err = dx - dy;
      int x = x1;
      int y = y1;
      int r2 = clipR * clipR;
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      PoseStack matrices = ctx.pose();
      matrices.pushPose();
      Matrix4f matrix = matrices.last().pose();
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

      while (true) {
         boolean inClip = !clipEnabled || inCircle(x, y, clipCx, clipCy, r2);
         if (inClip) {
            buffer.vertex(matrix, (float)x, (float)y, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, (float)x, (float)(y + 1), 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, (float)(x + 1), (float)(y + 1), 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, (float)(x + 1), (float)y, 0.0F).color(r, g, b, a).endVertex();
         }

         if (x == x2 && y == y2) {
            tessellator.end();
            matrices.popPose();
            return;
         }

         int e2 = err * 2;
         if (e2 > -dy) {
            err -= dy;
            x += sx;
         }

         if (e2 < dx) {
            err += dx;
            y += sy;
         }
      }
   }

   private static boolean inCircle(int x, int y, int cx, int cy, int r2) {
      int dx = x - cx;
      int dy = y - cy;
      return dx * dx + dy * dy <= r2;
   }
}
