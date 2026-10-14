package com.asterovie.momo.startrail.client.warp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarpPointStore {
   private final Map<String, List<WarpPoint>> byWorld = new HashMap<>();

   public WarpPointStore() {
   }

   public void replaceAll(List<WarpPoint> all) {
      this.byWorld.clear();

      for (WarpPoint w : all) {
         this.byWorld.computeIfAbsent(w.getWorld(), k -> new ArrayList<>()).add(w);
      }
   }

   public void clear() {
      this.byWorld.clear();
   }

   public List<WarpPoint> getForWorld(String world) {
      if (world == null) {
         return List.of();
      } else {
         List<WarpPoint> direct = this.byWorld.get(world);
         if (direct != null && !direct.isEmpty()) {
            return direct;
         } else {
            int colon = world.indexOf(58);
            if (colon >= 0) {
               String nsShort = world.substring(colon + 1);
               List<WarpPoint> byShort = this.byWorld.get(nsShort);
               if (byShort != null && !byShort.isEmpty()) {
                  return byShort;
               }

               String bukkitName = bukkitShortFromDimension(nsShort);
               if (bukkitName != null) {
                  List<WarpPoint> byBukkit = this.byWorld.get(bukkitName);
                  if (byBukkit != null && !byBukkit.isEmpty()) {
                     return byBukkit;
                  }
               }
            } else {
               String ns = dimensionNamespaceFromBukkitShort(world);
               if (ns != null) {
                  List<WarpPoint> byNs = this.byWorld.get(ns);
                  if (byNs != null && !byNs.isEmpty()) {
                     return byNs;
                  }
               }
            }

            return List.of();
         }
      }
   }

   private static String dimensionNamespaceFromBukkitShort(String shortName) {
      String s = shortName.toLowerCase();
      if (s.equals("world") || s.equals("overworld")) {
         return "minecraft:overworld";
      } else if (s.endsWith("_nether") || s.equals("nether")) {
         return "minecraft:the_nether";
      } else if (s.endsWith("_the_end") || s.equals("the_end") || s.equals("end")) {
         return "minecraft:the_end";
      } else {
         return shortName.contains(":") ? shortName : null;
      }
   }

   private static String bukkitShortFromDimension(String dimShort) {
      String s = dimShort.toLowerCase();
      if (s.equals("overworld")) {
         return "world";
      } else if (s.equals("the_nether")) {
         return "world_nether";
      } else {
         return s.equals("the_end") ? "world_the_end" : null;
      }
   }

   public Collection<WarpPoint> getAll() {
      List<WarpPoint> all = new ArrayList<>();

      for (List<WarpPoint> list : this.byWorld.values()) {
         all.addAll(list);
      }

      return all;
   }

   public int size() {
      int n = 0;

      for (List<WarpPoint> list : this.byWorld.values()) {
         n += list.size();
      }

      return n;
   }
}
