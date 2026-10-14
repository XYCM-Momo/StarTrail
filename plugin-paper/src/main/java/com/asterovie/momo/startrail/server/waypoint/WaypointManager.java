package com.asterovie.momo.startrail.server.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 导航点位管理：每个地图（图片绑定 ID）一个 JSON 文件，存放在
 * {@code plugins/StarTrail/waypoints/<地图ID>.json}。
 *
 * <p>文件格式（客户端从 {@code .minecraft/startrail/waypoints/} 读取同一格式）：
 * <pre>
 * {
 *   "world": "world",
 *   "waypoints": [
 *     {"id": "a", "x": 100.5, "y": 64, "z": 200.5, "links": ["b"]}
 *   ]
 * }
 * </pre>
 * 坐标支持小数（旧整数文件也兼容，Gson 自动转 double）。
 *
 * <p>服务端目前只作为备份与维护入口，客户端点位文件后续由启动器从后台自动更新。
 */
public class WaypointManager {
   /** 单个地图的点位文件内容。 */
   public static class WaypointFileData {
      public String world = "";
      public List<Waypoint> waypoints = new ArrayList<>();
   }

   private final JavaPlugin plugin;
   private final File waypointsDir;
   private final Gson gson;
   private final Map<String, WaypointFileData> byMap = new LinkedHashMap<>();

   public WaypointManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.waypointsDir = new File(plugin.getDataFolder(), "waypoints");
      this.gson = new GsonBuilder().setPrettyPrinting().create();
   }

   public void loadAll() {
      this.byMap.clear();
      if (!this.waypointsDir.isDirectory()) {
         return;
      }

      File[] files = this.waypointsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
      if (files == null) {
         return;
      }

      for (File f : files) {
         String mapId = f.getName().substring(0, f.getName().length() - 5);
         try (FileReader reader = new FileReader(f)) {
            Type type = new TypeToken<WaypointFileData>() {}.getType();
            WaypointFileData data = this.gson.fromJson(reader, type);
            if (data == null || data.waypoints == null) {
               data = new WaypointFileData();
            }

            this.byMap.put(mapId, data);
         } catch (IOException e) {
            this.plugin.getLogger().warning("加载点位文件失败: " + f.getName() + " (" + e.getMessage() + ")");
         }
      }
   }

   /** 保存某个地图的点位文件到磁盘（作为备份，也供拷贝到客户端）。 */
   public void save(String mapId) {
      WaypointFileData data = this.byMap.get(mapId);
      if (data == null) {
         return;
      }

      if (!this.waypointsDir.isDirectory()) {
         this.waypointsDir.mkdirs();
      }

      File f = new File(this.waypointsDir, sanitize(mapId) + ".json");
      try (FileWriter writer = new FileWriter(f)) {
         this.gson.toJson(data, writer);
      } catch (IOException e) {
         this.plugin.getLogger().warning("保存点位文件失败: " + f.getName() + " (" + e.getMessage() + ")");
      }
   }

   /** 删除某个地图的点位文件。 */
   public void delete(String mapId) {
      this.byMap.remove(mapId);
      File f = new File(this.waypointsDir, sanitize(mapId) + ".json");
      if (f.exists()) {
         f.delete();
      }
   }

   /** 地图 ID -> 文件名的安全转换（去掉非法字符）。 */
   public static String sanitize(String mapId) {
      return mapId.replaceAll("[\\\\/:*?\"<>|]", "_");
   }

   public boolean hasMap(String mapId) {
      return this.byMap.containsKey(mapId);
   }

   public WaypointFileData getData(String mapId) {
      return this.byMap.get(mapId);
   }

   /** 取某个地图的点位列表（不存在则返回空列表，不创建）。 */
   public List<Waypoint> getWaypoints(String mapId) {
      WaypointFileData data = this.byMap.get(mapId);
      return data != null ? data.waypoints : new ArrayList<>();
   }

   public Waypoint get(String mapId, String waypointId) {
      for (Waypoint wp : this.getWaypoints(mapId)) {
         if (wp.getId().equals(waypointId)) {
            return wp;
         }
      }

      return null;
   }

   public boolean hasWaypoint(String mapId, String waypointId) {
      return this.get(mapId, waypointId) != null;
   }

   /** 添加点位；重复 id 返回 false。world 用于新建文件时写入（已存在的文件不覆盖）。 */
   public boolean add(String mapId, Waypoint waypoint, String world) {
      WaypointFileData data = this.byMap.computeIfAbsent(mapId, k -> new WaypointFileData());
      if (data.world == null || data.world.isEmpty()) {
         data.world = world == null ? "" : world;
      }

      for (Waypoint wp : data.waypoints) {
         if (wp.getId().equals(waypoint.getId())) {
            return false;
         }
      }

      data.waypoints.add(waypoint);
      this.save(mapId);
      return true;
   }

   /** 删除点位；同时从其它点位的 links 里移除。 */
   public boolean remove(String mapId, String waypointId) {
      WaypointFileData data = this.byMap.get(mapId);
      if (data == null) {
         return false;
      }

      boolean found = data.waypoints.removeIf(wp -> wp.getId().equals(waypointId));
      if (found) {
         for (Waypoint wp : data.waypoints) {
            wp.removeLink(waypointId);
         }

         if (data.waypoints.isEmpty()) {
            this.delete(mapId);
         } else {
            this.save(mapId);
         }
      }

      return found;
   }

   /** 建立双向连通。两个点位都必须存在。 */
   public boolean link(String mapId, String waypointA, String waypointB) {
      Waypoint a = this.get(mapId, waypointA);
      Waypoint b = this.get(mapId, waypointB);
      if (a == null || b == null || a.equals(b)) {
         return false;
      }

      a.addLink(waypointB);
      b.addLink(waypointA);
      this.save(mapId);
      return true;
   }

   /** 取消双向连通。 */
   public boolean unlink(String mapId, String waypointA, String waypointB) {
      Waypoint a = this.get(mapId, waypointA);
      Waypoint b = this.get(mapId, waypointB);
      if (a == null || b == null) {
         return false;
      }

      a.removeLink(waypointB);
      b.removeLink(waypointA);
      this.save(mapId);
      return true;
   }

   public Collection<String> getMapIds() {
      return this.byMap.keySet();
   }

   public int totalCount() {
      int n = 0;
      for (WaypointFileData data : this.byMap.values()) {
         n += data.waypoints.size();
      }

      return n;
   }
}
