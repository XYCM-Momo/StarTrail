package com.asterovie.momo.startrail.client.crypto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class ImageCrypto {
   private static final byte[] MAGIC = new byte[]{77, 77, 65, 80};
   private static final int VERSION = 1;
   private static final int IV_LEN = 16;
   private static final byte[] PNG_MAGIC = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};
   private static final byte[] JPEG_MAGIC = new byte[]{-1, -40, -1};
   private static final SecureRandom RANDOM = new SecureRandom();

   private ImageCrypto() {
   }

   private static SecretKeySpec deriveKey() {
      try {
         // ⚠️ 冻结常量（来自 MMmap 时期）：.mmap / .startrail 加密文件的密钥种子。
         //    改品牌名时**绝对不要动**这三个片段，否则所有已加密图片都会解不开。
         //    密钥 = SHA-256("MMmap-Xaero-Static-Image-2026") 前 16 字节；
         //    若确需更换，必须同步 tools/waypoint-editor 里的 MMAP_KEY_STR。
         String s1 = "MMmap";
         String s2 = "Xaero";
         String s3 = "Static-Image-2026";
         MessageDigest md = MessageDigest.getInstance("SHA-256");
         byte[] hash = md.digest((s1 + "-" + s2 + "-" + s3).getBytes(StandardCharsets.UTF_8));
         return new SecretKeySpec(Arrays.copyOf(hash, 16), "AES");
      } catch (Exception var5) {
         throw new IllegalStateException("\u65e0\u6cd5\u521d\u59cb\u5316 AES \u5bc6\u94a5", var5);
      }
   }

   public static byte[] encrypt(byte[] pngBytes, String originalFileName) {
      try {
         byte[] iv = new byte[16];
         RANDOM.nextBytes(iv);
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         cipher.init(1, deriveKey(), new IvParameterSpec(iv));
         byte[] encrypted = cipher.doFinal(pngBytes);
         byte[] nameBytes = originalFileName.getBytes(StandardCharsets.UTF_8);
         if (nameBytes.length > 65535) {
            throw new IllegalArgumentException("\u6587\u4ef6\u540d\u8fc7\u957f: " + originalFileName);
         } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream(encrypted.length + 32);
            out.write(MAGIC);
            out.write(1);
            out.write(nameBytes.length >> 8 & 0xFF);
            out.write(nameBytes.length & 0xFF);
            out.write(nameBytes);
            out.write(iv);
            out.write(encrypted);
            return out.toByteArray();
         }
      } catch (IllegalArgumentException var7) {
         throw var7;
      } catch (Exception var8) {
         throw new IllegalStateException("\u56fe\u7247\u52a0\u5bc6\u5931\u8d25: " + originalFileName, var8);
      }
   }

   public static byte[] maybeDecrypt(byte[] fileBytes) {
      if (fileBytes != null && fileBytes.length >= MAGIC.length) {
         for (int i = 0; i < MAGIC.length; i++) {
            if (fileBytes[i] != MAGIC[i]) {
               return fileBytes;
            }
         }

         return decrypt(fileBytes);
      } else {
         return fileBytes;
      }
   }

   public static byte[] decrypt(byte[] fileBytes) {
      try {
         if (fileBytes.length < MAGIC.length + 1 + 2 + 16 + 16) {
            return null;
         } else if (fileBytes[MAGIC.length] != 1) {
            return null;
         } else {
            int nameLen = (fileBytes[MAGIC.length + 1] & 255) << 8 | fileBytes[MAGIC.length + 2] & 255;
            int offset = MAGIC.length + 1 + 2;
            if (offset + nameLen + 16 > fileBytes.length) {
               return null;
            } else {
               offset += nameLen;
               byte[] iv = Arrays.copyOfRange(fileBytes, offset, offset + 16);
               offset += 16;
               byte[] encrypted = Arrays.copyOfRange(fileBytes, offset, fileBytes.length);
               Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
               cipher.init(2, deriveKey(), new IvParameterSpec(iv));
               byte[] plain = cipher.doFinal(encrypted);
               return !isImage(plain) ? null : plain;
            }
         }
      } catch (Exception var7) {
         return null;
      }
   }

   private static boolean isImage(byte[] data) {
      if (data != null && data.length >= 8) {
         for (int i = 0; i < PNG_MAGIC.length && data[i] == PNG_MAGIC[i]; i++) {
            if (i == PNG_MAGIC.length - 1) {
               return true;
            }
         }

         for (int ix = 0; ix < JPEG_MAGIC.length && data[ix] == JPEG_MAGIC[ix]; ix++) {
            if (ix == JPEG_MAGIC.length - 1) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean isEncrypted(byte[] fileBytes) {
      if (fileBytes != null && fileBytes.length >= MAGIC.length) {
         for (int i = 0; i < MAGIC.length; i++) {
            if (fileBytes[i] != MAGIC[i]) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static File resolveImageFile(File dir, String fileName) {
      if (dir != null && fileName != null) {
         String base = fileName;
         int dot = fileName.lastIndexOf(46);
         if (dot > 0) {
            String ext = fileName.substring(dot).toLowerCase();
            if (ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg")) {
               base = fileName.substring(0, dot);
            }
         }

         File startrail = new File(dir, base + ".startrail");
         if (startrail.exists()) {
            return startrail;
         }
         // 兼容旧版 MMmap 时期的 .mmap 加密文件
         File legacy = new File(dir, base + ".mmap");
         return legacy.exists() ? legacy : new File(dir, fileName);
      } else {
         return null;
      }
   }
}
