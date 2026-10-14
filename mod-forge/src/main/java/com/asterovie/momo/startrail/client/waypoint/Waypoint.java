package com.asterovie.momo.startrail.client.waypoint;

import java.util.List;

/**
 * 导航点位（与服务器 {@code plugins/StarTrail/waypoints/<地图ID>.json} 同一 JSON 格式）。
 * 客户端从 {@code .minecraft/startrail/waypoints/} 读取。
 * 坐标支持小数（JSON 里也允许整数，Gson 自动转 double）。
 */
public class Waypoint {
   public String id;
   public double x;
   public double y;
   public double z;
   public List<String> links = new java.util.ArrayList<>();

   public Waypoint() {
   }

   public double dist2D(double px, double pz) {
      double dx = this.x - px;
      double dz = this.z - pz;
      return Math.sqrt(dx * dx + dz * dz);
   }

   public String getId() {
      return this.id;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public List<String> getLinks() {
      return this.links;
   }
}
