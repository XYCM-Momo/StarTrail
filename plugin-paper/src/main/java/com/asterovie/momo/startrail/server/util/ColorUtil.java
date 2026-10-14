package com.asterovie.momo.startrail.server.util;

public final class ColorUtil {
   private ColorUtil() {
   }

   public static int parseColor(Object raw, int defaultArgb) {
      if (raw == null) {
         return defaultArgb;
      } else if (raw instanceof Number) {
         return ((Number)raw).intValue();
      } else {
         String s = raw.toString().trim();
         if (s.isEmpty()) {
            return defaultArgb;
         } else {
            try {
               boolean allDigitsOrMinus = true;

               for (int i = 0; i < s.length(); i++) {
                  char c = s.charAt(i);
                  if ((i != 0 || c != '-' && c != '+') && !Character.isDigit(c)) {
                     allDigitsOrMinus = false;
                     break;
                  }
               }

               if (allDigitsOrMinus) {
                  return Integer.parseInt(s);
               } else {
                  String hex = s;
                  if (s.startsWith("#")) {
                     hex = s.substring(1);
                  } else if (s.startsWith("0x") || s.startsWith("0X")) {
                     hex = s.substring(2);
                  }

                  if (hex.length() == 3) {
                     StringBuilder sb = new StringBuilder(6);

                     for (int ix = 0; ix < 3; ix++) {
                        char c = hex.charAt(ix);
                        sb.append(c).append(c);
                     }

                     hex = sb.toString();
                  } else if (hex.length() == 4) {
                     StringBuilder sb = new StringBuilder(8);

                     for (int ix = 0; ix < 4; ix++) {
                        char c = hex.charAt(ix);
                        sb.append(c).append(c);
                     }

                     hex = sb.toString();
                  }

                  long val = Long.parseLong(hex, 16);
                  int argb;
                  if (hex.length() == 6) {
                     argb = 0xFF000000 | (int)val & 16777215;
                  } else {
                     if (hex.length() != 8) {
                        return defaultArgb;
                     }

                     argb = (int)val;
                  }

                  return argb;
               }
            } catch (Exception var8) {
               return defaultArgb;
            }
         }
      }
   }

   public static String toHexString(int argb) {
      return String.format("#%08X", (long)argb & 4294967295L);
   }
}
