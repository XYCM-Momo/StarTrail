package com.asterovie.momo.startrail.client.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.asterovie.momo.startrail.client.StarTrailMod;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * 客户端点位存储：读取 {@code .minecraft/startrail/waypoints/<地图ID>.json}（与服务器 waypoints 目录同一格式）。
 *
 * <p>文件内容：{@code {"world": "world", "waypoints": [{"id","x","y","z","links":[...]}]}}。
 * 每个文件属于一个地图（文件名 = 地图 ID），world 字段标明所属世界。
 */
public class WaypointStore {
   /** 单个地图的点位文件内容（与服务器 WaypointManager.WaypointFileData 对应）。 */
   public static class WaypointFile {
      public String world = "";
      public List<Waypoint> waypoints = new ArrayList<>();
   }

   private final File waypointsDir;
   private final Gson gson = new GsonBuilder().create();
   private final Map<String, WaypointFile> byMap = new LinkedHashMap<>();

   public WaypointStore() {
      this.waypointsDir = new File(FMLPaths.GAMEDIR.get().toFile(), "startrail/waypoints");
   }

   public File getWaypointsDir() {
      return this.waypointsDir;
   }

   /** 重新扫描 waypoints 目录（启动导航前调用，便于启动器更新后即时生效）。 */
   public void reload() {
      this.byMap.clear();
      if (!this.waypointsDir.isDirectory()) {
         return;
      }

      File[] files = this.waypointsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
      if (files == null) {
         return;
      }

      for (File f : files) {
         try (FileReader reader = new FileReader(f)) {
            Type type = new TypeToken<WaypointFile>() {}.getType();
            WaypointFile wf = this.gson.fromJson(reader, type);
            if (wf == null || wf.waypoints == null) {
               continue;
            }

            this.byMap.put(f.getName().substring(0, f.getName().length() - 5), wf);
         } catch (Exception e) {
            StarTrailMod.LOGGER.warn("[waypoint] \u52a0\u8f7d\u70b9\u4f4d\u6587\u4ef6\u5931\u8d25: {}", f.getName(), e);
         }
      }
   }

   /** 取指定世界（服务端世界名，如 world / world_nether）的全部点位。 */
   public List<Waypoint> getForWorld(String world) {
      List<Waypoint> out = new ArrayList<>();
      if (world == null) {
         return out;
      }

      for (WaypointFile wf : this.byMap.values()) {
         if (wf.world == null || wf.world.isEmpty() || wf.world.equals(world)) {
            out.addAll(wf.waypoints);
         }
      }

      return out;
   }

   public int totalCount() {
      int n = 0;
      for (WaypointFile wf : this.byMap.values()) {
         n += wf.waypoints.size();
      }

      return n;
   }
}
