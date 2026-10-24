# 星轨 StarTrail 工作区运维手册

> 这份文档面向**本仓库的维护者**，记录工作区结构、构建／备份／校验脚本用法，以及环境上的坑。
> 只想安装使用的读者请直接看 [README.md](../README.md)。

本工作区（`E:\Minecraft\服务器\插件\StarTrail`）是星轨**唯一的开发、构建、发布、备份目录**。

---

## 1. 目录结构

```
StarTrail/
├─ README.md                  面向使用者的项目说明
├─ LICENSE                    MIT 许可正文
├─ VERSIONING.md              版本规划、命名规则、发布/回滚流程
├─ CHANGELOG.md               变更记录（改了什么、哪个版本）
│
├─ dist/                      ★ 成品区：要拿到游戏里用的 jar 都从这里取
│  ├─ client-mod/
│  │  ├─ startrail-forge-1.0.3.jar     Forge 客户端模组（MC 1.20.1 / Forge 47.x）★ 当前在用
│  │  └─ mmap-fabric-1.9.5.jar         Fabric 客户端模组（上游原版，未改动、不参与改名）
│  ├─ server-plugin/
│  │  └─ StarTrail-Plugin-1.0.0.jar    服务端 Paper 插件
│  └─ SHA256SUMS.txt          成品校验清单（由 tools\verify-dist.ps1 生成/校验）
│
├─ releases/                  历史发布冻结区：每次正式发布存一份，不再改动
│  ├─ mod-1.9.5_plugin-1.9.4/ … mod-2.2.7_plugin-2.1.1/
│  ├─ mod-1.0.0_plugin-1.0.0/         ← 星轨首个版本
│  ├─ mod-1.0.1_plugin-1.0.0/         ← 修「设置面板盖不住名字」（只换客户端）
│  ├─ mod-1.0.2_plugin-1.0.0/         ← 小地图设置页隐藏大地图（只换客户端）
│  └─ mod-1.0.3_plugin-1.0.0/         ← 小地图设置页加实时预览（只换客户端）
│
├─ mod-forge/                 ★ Forge 模组工程（Gradle，可直接编译改代码）
│  ├─ gradlew.bat             自带 Gradle Wrapper 8.1.1
│  ├─ gradle.properties       ★ 版本号在这里改（mod_version）
│  ├─ build.gradle            版本/映射/依赖配置
│  ├─ README.md               工程说明 + Fabric→Forge 移植对照表
│  └─ src/main/
│     ├─ java/com/asterovie/momo/startrail/client/   客户端全部源码
│     └─ resources/            mods.toml / pack.mcmeta / 语言文件
│
├─ plugin-paper/              ★ Paper 服务端插件工程（Gradle，可直接编译改代码）
│  ├─ gradlew.bat             同样自带 Wrapper 8.1.1
│  ├─ gradle.properties       ★ Paper API 版本、MythicMobs jar 路径
│  ├─ build.gradle            版本号从 plugin.yml 读取，无需手改
│  ├─ README.md               工程说明 + 与原版 jar 的等价性验证结果
│  └─ src/main/
│     ├─ java/com/asterovie/momo/startrail/server/   服务端全部源码
│     └─ resources/              plugin.yml / config.yml / language.yml / regions.yml / warps.yml
│
├─ docs/
│  ├─ WORKSPACE.md            ← 你在这里
│  ├─ 移植说明.md             Fabric→Forge 移植的来龙去脉与关键改动
│  ├─ PROTOCOL.md             网络协议 v1 逐包字段表（改协议前必读）
│  ├─ DEPLOY.md               部署清单：装到哪个客户端 / 服务端、怎么验证
│  └─ sessions/               ★ 开发对话归档（详见第 6 节）
│     ├─ 2026-09-18至2026-10-14_星轨开发全程对话.md   可读的对话记录
│     └─ raw/                 逐字原始记录（session.v4.jsonl.zstd）
│
├─ tools/
│  ├─ jdk-17.0.20.1+1         工作区自带的 JDK 17（构建用，约 300MB，不进 Git/快照）
│  ├─ build-mod.ps1           编译模组 + 发布到 dist + 刷新校验清单
│  ├─ build-plugin.ps1        编译服务端插件 + 发布到 dist + 刷新校验清单
│  ├─ smoke-test-plugin.ps1   一次性 Paper 服冒烟测试插件（不动线上服务端）
│  ├─ verify-dist.ps1         列出/刷新/校验 dist 成品的 SHA256
│  ├─ backup.ps1              整个工作区打包快照到 backups\
│  ├─ waypoint-editor/        网页版点位编辑器（含启动脚本与测试）
│  └─ export-session.mjs      ★ 把 DSH 会话记录导出成可读 Markdown（见第 6 节）
│
├─ backups/                   快照备份（自动排除 build\、.gradle\、.gradle-home\、JDK、backups\ 自身）
│
└─ archive/                   归档区：只读参考，日常用不到，但别删
   └─ port-work/              移植过程的中间产物
      ├─ src-mod/             Fabric 原版反编译源码（intermediary 命名）
      ├─ src-mojmap/          Fabric 原版反编译源码（Mojang 官方命名）★ 移植就是照它改的
      ├─ src-plugin/          服务端插件反编译源码（移植期的插件源码）
      ├─ x-mod/ x-plugin/     两个 jar 解包后的原始内容（不进 Git）
      ├─ mappings/            映射表（大文件不进 Git）
      ├─ tools/               当时的转换脚本（genmap.js、copy-sources.js）
      ├─ fabric-leftovers/    从 Forge 源码树里清出来的 Fabric 残留文件
      └─ mmap-mojmap.jar      重映射后的中间 jar（不进 Git）
```

