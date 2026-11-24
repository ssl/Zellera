import { bundleExtension, escapeRegExp, fromProfile, furniPath } from './client-profile.mjs';

const ICON = '_icon.png';
const filePattern = fromProfile(() => (furniPath() && bundleExtension()
  ? new RegExp(`^${escapeRegExp(furniPath())}/(\\d+)/([A-Za-z0-9_.-]+?)(${escapeRegExp(ICON)}|${escapeRegExp(bundleExtension())})$`) : null));

export const furniFilePath = (rev, name, kind) => `${furniPath()}/${rev}/${name}${kind === 'bundle' ? bundleExtension() : ICON}`;

export function furnidataTypes(text) {
  const fd = JSON.parse(text);
  const types = new Map();
  for (const [key, type] of [['roomitemtypes', 's'], ['wallitemtypes', 'i']]) {
    for (const f of fd[key]?.furnitype ?? []) {
      if (!f.classname) continue;
      const [name0, colour] = String(f.classname).split('*');
      const name = type === 'i' && name0 === 'post.it' ? 'post_it' : type === 'i' && name0 === 'post.it.vd' ? 'post_it_vd' : name0;
      if (!types.has(name)) types.set(name, { type, revisions: new Set(), colours: new Set() });
      const t = types.get(name);
      if (Number(f.revision) > 0) t.revisions.add(Number(f.revision));
      if (colour !== undefined) t.colours.add(colour);
    }
  }
  return types;
}

export function furniFile(rel, types) {
  const m = filePattern()?.exec(rel);
  if (!m || !types) return null;
  const rev = Number(m[1]), name = m[2], kind = m[3] === ICON ? 'icon' : 'bundle';
  let type = name, colour = null;
  if (!types(type)) {
    const c = /^(.+)_([^_]+)$/.exec(name);
    const p = /^poster(\d+)$/.exec(name);
    if (kind === 'icon' && c && types(c[1])?.colours.has(c[2])) { type = c[1]; colour = c[2]; }
    else if (p) type = 'poster';
    else return null;
  }
  const t = types(type);
  const revisions = new Set([...t.revisions]);
  if (name.startsWith('poster')) for (const r of types('poster')?.revisions ?? []) revisions.add(r);
  if (!revisions.has(rev)) return null;
  return { rev, name, kind, type, colour, revisions: [rev, ...[...revisions].filter((r) => r !== rev).sort((a, b) => b - a)] };
}
