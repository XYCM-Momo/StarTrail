# 变更记录 CHANGELOG

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。
**每个会让玩家需要换 jar 的改动**都要在这里留一条，并写清「要不要同时升级客户端/服务端」。

---

## [模组 1.0.1] — 2026-10-24（**只换客户端，插件 1.0.0 不变**）

**修复：大地图打开设置面板后，区域名与传送点名「浮」在面板上方，遮挡关系颠倒。**

* **现象**：大地图左下角齿轮打开设置后，主城区域名（含下方类型行）、主城/竞技场传送点名如果正好落在设置面板范围内，
  不会隐藏也不会被压在面板下方，而是直接盖在面板上方，压住「显示玩家头像 / 显示怪物头像」等设置项。
* **成因**：面板底色走 `context.fill()`，名字走 `context.drawString()`。1.20.1 的 GUI 里文字 glyph
  与 fill 分属不同绘制批次、**文字批次更晚提交**，所以后画的面板底色盖不住文字。
  右键选项栏早就踩过同一个坑（`renderContextMenu` 里那段「不抬层盖不住文字本身」的注释），设置面板当时漏了同样的处理。
* **修法**（与右键选项栏保持同一套）：
  1. 设置面板整体抬到 tooltip 同层 `z=400`（`renderSettingsOverlay` 里 pushPose / translate / popPose）。
  2. 名字避让遮挡区：区域名（含下方类型行）、多边形区域名、传送点名只要与设置面板矩形相交就不绘制。
     由新增的 `getOverlayHideRect()` / `getSelectedHideRect()` / `getSettingsHideRect()` 统一给出遮挡矩形，
     右键菜单与传送点详情面板原有的避让逻辑一并归并到同一处，行为不变。
  3. 图标不处理：图标是 `fill` / `blit` 画的，本来就会被抬层后的面板正常盖住。
