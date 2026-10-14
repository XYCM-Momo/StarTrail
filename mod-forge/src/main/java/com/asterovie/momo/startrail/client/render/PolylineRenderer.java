package com.asterovie.momo.startrail.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * 屏幕空间折线绘制：给出一串屏幕坐标 (x, y) 点，画成带厚度的折线段（QUADS）。
 * 用于大地图/小地图上显示导航路径。
 */
public final class PolylineRenderer {
   private PolylineRenderer() {
   }

   /** @param pts 屏幕坐标点列表（每个 double[2]，x 在前 y 在后）
    *  @param color ARGB 颜色
    *  @param thickness 线宽（像素） */
   public static void draw(GuiGraphics ctx, List<double[]> pts, int color, float thickness) {
      if (pts.size() < 2) {
         return;
      }

      float a = ((color >> 24) & 0xFF) / 255.0F;
      float r = ((color >> 16) & 0xFF) / 255.0F;
      float g = ((color >> 8) & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      if (a <= 0.0F) {
         a = 1.0F;
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      PoseStack pose = ctx.pose();
      pose.pushPose();
      Matrix4f matrix = pose.last().pose();
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      float half = thickness / 2.0F;

      for (int i = 0; i < pts.size() - 1; i++) {
         double[] p0 = pts.get(i);
         double[] p1 = pts.get(i + 1);
         double dx = p1[0] - p0[0];
         double dy = p1[1] - p0[1];
         double len = Math.sqrt(dx * dx + dy * dy);
         if (len < 1.0E-6) {
            continue;
         }

         double nx = -dy / len * (double)half;
         double ny = dx / len * (double)half;
         buffer.vertex(matrix, (float)(p0[0] + nx), (float)(p0[1] + ny), 0.0F).color(r, g, b, a).endVertex();
         buffer.vertex(matrix, (float)(p1[0] + nx), (float)(p1[1] + ny), 0.0F).color(r, g, b, a).endVertex();
         buffer.vertex(matrix, (float)(p1[0] - nx), (float)(p1[1] - ny), 0.0F).color(r, g, b, a).endVertex();
         buffer.vertex(matrix, (float)(p0[0] - nx), (float)(p0[1] - ny), 0.0F).color(r, g, b, a).endVertex();
      }

      tessellator.end();
      RenderSystem.disableBlend();
      pose.popPose();
   }
}
