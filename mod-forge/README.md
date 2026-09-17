# MMmap — Forge 1.20.1 移植版

把工作区里的 **Fabric 客户端模组** `mmap-fabric-1.9.5.jar`（Minecraft 1.20.1 / Fabric）移植成了
**Forge 47.4.10（Minecraft 1.20.1）** 模组。

服务器端**不需要任何改动**：继续使用原来的 Bukkit 插件 `MMmap-Plugin-1.9.4.jar`。
网络协议完全保持兼容 —— 通道名 `mmap:main`、包类型 1~9、字段顺序与字节序都和 1.9.4 插件一致
（逐字段对照表见 [`../docs/PROTOCOL.md`](../docs/PROTOCOL.md)）。

> 工作区总览、版本规划、备份与部署流程见 [`../README.md`](../README.md)、
> [`../VERSIONING.md`](../VERSIONING.md)、[`../docs/DEPLOY.md`](../docs/DEPLOY.md)。

---

## 1. 直接使用

> ⚠️ **这是 Forge 模组，必须用 Forge 客户端加载。**
> 本机已有的 Forge 实例是 PCL 版本「**时之旅者**」（MC 1.20.1 + Forge 47.4.10），
> jar 已经装在 `versions\时之旅者\mods\` 里了。
> 「时之旅者：书启」和 `官方客户端` 都是 **Fabric** 实例，把本 jar 丢进去不会生效。
> 服务器端不用动，Paper 1.20.1 继续用 `MMmap-Plugin-1.9.4.jar`（协议一致）。

| 文件 | 说明 |
|---|---|
| `../dist/client-mod/mmap-forge-1.9.5.jar` | 编译好的 **Forge 客户端模组**，放进 Forge 客户端的 `mods/` |
| `mod-forge/`（本目录） | 完整的 Forge 工程源码（可自行修改、重新编译） |
| `../dist/server-plugin/MMmap-Plugin-1.9.4.jar` | 服务器插件，**保持不变**，继续放在服务端 `plugins/` |

环境要求：Minecraft **1.20.1** + Forge **47.4.10**（47.x 任意版本，`mods.toml` 里写的是 `[47,)`）+ Java 17。

> 这是**纯客户端模组**，服务端只有 Bukkit 插件，所以 `mods.toml` 里用了
> `displayTest="IGNORE_ALL_VERSION"`，进原版/Bukkit 服务器不会被踢。

## 2. 按键与指令

| 功能 | 默认键 |
|---|---|
| 打开大地图 | `M` |
| 切换小地图 | 未绑定（在“按键设置”里自己绑） |

本地加密指令（客户端执行，不会发到服务器，别名 `map` / `mmmap` / `minimap` 同样可用）：

* `/mmap crypt` — 加密 `map` / `legend` / `mm` 目录内所有 png/jpg 为 `.mmap`（保留原图）
* `/mmap crypt delete` — 加密并删除明文
* `/mmap crypt help` — 帮助

`/mmap warp nav <id>`、`/mmap warp tp <id>`（大地图里点传送点按钮发的）仍然是**服务端插件**的指令，会正常发给服务器。

## 3. 目录与配置（和原版 Fabric 版完全一致）

路径都是相对于**游戏目录**的。PCL 的「时之旅者」实例开了版本隔离，
所以实际路径是 `.minecraft\versions\时之旅者\` 而不是 `.minecraft\`：

```
<游戏目录>/mmap/map/          地图图片（由插件下发绑定，图片放这里）
<游戏目录>/mmap/legend/       传送点图标
<游戏目录>/mmap/mm/           MythicMobs 头颅图标
<游戏目录>/config/mmap.json   模组配置（首次启动自动生成）
```

## 4. 源码工程

```
mod-forge/                    ← 原 MMmap-Forge/，2026-09-17 改名
├── build.gradle             ForgeGradle 6 + 官方(official)映射
├── gradle.properties        ★ 版本号(mod_version) / JDK17 路径
├── settings.gradle
└── src/main/
    ├── java/com/mmap/client/          33 个原模组类（+ 新增 2 个）
    │   ├── MMmapMod.java              @Mod 入口
    │   ├── MMmapClient.java           客户端初始化（按键/事件/生命周期）
    │   ├── network/NetworkHandler.java 网络接收（Forge SimpleChannel）
    │   ├── command/EncryptCommand.java 本地加密逻辑
    │   ├── command/MMmapCommands.java  客户端指令注册（新）
    │   ├── config/ crypto/ image/ mythic/ region/ render/ warp/ tools/
    └── resources/
        ├── META-INF/mods.toml         Forge 模组描述
        ├── pack.mcmeta
        └── assets/mmap/lang/{zh_cn,en_us}.json
