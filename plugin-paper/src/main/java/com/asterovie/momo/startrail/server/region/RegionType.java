package com.asterovie.momo.startrail.server.region;

/**
 * 矩形区域类型。
 *
 * <p><b>枚举顺序必须与客户端 {@code client.region.RegionType} 完全一致</b>——
 * 协议里按 {@code ordinal()} 传输，顺序不同会让客户端把安全区显示成危险区。
 */
public enum RegionType {
   NORMAL,
   SAFE,
   DANGER,
   CUSTOM;

   /** 按顺序号安全取值，越界回退 NORMAL。 */
   public static RegionType fromOrdinal(int ordinal) {
      RegionType[] all = values();
      return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NORMAL;
   }

   /** 从配置字符串解析，无法识别时按 CUSTOM 处理（不擅自替用户改类型）。 */
   public static RegionType fromConfig(String raw) {
      if (raw == null) {
         return NORMAL;
      }

      switch (raw.trim().toLowerCase()) {
         case "normal":
            return NORMAL;
         case "safe":
            return SAFE;
         case "danger":
         case "dangerous":
            return DANGER;
         case "custom":
            return CUSTOM;
         default:
            return CUSTOM;
      }
   }
}
