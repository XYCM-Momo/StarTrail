package com.asterovie.momo.startrail.client.warp;

import net.minecraft.client.gui.Font;

public class WarpPoint {
   private final String id;
   private final String name;
   private final String world;
   private final int x;
   private final int y;
   private final int z;
   private final String iconKey;
   private final String descr;
   private transient Font cachedRenderer;
   private transient int cachedTextWidth = -1;

   public WarpPoint(String id, String name, String world, int x, int y, int z, String iconKey, String descr) {
      this.id = id;
      this.name = name;
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
      this.iconKey = iconKey != null && !iconKey.isEmpty() ? iconKey : "\u4f20\u9001\u70b9";
      this.descr = descr == null ? "" : descr;
   }

   public WarpPoint(String id, String name, String world, int x, int y, int z, String iconKey) {
      this(id, name, world, x, y, z, iconKey, "");
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

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public String getIconKey() {
      return this.iconKey;
   }

   public String getDescr() {
      return this.descr;
   }

   public int getCachedTextWidth(Font textRenderer) {
      if (textRenderer == null) {
         return 0;
      } else {
         if (this.cachedRenderer != textRenderer || this.cachedTextWidth < 0) {
            this.cachedTextWidth = textRenderer.width(this.name);
            this.cachedRenderer = textRenderer;
         }

         return this.cachedTextWidth;
      }
   }
}