```

> 2026-09-17 整理时，从 `src/main/java/` 下清出了 5 个混进去的 Fabric 残留文件
> （`fabric.mod.json`、`mmap-refmap.json`、`mmap.mixins.json`、`META-INF/`、`assets/`），
> 它们本来就不参与编译。已移到 `../archive/port-work/fabric-leftovers/`。

编译（需 JDK 17）：

```bat
gradlew.bat build            :: 产物 build/libs/mmap-1.9.5.jar（名字跟着 mod_version 走）
```

**推荐用工作区的包装脚本**，它会顺便把产物改名成 `mmap-forge-<版本>.jar` 发布到 `../dist/client-mod/`：

```powershell
powershell -ExecutionPolicy Bypass -File ..\tools\build-mod.ps1
```

（已自带 Gradle Wrapper 8.1.1，首次运行会自动下载 Gradle；也可以直接用本机 gradle。）

`gradle.properties` 里的 `org.gradle.java.home=E:/jdk-17.0.20.1+1` 是本机 JDK 17 路径，
换机器时改成自己的 JDK 17 路径，或者删掉这一行（把 `JAVA_HOME` 指向 JDK 17 即可）。
本工作区已经放好了一份 JDK 17（`E:\jdk-17.0.20.1+1`），所以直接构建即可。

## 5. 移植对照表（Fabric API → Forge 47.4.10）

| Fabric/原实现 | Forge 实现 |
|---|---|
| `fabric.mod.json` | `src/main/resources/META-INF/mods.toml` |
| `ClientModInitializer#onInitializeClient` | `@Mod` 构造函数 + `MMmapClient.init()` |
| `KeyBindingHelper.registerKeyBinding` | `RegisterKeyMappingsEvent`（mod 事件总线） |
| `KeyMapping.wasPressed()/isPressed()` | `KeyMapping.consumeClick()` |
| `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent`（`Phase.END`） |
| `ClientPlayConnectionEvents.DISCONNECT` | `ClientPlayerNetworkEvent.LoggingOut` |
| `HudRenderCallback` | `RenderGuiEvent.Post` |
| `ClientPlayNetworking.registerGlobalReceiver` | `NetworkRegistry.newSimpleChannel` + `SimpleChannel.registerMessage` |
| `FabricLoader.getGameDir() / getConfigDir()` | `FMLPaths.GAMEDIR / FMLPaths.CONFIGDIR` |
| Mixin 拦截 `ClientPacketListener#sendCommand` | `RegisterClientCommandsEvent`（Forge 原生客户端指令，**已彻底去掉 Mixin**） |
| `FabricClientCommandSource#sendFeedback` | `CommandSourceStack#sendSuccess` |

### 网络实现的关键点

1. 插件发出来的数据是 `[包类型字节 1~9][数据...]`。
2. Forge 的 `SimpleChannel` 会在每条消息最前面额外读写 **1 个字节作为消息索引**。
   所以这里把包类型 **1~9 注册成消息索引 1~9**，正好“复用”插件已经有了的那个首字节，
   解码器里捕获索引得知是哪个包，payload 一个字节都不用改。
