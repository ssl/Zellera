import { dbConfig, secretFile, secretsDir } from '../lib/env.mjs';

const env = process.env;

export const PROD = env.ZEL_ENV !== 'dev';
export const BETA_MODE = env.ZEL_BETA_MODE === '1' || env.ZEL_BETA_MODE === 'true';
export const BETA_CODE = env.ZEL_BETA_CODE ?? '';
export const DB_READONLY = env.ZEL_DB_READONLY === '1';
/** Whether the operator's config has a YouTube key; the web never needs the key itself. */
export const YOUTUBE_KEY_SET = String(env.ZEL_YOUTUBE_API_KEY ?? '').trim() !== '';

export const DB = dbConfig(env, env.ZEL_DB_WEB_USER, env.ZEL_DB_WEB_PASS);

export const SECRET = secretFile(secretsDir(env), 'cms-secret', { tag: 'cms', bytes: 32, pattern: /^.{32,}$/s, rule: 'at least 32 characters' });
