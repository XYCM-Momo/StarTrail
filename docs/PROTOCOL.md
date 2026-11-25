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
| 2 | 矩形区域全量同步 RegionSync | S→C | 进服 / reload / 换世界 | 协议 2 起边界为 double；**1.0.6 起重新启用** |
| 3 | ~~区域增量更新 RegionUpdate~~ | — | — | **v1.9.6 起已移除**（矩形系统删除，不再发送） |
| 4 | *（未使用）* | — | — | 保留空号，别占用 |
| 5 | 地图图片绑定同步 MapImageSync | S→C | 进服 / reload | |
| 6 | 多边形区域同步 PolygonRegions | S→C | 进服 / reload | |
| 7 | 传送点同步 Warps | S→C | 进服 / reload | |
| 8 | 世界切换 WorldChange | S→C | 玩家换维度 | |
| 9 | MythicMobs 同步 | S→C | 每 2 秒 | 需要服务端装 MythicMobs |
| 10 | 导航指令 NavCommand | S→C | OP 执行 `/startrail nav` | 需要客户端装模组；v1.1.3 起 |
| 11 | NPC 列表 NpcList | S→C | 每 2 秒 | Adyeshach NPC 的 UUID；v1.1.13 起 |

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

### 包 2 · 矩形区域全量同步

> **v1.9.6 曾整体移除**矩形系统（服务端不再加载、不再发送），但**客户端一直保留接收与渲染代码**。
> 结果是配置里写矩形区域时**静默失效**：既不显示，也没有任何警告。
> **插件 1.0.6 / 模组 1.1.23 起重新启用**，并与其他区域一样支持小数坐标。

```
byte   包类型 = 2
VarInt 数量 N
N × {
    string id
    string name
    string world          ← 已转成命名空间（如 minecraft:overworld）
    double minX           ← 协议 2 起是 double；协议 1 是 int
    double minZ
    double maxX
    double maxZ
    double minY           ← 协议 2 起是 double；协议 1 是 short
    double maxY
    byte   type           ← RegionType 序号：0=NORMAL 1=SAFE 2=DANGER 3=CUSTOM
    int    borderColor
    int    fillColor
    float  borderWidth
    string description
}
```

**区域分流**：服务端读同一个 `regions/` 目录，按 `shape` 字段分流——
`shape: polygon`（或含 `polygon` 段）走包 6，其余（含默认值 `rectangle`）走本包。

**`RegionType` 序号必须两端一致**，否则客户端会把安全区显示成危险区。

