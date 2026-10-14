package com.asterovie.momo.startrail.server;

import com.asterovie.momo.startrail.server.command.StarTrailCommand;
import com.asterovie.momo.startrail.server.image.MapImageManager;
import com.asterovie.momo.startrail.server.mythic.MythicMobHook;
import com.asterovie.momo.startrail.server.mythic.MythicMobTracker;
import com.asterovie.momo.startrail.server.network.PacketSender;
import com.asterovie.momo.startrail.server.player.PlayerTracker;
import com.asterovie.momo.startrail.server.player.PolygonRegionTracker;
import com.asterovie.momo.startrail.server.region.PolygonRegionManager;
import com.asterovie.momo.startrail.server.util.LanguageManager;
import com.asterovie.momo.startrail.server.warp.WarpPointManager;
import com.asterovie.momo.startrail.server.waypoint.WaypointManager;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class StarTrailPlugin extends JavaPlugin {
   private static StarTrailPlugin instance;
   private final Logger logger = this.getLogger();
   private MapImageManager mapImageManager;
   private PacketSender packetSender;
   private PlayerTracker playerTracker;
   private PolygonRegionManager polygonRegionManager;
   private PolygonRegionTracker polygonRegionTracker;
   private WarpPointManager warpPointManager;
   private WaypointManager waypointManager;
   private LanguageManager languageManager;
   private MythicMobHook mythicMobHook;
   private MythicMobTracker mythicMobTracker;

   public StarTrailPlugin() {
   }

   public void onEnable() {
      instance = this;
      this.logger.info("==================================================");
      this.logger.info(" StarTrail \u00b7 \u661f\u8f68   v" + this.getDescription().getVersion());
      this.logger.info(" \u672c\u63d2\u4ef6\u7531 \u661f\u91ce\u5c18\u68a6\u5de5\u4f5c\u5ba4 \u00b7 \u9ed8\u9ed8 \u5236\u4f5c");
      this.logger.info("==================================================");
      this.logger.info("StarTrail Plugin \u6b63\u5728\u542f\u7528...");
      this.saveDefaultConfig();
      this.mapImageManager = new MapImageManager(this);
      this.packetSender = new PacketSender(this);
      this.playerTracker = new PlayerTracker(this);
      this.polygonRegionManager = new PolygonRegionManager(this);
      this.polygonRegionTracker = new PolygonRegionTracker(this);
      this.warpPointManager = new WarpPointManager(this);
      this.waypointManager = new WaypointManager(this);
      this.languageManager = new LanguageManager(this);
      this.mapImageManager.loadImages();
      this.polygonRegionManager.load();
      this.warpPointManager.load();
      this.waypointManager.loadAll();
      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "startrail:main");
      this.getCommand("startrail").setExecutor(new StarTrailCommand(this));
      this.getServer().getPluginManager().registerEvents(this.playerTracker, this);
      this.getServer().getPluginManager().registerEvents(this.polygonRegionTracker, this);
      this.polygonRegionTracker.start();
      this.mythicMobHook = new MythicMobHook();
      this.mythicMobTracker = new MythicMobTracker(this, this.mythicMobHook);
      this.mythicMobTracker.start();
      this.logger.info("  MythicMobs \u96c6\u6210: " + (this.mythicMobHook.isEnabled() ? "\u5df2\u542f\u7528" : "\u672a\u5b89\u88c5/\u8df3\u8fc7"));
      this.logger.info("StarTrail Plugin \u5df2\u6210\u529f\u542f\u7528\uff01");
      this.logger.info("  \u5730\u56fe\u56fe\u7247\u7ed1\u5b9a\u6570\u91cf: " + this.mapImageManager.getImageCount());
      this.logger.info("  \u591a\u8fb9\u5f62\u533a\u57df\u6570\u91cf: " + this.polygonRegionManager.getAll().size());
      this.logger.info("  \u4f20\u9001\u70b9\u6570\u91cf: " + this.warpPointManager.size());
      this.logger.info("  \u5bfc\u822a\u70b9\u4f4d\u6570: " + this.waypointManager.totalCount() + " (\u5907\u4efd\u4e8e waypoints/ \u76ee\u5f55)");
   }

   public void onDisable() {
      this.logger.info("StarTrail Plugin \u6b63\u5728\u7981\u7528...");
      if (this.mapImageManager != null) {
         this.mapImageManager.saveImages();
      }

      if (this.polygonRegionManager != null) {
         this.polygonRegionManager.stop();
      }

      this.logger.info("StarTrail Plugin \u5df2\u7981\u7528");
   }

   public static StarTrailPlugin getInstance() {
      return instance;
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

   public WaypointManager getWaypointManager() {
      return this.waypointManager;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public MythicMobHook getMythicMobHook() {
      return this.mythicMobHook;
   }
}
