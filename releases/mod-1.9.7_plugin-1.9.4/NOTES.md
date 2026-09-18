# 冻结记录：mod-1.9.7_plugin-1.9.4

| 项目 | 值 |
|---|---|
| 冻结日期 | 2026-09-17 |
| 客户端模组（Forge） | **1.9.7** — `mmap-forge-1.9.7.jar` |
| 客户端模组（Fabric） | **1.9.5** — `mmap-fabric-1.9.5.jar`（上游原版，未改动） |
| 服务端插件（Paper） | **1.9.4** — `MMmap-Plugin-1.9.4.jar` |
| 网络协议 | **v1**（未改动） |
| 目标环境 | MC 1.20.1 / Forge 47.4.10 / Fabric / Paper 1.20.1，Java 17 |

## 校验值（SHA256）

```
F6C1163DBE48DB57A179A11E892071305135105E0E2B8280AF4DB9BA9CD46A6A  mmap-fabric-1.9.5.jar
E50716B1A7D6BBA89E66C45DCA2AA42ED97EEB1630E89CAC5AA5F28761170161  mmap-forge-1.9.7.jar
3E6AEF9C886188061FD6ED19106CFAACDDD588252DF7C44D00052EB8EA0ED0  MMmap-Plugin-1.9.4.jar
```

## 这一版改了什么

**修复：地图图片的世界边界整体偏了 1 格 —— 配置里必须把四个坐标统统 -1 才能对齐。**

* **原因**：`WorldMapScreen.renderMapImages()` 在算图片的四个屏幕顶点时，硬编码了 `+ 1.0F`：

  ```java
  // 旧代码
  screenX1 = worldToScreenX(image.getMinX() + 1.0F);
  screenX2 = worldToScreenX(image.getMinX() + imageWidth + 1.0F);
  ```

  于是整张图被往**东/南各推 1 格**。玩家只能在配置里把 `minX/minZ/maxX/maxZ` 全写小 1 来抵消。

* **顺带发现的第二个问题**：世界地图画图片时，跨度用的是**图片像素尺寸**（`imageWidth`），
  而配置里的 `maxX/maxZ` 只用于视野裁剪、不参与绘制；
  但**小地图**（`MinimapImageRenderer`）用的是配置的 `minX..maxX` 映射 UV，跨度取 `maxX - minX`。
  → 两个视图的算法不一致，同一张图在世界地图和小地图上会差 1 格。

* **改法**（`WorldMapScreen.java`）：直接按配置的矩形定位与铺图 ——
  西/北边缘 = `minX/minZ`，东/南边缘 = `maxX/maxZ`，纹理拉伸铺满该矩形。
  去掉 `+1.0F`；跨度改用 `maxX - minX`（若配置跨度 ≤ 0 则退回图片像素尺寸，避免整张图消失）。
  改完后世界地图与小地图**用同一套边界规则**，配置写真实边界即可。

* 改动仅这一个文件，渲染细节、协议、服务端逻辑均未改动。

## 需要配套做的事（重要）

**升级后必须把 `plugins\MMmap\images.yml` 里的坐标改回真实边界**（去掉之前的 -1 补偿），
否则图片会反向偏 1 格。以 `main_map` 为例：

```yaml
  main_map:
    fileName: main.png
    minX: -429
    minZ: -231
    maxX: 431
    maxZ: 629
```

（`main.png` 实测 860×860 像素，正好等于 431-(-429) = 860，所以 1 像素 = 1 格、不会有拉伸。）

## 验证情况

* ✅ `gradlew clean build` 编译通过（产物 `mmap-forge-1.9.7.jar`）。
* ✅ 已核对 `main.png` 真实像素尺寸为 860×860（AES 解密后读 PNG IHDR），
  与使用者给出的真实边界跨度 860×860 完全一致。
* ⏳ **尚未部署**：打包时使用者正在游戏内（jar 被占用），等关掉游戏后再覆盖 `mods\`。
* ⚠️ 待实机确认：改用真实边界后图片是否与地形完全对齐。

## 回滚方式

把 [`../mod-1.9.6_plugin-1.9.4/mmap-forge-1.9.6.jar`](../mod-1.9.6_plugin-1.9.4/) 复制回
`versions\时之旅者\mods\`，删掉 1.9.7 的 jar，并把 `images.yml` 的坐标改回 -1 版本。
