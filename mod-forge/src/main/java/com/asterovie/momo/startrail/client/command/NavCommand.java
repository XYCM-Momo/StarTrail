package com.asterovie.momo.startrail.client.command;

import com.asterovie.momo.startrail.client.StarTrailClient;
import com.asterovie.momo.startrail.client.nav.NavigationManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * 客户端本地导航命令：{@code /startrail nav <x> <z>}、{@code /startrail nav stop}、{@code /startrail nav help}。
 * 算法与显示全部在客户端完成，不会把命令转发给服务端。坐标支持小数。
 */
public final class NavCommand {
   private NavCommand() {
   }

   public static int nav(CommandSourceStack source, double x, double z) {
      StarTrailClient startrail = StarTrailClient.getInstance();
      NavigationManager nav = startrail != null ? startrail.getNavigationManager() : null;
      if (nav == null) {
         feedback(source, Component.literal("\u00a7c[\u5bfc\u822a] \u5bfc\u822a\u7cfb\u7edf\u672a\u521d\u59cb\u5316"));
         return 1;
      }

      if (nav.startNav(x, z)) {
         feedback(source, Component.literal("\u00a7a[\u5bfc\u822a] \u5df2\u5f00\u59cb\u5bfc\u822a\u5230 (" + fmt(x) + ", " + fmt(z) + ")\uff0c\u8bf7\u8ddf\u968f\u7ea2\u77f3\u7c92\u5b50"));
         return 0;
      } else {
         feedback(source, Component.literal("\u00a7c[\u5bfc\u822a] \u5bfc\u822a\u5931\u8d25\uff1a" + nav.getLastError()));
         return 1;
      }
   }

   public static int stop(CommandSourceStack source) {
      StarTrailClient startrail = StarTrailClient.getInstance();
      NavigationManager nav = startrail != null ? startrail.getNavigationManager() : null;
      if (nav != null) {
         nav.stopNav();
      }

      feedback(source, Component.literal("\u00a7e[\u5bfc\u822a] \u5df2\u505c\u6b62\u5bfc\u822a"));
      return 0;
   }

   public static int help(CommandSourceStack source) {
      feedback(source, Component.literal("\u00a76[StarTrail] \u5bfc\u822a\u547d\u4ee4\u5e2e\u52a9"));
      feedback(source, Component.literal("\u00a77/startrail nav <x> <z>   \u00a7f\u5bf9\u76ee\u6807\u5750\u6807\u53d1\u8d77\u5bfc\u822a\uff08\u7ea2\u77f3\u7c92\u5b50\u5f15\u8def\uff09"));
      feedback(source, Component.literal("\u00a77/startrail nav stop     \u00a7f\u505c\u6b62\u5f53\u524d\u5bfc\u822a"));
      return 0;
   }

   private static void feedback(CommandSourceStack source, Component message) {
      if (source != null) {
         source.sendSuccess(() -> message, false);
      }
   }

   /** 整数值去掉小数点显示（58.0 -> 58），小数保留（110.5 -> 110.5）。 */
   private static String fmt(double v) {
      if (v == Math.floor(v) && !Double.isInfinite(v)) {
         return String.valueOf((long)v);
      }

      return String.valueOf(v);
   }
}
