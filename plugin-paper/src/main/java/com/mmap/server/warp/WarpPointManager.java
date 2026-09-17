package com.mmap.server.warp;

import com.mmap.server.util.YamlFolderLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class WarpPointManager {
   private final JavaPlugin plugin;
   private final Logger logger;
   private final YamlFolderLoader loader;
   private final Map<String, WarpPoint> warpsById = new HashMap<>();

   public WarpPointManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.logger = plugin.getLogger();
      this.loader = new YamlFolderLoader(plugin, "warps", "warps");
   }

   public void load() {
      this.warpsById.clear();
      this.loader.loadAll((file, sec, qualifiedId) -> {
         String name = sec.getString("name", qualifiedId);
         String worldName = sec.getString("world", "world");
         int x = sec.getInt("x", 0);
         int y = sec.getInt("y", 64);
         int z = sec.getInt("z", 0);
         String iconKey = sec.getString("iconKey", "\u4f20\u9001\u70b9");
         String descr = sec.getString("descr", "");
         WarpPoint w = new WarpPoint(qualifiedId, name, worldName, x, y, z, iconKey, descr);
         this.warpsById.put(qualifiedId, w);
      });
      this.logger.info("\u4f20\u9001\u70b9\u52a0\u8f7d\u5b8c\u6210: " + this.warpsById.size() + " \u4e2a");
   }

   public void save() {
   }

   public WarpPoint get(String id) {
      return this.warpsById.get(id);
   }

   public Collection<WarpPoint> getAll() {
      return this.warpsById.values();
   }

   public int size() {
      return this.warpsById.size();
   }

   public List<WarpPoint> getInWorld(String world) {
      List<WarpPoint> result = new ArrayList<>();

      for (WarpPoint w : this.warpsById.values()) {
         if (w.getWorld().equals(world)) {
            result.add(w);
         }
      }

      return result;
   }
}
