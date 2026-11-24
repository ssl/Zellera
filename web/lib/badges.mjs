const ROMAN = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI', 'XII', 'XIII', 'XIV', 'XV',
  'XVI', 'XVII', 'XVIII', 'XIX', 'XX', 'XXI', 'XXII', 'XXIII', 'XXIV', 'XXV', 'XXVI', 'XXVII', 'XXVIII', 'XXIX', 'XXX'];

const isDigit = (c) => c >= '0' && c <= '9';

export const splitBadgeCode = (code) => {
  const s = String(code ?? '');
  let r = s.length - 1;
  while (r > 0 && isDigit(s.charAt(r))) r -= 1;
  const digits = s.substring(r + 1);
  return { base: s.substring(0, r + 1), level: digits.length > 0 ? Number.parseInt(digits, 10) : 1 };
};

export const romanLevel = (level) => ROMAN[Math.max(0, Number(level) - 1)] ?? '';

const interpolate = (texts, value) => {
  let out = value;
  for (let pass = 0; pass < 5; pass++) {
    let replaced = 0;
    out = out.replace(/\$\{([^}]*)\}/g, (whole, key) => {
      const v = texts.get(key, null);
      if (v == null) return whole;
      replaced += 1;
      return String(v).trim();
    });
    if (replaced === 0) break;
  }
  return out;
};

const text = (texts, key) => String(texts.get(key, '') ?? '').trim().replace(/\\n/g, ' ');

const resolve = (texts, prefix, code, limitOf) => {
  const c = String(code ?? '');
  if (!c || !texts || typeof texts.get !== 'function') return '';
  const { base, level } = splitBadgeCode(c);
  const keys = [`${prefix}_al_${c}`, `${prefix}_al_${base}`, `${prefix}_${c}`, `${prefix}_${base}`];
  let value = '';
  for (const key of keys) {
    const v = text(texts, key);
    if (v !== '' && interpolate(texts, v).trim() !== '') { value = v; break; }
  }
  if (value === '') return '';
  let out = value.replace(/%roman%/gi, romanLevel(level));
  if (/%limit%/i.test(out) && typeof limitOf === 'function') {
    const limit = limitOf(base, level);
    if (limit != null) out = out.replace(/%limit%/gi, String(limit));
  }
  return interpolate(texts, out).replace(/\s+/g, ' ').trim();
};

export const badgeName = (texts, code, fallback = '') => resolve(texts, 'badge_name', code, null) || fallback;

export const badgeNameOrCode = (texts, code) => badgeName(texts, code, String(code ?? ''));

export const badgeDescription = (texts, code, limitOf = null) => resolve(texts, 'badge_desc', code, limitOf);
