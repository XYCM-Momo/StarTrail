# mod-1.1.22_plugin-1.0.5

| 项目 | 值 |
|---|---|
| 日期 | 2026-11-24 |
| Forge 模组 | **1.1.22** |
| Fabric 模组（上游原版，未改动） | 1.9.5 |
| 服务端插件 | **1.0.5** |
| 协议版本 | **v2**（顶点 int → double） |
| 通道名 | `startrail:main`（未改动） |

## 改了什么

**修复：区域坐标写小数时，大地图画出来的线偏移到整数格。**

### 根因

多边形顶点**整条链路都用 `int[]`**，小数在最早的一步就被截断：

```java
// PolygonRegionManager 解析 yml
int x = ((Number)m.get("x")).intValue();   // ← -123.5 变成 -123
```

之后存储、发包（`writeInt`）、客户端接收（`readInt`）全程 int。
所以 `{x: -123.5}` 画出来是 `-123`，线整体**偏移 0.5 格**。

### 改法

顶点类型全线改为 `double`：

| 层 | 文件 | 改动 |
|---|---|---|
| 解析 | `PolygonRegionManager` | `intValue()` → `doubleValue()` |
| 模型 | 服务端 `PolygonRegion` | 顶点 `List<double[]>`，边界改 double |
| 发包 | `PacketSender` | `writeInt` → `writeDouble` |
| 收包 | 客户端 `NetworkHandler` | `readInt` → `readDouble`（按协议号分支） |
| 模型 | 客户端 `PolygonRegion` | 顶点 `List<double[]>` |
| 渲染 | `PolygonOverlay` | 顶点读取改 `double[]` |

### 协议版本升到 2

**顶点编码变了，两端必须同时升级。**

为防「新客户端 + 旧服务端」整包错位，客户端读握手包里的协议号：
`>= 2` 按 double 解析，否则按 int 解析（此时坐标仍是整数，但不会崩）。

### 顺带修了命令输出

`/startrail region info` 的顶点与中心原来用 `(int)` 强转显示，小数看不到；
现在保留小数，便于排查配置。

## 要不要换 jar

**两端都要换**：

* 客户端 `startrail-forge-1.1.22.jar` → `versions\时之旅者\mods\`（需重启游戏）
* 服务端 `StarTrail-Plugin-1.0.5.jar` → `时旅服务端\plugins\`（**需先关服**）

## 成品哈希

| 文件 | 字节 | SHA256 |
|---|---|---|
| `startrail-forge-1.1.22.jar` | 172,065 | `6589921828B22C6335684FD93BF2B79F85C3AFBD4C26359DB164F2B5BD347EC6` |
| `StarTrail-Plugin-1.0.5.jar` | 83,738 | `BCB42F1CD03632C3691320D542924FE35CE2C1E253F31326C3372615A5B0B000` |
| `mmap-fabric-1.9.5.jar` | 120,594 | `F6C1163DBE48DB57A179A11E892071305135105E0E2B8280AF4DB9BA9CD46A6A` |

## 验证情况

* ✅ 服务端与客户端 `gradlew compileJava`、`build-plugin.ps1`、`build-mod.ps1` 均 `BUILD SUCCESSFUL`。
  （服务端首次编译因 `PolygonRegion` 内部还有 4 处 `int xi/zi/xj/zj` 报精度丢失，已修；
  客户端首次编译因 `MinimapRenderer.containsPoint` 仍是 `List<int[]>` 失败，已修）
* ✅ **端到端往返测试 5 项全 PASS**，用线上后勤部的真实顶点：

  | 原始 | 旧实现（截断） | 新实现 |
  |---|---|---|
  | (-123.5, 318) | (-123, 318) | **(-123.5, 318)** |
  | (-75.5, 318) | (-75, 318) | **(-75.5, 318)** |
  | (-75.5, 373.5) | (-75, 373) | **(-75.5, 373.5)** |
  | (-123.5, 373.5) | (-123, 373) | **(-123.5, 373.5)** |

  另测 `0.1/0.2`、负坐标、`1e6+0.25` 均无损；整数坐标不受影响。
  旧实现偏移量确认为 **0.5 格**。
* ✅ `tools\verify-dist.ps1 -Check` 全绿。
* ⚠️ **未进游戏实测**。

## 实机确认清单

1. **先关服**换插件 1.0.5，再换模组 1.1.22 并重启游戏（两端都要换）。
2. 打开大地图，看**后勤部**区域的边框 —— 应精确落在 `-123.5 / -75.5 / 318 / 373.5`，
   不再整体偏移半格。
3. 在服务端执行 `/startrail region info support`（或对应区域 id），
   顶点应显示 `-123.5` 而不是 `-123`。
4. 若两端版本不一致：新客户端 + 旧服务端**不会崩**，但坐标仍是整数（协议号兜底生效）。

## 补充

**只对多边形区域生效。** 线上 `regions.yml` 目前只有多边形区域；
若以后加矩形区域（`shape: rectangle`），其 `minX/maxX` 等字段也是 int 语义，
需要另行评估。
