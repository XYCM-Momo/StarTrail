package com.asterovie.momo.startrail.client.warp;

import com.asterovie.momo.startrail.client.StarTrailMod;
import com.asterovie.momo.startrail.client.config.ConfigManager;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

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
      GuiGraphics ctx,
      Font textRenderer,
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
         -1,
         null
      );
   }

   /**
    * 绘制传送点图标。
    *
    * <p><b>注意</b>：自 1.1.11 起传送点**只画图标、不画名称**，
    * 所以 textRenderer / labelAbove / panelX0..panelY1 / hideRect
    * 已不再参与绘制（保留签名以兼容现有调用点）。名称改由选中后的详情面板承担。
    */
   public static void render(
      String world,
      WarpPointStore store,
      WarpIconManager iconMgr,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      GuiGraphics ctx,
      Font textRenderer,
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
      int panelY1,
      int[] hideRect
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

               // 传送点名称不再绘制：大小地图都只显示图标。
               // 名字改由「选中后的详情面板」和悬停提示承担，地图上更干净、不会互相压字。
               // （原实现在此处按 labelAbove 计算文字位置并画底衬 + 名字，已移除。）
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
      GuiGraphics ctx,
      Font textRenderer,
      int baseIconSize,
      String selectedId,
      int[] hideRect
   ) {
      render(world, store, iconMgr, camX, camZ, originX, originY, scale, ctx, textRenderer, false, 0, 0, 0, baseIconSize, true, selectedId, -1, 0, -1, 0, hideRect);
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
      GuiGraphics ctx,
      Font textRenderer,
      int baseIconSize,
      String selectedId,
      int panelX0,
      int panelY0,
      int panelX1,
      int panelY1,
      int[] hideRect
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
         panelY1,
         hideRect
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
      GuiGraphics ctx,
      Font textRenderer,
      int clipCx,
      int clipCy,
      int clipR,
      int baseIconSize,
      String selectedId
   ) {
      render(world, store, iconMgr, camX, camZ, originX, originY, scale, ctx, textRenderer, true, clipCx, clipCy, clipR, baseIconSize, false, selectedId);
   }

   private static void drawIcon(GuiGraphics ctx, WarpIconTexture tex, float centerX, float centerY, int iconSize) {
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
         ctx.pose().pushPose();
         ctx.pose().translate(fx - (float)ix, fy - (float)iy, 0.0F);
         ctx.blit(tex.getTextureId(), ix, iy, drawW, drawH, 0.0F, 0.0F, texW, texH, texW, texH);
         ctx.pose().popPose();
      }
   }

   private static void drawFallbackDot(GuiGraphics ctx, float centerX, float centerY, int iconSize, int color) {
      int x = (int)(centerX - (float)(iconSize / 2));
      int y = (int)(centerY - (float)(iconSize / 2));
      ctx.fill(x, y, x + iconSize, y + iconSize, color);
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
         StarTrailMod.LOGGER.info(sb.toString());
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
