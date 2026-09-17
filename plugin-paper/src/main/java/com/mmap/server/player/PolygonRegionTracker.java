package com.mmap.server.player;

import com.mmap.server.MMmapPlugin;
import com.mmap.server.config.EnterCommandConfig;
import com.mmap.server.region.PolygonRegion;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

public class PolygonRegionTracker implements Listener {
   private final MMmapPlugin plugin;
   private final Map<UUID, Set<String>> insideByPlayer = new HashMap<>();
   private final Map<UUID, Location> lastCheckLoc = new HashMap<>();
   private BukkitTask periodicTask;

   public PolygonRegionTracker(MMmapPlugin plugin) {
      this.plugin = plugin;
   }

   public void start() {
      this.periodicTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
         for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            Location loc = p.getLocation();
            if (p.getWorld() != null) {
               this.recalc(p, loc);
            }
         }
      }, 20L, 20L);
   }

   public void stop() {
      if (this.periodicTask != null) {
         this.periodicTask.cancel();
         this.periodicTask = null;
      }

      this.insideByPlayer.clear();
      this.lastCheckLoc.clear();
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player p = event.getPlayer();
      this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.recalc(p, p.getLocation()));
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      UUID id = event.getPlayer().getUniqueId();
      this.insideByPlayer.remove(id);
      this.lastCheckLoc.remove(id);
   }

   @EventHandler
   public void onTeleport(PlayerTeleportEvent event) {
      this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
         Player p = event.getPlayer();
         if (p.isOnline()) {
            this.recalc(p, p.getLocation());
         }
      });
   }

   @EventHandler
   public void onMove(PlayerMoveEvent event) {
      if (event.getFrom().getWorld() != null) {
         Location to = event.getTo();
         if (to != null && to.getWorld() != null) {
            if (event.getFrom().getWorld().equals(to.getWorld())) {
               double dx = to.getX() - event.getFrom().getX();
               double dz = to.getZ() - event.getFrom().getZ();
               if (dx * dx + dz * dz < 2.0) {
                  return;
               }
            }

            this.recalc(event.getPlayer(), to);
         }
      }
   }

   private void recalc(Player player, Location loc) {
      if (loc.getWorld() != null) {
         UUID id = player.getUniqueId();
         String world = loc.getWorld().getName();
         int x = loc.getBlockX();
         int z = loc.getBlockZ();
         Set<String> current = new HashSet<>();

         for (PolygonRegion r : this.plugin.getPolygonRegionManager().getRegionsAt(world, x, z)) {
            current.add(r.getId());
         }

         Set<String> prev = this.insideByPlayer.getOrDefault(id, new HashSet<>());
         if (current.equals(prev)) {
            this.lastCheckLoc.put(id, loc);
         } else {
            for (String rid : current) {
               if (!prev.contains(rid)) {
                  PolygonRegion r = this.plugin.getPolygonRegionManager().get(rid);
                  if (r != null) {
                     EnterCommandConfig.executeOnEnter(player, r.getId(), r.getName(), world, r.getEnterCommands());
                  }
               }
            }

            this.insideByPlayer.put(id, current);
            this.lastCheckLoc.put(id, loc);
         }
      }
   }
}
