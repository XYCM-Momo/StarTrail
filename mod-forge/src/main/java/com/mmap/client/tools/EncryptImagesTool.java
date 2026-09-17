package com.mmap.client.tools;

import com.mmap.client.crypto.ImageCrypto;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class EncryptImagesTool {
   private EncryptImagesTool() {
   }

   public static void main(String[] args) throws Exception {
      if (args.length < 1) {
         System.out.println("\u7528\u6cd5: java -cp mmap.jar com.mmap.client.tools.EncryptImagesTool <images\u76ee\u5f55> [--delete]");
         System.out.println("  --delete : \u52a0\u5bc6\u6210\u529f\u540e\u5220\u9664\u539f\u660e\u6587 PNG/JPG\uff08\u9ed8\u8ba4\u4fdd\u7559\uff09");
      } else {
         File dir = new File(args[0]);
         boolean deleteOriginal = args.length > 1 && "--delete".equals(args[1]);
         if (!dir.isDirectory()) {
            System.err.println("\u4e0d\u662f\u6709\u6548\u76ee\u5f55: " + dir.getAbsolutePath());
            System.exit(1);
         }

         File[] images = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
         });
         if (images != null && images.length != 0) {
            int ok = 0;
            int fail = 0;

            for (File img : images) {
               try {
                  byte[] png = Files.readAllBytes(img.toPath());
                  byte[] enc = ImageCrypto.encrypt(png, img.getName());
                  String base = img.getName();
                  int dot = base.lastIndexOf(46);
                  if (dot > 0) {
                     base = base.substring(0, dot);
                  }

                  File out = new File(dir, base + ".mmap");
                  Files.write(out.toPath(), enc);
                  System.out.println("[OK] " + img.getName() + " -> " + out.getName() + " (" + png.length + " -> " + enc.length + " bytes)");
                  if (deleteOriginal) {
                     Files.deleteIfExists(img.toPath());
                     System.out.println("     \u5df2\u5220\u9664\u660e\u6587: " + img.getName());
                  }

                  ok++;
               } catch (IOException var15) {
                  System.err.println("[FAIL] " + img.getName() + ": " + var15.getMessage());
                  fail++;
               }
            }

            System.out.println("\u5b8c\u6210: \u6210\u529f " + ok + " \u4e2a, \u5931\u8d25 " + fail + " \u4e2a");
            System.out
               .println(
                  "\u63d0\u793a: \u5206\u53d1\u7ed9\u73a9\u5bb6\u65f6\u53ea\u62f7\u8d1d .mmap \u6587\u4ef6\uff08\u4e0d\u8981\u62f7\u8d1d\u660e\u6587 PNG\uff09\u3002"
               );
         } else {
            System.out.println("\u76ee\u5f55\u4e2d\u6ca1\u6709\u627e\u5230 PNG/JPG \u56fe\u7247: " + dir.getAbsolutePath());
         }
      }
   }
}
