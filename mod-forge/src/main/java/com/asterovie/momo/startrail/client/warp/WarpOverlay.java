package com.asterovie.momo.startrail.client.warp;

import com.asterovie.momo.startrail.client.StarTrailMod;
import com.asterovie.momo.startrail.client.config.ConfigManager;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class WarpOverlay {
   /**
    * 选中效果的阴影色：深蓝黑（与面板底色同系），靠**透明度**做出渐隐。
    *
    * <p>1.1.17 试过"青色环 + 深色描边"的双色环，观感像轮胎、反而更丑（使用者反馈），
    * 这里改回单色思路：不描边，只在图标外缘铺一圈由深到透明的圆形阴影。
    */
   private static final int SELECTED_SHADOW_RGB = 0x0A1018;
   /** 阴影最内圈的 alpha（0~255）：紧贴图标处最深，向外衰减到 0。 */
   private static final int SELECTED_SHADOW_ALPHA_INNER = 110;
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
    * 选中传送点的标记：在图标外围铺一圈**径向渐隐的圆形阴影**。
    *
    * <p>设计思路：不描边、不画环，而是在图标下方"压"一个由深到透明的圆形阴影，
    * 像这块被照亮/托起来一样——选中感明显但不抢眼，也不会像双色环那样显得笨重。
    *
    * <p>做法：从图标外缘到外扩半径，用若干个同心圆环带依次填充，
    * 每层的 alpha 由 {@code SELECTED_SHADOW_ALPHA_INNER} 线性衰减到 0，
    * 得到平滑的渐隐过渡（GuiGraphics.fill 不支持渐变，只能分层近似）。
    * 半径与层数都随图标大小自适应，任何缩放下比例一致。
    */
   private static void drawSelectedRing(GuiGraphics ctx, float centerX, float centerY, int iconSize) {
      double iconR = (double)Math.max(4, iconSize) / 2.0;
      // 图标是按**方形**绘制的，而阴影是圆形：内缘必须用方形的**外接圆**（iconR * √2），
      // 否则圆形覆盖不到方形四角、却会盖住四条边的中部。
      double squareCircumRadius = iconR * 1.41421356;
      double innerR = squareCircumRadius + 1.0;
      double outerR = innerR + Math.max(6.0, iconR * 0.5);

      int cx = Math.round(centerX);
      int cy = Math.round(centerY);

      // 层数随环带宽度增长，保证过渡细腻
      int layers = (int)Math.max(6, Math.min(24, Math.round((outerR - innerR) / 1.5)));
      for (int i = layers - 1; i >= 0; i--) {
         // i=layers-1 最外层（最淡），i=0 最内层（最深）；倒序绘制让内层覆盖外层
         double t = (double)i / (double)Math.max(1, layers - 1); // 0=内 1=外
         double r = innerR + (outerR - innerR) * t;
         int alpha = (int)Math.round((double)SELECTED_SHADOW_ALPHA_INNER * (1.0 - t));
         if (alpha <= 0) {
            continue;
         }

         int color = (alpha << 24) | SELECTED_SHADOW_RGB;
         // 关键：这里必须画**环带**而不是实心圆盘。
         // 早先用实心圆叠加，最内层那个 r=innerR 的圆盘会把整个方形图标盖住
         // （表现就是一坨黑影糊在图标上）。
         fillRing(ctx, cx, cy, r, innerR, color);
      }
   }

   /**
    * 填充一条圆环带（内径固定为 {@code innerR}，外径为 {@code outerR}），内缘以内保持透明。
    */
   private static void fillRing(GuiGraphics ctx, int cx, int cy, double outerR, double innerR, int color) {
      int rows = (int)Math.ceil(outerR);
      for (int y = cy - rows; y <= cy + rows; y++) {
         double dy = (double)y + 0.5 - (double)cy;
         double outSq = outerR * outerR - dy * dy;
         if (outSq < 0.0) {
            continue;
         }

         int xOut = (int)Math.ceil(Math.sqrt(outSq));
         int xIn = 0;
         double inSq = innerR * innerR - dy * dy;
         if (inSq > 0.0) {
            xIn = (int)Math.ceil(Math.sqrt(inSq));
         }

         // 左侧环带
         if (cx - xIn > cx - xOut) {
            ctx.fill(cx - xOut, y, cx - xIn, y + 1, color);
         }

         // 右侧环带
         if (cx + xOut > cx + xIn) {
            ctx.fill(cx + xIn, y, cx + xOut + 1, y + 1, color);
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
