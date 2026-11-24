import { bundleFiles, decodePng } from './asset-bundle.mjs';
import { bundleExtension } from './client-profile.mjs';

const ENTITIES = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'" };
const unescape = (s) => s.replace(/&(amp|lt|gt|quot|apos|#\d+|#x[0-9a-f]+);/gi, (m, e) => (e[0] === '#'
  ? String.fromCodePoint(e[1] === 'x' || e[1] === 'X' ? parseInt(e.slice(2), 16) : parseInt(e.slice(1), 10))
  : ENTITIES[e.toLowerCase()]));

function parseXml(text) {
  const root = { name: '#document', attrs: {}, children: [], text: '' };
  const stack = [root];
  const tag = /<!--[\s\S]*?-->|<\?[\s\S]*?\?>|<!\[CDATA\[([\s\S]*?)\]\]>|<![^>]*>|<\/\s*([^\s>]+)\s*>|<([^\s/>!?]+)([^>]*?)(\/?)>|([^<]+)/g;
  const attr = /([^\s=]+)\s*=\s*(?:"([^"]*)"|'([^']*)')/g;
  for (let m; (m = tag.exec(text));) {
    const top = stack[stack.length - 1];
    if (m[1] !== undefined) top.text += m[1];
    else if (m[2] !== undefined) { if (stack.length > 1) stack.pop(); }
    else if (m[3] !== undefined) {
      const el = { name: m[3], attrs: {}, children: [], text: '' };
      for (let a; (a = attr.exec(m[4]));) el.attrs[a[1]] = unescape(a[2] ?? a[3]);
      top.children.push(el);
      if (!m[5]) stack.push(el);
    } else if (m[6] !== undefined) top.text += unescape(m[6]);
  }
  return root.children.find((c) => c.name) ?? root;
}
const kids = (el, name) => (el ? el.children.filter((c) => c.name === name) : []);
const kid = (el, name) => kids(el, name)[0] ?? null;
const str = (el, name, dflt = '') => el?.attrs[name] ?? dflt;
const int = (el, name) => { const v = parseInt(str(el, name), 10); return Number.isNaN(v) ? 0 : v; };
const num = (el, name) => { const v = parseFloat(str(el, name)); return Number.isNaN(v) ? 0 : v; };
const bool = (el, name) => { const v = str(el, name); return v === '1' || v === 'true'; };


const DIRECTION_ANGLE = [45, 90, 135, 180, 225, 270, 315, 0];
const MIRRORED = [false, false, false, false, true, true, true, false];

function parseGeometry(root) {
  const cam = kid(root, 'camera');
  const camera = { x: parseFloat(kid(cam, 'x')?.text ?? '0') || 0, y: parseFloat(kid(cam, 'y')?.text ?? '0') || 0, z: parseFloat(kid(cam, 'z')?.text ?? '10') || 10 };
  const canvases = new Map();
  for (const c of kids(root, 'canvas')) {
    const scale = str(c, 'scale');
    const m = new Map();
    for (const g of kids(c, 'geometry')) {
      const width = int(g, 'width'), height = int(g, 'height');
      m.set(str(g, 'id'), { width, height, dx: int(g, 'dx'), dy: int(g, 'dy'), regX: (width - (scale === 'h' ? 64 : 32)) / 2 });
    }
    canvases.set(scale, m);
  }
  const avatarSets = new Map();
  const walkSet = (el) => {
    const own = kids(el, 'bodypart').map((b) => str(b, 'id'));
    const all = [...own];
    for (const s of kids(el, 'avatarset')) all.push(...walkSet(s));
    avatarSets.set(str(el, 'id'), all);
    return all;
  };
  walkSet(kid(root, 'avatarset'));
  const types = new Map();
  const orderBefore = new Map(), orderAfter = new Map();
  for (const t of kids(root, 'type')) {
    const parts = new Map();
    for (const b of kids(t, 'bodypart')) {
      const id = str(b, 'id');
      parts.set(id, {
        id, x: num(b, 'x'), y: num(b, 'y'), z: num(b, 'z'), radius: num(b, 'radius'),
        items: kids(b, 'item').map((i) => ({ id: str(i, 'id'), x: num(i, 'x'), y: num(i, 'y'), z: num(i, 'z'), radius: num(i, 'radius') })),
      });
      if (str(b, 'order-before')) orderBefore.set(id, str(b, 'order-before'));
      if (str(b, 'order-after')) orderAfter.set(id, str(b, 'order-after'));
    }
    types.set(str(t, 'id'), parts);
  }
  return { camera, canvases, avatarSets, types, orderBefore, orderAfter };
}

