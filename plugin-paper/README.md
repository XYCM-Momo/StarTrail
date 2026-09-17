# MMmap — Paper 服务端插件工程

这个目录是**服务端插件 `MMmap-Plugin-1.9.4.jar` 的源码工程**。2026-09-17 建立。

在此之前插件是个黑盒（只有编译好的 jar，改不了、修不了）；现在可以正常改代码、编译、测试、发布了。

> 工作区总览 / 版本规划 / 备份部署见 [`../README.md`](../README.md)、[`../VERSIONING.md`](../VERSIONING.md)、
> [`../docs/DEPLOY.md`](../docs/DEPLOY.md)；网络协议见 [`../docs/PROTOCOL.md`](../docs/PROTOCOL.md)。

---

## 1. 怎么来的

原 jar 没有源码，所以这套工程是**从字节码恢复**出来的：

1. 用 Vineflower 反编译 `MMmap-Plugin-1.9.4.jar` → `../archive/port-work/src-plugin/`；
2. 把 20 个 `.java` **原样**放进 `src/main/java/com/mmap/server/`（一个字都没改）；
3. 把 6 个资源文件（`plugin.yml`、`config.yml`、`language.yml`、`regions.yml`、
   `polygon-regions.yml`、`warps.yml`）**从原 jar 里逐字节解出来**放进 `src/main/resources/`；
4. 配好 Gradle 工程编译通过。

所以：**这份源码的行为等价于原版 1.9.4**，但它是反编译产物，不是原始工程
（局部变量名是 `var4`、`var10` 这种，可读性一般）。

## 2. 编译

```powershell
# 推荐：编译 + 发布到 dist\server-plugin\ + 刷新校验清单
powershell -ExecutionPolicy Bypass -File ..\tools\build-plugin.ps1

# 或者直接 Gradle（产物在 build\libs\MMmap-Plugin-1.9.4.jar）
gradlew.bat build
```

> 改 `src\main\resources\plugin.yml` 里的 `version` 就是改插件版本号 —— 唯一来源，
> 产物名 `MMmap-Plugin-<版本>.jar` 跟着它走，见 [`../VERSIONING.md`](../VERSIONING.md) 第 1 节。

**编译前先跑冒烟测试**（起一个一次性 Paper 服，不碰线上服务端）：

```powershell
powershell -ExecutionPolicy Bypass -File ..\tools\smoke-test-plugin.ps1
powershell -ExecutionPolicy Bypass -File ..\tools\smoke-test-plugin.ps1 -WithMythicMobs -KeepServer
```

## 3. 依赖

| 依赖 | 用途 | 来源 |
|---|---|---|
| `io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT` | Bukkit/Paper API | `repo.papermc.io`，`compileOnly` |
| `MythicMobs-5.6.2.jar` | `io.lumine.mythic.*`（怪物头颅同步） | **服务端里那一份**，`compileOnly` |

两者都是 `compileOnly`，**不会打进我们的 jar**（服务端自己提供）。

MythicMobs 是付费插件、公共仓库没有，所以在 `gradle.properties` 里用一个路径指过去：

```properties
mythicmobs_jar=E:/Minecraft/<服务器目录>/<服务端子目录>/plugins/MythicMobs-5.6.2.jar
```

> ⚠️ `gradle.properties` 必须保持**纯 ASCII**：Gradle 按 ISO-8859-1 解码这个文件，
> 直接写中文路径会变乱码、构建失败。所以中文路径用 `\uXXXX` 转义写（Java properties 语法），
> 现在的写法就是转义过的。换机器时改这一行。

## 4. 源码结构

