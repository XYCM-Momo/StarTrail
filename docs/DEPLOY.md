# 部署与联调清单

> 所有路径都是本机实测过的真实路径（2026-09-17）。换机器的话按名字类推。

## 1. 目标环境（本机现状）

| 角色 | 实例 | 加载器 | 版本 | 目录 |
|---|---|---|---|---|
| 客户端 A | PCL「**时之旅者**」 | **Forge** | MC 1.20.1 + Forge **47.4.10** | `E:\Minecraft\服务器\自用客户端\.minecraft\versions\时之旅者\` |
| 客户端 B | PCL「**时之旅者：书启**」 | Fabric | MC 1.20.1 + Fabric | `E:\Minecraft\服务器\自用客户端\.minecraft\versions\时之旅者：书启\` |
| 客户端 C | 官方客户端 | Fabric | MC 1.20.1 + Fabric | `E:\Minecraft\服务器\官方客户端\Game\` |
| 服务端 | 时旅服务端 | Paper | **1.20.1** | `E:\Minecraft\服务器\时旅服务端\` |

> ⚠️ **Forge 模组只能放 Forge 实例、Fabric 模组只能放 Fabric 实例**，放错了游戏里根本没有这个模组，
> 而且不会有任何报错，很容易误判成「模组坏了」。

PCL 这三个实例都开了**版本隔离**，也就是「游戏目录」= `versions\<版本名>\`，
所以 mods / config / mmap 数据都在各自的版本文件夹里，**不共用** `.minecraft\` 根目录。

## 2. 客户端部署

### 2.1 Forge 客户端（要装 `mmap-forge-<版本>.jar`）

| 放什么 | 放到哪 |
|---|---|
| 模组 jar | `...\.minecraft\versions\时之旅者\mods\mmap-forge-1.9.5.jar` |
| 地图图片 | `...\versions\时之旅者\mmap\map\`（文件名要和插件里配的 `fileName` 对上） |
| 传送点图标 | `...\versions\时之旅者\mmap\legend\` |
| MythicMobs 头颅 | `...\versions\时之旅者\mmap\mm\` |
| 模组配置 | `...\versions\时之旅者\config\mmap.json`（首次启动自动生成） |

```powershell
# 部署 = 从 dist 复制过去（覆盖旧的）
$src = "E:\Minecraft\服务器\插件\MMmap\dist\client-mod\mmap-forge-1.9.5.jar"
$dst = "E:\Minecraft\服务器\自用客户端\.minecraft\versions\时之旅者\mods\mmap-forge-1.9.5.jar"
Copy-Item $src $dst -Force
# 核对是不是同一个文件
(Get-FileHash $src).Hash -eq (Get-FileHash $dst).Hash
```

> 换版本号时记得把旧版本的 jar 从 `mods\` 里**删掉**，同一个 mod id 放两个 jar 会加载失败。

**按键**：`M` 开大地图；小地图默认没绑键，去「选项 → 按键设置 → MMmap」自己绑。
**本地指令**：`/mmap crypt`、`/mmap crypt delete`、`/mmap crypt help`（别名 `map` / `mmap` / `minimap`）。
`/mmap warp nav|tp <id>` 是**服务端**指令，会正常发给服务器。

### 2.2 Fabric 客户端（要装 `mmap-fabric-<版本>.jar`）

| 放什么 | 放到哪 |
|---|---|
| 模组 jar | `...\versions\时之旅者：书启\mods\mmap-1.9.5.jar` |
| 模组 jar（另一个实例） | `E:\Minecraft\服务器\官方客户端\Game\mods\[游戏]mmap-1.9.5.jar` |

数据目录同理在各自版本的 `mmap\` 和 `config\` 下。

## 3. 服务端部署

| 放什么 | 放到哪 |
|---|---|
| 插件 jar | `E:\Minecraft\服务器\时旅服务端\plugins\MMmap-Plugin-1.9.4.jar` |
| 插件数据 | `E:\Minecraft\服务器\时旅服务端\plugins\MMmap\` |

插件数据目录结构（本机现状）：

```
plugins\MMmap\
├─ config.yml            settings.warp-icon-hide-threshold / show-mob-head-fallback
│                        settings.mythic-mob-track-radius
├─ language.yml          提示文案
├─ images.yml            地图图片绑定（id / fileName / world / 范围）
├─ regions\regions.yml   区域（矩形 shape: rectangle 默认，或多边形 shape: polygon）
└─ warps\main-city.yml   传送点
```

改完配置执行 `/mmap reload` 就会重新推给所有在线玩家。

> **配区域的约定（2026-09-17 决定）：一律用 `shape: polygon`，不要用 `shape: rectangle`。**
> 矩形路线有一个世界名格式不一致的问题：小地图上「当前所在区域名」那行字对矩形不显示
> （其他显示都正常）。**4 个顶点的多边形就是矩形**，写成多边形即可，
> 原因、影响面、示例配置见 [`PROTOCOL.md` 第 5.1 节](PROTOCOL.md)。

**服务端指令**：`/mmmap <add|remove|list|info|reload|help>`（别名 `mmap` / `map` / `minimap`），
权限 `mmap.command`（默认 op）。

> ✅ 插件**已有可编译的源码工程** `plugin-paper/`（2026-09-17 建立）。
> 改插件逻辑、编译、冒烟测试、发布的完整流程：
>
> ```powershell
> # 1. 改 plugin-paper\src\main\java\...   （改完记得在 CHANGELOG.md 记一笔）
> # 2. 编译 + 发布到 dist
> powershell -ExecutionPolicy Bypass -File tools\build-plugin.ps1
> # 3. 冒烟测试：起一个一次性 Paper 服装上新 jar，不碰线上服务端
> powershell -ExecutionPolicy Bypass -File tools\smoke-test-plugin.ps1
> # 4. 通过后再覆盖线上服务端的 plugins\MMmap-Plugin-<版本>.jar，重启服务端
> ```
>
> **换插件 jar 之前先备份服务端的 `plugins\MMmap\` 数据目录**（区域/传送点/图片绑定配置都在里面）。
> 详见 [`../plugin-paper/README.md`](../plugin-paper/README.md)。

## 4. 上线后怎么确认「通了」

**客户端日志**（`versions\<版本名>\logs\latest.log`）应该依次出现：

```
[mmap/]: MMmap 模组正在加载...
[mmap/]: 已注册网络包接收器，通道: mmap:main          ← 关键：没这行说明通道没注册
[mmap/]: 收到服务端握手 - 协议版本: 1, 标识: MMmap-Server, ...  ← 关键：没这行说明收不到插件数据
[mmap/]: 已同步 N 个区域 / 已同步 N 个多边形区域 / 已同步 N 个传送点
[mmap/]: 已加载地图图片: main.mmap (543x575) -> mmap:map_image/main_map ...  ← 图片纹理加载成功
```

**服务端日志**应该出现：

```
已同步 N 个区域 -> <玩家名>
```

**排查顺序**（收不到数据时）：

1. 客户端日志有没有 `已注册网络包接收器，通道: mmap:main`？没有 → 模组装错实例了（Forge/Fabric 混淆）。
2. 有没有 `收到服务端握手`？没有 → 服务端没给这个玩家发 `mmap:main`，检查插件是否加载、玩家是否已注册通道。
3. 插件日志有没有「已同步 N 个区域」？没有 → 插件没触发同步，让玩家重进或 `/mmap reload`。
4. 有握手但没图/没区域 → 看 `plugins\MMmap\` 下的 yml 有没有内容。

## 5. 已知问题（实测记录）

### 5.1 `Duplicate handler name: forge:vanilla_filter`

Forge 客户端**第二次连接 Paper 服务端**时，客户端日志会出现这条 ERROR（FATAL 级别但不会崩）：

```
java.lang.IllegalArgumentException: Duplicate handler name: forge:vanilla_filter
  at io.netty.channel.DefaultChannelPipeline.checkDuplicateName(...)
  ...
  at net.minecraftforge.network.filters.NetworkFilters.injectIfNecessary(NetworkFilters.java:35)
  at net.minecraftforge.network.NetworkHooks.sendMCRegistryPackets(NetworkHooks.java:115)
