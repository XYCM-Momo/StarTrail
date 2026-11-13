package com.asterovie.momo.startrail.client.nav;

import com.asterovie.momo.startrail.client.StarTrailClient;
import com.asterovie.momo.startrail.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * 导航目标的屏幕标点。
 *
 * <p>配合 {@code /startrail nav <x> <z>}（以及大地图右键「导航至此」）使用：地面仍由
 * {@link NavigationManager} 用红石粒子铺出路线，屏幕这一层再补一个始终可见的目标指示——
 * <ul>
 *   <li>目标在视野内：在目标的屏幕位置画准星 + 距离；</li>
 *   <li>目标在视野外或身后：把标点贴到屏幕边缘，用箭头指出方向并显示距离。</li>
 * </ul>
 *
 * <p>只做 HUD 绘制，不改动网络协议，属于纯客户端功能。
 */
public final class NavMarkerOverlay {
   /** 主色：青色，与大地图/小地图上的导航路径同色。 */
   private static final int COLOR_PRIMARY = -16721665;
   /** 文字：浅青。 */
   private static final int COLOR_TEXT = -6559489;
   /** 文字底衬：半透明黑，保证任何场景下都读得清。 */
   private static final int COLOR_TEXT_BG = Integer.MIN_VALUE;
   /** 目标超出屏幕时，标点离屏幕边缘的留白。 */
   private static final int EDGE_MARGIN = 24;

   private NavMarkerOverlay() {
   }

   /**
    * 画导航标点，由 {@code RenderGuiEvent.Post} 每帧调用。
    */
   public static void render(GuiGraphics ctx, Minecraft client) {
      if (client.player == null || client.level == null) {
         return;
      }

      StarTrailClient startrail = StarTrailClient.getInstance();
      NavigationManager nav = startrail != null ? startrail.getNavigationManager() : null;
      if (nav == null || !nav.isActive()) {
         return;
      }

      // 只在导航所在的世界显示：跑去下界/末地时不该还指着主世界的坐标
      String world = ConfigManager.getInstance().getCurrentWorld();
      if (world == null || !world.equals(nav.getNavWorld())) {
         return;
      }

      // 允许玩家只留地面粒子（config/startrail.json → nav.showMarker）
      ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
      if (cfg != null && cfg.nav != null && !cfg.nav.showMarker) {
         return;
      }

      int screenW = client.getWindow().getGuiScaledWidth();
      int screenH = client.getWindow().getGuiScaledHeight();
      if (screenW <= 0 || screenH <= 0) {
         return;
      }

      double distance = nav.getDistanceToTarget();
      // 已经贴到目标跟前时不用再指：3 格内导航本身就会自动结束，这里提前让位，免得标点糊在准星上
      if (distance < 3.0) {
         return;
      }

      double[] projected = projectToScreen(client, nav, screenW, screenH);
      if (projected != null) {
         drawOnScreenMarker(ctx, client.font, (float)projected[0], (float)projected[1], distance);
      } else {
         drawOffScreenMarker(ctx, client.font, client, nav, screenW, screenH, distance);
      }
   }

