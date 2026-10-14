package com.asterovie.momo.startrail.client.nav;

import com.asterovie.momo.startrail.client.config.ConfigManager;
import com.asterovie.momo.startrail.client.waypoint.Waypoint;
import com.asterovie.momo.startrail.client.waypoint.WaypointStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;

/**
 * 客户端导航：目标点 + 点位图 + 最短路径。
 *
 * <p>路径生成（全程客户端）：玩家 → 前方最近的入口点位 →（点位图 Dijkstra 最短路径）→
 * 离目标最近的点位 → 目标点。**玩家已经走过的点位不再接入路径**（自动换成前方未走过的
 * 节点进网络），只有整条点位网络都在玩家身后时才直接直线连目标（终点冲刺）。
 * 路径用原版红石粒子（DUST 红色）在游戏内显示，同时提供给大地图/小地图绘制折线。
 */
public class NavigationManager {
   private final WaypointStore waypointStore;
   private boolean active = false;
   private String navWorld = null;
   private double targetX;
   private double targetZ;
   /** 目标地表高度：启动时按目标坐标查高度图得出（参照 /startrail tp 的 Y 自判断）。 */
   private double targetY = 64.0;
   /** 当前导航世界的点位（启动时加载缓存，供每 5 tick 重算路径）。 */
   private List<Waypoint> wps = new ArrayList<>();
   private String lastError = "";
   private final List<double[]> path = new ArrayList<>();
   private static final int PARTICLE_PER_TICK = 100;
   private static final int PARTICLE_INTERVAL_TICKS = 4;
   /** 路径实时刷新间隔（tick）。 */
   private static final int PATH_REFRESH_TICKS = 5;

   public NavigationManager(WaypointStore waypointStore) {
      this.waypointStore = waypointStore;
   }

   public boolean isActive() {
      return this.active;
   }

   public String getNavWorld() {
      return this.navWorld;
   }

   public double getTargetX() {
      return this.targetX;
   }

   public double getTargetZ() {
      return this.targetZ;
   }

   public List<double[]> getPath() {
      return this.path;
   }

   public String getLastError() {
      return this.lastError;
   }

