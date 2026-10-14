# 星轨 StarTrail 网络协议 v1

> 改协议前**必读**。协议版本号 = **1**（插件握手包里的 `writeInt(1)`；模组侧对应
> `NetworkHandler.PROTOCOL_VERSION = "1"`）。改包结构时必须两边同时改并同步升版本（见 `VERSIONING.md`）。

## 1. 传输方式

| | |
|---|---|
| 通道名 | `startrail:main`（固定，不要改） |
| 方向 | **只有服务端 → 客户端**（单向下发，客户端不回包） |
| 服务端发送 | Bukkit `player.sendPluginMessage(plugin, "startrail:main", byte[])` |
| 客户端接收 | Forge `SimpleChannel`，消息索引 1~9 一一对应包类型 |
| 触发时机 | 玩家进服（握手 + 全量同步）、`/startrail reload`、区域/传送点增删改（增量广播）、世界切换、每 2 秒 MythicMobs 扫描 |

### Forge 侧为什么要「把包类型注册成消息索引」

Forge 的 `SimpleChannel` 会在每条消息最前面自己读写 **1 个字节作为消息索引**。
插件本来就在第 1 字节写了包类型（1~9），所以移植版直接把 **1~9 注册成消息索引 1~9**，
解码器拿索引当包类型、剩下全部当 payload —— **插件的字节流一个字节都不用改**。

另外：注册通道后 Forge 会在登录完成时自动向服务端发 `minecraft:register`（带上 `startrail:main`）。
Bukkit 只给「已注册该通道」的玩家发 `sendPluginMessage`，所以这一步是能收到数据的**必要条件**。

## 2. 基础编码

| 类型 | 编码 |
|---|---|
| `byte` / `boolean` | 1 字节 |
| `short` | 2 字节，大端 |
| `int` / `float` / `double` | 4 / 4 / 8 字节，大端（Java `DataOutputStream` 语义） |
| `VarInt` | 7 位一组、低位在前，最高位为续位标志（同 Minecraft 的 VarInt） |
| `string` | `VarInt(UTF-8 字节数)` + UTF-8 字节（**不是** `writeUTF`） |

## 3. 包类型总表

| 类型 | 名称 | 方向 | 触发 | 备注 |
|---|---|---|---|---|
| 1 | 握手 Handshake | S→C | 玩家进服 | 下发服务端配置 |
| 2 | ~~区域全量同步 RegionSync~~ | — | — | **v1.9.6 起已移除**（矩形系统删除，不再发送） |
| 3 | ~~区域增量更新 RegionUpdate~~ | — | — | **v1.9.6 起已移除**（矩形系统删除，不再发送） |
| 4 | *（未使用）* | — | — | 保留空号，别占用 |
| 5 | 地图图片绑定同步 MapImageSync | S→C | 进服 / reload | |
| 6 | 多边形区域同步 PolygonRegions | S→C | 进服 / reload | |
| 7 | 传送点同步 Warps | S→C | 进服 / reload | |
| 8 | 世界切换 WorldChange | S→C | 玩家换维度 | |
| 9 | MythicMobs 同步 | S→C | 每 2 秒 | 需要服务端装 MythicMobs |

## 4. 各包字段明细

偏移量从包首字节（包类型）之后算起。

### 包 1 · 握手

| # | 类型 | 字段 | 说明 |
|---|---|---|---|
| 0 | byte | 包类型 = 1 | |
| 1 | int | `protocolVersion` | 固定 1 |
| 2 | string | `serverId` | 固定 `"StarTrail-Server"` |
| 3 | double | `warpIconHideThreshold` | `settings.warp-icon-hide-threshold`，默认 0.8 |
| 4 | string | `currentWorld` | Bukkit 世界**短名**（如 `world`），客户端保存为「当前世界」 |
| 5 | boolean | `showMobHeadFallback` | `settings.show-mob-head-fallback`，默认 true |
| 6 | boolean | `canTeleportFromMap` | 是否允许大地图右键传送（`startrail.command.tp` 权限或 OP），默认 false |

> 客户端读第 5、6 个字段时都包了 `try/catch`：老服务端少发字段也不会崩，
> 分别退回默认 `true` / `false`。**加字段请保持这个习惯**，就能做到「服务端先升级、客户端后升级」。

### 包 2 · 区域全量同步（**v1.9.6 起已移除**）

