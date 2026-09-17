package com.mmap.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MMmap - Forge 1.20.1 (47.4.10) port of the Fabric client mod.
 *
 * <p>The mod is client only: it renders a minimap / world map from region, warp, image
 * and MythicMobs data pushed by the MMmap Bukkit server plugin over the {@code mmap:main}
 * plugin-message channel.
 */
@Mod(MMmapMod.MOD_ID)
public class MMmapMod {
    public static final String MOD_ID = "mmap";
    public static final Logger LOGGER = LoggerFactory.getLogger("mmap");
    public static volatile long lastWarpDiagLog = 0L;

    public MMmapMod() {
        LOGGER.info("MMmap 模组正在加载...");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(MMmapClient::onRegisterKeyMappings);
            MMmapClient.init();
        }
    }
}
