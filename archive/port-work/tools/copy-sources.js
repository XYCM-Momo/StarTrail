// Copy decompiled (mojmap) sources into the Forge project, skipping files we rewrite by hand.
const fs = require('fs');
const path = require('path');
// 2026-09-17 路径已更新到整理后的位置（原为 插件/_port_work → E:/MMmapForge）
const SRC = 'E:/Minecraft/服务器/插件/MMmap/archive/port-work/src-mojmap';
const DST = 'E:/Minecraft/服务器/插件/MMmap/mod-forge/src/main/java';
const SKIP = new Set([
  'com/mmap/client/MMmapMod.java',
  'com/mmap/client/MMmapClient.java',
  'com/mmap/client/network/NetworkHandler.java',
]);
let copied = 0, skipped = 0;
function walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) { walk(p); continue; }
    const rel = path.relative(SRC, p).split(path.sep).join('/');
    if (SKIP.has(rel)) { skipped++; console.log('skip  ', rel); continue; }
    const out = path.join(DST, rel);
    fs.mkdirSync(path.dirname(out), { recursive: true });
    fs.copyFileSync(p, out);
    copied++;
  }
}
walk(SRC);
console.log({ copied, skipped });
