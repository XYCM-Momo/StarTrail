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
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class WorldMapScreen extends Screen {
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
      super(Component.translatable("mmap.worldmap.title"));
      this.regionDataStore = regionDataStore;
      this.polygonRegionStore = polygonRegionStore;
      this.mapImageManager = mapImageManager;
      this.warpPointStore = warpPointStore;
      this.warpIconManager = warpIconManager;
   }

   protected void init() {
      super.init();
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         this.cameraX = client.player.getX();
         this.cameraZ = client.player.getZ();
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

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -15921894);
      this.scale = this.scale + (this.targetScale - this.scale) * 0.15;
      if (this.followPlayer) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null) {
            this.cameraX = client.player.xo + (client.player.getX() - client.player.xo) * (double)delta;
            this.cameraZ = client.player.zo + (client.player.getZ() - client.player.zo) * (double)delta;
         }
      }

      this.renderMapImages(context);
      this.renderRegionOverlays(context);
      this.renderRegionLabels(context);
      this.renderPolygonOverlays(context);
      this.renderWarps(context);
      this.renderPlayerMarker(context);
      this.renderUIOverlay(context, mouseX, mouseY);
      super.render(context, mouseX, mouseY, delta);
      this.renderSelectedWarpPanel(context, mouseX, mouseY);
   }

   private void renderMapImages(GuiGraphics context) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null) {
         this.renderedImages = 0;
         String world = client.level.dimension().location().toString();
         double halfWorldWidth = (double)this.width / 2.0 / this.scale;
         double halfWorldHeight = (double)this.height / 2.0 / this.scale;
         int viewMinX = (int)Math.floor(this.cameraX - halfWorldWidth);
         int viewMaxX = (int)Math.ceil(this.cameraX + halfWorldWidth);
         int viewMinZ = (int)Math.floor(this.cameraZ - halfWorldHeight);
         int viewMaxZ = (int)Math.ceil(this.cameraZ + halfWorldHeight);
         List<MapImage> images = this.mapImageManager.getImagesIntersecting(world, viewMinX, viewMinZ, viewMaxX, viewMaxZ);
         if (!images.isEmpty()) {
            PoseStack matrices = context.pose();
            matrices.pushPose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Matrix4f matrix = matrices.last().pose();
            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();

            for (MapImage image : images) {
               MapImageTexture tex = this.mapImageManager.getTexture(image);
               if (tex != null && tex.isReady()) {
                  int imageWidth = tex.getImageWidth();
                  int imageHeight = tex.getImageHeight();
                  float screenX1 = this.worldToScreenX((double)((float)image.getMinX() + 1.0F));
                  float screenY1 = this.worldToScreenY((double)((float)image.getMinZ() + 1.0F));
                  float screenX2 = this.worldToScreenX((double)((float)(image.getMinX() + imageWidth) + 1.0F));
                  float screenY2 = this.worldToScreenY((double)((float)(image.getMinZ() + imageHeight) + 1.0F));
                  if (!(screenX2 < 0.0F) && !(screenX1 > (float)this.width) && !(screenY2 < 0.0F) && !(screenY1 > (float)this.height)) {
                     RenderSystem.bindTexture(tex.getGlTextureId());
                     RenderSystem.setShaderTexture(0, tex.getTextureId());
                     RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                     buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                     buffer.vertex(matrix, screenX1, screenY1, 0.0F).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
                     buffer.vertex(matrix, screenX1, screenY2, 0.0F).uv(0.0F, 1.0F).color(255, 255, 255, 255).endVertex();
                     buffer.vertex(matrix, screenX2, screenY2, 0.0F).uv(1.0F, 1.0F).color(255, 255, 255, 255).endVertex();
                     buffer.vertex(matrix, screenX2, screenY1, 0.0F).uv(1.0F, 0.0F).color(255, 255, 255, 255).endVertex();
                     tessellator.end();
                     this.renderedImages++;
                  }
               }
            }

            RenderSystem.disableBlend();
            matrices.popPose();
         }
      }
   }

   private void renderRegionOverlays(GuiGraphics context) {
      Collection<MapRegion> regions = this.regionDataStore.getAllRegions();
      this.renderedRegions = 0;
      if (!regions.isEmpty()) {
         PoseStack matrices = context.pose();
         matrices.pushPose();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         Matrix4f matrix = matrices.last().pose();
         Tesselator tessellator = Tesselator.getInstance();
         BufferBuilder buffer = tessellator.getBuilder();

         for (MapRegion region : regions) {
            float sx1 = this.worldToScreenX((double)region.getMinX() - 0.5);
            float sy1 = this.worldToScreenY((double)region.getMinZ() - 0.5);
            float sx2 = this.worldToScreenX((double)region.getMaxX() + 0.5);
            float sy2 = this.worldToScreenY((double)region.getMaxZ() + 0.5);
            if (!(sx2 < 0.0F) && !(sx1 > (float)this.width) && !(sy2 < 0.0F) && !(sy1 > (float)this.height)) {
               int fillColor = region.getFillColor();
               float fr = (float)(fillColor >> 16 & 0xFF) / 255.0F;
               float fg = (float)(fillColor >> 8 & 0xFF) / 255.0F;
               float fb = (float)(fillColor & 0xFF) / 255.0F;
               buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
               buffer.vertex(matrix, sx1, sy1, 0.0F).color(fr, fg, fb, 0.3F).endVertex();
               buffer.vertex(matrix, sx1, sy2, 0.0F).color(fr, fg, fb, 0.3F).endVertex();
               buffer.vertex(matrix, sx2, sy2, 0.0F).color(fr, fg, fb, 0.3F).endVertex();
               buffer.vertex(matrix, sx2, sy1, 0.0F).color(fr, fg, fb, 0.3F).endVertex();
               tessellator.end();
               int borderColor = region.getBorderColor();
               float br = (float)(borderColor >> 16 & 0xFF) / 255.0F;
               float bg = (float)(borderColor >> 8 & 0xFF) / 255.0F;
               float bb = (float)(borderColor & 0xFF) / 255.0F;
               RenderSystem.lineWidth(2.0F);
               buffer.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
               buffer.vertex(matrix, sx1, sy1, 0.0F).color(br, bg, bb, 1.0F).endVertex();
               buffer.vertex(matrix, sx2, sy1, 0.0F).color(br, bg, bb, 1.0F).endVertex();
               buffer.vertex(matrix, sx2, sy2, 0.0F).color(br, bg, bb, 1.0F).endVertex();
               buffer.vertex(matrix, sx1, sy2, 0.0F).color(br, bg, bb, 1.0F).endVertex();
               buffer.vertex(matrix, sx1, sy1, 0.0F).color(br, bg, bb, 1.0F).endVertex();
               tessellator.end();
               this.renderedRegions++;
            }
         }

         RenderSystem.disableBlend();
         matrices.popPose();
      }
   }

   private void renderRegionLabels(GuiGraphics context) {
      Minecraft client = Minecraft.getInstance();
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
            && !(screenX > (float)(this.width + 100))
            && !(screenY < -50.0F)
            && !(screenY > (float)(this.height + 50))
            && (
               panelRect == null
                  || !(screenX >= (float)panelRect[0])
                  || !(screenX <= (float)panelRect[2])
                  || !(screenY >= (float)panelRect[1])
                  || !(screenY <= (float)panelRect[3])
            )) {
            String label = region.getName();
            Font tr = Minecraft.getInstance().font;
            int textWidth = tr.width(label);
            int textX = (int)screenX - textWidth / 2;
            int textY = (int)screenY - 4;
            context.fill(textX - 3, textY - 2, textX + textWidth + 3, textY + 9, -1728053248);
            context.drawString(tr, label, textX, textY, 16777215);
            String typeText = region.getType().getDisplayName();
            int typeWidth = tr.width(typeText);
            int typeX = (int)screenX - typeWidth / 2;
            context.drawString(tr, typeText, typeX, textY + 11, region.getBorderColor());
         }
      }
   }

   private void renderPolygonOverlays(GuiGraphics context) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null && this.polygonRegionStore != null) {
         String world = client.level.dimension().location().toString();
         ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
         if (cfg.worldmap.showPolygons) {
            int[] panelRect = this.selectedWarpId != null ? this.getSelectedPanelRect() : null;
            if (panelRect != null) {
               PolygonOverlay.render(
                  world,
                  this.polygonRegionStore,
                  this.cameraX,
                  this.cameraZ,
                  this.width / 2,
                  this.height / 2,
                  this.scale,
                  context,
                  client.font,
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
                  world, this.polygonRegionStore, this.cameraX, this.cameraZ, this.width / 2, this.height / 2, this.scale, context, client.font, 1.0F
               );
            }
         }
      }
   }

   private void renderWarps(GuiGraphics context) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null && this.warpPointStore != null) {
         ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
         if (cfg.worldmap.showWarps) {
            String world = client.level.dimension().location().toString();
            int baseIconSize = 6;
            if (this.selectedWarpId != null) {
               int[] panelRect = this.getSelectedPanelRect();
               WarpOverlay.renderWorldMap(
                  world,
                  this.warpPointStore,
                  this.warpIconManager,
                  this.cameraX,
                  this.cameraZ,
                  this.width / 2,
                  this.height / 2,
                  this.scale,
                  context,
                  client.font,
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
                  this.width / 2,
                  this.height / 2,
                  this.scale,
                  context,
                  client.font,
                  baseIconSize,
                  this.selectedWarpId
               );
            }

            MythicMobStore mobStore = MMmapClient.getInstance().getMythicMobStore();
            MobHeadManager headMgr = MMmapClient.getInstance().getMobHeadManager();
            if (mobStore != null && headMgr != null) {
               MobOverlay.render(world, mobStore, headMgr, this.cameraX, this.cameraZ, this.width / 2, this.height / 2, this.scale, context, false, 0, 0, 0, 6);
            }
         }
      }
   }

   private void renderPlayerMarker(GuiGraphics context) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         int faceSize = Math.max(8, Math.round(8.0F * (float)Math.max(1.0, this.scale / 16.0)));
         faceSize = Math.min(faceSize, 48);
         if (client.level != null) {
            for (AbstractClientPlayer entity : client.level.players()) {
               if (entity instanceof AbstractClientPlayer && entity != client.player) {
                  float sx = this.worldToScreenX(entity.getX());
                  float sz = this.worldToScreenY(entity.getZ());
                  if (!(sx < (float)(-faceSize))
                     && !(sx > (float)(this.width + faceSize))
                     && !(sz < (float)(-faceSize))
                     && !(sz > (float)(this.height + faceSize))) {
                     PlayerFaceRenderer.drawFace(context, client, entity, (int)sx, (int)sz, faceSize);
                  }
               }
            }
         }

         float screenX = this.worldToScreenX(client.player.getX());
         float screenY = this.worldToScreenY(client.player.getZ());
         if (screenX >= (float)(-faceSize)
            && screenX <= (float)(this.width + faceSize)
            && screenY >= (float)(-faceSize)
            && screenY <= (float)(this.height + faceSize)) {
            PlayerFaceRenderer.drawFace(context, client, client.player, (int)screenX, (int)screenY, faceSize);
         }
      }
   }

   private void renderUIOverlay(GuiGraphics context, int mouseX, int mouseY) {
      Minecraft client = Minecraft.getInstance();
      Font tr = client.font;
      context.fill(0, 0, this.width, 14, -1728053248);
      int y = 3;
      String zoomText = String.format("\u7f29\u653e: %.1f%%", this.scale * 100.0);
      context.drawString(tr, zoomText, 5, y, 16777215);
      if (client.player != null) {
         String coordText = String.format("\u73a9\u5bb6: X=%.1f, Z=%.1f", client.player.getX(), client.player.getZ());
         context.drawString(tr, coordText, 5, y + 12, 16777215);
      }

      this.tickCounter++;
      double mouseWorldX = this.screenToWorldX((double)mouseX);
      double mouseWorldZ = this.screenToWorldY((double)mouseY);
      String mouseText = String.format("\u9f20\u6807: X=%.1f, Z=%.1f", mouseWorldX, mouseWorldZ);
      context.drawString(tr, mouseText, this.width / 2 - 50, y, 16755200);
      String helpText = "\u62d6\u52a8: \u5e73\u79fb | \u6eda\u8f6e: \u7f29\u653e | M/ESC: \u5173\u95ed | \u7a7a\u683c: \u8ddf\u968f\u73a9\u5bb6";
      int helpWidth = tr.width(helpText);
      context.drawString(tr, helpText, (this.width - helpWidth) / 2, this.height - 12, 11184810);
      MapRegion hoveredRegion = this.findRegionAt((int)mouseWorldX, (int)mouseWorldZ);
      if (hoveredRegion != null) {
         String hoverText = "> " + hoveredRegion.getName() + " (" + hoveredRegion.getType().getDisplayName() + ")";
         int hoverWidth = tr.width(hoverText);
         context.fill((this.width - hoverWidth) / 2 - 4, this.height - 26, (this.width + hoverWidth) / 2 + 4, this.height - 14, -1728053248);
         context.drawString(tr, hoverText, (this.width - hoverWidth) / 2, this.height - 24, 16777130);
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
      return (float)((double)this.width / 2.0 + (worldX - this.cameraX) * this.scale);
   }

   private float worldToScreenY(double worldZ) {
      return (float)((double)this.height / 2.0 + (worldZ - this.cameraZ) * this.scale);
   }

   private double screenToWorldX(double screenX) {
      return this.cameraX + (screenX - (double)this.width / 2.0) / this.scale;
   }

   private double screenToWorldY(double screenY) {
      return this.cameraZ + (screenY - (double)this.height / 2.0) / this.scale;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.pressDownX = mouseX;
         this.pressDownY = mouseY;
         this.pressDownMs = System.currentTimeMillis();
         this.pendingClick = true;
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
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

      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
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
         return super.mouseReleased(mouseX, mouseY, button);
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
      Minecraft client = Minecraft.getInstance();
      if (client.level != null && this.warpPointStore != null) {
         String world = client.level.dimension().location().toString();
         List<WarpPoint> warps = this.warpPointStore.getForWorld(world);
         if (warps.isEmpty()) {
            return null;
         } else {
            int originX = this.width / 2;
            int originY = this.height / 2;
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
                  if (sx >= -hitR && sx <= this.width + hitR && sy >= -hitR && sy <= this.height + hitR) {
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
      int x0 = this.width - w - 20;
      int y0 = this.height / 2 - h / 2;
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
         Minecraft client = Minecraft.getInstance();
         if (client.player != null) {
            if (cmd.startsWith("/")) {
               client.player.connection.sendUnsignedCommand(cmd.substring(1));
            } else {
               client.player.connection.sendChat(cmd);
            }

            MMmapMod.LOGGER.info("[WarpCmd] \u5df2\u53d1\u9001 {} \u547d\u4ee4: {}", teleport ? "\u4f20\u9001" : "\u5bfc\u822a", cmd);
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      double worldX = this.screenToWorldX(mouseX);
      double worldZ = this.screenToWorldY(mouseY);
      double zoomFactor = amount > 0.0 ? 1.2 : 0.8333333333333334;
      this.targetScale = Mth.clamp(this.targetScale * zoomFactor, 0.0625, 16.0);
      this.scale = this.targetScale;
      this.cameraX = worldX - (mouseX - (double)this.width / 2.0) / this.scale;
      this.cameraZ = worldZ - (mouseY - (double)this.height / 2.0) / this.scale;
      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
            this.targetScale = Mth.clamp(this.targetScale * 1.3, 0.0625, 16.0);
         }

         if (keyCode == 333 || keyCode == 269) {
            this.targetScale = Mth.clamp(this.targetScale / 1.3, 0.0625, 16.0);
         }

         if (keyCode == 32) {
            this.followPlayer = !this.followPlayer;
            if (this.followPlayer) {
               Minecraft client = Minecraft.getInstance();
               if (client.player != null) {
                  this.cameraX = client.player.getX();
                  this.cameraZ = client.player.getZ();
               }
            }
         }

         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         this.onClose();
         return true;
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      this.persistCurrentScale();
      super.onClose();
   }

   public void removed() {
      this.persistCurrentScale();
      super.removed();
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

   private void renderSelectedWarpPanel(GuiGraphics context, int mouseX, int mouseY) {
      if (this.selectedWarpId != null) {
         WarpPoint w = this.findWarpById(this.selectedWarpId);
         if (w == null) {
            this.selectedWarpId = null;
         } else {
            Minecraft client = Minecraft.getInstance();
            Font tr = client.font;
            int[] rect = this.getSelectedPanelRect();
            int x0 = rect[0];
            int y0 = rect[1];
            int x1 = rect[2];
            int y1 = rect[3];
            context.fill(x0, y0, x1, y1, -15066578);
            PoseStack m = context.pose();
            Matrix4f mat = m.last().pose();
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
            context.drawString(tr, title, x0 + 8, y0 + 6, -171);
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

                  while (!last.isEmpty() && tr.width(last + "...") > descrMaxWidth) {
                     last = last.substring(0, last.length() - 1);
                  }

                  lines.set(maxDescrLines - 1, last + "...");
               }

               int col = -3355444;

               for (int i = 0; i < lines.size(); i++) {
                  context.drawString(tr, lines.get(i), x0 + 8, descrY + i * lineH, col);
               }
            }

            String coordLine = String.format("X: %d  Y: %d  Z: %d", w.getX(), w.getY(), w.getZ());
            int[][] btnRects = this.getSelectedPanelButtonRects();
            int coordY = btnRects[0][1] - 6 - 9;
            context.drawString(tr, coordLine, x0 + 8, coordY, -5592406);
            String[] btnLabels = new String[]{"\u5bfc\u822a", "\u4f20\u9001"};
            int[] btnColors = new int[]{-12877066, -1096636};
            int[] btnHoverColors = new int[]{-10443270, -495247};

            for (int i = 0; i < 2; i++) {
               int bx0 = btnRects[i][0];
               int by0 = btnRects[i][1];
               int bx1 = btnRects[i][2];
               int by1 = btnRects[i][3];
               boolean hover = mouseX >= bx0 && mouseX <= bx1 && mouseY >= by0 && mouseY <= by1;
               context.fill(bx0, by0, bx1, by1, hover ? btnHoverColors[i] : btnColors[i]);
               this.drawPanelBorder(context, bx0, by0, bx1, by1, -1, 1);
               String label = btnLabels[i];
               int lw = tr.width(label);
               context.drawString(tr, label, bx0 + (bx1 - bx0 - lw) / 2, by0 + (by1 - by0 - 9) / 2, -1);
            }
         }
      }
   }

   private List<String> wrapText(String text, int maxWidth, Font tr) {
      List<String> out = new ArrayList<>();
      if (text != null && !text.isEmpty()) {
         int len = text.length();
         int start = 0;

         while (start < len) {
            int end;
            for (end = start + 1; end <= len; end++) {
               int w = tr.width(text.substring(start, end));
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

            while (end > start + 1 && tr.width(text.substring(start, end)) > maxWidth) {
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

   private void drawPanelBorder(GuiGraphics ctx, int x0, int y0, int x1, int y1, int color, int thickness) {
      ctx.fill(x0, y0, x1, y0 + thickness, color);
      ctx.fill(x0, y1 - thickness, x1, y1, color);
      ctx.fill(x0, y0, x0 + thickness, y1, color);
      ctx.fill(x1 - thickness, y0, x1, y1, color);
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
