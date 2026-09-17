# MMmap 工作区

MMmap = **Minecraft 客户端地图模组**（小地图 / 大地图 / 区域 / 传送点 / MythicMobs 头颅）
＋ **服务端同步插件**（Paper，负责把区域、传送点、图片绑定、怪物位置推给客户端）。

这个文件夹是它**唯一的开发、构建、发布、备份目录**。所有成品、源码、文档、快照都在这里。

---

## 1. 当前状态（2026-09-17 整理）

| 组件 | 版本 | 成品文件 | 状态 |
|---|---|---|---|
| Forge 客户端模组 | **1.9.5** | `dist/client-mod/mmap-forge-1.9.5.jar` | ✅ **已实机验证**：加载/联网/图片/画面都正常 |
| Fabric 客户端模组（上游原版） | **1.9.5** | `dist/client-mod/mmap-fabric-1.9.5.jar` | 原样保留，未做任何改动 |
| 服务端 Paper 插件 | **1.9.4** | `dist/server-plugin/MMmap-Plugin-1.9.4.jar` | ✅ **已有源码工程** `plugin-paper/`，可自己编译发布 |
| 网络协议 | **v1** | [`docs/PROTOCOL.md`](docs/PROTOCOL.md) | 插件 1.9.4 ↔ 两个平台的模组 1.9.5 一致 |

三者可以互相独立升级，只要协议（包类型号 + 字段顺序）不变。规则见 [`VERSIONING.md`](VERSIONING.md)。

## 2. 目录结构

```
MMmap/
├─ README.md                  ← 你在这里：工作区总览
├─ VERSIONING.md              版本规划、命名规则、发布/回滚流程
├─ CHANGELOG.md               变更记录（改了什么、哪个版本）
│
├─ dist/                      ★ 成品区：要拿到游戏里用的 jar 都从这里取
│  ├─ client-mod/
│  │  ├─ mmap-forge-1.9.5.jar        Forge 客户端模组（MC 1.20.1 / Forge 47.x）
│  │  └─ mmap-fabric-1.9.5.jar       Fabric 客户端模组（上游原版，MC 1.20.1 / Fabric）
│  ├─ server-plugin/
│  │  └─ MMmap-Plugin-1.9.4.jar      服务端 Paper 插件
│  └─ SHA256SUMS.txt          成品校验清单（tools\verify-dist.ps1 生成/校验）
│
├─ releases/                  历史发布冻结区：每次正式发布存一份，不再改动
│  └─ mod-1.9.5_plugin-1.9.4/
│
├─ mod-forge/                 ★ Forge 模组工程（Gradle，可直接编译改代码）
│  ├─ gradlew.bat             自带 Gradle Wrapper 8.1.1
│  ├─ gradle.properties       ★ 版本号在这里改（mod_version）
│  ├─ build.gradle            版本/映射/依赖配置
│  ├─ README.md               工程说明 + Fabric→Forge 移植对照表
│  └─ src/main/
│     ├─ java/com/mmap/client/   35 个类（原模组 33 个 + 新增 2 个）
│     └─ resources/              mods.toml / pack.mcmeta / 语言文件
│
├─ plugin-paper/              ★ Paper 服务端插件工程（Gradle，可编译改代码）
│  ├─ gradlew.bat             同样自带 Wrapper 8.1.1
│  ├─ gradle.properties       ★ Paper API 版本、MythicMobs jar 路径
│  ├─ build.gradle            版本号从 plugin.yml 读，无需手改
│  ├─ README.md               工程说明 + 与原版 jar 的等价性验证结果
│  └─ src/main/
│     ├─ java/com/mmap/server/   20 个类（反编译恢复的源码）
│     └─ resources/              plugin.yml / config.yml / language.yml / regions.yml / warps.yml
│
├─ docs/
│  ├─ 移植说明.md             Fabric→Forge 移植的来龙去脉与关键改动
│  ├─ PROTOCOL.md             网络协议 v1 逐包字段表（改协议前必读）
│  └─ DEPLOY.md               部署清单：装到哪个客户端 / 服务端、怎么验证
│
├─ tools/
│  ├─ jdk-17.0.20.1+1        工作区自带的 JDK 17（构建用，约 300MB，不进 Git/快照）
│  ├─ build-mod.ps1           编译模组 + 发布到 dist + 刷新校验清单
│  ├─ build-plugin.ps1        编译服务端插件 + 发布到 dist + 刷新校验清单
│  ├─ smoke-test-plugin.ps1   一次性 Paper 服冒烟测试插件（不动线上服务端）
│  ├─ verify-dist.ps1         列出/刷新/校验 dist 成品的 SHA256
│  └─ backup.ps1              整个工作区打包快照到 backups\
│
├─ backups/                   快照备份（自动排除 build\、.gradle\、backups\ 自身）
│
└─ archive/                   归档区：只读参考，日常用不到，但别删
   └─ port-work/              移植过程的中间产物
      ├─ src-mod/             Fabric 原版反编译源码（intermediary 命名）
      ├─ src-mojmap/          Fabric 原版反编译源码（Mojang 官方命名）★ 移植就是照它改的
      ├─ src-plugin/          服务端插件反编译源码 ★ 目前唯一的插件源码
      ├─ x-mod/ x-plugin/     两个 jar 解包后的原始内容
      ├─ mappings/            intermediary / mojmap 映射表
      ├─ tools/               当时的转换脚本（genmap.js、copy-sources.js）
      ├─ fabric-leftovers/    从 Forge 源码树里清出来的 Fabric 残留文件
      └─ mmap-mojmap.jar      重映射后的中间 jar
```

