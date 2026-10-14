#!/usr/bin/env node
/**
 * 星轨 StarTrail · 会话记录导出工具
 * ---------------------------------------------------------------------------
 * 把 DSH 的会话记录（session.v4.jsonl.zstd）导出成可读的 Markdown 对话记录。
 *
 * 为什么不能直接用普通解压工具：
 *   DSH 是「每条记录追加一个 zstd 帧」写进同一个文件的。常见的解压库
 *   （Node 的 zstdDecompressSync、zstd -d 等）只解第一个帧，会得到一两百字节
 *   的会话头就结束。本脚本按 zstd 帧结构自己逐帧走，把所有帧都解出来。
 *
 * 用法：
 *   node tools/export-session.mjs <会话文件.jsonl.zstd> <输出.md>
 *   node tools/export-session.mjs --list          # 列出本机所有会话记录
 *
 * 会话记录位置（Windows）：
 *   %USERPROFILE%\.dsh\sessions\<工作区路径转义名>\<会话ID>\session.v4.jsonl.zstd
 *
 * 导出策略：
 *   · 用户消息、助手回复 —— 原文照录（含其中的 Markdown 围栏，保持原样）
 *   · 工具调用与结果 —— 收进 <details> 折叠块，用「4 空格缩进代码块」而非
 *     反引号围栏，这样工具输出里自带的 ``` 不会和正文围栏互相干扰
 *   · 单条工具入参/结果超长时截断，注明「完整内容见原始记录」
 *   · 内部推理（reasoning）不导出，只保留面向用户的回复
 */

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import zlib from 'node:zlib';

const MAX_RESULT = 1200;   // 单条工具结果保留字符数
const MAX_ARGS = 600;      // 单条工具入参保留字符数
const MAGIC = 0xfd2fb528;  // zstd 魔数（小端字节 28 B5 2F FD）

// ---------------------------------------------------------------- 帧遍历解压

/** 计算从 off 开始的一个 zstd 帧占用的压缩字节数 */
function frameLength(b, off) {
  if (b.readUInt32LE(off) !== MAGIC) throw new Error(`第 ${off} 字节不是 zstd 帧起始`);
  let p = off + 4;
  const fhd = b[p++];
  const fcsFlag = (fhd >> 6) & 3;
  const singleSegment = (fhd >> 5) & 1;
  const checksum = (fhd >> 2) & 1;
  const dictIdFlag = fhd & 3;
  if (!singleSegment) p += 1;                  // Window_Descriptor
  p += [0, 1, 2, 4][dictIdFlag];               // Dictionary_ID
  p += (fcsFlag === 0) ? (singleSegment ? 1 : 0) : [0, 2, 4, 8][fcsFlag]; // Frame_Content_Size
  for (;;) {
    const h = b[p] | (b[p + 1] << 8) | (b[p + 2] << 16);   // Block_Header
    p += 3;
    const last = h & 1, type = (h >> 1) & 3, size = h >> 3;
    if (type === 0 || type === 2) p += size;   // Raw / Compressed
    else if (type === 1) p += 1;               // RLE
    else throw new Error('保留的块类型');
    if (last) break;
  }
  if (checksum) p += 4;
  return p - off;
}

/** 解出文件里所有拼接帧的内容 */
function decompressAllFrames(buf) {
  const parts = [];
  let off = 0, frames = 0, failed = 0;
  while (off + 4 <= buf.length) {
    if (buf.readUInt32LE(off) !== MAGIC) break;
    let len;
    try { len = frameLength(buf, off); } catch { failed++; break; }
    try { parts.push(zlib.zstdDecompressSync(buf.subarray(off, off + len))); frames++; }
    catch { failed++; }
    off += len;
  }
  return { text: Buffer.concat(parts).toString('utf8'), frames, failed, consumed: off, total: buf.length };
}

// ------------------------------------------------------------------ 工具函数

const pad = n => String(n).padStart(2, '0');
const day = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const full = d => `${day(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;

const textOf = (content) => (content || [])
  .filter(c => c.type === 'text' && typeof c.text === 'string')
  .map(c => c.text).join('\n\n').trim();

function clip(s, n) {
  let str = typeof s === 'string' ? s : JSON.stringify(s ?? '');
  str = String(str).replace(/\r\n/g, '\n').trim();
  return str.length > n ? str.slice(0, n) + '\n\n…（已截断，完整内容见原始记录）' : str;
}

/** 转成 4 空格缩进代码块：对内容里的 ``` 与 HTML 标签完全免疫 */
const indent = (s) => s.split('\n').map(l => (l.trim() === '' ? '' : '    ' + l)).join('\n');

// ------------------------------------------------------------------ 主流程

function listSessions() {
  const root = path.join(os.homedir(), '.dsh', 'sessions');
  if (!fs.existsSync(root)) { console.log('未找到会话目录:', root); return; }
  for (const ws of fs.readdirSync(root)) {
    const wsDir = path.join(root, ws);
    if (!fs.statSync(wsDir).isDirectory()) continue;
    console.log(`\n[工作区] ${ws}`);
    for (const sid of fs.readdirSync(wsDir)) {
      const dir = path.join(wsDir, sid);
      if (!fs.statSync(dir).isDirectory()) continue;
      for (const f of fs.readdirSync(dir)) {
        const st = fs.statSync(path.join(dir, f));
        console.log(`   ${st.mtime.toISOString().slice(0, 16).replace('T', ' ')}  ` +
          `${String(st.size).padStart(10)} B  ${path.join(dir, f)}`);
      }
    }
  }
}

