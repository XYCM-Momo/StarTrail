package com.asterovie.momo.startrail.server.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class YamlFolderLoader {
   private final JavaPlugin plugin;
   private final Logger logger;
   private final File folder;
   private final String resourceBase;

   public YamlFolderLoader(JavaPlugin plugin, String folderName, String resourceBase) {
      this.plugin = plugin;
      this.logger = plugin.getLogger();
      this.folder = new File(plugin.getDataFolder(), folderName);
      this.resourceBase = resourceBase;
   }

   public void ensureFolder() {
      boolean firstCreated = false;
      if (!this.folder.exists()) {
         firstCreated = this.folder.mkdirs();
         if (firstCreated) {
            this.logger.info("\u5df2\u521b\u5efa\u914d\u7f6e\u6587\u4ef6\u5939: " + this.folder.getAbsolutePath());
         }
      }

      if (this.resourceBase != null && firstCreated) {
         File defaultFile = new File(this.folder, "default.yml");
         if (!defaultFile.exists()) {
            try (InputStream in = this.plugin.getResource(this.resourceBase + ".yml")) {
               if (in != null) {
                  Files.copy(in, defaultFile.toPath());
                  this.logger.info("\u5df2\u590d\u5236\u9ed8\u8ba4\u6a21\u677f\u5230: " + defaultFile.getAbsolutePath());
               }
            } catch (IOException var8) {
               this.logger.warning("\u65e0\u6cd5\u590d\u5236\u9ed8\u8ba4\u6a21\u677f " + this.resourceBase + ".yml: " + var8.getMessage());
            }
         }
      }
   }

   public void loadAll(YamlFolderLoader.EntryVisitor entryVisitor) {
      this.ensureFolder();
      File[] files = this.folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
      if (files != null && files.length != 0) {
         for (File f : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            if (this.resourceBase != null) {
               try (InputStream in = this.plugin.getResource(this.resourceBase + ".yml")) {
                  if (in != null) {
                     FileConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                     cfg.setDefaults(def);
                  }
               } catch (IOException var20) {
               }
            }

            String baseName = stripExt(f.getName());
            boolean consumedAny = false;

            for (String topKey : cfg.getKeys(false)) {
               ConfigurationSection sec = cfg.getConfigurationSection(topKey);
               if (sec != null) {
                  boolean sectionHasEntries = false;

                  for (String innerKey : sec.getKeys(false)) {
                     ConfigurationSection inner = sec.getConfigurationSection(innerKey);
                     if (inner != null) {
                        String qualifiedId = baseName + "." + innerKey;
                        entryVisitor.visit(f, inner, qualifiedId);
                        sectionHasEntries = true;
                        consumedAny = true;
                     }
                  }

                  if (!sectionHasEntries) {
                     consumedAny = true;
                  }
               }
            }

            if (!consumedAny && !cfg.getKeys(false).isEmpty()) {
               entryVisitor.visit(f, cfg, baseName);
            }
         }
      } else {
         this.logger.info(this.folder.getName() + "/ \u6587\u4ef6\u5939\u4e3a\u7a7a,\u6ca1\u6709 .yml \u914d\u7f6e");
      }
   }

   public List<String> listFiles() {
      this.ensureFolder();
      File[] files = this.folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
      List<String> names = new ArrayList<>();
      if (files != null) {
         for (File f : files) {
            names.add(f.getName());
         }
      }

      return names;
   }

   public File getFolder() {
      return this.folder;
   }

   private static String stripExt(String name) {
      int dot = name.lastIndexOf(46);
      return dot > 0 ? name.substring(0, dot) : name;
   }

   public static class Entry {
      public final File file;
      public final ConfigurationSection section;
      public final String id;

      public Entry(File file, ConfigurationSection section, String id) {
         this.file = file;
         this.section = section;
         this.id = id;
      }
   }

   public interface EntryVisitor {
      void visit(File var1, ConfigurationSection var2, String var3);
   }
}
