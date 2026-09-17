package com.mmap.server.region;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class MapRegion implements ConfigurationSerializable {
   private String id;
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

   public MapRegion(String id, String name, String world, int minX, int minZ, int maxX, int maxZ) {
      this.id = id;
      this.name = name;
      this.world = world;
      this.minX = Math.min(minX, maxX);
      this.maxX = Math.max(minX, maxX);
      this.minZ = Math.min(minZ, maxZ);
      this.maxZ = Math.max(minZ, maxZ);
      this.minY = 0;
      this.maxY = 256;
      this.type = RegionType.NORMAL;
      this.borderColor = -16711936;
      this.fillColor = 855703296;
      this.borderWidth = 2.0F;
      this.description = "";
   }

   public MapRegion(Map<String, Object> map) {
      this.id = (String)map.get("id");
      this.name = (String)map.get("name");
      this.world = (String)map.get("world");
      this.minX = readInt(map, "minX");
      this.minZ = readInt(map, "minZ");
      this.maxX = readInt(map, "maxX");
      this.maxZ = readInt(map, "maxZ");
      this.minY = readIntOr(map, "minY", 0);
      this.maxY = readIntOr(map, "maxY", 256);
      this.type = RegionType.valueOf((String)map.getOrDefault("type", "NORMAL"));
      this.borderColor = readIntOr(map, "borderColor", -16711936);
      this.fillColor = readIntOr(map, "fillColor", 855703296);
      Object bw = map.get("borderWidth");
      this.borderWidth = bw instanceof Number ? ((Number)bw).floatValue() : 2.0F;
      this.description = (String)map.getOrDefault("description", "");
   }

   private static int readInt(Map<String, Object> map, String key) {
      Object v = map.get(key);
      if (v instanceof Number) {
         return ((Number)v).intValue();
      } else {
         throw new IllegalArgumentException("\u7f3a\u5c11\u5fc5\u9700\u5b57\u6bb5\u6216\u7c7b\u578b\u9519\u8bef: " + key);
      }
   }

   private static int readIntOr(Map<String, Object> map, String key, int def) {
      Object v = map.get(key);
      return v instanceof Number ? ((Number)v).intValue() : def;
   }

   public Map<String, Object> serialize() {
      Map<String, Object> map = new HashMap<>();
      map.put("id", this.id);
      map.put("name", this.name);
      map.put("world", this.world);
      map.put("minX", this.minX);
      map.put("minZ", this.minZ);
      map.put("maxX", this.maxX);
      map.put("maxZ", this.maxZ);
      map.put("minY", this.minY);
      map.put("maxY", this.maxY);
      map.put("type", this.type.name());
      map.put("borderColor", this.borderColor);
      map.put("fillColor", this.fillColor);
      map.put("borderWidth", this.borderWidth);
      map.put("description", this.description);
      return map;
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

   public long getArea() {
      return (long)(this.maxX - this.minX + 1) * (long)(this.maxZ - this.minZ + 1);
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
      switch (type) {
         case SAFE:
            this.borderColor = -13408513;
            this.fillColor = 859006719;
            break;
         case DANGER:
            this.borderColor = -52429;
            this.fillColor = 872362803;
         case CUSTOM:
            break;
         default:
            this.borderColor = -16711936;
            this.fillColor = 855703296;
      }
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
