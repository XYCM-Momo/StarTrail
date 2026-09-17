package com.mmap.client.image;

import com.mmap.client.MMmapMod;
import com.mmap.client.config.ConfigManager;
import com.mmap.client.crypto.ImageCrypto;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.loading.FMLPaths;

public class MapImageManager {
   private final File imagesDir;
   private final Map<String, MapImage> images = new ConcurrentHashMap<>();
   private final Map<String, MapImageTexture> textures = new ConcurrentHashMap<>();

   public MapImageManager() {
      this.imagesDir = new File(FMLPaths.GAMEDIR.get().toFile(), "mmap/map");
      if (!this.imagesDir.exists()) {
         this.imagesDir.mkdirs();
         MMmapMod.LOGGER.info("\u521b\u5efa\u5730\u56fe\u56fe\u7247\u76ee\u5f55: {}", this.imagesDir.getAbsolutePath());
      }
   }

   public void setImages(List<MapImage> newImages) {
      Set<String> newIds = new HashSet<>();

      for (MapImage img : newImages) {
         newIds.add(img.getId());
      }

      Iterator<String> it = this.images.keySet().iterator();

      while (it.hasNext()) {
         String id = it.next();
         if (!newIds.contains(id)) {
            it.remove();
            MapImageTexture tex = this.textures.remove(id);
            if (tex != null) {
               tex.close();
            }

            MMmapMod.LOGGER.info("\u79fb\u9664\u5730\u56fe\u56fe\u7247\u7ed1\u5b9a: {}", id);
         }
      }

      for (MapImage img : newImages) {
         MapImage existing = this.images.get(img.getId());
         if (existing == null) {
            this.images.put(img.getId(), img);
            MMmapMod.LOGGER.info("\u65b0\u589e\u5730\u56fe\u56fe\u7247\u7ed1\u5b9a: {}", img);
         } else if (!this.sameBounds(existing, img) || !existing.getFileName().equals(img.getFileName())) {
            this.images.put(img.getId(), img);
            MapImageTexture tex = this.textures.remove(img.getId());
            if (tex != null) {
               tex.close();
            }

            MMmapMod.LOGGER.info("\u66f4\u65b0\u5730\u56fe\u56fe\u7247\u7ed1\u5b9a: {}", img);
         }
      }
   }

   public Collection<MapImage> getAllImages() {
      return Collections.unmodifiableCollection(this.images.values());
   }

   public List<MapImage> getImagesInWorld(String world) {
      List<MapImage> result = new ArrayList<>();

      for (MapImage img : this.images.values()) {
         if (img.getWorld().equals(world)) {
            result.add(img);
         }
      }

      return result;
   }

   public List<MapImage> getImagesIntersecting(String clientWorld, int x1, int z1, int x2, int z2) {
      List<MapImage> result = new ArrayList<>();
      String matchWorld = ConfigManager.getInstance().getCurrentWorld();
      if (matchWorld == null) {
         matchWorld = clientWorld;
      }

      for (MapImage img : this.images.values()) {
         if (worldMatches(img.getWorld(), matchWorld) && img.intersects(x1, z1, x2, z2)) {
            result.add(img);
         }
      }

      return result;
   }

   private static boolean worldMatches(String configWorld, String actualWorld) {
      if (configWorld == null || actualWorld == null) {
         return false;
      } else {
         return configWorld.equals(actualWorld) ? true : configWorld.startsWith(actualWorld) || actualWorld.startsWith(configWorld);
      }
   }

   public MapImageTexture getTexture(MapImage image) {
      if (image == null) {
         return null;
      } else {
         MapImageTexture tex = this.textures.get(image.getId());
         if (tex != null && tex.isReady()) {
            return tex;
         } else if (tex != null && tex.isLoadFailed()) {
            return null;
         } else {
            File file = ImageCrypto.resolveImageFile(this.imagesDir, image.getFileName());
            if (tex == null) {
               tex = new MapImageTexture(image, file);
               this.textures.put(image.getId(), tex);
            }

            return tex.ensureLoaded() ? tex : null;
         }
      }
   }

   public void clear() {
      for (MapImageTexture tex : this.textures.values()) {
         tex.close();
      }

      this.images.clear();
      this.textures.clear();
      MMmapMod.LOGGER.info("\u5df2\u6e05\u7a7a\u6240\u6709\u5730\u56fe\u56fe\u7247\u6570\u636e");
   }

   public File getImagesDir() {
      return this.imagesDir;
   }

   private boolean sameBounds(MapImage a, MapImage b) {
      return a.getMinX() == b.getMinX() && a.getMaxX() == b.getMaxX() && a.getMinZ() == b.getMinZ() && a.getMaxZ() == b.getMaxZ();
   }
}
