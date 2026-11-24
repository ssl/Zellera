import { achievementRows } from './queries.mjs';

const STATES = { 1: 'ENABLED', 2: 'ARCHIVED', 3: 'OFF_SEASON', 4: 'ENABLED' };
const HIDDEN_CATEGORIES = new Set(['test', 'invisible']);
const TTL_MS = 60_000;
const CREATION_TIME = '2010-01-01';

const plainName = (name) => String(name ?? '').replace(/^ACH_/, '');

export const buildCatalogue = (rows) => {
  const groups = new Map();
  for (const r of rows) {
    const name = plainName(r.name);
    const g = groups.get(name) ?? [];
    g.push({ id: Number(r.id), category: String(r.category), level: Number(r.level), needed: Number(r.progress_needed), state: Number(r.state) });
    groups.set(name, g);
  }
  const list = [];
  const byName = new Map();
  const needed = new Map();
  for (const [name, levels] of groups) {
    levels.sort((a, b) => a.level - b.level || a.id - b.id);
    for (const l of levels) if (!needed.has(`${name}:${l.level}`)) needed.set(`${name}:${l.level}`, l.needed);
    const head = levels[0];
    if (!STATES[head.state] || HIDDEN_CATEGORIES.has(head.category)) continue;
    const entry = {
      achievement: {
        id: head.id,
        name,
        creationTime: CREATION_TIME,
        state: STATES[head.state],
        category: head.category,
      },
      levels: levels.filter((l) => STATES[l.state]).map((l) => ({ level: l.level, requiredScore: l.needed })),
    };
    list.push(entry);
    byName.set(name, entry);
  }
  list.sort((a, b) => a.achievement.id - b.achievement.id);
  const limitOf = (base, level) => {
    if (!String(base).startsWith('ACH_')) return null;
    return needed.get(`${plainName(base)}:${Number(level)}`) ?? null;
  };
  return { list, byName, limitOf };
};

export const levelOf = (levels, progress) => {
  let level = 0;
  for (const l of levels) if (progress >= l.requiredScore && l.level > level) level = l.level;
  return level;
};

export const createCatalogueCache = ({ now = () => Date.now() } = {}) => {
  const cache = new WeakMap();
  return async (db) => {
    const hit = cache.get(db);
    if (hit && now() - hit.at < TTL_MS) return hit.value;
    const value = buildCatalogue(await achievementRows(db));
    cache.set(db, { at: now(), value });
    return value;
  };
};
