import { randomBytes } from 'node:crypto';
import { chmodSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';

export const root = normalize(join(fileURLToPath(new URL('.', import.meta.url)), '..', '..'));

export const VERSION = readFileSync(join(root, 'VERSION'), 'utf8').trim();
export const customDir = join(root, 'custom');

const ORIGIN = /^https?:\/\/(?:[a-z0-9.-]+|\[[0-9a-f:]+\])(?::\d{1,5})?$/i;
const origin = (name, value) => {
  if (!ORIGIN.test(value)) throw new Error(`${name} must be an origin like https://game.example.org (no path), not ${JSON.stringify(value)}`);
  return new URL(value).origin;
};
export const siteUrl = (env) => origin('ZEL_SITE_URL', String(env.ZEL_SITE_URL ?? '').trim() || 'https://zellera.localhost:8444');
export const extraOrigins = (env) => String(env.ZEL_EXTRA_ORIGINS ?? '').split(',').map((s) => s.trim()).filter(Boolean)
  .map((o) => origin('ZEL_EXTRA_ORIGINS', o));

export const secretsDir = (env) => normalize(env.ZEL_SECRETS_DIR || join(root, 'secrets'));

/** The modulus of the key exchange's key (ZEL_KEY_FILE, else rsa.env in the secrets folder); empty: no key exchange. */
export const keyModulus = (env) => {
  if (env.ZEL_KEY_EXCHANGE === '0') return '';
  const file = env.ZEL_KEY_FILE || join(secretsDir(env), 'rsa.env');
  if (!existsSync(file)) return '';
  const modulus = /^ZEL_RSA_N=([0-9A-Fa-f]+)\s*$/m.exec(readFileSync(file, 'utf8'))?.[1];
  if (!modulus) throw new Error(`${file} must hold the line ZEL_RSA_N= in hex`);
  return modulus;
};

export const dbConfig = (env, user, password) => (user
  ? {
      host: env.ZEL_DB_HOST || '127.0.0.1',
      port: Number(env.ZEL_DB_PORT || 3317),
      user,
      password: password ?? '',
      database: env.ZEL_DB_NAME || 'zellera',
    }
  : {
      socketPath: env.ZEL_DB_SOCKET || join(root, '.data', 'mariadb.sock'),
      user: env.USER,
      database: env.ZEL_DB_NAME || 'zellera',
    });

export const secretFile = (dir, name, { tag, bytes, pattern, rule }) => {
  const file = join(dir, name);
  if (!existsSync(file)) {
    try {
      mkdirSync(dir, { recursive: true, mode: 0o700 });
      writeFileSync(file, `${randomBytes(bytes).toString('hex')}\n`, { mode: 0o600, flag: 'wx' });
      chmodSync(file, 0o600);
      console.error(`${tag}: generated ${file}`);
    } catch (e) {
      if (e.code !== 'EEXIST') {
        throw new Error(`${tag}: ${file} is missing and could not be created (${e.code}). `
          + `Create it: openssl rand -hex ${bytes} > ${file} && chmod 600 ${file}`);
      }
    }
  }
  const value = readFileSync(file, 'utf8').trim();
  if (!pattern.test(value)) throw new Error(`${tag}: ${file} must hold ${rule}`);
  return value;
};
