package com.asterovie.momo.startrail.server.region;

import com.asterovie.momo.startrail.server.util.ColorUtil;
import com.asterovie.momo.startrail.server.util.YamlFolderLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 矩形区域加载器。
 *
 * <p>与 {@link PolygonRegionManager} 读同一个 {@code regions/} 目录，按 {@code shape} 分流：
 * {@code shape: polygon}（或含 {@code polygon} 段）走多边形，其余走本类。
 *
 * <p><b>为什么现在才补</b>：矩形系统在 v1.9.6 曾被移除，但客户端一直保留着接收逻辑（包 2）。
 * 结果配置里写矩形区域时，服务端既不加载也不发，客户端自然什么都看不到，
 * 而且**没有任何警告**——静默失效。这里把它补回来，坐标用 double 支持小数。
 */
public class MapRegionManager {
   private final JavaPlugin plugin;
   private final Logger logger;
   private final YamlFolderLoader loader;
   private final Map<String, MapRegion> regionsById = new HashMap<>();

   public MapRegionManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.logger = plugin.getLogger();
      this.loader = new YamlFolderLoader(plugin, "regions", "regions");
   }

   public void load() {
      this.regionsById.clear();
      this.loader.loadAll((file, sec, qualifiedId) -> {
         String shape = sec.getString("shape", "rectangle");
         boolean isPolygon = "polygon".equalsIgnoreCase(shape) || sec.contains("polygon");
         if (isPolygon) {
            // 多边形由 PolygonRegionManager 负责，这里跳过
            return;
         }

         try {
            String name = sec.getString("name", qualifiedId);
            String worldName = sec.getString("world", "world");
            // 坐标一律按 double 读：配置允许小数
            double minX = sec.getDouble("minX");
            double minZ = sec.getDouble("minZ");
            double maxX = sec.getDouble("maxX");
            double maxZ = sec.getDouble("maxZ");
            double minY = sec.getDouble("minY", sec.getDouble("minHeight", Double.NEGATIVE_INFINITY));
            double maxY = sec.getDouble("maxY", sec.getDouble("maxHeight", Double.POSITIVE_INFINITY));

            RegionType type = RegionType.fromConfig(sec.getString("type", "normal"));
            int borderColor = ColorUtil.parseColor(sec.get("borderColor"), type == RegionType.NORMAL ? -16711936 : -1);
            int fillColor = ColorUtil.parseColor(sec.get("fillColor"), 855703296);
            float borderWidth = (float)sec.getDouble("borderWidth", 2.0);
            String description = sec.getString("description", "");

            if (minX == 0.0 && minZ == 0.0 && maxX == 0.0 && maxZ == 0.0) {
               this.logger.warning(
                  "跳过矩形区域 [" + qualifiedId + "] (来自 " + file.getName()
                     + "): 四个边界 minX/minZ/maxX/maxZ 都没配置"
               );
               return;
            }

            MapRegion region = new MapRegion(
               qualifiedId, name, worldName, minX, minZ, maxX, maxZ, minY, maxY,
               type, borderColor, fillColor, borderWidth, description
            );
            this.regionsById.put(qualifiedId, region);
         } catch (Throwable t) {
            this.logger.warning(
               "加载矩形区域 [" + qualifiedId + "] (来自 " + file.getName() + ") 失败: " + t.getMessage()
            );
         }
      });
      this.logger.info("矩形区域加载完成: " + this.regionsById.size() + " 个");
   }

   public MapRegion get(String id) {
      return this.regionsById.get(id);
   }

   public Collection<MapRegion> getAll() {
      return this.regionsById.values();
   }

   public void stop() {
   }
}
