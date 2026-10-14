package com.asterovie.momo.startrail.client.region;

import com.asterovie.momo.startrail.client.StarTrailMod;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RegionDataStore {
   private final Map<String, MapRegion> regions = new ConcurrentHashMap<>();
   private final Map<String, List<MapRegion>> worldRegionsCache = new ConcurrentHashMap<>();

   public RegionDataStore() {
   }

   public void addOrUpdateRegion(MapRegion region) {
      this.regions.put(region.getId(), region);
      this.invalidateCache(region.getWorld());
      StarTrailMod.LOGGER.debug("\u533a\u57df\u5df2\u6dfb\u52a0/\u66f4\u65b0: {} ({})", region.getName(), region.getId());
   }

   public void removeRegion(String id) {
      MapRegion region = this.regions.remove(id);
      if (region != null) {
         this.invalidateCache(region.getWorld());
         StarTrailMod.LOGGER.debug("\u533a\u57df\u5df2\u79fb\u9664: {} ({})", region.getName(), id);
      }
   }

   public void clear() {
      this.regions.clear();
      this.worldRegionsCache.clear();
      StarTrailMod.LOGGER.info("\u533a\u57df\u6570\u636e\u5df2\u6e05\u7a7a");
   }

   public MapRegion getRegion(String id) {
      return this.regions.get(id);
   }

   public Collection<MapRegion> getAllRegions() {
      return Collections.unmodifiableCollection(this.regions.values());
   }

   public List<MapRegion> getRegionsInWorld(String world) {
      return this.worldRegionsCache
         .computeIfAbsent(world, w -> this.regions.values().stream().filter(r -> r.getWorld().equals(w)).collect(Collectors.toList()));
   }

   public List<MapRegion> getRegionsAt(String world, int x, int y, int z) {
      return this.getRegionsInWorld(world).stream().filter(r -> r.contains(x, y, z)).collect(Collectors.toList());
   }

   public List<MapRegion> getRegionsAt2D(String world, int x, int z) {
      return this.getRegionsInWorld(world).stream().filter(r -> r.contains2D(x, z)).collect(Collectors.toList());
   }

   public List<MapRegion> getRegionsIntersecting(String world, int x1, int z1, int x2, int z2) {
      return this.getRegionsInWorld(world).stream().filter(r -> r.intersects(x1, z1, x2, z2)).collect(Collectors.toList());
   }

   public int getRegionCount() {
      return this.regions.size();
   }

   private void invalidateCache(String world) {
      this.worldRegionsCache.remove(world);
   }
}
