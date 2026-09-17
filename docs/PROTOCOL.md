# MMmap 网络协议 v1

> 改协议前**必读**。协议版本号 = **1**（插件握手包里的 `writeInt(1)`；模组侧对应
> `NetworkHandler.PROTOCOL_VERSION = "1"`）。改包结构时必须两边同时改并同步升版本（见 `VERSIONING.md`）。

## 1. 传输方式

| | |
|---|---|
| 通道名 | `mmap:main`（固定，不要改） |
| 方向 | **只有服务端 → 客户端**（单向下发，客户端不回包） |
| 服务端发送 | Bukkit `player.sendPluginMessage(plugin, "mmap:main", byte[])` |
| 客户端接收 | Forge `SimpleChannel`，消息索引 1~9 一一对应包类型 |
| 触发时机 | 玩家进服（握手 + 全量同步）、`/mmap reload`、区域/传送点增删改（增量广播）、世界切换、每 2 秒 MythicMobs 扫描 |

### Forge 侧为什么要「把包类型注册成消息索引」

Forge 的 `SimpleChannel` 会在每条消息最前面自己读写 **1 个字节作为消息索引**。
插件本来就在第 1 字节写了包类型（1~9），所以移植版直接把 **1~9 注册成消息索引 1~9**，
解码器拿索引当包类型、剩下全部当 payload —— **插件的字节流一个字节都不用改**。

另外：注册通道后 Forge 会在登录完成时自动向服务端发 `minecraft:register`（带上 `mmap:main`）。
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
| 2 | 区域全量同步 RegionSync | S→C | 进服 / reload | |
| 3 | 区域增量更新 RegionUpdate | S→C | 区域增删改 | 含操作码 |
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
| 2 | string | `serverId` | 固定 `"MMmap-Server"` |
| 3 | double | `warpIconHideThreshold` | `settings.warp-icon-hide-threshold`，默认 0.8 |
| 4 | string | `currentWorld` | Bukkit 世界**短名**（如 `world`），客户端保存为「当前世界」 |
| 5 | boolean | `showMobHeadFallback` | `settings.show-mob-head-fallback`，默认 true |

> 客户端读第 5 个字段时包了 `try/catch`：老服务端不发这个字段也不会崩，退回默认 `true`。
> **加字段请保持这个习惯**，就能做到「服务端先升级、客户端后升级」。

### 包 2 · 区域全量同步

```
byte     包类型 = 2
VarInt   区域数量 N
N × 区域记录
```

### 包 3 · 区域增量更新

```
byte   包类型 = 3
byte   操作码: 0=ADD, 1=REMOVE, 2=UPDATE   （enum UpdateOperation 的 ordinal）
区域记录
```

**区域记录**（包 2/3 共用）：

| 顺序 | 类型 | 字段 |
|---|---|---|
| 1 | string | `id` |
| 2 | string | `name`（可含 `§` 颜色代码） |
| 3 | string | `world` ← ⚠️ **Bukkit 短名原样，没有转成命名空间**（见第 5 节） |
| 4 | int | `minX` |
| 5 | int | `minZ` |
| 6 | int | `maxX` |
| 7 | int | `maxZ` |
| 8 | short | `minY` |
| 9 | short | `maxY` |
| 10 | byte | `type`（`RegionType` 的 ordinal） |
| 11 | int | `borderColor`（ARGB） |
| 12 | int | `fillColor`（ARGB） |
| 13 | float | `borderWidth` |
| 14 | string | `description` |

### 包 5 · 地图图片绑定同步

```
byte   包类型 = 5
VarInt 数量 N
N × { string id, string fileName, string world, int minX, int minZ, int maxX, int maxZ }
```

> 图片文件本身**不走网络**，只同步「绑定关系」。玩家需要自己把图片放到
> `.minecraft/mmap/map/`（客户端本地目录），插件发的那句聊天提示
> 「`请将图片文件放在 .minecraft/config/mmap/map/` 目录下」是**旧路径、写错了**（见第 5 节）。

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
    string iconKey    ← 对应 .minecraft/mmap/legend/ 下的图标
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

