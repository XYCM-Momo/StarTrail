# 变更记录 CHANGELOG

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。
**每个会让玩家需要换 jar 的改动**都要在这里留一条，并写清「要不要同时升级客户端/服务端」。

---

## [未发布]

（下一次改动的暂存区）

---

## Forge 移植版 1.9.7 · 2026-09-17

**修复：地图图片的世界边界整体偏 1 格（配置里必须把四个坐标统统 -1 才能对齐）。**

* ⚠️ **需要玩家更新客户端模组**（`mmap-forge-1.9.7.jar`）；
  **服务端插件与协议不变**，但**升级后必须把 `plugins\MMmap\images.yml` 的坐标改回真实边界**
  （去掉之前的 -1 补偿），否则会反向偏 1 格。
* **原因**：`WorldMapScreen.renderMapImages()` 计算图片四个屏幕顶点时硬编码了 `+ 1.0F`：

  ```java
  // 旧代码
  screenX1 = worldToScreenX(image.getMinX() + 1.0F);
  screenX2 = worldToScreenX(image.getMinX() + imageWidth + 1.0F);
  ```

  整张图因此被往**东/南各推 1 格**，玩家只能在配置里全写小 1 抵消。
* **顺带发现**：世界地图画图片的跨度取自**图片像素尺寸**（`imageWidth`），配置里的 `maxX/maxZ`
  只用于裁剪、不参与绘制；而**小地图**用的是配置的 `minX..maxX` 映射 UV（跨度 `maxX - minX`）。
  两个视图算法不一致 → 同一张图在两张图上差 1 格。
* **改法**：世界地图改为按配置矩形定位与铺图（西/北 = `minX/minZ`，东/南 = `maxX/maxZ`，
  纹理拉伸铺满），去掉 `+1.0F`；跨度改用 `maxX - minX`（配置跨度 ≤ 0 时退回图片像素尺寸）。
  改完后两个视图**用同一套边界规则**。
* 改动仅 `WorldMapScreen.java` 一个文件；渲染细节、协议、服务端逻辑均未改动。

**验证**：

* ✅ `gradlew clean build` 编译通过，产物 `mmap-forge-1.9.7.jar`（`E50716B1…`）。
* ✅ AES 解开客户端里的 `main.mmap` 读 PNG IHDR，确认 `main.png` 实际是 **860×860** 像素，
  与「真实边界 -429 → 431，跨度 860」完全一致 → 1 像素 = 1 格，修正后不会有任何拉伸。
