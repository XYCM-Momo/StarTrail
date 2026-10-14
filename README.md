# 星轨 StarTrail

> **Minecraft 客户端地图模组 ＋ 服务端同步插件**
> 大地图 · 小地图 · 区域标记 · 传送点 · 点位导航 · MythicMobs 头像

由 **星野尘梦工作室 · 默默** 制作。

星轨由两个配合使用的部分组成：

| 组件 | 平台 | 作用 |
|---|---|---|
| **客户端模组** `startrail-forge-<版本>.jar` | Minecraft 1.20.1 + Forge 47.x | 渲染小地图／大地图、区域多边形、传送点、点位导航、怪物头像 |
| **服务端插件** `StarTrail-Plugin-<版本>.jar` | Paper 1.20.1 | 把区域、传送点、地图图片绑定、MythicMobs 怪物位置推送给客户端 |

模组也可以单独装在原版 / 其它服务端上使用，此时只是失去服务端同步的区域与传送点。

## ⚠️ 两端必须成对升级

插件与模组之间通过自定义网络通道通信（通道名 `startrail:main`）。
**插件与模组的版本必须成对更新**：混用新旧版本时，客户端会收不到任何服务端数据——区域、传送点、怪物位置全部不显示，且不会报错。

## 功能

- 🗺️ **小地图** —— 大小／缩放／位置／形状可调，跟随视角，5 tick 实时刷新
- 🌍 **大地图** —— 可缩放平移，玩家、区域、传送点、怪物同屏显示
- 🔷 **区域标记** —— 支持多边形与矩形区域，名称、边框颜色可配
- 📍 **传送点** —— 点位图标可自定义，使用加密图片分发
- 🧭 **点位导航** —— 路径规划，5 tick 实时刷新，支持分层地图寻路
- 👹 **MythicMobs 集成** —— 服务端同步怪物位置，客户端显示怪物头像
- 🔐 **图片加密** —— 地图图片以自有格式加密（`.startrail`），避免原图外流

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | **1.20.1** |
| Forge（客户端） | **47.x**（`mods.toml` 要求 `[47,)`），需要 **Java 17** |
| Paper（服务端） | **1.20.1** |
| MythicMobs（可选） | 装了才会同步怪物头像 |

## 安装

### 服务端（Paper）

1. 把 `StarTrail-Plugin-<版本>.jar` 放进服务端的 `plugins/` 目录
2. 重启服务端
3. 启动日志会打印署名横幅，看到它就说明加载成功：

   ```
   [StarTrail] ==================================================
   [StarTrail]  StarTrail · 星轨   v1.0.0
   [StarTrail]  本插件由 星野尘梦工作室 · 默默 制作
   [StarTrail] ==================================================
   ```

   插件的数据目录是 `plugins/StarTrail/`。

### 客户端（Forge）

1. 把 `startrail-forge-<版本>.jar` 放进 `.minecraft/mods/`
2. 启动游戏。首次启动会生成配置 `config/startrail.json` 与数据目录 `.minecraft/startrail/`

> 客户端的日志前缀是 `[startrail/]`，可用它排查问题。

## 命令与权限

### 服务端

主命令 `/startrail`，别名 `st` / `trail` / `map` / `minimap`。

| 子命令 | 说明 |
|---|---|
| `/startrail help` | 帮助 |
| `/startrail list` | 列出区域 / 传送点 |
| `/startrail info <ID>` | 查看某个区域或传送点的详情 |
| `/startrail reload` | 重载配置 |
| `/startrail map <add\|remove\|list>` | 管理地图图片绑定 |
| `/startrail warp <add\|remove\|list\|link\|unlink\|tp>` | 管理传送点 |
| `/startrail waypoint <add\|remove\|link>` | 管理导航点位 |

| 权限节点 | 说明 | 默认 |
|---|---|---|
| `startrail.command` | 允许使用星轨命令 | OP |
| `startrail.command.reload` | 允许重载配置 | OP |
| `startrail.command.tp` | 允许使用传送（`/startrail tp`、`/startrail warp tp`、大地图右键传送） | OP |

### 客户端（本地指令）

