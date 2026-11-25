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
 *   <li>目标在视野外或身后：把标点贴到屏幕边缘，用一个小点标出方位并显示距离。</li>
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
      // 已经到目标跟前时让位，免得标点糊在准星上。
      // 判据要和 NavigationManager 的到达判定一致：只比三维总距离的话，
      // 站在楼内点位的正下方（水平重合、高度差 5 格）也会被藏掉——那恰恰是最需要指引的时候。
      if (nav.isArrived(client.player)) {
         return;
      }

      double[] projected = projectToScreen(client, nav.getTargetX(), nav.getTargetY(), nav.getTargetZ(), screenW, screenH);
      String targetLabel = nav.getTargetLabel();
      if (projected != null) {
         drawOnScreenMarker(ctx, client.font, (float)projected[0], (float)projected[1], distance, targetLabel);
      } else {
         drawOffScreenMarker(ctx, client.font, client, nav.getTargetX(), nav.getTargetZ(), screenW, screenH, distance, targetLabel);
      }
   }

   /**
    * 渲染「创建点」指令产生的独立标记点。
    *
    * <p>与导航共用同一套绘制（{@code drawOnScreenMarker} / {@code drawOffScreenMarker}），
    * 但**不算路径、不铺粒子、不做到达判定**——只是把位置标在屏幕上，且**可以有多个**。
    */
   public static void renderPointMarkers(GuiGraphics ctx, Minecraft client) {
      if (client.player == null || client.level == null) {
         return;
      }

      StarTrailClient startrail = StarTrailClient.getInstance();
      if (startrail == null) {
         return;
      }

      NavPointMarkerStore store = startrail.getNavPointMarkerStore();
      if (store == null || store.isEmpty()) {
         return;
      }

      ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
      if (cfg != null && cfg.nav != null && !cfg.nav.showMarker) {
         return;
      }

      int screenW = client.getWindow().getGuiScaledWidth();
      int screenH = client.getWindow().getGuiScaledHeight();
      if (screenW <= 0 || screenH <= 0) {
         return;
      }

      NavigationManager nav = startrail.getNavigationManager();
      String currentWorld = ConfigManager.getInstance().getCurrentWorld();

      for (NavPointMarkerStore.Marker marker : store.snapshot()) {
         // 只显示在创建该标记时所在的世界
         if (!marker.getWorld().isEmpty() && currentWorld != null && !marker.getWorld().equals(currentWorld)) {
            continue;
         }

         // 未指定 Y 时按地表推断（与导航的 hasY=false 行为一致）
         double ty = marker.hasY()
            ? marker.getY()
            : (nav != null ? nav.resolveSurfaceY(marker.getX(), marker.getZ()) : client.player.getY());

         double dx = marker.getX() - client.player.getX();
         double dz = marker.getZ() - client.player.getZ();
         double distance = Math.sqrt(dx * dx + dz * dz);

         double[] projected = projectToScreen(client, marker.getX(), ty, marker.getZ(), screenW, screenH);
         if (projected != null) {
            drawOnScreenMarker(ctx, client.font, (float)projected[0], (float)projected[1], distance, marker.getLabel());
         } else {
            drawOffScreenMarker(ctx, client.font, client, marker.getX(), marker.getZ(), screenW, screenH, distance, marker.getLabel());
         }
      }
   }
   /**
    * 把目标投到屏幕上；返回 {x, y}，目标在相机背后或投影落到屏幕外时返回 null。
    */
   private static double[] projectToScreen(Minecraft client, double tx, double ty, double tz, int screenW, int screenH) {
      double cameraX = client.gameRenderer.getMainCamera().getPosition().x;
      double cameraY = client.gameRenderer.getMainCamera().getPosition().y;
      double cameraZ = client.gameRenderer.getMainCamera().getPosition().z;

      double dx = tx - cameraX;
      double dy = ty - cameraY;
      double dz = tz - cameraZ;

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
   /** 目标在视野内：四角准星 + 中心点 + 名称与距离。 */
   private static void drawOnScreenMarker(GuiGraphics ctx, Font font, float screenX, float screenY, double distance, String targetLabel) {
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

      // 有名称（服务端 /startrail nav 下发的导航点）就把它顶在最前面，玩家一眼知道在往哪走
      String label = targetLabel.isEmpty()
         ? "导航目标  " + formatDistance(distance)
         : targetLabel + "  " + formatDistance(distance);
      int w = font.width(label);
      int lx = cx - w / 2;
      int ly = cy - arm - gap - 13;
      ctx.fill(lx - 3, ly - 1, lx + w + 3, ly + 9, COLOR_TEXT_BG);
      ctx.drawString(font, label, lx, ly, COLOR_TEXT, false);
   }

   /**
    * 目标在视野外：把标点贴到屏幕边缘，标出方位，附距离。
    *
    * <p>方向用「目标相对玩家的水平方位」算，而不是拿相机投影结果外推——
    * 那样目标在身后时方向会反。
    */
   private static void drawOffScreenMarker(
      GuiGraphics ctx, Font font, Minecraft client, double tx, double tz, int screenW, int screenH, double distance, String targetLabel
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
      double hx = tx - px;
      double hz = tz - pz;
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

      // 视野外标记：一个小方点。
      // 演变：箭头（依赖朝向角，视角一偏就错位）→ 圆圈+中心点（形状仍偏重）→ 现在只留一个点。
      // 落点本身就表示方位（贴在屏幕哪一侧），点已经足够，不必再画任何形状。
      ctx.fill(ix - 2, iy - 2, ix + 3, iy + 3, COLOR_PRIMARY);

      // 贴边文字：有名称就「名称  距离」，否则只有距离；夹在屏幕内
      String label = targetLabel.isEmpty() ? formatDistance(distance) : targetLabel + "  " + formatDistance(distance);
      int tw = font.width(label);
      int labelX = Mth.clamp(ix - tw / 2, 2, Math.max(2, screenW - tw - 2));
      int labelY = Mth.clamp(iy + 11, 2, Math.max(2, screenH - 12));
      ctx.fill(labelX - 2, labelY - 1, labelX + tw + 2, labelY + 9, COLOR_TEXT_BG);
      ctx.drawString(font, label, labelX, labelY, COLOR_TEXT, false);
   }

   /** 距离显示：1000 格以内取整，超过则压成「1.2k」，避免长数字占屏幕。 */
   private static String formatDistance(double distance) {
      if (distance >= 1000.0) {
         return String.format("%.1fk 格", distance / 1000.0);
      }

      return Math.round(distance) + " 格";
   }
}