function main() {
  const [src, out] = process.argv.slice(2);
  if (!src || src === '--list' || src === '-h' || src === '--help') {
    console.log('用法: node tools/export-session.mjs <会话文件.jsonl.zstd> <输出.md>');
    console.log('      node tools/export-session.mjs --list    # 列出本机会话记录');
    if (src === '--list') listSessions();
    process.exit(src ? 0 : 1);
  }
  if (!out) { console.error('缺少输出路径'); process.exit(1); }

  const buf = fs.readFileSync(src);
  const { text, frames, failed, consumed, total } = decompressAllFrames(buf);
  const recs = text.split('\n').filter(l => l.trim())
    .map(l => { try { return JSON.parse(l); } catch { return null; } }).filter(Boolean);
  if (!recs.length) { console.error('未解析出任何记录'); process.exit(1); }

  const sess = recs.find(r => r.type === 'session') || {};
  const times = recs.map(r => r.time || r.time0).filter(Boolean);
  const start = new Date(sess.createdAt || Math.min(...times));
  const end = new Date(Math.max(...times));

  const L = [];
  let toolBuf = [], nUser = 0, nAsst = 0, nTool = 0, nSys = 0, nCompact = 0;

  function flushTools() {
    if (!toolBuf.length) return;
    L.push(`<details><summary>🔧 工具调用 · ${toolBuf.length} 次（点击展开）</summary>`, '');
    toolBuf.forEach((t, i) => {
      L.push(`**${i + 1}. \`${t.name}\`**${t.err ? ' ⚠️ 报错' : ''}`, '');
      L.push(indent(clip(t.args, MAX_ARGS)), '');
      if (t.out) L.push('<sub>结果：</sub>', '', indent(clip(t.out, MAX_RESULT)), '');
    });
    L.push('</details>', '');
    toolBuf = [];
  }

  L.push(
    '# 星轨 StarTrail · 开发对话记录',
    '',
    `> **时间跨度** ${full(start)} → ${full(end)}（${day(start)} ~ ${day(end)}）`,
    `> **会话 ID** \`${sess.id || '?'}\` · **工作目录** \`${sess.cwd || '?'}\``,
    `> **来源** \`${path.basename(src)}\`（${frames.toLocaleString('en-US')} 个 zstd 帧 → ${recs.length.toLocaleString('en-US')} 条记录）`,
    '> 本文保留对话原文；工具调用与结果折叠收纳（缩进代码块），超长部分截断；运行环境等系统注入消息也一并折叠。',
    '> 由 `tools/export-session.mjs` 导出；内部推理（reasoning）不在本文中，逐字完整记录见同目录 `raw/`。',
  );

  for (const r of recs) {
    if (r.type === 'user/message') {
      const kind = r.data?.source?.kind;
      const t = textOf(r.data?.content);
      if (!t) continue;
      flushTools();
      if (kind === 'user') { nUser++; L.push('', '## 👤 用户', '', t, '', '---'); }
      else { nSys++; L.push('', `<details><summary>⚙️ 系统注入消息（${kind || 'unknown'}）</summary>`, '', t, '', '</details>'); }
    } else if (r.type === 'assistant/message') {
      const t = textOf(r.data?.message?.content);
      if (!t) continue;
      flushTools();
      nAsst++;
      L.push('', '### 🤖 助手', '', t);
    } else if (r.type === 'tool/call') {
      nTool++;
      toolBuf.push({ name: r.data?.name || '?', args: r.data?.arguments ?? '', out: '', err: false, callId: r.data?.callId });
    } else if (r.type === 'tool/result') {
      const c = r.data?.message?.content?.[0];
      const outText = (c?.content || []).filter(x => x.type === 'text').map(x => x.text).join('\n');
      const target = toolBuf.find(t => t.callId === (c?.toolCallId || '')) || toolBuf[toolBuf.length - 1];
      if (target) { target.out = outText; target.err = !!c?.isError; }
    } else if (r.type === 'compaction/start' || r.type === 'compaction/summary') {
      flushTools(); nCompact++;
      L.push('', '> ⚠️ **上下文压缩（compaction）**：更早的对话被摘要替代，逐字原文见 `raw/` 下的会话文件。');
    }
  }
  flushTools();

  L.push('', '---', '',
    `**统计**：用户消息 **${nUser}** 条 · 助手回复 **${nAsst}** 条 · 工具调用 **${nTool}** 次 · ` +
    `系统注入消息 **${nSys}** 条（已折叠）· 上下文压缩 **${nCompact}** 次。`,
    '', '---', '');

  fs.mkdirSync(path.dirname(path.resolve(out)), { recursive: true });
  fs.writeFileSync(out, L.join('\n'), 'utf8');

  console.log(`解压: ${frames.toLocaleString('en-US')} 帧 / ${failed} 失败 · ${consumed}/${total} 字节`);
  console.log(`记录: ${recs.length.toLocaleString('en-US')} 条`);
  console.log(`跨度: ${full(start)} → ${full(end)}`);
  console.log(`内容: 用户 ${nUser} · 助手 ${nAsst} · 工具 ${nTool} · 系统 ${nSys} · 压缩 ${nCompact}`);
  console.log(`已写出: ${out}（${(fs.statSync(out).size / 1024).toFixed(0)} KB）`);
}

main();
