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
import java.util.List;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_757;
import net.minecraft.class_293.class_5596;
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

   public void render(class_332 drawContext, class_310 client, float tickDelta) {
      if (client.field_1724 != null && client.field_1687 != null) {
         ConfigManager.ClientConfig config = this.getConfig();
         String worldKey = client.field_1687.method_27983().method_29177().toString();
         this.mapSize = config.minimap.size;
         this.zoom = config.minimap.zoom;
         this.position = config.minimap.position;
         double playerX = client.field_1724.field_6014 + (client.field_1724.method_23317() - client.field_1724.field_6014) * (double)tickDelta;
         double playerZ = client.field_1724.field_5969 + (client.field_1724.method_23321() - client.field_1724.field_5969) * (double)tickDelta;
         int screenWidth = client.method_22683().method_4486();
         int screenHeight = client.method_22683().method_4502();
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
         class_4587 matrices = drawContext.method_51448();
         matrices.method_22903();
         this.drawFilledCircle(matrices, (float)centerX, (float)centerY, (float)(radius + 2), -871559910);
         this.imageRenderer.render(drawContext, client, mapX, mapY, this.mapSize, this.zoom, tickDelta, playerX, playerZ);
         if (config.minimap.showRegions) {
            this.renderRegionBorders(matrices, client, centerX, centerY, radius, playerX, playerZ);
         }

         if (config.minimap.showPolygons && client.field_1724 != null && client.field_1687 != null) {
            PolygonOverlay.render(
               worldKey,
               this.polygonRegionStore,
               playerX,
               playerZ,
               centerX,
               centerY,
               this.zoom,
               drawContext,
               client.field_1772,
               1.0F,
               true,
               centerX,
               centerY,
               radius
            );
         }

         if (config.minimap.showWarps && client.field_1724 != null && client.field_1687 != null) {
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
               client.field_1772,
               centerX,
               centerY,
               radius,
               baseIconSize,
               null
            );
         }

         if (client.field_1724 != null && client.field_1687 != null) {
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
         if (client.field_1724 != null) {
            float yaw = client.field_1724.method_5705(tickDelta);
            double fov = 70.0;

            try {
               fov = (double)((Integer)client.field_1690.method_41808().method_41753()).intValue();
            } catch (Exception var25) {
            }

            this.renderYawCone(matrices, centerX, centerY, radius, faceSize, yaw, (float)fov);
            this.renderPlayerFace(drawContext, client, centerX, centerY, faceSize);
         }

         this.renderOtherPlayerFaces(drawContext, client, centerX, centerY, radius, this.zoom, faceSize);
         this.drawCircleOutline(matrices, (float)centerX, (float)centerY, (float)radius, 1.0F, -1);
         matrices.method_22909();
         if (client.field_1724 != null) {
            String coords = String.format("X: %.1f  Z: %.1f", client.field_1724.method_23317(), client.field_1724.method_23321());
            class_327 tr = client.field_1772;
            int textWidth = tr.method_1727(coords);
            int textX = centerX - textWidth / 2;
            int textY = mapY + this.mapSize + 4;
            drawContext.method_25294(textX - 3, textY - 2, textX + textWidth + 3, textY + 9, Integer.MIN_VALUE);
            drawContext.method_25303(tr, coords, textX, textY, 16776960);
         }

         if (config.minimap.showRegionNames) {
            this.renderRegionName(drawContext, client, centerX, mapY - 12);
         }

         RenderSystem.disableBlend();
         RenderSystem.setShader(class_757::method_34540);
      }
   }

   private void renderRegionBorders(class_4587 matrices, class_310 client, int centerX, int centerY, int radius, double playerX, double playerZ) {
      if (client.field_1724 != null && client.field_1687 != null) {
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

   private void renderPlayerFace(class_332 drawContext, class_310 client, int centerX, int centerY, int size) {
      if (client.field_1724 != null) {
         PlayerFaceRenderer.drawFace(drawContext, client, client.field_1724, centerX, centerY, size);
      }
   }

   private void renderYawCone(class_4587 matrices, int centerX, int centerY, int radius, int faceSize, float yaw, float totalAngle) {
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
      RenderSystem.setShader(class_757::method_34540);
      Matrix4f matrix = matrices.method_23760().method_23761();
      class_289 tessellator = class_289.method_1348();
      class_287 buffer = tessellator.method_1349();
      buffer.method_1328(class_5596.field_27380, class_290.field_1576);

      for (int i = 0; i <= segments; i++) {
         float angle = yawRad - halfAngle + 2.0F * halfAngle * (float)i / (float)segments;
         float sinA = (float)Math.sin((double)angle);
         float cosA = (float)Math.cos((double)angle);
         float ix = (float)centerX + -sinA * innerR;
         float iy = (float)centerY + cosA * innerR;
         float ox = (float)centerX + -sinA * outerR;
         float oy = (float)centerY + cosA * outerR;
         buffer.method_22918(matrix, ix, iy, 0.0F).method_22915(cr, cg, cb, innerAlpha).method_1344();
         buffer.method_22918(matrix, ox, oy, 0.0F).method_22915(cr, cg, cb, outerAlpha).method_1344();
      }

      tessellator.method_1350();
      buffer.method_1328(class_5596.field_27382, class_290.field_1576);
      this.addEdgeLine(buffer, matrix, centerX, centerY, yawRad - halfAngle, innerR, outerR);
      this.addEdgeLine(buffer, matrix, centerX, centerY, yawRad + halfAngle, innerR, outerR);
      tessellator.method_1350();
      RenderSystem.enableCull();
   }

   private void addEdgeLine(class_287 buffer, Matrix4f matrix, int cx, int cy, float angle, float innerR, float outerR) {
      float sinA = (float)Math.sin((double)angle);
      float cosA = (float)Math.cos((double)angle);
      float halfW = 1.0F;
      float ix = (float)cx + -sinA * innerR;
      float iy = (float)cy + cosA * innerR;
      float ox = (float)cx + -sinA * outerR;
      float oy = (float)cy + cosA * outerR;
      buffer.method_22918(matrix, ix + cosA * halfW, iy + sinA * halfW, 0.0F).method_1336(1, 1, 1, 1).method_1344();
      buffer.method_22918(matrix, ix - cosA * halfW, iy - sinA * halfW, 0.0F).method_1336(1, 1, 1, 1).method_1344();
      buffer.method_22918(matrix, ox - cosA * halfW, oy - sinA * halfW, 0.0F).method_1336(1, 1, 1, 1).method_1344();
      buffer.method_22918(matrix, ox + cosA * halfW, oy + sinA * halfW, 0.0F).method_1336(1, 1, 1, 1).method_1344();
   }

   private void renderOtherPlayerFaces(class_332 drawContext, class_310 client, int centerX, int centerY, int radius, double zoom, int faceSize) {
      if (client.field_1724 != null && client.field_1687 != null) {
         double playerX = client.field_1724.method_23317();
         double playerZ = client.field_1724.method_23321();

         for (class_742 entity : client.field_1687.method_18456()) {
            if (entity instanceof class_742 && entity != client.field_1724) {
               double dx = (entity.method_23317() - playerX) * zoom;
               double dz = (entity.method_23321() - playerZ) * zoom;
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

   private void renderPlayerArrowOld(class_4587 matrices, int centerX, int centerY, class_310 client) {
      if (client.field_1724 != null) {
         float yaw = (float)Math.toRadians((double)client.field_1724.method_36454());
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

   private void renderRegionName(class_332 drawContext, class_310 client, int centerX, int y) {
      if (client.field_1724 != null && client.field_1687 != null) {
         String world = client.field_1687.method_27983().method_29177().toString();
         List<MapRegion> regions = this.regionDataStore.getRegionsAt2D(world, (int)client.field_1724.method_23317(), (int)client.field_1724.method_23321());
         if (!regions.isEmpty()) {
            MapRegion region = regions.get(0);
            String name = region.getName();
            class_327 tr = client.field_1772;
            int textWidth = tr.method_1727(name);
            int textX = centerX - textWidth / 2;
            drawContext.method_25294(textX - 3, y - 2, textX + textWidth + 3, y + 9, Integer.MIN_VALUE);
            drawContext.method_25303(tr, name, textX, y, 16777215);
         }
      }
   }

   private void drawFilledRect(class_4587 matrices, float x1, float y1, float x2, float y2, int color) {
      Matrix4f matrix = matrices.method_23760().method_23761();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      class_289 tessellator = class_289.method_1348();
      class_287 buffer = tessellator.method_1349();
      RenderSystem.setShader(class_757::method_34540);
      class_289.method_1348();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      buffer.method_1328(class_5596.field_27382, class_290.field_1576);
      buffer.method_22918(matrix, x1, y1, 0.0F).method_22915(r, g, b, a).method_1344();
      buffer.method_22918(matrix, x1, y2, 0.0F).method_22915(r, g, b, a).method_1344();
      buffer.method_22918(matrix, x2, y2, 0.0F).method_22915(r, g, b, a).method_1344();
      buffer.method_22918(matrix, x2, y1, 0.0F).method_22915(r, g, b, a).method_1344();
      tessellator.method_1350();
   }

   private void drawFilledCircle(class_4587 matrices, float cx, float cy, float radius, int color) {
      Matrix4f matrix = matrices.method_23760().method_23761();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      class_289 tessellator = class_289.method_1348();
      class_287 buffer = tessellator.method_1349();
      RenderSystem.setShader(class_757::method_34540);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int segments = 32;
      buffer.method_1328(class_5596.field_27381, class_290.field_1576);
      buffer.method_22918(matrix, cx, cy, 0.0F).method_22915(r, g, b, a).method_1344();

      for (int i = 0; i <= segments; i++) {
         double angle = (Math.PI * 2) * (double)i / (double)segments;
         buffer.method_22918(matrix, cx + (float)(Math.cos(angle) * (double)radius), cy + (float)(Math.sin(angle) * (double)radius), 0.0F)
            .method_22915(r, g, b, a)
            .method_1344();
      }

      tessellator.method_1350();
   }

   private void drawCircleOutline(class_4587 matrices, float cx, float cy, float radius, float lineWidth, int color) {
      Matrix4f matrix = matrices.method_23760().method_23761();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      float halfWidth = lineWidth / 2.0F;
      float innerR = Math.max(0.0F, radius - halfWidth);
      float outerR = radius + halfWidth;
      class_289 tessellator = class_289.method_1348();
      class_287 buffer = tessellator.method_1349();
      RenderSystem.setShader(class_757::method_34540);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int segments = 64;
      buffer.method_1328(class_5596.field_27380, class_290.field_1576);

      for (int i = 0; i <= segments; i++) {
         double angle = (Math.PI * 2) * (double)i / (double)segments;
         float cos = (float)Math.cos(angle);
         float sin = (float)Math.sin(angle);
         buffer.method_22918(matrix, cx + cos * outerR, cy + sin * outerR, 0.0F).method_22915(r, g, b, a).method_1344();
         buffer.method_22918(matrix, cx + cos * innerR, cy + sin * innerR, 0.0F).method_22915(r, g, b, a).method_1344();
      }

      tessellator.method_1350();
   }

   private void drawTriangle(class_4587 matrices, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
      Matrix4f matrix = matrices.method_23760().method_23761();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      class_289 tessellator = class_289.method_1348();
      class_287 buffer = tessellator.method_1349();
      RenderSystem.setShader(class_757::method_34540);
      RenderSystem.enableBlend();
      buffer.method_1328(class_5596.field_27379, class_290.field_1576);
      buffer.method_22918(matrix, x1, y1, 0.0F).method_22915(r, g, b, a).method_1344();
      buffer.method_22918(matrix, x2, y2, 0.0F).method_22915(r, g, b, a).method_1344();
      buffer.method_22918(matrix, x3, y3, 0.0F).method_22915(r, g, b, a).method_1344();
      tessellator.method_1350();
      RenderSystem.disableBlend();
   }

   private void drawTriangleOutline(class_4587 matrices, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
      Matrix4f matrix = matrices.method_23760().method_23761();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      class_289 tessellator = class_289.method_1348();
      class_287 buffer = tessellator.method_1349();
      RenderSystem.setShader(class_757::method_34540);
      RenderSystem.enableBlend();
      RenderSystem.lineWidth(1.0F);
      buffer.method_1328(class_5596.field_29345, class_290.field_1576);
      buffer.method_22918(matrix, x1, y1, 0.0F).method_22915(r, g, b, a).method_1344();
      buffer.method_22918(matrix, x2, y2, 0.0F).method_22915(r, g, b, a).method_1344();
      buffer.method_22918(matrix, x3, y3, 0.0F).method_22915(r, g, b, a).method_1344();
      buffer.method_22918(matrix, x1, y1, 0.0F).method_22915(r, g, b, a).method_1344();
      tessellator.method_1350();
      RenderSystem.disableBlend();
   }

   private ConfigManager.ClientConfig getConfig() {
      ConfigManager cm = ConfigManager.getInstance();
      return cm != null ? cm.getConfig() : new ConfigManager.ClientConfig();
   }
}
