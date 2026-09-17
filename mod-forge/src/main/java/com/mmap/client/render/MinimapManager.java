package com.mmap.client.render;

import com.mmap.client.config.ConfigManager;
import com.mmap.client.image.MapImageManager;
import com.mmap.client.region.PolygonRegionStore;
import com.mmap.client.region.RegionDataStore;
import com.mmap.client.warp.WarpIconManager;
import com.mmap.client.warp.WarpPointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

   /**
    * Replaces Fabric's HudRenderCallback with Forge's RenderGuiEvent.Post.
    *
    * <p>Forge fires this event from ForgeGui#render, which GameRenderer still calls while a screen
    * is open (that is why the hotbar stays visible behind the chat). The vanilla mod used to bail
    * out whenever any screen was open, which made the minimap vanish as soon as the chat bar was
    * opened. Now the minimap keeps rendering while a GUI is open, unless the player turns that off
    * with {@code minimap.hideInGui} in config/mmap.json.
    */
   private void onRenderGui(RenderGuiEvent.Post event) {
      if (!this.enabled) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) {
         return;
      }

      Screen screen = client.screen;
      if (screen != null) {
         // The mod's own world map already fills the screen; drawing the minimap behind it is pointless.
         if (screen instanceof WorldMapScreen) {
            return;
         }

         ConfigManager.ClientConfig config = ConfigManager.getInstance().getConfig();
         if (config != null && config.minimap != null && config.minimap.hideInGui) {
            return;
         }
      }

      this.renderer.render(event.getGuiGraphics(), client, event.getPartialTick());
   }

   public void toggleMinimap() {
      this.enabled = !this.enabled;
   }

   public PolygonRegionStore getPolygonRegionStore() {
      return this.polygonRegionStore;
   }
}
