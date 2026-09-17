package com.mmap.client.render;

import com.mmap.client.MMmapMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class MinimapFBO {
   private int fboId = -1;
   private int colorTextureId = -1;
   private int depthStencilTextureId = -1;
   private int width = 0;
   private int height = 0;
   private int savedViewportX;
   private int savedViewportY;
   private int savedViewportW;
   private int savedViewportH;
   private Matrix4f savedModelViewTop;
   private Matrix4f savedProjection;

   public MinimapFBO() {
   }

   public void ensureSize(int newWidth, int newHeight) {
      if (this.fboId == -1 || this.width != newWidth || this.height != newHeight) {
         this.cleanup();
         this.width = newWidth;
         this.height = newHeight;
         this.colorTextureId = GL11.glGenTextures();
         GL11.glBindTexture(3553, this.colorTextureId);
         GL11.glTexImage2D(3553, 0, 32856, this.width, this.height, 0, 6408, 5121, (ByteBuffer)null);
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glBindTexture(3553, 0);
         this.depthStencilTextureId = GL11.glGenTextures();
         GL11.glBindTexture(3553, this.depthStencilTextureId);
         GL30.glTexImage2D(3553, 0, 35056, this.width, this.height, 0, 34041, 34042, (ByteBuffer)null);
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glBindTexture(3553, 0);
         this.fboId = GL30.glGenFramebuffers();
         GL30.glBindFramebuffer(36160, this.fboId);
         GL30.glFramebufferTexture2D(36160, 36064, 3553, this.colorTextureId, 0);
         GL30.glFramebufferTexture2D(36160, 33306, 3553, this.depthStencilTextureId, 0);
         int status = GL30.glCheckFramebufferStatus(36160);
         if (status != 36053) {
            MMmapMod.LOGGER.error("[FBO] \u521b\u5efa\u5931\u8d25 status=0x{}", Integer.toHexString(status));
            this.cleanup();
         } else {
            MMmapMod.LOGGER.info("[FBO] \u5df2\u521b\u5efa {}x{}", this.width, this.height);
         }

         GL30.glBindFramebuffer(36160, 0);
      }
   }

   public void beginRender() {
      if (this.fboId != -1) {
         IntBuffer vp = BufferUtils.createIntBuffer(4);
         GL11.glGetIntegerv(2978, vp);
         this.savedViewportX = vp.get(0);
         this.savedViewportY = vp.get(1);
         this.savedViewportW = vp.get(2);
         this.savedViewportH = vp.get(3);
         this.savedModelViewTop = new Matrix4f(RenderSystem.getModelViewStack().last().pose());
         this.savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
         GL30.glBindFramebuffer(36160, this.fboId);
         GL11.glViewport(0, 0, this.width, this.height);
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glClear(17664);
         Matrix4f proj = new Matrix4f().ortho(0.0F, (float)this.width, (float)this.height, 0.0F, -1000.0F, 1000.0F);
         RenderSystem.setProjectionMatrix(proj, VertexSorting.ORTHOGRAPHIC_Z);
         RenderSystem.getModelViewStack().setIdentity();
         RenderSystem.applyModelViewMatrix();
      }
   }

   public void drawStencilCircle(float cx, float cy, float radius) {
      GL11.glEnable(2960);
      GL11.glStencilFunc(519, 1, 255);
      GL11.glStencilOp(7680, 7680, 7681);
      GL11.glColorMask(false, false, false, false);
      GL11.glDepthMask(false);
      Matrix4f matrix = new Matrix4f();
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      int segments = 64;
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, cx, cy, 0.0F).color(255, 255, 255, 255).endVertex();

      for (int i = 0; i <= segments; i++) {
         double angle = (Math.PI * 2) * (double)i / (double)segments;
         float x = cx + (float)(Math.cos(angle) * (double)radius);
         float y = cy + (float)(Math.sin(angle) * (double)radius);
         buffer.vertex(matrix, x, y, 0.0F).color(255, 255, 255, 255).endVertex();
      }

      tessellator.end();
      GL11.glColorMask(true, true, true, true);
      GL11.glDepthMask(true);
      GL11.glStencilFunc(514, 1, 255);
      GL11.glStencilOp(7680, 7680, 7680);
   }

   public void endRender() {
      GL11.glDisable(2960);
      GL30.glBindFramebuffer(36160, 0);
      GL11.glViewport(this.savedViewportX, this.savedViewportY, this.savedViewportW, this.savedViewportH);
      RenderSystem.setProjectionMatrix(this.savedProjection, VertexSorting.ORTHOGRAPHIC_Z);
      RenderSystem.getModelViewStack().setIdentity();
      RenderSystem.getModelViewStack().mulPoseMatrix(this.savedModelViewTop);
      RenderSystem.applyModelViewMatrix();
   }

   public void drawToScreen(float x, float y, float size, Matrix4f guiMatrix) {
      if (this.colorTextureId != -1) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.bindTexture(this.colorTextureId);
         RenderSystem.setShaderTexture(0, this.colorTextureId);
         RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         Tesselator tessellator = Tesselator.getInstance();
         BufferBuilder buffer = tessellator.getBuilder();
         buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         buffer.vertex(guiMatrix, x, y, 0.0F).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
         buffer.vertex(guiMatrix, x, y + size, 0.0F).uv(0.0F, 1.0F).color(255, 255, 255, 255).endVertex();
         buffer.vertex(guiMatrix, x + size, y + size, 0.0F).uv(1.0F, 1.0F).color(255, 255, 255, 255).endVertex();
         buffer.vertex(guiMatrix, x + size, y, 0.0F).uv(1.0F, 0.0F).color(255, 255, 255, 255).endVertex();
         tessellator.end();
      }
   }

   public int getColorTextureId() {
      return this.colorTextureId;
   }

   public boolean isReady() {
      return this.fboId != -1;
   }

   public void cleanup() {
      if (this.fboId != -1) {
         GL30.glDeleteFramebuffers(this.fboId);
         this.fboId = -1;
      }

      if (this.colorTextureId != -1) {
         GL11.glDeleteTextures(this.colorTextureId);
         this.colorTextureId = -1;
      }

      if (this.depthStencilTextureId != -1) {
         GL11.glDeleteTextures(this.depthStencilTextureId);
         this.depthStencilTextureId = -1;
      }

      this.width = 0;
      this.height = 0;
   }
}
