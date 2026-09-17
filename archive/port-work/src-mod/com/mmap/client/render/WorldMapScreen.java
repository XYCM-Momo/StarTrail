package com.mmap.client.render;

import com.mmap.client.MMmapClient;
import com.mmap.client.MMmapMod;
import com.mmap.client.config.ConfigManager;
import com.mmap.client.image.MapImage;
import com.mmap.client.image.MapImageManager;
import com.mmap.client.image.MapImageTexture;
import com.mmap.client.mythic.MobHeadManager;
import com.mmap.client.mythic.MobOverlay;
import com.mmap.client.mythic.MythicMobStore;
import com.mmap.client.region.MapRegion;
import com.mmap.client.region.PolygonRegionStore;
import com.mmap.client.region.RegionDataStore;
import com.mmap.client.warp.WarpIconManager;
import com.mmap.client.warp.WarpOverlay;
import com.mmap.client.warp.WarpPoint;
import com.mmap.client.warp.WarpPointStore;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_757;
import net.minecraft.class_293.class_5596;
import org.joml.Matrix4f;

public class WorldMapScreen extends class_437 {
   private final RegionDataStore regionDataStore;
   private final PolygonRegionStore polygonRegionStore;
   private final MapImageManager mapImageManager;
   private final WarpPointStore warpPointStore;
   private final WarpIconManager warpIconManager;
   private double cameraX;
   private double cameraZ;
   private boolean followPlayer = true;
   private double scale = 1.0;
   private double targetScale = 16.0;
   private boolean isDragging = false;
   private double lastMouseX;
   private double lastMouseY;
   private int tickCounter = 0;
   private int renderedImages = 0;
   private int renderedRegions = 0;
   private String selectedWarpId = null;
   private static final double DRAG_THRESHOLD_PX = 4.0;
   private boolean pendingClick = false;
   private double pressDownX;
   private double pressDownY;
   private long pressDownMs;

   public WorldMapScreen(
      RegionDataStore regionDataStore,
      PolygonRegionStore polygonRegionStore,
      MapImageManager mapImageManager,
      WarpPointStore warpPointStore,
      WarpIconManager warpIconManager
   ) {
      super(class_2561.method_43471("mmap.worldmap.title"));
      this.regionDataStore = regionDataStore;
      this.polygonRegionStore = polygonRegionStore;
      this.mapImageManager = mapImageManager;
      this.warpPointStore = warpPointStore;
      this.warpIconManager = warpIconManager;
   }

   protected void method_25426() {
      super.method_25426();
      class_310 client = class_310.method_1551();
      if (client.field_1724 != null) {
         this.cameraX = client.field_1724.method_23317();
         this.cameraZ = client.field_1724.method_23321();
      }

      double initialScale = 2.0;

      try {
         ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
         if (cfg != null && cfg.worldmap != null) {
            double last = cfg.worldmap.lastZoom;
            double def = cfg.worldmap.defaultZoom;
            if (last > 0.0) {
               initialScale = last;
            } else if (def > 0.0) {
               initialScale = def;
            }
         }
      } catch (Exception var9) {
      }

      this.targetScale = initialScale;
      this.scale = initialScale;
      this.tickCounter = 0;
      this.followPlayer = true;
   }

   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
      context.method_25294(0, 0, this.field_22789, this.field_22790, -15921894);
      this.scale = this.scale + (this.targetScale - this.scale) * 0.15;
      if (this.followPlayer) {
         class_310 client = class_310.method_1551();
         if (client.field_1724 != null) {
            this.cameraX = client.field_1724.field_6014 + (client.field_1724.method_23317() - client.field_1724.field_6014) * (double)delta;
            this.cameraZ = client.field_1724.field_5969 + (client.field_1724.method_23321() - client.field_1724.field_5969) * (double)delta;
         }
      }

