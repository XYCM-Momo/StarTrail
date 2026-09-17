package com.mmap.client.render;

import com.mmap.client.MMmapClient;
import com.mmap.client.config.ConfigManager;
import com.mmap.client.image.MapImageManager;
import com.mmap.client.mythic.MobHeadManager;
import com.mmap.client.mythic.MobOverlay;
import com.mmap.client.mythic.MythicMobStore;
import com.mmap.client.region.MapRegion;
import com.mmap.client.region.PolygonRegionStore;
import com.mmap.client.region.RegionDataStore;
import com.mmap.client.warp.WarpIconManager;
import com.mmap.client.warp.WarpOverlay;
import com.mmap.client.warp.WarpPointStore;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class MinimapRenderer {
   private final RegionDataStore regionDataStore;
   private final PolygonRegionStore polygonRegionStore;
   private final MinimapImageRenderer imageRenderer;
   private final WarpPointStore warpPointStore;
   private final WarpIconManager warpIconManager;
   private int mapSize = 128;
   private double zoom = 1.0;
   private int position = 0;

   public MinimapRenderer(
      RegionDataStore regionDataStore,
      PolygonRegionStore polygonRegionStore,
      MapImageManager mapImageManager,
      WarpPointStore warpPointStore,
      WarpIconManager warpIconManager
   ) {
      this.regionDataStore = regionDataStore;
      this.polygonRegionStore = polygonRegionStore;
      this.warpPointStore = warpPointStore;
      this.warpIconManager = warpIconManager;
      this.imageRenderer = new MinimapImageRenderer(mapImageManager);
   }

   public void render(GuiGraphics drawContext, Minecraft client, float tickDelta) {
      if (client.player != null && client.level != null) {
         ConfigManager.ClientConfig config = this.getConfig();
         String worldKey = client.level.dimension().location().toString();
         this.mapSize = config.minimap.size;
         this.zoom = config.minimap.zoom;
         this.position = config.minimap.position;
         double playerX = client.player.xo + (client.player.getX() - client.player.xo) * (double)tickDelta;
         double playerZ = client.player.zo + (client.player.getZ() - client.player.zo) * (double)tickDelta;
         int screenWidth = client.getWindow().getGuiScaledWidth();
         int screenHeight = client.getWindow().getGuiScaledHeight();
         int margin = 5;
         int mapX;
         int mapY;
         switch (this.position) {
            case 1:
               mapX = margin;
               mapY = margin;
               break;
            case 2:
               mapX = margin;
               mapY = screenHeight - this.mapSize - margin;
               break;
            case 3:
               mapX = screenWidth - this.mapSize - margin;
               mapY = screenHeight - this.mapSize - margin;
               break;
            default:
               mapX = screenWidth - this.mapSize - margin;
               mapY = margin;
         }

         int centerX = mapX + this.mapSize / 2;
         int centerY = mapY + this.mapSize / 2;
         int radius = this.mapSize / 2;
         PoseStack matrices = drawContext.pose();
         matrices.pushPose();
         this.drawFilledCircle(matrices, (float)centerX, (float)centerY, (float)(radius + 2), -871559910);
         this.imageRenderer.render(drawContext, client, mapX, mapY, this.mapSize, this.zoom, tickDelta, playerX, playerZ);
         if (config.minimap.showRegions) {
            this.renderRegionBorders(matrices, client, centerX, centerY, radius, playerX, playerZ);
         }

         if (config.minimap.showPolygons && client.player != null && client.level != null) {
            PolygonOverlay.render(
               worldKey, this.polygonRegionStore, playerX, playerZ, centerX, centerY, this.zoom, drawContext, client.font, 1.0F, true, centerX, centerY, radius
            );
         }

         if (config.minimap.showWarps && client.player != null && client.level != null) {
            int baseIconSize = 8;
            WarpOverlay.renderMinimap(
               worldKey,
               this.warpPointStore,
               this.warpIconManager,
               playerX,
               playerZ,
               centerX,
               centerY,
               this.zoom,
               drawContext,
               client.font,
               centerX,
               centerY,
               radius,
               baseIconSize,
               null
            );
         }

         if (client.player != null && client.level != null) {
            MythicMobStore mobStore = MMmapClient.getInstance().getMythicMobStore();
            MobHeadManager headMgr = MMmapClient.getInstance().getMobHeadManager();
            if (mobStore != null && headMgr != null) {
               MobOverlay.render(
                  worldKey,
                  mobStore,
                  headMgr,
                  playerX,
                  playerZ,
                  centerX,
                  centerY,
                  this.zoom,
                  drawContext,
                  true,
                  centerX,
                  centerY,
                  radius,
                  Math.max(6, this.mapSize / 12)
               );
            }
         }

         int faceSize = Math.max(6, this.mapSize / 12);
         if (client.player != null) {
            float yaw = client.player.getViewYRot(tickDelta);
            double fov = 70.0;

            try {
               fov = (double)((Integer)client.options.fov().get()).intValue();
            } catch (Exception var25) {
            }

            this.renderYawCone(matrices, centerX, centerY, radius, faceSize, yaw, (float)fov);
            this.renderPlayerFace(drawContext, client, centerX, centerY, faceSize);
         }

         this.renderOtherPlayerFaces(drawContext, client, centerX, centerY, radius, this.zoom, faceSize);
         this.drawCircleOutline(matrices, (float)centerX, (float)centerY, (float)radius, 1.0F, -1);
         matrices.popPose();
         if (client.player != null) {
            String coords = String.format("X: %.1f  Z: %.1f", client.player.getX(), client.player.getZ());
            Font tr = client.font;
            int textWidth = tr.width(coords);
            int textX = centerX - textWidth / 2;
            int textY = mapY + this.mapSize + 4;
            drawContext.fill(textX - 3, textY - 2, textX + textWidth + 3, textY + 9, Integer.MIN_VALUE);
            drawContext.drawString(tr, coords, textX, textY, 16776960);
         }

         if (config.minimap.showRegionNames) {
            this.renderRegionName(drawContext, client, centerX, mapY - 12);
         }

         RenderSystem.disableBlend();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
      }
   }

   private void renderRegionBorders(PoseStack matrices, Minecraft client, int centerX, int centerY, int radius, double playerX, double playerZ) {
      if (client.player != null && client.level != null) {
         int viewRange = (int)((double)radius / this.zoom);

         for (MapRegion region : this.regionDataStore.getAllRegions()) {
            if (region.intersects(
               (int)(playerX - (double)viewRange), (int)(playerZ - (double)viewRange), (int)(playerX + (double)viewRange), (int)(playerZ + (double)viewRange)
            )) {
               float x1 = (float)centerX + (float)(((double)region.getMinX() - playerX) * this.zoom);
               float z1 = (float)centerY + (float)(((double)region.getMinZ() - playerZ) * this.zoom);
               float x2 = (float)centerX + (float)(((double)(region.getMaxX() + 1) - playerX) * this.zoom);
               float z2 = (float)centerY + (float)(((double)(region.getMaxZ() + 1) - playerZ) * this.zoom);
               int fillColor = region.getFillColor();
               if ((fillColor >> 24 & 0xFF) > 0) {
                  this.drawFilledRect(matrices, x1, z1, x2, z2, fillColor);
               }

               float bw = Math.max(1.0F, region.getBorderWidth());
               int bc = region.getBorderColor();
               this.drawFilledRect(matrices, x1 - bw / 2.0F, z1 - bw / 2.0F, x2 + bw / 2.0F, z1 + bw / 2.0F, bc);
               this.drawFilledRect(matrices, x1 - bw / 2.0F, z2 - bw / 2.0F, x2 + bw / 2.0F, z2 + bw / 2.0F, bc);
               this.drawFilledRect(matrices, x1 - bw / 2.0F, z1 - bw / 2.0F, x1 + bw / 2.0F, z2 + bw / 2.0F, bc);
               this.drawFilledRect(matrices, x2 - bw / 2.0F, z1 - bw / 2.0F, x2 + bw / 2.0F, z2 + bw / 2.0F, bc);
            }
         }
      }
   }

   private void renderPlayerFace(GuiGraphics drawContext, Minecraft client, int centerX, int centerY, int size) {
      if (client.player != null) {
         PlayerFaceRenderer.drawFace(drawContext, client, client.player, centerX, centerY, size);
      }
   }

   private void renderYawCone(PoseStack matrices, int centerX, int centerY, int radius, int faceSize, float yaw, float totalAngle) {
      float yawRad = (float)Math.toRadians((double)yaw);
      float halfAngle = (float)Math.toRadians((double)(totalAngle / 2.0F));
      int segments = 24;
      float innerR = Math.max(1.0F, (float)faceSize / 2.0F - 2.0F);
      float outerR = (float)radius / 3.0F;
      float innerAlpha = 0.5647059F;
      float outerAlpha = 0.0627451F;
      float cr = 1.0F;
      float cg = 1.0F;
      float cb = 1.0F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f matrix = matrices.last().pose();
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      buffer.begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int i = 0; i <= segments; i++) {
         float angle = yawRad - halfAngle + 2.0F * halfAngle * (float)i / (float)segments;
         float sinA = (float)Math.sin((double)angle);
         float cosA = (float)Math.cos((double)angle);
         float ix = (float)centerX + -sinA * innerR;
         float iy = (float)centerY + cosA * innerR;
         float ox = (float)centerX + -sinA * outerR;
         float oy = (float)centerY + cosA * outerR;
         buffer.vertex(matrix, ix, iy, 0.0F).color(cr, cg, cb, innerAlpha).endVertex();
         buffer.vertex(matrix, ox, oy, 0.0F).color(cr, cg, cb, outerAlpha).endVertex();
      }

      tessellator.end();
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      this.addEdgeLine(buffer, matrix, centerX, centerY, yawRad - halfAngle, innerR, outerR);
      this.addEdgeLine(buffer, matrix, centerX, centerY, yawRad + halfAngle, innerR, outerR);
      tessellator.end();
      RenderSystem.enableCull();
   }

   private void addEdgeLine(BufferBuilder buffer, Matrix4f matrix, int cx, int cy, float angle, float innerR, float outerR) {
      float sinA = (float)Math.sin((double)angle);
      float cosA = (float)Math.cos((double)angle);
      float halfW = 1.0F;
      float ix = (float)cx + -sinA * innerR;
      float iy = (float)cy + cosA * innerR;
      float ox = (float)cx + -sinA * outerR;
      float oy = (float)cy + cosA * outerR;
      buffer.vertex(matrix, ix + cosA * halfW, iy + sinA * halfW, 0.0F).color(1, 1, 1, 1).endVertex();
      buffer.vertex(matrix, ix - cosA * halfW, iy - sinA * halfW, 0.0F).color(1, 1, 1, 1).endVertex();
      buffer.vertex(matrix, ox - cosA * halfW, oy - sinA * halfW, 0.0F).color(1, 1, 1, 1).endVertex();
      buffer.vertex(matrix, ox + cosA * halfW, oy + sinA * halfW, 0.0F).color(1, 1, 1, 1).endVertex();
   }

   private void renderOtherPlayerFaces(GuiGraphics drawContext, Minecraft client, int centerX, int centerY, int radius, double zoom, int faceSize) {
      if (client.player != null && client.level != null) {
         double playerX = client.player.getX();
         double playerZ = client.player.getZ();

         for (AbstractClientPlayer entity : client.level.players()) {
            if (entity instanceof AbstractClientPlayer && entity != client.player) {
               double dx = (entity.getX() - playerX) * zoom;
               double dz = (entity.getZ() - playerZ) * zoom;
               double distSq = dx * dx + dz * dz;
               int limitR = radius - faceSize / 2 - 1;
               if (!(distSq > (double)(limitR * limitR))) {
                  int sx = centerX + (int)Math.round(dx);
                  int sy = centerY + (int)Math.round(dz);
                  PlayerFaceRenderer.drawFace(drawContext, client, entity, sx, sy, faceSize);
               }
            }
         }
      }
   }

   private void renderPlayerArrowOld(PoseStack matrices, int centerX, int centerY, Minecraft client) {
      if (client.player != null) {
         float yaw = (float)Math.toRadians((double)client.player.getYRot());
         float size = 5.0F;
         float tipX = (float)centerX + (float)(-Math.sin((double)yaw) * (double)size);
         float tipY = (float)centerY + (float)(Math.cos((double)yaw) * (double)size);
         float leftX = (float)centerX + (float)(-Math.sin((double)yaw + 2.6) * (double)size * 0.7);
         float leftY = (float)centerY + (float)(Math.cos((double)yaw + 2.6) * (double)size * 0.7);
         float rightX = (float)centerX + (float)(-Math.sin((double)yaw - 2.6) * (double)size * 0.7);
         float rightY = (float)centerY + (float)(Math.cos((double)yaw - 2.6) * (double)size * 0.7);
         this.drawTriangle(matrices, tipX, tipY, leftX, leftY, rightX, rightY, -1);
         this.drawTriangleOutline(matrices, tipX, tipY, leftX, leftY, rightX, rightY, -16777216);
      }
   }

   private void renderRegionName(GuiGraphics drawContext, Minecraft client, int centerX, int y) {
      if (client.player != null && client.level != null) {
         String world = client.level.dimension().location().toString();
         List<MapRegion> regions = this.regionDataStore.getRegionsAt2D(world, (int)client.player.getX(), (int)client.player.getZ());
         if (!regions.isEmpty()) {
            MapRegion region = regions.get(0);
            String name = region.getName();
            Font tr = client.font;
            int textWidth = tr.width(name);
            int textX = centerX - textWidth / 2;
            drawContext.fill(textX - 3, y - 2, textX + textWidth + 3, y + 9, Integer.MIN_VALUE);
            drawContext.drawString(tr, name, textX, y, 16777215);
         }
      }
   }

   private void drawFilledRect(PoseStack matrices, float x1, float y1, float x2, float y2, int color) {
      Matrix4f matrix = matrices.last().pose();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator.getInstance();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, x1, y2, 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, x2, y2, 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, x2, y1, 0.0F).color(r, g, b, a).endVertex();
      tessellator.end();
   }

   private void drawFilledCircle(PoseStack matrices, float cx, float cy, float radius, int color) {
      Matrix4f matrix = matrices.last().pose();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int segments = 32;
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, cx, cy, 0.0F).color(r, g, b, a).endVertex();

      for (int i = 0; i <= segments; i++) {
         double angle = (Math.PI * 2) * (double)i / (double)segments;
         buffer.vertex(matrix, cx + (float)(Math.cos(angle) * (double)radius), cy + (float)(Math.sin(angle) * (double)radius), 0.0F)
            .color(r, g, b, a)
            .endVertex();
      }

      tessellator.end();
   }

   private void drawCircleOutline(PoseStack matrices, float cx, float cy, float radius, float lineWidth, int color) {
      Matrix4f matrix = matrices.last().pose();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      float halfWidth = lineWidth / 2.0F;
      float innerR = Math.max(0.0F, radius - halfWidth);
      float outerR = radius + halfWidth;
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int segments = 64;
      buffer.begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int i = 0; i <= segments; i++) {
         double angle = (Math.PI * 2) * (double)i / (double)segments;
         float cos = (float)Math.cos(angle);
         float sin = (float)Math.sin(angle);
         buffer.vertex(matrix, cx + cos * outerR, cy + sin * outerR, 0.0F).color(r, g, b, a).endVertex();
         buffer.vertex(matrix, cx + cos * innerR, cy + sin * innerR, 0.0F).color(r, g, b, a).endVertex();
      }

      tessellator.end();
   }

   private void drawTriangle(PoseStack matrices, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
      Matrix4f matrix = matrices.last().pose();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      buffer.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, x2, y2, 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, x3, y3, 0.0F).color(r, g, b, a).endVertex();
      tessellator.end();
      RenderSystem.disableBlend();
   }

   private void drawTriangleOutline(PoseStack matrices, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
      Matrix4f matrix = matrices.last().pose();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      RenderSystem.lineWidth(1.0F);
      buffer.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, x2, y2, 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, x3, y3, 0.0F).color(r, g, b, a).endVertex();
      buffer.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).endVertex();
      tessellator.end();
      RenderSystem.disableBlend();
   }

   private ConfigManager.ClientConfig getConfig() {
      ConfigManager cm = ConfigManager.getInstance();
      return cm != null ? cm.getConfig() : new ConfigManager.ClientConfig();
   }
}
