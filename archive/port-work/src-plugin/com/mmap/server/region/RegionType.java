package com.mmap.server.region;

public enum RegionType {
   NORMAL("\u666e\u901a\u533a\u57df", -16711936, 855703296),
   SAFE("\u5b89\u5168\u533a\u57df", -13408513, 859006719),
   DANGER("\u5371\u9669\u533a\u57df", -52429, 872362803),
   CUSTOM("\u81ea\u5b9a\u4e49\u533a\u57df", -1, 872415231);

   private final String displayName;
   private final int defaultBorderColor;
   private final int defaultFillColor;

   private RegionType(String displayName, int defaultBorderColor, int defaultFillColor) {
      this.displayName = displayName;
      this.defaultBorderColor = defaultBorderColor;
      this.defaultFillColor = defaultFillColor;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public int getDefaultBorderColor() {
      return this.defaultBorderColor;
   }

   public int getDefaultFillColor() {
      return this.defaultFillColor;
   }
}