* **影响面**：只改客户端渲染，**没有动网络协议、没有动通道名**，插件无需更换。
* **要不要换 jar**：客户端 `startrail-forge-1.0.1.jar` → `versions\时之旅者\mods\`，**需重启游戏**。
  服务端 `StarTrail-Plugin-1.0.0.jar` 保持不变。

---

## [插件 1.0.0 / 模组 1.0.0] — 2026-10-14（**正式命名「星轨 StarTrail」，两端都要换**）

**产品定名「星轨 StarTrail」，署名 星野尘梦工作室 · 默默。版本号归零到 1.0.0，与 MMmap 时代切割。**

* ⚠️ **两端必须成对升级**：服务端 `StarTrail-Plugin-1.0.0.jar` **且** 客户端 `startrail-forge-1.0.0.jar`。
  **网络通道名由 `mmap:main` 改为 `startrail:main`** —— 新插件配旧模组、或旧插件配新模组都收不到数据。
* **命名与标识改动**：

  | 项目 | 旧 | 新 |
  |---|---|---|
  | 产品名 | MMmap | **星轨 StarTrail** |
  | Java 包 | `com.asterovie.momo.mmmap` | `com.asterovie.momo.startrail` |
  | 主要类 | MMmapPlugin / MMmapCommand / MMmapMod / MMmapClient / MMmapCommands | **StarTrailPlugin / StarTrailCommand / StarTrailMod / StarTrailClient / StarTrailCommands** |
  | 插件名 | `MMmap` | **`StarTrail`**（数据目录随之变为 `plugins/StarTrail/`） |
  | 模组 ID | `mmap` | **`startrail`**（材质命名空间、jar 名随之变化） |
  | 命令 | `/mmmap`（别名 mmap/map/minimap） | **`/startrail`**（别名 st/trail/map/minimap） |
  | 权限节点 | `mmap.command*` | `startrail.command*` |
  | 网络通道 | `mmap:main` | `startrail:main` |
  | 握手标识 | `MMmap-Server` | `StarTrail-Server` |
  | 客户端数据目录 | `.minecraft/mmap/` | `.minecraft/startrail/` |
  | 客户端配置 | `config/mmap.json` | `config/startrail.json` |
  | 加密图片扩展名 | `.mmap` | `.startrail`（**读取时兼容旧 `.mmap`**） |

* **启动署名**：插件与模组启动时都会在日志打印横幅——
  `StarTrail · 星轨  v1.0.0` / `本插件由 星野尘梦工作室 · 默默 制作`。
* 🔒 **刻意保持不变（很重要）**：加密文件的 **AES 密钥种子仍是 `MMmap-Xaero-Static-Image-2026`**，
  文件魔数 `MMAP` 与版本号也没动。这是**文件格式常量，不是品牌**——改名会让所有已加密图片解不开。
  代码里已加醒目注释；`tools/waypoint-editor` 的 `MMAP_KEY_STR` 必须与它一致。
* **数据迁移（已执行，玩家/服主无需手动操作）**：
  * 服务端 `plugins/MMmap/` → **`plugins/StarTrail/`**（config.yml、images.yml、language.yml、regions/、warps/、waypoints/ 共 6 个数据文件随迁，主城 300 点位在内）
  * 客户端 `.minecraft/mmap/` 与 `versions/时之旅者/mmap/` → **`startrail/`**；22 个加密图片重命名为 `.startrail`
  * `config/mmap.json` → `config/startrail.json`；其中的 `/mmap tp`、`/mmap warp nav|tp` 命令模板改写为 `/startrail ...`
  * `options.txt` 键位 `key_key.mmap.*` → `key_key.startrail.*`（**保住原有 M / N 绑定，不会重置**）
  * 服务端 language.yml 的 `/mmap` 帮助文案、`[MMmap]` 聊天前缀一并改写
* **保留原样**：`dist/client-mod/mmap-fabric-1.9.5.jar`（上游 Fabric 版，不参与改名）、`archive/port-work/**`、
  本文件 1.0.0 之前的历史条目、`docs/移植说明.md`（Forge 移植过程记录）、`tools/backup.ps1` 的历史快照命名。
* **工具脚本已同步**：`build-mod.ps1` / `build-plugin.ps1` / `verify-dist.ps1` / `smoke-test-plugin.ps1` / `backup.ps1`、
  点位编辑器（含文件名改为 `StarTrail点位编辑器.html`）。
  ⚠️ **这些 .ps1 必须带 UTF-8 BOM**，否则 `powershell.exe`(5.1) 会把中文按 ANSI 解码成乱码并报语法错（本次踩到并已修复）。
  另外给 `backup.ps1` 补了 `.gradle-home\` 排除——此前它会把 Gradle 依赖缓存打进快照（1GB+）。
* **工作区目录已改名**：`插件\MMmap` → `插件\StarTrail`；两个 `gradle.properties` 的 JDK 绝对路径、
  文档与工具脚本内的路径已全部同步，改名后重新构建验证通过。
* **部署**：
  * `StarTrail-Plugin-1.0.0.jar`（74,784 字节，`7B27D5431A5240FF1E85A05F1ED28B3292C3A1461FBF42A238C74C06123D4040`）→ 服务端 `plugins\`
  * `startrail-forge-1.0.0.jar`（154,851 字节，`ABBC6D05BF76B4F09FC08AC0C35FC3CA655E5E430AB16722F4897AE4D95EC566`）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏**）
  * `dist\`、`releases\mod-1.0.0_plugin-1.0.0\`、`SHA256SUMS` 已更新，`verify-dist -Check` 全绿。
* **已实测验证**：`tools\smoke-test-plugin.ps1` 实跑**通过** —— 测试服日志 `[StarTrail] Loading server plugin StarTrail v1.0.0`
  → 署名横幅 → `StarTrail Plugin 已成功启用！`，数据目录正确落在 `plugins\StarTrail\`。
* **仓库与文档整理**：
  * 补 `LICENSE`（MIT 正文）——此前 `mods.toml` / `plugin.yml` 都声明了 MIT，但仓库里没有正文文件。
  * `README.md` 重写为**面向使用者**的项目说明（功能、安装、命令与权限、数据目录、从源码构建、必须改的本机路径）；
    原「工作区运维手册」内容迁到 `docs/WORKSPACE.md`（构建/备份/校验脚本用法、环境注意事项、编码坑），信息无丢失。
  * 修正 `VERSIONING.md` / `DEPLOY.md` 里因批量改名产生的**错误 jar 名**：`StarTrail-Plugin-1.9.4.jar`、
    `startrail-forge-1.9.7.jar` 这类名字从未存在过（改名时只换了名、版本号还是旧的），已改为当前版本或 `<版本>` 占位。
  * 仓库已推送到 GitHub 私有仓库 `XYCM-Momo/StarTrail`（`main` 分支）。
* **维护提醒**：改完 `tools\*.ps1` 请确认 **UTF-8 BOM 仍在**（丢了会让 PowerShell 5.1 中文乱码并报语法错）；
  改完 Markdown 文档请确认粗体标记 `**` 没有被自动改写工具吃掉（本次踩到过一次，已修复）。

---

## [插件 2.1.1 / 模组 2.2.7] — 2026-09-20（**包名重构，两端都要换 jar**）

> ⚠️ 本条与上面的 1.0.0 是同一天连续完成的中间步骤（先改包名、再整体改名），**已被 1.0.0 取代**，未长期驻留。

**改动：Java 包名 `com.mmap` → `com.asterovie.momo.mmmap`（纯重构，无功能变化）。**

* ⚠️ **两端都要更新**：服务端插件 2.1.0→**2.1.1**（`MMmap-Plugin-2.1.1.jar`）**且** 客户端模组 2.2.6→**2.2.7**（`mmap-forge-2.2.7.jar`）。**两个必须同时换**，不能只换一边。
* **新包结构**（保留 client/server 子包）：
  * 服务端插件：`com.mmap.server.**` → **`com.asterovie.momo.mmmap.server.**`**（19 个类）
  * 客户端模组：`com.mmap.client.**` → **`com.asterovie.momo.mmmap.client.**`**（39 个类）
* **改动范围**：58 个 java 文件（`package` / `import` 全量改写）、`plugin.yml` 的 `main:`、`plugin-paper/build.gradle` 的 `group`、`mod-forge/gradle.properties` 的 `mod_group_id`、`tools/smoke-test-plugin.ps1`（栈帧匹配正则改为兼容新旧包名）。
* **无需数据迁移**（已实测确认）：
  * 插件由 `plugin.yml` 的 `name: MMmap` 标识、模组由 `mods.toml` 的 `modId=mmap` 标识，**都与包名无关** → `plugins/MMmap/`（配置、点位、图片、区域、传送点）原样可用。
  * 存档文件（服务端 `images.yml`/`waypoints/*.json`、客户端 `mmap/*.json`/`mmap.json`）均为**纯字段 map**，未使用 `==: 完整类名` 标记 → 全量扫描确认无 `com.mmap` 类名残留。
  * 网络协议基于 `mmap:main` 通道 + 手动字节读写，与包名无关。
* **未改动**：`archive/port-work/**`（旧移植存档，保留原样）；`docs/DEPLOY.md` 里一处 `com.mmap` 是历史堆栈记录（描述当时日志原文）。
* **部署**：
  * `MMmap-Plugin-2.1.1.jar`（74,128 字节，`2C2317A7278103D959CE219BB774CA7E83162BBBF431376ED42FC04F508D5049`）→ 服务端 `plugins\`
  * `mmap-forge-2.2.7.jar`（153,741 字节，`679763AE248EB9EFE965CFA85EA01EFE2DA551E0E9E0BEDB6B0E753F16298B8B`）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏**）
  * dist\、releases\mod-2.2.7_plugin-2.1.1\、SHA256SUMS 已更新，`verify-dist -Check` 全绿。
* **jar 体积变化**：模组 150,551→153,741 字节（包路径变长，属正常）。

---

## [2.2.6] — 2026-09-20（客户端模组小版本）

**改动：Dijkstra 边权从 2D 改为 3D（为分层地图铺路），同层路径行为不变。**

* 🔧 **只需更新客户端模组** 2.2.5→**2.2.6**（`mmap-forge-2.2.6.jar`），服务端插件 2.1.0 不用动。
* **内容**：路径网络的边权 `sqrt(dx²+dz²)` → `sqrt(dx²+dy²+dz²)`（含 Y）。同一层点位 Y 几乎相等 → 结果与 2D 完全一致；**跨层连线（楼梯/电梯等显式竖向连线）按真实高度计权**，为后续分层地图（多楼层路径）提前铺路。
* **注意**：跨层移动必须通过**显式连线**（点位之间用 `/mmap waypoint link` 连起来）——Dijkstra 只走连线，不会自动把上下层点位连上，所以不会「穿楼板」。
* **性能**：每边多一次开方，复杂度不变 O((n+e)·log n)，300 点位依然微秒级。
* **涉及文件**：客户端 `nav/NavigationManager.java`（`dijkstra` 边权 3D）、`gradle.properties`（mod_version=2.2.6）。
* **部署**：`mmap-forge-2.2.6.jar`（150,551 字节，`0B3754936588DD5E7300EB3FF3A932E795E9BC0312AD5F0D6D92D5E5983BAB6F`）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏生效**）。dist\、releases\mod-2.2.6_plugin-2.1.0\、SHA256SUMS 已更新，`verify-dist -Check` 全绿。

---

## [2.2.5] — 2026-09-20（客户端模组小版本）

**修复：大地图右键菜单锚定屏幕坐标，拖拽平移地图时菜单钉在屏幕上不动。**

* 🔧 **只需更新客户端模组** 2.2.4→**2.2.5**（`mmap-forge-2.2.5.jar`），服务端插件 2.1.0 不用动。
* **原因**：菜单矩形由右键瞬间的屏幕坐标（`contextMenuX/Y`）计算，平移/缩放地图时不再更新。
* **修复**：`getContextMenuRect()` 改为**每帧从右键点的世界坐标（`contextMenuTargetX/Z`）经 `worldToScreenX/Y` 换算**屏幕位置——菜单锚定在地图上的世界位置，拖地图、滚轮缩放时跟随目标点一起移动；点击命中检测与渲染共用同一矩形，自动一致。顺带删除废弃的 `contextMenuX/Y` 字段。
* **涉及文件**：客户端 `render/WorldMapScreen.java`、`gradle.properties`（mod_version=2.2.5）。
* **部署**：`mmap-forge-2.2.5.jar`（150,535 字节，`D82B0C6B66933D6937FAC58C2F4582C0E81C5AC83BAA8BA97EA118B8451C0C8B`）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏生效**）。dist\、releases\mod-2.2.5_plugin-2.1.0\、SHA256SUMS 已更新，`verify-dist -Check` 全绿。

---

## [2.2.4] — 2026-09-20（客户端模组小版本）

**修复：大地图右键菜单仍遮不住区域名/传送点名的文字（2.2.3 只盖住了名字的黑色阴影框）。**

* 🔧 **只需更新客户端模组** 2.2.3→**2.2.4**（`mmap-forge-2.2.4.jar`），服务端插件 2.1.0 不用动。
* **原因**：地图上文字 glyph 画在比 fill 更高的 z 层——2.2.3 把菜单背景改不透明后，菜单底只能盖住名字底下的黑色阴影框（同为 fill），**盖不住文字本身**（glyph）。
* **修复（双保险）**：
  * ① 菜单整体抬到 tooltip 同层 **z=400**（`pose().translate(0,0,400)`，原版 tooltip 盖层做法），不透明底从上方压住所有下层文字。
  * ② 菜单打开时**跳过绘制落在菜单范围内的区域名/传送点名**（`renderRegionLabels` 加菜单矩形相交判断；`WarpOverlay` render 系列新增 `hideRect` 参数），名字不画就绝不会透过菜单。
* **涉及文件**：客户端 `render/WorldMapScreen.java`（菜单 z=400 盖层 + 区域名跳过 + 传 hideRect）、`warp/WarpOverlay.java`（`render`/`renderWorldMap` 增加 `int[] hideRect` 参数）、`gradle.properties`（mod_version=2.2.4）。
* **部署**：`mmap-forge-2.2.4.jar`（150,571 字节，`D9942BF0D82A3F3F0CFAD8FB3D6283B9EA10C83059FF6A90398785877A10F9C7`）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏生效**）。dist\、releases\mod-2.2.4_plugin-2.1.0\、SHA256SUMS 已更新，`verify-dist -Check` 全绿。

---

## [2.2.3] — 2026-09-20（客户端模组小版本）

**修复：① 导航目标 Y 高出一格；② 大地图右键菜单被区域名/传送点名透过半透明背景干扰。**

* 🔧 **只需更新客户端模组** 2.2.2→**2.2.3**（`mmap-forge-2.2.3.jar`），服务端插件 2.1.0 不用动。
* **① 目标 Y 高一格**：`findSurfaceY` 原为 `getHeight(MOTION_BLOCKING) + 1`，实测目标粒子比地面高一格——客户端 `getHeight` 返回的已是方块顶高度，去掉 `+1`，目标落在方块顶部（粒子再 +0.2 悬空可见），与服务端 `/mmap tp` 落点一致。
* **② 右键菜单遮挡**：菜单背景原为半透明（`0xCC0B1220`，alpha 204），底下的区域名/传送点名透过背景显示出来、压在菜单信息上。改为不透明深蓝底（`0xFF0B1220`），彻底遮住下层文字。
* **涉及文件**：客户端 `nav/NavigationManager.java`（`findSurfaceY` 去 +1）、`render/WorldMapScreen.java`（`renderContextMenu` 背景改不透明）、`gradle.properties`（mod_version=2.2.3）。
* **部署**：`mmap-forge-2.2.3.jar`（150,360 字节，`4FD3EFA850950CD4249C74B57688D4829C79B910A8B72A8FDDDF529E5EB58198`）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏生效**）。dist\、releases\mod-2.2.3_plugin-2.1.0\、SHA256SUMS 已更新，`verify-dist -Check` 全绿。

---

## [2.2.2] — 2026-09-20（客户端模组小版本）

**修复：路径退化成「玩家→离目标最近的节点→目标」的问题（2.2.1 自建版评分选入口的缺陷）。**

* 🔧 **只需更新客户端模组** 2.2.1→**2.2.2**（`mmap-forge-2.2.2.jar`），服务端插件 2.1.0 不用动。
* **原因**：2.2.1 用「玩家到节点直线距离 + 节点沿网络到目标距离」评分选入口，但**目标侧节点网络距离为 0**，评分几乎总是最小；直线链上各节点评分恰好相等（10+30=20+20=30+10=40+0），平局又被 targetWp 抢先——入口永远是离目标最近的节点，路径退化成 `玩家→离目标最近的节点→目标`，绕过了中间的路线网络。
* **修复**：入口改回**「前方未走过的节点里离玩家最近的那个」**（与设计注释一致），再沿反向 Dijkstra 网络走到目标：顺着链走、走过的节点自动淘汰、整条网络走完（targetWp 也在身后）才直线冲刺。保留 2.2.1 的反向 Dijkstra / reconstruct / isBehind / 终点冲刺基建。
* **涉及文件**：客户端 `nav/NavigationManager.java`（`rebuildPath` 入口选择改为最近前方可达节点）、`gradle.properties`（mod_version=2.2.2）。
* **部署**：`mmap-forge-2.2.2.jar`（150,353 字节，`25E41C72EA43A966DFFDDD0138D30F55708796DD66548AE68410489EF76C4DE7`）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏生效**）。dist\、releases\mod-2.2.2_plugin-2.1.0\、SHA256SUMS 已更新，`verify-dist -Check` 全绿。

---

## [2.2.1] — 2026-09-19（客户端模组小版本）

**修复：越过最后一个导航节点后，路径不再绕回身后节点折返。**

* 🔧 **只需更新客户端模组** 2.2.0→**2.2.1**（`mmap-forge-2.2.1.jar`），服务端插件 2.1.0 不用动。
* **原因**：路径「玩家→最近点位→(Dijkstra)→目标」中，玩家越过最后一个节点后，`nearest()` 仍把身后那个节点当最近点，路径就绕回去连它再折回目标，折线/粒子出现「往回连」。
* **修复**：改用**点积方向判据**——最近节点相对玩家是否在目标方向的身后（`(目标-玩家)·(节点-玩家) < 0`）。是则跳过点位图，路径直接「玩家→目标」直线；否则照常走点位图。修正了初版距离判据的漏洞（玩家位于节点与目标之间时，`dist(玩家,目标)>dist(玩家,节点)`，距离判据不生效）。
* **涉及文件**：客户端 `nav/NavigationManager.java`（`rebuildPath` 增加点积越界判断）、`gradle.properties`（mod_version=2.2.1）。
* **部署**：`mmap-forge-2.2.1.jar`（150,375 字节，`1D6AE475CBF44F7DBEE69C163719E1B5E21CCB2B49D4A75F1139151CF8CEBB6B`，**玩家自建版**：重写为反向 Dijkstra + 前方入口评分路由）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏生效**）。dist\、releases\mod-2.2.1_plugin-2.1.0\、SHA256SUMS 已更新，`verify-dist -Check` 全绿。

---

## [2.2.0] — 2026-09-19（客户端模组升级）

**功能：导航路径实时刷新（5 tick）+ 目标 Y 自动判断地表高度。**

* 🔧 **只需更新客户端模组** 2.1.0→**2.2.0**（`mmap-forge-2.2.0.jar`），服务端插件 2.1.0 不用动。
* **路径实时更新**：原来路径一次性生成，玩家移动后「玩家→最近点位」线段会过时。现在每 **5 tick**（0.25 秒）重算一次路径——玩家到最近点位 + Dijkstra 到目标，跟随移动实时更新；经过点位后自动衔接下一段，路径始终是最新的。大地图/小地图折线与红石粒子都会随之更新。
* **目标 Y 自判断（参照 /mmap tp）**：大地图右键「导航至此」和 `/mmap nav <x> <z>` 的目标高度不再用玩家当前 Y，而是按目标坐标查客户端高度图 `MOTION_BLOCKING 最高方块 +1`（与服务端 `/mmap tp` 的 `getHighestBlockYAt+1` 一致）。目标区块已加载立即得到正确地表；远处目标区块未加载时先回退玩家高度，**随玩家接近、区块加载后自动校正**。
* **涉及文件**：客户端 `nav/NavigationManager.java`（`PATH_REFRESH_TICKS=5` 实时重算、`rebuildPath`、`findSurfaceY`、目标高度渐进校正）、`gradle.properties`（mod_version=2.2.0）。
* **部署**：`mmap-forge-2.2.0.jar`（149,180 字节，`8C9567D513BEFBB27A1F47732F8E8D4898A309633FF9AAF3928B5FC121B26E7F`）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏生效**）。dist\、releases\mod-2.2.0_plugin-2.1.0\、SHA256SUMS 已更新，`verify-dist -Check` 全绿。

---

## [2.1.0] — 2026-09-19（模组 + 服务端同步升级）

**功能：导航点位坐标全面支持小数（方块中心等精确位置）。**

* ⚠️ **两端都要更新**：服务端插件 **2.0.4→2.1.0**（`MMmap-Plugin-2.1.0.jar`）+ 客户端模组 **2.0.0→2.1.0**（`mmap-forge-2.1.0.jar`）。
* **服务端**：
  * `/mmap waypoint add <地图ID> <点位ID> <x> <y> <z> [世界]` 坐标改为 `double` 解析——`/mmap waypoint add main_map 0 0.5 58 110.5` 现在可以直接用；
  * 未给 y 时取玩家当前高度（改为精确 `getY()` 而非方块整高度）；
  * 列表/添加回显用格式化助手：整数值显示 `58`，小数显示 `110.5`（不再出现 `58.0`）；
  * `coord-must-number` 文案改为「坐标必须是数字（支持小数）」。
* **客户端模组**：
  * 点位模型 `Waypoint` 坐标 int → double，JSON 与旧整数文件双向兼容（Gson 自动转）；
  * `/mmap nav <x> <z>` 从整数参数改为 `DoubleArgumentType`，导航目标坐标也支持小数；
  * 导航 Dijkstra / 粒子 / 折线全部按 double 精度计算（方块级精度提升到精确位置）。
* **涉及文件**：服务端 `waypoint/Waypoint.java`、`command/MMmapCommand.java`（解析 + `fmtCoord`）、`resources/language.yml`、`plugin.yml`；客户端 `waypoint/Waypoint.java`、`nav/NavigationManager.java`（去冗余强转）、`command/NavCommand.java`、`command/MMmapCommands.java`、`gradle.properties`（mod_version=2.1.0）。
* **部署**：模组 `mmap-forge-2.1.0.jar`（148,653 字节，`D4CE69EE855B43578588F9D12F896BABD2CE24AA251EB747AA59B0E2391C3794`）→ 客户端 `versions\时之旅者\mods\`（**需重启游戏生效**）；插件 `MMmap-Plugin-2.1.0.jar`（72,138 字节，`14B111D7792CC553A87508029F7DE6BF1432EFC37D05752AC2A0DE51D1912A78`）→ 服务端 `plugins\`（**需重启服务端生效**）。dist\、releases\mod-2.1.0_plugin-2.1.0\、SHA256SUMS 已更新，`verify-dist -Check` 全绿。

---

## [2.0.4] — 2026-09-19（服务端小版本）

**改进：waypoint 命令坐标报错更明确，并修复参数解析顺序 bug。**

* 🔧 **只需更新服务端插件** 2.0.3→**2.0.4**（`MMmap-Plugin-2.0.4.jar`），客户端模组 2.0.0 不用动。
* **说明**：`/mmap waypoint add` 的点位坐标是**整数方块坐标**（客户端/服端两端都存 `int`，导航按方块算），**不支持小数**——输入 `0.5`、`110.5` 会报「坐标必须是数字」。现已把文案改为 `坐标必须是整数（方块坐标，不支持小数），例如：0 58 110`，一眼看懂。
* **修复**：原解析顺序会在缺 y/z 时先读 `args[6]` 导致数组越界（未捕获）；现改为按 `x→y→z` 顺序、带长度判断解析；未给 y 时取玩家当前高度（仅玩家，控制台要求显式给 y）。
* **涉及文件**：服务端 `command/MMmapCommand.java`（`handleWaypointAdd` 解析重写）、`resources/language.yml`（`coord-must-number` 文案）、`plugin.yml`（版本 2.0.4）。
* **验证**：✅ 编译通过（`MMmap-Plugin-2.0.4.jar`，71,920 字节，`1F663B2580453B7B597A80F3148AA286D1FB594C0C8214698522D597823B2820`，plugin.yml version=2.0.4）；`dist\`、`releases\mod-2.0.0_plugin-2.0.4\`、SHA256SUMS 已更新，`verify-dist -Check` 全绿；服务端 `plugins\` 已拷入 2.0.4（**需重启服务端生效**），旧 2.0.2 由后台任务随停服自动删除。

---

## [2.0.3] — 2026-09-19（服务端小版本）

**调整：进服「已同步地图图片绑定」提示改为仅 OP 可见。**

* 🔧 **只需更新服务端插件** 2.0.2→**2.0.3**（`MMmap-Plugin-2.0.3.jar`），客户端模组 2.0.0 不用动。
* **说明**：`已同步 X 个地图图片绑定` + `请将图片文件放在…` 是给服主/管理员看的配置提示，普通玩家进服不该刷屏。现在只有 **OP（或有 `mmap.command` 权限）** 能看到；数据同步对所有人照常，只是不显示聊天提示。
* **涉及文件**：服务端 `network/PacketSender.java`（notify 分支加 `player.isOp() || hasPermission("mmap.command")` 判断）、`plugin.yml`（版本 2.0.3）。
* **验证**：✅ 编译通过（`MMmap-Plugin-2.0.3.jar`，71,866 字节，`1E4C8A57D78DA10FE5BE3E752F0AC5B79D5F451F9D385A36174812DCA8A6F002`，plugin.yml version=2.0.3）；`dist\`、`releases\mod-2.0.0_plugin-2.0.3\`、SHA256SUMS 已更新，`verify-dist -Check` 全绿；服务端 `plugins\` 已拷入 2.0.3（**需重启服务端生效**），旧 2.0.2 由后台任务随停服自动删除。

---

## [2.0.2] — 2026-09-19（服务端小版本）

**修复：进服后「已同步 X 个地图图片绑定」提示刷屏 4 次（2.0.1 重试同步的副作用）。**

* 🔧 **只需更新服务端插件** 2.0.1→**2.0.2**（`MMmap-Plugin-2.0.2.jar`），客户端模组 2.0.0 不用动。
* **原因**：2.0.1 为修进服丢包做了 4 次重发同步，但每次 `sendMapImageSync` 都附带两条聊天提示（`已同步…` + `请将图片文件放在…`），于是进服连刷 4 遍。
* **修复**：`sendMapImageSync` 增加 `notify` 参数——**数据包照常重发**（幂等、防丢包），**聊天提示只在首次（1s）发**，重试与换世界同步全部静默。`/mmap reload` 广播仍带提示（显式操作，反馈一次）。
* **涉及文件**：服务端 `network/PacketSender.java`（`sendMapImageSync(Player, boolean)`）、`player/PlayerTracker.java`（首次 `notify=true`，重试 `false`）、`plugin.yml`（版本 2.0.2）。
* **验证**：✅ 编译通过（`MMmap-Plugin-2.0.2.jar`，71,845 字节，`6101AD9B9CDB9A6735B16E8A2BAA35C81B0C84420D15F6F5BC8E84BD64D1832C`，plugin.yml version=2.0.2）；`dist\`、`releases\mod-2.0.0_plugin-2.0.2\`、SHA256SUMS 已更新，`verify-dist -Check` 全绿；服务端 `plugins\` 已拷入 2.0.2（**需重启服务端生效**），旧 2.0.1 由后台任务随停服自动删除。

---

## [2.0.1] — 2026-09-19（服务端小版本）

**修复：进服/换世界时同步包偶发丢失，导致大小地图空白（客户端必须 `/mmap reload` 或重进才恢复）。**

* 🔧 **只需更新服务端插件** 1.9.8→2.0.0 之后再升 **2.0.1**（`MMmap-Plugin-2.0.1.jar`），客户端模组 2.0.0 不用动。
* **原因**：玩家进服时，服务端在 join 事件后 1 秒（20 tick）才发同步包（握手/图片/多边形/传送点）。但客户端刚建立连接时，Forge 的 `mmap:main` 频道还没接好线（连原生 Paper 服务端有「Catching up missing behaviour」过渡期），**太早到达的数据包被客户端静默丢弃**；此时地图就是空的，`/mmap reload` 的广播（等频道就绪后才发）才能送到。客户端刚启动、登录阶段较长时最容易触发（本次 2.0.0 首次进服即中招，14:04 那次 1.9.14 进服没触发——时序竞态，随机出现）。
* **修复**：进服后按 1s / 3s / 6s / 10s 共 4 次重发同步；换世界后按 0.5s / 2s / 5s 重发。重发幂等（客户端按 id 覆盖），无副作用，保证任何情况下数据都能送达。
* **涉及文件**：服务端 `player/PlayerTracker.java`（`scheduleJoinSync`/`scheduleWorldSync` 多次重发）、`plugin.yml`（版本 2.0.1）。
* **验证**：✅ 编译通过（`MMmap-Plugin-2.0.1.jar`，71,671 字节，`7BED544C8C0ABAED80265376225A26756A56CB516398FA6E5F0C7C13E137F734`，plugin.yml version=2.0.1）；`dist\`、`releases\mod-2.0.0_plugin-2.0.1\`、SHA256SUMS 已更新，`verify-dist -Check` 全绿；服务端 `plugins\` 已替换（**需重启服务端生效**，重启后进服不再需要手动 `/mmap reload`）。

---

## [2.0.0] — 2026-09-19（未正式发布，标记为版本基线）

**新增：导航系统（客户端算法 + 服务端点位备份）。**

* 🔧 **客户端模组 1.9.14 → 2.0.0 + 服务端插件 1.9.8 → 2.0.0，两边都要换 jar。**
* **导航入口**：
  1. 大地图右键任意位置 → **「导航至此」**（所有玩家可用，第一行）；
  2. 命令 `/mmap nav <x> <z>` 发起导航、`/mmap nav stop` 停止（客户端本地指令，不会发给服务端）。
* **路径生成（全程客户端，`NavigationManager`）**：玩家 → 离玩家最近的点位 →（点位图 **Dijkstra 最短路径**，2D 距离为权重、双向连通）→ 离目标最近的点位 → 目标点。
* **路径显示**：
  1. **游戏内**：沿路径生成**原版红石粒子**（红色 DUST，100 个/4tick，`addAlwaysVisibleParticle` 远距离也可见），到达目标 3 格内自动停止并提示；
  2. **大地图**：青色折线 + 目标十字标记；
  3. **小地图**：青色折线 + 目标十字标记（跟随玩家/缩放）。
* **点位配置文件**（格式：`{"world": "...", "waypoints": [{"id","x","y","z","links":[...]}]}`）：
  * **客户端**：`.minecraft/mmap/waypoints/<地图ID>.json`，一个地图一个文件、文件名对应地图 ID（已创建目录）；
  * **服务端备份**：`plugins/MMmap/waypoints/<地图ID>.json`，`/mmap waypoint` 命令维护后自动写入（后续由启动器从后台自动更新客户端副本）。
* **服务端命令 `/mmap waypoint`**：
  * `add <地图ID> <点位ID> <x> <y> <z> [世界]`（省略 y 取脚下；世界默认取图片绑定/玩家所在世界）；
  * `remove <地图ID> <点位ID>`、`list [地图ID]`、`link <地图ID> <点位A> <点位B>`（双向）、`unlink ...`。
* **点位文件复制到客户端后生效**（导航发起时自动重读目录，无需重启）。
* **涉及文件**：服务端 `waypoint/Waypoint.java`、`waypoint/WaypointManager.java`、`MMmapPlugin.java`、`MMmapCommand.java`、`language.yml`、`plugin.yml`；客户端 `waypoint/Waypoint.java`、`waypoint/WaypointStore.java`、`nav/NavigationManager.java`、`render/PolylineRenderer.java`、`command/NavCommand.java`、`command/MMmapCommands.java`、`MMmapClient.java`、`render/WorldMapScreen.java`、`render/MinimapRenderer.java`、`gradle.properties`。

**验证与部署**：

* ✅ 两端 `gradlew build`（离线）编译通过：
  * 模组 `mmap-2.0.0.jar`（148,483 字节，`ECFBCEF02ED819D58B8930AD4AC02982136AA76E831B12DA86CEAB6E2BCA2687`）；
  * 插件 `MMmap-Plugin-2.0.0.jar`（71,476 字节，`FF70C95359626A3771AE11118B7723D586E6F193B3F1504F97A8324D4F1E3004`，plugin.yml version=2.0.0）。
* ✅ `dist\` 已更新（`mmap-forge-2.0.0.jar` / `MMmap-Plugin-2.0.0.jar`），SHA256SUMS 已刷新，`verify-dist.ps1 -Check` 全部一致；`releases\mod-2.0.0_plugin-2.0.0\` 快照已建。
* ✅ **已部署**：客户端 `时之旅者\mods\mmap-forge-2.0.0.jar`（旧 1.9.14 备份 `backups\_deployed-old-mmap-forge-1.9.14.jar`）；服务端 `时旅服务端\plugins\MMmap-Plugin-2.0.0.jar`（旧 1.9.8 备份 `backups\_deployed-old-MMmap-Plugin-1.9.8.jar`）；客户端 `.minecraft\mmap\waypoints\` 目录已创建。
* ⚠️ **使用流程**：重启服务端加载 2.0.0 → 用 `/mmap waypoint add/link` 维护点位 → 把 `plugins/MMmap/waypoints/*.json` 拷到客户端 `.minecraft/mmap/waypoints/` → 重启客户端 → 大地图右键「导航至此」或 `/mmap nav <x> <z>`。

---

## [未发布]

**修复：设置界面「小地图设置」里「形状」标签与「圆形」按钮重叠（模组 1.9.13 → 1.9.14）。**

* ⚠️ **只需更新客户端模组**（`mmap-forge-1.9.14.jar`），服务端插件 1.9.8 不用动。
* **改动内容**（仅模组）：`WorldMapScreen.java` 的 `getSettingsShapeRect()` 把「圆形/正方形」按钮整体右移到「形状」标签右侧，不再重叠；其余设置项布局不变。
* **验证与部署**：
  * ✅ `gradlew build`（离线）编译通过，产物 `mod-forge\build\libs\mmap-1.9.14.jar`（131,379 字节，`2B2E8EF9C5CF2B426960A5E3CDB32FD59528361CB13B483A10BB2D245C08FA42`）；插件产物保持 1.9.8 不变。
  * ✅ `dist\` 已更新（`mmap-forge-1.9.14.jar`），SHA256SUMS 已刷新，`verify-dist.ps1 -Check` 全部一致；`releases\mod-1.9.14_plugin-1.9.8\` 快照已建。
  * ✅ 客户端 `时之旅者\mods\mmap-forge-1.9.14.jar` 已替换（旧 1.9.13 备份在 `backups\_deployed-old-mmap-forge-1.9.13.jar`）。
  * ⚠️ 客户端下次启动游戏生效。

---

**新增：大地图左下角齿轮 → 设置界面（模组 1.9.12 → 1.9.13）。**

* ⚠️ **只需更新客户端模组**（`mmap-forge-1.9.13.jar`），服务端插件 1.9.8 不用动。
* **改动内容**（仅模组）：
  1. **齿轮入口**：大地图左下角新增齿轮图标按钮，点击打开设置界面（带点击音效、悬停高亮）；
  2. **设置界面**（与右键菜单/传送点面板同一科幻风格：半透明深蓝 + 霓虹青细边框）：
     * 顶部两个页签，**默认「大地图设置」**；ESC 先关设置、再按一次才关大地图；
     * **大地图设置**：显示玩家头像开关、显示怪物头像开关；
     * **小地图设置**：方位（左上/右上/左下/右下）、大小（64–256，步进 16）、形状（圆形/正方形）；
  3. 所有修改即时生效并写入 `config/mmap.json`（新增字段 `minimap.shape`、`worldmap.showMobs`，旧配置自动补默认值）；
  4. 小地图**正方形形状**支持：背景/边框/图片渲染全部按正方形（圆形时维持原样）。
* **涉及文件**：`ConfigManager.java`、`MinimapImageRenderer.java`、`MinimapRenderer.java`、`WorldMapScreen.java`、`gradle.properties`。

**验证与部署**：

* ✅ `gradlew build`（离线）编译通过：
  * 模组产物 `mod-forge\build\libs\mmap-1.9.13.jar`（131,375 字节，`9259657D7416B05E87939DFBB2CF10D962773792479CEF0AF26A06CFFB9A6105`）；
  * 插件产物保持 `MMmap-Plugin-1.9.8.jar`（61,554 字节，`0A06E4CE…DB97`）不变。
* ✅ `dist\` 已更新（`mmap-forge-1.9.13.jar` / `MMmap-Plugin-1.9.8.jar`，SHA256SUMS 已刷新，`verify-dist.ps1 -Check` 全部一致）；
  `releases\mod-1.9.13_plugin-1.9.8\` 快照已建（旧快照保留在 `releases\mod-1.9.12_plugin-1.9.8\` 及更早目录）。
* ✅ **已部署**：客户端 `时之旅者\mods\mmap-forge-1.9.13.jar` 已替换（旧 1.9.12 备份在
  `backups\_deployed-old-mmap-forge-1.9.12.jar`）；服务端保持 `MMmap-Plugin-1.9.8.jar` 不动。
* ⚠️ **生效时机**：客户端下次启动游戏生效（正在运行的游戏仍是旧版，需重启客户端）。

---

**修复：缩小窗口时右上角小地图被聊天框遮挡（模组 1.9.11 → 1.9.12）。**

* ⚠️ **只需更新客户端模组**（`mmap-forge-1.9.12.jar`），服务端插件 1.9.8 不用动。
* **改动内容**（`MinimapRenderer.java`）：
  1. 聊天框（开着或正显示消息）盖到小地图时自动避让：先把地图**上移到聊天框上方**；
  2. 上方放不下就**缩小地图**适配剩余高度；屏幕实在太小则**暂时隐藏**，聊天框收起来后恢复；
  3. 只在聊天框与小地图实际重叠时触发，正常屏幕/正常窗口完全不受影响。
* **涉及文件**：仅模组 `MinimapRenderer.java`、`gradle.properties`。

**验证与部署**：

* ✅ `gradlew build`（离线）编译通过：
  * 模组产物 `mod-forge\build\libs\mmap-1.9.12.jar`（126,456 字节，`327EA842943B2BD9A8585958C62D879F24DC98952FE9EA5B49D0290A3669DB61`）；
  * 插件产物保持 `MMmap-Plugin-1.9.8.jar`（61,554 字节，`0A06E4CE…DB97`）不变。
* ✅ `dist\` 已更新（`mmap-forge-1.9.12.jar` / `MMmap-Plugin-1.9.8.jar`，SHA256SUMS 已刷新，`verify-dist.ps1 -Check` 全部一致）；
  `releases\mod-1.9.12_plugin-1.9.8\` 快照已建（旧快照保留在 `releases\mod-1.9.11_plugin-1.9.8\` 及更早目录）。
* ✅ **已部署**：客户端 `时之旅者\mods\mmap-forge-1.9.12.jar` 已替换（旧 1.9.11 备份在
  `backups\_deployed-old-mmap-forge-1.9.11.jar`）；服务端保持 `MMmap-Plugin-1.9.8.jar` 不动。
* ⚠️ **生效时机**：客户端下次启动游戏生效（正在运行的游戏仍是旧版，需重启客户端）。

---

**优化：`/mmap info` 把坐标范围换成顶点列表 + 面积（插件 1.9.7 → 1.9.8）。**

* ⚠️ **只需更新服务端插件**（`MMmap-Plugin-1.9.8.jar`），客户端模组 1.9.11 不用动。
* **改动内容（服务端）**：`/mmap info <区域ID>` 移除「坐标范围」（包围盒，意义不大），
  改为显示 **顶点坐标列表**（最多列 15 个，超出提示剩余数量）和 **多边形面积**（鞋带公式）。
* **涉及文件**：插件 `MMmapCommand.java`、`PolygonRegion.java`（新增 `getArea`）、`language.yml`、`plugin.yml`。

**验证与部署**：

* ✅ `gradlew build`（离线）编译通过：
  * 插件产物 `plugin-paper\build\libs\MMmap-Plugin-1.9.8.jar`（61,554 字节，`0A06E4CE67023888B6293CBF487B6AD5E5C3AE5D35C455F97DAA1E68201DDB97`）。
* ✅ `dist\` 已更新（`MMmap-Plugin-1.9.8.jar`，SHA256SUMS 已刷新，`verify-dist.ps1 -Check` 全部一致）；
  `releases\mod-1.9.11_plugin-1.9.8\` 快照已建。
* ✅ **已部署（服务端待重启生效）**：`时旅服务端\plugins\MMmap-Plugin-1.9.8.jar` 已放入；
  旧 `MMmap-Plugin-1.9.7.jar` 已在服务端停止后删除（备份 `backups\_deployed-old-MMmap-Plugin-1.9.7.jar`）。
* ⚠️ **生效时机**：服务端重启后加载 1.9.8；客户端无需任何操作。

---

**恢复：`/mmap info` 指令保留，改为查询多边形区域详情（插件 1.9.6 → 1.9.7）。**

* ⚠️ **只需更新服务端插件**（`MMmap-Plugin-1.9.7.jar`），客户端模组 1.9.11 不用动。
* **背景**：1.9.6 删除矩形系统时误把 `info` 一起删了，实际需要保留。
* **改动内容（服务端）**：
  1. 恢复 `/mmap info <区域ID>`（`/map info` 同义），查询对象改为**多边形区域**；
  2. 显示内容：ID、名称、世界、顶点数、X/Z 范围、中心坐标、进入命令数；
  3. Tab 补全 `info` 时列出已加载的多边形 ID；帮助里恢复 `help-info` 一行。
* **涉及文件**：插件 `MMmapCommand.java`、`language.yml`、`plugin.yml`。

**验证与部署**：

* ✅ `gradlew build`（离线）编译通过：
  * 插件产物 `plugin-paper\build\libs\MMmap-Plugin-1.9.7.jar`（61,134 字节，`BB37A13EEB946FA4D2976255D84AD8AE5C58A4389F42FAAA059EFA13D5BBCF11`）。
* ✅ `dist\` 已更新（`MMmap-Plugin-1.9.7.jar`，SHA256SUMS 已刷新，`verify-dist.ps1 -Check` 全部一致）；
  `releases\mod-1.9.11_plugin-1.9.7\` 快照已建。
* ✅ **已部署（服务端待重启生效）**：`时旅服务端\plugins\MMmap-Plugin-1.9.7.jar` 已放入；
  旧 `MMmap-Plugin-1.9.6.jar` 已在服务端停止后删除（备份 `backups\_deployed-old-MMmap-Plugin-1.9.6.jar`）。
* ⚠️ **生效时机**：服务端重启后加载 1.9.7；客户端无需任何操作。

---

**重构：删除矩形区域系统，`list` 改为按世界查询多边形区域（插件 1.9.5 → 1.9.6）。**

* ⚠️ **只需更新服务端插件**（`MMmap-Plugin-1.9.6.jar`），客户端模组 1.9.11 不用动、协议不变。
* **为什么**：矩形已被多边形完全覆盖（4 个顶点的多边形就是矩形），且矩形还有世界名
  格式不一致的历史问题（见 `docs/PROTOCOL.md §5.1`）。现在服务端全部用 `shape: polygon`。
* **改动内容（服务端）**：
  1. **删除矩形区域系统**：`RegionManager` / `MapRegion` / `RegionType` 三个类整体移除；
  2. **删除矩形相关命令**：`/mmap add`、`/mmap remove`、`/mmap info` 及其权限节点
     （`mmap.command.add` / `mmap.command.remove`）、Tab 补全与帮助文案；
  3. **删除矩形同步包**：包 2（RegionSync）/ 包 3（RegionUpdate）不再发送，`PlayerTracker`
     进服/换世界、`reload` 不再同步矩形数据；
  4. **`/mmap list [世界]` 重写**：列出**多边形区域**，`/mmap list` 列全部，
     `/mmap list <世界名>` 只列该世界（`PolygonRegionManager.getInWorld()`），
     每行显示 `ID - 名称 [世界]`；
  5. `regions.yml` 模板改为纯多边形说明；`reload` 汇总改为「多边形 | 图片 | 传送点」。
* **客户端行为**：模组 1.9.11 仍保留矩形渲染代码，但收不到包 2/3 自然不显示任何矩形，
  多边形/传送点/图片一切照旧——**无需更新客户端，兼容新旧插件**。
* **涉及文件**：插件 `MMmapCommand.java`、`MMmapPlugin.java`、`PlayerTracker.java`、
  `PacketSender.java`、`PolygonRegionManager.java`（新增 `getInWorld`）、
  `language.yml`、`plugin.yml`、`regions.yml` 模板；文档 `docs/PROTOCOL.md`。

**优化：传送点面板视觉与右键选项栏统一（模组 1.9.10 → 1.9.11）。**

* ⚠️ **只需更新客户端模组**（`mmap-forge-1.9.11.jar`），服务端插件 1.9.5 与协议不变。
* **改动内容**（左键选中传送点后右侧弹出的面板）：
  1. 与右键选项栏同一套视觉：半透明深蓝底 + 霓虹青细边框、青色系文字（标题/描述/坐标）；
  2. 「导航 / 传送」按钮改为与右键菜单相同的小按钮样式（16px、悬停亮边），点击同样有 `ui.button.click` 音效；
  3. 顺带清掉 `renderSelectedWarpPanel` 里每帧打印的调试输出（`[DEBUG-PANEL-V177]`）。
* **涉及文件**：仅模组 `WorldMapScreen.java`、`gradle.properties`。

**优化：右键菜单按钮点击音效 + 去掉装饰元素（模组 1.9.9 → 1.9.10）。**

* ⚠️ **只需更新客户端模组**（`mmap-forge-1.9.10.jar`），服务端插件 1.9.5 与协议不变。
* **改动内容**：
  1. **按钮点击音效**：右键菜单点「传送到此处」或「关闭」时播放 UI 点击音（`ui.button.click`）；
  2. **菜单更简洁**：去掉顶部霓虹强调线和四角括号装饰，只保留半透明深蓝底 + 霓虹青细边框。
* **涉及文件**：仅模组 `WorldMapScreen.java`、`gradle.properties`。

**优化：右键选项栏改为科幻简洁风 + 传送后自动关闭大地图（模组 1.9.8 → 1.9.9）。**

* ⚠️ **只需更新客户端模组**（`mmap-forge-1.9.9.jar`），服务端插件 1.9.5 与协议不变。
* **改动内容**：
  1. **传送后关闭大地图**：右键菜单点「传送到此处」、以及传送点面板点「传送」，都会在发出命令后直接关闭大地图界面；
  2. **右键菜单重设计**：半透明深蓝玻璃底 + 霓虹青细边框 + 顶部强调线 + 四角括号（科幻 HUD 风），
     面板宽度 168 → 132、按钮高度 20 → 16、行距收紧，整体更紧凑简洁；
     按钮悬停时边框/底色变亮，传送按钮文字用青色系区分。
* **涉及文件**：仅模组 `WorldMapScreen.java`、`gradle.properties`。

**新增：大地图右键选项栏 —— 右键任意位置弹出「选项」菜单，可「传送到此处」，仅 OP 可用。**

* ⚠️ **客户端模组 + 服务端插件都要升级**（`mmap-forge-1.9.8.jar` + `MMmap-Plugin-1.9.5.jar`）：
  * 服务端新增 `/mmap tp <x> <z> [y]` 与 `/mmap warp tp <传送点ID>` 两个子命令（权限 `mmap.command.tp`，默认仅 OP）；
  * 握手包**末尾追加 1 个 boolean**（`canTeleportFromMap`），客户端 try/catch 读取、旧服务端缺字段时默认 false。
    按 `VERSIONING.md` 的兼容约定，**协议版本号仍为 1，不用同时升**——服务端先升级、客户端后升级都安全：
    * 新客户端 + 旧服务端：读不到新字段 → 默认 false → 右键菜单不显示「传送到此处」；
    * 旧客户端 + 新服务端：忽略多余字节，行为不变。
* **操作方式**：大地图（M 键打开）里**右键地图任意位置** → 光标旁弹出选项栏：
  * 显示鼠标所指的方块坐标（X/Z）；
  * **「传送到此处」**：向服务端发 `tpCmd.mapTeleport` 模板（默认 `/mmap tp {x} {z}`，可在客户端 `config/mmap.json` 的 `tpCmd` 段改），
    服务端校验权限后把玩家传到该坐标（未指定 Y 时自动取最高方块 +1，避免传进地里）；
  * **非 OP 看不到「传送到此处」**，选项栏只剩坐标信息和「关闭」；
  * 左键点选项栏外任意处或点「关闭」收起。
* **顺带修复**：服务端此前**根本没有实现** `/mmap warp tp` / `/mmap warp nav`，导致传送点面板的「传送」按钮点了无效
  （服务端回"未知命令"）。本次实现了 `/mmap warp tp <ID>`（同样仅 OP），「传送」按钮现在可用；
  「导航」仍为预留（客户端未实现导航逻辑）。
* **涉及文件**：
  * 模组：`WorldMapScreen.java`、`NetworkHandler.java`、`MMmapClient.java`、`ConfigManager.java`、`gradle.properties`（1.9.7 → 1.9.8）；
  * 插件：`MMmapCommand.java`、`PacketSender.java`、`plugin.yml`（1.9.4 → 1.9.5）、`language.yml`。

**验证与部署**：

* ✅ `gradlew build`（离线）编译通过：
  * 插件产物 `plugin-paper\build\libs\MMmap-Plugin-1.9.6.jar`（60,369 字节，`5F8876BDD8BA7D9CB5BB19AC309F050E5960E6D54D3C78F205873199A9BC71D1`）；
  * 模组产物保持 `mmap-forge-1.9.11.jar`（126,104 字节，`F20645D00F5EC3F2FA93B39A2B5F56A5DC3EFE620C93CEFD015DEA0ACAD1D529`）不变。
* ✅ `dist\` 已更新（`MMmap-Plugin-1.9.6.jar` / `mmap-forge-1.9.11.jar`，SHA256SUMS 已刷新，`verify-dist.ps1 -Check` 全部一致）；
  `releases\mod-1.9.11_plugin-1.9.6\` 快照已建（旧快照保留在 `releases\mod-1.9.11_plugin-1.9.5\` 及更早目录）。
* ✅ **已部署（服务端待重启生效）**：
  * 服务端：`时旅服务端\plugins\MMmap-Plugin-1.9.6.jar` 已放入，旧 `MMmap-Plugin-1.9.5.jar` 已删除
    （服务端停止后清理完成；旧件备份在 `backups\_deployed-old-MMmap-Plugin-1.9.5.jar`）；
  * 客户端：`时之旅者\mods\mmap-forge-1.9.11.jar` 保持不变（无需更新）。
* ⚠️ **生效时机**：服务端重启后加载 1.9.6；客户端无需任何操作。

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
* ✅ **已部署**：使用者关闭游戏后自动替换到 `versions\时之旅者\mods\mmap-forge-1.9.7.jar`
  （旧 1.9.6 已删除），哈希与 `dist/` 一致（`E50716B1…`），jar 内 `mods.toml` 版本号为 1.9.7。
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
