package com.mmap.client.warp;

import com.mmap.client.MMmapMod;
import com.mmap.client.crypto.ImageCrypto;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.loading.FMLPaths;

public class WarpIconManager {
   private static final String SELECTED_SUFFIX = "(\u9009\u4e2d)";
   private final File imagesDir;
   private final Map<String, WarpIconTexture> textures = new ConcurrentHashMap<>();
   private final Map<String, WarpIconTexture> selectedTextures = new ConcurrentHashMap<>();

   public WarpIconManager() {
      this.imagesDir = new File(FMLPaths.GAMEDIR.get().toFile(), "mmap/legend");
      if (!this.imagesDir.exists()) {
         this.imagesDir.mkdirs();
         MMmapMod.LOGGER.info("\u521b\u5efa\u4f20\u9001\u70b9\u56fe\u6807\u76ee\u5f55: {}", this.imagesDir.getAbsolutePath());
      }
   }

   public WarpIconTexture getTexture(String iconKey) {
      if (iconKey == null || iconKey.isEmpty()) {
         iconKey = "\u4f20\u9001\u70b9";
      }

      File file = ImageCrypto.resolveImageFile(this.imagesDir, iconKey + ".png");
      return this.getOrLoad(this.textures, iconKey, file);
   }

   public WarpIconTexture getTextureSelected(String iconKey) {
      if (iconKey == null || iconKey.isEmpty()) {
         iconKey = "\u4f20\u9001\u70b9";
      }

      String selectedKey = iconKey + "(\u9009\u4e2d)";
      WarpIconTexture selectedTex = this.selectedTextures.get(selectedKey);
      if (selectedTex != null && selectedTex.isReady()) {
         return selectedTex;
      } else if (selectedTex != null && selectedTex.isLoadFailed()) {
         return this.getTexture(iconKey);
      } else {
         File file = ImageCrypto.resolveImageFile(this.imagesDir, selectedKey + ".png");
         if (!file.exists()) {
            WarpIconTexture sentinel = WarpIconTexture.missingSentinel(selectedKey, file);
            this.selectedTextures.put(selectedKey, sentinel);
            return this.getTexture(iconKey);
         } else {
            if (selectedTex == null) {
               selectedTex = new WarpIconTexture(selectedKey, file);
               this.selectedTextures.put(selectedKey, selectedTex);
            }

            return selectedTex.ensureLoaded() ? selectedTex : this.getTexture(iconKey);
         }
      }
   }

   private WarpIconTexture getOrLoad(Map<String, WarpIconTexture> cache, String key, File file) {
      WarpIconTexture tex = cache.get(key);
      if (tex != null && tex.isReady()) {
         return tex;
      } else if (tex != null && tex.isLoadFailed()) {
         return null;
      } else {
         if (tex == null) {
            if (!file.exists()) {
               File matched = findPrefixMatch(this.imagesDir, key);
               if (matched != null) {
                  file = matched;
                  MMmapMod.LOGGER.info("\u56fe\u6807\u524d\u7f00\u5339\u914d: '{}' -> '{}'", key, matched.getName());
               }
            }

            tex = new WarpIconTexture(key, file);
            cache.put(key, tex);
         }

         return tex.ensureLoaded() ? tex : null;
      }
   }

   private static File findPrefixMatch(File dir, String prefix) {
      if (dir != null && dir.isDirectory()) {
         File[] mmaps = dir.listFiles((d, name) -> name.endsWith(".mmap") && name.startsWith(prefix));
         if (mmaps != null && mmaps.length > 0) {
            return mmaps[0];
         } else {
            File[] pngs = dir.listFiles((d, name) -> name.endsWith(".png") && name.startsWith(prefix));
            return pngs != null && pngs.length != 0 ? pngs[0] : null;
         }
      } else {
         return null;
      }
   }

   public File getImagesDir() {
      return this.imagesDir;
   }

   public void clear() {
      for (WarpIconTexture tex : this.textures.values()) {
         tex.close();
      }

      this.textures.clear();

      for (WarpIconTexture tex : this.selectedTextures.values()) {
         tex.close();
      }

      this.selectedTextures.clear();
   }
}
