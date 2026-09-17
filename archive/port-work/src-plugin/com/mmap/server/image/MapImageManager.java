package com.mmap.server.image;

import com.mmap.server.MMmapPlugin;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class MapImageManager {
   private final MMmapPlugin plugin;
   private final Map<String, MapImage> images;
   private final File imagesFile;

   public MapImageManager(MMmapPlugin plugin) {
      this.plugin = plugin;
      this.images = new ConcurrentHashMap<>();
      this.imagesFile = new File(plugin.getDataFolder(), "images.yml");
   }

   public void loadImages() {
      this.images.clear();
      if (!this.imagesFile.exists()) {
         this.plugin.getLogger().info("\u672a\u627e\u5230 images.yml\uff0c\u5c06\u521b\u5efa\u9ed8\u8ba4\u6587\u4ef6");
         this.saveImages();
      } else {
         try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(this.imagesFile);
            if (!config.contains("images")) {
               this.plugin.getLogger().info("images.yml \u4e2d\u6ca1\u6709\u56fe\u7247\u7ed1\u5b9a\u6570\u636e");
               return;
            }

            ConfigurationSection imagesSection = config.getConfigurationSection("images");
            if (imagesSection == null) {
               this.plugin.getLogger().info("images.yml \u4e2d images \u8282\u70b9\u4e3a\u7a7a");
               return;
            }

            for (String id : imagesSection.getKeys(false)) {
               try {
                  ConfigurationSection imgSection = imagesSection.getConfigurationSection(id);
                  if (imgSection == null) {
                     this.plugin.getLogger().warning("\u8df3\u8fc7\u56fe\u7247\u7ed1\u5b9a " + id + "\uff1a\u683c\u5f0f\u65e0\u6548");
                  } else {
                     Map<String, Object> data = imgSection.getValues(true);
                     data.put("id", id);
                     MapImage image = new MapImage(data);
                     this.images.put(id, image);
                  }
               } catch (Exception var9) {
                  this.plugin.getLogger().log(Level.WARNING, "\u52a0\u8f7d\u56fe\u7247\u7ed1\u5b9a " + id + " \u5931\u8d25", (Throwable)var9);
               }
            }

            this.plugin.getLogger().info("\u6210\u529f\u52a0\u8f7d " + this.images.size() + " \u4e2a\u5730\u56fe\u56fe\u7247\u7ed1\u5b9a");
         } catch (Exception var10) {
            this.plugin.getLogger().log(Level.SEVERE, "\u52a0\u8f7d\u56fe\u7247\u7ed1\u5b9a\u6570\u636e\u5931\u8d25", (Throwable)var10);
         }
      }
   }

   public void saveImages() {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.imagesFile))) {
         writer.write("# ==================== MMmap \u5730\u56fe\u56fe\u7247\u7ed1\u5b9a\u914d\u7f6e ====================");
         writer.newLine();
         writer.write("#");
         writer.newLine();
         writer.write(
            "# \u5728\u6b64\u6587\u4ef6\u4e2d\u6dfb\u52a0/\u7f16\u8f91\u56fe\u7247\u7ed1\u5b9a\uff0c\u7136\u540e\u6267\u884c /mmap reload \u91cd\u65b0\u52a0\u8f7d"
         );
         writer.newLine();
         writer.write("#");
         writer.newLine();
         writer.write("# \u5b57\u6bb5\u8bf4\u660e\uff1a");
         writer.newLine();
         writer.write("#   fileName: \u5ba2\u6237\u7aef .minecraft/config/mmap/images/ \u76ee\u5f55\u4e0b\u7684\u56fe\u7247\u6587\u4ef6\u540d");
         writer.newLine();
         writer.write("#   world:    \u6240\u5c5e\u4e16\u754c\u540d\uff08\u5982 world, world_nether, world_the_end\uff09");
         writer.newLine();
         writer.write("#   minX, minZ: \u56fe\u7247\u5de6\u4e0a\u89d2\uff08\u897f\u5317\u89d2\uff09\u5bf9\u5e94\u7684\u6e38\u620f\u65b9\u5757\u5750\u6807");
         writer.newLine();
         writer.write("#   maxX, maxZ: \u56fe\u7247\u53f3\u4e0b\u89d2\uff08\u4e1c\u5357\u89d2\uff09\u5bf9\u5e94\u7684\u6e38\u620f\u65b9\u5757\u5750\u6807");
         writer.newLine();
         writer.write("#");
         writer.newLine();
         writer.write("# \u5750\u6807\u65b9\u5411\uff1a");
         writer.newLine();
         writer.write("#   - X \u589e\u5927 = \u4e1c\uff0cZ \u589e\u5927 = \u5357");
         writer.newLine();
         writer.write("#   - \u56fe\u7247\u4e0a\u8fb9 = \u5317\u65b9\uff08Z \u5c0f\uff09\uff0c\u56fe\u7247\u5de6\u8fb9 = \u897f\u65b9\uff08X \u5c0f\uff09");
         writer.newLine();
         writer.write(
            "#   - \u56fe\u7247\u50cf\u7d20\u5bbd\u9ad8\u6bd4\u5e94\u4e0e\u8986\u76d6\u65b9\u5757\u6570\u5bbd\u9ad8\u6bd4\u4e00\u81f4\uff0c\u5426\u5219\u4f1a\u62c9\u4f38"
         );
         writer.newLine();
         writer.write("#");
         writer.newLine();
         writer.write("# \u793a\u4f8b\uff1a");
         writer.newLine();
         writer.write("#   main_map:");
         writer.newLine();
         writer.write("#     fileName: overworld.png");
         writer.newLine();
         writer.write("#     world: world");
         writer.newLine();
         writer.write("#     minX: -1000");
         writer.newLine();
         writer.write("#     minZ: -1000");
         writer.newLine();
         writer.write("#     maxX: 1000");
         writer.newLine();
         writer.write("#     maxZ: 1000");
         writer.newLine();
         writer.write("#");
         writer.newLine();
         writer.write("# ============================================================");
         writer.newLine();
         writer.newLine();
         writer.write("images:");
         writer.newLine();
         if (this.images.isEmpty()) {
            writer.write("  # \u5728\u6b64\u6dfb\u52a0\u56fe\u7247\u7ed1\u5b9a\uff0c\u683c\u5f0f\u5982\u4e0b\uff1a");
            writer.newLine();
            writer.write("  # main_map:");
            writer.newLine();
            writer.write("  #   fileName: overworld.png");
            writer.newLine();
            writer.write("  #   world: world");
            writer.newLine();
            writer.write("  #   minX: -1000");
            writer.newLine();
            writer.write("  #   minZ: -1000");
            writer.newLine();
            writer.write("  #   maxX: 1000");
            writer.newLine();
            writer.write("  #   maxZ: 1000");
            writer.newLine();
         } else {
            for (MapImage image : this.images.values()) {
               writer.write("  " + image.getId() + ":");
               writer.newLine();
               writer.write("    fileName: " + image.getFileName());
               writer.newLine();
               writer.write("    world: " + image.getWorld());
               writer.newLine();
               writer.write("    minX: " + image.getMinX());
               writer.newLine();
               writer.write("    minZ: " + image.getMinZ());
               writer.newLine();
               writer.write("    maxX: " + image.getMaxX());
               writer.newLine();
               writer.write("    maxZ: " + image.getMaxZ());
               writer.newLine();
            }
         }

         this.plugin.getLogger().info("\u6210\u529f\u4fdd\u5b58 " + this.images.size() + " \u4e2a\u5730\u56fe\u56fe\u7247\u7ed1\u5b9a\u5230 images.yml");
      } catch (IOException var6) {
         this.plugin.getLogger().log(Level.SEVERE, "\u4fdd\u5b58\u56fe\u7247\u7ed1\u5b9a\u6570\u636e\u5931\u8d25", (Throwable)var6);
      }
   }

   public boolean addImage(MapImage image) {
      if (this.images.containsKey(image.getId())) {
         return false;
      } else {
         this.images.put(image.getId(), image);
         this.saveImages();
         return true;
      }
   }

   public boolean removeImage(String id) {
      if (this.images.remove(id) != null) {
         this.saveImages();
         return true;
      } else {
         return false;
      }
   }

   public MapImage getImage(String id) {
      return this.images.get(id);
   }

   public Collection<MapImage> getAllImages() {
      return Collections.unmodifiableCollection(this.images.values());
   }

   public List<MapImage> getImagesInWorld(String world) {
      return this.images.values().stream().filter(i -> i.getWorld().equals(world)).collect(Collectors.toList());
   }

   public int getImageCount() {
      return this.images.size();
   }

   public boolean imageExists(String id) {
      return this.images.containsKey(id);
   }

   public String generateImageId(String baseName) {
      String id = baseName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
      if (!this.images.containsKey(id)) {
         return id;
      } else {
         int counter = 1;

         while (this.images.containsKey(id + "_" + counter)) {
            counter++;
         }

         return id + "_" + counter;
      }
   }
}
