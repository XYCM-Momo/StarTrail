package com.asterovie.momo.startrail.client.region;

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
   private final double centerX;
   private final double centerZ;

   public PolygonRegion(String id, String name, String world, List<int[]> vertices, int lineColor, int fillColor, boolean showLine, boolean fillEnabled) {
      this.id = id;
      this.name = name;
      this.world = world;
      this.vertices = vertices;
      this.lineColor = lineColor;
      this.fillColor = fillColor;
      this.showLine = showLine;
      this.fillEnabled = fillEnabled;
      if (vertices != null && !vertices.isEmpty()) {
         double sumX = 0.0;
         double sumZ = 0.0;

         for (int[] v : vertices) {
            sumX += (double)v[0];
            sumZ += (double)v[1];
         }

         this.centerX = sumX / (double)vertices.size();
         this.centerZ = sumZ / (double)vertices.size();
      } else {
         this.centerX = 0.0;
         this.centerZ = 0.0;
      }
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

   public double getCenterX() {
      return this.centerX;
   }

   public double getCenterZ() {
      return this.centerZ;
   }
}
