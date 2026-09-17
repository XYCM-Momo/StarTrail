package com.mmap.client;

import com.mmap.client.config.ConfigManager;
import com.mmap.client.image.MapImageManager;
import com.mmap.client.mythic.MobHeadManager;
import com.mmap.client.mythic.MythicMobStore;
import com.mmap.client.network.NetworkHandler;
import com.mmap.client.region.PolygonRegionStore;
import com.mmap.client.region.RegionDataStore;
import com.mmap.client.render.MinimapManager;
import com.mmap.client.render.WorldMapManager;
import com.mmap.client.warp.WarpIconManager;
import com.mmap.client.warp.WarpPointStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.minecraft.class_304;
import net.minecraft.class_3675.class_307;

public class MMmapClient implements ClientModInitializer {
   private static MMmapClient instance;
   private RegionDataStore regionDataStore;
   private PolygonRegionStore polygonRegionStore;
   private WarpPointStore warpPointStore;
   private WarpIconManager warpIconManager;
   private ConfigManager configManager;
   private NetworkHandler networkHandler;
   private MinimapManager minimapManager;
   private WorldMapManager worldMapManager;
   private MapImageManager mapImageManager;
   private MythicMobStore mythicMobStore;
   private MobHeadManager mobHeadManager;
   private class_304 openWorldMapKey;
   private class_304 toggleMinimapKey;

   public MMmapClient() {
   }

   public void onInitializeClient() {
      instance = this;
      MMmapMod.LOGGER.info("MMmap \u5ba2\u6237\u7aef\u6b63\u5728\u521d\u59cb\u5316...");
      this.configManager = new ConfigManager();
      this.regionDataStore = new RegionDataStore();
      this.polygonRegionStore = new PolygonRegionStore();
      this.warpPointStore = new WarpPointStore();
      this.warpIconManager = new WarpIconManager();
      this.mapImageManager = new MapImageManager();
      this.mythicMobStore = new MythicMobStore();
      this.mobHeadManager = new MobHeadManager();
      this.networkHandler = new NetworkHandler(this.regionDataStore, this.polygonRegionStore, this.warpPointStore);
      this.minimapManager = new MinimapManager(this.regionDataStore, this.polygonRegionStore, this.mapImageManager, this.warpPointStore, this.warpIconManager);
      this.worldMapManager = new WorldMapManager(this.regionDataStore, this.polygonRegionStore, this.mapImageManager, this.warpPointStore, this.warpIconManager);
      this.networkHandler.register();
      this.minimapManager.init();
      this.registerKeyBindings();
      this.registerClientEvents();
      MMmapMod.LOGGER.info("MMmap \u5ba2\u6237\u7aef\u521d\u59cb\u5316\u5b8c\u6210");
      MMmapMod.LOGGER.info("\u5730\u56fe\u56fe\u7247\u76ee\u5f55: {}", this.mapImageManager.getImagesDir().getAbsolutePath());
   }

   private void registerKeyBindings() {
      this.openWorldMapKey = KeyBindingHelper.registerKeyBinding(new class_304("key.mmap.openworldmap", class_307.field_1668, 77, "category.mmap"));
      this.toggleMinimapKey = KeyBindingHelper.registerKeyBinding(new class_304("key.mmap.toggleminimap", class_307.field_1668, -1, "category.mmap"));
   }

   private void registerClientEvents() {
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         while (this.openWorldMapKey.method_1436()) {
            this.worldMapManager.openWorldMapScreen();
         }

         while (this.toggleMinimapKey.method_1436()) {
            this.minimapManager.toggleMinimap();
         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         this.regionDataStore.clear();
         this.polygonRegionStore.clear();
         this.warpPointStore.clear();
         this.mapImageManager.clear();
         this.warpIconManager.clear();
         this.mythicMobStore.clear();
         this.mobHeadManager.clear();
         MMmapMod.LOGGER.info("\u5df2\u65ad\u5f00\u670d\u52a1\u5668\u8fde\u63a5\uff0c\u6e05\u7a7a\u5730\u56fe\u6570\u636e");
      });
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public static MMmapClient getInstance() {
      return instance;
   }

   public RegionDataStore getRegionDataStore() {
      return this.regionDataStore;
   }

   public PolygonRegionStore getPolygonRegionStore() {
      return this.polygonRegionStore;
   }

   public NetworkHandler getNetworkHandler() {
      return this.networkHandler;
   }

   public MinimapManager getMinimapManager() {
      return this.minimapManager;
   }

   public WorldMapManager getWorldMapManager() {
      return this.worldMapManager;
   }

   public MapImageManager getMapImageManager() {
      return this.mapImageManager;
   }

   public WarpPointStore getWarpPointStore() {
      return this.warpPointStore;
   }

   public WarpIconManager getWarpIconManager() {
      return this.warpIconManager;
   }

   public MythicMobStore getMythicMobStore() {
      return this.mythicMobStore;
   }

   public MobHeadManager getMobHeadManager() {
      return this.mobHeadManager;
   }
}
