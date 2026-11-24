export const DEFAULT_MAX = 38;
export const SETTING_KEYS = Object.freeze(['player.motto.max.characters']);

export const javaTrim = (s) => String(s ?? '').replace(/^[\u0000- ]+|[\u0000- ]+$/g, '');

const javaInt = (v, fallback) => {
  if (typeof v !== 'string' || !/^[+-]?\d{1,10}$/.test(v)) return fallback;
  const n = Number(v);
  return n >= -2147483648 && n <= 2147483647 ? n : fallback;
};

export const maxFrom = (settingRows = []) => {
  const row = [...settingRows].find((r) => String(r.name) === 'player.motto.max.characters');
  return javaInt(row?.value == null ? null : String(row.value), DEFAULT_MAX);
};

export const checkTyped = (raw, max = DEFAULT_MAX) => {
  const motto = javaTrim(raw);
  if (/[\u0000-\u001f\u007f-\u009f\u2028\u2029]/.test(motto)) return { motto, error: 'line' };
  if (/[\u{10000}-\u{10FFFF}]/u.test(motto)) return { motto, error: 'symbols' };
  if (motto.length > max) return { motto, error: 'length' };
  return { motto, error: null };
};
