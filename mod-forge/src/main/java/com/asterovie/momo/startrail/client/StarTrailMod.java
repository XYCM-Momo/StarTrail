package com.asterovie.momo.startrail.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StarTrail - Forge 1.20.1 (47.4.10) port of the Fabric client mod.
 *
 * <p>The mod is client only: it renders a minimap / world map from region, warp, image
 * and MythicMobs data pushed by the StarTrail Bukkit server plugin over the {@code startrail:main}
 * plugin-message channel.
 */
@Mod(StarTrailMod.MOD_ID)
public class StarTrailMod {
    public static final String MOD_ID = "startrail";
    public static final Logger LOGGER = LoggerFactory.getLogger("startrail");
    public static volatile long lastWarpDiagLog = 0L;

    public StarTrailMod() {
        LOGGER.info("==================================================");
        LOGGER.info(" StarTrail · 星轨");
        LOGGER.info(" 本模组由 星野尘梦工作室 · 默默 制作");
        LOGGER.info("==================================================");
        LOGGER.info("StarTrail 模组正在加载...");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(StarTrailClient::onRegisterKeyMappings);
            StarTrailClient.init();
        }
    }
}