   /**
    * 把目标投到屏幕上；返回 {x, y}，目标在相机背后或投影落到屏幕外时返回 null。
    */
   private static double[] projectToScreen(Minecraft client, NavigationManager nav, int screenW, int screenH) {
      double cameraX = client.gameRenderer.getMainCamera().getPosition().x;
      double cameraY = client.gameRenderer.getMainCamera().getPosition().y;
      double cameraZ = client.gameRenderer.getMainCamera().getPosition().z;

      double dx = nav.getTargetX() - cameraX;
      double dy = nav.getTargetY() - cameraY;
      double dz = nav.getTargetZ() - cameraZ;

      // 相机的三根基向量（与 MC 的 yaw/pitch 约定一致：
      // yaw=0 朝 +Z，yaw 增大向 -X 转；pitch>0 低头）
      float yaw = client.gameRenderer.getMainCamera().getYRot();
      float pitch = client.gameRenderer.getMainCamera().getXRot();
      double yawRad = Math.toRadians((double)yaw);
      double pitchRad = Math.toRadians((double)pitch);
      double sinYaw = Math.sin(yawRad);
      double cosYaw = Math.cos(yawRad);
      double sinPitch = Math.sin(pitchRad);
      double cosPitch = Math.cos(pitchRad);

      // forward = (-sin(yaw)cos(pitch), -sin(pitch), cos(yaw)cos(pitch))
      double fx = -sinYaw * cosPitch;
      double fy = -sinPitch;
      double fz = cosYaw * cosPitch;
      // right = (-cos(yaw), 0, -sin(yaw))
      // 即 Camera#getLeftVector() 取反。定标用的是「转身不变量」：
      // 目标固定在世界某处、玩家向右转（yaw 增大）时，目标必须往屏幕左边走。
      double rx = -cosYaw;
      double ry = 0.0;
      double rz = -sinYaw;
      // up = right × forward（与上面的 right 配套；写成 forward × right 会让上下翻转）
      double ux = ry * fz - rz * fy;
      double uy = rz * fx - rx * fz;
      double uz = rx * fy - ry * fx;

      double depth = dx * fx + dy * fy + dz * fz;
      if (depth <= 0.05) {
         return null; // 在相机背后或贴着相机：交给边缘指示
      }

      double horiz = dx * rx + dz * rz;
      double vert = dx * ux + dy * uy + dz * uz;

      double fovDeg = (double)((Integer)client.options.fov().get()).intValue();
      double tanHalf = Math.tan(Math.toRadians(fovDeg) / 2.0);
      if (tanHalf <= 1.0E-6) {
         tanHalf = Math.tan(Math.toRadians(70.0) / 2.0);
      }

      // 纵向按 fov 展开（屏幕高度对应 tanHalf），横向按宽高比自然展开
      double screenX = (double)screenW / 2.0 + horiz / depth / tanHalf * ((double)screenH / 2.0);
      double screenY = (double)screenH / 2.0 - vert / depth / tanHalf * ((double)screenH / 2.0);

      if (screenX < 0.0 || screenX > (double)screenW || screenY < 0.0 || screenY > (double)screenH) {
         return null; // 投影落在屏幕外：同样交给边缘指示
      }

      return new double[]{screenX, screenY};
   }
   /** 目标在视野内：四角准星 + 中心点 + 距离。 */
   private static void drawOnScreenMarker(GuiGraphics ctx, Font font, float screenX, float screenY, double distance) {
      int cx = Mth.floor(screenX);
      int cy = Mth.floor(screenY);
      int arm = 5;
      int gap = 2;
      // 四角准星，不画实心块，避免糊住目标本身
      ctx.fill(cx - arm - gap, cy - gap - 1, cx - gap, cy - gap, COLOR_PRIMARY);
      ctx.fill(cx - gap - 1, cy - arm - gap, cx - gap, cy - gap, COLOR_PRIMARY);
      ctx.fill(cx + gap + 1, cy - gap - 1, cx + arm + gap + 1, cy - gap, COLOR_PRIMARY);
      ctx.fill(cx + gap + 1, cy - arm - gap, cx + gap + 2, cy - gap, COLOR_PRIMARY);
      ctx.fill(cx - arm - gap, cy + gap + 1, cx - gap, cy + gap + 2, COLOR_PRIMARY);
      ctx.fill(cx - gap - 1, cy + gap + 1, cx - gap, cy + arm + gap + 2, COLOR_PRIMARY);
      ctx.fill(cx + gap + 1, cy + gap + 1, cx + arm + gap + 1, cy + gap + 2, COLOR_PRIMARY);
      ctx.fill(cx + gap + 1, cy + gap + 1, cx + gap + 2, cy + arm + gap + 2, COLOR_PRIMARY);
      // 中心点
      ctx.fill(cx - 1, cy - 1, cx + 1, cy + 1, COLOR_PRIMARY);

      String label = "导航目标  " + formatDistance(distance);
      int w = font.width(label);
      int lx = cx - w / 2;
      int ly = cy - arm - gap - 13;
      ctx.fill(lx - 3, ly - 1, lx + w + 3, ly + 9, COLOR_TEXT_BG);
      ctx.drawString(font, label, lx, ly, COLOR_TEXT, false);
   }

