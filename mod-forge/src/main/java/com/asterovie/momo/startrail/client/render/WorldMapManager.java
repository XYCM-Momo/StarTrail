package com.asterovie.momo.startrail.client.render;

import com.asterovie.momo.startrail.client.image.MapImageManager;
import com.asterovie.momo.startrail.client.region.PolygonRegionStore;
import com.asterovie.momo.startrail.client.region.RegionDataStore;
import com.asterovie.momo.startrail.client.warp.WarpIconManager;
import com.asterovie.momo.startrail.client.warp.WarpPointStore;
import net.minecraft.client.Minecraft;

public class WorldMapManager {
   private final RegionDataStore regionDataStore;
   private final PolygonRegionStore polygonRegionStore;
   private final MapImageManager mapImageManager;
   private final WarpPointStore warpPointStore;
   private final WarpIconManager warpIconManager;
   private boolean isOpen = false;

   public WorldMapManager(
      RegionDataStore regionDataStore,
      PolygonRegionStore polygonRegionStore,
      MapImageManager mapImageManager,
      WarpPointStore warpPointStore,
      WarpIconManager warpIconManager
   ) {
      this.regionDataStore = regionDataStore;
      this.polygonRegionStore = polygonRegionStore;
      this.mapImageManager = mapImageManager;
      this.warpPointStore = warpPointStore;
      this.warpIconManager = warpIconManager;
   }

   public void openWorldMapScreen() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.level != null) {
         WorldMapScreen screen = new WorldMapScreen(
            this.regionDataStore, this.polygonRegionStore, this.mapImageManager, this.warpPointStore, this.warpIconManager
         );
         client.setScreen(screen);
         this.isOpen = true;
      }
   }

   public boolean isOpen() {
      return this.isOpen;
   }
}
