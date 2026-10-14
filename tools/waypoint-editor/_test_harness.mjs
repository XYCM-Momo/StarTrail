// 星轨 StarTrail 点位编辑器 - DOM mock 运行时测试
// 用法: node _test_harness.mjs "StarTrail点位编辑器.html"
import fs from "node:fs";

const htmlPath = process.argv[2];
const html = fs.readFileSync(htmlPath, "utf8");
const m = html.match(/<script>([\s\S]*?)<\/script>/);
if (!m) { console.error("no script block"); process.exit(1); }
let js = m[1];

/* ---------- stubs（Blob/URL/crypto 用 Node 原生实现，保证 .mmap 解密可测） ---------- */
const ctxStub = new Proxy({}, {
  get(t, p) { return (p in t) ? t[p] : ((...a) => {}); },
  set(t, p, v) { t[p] = v; return true; }
});
function makeEl(id) {
  const listeners = {};
  const el = {
    id: id || "", value: "", textContent: "", innerHTML: "", className: "",
    style: {}, checked: false, type: "",
    _listeners: listeners,
    classList: { add() {}, remove() {}, toggle() {}, contains() { return false; } },
    addEventListener(ev, fn) { (listeners[ev] = listeners[ev] || []).push(fn); },
    click() { (listeners.click || []).forEach(f => f()); },
    appendChild(c) { return c; },
    setPointerCapture() {},
    getBoundingClientRect() { return { left: 0, top: 0 }; },
    clientWidth: 800, clientHeight: 600,
    getContext() { return ctxStub; },
    width: 0, height: 0
  };
  return el;
}
const els = {};
const docListeners = {};
globalThis.document = {
  _listeners: docListeners,
  getElementById(id) { return els[id] || (els[id] = makeEl(id)); },
  createElement(tag) { return makeEl(tag); },
  createTextNode(t) { return { text: t }; },
  addEventListener(ev, fn) { (docListeners[ev] = docListeners[ev] || []).push(fn); },
  body: { appendChild() {} }
};
globalThis.window = { devicePixelRatio: 1, addEventListener() {} };
globalThis.ResizeObserver = class { constructor() {} observe() {} disconnect() {} };
const ls = new Map();
globalThis.localStorage = {
  getItem: k => (ls.has(k) ? ls.get(k) : null),
  setItem: (k, v) => ls.set(k, String(v)),
  removeItem: k => ls.delete(k)
};
globalThis.FileReader = class {
  readAsDataURL() { this.result = ""; this.onload && this.onload(); }
  readAsText() { this.result = ""; this.onload && this.onload(); }
  readAsArrayBuffer(b) { this.result = b; this.onload && this.onload(); }
};
globalThis.Image = class {
  set src(v) { this.naturalWidth = 2000; this.naturalHeight = 1000; this.onload && this.onload(); }
  get src() { return this._src; }
};

