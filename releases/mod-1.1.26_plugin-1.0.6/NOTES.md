# mod-1.1.26_plugin-1.0.6

| 项目 | 值 |
|---|---|
| 日期 | 2026-11-24 |
| Forge 模组 | **1.1.26** |
| Fabric 模组（上游原版，未改动） | 1.9.5 |
| 服务端插件 | **1.0.6**（本次未变） |
| 协议版本 | v2（未改动） |
| 通道名 | `startrail:main`（未改动） |

## 改了什么

**F3 调试屏改为叠在小地图上方**（1.1.25 的方向搞反了）。

### 反馈

要的是「**F3 面板盖在小地图上面**」，而不是把小地图挪到 F3 信息下方。

### 根因

小地图原来挂在 `RenderGuiEvent.Post`，而该事件在 **所有 GUI overlay 之后**触发：

```
ForgeGui.render():
   所有 GUI overlay（含 F3 调试屏 DEBUG_TEXT）
   ↓
   RenderGuiEvent.Post  ← 小地图在这里画
```

所以小地图**永远画在 F3 上面**，把调试信息盖住了。

### 改法

小地图改为注册成 **GUI overlay**，并排在 F3 之前：

```java
event.registerBelow(
   VanillaGuiOverlay.DEBUG_TEXT.id(),
   "minimap",
   (forgeGui, guiGraphics, partialTick, sw, sh) -> this.renderOverlay(guiGraphics, partialTick)
);
```

Forge 按注册顺序依次绘制，**排在前面 ⇒ 被 F3 盖住**。

* 判定逻辑（大地图界面不画、`minimap.hideInGui` 等）原样迁移到 `renderOverlay`，行为不变；
* `IGuiOverlay.render` 签名是 `(ForgeGui, GuiGraphics, float, int, int)` 共 **5 个参数**
  （首次编译漏了 `ForgeGui`，已修）。

### 同时撤掉 1.1.25 的挪移逻辑

不再为 F3 下移或缩小地图，**小地图保持用户设置的方位与大小**。
`DebugScreenProbe` 已无调用者，整个文件已删除。

## 要不要换 jar

**只需客户端**：`startrail-forge-1.1.26.jar` → `versions\时之旅者\mods\`，**需重启游戏**。
服务端 `StarTrail-Plugin-1.0.6.jar` 不变。

## 成品哈希

| 文件 | 字节 | SHA256 |
|---|---|---|
| `startrail-forge-1.1.26.jar` | 172,680 | `EFD9BC7E20A665A186212ECECAEBA755A37473431EA5B307F7D24007B94A5002` |
| `StarTrail-Plugin-1.0.6.jar` | 89,772 | `FA931883F23BC97844BE433594D6C7608A05E07D412E11C151CC20F6F6A1D6CC` |
| `mmap-fabric-1.9.5.jar` | 120,594 | `F6C1163DBE48DB57A179A11E892071305135105E0E2B8280AF4DB9BA9CD46A6A` |

## 验证情况

* ✅ `gradlew compileJava` 与 `tools\build-mod.ps1 -Offline` 均 `BUILD SUCCESSFUL`，产物 172,680 字节。
  （首次编译失败：lambda 参数少写 `ForgeGui`，`ForgeGui` 无法转 `GuiGraphics`，已修）
* ✅ 逐项核对改动完整性：

  | 检查项 | 结果 |
  |---|---|
  | mod 总线已监听 `RegisterGuiOverlaysEvent` | ✅ |
  | 注册锚点是 `VanillaGuiOverlay.DEBUG_TEXT.id()` | ✅ |
  | 小地图旧的 `addListener(this::onRenderGui)` 已移除 | ✅ |
  | `MinimapRenderer` 已无任何 F3 挪移代码 | ✅ |
  | `DebugScreenProbe` 无残留引用（文件已删） | ✅ |

  （`StarTrailClient.onRenderGui` 仍在，但它只管导航标记，与小地图无关。）
* ✅ `tools\verify-dist.ps1 -Check` 全绿。
* ⚠️ **未进游戏实测** —— overlay 注册顺序的实际视觉效果必须进游戏验证。

## 实机确认清单

1. 换 1.1.26 并重启游戏。
2. **小地图保持在右上角原位**（不再因为 F3 而下移或缩小）。
3. 按 F3 → **调试面板应盖在小地图上面**，右侧那栏文字能看清。
4. 再按 F3 关闭 → 小地图恢复正常（本来就没动过，位置应无变化）。
5. 顺带确认小地图平时显示正常（改成 overlay 后仍然该出现在原来的位置）。

## 风险提示

这次把小地图从 `RenderGuiEvent.Post` 迁到 GUI overlay 体系，是**渲染时机的改变**。
理论上 overlay 与 Post 都在 HUD 层、绘制内容一致，但若出现「小地图不见了」或「位置偏移」，
请立刻告诉我——回滚只需把版本换回 1.1.24（`releases/mod-1.1.24_plugin-1.0.6/`）。
