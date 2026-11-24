const SCHEMA_REF = '#/components/schemas/';
const UNDECLARED_TAGS = new Set(['default']);

const convertSchema = (value) => {
  if (Array.isArray(value)) return value.map(convertSchema);
  if (!value || typeof value !== 'object') return value;
  const out = {};
  for (const [k, v] of Object.entries(value)) {
    if (k === '$ref' && typeof v === 'string') out.$ref = v.startsWith('#/') ? v : `${SCHEMA_REF}${v}`;
    else if (k === 'example' || k === 'enum' || k === 'default') out[k] = v;
    else if (k === 'properties' && v && typeof v === 'object') out.properties = Object.fromEntries(Object.entries(v).map(([name, s]) => [name, convertSchema(s)]));
    else out[k] = convertSchema(v);
  }
  return out;
};

const mediaType = (contentType) => String(contentType).split(';')[0].trim();

const examplesOf = (examples) => {
  const entries = Object.entries(examples ?? {});
  return entries.length ? Object.fromEntries(entries.map(([name, value]) => [name, { value }])) : undefined;
};

const parameterObject = (p, { withExample = true } = {}) => {
  const o = { in: p.in, name: p.name, required: !!p.required };
  if (p.description) o.description = p.description;
  o.schema = convertSchema(p.schema);
  if (withExample && p.example !== undefined) o.example = p.example;
  return o;
};

const headersOf = (headers) => {
  if (!headers) return undefined;
  return Object.fromEntries(Object.entries(headers).map(([name, h]) => {
    const o = {};
    if (h.description) o.description = h.description;
    o.schema = convertSchema(h.schema ?? { type: 'string' });
    if (h.example !== undefined) o.example = h.example;
    return [name, o];
  }));
};

const responseObject = (r) => {
  const o = { description: r.description || 'Answer.' };
  const headers = headersOf(r.headers);
  if (headers) o.headers = headers;
  if (r.contentType) {
    const media = {};
    if (r.schema) media.schema = convertSchema(r.schema);
    const examples = examplesOf(r.examples);
    if (examples) media.examples = examples;
    o.content = { [mediaType(r.contentType)]: media };
  }
  return o;
};

export const buildOpenApi = (spec) => {
  const shared = new Map();
  for (const e of spec.endpoints) {
    for (const p of e.params ?? []) {
      if (!p.shared) continue;
      const list = shared.get(p.shared) ?? [];
      list.push(p);
      shared.set(p.shared, list);
    }
  }
  const parameters = {};
  for (const [name, uses] of shared) {
    const sameExample = uses.every((u) => JSON.stringify(u.example) === JSON.stringify(uses[0].example));
    parameters[name] = parameterObject(uses[0], { withExample: sameExample });
  }

  const tags = spec.groups.filter((g) => !UNDECLARED_TAGS.has(g.id)).map((g) => ({ name: g.name, description: g.text }));
  const paths = {};
  for (const e of spec.endpoints) {
    const op = {};
    if (e.group !== 'default') op.tags = [spec.groups.find((g) => g.id === e.group)?.name ?? e.group];
    op.summary = e.summary;
    if (e.description) op.description = e.description;
    op.operationId = e.id;
    const params = (e.params ?? []).map((p) => (p.shared ? { $ref: `#/components/parameters/${p.shared}` } : parameterObject(p)));
    if (params.length) op.parameters = params;
    if (e.requestBody) {
      const media = { schema: convertSchema(e.requestBody.schema) };
      const examples = examplesOf(e.requestBody.examples);
      if (examples) media.examples = examples;
      op.requestBody = { required: !!e.requestBody.required, content: { [mediaType(e.requestBody.contentType ?? 'application/json')]: media } };
    }
    op.responses = Object.fromEntries(Object.entries(e.responses).map(([status, r]) => [status, responseObject(r)]));
    if (e.rateLimit) op['x-rate-limit'] = e.rateLimit;
    if (e.notes?.length) op['x-notes'] = e.notes;
    if (e.differsFromBaseSpec?.length) op['x-differs-from-base-spec'] = e.differsFromBaseSpec;
    if (!e.inBaseSpec) op['x-not-in-base-spec'] = true;
    (paths[e.path] ??= {})[e.method.toLowerCase()] = op;
  }

  return {
    openapi: '3.0.0',
    info: { title: spec.info.title, version: spec.info.version, description: spec.info.description },
    tags,
    paths,
    components: {
      parameters,
      schemas: Object.fromEntries(Object.entries(spec.schemas).map(([name, s]) => [name, convertSchema(s)])),
    },
  };
};