   /**
    * 目标在视野外：把标点贴到屏幕边缘，箭头指出方向，附距离。
    *
    * <p>方向用「目标相对玩家的水平方位」算，而不是拿相机投影结果外推——
    * 那样目标在身后时方向会反。
    */
   private static void drawOffScreenMarker(
      GuiGraphics ctx, Font font, Minecraft client, NavigationManager nav, int screenW, int screenH, double distance
   ) {
      double px = client.player != null ? client.player.getX() : 0.0;
      double pz = client.player != null ? client.player.getZ() : 0.0;
      // 直接用与 projectToScreen 同源的 right / forward 求「目标相对屏幕上方」的角度，
      // 不再单独推 atan2(bearing ± yaw) —— 那套和投影各写一套三角函数，改一处漏一处就会左右打架。
      //   h = 目标方向 · right   （屏幕横向分量，正为右）
      //   d = 目标方向 · forward （纵深分量，正为前）
      //   rel = atan2(h, d)：0 = 正前（贴在上边中间），+90° = 正右，±180° = 正后，-90° = 正左
      float yaw = client.gameRenderer.getMainCamera().getYRot();
      double yawRad = Math.toRadians((double)yaw);
      double hx = nav.getTargetX() - px;
      double hz = nav.getTargetZ() - pz;
      double fwdX = -Math.sin(yawRad);
      double fwdZ = Math.cos(yawRad);
      double rightX = -Math.cos(yawRad);
      double rightZ = -Math.sin(yawRad);
      double rel = Math.atan2(hx * rightX + hz * rightZ, hx * fwdX + hz * fwdZ);

      // 屏幕椭圆上的落点：rel=0（正前）落在正上方，顺时针沿边缘走
      double cx = (double)screenW / 2.0;
      double cy = (double)screenH / 2.0;
      double radiusX = Math.max(1.0, cx - (double)EDGE_MARGIN);
      double radiusY = Math.max(1.0, cy - (double)EDGE_MARGIN);
      double nx = Math.sin(rel);
      double ny = -Math.cos(rel);
      double ex = cx + nx * radiusX;
      double ey = cy + ny * radiusY;
      int ix = Mth.floor(ex);
      int iy = Mth.floor(ey);

      // 箭头：从贴边点朝外指
      double size = 8.0;
      double perpX = -ny;
      double perpY = nx;
      double tipX = ex + nx * size;
      double tipY = ey + ny * size;
      double baseX = ex - nx * 2.0;
      double baseY = ey - ny * 2.0;
      drawLine(ctx, tipX, tipY, baseX + perpX * size * 0.55, baseY + perpY * size * 0.55);
      drawLine(ctx, tipX, tipY, baseX - perpX * size * 0.55, baseY - perpY * size * 0.55);
      ctx.fill(ix - 2, iy - 2, ix + 2, iy + 2, COLOR_PRIMARY);

      // 距离文字：贴着标点，但夹在屏幕内
      String label = formatDistance(distance);
      int tw = font.width(label);
      int labelX = Mth.clamp(ix - tw / 2, 2, Math.max(2, screenW - tw - 2));
      int labelY = Mth.clamp(iy + 11, 2, Math.max(2, screenH - 12));
      ctx.fill(labelX - 2, labelY - 1, labelX + tw + 2, labelY + 9, COLOR_TEXT_BG);
      ctx.drawString(font, label, labelX, labelY, COLOR_TEXT, false);
   }

   /** 用一串 1px 方块画直线（Bresenham），不引入额外的渲染类型。 */
   private static void drawLine(GuiGraphics ctx, double x1, double y1, double x2, double y2) {
      int ix1 = Mth.floor(x1);
      int iy1 = Mth.floor(y1);
      int ix2 = Mth.floor(x2);
      int iy2 = Mth.floor(y2);
      int dx = Math.abs(ix2 - ix1);
      int dy = Math.abs(iy2 - iy1);
      int sx = ix1 < ix2 ? 1 : -1;
      int sy = iy1 < iy2 ? 1 : -1;
      int err = dx - dy;
      int x = ix1;
      int y = iy1;
      int guard = dx + dy + 2;

      while (guard-- > 0) {
         ctx.fill(x, y, x + 1, y + 1, COLOR_PRIMARY);
         if (x == ix2 && y == iy2) {
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

   /** 距离显示：1000 格以内取整，超过则压成「1.2k」，避免长数字占屏幕。 */
   private static String formatDistance(double distance) {
      if (distance >= 1000.0) {
         return String.format("%.1fk 格", distance / 1000.0);
      }

      return Math.round(distance) + " 格";
   }
}
