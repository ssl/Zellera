const split = (path) => path.split('/').filter((s, i) => i > 0 || s !== '');

export const compileRoutes = (endpoints) => {
  const routes = endpoints.map((e) => {
    const routeEnums = Object.fromEntries((e.params ?? []).filter((p) => p.in === 'path' && p.route?.match)
      .map((p) => [p.name, new Set(p.route.match.map((v) => v.toLowerCase()))]));
    const segments = split(e.path).map((s) => {
      const m = /^\{([^}]+)\}$/.exec(s);
      return m ? { param: m[1], only: routeEnums[m[1]] ?? null } : { fixed: s.toLowerCase() };
    });
    return { endpoint: e, method: e.method, segments };
  });
  const cmp = (a, b) => {
    const n = Math.min(a.r.segments.length, b.r.segments.length);
    for (let k = 0; k < n; k++) {
      const sa = a.r.segments[k];
      const sb = b.r.segments[k];
      const fa = 'fixed' in sa;
      const fb = 'fixed' in sb;
      if (fa !== fb) return fa ? -1 : 1;
      if (fa && sa.fixed !== sb.fixed) return sa.fixed < sb.fixed ? -1 : 1;
    }
    return a.r.segments.length - b.r.segments.length || a.i - b.i;
  };
  return routes.map((r, i) => ({ r, i })).sort(cmp).map(({ r }) => r);
};

export class BadParam extends Error {}

export const matchRoute = (table, method, pathname) => {
  let parts = pathname.split('/').slice(1);
  if (parts.length > 1 && parts[parts.length - 1] === '') parts = parts.slice(0, -1);
  const want = method === 'HEAD' ? 'GET' : method;
  for (const route of table) {
    if (route.method !== want || route.segments.length !== parts.length) continue;
    const params = {};
    let fits = true;
    for (let k = 0; k < parts.length && fits; k++) {
      const seg = route.segments[k];
      const raw = parts[k];
      if ('fixed' in seg) { fits = raw.toLowerCase() === seg.fixed; continue; }
      if (raw === '') { fits = false; continue; }
      let value;
      try { value = decodeURIComponent(raw); } catch { throw new BadParam(seg.param); }
      if (seg.only && !seg.only.has(value.toLowerCase())) { fits = false; continue; }
      params[seg.param] = value;
    }
    if (fits) return { endpoint: route.endpoint, params };
  }
  return null;
};
