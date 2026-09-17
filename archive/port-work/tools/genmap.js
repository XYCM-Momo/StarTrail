// Generates a tiny v2 mapping file: official(obf) -> intermediary -> named(mojmap)
// by joining Fabric intermediary mappings with Mojang official (client.txt) mappings.
const fs = require('fs');
const path = require('path');

// 2026-09-17 路径已更新到整理后的位置（原为 插件/_port_work）
const W = 'E:/Minecraft/服务器/插件/MMmap/archive/port-work';
const TINY = path.join(W, 'mappings/intermediary.tiny');
const MOJ = path.join(W, 'mappings/client-1.20.1-mojmap.txt');
const OUT = path.join(W, 'mappings/intermediary-mojmap.tiny');

const PRIM = { void: 'V', boolean: 'Z', byte: 'B', char: 'C', short: 'S', int: 'I', long: 'J', float: 'F', double: 'D' };
const canon = n => n.replace(/\./g, '/');

// ---------- parse mojmap ----------
const mojClass = new Map();     // obf -> mojmap dotted name
const mojToObf = new Map();     // mojmap dotted name -> obf
const mojMembers = new Map();   // obfClass -> [{name, obf, desc}]
let curClass = null;
const lines = fs.readFileSync(MOJ, 'utf8').split(/\r?\n/);
for (const line of lines) {
  if (!line || line.startsWith('#')) continue;
  if (!line.startsWith(' ')) {
    const m = line.match(/^(\S+) -> (\S+):$/);
    if (!m) { curClass = null; continue; }
    curClass = canon(m[2]);
    mojClass.set(curClass, m[1]);
    if (!mojToObf.has(m[1])) mojToObf.set(m[1], curClass);
    if (!mojMembers.has(curClass)) mojMembers.set(curClass, []);
    continue;
  }
  if (!curClass) continue;
  const body = line.trim();
  const arrow = body.lastIndexOf(' -> ');
  if (arrow < 0) continue;
  const left = body.slice(0, arrow).trim();
  const obfName = body.slice(arrow + 4).trim();
  if (left.includes('(')) {
    const mm = left.match(/^(?:\d+:\d+:)?(.+?) ([^\s(]+)\((.*)\)$/);
    if (!mm) continue;
    const ret = mm[1], name = mm[2], args = mm[3].trim();
    const argList = args.length ? splitTypes(args) : [];
    const desc = '()';  // placeholder, computed below
    mojMembers.get(curClass).push({ kind: 'm', name, obf: obfName, rawRet: ret, rawArgs: argList });
  } else {
    const mm = left.match(/^(.+?) ([^\s]+)$/);
    if (!mm) continue;
    mojMembers.get(curClass).push({ kind: 'f', name: mm[2], obf: obfName, rawType: mm[1] });
  }
}

function splitTypes(s) {
  const out = [];
  let depth = 0, cur = '';
  for (const ch of s) {
    if (ch === '<') depth++;
    else if (ch === '>') depth--;
    if (ch === ',' && depth === 0) { out.push(cur.trim()); cur = ''; } else cur += ch;
  }
  if (cur.trim()) out.push(cur.trim());
  return out;
}

function typeToDesc(t) {
  t = t.trim();
  let dims = 0;
  while (t.endsWith('[]')) { dims++; t = t.slice(0, -2).trim(); }
  let base;
  if (PRIM[t] !== undefined) base = PRIM[t];
  else {
    if (mojToObf.has(t)) base = 'L' + mojToObf.get(t) + ';';
    else base = 'L' + t.replace(/\./g, '/') + ';';
  }
  return '['.repeat(dims) + base;
}

// obfClass|obfName|desc -> mojmap simple name
const memberKey = new Map();
const ambiguous = new Map();
for (const [obfClass, mems] of mojMembers) {
  for (const mem of mems) {
    let desc;
    if (mem.kind === 'm') {
      const args = mem.rawArgs.filter(a => a.length).map(typeToDesc).join('');
      // constructors: ret is 'void' in mojmap listing
      desc = '(' + args + ')' + typeToDesc(mem.rawRet);
    } else {
      desc = typeToDesc(mem.rawType);
    }
    const key = obfClass + '|' + mem.obf + '|' + desc;
    if (memberKey.has(key) && memberKey.get(key) !== mem.name) {
      ambiguous.set(key, true);
    } else {
      memberKey.set(key, mem.name);
    }
  }
}

// ---------- parse tiny (official -> intermediary) ----------
const tlines = fs.readFileSync(TINY, 'utf8').split(/\r?\n/);
const out = ['tiny\t2\t0\tofficial\tintermediary\tnamed'];
let stats = { classes: 0, mapped: 0, methods: 0, fields: 0, unresolved: 0, classMiss: 0 };
let tinyClass = null;
for (const line of tlines) {
  if (!line || line.startsWith('tiny')) continue;
  const parts = line.split('\t');
  if (parts[0] === 'c') {
    const obf = parts[1], inter = parts[2];
    tinyClass = obf;
    const named = mojClass.get(obf);
    stats.classes++;
    if (!named) { stats.classMiss++; }
    out.push(['c', obf, inter, named ? named.replace(/\./g, '/') : inter].join('\t'));
    continue;
  }
  if (parts[0] === '') {
    const kind = parts[1]; // after leading empty from leading tab
    const desc = parts[2], obfName = parts[3], interName = parts[4];
    const named = memberKey.get(tinyClass + '|' + obfName + '|' + desc);
    if (interName === 'method_22903') console.log('DEBUG tiny', JSON.stringify({ tinyClass, obfName, desc, named }));
    if (kind === 'm') stats.methods++; else stats.fields++;
    if (named && !ambiguous.has(tinyClass + '|' + obfName + '|' + desc)) {
      stats.mapped++;
      out.push(['', kind, desc, obfName, interName, named].join('\t'));
    } else {
      stats.unresolved++;
      out.push(['', kind, desc, obfName, interName, interName].join('\t'));
    }
    continue;
  }
  if (parts[0] === 'p') { continue; } // skip param lines
}

console.log('DEBUG eij|a|()V =>', memberKey.get('eij|a|()V'));
console.log('DEBUG memberKey size', memberKey.size);
fs.writeFileSync(OUT, out.join('\n') + '\n');
console.log(JSON.stringify(stats, null, 2));
console.log('ambiguous mojmap member names:', ambiguous.size);
