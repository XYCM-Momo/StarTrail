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
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Receives the MMmap Bukkit plugin messages.
 *
 * <p>The Fabric version used {@code ClientPlayNetworking.registerGlobalReceiver}. Forge's
 * equivalent is a {@link SimpleChannel}: registering it also makes Forge announce the
 * channel to the server with a {@code minecraft:register} payload
 * ({@code NetworkHooks#sendMCRegistryPackets}), which is what makes a Bukkit plugin able to
 * deliver {@code mmap:main} messages to this client.
 *
 * <p>Forge prefixes every SimpleChannel message with a one byte discriminator. The plugin
 * already prefixes its payload with a packet-type byte (1..9), so packet types 1..9 are
 * registered as message indices 1..9 and the discriminator byte is reused as the packet type.
 * The index is captured in the decoder, so the handler knows which packet arrived.
 */
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

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL_IMPL = NetworkRegistry.newSimpleChannel(
            CHANNEL,
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION));

    private static NetworkHandler active;
    private static boolean messagesRegistered;

    private final RegionDataStore regionDataStore;
    private final PolygonRegionStore polygonRegionStore;
    private final WarpPointStore warpPointStore;

    public NetworkHandler(RegionDataStore regionDataStore, PolygonRegionStore polygonRegionStore, WarpPointStore warpPointStore) {
        this.regionDataStore = regionDataStore;
        this.polygonRegionStore = polygonRegionStore;
        this.warpPointStore = warpPointStore;
    }

    /** Raw payload sent by the Bukkit plugin; the type comes from the Forge discriminator byte. */
    public static final class MmapPayload {
        public final int type;
        public final byte[] data;

        public MmapPayload(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }

    public void register() {
        active = this;
        if (messagesRegistered) {
            return;
        }
        messagesRegistered = true;

        for (int type = PACKET_HANDSHAKE; type <= PACKET_MYTHIC_MOBS; type++) {
            final int packetType = type;
            CHANNEL_IMPL.registerMessage(
                    packetType,
                    MmapPayload.class,
                    (msg, buf) -> buf.writeBytes(msg.data),
                    buf -> {
                        byte[] data = new byte[buf.readableBytes()];
                        buf.readBytes(data);
                        return new MmapPayload(packetType, data);
                    },
                    (msg, contextSupplier) -> {
                        NetworkEvent.Context context = contextSupplier.get();
                        context.enqueueWork(() -> {
                            NetworkHandler handler = active;
                            if (handler != null) {
                                handler.handlePacket(msg.type, msg.data);
                            }
                        });
                        context.setPacketHandled(true);
                    });
        }
        MMmapMod.LOGGER.info("已注册网络包接收器，通道: {}", CHANNEL);
    }

    /** Runs on the client thread. */
    private void handlePacket(int packetType, byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        if (buf.readableBytes() < 1) {
            MMmapMod.LOGGER.warn("收到空数据包");
            return;
        }
        Minecraft client = Minecraft.getInstance();
        switch (packetType) {
            case PACKET_HANDSHAKE:
                this.handleHandshake(client, buf);
                break;
            case PACKET_REGION_SYNC:
                this.handleRegionSync(client, buf);
                break;
            case PACKET_REGION_UPDATE:
                this.handleRegionUpdate(client, buf);
                break;
            case PACKET_CHUNK_COLORS:
                MMmapMod.LOGGER.debug("收到已废弃的 PACKET_CHUNK_COLORS，忽略");
                break;
            case PACKET_MAP_IMAGE:
                this.handleMapImage(client, buf);
                break;
            case PACKET_POLYGON_REGIONS:
                this.handlePolygonRegions(client, buf);
                break;
            case PACKET_WARPS:
                this.handleWarps(client, buf);
                break;
            case PACKET_WORLD_CHANGE:
                this.handleWorldChange(client, buf);
                break;
            case PACKET_MYTHIC_MOBS:
                this.handleMythicMobs(client, buf);
                break;
            default:
                MMmapMod.LOGGER.warn("未知数据包类型: 0x{}", Integer.toHexString(packetType & 255));
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
            MMmapMod.LOGGER.warn("读取 showMobHeadFallback 失败（旧服务端？），用默认值 true");
            showMobHeadFallback = true;
        }

        boolean fallback = showMobHeadFallback;
        client.execute(() -> {
            MMmapMod.LOGGER.info(
                    "收到服务端握手 - 协议版本: {}, 标识: {}, 传送点消失阈值: {}, 当前世界: {}, 怪物头颅fallback: {}",
                    new Object[]{protocolVersion, serverId, warpIconHideThreshold, currentWorld, fallback});
            ConfigManager.getInstance().getConfig().worldmap.warpIconHideThreshold = warpIconHideThreshold;
            ConfigManager.getInstance().setCurrentWorld(currentWorld);
            ConfigManager.getInstance().getConfig().general.showMobHeadFallback = fallback;
        });
    }

    private void handleWorldChange(Minecraft client, FriendlyByteBuf buf) {
        String worldName = buf.readUtf();
        client.execute(() -> {
            MMmapMod.LOGGER.info("世界切换: {}", worldName);
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
            MythicMobStore store = MMmapClient.getInstance() == null ? null : MMmapClient.getInstance().getMythicMobStore();
            if (store != null) {
                store.replaceAll(all);
                if (!all.isEmpty()) {
                    MMmapMod.LOGGER.info("[MythicMobs] 同步 {} 个怪物", all.size());
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

        client.execute(() -> MMmapMod.LOGGER.info("已同步 {} 个区域", regionCount));
    }

    private void handleRegionUpdate(Minecraft client, FriendlyByteBuf buf) {
        byte operation = buf.readByte();
        MapRegion region = this.readRegion(buf);
        client.execute(() -> {
            switch (operation) {
                case 0:
                    this.regionDataStore.addOrUpdateRegion(region);
                    MMmapMod.LOGGER.info("区域已添加: {}", region.getName());
                    break;
                case 1:
                    this.regionDataStore.removeRegion(region.getId());
                    MMmapMod.LOGGER.info("区域已移除: {}", region.getId());
                    break;
                case 2:
                    this.regionDataStore.addOrUpdateRegion(region);
                    MMmapMod.LOGGER.info("区域已更新: {}", region.getName());
                    break;
                default:
                    MMmapMod.LOGGER.warn("未知的区域更新操作: {}", operation);
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
            MMmapMod.LOGGER.info(
                    "[网络] 接收图片绑定 #{}: id={}, fileName={}, world={}, bounds=({},{})-({},{})",
                    new Object[]{i, id, fileName, world, x1, z1, x2, z2});
            images.add(new MapImage(id, fileName, world, x1, z1, x2, z2));
        }

        client.execute(() -> {
            MapImageManager mgr = MMmapClient.getInstance() == null ? null : MMmapClient.getInstance().getMapImageManager();
            if (mgr != null) {
                mgr.setImages(images);
                MMmapMod.LOGGER.info("已同步 {} 个地图图片绑定", images.size());
                if (!images.isEmpty()) {
                    MMmapMod.LOGGER.info("图片目录: {}", mgr.getImagesDir().getAbsolutePath());
                }
            } else {
                MMmapMod.LOGGER.warn("MapImageManager 未初始化，无法处理图片绑定包");
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
            MMmapMod.LOGGER.info("已同步 {} 个多边形区域", all.size());
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
                MMmapMod.LOGGER.warn("[WarpRecv] 读 descr 失败（v1.6.x 服务端？），用空字符串: {}", var15.getMessage());
            }

            all.add(new WarpPoint(id, name, world, x, y, z, iconKey, descr));
        }

        client.execute(() -> {
            this.warpPointStore.replaceAll(all);
            MMmapMod.LOGGER.info("[WarpRecv] 已同步 {} 个传送点", all.size());

            for (WarpPoint w : all) {
                MMmapMod.LOGGER.info(
                        "[WarpRecv]   - id={} name='{}' world='{}' ({},{},{}) icon='{}' descr.len={}",
                        new Object[]{w.getId(), w.getName(), w.getWorld(), w.getX(), w.getY(), w.getZ(), w.getIconKey(),
                                w.getDescr() == null ? 0 : w.getDescr().length()});
            }
        });
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
