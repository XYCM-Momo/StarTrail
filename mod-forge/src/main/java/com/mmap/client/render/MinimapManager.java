package com.mmap.client.render;

import com.mmap.client.image.MapImageManager;
import com.mmap.client.region.PolygonRegionStore;
import com.mmap.client.region.RegionDataStore;
import com.mmap.client.warp.WarpIconManager;
import com.mmap.client.warp.WarpPointStore;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;

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
      MinecraftForge.EVENT_BUS.addListener(this::onRenderGui);
   }

   /** Replaces Fabric's HudRenderCallback with Forge's RenderGuiEvent.Post. */
   private void onRenderGui(RenderGuiEvent.Post event) {
      if (this.enabled) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && client.level != null) {
            if (client.screen == null) {
               this.renderer.render(event.getGuiGraphics(), client, event.getPartialTick());
            }
         }
      }
   }

   public void toggleMinimap() {
      this.enabled = !this.enabled;
   }

   public PolygonRegionStore getPolygonRegionStore() {
      return this.polygonRegionStore;
   }
}
