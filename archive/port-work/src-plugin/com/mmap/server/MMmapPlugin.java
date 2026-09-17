package com.mmap.server;

import com.mmap.server.command.MMmapCommand;
import com.mmap.server.image.MapImageManager;
import com.mmap.server.mythic.MythicMobHook;
import com.mmap.server.mythic.MythicMobTracker;
import com.mmap.server.network.PacketSender;
import com.mmap.server.player.PlayerTracker;
import com.mmap.server.player.PolygonRegionTracker;
import com.mmap.server.region.PolygonRegionManager;
import com.mmap.server.region.RegionManager;
import com.mmap.server.util.LanguageManager;
import com.mmap.server.warp.WarpPointManager;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class MMmapPlugin extends JavaPlugin {
   private static MMmapPlugin instance;
   private final Logger logger = this.getLogger();
   private RegionManager regionManager;
   private MapImageManager mapImageManager;
   private PacketSender packetSender;
   private PlayerTracker playerTracker;
   private PolygonRegionManager polygonRegionManager;
   private PolygonRegionTracker polygonRegionTracker;
   private WarpPointManager warpPointManager;
   private LanguageManager languageManager;
   private MythicMobHook mythicMobHook;
   private MythicMobTracker mythicMobTracker;

   public MMmapPlugin() {
   }

   public void onEnable() {
      instance = this;
      this.logger.info("MMmap Plugin \u6b63\u5728\u542f\u7528...");
      this.saveDefaultConfig();
      this.regionManager = new RegionManager(this);
      this.mapImageManager = new MapImageManager(this);
      this.packetSender = new PacketSender(this);
      this.playerTracker = new PlayerTracker(this);
      this.polygonRegionManager = new PolygonRegionManager(this);
      this.polygonRegionTracker = new PolygonRegionTracker(this);
      this.warpPointManager = new WarpPointManager(this);
      this.languageManager = new LanguageManager(this);
      this.regionManager.loadRegions();
      this.mapImageManager.loadImages();
      this.polygonRegionManager.load();
      this.warpPointManager.load();
      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "mmap:main");
      this.getCommand("mmmap").setExecutor(new MMmapCommand(this));
      this.getServer().getPluginManager().registerEvents(this.playerTracker, this);
      this.getServer().getPluginManager().registerEvents(this.polygonRegionTracker, this);
      this.polygonRegionTracker.start();
      this.mythicMobHook = new MythicMobHook();
      this.mythicMobTracker = new MythicMobTracker(this, this.mythicMobHook);
      this.mythicMobTracker.start();
      this.logger.info("  MythicMobs \u96c6\u6210: " + (this.mythicMobHook.isEnabled() ? "\u5df2\u542f\u7528" : "\u672a\u5b89\u88c5/\u8df3\u8fc7"));
      this.logger.info("MMmap Plugin \u5df2\u6210\u529f\u542f\u7528\uff01");
      this.logger.info("  \u533a\u57df\u6570\u91cf: " + this.regionManager.getRegionCount());
      this.logger.info("  \u5730\u56fe\u56fe\u7247\u7ed1\u5b9a\u6570\u91cf: " + this.mapImageManager.getImageCount());
      this.logger.info("  \u591a\u8fb9\u5f62\u533a\u57df\u6570\u91cf: " + this.polygonRegionManager.getAll().size());
      this.logger.info("  \u4f20\u9001\u70b9\u6570\u91cf: " + this.warpPointManager.size());
   }

   public void onDisable() {
      this.logger.info("MMmap Plugin \u6b63\u5728\u7981\u7528...");
      if (this.regionManager != null) {
         this.regionManager.saveRegions();
      }

      if (this.mapImageManager != null) {
         this.mapImageManager.saveImages();
      }

      if (this.polygonRegionManager != null) {
         this.polygonRegionManager.stop();
      }

      this.logger.info("MMmap Plugin \u5df2\u7981\u7528");
   }

   public static MMmapPlugin getInstance() {
      return instance;
   }

   public RegionManager getRegionManager() {
      return this.regionManager;
   }

   public MapImageManager getMapImageManager() {
      return this.mapImageManager;
   }

   public PacketSender getPacketSender() {
      return this.packetSender;
   }

   public PlayerTracker getPlayerTracker() {
      return this.playerTracker;
   }

   public PolygonRegionManager getPolygonRegionManager() {
      return this.polygonRegionManager;
   }

   public WarpPointManager getWarpPointManager() {
      return this.warpPointManager;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public MythicMobHook getMythicMobHook() {
      return this.mythicMobHook;
   }
}