const distance = (camera, p, angle) => {
  const a = (angle * Math.PI) / 180;
  const z = p.x * Math.sin(a) + p.z * Math.cos(a);
  return Math.min(Math.abs(camera.z - z - p.radius), Math.abs(camera.z - z + p.radius));
};
const sortByDistance = (camera, list, angle) => list
  .map((p) => [distance(camera, p, angle), p])
  .sort((a, b) => a[0] - b[0])
  .map(([, p]) => p);

function parsePartSets(root) {
  const parts = new Map();
  for (const p of kids(kid(root, 'partSet'), 'part')) parts.set(str(p, 'set-type'), { flipped: str(p, 'flipped-set-type'), appendToFigure: false });
  for (const t of ['ri', 'li']) if (parts.has(t)) parts.get(t).appendToFigure = true;
  const active = new Map();
  for (const a of kids(root, 'activePartSet')) active.set(str(a, 'id'), kids(a, 'activePart').map((p) => str(p, 'set-type')));
  return { parts, active };
}

function parseAnimations(root) {
  const actions = new Map();
  for (const a of kids(root, 'action')) {
    const parts = new Map();
    for (const p of kids(a, 'part')) {
      const frames = [];
      for (const f of kids(p, 'frame')) {
        const fr = { number: int(f, 'number'), apd: str(f, 'assetpartdefinition') };
        frames.push(fr);
        for (let r = int(f, 'repeats'); r > 1; r--) frames.push(fr);
      }
      parts.set(str(p, 'set-type'), frames);
    }
    const frameIds = [], offsets = new Map();
    for (const f of kids(kid(a, 'offsets'), 'frame')) {
      const id = int(f, 'id');
      const dirs = new Map();
      for (const d of kids(kid(f, 'directions'), 'direction')) {
        const bps = new Map();
        for (const b of kids(d, 'bodypart')) bps.set(str(b, 'id'), [int(b, 'dx'), int(b, 'dy')]);
        dirs.set(int(d, 'id'), bps);
      }
      offsets.set(id, dirs);
      frameIds.push(id);
      for (let r = int(f, 'repeats'); r > 1; r--) frameIds.push(id);
    }
    actions.set(str(a, 'id'), { parts, frameIds, offsets });
  }
  return actions;
}

function parseActions(root) {
  const byState = new Map();
  for (const a of kids(root, 'action')) {
    const state = str(a, 'state');
    if (!state) continue;
    const types = new Map();
    for (const t of kids(a, 'type')) types.set(str(t, 'id'), { prevents: str(t, 'prevents') ? str(t, 'prevents').split(',') : [], preventHeadTurn: str(t, 'preventheadturn') === '' ? null : str(t, 'preventheadturn') === 'true' });
    byState.set(state, {
      id: str(a, 'id'), state, precedence: int(a, 'precedence'), activePartSet: str(a, 'activepartset'),
      apd: str(a, 'assetpartdefinition'), geometryType: str(a, 'geometrytype'), isMain: bool(a, 'main'),
      isDefault: bool(a, 'isdefault'), isAnimation: bool(a, 'animation'),
      prevents: str(a, 'prevents') ? str(a, 'prevents').split(',') : [], preventHeadTurn: str(a, 'preventheadturn') === 'true', types,
    });
  }
  return { byState, std: [...byState.values()].find((a) => a.isDefault) };
}