* ⏳ **尚未部署**：打包时使用者正在游戏内（jar 被占用），需关掉游戏后覆盖 `mods\`。
* ⚠️ **待使用者实机确认**：改用真实边界后图片是否与地形完全对齐。

---

## Forge 移植版 1.9.6 · 2026-09-17

**修复：打开聊天栏（以及其它界面）时小地图消失。**

* ⚠️ **需要玩家更新客户端模组**（`mmap-forge-1.9.6.jar`）。
  **服务端插件与协议不变，服务端不用动。** Fabric 版仍是 1.9.5（这次只改了 Forge 侧）。
* **原因**：`MinimapManager` 里有一句「只要开着界面就不画小地图」（继承自上游 Fabric 模组的行为）。
  而 Forge 的 `RenderGuiEvent.Post` 在开着界面时**照样会触发** ——
  `GameRenderer` 里是 `if (!hideGui || screen != null) gui.render(...)`，
  这正是「聊天栏打开时快捷栏仍然可见」的原因；界面本身是在 HUD 之后才画的。
  所以小地图是被自己那行判断挡掉的，不是引擎不画。
* **改法**：默认**一直显示小地图**，只在本模组自己的大地图界面隐藏；
  新增配置项 `minimap.hideInGui`（`config/mmap.json`，默认 `false`）可切回旧行为。
  改动文件：`MinimapManager.java`（渲染判断）、`ConfigManager.java`（新增配置字段）。
  渲染细节、网络协议、服务端逻辑均未改动。
* 新增配置项在旧配置文件里不存在时会自动取默认值（Gson 走无参构造，字段初始值生效），
  无需手动改 `config/mmap.json`。

**验证**：

* ✅ `gradlew clean build` 编译通过，产物 `mmap-forge-1.9.6.jar`（`F47099C3…`）。
* ✅ jar 内 `mods.toml` 的 `version="1.9.6"` 正确。
* ✅ 已部署到 Forge 实例 `versions\时之旅者\mods\`，并**删除了旧的 1.9.5 jar**
  （同一 modId 放两个 jar 会导致加载失败），部署后哈希与 `dist/` 一致。
* ⚠️ **待使用者实机确认**：打开聊天栏 / 背包 / ESC 菜单时小地图是否按预期保留。

**产物变化**：`dist/client-mod/` 里换成 `mmap-forge-1.9.6.jar`，
旧的 `mmap-forge-1.9.5.jar` 已移出 dist（冻结在 `releases/mod-1.9.5_plugin-1.9.4/` 里可回滚）。

---

## 2026-09-17 · 安装 Git、部署重建模组、实机确认（不涉及版本号）

* **安装 Git** 2.55.0 到 `D:\Git`（仅当前用户、无需管理员；配置全部写在 `D:\Git\etc\gitconfig`，
  C 盘只有一条 PATH 环境变量），并把 `D:\Git\cmd` 加入用户 PATH。
* **工作区纳入版本控制**：`git init`（分支 `main`），首次提交 216 个文件。
  纳管范围见 `.gitignore`：源码/资源/文档/`dist/`/`releases/`/`tools/` 进库，
  `backups/`、两个工程的 `build/` 与 `.gradle/`、`archive/port-work/` 里的大二进制不进库
  —— 所以 **Git 不能替代 `backups/`**。
  提交身份：**`XY_Momo <1756784800@qq.com>`**（本仓库与系统级都已设置；系统级那份在
  `D:\Git\etc\gitconfig`，以后新建的仓库也用它）。首次提交时的占位身份已一并改写为它。
* **部署重建后的 Forge 模组**到 `versions\时之旅者\mods\mmap-forge-1.9.5.jar`，
  与 `dist/` 哈希一致（`C990704B…`）。部署前的旧文件留档在
  `backups\_deployed-old-mmap-forge-1.9.5.jar`（不在快照范围内，是单独留的）。
* **使用者实机确认**：Forge 版画面正常，从 Fabric 版迁移没有问题。

---

## 2026-09-17 · 建立服务端插件源码工程（不涉及版本号）

**改动**：新增 `plugin-paper/` —— 服务端插件 `MMmap-Plugin-1.9.4.jar` 的 Gradle 工程。
**插件版本号仍是 1.9.4，协议仍是 v1，行为与原版一致**（验证见下）。

在此之前插件是黑盒（只有编译好的 jar，改不了），现在可以改代码、编译、测试、发布了。

* 源码来源：Vineflower 反编译原 jar → 20 个 `.java` **原样**放进 `src/main/java/com/mmap/server/`；
  6 个资源文件从原 jar **逐字节**解出放进 `src/main/resources/`。
* 依赖：`paper-api:1.20.1-R0.1-SNAPSHOT`（repo.papermc.io）+ 服务端里的 `MythicMobs-5.6.2.jar`，
  两者都是 `compileOnly`，不打进 jar。
* 新增工具：`tools/build-plugin.ps1`（编译+发布到 dist）、`tools/smoke-test-plugin.ps1`（一次性 Paper 服冒烟测试）。

**等价性验证（全部通过）**：

| 检查项 | 结果 |
|---|---|
| jar 条目清单 | 36 项对 36 项完全一致（含 29 个 .class） |
| 6 个 yml + MANIFEST.MF | 逐字节一致（SHA256 相同） |
| 29 个类 `javap -p` 成员声明比对 | 字段/方法/修饰符全部一致，零差异 |
| 一次性 Paper 1.20.1 实机启动 | 正常加载并启用，无任何 ERROR/Exception |
| 一次性 Paper + **完整 MythicMobs 5.6.2** 实机启动 | `MythicMobs 集成: 已启用`、`怪物头像同步已启动 (半径 128.0)`，无 MMmap 异常 |

> MythicMobs 那条路径的验证有个坑：测试服里只放 MythicMobs 的 jar 是**不够的**，
> 它自己会因为缺少数据目录而 NPE 启动失败（`Error occurred while enabling MythicMobs`）。
> `tools\smoke-test-plugin.ps1 -WithMythicMobs` 现在会连它的数据目录一起复制过去。
> 另外该脚本现在会把「MMmap 自己的报错」和「其它插件的报错」分开判定，
> 只有前者才会让测试失败。

**产物变化**：`dist/server-plugin/MMmap-Plugin-1.9.4.jar` 换成了**从源码编译**的版本
（SHA256 `3E6AEF9C…`，72,017 字节；原版是 `561B0789…`，72,076 字节）。
两者行为经上述四项检查判定等价。**原版 jar 完整保留在** `releases/mod-1.9.5_plugin-1.9.4/`。

**线上服务端已于同日换用源码编译版**：`时旅服务端\plugins\MMmap-Plugin-1.9.4.jar`
现为 `3E6AEF9C…`，与 `dist/` 哈希一致。
* 换之前先备份了服务端插件数据目录（5 个文件：`config.yml`、`images.yml`、`language.yml`、
  `regions\regions.yml`、`warps\main-city.yml`）→ `backups\_live-server-MMmap-data_20260917_162810.zip`。
* 数据目录 `plugins\MMmap\` 未做任何改动。
* 回滚方式：把 `releases/mod-1.9.5_plugin-1.9.4/MMmap-Plugin-1.9.4.jar` 覆盖回去即可。

---

## 2026-09-17 · 构建改为可复现（不涉及版本号）

**改动**：`mod-forge/build.gradle` 里给 `jar` 任务加上 `preserveFileTimestamps = false`、
`reproducibleFileOrder = true`，并去掉 MANIFEST 里的 `Implementation-Timestamp`（构建时间）。
**源码与 jar 内的类文件一个字节都没改**，只是让构建结果不再随时间变化。

**为什么**：以前每次编译，MANIFEST 里的构建时间戳都不同，导致 jar 的 SHA256 每次都变，
`SHA256SUMS.txt` / `releases/` 的校验值形同虚设，也没法用哈希判断「这个 jar 是哪份源码编的」。

**验证**：连续两次 `gradlew clean build`，产物 SHA256 完全相同 ——
`C990704B8BE0890D98A7107FCB4F49B17EAC507670901C6806FDD76E28AAE563`。

**影响**：`dist/client-mod/mmap-forge-1.9.5.jar` 的哈希从 `A089745C…` 变成 `C990704B…`。
和已经装进客户端的旧 jar **逐类一致**（只有 MANIFEST 不同），功能完全相同，不换也不影响使用。
Fabric 模组与服务端插件不受影响。

---

## 2026-09-17 · 工作区整理（不涉及版本号）

**改动**：把散在根目录的文件收进分类目录，补齐版本规划与备份机制。**没有改任何代码，成品 jar 内容未变。**

* 新增 `dist/`（成品区）、`releases/`（发布冻结区）、`docs/`、`tools/`、`backups/`、`archive/`。
* 目录改名/移动：
  * `MMmap-Forge/` → **`mod-forge/`**
  * `_port_work/` → **`archive/port-work/`**
  * `移植说明.md` → **`docs/移植说明.md`**
  * `mmap-1.9.5.jar` → **`dist/client-mod/mmap-fabric-1.9.5.jar`**（加平台后缀，避免和 Forge 产物同名）
  * `mmap-forge-1.9.5.jar` → **`dist/client-mod/mmap-forge-1.9.5.jar`**
  * `MMmap-Plugin-1.9.4.jar` → **`dist/server-plugin/MMmap-Plugin-1.9.4.jar`**
* 清理 `mod-forge/src/main/java/` 下混进去的 5 个 Fabric 残留文件（`fabric.mod.json`、
  `mmap-refmap.json`、`mmap.mixins.json`、`META-INF/`、`assets/`）→ 移到 `archive/port-work/fabric-leftovers/`。
  它们本来就不参与编译，只是噪音。
* 新增工具：`tools/build-mod.ps1`、`tools/verify-dist.ps1`、`tools/backup.ps1`。
* 新增文档：`README.md`、`VERSIONING.md`、`CHANGELOG.md`、`docs/PROTOCOL.md`、`docs/DEPLOY.md`。
* 新增 `dist/SHA256SUMS.txt`。

**验证**：移动后重新执行 `gradlew.bat build` → **BUILD SUCCESSFUL**（18 秒）；
新产物与原成品逐类比对，**除 `META-INF/MANIFEST.MF` 里的构建时间戳外完全一致**（所有 .class 哈希相同）。

**技术决策（同日）**：查到「矩形区域（包 2/3）的世界名是 Bukkit 短名 `world`，
多边形/传送点/怪物（包 6/7/9）是命名空间 `minecraft:overworld`，客户端严格比较」的问题。
逐条核实后确认**影响面只有小地图上「当前所在区域名」那一行字**（其他绘制路径根本不按世界过滤），
且多边形路线完好、还能自己画名字。因此**决定不修**，改为**服务端一律用 `shape: polygon` 配区域**
（4 个顶点的多边形即矩形）。详见 [`docs/PROTOCOL.md` 第 5.1 节](docs/PROTOCOL.md)。
另附带记录一个上游原有行为：绘制不看世界，配在 `world` 的矩形在下界/末地也会被画出来。

**回滚**：整理前的完整状态在 `backups/MMmap_20260917_151316_01-整理前基线.zip`。

---

## Forge 移植版 1.9.5 · 2026-09-17

**把上游 Fabric 客户端模组 `mmap-1.9.5.jar` 移植成 Forge 47.4.10 客户端模组。**

* 新增 `mod-forge/` 完整 Gradle 工程，产物 `dist/client-mod/mmap-forge-1.9.5.jar`。
* 移植方式：intermediary → Mojang 官方映射（68666 成员 / 7423 类，解析率 100%）→ tiny-remapper
  重映射 → Vineflower 反编译 → 只改写 Fabric API 相关部分，渲染与解析逻辑 1:1 保留。
* 关键替换：`@Mod` 入口、`RegisterKeyMappingsEvent`、`TickEvent.ClientTickEvent`、
  `ClientPlayerNetworkEvent.LoggingOut`、`RenderGuiEvent.Post`、Forge `SimpleChannel`（复用插件的包类型首字节）、
  **彻底去掉 Mixin**，本地指令改用 `RegisterClientCommandsEvent`。
* 服务端**无需任何改动**，继续用插件 1.9.4；协议保持 v1。

**验证情况**：

* ✅ `gradle build` 干净编译通过（所有 MC / Forge API 调用都在真实 1.20.1-47.4.10 类上解析成功）。
* ✅ `reobfJar` 正常执行，产物里 Minecraft 成员已转成 SRG 名。
* ✅ 专用服务器能加载（`mods.toml`、`@Mod` 构造、不误加载客户端类）。
* ✅ jar 已装入 Forge 1.20.1-47.4.10 客户端实例 `自用客户端\.minecraft\versions\时之旅者\mods\`。
* ✅ **2026-09-17 14:14 已在 时之旅者（Forge）实例实机跑过一次，与 Paper 服务端联调成功**，日志证据：
  * `Found valid mod file mmap-forge-1.9.5.jar with {mmap} mods - versions {1.9.5}`（Forge 认出了模组）
  * `已注册网络包接收器，通道: mmap:main` + `Channel 'mmap:main' : Vanilla acceptance test: ACCEPTED`
  * `收到服务端握手 - 协议版本: 1, 标识: MMmap-Server, 传送点消失阈值: 0.8, 当前世界: world, 怪物头颅fallback: true`
  * `已同步 2 个地图图片绑定` / `已同步 1 个多边形区域`（**0 个矩形区域、0 个传送点**，服务器就是这么配的）
  * `已加载地图图片: main.mmap (543x575) -> mmap:map_image/main_map [CLAMP_TO_EDGE] glId=60`（图片成功上传成 GL 纹理）
  * 大地图打开过并持久化了缩放（`config\mmap.json` 里 `lastZoom` 有值）
* ✅ **2026-09-17 使用者实机确认**：从 Fabric 版转到 Forge 版**没有问题**，画面表现一致
  （小地图/大地图绘制、传送点、怪物头颅等）。
* ✅ 同日已把重建后的 jar 部署到 Forge 实例 `versions\时之旅者\mods\`，与 `dist\` 哈希一致。
* ⚠️ 已知问题见 [`docs/DEPLOY.md` 第 5 节](docs/DEPLOY.md)：
  Forge 客户端第二次连 Paper 服务端时会报 `Duplicate handler name: forge:vanilla_filter`
  （Forge 自身的网络过滤器重复注入，堆栈里没有 mmap 代码，实测不影响功能）；
  以及 [`docs/PROTOCOL.md` 第 5 节](docs/PROTOCOL.md) 记录的「矩形区域世界名格式不一致」潜在 bug。

---

## 服务端插件 1.9.4 · 2026-08-10

* 服务端 Paper 1.20.1 插件成品进入本工作区，只有编译好的 jar，**没有可编译的源码工程**
  （反编译源码在 `archive/port-work/src-plugin/`，仅供阅读）。
* 协议 v1：通道 `mmap:main`，包类型 1~9。
* 已知行为：插件按 `plugins/MMmap/` 下的 yml 组织数据（`config.yml`、`language.yml`、`images.yml`、
  `regions/`、`warps/`），支持文件夹形式分文件管理。

---

## Fabric 上游原版 1.9.5 · 2026-08-10

* 上游 Fabric 客户端模组 `mmap-1.9.5.jar` 进入本工作区，作为移植基准保留，
  现存放为 `dist/client-mod/mmap-fabric-1.9.5.jar`。**这份文件不做任何修改。**

---

## 历史遗留信息（整理时记录，来源为文件时间戳与原有说明）

* 更早的 1.9.3 版本：插件 `config.yml` 里注明了「已清理 v1.9.3：移除全部无用的旧配置
  （区块颜色/同步距离/自动保存/日志开关，代码均未读取）」。
* 上游 Fabric 版与插件的更早版本、以及本工作区之外的改动历史目前**没有记录**
  （本机没装 Git，之前也没有变更日志）。
