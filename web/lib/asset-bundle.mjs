import { deflateSync, inflateSync } from 'node:zlib';
import { bundleMagic, escapeRegExp } from './client-profile.mjs';

const BUNDLE_HEADER = 20;

const bundleIndex = (buf) => {
  const magic = bundleMagic();
  if (!magic || buf.length < BUNDLE_HEADER || buf.toString('latin1', 0, magic.length) !== magic) return null;
  const storedIndex = buf.readUInt32LE(8);
  return { index: JSON.parse(inflateSync(buf.subarray(BUNDLE_HEADER, BUNDLE_HEADER + storedIndex)).toString('utf8')), data: buf.subarray(BUNDLE_HEADER + storedIndex) };
};

export function bundleFiles(buf) {
  const bundle = bundleIndex(buf);
  if (!bundle) return null;
  const files = new Map();
  for (const e of bundle.index.entries ?? []) {
    const raw = bundle.data.subarray(e.offset, e.offset + e.storedLength);
    files.set(e.name, e.compression === 'deflate' ? inflateSync(raw) : raw);
  }
  return files;
}

const LETTERS = 'abcdefghijklmnopqrstuvwxyz';

export function bundleIcon(buf, colour = null) {
  const files = bundleFiles(buf);
  if (!files) return null;
  const jsonName = [...files.keys()].find((n) => n.endsWith('.json'));
  if (!jsonName) return null;
  const doc = JSON.parse(files.get(jsonName).toString('utf8'));
  const name = doc.name;
  const sheet = doc.spritesheet;
  const image = sheet?.meta?.image && files.get(sheet.meta.image);
  if (!name || !image) return null;
  const png = decodePng(image);
  if (!png) return null;
  const viz = (size) => (doc.visualizations ?? []).find((v) => v.size === size) ?? null;
  const layer = (key, v, i) => {
    const asset = doc.assets?.[key];
    if (!asset) return undefined;
    const frame = sheet.frames?.[`${name}_${asset.source ?? key}`]?.frame;
    if (!frame) return null;
    const tint = colour == null ? null : v?.colors?.[colour]?.layers?.[i]?.color;
    const alpha = v?.layers?.[i]?.alpha;
    return { x: -(asset.x ?? 0), y: -(asset.y ?? 0), flip: Boolean(asset.flipH), frame, tint: tint == null ? null : Number(tint), alpha: alpha == null ? 255 : Number(alpha) };
  };
  const layers = [];
  const icon = viz(1);
  for (let i = 0; i < LETTERS.length; i++) {
    const l = layer(`${name}_icon_${LETTERS[i]}`, icon, i);
    if (l === undefined) break;
    if (!l) return null;
    layers.push(l);
  }
  if (!layers.length) {
    const room = viz(32);
    const dirs = Object.keys(room?.directions ?? {}).map(Number);
    const dir = [2, 4, ...dirs, 0].find((d) => doc.assets?.[`${name}_32_a_${d}_0`]);
    if (dir === undefined) return null;
    for (let i = 0; i < Math.min(room.layerCount ?? 1, LETTERS.length); i++) {
      const l = layer(`${name}_32_${LETTERS[i]}_${dir}_0`, room, i);
      if (l && room.layers?.[i]?.ink !== 'ADD') layers.push(l);
    }
    if (!layers.length) return null;
  }
  const left = Math.min(...layers.map((l) => l.x)), top = Math.min(...layers.map((l) => l.y));
  const width = Math.max(...layers.map((l) => l.x + l.frame.w)) - left;
  const height = Math.max(...layers.map((l) => l.y + l.frame.h)) - top;
  const out = Buffer.alloc(width * height * 4);
  for (const l of layers) {
    const mul = l.tint == null ? [255, 255, 255] : [(l.tint >> 16) & 255, (l.tint >> 8) & 255, l.tint & 255];
    for (let y = 0; y < l.frame.h; y++) {
      for (let x = 0; x < l.frame.w; x++) {
        const sx = l.frame.x + (l.flip ? l.frame.w - 1 - x : x), s = ((l.frame.y + y) * png.width + sx) * 4;
        const d = ((l.y - top + y) * width + (l.x - left + x)) * 4;
        const a = (png.rgba[s + 3] / 255) * (l.alpha / 255);
        if (!a) continue;
        const da = out[d + 3] / 255, oa = a + da * (1 - a);
        for (let c = 0; c < 3; c++) out[d + c] = Math.round(((png.rgba[s + c] * mul[c]) / 255 * a + out[d + c] * da * (1 - a)) / oa);
        out[d + 3] = Math.round(oa * 255);
      }
    }
  }
  return encodePng(width, height, out);
}

