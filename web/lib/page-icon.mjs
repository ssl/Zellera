import { decodePng, encodePng } from './asset-bundle.mjs';
import { catalogIconPath, escapeRegExp, fromProfile } from './client-profile.mjs';
import { furniFilePath } from './furni-files.mjs';

export const PAGE_ICON_BASE = 1_000_000;
const PAGE_ICON_SIZE = 18;

const pageIconPattern = fromProfile(() => (catalogIconPath() ? new RegExp(`^${escapeRegExp(catalogIconPath())}/icon_(\\d{7,9})\\.png$`) : null));

export const pageIconFurniId = (rel) => {
  const m = pageIconPattern()?.exec(rel);
  const n = m ? Number(m[1]) : 0;
  return n > PAGE_ICON_BASE ? n - PAGE_ICON_BASE : null;
};

export const furniIconPath = (f) => furniFilePath(f.revision, String(f.classname).replace('*', '_'), 'icon');

export const furnidataIndex = (text) => {
  const fd = JSON.parse(text);
  const all = [...(fd.roomitemtypes?.furnitype ?? []), ...(fd.wallitemtypes?.furnitype ?? [])];
  return new Map(all.filter((f) => f.classname).map((f) => [Number(f.id), f]));
};

export function fitIcon(png, size = PAGE_ICON_SIZE) {
  const img = decodePng(png);
  if (!img) return null;
  let x0 = img.width, y0 = img.height, x1 = -1, y1 = -1;
  for (let y = 0; y < img.height; y++) {
    for (let x = 0; x < img.width; x++) {
      if (img.rgba[(y * img.width + x) * 4 + 3]) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
    }
  }
  if (x1 < 0) return null;
  const w = x1 - x0 + 1, h = y1 - y0 + 1, scale = Math.min(1, size / Math.max(w, h));
  const tw = Math.max(1, Math.round(w * scale)), th = Math.max(1, Math.round(h * scale));
  const out = Buffer.alloc(size * size * 4);
  const ox = (size - tw) >> 1, oy = (size - th) >> 1;
  for (let ty = 0; ty < th; ty++) {
    for (let tx = 0; tx < tw; tx++) {
      const sx0 = x0 + Math.floor(tx / scale), sx1 = x0 + Math.max(Math.floor(tx / scale) + 1, Math.floor((tx + 1) / scale));
      const sy0 = y0 + Math.floor(ty / scale), sy1 = y0 + Math.max(Math.floor(ty / scale) + 1, Math.floor((ty + 1) / scale));
      let r = 0, g = 0, b = 0, a = 0, n = 0;
      for (let sy = sy0; sy < Math.min(sy1, y1 + 1); sy++) {
        for (let sx = sx0; sx < Math.min(sx1, x1 + 1); sx++) {
          const s = (sy * img.width + sx) * 4, al = img.rgba[s + 3];
          r += img.rgba[s] * al; g += img.rgba[s + 1] * al; b += img.rgba[s + 2] * al; a += al; n++;
        }
      }
      if (!a) continue;
      const d = ((oy + ty) * size + ox + tx) * 4;
      out[d] = Math.round(r / a); out[d + 1] = Math.round(g / a); out[d + 2] = Math.round(b / a); out[d + 3] = Math.round(a / n);
    }
  }
  return encodePng(size, size, out);
}
