# 冻结记录：mod-1.9.6_plugin-1.9.4

| 项目 | 值 |
|---|---|
| 冻结日期 | 2026-09-17 |
| 客户端模组（Forge） | **1.9.6** — `mmap-forge-1.9.6.jar` |
| 客户端模组（Fabric） | **1.9.5** — `mmap-fabric-1.9.5.jar`（上游原版，未改动） |
| 服务端插件（Paper） | **1.9.4** — `MMmap-Plugin-1.9.4.jar` |
| 网络协议 | **v1**（未改动） |
| 目标环境 | MC 1.20.1 / Forge 47.4.10 / Fabric / Paper 1.20.1，Java 17 |

> 注意 Forge 版是 **1.9.6**、Fabric 版还是 **1.9.5** —— 正常现象：
> 这次只改了 Forge 侧的渲染逻辑，按 [`../../VERSIONING.md`](../../VERSIONING.md) 第 3 节
> 「上游没动、只改 Forge 侧 → 修订号 +1」，所以只升 Forge 版。

## 校验值（SHA256）

```
F6C1163DBE48DB57A179A11E892071305135105E0E2B8280AF4DB9BA9CD46A6A  mmap-fabric-1.9.5.jar
F47099C3663EB8C721D95D8245B4329C02DC3000A4D751B4B87DCA9A3E3D0F1D  mmap-forge-1.9.6.jar
3E6AEF9C886188061FD6ED19106CFAACDDD588252DF7C44D000CB52EB8EA0ED0  MMmap-Plugin-1.9.4.jar
```

## 这一版改了什么

**修复：打开聊天栏（以及其它界面）时小地图消失。**

* 原因：`MinimapManager` 里有「只要开着界面就不画小地图」的判断（继承自原版 Fabric 模组）。
  而 Forge 的 `RenderGuiEvent.Post` 在开着界面时**照样会触发**
  （`GameRenderer` 在 `screen != null` 时依然调用 `gui.render` —— 这正是聊天栏打开时快捷栏仍然可见的原因），
  所以小地图是被自己那行判断挡掉的，不是引擎不画。
* 改法：**默认一直显示小地图**，只在本模组自己的大地图界面隐藏；
  新增配置项 `minimap.hideInGui`（默认 `false`）可切回「开界面就隐藏」的旧行为。
* 改动文件：`MinimapManager.java`（渲染判断）、`ConfigManager.java`（新增配置字段）。
  渲染细节、协议、服务端逻辑均未改动。

## 验证情况

* ✅ `gradlew clean build` 编译通过（产物 `mmap-forge-1.9.6.jar`）。
* ✅ jar 内 `mods.toml` 的 `version="1.9.6"` 正确。
* ✅ 已部署到 Forge 实例 `自用客户端\.minecraft\versions\时之旅者\mods\`
  （旧 `mmap-forge-1.9.5.jar` 已删除，避免同 modId 双 jar 加载失败），
  部署后哈希与 `dist/` 一致：`F47099C3…`。
* ⚠️ **待使用者实机确认**：打开聊天栏 / 背包 / ESC 菜单时小地图是否按预期保留。

## 回滚方式

把本目录的 `mmap-forge-1.9.5.jar`（在 [`../mod-1.9.5_plugin-1.9.4/`](../mod-1.9.5_plugin-1.9.4/) 里）
复制回 `versions\时之旅者\mods\`，并删掉 `mmap-forge-1.9.6.jar` 即可。
