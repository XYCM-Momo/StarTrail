package com.asterovie.momo.startrail.server.region;

import java.util.ArrayList;
import java.util.List;

public class PolygonRegion {
   private final String id;
   private final String name;
   private final String world;
   private final List<int[]> vertices;
   private final int lineColor;
   private final int fillColor;
   private final boolean showLine;
   private final boolean fillEnabled;
   private final int minX;
   private final int maxX;
   private final int minZ;
   private final int maxZ;
   private final List<String> enterCommands;

   public PolygonRegion(String id, String name, String world, List<int[]> vertices, int lineColor, int fillColor, boolean showLine, boolean fillEnabled) {
      this(id, name, world, vertices, lineColor, fillColor, showLine, fillEnabled, null);
   }

   public PolygonRegion(
      String id,
      String name,
      String world,
      List<int[]> vertices,
      int lineColor,
      int fillColor,
      boolean showLine,
      boolean fillEnabled,
      List<String> enterCommands
   ) {
      this.id = id;
      this.name = name;
      this.world = world;
      this.vertices = vertices;
      this.lineColor = lineColor;
      this.fillColor = fillColor;
      this.showLine = showLine;
      this.fillEnabled = fillEnabled;
      this.enterCommands = (List<String>)(enterCommands != null ? enterCommands : new ArrayList<>());
      int minx = Integer.MAX_VALUE;
      int maxx = Integer.MIN_VALUE;
      int minz = Integer.MAX_VALUE;
      int maxz = Integer.MIN_VALUE;

      for (int[] v : vertices) {
         if (v[0] < minx) {
            minx = v[0];
         }

         if (v[0] > maxx) {
            maxx = v[0];
         }

         if (v[1] < minz) {
            minz = v[1];
         }

         if (v[1] > maxz) {
            maxz = v[1];
         }
      }

      this.minX = minx;
      this.maxX = maxx;
      this.minZ = minz;
      this.maxZ = maxz;
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

   public List<int[]> getVertices() {
      return this.vertices;
   }

   public int getLineColor() {
      return this.lineColor;
   }

   public int getFillColor() {
      return this.fillColor;
   }

   public boolean isShowLine() {
      return this.showLine;
   }

   public boolean isFillEnabled() {
      return this.fillEnabled;
   }

   public int getMinX() {
      return this.minX;
   }

   public int getMaxX() {
      return this.maxX;
   }

   public int getMinZ() {
      return this.minZ;
   }

   public int getMaxZ() {
      return this.maxZ;
   }

   public List<String> getEnterCommands() {
      return this.enterCommands;
   }

   public boolean isInBoundingBox(int x, int z) {
      return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
   }

   /** 多边形面积（鞋带公式），单位方块，取整。 */
   public long getArea() {
      double sum = 0.0;
      int n = this.vertices.size();
      if (n < 3) {
         return 0L;
      }

      for (int i = 0, j = n - 1; i < n; j = i++) {
         int xi = this.vertices.get(i)[0];
         int zi = this.vertices.get(i)[1];
         int xj = this.vertices.get(j)[0];
         int zj = this.vertices.get(j)[1];
         sum += (double)xj * (double)zi - (double)xi * (double)zj;
      }

      return Math.abs(Math.round(sum / 2.0));
   }

   public boolean contains(int x, int z) {
      if (!this.isInBoundingBox(x, z)) {
         return false;
      } else {
         int n = this.vertices.size();
         if (n < 3) {
            return false;
         } else {
            boolean inside = false;
            int j = n - 1;

            for (int i = 0; i < n; j = i++) {
               int xi = this.vertices.get(i)[0];
               int zi = this.vertices.get(i)[1];
               int xj = this.vertices.get(j)[0];
               int zj = this.vertices.get(j)[1];
               if (zi > z != zj > z && (double)x < (double)((xj - xi) * (z - zi)) / (double)(zj - zi) + (double)xi) {
                  inside = !inside;
               }
            }

            return inside;
         }
      }
   }

   public double getCenterX() {
      double sum = 0.0;

      for (int[] v : this.vertices) {
         sum += (double)v[0];
      }

      return sum / (double)this.vertices.size();
   }

   public double getCenterZ() {
      double sum = 0.0;

      for (int[] v : this.vertices) {
         sum += (double)v[1];
      }

      return sum / (double)this.vertices.size();
   }
}
