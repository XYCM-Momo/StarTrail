package com.mmap.client.mythic;

import com.mmap.client.MMmapMod;
import com.mmap.client.crypto.ImageCrypto;
import com.mmap.client.warp.WarpIconTexture;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.loading.FMLPaths;

public class MobHeadManager {
   private final File headsDir;
   private final Map<String, WarpIconTexture> textures = new ConcurrentHashMap<>();

   public MobHeadManager() {
      this.headsDir = new File(FMLPaths.GAMEDIR.get().toFile(), "mmap/mm");
      if (!this.headsDir.exists()) {
         this.headsDir.mkdirs();
         MMmapMod.LOGGER.info("\u521b\u5efa\u602a\u7269\u5934\u50cf\u76ee\u5f55: {}", this.headsDir.getAbsolutePath());
      }
   }

   public WarpIconTexture getTexture(String type) {
      if (type != null && !type.isEmpty()) {
         WarpIconTexture tex = this.textures.get(type);
         if (tex != null && tex.isReady()) {
            return tex;
         } else if (tex != null && tex.isLoadFailed()) {
            return null;
         } else {
            File file = ImageCrypto.resolveImageFile(this.headsDir, type + ".png");
            if (tex == null) {
               tex = new WarpIconTexture(type, file);
               this.textures.put(type, tex);
            }

            return tex.ensureLoaded() ? tex : null;
         }
      } else {
         return null;
      }
   }

   public File getHeadsDir() {
      return this.headsDir;
   }

   public void clear() {
      for (WarpIconTexture tex : this.textures.values()) {
         tex.close();
      }

      this.textures.clear();
   }
}
