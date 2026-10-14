package com.asterovie.momo.startrail.client;

import com.asterovie.momo.startrail.client.command.StarTrailCommands;
import com.asterovie.momo.startrail.client.config.ConfigManager;
import com.asterovie.momo.startrail.client.image.MapImageManager;
import com.asterovie.momo.startrail.client.mythic.MobHeadManager;
import com.asterovie.momo.startrail.client.mythic.MythicMobStore;
import com.asterovie.momo.startrail.client.nav.NavigationManager;
import com.asterovie.momo.startrail.client.network.NetworkHandler;
import com.asterovie.momo.startrail.client.region.PolygonRegionStore;
import com.asterovie.momo.startrail.client.region.RegionDataStore;
import com.asterovie.momo.startrail.client.render.MinimapManager;
import com.asterovie.momo.startrail.client.render.WorldMapManager;
import com.asterovie.momo.startrail.client.warp.WarpIconManager;
import com.asterovie.momo.startrail.client.warp.WarpPointStore;
import com.asterovie.momo.startrail.client.waypoint.WaypointStore;
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
public class StarTrailClient {
    private static StarTrailClient instance;

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
    private WaypointStore waypointStore;
    private NavigationManager navigationManager;

    /** 服务端握手包下发的「是否允许大地图传送」（仅 OP 为 true），用于决定右键菜单是否显示传送选项。 */
    private volatile boolean canTeleportFromMap = false;

    private static KeyMapping openWorldMapKey;
    private static KeyMapping toggleMinimapKey;

    private StarTrailClient() {
    }

    /** Called from the @Mod constructor when running on a physical client. */
    public static void init() {
        if (instance != null) {
            return;
        }
        instance = new StarTrailClient();
        instance.setup();
    }

    private void setup() {
        StarTrailMod.LOGGER.info("StarTrail 客户端正在初始化...");
        this.configManager = new ConfigManager();
        this.regionDataStore = new RegionDataStore();
        this.polygonRegionStore = new PolygonRegionStore();
        this.warpPointStore = new WarpPointStore();
        this.warpIconManager = new WarpIconManager();
        this.mapImageManager = new MapImageManager();
        this.mythicMobStore = new MythicMobStore();
        this.mobHeadManager = new MobHeadManager();
        this.waypointStore = new WaypointStore();
        this.navigationManager = new NavigationManager(this.waypointStore);
        this.waypointStore.reload();
        this.networkHandler = new NetworkHandler(this.regionDataStore, this.polygonRegionStore, this.warpPointStore);
        this.minimapManager = new MinimapManager(
                this.regionDataStore, this.polygonRegionStore, this.mapImageManager, this.warpPointStore, this.warpIconManager);
        this.worldMapManager = new WorldMapManager(
                this.regionDataStore, this.polygonRegionStore, this.mapImageManager, this.warpPointStore, this.warpIconManager);
        this.networkHandler.register();
        this.minimapManager.init();
        StarTrailCommands.register();
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(this::onLoggingOut);
        StarTrailMod.LOGGER.info("StarTrail 客户端初始化完成");
        StarTrailMod.LOGGER.info("地图图片目录: {}", this.mapImageManager.getImagesDir().getAbsolutePath());
    }

    /** Registered on the mod event bus by {@link StarTrailMod}. */
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        openWorldMapKey = new KeyMapping("key.startrail.openworldmap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.startrail");
        toggleMinimapKey = new KeyMapping("key.startrail.toggleminimap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.startrail");
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
        if (this.navigationManager != null) {
            this.navigationManager.tick();
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
        if (this.navigationManager != null) {
            this.navigationManager.stopNav();
        }
        StarTrailMod.LOGGER.info("已断开服务器连接，清空地图数据");
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public static StarTrailClient getInstance() {
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

    public WaypointStore getWaypointStore() {
        return this.waypointStore;
    }

    public NavigationManager getNavigationManager() {
        return this.navigationManager;
    }

    public boolean canTeleportFromMap() {
        return this.canTeleportFromMap;
    }

    public void setCanTeleportFromMap(boolean canTeleportFromMap) {
        this.canTeleportFromMap = canTeleportFromMap;
    }
}
