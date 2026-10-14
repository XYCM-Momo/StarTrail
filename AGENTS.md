# 星轨 StarTrail · 工作区指令

> 本文件由 DSH 在**每个新会话首次请求时自动加载**（项目指令），不需要手动粘贴。
> 面向在本工作区工作的 AI 助手。人类维护者请看 [README.md](README.md) 与 [docs/WORKSPACE.md](docs/WORKSPACE.md)。
>
> 标记含义：**【硬性】** = 违反会踩坑、损坏产物或让游戏端失效；**【偏好】** = 作者的既有选择，除非有理由否则沿用。

---

## 0. 项目一句话

Minecraft **1.20.1** 双端地图系统「**星轨 StarTrail**」：Forge 客户端模组（`mod-forge/`）+ Paper 服务端插件（`plugin-paper/`）。
Java 包 `com.asterovie.momo.startrail.{client,server}`，署名 **星野尘梦工作室 · 默默**。
当前版本 **1.0.0**（模组与插件同步）。

---

## 1. 构建（最容易踩坑的地方）

**【硬性】构建前必须设置 `GRADLE_USER_HOME`**，指向工作区内的缓存目录，否则会污染用户级 Gradle 缓存：

```powershell
$env:GRADLE_USER_HOME='E:\Minecraft\服务器\插件\StarTrail\.gradle-home'
```

**【硬性】两个工程绝对不能并行构建。** 它们共用同一个 `.gradle-home`，并行会争锁，表现为**静默挂起 18 分钟以上**——没有任何报错，看起来就像卡住了。必须一个建完再建另一个：

```powershell
cd E:\Minecraft\服务器\插件\StarTrail\mod-forge
.\gradlew.bat build --offline --console=plain

cd ..\plugin-paper
.\gradlew.bat build --offline --console=plain
```

**【硬性】退出码 1 不等于构建失败。** 只要输出里有 `BUILD SUCCESSFUL`，那个 1 就只是 Gradle deprecation 警告造成的。**判断成败看 `BUILD SUCCESSFUL`，不要看退出码。**

**【硬性】构建是可复现的**：同样的源码重建出的 jar 应当**字节一致**。只改了注释/文档时，用 SHA256 对比确认没有意外改动（当前值见 `dist/SHA256SUMS.txt`）。

**【偏好】改完代码用工作区脚本发布，不要手动拷 jar：**
`tools\build-mod.ps1` / `tools\build-plugin.ps1` → 编译 → 发布到 `dist/` → 刷新校验清单。
完成前 `tools\verify-dist.ps1 -Check` 必须**全绿**。

---

## 2. 编码（三个已经踩过的坑，别重犯）

**【硬性】`tools\*.ps1` 必须是 UTF-8 with BOM。** 本机 `powershell.exe` 是 5.1，没有 BOM 会把中文按 ANSI 解码 → 乱码 + 语法报错。改完这样检查（应为 `EF BB BF`）：

```powershell
([IO.File]::ReadAllBytes($f))[0..2] | ForEach-Object { $_.ToString('X2') }
```

**【硬性】`gradle.properties` 的编码是 ISO-8859-1**，中文必须写成 `\uXXXX` 转义（例如 `org.gradle.java.home` 路径里的「服务器/插件」）。直接写中文会报 `Value ... is invalid`。

**【硬性】两个 `build.gradle` 里已有 `options.encoding='UTF-8'`**，所以 `.java` 源码可以放心写中文；`processResources` 的 `filteringCharset='UTF-8'` 也不要删，否则 `mods.toml` 里的中文会变乱码。

**【硬性】⚠️ 自动化文件改写会导致两种静默损坏**（本工作区真实发生过）：

- (a) **丢掉 UTF-8 BOM** → `.ps1` 里的中文全乱、PowerShell 报语法错；
- (b) **把 Markdown 的 `**` 粗体标记替换成两个空格** → 曾把 `CHANGELOG.md` 里 **641 处** `**` 全部毁掉，而且连历史条目一起毁。

