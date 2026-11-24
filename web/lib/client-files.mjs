import { createHash } from 'node:crypto';
import { existsSync, readFileSync, realpathSync, statSync } from 'node:fs';
import { normalize, posix, sep } from 'node:path';
import { MIME, safeJoin, sendFile, sendText } from './files.mjs';

export const CLIENT_MANIFEST = 'client.json';
export const CLIENT_FORMAT = 1;

const FILE = /^(?!.*\.\.)[A-Za-z0-9][A-Za-z0-9_./-]{0,199}$/;
const FIELD = /^[A-Za-z][A-Za-z0-9_]{0,63}$/;

const CLIENT_HEADERS = Object.freeze({ 'content-security-policy': "frame-ancestors 'self'", 'x-frame-options': 'SAMEORIGIN', 'x-content-type-options': 'nosniff' });

export class ClientError extends Error {}

export const readClientManifest = (folder) => {
  const clientDir = normalize(folder);
  const fail = (msg) => { throw new ClientError(`no usable client at ${clientDir} (ZEL_CLIENT_DIR): ${msg}`); };
  const file = safeJoin(clientDir, CLIENT_MANIFEST);
  if (!file || !existsSync(file)) fail(`it has no ${CLIENT_MANIFEST}; put a built client there`);
  let m;
  try { m = JSON.parse(readFileSync(file, 'utf8')); } catch (e) { fail(`${CLIENT_MANIFEST} is not JSON (${e.message})`); }
  if (!m || typeof m !== 'object' || Array.isArray(m)) fail(`${CLIENT_MANIFEST} is not an object`);
  if (!Number.isInteger(m.format)) fail(`${CLIENT_MANIFEST} has no format number`);
  if (m.format !== CLIENT_FORMAT) fail(`it has client format ${m.format}, this version serves format ${CLIENT_FORMAT}; build the client for this version`);
  const path = (key, needed) => {
    if (m[key] == null && !needed) return null;
    if (typeof m[key] !== 'string' || !FILE.test(m[key])) fail(`${CLIENT_MANIFEST}: ${key} names a file in the client folder`);
    const found = safeJoin(clientDir, m[key]);
    if (!found || !existsSync(found) || !statSync(found).isFile()) fail(`${m[key]} (${key} in ${CLIENT_MANIFEST}) is missing`);
    return found;
  };
  if (typeof m.keyField !== 'string' || !FIELD.test(m.keyField)) fail(`${CLIENT_MANIFEST}: keyField names a field of the config file`);
  return Object.freeze({
    entry: path('entry', true), script: path('script', true), config: path('config', true), renderLibrary: path('renderLibrary', false),
    base: posix.dirname(m.entry), keyField: m.keyField,
  });
};

export const launcherConfig = (text, modulus, keyField) => {
  if (!modulus) return text;
  let config;
  try { config = JSON.parse(text); } catch { return text; }
  if (config?.[keyField] !== '') return text;
  return `${JSON.stringify(Object.assign({ [keyField]: '' }, config, { [keyField]: modulus }), null, 2)}\n`;
};

export const createClientFiles = ({ clientDir, manifest = readClientManifest(clientDir), modulus = '' }) => {
  const key = /^[0-9a-f]+$/i.test(modulus) ? modulus : '';
  const config = () => {
    const bytes = readFileSync(manifest.config);
    const text = bytes.toString('utf8');
    const filled = launcherConfig(text, key, manifest.keyField);
    return filled === text ? bytes : filled;
  };

  let versionCache = { mtime: '', id: '' };
  const version = () => {
    const files = [manifest.script, manifest.config];
    const mtime = files.map((f) => statSync(f).mtimeMs).join();
    if (versionCache.mtime !== mtime) {
      const hash = createHash('sha1');
      for (const f of files) hash.update(f === manifest.config ? config() : readFileSync(f));
      versionCache = { mtime, id: hash.digest('hex').slice(0, 12) };
    }
    return versionCache.id;
  };

  const root = normalize(clientDir);
  const fileOf = (rel) => {
    if (rel.split('/').some((part) => part === '' || part.startsWith('.'))) return null;
    const file = safeJoin(root, rel);
    if (!file || !existsSync(file) || !statSync(file).isFile()) return null;
    const real = realpathSync(file);
    return real.startsWith(realpathSync(root) + sep) ? file : null;
  };

  const send = (req, res, path) => {
    const versioned = /^\/client\/v\/([0-9a-f]{12})\/(.+)$/.exec(path);
    const rel = versioned ? versioned[2] : path.slice('/client/'.length);
    const file = fileOf(rel);
    if (!file) return sendText(res, 404, 'not found', undefined, CLIENT_HEADERS);
    const headers = { 'cache-control': versioned && versioned[1] === version() ? 'public, max-age=31536000, immutable' : 'no-cache', ...CLIENT_HEADERS };
    const filled = file.toLowerCase() === manifest.config.toLowerCase() ? config() : null;
    if (typeof filled === 'string') return sendText(res, 200, filled, MIME['.json'], headers);
    return sendFile(req, res, file, headers);
  };

  return {
    version, send,
    entryPage: () => readFileSync(manifest.entry, 'utf8'),
    base: () => `/client/v/${version()}/${manifest.base === '.' ? '' : `${manifest.base}/`}`,
    renderLibrary: () => manifest.renderLibrary,
    present: () => existsSync(manifest.entry),
  };
};
