import { createHash } from 'node:crypto';
import { today } from '../lib/dates.mjs';
import { empty, json, notFound, ok, validation, xml } from '../lib/http.mjs';
import { SECONDS_PER_DAY } from '../../lib/time.mjs';

export const clock = { now: () => Date.now() };

const ping = async (ctx) => {
  try {
    await ctx.db.query('SELECT 1');
    return empty(200);
  } catch {
    return empty(503);
  }
};

const FEATURED_LOOKS_MAX = 10;
const AVATAR_PATH = '/imaging/avatar/';
const XML_INVALID = /[^\t\n\r\x20-퟿-�\u{10000}-\u{10FFFF}]/gu;
const ATTR_ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;', '\t': '&#9;', '\n': '&#10;', '\r': '&#13;' };
const xmlAttr = (value) => String(value).replace(XML_INVALID, '').replace(/[&<>"'\t\n\r]/g, (c) => ATTR_ESCAPES[c]);

export const lookGender = (g) => (String(g ?? '').trim().charAt(0).toUpperCase() === 'F' ? 'f' : 'm');
export const lookHash = (gender, figure) => createHash('md5').update(`${gender}:${figure}`, 'utf8').digest('hex');

const featuredLooksXml = (looks) => {
  let out = `<looks url="${AVATAR_PATH}">`;
  for (const { gender, figure } of looks) {
    out += `<look gender="${gender}" figure="${xmlAttr(figure)}" hash="${lookHash(gender, figure)}"/>`;
  }
  return `${out}</looks>`;
};

const getFeaturedLooks = async (ctx) => {
  const rows = await ctx.db.query(
    `SELECT gender, figure FROM featured_looks WHERE enabled = TRUE AND figure REGEXP '[^[:space:]]' ORDER BY RAND() LIMIT ${FEATURED_LOOKS_MAX}`,
  );
  const looks = rows
    .filter((r) => typeof r.figure === 'string' && /\S/.test(r.figure))
    .slice(0, FEATURED_LOOKS_MAX)
    .map((r) => ({ gender: lookGender(r.gender), figure: r.figure }));
  return xml(200, featuredLooksXml(looks));
};

export const MARKET = {
  maxItemsPerList: 25,
  historyDays: 30,
  listingWindowSeconds: 172800,
  minListingPrice: 1,
  maxListingPrice: 1_000_000_000,
};
const CLASS_NAME = /^[\x20-\x7E]{1,70}$/;

export const buyerPrice = (price) => price + Math.ceil(price / 100);

export const tooManyItems = () => json(400, { status: 'NOK', message: `Too many items in the request. The maximum allowed is ${MARKET.maxItemsPerList} per type.` });

const TOO_MANY = Symbol('too many');

const itemList = (value) => {
  if (value === undefined) return [];
  if (!Array.isArray(value)) return null;
  if (value.length > MARKET.maxItemsPerList) return TOO_MANY;
  const names = [];
  for (const e of value) {
    if (typeof e === 'string') { names.push(e); continue; }
    if (!e || typeof e !== 'object' || Array.isArray(e) || typeof e.item !== 'string') return null;
    names.push(e.item);
  }
  return names;
};

const placeholders = (n) => Array(n).fill('?').join(', ');

const spriteCondition = (sprites) => {
  const parts = [];
  const params = [];
  for (const type of ['s', 'i']) {
    const ids = [...(sprites[type] ?? [])];
    if (!ids.length) continue;
    parts.push(`(b.kind = ? AND b.sprite_id IN (${placeholders(ids.length)}))`);
    params.push(type, ...ids);
  }
  return { sql: `(${parts.join(' OR ')})`, params };
};

const SOLD_AT = 'COALESCE(m.sold_at, m.listed_at)';

const marketNumbers = async (db, sprites, nowSec) => {
  const cond = spriteCondition(sprites);
  const todayNum = Math.floor(nowSec / SECONDS_PER_DAY);
  const from = (todayNum - MARKET.historyDays) * SECONDS_PER_DAY;
  const to = (todayNum + 1) * SECONDS_PER_DAY;
  const [sales, listings, open] = await Promise.all([
    db.query(
      `SELECT b.kind AS type, b.sprite_id, FLOOR(${SOLD_AT} / ${SECONDS_PER_DAY}) AS day, COUNT(*) AS sold, SUM(m.price + CEIL(m.price / 100)) AS credits`
      + ' FROM marketplace_listings m JOIN furni i ON i.id = m.furni_id JOIN furni_definitions b ON b.id = i.definition_id'
      + ` WHERE m.state = 2 AND i.limited_number = 0 AND i.limited_total = 0 AND ${cond.sql} AND ${SOLD_AT} >= ? AND ${SOLD_AT} < ?`
      + ' GROUP BY b.kind, b.sprite_id, day',
      [...cond.params, from, to],
    ),
    db.query(
      `SELECT b.kind AS type, b.sprite_id, FLOOR(m.listed_at / ${SECONDS_PER_DAY}) AS day, COUNT(*) AS listed`
      + ' FROM marketplace_listings m JOIN furni i ON i.id = m.furni_id JOIN furni_definitions b ON b.id = i.definition_id'
      + ` WHERE i.limited_number = 0 AND i.limited_total = 0 AND ${cond.sql} AND m.listed_at >= ? AND m.listed_at < ?`
      + ' GROUP BY b.kind, b.sprite_id, day',
      [...cond.params, from, to],
    ),
    db.query(
      'SELECT b.kind AS type, b.sprite_id, COUNT(*) AS open, MIN(m.price) AS min_price'
      + ' FROM marketplace_listings m JOIN furni i ON i.id = m.furni_id JOIN furni_definitions b ON b.id = i.definition_id'
      + ` WHERE m.state = 1 AND m.listed_at > ? AND m.price BETWEEN ? AND ? AND i.limited_number = 0 AND i.limited_total = 0 AND ${cond.sql}`
      + ' GROUP BY b.kind, b.sprite_id',
      [nowSec - MARKET.listingWindowSeconds, MARKET.minListingPrice, MARKET.maxListingPrice, ...cond.params],
    ),
  ]);
  const byKey = new Map();
  const of = (r) => {
    const k = `${r.type}:${Number(r.sprite_id)}`;
    if (!byKey.has(k)) byKey.set(k, { days: new Map(), open: 0, minPrice: 0 });
    return byKey.get(k);
  };
  const dayOf = (s, d) => {
    if (!s.days.has(d)) s.days.set(d, { sold: 0, credits: 0, listed: 0 });
    return s.days.get(d);
  };
  for (const r of sales) {
    const d = dayOf(of(r), Number(r.day));
    d.sold += Number(r.sold);
    d.credits += Number(r.credits);
  }
  for (const r of listings) dayOf(of(r), Number(r.day)).listed += Number(r.listed);
  for (const r of open) {
    const s = of(r);
    s.open += Number(r.open);
    s.minPrice = Number(r.min_price);
  }
  return byKey;
};

const combine = (keys, byKey) => {
  const out = { days: new Map(), open: 0, minPrice: 0 };
  for (const k of keys) {
    const s = byKey.get(k);
    if (!s) continue;
    for (const [d, v] of s.days) {
      const t = out.days.get(d) ?? { sold: 0, credits: 0, listed: 0 };
      t.sold += v.sold;
      t.credits += v.credits;
      t.listed += v.listed;
      out.days.set(d, t);
    }
    out.open += s.open;
    if (s.open && (!out.minPrice || s.minPrice < out.minPrice)) out.minPrice = s.minPrice;
  }
  return out;
};

export const statsEntry = (item, stats, nowMs) => {
  const todayNum = Math.floor(nowMs / 1000 / SECONDS_PER_DAY);
  const history = [];
  let soldSum = 0;
  let creditTotal = 0;
  for (let k = -MARKET.historyDays; k < 0; k++) {
    const d = stats?.days.get(todayNum + k);
    const sold = d?.sold ?? 0;
    const credits = d?.credits ?? 0;
    if (!sold) continue;
    const average = Math.round(credits / sold);
    soldSum += sold;
    creditTotal += credits;
    history.push({
      dayOffset: String(k),
      averagePrice: String(average),
      totalSoldItems: String(sold),
      totalCreditSum: String(credits),
      totalOpenOffers: String(d?.listed ?? 0),
    });
  }
  const t = stats?.days.get(todayNum);
  soldSum += t?.sold ?? 0;
  creditTotal += t?.credits ?? 0;
  return {
    item,
    extraData: null,
    statsDate: today(nowMs),
    history,
    soldItemCount: t?.sold ?? 0,
    creditSum: t?.credits ?? 0,
    averagePrice: soldSum ? Math.round(creditTotal / soldSum) : 0,
    totalOpenOffers: t?.listed ?? 0,
    currentOpenOffers: stats?.open ?? 0,
    currentPrice: stats?.open ? buyerPrice(stats.minPrice) : 0,
    historyLimitInDays: MARKET.historyDays,
  };
};

const getMarketplaceStatsBatch = async (ctx) => {
  const body = await ctx.body();
  if (!body || typeof body !== 'object' || Array.isArray(body)) return notFound();
  const roomNames = itemList(body.roomItems);
  const wallNames = itemList(body.wallItems);
  if (roomNames === TOO_MANY || wallNames === TOO_MANY) return tooManyItems();
  if (!roomNames || !wallNames) return notFound();
  const nowMs = clock.now();

  const asked = { s: new Set(roomNames.filter((n) => CLASS_NAME.test(n))), i: new Set(wallNames.filter((n) => CLASS_NAME.test(n))) };
  const keysOf = { s: new Map(), i: new Map() };
  const sprites = { s: new Set(), i: new Set() };
  if (asked.s.size || asked.i.size) {
    const parts = [];
    const params = [];
    for (const type of ['s', 'i']) {
      if (!asked[type].size) continue;
      parts.push(`(kind = ? AND code IN (${placeholders(asked[type].size)}))`);
      params.push(type, ...asked[type]);
    }
    const rows = await ctx.db.query(`SELECT code, kind, sprite_id FROM furni_definitions WHERE sprite_id > 0 AND (${parts.join(' OR ')})`, params);
    for (const r of rows) {
      const type = r.kind;
      if (!asked[type]?.has(r.code)) continue;
      const sprite = Number(r.sprite_id);
      if (!keysOf[type].has(r.code)) keysOf[type].set(r.code, new Set());
      keysOf[type].get(r.code).add(`${type}:${sprite}`);
      sprites[type].add(sprite);
    }
  }
  const byKey = sprites.s.size || sprites.i.size ? await marketNumbers(ctx.db, sprites, Math.floor(nowMs / 1000)) : new Map();
  const entries = (type, names) => names.map((n) => {
    const keys = keysOf[type].get(n);
    return statsEntry(n, keys ? combine(keys, byKey) : null, nowMs);
  });
  return ok({ status: 'OK', roomItemData: entries('s', roomNames), wallItemData: entries('i', wallNames) });
};

export const handlers = {
  ping,
  getFeaturedLooks,
  getMarketplaceStatsBatch,
};

export const grants = [
  { table: 'featured_looks', select: ['gender', 'figure', 'enabled'] },
  { table: 'furni_definitions', select: ['id', 'code', 'kind', 'sprite_id'] },
  { table: 'furni', select: ['id', 'definition_id', 'limited_number', 'limited_total'] },
  { table: 'marketplace_listings', select: ['furni_id', 'price', 'listed_at', 'sold_at', 'state'] },
];
