const PARAM = /^:([A-Za-z_][A-Za-z0-9_]*)(?:\((.+)\))?$/;
const REST = /^\*([A-Za-z_][A-Za-z0-9_]*)$/;
const escapeRe = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

export const compile = (pattern) => {
  if (typeof pattern !== 'string' || !pattern.startsWith('/')) throw new Error(`route pattern must start with '/': ${pattern}`);
  if (pattern !== '/' && pattern.endsWith('/')) throw new Error(`route pattern must not end with '/': ${pattern}`);
  const segs = pattern === '/' ? [] : pattern.slice(1).split('/');
  const names = new Set();
  let rest = null;
  let src = '';
  segs.forEach((seg, i) => {
    let m = REST.exec(seg);
    if (m) {
      if (i !== segs.length - 1) throw new Error(`*${m[1]} must be the last segment: ${pattern}`);
      rest = m[1];
      if (names.has(m[1])) throw new Error(`duplicate param ${m[1]}: ${pattern}`);
      names.add(m[1]);
      src += `/(?<${m[1]}>.+)`;
      return;
    }
    m = PARAM.exec(seg);
    if (m) {
      if (names.has(m[1])) throw new Error(`duplicate param ${m[1]}: ${pattern}`);
      names.add(m[1]);
      if (m[2] != null && (m[2].includes('/') || /\(\?<[A-Za-z]/.test(m[2]))) {
        throw new Error(`param regex may not contain '/' or named groups: ${pattern}`);
      }
      const inner = m[2] ?? '[^/]+';
      new RegExp(`^(?:${inner})$`);
      src += `/(?<${m[1]}>(?:${inner}))`;
      return;
    }
    if (seg === '' || /[:*()]/.test(seg)) throw new Error(`bad segment '${seg}' in ${pattern}`);
    src += `/${escapeRe(seg)}`;
  });
  return { pattern, re: new RegExp(`^${src || '/'}$`), rest };
};

export const match = (compiled, path) => {
  const m = compiled.re.exec(path);
  if (!m) return null;
  const params = { ...(m.groups ?? {}) };
  for (const [k, v] of Object.entries(params)) if (k !== compiled.rest && v.includes('/')) return null;
  return params;
};

export const normalizePath = (path) => (path.length > 1 && path.endsWith('/') ? path.slice(0, -1) : path);
