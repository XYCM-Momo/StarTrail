# mod-1.1.3_plugin-1.0.1

| 项目 | 值 |
|---|---|
| 日期 | 2026-11-14 |
| Forge 模组 | **1.1.3** |
| Fabric 模组（上游原版，未改动） | 1.9.5 |
| 服务端插件 | **1.0.1** |
| 协议版本 | v1（**新增包类型 10，未升版本号**） |
| 通道名 | `startrail:main`（未改动） |

## 改了什么

**新增：服务端可以给指定玩家下发导航，并可带上导航点名称。**

背景：导航原本 100% 在客户端（`/startrail nav` 是 Forge 客户端指令，不发任何网络包），
服务端无从触发。本次新增协议包 **类型 10 `NavCommand`**（仅 S→C 单向），
把「目标坐标 + 名称」投递给客户端，**寻路与显示仍然全在客户端完成**。

### 新指令（权限 `startrail.command.nav`，默认 OP）

```
/startrail nav <玩家> <x> <z> [导航点名称]
/startrail nav stop <玩家>
```

* 名称允许带空格（第 4 个参数起整串拼接）：
  `/startrail nav 默默 100 200 主城传送门`
* 坐标支持小数。

### 玩家侧表现

* 聊天提示「服务端开始导航：<名称>，请跟随红石粒子」；
* 屏幕标点由「导航目标  123 格」变为「**主城传送门  123 格**」，视野外贴边箭头同样带名称；
* 未提供名称时退回原来的「导航目标  N 格」。

### 协议

`byte 10` + `boolean stop` + `double x` + `double z` + `string label`

* `label` 为后加字段，客户端用 `isReadable()` + `try/catch` 读取，老服务端不发也不会崩。
* 老客户端（≤1.1.2）收到类型 10 只打一条「未知数据包类型」日志，不崩但不生效。
* 已补进 `docs/PROTOCOL.md` 第 3、4 节。

## 要不要换 jar

**两端都要换**：

* 客户端 `startrail-forge-1.1.3.jar` → `versions\时之旅者\mods\`（需重启游戏）
* 服务端 `StarTrail-Plugin-1.0.1.jar` → `时旅服务端\plugins\`（**需先关服**，运行中 jar 被占用）

## 成品哈希

| 文件 | 字节 | SHA256 |
|---|---|---|
| `startrail-forge-1.1.3.jar` | 163,456 | `3160E1FE730C3CD29EDDE0243C76FBAF9319980A3C3EAAA0D5AD73C58CA51D74` |
| `StarTrail-Plugin-1.0.1.jar` | 76,307 | `8998D59111FB53D555DEAB8DFF17D445BD00BD9D800AFBB337A9503AF0364FFE` |
| `mmap-fabric-1.9.5.jar` | 120,594 | `F6C1163DBE48DB57A179A11E892071305135105E0E2B8280AF4DB9BA9CD46A6A` |

## 验证情况

* ✅ `mod-forge` 与 `plugin-paper` 分别编译通过（串行构建，`BUILD SUCCESSFUL`）。
* ✅ `tools\build-mod.ps1 -Offline`、`tools\build-plugin.ps1 -Offline` 均成功。
* ✅ `tools\smoke-test-plugin.ps1` 通过：插件正常启用，无自身异常。
* ✅ **起独立 Paper 测试服（端口 25577）实测指令**：
  * `/startrail help` 输出里已含 `/startrail nav <玩家> <x> <z> [导航点名称]`；
  * `/startrail nav`（缺参数）→ 正确打印用法；
  * `/startrail nav Nobody 100 200 TestPoint` → 回复「玩家不在线: Nobody」，无异常；
  * `/startrail nav stop Nobody` → 同上。
* ✅ `tools\verify-dist.ps1 -Check` 全绿。
* ⚠️ **未做两端联机的完整实测**（需要真实玩家客户端接入），
  即「服务端下发 → 客户端收到并开始导航 → 标点显示名称」这条链路还没在游戏里跑通。
  另外冒烟测试只能验证插件加载，**不能验证数据包内容**——包格式正确性是靠代码审查 + 协议文档比对确认的。

## 实机确认清单

1. 服务端装 `StarTrail-Plugin-1.0.1`，客户端装 `startrail-forge-1.1.3`，两端都重启。
2. OP 执行 `/startrail nav <自己> 100 200 测试点` → 客户端应收到聊天提示并开始导航。
3. 屏幕标点上方应显示「测试点  123 格」；转出视野后贴边箭头也应带「测试点」。
4. `/startrail nav stop <自己>` → 导航停止，粒子与标点消失。
5. 不给名称执行 → 标点显示「导航目标  123 格」（旧样式）。
