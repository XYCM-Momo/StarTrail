package com.asterovie.momo.startrail.client.region;

/**
 * 矩形区域。
 *
 * <p><b>坐标是 double</b>：区域配置允许写小数（如 {@code minX: -123.5}）。
 * 早先这里用 int，小数在传输与解析过程中会被截断，导致地图上画出来的
 * 边框偏移到整数格——与多边形顶点是同一类问题，已一并修正。
 */
public class MapRegion {
   private final String id;
   private String name;
   private String world;
   private double minX;
   private double minZ;
   private double maxX;
   private double maxZ;
   private double minY;
   private double maxY;
   private RegionType type;
   private int borderColor;
   private int fillColor;
   private float borderWidth;
   private String description;

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

   /** 兼容旧调用：整数坐标版本。 */
   public MapRegion(
      String id,
      String name,
      String world,
      int minX,
      int minZ,
      int maxX,
      int maxZ,
      int minY,
      int maxY,
      RegionType type,
      int borderColor,
      int fillColor,
      float borderWidth,
      String description
   ) {
      this(id, name, world, (double)minX, (double)minZ, (double)maxX, (double)maxZ,
         (double)minY, (double)maxY, type, borderColor, fillColor, borderWidth, description);
   }

   public boolean contains(double x, double y, double z) {
      return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY;
   }

   public boolean contains2D(double x, double z) {
      return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
   }

   public boolean intersects(double x1, double z1, double x2, double z2) {
      return this.maxX >= x1 && this.minX <= x2 && this.maxZ >= z1 && this.minZ <= z2;
   }

   public double getWidth() {
      return this.maxX - this.minX;
   }

   public double getDepth() {
      return this.maxZ - this.minZ;
   }

   public double getCenterX() {
      return (this.minX + this.maxX) / 2.0;
   }

   public double getCenterZ() {
      return (this.minZ + this.maxZ) / 2.0;
   }

   public long getArea() {
      return (long)Math.round(this.getWidth() * this.getDepth());
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

   public void setName(String name) {
      this.name = name;
   }

   public void setWorld(String world) {
      this.world = world;
   }

   public void setBounds(double minX, double minZ, double maxX, double maxZ) {
      this.minX = Math.min(minX, maxX);
      this.maxX = Math.max(minX, maxX);
      this.minZ = Math.min(minZ, maxZ);
      this.maxZ = Math.max(minZ, maxZ);
   }

   public void setYBounds(double minY, double maxY) {
      this.minY = minY;
      this.maxY = maxY;
   }

   public void setType(RegionType type) {
      this.type = type;
   }

   public void setBorderColor(int borderColor) {
      this.borderColor = borderColor;
   }

   public void setFillColor(int fillColor) {
      this.fillColor = fillColor;
   }

   public void setBorderWidth(float borderWidth) {
      this.borderWidth = borderWidth;
   }

   public void setDescription(String description) {
      this.description = description;
   }
}
