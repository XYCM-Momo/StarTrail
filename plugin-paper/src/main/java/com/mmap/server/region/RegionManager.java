package com.mmap.server.region;

import com.mmap.server.MMmapPlugin;
import com.mmap.server.util.YamlFolderLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RegionManager {
   private final MMmapPlugin plugin;
   private final Map<String, MapRegion> regions;
   private final YamlFolderLoader loader;

   public RegionManager(MMmapPlugin plugin) {
      this.plugin = plugin;
      this.regions = new ConcurrentHashMap<>();
      this.loader = new YamlFolderLoader(plugin, "regions", "regions");
   }

   public void loadRegions() {
      this.regions.clear();
      this.loader
         .loadAll(
            (file, sec, qualifiedId) -> {
               String shape = sec.getString("shape", "rectangle");
               if (!"polygon".equalsIgnoreCase(shape) && !sec.contains("polygon")) {
                  try {
                     Map<String, Object> data = sec.getValues(true);
                     data.put("id", qualifiedId);
                     MapRegion region = new MapRegion(data);
                     this.regions.put(qualifiedId, region);
                  } catch (Exception var7) {
                     this.plugin
                        .getLogger()
                        .log(Level.WARNING, "\u52a0\u8f7d\u533a\u57df [" + qualifiedId + "] \u6765\u81ea " + file.getName() + " \u5931\u8d25", (Throwable)var7);
                  }
               }
            }
         );
      this.plugin.getLogger().info("\u6210\u529f\u52a0\u8f7d " + this.regions.size() + " \u4e2a\u77e9\u5f62\u533a\u57df");
   }

   public void saveRegions() {
   }

   public boolean addRegion(MapRegion region) {
      if (this.regions.containsKey(region.getId())) {
         return false;
      } else {
         this.regions.put(region.getId(), region);
         return true;
      }
   }

   public boolean removeRegion(String id) {
      return this.regions.remove(id) != null;
   }

   public MapRegion getRegion(String id) {
      return this.regions.get(id);
   }

   public Collection<MapRegion> getAllRegions() {
      return Collections.unmodifiableCollection(this.regions.values());
   }

   public List<MapRegion> getRegionsInWorld(String world) {
      return this.regions.values().stream().filter(r -> r.getWorld().equals(world)).collect(Collectors.toList());
   }

   public List<MapRegion> getRegionsAt(String world, int x, int y, int z) {
      return this.regions.values().stream().filter(r -> r.getWorld().equals(world) && r.contains(x, y, z)).collect(Collectors.toList());
   }

   public List<MapRegion> getRegionsAt2D(String world, int x, int z) {
      return this.regions.values().stream().filter(r -> r.getWorld().equals(world) && r.contains2D(x, z)).collect(Collectors.toList());
   }

   public List<MapRegion> getRegionsIntersecting(String world, int x1, int z1, int x2, int z2) {
      return this.regions.values().stream().filter(r -> r.getWorld().equals(world) && r.intersects(x1, z1, x2, z2)).collect(Collectors.toList());
   }

   public int getRegionCount() {
      return this.regions.size();
   }

   public boolean regionExists(String id) {
      return this.regions.containsKey(id);
   }

   public String generateRegionId(String baseName) {
      String id = baseName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
      if (!this.regions.containsKey(id)) {
         return id;
      } else {
         int counter = 1;

         while (this.regions.containsKey(id + "_" + counter)) {
            counter++;
         }

         return id + "_" + counter;
      }
   }
}
