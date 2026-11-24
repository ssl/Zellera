import { existsSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { config } from './config.mjs';
import { parseTexts } from '../../cms/lib/texts.mjs';

const createFileTexts = (files, { checkEveryMs = 10_000, now = () => Date.now() } = {}) => {
  let map = new Map();
  let stamp = null;
  let checkedAt = -Infinity;
  const current = () => {
    if (now() - checkedAt < checkEveryMs) return map;
    checkedAt = now();
    const s = files().map((f) => (existsSync(f) ? `${f}:${statSync(f).mtimeMs}` : `${f}:-`)).join('|');
    if (s !== stamp) {
      stamp = s;
      map = parseTexts(files().filter((f) => existsSync(f)).map((f) => readFileSync(f, 'utf8')).join('\n'));
    }
    return map;
  };
  return {
    get: (key, fallback = '') => current().get(key) ?? fallback,
    has: (key) => current().has(key),
  };
};

export const texts = createFileTexts(() => [
  join(config.gamedataDir, 'external_texts'),
  join(config.overridesDir, 'external_texts.txt'),
]);
