package com.mmap.client.warp;

import com.mmap.client.MMmapMod;
import com.mmap.client.config.ConfigManager;
import java.util.List;
import net.minecraft.class_327;
import net.minecraft.class_332;

public final class WarpOverlay {
   private static long lastDbgMs = 0L;
   private static double lastDbgCamX = 0.0;
   private static double lastDbgCamZ = 0.0;
   private static double lastDbgScale = 1.0;
   private static int lastDbgOriginX = 0;
   private static int lastDbgOriginY = 0;

   private WarpOverlay() {
   }

   public static void render(
      String world,
      WarpPointStore store,
      WarpIconManager iconMgr,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      class_332 ctx,
      class_327 textRenderer,
      boolean clipEnabled,
      int clipCx,
      int clipCy,
      int clipR,
      int baseIconSize,
      boolean labelAbove,
      String selectedId
   ) {
      render(
         world,
         store,
         iconMgr,
         camX,
         camZ,
         originX,
         originY,
         scale,
         ctx,
         textRenderer,
         clipEnabled,
         clipCx,
         clipCy,
         clipR,
         baseIconSize,
         labelAbove,
         selectedId,
         -1,
         -1,
         -1,
         -1
      );
   }

   public static void render(
      String world,
      WarpPointStore store,
      WarpIconManager iconMgr,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      class_332 ctx,
      class_327 textRenderer,
      boolean clipEnabled,
      int clipCx,
      int clipCy,
      int clipR,
      int baseIconSize,
      boolean labelAbove,
      String selectedId,
      int panelX0,
      int panelY0,
      int panelX1,
      int panelY1
   ) {
      if (store != null) {
         List<WarpPoint> warps = store.getForWorld(world);
         if (!warps.isEmpty()) {
            updateDbgSnapshot(camX, camZ, originX, originY, scale);
            int iconSize;
            if (clipEnabled) {
               iconSize = baseIconSize;
            } else {
               double threshold = ConfigManager.getInstance().getConfig().worldmap.warpIconHideThreshold;
               if (scale < threshold) {
                  return;
               }

               double s = Math.max(0.0625, Math.min(16.0, scale));
               iconSize = (int)Math.round((double)baseIconSize * s);
               int lo = 10;
               int hi = 128;
               if (iconSize < lo) {
                  iconSize = lo;
               }

               if (iconSize > hi) {
                  iconSize = hi;
               }
            }

            for (WarpPoint w : warps) {
               float sx = (float)((double)originX + ((double)w.getX() - camX) * scale);
               float sy = (float)((double)originY + ((double)w.getZ() - camZ) * scale);
               if (clipEnabled) {
                  int dx = (int)sx - clipCx;
                  int dy = (int)sy - clipCy;
                  int limitR = clipR + iconSize / 2;
                  if (dx * dx + dy * dy > limitR * limitR) {
                     continue;
                  }
               }

               boolean isSelected = selectedId != null && selectedId.equals(w.getId());
               WarpIconTexture tex = isSelected ? iconMgr.getTextureSelected(w.getIconKey()) : iconMgr.getTexture(w.getIconKey());
               throttledDbg(w, tex, sx, sy, iconSize, clipEnabled, scale, isSelected);
               if (tex != null && tex.isReady()) {
                  drawIcon(ctx, tex, sx, sy, iconSize);
               } else {
                  drawFallbackDot(ctx, sx, sy, iconSize, isSelected ? -256 : -65536);
               }

               if (textRenderer != null) {
                  String text = w.getName();
                  int textW = textRenderer.method_1727(text);
                  int textX = (int)sx - textW / 2;
                  int textY;
                  if (labelAbove) {
                     textY = (int)sy - iconSize / 2 - 10;
                  } else {
                     textY = (int)sy + iconSize / 2 + 2;
                  }

                  if (clipEnabled) {
                     int dy = textY - clipCy;
                     if (Math.abs(dy) > clipR) {
                        continue;
                     }
                  }

                  int textCx = textX + textW / 2;
                  int textCy = textY + 4;
                  if (panelX0 < 0 || textCx < panelX0 || textCx > panelX1 || textCy < panelY0 || textCy > panelY1) {
                     ctx.method_25294(textX - 1, textY - 1, textX + textW + 1, textY + 9, Integer.MIN_VALUE);
                     ctx.method_25303(textRenderer, text, textX, textY, -1);
                  }
               }
            }
         }
      }
   }

   public static void renderWorldMap(
      String world,
      WarpPointStore store,
      WarpIconManager iconMgr,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      class_332 ctx,
      class_327 textRenderer,
      int baseIconSize,
      String selectedId
   ) {
      render(world, store, iconMgr, camX, camZ, originX, originY, scale, ctx, textRenderer, false, 0, 0, 0, baseIconSize, true, selectedId);
   }

