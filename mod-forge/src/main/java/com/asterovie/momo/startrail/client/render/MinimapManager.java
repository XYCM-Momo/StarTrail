package com.asterovie.momo.startrail.client.render;

import com.asterovie.momo.startrail.client.config.ConfigManager;
import com.asterovie.momo.startrail.client.image.MapImageManager;
import com.asterovie.momo.startrail.client.region.PolygonRegionStore;
import com.asterovie.momo.startrail.client.region.RegionDataStore;
import com.asterovie.momo.startrail.client.warp.WarpIconManager;
import com.asterovie.momo.startrail.client.warp.WarpPointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class MinimapManager {
   private final RegionDataStore regionDataStore;
   private final PolygonRegionStore polygonRegionStore;
   private final MapImageManager mapImageManager;
   private final WarpPointStore warpPointStore;
   private final WarpIconManager warpIconManager;
   private final MinimapRenderer renderer;
   private boolean enabled = true;

   public MinimapManager(
      RegionDataStore regionDataStore,
      PolygonRegionStore polygonRegionStore,
      MapImageManager mapImageManager,
      WarpPointStore warpPointStore,
      WarpIconManager warpIconManager
   ) {
      this.regionDataStore = regionDataStore;
      this.polygonRegionStore = polygonRegionStore;
      this.mapImageManager = mapImageManager;
      this.warpPointStore = warpPointStore;
      this.warpIconManager = warpIconManager;
      this.renderer = new MinimapRenderer(regionDataStore, polygonRegionStore, mapImageManager, warpPointStore, warpIconManager);
   }

   public void init() {
      // 小地图改为注册成 GUI overlay（见 registerOverlay），不再走 RenderGuiEvent.Post。
      // 原因：Post 在「所有 overlay 之后」触发，会把 F3 调试屏盖住；
      // 排在 DEBUG_TEXT 之前渲染，F3 才能正常叠在小地图上面。
   }

   /**
    * 注册为 GUI overlay，并排在 F3 调试屏（{@code DEBUG_TEXT}）**之前**。
    *
    * <p>Forge 按 overlay 注册顺序依次渲染，后画的盖住先画的。
    * 排在 DEBUG_TEXT 之前 ⇒ F3 面板画在小地图之上，玩家开 F3 时能看清调试信息，
    * 小地图也不再需要为了躲避 F3 而挪位置/缩小。
    */
   public void registerOverlay(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
      event.registerBelow(
         net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.DEBUG_TEXT.id(),
         "minimap",
         // IGuiOverlay.render(ForgeGui, GuiGraphics, partialTick, screenWidth, screenHeight)
         (forgeGui, guiGraphics, partialTick, screenWidth, screenHeight) -> this.renderOverlay(guiGraphics, partialTick)
      );
   }

   private void renderOverlay(net.minecraft.client.gui.GuiGraphics gui, float partialTick) {
      if (!this.enabled) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.player == null || client.level == null) {
         return;
      }

      // 开着界面时：大地图铺满屏幕不画（「小地图设置」页例外），其它界面按配置决定
      Screen screen = client.screen;
      if (screen != null) {
         if (screen instanceof WorldMapScreen) {
            if (!((WorldMapScreen)screen).showsMinimapInSettings()) {
               return;
            }
         } else {
            ConfigManager.ClientConfig config = ConfigManager.getInstance().getConfig();
            if (config != null && config.minimap != null && config.minimap.hideInGui) {
               return;
            }
         }
      }

      this.renderer.render(gui, client, partialTick);
   }

   public void toggleMinimap() {
      this.enabled = !this.enabled;
   }

   public PolygonRegionStore getPolygonRegionStore() {
      return this.polygonRegionStore;
   }
}