3. 注册通道后，Forge 在登录完成时会自动执行
   `NetworkHooks.sendMCRegistryPackets(connection, "PLAY_TO_SERVER")`，
   也就是替客户端向服务器发 `minecraft:register`（带上 `mmap:main`）。
   Bukkit 只给“已注册该通道”的玩家发 `sendPluginMessage`，所以这一步是能收到数据的必要条件。
4. 因此**不需要任何 Mixin / refmap**，工程里也彻底没有 Mixin 依赖。

### 反编译与重映射流程（本次移植是怎么做的）

原 jar 只有字节码且是 Fabric **intermediary** 命名（`class_310` / `method_1551` 这种），所以：

1. 拉取 Fabric `intermediary-1.20.1-v2` 映射与 Mojang 官方 `client.txt`（1.20.1）映射；
2. 按 `(类, 名字, 描述符)` 合并成 **intermediary → Mojang 官方名** 的 tiny-v2 映射表，
   共 68666 个成员 + 7423 个类，**解析率 100%，零歧义**；
3. 用 `tiny-remapper`（classpath 传入重映射成 intermediary 的官方 client.jar，
   否则引用型成员不会被改名）把模组 jar 重映射成 **Mojang 官方命名**；
4. 用 Vineflower 反编译 → 得到和 Forge/官方映射一致的可读源码；
5. 只有 Fabric API 相关部分被改写为 Forge 等价实现，其余渲染 / 解析逻辑是 1:1 保留。

## 6. 已验证 / 未验证

已验证：

* `gradle build` 干净编译通过 —— 也就是说所有对 Minecraft / Forge API 的调用
  都在真实的 Forge 1.20.1-47.4.10 + MC 1.20.1 类上解析成功（方法、字段、描述符全对）。
* `reobfJar` 正常执行，产物里的 Minecraft 成员已转换为 SRG 名（生产环境需要）。
* 专用服务器能加载本模组（`mods.toml` 解析、`@Mod` 构造、不会误加载客户端类）。
* **2026-09-17 14:14 在 PCL「时之旅者」（Forge 47.4.10）实例实机跑过一次，
  连 Paper 服务端联调成功**，客户端日志摘录：

  ```
  Found valid mod file mmap-forge-1.9.5.jar with {mmap} mods - versions {1.9.5}
  [mmap/]: 已注册网络包接收器，通道: mmap:main
  [mmap/]: Channel 'mmap:main' : Vanilla acceptance test: ACCEPTED
  [mmap/]: 收到服务端握手 - 协议版本: 1, 标识: MMmap-Server, 传送点消失阈值: 0.8, 当前世界: world, 怪物头颅fallback: true
  [mmap/]: 已同步 2 个地图图片绑定 / 已同步 1 个多边形区域
  [mmap/]: 已加载地图图片: main.mmap (543x575) -> mmap:map_image/main_map [CLAMP_TO_EDGE] glId=60
  ```

  也就是说：模组被 Forge 正常加载、通道注册成功、能收到插件数据、图片能上传成 GL 纹理、大地图打开过。

未验证：

* 画面**细节**没有人工确认：小地图/大地图的区域绘制、传送点图标、MythicMobs 头颅、FBO 表现。
* 矩形区域（`shape: rectangle`）路径没实测过。已核实其影响面**只有**小地图上「当前所在区域名」那一行字，
  其他显示正常；**决定不修，服务端一律改用 `shape: polygon` 配区域**（4 个顶点即矩形），
  理由与示例见 [`../docs/PROTOCOL.md` 第 5.1 节](../docs/PROTOCOL.md)。
* 已知 Forge 侧报错 `Duplicate handler name: forge:vanilla_filter`（重连 Paper 服务端时出现，
  堆栈无 mmap 代码，实测不影响功能），见 [`../docs/DEPLOY.md` 第 5 节](../docs/DEPLOY.md)。

联调时如果收不到数据，按 [`../docs/DEPLOY.md` 第 4 节](../docs/DEPLOY.md) 的排查顺序走：
先看客户端日志有没有 `已注册网络包接收器，通道: mmap:main` 和 `收到服务端握手`，
再确认插件日志里有没有「已同步 N 个区域」。
