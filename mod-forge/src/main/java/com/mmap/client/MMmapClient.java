package com.mmap.client;

import com.mmap.client.command.MMmapCommands;
import com.mmap.client.config.ConfigManager;
import com.mmap.client.image.MapImageManager;
import com.mmap.client.mythic.MobHeadManager;
import com.mmap.client.mythic.MythicMobStore;
import com.mmap.client.network.NetworkHandler;
import com.mmap.client.region.PolygonRegionStore;
import com.mmap.client.region.RegionDataStore;
import com.mmap.client.render.MinimapManager;
import com.mmap.client.render.WorldMapManager;
import com.mmap.client.warp.WarpIconManager;
import com.mmap.client.warp.WarpPointStore;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side bootstrap. Fabric's ClientModInitializer / ClientTickEvents /
 * KeyBindingHelper / ClientPlayConnectionEvents are replaced by their Forge equivalents:
 * RegisterKeyMappingsEvent, TickEvent.ClientTickEvent and ClientPlayerNetworkEvent.LoggingOut.
 */
public class MMmapClient {
    private static MMmapClient instance;

    private RegionDataStore regionDataStore;
    private PolygonRegionStore polygonRegionStore;
    private WarpPointStore warpPointStore;
    private WarpIconManager warpIconManager;
    private ConfigManager configManager;
    private NetworkHandler networkHandler;
    private MinimapManager minimapManager;
    private WorldMapManager worldMapManager;
    private MapImageManager mapImageManager;
    private MythicMobStore mythicMobStore;
    private MobHeadManager mobHeadManager;

    private static KeyMapping openWorldMapKey;
    private static KeyMapping toggleMinimapKey;

    private MMmapClient() {
    }

    /** Called from the @Mod constructor when running on a physical client. */
    public static void init() {
        if (instance != null) {
            return;
        }
        instance = new MMmapClient();
        instance.setup();
    }

    private void setup() {
        MMmapMod.LOGGER.info("MMmap 客户端正在初始化...");
        this.configManager = new ConfigManager();
        this.regionDataStore = new RegionDataStore();
        this.polygonRegionStore = new PolygonRegionStore();
        this.warpPointStore = new WarpPointStore();
        this.warpIconManager = new WarpIconManager();
        this.mapImageManager = new MapImageManager();
        this.mythicMobStore = new MythicMobStore();
        this.mobHeadManager = new MobHeadManager();
        this.networkHandler = new NetworkHandler(this.regionDataStore, this.polygonRegionStore, this.warpPointStore);
        this.minimapManager = new MinimapManager(
                this.regionDataStore, this.polygonRegionStore, this.mapImageManager, this.warpPointStore, this.warpIconManager);
        this.worldMapManager = new WorldMapManager(
                this.regionDataStore, this.polygonRegionStore, this.mapImageManager, this.warpPointStore, this.warpIconManager);
        this.networkHandler.register();
        this.minimapManager.init();
        MMmapCommands.register();
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(this::onLoggingOut);
        MMmapMod.LOGGER.info("MMmap 客户端初始化完成");
        MMmapMod.LOGGER.info("地图图片目录: {}", this.mapImageManager.getImagesDir().getAbsolutePath());
    }

    /** Registered on the mod event bus by {@link MMmapMod}. */
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        openWorldMapKey = new KeyMapping("key.mmap.openworldmap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.mmap");
        toggleMinimapKey = new KeyMapping("key.mmap.toggleminimap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.mmap");
        event.register(openWorldMapKey);
        event.register(toggleMinimapKey);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (openWorldMapKey != null) {
            while (openWorldMapKey.consumeClick()) {
                this.worldMapManager.openWorldMapScreen();
            }
        }
        if (toggleMinimapKey != null) {
            while (toggleMinimapKey.consumeClick()) {
                this.minimapManager.toggleMinimap();
            }
        }
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        this.regionDataStore.clear();
        this.polygonRegionStore.clear();
        this.warpPointStore.clear();
        this.mapImageManager.clear();
        this.warpIconManager.clear();
        this.mythicMobStore.clear();
        this.mobHeadManager.clear();
        MMmapMod.LOGGER.info("已断开服务器连接，清空地图数据");
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public static MMmapClient getInstance() {
        return instance;
    }

    public RegionDataStore getRegionDataStore() {
        return this.regionDataStore;
    }

    public PolygonRegionStore getPolygonRegionStore() {
        return this.polygonRegionStore;
    }

    public NetworkHandler getNetworkHandler() {
        return this.networkHandler;
    }

    public MinimapManager getMinimapManager() {
        return this.minimapManager;
    }

    public WorldMapManager getWorldMapManager() {
        return this.worldMapManager;
    }

    public MapImageManager getMapImageManager() {
        return this.mapImageManager;
    }

    public WarpPointStore getWarpPointStore() {
        return this.warpPointStore;
    }

    public WarpIconManager getWarpIconManager() {
        return this.warpIconManager;
    }

    public MythicMobStore getMythicMobStore() {
        return this.mythicMobStore;
    }

    public MobHeadManager getMobHeadManager() {
        return this.mobHeadManager;
    }
}
