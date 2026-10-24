# releases/ — 发布冻结区

## 这个目录是干什么的

每次「正式发布」——也就是**一组实测过、可以拿去用的客户端模组 + 服务端插件**——
在这里冻结一份，**之后不再改动**。

和 `dist/` 的区别：

| | `dist/` | `releases/` |
|---|---|---|
| 含义 | **当前在用**的那一组成品 | **历史上发布过的**每一组 |
| 会不会被覆盖 | 会（每次 `build-mod.ps1` 覆盖同名文件） | 不会，只增不改 |
| 什么时候用 | 日常部署、复制到客户端/服务端 | 回滚、查「上个版本是什么样」、对比 |

## 目录命名

```
releases\mod-<模组版本>_plugin-<插件版本>\
```

例如 `mod-1.9.5_plugin-1.9.4\` = 模组 1.9.5 + 插件 1.9.4 这一组。

每个目录里放：本次涉及的成品 jar（直接复制 `dist\` 里的）+ 一份 `NOTES.md`
（日期、版本、改了什么、在哪个客户端实测过、已知问题）。

## 已冻结的版本

| 目录 | 日期 | 模组 | 插件 | 协议 | 状态 |
|---|---|---|---|---|---|
| `mod-1.9.5_plugin-1.9.4/` | 2026-09-17 | 1.9.5 (Fabric + Forge) | 1.9.4 | v1 | 首次 Forge 移植，已实机验证 |
| `mod-1.9.6_plugin-1.9.4/` | 2026-09-17 | Forge **1.9.6** / Fabric 1.9.5 | 1.9.4 | v1 | 修「开聊天栏小地图消失」；已部署 |
| `mod-1.9.7_plugin-1.9.4/` | 2026-09-17 | Forge **1.9.7** / Fabric 1.9.5 | 1.9.4 | v1 | 修「图片边界偏 1 格」；已部署，待实机确认 |
| `mod-1.0.0_plugin-1.0.0/` | 2026-10-14 | Forge **1.0.0** / Fabric 1.9.5 | 1.0.0 | v1 | 正式命名「星轨 StarTrail」，两端同时升级 |
| `mod-1.0.1_plugin-1.0.0/` | 2026-10-24 | Forge **1.0.1** / Fabric 1.9.5 | 1.0.0 | v1 | 修「设置面板盖不住区域名/传送点名」；**只换客户端**，待实机确认 |
| `mod-1.0.2_plugin-1.0.0/` | 2026-10-24 | Forge **1.0.2** / Fabric 1.9.5 | 1.0.0 | v1 | 小地图设置页隐藏整张大地图；**只换客户端**，待实机确认 |

## 怎么新增一份

```powershell
# 1. 复制成品（示例：发布 1.9.6 的 Forge 模组）
$tag = "mod-1.9.6_plugin-1.9.4"
New-Item -ItemType Directory "releases\$tag" -Force
Copy-Item "dist\client-mod\mmap-forge-1.9.6.jar" "releases\$tag\"
Copy-Item "dist\server-plugin\MMmap-Plugin-1.9.4.jar" "releases\$tag\"
# 2. 写 NOTES.md，并更新上面那张表
# 3. 收尾备份
powershell -ExecutionPolicy Bypass -File ..\tools\backup.ps1 -Tag "$tag-发布"
```

完整流程见 [`../VERSIONING.md`](../VERSIONING.md) 第 6 节。
