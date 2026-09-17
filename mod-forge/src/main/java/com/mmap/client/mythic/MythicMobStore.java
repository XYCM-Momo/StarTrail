package com.mmap.client.mythic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MythicMobStore {
   private final Map<String, MythicMob> mobs = new ConcurrentHashMap<>();

   public MythicMobStore() {
   }

   public void replaceAll(List<MythicMob> list) {
      this.mobs.clear();

      for (MythicMob m : list) {
         this.mobs.put(m.getUuid(), m);
      }
   }

   public Collection<MythicMob> getAll() {
      return this.mobs.values();
   }

   public int size() {
      return this.mobs.size();
   }

   public List<MythicMob> getInWorld(String world) {
      List<MythicMob> result = new ArrayList<>();
      if (world == null) {
         return result;
      } else {
         for (MythicMob m : this.mobs.values()) {
            if (world.equals(m.getWorld())) {
               result.add(m);
            }
         }

         return result;
      }
   }

   public void clear() {
      this.mobs.clear();
   }
}