**兼容性**：客户端按握手包协议号决定按 double 还是 int 解析；
「新客户端 + 旧服务端」不会崩，只是坐标被取整。

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
    M × { double x, double z }    ← 协议 2 起是 double；协议 1 是 int
    int    lineColor
    int    fillColor
    byte   showLine     (1/0)
    byte   fillEnabled  (1/0)
}
```

> **协议 2（插件 1.0.5 / 模组 1.1.22 起）顶点由 `int` 改为 `double`。**
>
> 原因：区域坐标允许写小数（如 `{x: -123.5, z: 373.5}`），
> 用 int 传输会把小数截断，导致大地图画出来的线**偏移到整数格**（最大偏 1 格）。
>
> **兼容性**：客户端按握手包里的协议号选择解析方式——
> `>= 2` 读 double，否则读 int。所以「新客户端 + 旧服务端」仍能正常显示（只是坐标被取整），
> 不会出现整包错位。但要让小数真正生效，**两端都必须升级**。

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

### 包 10 · 导航指令 NavCommand

服务端用 `/startrail nav <玩家> <x> <z> [导航点名称]` 触发。**只下发目标，寻路在客户端完成**——
服务端不发路径、不参与计算，客户端收到后调自己的 `NavigationManager.startNav(x, z, label)`。

```
byte   包类型 = 10
boolean stop         ← true = 停止当前导航（此时忽略后面所有字段）
double  x            ← 目标 X（支持小数）
double  z            ← 目标 Z（支持小数）
string  label        ← 导航点名称，显示在屏幕标点与聊天提示上；可为空串
boolean hasY         ← 【v1.1.4 后加】是否指定了目标 Y
double  y            ← 【v1.1.4 后加】目标 Y；hasY=false 时无意义（发 0）
```

**兼容性说明**：

* `label` 是后加字段。客户端用 `buf.isReadable()` + `try/catch` 读取，
  老服务端不发该字段时按空串处理，不会崩。
* 老客户端（1.1.2 及以前）收到类型 10 会走 `default` 分支，只打一条「未知数据包类型」日志，
  **不会崩、不影响其它功能**，但也不会导航——所以要让服务端指令生效，**客户端必须换到 1.1.3+**。
* 目标世界不随包下发：客户端只在「自己当前所在世界 == 导航世界」时显示，避免跨维度乱指。
* `hasY` / `y` 同样是后加字段。客户端按「能读就读，读不到就视为未指定」处理：
  未指定 Y 时按该列 `MOTION_BLOCKING` 最高方块推断——**楼内/洞穴点位必须带 Y**，否则终点会跑到屋顶上方。

#### 1.0.7 起：追加 `mode` 字段（四模式）

包尾再追加一个 `byte mode`，把「导航」拆成独立操作：

```
byte   包类型 = 10
boolean stop
double  x
double  z
string  label
boolean hasY
double  y             ← **无论 hasY 真假都会写一个占位 8 字节**
byte    mode          ← 0停止 1导航 2标记点 3清空
```

| mode | 行为 | 对应指令 |
|---|---|---|
| 0 | 停止当前导航 | `/startrail nav stop <玩家>` |
| 1 | 开始导航（算路径、铺粒子、判到达） | `/startrail nav` / `/startrail navpath` |
| 2 | 只创建标记点（不导航、不画路径，**可多个**） | `/startrail navpoint` |
| 3 | **清空全部**（标记点 + 正在进行的导航） | `/startrail navclear` |

> ⚠️ **读 `y` 时不能带 `hasY` 条件**：服务端无论 `hasY` 真假都写一个 `double` 占位，
> 客户端若在 `hasY=false` 时跳过这 8 字节，后续 `mode` 就会落在 y 的字节上，
> 表现为「清空」被误读成 `op=0`。**必须无条件读掉这 8 字节**，再按 `hasY` 决定是否采用。
>
> 老服务端（无 `mode`）时：客户端按 `stop ? 0 : 1` 推断，行为与旧版一致。
> 老客户端（无 `mode` 读取逻辑）同样安全：它在 `label` 之后就不再读，多出的字节不影响。

### 包 11 · NPC 列表 NpcList

服务端每 2 秒下发一次「玩家附近属于 Adyeshach 的 NPC UUID」，客户端据此把 NPC 和真人玩家区分开。

```
byte   包类型 = 11
VarInt 数量 N
N × string uuid      ← Adyeshach NPC 的 UUID（与客户端实体 UUID 相同）
```

**为什么需要**：Adyeshach 的 NPC 是 **PLAYER 类型实体**，在客户端看来与真人玩家无异，
地图原本会把它们当作「其他玩家」画头像。要让「显示 NPC 头像」开关只影响 NPC、不动真人，
客户端必须先知道哪些 UUID 是 NPC。

**实测确认**（Adyeshach 2.1.35）：NPC 的 `getUniqueId()` 与 `getNormalizeUniqueId()` 完全相同，
即客户端实体 UUID 就是 NPC 数据文件（`plugins/Adyeshach/npc/<uuid>.json`）里的 UUID。

**同步范围**：`settings.npc-track-radius`（默认 128），只发该半径内的 NPC。
未安装 Adyeshach 时服务端不发此包，客户端保持空集合（等于全部按真人处理，行为与旧版一致）。

**⚠️ 已知坑**：Adyeshach 的 NPC 数据**绑定世界名**。测试时把测试服世界改名为 `testworld` 后，
15 个 NPC 一个都没加载，**日志里没有任何提示**——非常隐蔽。换世界名时务必留意。
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
