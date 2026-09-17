package com.mmap.server.config;

import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class EnterCommandConfig {
   private EnterCommandConfig() {
   }

   public static void executeOnEnter(Player player, String regionId, String regionName, String world, List<String> commands) {
      if (commands != null && !commands.isEmpty() && player != null) {
         String playerName = player.getName();
         String uuid = player.getUniqueId().toString();

         for (String template : commands) {
            if (template != null) {
               String cmd = template.replace("{player}", playerName)
                  .replace("{uuid}", uuid)
                  .replace("{region}", regionId)
                  .replace("{name}", regionName)
                  .replace("{world}", world);
               if (cmd.startsWith("/")) {
                  cmd = cmd.substring(1);
               }

               boolean asPlayer;
               String actual;
               if (cmd.startsWith("<")) {
                  asPlayer = true;
                  actual = cmd.substring(1).trim();
               } else {
                  asPlayer = false;
                  actual = cmd;
               }

               if (!actual.isEmpty()) {
                  if (actual.startsWith("tellraw ")) {
                     actual = autoJsonTellraw(actual);
                  }

                  try {
                     boolean ok;
                     if (asPlayer) {
                        ok = player.performCommand(actual);
                     } else {
                        CommandSender console = Bukkit.getConsoleSender();
                        ok = Bukkit.dispatchCommand(console, actual);
                     }

                     if (!ok) {
                        Bukkit.getLogger().warning("[MMmap] \u6267\u884c\u8fdb\u5165\u533a\u57df\u547d\u4ee4\u8fd4\u56de false: " + actual);
                     }
                  } catch (Exception var14) {
                     Bukkit.getLogger().log(Level.WARNING, "[MMmap] \u6267\u884c\u8fdb\u5165\u533a\u57df\u547d\u4ee4\u5f02\u5e38: " + actual, (Throwable)var14);
                  }
               }
            }
         }
      }
   }

   private static String autoJsonTellraw(String cmd) {
      int spaceIdx = cmd.indexOf(32, "tellraw ".length());
      if (spaceIdx < 0) {
         return cmd;
      } else {
         String target = cmd.substring("tellraw ".length(), spaceIdx);
         String msg = cmd.substring(spaceIdx + 1).trim();
         if (msg.isEmpty()) {
            return cmd;
         } else if (msg.startsWith("{")) {
            return cmd;
         } else {
            String json = "{\"text\":\"" + escapeJson(msg) + "\"}";
            return "tellraw " + target + " " + json;
         }
      }
   }

   private static String escapeJson(String s) {
      return s.replace("\\", "\\\\").replace("\"", "\\\"");
   }
}
