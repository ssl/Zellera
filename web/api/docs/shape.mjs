const MAX_PROBLEMS = 25;

const TOKENS = [['yyyy', '\\d{4}'], ['SSS', '\\d{3}'], ['MM', '\\d{2}'], ['dd', '\\d{2}'], ['HH', '\\d{2}'], ['mm', '\\d{2}'], ['ss', '\\d{2}']];
const formatCache = new Map();
export const formatRegExp = (layout) => {
  if (formatCache.has(layout)) return formatCache.get(layout);
  let out = '';
  for (let i = 0; i < layout.length;) {
    const tok = TOKENS.find(([t]) => layout.startsWith(t, i));
    if (tok) { out += tok[1]; i += tok[0].length; continue; }
    out += layout[i].replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    i++;
  }
  const re = new RegExp(`^${out}$`);
  formatCache.set(layout, re);
  return re;
};

const kindOf = (v) => {
  if (v === null) return 'null';
  if (Array.isArray(v)) return 'array';
  if (typeof v === 'number') return Number.isInteger(v) ? 'integer' : 'number';
  return typeof v;
};
const fitsType = (type, v) => {
  const k = kindOf(v);
  if (type === 'number') return k === 'number' || k === 'integer';
  return k === type;
};
const show = (v) => {
  const s = JSON.stringify(v);
  return s === undefined ? String(v) : s.length > 60 ? `${s.slice(0, 57)}...` : s;
};

const resolveSchema = (schema, schemas) => {
  let s = schema;
  for (let n = 0; s && typeof s.$ref === 'string' && n < 20; n++) s = schemas[s.$ref] ?? null;
  return s;
};

const knownKeys = (schema, schemas, seen = new Set()) => {
  const s = resolveSchema(schema, schemas);
  if (!s || seen.has(s)) return { keys: new Set(), open: false };
  seen.add(s);
  const keys = new Set(Object.keys(s.properties ?? {}));
  let open = s.additionalProperties !== undefined && s.additionalProperties !== false;
  for (const part of s.allOf ?? []) {
    const k = knownKeys(part, schemas, seen);
    k.keys.forEach((x) => keys.add(x));
    open ||= k.open;
  }
  return { keys, open };
};

const walk = (value, schema, schemas, path, out, opt) => {
  if (out.length >= MAX_PROBLEMS) return;
  if (schema === undefined || schema === null) return;
  if (typeof schema.$ref === 'string') {
    const target = resolveSchema(schema, schemas);
    if (!target) { out.push(`${path}: unknown schema ${schema.$ref}`); return; }
    walk(value, target, schemas, path, out, opt);
    return;
  }
  if (value === null && schema.nullable) return;

  const branches = schema.oneOf ?? schema.anyOf;
  if (branches) {
    let best = null;
    for (const b of branches) {
      const sub = [];
      walk(value, b, schemas, path, sub, opt);
      if (sub.length === 0) { best = null; break; }
      if (!best || sub.length < best.length) best = sub;
    }
    if (best) out.push(...best.slice(0, MAX_PROBLEMS - out.length));
    if (best || !schema.type) return;
  }
  if (schema.allOf) {
    for (const part of schema.allOf) walk(value, part, schemas, path, out, { ...opt, inAllOf: true });
  }
  const here = opt.inAllOf;
  opt = { ...opt, inAllOf: false };

  const type = schema.type;
  if (type && !fitsType(type, value)) {
    out.push(`${path}: expected ${schema.nullable ? `${type} or null` : type}, got ${kindOf(value)} ${show(value)}`);
    return;
  }
  if (schema.enum && !schema.enum.includes(value)) out.push(`${path}: ${show(value)} is not one of ${schema.enum.join(', ')}`);

  if (typeof value === 'string') {
    if (schema.pattern && !new RegExp(schema.pattern).test(value)) out.push(`${path}: ${show(value)} does not match ${schema.pattern}`);
    if (schema['x-format'] && !formatRegExp(schema['x-format']).test(value)) out.push(`${path}: ${show(value)} is not ${schema['x-format']}`);
  }

  if (Array.isArray(value)) {
    if (schema.minItems !== undefined && value.length < schema.minItems) out.push(`${path}: fewer than ${schema.minItems} items`);
    if (schema.maxItems !== undefined && value.length > schema.maxItems) out.push(`${path}: more than ${schema.maxItems} items`);
    if (schema.items) value.forEach((item, i) => walk(item, schema.items, schemas, `${path}[${i}]`, out, opt));
    return;
  }

  if (kindOf(value) !== 'object') return;
  const props = schema.properties ?? {};
  for (const key of schema.required ?? []) if (!(key in value)) out.push(`${path}: missing key ${key}`);
  for (const [key, v] of Object.entries(value)) {
    if (Object.hasOwn(props, key)) walk(v, props[key], schemas, `${path}.${key}`, out, opt);
    else if (schema.additionalProperties && typeof schema.additionalProperties === 'object') walk(v, schema.additionalProperties, schemas, `${path}.${key}`, out, opt);
    else if (schema.additionalProperties === false) out.push(`${path}: unexpected key ${key}`);
  }
  if (opt.strictKeys && !here && schema.additionalProperties === undefined && (schema.properties || schema.allOf)) {
    const { keys, open } = knownKeys(schema, schemas);
    if (!open) for (const key of Object.keys(value)) if (!keys.has(key)) out.push(`${path}: key ${key} is not in the schema`);
  }
};

export const checkShape = (value, schema, schemas, { strictKeys = false } = {}) => {
  const out = [];
  walk(value, schema, schemas ?? {}, '$', out, { strictKeys, inAllOf: false });
  return out;
};
