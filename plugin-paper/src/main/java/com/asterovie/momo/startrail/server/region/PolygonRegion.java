package com.asterovie.momo.startrail.server.region;

import java.util.ArrayList;
import java.util.List;

public class PolygonRegion {
   private final String id;
   private final String name;
   private final String world;
   private final List<double[]> vertices;
   private final int lineColor;
   private final int fillColor;
   private final boolean showLine;
   private final boolean fillEnabled;
   private final double minX;
   private final double maxX;
   private final double minZ;
   private final double maxZ;
   private final List<String> enterCommands;

   public PolygonRegion(String id, String name, String world, List<double[]> vertices, int lineColor, int fillColor, boolean showLine, boolean fillEnabled) {
      this(id, name, world, vertices, lineColor, fillColor, showLine, fillEnabled, null);
   }

   public PolygonRegion(
      String id,
      String name,
      String world,
      List<double[]> vertices,
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
      double minx = Double.MAX_VALUE;
      double maxx = -Double.MAX_VALUE;
      double minz = Double.MAX_VALUE;
      double maxz = -Double.MAX_VALUE;

      for (double[] v : vertices) {
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

   public List<double[]> getVertices() {
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

   public double getMinX() {
      return this.minX;
   }

   public double getMaxX() {
      return this.maxX;
   }

   public double getMinZ() {
      return this.minZ;
   }

   public double getMaxZ() {
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
         double xi = this.vertices.get(i)[0];
         double zi = this.vertices.get(i)[1];
         double xj = this.vertices.get(j)[0];
         double zj = this.vertices.get(j)[1];
         sum += xj * zi - xi * zj;
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
               double xi = this.vertices.get(i)[0];
               double zi = this.vertices.get(i)[1];
               double xj = this.vertices.get(j)[0];
               double zj = this.vertices.get(j)[1];
               if (zi > z != zj > z && x < (xj - xi) * (z - zi) / (zj - zi) + xi) {
                  inside = !inside;
               }
            }

            return inside;
         }
      }
   }

   public double getCenterX() {
      double sum = 0.0;

      for (double[] v : this.vertices) {
         sum += (double)v[0];
      }

      return sum / (double)this.vertices.size();
   }

   public double getCenterZ() {
      double sum = 0.0;

      for (double[] v : this.vertices) {
         sum += (double)v[1];
      }

      return sum / (double)this.vertices.size();
   }
}
