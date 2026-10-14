package com.asterovie.momo.startrail.server.network;

import com.asterovie.momo.startrail.server.StarTrailPlugin;
import com.asterovie.momo.startrail.server.image.MapImage;
import com.asterovie.momo.startrail.server.mythic.MythicMobTracker;
import com.asterovie.momo.startrail.server.region.PolygonRegion;
import com.asterovie.momo.startrail.server.warp.WarpPoint;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PacketSender {
   private final StarTrailPlugin plugin;
   public static final String CHANNEL = "startrail:main";
   public static final byte PACKET_HANDSHAKE = 1;
   public static final byte PACKET_CHUNK_COLORS = 4;
   public static final byte PACKET_MAP_IMAGE = 5;
   public static final byte PACKET_POLYGON_REGIONS = 6;
   public static final byte PACKET_WARPS = 7;
   public static final byte PACKET_WORLD_CHANGE = 8;
   public static final byte PACKET_MYTHIC_MOBS = 9;

   public PacketSender(StarTrailPlugin plugin) {
      this.plugin = plugin;
   }

   public void sendHandshake(Player player) {
      try {
         PacketSender.PacketBuffer buf = new PacketSender.PacketBuffer();
         buf.writeByte(1);
         buf.writeInt(1);
         buf.writeString("StarTrail-Server");
         double threshold = this.plugin.getConfig().getDouble("settings.warp-icon-hide-threshold", 0.8);
         buf.writeDouble(threshold);
         buf.writeString(player.getWorld().getName());
         boolean showMobHeadFallback = this.plugin.getConfig().getBoolean("settings.show-mob-head-fallback", true);
         buf.writeBoolean(showMobHeadFallback);
         // 是否允许大地图右键传送（仅 OP / 有 startrail.command.tp 权限）；客户端据此隐藏/显示「传送到此处」
         boolean canTeleport = player.hasPermission("startrail.command.tp") || player.isOp();
         buf.writeBoolean(canTeleport);
         this.sendPacket(player, buf.toByteArray());
      } catch (Exception var6) {
         this.plugin.getLogger().warning("\u53d1\u9001\u63e1\u624b\u5305\u5931\u8d25: " + var6.getMessage());
      }
   }

   public void sendWorldChange(Player player) {
      try {
         PacketSender.PacketBuffer buf = new PacketSender.PacketBuffer();
         buf.writeByte(8);
         buf.writeString(player.getWorld().getName());
         this.sendPacket(player, buf.toByteArray());
      } catch (Exception var3) {
         this.plugin.getLogger().warning("\u53d1\u9001\u4e16\u754c\u5207\u6362\u5305\u5931\u8d25: " + var3.getMessage());
      }
   }

   public void sendMapImageSync(Player player) {
      this.sendMapImageSync(player, true);
   }

   /** @param notify 是否顺带发聊天提示（进服首次同步才发，重试只发数据避免刷屏） */
   public void sendMapImageSync(Player player, boolean notify) {
      try {
         Collection<MapImage> images = this.plugin.getMapImageManager().getAllImages();
         PacketSender.PacketBuffer buf = new PacketSender.PacketBuffer();
         buf.writeByte(5);
         buf.writeVarInt(images.size());

         for (MapImage image : images) {
            this.writeMapImage(buf, image);
         }

         this.sendPacket(player, buf.toByteArray());
         // 同步提示只有 OP（或有 startrail.command 权限）能看，普通玩家不刷屏
         if (notify && !images.isEmpty() && (player.isOp() || player.hasPermission("startrail.command"))) {
            player.sendMessage("\u00a77[StarTrail] \u5df2\u540c\u6b65 " + images.size() + " \u4e2a\u5730\u56fe\u56fe\u7247\u7ed1\u5b9a\u3002");
            player.sendMessage("\u00a77\u8bf7\u5c06\u56fe\u7247\u6587\u4ef6\u653e\u5728 .minecraft/config/startrail/map/ \u76ee\u5f55\u4e0b\u3002");
         }
      } catch (Exception var6) {
         this.plugin.getLogger().warning("\u53d1\u9001\u5730\u56fe\u56fe\u7247\u540c\u6b65\u5305\u5931\u8d25: " + var6.getMessage());
      }
   }

   public void broadcastMapImageSync() {
      for (Player player : this.plugin.getServer().getOnlinePlayers()) {
         this.sendMapImageSync(player);
      }
   }

   private void writeMapImage(PacketSender.PacketBuffer buf, MapImage image) throws IOException {
      buf.writeString(image.getId());
      buf.writeString(image.getFileName());
      buf.writeString(image.getWorld());
      buf.writeInt(image.getMinX());
      buf.writeInt(image.getMinZ());
      buf.writeInt(image.getMaxX());
      buf.writeInt(image.getMaxZ());
   }

   public void sendPolygonRegions(Player player) {
      try {
         Collection<PolygonRegion> all = this.plugin.getPolygonRegionManager().getAll();
         PacketSender.PacketBuffer buf = new PacketSender.PacketBuffer();
         buf.writeByte(6);
         buf.writeVarInt(all.size());

         for (PolygonRegion r : all) {
            buf.writeString(r.getId());
            buf.writeString(r.getName());
            buf.writeString(this.toClientWorldKey(r.getWorld()));
            buf.writeVarInt(r.getVertices().size());

            for (int[] v : r.getVertices()) {
               buf.writeInt(v[0]);
               buf.writeInt(v[1]);
            }

            buf.writeInt(r.getLineColor());
            buf.writeInt(r.getFillColor());
            buf.writeByte(r.isShowLine() ? 1 : 0);
            buf.writeByte(r.isFillEnabled() ? 1 : 0);
         }

         byte[] data = buf.toByteArray();
         this.sendPacket(player, data);
      } catch (Exception var8) {
         this.plugin.getLogger().warning("\u53d1\u9001\u591a\u8fb9\u5f62\u533a\u57df\u540c\u6b65\u5305\u5931\u8d25: " + var8.getMessage());
      }
   }

   private String toClientWorldKey(String shortName) {
      if (shortName == null) {
         return "minecraft:overworld";
      } else if (shortName.contains(":")) {
         return shortName;
      } else {
         World w = Bukkit.getWorld(shortName);
         if (w == null) {
            return shortName;
         } else {
            String ns = "minecraft";
            switch (w.getEnvironment()) {
               case NETHER:
                  return ns + ":the_nether";
               case THE_END:
                  return ns + ":the_end";
               case NORMAL:
               case CUSTOM:
               default:
                  return ns + ":overworld";
            }
         }
      }
   }

   public void broadcastPolygonRegions() {
      for (Player player : this.plugin.getServer().getOnlinePlayers()) {
         this.sendPolygonRegions(player);
      }
   }

   public void sendWarps(Player player) {
      try {
         Collection<WarpPoint> all = this.plugin.getWarpPointManager().getAll();
         PacketSender.PacketBuffer buf = new PacketSender.PacketBuffer();
         buf.writeByte(7);
         buf.writeVarInt(all.size());

         for (WarpPoint w : all) {
            buf.writeString(w.getId());
            buf.writeString(w.getName());
            buf.writeString(this.toClientWorldKey(w.getWorld()));
            buf.writeInt(w.getX());
            buf.writeInt(w.getY());
            buf.writeInt(w.getZ());
            buf.writeString(w.getIconKey());
            buf.writeString(w.getDescr());
         }

         byte[] data = buf.toByteArray();
         this.sendPacket(player, data);
      } catch (Exception var6) {
         this.plugin.getLogger().warning("\u53d1\u9001\u4f20\u9001\u70b9\u540c\u6b65\u5305\u5931\u8d25: " + var6.getMessage());
      }
   }

   public void broadcastWarps() {
      for (Player player : this.plugin.getServer().getOnlinePlayers()) {
         this.sendWarps(player);
      }
   }

   public void sendMythicMobs(Player player, List<MythicMobTracker.MobEntry> mobs, String bukkitWorld) {
      try {
         PacketSender.PacketBuffer buf = new PacketSender.PacketBuffer();
         buf.writeByte(9);
         buf.writeVarInt(mobs.size());
         String worldKey = this.toClientWorldKey(bukkitWorld);

         for (MythicMobTracker.MobEntry m : mobs) {
            buf.writeString(m.uuid);
            buf.writeString(m.type);
            buf.writeString(worldKey);
            buf.writeDouble(m.x);
            buf.writeDouble(m.y);
            buf.writeDouble(m.z);
         }

         this.sendPacket(player, buf.toByteArray());
      } catch (Exception var8) {
         this.plugin.getLogger().warning("\u53d1\u9001 MythicMobs \u5305\u5931\u8d25: " + var8.getMessage());
      }
   }

   private void sendPacket(Player player, byte[] data) {
      try {
         player.sendPluginMessage(this.plugin, "startrail:main", data);
      } catch (Exception var4) {
         this.plugin.getLogger().warning("\u53d1\u9001\u6570\u636e\u5305\u7ed9 " + player.getName() + " \u5931\u8d25: " + var4.getMessage());
      }
   }

   public static class PacketBuffer {
      private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(256);
      private final DataOutputStream out = new DataOutputStream(this.byteStream);

      public PacketBuffer() {
      }

      public void writeByte(int value) throws IOException {
         this.out.writeByte(value);
      }

      public void writeBoolean(boolean value) throws IOException {
         this.out.writeBoolean(value);
      }

      public void writeShort(int value) throws IOException {
         this.out.writeShort(value);
      }

      public void writeInt(int value) throws IOException {
         this.out.writeInt(value);
      }

      public void writeFloat(float value) throws IOException {
         this.out.writeFloat(value);
      }

      public void writeDouble(double value) throws IOException {
         this.out.writeDouble(value);
      }

      public void writeVarInt(int value) throws IOException {
         while ((value & -128) != 0) {
            this.out.writeByte(value & 127 | 128);
            value >>>= 7;
         }

         this.out.writeByte(value);
      }

      public void writeString(String value) throws IOException {
         byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
         this.writeVarInt(bytes.length);
         this.out.write(bytes);
      }

      public void writeBytes(byte[] data) throws IOException {
         this.out.write(data);
      }

      public byte[] toByteArray() {
         return this.byteStream.toByteArray();
      }
   }
}
