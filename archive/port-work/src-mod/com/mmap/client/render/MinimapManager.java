package com.mmap.client.render;

import com.mmap.client.image.MapImageManager;
import com.mmap.client.region.PolygonRegionStore;
import com.mmap.client.region.RegionDataStore;
import com.mmap.client.warp.WarpIconManager;
import com.mmap.client.warp.WarpPointStore;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.class_310;

public class MinimapManager {
   private final RegionDataStore regionDataStore;
   private final PolygonRegionStore polygonRegionStore;
   private final MapImageManager mapImageManager;
   private final WarpPointStore warpPointStore;
   private final WarpIconManager warpIconManager;
   private final MinimapRenderer renderer;
   private boolean enabled = true;

   public MinimapManager(
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
      this.renderer = new MinimapRenderer(regionDataStore, polygonRegionStore, mapImageManager, warpPointStore, warpIconManager);
   }

   public void init() {
      HudRenderCallback.EVENT.register((HudRenderCallback)(drawContext, tickDelta) -> {
         if (this.enabled) {
            class_310 client = class_310.method_1551();
            if (client.field_1724 != null && client.field_1687 != null) {
               if (client.field_1755 == null) {
                  this.renderer.render(drawContext, client, tickDelta);
               }
            }
         }
      });
   }

   public void toggleMinimap() {
      this.enabled = !this.enabled;
   }

   public PolygonRegionStore getPolygonRegionStore() {
      return this.polygonRegionStore;
   }
}