```
src/main/
├── java/com/mmap/server/
│   ├── MMmapPlugin.java            JavaPlugin 入口：onEnable/onDisable，装配各管理器
│   ├── command/MMmapCommand.java    /mmmap add|remove|list|info|reload|help 及 tab 补全
│   ├── config/EnterCommandConfig.java   进入区域执行命令（tellraw/title 等）
│   ├── image/MapImage.java, MapImageManager.java    地图图片绑定（images.yml）
│   ├── mythic/MythicMobHook.java, MythicMobTracker.java   MythicMobs 钩子 + 每 2 秒扫描同步
│   ├── network/PacketSender.java   ★ 协议实现：包类型 1~9，通道 mmap:main
│   ├── player/PlayerTracker.java   进服/换世界/退出 → 触发同步
│   ├── player/PolygonRegionTracker.java   多边形区域进出检测 → enter-commands
│   ├── region/MapRegion, PolygonRegion, RegionManager, PolygonRegionManager, RegionType
│   ├── util/ColorUtil, LanguageManager, YamlFolderLoader
│   └── warp/WarpPoint, WarpPointManager
└── resources/
    ├── plugin.yml        ★ 插件描述 + 版本号（也是 build 的版本来源）
    ├── config.yml        默认配置（首次启用会复制到 plugins\MMmap\）
    ├── language.yml      默认语言文件
    ├── regions.yml       区域默认模板（会被复制成 plugins\MMmap\regions\default.yml）
    ├── warps.yml         传送点默认模板（→ plugins\MMmap\warps\default.yml）
    └── polygon-regions.yml  旧版遗留模板，代码里没引用，为保持与原 jar 一致而保留
```

## 5. 与原版 jar 的等价性验证（2026-09-17）

| 检查项 | 结果 |
|---|---|
| jar 内条目清单 | 36 项对 36 项，**完全一致**（含 29 个 .class） |
| 6 个 yml 资源 + MANIFEST.MF | **逐字节一致**（SHA256 相同） |
| 29 个类用 `javap -p` 比对成员声明 | 字段/方法/修饰符**全部一致**，零差异 |
| 一次性 Paper 1.20.1 实机启动 | **加载并启用成功，无任何 ERROR/Exception** |

实测启动日志（`tools\smoke-test-plugin.ps1` 的输出）：

```
[MMmap] Loading server plugin MMmap v1.9.4
[MMmap] Enabling MMmap v1.9.4
[MMmap] MMmap Plugin 正在启用...
[MMmap] 已创建配置文件夹: ...\plugins\MMmap\regions
[MMmap] 已复制默认模板到: ...\plugins\MMmap\regions\default.yml
[MMmap] 成功加载 0 个矩形区域
[MMmap] 未找到 images.yml，将创建默认文件
[MMmap] 多边形加载完成: 0 个
[MMmap] 已创建配置文件夹: ...\plugins\MMmap\warps
[MMmap] 已复制默认模板到: ...\plugins\MMmap\warps\default.yml
[MMmap] 传送点加载完成: 0 个
[MMmap] MythicMobs 未安装或未启用，跳过怪物头像同步
[MMmap] MMmap Plugin 已成功启用！
[MMmap]   区域数量: 0 / 地图图片绑定数量: 0 / 多边形区域数量: 0 / 传送点数量: 0
Done (8.229s)!
```

顺带确认了资源加载路径正确：测试服自动生成了 `plugins\MMmap\{config.yml, language.yml, images.yml,
regions\default.yml, warps\default.yml}`，其中 `regions\default.yml`、`warps\default.yml`
就是 jar 里那两个模板文件的副本。

**还没验证的**：玩家实际进服后的同步流程（需要真客户端连上测试服），
以及 MythicMobs 集成那条路径（`-WithMythicMobs` 可以测加载，但要有怪物才看得到同步）。

## 6. 改插件前请记住

1. 这是**反编译源码**，改之前先在实机/测试服确认原行为，别想当然；
2. 改完**必须**跑 `tools\smoke-test-plugin.ps1`，通过后再往线上服务端放；
3. 动了协议（`PacketSender` 的包类型/字段）就按 [`../docs/PROTOCOL.md`](../docs/PROTOCOL.md)
   和 [`../VERSIONING.md`](../VERSIONING.md) 第 4 节走 —— 客户端必须同步升级；
4. 线上服务端的插件数据在 `plugins\MMmap\`（`config.yml`、`language.yml`、`images.yml`、
   `regions\*.yml`、`warps\*.yml`），**改代码不会动这些数据**，但换 jar 前建议先备份那个目录。