   /** 发起导航；失败返回 false，原因见 {@link #getLastError()}。 */
   public boolean startNav(double targetX, double targetZ) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) {
         this.lastError = "\u5f53\u524d\u4e0d\u5728\u6e38\u620f\u4e2d";
         return false;
      }

      this.waypointStore.reload();
      String world = ConfigManager.getInstance().getCurrentWorld();
      if (world == null) {
         this.lastError = "\u5c1a\u672a\u8fde\u63a5\u670d\u52a1\u5668";
         return false;
      }

      List<Waypoint> wps = this.waypointStore.getForWorld(world);
      if (wps.isEmpty()) {
         this.lastError = "\u5f53\u524d\u4e16\u754c\u6ca1\u6709\u70b9\u4f4d\uff08\u7f3a\u5c11 .minecraft/startrail/waypoints/ \u914d\u7f6e\uff09";
         return false;
      }

      this.wps = wps;
      this.targetX = targetX;
      this.targetZ = targetZ;
      // 目标 Y：参照 /startrail tp 自动判断地表高度，而不是用玩家当前高度
      this.targetY = this.findSurfaceY(targetX, targetZ);
      this.active = true;
      this.navWorld = world;

      if (!this.rebuildPath(client.player.getX(), client.player.getY(), client.player.getZ())) {
         this.active = false;
         this.navWorld = null;
         this.wps = new ArrayList<>();
         this.lastError = "\u70b9\u4f4d\u4e4b\u95f4\u6ca1\u6709\u8fde\u901a\u8def\u5f84\uff08\u8bf7\u5148\u7528 /startrail waypoint link \u8fde\u901a\uff09";
         return false;
      }

      this.lastError = "";
      return true;
   }

   /**
    * 按当前玩家位置重算路径（玩家 → 前方入口点位 →（Dijkstra）→ 目标点）。
    * 核心原则：**玩家已经走过的点位（在玩家身后）不再接入路径**，入口自动选前方
    * 未走过的节点；只有整条点位网络都在身后时才直接直线连目标。
    * 返回 false 表示无连通路径（此时保留旧路径不动）。
    */
   private boolean rebuildPath(double px, double py, double pz) {
      if (!this.active) {
         return false;
      }

      // 目标区块随接近加载后，自动把目标高度校正为真实地表（远处目标渐进修正）
      this.targetY = this.findSurfaceY(this.targetX, this.targetZ);

      Waypoint targetWp = nearest(this.wps, this.targetX, this.targetZ);

      // 终点侧：离目标最近的节点也已在玩家身后 = 玩家已经走过整条点位网络
      //（比如目标在点位网络尽头之外、玩家已走到最后一段），此时没有节点可跟，
      // 直接直线连目标（真正的终点冲刺，不会中途乱拉直线）。
      if (targetWp == null || isBehind(targetWp, px, pz)) {
         this.path.clear();
         this.path.add(new double[]{px, py, pz});
         this.path.add(new double[]{this.targetX, this.targetY, this.targetZ});
         return true;
      }

      // 反向 Dijkstra（从 targetWp 出发）：得到每个节点沿网络到目标的最短距离
      DijkstraResult res = dijkstra(this.wps, targetWp);

      // 入口节点：前方还没走过的节点里，选「离玩家最近」的那个当入口（沿网络走到目标）。
      // 注意不要用「直线距离+网络距离」评分选入口——目标侧节点网络距离为 0，评分几乎
      // 总是最小，路径就会退化成「玩家→离目标最近的节点→目标」，绕过中间的路线网络
      // （直线链上各节点评分恰好相等，平局被 targetWp 抢走，更会稳定触发）。
      Waypoint entry = null;
      double bestD = Double.MAX_VALUE;
      for (Waypoint wp : this.wps) {
         if (isBehind(wp, px, pz)) {
            continue; // 走过的节点不再接入路径
         }
         if (!res.dist.containsKey(wp.getId())) {
            continue; // 到目标不可达的节点不参与
         }
         double d = wp.dist2D(px, pz);
         if (d < bestD) {
            bestD = d;
            entry = wp;
         }
      }

      if (entry == null) {
         // 前方没有任何可达节点（理论上只在玩家已越过全部节点时发生）：直线连目标
         this.path.clear();
         this.path.add(new double[]{px, py, pz});
         this.path.add(new double[]{this.targetX, this.targetY, this.targetZ});
         return true;
      }

      List<Waypoint> route = reconstruct(this.wps, res.prev, entry, targetWp);
      if (route.isEmpty()) {
         return false;
      }

      this.path.clear();
      this.path.add(new double[]{px, py, pz});
      for (Waypoint wp : route) {
         this.path.add(new double[]{wp.getX(), wp.getY(), wp.getZ()});
      }

      this.path.add(new double[]{this.targetX, this.targetY, this.targetZ});
      return true;
   }

   /** 节点是否在玩家身后（相对玩家→目标方向，夹角 > 90° 即视为已走过）。 */
   private boolean isBehind(Waypoint wp, double px, double pz) {
      double toTargetX = this.targetX - px;
      double toTargetZ = this.targetZ - pz;
      double toWpX = wp.getX() - px;
      double toWpZ = wp.getZ() - pz;
      return toTargetX * toWpX + toTargetZ * toWpZ < 0.0;
   }

   /**
    * 目标坐标的地表高度（参照服务端 /startrail tp 的 Y 自判断）：
    * 目标区块已加载时取 MOTION_BLOCKING 最高方块 Y（即方块顶部所在高度，粒子再 +0.2 悬空可见）；
    * 未加载/未知时退回玩家当前高度（客户端不能同步加载远距离区块）。
    */
   private double findSurfaceY(double tx, double tz) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) {
         return 64.0;
      }

      int cx = (int)Math.floor(tx);
      int cz = (int)Math.floor(tz);
      if (client.level.hasChunk(cx >> 4, cz >> 4)) {
         int y = client.level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
         if (y > client.level.getMinBuildHeight()) {
            return (double)y;
         }
      }

      return client.player.getY();
   }

   public void stopNav() {
      this.active = false;
      this.navWorld = null;
      this.path.clear();
      this.wps = new ArrayList<>();
   }

   /** 客户端 tick：路径每 5 tick 重算（跟随玩家移动）；生成粒子；到达目的地自动停止。 */
   public void tick() {
      if (!this.active) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) {
         this.stopNav();
         return;
      }

      String world = ConfigManager.getInstance().getCurrentWorld();
      if (world == null || !world.equals(this.navWorld)) {
         return; // 在其它世界保持挂起，回到导航世界继续
      }

      // 实时刷新：玩家到最近点位的线段随移动更新
      if (client.player.tickCount % PATH_REFRESH_TICKS == 0) {
         this.rebuildPath(client.player.getX(), client.player.getY(), client.player.getZ());
      }

      double dx = client.player.getX() - this.targetX;
      double dz = client.player.getZ() - this.targetZ;
      if (dx * dx + dz * dz < 9.0) {
         this.stopNav();
         if (client.player != null) {
            client.player.displayClientMessage(Component.literal("\u00a7a[\u5bfc\u822a] \u5df2\u5230\u8fbe\u76ee\u7684\u5730"), false);
         }

         return;
      }

      if (client.player.tickCount % PARTICLE_INTERVAL_TICKS == 0) {
         this.spawnParticles(client);
      }
   }

   private void spawnParticles(Minecraft client) {
      if (this.path.size() < 2) {
         return;
      }

      double total = pathLength();
      if (total <= 0.0) {
         return;
      }

      int phase = (client.player.tickCount / PARTICLE_INTERVAL_TICKS) % 2;
      DustParticleOptions dust = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.0F);

      for (int i = 0; i < PARTICLE_PER_TICK; i++) {
         double f = ((double)i + 0.5 + (double)phase) / (double)PARTICLE_PER_TICK;
         if (f > 1.0) {
            f -= 1.0;
         }

         double[] p = this.pointAt(f);
         client.level.addAlwaysVisibleParticle(dust, p[0], p[1] + 0.2, p[2], 0.0, 0.0, 0.0);
      }
   }

   private double pathLength() {
      double total = 0.0;
      for (int i = 0; i < this.path.size() - 1; i++) {
         double[] a = this.path.get(i);
         double[] b = this.path.get(i + 1);
         double dx = b[0] - a[0];
         double dy = b[1] - a[1];
         double dz = b[2] - a[2];
         total += Math.sqrt(dx * dx + dy * dy + dz * dz);
      }

      return total;
   }

   /** 在折线上按比例取点（线性插值）。 */
   private double[] pointAt(double fraction) {
      int n = this.path.size();
      double total = 0.0;
      double[] segLen = new double[n - 1];
      for (int i = 0; i < n - 1; i++) {
         double[] a = this.path.get(i);
         double[] b = this.path.get(i + 1);
         double dx = b[0] - a[0];
         double dy = b[1] - a[1];
         double dz = b[2] - a[2];
         segLen[i] = Math.sqrt(dx * dx + dy * dy + dz * dz);
         total += segLen[i];
      }

      double target = fraction * total;
      double acc = 0.0;
      for (int i = 0; i < n - 1; i++) {
         if (acc + segLen[i] >= target || i == n - 2) {
            double t = segLen[i] <= 1.0E-6 ? 0.0 : (target - acc) / segLen[i];
            double[] a = this.path.get(i);
            double[] b = this.path.get(i + 1);
            return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
         }

         acc += segLen[i];
      }

      double[] last = this.path.get(n - 1);
      return new double[]{last[0], last[1], last[2]};
   }

   private static Waypoint nearest(List<Waypoint> wps, double x, double z) {
      Waypoint best = null;
      double bestD = Double.MAX_VALUE;
      for (Waypoint wp : wps) {
         double d = wp.dist2D(x, z);
         if (d < bestD) {
            bestD = d;
            best = wp;
         }
      }

      return best;
   }

   /** Dijkstra 结果：dist = 各节点到源点的最短距离，prev = 回溯链（指向更靠近源点的一侧）。 */
   private static class DijkstraResult {
      final Map<String, Double> dist = new HashMap<>();
      final Map<String, String> prev = new HashMap<>();
   }

   /** 从 source 出发跑一遍 Dijkstra（3D 距离为权重，双向边；同层≈2D，跨层按高度计权），供路径回溯复用。 */
   private static DijkstraResult dijkstra(List<Waypoint> wps, Waypoint source) {
      DijkstraResult r = new DijkstraResult();
      Map<String, Waypoint> byId = new HashMap<>();
      for (Waypoint wp : wps) {
         byId.put(wp.getId(), wp);
      }

      Set<String> done = new HashSet<>();
      PriorityQueue<Waypoint> pq = new PriorityQueue<>(
         Comparator.comparingDouble(w -> r.dist.getOrDefault(w.getId(), Double.MAX_VALUE))
      );
      r.dist.put(source.getId(), 0.0);
      pq.add(source);

      while (!pq.isEmpty()) {
         Waypoint cur = pq.poll();
         if (!done.add(cur.getId())) {
            continue;
         }

         for (String linkId : cur.getLinks()) {
            Waypoint next = byId.get(linkId);
            if (next == null || done.contains(linkId)) {
               continue;
            }

            double dx = next.getX() - cur.getX();
            double dy = next.getY() - cur.getY();
            double dz = next.getZ() - cur.getZ();
            // 3D 边权：层内两点 Y 几乎相等 → 与 2D 距离一致；跨层连线（楼梯/电梯）按真实高度计权
            double w = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double nd = r.dist.getOrDefault(cur.getId(), Double.MAX_VALUE) + w;
            if (nd < r.dist.getOrDefault(linkId, Double.MAX_VALUE)) {
               r.dist.put(linkId, nd);
               r.prev.put(linkId, cur.getId());
               pq.add(next);
            }
         }
      }

      return r;
   }

   /** 用 Dijkstra（从 target 出发）的 prev 链还原 entry → target 的路径；不可达返回空列表。 */
   private static List<Waypoint> reconstruct(List<Waypoint> wps, Map<String, String> prev, Waypoint entry, Waypoint target) {
      Map<String, Waypoint> byId = new HashMap<>();
      for (Waypoint wp : wps) {
         byId.put(wp.getId(), wp);
      }

      List<Waypoint> route = new ArrayList<>();
      String curId = entry.getId();
      Set<String> guard = new HashSet<>();
      while (curId != null) {
         Waypoint wp = byId.get(curId);
         if (wp == null || !guard.add(curId)) {
            break;
         }

         route.add(wp);
         if (curId.equals(target.getId())) {
            break;
         }

         curId = prev.get(curId);
      }

      if (route.isEmpty() || !route.get(route.size() - 1).getId().equals(target.getId())) {
         return new ArrayList<>(); // 不可达
      }

      return route;
   }
}
