package com.mmap.client;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MMmapMod implements ModInitializer {
   public static final String MOD_ID = "mmap";
   public static final Logger LOGGER = LoggerFactory.getLogger("mmap");
   public static volatile long lastWarpDiagLog = 0L;

   public MMmapMod() {
   }

   public void onInitialize() {
      LOGGER.info("MMmap \u6a21\u7ec4\u6b63\u5728\u52a0\u8f7d...");
   }
}
