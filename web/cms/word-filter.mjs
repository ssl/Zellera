const GROUPS = ['il1|!', 'o0', 'e3', 'a4@', 's5$', 'z2', 't7+', 'b8', 'g9'];
const GROUP_OF = new Map(GROUPS.flatMap((group) => [...group].map((c) => [c, group[0]])));
const LETTER_OR_DIGIT = /^[\p{L}\p{Nd}]$/u;

const HOMOGLYPHS = new Map([
  ['а', 'a'], ['в', 'b'], ['с', 'c'], ['е', 'e'], ['н', 'h'], ['і', 'i'], ['ј', 'j'], ['к', 'k'],
  ['м', 'm'], ['о', 'o'], ['р', 'p'], ['ԛ', 'q'], ['т', 't'], ['у', 'y'], ['х', 'x'], ['ѕ', 's'],
]);
const IGNORED = new Set([0x200b, 0x200c, 0x200d, 0x2060, 0xfeff, 0x00ad]);

const lowerUnit = (c) => {
  const l = c.toLowerCase();
  return l.length === 1 ? l : l[0];
};

const plain = (codePoint) => {
  const c = String.fromCodePoint(codePoint);
  const base = c.normalize('NFKD')[0];
  let read;
  if (LETTER_OR_DIGIT.test(base)) read = lowerUnit(base);
  else if (c.length === 1) read = lowerUnit(c);
  else return null;
  const mapped = HOMOGLYPHS.get(read) ?? read;
  return GROUP_OF.get(mapped) ?? mapped;
};

const folded = (text) => {
  let out = '';
  const origin = [];
  for (let i = 0; i < text.length;) {
    const codePoint = text.codePointAt(i);
    const width = codePoint > 0xffff ? 2 : 1;
    if (!IGNORED.has(codePoint)) {
      const read = plain(codePoint);
      if (read != null) {
        out += read;
        origin.push(i);
      } else {
        for (let j = 0; j < width; j++) {
          out += text[i + j];
          origin.push(i + j);
        }
      }
    }
    i += width;
  }
  origin.push(text.length);
  return { text: out, origin };
};

const lowered = (text) => {
  let out = '';
  for (let i = 0; i < text.length; i++) out += lowerUnit(text[i]);
  return { text: out, origin: Array.from({ length: text.length + 1 }, (_, i) => i) };
};

export const normalise = (text) => folded(text).text;

const matcher = (words, foldText) => {
  const keys = words.map((w) => [foldText(w.word).text, w]).filter(([key]) => key.length > 0)
    .sort((a, b) => b[0].length - a[0].length);
  return (text) => {
    if (!keys.length) return [];
    const { text: read, origin } = foldText(text);
    const hits = [];
    let at = 0;
    while (at < read.length) {
      const hit = keys.find(([key]) => read.startsWith(key, at));
      if (!hit) at++;
      else {
        hits.push([origin[at], origin[at + hit[0].length], hit[1]]);
        at += hit[0].length;
      }
    }
    return hits;
  };
};

export const wordList = (words, normalising = false) => {
  const matched = words.filter((w) => w.word.length > 0);
  const censoring = matcher(matched, normalising ? folded : lowered);
  const hiding = matcher(matched.filter((w) => w.hideMessage), folded);
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