```

* 堆栈里**没有 `com.mmap` 的帧**，是 Forge 自己的网络过滤器注入逻辑重复注入导致的（Forge 侧问题）。
* 触发路径 `sendMCRegistryPackets` 是「有模组注册了网络通道」时才会走的，所以装了本模组就会碰到；
  任何带自定义通道的 Forge 模组都一样。
* **实测不影响功能**：该次连接之后握手包、图片绑定、多边形区域都正常收到了。
* 待确认：是否只在「同一游戏会话内重连」时出现；如果是，可以在重连前退到主菜单看一眼就知道了。
* 想彻底消掉的话可以试试在 `mods.toml` 里保留 `displayTest="IGNORE_ALL_VERSION"`（现在就是这样）之外的
  其它方式，但没必要——目前不影响使用。

### 5.2 小地图上的矩形区域名不显示

原因和修法见 [`PROTOCOL.md` 第 5.1 节](PROTOCOL.md)（客户端/服务端世界名格式不一致）。
当前服务器只用了多边形区域，所以暂时看不到这个问题。

### 5.3 玩家皮肤显示成 Steve

`[PlayerFace] 玩家 XXX 没有皮肤（CustomSkinLoader+LocalSkin 都未找到）` 是**正常提示**：
需要在 `CustomSkinLoader/LocalSkin/skins/<玩家名>.png` 放本地皮肤，或在客户端设置里连上皮肤站。
和 MMmap 无关。