      this.renderMapImages(context);
      this.renderRegionOverlays(context);
      this.renderRegionLabels(context);
      this.renderPolygonOverlays(context);
      this.renderWarps(context);
      this.renderPlayerMarker(context);
      this.renderUIOverlay(context, mouseX, mouseY);
      super.method_25394(context, mouseX, mouseY, delta);
      this.renderSelectedWarpPanel(context, mouseX, mouseY);
   }

   private void renderMapImages(class_332 context) {
      class_310 client = class_310.method_1551();
      if (client.field_1687 != null) {
         this.renderedImages = 0;
         String world = client.field_1687.method_27983().method_29177().toString();
         double halfWorldWidth = (double)this.field_22789 / 2.0 / this.scale;
         double halfWorldHeight = (double)this.field_22790 / 2.0 / this.scale;
         int viewMinX = (int)Math.floor(this.cameraX - halfWorldWidth);
         int viewMaxX = (int)Math.ceil(this.cameraX + halfWorldWidth);
         int viewMinZ = (int)Math.floor(this.cameraZ - halfWorldHeight);
         int viewMaxZ = (int)Math.ceil(this.cameraZ + halfWorldHeight);
         List<MapImage> images = this.mapImageManager.getImagesIntersecting(world, viewMinX, viewMinZ, viewMaxX, viewMaxZ);
         if (!images.isEmpty()) {
            class_4587 matrices = context.method_51448();
            matrices.method_22903();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Matrix4f matrix = matrices.method_23760().method_23761();
            class_289 tessellator = class_289.method_1348();
            class_287 buffer = tessellator.method_1349();

            for (MapImage image : images) {
               MapImageTexture tex = this.mapImageManager.getTexture(image);
               if (tex != null && tex.isReady()) {
                  int imageWidth = tex.getImageWidth();
                  int imageHeight = tex.getImageHeight();
                  float screenX1 = this.worldToScreenX((double)((float)image.getMinX() + 1.0F));
                  float screenY1 = this.worldToScreenY((double)((float)image.getMinZ() + 1.0F));
                  float screenX2 = this.worldToScreenX((double)((float)(image.getMinX() + imageWidth) + 1.0F));
                  float screenY2 = this.worldToScreenY((double)((float)(image.getMinZ() + imageHeight) + 1.0F));
                  if (!(screenX2 < 0.0F) && !(screenX1 > (float)this.field_22789) && !(screenY2 < 0.0F) && !(screenY1 > (float)this.field_22790)) {
                     RenderSystem.bindTexture(tex.getGlTextureId());
                     RenderSystem.setShaderTexture(0, tex.getTextureId());
                     RenderSystem.setShader(class_757::method_34543);
                     buffer.method_1328(class_5596.field_27382, class_290.field_1575);
                     buffer.method_22918(matrix, screenX1, screenY1, 0.0F).method_22913(0.0F, 0.0F).method_1336(255, 255, 255, 255).method_1344();
                     buffer.method_22918(matrix, screenX1, screenY2, 0.0F).method_22913(0.0F, 1.0F).method_1336(255, 255, 255, 255).method_1344();
                     buffer.method_22918(matrix, screenX2, screenY2, 0.0F).method_22913(1.0F, 1.0F).method_1336(255, 255, 255, 255).method_1344();
                     buffer.method_22918(matrix, screenX2, screenY1, 0.0F).method_22913(1.0F, 0.0F).method_1336(255, 255, 255, 255).method_1344();
                     tessellator.method_1350();
                     this.renderedImages++;
                  }
               }
            }

            RenderSystem.disableBlend();
            matrices.method_22909();
         }
      }
   }

   private void renderRegionOverlays(class_332 context) {
      Collection<MapRegion> regions = this.regionDataStore.getAllRegions();
      this.renderedRegions = 0;
      if (!regions.isEmpty()) {
         class_4587 matrices = context.method_51448();
         matrices.method_22903();
         RenderSystem.setShader(class_757::method_34540);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         Matrix4f matrix = matrices.method_23760().method_23761();
         class_289 tessellator = class_289.method_1348();
         class_287 buffer = tessellator.method_1349();

         for (MapRegion region : regions) {
            float sx1 = this.worldToScreenX((double)region.getMinX() - 0.5);
            float sy1 = this.worldToScreenY((double)region.getMinZ() - 0.5);
            float sx2 = this.worldToScreenX((double)region.getMaxX() + 0.5);
            float sy2 = this.worldToScreenY((double)region.getMaxZ() + 0.5);
            if (!(sx2 < 0.0F) && !(sx1 > (float)this.field_22789) && !(sy2 < 0.0F) && !(sy1 > (float)this.field_22790)) {
               int fillColor = region.getFillColor();
               float fr = (float)(fillColor >> 16 & 0xFF) / 255.0F;
               float fg = (float)(fillColor >> 8 & 0xFF) / 255.0F;
               float fb = (float)(fillColor & 0xFF) / 255.0F;
               buffer.method_1328(class_5596.field_27382, class_290.field_1576);
               buffer.method_22918(matrix, sx1, sy1, 0.0F).method_22915(fr, fg, fb, 0.3F).method_1344();
               buffer.method_22918(matrix, sx1, sy2, 0.0F).method_22915(fr, fg, fb, 0.3F).method_1344();
               buffer.method_22918(matrix, sx2, sy2, 0.0F).method_22915(fr, fg, fb, 0.3F).method_1344();
               buffer.method_22918(matrix, sx2, sy1, 0.0F).method_22915(fr, fg, fb, 0.3F).method_1344();
               tessellator.method_1350();
               int borderColor = region.getBorderColor();
               float br = (float)(borderColor >> 16 & 0xFF) / 255.0F;
               float bg = (float)(borderColor >> 8 & 0xFF) / 255.0F;
               float bb = (float)(borderColor & 0xFF) / 255.0F;
               RenderSystem.lineWidth(2.0F);
               buffer.method_1328(class_5596.field_29345, class_290.field_1576);
               buffer.method_22918(matrix, sx1, sy1, 0.0F).method_22915(br, bg, bb, 1.0F).method_1344();
               buffer.method_22918(matrix, sx2, sy1, 0.0F).method_22915(br, bg, bb, 1.0F).method_1344();
               buffer.method_22918(matrix, sx2, sy2, 0.0F).method_22915(br, bg, bb, 1.0F).method_1344();
               buffer.method_22918(matrix, sx1, sy2, 0.0F).method_22915(br, bg, bb, 1.0F).method_1344();
               buffer.method_22918(matrix, sx1, sy1, 0.0F).method_22915(br, bg, bb, 1.0F).method_1344();
               tessellator.method_1350();
               this.renderedRegions++;
            }
         }

         RenderSystem.disableBlend();
         matrices.method_22909();
      }
   }

   private void renderRegionLabels(class_332 context) {
      class_310 client = class_310.method_1551();
      Collection<MapRegion> regions = this.regionDataStore.getAllRegions();
      int[] panelRect = null;
      if (this.selectedWarpId != null) {
         panelRect = this.getSelectedPanelRect();
      }

      for (MapRegion region : regions) {
         double centerX = (double)(region.getMinX() + region.getMaxX()) / 2.0;
         double centerZ = (double)(region.getMinZ() + region.getMaxZ()) / 2.0;
         float screenX = this.worldToScreenX(centerX);
         float screenY = this.worldToScreenY(centerZ);
         if (!(screenX < -100.0F)
            && !(screenX > (float)(this.field_22789 + 100))
            && !(screenY < -50.0F)
            && !(screenY > (float)(this.field_22790 + 50))
            && (
               panelRect == null
                  || !(screenX >= (float)panelRect[0])
                  || !(screenX <= (float)panelRect[2])
                  || !(screenY >= (float)panelRect[1])
                  || !(screenY <= (float)panelRect[3])
            )) {
            String label = region.getName();
            class_327 tr = class_310.method_1551().field_1772;
            int textWidth = tr.method_1727(label);
            int textX = (int)screenX - textWidth / 2;
            int textY = (int)screenY - 4;
            context.method_25294(textX - 3, textY - 2, textX + textWidth + 3, textY + 9, -1728053248);
            context.method_25303(tr, label, textX, textY, 16777215);
            String typeText = region.getType().getDisplayName();
            int typeWidth = tr.method_1727(typeText);
            int typeX = (int)screenX - typeWidth / 2;
            context.method_25303(tr, typeText, typeX, textY + 11, region.getBorderColor());
         }
      }
   }

   private void renderPolygonOverlays(class_332 context) {
      class_310 client = class_310.method_1551();
      if (client.field_1687 != null && this.polygonRegionStore != null) {
         String world = client.field_1687.method_27983().method_29177().toString();
         ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
         if (cfg.worldmap.showPolygons) {
            int[] panelRect = this.selectedWarpId != null ? this.getSelectedPanelRect() : null;
            if (panelRect != null) {
               PolygonOverlay.render(
                  world,
                  this.polygonRegionStore,
                  this.cameraX,
                  this.cameraZ,
                  this.field_22789 / 2,
                  this.field_22790 / 2,
                  this.scale,
                  context,
                  client.field_1772,
                  1.0F,
                  false,
                  0,
                  0,
                  0,
                  panelRect[0],
                  panelRect[1],
                  panelRect[2],
                  panelRect[3]
               );
            } else {
               PolygonOverlay.render(
                  world,
                  this.polygonRegionStore,
                  this.cameraX,
                  this.cameraZ,
                  this.field_22789 / 2,
                  this.field_22790 / 2,
                  this.scale,
                  context,
                  client.field_1772,
                  1.0F
               );
            }
         }
      }
   }

   private void renderWarps(class_332 context) {
      class_310 client = class_310.method_1551();
      if (client.field_1687 != null && this.warpPointStore != null) {
         ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
         if (cfg.worldmap.showWarps) {
            String world = client.field_1687.method_27983().method_29177().toString();
            int baseIconSize = 6;
            if (this.selectedWarpId != null) {
               int[] panelRect = this.getSelectedPanelRect();
               WarpOverlay.renderWorldMap(
                  world,
                  this.warpPointStore,
                  this.warpIconManager,
                  this.cameraX,
                  this.cameraZ,
                  this.field_22789 / 2,
                  this.field_22790 / 2,
                  this.scale,
                  context,
                  client.field_1772,
                  baseIconSize,
                  this.selectedWarpId,
                  panelRect[0],
                  panelRect[1],
                  panelRect[2],
                  panelRect[3]
               );
            } else {
               WarpOverlay.renderWorldMap(
                  world,
                  this.warpPointStore,
                  this.warpIconManager,
                  this.cameraX,
                  this.cameraZ,
                  this.field_22789 / 2,
                  this.field_22790 / 2,
                  this.scale,
                  context,
                  client.field_1772,
                  baseIconSize,
                  this.selectedWarpId
               );
            }

            MythicMobStore mobStore = MMmapClient.getInstance().getMythicMobStore();
            MobHeadManager headMgr = MMmapClient.getInstance().getMobHeadManager();
            if (mobStore != null && headMgr != null) {
               MobOverlay.render(
                  world, mobStore, headMgr, this.cameraX, this.cameraZ, this.field_22789 / 2, this.field_22790 / 2, this.scale, context, false, 0, 0, 0, 6
               );
            }
         }
      }
   }

   private void renderPlayerMarker(class_332 context) {
      class_310 client = class_310.method_1551();
      if (client.field_1724 != null) {
         int faceSize = Math.max(8, Math.round(8.0F * (float)Math.max(1.0, this.scale / 16.0)));
         faceSize = Math.min(faceSize, 48);
         if (client.field_1687 != null) {
            for (class_742 entity : client.field_1687.method_18456()) {
               if (entity instanceof class_742 && entity != client.field_1724) {
                  float sx = this.worldToScreenX(entity.method_23317());
                  float sz = this.worldToScreenY(entity.method_23321());
                  if (!(sx < (float)(-faceSize))
                     && !(sx > (float)(this.field_22789 + faceSize))
                     && !(sz < (float)(-faceSize))
                     && !(sz > (float)(this.field_22790 + faceSize))) {
                     PlayerFaceRenderer.drawFace(context, client, entity, (int)sx, (int)sz, faceSize);
                  }
               }
            }
         }

         float screenX = this.worldToScreenX(client.field_1724.method_23317());
         float screenY = this.worldToScreenY(client.field_1724.method_23321());
         if (screenX >= (float)(-faceSize)
            && screenX <= (float)(this.field_22789 + faceSize)
            && screenY >= (float)(-faceSize)
            && screenY <= (float)(this.field_22790 + faceSize)) {
            PlayerFaceRenderer.drawFace(context, client, client.field_1724, (int)screenX, (int)screenY, faceSize);
         }
      }
   }

   private void renderUIOverlay(class_332 context, int mouseX, int mouseY) {
      class_310 client = class_310.method_1551();
      class_327 tr = client.field_1772;
      context.method_25294(0, 0, this.field_22789, 14, -1728053248);
      int y = 3;
      String zoomText = String.format("\u7f29\u653e: %.1f%%", this.scale * 100.0);
      context.method_25303(tr, zoomText, 5, y, 16777215);
      if (client.field_1724 != null) {
         String coordText = String.format("\u73a9\u5bb6: X=%.1f, Z=%.1f", client.field_1724.method_23317(), client.field_1724.method_23321());
         context.method_25303(tr, coordText, 5, y + 12, 16777215);
      }

      this.tickCounter++;
      double mouseWorldX = this.screenToWorldX((double)mouseX);
      double mouseWorldZ = this.screenToWorldY((double)mouseY);
      String mouseText = String.format("\u9f20\u6807: X=%.1f, Z=%.1f", mouseWorldX, mouseWorldZ);
      context.method_25303(tr, mouseText, this.field_22789 / 2 - 50, y, 16755200);
      String helpText = "\u62d6\u52a8: \u5e73\u79fb | \u6eda\u8f6e: \u7f29\u653e | M/ESC: \u5173\u95ed | \u7a7a\u683c: \u8ddf\u968f\u73a9\u5bb6";
      int helpWidth = tr.method_1727(helpText);
      context.method_25303(tr, helpText, (this.field_22789 - helpWidth) / 2, this.field_22790 - 12, 11184810);
      MapRegion hoveredRegion = this.findRegionAt((int)mouseWorldX, (int)mouseWorldZ);
      if (hoveredRegion != null) {
         String hoverText = "> " + hoveredRegion.getName() + " (" + hoveredRegion.getType().getDisplayName() + ")";
         int hoverWidth = tr.method_1727(hoverText);
         context.method_25294(
            (this.field_22789 - hoverWidth) / 2 - 4, this.field_22790 - 26, (this.field_22789 + hoverWidth) / 2 + 4, this.field_22790 - 14, -1728053248
         );
         context.method_25303(tr, hoverText, (this.field_22789 - hoverWidth) / 2, this.field_22790 - 24, 16777130);
      }
   }

   private MapRegion findRegionAt(int x, int z) {
      for (MapRegion region : this.regionDataStore.getAllRegions()) {
         if (region.contains2D(x, z)) {
            return region;
         }
      }

      return null;
   }

   private float worldToScreenX(double worldX) {
      return (float)((double)this.field_22789 / 2.0 + (worldX - this.cameraX) * this.scale);
   }

   private float worldToScreenY(double worldZ) {
      return (float)((double)this.field_22790 / 2.0 + (worldZ - this.cameraZ) * this.scale);
   }

   private double screenToWorldX(double screenX) {
      return this.cameraX + (screenX - (double)this.field_22789 / 2.0) / this.scale;
   }

   private double screenToWorldY(double screenY) {
      return this.cameraZ + (screenY - (double)this.field_22790 / 2.0) / this.scale;
   }

   public boolean method_25402(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.pressDownX = mouseX;
         this.pressDownY = mouseY;
         this.pressDownMs = System.currentTimeMillis();
         this.pendingClick = true;
         return true;
      } else {
         return super.method_25402(mouseX, mouseY, button);
      }
   }

   public boolean method_25403(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (button == 0) {
         double offX = mouseX - this.pressDownX;
         double offY = mouseY - this.pressDownY;
         double distSq = offX * offX + offY * offY;
         if (this.pendingClick && distSq >= 16.0) {
            this.pendingClick = false;
            if (this.selectedWarpId != null) {
               this.selectedWarpId = null;
            }

            this.followPlayer = false;
            this.isDragging = true;
            this.cameraX = this.cameraX - deltaX / this.scale;
            this.cameraZ = this.cameraZ - deltaY / this.scale;
            return true;
         }

         if (this.isDragging && button == 0) {
            this.cameraX = this.cameraX - deltaX / this.scale;
            this.cameraZ = this.cameraZ - deltaY / this.scale;
            return true;
         }
      }

      return super.method_25403(mouseX, mouseY, button, deltaX, deltaY);
   }

   public boolean method_25406(double mouseX, double mouseY, int button) {
      if (button == 0) {
         boolean wasDragging = this.isDragging;
         this.isDragging = false;
         if (this.pendingClick) {
            this.pendingClick = false;
            this.handleSingleClick(mouseX, mouseY);
            return true;
         } else {
            return true;
         }
      } else {
         return super.method_25406(mouseX, mouseY, button);
      }
   }

   private void handleSingleClick(double mouseX, double mouseY) {
      int mx = (int)mouseX;
      int my = (int)mouseY;
      if (this.selectedWarpId != null) {
         int[][] btnRects = this.getSelectedPanelButtonRects();

         for (int i = 0; i < btnRects.length; i++) {
            int bx0 = btnRects[i][0];
            int by0 = btnRects[i][1];
            int bx1 = btnRects[i][2];
            int by1 = btnRects[i][3];
            if (mx >= bx0 && mx <= bx1 && my >= by0 && my <= by1) {
               this.sendWarpCommand(i == 1);
               this.selectedWarpId = null;
               return;
            }
         }

         if (this.isClickInsideSelectedPanel(mx, my)) {
            return;
         }
      }

      WarpPoint hit = this.findWarpAtScreen(mx, my);
      if (hit != null) {
         if (hit.getId().equals(this.selectedWarpId)) {
            this.selectedWarpId = null;
         } else {
            this.selectedWarpId = hit.getId();
         }
      } else {
         if (this.selectedWarpId != null) {
            this.selectedWarpId = null;
         }
      }
   }

   private WarpPoint findWarpAtScreen(int mouseX, int mouseY) {
      class_310 client = class_310.method_1551();
      if (client.field_1687 != null && this.warpPointStore != null) {
         String world = client.field_1687.method_27983().method_29177().toString();
         List<WarpPoint> warps = this.warpPointStore.getForWorld(world);
         if (warps.isEmpty()) {
            return null;
         } else {
            int originX = this.field_22789 / 2;
            int originY = this.field_22790 / 2;
            int baseIconSize = 6;
            double threshold = ConfigManager.getInstance().getConfig().worldmap.warpIconHideThreshold;
            if (this.scale < threshold) {
               return null;
            } else {
               double s = Math.max(0.0625, Math.min(16.0, this.scale));
               int iconSize = (int)Math.round((double)baseIconSize * s);
               iconSize = Math.max(10, Math.min(128, iconSize));
               int hitR = iconSize / 2 + 4;
               int hitR2 = hitR * hitR;
               WarpPoint best = null;
               int bestDist2 = Integer.MAX_VALUE;

               for (WarpPoint w : warps) {
                  int sx = (int)Math.round((double)originX + ((double)w.getX() - this.cameraX) * this.scale);
                  int sy = (int)Math.round((double)originY + ((double)w.getZ() - this.cameraZ) * this.scale);
                  if (sx >= -hitR && sx <= this.field_22789 + hitR && sy >= -hitR && sy <= this.field_22790 + hitR) {
                     int dx = mouseX - sx;
                     int dy = mouseY - sy;
                     int d2 = dx * dx + dy * dy;
                     if (d2 <= hitR2 && d2 < bestDist2) {
                        bestDist2 = d2;
                        best = w;
                     }
                  }
               }

               return best;
            }
         }
      } else {
         return null;
      }
   }

   private boolean isClickInsideSelectedPanel(int mouseX, int mouseY) {
      if (this.selectedWarpId == null) {
         return false;
      } else {
         int[] rect = this.getSelectedPanelRect();
         return mouseX >= rect[0] && mouseX <= rect[2] && mouseY >= rect[1] && mouseY <= rect[3];
      }
   }

   private int[] getSelectedPanelRect() {
      int w = 220;
      int h = 150;
      int x0 = this.field_22789 - w - 20;
      int y0 = this.field_22790 / 2 - h / 2;
      return new int[]{x0, y0, x0 + w, y0 + h};
   }

   private int[][] getSelectedPanelButtonRects() {
      int[] p = this.getSelectedPanelRect();
      int x0 = p[0] + 8;
      int x1 = p[2] - 8;
      int bottomMargin = 8;
      int btnH = 22;
      int btnGap = 4;
      int upY0 = p[3] - bottomMargin - btnH - btnGap - btnH;
      int upY1 = p[3] - bottomMargin - btnH - btnGap;
      int dnY0 = p[3] - bottomMargin - btnH;
      int dnY1 = p[3] - bottomMargin;
      return new int[][]{{x0, upY0, x1, upY1}, {x0, dnY0, x1, dnY1}};
   }

   private void sendWarpCommand(boolean teleport) {
      if (this.selectedWarpId != null) {
         ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
         String template = teleport ? cfg.warpCmd.teleport : cfg.warpCmd.navigate;
         String cmd = template.replace("{id}", this.selectedWarpId);
         class_310 client = class_310.method_1551();
         if (client.field_1724 != null) {
            if (cmd.startsWith("/")) {
               client.field_1724.field_3944.method_45731(cmd.substring(1));
            } else {
               client.field_1724.field_3944.method_45729(cmd);
            }

            MMmapMod.LOGGER.info("[WarpCmd] \u5df2\u53d1\u9001 {} \u547d\u4ee4: {}", teleport ? "\u4f20\u9001" : "\u5bfc\u822a", cmd);
         }
      }
   }

   public boolean method_25401(double mouseX, double mouseY, double amount) {
      double worldX = this.screenToWorldX(mouseX);
      double worldZ = this.screenToWorldY(mouseY);
      double zoomFactor = amount > 0.0 ? 1.2 : 0.8333333333333334;
      this.targetScale = class_3532.method_15350(this.targetScale * zoomFactor, 0.0625, 16.0);
      this.scale = this.targetScale;
      this.cameraX = worldX - (mouseX - (double)this.field_22789 / 2.0) / this.scale;
      this.cameraZ = worldZ - (mouseY - (double)this.field_22790 / 2.0) / this.scale;
      return true;
   }

   public boolean method_25404(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && keyCode != 77) {
         double panSpeed = 50.0 / this.scale;
         if (keyCode == 263) {
            this.cameraX -= panSpeed;
         }

         if (keyCode == 262) {
            this.cameraX += panSpeed;
         }

         if (keyCode == 265) {
            this.cameraZ -= panSpeed;
         }

         if (keyCode == 264) {
            this.cameraZ += panSpeed;
         }

         if (keyCode == 334 || keyCode == 270) {
            this.targetScale = class_3532.method_15350(this.targetScale * 1.3, 0.0625, 16.0);
         }

         if (keyCode == 333 || keyCode == 269) {
            this.targetScale = class_3532.method_15350(this.targetScale / 1.3, 0.0625, 16.0);
         }

         if (keyCode == 32) {
            this.followPlayer = !this.followPlayer;
            if (this.followPlayer) {
               class_310 client = class_310.method_1551();
               if (client.field_1724 != null) {
                  this.cameraX = client.field_1724.method_23317();
                  this.cameraZ = client.field_1724.method_23321();
               }
            }
         }

         return super.method_25404(keyCode, scanCode, modifiers);
      } else {
         this.method_25419();
         return true;
      }
   }

   public boolean method_25421() {
      return false;
   }

   public void method_25419() {
      this.persistCurrentScale();
      super.method_25419();
   }

   public void method_25432() {
      this.persistCurrentScale();
      super.method_25432();
   }

   private void persistCurrentScale() {
      try {
         ConfigManager cm = ConfigManager.getInstance();
         if (cm == null) {
            MMmapMod.LOGGER.warn("[MMmap] persistCurrentScale: ConfigManager is null");
            return;
         }

         ConfigManager.ClientConfig cfg = cm.getConfig();
         if (cfg == null || cfg.worldmap == null) {
            return;
         }

         double v = this.targetScale > 0.0 ? this.targetScale : this.scale;
         cfg.worldmap.lastZoom = v;
         cm.saveConfig();
         MMmapMod.LOGGER.info("[MMmap] \u5df2\u6301\u4e45\u5316\u5927\u5730\u56fe\u7f29\u653e lastZoom=" + v);
      } catch (Exception var5) {
         MMmapMod.LOGGER.warn("\u4fdd\u5b58\u5927\u5730\u56fe\u7f29\u653e\u5931\u8d25", var5);
      }
   }

   public double getCameraX() {
      return this.cameraX;
   }

   public double getCameraZ() {
      return this.cameraZ;
   }

   public double getScale() {
      return this.scale;
   }

   public double getTargetScale() {
      return this.targetScale;
   }

   public void setCameraX(double cameraX) {
      this.cameraX = cameraX;
   }

   public void setCameraZ(double cameraZ) {
      this.cameraZ = cameraZ;
   }

   public void setScale(double scale) {
      this.scale = scale;
      this.targetScale = scale;
   }

   private void renderSelectedWarpPanel(class_332 context, int mouseX, int mouseY) {
      if (this.selectedWarpId != null) {
         WarpPoint w = this.findWarpById(this.selectedWarpId);
         if (w == null) {
            this.selectedWarpId = null;
         } else {
            class_310 client = class_310.method_1551();
            class_327 tr = client.field_1772;
            int[] rect = this.getSelectedPanelRect();
            int x0 = rect[0];
            int y0 = rect[1];
            int x1 = rect[2];
            int y1 = rect[3];
            context.method_25294(x0, y0, x1, y1, -15066578);
            class_4587 m = context.method_51448();
            Matrix4f mat = m.method_23760().method_23761();
            System.out
               .println(
                  "[DEBUG-PANEL-V177] x0="
                     + x0
                     + " y0="
                     + y0
                     + " x1="
                     + x1
                     + " y1="
                     + y1
                     + " | matrix=[m00="
                     + mat.m00()
                     + " m11="
                     + mat.m11()
                     + " m30="
                     + mat.m30()
                     + " m31="
                     + mat.m31()
                     + "]"
               );
            this.drawPanelBorder(context, x0, y0, x1, y1, -86, 1);
            String title = w.getName();
            context.method_25303(tr, title, x0 + 8, y0 + 6, -171);
            String descr = w.getDescr();
            int descrMaxWidth = x1 - x0 - 16;
            int descrY = y0 + 20;
            int lineH = 11;
            int maxDescrLines = 4;
            if (descr != null && !descr.isEmpty()) {
               List<String> lines = this.wrapText(descr, descrMaxWidth, tr);
               if (lines.size() > maxDescrLines) {
                  lines = lines.subList(0, maxDescrLines);
                  String last = lines.get(maxDescrLines - 1);

                  while (!last.isEmpty() && tr.method_1727(last + "...") > descrMaxWidth) {
                     last = last.substring(0, last.length() - 1);
                  }

                  lines.set(maxDescrLines - 1, last + "...");
               }

               int col = -3355444;

               for (int i = 0; i < lines.size(); i++) {
                  context.method_25303(tr, lines.get(i), x0 + 8, descrY + i * lineH, col);
               }
            }

            String coordLine = String.format("X: %d  Y: %d  Z: %d", w.getX(), w.getY(), w.getZ());
            int[][] btnRects = this.getSelectedPanelButtonRects();
            int coordY = btnRects[0][1] - 6 - 9;
            context.method_25303(tr, coordLine, x0 + 8, coordY, -5592406);
            String[] btnLabels = new String[]{"\u5bfc\u822a", "\u4f20\u9001"};
            int[] btnColors = new int[]{-12877066, -1096636};
            int[] btnHoverColors = new int[]{-10443270, -495247};

            for (int i = 0; i < 2; i++) {
               int bx0 = btnRects[i][0];
               int by0 = btnRects[i][1];
               int bx1 = btnRects[i][2];
               int by1 = btnRects[i][3];
               boolean hover = mouseX >= bx0 && mouseX <= bx1 && mouseY >= by0 && mouseY <= by1;
               context.method_25294(bx0, by0, bx1, by1, hover ? btnHoverColors[i] : btnColors[i]);
               this.drawPanelBorder(context, bx0, by0, bx1, by1, -1, 1);
               String label = btnLabels[i];
               int lw = tr.method_1727(label);
               context.method_25303(tr, label, bx0 + (bx1 - bx0 - lw) / 2, by0 + (by1 - by0 - 9) / 2, -1);
            }
         }
      }
   }

   private List<String> wrapText(String text, int maxWidth, class_327 tr) {
      List<String> out = new ArrayList<>();
      if (text != null && !text.isEmpty()) {
         int len = text.length();
         int start = 0;

         while (start < len) {
            int end;
            for (end = start + 1; end <= len; end++) {
               int w = tr.method_1727(text.substring(start, end));
               if (w > maxWidth) {
                  if (end == start + 1) {
                     end++;
                  }
                  break;
               }
            }

            if (end > len) {
               end = len;
            }

            while (end > start + 1 && tr.method_1727(text.substring(start, end)) > maxWidth) {
               end--;
            }

            out.add(text.substring(start, end));
            start = end;
         }

         return out;
      } else {
         return out;
      }
   }

   private void drawPanelBorder(class_332 ctx, int x0, int y0, int x1, int y1, int color, int thickness) {
      ctx.method_25294(x0, y0, x1, y0 + thickness, color);
      ctx.method_25294(x0, y1 - thickness, x1, y1, color);
      ctx.method_25294(x0, y0, x0 + thickness, y1, color);
      ctx.method_25294(x1 - thickness, y0, x1, y1, color);
   }

   private WarpPoint findWarpById(String id) {
      if (this.warpPointStore != null && id != null) {
         for (WarpPoint w : this.warpPointStore.getAll()) {
            if (id.equals(w.getId())) {
               return w;
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