export function bundleRename(buf, from, to) {
  const bundle = bundleIndex(buf);
  if (!bundle) return null;
  const word = new RegExp(`(?<![A-Za-z0-9])${escapeRegExp(from)}(?![A-Za-z0-9])`, 'g');
  const rename = (text) => text.replace(word, to);
  if (JSON.stringify(bundle.index).includes(to)) return null;
  const parts = [], entries = [];
  let offset = 0;
  for (const e of bundle.index.entries ?? []) {
    let raw = bundle.data.subarray(e.offset, e.offset + e.storedLength);
    let original = e.originalLength;
    if (/\.(json|xml|txt)$/i.test(e.name)) {
      const text = (e.compression === 'deflate' ? inflateSync(raw) : raw).toString('utf8');
      if (text.includes(to)) return null;
      const plain = Buffer.from(rename(text), 'utf8');
      original = plain.length;
      raw = e.compression === 'deflate' ? deflateSync(plain) : plain;
    }
    entries.push({ ...e, name: rename(e.name), offset, storedLength: raw.length, originalLength: original });
    parts.push(raw);
    offset += raw.length;
  }
  const index = Buffer.from(JSON.stringify({ ...JSON.parse(rename(JSON.stringify({ ...bundle.index, entries: [] }))), entries }), 'utf8');
  const stored = deflateSync(index);
  const head = Buffer.alloc(BUNDLE_HEADER);
  buf.copy(head, 0, 0, 8);
  head.writeUInt32LE(stored.length, 8);
  head.writeUInt32LE(index.length, 12);
  head.writeUInt32LE(offset, 16);
  return Buffer.concat([head, stored, ...parts]);
}

export function decodePng(buf) {
  if (buf.readUInt32BE(0) !== 0x89504e47) return null;
  let p = 8, width = 0, height = 0, depth = 0, type = 0, interlace = 0, palette = null, trns = null;
  const idat = [];
  while (p < buf.length) {
    const len = buf.readUInt32BE(p), kind = buf.toString('latin1', p + 4, p + 8), body = buf.subarray(p + 8, p + 8 + len);
    if (kind === 'IHDR') { width = body.readUInt32BE(0); height = body.readUInt32BE(4); depth = body[8]; type = body[9]; interlace = body[12]; }
    else if (kind === 'PLTE') palette = body;
    else if (kind === 'tRNS') trns = body;
    else if (kind === 'IDAT') idat.push(body);
    else if (kind === 'IEND') break;
    p += 12 + len;
  }
  const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[type];
  if (interlace || !channels || (depth !== 8 && !(depth < 8 && (type === 3 || type === 0)))) return null;
  const raw = inflateSync(Buffer.concat(idat));
  const bpp = Math.max(1, (channels * depth) >> 3), stride = (width * channels * depth + 7) >> 3;
  const rows = Buffer.alloc(stride * height);
  for (let y = 0, q = 0; y < height; y++) {
    const filter = raw[q++], row = rows.subarray(y * stride, (y + 1) * stride), prev = y ? rows.subarray((y - 1) * stride, y * stride) : null;
    for (let x = 0; x < stride; x++, q++) {
      const a = x >= bpp ? row[x - bpp] : 0, b = prev ? prev[x] : 0, c = prev && x >= bpp ? prev[x - bpp] : 0;
      let v = raw[q];
      if (filter === 1) v += a;
      else if (filter === 2) v += b;
      else if (filter === 3) v += (a + b) >> 1;
      else if (filter === 4) { const pa = Math.abs(b - c), pb = Math.abs(a - c), pc = Math.abs(a + b - 2 * c); v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
      row[x] = v & 255;
    }
  }
  const rgba = Buffer.alloc(width * height * 4);
  for (let y = 0; y < height; y++) {
    const row = rows.subarray(y * stride, (y + 1) * stride);
    for (let x = 0; x < width; x++) {
      const o = (y * width + x) * 4;
      const sample = (i) => (depth === 8 ? row[x * channels + i] : (row[(x * depth) >> 3] >> (8 - depth - ((x * depth) & 7))) & ((1 << depth) - 1));
      if (type === 6) { for (let c = 0; c < 4; c++) rgba[o + c] = row[x * 4 + c]; }
      else if (type === 2) { for (let c = 0; c < 3; c++) rgba[o + c] = row[x * 3 + c]; rgba[o + 3] = 255; }
      else if (type === 3) {
        const i = sample(0);
        rgba[o] = palette[i * 3]; rgba[o + 1] = palette[i * 3 + 1]; rgba[o + 2] = palette[i * 3 + 2];
        rgba[o + 3] = trns && i < trns.length ? trns[i] : 255;
      } else if (type === 0) { const g = depth === 8 ? row[x] : Math.round((sample(0) * 255) / ((1 << depth) - 1)); rgba[o] = rgba[o + 1] = rgba[o + 2] = g; rgba[o + 3] = 255; }
      else { rgba[o] = rgba[o + 1] = rgba[o + 2] = row[x * 2]; rgba[o + 3] = row[x * 2 + 1]; }
    }
  }
  return { width, height, rgba };
}

const CRC = new Int32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c; });
const crc32 = (b) => { let c = -1; for (const x of b) c = CRC[(c ^ x) & 255] ^ (c >>> 8); return (c ^ -1) >>> 0; };
const chunk = (kind, body) => {
  const head = Buffer.alloc(8); head.writeUInt32BE(body.length); head.write(kind, 4, 'latin1');
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(Buffer.concat([head.subarray(4), body])));
  return Buffer.concat([head, body, crc]);
};

export function encodePng(width, height, rgba) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width); ihdr.writeUInt32BE(height, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y++) rgba.copy(raw, y * (width * 4 + 1) + 1, y * width * 4, (y + 1) * width * 4);
  return Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]), chunk('IHDR', ihdr), chunk('IDAT', deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
