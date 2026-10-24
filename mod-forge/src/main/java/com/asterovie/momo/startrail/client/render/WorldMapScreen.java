package com.asterovie.momo.startrail.client.render;

import com.asterovie.momo.startrail.client.StarTrailClient;
import com.asterovie.momo.startrail.client.StarTrailMod;
import com.asterovie.momo.startrail.client.config.ConfigManager;
import com.asterovie.momo.startrail.client.image.MapImage;
import com.asterovie.momo.startrail.client.image.MapImageManager;
import com.asterovie.momo.startrail.client.image.MapImageTexture;
import com.asterovie.momo.startrail.client.mythic.MobHeadManager;
import com.asterovie.momo.startrail.client.mythic.MobOverlay;
import com.asterovie.momo.startrail.client.mythic.MythicMobStore;
import com.asterovie.momo.startrail.client.nav.NavigationManager;
import com.asterovie.momo.startrail.client.region.MapRegion;
import com.asterovie.momo.startrail.client.region.PolygonRegionStore;
import com.asterovie.momo.startrail.client.region.RegionDataStore;
import com.asterovie.momo.startrail.client.warp.WarpIconManager;
import com.asterovie.momo.startrail.client.warp.WarpOverlay;
import com.asterovie.momo.startrail.client.warp.WarpPoint;
import com.asterovie.momo.startrail.client.warp.WarpPointStore;
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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
   // 右键选项栏（右键任意位置弹出，内含「传送到此处」，仅 OP 显示）
   private boolean contextMenuOpen = false;
   private int contextMenuTargetX;
   private int contextMenuTargetZ;
   // 设置界面（左下角齿轮打开，默认「大地图设置」页）
   private static final int SETTINGS_TAB_WORLDMAP = 0;
   private static final int SETTINGS_TAB_MINIMAP = 1;
   private boolean settingsOpen = false;
   private int settingsTab = SETTINGS_TAB_WORLDMAP;

   public WorldMapScreen(
      RegionDataStore regionDataStore,
      PolygonRegionStore polygonRegionStore,
      MapImageManager mapImageManager,
      WarpPointStore warpPointStore,
      WarpIconManager warpIconManager
   ) {
      super(Component.translatable("startrail.worldmap.title"));
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

      boolean mapVisible = !this.isMapHiddenBySettings();
      if (mapVisible) {
         this.renderMapImages(context);
         this.renderRegionOverlays(context);
         this.renderRegionLabels(context);
         this.renderPolygonOverlays(context);
         this.renderWarps(context);
         ConfigManager.ClientConfig rcfg = ConfigManager.getInstance().getConfig();
         if (rcfg != null && rcfg.worldmap != null && rcfg.worldmap.showPlayers) {
            this.renderPlayerMarker(context);
         }

         this.renderNavPath(context);
         this.renderUIOverlay(context, mouseX, mouseY);
      }

      super.render(context, mouseX, mouseY, delta);
      if (mapVisible) {
         this.renderSelectedWarpPanel(context, mouseX, mouseY);
         this.renderContextMenu(context, mouseX, mouseY);
      }

      if (this.settingsOpen) {
         this.renderSettingsOverlay(context, mouseX, mouseY);
      } else {
         this.renderGearButton(context, mouseX, mouseY);
      }
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
                  // 图片画在配置的矩形里：西/北边缘 = minX/minZ，东/南边缘 = maxX/maxZ，
                  // 纹理拉伸铺满这个矩形（1 像素 = 1 格的配置下不会有任何拉伸）。
                  //
                  // 旧代码这里写的是 minX + 1 / minZ + 1，导致整张图往东/南偏 1 格，
                  // 玩家只能在配置里把四个值统统 -1 来抵消。已修正为直接使用配置值。
                  // 与之配套：小地图那边（MinimapImageRenderer）本来就是按 minX..maxX 映射 UV 的，
                  // 所以修正后两个视图才会一致。
                  int spanX = image.getMaxX() - image.getMinX();
                  int spanZ = image.getMaxZ() - image.getMinZ();
                  if (spanX <= 0) {
                     // 配置的跨度不合理（比如只填了 minX/minZ），退回按图片像素尺寸铺
                     spanX = imageWidth;
                  }
                  if (spanZ <= 0) {
                     spanZ = imageHeight;
                  }
                  float screenX1 = this.worldToScreenX(image.getMinX());
                  float screenY1 = this.worldToScreenY(image.getMinZ());
                  float screenX2 = this.worldToScreenX(image.getMinX() + spanX);
                  float screenY2 = this.worldToScreenY(image.getMinZ() + spanZ);
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
      // 设置面板打开时同样要避开：名字是 drawString 画的，晚于面板底色的 fill 才栅格化，
      // 不主动跳过的话会「浮」在面板上方（与右键选项栏同一个成因）。
      int[] settingsRect = this.getSettingsHideRect();

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
            // 右键菜单打开时，不画落在菜单范围内的区域名（避免名字透过菜单干扰信息）
            int[] menuRect = this.contextMenuOpen ? this.getContextMenuRect() : null;
            if (menuRect != null
               && textX - 3 < menuRect[2]
               && textX + textWidth + 3 > menuRect[0]
               && textY - 2 < menuRect[3]
               && textY + 9 > menuRect[1]) {
               continue;
            }
            if (this.intersectsRect(settingsRect, textX - 3, textY - 2, textX + textWidth + 3, textY + 20)) {
               continue;
            }
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
            int[] panelRect = this.getOverlayHideRect();
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
            int[] menuRect = this.contextMenuOpen ? this.getContextMenuRect() : null;
            // 设置面板 / 传送点面板 / 右键菜单：名字落进去就不画（见 getOverlayHideRect 注释）
            int[] hideRect = this.mergeRect(this.getSelectedHideRect(), menuRect);
            int[] panelRect = this.getSelectedPanelRectOrNull();
            if (panelRect != null) {
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
                  panelRect[3],
                  hideRect
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
                  this.selectedWarpId,
                  hideRect
               );
            }

            MythicMobStore mobStore = StarTrailClient.getInstance().getMythicMobStore();
            MobHeadManager headMgr = StarTrailClient.getInstance().getMobHeadManager();
            if (mobStore != null && headMgr != null && cfg.worldmap.showMobs) {
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
      if (this.settingsOpen) {
         if (button == 0) {
            this.handleSettingsClick(mouseX, mouseY);
         }

         return true;
      } else if (button == 1) {
         // 右键：在鼠标位置弹出选项栏（传送到此处 / 关闭）
         this.openContextMenu(mouseX, mouseY);
         return true;
      } else if (button == 0) {
         int[] gear = this.getGearRect();
         if (mouseX >= (double)gear[0] && mouseX <= (double)gear[2] && mouseY >= (double)gear[1] && mouseY <= (double)gear[3]) {
            this.openSettings();
            return true;
         }

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
      if (this.contextMenuOpen) {
         int[][] menuBtnRects = this.getContextMenuButtonRects();

         for (int i = 0; i < menuBtnRects.length; i++) {
            int bx0 = menuBtnRects[i][0];
            int by0 = menuBtnRects[i][1];
            int bx1 = menuBtnRects[i][2];
            int by1 = menuBtnRects[i][3];
            if (mx >= bx0 && mx <= bx1 && my >= by0 && my <= by1) {
               this.handleContextMenuAction(i);
               return;
            }
         }

         if (this.isInsideContextMenu(mx, my)) {
            return;
         }

         // 点在选项栏外面：关闭选项栏，继续执行原有的选择逻辑
         this.contextMenuOpen = false;
      }

      if (this.selectedWarpId != null) {
         int[][] btnRects = this.getSelectedPanelButtonRects();

         for (int i = 0; i < btnRects.length; i++) {
            int bx0 = btnRects[i][0];
            int by0 = btnRects[i][1];
            int bx1 = btnRects[i][2];
            int by1 = btnRects[i][3];
            if (mx >= bx0 && mx <= bx1 && my >= by0 && my <= by1) {
               boolean teleport = i == 1;
               // 按钮点击音效（与右键选项栏统一）
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               this.sendWarpCommand(teleport);
               this.selectedWarpId = null;
               if (teleport) {
                  // 传送后直接关闭大地图
                  this.onClose();
               }

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

   /** 传送点详情面板矩形；没选中时返回 null（供渲染分支做 x0<0 判定）。 */
   private int[] getSelectedPanelRectOrNull() {
      return this.selectedWarpId != null ? this.getSelectedPanelRect() : null;
   }

   /**
    * 地图上「名字」需要避让的所有遮挡区，取这些矩形的并集包围盒。
    *
    * 为什么需要：区域名 / 传送点名都是 drawString 画的，在 1.20.1 的 GUI 里文字 glyph
    * 与 fill 分属不同批次、文字批次更晚提交，所以后画的面板底色盖不住文字，名字会浮在面板上方。
    * 右键选项栏早在 1.20.1 就踩过这个坑（见 renderContextMenu 的抬层注释），
    * 这里补上设置面板与传送点面板：名字落进这些矩形就不画。
    *
    * 返回 null 表示当前没有任何面板遮挡。
    */
   private int[] getOverlayHideRect() {
      int[] rect = this.getSelectedHideRect();
      if (this.contextMenuOpen) {
         rect = this.mergeRect(rect, this.getContextMenuRect());
      }

      return rect;
   }

   /** 传送点详情面板 / 设置面板的遮挡矩形（两者不会同时打开）。 */
   private int[] getSelectedHideRect() {
      if (this.selectedWarpId != null) {
         return this.getSelectedPanelRect();
      } else {
         return this.getSettingsHideRect();
      }
   }

   /** 遮罩矩形；小地图设置页把大地图整个藏起来后，没有需要避让的东西。 */
   private int[] getSettingsHideRect() {
      return this.settingsOpen && !this.isMapHiddenBySettings() ? this.getSettingsPanelRect() : null;
   }

   private int[] mergeRect(int[] a, int[] b) {
      if (a == null) {
         return b;
      } else if (b == null) {
         return a;
      } else {
         return new int[]{Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[2], b[2]), Math.max(a[3], b[3])};
      }
   }

   private boolean intersectsRect(int[] rect, int x0, int y0, int x1, int y1) {
      return rect != null && x0 < rect[2] && x1 > rect[0] && y0 < rect[3] && y1 > rect[1];
   }

   private int[][] getSelectedPanelButtonRects() {
      int[] p = this.getSelectedPanelRect();
      int x0 = p[0] + 8;
      int x1 = p[2] - 8;
      int bottomMargin = 6;
      int btnH = 16;
      int btnGap = 3;
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

            StarTrailMod.LOGGER.info("[WarpCmd] \u5df2\u53d1\u9001 {} \u547d\u4ee4: {}", teleport ? "\u4f20\u9001" : "\u5bfc\u822a", cmd);
         }
      }
   }

   // ==================== 设置界面（左下角齿轮入口） ====================

   private void playClickSound() {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   private boolean inRect(int mx, int my, int[] r) {
      return mx >= r[0] && mx <= r[2] && my >= r[1] && my <= r[3];
   }

   /** 设置界面里「小地图设置」页（tab=1）会盖住整张大地图，只留设置面板。 */
   private boolean isMapHiddenBySettings() {
      return this.settingsOpen && this.settingsTab == SETTINGS_TAB_MINIMAP;
   }

   private void openSettings() {
      this.playClickSound();
      this.settingsOpen = true;
      this.settingsTab = SETTINGS_TAB_WORLDMAP;
      this.contextMenuOpen = false;
      this.selectedWarpId = null;
   }

   private int[] getGearRect() {
      int size = 24;
      int x0 = 10;
      int y0 = this.height - size - 10;
      return new int[]{x0, y0, x0 + size, y0 + size};
   }

   private void renderGearButton(GuiGraphics context, int mouseX, int mouseY) {
      int[] r = this.getGearRect();
      boolean hover = this.inRect(mouseX, mouseY, r);
      context.fill(r[0], r[1], r[2], r[3], hover ? -14725525 : -535156156);
      this.drawPanelBorder(context, r[0], r[1], r[2], r[3], hover ? -16721665 : -13735025, 1);
      int cx = (r[0] + r[2]) / 2;
      int cy = (r[1] + r[3]) / 2;
      int toothColor = hover ? -1 : -2624001;
      int toothR = 7;

      for (int i = 0; i < 8; i++) {
         double ang = 0.7853981633974483 * (double)i;
         int tx = cx + (int)Math.round(Math.cos(ang) * (double)toothR);
         int ty = cy + (int)Math.round(Math.sin(ang) * (double)toothR);
         context.fill(tx - 1, ty - 1, tx + 2, ty + 2, toothColor);
      }

      context.fill(cx - 4, cy - 4, cx + 5, cy + 5, toothColor);
      context.fill(cx - 1, cy - 1, cx + 2, cy + 2, -871689696);
   }

   private int[] getSettingsPanelRect() {
      int w = Math.min(380, this.width - 40);
      int h = Math.min(280, this.height - 40);
      int x0 = (this.width - w) / 2;
      int y0 = (this.height - h) / 2;
      return new int[]{x0, y0, x0 + w, y0 + h};
   }

   private int[] getSettingsTabRect(int index) {
      int[] p = this.getSettingsPanelRect();
      int tabW = 108;
      int tabH = 18;
      int gap = 6;
      int x0 = p[0] + 12 + index * (tabW + gap);
      int y0 = p[1] + 24;
      return new int[]{x0, y0, x0 + tabW, y0 + tabH};
   }

   private int[] getSettingsBackRect() {
      int[] p = this.getSettingsPanelRect();
      int w = 64;
      int h = 18;
      return new int[]{p[2] - 12 - w, p[3] - 12 - h, p[2] - 12, p[3] - 12};
   }

   private int settingsRowY(int row) {
      int[] p = this.getSettingsPanelRect();
      return p[1] + 56 + row * 26;
   }

   private int[] getSettingsToggleRect(int row) {
      int[] p = this.getSettingsPanelRect();
      int w = 44;
      int h = 16;
      int y = this.settingsRowY(row);
      return new int[]{p[2] - 14 - w, y, p[2] - 14, y + h};
   }

   private int[][] getSettingsPosRects() {
      int[] p = this.getSettingsPanelRect();
      int cellW = 84;
      int cellH = 18;
      int gap = 6;
      int totalW = cellW * 2 + gap;
      int startX = p[0] + (p[2] - p[0] - totalW) / 2;
      int y = this.settingsRowY(1);
      int[][] rects = new int[4][4];
      rects[0] = new int[]{startX, y, startX + cellW, y + cellH};
      rects[1] = new int[]{startX + cellW + gap, y, startX + cellW + gap + cellW, y + cellH};
      rects[2] = new int[]{startX, y + cellH + gap, startX + cellW, y + cellH + gap + cellH};
      rects[3] = new int[]{startX + cellW + gap, y + cellH + gap, startX + cellW + gap + cellW, y + cellH + gap + cellH};
      return rects;
   }

   private int[] getSettingsSizeRect(boolean plus) {
      int[] p = this.getSettingsPanelRect();
      int w = 26;
      int h = 18;
      int y = this.settingsRowY(3);
      int x0 = plus ? p[2] - 14 - w : p[0] + 14;
      return new int[]{x0, y, x0 + w, y + h};
   }

   private int[] getSettingsShapeRect(int shape) {
      int[] p = this.getSettingsPanelRect();
      int w = 76;
      int h = 18;
      int gap = 6;
      int y = this.settingsRowY(5);
      // 按钮整体放在「形状」标签右侧，避免重叠
      int x0 = p[0] + 54 + shape * (w + gap);
      return new int[]{x0, y, x0 + w, y + h};
   }

   private void drawSettingsButton(GuiGraphics ctx, int[] r, String label, boolean active, boolean hover) {
      ctx.fill(r[0], r[1], r[2], r[3], hover ? -14725525 : -535156156);
      this.drawPanelBorder(ctx, r[0], r[1], r[2], r[3], active ? -16721665 : (hover ? -16721665 : -13735025), 1);
      Minecraft client = Minecraft.getInstance();
      Font tr = client.font;
      int lw = tr.width(label);
      int col = active ? -9502768 : -2624001;
      ctx.drawString(tr, label, r[0] + (r[2] - r[0] - lw) / 2, r[1] + (r[3] - r[1] - 9) / 2 + 1, col);
   }

   private void drawSettingsLabel(GuiGraphics ctx, String text, int x, int y) {
      ctx.drawString(Minecraft.getInstance().font, text, x, y, -9456679);
   }

   private void renderSettingsOverlay(GuiGraphics context, int mouseX, int mouseY) {
      // 抬到 tooltip 同层 z=400：地图上的名字是文字批次，晚于 fill 栅格化，
      // 不抬层时半透明底盖不住它们，名字会浮在设置面板上方。
      PoseStack pose = context.pose();
      pose.pushPose();
      pose.translate(0.0F, 0.0F, 400.0F);
      context.fill(0, 0, this.width, this.height, -1442840576);
      int[] p = this.getSettingsPanelRect();
      context.fill(p[0], p[1], p[2], p[3], -871689696);
      this.drawPanelBorder(context, p[0], p[1], p[2], p[3], -16721665, 1);
      Minecraft client = Minecraft.getInstance();
      Font tr = client.font;
      String title = "\u8bbe \u7f6e";
      int tw = tr.width(title);
      context.drawString(tr, title, p[0] + (p[2] - p[0] - tw) / 2, p[1] + 6, -6559489);

      for (int i = 0; i < 2; i++) {
         int[] t = this.getSettingsTabRect(i);
         boolean hover = this.inRect(mouseX, mouseY, t);
         this.drawSettingsButton(context, t, i == 0 ? "\u5927\u5730\u56fe\u8bbe\u7f6e" : "\u5c0f\u5730\u56fe\u8bbe\u7f6e", this.settingsTab == i, hover);
      }

      ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
      if (this.settingsTab == SETTINGS_TAB_WORLDMAP) {
         this.renderWorldMapSettings(context, mouseX, mouseY, cfg);
      } else {
         this.renderMinimapSettings(context, mouseX, mouseY, cfg);
      }

      int[] back = this.getSettingsBackRect();
      boolean backHover = this.inRect(mouseX, mouseY, back);
      this.drawSettingsButton(context, back, "\u8fd4\u56de", false, backHover);
      pose.popPose();
   }

   private void renderWorldMapSettings(GuiGraphics context, int mouseX, int mouseY, ConfigManager.ClientConfig cfg) {
      int[] p = this.getSettingsPanelRect();
      int labelX = p[0] + 14;
      boolean showPlayers = cfg.worldmap.showPlayers;
      boolean showMobs = cfg.worldmap.showMobs == null || cfg.worldmap.showMobs;
      int[] t0 = this.getSettingsToggleRect(0);
      int[] t1 = this.getSettingsToggleRect(1);
      this.drawSettingsLabel(context, "\u663e\u793a\u73a9\u5bb6\u5934\u50cf", labelX, t0[1] + 4);
      this.drawSettingsButton(context, t0, showPlayers ? "\u5f00" : "\u5173", showPlayers, this.inRect(mouseX, mouseY, t0));
      this.drawSettingsLabel(context, "\u663e\u793a\u602a\u7269\u5934\u50cf", labelX, t1[1] + 4);
      this.drawSettingsButton(context, t1, showMobs ? "\u5f00" : "\u5173", showMobs, this.inRect(mouseX, mouseY, t1));
   }

   private void renderMinimapSettings(GuiGraphics context, int mouseX, int mouseY, ConfigManager.ClientConfig cfg) {
      int[] p = this.getSettingsPanelRect();
      int labelX = p[0] + 14;
      this.drawSettingsLabel(context, "\u65b9\u4f4d", labelX, this.settingsRowY(0) + 4);
      int[] posVals = new int[]{1, 0, 2, 3};
      String[] posNames = new String[]{"\u5de6\u4e0a\u89d2", "\u53f3\u4e0a\u89d2", "\u5de6\u4e0b\u89d2", "\u53f3\u4e0b\u89d2"};
      int[][] posRects = this.getSettingsPosRects();

      for (int i = 0; i < 4; i++) {
         boolean active = cfg.minimap.position == posVals[i];
         this.drawSettingsButton(context, posRects[i], posNames[i], active, this.inRect(mouseX, mouseY, posRects[i]));
      }

      this.drawSettingsLabel(context, "\u5927\u5c0f: " + cfg.minimap.size, labelX + 40, this.settingsRowY(3) + 4);
      int[] minus = this.getSettingsSizeRect(false);
      int[] plus = this.getSettingsSizeRect(true);
      this.drawSettingsButton(context, minus, "-", false, this.inRect(mouseX, mouseY, minus));
      this.drawSettingsButton(context, plus, "+", false, this.inRect(mouseX, mouseY, plus));
      this.drawSettingsLabel(context, "\u5f62\u72b6", labelX, this.settingsRowY(5) + 4);
      int[] s0 = this.getSettingsShapeRect(0);
      int[] s1 = this.getSettingsShapeRect(1);
      this.drawSettingsButton(context, s0, "\u5706\u5f62", cfg.minimap.shape == 0, this.inRect(mouseX, mouseY, s0));
      this.drawSettingsButton(context, s1, "\u6b63\u65b9\u5f62", cfg.minimap.shape == 1, this.inRect(mouseX, mouseY, s1));
      // 本页会盖住整张大地图，明确告诉使用者怎么把它叫回来。
      // 窗口很矮时面板会被压缩，这一行放不下就干脆不画，免得压到「返回」按钮上。
      int hintY = this.settingsRowY(6) + 4;
      if (hintY + 9 <= p[3] - 12) {
         this.drawSettingsLabel(context, "\u672c\u9875\u5df2\u9690\u85cf\u5927\u5730\u56fe\uff0c\u5207\u56de\u300c\u5927\u5730\u56fe\u8bbe\u7f6e\u300d\u5373\u53ef\u6062\u590d", p[0] + 14, hintY);
      }
   }

   private void handleSettingsClick(double mouseX, double mouseY) {
      int mx = (int)mouseX;
      int my = (int)mouseY;
      int[] back = this.getSettingsBackRect();
      if (this.inRect(mx, my, back)) {
         this.playClickSound();
         this.settingsOpen = false;
         return;
      }

      for (int i = 0; i < 2; i++) {
         int[] t = this.getSettingsTabRect(i);
         if (this.inRect(mx, my, t)) {
            this.playClickSound();
            this.settingsTab = i;
            if (this.isMapHiddenBySettings()) {
               // 大地图被藏起来时，选项栏/传送点面板没有渲染的意义
               this.contextMenuOpen = false;
               this.selectedWarpId = null;
            }

            return;
         }
      }

      ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
      if (cfg == null) {
         return;
      }

      if (this.settingsTab == SETTINGS_TAB_WORLDMAP) {
         int[] t0 = this.getSettingsToggleRect(0);
         if (this.inRect(mx, my, t0)) {
            this.playClickSound();
            cfg.worldmap.showPlayers = !cfg.worldmap.showPlayers;
            this.saveSettings();
            return;
         }

         int[] t1 = this.getSettingsToggleRect(1);
         if (this.inRect(mx, my, t1)) {
            this.playClickSound();
            cfg.worldmap.showMobs = Boolean.valueOf(!(cfg.worldmap.showMobs == null || cfg.worldmap.showMobs));
            this.saveSettings();
            return;
         }
      } else {
         int[] posVals = new int[]{1, 0, 2, 3};
         int[][] posRects = this.getSettingsPosRects();

         for (int i = 0; i < 4; i++) {
            if (this.inRect(mx, my, posRects[i])) {
               this.playClickSound();
               cfg.minimap.position = posVals[i];
               this.saveSettings();
               return;
            }
         }

         int[] minus = this.getSettingsSizeRect(false);
         int[] plus = this.getSettingsSizeRect(true);
         if (this.inRect(mx, my, minus)) {
            this.playClickSound();
            cfg.minimap.size = Math.max(64, Math.min(256, cfg.minimap.size - 16));
            this.saveSettings();
            return;
         }

         if (this.inRect(mx, my, plus)) {
            this.playClickSound();
            cfg.minimap.size = Math.max(64, Math.min(256, cfg.minimap.size + 16));
            this.saveSettings();
            return;
         }

         int[] s0 = this.getSettingsShapeRect(0);
         int[] s1 = this.getSettingsShapeRect(1);
         if (this.inRect(mx, my, s0)) {
            this.playClickSound();
            cfg.minimap.shape = 0;
            this.saveSettings();
            return;
         }

         if (this.inRect(mx, my, s1)) {
            this.playClickSound();
            cfg.minimap.shape = 1;
            this.saveSettings();
         }
      }
   }

   private void saveSettings() {
      try {
         ConfigManager cm = ConfigManager.getInstance();
         if (cm != null) {
            cm.saveConfig();
         }
      } catch (Exception var2) {
         StarTrailMod.LOGGER.warn("\u4fdd\u5b58\u8bbe\u7f6e\u5931\u8d25", var2);
      }
   }

   // ==================== 右键选项栏 ====================

   private void openContextMenu(double mouseX, double mouseY) {
      // 大地图被设置页藏起来时不允许再弹选项栏（否则会对着看不见的地图导航/传送）
      if (this.isMapHiddenBySettings()) {
         return;
      }

      this.contextMenuOpen = true;
      this.contextMenuTargetX = (int)Math.floor(this.screenToWorldX(mouseX));
      this.contextMenuTargetZ = (int)Math.floor(this.screenToWorldY(mouseY));
      // 选项栏与传送点面板互斥，打开选项栏时收起面板
      this.selectedWarpId = null;
      StarTrailMod.LOGGER.info(
         "[WorldMap] \u6253\u5f00\u9009\u9879\u680f: \u5c4f\u5e55({}, {}) -> \u4e16\u754c\u5750\u6807({}, {})",
         new Object[]{mouseX, mouseY, this.contextMenuTargetX, this.contextMenuTargetZ}
      );
   }

   /** 是否允许大地图传送（由服务端握手包下发，仅 OP 为 true）。 */
   private boolean canUseMapTeleport() {
      StarTrailClient instance = StarTrailClient.getInstance();
      return instance != null && instance.canTeleportFromMap();
   }

   private void handleContextMenuAction(int index) {
      // 按钮点击音效
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      int teleportIndex = this.canUseMapTeleport() ? 1 : -1;
      if (index == 0) {
         // 导航至此（所有玩家可用；客户端本地生成路径 + 红石粒子）
         this.contextMenuOpen = false;
         this.startMapNavigation(this.contextMenuTargetX, this.contextMenuTargetZ);
      } else if (index == teleportIndex && this.canUseMapTeleport()) {
         this.sendTeleportCommand(this.contextMenuTargetX, this.contextMenuTargetZ);
         this.contextMenuOpen = false;
         // 传送后直接关闭大地图
         this.onClose();
      } else {
         this.contextMenuOpen = false;
      }
   }

   /** 大地图右键「导航至此」：交给客户端导航管理器，大地图保持打开。 */
   private void startMapNavigation(int targetX, int targetZ) {
      NavigationManager nav = StarTrailClient.getInstance().getNavigationManager();
      Minecraft client = Minecraft.getInstance();
      if (nav == null || client.player == null) {
         return;
      }

      if (nav.startNav((double)targetX, (double)targetZ)) {
         client.player.displayClientMessage(
            Component.literal("\u00a7a[\u5bfc\u822a] \u5df2\u5f00\u59cb\u5bfc\u822a\u5230 (" + targetX + ", " + targetZ + ")\uff0c\u8bf7\u8ddf\u968f\u7ea2\u77f3\u7c92\u5b50"), false
         );
      } else {
         client.player.displayClientMessage(Component.literal("\u00a7c[\u5bfc\u822a] \u5bfc\u822a\u5931\u8d25\uff1a" + nav.getLastError()), false);
      }
   }

   /** 在大地图上绘制当前导航路径（青色折线）+ 目标标记。 */
   private void renderNavPath(GuiGraphics context) {
      NavigationManager nav = StarTrailClient.getInstance().getNavigationManager();
      if (nav == null || !nav.isActive()) {
         return;
      }

      String world = ConfigManager.getInstance().getCurrentWorld();
      if (world == null || !world.equals(nav.getNavWorld())) {
         return;
      }

      List<double[]> path = nav.getPath();
      if (path.size() < 2) {
         return;
      }

      List<double[]> screenPts = new ArrayList<>();
      for (double[] p : path) {
         screenPts.add(new double[]{this.worldToScreenX(p[0]), this.worldToScreenY(p[2])});
      }

      PolylineRenderer.draw(context, screenPts, -16721665, 2.0F);
      int ix = (int)Math.round(this.worldToScreenX(nav.getTargetX()));
      int iz = (int)Math.round(this.worldToScreenY(nav.getTargetZ()));
      context.fill(ix - 4, iz - 4, ix + 5, iz - 1, -16721665);
      context.fill(ix - 4, iz + 2, ix + 5, iz + 5, -16721665);
      context.fill(ix - 4, iz - 1, ix - 1, iz + 2, -16721665);
      context.fill(ix + 2, iz - 1, ix + 5, iz + 2, -16721665);
   }

   private void sendTeleportCommand(int x, int z) {
      try {
         ConfigManager.ClientConfig cfg = ConfigManager.getInstance().getConfig();
         String template = cfg.tpCmd != null ? cfg.tpCmd.mapTeleport : "/startrail tp {x} {z}";
         String cmd = template.replace("{x}", String.valueOf(x)).replace("{z}", String.valueOf(z));
         Minecraft client = Minecraft.getInstance();
         if (client.player != null) {
            if (cmd.startsWith("/")) {
               client.player.connection.sendUnsignedCommand(cmd.substring(1));
            } else {
               client.player.connection.sendChat(cmd);
            }

            StarTrailMod.LOGGER.info("[MapTp] \u5df2\u53d1\u9001\u4f20\u9001\u547d\u4ee4: {}", cmd);
         }
      } catch (Exception var5) {
         StarTrailMod.LOGGER.warn("\u53d1\u9001\u4f20\u9001\u547d\u4ee4\u5931\u8d25", var5);
      }
   }

   private int[] getContextMenuRect() {
      int menuW = 132;
      int titleH = 14;
      int pad = 5;
      int rowH = 16;
      // 行数 = 导航至此 + (传送到此处，仅 OP) + 关闭
      int rows = (this.canUseMapTeleport() ? 1 : 0) + 2;
      int rowGap = 3;
      int menuH = titleH + pad + rows * rowH + (rows - 1) * rowGap + pad;
      // 菜单锚定世界坐标：平移/缩放地图时菜单跟随右键点到的位置一起移动，而不是钉在屏幕上
      double x0 = this.worldToScreenX((double)this.contextMenuTargetX) + 6.0;
      if (x0 + (double)menuW > (double)this.width) {
         x0 = (double)this.width - (double)menuW - 4.0;
      }

      if (x0 < 4.0) {
         x0 = 4.0;
      }

      double y0 = this.worldToScreenY((double)this.contextMenuTargetZ) + 6.0;
      if (y0 + (double)menuH > (double)this.height) {
         y0 = (double)this.height - (double)menuH - 4.0;
      }

      if (y0 < 4.0) {
         y0 = 4.0;
      }

      return new int[]{(int)Math.round(x0), (int)Math.round(y0), (int)Math.round(x0) + menuW, (int)Math.round(y0) + menuH};
   }

   private int[][] getContextMenuButtonRects() {
      int[] r = this.getContextMenuRect();
      int x0 = r[0] + 5;
      int x1 = r[2] - 5;
      int rowH = 16;
      int rowGap = 3;
      int titleH = 14;
      int pad = 5;
      int firstY = r[1] + titleH + pad;
      List<int[]> rects = new ArrayList<>();
      // 第一行：导航至此（所有玩家可用）
      rects.add(new int[]{x0, firstY, x1, firstY + rowH});
      firstY += rowH + rowGap;
      if (this.canUseMapTeleport()) {
         rects.add(new int[]{x0, firstY, x1, firstY + rowH});
         firstY += rowH + rowGap;
      }

      rects.add(new int[]{x0, firstY, x1, firstY + rowH});
      return rects.toArray(new int[0][]);
   }

   private boolean isInsideContextMenu(int mouseX, int mouseY) {
      int[] r = this.getContextMenuRect();
      return mouseX >= r[0] && mouseX <= r[2] && mouseY >= r[1] && mouseY <= r[3];
   }

   private void renderContextMenu(GuiGraphics context, int mouseX, int mouseY) {
      if (!this.contextMenuOpen) {
         return;
      }

      // 盖层：整体抬到 tooltip 同层 z=400。地图上的名字/文字 glyph 画在比 fill 更高的 z 上，
      // 菜单如果不抬层，不透明底只能盖住名字的黑色阴影框、盖不住文字本身。
      PoseStack pose = context.pose();
      pose.pushPose();
      pose.translate(0.0F, 0.0F, 400.0F);

      Minecraft client = Minecraft.getInstance();
      Font tr = client.font;
      int[] r = this.getContextMenuRect();
      int x0 = r[0];
      int y0 = r[1];
      int x1 = r[2];
      int y1 = r[3];
      int border = -16721665;
      // 不透明深蓝底：遮住底下的区域名/传送点名，避免透过半透明背景干扰菜单信息
      context.fill(x0, y0, x1, y1, -16051680);
      // 细边框
      this.drawPanelBorder(context, x0, y0, x1, y1, border, 1);
      // 标题与坐标（小号、青色系）
      context.drawString(tr, "\u9009\u9879", x0 + 5, y0 + 3, -6559489);
      String coordLine = String.format("X:%d Z:%d", this.contextMenuTargetX, this.contextMenuTargetZ);
      int coordW = tr.width(coordLine);
      context.drawString(tr, coordLine, x1 - 5 - coordW, y0 + 3, -9456679);
      int[][] btnRects = this.getContextMenuButtonRects();

      for (int i = 0; i < btnRects.length; i++) {
         int bx0 = btnRects[i][0];
         int by0 = btnRects[i][1];
         int bx1 = btnRects[i][2];
         int by1 = btnRects[i][3];
         boolean hover = mouseX >= bx0 && mouseX <= bx1 && mouseY >= by0 && mouseY <= by1;
         boolean isNav = i == 0;
         boolean isTeleport = this.canUseMapTeleport() && i == 1;
         context.fill(bx0, by0, bx1, by1, hover ? -14725525 : -535156156);
         this.drawPanelBorder(context, bx0, by0, bx1, by1, hover ? -16721665 : -13735025, 1);
         String label = isNav ? "\u5bfc\u822a\u81f3\u6b64" : (isTeleport ? "\u4f20\u9001\u5230\u6b64\u5904" : "\u5173\u95ed");
         int lw = tr.width(label);
         int labelColor = isNav ? -16721665 : (isTeleport ? -9502768 : -2624001);
         context.drawString(tr, label, bx0 + (bx1 - bx0 - lw) / 2, by0 + (by1 - by0 - 9) / 2 + 1, labelColor);
      }

      pose.popPose();
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      // 大地图被设置页藏起来时，滚轮不该再缩放一张看不见的地图
      if (this.isMapHiddenBySettings()) {
         return true;
      }

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
      if (this.settingsOpen && keyCode == 256) {
         // ESC 在设置界面里先关设置，再按一次才关大地图
         this.settingsOpen = false;
         return true;
      }

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
            StarTrailMod.LOGGER.warn("[StarTrail] persistCurrentScale: ConfigManager is null");
            return;
         }

         ConfigManager.ClientConfig cfg = cm.getConfig();
         if (cfg == null || cfg.worldmap == null) {
            return;
         }

         double v = this.targetScale > 0.0 ? this.targetScale : this.scale;
         cfg.worldmap.lastZoom = v;
         cm.saveConfig();
         StarTrailMod.LOGGER.info("[StarTrail] \u5df2\u6301\u4e45\u5316\u5927\u5730\u56fe\u7f29\u653e lastZoom=" + v);
      } catch (Exception var5) {
         StarTrailMod.LOGGER.warn("\u4fdd\u5b58\u5927\u5730\u56fe\u7f29\u653e\u5931\u8d25", var5);
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
            // 与右键选项栏统一：半透明深蓝底 + 霓虹青细边框
            context.fill(x0, y0, x1, y1, -871689696);
            this.drawPanelBorder(context, x0, y0, x1, y1, -16721665, 1);
            String title = w.getName();
            context.drawString(tr, title, x0 + 8, y0 + 6, -6559489);
            String descr = w.getDescr();
            int descrMaxWidth = x1 - x0 - 16;
            int descrY = y0 + 18;
            int lineH = 10;
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

               for (int i = 0; i < lines.size(); i++) {
                  context.drawString(tr, lines.get(i), x0 + 8, descrY + i * lineH, -7358516);
               }
            }

            String coordLine = String.format("X: %d  Y: %d  Z: %d", w.getX(), w.getY(), w.getZ());
            int[][] btnRects = this.getSelectedPanelButtonRects();
            int coordY = btnRects[0][1] - 11;
            context.drawString(tr, coordLine, x0 + 8, coordY, -9456679);

            for (int i = 0; i < 2; i++) {
               int bx0 = btnRects[i][0];
               int by0 = btnRects[i][1];
               int bx1 = btnRects[i][2];
               int by1 = btnRects[i][3];
               boolean hover = mouseX >= bx0 && mouseX <= bx1 && mouseY >= by0 && mouseY <= by1;
               boolean teleport = i == 1;
               context.fill(bx0, by0, bx1, by1, hover ? -14725525 : -535156156);
               this.drawPanelBorder(context, bx0, by0, bx1, by1, hover ? -16721665 : -13735025, 1);
               String label = teleport ? "\u4f20\u9001" : "\u5bfc\u822a";
               int lw = tr.width(label);
               context.drawString(tr, label, bx0 + (bx1 - bx0 - lw) / 2, by0 + (by1 - by0 - 9) / 2 + 1, teleport ? -9502768 : -2624001);
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
