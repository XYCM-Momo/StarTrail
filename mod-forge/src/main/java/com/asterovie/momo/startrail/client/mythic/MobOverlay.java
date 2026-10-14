package com.asterovie.momo.startrail.client.mythic;

import com.asterovie.momo.startrail.client.config.ConfigManager;
import com.asterovie.momo.startrail.client.warp.WarpIconTexture;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

public final class MobOverlay {
   private MobOverlay() {
   }

   public static void render(
      String world,
      MythicMobStore store,
      MobHeadManager headMgr,
      double camX,
      double camZ,
      int originX,
      int originY,
      double scale,
      GuiGraphics ctx,
      boolean clipEnabled,
      int clipCx,
      int clipCy,
      int clipR,
      int baseIconSize
   ) {
      if (store != null && headMgr != null) {
         List<MythicMob> mobs = store.getInWorld(world);
         if (!mobs.isEmpty()) {
            int iconSize;
            if (clipEnabled) {
               iconSize = baseIconSize;
            } else {
               double threshold = ConfigManager.getInstance().getConfig().worldmap.warpIconHideThreshold;
               if (scale < threshold) {
                  return;
               }

               iconSize = Math.max(8, Math.round(8.0F * (float)Math.max(1.0, scale / 16.0)));
               iconSize = Math.min(iconSize, 48);
            }

            for (MythicMob m : mobs) {
               float sx = (float)((double)originX + (m.getX() - camX) * scale);
               float sy = (float)((double)originY + (m.getZ() - camZ) * scale);
               if (clipEnabled) {
                  int dx = (int)sx - clipCx;
                  int dy = (int)sy - clipCy;
                  int limitR = clipR + iconSize / 2;
                  if (dx * dx + dy * dy > limitR * limitR) {
                     continue;
                  }
               }

               WarpIconTexture tex = headMgr.getTexture(m.getType());
               if (tex != null && tex.isReady()) {
                  drawIcon(ctx, tex, sx, sy, iconSize);
               } else if (ConfigManager.getInstance().getConfig().general.showMobHeadFallback) {
                  int x = (int)(sx - (float)(iconSize / 2));
                  int y = (int)(sy - (float)(iconSize / 2));
                  ctx.fill(x, y, x + iconSize, y + iconSize, -65281);
               }
            }
         }
      }
   }

   private static void drawIcon(GuiGraphics ctx, WarpIconTexture tex, float centerX, float centerY, int iconSize) {
      int texW = tex.getImageWidth();
      int texH = tex.getImageHeight();
      if (texW > 0 && texH > 0) {
         int effSize = Math.max(4, iconSize);
         float s = (float)effSize / (float)Math.max(texW, texH);
         int drawW = Math.max(1, Math.round((float)texW * s));
         int drawH = Math.max(1, Math.round((float)texH * s));
         float fx = centerX - (float)drawW / 2.0F;
         float fy = centerY - (float)drawH / 2.0F;
         int ix = Math.round(fx);
         int iy = Math.round(fy);
         ctx.pose().pushPose();
         ctx.pose().translate(fx - (float)ix, fy - (float)iy, 0.0F);
         ctx.blit(tex.getTextureId(), ix, iy, drawW, drawH, 0.0F, 0.0F, texW, texH, texW, texH);
         ctx.pose().popPose();
      }
   }
}