**结论：对 `.ps1` 和含 `**` 的 Markdown，优先用 `write` 工具或 PowerShell 改写，改完必须复查**——查 `**` 数量、查 BOM 三字节。
（这也是为什么 `docs/WORKSPACE.md` 第 4 节专门记了「三个必须记住的编码坑」。）

---

## 3. 绝对不能改的东西（这是文件格式常量，不是品牌！）

**【硬性】下面这些**故意**保留 `MMmap` 字样，不是漏改，别「顺手清理」**：

- `ImageCrypto.java` 里的 **`String s1 = "MMmap";`** —— 加密密钥种子的一部分（完整种子 `MMmap-Xaero-Static-Image-2026`）。**改掉它，所有已加密图片全部解不开。**
- 文件**魔数 `MMAP`**。
- `tools/waypoint-editor` 前端的 `MMAP_KEY_STR` —— 必须与上面的种子保持一致，否则编辑器读不了图。
- 图片扩展名 `.startrail` **必须保留读取旧 `.mmap` 的回退**（老数据兼容）。
- `dist/client-mod/mmap-fabric-1.9.5.jar` —— 上游 Fabric 版原样保留，不参与改名。
- `archive/port-work/**`、`docs/移植说明.md`、`CHANGELOG.md` 里 1.0.0 之前的历史条目。

代码里已加醒目注释，看到「刻意保留」的注释就不要再动它。

---

## 4. 双端必须成对升级

**【硬性】**模组与插件之间有自定义网络通道：

- 通道名 **`startrail:main`** · 握手标识 **`StarTrail-Server`**

**改协议或改通道名 → 两端必须同时换 jar**，否则客户端收不到任何数据（表现为**地图空白**，不报错）。
改协议前必读 [`docs/PROTOCOL.md`](docs/PROTOCOL.md)。

---

## 5. 版本号从哪来（别改错地方）

- **插件版本**：`plugin-paper/src/main/resources/plugin.yml` 的 `version` —— **唯一真源**，`build.gradle` 从它读取，不用手改别处。
- **模组版本**：`mod-forge/gradle.properties` 的 `mod_version`。
- 发布/回滚流程见 [`VERSIONING.md`](VERSIONING.md)。
- **凡是要玩家换 jar 的改动，必须在 [`CHANGELOG.md`](CHANGELOG.md) 留一条**，并写明「要不要同时升级客户端/服务端」。

---

## 6. 本机环境事实（换机器或挪工作区时要同步改）

- **JDK**：工作区自带 `tools/jdk-17.0.20.1+1`。**两个 `gradle.properties` 里都硬编码了它的绝对路径**（`org.gradle.java.home`）。工作区一改名/一挪窝，这两处必须同步改，否则 Gradle 直接报 `Value '...' given for property 'java.home' is invalid`。本工作区刚从 `MMmap` 改名为 `StarTrail`，就是靠改这两处。
- **Git**：`D:\Git`，身份 `XY_Momo <1756784800@qq.com>`，分支 `main`，远程 **`https://github.com/XYCM-Momo/StarTrail.git`（私有仓库）**。凭据已存 Windows 凭据管理器，可直接 `git push`。
  ⚠️ 访问 GitHub **偶发不稳定**（`Connection was reset` / `Could not connect to server`）——那是网络问题，不是 Git 坏了，重试即可。