function parseFiguredata(root, into = { palettes: new Map(), setTypes: new Map() }) {
  for (const p of kids(kid(root, 'colors'), 'palette')) {
    const id = str(p, 'id');
    if (!into.palettes.has(id)) into.palettes.set(id, new Map());
    const colors = into.palettes.get(id);
    for (const c of kids(p, 'color')) {
      const rgb = parseInt(c.text.trim() || '0', 16) >>> 0;
      const color = [(rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255];
      colors.set(int(c, 'id'), color);
      if (colors.firstIndex === undefined || int(c, 'index') < colors.firstIndex) { colors.firstIndex = int(c, 'index'); colors.first = color; }
    }
  }
  for (const st of kids(kid(root, 'sets'), 'settype')) {
    const type = str(st, 'type');
    if (!into.setTypes.has(type)) into.setTypes.set(type, { paletteId: str(st, 'paletteid'), sets: new Map() });
    const target = into.setTypes.get(type);
    for (const s of kids(st, 'set')) {
      const parts = [];
      for (const p of kids(s, 'part')) {
        const part = { id: int(p, 'id'), type: str(p, 'type'), index: int(p, 'index'), colorIndex: int(p, 'colorindex') };
        const at = parts.findIndex((q) => q.type === part.type && q.index < part.index);
        if (at === -1) parts.push(part); else parts.splice(at, 0, part);
      }
      target.sets.set(str(s, 'id'), { parts, hidden: kids(kid(s, 'hiddenlayers'), 'layer').map((l) => str(l, 'parttype')) });
    }
  }
  return into;
}

export function parseFigure(figure) {
  const parts = new Map();
  for (const piece of String(figure ?? '').split('.')) {
    const t = piece.split('-');
    if (t.length < 2) continue;
    const setId = parseInt(t[1], 10);
    parts.delete(t[0]);
    parts.set(t[0], { type: t[0], setId: Number.isNaN(setId) ? 0 : setId, colors: t.slice(2).map((c) => parseInt(c, 10)).filter((c) => !Number.isNaN(c)) });
  }
  return [...parts.values()];
}

const SHADES = { hh_human_body: { 215: 237, 205: 232, 204: 231, 153: 203 } };


export function createAvatarRenderer({ readRenderLib, readAsset, readFiguredata, version = () => '' }) {
  let data = null;
  let dataJob = null;
  let loadedVersion = null;
  const libs = new Map();

  const load = async () => {
    const lib = bundleFiles(readRenderLib());
    if (!lib) throw new Error('avatar render library unreadable');
    const geometryName = [...lib.keys()].find((k) => k.endsWith('AvatarGeometry'));
    if (!geometryName) throw new Error('avatar render library has no avatar geometry');
    const prefix = geometryName.slice(0, -'AvatarGeometry'.length);
    const xml = (name) => (lib.get(prefix + name) ? parseXml(lib.get(prefix + name).toString('utf8')) : null);
    const actionsName = `${prefix}AvatarActions.xml`;
    const [actionsXml, mapXml] = await Promise.all([readAsset(actionsName), readAsset('figuremap.xml')]);
    if (!actionsXml || !mapXml) throw new Error(`${actionsName} / figuremap.xml missing`);
    const figuremap = new Map();
    for (const l of kids(parseXml(mapXml.toString('utf8')), 'lib')) {
      for (const p of kids(l, 'part')) {
        const key = `${str(p, 'type')}:${str(p, 'id')}`;
        if (!figuremap.has(key)) figuremap.set(key, []);
        figuremap.get(key).push(str(l, 'id'));
      }
    }
    const figuredata = parseFiguredata(xml('AvatarFigure') ?? { children: [] });
    parseFiguredata(parseXml(readFiguredata()), figuredata);
    return {
      geometry: parseGeometry(xml('AvatarGeometry')),
      partSets: parsePartSets(xml('AvatarPartSets')),
      animations: parseAnimations(xml('AvatarAnimation')),
      actions: parseActions(parseXml(actionsXml.toString('utf8'))),
      figuredata, figuremap,
    };
  };
  const ready = () => {
    if (!dataJob) dataJob = load().then((d) => { data = d; return d; }, (e) => { dataJob = null; throw e; });
    return dataJob;
  };

  const loadLib = (name) => {
    if (!libs.has(name)) {
      libs.set(name, (async () => {
        const buf = await readAsset(`${name}${bundleExtension()}`);
        if (!buf) return null;
        const files = bundleFiles(buf);
        const jsonName = files && [...files.keys()].find((k) => k.endsWith('.json'));
        if (!jsonName) return null;
        const doc = JSON.parse(files.get(jsonName).toString('utf8'));
        const sheet = doc.spritesheet;
        const png = sheet?.meta?.image && files.get(sheet.meta.image) ? decodePng(files.get(sheet.meta.image)) : null;
        const assets = new Map();
        if (!png) return assets;
        const prefix = doc.name ?? name;
        for (const [assetName, a] of Object.entries(doc.assets ?? {})) {
          const f = sheet.frames?.[`${prefix}_${a.source ?? assetName}`];
          if (!f?.frame) continue;
          const trim = f.trimmed && f.sourceSize ? { x: f.spriteSourceSize?.x ?? 0, y: f.spriteSourceSize?.y ?? 0, w: f.sourceSize.w, h: f.sourceSize.h } : null;
          assets.set(assetName, { offsetX: a.x ?? 0, offsetY: a.y ?? 0, frame: f.frame, trim, sheet: png, flipH: Boolean(a.flipH), shades: SHADES[name] ?? null });
        }
        return assets;
      })().catch((e) => { libs.delete(name); throw e; }));
    }
    return libs.get(name);
  };

  const unpremultiplied = (c, a) => {
    const p = Math.ceil((c * a) / 255);
    if (Math.floor((p * 255) / a) !== c) return c;
    return Math.min(255, Math.ceil(((p + 1) * 255) / a) - 1);
  };

  const content = (asset) => {
    if (!asset.image) {
      const { frame, sheet, trim } = asset;
      const width = trim?.w ?? frame.w, height = trim?.h ?? frame.h, tx = trim?.x ?? 0, ty = trim?.y ?? 0;
      const rgba = Buffer.alloc(width * height * 4);
      for (let y = 0; y < frame.h; y++) {
        for (let x = 0; x < frame.w; x++) {
          const s = ((frame.y + y) * sheet.width + frame.x + x) * 4;
          const dx = asset.flipH ? width - 1 - (tx + x) : tx + x;
          const d = ((ty + y) * width + dx) * 4;
          sheet.rgba.copy(rgba, d, s, s + 4);
          const a = rgba[d + 3];
          if (a > 0 && a < 255) for (let c = 0; c < 3; c++) rgba[d + c] = unpremultiplied(rgba[d + c], a);
          else if (asset.shades && a === 255 && rgba[d] === rgba[d + 1] && rgba[d] === rgba[d + 2] && asset.shades[rgba[d]]) rgba.fill(asset.shades[rgba[d]], d, d + 3);
        }
      }
      asset.image = { width, height, rgba };
    }
    return asset.image;
  };

  const resolveActions = (list) => {
    const { byState } = data.actions;
    const prevents = [];
    for (const a of list) {
      const def = byState.get(a.type);
      if (def) prevents.push(...def.prevents, ...(a.param ? def.types.get(a.param)?.prevents ?? [] : []));
    }
    return list
      .filter((a) => !prevents.includes(a.type === 'fx' ? `fx.${a.param}` : a.type))
      .map((a) => ({ ...a, def: byState.get(a.type) }))
      .filter((a) => a.def)
      .sort((a, b) => b.def.precedence - a.def.precedence);
  };

  const bodypartOf = (geometryType, type) => {
    for (const bp of data.geometry.types.get(geometryType)?.values() ?? []) if (bp.items.some((i) => i.id === type)) return bp;
    return null;
  };

  const bodypartParts = (bodypartId, figure, action, geometryType, direction) => {
    const { partSets, animations, figuredata, geometry } = data;
    const active = partSets.active.get(action.def.activePartSet) ?? [];
    const anim = animations.get(action.def.id);
    const bp = geometry.types.get(geometryType)?.get(bodypartId);
    if (!bp) return [];
    const order = sortByDistance(geometry.camera, bp.items, direction).map((i) => i.id);
    const hidden = [];
    const found = [];
    for (const fp of figure) {
      const st = figuredata.setTypes.get(fp.type);
      if (!st) continue;
      const palette = figuredata.palettes.get(st.paletteId);
      if (!palette) continue;
      const set = st.sets.get(String(fp.setId));
      if (!set) continue;
      hidden.push(...set.hidden);
      for (const p of set.parts) {
        if (!order.includes(p.type)) continue;
        const frames = anim?.parts.get(p.type) ?? null;
        const flipped = partSets.parts.get(p.type)?.flipped || p.type;
        const color = p.colorIndex > 0 ? palette.get(fp.colors[p.colorIndex - 1]) ?? palette.first ?? null : null;
        found.push({ type: p.type, id: String(p.id), color, colorable: p.colorIndex > 0 && p.type !== 'ey', frames, flipped });
      }
    }
    const out = [];
    for (const type of order) {
      let inFigure = false;
      for (const c of found) {
        if (c.type !== type) continue;
        inFigure = true;
        if (!hidden.includes(type)) out.push(c);
      }
      if (inFigure || !active.includes(type)) continue;
      const owner = bodypartOf(geometryType, type);
      if (!owner || owner.id !== bodypartId) continue;
      const ps = partSets.parts.get(type);
      if (!ps?.appendToFigure) continue;
      out.push({ type, id: action.param || '1', color: null, colorable: false, frames: anim?.parts.get(type) ?? null, flipped: type });
    }
    return out;
  };

  const findAsset = (assets, scale, apd, type, id, dir, frame, fallbackApd) => {
    for (const [a, f] of [[apd, frame], [apd, 0], [fallbackApd, frame], [fallbackApd, 0]]) {
      const hit = assets.get(`${scale}_${a}_${type}_${id}_${dir}_${f}`);
      if (hit) return hit;
    }
    return null;
  };

  const renderBodypart = (assets, scale, canvas, parts, action, direction, frame, fallbackApd, mirror) => {
    const mirrored = MIRRORED[direction];
    let apd = action.def.apd;
    const layers = [];
    for (let i = parts.length - 1; i >= 0; i--) {
      const part = parts[i];
      if (direction === 7 && (part.type === 'fc' || part.type === 'ey')) continue;
      if (part.type === 'ri' && !part.id) continue;
      let type = part.type;
      let number = frame;
      if (part.frames?.length) {
        const f = part.frames[frame % part.frames.length];
        number = f.number;
        if (f.apd) apd = f.apd;
      }
      let dir = direction;
      let keep = false;
      if (mirrored) {
        if ((apd === 'wav' && ['lh', 'ls', 'lc', 'mcl'].includes(type)) || (apd === 'drk' && ['rh', 'rs', 'rc', 'mcr'].includes(type))
          || (apd === 'blw' && type === 'rh') || (apd === 'sig' && type === 'lh') || (apd === 'respect' && type === 'lh')
          || type === 'ri' || type === 'li' || type === 'cp') keep = true;
        else {
          dir = direction === 4 ? 2 : direction === 5 ? 1 : direction === 6 ? 0 : direction;
          if (part.flipped !== type) type = part.flipped;
        }
      }
      const asset = findAsset(assets, 'h', apd, type, part.id, dir, number, fallbackApd);
      if (!asset) continue;
      const image = content(asset);
      let ox = asset.offsetX, oy = asset.offsetY;
      if (keep) ox += mirror?.keep ?? (scale === 'h' ? 65 : 31);
      if (keep) ox = -ox + image.width;
      layers.push({ image, ox, oy, flip: keep, color: part.colorable ? part.color : null });
    }
    if (!layers.length) return null;
    let left = Infinity, top = Infinity, right = -Infinity, bottom = -Infinity;
    for (const l of layers) {
      left = Math.min(left, -l.ox); top = Math.min(top, -l.oy);
      right = Math.max(right, -l.ox + l.image.width); bottom = Math.max(bottom, -l.oy + l.image.height);
    }
    const width = right - left, height = bottom - top;
    const rgba = Buffer.alloc(width * height * 4);
    for (const l of layers) {
      let x = -left - l.ox, y = -top - l.oy;
      if (mirrored) x = width - (x + l.image.width);
      draw(rgba, width, height, l.image, x, y, mirrored !== l.flip, l.color);
    }
    const baseY = scale === 'h' ? canvas.height - 16 : canvas.height - 8;
    const f = scale === 'h' ? 1 : 2;
    let x = (mirrored ? -(left + width) : left) / f;
    const y = baseY + top / f;
    if (mirrored && apd !== 'lay') x += mirror?.shift ?? (scale === 'h' ? 67 : 31);
    const image = f === 1 ? { width, height, rgba } : halve({ width, height, rgba });
    return { image, x: Math.trunc(x), y: Math.trunc(y) };
  };

  const render = async ({ figure, direction = 2, headDirection = direction, headonly = false, gesture = '', actions = [], scale = 'h', mirror = null }) => {
    const v = version();
    if (v !== loadedVersion) {
      loadedVersion = v;
      data = null; dataJob = null; libs.clear();
    }
    if (!data) await ready();
    const parts = parseFigure(figure);
    const libNames = new Set();
    for (const fp of parts) {
      const set = data.figuredata.setTypes.get(fp.type)?.sets.get(String(fp.setId));
      for (const p of set?.parts ?? []) for (const n of data.figuremap.get(`${p.type}:${p.id}`) ?? []) libNames.add(n);
    }
    const assets = new Map();
    for (const lib of await Promise.all([...libNames].map(loadLib))) for (const [k, v] of lib ?? []) if (!assets.has(k)) assets.set(k, v);

    const list = [{ type: 'std', param: '' }, ...actions.map((a) => ({ type: a.type, param: a.param ?? '' }))];
    if (gesture) list.push({ type: gesture, param: '' });
    const active = resolveActions(list);
    const { std } = data.actions;
    let bodyDir = direction;
    const lying = active.some((a) => a.type === 'lay');
    if (lying) bodyDir = bodyDir === 0 ? 4 : 2;
    const headTurn = !active.some((a) => (a.param && a.def.types.get(a.param)?.preventHeadTurn != null ? a.def.types.get(a.param).preventHeadTurn : a.def.preventHeadTurn)
      && !(a.type === 'Sleep' && !lying));
    const headDir = headTurn && !lying ? (headDirection ?? bodyDir) : bodyDir;

    const defaultAction = { type: 'std', param: '', def: std };
    let main = defaultAction;
    const bpAction = new Map();
    for (const a of active) {
      if (!a.def.apd) continue;
      if (a.def.isMain) main = a;
      for (const t of data.partSets.active.get(a.def.activePartSet) ?? []) {
        const bp = bodypartOf(a.def.geometryType, t);
        if (bp) bpAction.set(bp.id, a);
      }
    }
    const geometryType = main.def.geometryType || 'vertical';
    const fallbackApd = geometryType === 'horizontal' ? 'lay' : 'std';
    const canvas = data.geometry.canvases.get(scale)?.get(geometryType);
    if (!canvas) return null;
    const setId = headonly ? 'head' : 'full';
    const bodyparts = (data.geometry.avatarSets.get(setId) ?? [])
      .map((id) => data.geometry.types.get(geometryType)?.get(id))
      .filter(Boolean);
    const sorted = sortByDistance(data.geometry.camera, bodyparts, DIRECTION_ANGLE[bodyDir]).map((b) => b.id);
    for (const [a, b] of data.geometry.orderBefore) {
      const i = sorted.indexOf(a);
      if (i === -1) continue;
      sorted.splice(i, 1);
      const j = sorted.indexOf(b);
      if (j !== -1) sorted.splice(j, 0, a);
    }
    for (const [a, b] of data.geometry.orderAfter) {
      const i = sorted.indexOf(a);
      if (i === -1) continue;
      sorted.splice(i, 1);
      const j = sorted.indexOf(b);
      if (j !== -1) sorted.splice(j + 1, 0, a);
    }
    const headSet = data.geometry.avatarSets.get('head') ?? [];
    const out = { width: canvas.width, height: canvas.height, rgba: Buffer.alloc(canvas.width * canvas.height * 4), geometryType, regX: canvas.regX };
    for (let i = sorted.length - 1; i >= 0; i--) {
      const id = sorted[i];
      const dir = headSet.includes(id) ? headDir : bodyDir;
      const action = bpAction.get(id) ?? defaultAction;
      const list2 = bodypartParts(id, parts, action, geometryType, dir);
      const img = renderBodypart(assets, scale, canvas, list2, action, dir, 0, fallbackApd, mirror);
      if (!img) continue;
      const anim = data.animations.get(action.def.id);
      let dx = 0, dy = 0;
      if (anim?.frameIds.length) {
        const off = anim.offsets.get(anim.frameIds[0])?.get(dir)?.get(id);
        if (off) [dx, dy] = off;
      }
      draw(out.rgba, out.width, out.height, img.image, img.x + dx + canvas.dx + canvas.regX, img.y + dy + canvas.dy, false, null);
    }
    return out;
  };

  return { render, version };
}

function halve(src) {
  const w = Math.round(src.width / 2), h = Math.round(src.height / 2), rgba = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const sx = Math.min(src.width - 1, x * 2), sy = Math.min(src.height - 1, y * 2);
      src.rgba.copy(rgba, (y * w + x) * 4, (sy * src.width + sx) * 4, (sy * src.width + sx) * 4 + 4);
    }
  }
  return { width: w, height: h, rgba };
}