| 指令 | 说明 |
|---|---|
| `/startrail crypt` | 加密地图图片 |
| `/startrail crypt delete` | 删除已加密图片 |
| `/startrail crypt help` | 加密功能帮助 |
| `/startrail nav <x> <z>` | 导航到指定坐标 |
| `/startrail nav stop` | 停止导航 |

## 数据目录

### 服务端 `plugins/StarTrail/`

```
plugins/StarTrail/
├─ config.yml          主配置（图标阈值、MythicMobs 同步半径等）
├─ images.yml          地图图片绑定
├─ language.yml        所有提示文本（改完 /startrail reload 生效）
├─ regions/*.yml       区域定义
├─ warps/*.yml         传送点定义
└─ waypoints/          点位数据备份
```

### 客户端 `.minecraft/startrail/`

```
.minecraft/startrail/
├─ map/          大地图加密图片（*.startrail）
├─ legend/       图例图标
├─ mm/           MythicMobs 怪物头像
└─ waypoints/    点位数据
```

配置文件：`.minecraft/config/startrail.json`

> 加密图片的扩展名是 `.startrail`；为了向后兼容，读取时**同样接受旧版的 `.mmap`**。

## 从源码构建

两个工程都自带 Gradle Wrapper（8.1.1），**不需要单独安装 Gradle**。

```bash
# 服务端插件
cd plugin-paper
./gradlew build            # Windows: gradlew.bat build
# 产物: build/libs/StarTrail-Plugin-<版本>.jar
#       版本号读自 src/main/resources/plugin.yml

# 客户端模组
cd mod-forge
./gradlew build            # Windows: gradlew.bat build
# 产物: build/libs/startrail-<版本>.jar
#       版本号读自 gradle.properties 的 mod_version
```

### ⚠️ 构建前需要处理的本机路径

仓库里保留了开发者的本机绝对路径配置，**直接 clone 下来构建会失败**，请按需修改：

| 文件 | 键 | 怎么处理 |
|---|---|---|
| `mod-forge/gradle.properties` | `org.gradle.java.home` | 指向本机 JDK 17。改成你的 JDK 路径，**或直接删掉这一行**（删掉后使用当前 `JAVA_HOME`） |
| `plugin-paper/gradle.properties` | `org.gradle.java.home` | 同上 |
| `plugin-paper/gradle.properties` | `mythicmobs_jar` | 指向本机已安装的 MythicMobs jar，**仅编译期使用，不会打进产物**。没有 MythicMobs 时该行可留空或删除，插件运行时会自动跳过这部分集成 |

> `plugin-paper/gradle.properties` 由 Gradle 按 **ISO-8859-1** 解码，中文路径必须写成 `\uXXXX` 转义形式，否则会出现乱码导致构建失败。

依赖已缓存在 Gradle 用户目录时，可以加 `--offline` 离线构建。

## 项目结构

```
StarTrail/
├─ mod-forge/      Forge 客户端模组工程（Java 17 / Gradle）
├─ plugin-paper/   Paper 服务端插件工程（Java 17 / Gradle）
├─ dist/           成品区：当前版本 jar ＋ SHA256SUMS.txt 校验清单
├─ releases/       历史版本冻结区（每个正式版本一份，不再改动）
├─ docs/           协议、部署、移植说明
├─ tools/          构建 / 冒烟测试 / 校验 / 备份脚本
└─ archive/        移植期归档（只读参考，日常用不到）
```

## 文档

| 文档 | 内容 |
|---|---|
| [docs/PROTOCOL.md](docs/PROTOCOL.md) | 网络协议逐包字段表（**改协议前必读**） |
| [docs/DEPLOY.md](docs/DEPLOY.md) | 部署清单与验证步骤 |
| [docs/移植说明.md](docs/移植说明.md) | Fabric → Forge 移植过程记录 |
| [docs/WORKSPACE.md](docs/WORKSPACE.md) | 工作区运维手册（构建／备份／校验脚本、环境注意事项） |
| [VERSIONING.md](VERSIONING.md) | 版本规划、发布与回滚流程 |
| [CHANGELOG.md](CHANGELOG.md) | 变更记录 |

## 许可

[MIT License](LICENSE) © 2026 星野尘梦工作室 · 默默

---

**星野尘梦工作室 · 默默** 制作