   public static void renderWorldMap(
      String world,
      WarpPointStore store,
      WarpIconManager iconMgr,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      class_332 ctx,
      class_327 textRenderer,
      int baseIconSize,
      String selectedId,
      int panelX0,
      int panelY0,
      int panelX1,
      int panelY1
   ) {
      render(
         world,
         store,
         iconMgr,
         camX,
         camZ,
         originX,
         originY,
         scale,
         ctx,
         textRenderer,
         false,
         0,
         0,
         0,
         baseIconSize,
         true,
         selectedId,
         panelX0,
         panelY0,
         panelX1,
         panelY1
      );
   }

   public static void renderMinimap(
      String world,
      WarpPointStore store,
      WarpIconManager iconMgr,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      class_332 ctx,
      class_327 textRenderer,
      int clipCx,
      int clipCy,
      int clipR,
      int baseIconSize,
      String selectedId
   ) {
      render(world, store, iconMgr, camX, camZ, originX, originY, scale, ctx, textRenderer, true, clipCx, clipCy, clipR, baseIconSize, false, selectedId);
   }

   private static void drawIcon(class_332 ctx, WarpIconTexture tex, float centerX, float centerY, int iconSize) {
      int texW = tex.getImageWidth();
      int texH = tex.getImageHeight();
      if (texW > 0 && texH > 0) {
         int effSize = Math.max(4, iconSize);
         float scale = (float)effSize / (float)Math.max(texW, texH);
         int drawW = Math.max(1, Math.round((float)texW * scale));
         int drawH = Math.max(1, Math.round((float)texH * scale));
         float fx = centerX - (float)drawW / 2.0F;
         float fy = centerY - (float)drawH / 2.0F;
         int ix = Math.round(fx);
         int iy = Math.round(fy);
         ctx.method_51448().method_22903();
         ctx.method_51448().method_46416(fx - (float)ix, fy - (float)iy, 0.0F);
         ctx.method_25293(tex.getTextureId(), ix, iy, drawW, drawH, 0.0F, 0.0F, texW, texH, texW, texH);
         ctx.method_51448().method_22909();
      }
   }

   private static void drawFallbackDot(class_332 ctx, float centerX, float centerY, int iconSize, int color) {
      int x = (int)(centerX - (float)(iconSize / 2));
      int y = (int)(centerY - (float)(iconSize / 2));
      ctx.method_25294(x, y, x + iconSize, y + iconSize, color);
   }

   private static void throttledDbg(WarpPoint w, WarpIconTexture tex, float sx, float sy, int iconSize, boolean clip, double scale, boolean selected) {
      long now = System.currentTimeMillis();
      if (now - lastDbgMs >= 2000L) {
         lastDbgMs = now;
         StringBuilder sb = new StringBuilder("[WarpRender-dbg]");
         sb.append(" warp=").append(w.getName());
         sb.append(" iconKey=").append(w.getIconKey());
         sb.append(" tex=").append(tex == null ? "null" : tex.getTextureId().toString());
         sb.append(" ready=").append(tex != null && tex.isReady());
         sb.append(" texWH=").append(tex == null ? -1 : tex.getImageWidth()).append("x").append(tex == null ? -1 : tex.getImageHeight());
         sb.append(" screen=(").append((int)sx).append(",").append((int)sy).append(")");
         sb.append(" iconSize=").append(iconSize);
         sb.append(" clip=").append(clip);
         sb.append(" selected=").append(selected);
         sb.append(" scale=").append(String.format("%.2f", scale));
         sb.append(" | cam=(").append(String.format("%.1f", lastDbgCamX)).append(",").append(String.format("%.1f", lastDbgCamZ)).append(")");
         sb.append(" origin=(").append(lastDbgOriginX).append(",").append(lastDbgOriginY).append(")");
         sb.append(" warpXYZ=(").append(w.getX()).append(",").append(w.getY()).append(",").append(w.getZ()).append(")");
         sb.append(" expected=(")
            .append((int)((double)lastDbgOriginX + ((double)w.getX() - lastDbgCamX) * lastDbgScale))
            .append(",")
            .append((int)((double)lastDbgOriginY + ((double)w.getZ() - lastDbgCamZ) * lastDbgScale))
            .append(")");
         MMmapMod.LOGGER.info(sb.toString());
      }
   }

   private static void updateDbgSnapshot(double camX, double camZ, int originX, int originY, double scale) {
      lastDbgCamX = camX;
      lastDbgCamZ = camZ;
      lastDbgOriginX = originX;
      lastDbgOriginY = originY;
      lastDbgScale = scale;
   }
}
