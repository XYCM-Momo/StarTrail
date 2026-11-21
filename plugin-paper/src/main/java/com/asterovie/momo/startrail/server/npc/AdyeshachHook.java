package com.asterovie.momo.startrail.server.npc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * 与 Adyeshach 的桥接（**全程反射**）。
 *
 * <p>Adyeshach 是闭源插件，不能编译期依赖，所以这里所有调用都走反射；
 * 未安装 / 调用失败时一律安全降级为「没有 NPC」，绝不影响插件其余功能。
 *
 * <p>实测确认（2026-11-22，Adyeshach 2.1.35）：
 * <ul>
 *   <li>{@code AdyeshachAPI.INSTANCE.getEntityManagerPublic().getEntities()} 能取到全部持久化 NPC；</li>
 *   <li>NPC 类型为 {@code PLAYER}（客户端看到的就是玩家实体）；</li>
 *   <li>{@code getUniqueId()} 与 {@code getNormalizeUniqueId()} **完全相同**，
 *       即客户端实体的 UUID 就是 NPC 数据文件里的那个 UUID——可以据此在客户端识别 NPC。</li>
 * </ul>
 */
public final class AdyeshachHook {
   /** 单个 NPC 的标识信息（客户端只需要 UUID 来区分「这是 NPC 不是真人」）。 */
   public static final class NpcEntry {
      public final String uuid;
      public final double x;
      public final double z;
      public final String world;

      NpcEntry(String uuid, double x, double z, String world) {
         this.uuid = uuid;
         this.x = x;
         this.z = z;
         this.world = world;
      }
   }

   private final boolean available;
   private Object api;
   private Method getManagerPublic;

   public AdyeshachHook() {
      boolean ok = false;
      Object apiObj = null;
      Method mgrMethod = null;

      if (Bukkit.getPluginManager().getPlugin("Adyeshach") != null) {
         try {
            Class<?> apiCls = Class.forName("ink.ptms.adyeshach.api.AdyeshachAPI");
            apiObj = apiCls.getField("INSTANCE").get(null);
            mgrMethod = apiCls.getMethod("getEntityManagerPublic");
            ok = apiObj != null;
         } catch (Throwable t) {
            ok = false;
         }
      }

      this.api = apiObj;
      this.getManagerPublic = mgrMethod;
      this.available = ok;
   }

   public boolean isAvailable() {
      return this.available;
   }

   /**
    * 取指定世界、指定中心点附近的所有 NPC。
    *
    * @param world      Bukkit 世界名
    * @param cx         中心 X
    * @param cz         中心 Z
    * @param radius     半径（格）
    */
   public List<NpcEntry> getNpcsNear(String world, double cx, double cz, double radius) {
      List<NpcEntry> out = new ArrayList<>();
      if (!this.available) {
         return out;
      }

      try {
         Object manager = this.getManagerPublic.invoke(this.api);
         if (manager == null) {
            return out;
         }

         Object listObj = manager.getClass().getMethod("getEntities").invoke(manager);
         if (!(listObj instanceof Collection)) {
            return out;
         }

         Class<?> base = Class.forName("ink.ptms.adyeshach.common.entity.EntityBase");
         Method getIdUuid = base.getMethod("getUniqueId");
         Method getLoc = base.getMethod("getLocation");
         double r2 = radius * radius;

         for (Object npc : (Collection<?>) listObj) {
            try {
               Object uid = getIdUuid.invoke(npc);
               if (uid == null) {
                  continue;
               }

               Object locObj = getLoc.invoke(npc);
               if (!(locObj instanceof Location)) {
                  continue;
               }

               Location loc = (Location) locObj;
               World w = loc.getWorld();
               if (w == null || !w.getName().equals(world)) {
                  continue;
               }

               double dx = loc.getX() - cx;
               double dz = loc.getZ() - cz;
               if (dx * dx + dz * dz > r2) {
                  continue;
               }

               out.add(new NpcEntry(String.valueOf(uid), loc.getX(), loc.getZ(), w.getName()));
            } catch (Throwable ignored) {
               // 单个 NPC 取失败不影响其他
            }
         }
      } catch (Throwable t) {
         // 管理器结构变化时不刷屏，调用方按「没有 NPC」处理
      }

      return out;
   }
}
