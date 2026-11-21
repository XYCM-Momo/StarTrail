# mod-1.1.12_plugin-1.0.3

| 项目 | 值 |
|---|---|
| 日期 | 2026-11-21 |
| Forge 模组 | **1.1.12** |
| Fabric 模组（上游原版，未改动） | 1.9.5 |
| 服务端插件 | **1.0.3**（本次未变） |
| 协议版本 | v1（未改动） |
| 通道名 | `startrail:main`（未改动） |

## 改了什么

**修复：1.1.11 只去了底衬，区域名的阴影还在。**

### 漏掉的两处

1. **文字投影**：`drawString` 的整型重载默认 `dropShadow = true`，
   所以即便删了黑色底衬，字右下仍有一圈黑边。
2. **小地图 / 多边形的底衬**：1.1.11 只删了大地图矩形区域名那一处，
   小地图区域名（`MinimapRenderer`）和多边形区域名（`PolygonOverlay`）的黑底还在。

### 覆盖范围（四处已全部统一）

| 位置 | 改动 |
|---|---|
| 大地图矩形区域名 | 去底衬 + `dropShadow=false` |
| 大地图区域类型行 | 去底衬 + `dropShadow=false` |
| 大地图多边形区域名 | 去底衬 + `dropShadow=false` |
| 小地图当前区域名 | 去底衬 + `dropShadow=false` |

**保留**：小地图下方的玩家坐标仍带黑底衬——那不属于区域名，且当时的要求只是改白色。

## 要不要换 jar

**只需客户端**：`startrail-forge-1.1.12.jar` → `versions\时之旅者\mods\`，**需重启游戏**。
服务端 `StarTrail-Plugin-1.0.3.jar` 不变。

## 成品哈希

| 文件 | 字节 | SHA256 |
|---|---|---|
| `startrail-forge-1.1.12.jar` | 166,443 | `7FC28B6A42349F7EF8F1419FF44EF6A3A1E3AC97645D2DE8E337A0951198E747` |
| `StarTrail-Plugin-1.0.3.jar` | 77,213 | `097D9E9EA4186DD20E0379DC102C0DBA2F7D031B6C19100A22FD728AE781D74D` |
| `mmap-fabric-1.9.5.jar` | 120,594 | `F6C1163DBE48DB57A179A11E892071305135105E0E2B8280AF4DB9BA9CD46A6A` |

## 验证情况

* ✅ `gradlew compileJava` 与 `tools\build-mod.ps1 -Offline` 均 `BUILD SUCCESSFUL`，产物 166,443 字节。
* ✅ 逐一核对了四处绘制调用，确认都已传 `dropShadow=false` 且无 `fill` 底衬。
* ✅ `tools\verify-dist.ps1 -Check` 全绿。
* ⚠️ **未进游戏实测**。

## 实机确认清单

1. 换 1.1.12 并重启游戏。
2. 大地图：区域名（含下方类型行）应**既无黑底、也无文字黑边**，纯文字压在贴图上。
3. 小地图：当前区域名（地图上方那行）同样无黑底、无黑边。
4. 若某些贴图背景下白字发虚，可考虑加描边（outline）替代，届时再提。
