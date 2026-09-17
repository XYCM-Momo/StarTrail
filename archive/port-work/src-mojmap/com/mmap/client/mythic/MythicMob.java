package com.mmap.client.mythic;

public class MythicMob {
   private final String uuid;
   private final String type;
   private final String world;
   private final double x;
   private final double y;
   private final double z;

   public MythicMob(String uuid, String type, String world, double x, double y, double z) {
      this.uuid = uuid;
      this.type = type;
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public String getUuid() {
      return this.uuid;
   }

   public String getType() {
      return this.type;
   }

   public String getWorld() {
      return this.world;
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

   @Override
   public String toString() {
      return "MythicMob{type=" + this.type + ", pos=(" + this.x + "," + this.y + "," + this.z + "), world=" + this.world + "}";
   }
}
