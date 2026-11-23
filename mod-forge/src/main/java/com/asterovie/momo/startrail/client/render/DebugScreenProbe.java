package com.asterovie.momo.startrail.client.render;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * 探测 F3 调试屏是否打开、以及左侧信息面板占用的高度。
 *
 * <p>小地图默认停在右上角，正好会被 F3 的左栏压住。有了这里给出的高度，
 * 小地图就能自动挪到 F3 面板下方（见 {@code MinimapRenderer} 里的避让逻辑）。
 *
 * <p>实现说明：{@code Gui.debugScreen} 与 {@code DebugScreenOverlay.getGameInformation()}
 * 都是 {@code protected}，没有公开接口，所以这里用反射取行数；
 * 反射失败时退回一个保守的固定高度，保证功能不至于整个失效。
 */
public final class DebugScreenProbe {
   /** F3 每一行的行高（原版用 9px 字体，行距同为 9）。 */
   private static final int LINE_HEIGHT = 9;
   /** 面板顶部留白（原版从 y=2 开始画）。 */
   private static final int TOP_OFFSET = 2;
   /** 左右两栏的间距 / 底部余量。 */
   private static final int BOTTOM_PAD = 4;
   /** 反射失败时假定的行数（原版常态大约这么多行）。 */
   private static final int FALLBACK_LINES = 30;

   private static Field debugScreenField;
   private static Method getGameInformationMethod;
   private static boolean resolved;

   private DebugScreenProbe() {
   }

   /** F3 是否打开。 */
   public static boolean isDebugScreenOpen(Minecraft client) {
      return client != null && client.options != null && client.options.renderDebug;
   }

   /**
    * F3 左侧信息面板占用的高度（像素）。F3 未打开时返回 0。
    *
    * <p>只取左栏（{@code getGameInformation}）的行数——两栏并排，
    * 面板整体高度取决于行数较多的那一栏底边对齐，这里取左栏即可覆盖右上角的小地图。
    */
   public static int debugPanelHeight(Minecraft client) {
      if (!isDebugScreenOpen(client)) {
         return 0;
      }

      int lines = FALLBACK_LINES;
      try {
         resolve(client);
         if (debugScreenField != null && getGameInformationMethod != null) {
            Object overlay = debugScreenField.get(client.gui);
            if (overlay != null) {
               Object result = getGameInformationMethod.invoke(overlay);
               if (result instanceof List) {
                  int n = ((List<?>)result).size();
                  if (n > 0) {
                     lines = n;
                  }
               }
            }
         }
      } catch (Throwable ignored) {
         // 反射失败就用兜底行数，不影响主流程
      }

      return TOP_OFFSET + lines * LINE_HEIGHT + BOTTOM_PAD;
   }

   private static void resolve(Minecraft client) {
      if (resolved) {
         return;
      }

      resolved = true;
      try {
         debugScreenField = findField(client.gui.getClass(), "debugScreen");
         Method m = Class.forName("net.minecraft.client.gui.components.DebugScreenOverlay")
            .getDeclaredMethod("getGameInformation");
         m.setAccessible(true);
         getGameInformationMethod = m;
      } catch (Throwable ignored) {
         debugScreenField = null;
         getGameInformationMethod = null;
      }
   }

   /** 沿类层次往上找字段：{@code debugScreen} 声明在父类 Gui 上，直接 getDeclaredField 可能找不到。 */
   private static Field findField(Class<?> start, String name) {
      for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
         try {
            Field f = c.getDeclaredField(name);
            f.setAccessible(true);
            return f;
         } catch (NoSuchFieldException ignored) {
            // 继续往上找
         }
      }

      return null;
   }
}