- **Node**：`D:\nodejs\`（v24，自带 `zlib.zstdDecompressSync`）。
- **Python**：`D:\Python\Python311\`（3.11，**没有** `zstandard` 模块）。
- **没装的**：7-Zip、`zstd`、`gh` CLI。

---

## 7. 部署位置（要真正生效，必须换这里的 jar）

**服务端**：`E:\Minecraft\服务器\时旅服务端\plugins\StarTrail-Plugin-<版本>.jar`
数据目录 `plugins\StarTrail\`（`config.yml` / `images.yml` / `language.yml` / `regions/` / `warps/` / `waypoints/`）。
⚠️ **服务端在运行时插件 jar 被锁定**，替换前必须先关服。

**客户端**：`E:\Minecraft\服务器\自用客户端\.minecraft\versions\时之旅者\mods\startrail-forge-<版本>.jar`
数据目录 `.minecraft\startrail\`（`legend` / `map` / `mm` / `waypoints`）、配置 `config\startrail.json`。
⚠️ 换模组 jar 必须**重启游戏**才生效。

改完插件后应当跑一次 `tools\smoke-test-plugin.ps1` **实测**（它起独立测试服，不动线上服务端）。详见 [`docs/DEPLOY.md`](docs/DEPLOY.md)。

---

## 8. 目录约定

- **`dist/` 是唯一成品区**：要装进游戏的 jar 都从这里取，别从 `build/libs/` 拿。
- **`releases/` 冻结区**：每次正式发布存一份，进去之后不再改动。
- **`archive/` 归档区**：只读参考（移植过程的中间产物），日常用不到但**别删**。
- **`backups/` 快照**：`tools\backup.ps1` 打包（已排除 `.gradle-home\`、`build\`、JDK、`backups\` 自身）。
  ⚠️ **Git 不能替代它**——映射表、大 jar、中间产物都在 `.gitignore` 里，要整体回滚仍得靠 zip 快照。
- **`docs/sessions/` 开发对话归档**：含逐字原始记录；导出新会话用 `tools\export-session.mjs`（见 `docs/WORKSPACE.md` 第 6 节）。

---

## 9. 工具使用注意

**【硬性】如果 `grep` / `glob` 工具报 `ripgrep provider failure`**，通常不是工具坏了，而是**会话的工作区根目录已经不存在**（工作区被改名/移动过，但会话还记着旧路径）。本工作区就发生过：会话根还指着 `...\MMmap`，实际早已是 `...\StarTrail`。
→ 改用 PowerShell 的 `Select-String` / `Get-ChildItem`，并且**一律用绝对路径**。

**【偏好】**排查 `.jsonl.zstd` 这类文件时记住：DSH 会话记录是**「每条记录追加一个 zstd 帧」**拼接写入的，常规解压（`zstdDecompressSync`、`zstd -d`）只解第一个帧就结束，得到的是一两百字节的会话头——**看起来像文件坏了，其实只是没解完**。要逐帧遍历，做法见 `tools/export-session.mjs`。

---

## 10. 协作偏好（作者的既有选择）

- 一律用**中文**回复。
- 改动要「**一次性全改到位**」，但**真实的回归必须被抓出来并明确报告**——不要为了显得顺利而隐瞒问题。
- 提交信息用中文，写清「改了什么 + 为什么」，按要点列。
- **不许把没验证的东西说成验证过**：说「已实测」，就必须真跑过（冒烟测试 / 构建 / 哈希对比），并给出实际输出。
- 文档与代码不一致时，**以代码为准**，并同步修正文档。
- UI / 美术风格：**克制的科幻感配色**（深色底、低饱和青紫色点缀），不要花哨。
- 地图与点位相关：坐标用**小数**、实时路径刷新周期 **5 tick** —— 这些是既有实现的选择，别擅自改。

---

## 11. 上手建议先读

1. [`README.md`](README.md) —— 这是什么、怎么装
2. [`docs/WORKSPACE.md`](docs/WORKSPACE.md) —— 工作区运维：目录、脚本、编码坑、Git
3. [`CHANGELOG.md`](CHANGELOG.md) —— 最近几条，看当前进展到哪一步
4. [`VERSIONING.md`](VERSIONING.md) —— 版本与发布规则
5. [`docs/DEPLOY.md`](docs/DEPLOY.md) —— 部署与验证清单
