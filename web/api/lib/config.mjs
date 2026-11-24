import { join, normalize } from 'node:path';
import { FALLBACK_NAME, settingName } from '../../cms/game-name.mjs';
import { dbConfig, root, secretFile, secretsDir, siteUrl } from '../../lib/env.mjs';
import { serverId } from '../../lib/client-profile.mjs';
import { specData } from './spec-data.mjs';

export const parseIpLimit = (raw) => {
  if (raw === '0' || raw === 'off') return null;
  const { perMinute: defMinute, per10s: def10s } = specData.rateLimits.public;
  const m = /^(\d{1,6})\/(\d{1,6})$/.exec(String(raw ?? `${defMinute}/${def10s}`).trim());
  const [perMinute, per10s] = m ? [Number(m[1]), Number(m[2])] : [defMinute, def10s];
  return [{ windowMs: 60_000, max: perMinute }, { windowMs: 10_000, max: per10s }];
};

const fromEnv = (env) => {
  const wsPort = Number(env.ZEL_WS_PORT ?? 2076);
  const gamedataDir = normalize(env.ZEL_GAMEDATA_DIR || join(root, '.data', 'gamedata'));
  return {
    site: siteUrl(env),
    get serverId() { return serverId(); },
    gameName: FALLBACK_NAME,
    db: dbConfig(env, env.ZEL_DB_WEB_USER, env.ZEL_DB_WEB_PASS),
    secretsDir: secretsDir(env),
    gamedataDir,
    overridesDir: join(gamedataDir, 'overrides'),
    bridgeUrl: `http://127.0.0.1:${wsPort}/internal/wired-variables/v1`,
    wiredApiEnabled: env.ZEL_WIRED_API !== '0',
    ipLimit: parseIpLimit(env.ZEL_API_IP_LIMIT),
  };
};

export const config = fromEnv(process.env);

export const gameNameNow = () => settingName() ?? config.gameName;

const hexSecret = (name, hexChars) => secretFile(config.secretsDir, name, {
  tag: 'api', bytes: hexChars / 2, pattern: new RegExp(`^[0-9a-f]{${hexChars}}$`), rule: `${hexChars} lower-case hex characters`,
});

let idKeyCache = null;
export const idKey = () => {
  idKeyCache ??= Buffer.from(hexSecret('api-id-key', 32), 'hex');
  return idKeyCache;
};

let bridgeSecretCache = null;
export const bridgeSecret = () => {
  bridgeSecretCache ??= hexSecret('wired-bridge-secret', 64);
  return bridgeSecretCache;
};
