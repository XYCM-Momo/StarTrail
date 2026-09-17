package com.mmap.server.util;

import com.mmap.server.MMmapPlugin;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LanguageManager {
   private final MMmapPlugin plugin;
   private FileConfiguration messages;

   public LanguageManager(MMmapPlugin plugin) {
      this.plugin = plugin;
      this.load();
   }

   public void load() {
      File file = new File(this.plugin.getDataFolder(), "language.yml");
      if (!file.exists()) {
         this.plugin.saveResource("language.yml", false);
      }

      this.messages = YamlConfiguration.loadConfiguration(file);

      try (InputStreamReader reader = new InputStreamReader(this.plugin.getResource("language.yml"), StandardCharsets.UTF_8)) {
         if (reader != null) {
            FileConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            this.messages.setDefaults(defaults);
         }
      } catch (Exception var7) {
         this.plugin.getLogger().warning("\u52a0\u8f7d language.yml \u9ed8\u8ba4\u503c\u5931\u8d25: " + var7.getMessage());
      }
   }

   public String get(String key) {
      String msg = this.messages.getString(key);
      return msg == null ? ChatColor.RED + "Missing message: " + key : ChatColor.translateAlternateColorCodes('&', msg);
   }

   public String get(String key, Object... args) {
      String msg = this.messages.getString(key);
      if (msg == null) {
         return ChatColor.RED + "Missing message: " + key;
      } else {
         for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
         }

         return ChatColor.translateAlternateColorCodes('&', msg);
      }
   }
}
