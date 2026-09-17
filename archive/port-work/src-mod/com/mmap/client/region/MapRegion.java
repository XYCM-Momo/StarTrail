package com.mmap.client.region;

public class MapRegion {
   private final String id;
   private String name;
   private String world;
   private int minX;
   private int minZ;
   private int maxX;
   private int maxZ;
   private int minY;
   private int maxY;
   private RegionType type;
   private int borderColor;
   private int fillColor;
   private float borderWidth;
   private String description;

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

   public boolean contains(int x, int y, int z) {
      return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY;
   }

   public boolean contains2D(int x, int z) {
      return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
   }

   public boolean intersects(int x1, int z1, int x2, int z2) {
      return this.maxX >= x1 && this.minX <= x2 && this.maxZ >= z1 && this.minZ <= z2;
   }

   public int getWidth() {
      return this.maxX - this.minX + 1;
   }

   public int getDepth() {
      return this.maxZ - this.minZ + 1;
   }

   public int getCenterX() {
      return (this.minX + this.maxX) / 2;
   }

   public int getCenterZ() {
      return (this.minZ + this.maxZ) / 2;
   }

   public long getArea() {
      return (long)this.getWidth() * (long)this.getDepth();
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

   public int getMinX() {
      return this.minX;
   }

   public int getMinZ() {
      return this.minZ;
   }

   public int getMaxX() {
      return this.maxX;
   }

   public int getMaxZ() {
      return this.maxZ;
   }

   public int getMinY() {
      return this.minY;
   }

   public int getMaxY() {
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

   public void setBounds(int minX, int minZ, int maxX, int maxZ) {
      this.minX = Math.min(minX, maxX);
      this.maxX = Math.max(minX, maxX);
      this.minZ = Math.min(minZ, maxZ);
      this.maxZ = Math.max(minZ, maxZ);
   }

   public void setYBounds(int minY, int maxY) {
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
