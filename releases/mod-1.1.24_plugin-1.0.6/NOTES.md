# mod-1.1.24_plugin-1.0.6

| 项目 | 值 |
|---|---|
| 日期 | 2026-11-24 |
| Forge 模组 | **1.1.24** |
| Fabric 模组（上游原版，未改动） | 1.9.5 |
| 服务端插件 | **1.0.6**（本次未变） |
| 协议版本 | v2（未改动） |
| 通道名 | `startrail:main`（未改动） |

## 改了什么

**修复：小地图里又出现了多边形区域名。**

### 根因

1.1.14 让「区内不显示区域名」时，只改了 **大地图** 的调用点：
给 `PolygonOverlay.render` 加了 `showLabels` 参数，大地图传 `shouldShowRegionLabels()`。

**但漏掉了小地图那处调用** —— 它用的是**简写重载**，而简写重载一路走到 `showLabels = true`。
所以小地图内部一直还在画多边形区域名。

> 为什么 1.1.14 之后「看起来没了」？
> 因为当时多边形区域一个都查不到（`currentRegionName` 只查矩形存储）。
> **1.1.21 修好多边形查询后，这些名字就一起冒出来了** —— 问题一直在，只是之前碰巧不可见。

### 改法

小地图改调完整重载，最后一个参数传 **`false`**：

```java
PolygonOverlay.render(
   worldKey, ..., drawContext, client.font, 1.0F,
   true, centerX, centerY, radius,     // 裁剪（圆形小地图）
   -1, -1, -1, -1,
   false                                // showLabels = false
);
```

填充（`drawPolygonFill`）与边框（`drawAllPolygonLines`）在本判断**之前**执行，
由 `if (!showLabels) return;` 拦住 —— 所以**区域形状照常显示，只是不画名字**。

### 保持不变的部分

大地图仍按原规则：缩放 **<** `warp-icon-hide-threshold` 时显示区域名，
**≥** 时让位给传送点图标。

## 要不要换 jar

**只需客户端**：`startrail-forge-1.1.24.jar` → `versions\时之旅者\mods\`，**需重启游戏**。
服务端 `StarTrail-Plugin-1.0.6.jar` 不变。

## 成品哈希

| 文件 | 字节 | SHA256 |
|---|---|---|
| `startrail-forge-1.1.24.jar` | 172,396 | `C2A0A7B3E3DB2E68E113E644B268A80173D5DD7D178A204FB8DC1E2EBB0B8534` |
| `StarTrail-Plugin-1.0.6.jar` | 89,772 | `FA931883F23BC97844BE433594D6C7608A05E07D412E11C151CC20F6F6A1D6CC` |
| `mmap-fabric-1.9.5.jar` | 120,594 | `F6C1163DBE48DB57A179A11E892071305135105E0E2B8280AF4DB9BA9CD46A6A` |

## 验证情况

* ✅ `gradlew compileJava` 与 `tools\build-mod.ps1 -Offline` 均 `BUILD SUCCESSFUL`，产物 172,396 字节。
* ✅ 确认小地图自身**只有两处** `drawString`（坐标 + 下方地区名），
  区内文字**只能来自** `PolygonOverlay`，本次已关闭。
* ✅ 确认 `showLabels` 的判断位于填充与描线**之后**，不影响区域形状显示。
* ✅ `tools\verify-dist.ps1 -Check` 全绿。
* ⚠️ **未进游戏实测**。

## 实机确认清单

1. 换 1.1.24 并重启游戏。
2. 走进「主城」或「后勤部」范围 → 小地图**内部不应再出现区域名**。
3. 小地图下方的坐标、以及坐标下方的地区名**仍然正常**（那是另一处绘制，不受影响）。
4. 大地图：缩小到阈值以下应显示区域名，放大后让位给传送点图标（行为不变）。

## 说明

小地图下方那行地区名（`显示地区名` 开关控制）与本次修复的是**两处不同的东西**：

| 位置 | 内容 | 控制开关 |
|---|---|---|
| 小地图**内部** | 多边形区域名 | 本次已**彻底关闭**（无开关） |
| 小地图**下方** | 当前所在地区名 | 设置里 `显示地区名`，默认开 |

如果下方那行也不想留，可以在设置里关掉，或者告诉我，我把它默认值改成关。
