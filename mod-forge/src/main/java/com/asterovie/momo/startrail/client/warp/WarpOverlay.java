package com.asterovie.momo.startrail.client.warp;

import com.asterovie.momo.startrail.client.StarTrailMod;
import com.asterovie.momo.startrail.client.config.ConfigManager;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class WarpOverlay {
   /** 选中圈主色：与面板边框 / 导航路径同色（#00D8FF）。 */
   private static final int SELECTED_RING_COLOR = -16721665;
   /** 选中圈外描边色：深蓝黑，用来在明亮贴图上压出轮廓（不透明）。 */
   private static final int SELECTED_RING_OUTLINE_COLOR = -15658724;
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

               // 选中的传送点在图标外围套一个圈，一眼能看出"当前选的是这个"
               if (isSelected) {
                  drawSelectedRing(ctx, sx, sy, iconSize);
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

   /**
    * 给选中的传送点画一个圆环标记。
    *
    * <p>为了在**明亮的地图贴图**上也一眼可见，做了三件事（1.1.16 的细线在 1600% 缩放下
    * 几乎融进背景，80% 时圈又比图标还小）：
    * <ol>
    *   <li><b>线宽随图标缩放</b>：从 2px 起，图标越大线越粗（上限 8px），
    *       不再是任何缩放下都只有 1px；</li>
    *   <li><b>留白随图标缩放</b>：圈与图标之间留出与图标成比例的间隙，
    *       看起来是「外圈标记」而不是图标自带的花纹；</li>
    *   <li><b>双色环</b>：青色环外再包一圈深色描边，亮底暗底都能看清轮廓。</li>
    * </ol>
    *
    * <p>逐行计算弦宽再填充，比逐点采样更圆、不断点；上下边界按**行中心**对称取整。
    */
   private static void drawSelectedRing(GuiGraphics ctx, float centerX, float centerY, int iconSize) {
      // 图标半径；线宽与留白都按它成比例，保证小图标不糊、大图标不细
      double iconR = (double)Math.max(4, iconSize) / 2.0;
      int lineW = (int)Math.round(Math.max(2.0, Math.min(8.0, iconR * 0.18)));
      double gap = Math.max(3.0, iconR * 0.22);

      double outerR = iconR + gap + (double)lineW;
      int cx = Math.round(centerX);
      int cy = Math.round(centerY);

      // 先画外圈深色描边（比青环再粗 1px），形成明显的双色轮廓
      drawRingBand(ctx, cx, cy, outerR, (double)(lineW + 1), SELECTED_RING_OUTLINE_COLOR);
      // 再画青色主环
      drawRingBand(ctx, cx, cy, iconR + gap + (double)lineW / 2.0, (double)lineW, SELECTED_RING_COLOR);
   }

   /**
    * 画一条指定线宽的圆环带。
    *
    * @param midRadius 环带中心线的半径
    * @param width     环带宽度（像素）
    */
   private static void drawRingBand(GuiGraphics ctx, int cx, int cy, double midRadius, double width, int color) {
      double half = Math.max(0.5, width / 2.0);
      double rOuter = midRadius + half;
      double rInner = Math.max(0.0, midRadius - half);
      int rows = (int)Math.ceil(rOuter);
      int top = cy - rows;
      int bottom = cy + rows;

      for (int y = top; y <= bottom; y++) {
         double dy = (double)y + 0.5 - (double)cy;
         double outSq = rOuter * rOuter - dy * dy;
         if (outSq < 0.0) {
            continue;
         }

         int xOut = (int)Math.ceil(Math.sqrt(outSq));
         int xIn = 0;
         double inSq = rInner * rInner - dy * dy;
         if (inSq > 0.0) {
            xIn = (int)Math.floor(Math.sqrt(inSq));
         }

         // 左侧环带
         int lx0 = cx - xOut;
         int lx1 = cx - xIn;
         if (lx1 > lx0) {
            ctx.fill(lx0, y, lx1, y + 1, color);
         }

         // 右侧环带
         int rx0 = cx + xIn;
         int rx1 = cx + xOut;
         if (rx1 > rx0) {
            ctx.fill(rx0, y, rx1 + 1, y + 1, color);
         }
      }
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