### 5.1 世界名两种写法混用（**已决策：不修，一律用多边形区域**）

`world` 字段在**包 2/3 里是 Bukkit 短名**（`world`），而在**包 6/7/9 里是命名空间**
（`minecraft:overworld`）。原因是插件里只有多边形/传送点/怪物走了 `toClientWorldKey()` 转换，
矩形区域是 `region.getWorld()` 原样写出。

而客户端 `RegionDataStore.getRegionsInWorld()` 用的是**严格 `equals` 比较**。

**实际影响（比想象的窄，逐条核实过）**：

| 功能 | 受影响？ | 原因 |
|---|---|---|
| 矩形区域在大地图上的填色/边框/名字/类型标签 | ❌ 正常 | `WorldMapScreen` 用 `getAllRegions()`，**不按世界过滤** |
| 矩形区域在小地图上的填色/边框 | ❌ 正常 | `MinimapRenderer.renderRegionBorders` 同样不按世界过滤 |
| 小地图上的「当前所在区域名」那一行 | ✅ **不显示** | 唯一走 `getRegionsAt2D(minecraft:overworld, …)` 的地方，严格比较失败 |
| 多边形区域的所有显示 | ❌ 正常 | 包 6 发的是命名空间格式，`PolygonOverlay` 传入的也是命名空间格式，**匹配** |
| 传送点 / MythicMobs 头颅 | ❌ 正常 | 包 7/9 同样是命名空间格式 |

> 附带发现（上游原有行为，与本节 bug 无关）：因为绘制路径完全不看世界，
> 一个配在 `world` 的矩形区域在**下界/末地也会被画出来**。

**决策（2026-09-17）**：**不去掉这个 bug，服务端一律用 `shape: polygon` 配区域。**
理由：

1. 影响面只有小地图那一行名字，而多边形路线本来就是完好的、还会自己画名字；
2. 多边形能表达矩形 —— **4 个顶点的多边形就是矩形**：

   ```yaml
   # 用多边形写一个矩形（等价于 shape: rectangle 的 (-100,-100)~(100,100)）
   my_rect:
     shape: polygon
     name: "§a我的区域"
     world: "world"
     lineColor: "#00FF00"
     fillColor: "#3300FF00"
     showLine: true
     fillEnabled: true
     polygon:
       - {x: -100, z: -100}
       - {x:  100, z: -100}
       - {x:  100, z:  100}
       - {x: -100, z:  100}
   ```

3. 多边形还支持 `enter-commands`（矩形也支持，但多边形这条路已验证可用）；
4. 矩形相比多边形多出来的只有 `type`（普通/安全/危险/自定义 标签）、`borderWidth`、
   `description`、`minY/maxY`（Y 范围限制）——目前都没有用到。

**如果将来确实要用矩形**，两条修法（都还没做）：

1. **客户端侧**（只动我们自己的工程，推荐）：把 `RegionDataStore.getRegionsInWorld()` 的
   `equals` 改成宽松匹配（完全相同，或一方以 `":" + 另一方` 结尾），两种写法都能认。
   代价：改代码 → 按 `VERSIONING.md` 升版本 → 重新构建发布，客户端要换 jar。
2. **服务端侧**：让插件写矩形时也调 `toClientWorldKey()` —— 但插件目前**没有可编译的工程**，
   得先搭源码工程才能改。

### 5.2 大地图点选不区分世界

`WorldMapScreen.findRegionAt()` 只判断 `contains2D`，不看世界。
如果不同维度存在坐标重叠的区域，大地图悬停可能取到别的维度的区域名。同样属于上游原有行为。

### 5.3 插件聊天提示里的目录是旧的

包 5 同步后插件会给玩家发「请将图片文件放在 `.minecraft/config/mmap/map/` 目录下」，
而模组实际读取的是 **`.minecraft/mmap/map/`**。属于纯文案问题，改插件才能修（同上，缺源码工程）。
