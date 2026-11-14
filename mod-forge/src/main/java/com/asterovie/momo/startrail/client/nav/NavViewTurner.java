package com.asterovie.momo.startrail.client.nav;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * 导航开始时的「视角平滑转向目标」。
 *
 * <p>玩家发起导航（或服务端下发导航）后，把视角在一小段时间内平滑转到导航标点上，
 * 让人一眼知道该往哪走，不用自己找。
 *
 * <p>设计取舍（按作者要求）：
 * <ul>
 *   <li><b>任何导航都转</b>，包括玩家自己敲的 {@code /startrail nav}；</li>
 *   <li><b>转向期间不被鼠标打断</b>：每 tick 重新覆盖 yaw/pitch，一直转到对准为止；</li>
 *   <li><b>约 0.6 秒转完</b>，角度大时按比例延长（上限约 1.6 秒），避免 180° 时瞬移。</li>
 * </ul>
 *
 * <p>转向只改 yaw / pitch，**不动玩家位置**，也不影响寻路（寻路走的是坐标，与朝向无关）。
 */
public final class NavViewTurner {
   /** 基准时长（毫秒）：转 90° 大约用这么久。 */
   private static final double BASE_MS_PER_90_DEG = 600.0;
   /** 单次转向的最长时间（毫秒），防止 180° 时拖太久。 */
   private static final double MAX_MS = 1600.0;
   /** 每 tick 结束后，当剩余角度小于该值（度）就认为对准了，直接收尾。 */
   private static final double SNAP_DEG = 0.5;

   private boolean active = false;
   private long startMs;
   private long durationMs;
   private double startYaw;
   private double startPitch;
   /** 目标朝向（度）。yaw 用「展开后」的角度，避免 ±180° 边界处抄近路时方向乱跳。 */
   private double targetYawUnwrapped;
   private double targetPitch;

   /** 开始一次转向；from 为玩家当前朝向。 */
   public void start(LocalPlayer player, double targetX, double targetY, double targetZ) {
      double dx = targetX - player.getX();
      double dy = targetY - (player.getY() + (double)player.getEyeHeight());
      double dz = targetZ - player.getZ();
      double horizontal = Math.sqrt(dx * dx + dz * dz);

      // MC 约定：yaw=0 朝 +Z，yaw 增大向右转（yaw=90 朝 -X）
      double wantYaw = Math.toDegrees(Math.atan2(-dx, dz));
      // pitch：正值向下看；目标在下方时为正
      double wantPitch = -Math.toDegrees(Math.atan2(dy, horizontal));

      this.startYaw = player.getYRot();
      this.startPitch = player.getXRot();
      // 把目标 yaw 对齐到离当前朝向最近的等价角，保证走最短弧、不绕圈
      this.targetYawUnwrapped = this.startYaw + Mth.wrapDegrees((float)(wantYaw - this.startYaw));
      this.targetPitch = Mth.clamp((float)wantPitch, -90.0F, 90.0F);

      double deltaYaw = Math.abs(this.targetYawUnwrapped - this.startYaw);
      double deltaPitch = Math.abs(this.targetPitch - this.startPitch);
      // 以「主要转动的角度」估算时长：90° 用 BASE_MS_PER_90_DEG，按比例放大并封顶
      double dominant = Math.max(deltaYaw, deltaPitch);
      this.durationMs = (long)Math.min(MAX_MS, BASE_MS_PER_90_DEG * Math.max(1.0, dominant / 90.0));

      this.startMs = System.currentTimeMillis();
      this.active = true;
   }

   public boolean isActive() {
      return this.active;
   }

   public void cancel() {
      this.active = false;
   }

   /**
    * 每帧调用（由 {@code RenderGuiEvent.Post} 驱动）：把玩家朝向朝目标推进一步。
    *
    * <p><b>为什么用帧而不是 tick</b>：tick 只有 20Hz，转 90° 时每 tick 要跳 4.5°，
    * 肉眼就是「一格一格」的。渲染帧通常是 60~144Hz，逐帧推进才平滑。
    *
    * <p>用「每帧直接覆盖」而不是累积增量，所以鼠标在转向期间拖动会被抵消——
    * 这是刻意的（作者要求转到对准为止）。
    */
   public void tick(LocalPlayer player) {
      if (!this.active || player == null) {
         return;
      }

      long elapsed = System.currentTimeMillis() - this.startMs;
      double t = this.durationMs <= 0L ? 1.0 : Math.min(1.0, (double)elapsed / (double)this.durationMs);
      // ease-in-out（smoothstep）：两端都收着走，中段快，比纯 ease-out 更顺、没有突然启动的顿感
      double eased = t * t * (3.0 - 2.0 * t);

      float yaw = (float)(this.startYaw + (this.targetYawUnwrapped - this.startYaw) * eased);
      float pitch = (float)(this.startPitch + (this.targetPitch - this.startPitch) * eased);
      player.setYRot(yaw);
      player.setXRot(pitch);
      // 头部朝向跟着走，否则第三人称下脑袋会往回拧
      player.setYHeadRot(yaw);

      // 收尾：时间到，或已经足够接近
      double restYaw = Math.abs(this.targetYawUnwrapped - (double)yaw);
      double restPitch = Math.abs(this.targetPitch - (double)pitch);
      if (t >= 1.0 || (restYaw < SNAP_DEG && restPitch < SNAP_DEG)) {
         player.setYRot((float)this.targetYawUnwrapped);
         player.setXRot((float)this.targetPitch);
         player.setYHeadRot((float)this.targetYawUnwrapped);
         this.active = false;
      }
   }

   /** 供调试/显示用：剩余时长（毫秒）。 */
   public long getRemainingMs() {
      if (!this.active) {
         return 0L;
      }

      long elapsed = System.currentTimeMillis() - this.startMs;
      return Math.max(0L, this.durationMs - elapsed);
   }
}
