package com.asterovie.momo.startrail.client.nav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 「创建点」指令产生的独立标记点集合。
 *
 * <p>与导航（{@link NavigationManager}）分开：
 * <ul>
 *   <li>导航：一个终点，会算路径、铺地面粒子、做到达判定，同一时刻只能有一个；</li>
 *   <li>标记点：只是把位置标在屏幕上（与导航用同一套绘制），不导航、不画路径、不判到达，
 *       <b>可以同时存在多个</b>。</li>
 * </ul>
 *
 * <p>清空指令会把这个集合与导航一并清掉。
 */
public final class NavPointMarkerStore {
   /**
    * 一个标记点。
    *
    * <p>{@code y} 用 {@link Double#NaN} 表示「未指定高度」——
    * 渲染时按客户端本地推断（与导航的 hasY 语义一致）。
    */
   public static final class Marker {
      private final double x;
      private final double z;
      private final double y;
      private final String label;
      private final String world;

      public Marker(double x, double y, double z, String label, String world) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.label = label == null ? "" : label;
         this.world = world == null ? "" : world;
      }

      public double getX() {
         return this.x;
      }

      public double getZ() {
         return this.z;
      }

      /** 未指定高度时返回 {@link Double#NaN}。 */
      public double getY() {
         return this.y;
      }

      public boolean hasY() {
         return !Double.isNaN(this.y);
      }

      public String getLabel() {
         return this.label;
      }

      /** 创建该标记时玩家所在世界；空字符串表示「不限世界」。 */
      public String getWorld() {
         return this.world;
      }
   }

   private final List<Marker> markers = new ArrayList<>();

   /** 添加一个标记点；同一位置（XZ 相同且标签相同）不重复添加。 */
   public synchronized void add(Marker marker) {
      if (marker == null) {
         return;
      }

      for (Marker m : this.markers) {
         if (Math.abs(m.getX() - marker.getX()) < 0.01
            && Math.abs(m.getZ() - marker.getZ()) < 0.01
            && m.getLabel().equals(marker.getLabel())) {
            return;
         }
      }

      this.markers.add(marker);
   }

   /** 清空全部标记点。 */
   public synchronized void clear() {
      this.markers.clear();
   }

   public synchronized int size() {
      return this.markers.size();
   }

   public synchronized boolean isEmpty() {
      return this.markers.isEmpty();
   }

   /** 取快照，避免渲染时持有锁。 */
   public synchronized List<Marker> snapshot() {
      return Collections.unmodifiableList(new ArrayList<>(this.markers));
   }
}
