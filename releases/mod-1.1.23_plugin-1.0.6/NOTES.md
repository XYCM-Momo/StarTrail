# mod-1.1.23_plugin-1.0.6

| 项目 | 值 |
|---|---|
| 日期 | 2026-11-24 |
| Forge 模组 | **1.1.23** |
| Fabric 模组（上游原版，未改动） | 1.9.5 |
| 服务端插件 | **1.0.6** |
| 协议版本 | v2（沿用；矩形边界也改为 double） |
| 通道名 | `startrail:main`（未改动） |

## 改了什么

**矩形区域：补回加载与下发，坐标同样支持小数。**

### 发现的两个问题

1. **矩形区域一直在静默失效**

   v1.9.6 把矩形系统从服务端整体删掉了（`PolygonRegionManager` 的 `if (isPolygon)` 之后**没有 else**），
   但**客户端一直保留接收与渲染代码**（包 2/3）。
   结果配置里写 `shape: rectangle` 的区域**既不加载、也不下发**，
   地图上什么都看不到，**而且没有任何警告** —— 典型的静默失效。

2. **矩形坐标是 int**

   即便能加载，`minX: -123.5` 也会被截断、边框偏移半格，与上一版多边形是同一类问题。

### 改法

* 新增服务端 `MapRegion` / `MapRegionManager` / `RegionType`；
  与多边形**读同一个 `regions/` 目录**，按 `shape` 分流
  （`polygon` 走多边形，其余含默认值 `rectangle` 走矩形）。
* 补回**包 2（矩形全量同步）**的发送：**进服、`/startrail reload`、换世界**三处都会发。
* 矩形边界 int → **double**（`minY`/`maxY` 原本还是 short），解析统一 `sec.getDouble()`。
* 客户端 `MapRegion` 全部坐标改 double，保留 int 兼容构造函数；
  `readRegion` 按握手协议号分支（`>=2` 读 double，否则读 int/short）。

### 一处必须两端一致的约定

`RegionType` 序号 **0=NORMAL 1=SAFE 2=DANGER 3=CUSTOM**，
服务端按 `ordinal()` 发包、客户端按序号还原。顺序写错会把安全区显示成危险区。
已在 `PROTOCOL.md` 中写明。

## 要不要换 jar

**两端都要换**：

* 客户端 `startrail-forge-1.1.23.jar` → `versions\时之旅者\mods\`（需重启游戏）
* 服务端 `StarTrail-Plugin-1.0.6.jar` → `时旅服务端\plugins\`（**需先关服**）

## 成品哈希

| 文件 | 字节 | SHA256 |
|---|---|---|
| `startrail-forge-1.1.23.jar` | 172,393 | `4BA41E516107EC210058249F3DD5A8B5EBC1C0C423C1FFB94F42AF42301F8362` |
| `StarTrail-Plugin-1.0.6.jar` | 89,772 | `FA931883F23BC97844BE433594D6C7608A05E07D412E11C151CC20F6F6A1D6CC` |

## 验证情况

* ✅ 服务端与客户端 `gradlew compileJava`、`build-plugin.ps1`、`build-mod.ps1` 均 `BUILD SUCCESSFUL`。
* ✅ **矩形往返测试 13 项全 PASS**：

  | 检查项 | 结果 |
  |---|---|
  | 六个边界无损 | PASS |
  | 旧实现 `-123.5 → -123`、`373.5 → 373` | 确认为截断 |
  | 宽度 48.0 / 深度 55.5 | 保留小数 |
  | 中心 (-99.5, 345.75) | 保留小数 |
  | 边界内外判定（含「恰在边界上算内」） | PASS |
  | `shape` 分流三种情况 | PASS |

* ✅ **YAML 层确认**小数点被解析为 float（`-123.5` → float），服务端 `getDouble()` 读取正常。
* ✅ `tools\verify-dist.ps1 -Check` 全绿。
* ⚠️ **未进游戏实测**。

## 实机确认清单

1. **先关服**换插件 1.0.6，再换模组 1.1.23 并重启游戏。
2. 在 `regions/` 里加一个矩形区域测试：

   ```yaml
   rect_test:
     shape: rectangle
     name: "测试矩形"
     world: "world"
     minX: -123.5
     minZ: 318
     maxX: -75.5
     maxZ: 373.5
     type: safe
   ```

3. 执行 `/startrail reload` —— 控制台应出现 **`矩形区域加载完成: N 个`**（以前这条日志根本不存在）。
4. 打开大地图 → 应能看到该矩形边框，且精确落在 `-123.5 / -75.5 / 318 / 373.5`。
5. 服务端启动日志里也应有 `矩形区域加载完成: N 个`。

## 补充说明

矩形区域现在与多边形**共用 `regions/` 目录**，靠 `shape` 区分。
以前写了矩形配置却看不到，不是配置写错，而是服务端根本没有实现 —— 现在补上了。
