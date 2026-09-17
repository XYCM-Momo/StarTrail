package com.mmap.server.mythic;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class MythicMobHook {
   private final boolean enabled;
   private final BukkitAPIHelper helper;

   public MythicMobHook() {
      BukkitAPIHelper h = null;
      boolean ok = false;
      if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
         try {
            h = MythicBukkit.inst().getAPIHelper();
            ok = h != null;
         } catch (Throwable var4) {
            ok = false;
         }
      }

      this.helper = h;
      this.enabled = ok;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public String getMobType(Entity e) {
      if (this.enabled && this.helper != null && e != null) {
         try {
            if (!this.helper.isMythicMob(e)) {
               return null;
            } else {
               ActiveMob mob = this.helper.getMythicMobInstance(e);
               return mob != null ? mob.getMobType() : null;
            }
         } catch (Throwable var3) {
            return null;
         }
      } else {
         return null;
      }
   }
}