## 3. 常用操作

> 本机 PowerShell 禁止运行脚本，所有 .ps1 都要加 `-ExecutionPolicy Bypass`。

**重新编译模组**（产物自动进 `dist\client-mod\`）：
```powershell
powershell -ExecutionPolicy Bypass -File tools\build-mod.ps1
powershell -ExecutionPolicy Bypass -File tools\build-mod.ps1 -Clean -Offline   # 全量重编译、离线
```

**重新编译服务端插件**（产物自动进 `dist\server-plugin\`）：
```powershell
powershell -ExecutionPolicy Bypass -File tools\build-plugin.ps1
```

**改完插件先冒烟测试**（起一个一次性 Paper 服装上新 jar，**不动线上服务端**）：
```powershell
powershell -ExecutionPolicy Bypass -File tools\smoke-test-plugin.ps1
powershell -ExecutionPolicy Bypass -File tools\smoke-test-plugin.ps1 -WithMythicMobs -KeepServer
```

**备份整个工作区**：
```powershell
powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -Tag 改完XXX之前
powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -List                # 看有哪些快照
powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -Prune -Keep 20      # 只留最近 20 份
```

**校验成品没被动过**：
```powershell
powershell -ExecutionPolicy Bypass -File tools\verify-dist.ps1 -Check
```
> Forge 模组是**可复现构建**：同样源码 + 同样版本号 → 逐字节相同的 jar、相同的 SHA256。
> 所以「这个 jar 是哪份源码编的」可以直接用哈希判定，规则见 [`VERSIONING.md` 第 9 节](VERSIONING.md)。

**发新版本 / 部署**：见 [`VERSIONING.md`](VERSIONING.md) 和 [`docs/DEPLOY.md`](docs/DEPLOY.md)。

## 4. 环境与前提

| 项目 | 值 |
|---|---|
| 客户端目标 | Minecraft **1.20.1** + Forge **47.4.10**（`mods.toml` 写的是 `[47,)`），**Java 17** |
| Fabric 目标 | Minecraft 1.20.1 + Fabric API |
| 服务端 | Paper **1.20.1** |
| 本工作区自带 JDK 17 | `tools\jdk-17.0.20.1+1`（约 300MB，两个工程的 `gradle.properties` 都指向它；已排除在 Git 与快照外） |
| 服务端运行环境 | JDK **21**（`C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot`）+ `Paper-1.20.1.jar` |
| Gradle | 两个工程都自带 Wrapper 8.1.1，发行版已缓存在 `C:\Users\Lenovo\.gradle\wrapper\dists`，不用单独装 Gradle |
| 依赖缓存 | `C:\Users\Lenovo\.gradle\caches`，已缓存 Forge 1.20.1-47.4.10、MCP 配置、Paper API，**可以离线编译** |
| Git | **已装**：`D:\Git`（2.55.0，仅当前用户，配置全在 `D:\Git\etc\gitconfig`，C 盘只有一条 PATH 环境变量） |

> **改 `tools\*.ps1` 时的坑**：本机是 Windows PowerShell 5.1，读**不带 BOM** 的 UTF-8 脚本时
> 会把中文当 GBK 解码，脚本直接语法报错。所以这些脚本都必须存成 **UTF-8 with BOM**
> （现有 5 个脚本都带 BOM，已验证可正常运行）。用编辑器改完请确认编码没被改成「UTF-8 无 BOM」。

> **改 `plugin-paper\gradle.properties` 时的坑**：Gradle 按 ISO-8859-1 解码这个文件，
> 直接写中文路径会变乱码导致构建失败。中文路径要用 `\uXXXX` 转义写（现在就是这么写的）。

## 5. 还没做的事（待办）

- [x] ~~服务端插件没有源码工程~~ → ✅ 已建好 `plugin-paper/`，并通过等价性验证与冒烟测试。
- [x] ~~Forge 模组画面细节没确认~~ → ✅ 使用者实机确认：从 Fabric 版转过来**没有问题**。
- [x] ~~本机没装 Git~~ → ✅ 已装 `D:\Git`，工作区已纳入版本控制。
- [x] ~~矩形区域世界名不一致~~ → **已决策：不修**，服务端一律用 `shape: polygon`。
- [ ] **线上服务端是否换用「源码编译版」插件 jar**：`dist/` 里现在放的是从源码编译的版本
      （已验证等价），但线上服务端仍跑着原版 jar。要不要换，等你定。
- [ ] 插件源码是**反编译产物**（局部变量名是 `var4` 这种）。能正常改能编译，
      但如果你以后要长期维护插件，可以顺手把改到的那几个类整理一下命名。
- [ ] 上游 Fabric 版若出新版本，要重走一遍移植流程（步骤见 [`docs/移植说明.md`](docs/移植说明.md)）。

## 6. 版本控制（Git）

Git 装在 **`D:\Git`**（2.55.0，仅当前用户）；`D:\Git\cmd` 已加入用户 PATH，
新开的终端里直接 `git` 就能用。所有 Git 配置写在 **`D:\Git\etc\gitconfig`**（不占 C 盘）。

```powershell
cd E:\Minecraft\服务器\插件\MMmap
git status
git log --oneline
git diff                 # 看改了什么
git checkout -- <文件>   # 丢掉某个文件的改动
```

* **提交身份**：`XY_Momo <1756784800@qq.com>`
  （本仓库与系统级都写了；系统级那份在 `D:\Git\etc\gitconfig`，所以以后新建的仓库也用它）。
  要换的话：`git config user.name "..."` / `git config user.email "..."`。
* **谁被纳管**：源码、资源、文档、`dist/` 成品、`releases/` 冻结件、`tools/` 脚本。
* **谁不被纳管**（体积大或可重新生成，见 `.gitignore`）：`backups/`、两个工程的 `build/` 与 `.gradle/`、
  `archive/port-work/` 里的映射表和大 jar、解包出来的 `x-mod/` `x-plugin/`。
  → 所以 **Git 不能替代 `backups/`**：要回滚「整个工作区（含中间产物）」还是用 zip 快照。
