# 星轨 StarTrail 版本规划与发布流程

## 1. 三个版本号，各管各的

星轨 StarTrail 由两个可独立升级的部件组成，各自有自己的版本号，**不要混用**：

| 部件 | 版本号写在哪（唯一事实来源） | 当前值 |
|---|---|---|
| Forge 客户端模组 | `mod-forge/gradle.properties` → `mod_version` | `1.0.0` |
| Fabric 客户端模组（上游原版） | 上游 jar 自带，我们不写 | `1.9.5` |
| 服务端 Paper 插件 | `plugin-paper/src/main/resources/plugin.yml` → `version:` | `1.0.0` |

> 改模组版本**只改 `gradle.properties` 一处**：`build-mod.ps1` 会读出它决定产物名，
> `build.gradle` 也会把它展开进 `mods.toml`。别去手改 `mods.toml`。

## 2. 成品命名规则（强制）

平台后缀必须带，**两种平台的 jar 绝不允许同名**：

| 平台 | 文件名格式 | 例 |
|---|---|---|
| Forge | `startrail-forge-<版本>.jar` | `startrail-forge-1.0.0.jar` |
| Fabric | `mmap-fabric-<版本>.jar` | `mmap-fabric-1.9.5.jar` |
| 服务端插件 | `StarTrail-Plugin-<版本>.jar` | `StarTrail-Plugin-1.0.0.jar` |

Gradle 构建出来的原始文件名叫 `startrail-<版本>.jar`（`archivesName = startrail`），发布时由
`tools\build-mod.ps1` 自动改名成 `startrail-forge-<版本>.jar` 放进 `dist\client-mod\`。
**不要把 `build\libs\startrail-<版本>.jar` 直接发给别人**，那个名字和 Fabric 版会撞车。

## 3. 什么时候加版本号

采用 `主.次.修订`，跟上游 Fabric 版保持同号：

| 情况 | 动作 |
|---|---|
| 上游 Fabric 版出了新版本（比如 1.9.6） | Forge 移植版跟着变成 **1.9.6**（先按 `docs/移植说明.md` 重做一遍移植） |
| 上游没动，只改 Forge 侧代码（修 bug / 加功能） | 修订号 +1：1.9.5 → **1.9.6** |
| 只改服务端插件 | 插件版本 +1，模组版本**不动** |
| 只改文档 / 工具脚本 / 工作区结构 | **不动版本号**，只在 `CHANGELOG.md` 记一笔 |

规则背后的理由：版本号对玩家只有一个含义——「客户端和服务端是不是一组」。
所以**只有会让玩家需要重新拿 jar 的改动才动版本号**。

## 4. 协议版本是另一回事

网络协议版本（现在 = **1**）跟上面的版本号无关，它管的是「客户端和服务端能不能对话」：

* 位置：插件握手包第 2~5 字节（`PacketSender.sendHandshake` 里的 `writeInt(1)`）。
* 只要**包类型号（1~9）和字段顺序不变**，就永远是 1，插件和模组随便各自升级。
* 一旦要改包结构（加字段、改顺序、换类型号）：
  1. 协议版本 +1（插件端 `writeInt`，模组端 `NetworkHandler.PROTOCOL_VERSION` 字符串同步改成 `"2"`）；
  2. **必须同时发布**插件 + Fabric 模组 + Forge 模组；
  3. 在 `CHANGELOG.md` 该版本条目顶部写 **【协议不兼容，客户端与服务端须同时升级】**；
  4. 先更新 [`docs/PROTOCOL.md`](docs/PROTOCOL.md) 再动代码。

> 小技巧（已经在用）：模组读握手包时把最后几个字段包在 `try/catch` 里，
> 老服务端少发字段也不会崩，会退回默认值 —— 加字段时保持这个习惯，就能做到「服务端先升级」。

## 5. 当前兼容矩阵

| | 插件 1.9.4（协议 1） |
|---|---|
| Forge 模组 **1.9.7**（协议 1） | ✅ 兼容（当前在用） |
| Forge 模组 1.9.6 / 1.9.5（协议 1） | ✅ 兼容（分别归档在 `releases/mod-1.9.6_plugin-1.9.4/`、`mod-1.9.5_plugin-1.9.4/`） |
| Fabric 模组 1.9.5（协议 1） | ✅ 兼容 |

## 6. 发布流程（照做即可）

```
0. 改代码前先备份
   powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -Tag 改XXX之前

1. 改代码
   · 客户端模组：mod-forge\src\main\java\...
   · 服务端插件：plugin-paper\src\main\java\...

