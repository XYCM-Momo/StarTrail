package com.mmap.server.image;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class MapImage implements ConfigurationSerializable {
   private String id;
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

   public MapImage(Map<String, Object> map) {
      this.id = (String)map.get("id");
      this.fileName = (String)map.get("fileName");
      this.world = (String)map.get("world");
      this.minX = (Integer)map.get("minX");
      this.minZ = (Integer)map.get("minZ");
      this.maxX = (Integer)map.get("maxX");
      this.maxZ = (Integer)map.get("maxZ");
   }

   public Map<String, Object> serialize() {
      Map<String, Object> map = new HashMap<>();
      map.put("id", this.id);
      map.put("fileName", this.fileName);
      map.put("world", this.world);
      map.put("minX", this.minX);
      map.put("minZ", this.minZ);
      map.put("maxX", this.maxX);
      map.put("maxZ", this.maxZ);
      return map;
   }

   public boolean intersects(int x1, int z1, int x2, int z2) {
      return this.maxX >= x1 && this.minX <= x2 && this.maxZ >= z1 && this.minZ <= z2;
   }

   public int getWorldWidth() {
      return this.maxX - this.minX;
   }

   public int getWorldDepth() {
      return this.maxZ - this.minZ;
   }

   public long getArea() {
      return (long)this.getWorldWidth() * (long)this.getWorldDepth();
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
}