## 2. 常用操作

> 本机 PowerShell 禁止运行脚本，所有 `.ps1` 都要加 `-ExecutionPolicy Bypass`。

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

> **手动构建**（不走脚本时）必须串行，且要指定 Gradle 用户目录：
> ```powershell
> $env:GRADLE_USER_HOME='E:\Minecraft\服务器\插件\StarTrail\.gradle-home'
> cd plugin-paper; .\gradlew.bat build --offline --console=plain
> cd ..\mod-forge; .\gradlew.bat build --offline --console=plain
> ```
> ⚠️ **两个工程绝不能并行构建**——它们共用 `.gradle-home`，并行会抢锁，表现为静默卡住十几分钟。

> Forge 模组是**可复现构建**：同样源码 + 同样版本号 → 逐字节相同的 jar、相同的 SHA256。
> 所以「这个 jar 是哪份源码编的」可以直接用哈希判定，规则见 [`VERSIONING.md` 第 9 节](../VERSIONING.md)。

**发新版本 / 部署**：见 [`VERSIONING.md`](../VERSIONING.md) 和 [`DEPLOY.md`](DEPLOY.md)。

## 3. 环境与前提

| 项目 | 值 |
|---|---|
| 客户端目标 | Minecraft **1.20.1** + Forge **47.4.10**（`mods.toml` 写的是 `[47,)`），**Java 17** |
| Fabric 目标 | Minecraft 1.20.1 + Fabric API |
| 服务端 | Paper **1.20.1** |
| 本工作区自带 JDK 17 | `tools\jdk-17.0.20.1+1`（约 300MB，两个工程的 `gradle.properties` 都指向它；已排除在 Git 与快照外） |
| 服务端运行环境 | JDK **21**（`C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot`）+ `Paper-1.20.1.jar` |
| Gradle | 两个工程都自带 Wrapper 8.1.1；本工作区还把依赖缓存在 `.gradle-home\`，**可完全离线编译** |
| Git | 已装：`D:\Git`（2.55.0，仅当前用户，配置全在 `D:\Git\etc\gitconfig`，C 盘只有一条 PATH 环境变量） |

## 4. 三个必须记住的编码坑

1. **`tools\*.ps1` 必须存成 UTF-8 with BOM。**
   本机是 Windows PowerShell 5.1，读**不带 BOM** 的 UTF-8 脚本时会把中文当 GBK 解码，
   脚本直接语法报错（表现是 `Missing expression after ','` 之类莫名其妙的错误）。
   现有 6 个脚本都带 BOM。**用编辑器或自动化工具改完，务必确认 BOM 还在。**
   补 BOM 的方法：
   ```powershell
   $p='tools\backup.ps1'; $b=[IO.File]::ReadAllBytes($p)
   $n=New-Object byte[] ($b.Length+3); $n[0]=0xEF;$n[1]=0xBB;$n[2]=0xBF
   [Array]::Copy($b,0,$n,3,$b.Length); [IO.File]::WriteAllBytes($p,$n)
   ```

2. **`plugin-paper\gradle.properties` 是 ISO-8859-1。**
   Gradle 按 ISO-8859-1 解码这个文件，直接写中文路径会变乱码导致构建失败。
   中文路径必须用 `\uXXXX` 转义写（现在就是这么写的）。

3. **`mod-forge\build.gradle` 的 `processResources` 必须带 `filteringCharset = 'UTF-8'`。**
   否则 `mods.toml` 里的中文（模组名、作者）在中文 Windows 上会按 GBK 展开成乱码。

## 5. 版本控制（Git）

**远程仓库**：`https://github.com/XYCM-Momo/StarTrail.git`（**私有**），主分支 `main`。

```powershell
cd E:\Minecraft\服务器\插件\StarTrail
git status
git log --oneline
git diff                 # 看改了什么
git add -A
git commit -m "说明"
git push
```