const tint = (c, k) => Math.round(((c === 254 ? 255 : c) * k) / 255);

function draw(dst, dw, dh, src, x, y, flip, color) {
  x = Math.trunc(x); y = Math.trunc(y);
  for (let sy = 0; sy < src.height; sy++) {
    const ty = y + sy;
    if (ty < 0 || ty >= dh) continue;
    for (let sx = 0; sx < src.width; sx++) {
      const tx = x + (flip ? src.width - 1 - sx : sx);
      if (tx < 0 || tx >= dw) continue;
      const s = (sy * src.width + sx) * 4;
      const sa = src.rgba[s + 3];
      if (!sa) continue;
      let r = src.rgba[s], g = src.rgba[s + 1], b = src.rgba[s + 2];
      if (color) { r = tint(r, color[0]); g = tint(g, color[1]); b = tint(b, color[2]); }
      const d = (ty * dw + tx) * 4;
      const da = dst[d + 3];
      if (sa === 255 || !da) { dst[d] = r; dst[d + 1] = g; dst[d + 2] = b; dst[d + 3] = sa; continue; }
      const a = sa / 255, ba = (da / 255) * (1 - a), oa = a + ba;
      dst[d] = Math.floor((r * a + dst[d] * ba) / oa);
      dst[d + 1] = Math.floor((g * a + dst[d + 1] * ba) / oa);
      dst[d + 2] = Math.floor((b * a + dst[d + 2] * ba) / oa);
      dst[d + 3] = Math.floor(oa * 255);
    }
  }
}
