// The names and ids the site shows for the currencies and the build pass: the currencies and display_names tables the
// content fills. Before they are read, and for a role the content leaves out, a name is its own code.
const FALLBACK = Object.freeze({ credits: 'credits', points: 'points', premium: 'premium', buildPass: 'build pass' });
const MONEY_COLUMN = -1;
const NAME_MAX = 40;
const REFRESH_MS = 60_000;

const clean = (value) => String(value ?? '').replace(/[\u0000-\u001f\u007f-\u009f<>"&]+/g, ' ').replace(/\s+/g, ' ').trim()
  .slice(0, NAME_MAX).trim();

let current = FALLBACK;
let ids = Object.freeze({ credits: MONEY_COLUMN, points: null, premium: null });

const capital = (s) => s.charAt(0).toUpperCase() + s.slice(1);
const single = (s) => s.replace(/s$/i, '');

const withCapitals = (n) => Object.freeze({
  ...n,
  Credits: capital(n.credits),
  credit: single(n.credits),
  Credit: capital(single(n.credits)),
  Points: capital(n.points),
  point: single(n.points),
  Premium: capital(n.premium),
});

/** credits, points and premium (in lower case), the same with a capital, credit and point (one of them), and buildPass. */
export const displayNames = () => withCapitals(current);

/** The currency ids by role: credits is the money column, points and premium are null until the content names them. */
export const currencyIds = () => ids;

/** [currencies] are rows of { id, name, role }, [names] rows of { code, name }. */
export const setDisplayNames = ({ currencies = [], names = [] } = {}) => {
  const byRole = new Map(currencies.map((c) => [String(c.role), c]));
  const named = (role) => (clean(byRole.get(role)?.name) || FALLBACK[role]).toLowerCase();
  current = Object.freeze({
    credits: named('credits'),
    points: named('points'),
    premium: named('premium'),
    buildPass: clean(names.find((n) => n.code === 'build_pass')?.name) || FALLBACK.buildPass,
  });
  const idOf = (role) => (byRole.has(role) ? Number(byRole.get(role).id) : null);
  ids = Object.freeze({ credits: idOf('credits') ?? MONEY_COLUMN, points: idOf('points'), premium: idOf('premium') });
};

export const startDisplayNames = async (read, { everyMs = REFRESH_MS } = {}) => {
  const refresh = async () => {
    try { setDisplayNames(await read()); } catch (e) { console.error(`web: the currency and display names were not read: ${e?.message ?? e}`); }
  };
  await refresh();
  const timer = setInterval(refresh, everyMs);
  timer.unref?.();
  return () => clearInterval(timer);
};

/** Fills {names.credits}, {names.points}, {names.premium}, their capitalised and singular forms and {names.buildPass} in a text. */
export const fillNames = (text, names = displayNames()) =>
  String(text).replace(/\{names\.(credits|Credits|credit|Credit|points|Points|point|premium|Premium|buildPass)\}/g, (all, key) => names[key] ?? all);
