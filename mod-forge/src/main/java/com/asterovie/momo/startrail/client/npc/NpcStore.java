package com.asterovie.momo.startrail.client.npc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端侧记录「哪些实体 UUID 是服务端 Adyeshach 的 NPC」。
 *
 * <p>为什么需要：Adyeshach 的 NPC 是 **PLAYER 类型**实体，在客户端看来和真人玩家一模一样，
 * 地图上原本会被当作「其他玩家」画头像。要单独给 NPC 做开关，必须先能把它和真人区分开。
 *
 * <p>服务端每隔一段时间下发附近 NPC 的 UUID 列表（包类型 11），这里整表替换。
 * 实测确认 Adyeshach 的 NPC UUID 就是客户端实体的 UUID（uniqueId == normalizeUniqueId）。
 */
public final class NpcStore {
   private volatile Set<UUID> npcIds = Collections.emptySet();

   /** 整表替换（每次收到服务端下发）。解析失败的条目直接跳过。 */
   public void replaceAll(Set<String> rawIds) {
      Set<UUID> next = new HashSet<>();
      if (rawIds != null) {
         for (String s : rawIds) {
            if (s == null || s.isEmpty()) {
               continue;
            }

            try {
               next.add(UUID.fromString(s.trim()));
            } catch (IllegalArgumentException ignored) {
               // 格式不对就忽略这一条
            }
         }
      }

      this.npcIds = Collections.unmodifiableSet(next);
   }

   /** 该实体是否是服务端下发的 NPC。 */
   public boolean isNpc(UUID id) {
      return id != null && this.npcIds.contains(id);
   }

   public int size() {
      return this.npcIds.size();
   }

   public void clear() {
      this.npcIds = Collections.emptySet();
   }
}
