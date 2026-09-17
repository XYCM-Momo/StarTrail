package com.mmap.server.player;

import com.mmap.server.MMmapPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerTracker implements Listener {
   private final MMmapPlugin plugin;
   private final Map<UUID, PlayerTracker.PlayerData> playerDataMap;

   public PlayerTracker(MMmapPlugin plugin) {
      this.plugin = plugin;
      this.playerDataMap = new ConcurrentHashMap<>();
   }

   @EventHandler(
      priority = EventPriority.NORMAL
   )
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         this.plugin.getPacketSender().sendHandshake(player);
         this.plugin.getPacketSender().sendRegionSync(player);
         this.plugin.getPacketSender().sendMapImageSync(player);
         this.plugin.getPacketSender().sendPolygonRegions(player);
         this.plugin.getPacketSender().sendWarps(player);
      }, 20L);
      this.playerDataMap.put(player.getUniqueId(), new PlayerTracker.PlayerData(player));
      this.plugin.getLogger().info("\u73a9\u5bb6 " + player.getName() + " \u5df2\u8fde\u63a5\uff0c\u51c6\u5907\u540c\u6b65\u533a\u57df\u6570\u636e");
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerMove(PlayerMoveEvent event) {
      if (!event.getFrom().getChunk().equals(event.getTo().getChunk())) {
         Player player = event.getPlayer();
         PlayerTracker.PlayerData data = this.playerDataMap.get(player.getUniqueId());
         if (data != null) {
            data.updateLocation(event.getTo());
         }
      }
   }

   @EventHandler(
      priority = EventPriority.NORMAL
   )
   public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
      Player player = event.getPlayer();
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         this.plugin.getPacketSender().sendWorldChange(player);
         this.plugin.getPacketSender().sendRegionSync(player);
         this.plugin.getPacketSender().sendMapImageSync(player);
         this.plugin.getPacketSender().sendPolygonRegions(player);
         this.plugin.getPacketSender().sendWarps(player);
      }, 10L);
   }

   @EventHandler(
      priority = EventPriority.NORMAL
   )
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.playerDataMap.remove(event.getPlayer().getUniqueId());
   }

   public PlayerTracker.PlayerData getPlayerData(UUID playerId) {
      return this.playerDataMap.get(playerId);
   }

   public static class PlayerData {
      private final Player player;
      private int lastX;
      private int lastY;
      private int lastZ;
      private String lastWorld;

      public PlayerData(Player player) {
         this.player = player;
         this.lastX = player.getLocation().getBlockX();
         this.lastY = player.getLocation().getBlockY();
         this.lastZ = player.getLocation().getBlockZ();
         this.lastWorld = player.getWorld().getName();
      }

      public void updateLocation(Location location) {
         this.lastX = location.getBlockX();
         this.lastY = location.getBlockY();
         this.lastZ = location.getBlockZ();
         this.lastWorld = location.getWorld().getName();
      }

      public Player getPlayer() {
         return this.player;
      }

      public int getLastX() {
         return this.lastX;
      }

      public int getLastY() {
         return this.lastY;
      }

      public int getLastZ() {
         return this.lastZ;
      }

      public String getLastWorld() {
         return this.lastWorld;
      }
   }
}
