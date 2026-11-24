export const parseTexts = (body) => {
  const map = new Map();
  for (const line of String(body ?? '').split(/\r?\n/)) {
    const i = line.indexOf('=');
    if (i > 0 && !line.startsWith('#')) map.set(line.slice(0, i), line.slice(i + 1));
  }
  return map;
};

export const createTexts = (getBody) => {
  let last;
  let map = new Map();
  const current = () => {
    const body = getBody?.();
    if (body !== last) { last = body; map = parseTexts(body); }
    return map;
  };
  return {
    get: (key, fallback = '') => current().get(key) ?? fallback,
    has: (key) => current().has(key),
  };
};

export const emptyTexts = { get: (_key, fallback = '') => fallback, has: () => false };
