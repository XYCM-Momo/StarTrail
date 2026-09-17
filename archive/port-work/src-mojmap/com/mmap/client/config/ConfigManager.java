package com.mmap.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mmap.client.MMmapMod;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigManager {
   private static ConfigManager instance;
   private final File configFile;
   private final Gson gson;
   private ConfigManager.ClientConfig config;
   private String currentWorld = null;

   public ConfigManager() {
      instance = this;
      this.configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "mmap.json");
      this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
      this.loadConfig();
      this.ensureAllFieldsSaved();
   }

   public void ensureAllFieldsSaved() {
      if (this.config == null) {
         this.config = new ConfigManager.ClientConfig();
      }

      boolean changed = false;
      ConfigManager.ClientConfig fresh = new ConfigManager.ClientConfig();
      if (this.config.minimap == null) {
         this.config.minimap = fresh.minimap;
         changed = true;
      }

      if (this.config.worldmap == null) {
         this.config.worldmap = fresh.worldmap;
         changed = true;
      }

      if (this.config.general == null) {
         this.config.general = fresh.general;
         changed = true;
      }

      if (this.config.warpCmd == null) {
         this.config.warpCmd = fresh.warpCmd;
         changed = true;
      }

      if (this.config.worldmap.lastZoom <= 0.0) {
         this.config.worldmap.lastZoom = fresh.worldmap.defaultZoom > 0.0 ? fresh.worldmap.defaultZoom : 2.0;
         changed = true;
      }

      if (changed) {
         this.saveConfig();
      }
   }

   public void loadConfig() {
      if (this.configFile.exists()) {
         try (FileReader reader = new FileReader(this.configFile)) {
            this.config = (ConfigManager.ClientConfig)this.gson.fromJson(reader, ConfigManager.ClientConfig.class);
            MMmapMod.LOGGER.info("\u914d\u7f6e\u5df2\u52a0\u8f7d");
         } catch (IOException var6) {
            MMmapMod.LOGGER.error("\u52a0\u8f7d\u914d\u7f6e\u5931\u8d25", var6);
            this.config = new ConfigManager.ClientConfig();
         }
      } else {
         this.config = new ConfigManager.ClientConfig();
         this.saveConfig();
      }
   }

   public void saveConfig() {
      try {
         if (!this.configFile.getParentFile().exists()) {
            this.configFile.getParentFile().mkdirs();
         }

         try (FileWriter writer = new FileWriter(this.configFile)) {
            this.gson.toJson(this.config, writer);
            MMmapMod.LOGGER.info("\u914d\u7f6e\u5df2\u4fdd\u5b58");
         }
      } catch (IOException var6) {
         MMmapMod.LOGGER.error("\u4fdd\u5b58\u914d\u7f6e\u5931\u8d25", var6);
      }
   }

   public static ConfigManager getInstance() {
      return instance;
   }

   public String getCurrentWorld() {
      return this.currentWorld;
   }

   public void setCurrentWorld(String world) {
      this.currentWorld = world;
   }

   public ConfigManager.ClientConfig getConfig() {
      return this.config;
   }

   public static class ClientConfig {
      public ConfigManager.MinimapConfig minimap = new ConfigManager.MinimapConfig();
      public ConfigManager.WorldMapConfig worldmap = new ConfigManager.WorldMapConfig();
      public ConfigManager.GeneralConfig general = new ConfigManager.GeneralConfig();
      public ConfigManager.WarpCmdConfig warpCmd = new ConfigManager.WarpCmdConfig();

      public ClientConfig() {
      }
   }

   public static class GeneralConfig {
      public boolean debugMode = false;
      public int syncDistance = 1024;
      public boolean showMobHeadFallback = true;

      public GeneralConfig() {
      }
   }

   public static class MinimapConfig {
      public boolean enabled = true;
      public int size = 128;
      public double zoom = 1.0;
      public int position = 0;
      public boolean showPlayers = true;
      public boolean showRegions = true;
      public boolean showRegionNames = true;
      public boolean showPolygons = true;
      public boolean showWarps = true;
      public boolean showCoordinates = true;
      public float regionBorderWidth = 2.0F;
      public int chunkUpdateInterval = 5;
      public boolean caveMode = false;

      public MinimapConfig() {
      }
   }

   public static class WarpCmdConfig {
      public String navigate = "/mmap warp nav {id}";
      public String teleport = "/mmap warp tp {id}";

      public WarpCmdConfig() {
      }
   }

   public static class WorldMapConfig {
      public double defaultZoom = 2.0;
      public double minZoom = 0.1;
      public double maxZoom = 4.0;
      public double lastZoom = 2.0;
      public boolean showPlayers = true;
      public boolean showRegions = true;
      public boolean showRegionNames = true;
      public boolean showPolygons = true;
      public boolean showWarps = true;
      public boolean smoothZoom = true;
      public boolean smoothPan = true;
      public int lodUpdateInterval = 10;
      public double warpIconHideThreshold = 0.8;

      public WorldMapConfig() {
      }
   }
}
