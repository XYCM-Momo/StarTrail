package com.asterovie.momo.startrail.server.npc;

import com.asterovie.momo.startrail.server.StarTrailPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 定时把玩家附近的 Adyeshach NPC 标识下发给客户端。
 *
 * <p>客户端只靠这份列表区分「NPC」和「真人玩家」，从而让「显示 NPC 头像」开关
 * 只影响 NPC、不影响真人。与 MythicMobs 的追踪器同一节奏（每 2 秒）。
 *
 * <p>NPC 变化不频繁，间隔可以比怪物长；这里用 40 tick（2 秒）与原实现保持一致，
 * 避免玩家走到 NPC 附近时地图迟迟不显示。
 */
public class NpcTracker {
   private final StarTrailPlugin plugin;
   private final AdyeshachHook hook;
   private final double radius;

   public NpcTracker(StarTrailPlugin plugin, AdyeshachHook hook) {
      this.plugin = plugin;
      this.hook = hook;
      this.radius = plugin.getConfig().getDouble("settings.npc-track-radius", 128.0);
   }

   public void start() {
      if (!this.hook.isAvailable()) {
         this.plugin.getLogger().info("Adyeshach 未安装或未启用，跳过 NPC 同步");
         return;
      }

      this.plugin.getLogger().info("Adyeshach NPC 同步已启动 (半径 " + this.radius + ")");
      (new BukkitRunnable() {
         public void run() {
            for (Player player : NpcTracker.this.plugin.getServer().getOnlinePlayers()) {
               NpcTracker.this.syncForPlayer(player);
            }
         }
      }).runTaskTimer(this.plugin, 40L, 40L);
   }

   private void syncForPlayer(Player player) {
      try {
         List<AdyeshachHook.NpcEntry> npcs =
            this.hook.getNpcsNear(player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getZ(), this.radius);
         List<String> ids = new ArrayList<>(npcs.size());
         for (AdyeshachHook.NpcEntry npc : npcs) {
            ids.add(npc.uuid);
         }

         this.plugin.getPacketSender().sendNpcList(player, ids);
      } catch (Throwable ignored) {
         // 单个玩家同步失败不影响其他玩家
      }
   }
}
