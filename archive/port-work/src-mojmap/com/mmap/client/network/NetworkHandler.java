package com.mmap.client.network;

import com.mmap.client.MMmapClient;
import com.mmap.client.MMmapMod;
import com.mmap.client.config.ConfigManager;
import com.mmap.client.image.MapImage;
import com.mmap.client.image.MapImageManager;
import com.mmap.client.mythic.MythicMob;
import com.mmap.client.mythic.MythicMobStore;
import com.mmap.client.region.MapRegion;
import com.mmap.client.region.PolygonRegion;
import com.mmap.client.region.PolygonRegionStore;
import com.mmap.client.region.RegionDataStore;
import com.mmap.client.region.RegionType;
import com.mmap.client.warp.WarpPoint;
import com.mmap.client.warp.WarpPointStore;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class NetworkHandler {
   public static final ResourceLocation CHANNEL = new ResourceLocation("mmap", "main");
   public static final byte PACKET_HANDSHAKE = 1;
   public static final byte PACKET_REGION_SYNC = 2;
   public static final byte PACKET_REGION_UPDATE = 3;
   public static final byte PACKET_CHUNK_COLORS = 4;
   public static final byte PACKET_MAP_IMAGE = 5;
   public static final byte PACKET_POLYGON_REGIONS = 6;
   public static final byte PACKET_WARPS = 7;
   public static final byte PACKET_WORLD_CHANGE = 8;
   public static final byte PACKET_MYTHIC_MOBS = 9;
   private final RegionDataStore regionDataStore;
   private final PolygonRegionStore polygonRegionStore;
   private final WarpPointStore warpPointStore;

   public NetworkHandler(RegionDataStore regionDataStore, PolygonRegionStore polygonRegionStore, WarpPointStore warpPointStore) {
      this.regionDataStore = regionDataStore;
      this.polygonRegionStore = polygonRegionStore;
      this.warpPointStore = warpPointStore;
   }

   public void register() {
      ClientPlayNetworking.registerGlobalReceiver(CHANNEL, this::onPacketReceived);
      MMmapMod.LOGGER.info("\u5df2\u6ce8\u518c\u7f51\u7edc\u5305\u63a5\u6536\u5668\uff0c\u901a\u9053: {}", CHANNEL);
   }

   private void onPacketReceived(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
      if (buf.readableBytes() < 1) {
         MMmapMod.LOGGER.warn("\u6536\u5230\u7a7a\u6570\u636e\u5305");
      } else {
         byte packetType = buf.readByte();
         switch (packetType) {
            case 1:
               this.handleHandshake(client, buf);
               break;
            case 2:
               this.handleRegionSync(client, buf);
               break;
            case 3:
               this.handleRegionUpdate(client, buf);
               break;
            case 4:
               MMmapMod.LOGGER.debug("\u6536\u5230\u5df2\u5e9f\u5f03\u7684 PACKET_CHUNK_COLORS\uff0c\u5ffd\u7565");
               break;
            case 5:
               this.handleMapImage(client, buf);
               break;
            case 6:
               this.handlePolygonRegions(client, buf);
               break;
            case 7:
               this.handleWarps(client, buf);
               break;
            case 8:
               this.handleWorldChange(client, buf);
               break;
            case 9:
               this.handleMythicMobs(client, buf);
               break;
            default:
               MMmapMod.LOGGER.warn("\u672a\u77e5\u6570\u636e\u5305\u7c7b\u578b: 0x{}", Integer.toHexString(packetType & 255));
         }
      }
   }

   private void handleHandshake(Minecraft client, FriendlyByteBuf buf) {
      int protocolVersion = buf.readInt();
      String serverId = buf.readUtf();
      double warpIconHideThreshold = buf.readDouble();
      String currentWorld = buf.readUtf();

      boolean showMobHeadFallback;
      try {
         showMobHeadFallback = buf.readBoolean();
      } catch (Exception var10) {
         MMmapMod.LOGGER.warn("\u8bfb\u53d6 showMobHeadFallback \u5931\u8d25\uff08\u65e7\u670d\u52a1\u7aef\uff1f\uff09\uff0c\u7528\u9ed8\u8ba4\u503c true");
         showMobHeadFallback = true;
      }

      boolean fallback = showMobHeadFallback;
      client.execute(
         () -> {
            MMmapMod.LOGGER
               .info(
                  "\u6536\u5230\u670d\u52a1\u7aef\u63e1\u624b - \u534f\u8bae\u7248\u672c: {}, \u6807\u8bc6: {}, \u4f20\u9001\u70b9\u6d88\u5931\u9608\u503c: {}, \u5f53\u524d\u4e16\u754c: {}, \u602a\u7269\u5934\u50cffallback: {}",
                  new Object[]{protocolVersion, serverId, warpIconHideThreshold, currentWorld, fallback}
               );
            ConfigManager.getInstance().getConfig().worldmap.warpIconHideThreshold = warpIconHideThreshold;
            ConfigManager.getInstance().setCurrentWorld(currentWorld);
            ConfigManager.getInstance().getConfig().general.showMobHeadFallback = fallback;
         }
      );
   }

   private void handleWorldChange(Minecraft client, FriendlyByteBuf buf) {
      String worldName = buf.readUtf();
      client.execute(() -> {
         MMmapMod.LOGGER.info("\u4e16\u754c\u5207\u6362: {}", worldName);
         ConfigManager.getInstance().setCurrentWorld(worldName);
      });
   }

   private void handleMythicMobs(Minecraft client, FriendlyByteBuf buf) {
      int count = buf.readVarInt();
      List<MythicMob> all = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         String uuid = buf.readUtf();
         String type = buf.readUtf();
         String world = buf.readUtf();
         double x = buf.readDouble();
         double y = buf.readDouble();
         double z = buf.readDouble();
         all.add(new MythicMob(uuid, type, world, x, y, z));
      }

      client.execute(() -> {
         MythicMobStore store = MMmapClient.getInstance().getMythicMobStore();
         if (store != null) {
            store.replaceAll(all);
            if (!all.isEmpty()) {
               MMmapMod.LOGGER.info("[MythicMobs] \u540c\u6b65 {} \u4e2a\u602a\u7269", all.size());
            }
         }
      });
   }

   private void handleRegionSync(Minecraft client, FriendlyByteBuf buf) {
      int regionCount = buf.readVarInt();
      this.regionDataStore.clear();

      for (int i = 0; i < regionCount; i++) {
         MapRegion region = this.readRegion(buf);
         this.regionDataStore.addOrUpdateRegion(region);
      }

      client.execute(() -> MMmapMod.LOGGER.info("\u5df2\u540c\u6b65 {} \u4e2a\u533a\u57df", regionCount));
   }

   private void handleRegionUpdate(Minecraft client, FriendlyByteBuf buf) {
      byte operation = buf.readByte();
      MapRegion region = this.readRegion(buf);
      client.execute(() -> {
         switch (operation) {
            case 0:
               this.regionDataStore.addOrUpdateRegion(region);
               MMmapMod.LOGGER.info("\u533a\u57df\u5df2\u6dfb\u52a0: {}", region.getName());
               break;
            case 1:
               this.regionDataStore.removeRegion(region.getId());
               MMmapMod.LOGGER.info("\u533a\u57df\u5df2\u79fb\u9664: {}", region.getId());
               break;
            case 2:
               this.regionDataStore.addOrUpdateRegion(region);
               MMmapMod.LOGGER.info("\u533a\u57df\u5df2\u66f4\u65b0: {}", region.getName());
               break;
            default:
               MMmapMod.LOGGER.warn("\u672a\u77e5\u7684\u533a\u57df\u66f4\u65b0\u64cd\u4f5c: {}", operation);
         }
      });
   }

   private void handleMapImage(Minecraft client, FriendlyByteBuf buf) {
      int imageCount = buf.readVarInt();
      List<MapImage> images = new ArrayList<>(imageCount);

      for (int i = 0; i < imageCount; i++) {
         String id = buf.readUtf();
         String fileName = buf.readUtf();
         String world = buf.readUtf();
         int x1 = buf.readInt();
         int z1 = buf.readInt();
         int x2 = buf.readInt();
         int z2 = buf.readInt();
         MMmapMod.LOGGER
            .info(
               "[\u7f51\u7edc] \u63a5\u6536\u56fe\u7247\u7ed1\u5b9a #{}: id={}, fileName={}, world={}, bounds=({},{})-({},{})",
               new Object[]{i, id, fileName, world, x1, z1, x2, z2}
            );
         images.add(new MapImage(id, fileName, world, x1, z1, x2, z2));
      }

      client.execute(() -> {
         MapImageManager mgr = MMmapClient.getInstance().getMapImageManager();
         if (mgr != null) {
            mgr.setImages(images);
            MMmapMod.LOGGER.info("\u5df2\u540c\u6b65 {} \u4e2a\u5730\u56fe\u56fe\u7247\u7ed1\u5b9a", images.size());
            if (!images.isEmpty()) {
               MMmapMod.LOGGER.info("\u56fe\u7247\u76ee\u5f55: {}", mgr.getImagesDir().getAbsolutePath());
            }
         } else {
            MMmapMod.LOGGER.warn("MapImageManager \u672a\u521d\u59cb\u5316\uff0c\u65e0\u6cd5\u5904\u7406\u56fe\u7247\u7ed1\u5b9a\u5305");
         }
      });
   }

   private void handlePolygonRegions(Minecraft client, FriendlyByteBuf buf) {
      int count = buf.readVarInt();
      List<PolygonRegion> all = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         String id = buf.readUtf();
         String name = buf.readUtf();
         String world = buf.readUtf();
         int vertexCount = buf.readVarInt();
         List<int[]> verts = new ArrayList<>(vertexCount);

         for (int j = 0; j < vertexCount; j++) {
            int x = buf.readInt();
            int z = buf.readInt();
            verts.add(new int[]{x, z});
         }

         int lineColor = buf.readInt();
         int fillColor = buf.readInt();
         boolean showLine = buf.readByte() != 0;
         boolean fillEnabled = buf.readByte() != 0;
         all.add(new PolygonRegion(id, name, world, verts, lineColor, fillColor, showLine, fillEnabled));
      }

      client.execute(() -> {
         this.polygonRegionStore.replaceAll(all);
         MMmapMod.LOGGER.info("\u5df2\u540c\u6b65 {} \u4e2a\u591a\u8fb9\u5f62\u533a\u57df", all.size());
      });
   }

   private void handleWarps(Minecraft client, FriendlyByteBuf buf) {
      int count = buf.readVarInt();
      List<WarpPoint> all = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         String id = buf.readUtf();
         String name = buf.readUtf();
         String world = buf.readUtf();
         int x = buf.readInt();
         int y = buf.readInt();
         int z = buf.readInt();
         String iconKey = buf.readUtf();
         String descr = "";

         try {
            descr = buf.readUtf();
         } catch (Exception var15) {
            MMmapMod.LOGGER
               .warn(
                  "[WarpRecv] \u8bfb descr \u5931\u8d25\uff08v1.6.x \u670d\u52a1\u7aef\uff1f\uff09\uff0c\u7528\u7a7a\u5b57\u7b26\u4e32: {}", var15.getMessage()
               );
         }

         all.add(new WarpPoint(id, name, world, x, y, z, iconKey, descr));
      }

      client.execute(
         () -> {
            this.warpPointStore.replaceAll(all);
            MMmapMod.LOGGER.info("[WarpRecv] \u5df2\u540c\u6b65 {} \u4e2a\u4f20\u9001\u70b9", all.size());

            for (WarpPoint w : all) {
               MMmapMod.LOGGER
                  .info(
                     "[WarpRecv]   - id={} name='{}' world='{}' ({},{},{}) icon='{}' descr.len={}",
                     new Object[]{
                        w.getId(), w.getName(), w.getWorld(), w.getX(), w.getY(), w.getZ(), w.getIconKey(), w.getDescr() == null ? 0 : w.getDescr().length()
                     }
                  );
            }
         }
      );
   }

   private MapRegion readRegion(FriendlyByteBuf buf) {
      String id = buf.readUtf();
      String name = buf.readUtf();
      String world = buf.readUtf();
      int minX = buf.readInt();
      int minZ = buf.readInt();
      int maxX = buf.readInt();
      int maxZ = buf.readInt();
      int minY = buf.readShort();
      int maxY = buf.readShort();
      RegionType type = RegionType.fromOrdinal(buf.readByte());
      int borderColor = buf.readInt();
      int fillColor = buf.readInt();
      float borderWidth = buf.readFloat();
      String description = buf.readUtf();
      return new MapRegion(id, name, world, minX, minZ, maxX, maxZ, minY, maxY, type, borderColor, fillColor, borderWidth, description);
   }
}
