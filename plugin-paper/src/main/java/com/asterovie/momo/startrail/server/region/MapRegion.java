package com.asterovie.momo.startrail.server.region;

/**
 * 矩形区域（服务端）。
 *
 * <p>坐标全部用 <b>double</b>：配置里允许写小数（如 {@code minX: -123.5}）。
 * 早先客户端那套用 int，小数会被截断、边框偏移到整数格；
 * 这次矩形也统一成 double，与多边形顶点保持一致。
 */
public class MapRegion {
   private final String id;
   private final String name;
   private final String world;
   private final double minX;
   private final double minZ;
   private final double maxX;
   private final double maxZ;
   private final double minY;
   private final double maxY;
   private final RegionType type;
   private final int borderColor;
   private final int fillColor;
   private final float borderWidth;
   private final String description;

   public MapRegion(
      String id,
      String name,
      String world,
      double minX,
      double minZ,
      double maxX,
      double maxZ,
      double minY,
      double maxY,
      RegionType type,
      int borderColor,
      int fillColor,
      float borderWidth,
      String description
   ) {
      this.id = id;
      this.name = name;
      this.world = world;
      // 与客户端一致：始终保证 min <= max
      this.minX = Math.min(minX, maxX);
      this.maxX = Math.max(minX, maxX);
      this.minZ = Math.min(minZ, maxZ);
      this.maxZ = Math.max(minZ, maxZ);
      this.minY = minY;
      this.maxY = maxY;
      this.type = type;
      this.borderColor = borderColor;
      this.fillColor = fillColor;
      this.borderWidth = borderWidth;
      this.description = description;
   }

   public boolean contains2D(double x, double z) {
      return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public String getWorld() {
      return this.world;
   }

   public double getMinX() {
      return this.minX;
   }

   public double getMinZ() {
      return this.minZ;
   }

   public double getMaxX() {
      return this.maxX;
   }

   public double getMaxZ() {
      return this.maxZ;
   }

   public double getMinY() {
      return this.minY;
   }

   public double getMaxY() {
      return this.maxY;
   }

   public RegionType getType() {
      return this.type;
   }

   public int getBorderColor() {
      return this.borderColor;
   }

   public int getFillColor() {
      return this.fillColor;
   }

   public float getBorderWidth() {
      return this.borderWidth;
   }

   public String getDescription() {
      return this.description;
   }
}
