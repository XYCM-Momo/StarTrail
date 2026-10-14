package com.asterovie.momo.startrail.server.region;

import com.asterovie.momo.startrail.server.util.ColorUtil;
import com.asterovie.momo.startrail.server.util.YamlFolderLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class PolygonRegionManager {
   private final JavaPlugin plugin;
   private final Logger logger;
   private final YamlFolderLoader loader;
   private final Map<String, PolygonRegion> regionsById = new HashMap<>();

   public PolygonRegionManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.logger = plugin.getLogger();
      this.loader = new YamlFolderLoader(plugin, "regions", "regions");
   }

   public void load() {
      this.regionsById.clear();
      this.loader
         .loadAll(
            (file, sec, qualifiedId) -> {
               String shape = sec.getString("shape", "rectangle");
               boolean isPolygon = "polygon".equalsIgnoreCase(shape) || sec.contains("polygon");
               if (isPolygon) {
                  String name = sec.getString("name", qualifiedId);
                  String worldName = sec.getString("world", "world");
                  int lineColor = ColorUtil.parseColor(sec.get("lineColor"), -16711936);
                  int fillColor = ColorUtil.parseColor(sec.get("fillColor"), 855703296);
                  boolean showLine = sec.getBoolean("showLine", true);
                  boolean fillEnabled = sec.getBoolean("fillEnabled", false);
                  List<String> enterCommands = new ArrayList<>();
                  List<?> rawCmds = sec.getList("enter-commands");
                  if (rawCmds != null) {
                     for (Object o : rawCmds) {
                        if (o != null) {
                           enterCommands.add(o.toString());
                        }
                     }
                  }

                  List<?> rawVerts = sec.getList("polygon");
                  if (rawVerts != null && rawVerts.size() >= 3) {
                     List<int[]> verts = new ArrayList<>();

                     for (Object obj : rawVerts) {
                        if (obj instanceof Map<?, ?> m) {
                           int x = ((Number)m.get("x")).intValue();
                           int z = ((Number)m.get("z")).intValue();
                           verts.add(new int[]{x, z});
                        }
                     }

                     if (verts.size() < 3) {
                        this.logger
                           .warning(
                              "\u8df3\u8fc7\u591a\u8fb9\u5f62 ["
                                 + qualifiedId
                                 + "] (\u6765\u81ea "
                                 + file.getName()
                                 + "): \u89e3\u6790\u540e\u9876\u70b9\u4e0d\u8db3 3"
                           );
                     } else {
                        PolygonRegion region = new PolygonRegion(
                           qualifiedId, name, worldName, verts, lineColor, fillColor, showLine, fillEnabled, enterCommands
                        );
                        this.regionsById.put(qualifiedId, region);
                     }
                  } else {
                     this.logger
                        .warning(
                           "\u8df3\u8fc7\u65e0\u6548\u591a\u8fb9\u5f62 ["
                              + qualifiedId
                              + "] (\u6765\u81ea "
                              + file.getName()
                              + "): polygon \u9876\u70b9\u5c11\u4e8e 3 \u4e2a"
                        );
                  }
               }
            }
         );
      this.logger.info("\u591a\u8fb9\u5f62\u52a0\u8f7d\u5b8c\u6210: " + this.regionsById.size() + " \u4e2a");
   }

   public PolygonRegion get(String id) {
      return this.regionsById.get(id);
   }

   public void save() {
   }

   public void stop() {
   }

   public Collection<PolygonRegion> getAll() {
      return this.regionsById.values();
   }

   public List<PolygonRegion> getInWorld(String world) {
      List<PolygonRegion> result = new ArrayList<>();

      for (PolygonRegion r : this.regionsById.values()) {
         if (r.getWorld().equals(world)) {
            result.add(r);
         }
      }

      return result;
   }

   public List<PolygonRegion> getRegionsAt(String world, int x, int z) {
      List<PolygonRegion> result = new ArrayList<>();

      for (PolygonRegion r : this.regionsById.values()) {
         if (r.getWorld().equals(world) && r.contains(x, z)) {
            result.add(r);
         }
      }

      return result;
   }

   public List<PolygonRegion> getRegionsIntersecting(String world, int x1, int z1, int x2, int z2) {
      List<PolygonRegion> result = new ArrayList<>();

      for (PolygonRegion r : this.regionsById.values()) {
         if (r.getWorld().equals(world) && r.getMaxX() >= x1 && r.getMinX() <= x2 && r.getMaxZ() >= z1 && r.getMinZ() <= z2) {
            result.add(r);
         }
      }

      return result;
   }
}
