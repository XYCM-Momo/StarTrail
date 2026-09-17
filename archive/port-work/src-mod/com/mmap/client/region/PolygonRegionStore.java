package com.mmap.client.region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolygonRegionStore {
   private final Map<String, List<PolygonRegion>> byWorld = new HashMap<>();

   public PolygonRegionStore() {
   }

   public void replaceAll(List<PolygonRegion> all) {
      this.byWorld.clear();

      for (PolygonRegion r : all) {
         this.byWorld.computeIfAbsent(r.getWorld(), k -> new ArrayList<>()).add(r);
      }
   }

   public void clear() {
      this.byWorld.clear();
   }

   public List<PolygonRegion> getForWorld(String world) {
      if (world == null) {
         return List.of();
      } else {
         List<PolygonRegion> direct = this.byWorld.get(world);
         if (direct != null && !direct.isEmpty()) {
            return direct;
         } else {
            int colon = world.indexOf(58);
            if (colon >= 0) {
               String shortName = world.substring(colon + 1);
               List<PolygonRegion> byShort = this.byWorld.get(shortName);
               if (byShort != null && !byShort.isEmpty()) {
                  return byShort;
               }
            } else {
               List<PolygonRegion> byNs = this.byWorld.get("minecraft:" + world);
               if (byNs != null && !byNs.isEmpty()) {
                  return byNs;
               }
            }

            return List.of();
         }
      }
   }

   public Collection<PolygonRegion> getAll() {
      List<PolygonRegion> all = new ArrayList<>();

      for (List<PolygonRegion> list : this.byWorld.values()) {
         all.addAll(list);
      }

      return all;
   }

   public int size() {
      int n = 0;

      for (List<PolygonRegion> list : this.byWorld.values()) {
         n += list.size();
      }

      return n;
   }
}
