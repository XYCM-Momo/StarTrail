package com.mmap.client.command;

import com.mmap.client.MMmapMod;
import com.mmap.client.crypto.ImageCrypto;
import java.io.File;
import java.nio.file.Files;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2561;
import net.minecraft.class_310;

public final class EncryptCommand {
   private static final String[] ALIASES = new String[]{"mmap", "map", "mmmap", "minimap"};

   private EncryptCommand() {
   }

   public static boolean tryExecuteLocal(String command) {
      if (command == null) {
         return false;
      } else {
         FabricClientCommandSource source = getSource();
         if (source == null) {
            return false;
         } else {
            String cmd = command.trim();

            for (String alias : ALIASES) {
               String base = alias + " crypt";
               if (cmd.equals(base)) {
                  runEncrypt(source, false);
                  return true;
               }

               if (cmd.equals(base + " delete")) {
                  runEncrypt(source, true);
                  return true;
               }

               if (cmd.equals(base + " help")) {
                  showHelp(source);
                  return true;
               }
            }

            return false;
         }
      }
   }

   private static FabricClientCommandSource getSource() {
      try {
         class_310 client = class_310.method_1551();
         return client.method_1562() == null ? null : (FabricClientCommandSource)client.method_1562().method_2875();
      } catch (Exception var1) {
         return null;
      }
   }

   private static int runEncrypt(FabricClientCommandSource source, boolean deleteOriginal) {
      File root = getMmapDir();
      if (root == null) {
         source.sendFeedback(class_2561.method_43470("\u00a7c\u65e0\u6cd5\u5b9a\u4f4d mmap \u914d\u7f6e\u76ee\u5f55"));
         return 1;
      } else {
         int totalOk = 0;
         int totalFail = 0;
         boolean anyProcessed = false;

         for (String sub : new String[]{"map", "legend", "mm"}) {
            File dir = new File(root, sub);
            if (dir.isDirectory()) {
               File[] images = dir.listFiles((d, name) -> {
                  String lower = name.toLowerCase();
                  return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
               });
               if (images != null && images.length != 0) {
                  anyProcessed = true;
                  int ok = 0;
                  int fail = 0;
                  StringBuilder failNames = new StringBuilder();

                  for (File img : images) {
                     try {
                        byte[] raw = Files.readAllBytes(img.toPath());
                        byte[] enc = ImageCrypto.encrypt(raw, img.getName());
                        String base = img.getName();
                        int dot = base.lastIndexOf(46);
                        if (dot > 0) {
                           base = base.substring(0, dot);
                        }

                        File out = new File(dir, base + ".mmap");
                        Files.write(out.toPath(), enc);
                        if (deleteOriginal) {
                           Files.deleteIfExists(img.toPath());
                        }

                        ok++;
                     } catch (Exception var24) {
                        fail++;
                        if (failNames.length() > 0) {
                           failNames.append(", ");
                        }

                        failNames.append(img.getName());
                        MMmapMod.LOGGER.error("\u52a0\u5bc6\u5931\u8d25: {}/{}", new Object[]{sub, img.getName(), var24});
                     }
                  }

                  totalOk += ok;
                  totalFail += fail;
                  source.sendFeedback(
                     class_2561.method_43470(
                        "\u00a7a  "
                           + sub
                           + "/: \u6210\u529f \u00a7e"
                           + ok
                           + "\u00a7a, \u5931\u8d25 \u00a7e"
                           + fail
                           + (fail > 0 ? " \u00a7c(" + failNames + ")" : "")
                     )
                  );
               }
            }
         }

         if (!anyProcessed) {
            source.sendFeedback(class_2561.method_43470("\u00a7e\u6ca1\u6709\u627e\u5230\u4efb\u4f55 PNG/JPG \u56fe\u7247\u3002"));
            source.sendFeedback(class_2561.method_43470("\u00a77\u626b\u63cf\u76ee\u5f55: " + root.getAbsolutePath() + "/{map,legend,mm}"));
            return 1;
         } else {
            source.sendFeedback(
               class_2561.method_43470(
                  "\u00a7a[MMmap] \u52a0\u5bc6\u5b8c\u6210: \u5171\u6210\u529f \u00a7e"
                     + totalOk
                     + "\u00a7a \u4e2a, \u5931\u8d25 \u00a7e"
                     + totalFail
                     + "\u00a7a \u4e2a"
                     + (deleteOriginal ? " \u00a77\uff08\u5df2\u5220\u9664\u660e\u6587\uff09" : " \u00a77\uff08\u660e\u6587\u5df2\u4fdd\u7559\uff09")
               )
            );
            source.sendFeedback(
               class_2561.method_43470(
                  "\u00a77\u63d0\u793a: \u5206\u53d1\u7ed9\u73a9\u5bb6\u65f6\u53ea\u62f7\u8d1d .mmap\uff0c\u4e0d\u8981\u62f7\u8d1d\u660e\u6587 PNG\u3002"
               )
            );
            return totalFail > 0 ? 1 : 0;
         }
      }
   }

   private static int showHelp(FabricClientCommandSource source) {
      source.sendFeedback(class_2561.method_43470("\u00a76[MMmap] \u56fe\u7247\u52a0\u5bc6\u6307\u4ee4\u5e2e\u52a9"));
      source.sendFeedback(
         class_2561.method_43470(
            "\u00a77/mmap crypt          \u00a7f\u52a0\u5bc6 map/legend/mm \u76ee\u5f55\u6240\u6709\u56fe\u7247\u4e3a .mmap\uff08\u4fdd\u7559\u660e\u6587\uff09"
         )
      );
      source.sendFeedback(class_2561.method_43470("\u00a77/mmap crypt delete   \u00a7f\u52a0\u5bc6\u5e76\u5220\u9664\u660e\u6587\uff08\u63a8\u8350\uff09"));
      source.sendFeedback(class_2561.method_43470("\u00a77/mmap crypt help     \u00a7f\u663e\u793a\u672c\u5e2e\u52a9"));
      source.sendFeedback(class_2561.method_43470("\u00a77\u626b\u63cf\u76ee\u5f55: .minecraft/mmap/{map,legend,mm}/"));
      return 0;
   }

   private static File getMmapDir() {
      try {
         return new File(FabricLoader.getInstance().getGameDir().toFile(), "mmap");
      } catch (Exception var1) {
         return null;
      }
   }
}
