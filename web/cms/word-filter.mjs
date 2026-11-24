const GROUPS = ['il1|!', 'o0', 'e3', 'a4@', 's5$', 'z2', 't7+', 'b8', 'g9'];
const GROUP_OF = new Map(GROUPS.flatMap((group) => [...group].map((c) => [c, group[0]])));
const LETTER_OR_DIGIT = /^[\p{L}\p{Nd}]$/u;

const lowerUnit = (c) => {
  const l = c.toLowerCase();
  return l.length === 1 ? l : l[0];
};

const plain = (c) => {
  const base = c.normalize('NFKD')[0];
  const read = lowerUnit(LETTER_OR_DIGIT.test(base) ? base : c);
  return GROUP_OF.get(read) ?? read;
};

const fold = (f) => (text) => {
  let out = '';
  for (let i = 0; i < text.length; i++) out += f(text[i]);
  return out;
};
export const normalise = fold(plain);
const lowercase = fold(lowerUnit);

const matcher = (words, foldText) => {
  const keys = words.map((w) => [foldText(w.word), w]).sort((a, b) => b[0].length - a[0].length);
  return (text) => {
    if (!keys.length) return [];
    const folded = foldText(text);
    const hits = [];
    let at = 0;
    while (at < folded.length) {
      const hit = keys.find(([key]) => folded.startsWith(key, at));
      if (!hit) at++;
      else {
        hits.push([at, at + hit[0].length, hit[1]]);
        at += hit[0].length;
      }
    }
    return hits;
  };
};

export const wordList = (words, normalising = false) => {
  const matched = words.filter((w) => w.word.length > 0);
  const censoring = matcher(matched, normalising ? normalise : lowercase);
  const hiding = matcher(matched.filter((w) => w.hideMessage), normalise);
  return Object.freeze({
    hides: (text) => hiding(text).length > 0,
    censor: (text) => {
      let out = '';
      let at = 0;
      for (const [from, to, w] of censoring(text)) {
        out += text.slice(at, from) + w.replacement;
        at = to;
      }
      return out + text.slice(at);
    },
  });
};

const flag = (value, fallback) => {
  const v = value == null ? '' : String(value).trim().toLowerCase();
  if (v === 'true' || v === '1') return true;
  if (v === 'false' || v === '0') return false;
  return fallback;
};

const FILTER_SETTINGS = Object.freeze(['word_filter.is_on', 'word_filter.normalises_text']);
export const RELOAD_MS = 2000;

export const createWordFilter = (query, { now = Date.now } = {}) => {
  let cached = null;
  let readAt = -Infinity;
  const list = async () => {
    if (cached && now() - readAt < RELOAD_MS) return cached;
    const settings = new Map((await query(`SELECT name, value FROM settings WHERE name IN (${FILTER_SETTINGS.map(() => '?').join(', ')})`, FILTER_SETTINGS))
      .map((r) => [String(r.name), r.value]));
    const enabled = flag(settings.get('word_filter.is_on'), false);
    const rows = enabled ? await query('SELECT word, replacement FROM word_filters') : [];
    cached = wordList(rows.map((r) => ({
      word: String(r.word ?? ''), replacement: String(r.replacement ?? ''), hideMessage: r.replacement == null,
    })), flag(settings.get('word_filter.normalises_text'), false));
    readAt = now();
    return cached;
  };
  return Object.freeze({
    filter: async (text) => {
      const l = await list();
      const s = String(text ?? '');
      return l.hides(s) ? { text: '', hidden: true } : { text: l.censor(s), hidden: false };
    },
  });
};