> 矩形区域系统已从插件中整体删除（多边形可表达任意矩形，见 5.1）。
> 历史格式：`byte 2 / VarInt 数量 N / N × 区域记录`；包 3 为区域增量更新（带操作码）。
> 客户端（模组 ≤ 1.9.11）仍保留矩形渲染代码，收不到包 2/3 自然不显示任何矩形，**无需改客户端**。

### 包 5 · 地图图片绑定同步

```
byte   包类型 = 5
VarInt 数量 N
N × { string id, string fileName, string world, int minX, int minZ, int maxX, int maxZ }
```

> 图片文件本身**不走网络**，只同步「绑定关系」。玩家需要自己把图片放到
> `.minecraft/startrail/map/`（客户端本地目录），插件发的那句聊天提示
> 「`请将图片文件放在 .minecraft/config/startrail/map/` 目录下」是**旧路径、写错了**（见第 5 节）。

### 包 6 · 多边形区域同步

```
byte   包类型 = 6
VarInt 数量 N
N × {
    string id
    string name
    string world      ← 已转成命名空间（如 minecraft:overworld）
    VarInt 顶点数 M
    M × { int x, int z }
    int    lineColor
    int    fillColor
    byte   showLine     (1/0)
    byte   fillEnabled  (1/0)
}
```

### 包 7 · 传送点同步

```
byte   包类型 = 7
VarInt 数量 N
N × {
    string id
    string name
    string world      ← 已转成命名空间
    int    x, int y, int z     ← 取整后的坐标
    string iconKey    ← 对应 .minecraft/startrail/legend/ 下的图标
    string descr
}
```

### 包 8 · 世界切换

```
byte   包类型 = 8
string worldName     ← Bukkit 世界短名
```

### 包 9 · MythicMobs 同步

```
byte   包类型 = 9
VarInt 怪物数量 N
N × {
    string uuid
    string type       ← MythicMobs 怪物类型名
    string world      ← 已转成命名空间
    double x, double y, double z
}
```

扫描半径由 `settings.mythic-mob-track-radius`（默认 128 格）控制，每 2 秒一次。

## 5. 已知坑 / 待确认问题（整理时发现）

### 5.1 世界名两种写法混用（**已解决：矩形系统已删除，v1.9.6**）

`world` 字段在**包 2/3（矩形）里是 Bukkit 短名**（`world`），在**包 6/7/9 里是命名空间**
（`minecraft:overworld`）。原因是插件里只有多边形/传送点/怪物走了 `toClientWorldKey()` 转换，
矩形区域是 `region.getWorld()` 原样写出。

而客户端 `RegionDataStore.getRegionsInWorld()` 用的是**严格 `equals` 比较**。

**处理过程（2026-09-17 决策 → 2026-09-18 落地）**：

1. 最初决策：**不去掉这个 bug，服务端一律用 `shape: polygon` 配区域**——多边形能表达矩形
   （4 个顶点的多边形就是矩形）、支持 `enter-commands`、渲染路径完好，只有小地图
   「当前所在区域名」那一行受影响（矩形数据在世界匹配时失败）。
2. **插件 v1.9.6 起矩形区域系统整体删除**：`RegionManager`/`MapRegion`/`RegionType`、
   `/startrail add|remove|info` 命令、包 2/3 全部移除；`/startrail list [世界]` 现在列出的是
   **多边形区域**（按世界过滤，`PolygonRegionManager.getInWorld()`）。
3. 客户端矩形渲染代码保留但已无数据（收不到包 2/3），行为兼容，不显示任何矩形。

**若将来想要矩形**（目前没必要，多边形全覆盖）：客户端 `RegionDataStore` 那套
代码 + 宽松世界匹配 + 恢复包 2/3 发送，属新功能而非修复。

### 5.2 大地图点选不区分世界（**已过时：矩形数据已无来源**）

`WorldMapScreen.findRegionAt()` 只判断 `contains2D`，不看世界——但这是矩形渲染代码，
v1.9.6 起插件不再发送矩形数据，该路径已无数据可命中。若未来恢复矩形系统需一并处理。

### 5.3 插件聊天提示里的目录是旧的

包 5 同步后插件会给玩家发「请将图片文件放在 `.minecraft/config/startrail/map/` 目录下」，
而模组实际读取的是 **`.minecraft/startrail/map/`**。属于纯文案问题，改插件才能修（同上，缺源码工程）。
