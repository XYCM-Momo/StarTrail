package com.asterovie.momo.startrail.client.image;

public class MapImage {
   private final String id;
   private String fileName;
   private String world;
   private int minX;
   private int minZ;
   private int maxX;
   private int maxZ;

   public MapImage(String id, String fileName, String world, int x1, int z1, int x2, int z2) {
      this.id = id;
      this.fileName = fileName;
      this.world = world;
      this.minX = Math.min(x1, x2);
      this.maxX = Math.max(x1, x2);
      this.minZ = Math.min(z1, z2);
      this.maxZ = Math.max(z1, z2);
   }

   public boolean contains(double worldX, double worldZ) {
      return worldX >= (double)this.minX && worldX <= (double)this.maxX && worldZ >= (double)this.minZ && worldZ <= (double)this.maxZ;
   }

   public boolean intersects(int x1, int z1, int x2, int z2) {
      return this.maxX >= x1 && this.minX <= x2 && this.maxZ >= z1 && this.minZ <= z2;
   }

   public float worldXToU(double worldX) {
      double width = (double)(this.maxX - this.minX);
      return width == 0.0 ? 0.5F : (float)((worldX - (double)this.minX) / width);
   }

   public float worldZToV(double worldZ) {
      double depth = (double)(this.maxZ - this.minZ);
      return depth == 0.0 ? 0.5F : (float)((worldZ - (double)this.minZ) / depth);
   }

   public int getWorldWidth() {
      return this.maxX - this.minX;
   }

   public int getWorldDepth() {
      return this.maxZ - this.minZ;
   }

   public int getCenterX() {
      return (this.minX + this.maxX) / 2;
   }

   public int getCenterZ() {
      return (this.minZ + this.maxZ) / 2;
   }

   public String getId() {
      return this.id;
   }

   public String getFileName() {
      return this.fileName;
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

   public void setFileName(String fileName) {
      this.fileName = fileName;
   }

   public void setWorld(String world) {
      this.world = world;
   }

   public void setBounds(int x1, int z1, int x2, int z2) {
      this.minX = Math.min(x1, x2);
      this.maxX = Math.max(x1, x2);
      this.minZ = Math.min(z1, z2);
      this.maxZ = Math.max(z1, z2);
   }

   @Override
   public String toString() {
      return String.format(
         "MapImage{id=%s, file=%s, world=%s, bounds=(%d,%d)-(%d,%d)}", this.id, this.fileName, this.world, this.minX, this.minZ, this.maxX, this.maxZ
      );
   }
}