* **提交身份**：`XY_Momo <1756784800@qq.com>`（仓库级与系统级都写了；系统级那份在 `D:\Git\etc\gitconfig`，所以以后新建的仓库也用它）
* **推送凭据**：已存入 Windows 凭据管理器，`git push` 不再需要每次登录
* ⚠️ **邮箱会出现在每条提交的元数据里**。若将来要把仓库转为公开，建议先换成 GitHub 匿名邮箱
  （`<用户ID>+<用户名>@users.noreply.github.com`）再公开。
* **谁被纳管**：源码、资源、文档、`dist/` 成品、`releases/` 冻结件、`tools/` 脚本。
* **谁不被纳管**（体积大或可重新生成，见 `.gitignore`）：`backups/`、`.gradle-home/`、
  两个工程的 `build/` 与 `.gradle/`、`tools/jdk-17.0.20.1+1/`、
  `archive/port-work/` 里的映射表与大 jar（解包出来的 `x-mod/` `x-plugin/`）。
  → 所以 **Git 不能替代 `backups/`**：要回滚「整个工作区（含中间产物）」还是用 zip 快照。

## 6. 会话记录（开发对话归档）

本项目的开发过程是在 DSH（DeepSeek Harness）里进行的，会话记录可以导出成可读文档放进
`docs/sessions/`，作为「当时为什么这么改」的一手材料 —— 代码只说明结果，对话才说明原因。

- `docs/sessions/2026-09-18至2026-10-14_星轨开发全程对话.md`
  2026-09-18 ~ 2026-10-14 的完整开发对话（55 条用户消息、564 条助手回复、894 次工具调用）。
  工具调用与输出折叠在 `<details>` 里、超长部分截断，用户与助手的原话保持原样。
- `docs/sessions/raw/session-*.jsonl.zstd`
  逐字原始记录，含被折叠、被截断、以及内部推理的全部内容。**只有它是完整的**，
  上面那份 Markdown 是它的可读视图。两者一起提交进 Git，所以仓库里的记录是自洽的。

### 导出方式（以后有新会话照做即可）

```powershell
# 1. 列出本机所有会话记录，找到目标会话（按修改时间排序看最新的即可）
node tools\export-session.mjs --list

# 2. 导出成 Markdown（上一步会打印完整路径，直接照抄）
node tools\export-session.mjs "$env:USERPROFILE\.dsh\sessions\<工作区转义名>\<会话ID>\session.v4.jsonl.zstd" docs\sessions\<文件名>.md

# 3. 顺手把原始记录也归档一份
Copy-Item "$env:USERPROFILE\.dsh\sessions\<工作区转义名>\<会话ID>\session.v4.jsonl.zstd" docs\sessions\raw\
```

> ⚠️ **别用普通解压工具打开这些 `.jsonl.zstd`**。DSH 是「每条记录追加一个 zstd 帧」写进同一个
> 文件的，常见解压（Node 的 `zstdDecompressSync`、`zstd -d` 等）只解第一个帧，会得到一两百字节
> 的会话头就结束，看起来像是文件坏了 —— 其实是没解完。`export-session.mjs` 自己按 zstd 帧结构
> 逐帧遍历，所以能全部解出来。
>
> 另外：同一会话目录里可能同时存在 `session.jsonl.zstd`（旧格式，v0）和 `session.v4.jsonl.zstd`
> （新格式）。旧格式那份**时间上是残缺的**（我们的例子里它 2026-09-21 就停写了），**优先用 v4 那份**。
## 7. 还没做的事（待办）

- [x] ~~服务端插件没有源码工程~~ → ✅ 已建好 `plugin-paper/`，并通过等价性验证与冒烟测试。
- [x] ~~Forge 模组画面细节没确认~~ → ✅ 使用者实机确认：从 Fabric 版转过来**没有问题**。
- [x] ~~本机没装 Git~~ → ✅ 已装 `D:\Git`，工作区已纳入版本控制。
- [x] ~~矩形区域世界名不一致~~ → **已决策：不修**，服务端一律用 `shape: polygon`。
- [x] ~~线上服务端是否换用源码编译版插件 jar~~ → ✅ 1.0.0 已直接部署源码编译版。
- [x] ~~工作区目录仍叫 MMmap~~ → ✅ 已改名为 `StarTrail`，硬编码路径同步，重建后产物哈希不变。
- [ ] 插件源码是**反编译产物**（局部变量名是 `var4` 这种）。能正常改能编译，
      但如果你以后要长期维护插件，可以顺手把改到的那几个类整理一下命名。
- [ ] 上游 Fabric 版若出新版本，要重走一遍移植流程（步骤见 [`移植说明.md`](移植说明.md)）。
