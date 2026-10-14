package com.asterovie.momo.startrail.server.waypoint;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 导航点位：一个 id + 世界坐标 + 可连通的其它点位 id 列表。
 * 序列化为 JSON（{id, x, y, z, links}），与客户端点位文件共用同一格式。
 * 坐标支持小数（方块中心等），JSON 里也允许整数。
 */
public class Waypoint {
   private String id;
   private double x;
   private double y;
   private double z;
   private final Set<String> links = new LinkedHashSet<>();

   public Waypoint() {
   }

   public Waypoint(String id, double x, double y, double z) {
      this.id = id;
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public String getId() {
      return this.id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public double getX() {
      return this.x;
   }

   public void setX(double x) {
      this.x = x;
   }

   public double getY() {
      return this.y;
   }

   public void setY(double y) {
      this.y = y;
   }

   public double getZ() {
      return this.z;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public Set<String> getLinks() {
      return this.links;
   }

   public boolean addLink(String targetId) {
      return this.links.add(targetId);
   }

   public boolean removeLink(String targetId) {
      return this.links.remove(targetId);
   }

   public boolean isLinked(String targetId) {
      return this.links.contains(targetId);
   }
}