/* ---------- 运行脚本 + 测试（同一作用域，异步测试通过 __TEST_RUN 暴露） ---------- */
const tests = `
globalThis.__TEST_RUN = (async () => {
  const results = [];
  const assert = (name, cond) => { results.push([name, !!cond]); if (!cond) console.error("FAIL: " + name); };
  const c = globalThis.crypto;

  // 先同步输入框，readFields() 才能读到
  fields.mapId.value = "main_map"; fields.fileName.value = "overworld.png"; fields.world.value = "world";
  fields.minX.value = -1000; fields.minZ.value = -1000; fields.maxX.value = 1000; fields.maxZ.value = 1000;

  // 1. 像素<->世界 换算
  state.minX = -1000; state.minZ = -1000; state.maxX = 1000; state.maxZ = 1000;
  state.image = { naturalWidth: 2000, naturalHeight: 1000 };
  let w = pixelToWorld(0, 0);
  assert("pixel(0,0) -> NW(-1000,-1000)", w && w.x === -1000 && w.z === -1000);
  w = pixelToWorld(2000, 1000);
  assert("pixel(W,H) -> SE(1000,1000)", w && w.x === 1000 && w.z === 1000);
  w = pixelToWorld(1000, 500);
  assert("pixel center -> world center", w && w.x === 0 && w.z === 0);
  let p = worldToPixel(-1000, -1000);
  assert("world NW -> pixel(0,0)", p.px === 0 && p.py === 0);
  p = worldToPixel(1000, 1000);
  assert("world SE -> pixel(W,H)", p.px === 2000 && p.py === 1000);

  // 2. 点位操作
  state.waypoints = [
    { id: "a", x: 0, y: 64, z: 0, links: [], _extra: {} },
    { id: "b", x: 100, y: 70, z: 200, links: [], _extra: {} }
  ];
  assert("nextWpId 从 0 开始", nextWpId() === "wp0");
  // 2b. 自动 ID 取已有最大数字 +1
  state.waypoints = [
    { id: "wp5", x: 0, y: 64, z: 0, links: [], _extra: {} },
    { id: "wp12", x: 0, y: 64, z: 0, links: [], _extra: {} }
  ];
  assert("已有 wp5/wp12 -> wp13", nextWpId() === "wp13");
  state.waypoints = [
    { id: "shop1", x: 0, y: 64, z: 0, links: [], _extra: {} },
    { id: "spawn2", x: 0, y: 64, z: 0, links: [], _extra: {} }
  ];
  assert("任意含数字 ID 取最大+1", nextWpId() === "wp3");
  state.waypoints = [
    { id: "wp3", x: 0, y: 64, z: 0, links: [], _extra: {} },
    { id: "x9", x: 0, y: 64, z: 0, links: [], _extra: {} }
  ];
  assert("混合 ID 取全局最大", nextWpId() === "wp10");
  state.waypoints = [
    { id: "wp0", x: 0, y: 64, z: 0, links: [], _extra: {} }
  ];
  assert("只有 wp0 -> wp1", nextWpId() === "wp1");
  state.waypoints = [
    { id: "a", x: 0, y: 64, z: 0, links: [], _extra: {} },
    { id: "b", x: 100, y: 70, z: 200, links: [], _extra: {} }
  ];
  setLink("a", "b", true);
  assert("setLink 双向", state.waypoints[0].links.includes("b") && state.waypoints[1].links.includes("a"));
  setLink("a", "b", false);
  assert("unlink 双向清空", state.waypoints[0].links.length === 0 && state.waypoints[1].links.length === 0);
  deleteWaypoint("a");
  assert("deleteWaypoint 移除自身", state.waypoints.length === 1);
  state.waypoints = [
    { id: "a", x: 0, y: 64, z: 0, links: ["b"], _extra: {} },
    { id: "b", x: 100, y: 70, z: 200, links: ["a"], _extra: {} }
  ];

  // 3. 导出点位 JSON
  const json = JSON.parse(buildWaypointJson());
  assert("json.world", json.world === "world");
  assert("json.waypoints.length = 2", json.waypoints.length === 2);
  const a = json.waypoints[0];
  assert("json 字段顺序 id,x,y,z,links", Object.keys(a).join(",") === "id,x,y,z,links");
  assert("json links", JSON.stringify(a.links) === '["b"]');
  assert("json 坐标为数字", typeof a.x === "number" && typeof a.y === "number" && typeof a.z === "number");

  // 4. 导出 images.yml 片段
  const yaml = buildYamlSnippet();
  assert("yaml 含地图ID", yaml.includes("main_map:"));
  assert("yaml 含 fileName", yaml.includes("fileName: overworld.png"));
  assert("yaml 含边界", yaml.includes("minX: -1000") && yaml.includes("maxZ: 1000"));

  // 5. 导入点位 JSON（含文件名推断地图ID）
  importJsonText(JSON.stringify({ world: "nether", waypoints: [{ id: "c", x: 1, y: 2, z: 3, links: [] }, { id: "d", x: 4, y: 5, z: 6, links: ["c"] }] }), "nether.json");
  assert("导入数量", state.waypoints.length === 2);
  assert("导入 world", state.world === "nether");
  assert("从文件名推断 mapId", state.mapId === "nether");

  // 6. 解析 images.yml 片段
  parseYamlSnippet("  nether_map:\\n    fileName: nether.png\\n    world: world_nether\\n    minX: -500\\n    minZ: -500\\n    maxX: 500\\n    maxZ: 500");
  assert("yml 地图ID", state.mapId === "nether_map");
  assert("yml fileName", state.fileName === "nether.png");
  assert("yml world", state.world === "world_nether");
  assert("yml 边界", state.minX === -500 && state.maxZ === 500);

  // 7. 保留未知字段（往返不丢数据）
  const n = normalizeWp({ id: "e", x: 1, y: 2, z: 3, links: [], displayName: "E点" });
  assert("normalizeWp 保留 extra", n._extra.displayName === "E点");
  state.waypoints.push(n);
  const json2 = JSON.parse(buildWaypointJson());
  const e = json2.waypoints.find(w => w.id === "e");
  assert("导出包含 extra", e && e.displayName === "E点");

  // 8. 添加点位换算（点击像素 -> 世界坐标）
  state.image = { naturalWidth: 2000, naturalHeight: 1000 };
  state.minX = 0; state.minZ = 0; state.maxX = 2000; state.maxZ = 1000;
  fields.minX.value = 0; fields.minZ.value = 0; fields.maxX.value = 2000; fields.maxZ.value = 1000;
  state.defaultY = 80;
  fields.defaultY.value = 80;
  const before = state.waypoints.length;
  addWaypointAt(1000, 500);
  assert("点击添加点位", state.waypoints.length === before + 1);
  const added = state.waypoints[state.waypoints.length - 1];
  assert("新点位 X 换算", added.x === 1000);
  assert("新点位 Z 换算", added.z === 500);
  assert("新点位默认 Y", added.y === 80);
  assert("新点位自动 id", /^wp\\d+$/.test(added.id));

  // ============ .mmap 加密格式（与 mod-forge ImageCrypto 一致） ============
  // 用 Node WebCrypto 按 Java 同款算法（SHA-256 前16字节 + AES/CBC/PKCS5）构造真实 .mmap
  const PNG_1x1 = new Uint8Array([
    0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A, 0,0,0,0x0D,0x49,0x48,0x44,0x52,
    0,0,0,1,0,0,0,1,8,6,0,0,0,0x1F,0x15,0xC4,0x89,
    0,0,0,0x0D,0x49,0x44,0x41,0x54,0x78,0x9C,0x62,0,1,0,0,5,0,1,
    0x0D,0x0A,0x2D,0xB4, 0,0,0,0,0x49,0x45,0x4E,0x44,0xAE,0x42,0x60,0x82
  ]);
  async function makeMmap(bytes, name, keyStr) {
    const km = await c.subtle.digest("SHA-256", new TextEncoder().encode(keyStr));
    const key = await c.subtle.importKey("raw", km.slice(0, 16), { name: "AES-CBC" }, false, ["encrypt"]);
    const iv = c.getRandomValues(new Uint8Array(16));
    const enc = new Uint8Array(await c.subtle.encrypt({ name: "AES-CBC", iv }, key, bytes));
    const nb = new TextEncoder().encode(name);
    const hdr = new Uint8Array([0x4D, 0x4D, 0x41, 0x50, 1, (nb.length >> 8) & 0xFF, nb.length & 0xFF]);
    const out = new Uint8Array(hdr.length + nb.length + 16 + enc.length);
    out.set(hdr, 0); out.set(nb, hdr.length); out.set(iv, hdr.length + nb.length); out.set(enc, hdr.length + nb.length + 16);
    return out;
  }
  const KEY = "MMmap-Xaero-Static-Image-2026";

  // 9. 正确密钥解密
  const mmap = await makeMmap(PNG_1x1, "overworld.png", KEY);
  const dec = await decryptMmap(mmap);
  assert("mmap 解密成功", dec !== null);
  assert("mmap 还原原始文件名", dec && dec.name === "overworld.png");
  const decBytes = dec ? new Uint8Array(await dec.blob.arrayBuffer()) : null;
  assert("mmap 还原图片字节", decBytes && decBytes.length === PNG_1x1.length);
  assert("mmap 图片内容一致", decBytes && decBytes.every((v, i) => v === PNG_1x1[i]));

  // 10. 错误密钥 -> null
  const wrong = await makeMmap(PNG_1x1, "x.png", "WrongKey");
  assert("错误密钥解密为 null", (await decryptMmap(wrong)) === null);

  // 11. 非 mmap 魔数 -> null（编辑器兼容明文图片路径，不经过这里）
  assert("非 mmap 魔数返回 null", (await decryptMmap(new Uint8Array([0x89, 0x50, 0x4E, 0x47]))) === null);

  // 12. 版本不符 / 截断 -> null
  const badVer = Uint8Array.from(mmap); badVer[4] = 9;
  assert("版本不符返回 null", (await decryptMmap(badVer)) === null);
  assert("长度不足返回 null", (await decryptMmap(new Uint8Array([0x4D, 0x4D, 0x41, 0x50, 1, 0, 5]))) === null);

  // 13. applyImageBlob 用 .mmap 内嵌原始名命名（fileName=原始名, mapId=去扩展名）
  state.fileName = ""; state.mapId = "";
  await applyImageBlob(dec.blob, "foo.mmap", dec.name);
  assert("fileName 取 .mmap 内原始名", state.fileName === "overworld.png");
  assert("mapId 取原始名去扩展名", state.mapId === "overworld");
  assert("applyImageBlob 设好图片", state.image && state.imageName === "foo.mmap");

  // ============ 吸附网格（0.5 格 = MC 0.5 方块） ============
  // 14. snapCoord
  fields.snapEnabled.checked = true;
  fields.snapGrid.value = 0.5;
  readFields();
  assert("snapCoord 0.37 -> 0.5", snapCoord(0.37) === 0.5);
  assert("snapCoord 0.2 -> 0", snapCoord(0.2) === 0);
  assert("snapCoord 1.0 -> 1", snapCoord(1.0) === 1);
  assert("snapCoord 清除浮点尾巴 0.1+0.2 -> 0.5", snapCoord(0.1 + 0.2) === 0.5);
  assert("snapCoord 关闭吸附则原样", (() => { const old = state.snapEnabled; state.snapEnabled = false; const r = snapCoord(0.37); state.snapEnabled = old; return r === 0.37; })());

  // 15. 添加点位吸附（先重置视口：applyImageBlob 的 fitView 会改 scale/offset）
  view.offsetX = 0; view.offsetY = 0; view.scale = 1;
  state.image = { naturalWidth: 2000, naturalHeight: 1000 };
  fields.minX.value = 0; fields.minZ.value = 0; fields.maxX.value = 2000; fields.maxZ.value = 1000;
  state.defaultY = 80; fields.defaultY.value = 80;
  const before2 = state.waypoints.length;
  addWaypointAt(1040, 499);
  const added2 = state.waypoints[state.waypoints.length - 1];
  assert("添加吸附 X=1040", added2.x === 1040);
  assert("添加吸附 Z=499", added2.z === 499);
  assert("添加后数量 +1", state.waypoints.length === before2 + 1);

  // 16. 拖动移动吸附（屏幕->像素->世界->吸附）
  moveWaypoint(added2.id, 1000.37, 500.24);
  assert("拖动吸附 X 1000.37 -> 1000.5", added2.x === 1000.5);
  assert("拖动吸附 Z 500.24 -> 500", added2.z === 500);

  // 17. 吸附预览跟随鼠标：添加模式下 pointermove 即使 hoverId 不变也要重绘
  {
    state.image = { naturalWidth: 2000, naturalHeight: 1000 };
    state.waypoints = []; // 保证 hitTest 不命中，hoverId 不变
    hoverId = null;
    const origRedraw = redraw;
    let rc = 0;
    redraw = () => { rc++; origRedraw(); };
    const evt = (x, y) => ({ clientX: x, clientY: y, pointerId: 1 });
    addMode = true;
    canvas._listeners.pointermove.forEach(fn => fn(evt(1000, 500)));
    const rcFirst = rc;
    canvas._listeners.pointermove.forEach(fn => fn(evt(1001, 501))); // hoverId 不变
    const rcSecond = rc;
    addMode = false;
    redraw = origRedraw;
    assert("添加模式鼠标移动触发重绘(预览跟随)", rcFirst > 0 && rcSecond > rcFirst);
  }

  // 18. 新建点位自动连通上次选中的点位
  view.offsetX = 0; view.offsetY = 0; view.scale = 1;
  state.image = { naturalWidth: 2000, naturalHeight: 1000 };
  fields.minX.value = 0; fields.minZ.value = 0; fields.maxX.value = 2000; fields.maxZ.value = 1000;
  fields.autoLink.checked = true;
  readFields();
  state.waypoints = [{ id: "hub", x: 100, y: 64, z: 100, links: [], _extra: {} }];
  selectedId = "hub";
  addWaypointAt(500, 500);
  const newWp = state.waypoints[state.waypoints.length - 1];
  assert("自动连通：新点位 id 从 0 开始", newWp.id === "wp0");
  assert("自动连通：新点位包含 hub", newWp.links.includes("hub"));
  assert("自动连通：hub 包含新点位（双向）", getWp("hub").links.includes(newWp.id));
  assert("自动连通后新点位成为选中", selectedId === newWp.id);

  // 19. 关闭自动连通后新建不连接
  fields.autoLink.checked = false;
  selectedId = "hub";
  addWaypointAt(600, 500);
  const newWp2 = state.waypoints[state.waypoints.length - 1];
  assert("关闭自动连通：新点位无连接", !newWp2.links.includes("hub") && !getWp("hub").links.includes(newWp2.id));
  fields.autoLink.checked = true;
  readFields();

  // 20. 表单修改实时自动应用（X/Y/Z）
  const tgt = getWp("wp1");
  selectedId = tgt.id;
  $("f-wp-x").value = "333.5"; $("f-wp-x")._listeners.input.forEach(fn => fn({}));
  assert("自动应用 X=333.5", tgt.x === 333.5);
  $("f-wp-y").value = "70"; $("f-wp-y")._listeners.input.forEach(fn => fn({}));
  assert("自动应用 Y=70", tgt.y === 70);
  $("f-wp-z").value = "-12.25"; $("f-wp-z")._listeners.input.forEach(fn => fn({}));
  assert("自动应用 Z=-12.25", tgt.z === -12.25);
  $("f-wp-x").value = "abc"; $("f-wp-x")._listeners.input.forEach(fn => fn({}));
  assert("非法数字不应用", tgt.x === 333.5);

  // 21. 自动应用 ID + links 引用重映射 + 重复 ID 拒绝
  state.waypoints.push({ id: "oldA", x: 1, y: 2, z: 3, links: ["oldB"], _extra: {} });
  state.waypoints.push({ id: "oldB", x: 4, y: 5, z: 6, links: ["oldA"], _extra: {} });
  selectedId = "oldA";
  $("f-wp-id").value = "newA"; $("f-wp-id")._listeners.input.forEach(fn => fn({}));
  assert("自动应用 ID newA", !!getWp("newA") && !getWp("oldA"));
  assert("ID 改名后 oldB.links 重映射", getWp("oldB").links.includes("newA"));
  selectedId = "newA";
  $("f-wp-id").value = "oldB"; $("f-wp-id")._listeners.input.forEach(fn => fn({}));
  assert("重复 ID 拒绝应用", !!getWp("newA") && getWp("newA").id === "newA");
  $("f-wp-id").value = ""; $("f-wp-id")._listeners.input.forEach(fn => fn({}));
  assert("空 ID 拒绝应用", getWp("newA").id === "newA");

  // 22. Shift+方向键/WASD 直线创建点位
  view.offsetX = 0; view.offsetY = 0; view.scale = 1;
  state.image = { naturalWidth: 2000, naturalHeight: 1000 };
  fields.minX.value = 0; fields.minZ.value = 0; fields.maxX.value = 2000; fields.maxZ.value = 1000;
  fields.stepDist.value = 10;
  fields.autoLink.checked = true;
  readFields();
  state.waypoints = [{ id: "hub", x: 100, y: 64, z: 100, links: [], _extra: {} }];
  selectedId = "hub";
  const press = (key, shift) => document._listeners.keydown.forEach(fn => fn({ key, shiftKey: !!shift, target: document.body, preventDefault() {} }));
  press("ArrowRight", true);
  const e1 = state.waypoints[state.waypoints.length - 1];
  assert("Shift+→ 创建东侧点位 10 格", e1.x === 110 && e1.z === 100);
  assert("方向创建自动双向连通", e1.links.includes("hub") && getWp("hub").links.includes(e1.id));
  assert("方向创建后新点位选中", selectedId === e1.id);
  press("w", true);
  const e2 = state.waypoints[state.waypoints.length - 1];
  assert("Shift+W 从新点位向北延伸", e2.x === 110 && e2.z === 90);
  assert("延伸自动连通上一点", e2.links.includes(e1.id));
  const addModeBefore = addMode;
  press("a", true);
  assert("Shift+A 创建西侧点位而非切换添加模式", state.waypoints.length === 4 && addMode === addModeBefore);
  const e3 = state.waypoints[state.waypoints.length - 1];
  assert("西侧点位坐标 (100, 90)", e3.x === 100 && e3.z === 90);
  const nBefore = state.waypoints.length;
  selectedId = null;
  press("ArrowDown", true);
  assert("无选中时不创建", state.waypoints.length === nBefore);
  selectedId = "hub";
  fields.autoLink.checked = false;
  readFields();
  press("ArrowDown", true);
  const e4 = state.waypoints[state.waypoints.length - 1];
  assert("autoLink 关闭时不连通", !e4.links.includes("hub") && !getWp("hub").links.includes(e4.id));
  fields.autoLink.checked = true;
  readFields();

  // 23. 实时连接线/十字线绘制不抛异常（选中点 + 鼠标位置）
  selectedId = "hub";
  hoverPos = { sx: 300, sy: 200 };
  drawLinkPreview();
  addMode = true;
  drawSnapPreview();
  drawLinkPreview();
  addMode = false;
  assert("连接线/十字线预览绘制无异常", true);

  // 24. 多选模式：点选 / 框选 / 点空白清除 / 批量删除
  view.offsetX = 0; view.offsetY = 0; view.scale = 1;
  state.image = { naturalWidth: 2000, naturalHeight: 1000 };
  fields.minX.value = 0; fields.minZ.value = 0; fields.maxX.value = 2000; fields.maxZ.value = 1000;
  multiIds.clear();
  state.waypoints = [
    { id: "p1", x: 100, y: 64, z: 100, links: ["p3"], _extra: {} },
    { id: "p2", x: 200, y: 64, z: 100, links: [], _extra: {} },
    { id: "p3", x: 300, y: 64, z: 100, links: ["p1"], _extra: {} },
    { id: "p4", x: 500, y: 64, z: 500, links: ["p3"], _extra: {} }
  ];
  selectedId = null;
  toggleMultiMode();
  assert("多选模式开启", multiMode === true);
  assert("多选与添加互斥", addMode === false);
  const pd = (x, y) => ({ clientX: x, clientY: y, pointerId: 1 });
  canvas._listeners.pointerdown.forEach(fn => fn(pd(200, 100)));
  canvas._listeners.pointerup.forEach(fn => fn(pd(200, 100)));
  assert("点击切换多选 p2", multiIds.has("p2"));
  canvas._listeners.pointerdown.forEach(fn => fn(pd(200, 100)));
  canvas._listeners.pointerup.forEach(fn => fn(pd(200, 100)));
  assert("再次点击取消 p2", !multiIds.has("p2"));
  canvas._listeners.pointerdown.forEach(fn => fn(pd(50, 50)));
  canvas._listeners.pointermove.forEach(fn => fn(pd(350, 150)));
  canvas._listeners.pointerup.forEach(fn => fn(pd(350, 150)));
  assert("框选 p1/p2/p3", multiIds.has("p1") && multiIds.has("p2") && multiIds.has("p3"));
  assert("框选不含 p4", !multiIds.has("p4"));
  canvas._listeners.pointerdown.forEach(fn => fn(pd(900, 900)));
  canvas._listeners.pointerup.forEach(fn => fn(pd(900, 900)));
  assert("点空白清除多选", multiIds.size === 0);
  canvas._listeners.pointerdown.forEach(fn => fn(pd(50, 50)));
  canvas._listeners.pointermove.forEach(fn => fn(pd(350, 150)));
  canvas._listeners.pointerup.forEach(fn => fn(pd(350, 150)));
  deleteMultiSelected();
  assert("批量删除后只剩 p4", state.waypoints.length === 1 && state.waypoints[0].id === "p4");
  assert("批量删除清理 links", state.waypoints[0].links.length === 0);
  assert("批量删除后多选清空", multiIds.size === 0);
  toggleMultiMode();
  assert("退出多选模式", multiMode === false);

  // 25. M 键切换多选 / Esc 清除
  document._listeners.keydown.forEach(fn => fn({ key: "m", shiftKey: false, target: document.body, preventDefault() {} }));
  assert("M 键开启多选", multiMode === true);
  multiIds.add("p4");
  document._listeners.keydown.forEach(fn => fn({ key: "Escape", shiftKey: false, target: document.body, preventDefault() {} }));
  assert("Esc 清除多选", multiIds.size === 0);
  document._listeners.keydown.forEach(fn => fn({ key: "m", shiftKey: false, target: document.body, preventDefault() {} }));
  assert("M 键关闭多选", multiMode === false);

  // 26. 关闭添加模式后：预览函数守卫跳过，且切换触发重绘清掉残留
  addMode = false;
  selectedId = "p4";
  hoverPos = { sx: 300, sy: 200 };
  drawLinkPreview();
  drawSnapPreview();
  assert("关闭添加模式后预览绘制直接跳过", true);
  const origRedraw26 = redraw;
  let rc26 = 0;
  redraw = () => { rc26++; origRedraw26(); };
  toggleAddMode(); // 开
  toggleAddMode(); // 关
  redraw = origRedraw26;
  assert("切换添加模式触发重绘(清残留)", rc26 >= 2);

  // 27. 关闭多选模式后清空选中
  toggleMultiMode(); // 开
  multiIds.add("p4");
  toggleMultiMode(); // 关 -> 应清空
  assert("关闭多选模式后清空选中", multiIds.size === 0 && multiMode === false);

  // 28. 选择点位文件：浏览器无 File System Access API 时安全兜底（普通文件选择）
  fileHandle = null;
  await openWaypointFile();
  assert("无 API 时选择文件安全兜底", fileHandle === null);

  // 29. 选择点位文件：绑定句柄 + 实时写回 + 失败自动解除绑定
  class MockWritable {
    constructor() { this.text = ""; }
    async write(s) { this.text += s; }
    async close() {}
  }
  class MockHandle {
    constructor(name, text) { this.name = name; this._text = text; this._w = null; }
    async getFile() { return new File([this._text], this.name); }
    async createWritable() { this._w = new MockWritable(); return this._w; }
  }
  window.showOpenFilePicker = async () => [new MockHandle("nether.json", JSON.stringify({
    world: "world_nether",
    waypoints: [{ id: "c1", x: 1, y: 2, z: 3, links: [] }]
  }))];
  await openWaypointFile();
  assert("选择文件后绑定句柄", fileHandle && fileHandle.name === "nether.json");
  assert("选择文件内容已载入", state.waypoints.length === 1 && state.waypoints[0].id === "c1");
  state.waypoints[0].x = 999;
  await saveToFile();
  assert("实时写回文件内容", fileHandle._w && fileHandle._w.text.includes('"x": 999'));
  fileHandle.createWritable = async () => { throw new Error("权限失效"); };
  await saveToFile();
  assert("写回失败自动解除绑定", fileHandle === null);
  window.showOpenFilePicker = undefined;

  // 30. _extra 不再被重复包裹（历史嵌套 bug 回归）
  const n1 = normalizeWp({ id: "a", x: 1, y: 2, z: 3, links: ["b"], _extra: { note: "x" } });
  assert("normalize 不包裹已存在的 _extra", n1._extra.note === "x" && !("_extra" in n1._extra));
  const n2 = normalizeWp({ id: "b", x: 1, y: 2, z: 3, links: [], _extra: { _extra: { _extra: {} } } });
  assert("normalize 平铺历史嵌套 _extra", JSON.stringify(n2._extra) === "{}");
  const n3 = normalizeWp({ id: "c", x: 1, y: 2, z: 3, links: [], _extra: { note: "x", _extra: { color: "red" } } });
  assert("normalize 合并历史嵌套字段", n3._extra.note === "x" && n3._extra.color === "red" && !("_extra" in n3._extra));
  state.waypoints = [n1];
  const j30 = buildWaypointJson();
  assert("导出不再包含嵌套 _extra 键", !j30.includes('"_extra"'));

  // 31. 连线模式：地图上点两点互连 / 再点断开 / 链式 / 取消起点 / 互斥
  view.offsetX = 0; view.offsetY = 0; view.scale = 1;
  state.image = { naturalWidth: 2000, naturalHeight: 1000 };
  fields.minX.value = 0; fields.minZ.value = 0; fields.maxX.value = 2000; fields.maxZ.value = 1000;
  state.waypoints = [
    { id: "l1", x: 100, y: 64, z: 100, links: [], _extra: {} },
    { id: "l2", x: 300, y: 64, z: 100, links: [], _extra: {} },
    { id: "l3", x: 500, y: 64, z: 500, links: [], _extra: {} }
  ];
  selectedId = null; linkStartId = null; multiIds.clear();
  toggleLinkMode();
  assert("连线模式开启且与添加互斥", linkMode === true && addMode === false);
  const pdl = (x, y) => ({ clientX: x, clientY: y, pointerId: 1 });
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(100, 100)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(100, 100)));
  assert("点击设为连线起点", linkStartId === "l1");
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(300, 100)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(300, 100)));
  assert("连线后双向连通", getWp("l1").links.includes("l2") && getWp("l2").links.includes("l1"));
  assert("连线后链式起点为 l2", linkStartId === "l2");
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(300, 100)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(300, 100)));
  assert("再点同一点取消起点", linkStartId === null);
  assert("取消起点不影响已连通", getWp("l1").links.includes("l2"));
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(100, 100)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(100, 100)));
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(300, 100)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(300, 100)));
  assert("再连一次即断开", !getWp("l1").links.includes("l2") && !getWp("l2").links.includes("l1"));
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(900, 900)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(900, 900)));
  assert("链式前置：点空白取消起点", linkStartId === null);
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(100, 100)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(100, 100)));
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(300, 100)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(300, 100)));
  assert("链式第一步连通 l1-l2", getWp("l1").links.includes("l2") && linkStartId === "l2");
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(500, 500)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(500, 500)));
  assert("链式连续连线", getWp("l2").links.includes("l3") && linkStartId === "l3");
  canvas._listeners.pointerdown.forEach(fn => fn(pdl(900, 900)));
  canvas._listeners.pointerup.forEach(fn => fn(pdl(900, 900)));
  assert("点空白取消起点", linkStartId === null);
  toggleMultiMode();
  assert("开多选自动关连线", linkMode === false && multiMode === true);
  toggleMultiMode();
  assert("关多选仍保持连线关闭", linkMode === false);
  toggleLinkMode();
  assert("开启连线模式", linkMode === true);
  toggleLinkMode();
  assert("退出连线模式", linkMode === false);
  linkMode = true; linkStartId = "l1";
  hoverPos = { sx: 400, sy: 300 };
  drawLinkPreview();
  assert("连线模式预览绘制无异常", true);
  linkMode = false; linkStartId = null; hoverPos = null;

  // 32. 记忆上次点位文件：IndexedDB 持久化 + 自动恢复 + 权限失效重开按钮
  const idbStore = new Map();
  const makeIdbReq = (result) => {
    const req = { result, onsuccess: null, onerror: null, error: null };
    queueMicrotask(() => {
      if (req.onerror && req.error) req.onerror();
      else if (req.onsuccess) req.onsuccess();
    });
    return req;
  };
  globalThis.indexedDB = {
    open() {
      return makeIdbReq({
        createObjectStore() {},
        transaction() {
          return {
            objectStore() {
              return {
                put(val, key) { idbStore.set(key, val); return makeIdbReq(undefined); },
                get(key) { return makeIdbReq(idbStore.get(key)); }
              };
            }
          };
        },
        close() {}
      });
    }
  };
  class MockHandle2 {
    constructor(name, text, perm) { this.name = name; this._text = text; this._perm = perm || "granted"; this._w = null; }
    async getFile() { return new File([this._text], this.name); }
    async createWritable() { this._w = new MockWritable(); return this._w; }
    async queryPermission() { return this._perm; }
    async requestPermission() { this._perm = "granted"; return "granted"; }
    async getParent() {
      return {
        name: "dir-" + this.name,
        _perm: "granted",
        async queryPermission() { return this._perm; },
        async requestPermission() { this._perm = "granted"; return "granted"; }
      };
    }
  }
  window.showOpenFilePicker = async () => [new MockHandle2("last.json", JSON.stringify({
    world: "world_last",
    waypoints: [{ id: "z1", x: 5, y: 6, z: 7, links: [] }]
  }))];
  await openWaypointFile();
  assert("选择文件后句柄写入 IndexedDB", idbStore.has("waypointFile"));
  assert("选择文件后同时记住所在目录", idbStore.has("waypointDir") && idbStore.get("waypointDir").name === "dir-last.json");
  fileHandle = null;
  await restoreLastFile();
  assert("权限有效时自动恢复并绑定", fileHandle && fileHandle.name === "last.json");
  assert("自动恢复载入点位数据", state.waypoints.length === 1 && state.waypoints[0].id === "z1");
  fileHandle = null;
  idbStore.set("waypointFile", new MockHandle2("last.json", '{"world":"w","waypoints":[]}', "denied"));
  await restoreLastFile();
  assert("权限失效时保留待恢复句柄", lastFileHandle !== null && lastFileHandle.name === "last.json");
  await Promise.all($("btn-restore-file")._listeners.click.map(fn => fn()));
  assert("点击重开按钮后重新绑定", fileHandle && fileHandle.name === "last.json");
  assert("重开后清除待恢复句柄", lastFileHandle === null);
  // 再次点选择按钮：起始目录应使用记住的上次目录
  let pickedStartIn = null;
  window.showOpenFilePicker = async (opts) => {
    pickedStartIn = opts && opts.startIn;
    return [new MockHandle2("next.json", '{"world":"w","waypoints":[]}')];
  };
  await openWaypointFile();
  assert("选择按钮使用上次目录作为起始目录", pickedStartIn && pickedStartIn.name === "dir-last.json");
  // 起始目录失效时自动去掉 startIn 重试
  pickedStartIn = null;
  let throwOnce = true;
  window.showOpenFilePicker = async (opts) => {
    if (throwOnce && opts && opts.startIn) {
      throwOnce = false;
      const err = new Error("startIn invalid");
      err.name = "InvalidStateError";
      throw err;
    }
    pickedStartIn = opts && opts.startIn;
    return [new MockHandle2("ok.json", '{"world":"w","waypoints":[]}')];
  };
  await openWaypointFile();
  assert("起始目录失效时去掉 startIn 重试成功", pickedStartIn === undefined && fileHandle && fileHandle.name === "ok.json");
  // 目录权限为 prompt（未授权）时：点击手势内请求授权后仍传 startIn
  let reqCalled = false;
  const promptDir = {
    name: "dir-prompt",
    _perm: "prompt",
    async queryPermission() { return this._perm; },
    async requestPermission() { reqCalled = true; this._perm = "granted"; return "granted"; }
  };
  idbStore.set("waypointDir", promptDir);
  pickedStartIn = null;
  window.showOpenFilePicker = async (opts) => {
    pickedStartIn = opts && opts.startIn;
    return [new MockHandle2("p.json", '{"world":"w","waypoints":[]}')];
  };
  await openWaypointFile();
  assert("目录权限 prompt 时请求授权后仍传 startIn", reqCalled === true && pickedStartIn === promptDir);
  // 目录权限 denied（用户拒绝过）时不传 startIn，避免反复打扰
  idbStore.set("waypointDir", {
    name: "dir-denied",
    async queryPermission() { return "denied"; },
    async requestPermission() { return "denied"; }
  });
  pickedStartIn = "sentinel";
  window.showOpenFilePicker = async (opts) => {
    pickedStartIn = opts && opts.startIn;
    return [new MockHandle2("d.json", '{"world":"w","waypoints":[]}')];
  };
  await openWaypointFile();
  assert("目录权限 denied 时不传 startIn", pickedStartIn === undefined);
  window.showOpenFilePicker = undefined;
  globalThis.indexedDB = undefined;

  // 33. 手动「记住目录」按钮 + getParent 失败红字报错
  globalThis.indexedDB = {
    open() {
      return makeIdbReq({
        createObjectStore() {},
        transaction() {
          return {
            objectStore() {
              return {
                put(val, key) { idbStore.set(key, val); return makeIdbReq(undefined); },
                get(key) { return makeIdbReq(idbStore.get(key)); }
              };
            }
          };
        },
        close() {}
      });
    }
  };
  window.showDirectoryPicker = async () => ({
    name: "waypoints-manual",
    _perm: "prompt",
    async queryPermission() { return this._perm; },
    async requestPermission() { this._perm = "granted"; return "granted"; }
  });
  await Promise.all($("btn-set-dir")._listeners.click.map(fn => fn()));
  assert("手动记住目录按钮生效", idbStore.get("waypointDir") && idbStore.get("waypointDir").name === "waypoints-manual");
  assert("记住目录写入 localStorage", ls.get("mmap-last-dir") === "waypoints-manual");
  const noParent = new MockHandle2("np.json", '{"world":"w","waypoints":[]}');
  noParent.getParent = undefined;
  await persistLastFile(noParent);
  assert("getParent 不可用时静默跳过不报错", !$("dir-name-show").textContent.includes("getParent"));
  assert("getParent 不可用时不覆盖已有目录记忆", $("dir-name-show").textContent.includes("waypoints-manual"));
  idbStore.delete("waypointDir");
  ls.delete("mmap-last-dir");
  await persistLastFile(noParent);
  assert("完全无目录记忆时显示浅色提示", $("dir-name-show").textContent.includes("手动指定"));
  window.showDirectoryPicker = undefined;
  globalThis.indexedDB = undefined;

  const failed = results.filter(r => !r[1]).length;
  console.log("TOTAL: " + results.length + ", FAILED: " + failed);
  results.forEach(r => { if (!r[1]) console.error("  - " + r[0]); });
  globalThis.__TESTS_PASSED = failed === 0;
})().catch(err => { console.error("TEST ERROR:", err); globalThis.__TESTS_PASSED = false; });
`;

// 视口 1:1 映射：让 addWaypointAt 的屏幕坐标即像素坐标
const pre = `
  view.offsetX = 0; view.offsetY = 0; view.scale = 1;
  Object.defineProperty(wrap, "clientWidth", { value: 2000, configurable: true });
  Object.defineProperty(wrap, "clientHeight", { value: 1000, configurable: true });
`;

try {
  eval(js + pre + tests);
} catch (err) {
  console.error("SCRIPT ERROR:", err);
  process.exit(1);
}
await globalThis.__TEST_RUN;
process.exit(globalThis.__TESTS_PASSED ? 0 : 1);