2. 改版本号（只有需要玩家换 jar 时才改）
   · 模组：mod-forge\gradle.properties       →  mod_version=1.9.6
   · 插件：plugin-paper\src\main\resources\plugin.yml → version: '1.9.5'

3. 写 CHANGELOG.md
   在文件顶部加一条，写清「改了什么 / 要不要同时升级 / 怎么验证」

4. 编译并发布到 dist（自动起对文件名 + 刷新 SHA256SUMS.txt）
   · 模组：powershell -ExecutionPolicy Bypass -File tools\build-mod.ps1
   · 插件：powershell -ExecutionPolicy Bypass -File tools\build-plugin.ps1

5. 自测（**改过插件一定要先跑这个**：起一个一次性 Paper 服，不动线上服务端）
   powershell -ExecutionPolicy Bypass -File tools\smoke-test-plugin.ps1
   · 模组：进游戏实测（见 docs\DEPLOY.md）
   · 成品校验：powershell -ExecutionPolicy Bypass -File tools\verify-dist.ps1 -Check

6. 冻结这一版（不可再改）
   新建 releases\mod-<模组版本>_plugin-<插件版本>\
   把 dist 里本次涉及的 jar 复制进去，附一份 NOTES.md（版本号、日期、改了什么、验证情况）

7. 收尾备份
   powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -Tag v1.9.6发布
   powershell -ExecutionPolicy Bypass -File tools\backup.ps1 -Prune -Keep 20

8. 部署到客户端 / 服务端（见 docs\DEPLOY.md），部署后核对 SHA256 与 dist 一致
```

## 7. 回滚流程

| 想回到 | 从哪拿 |
|---|---|
| 上一个发布版本的成品 jar | `releases\<对应目录>\` |
| 某个时间点的整套工作区（含源码、工具、文档） | `backups\StarTrail_<时间戳>_<标签>.zip`，直接解压覆盖回工作区 |
| 只回滚某个文件 | 同上，从 zip 里单独取 |

回滚后**记得同步把 `dist\` 里被覆盖的 jar 也换回去**，`dist` 必须始终等于「当前在用的一组」。
换完跑一次 `tools\verify-dist.ps1 -Check`；如果 `SHA256SUMS.txt` 已过期，用 `-Write` 重生成。

## 8. 备份策略

* **什么时候备份**：改动代码/配置前、发布后、动工作区结构前。`backup.ps1` 只要几秒。
* **存在哪**：默认 `<工作区>\backups\`（快照会自动排除 `backups\` 自身，不会套娃）。
  强烈建议定期把 `backups\` 整个复制到**别的盘**，例如 `E:\Minecraft\_backups\StarTrail\`：
  ```powershell
  robocopy "E:\Minecraft\服务器\插件\StarTrail\backups" "E:\Minecraft\_backups\StarTrail" /MIR
  ```
* **保留多少**：默认 `-Prune -Keep 20` 留最近 20 份。一份约 6~10MB（已排除 Gradle 构建产物）。
* **快照里有什么/没什么**：有源码、成品 jar、文档、`archive\port-work` 全套中间产物；
  没有 `build\`、`.gradle\`（能重新生成，且现有依赖缓存都在 `C:\Users\Lenovo\.gradle`，
  换机器时那个缓存目录不在备份范围内，需要重新联网拉一次）。

## 9. 成品哈希与可复现构建

**规则：一个版本的源码 ↔ 一个确定的 SHA256。**

Forge 模组自 2026-09-17 起是**可复现构建**（`mod-forge/build.gradle` 里
`preserveFileTimestamps = false` + `reproducibleFileOrder = true`，并去掉了 MANIFEST 里的构建时间戳），
所以**同样的源码 + 同样的 `mod_version`，重新编译出来的 jar 逐字节相同**。

这意味着：

* 想知道「手上的 jar 是不是这个版本」→ 算 SHA256 和 `dist\SHA256SUMS.txt` 比一比就行，
  不需要再进游戏试。命令：
  ```powershell
  powershell -ExecutionPolicy Bypass -File tools\verify-dist.ps1 -Check     # 校验 dist 自身
  (Get-FileHash <某个jar> -Algorithm SHA256).Hash                            # 手工算某个文件的
  ```
* **不要**为了「重新出一份干净的」而随手重编译后分发——哈希变了会让玩家的客户端和服务端
  对不上号。要重出就得同步更新 `dist\SHA256SUMS.txt`、`releases\` 和 `CHANGELOG.md` 里的哈希。
* Fabric 模组和服务端插件**不是**我们构建的，哈希由上游决定，改不了。
