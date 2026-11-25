# mod-1.1.27_plugin-1.0.7

| 项目 | 值 |
|---|---|
| 日期 | 2026-11-26 |
| Forge 模组 | **1.1.27** |
| Fabric 模组（上游原版，未改动） | 1.9.5 |
| 服务端插件 | **1.0.7** |
| 协议版本 | v2（包 10 追加 `mode` 字段，向后兼容） |
| 通道名 | `startrail:main`（未改动） |

## 改了什么

**导航指令拆分：`navpoint` 只标点、`navpath` 发起导航、`navclear` 清空全部；保留 `nav`。**

### 新指令（都需 `startrail.command.nav` 权限）

| 指令 | 作用 |
|---|---|
| `/startrail navpath <玩家> <x> <y> <z> [名称]` | 发起导航，**与 `nav` 完全等价** |
| `/startrail navpoint <玩家> <x> <y> <z> [名称]` | **只创建标记点**：不导航、不算路径、不铺粒子，**可同时存在多个** |
| `/startrail navclear <玩家>` | **清空全部**：所有标记点 + 正在进行的导航 |
| `/startrail nav ...` | 保留不变，行为与以前一致 |

坐标写法与 `nav` 一致：`<x> <y> <z>` 或省略 Y 的 `<x> <z>`（Y 按地表推断）。

### 协议

包 10 末尾追加 `byte mode`（0 停止 / 1 导航 / 2 标记点 / 3 清空）。
服务端旧重载 `sendNavCommand(...)` 保留，内部按 `stop ? 0 : 1` 转发，不破坏既有调用。

### 客户端

* 新增 `NavPointMarkerStore` 保存多个标记点（同位置同名称去重）；
* `NavMarkerOverlay` 的绘制函数从「直接读 `NavigationManager`」**解耦成接收 `(tx, ty, tz)`**，
  导航与标记点**共用同一套屏幕标记绘制**；新增 `renderPointMarkers`；
* 标记点未指定 Y 时复用 `NavigationManager.resolveSurfaceY`（新公开的方法）按地表推断。

### 顺带修掉一个潜伏的真 bug

客户端读 `y` 原本写成：

```java
if (hasY && buf.isReadable()) {
    y = buf.readDouble();
}
```

但**服务端无论 `hasY` 真假都会写一个 `double` 占位**。`hasY=false` 时那 8 字节没人读，
新加的 `mode` 就落在 y 的字节上——**「清空」被误读成 `op=0`，标记点 2 和清空 3 全部退化成「停止导航」**。

改为**无条件读掉这 8 字节**，再按 `hasY` 决定是否采用。

> 旧版本因为 `y` 之后没有别的字段，这个错位才一直没暴露。

## 要不要换 jar

**两端都要换**：

* 客户端 `startrail-forge-1.1.27.jar` → `versions\时之旅者\mods\`（需重启游戏）
* 服务端 `StarTrail-Plugin-1.0.7.jar` → `时旅服务端\plugins\`（**需先关服**）

## 成品哈希

| 文件 | 字节 | SHA256 |
|---|---|---|
| `startrail-forge-1.1.27.jar` | 176,230 | `646172E54476DA29BB374F2ED2061397ECB19B94DE1D73B7D11DC644C756960D` |
| `StarTrail-Plugin-1.0.7.jar` | 91,839 | `645284DFD4340BC1F3823D668AE15ECC1FB60C8F0AF68667E5502C2CBDF369BA` |
| `mmap-fabric-1.9.5.jar` | 120,594 | `F6C1163DBE48DB57A179A11E892071305135105E0E2B8280AF4DB9BA9CD46A6A` |

## 验证情况

* ✅ 两端 `gradlew compileJava`、`build-plugin.ps1`、`build-mod.ps1` 均 `BUILD SUCCESSFUL`。
* ✅ **协议端到端测试 10 项全 PASS**：

  | 检查项 | 结果 |
  |---|---|
  | 导航 `op=1`、标签正确 | PASS |
  | 标记点 `op=2`、小数坐标无损 | PASS |
  | **清空 `op=3`**（修正前读出 0） | PASS |
  | 停止 `op=0` | PASS |
  | `hasY=false` 时 y 为 NaN 且 op 正确 | PASS |
  | 旧服务端无 mode → 按 stop 推断 | PASS |
  | 标记点可多个 / 同位置同名去重 / 清空 | PASS |

* ✅ `tools\verify-dist.ps1 -Check` 全绿。
* ⚠️ **未进游戏实测**。

## 实机确认清单

1. **先关服**换插件 1.0.7，再换模组 1.1.27 并重启游戏。
2. `/startrail navpoint <你> 100 64 200 测试点` → 屏幕出现一个标记，**不铺粒子路径**。
3. 再执行两次 `navpoint`（不同坐标）→ **多个标记同时存在**（每个都带名称与距离）。
4. `/startrail navpath <你> 300 64 400 目标` → 正常导航（粒子路径 + 屏幕标记）。
5. `/startrail navclear <你>` → **标记点与导航同时消失**。
6. `/startrail nav <你> 300 64 400` → 仍与以前一样能导航（旧指令保留）。
7. `/startrail help` → 应能看到 navpath / navpoint / navclear 三行说明。
