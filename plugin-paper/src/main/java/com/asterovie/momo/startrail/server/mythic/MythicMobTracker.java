package com.asterovie.momo.startrail.server.mythic;

import com.asterovie.momo.startrail.server.StarTrailPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MythicMobTracker {
   private final StarTrailPlugin plugin;
   private final MythicMobHook hook;
   private final double radius;

   public MythicMobTracker(StarTrailPlugin plugin, MythicMobHook hook) {
      this.plugin = plugin;
      this.hook = hook;
      this.radius = plugin.getConfig().getDouble("settings.mythic-mob-track-radius", 128.0);
   }

   public void start() {
      if (!this.hook.isEnabled()) {
         this.plugin.getLogger().info("MythicMobs \u672a\u5b89\u88c5\u6216\u672a\u542f\u7528\uff0c\u8df3\u8fc7\u602a\u7269\u5934\u50cf\u540c\u6b65");
      } else {
         this.plugin.getLogger().info("MythicMobs \u602a\u7269\u5934\u50cf\u540c\u6b65\u5df2\u542f\u52a8 (\u534a\u5f84 " + this.radius + ")");
         (new BukkitRunnable() {
            public void run() {
               for (Player player : MythicMobTracker.this.plugin.getServer().getOnlinePlayers()) {
                  MythicMobTracker.this.syncForPlayer(player);
               }
            }
         }).runTaskTimer(this.plugin, 40L, 40L);
      }
   }

   private void syncForPlayer(Player player) {
      try {
         List<MythicMobTracker.MobEntry> entries = new ArrayList<>();
         String world = player.getWorld().getName();

         for (Entity e : player.getNearbyEntities(this.radius, this.radius, this.radius)) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
               String type = this.hook.getMobType(e);
               if (type != null) {
                  entries.add(
                     new MythicMobTracker.MobEntry(e.getUniqueId().toString(), type, e.getLocation().getX(), e.getLocation().getY(), e.getLocation().getZ())
                  );
               }
            }
         }

         this.plugin.getPacketSender().sendMythicMobs(player, entries, world);
      } catch (Throwable var7) {
      }
   }

   public static class MobEntry {
      public final String uuid;
      public final String type;
      public final double x;
      public final double y;
      public final double z;

      public MobEntry(String uuid, String type, double x, double y, double z) {
         this.uuid = uuid;
         this.type = type;
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
