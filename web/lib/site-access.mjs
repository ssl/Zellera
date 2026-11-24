export const SWITCHES = Object.freeze({
  photos: Object.freeze({ key: 'site.photos.are_public', env: 'ZEL_WEB_PUBLIC_PHOTOS', on: false }),
  players: Object.freeze({ key: 'site.players.are_public', env: 'ZEL_WEB_PUBLIC_PROFILES', on: false }),
  api: Object.freeze({ key: 'site.api.is_public', env: 'ZEL_WEB_PUBLIC_API', on: false }),
  community: Object.freeze({ key: 'site.community.is_public', env: 'ZEL_WEB_PUBLIC_COMMUNITY', on: true }),
  staff: Object.freeze({ key: 'site.staff_page.is_shown', env: 'ZEL_WEB_STAFF_PAGE', on: true }),
  avatars: Object.freeze({ key: 'site.avatar_images.are_shown', env: 'ZEL_WEB_AVATAR_IMAGES', on: true }),
  guests: Object.freeze({ key: 'site.guest_accounts.are_allowed', env: 'ZEL_WEB_GUEST_ACCOUNTS', on: false }),
});

export const PLACE = Object.freeze({ key: 'site.place.word', word: 'game' });
const PLACE_WORD = /^[a-z]{2,20}$/;

export const placeForms = (word) => {
  const w = PLACE_WORD.test(String(word ?? '')) ? String(word) : PLACE.word;
  const plural = /(s|x|z|ch|sh)$/.test(w) ? `${w}es` : /[^aeiou]y$/.test(w) ? `${w.slice(0, -1)}ies` : `${w}s`;
  return { word: w, plural };
};

export const HELP_TICKETS = Object.freeze({ key: 'site.help_requests.mode', env: 'ZEL_WEB_HELP_TICKETS', mode: 'account', modes: Object.freeze(['account', 'anyone', 'off']) });

const EMAIL = Object.freeze({ key: 'site.email.mode', env: 'ZEL_WEB_EMAIL', mode: 'off', modes: Object.freeze(['off', 'optional', 'required']) });
const AVATARS = Object.freeze({ key: 'site.avatars.per_account', env: 'ZEL_WEB_AVATARS_PER_ACCOUNT', count: 1, max: 50 });

export const accountRules = (rawEmail, rawAvatars) => {
  const email = EMAIL.modes.includes(String(rawEmail ?? '')) ? String(rawEmail) : EMAIL.mode;
  const n = /^\d{1,3}$/.test(String(rawAvatars ?? '')) ? Number(rawAvatars) : AVATARS.count;
  const avatars = n >= 1 && n <= AVATARS.max && (n === 1 || email === 'required') ? n : AVATARS.count;
  return Object.freeze({ email, avatars });
};
export const DEFAULT_ACCOUNT_RULES = accountRules();

const EXTENSIONS_KEY = 'site.extensions';

const CACHE_MS = 2000;
const KEYS = [...Object.values(SWITCHES).map((s) => s.key), PLACE.key, HELP_TICKETS.key, EXTENSIONS_KEY, EMAIL.key, AVATARS.key];
const valueOf = (raw, fallback) => (raw === '1' ? true : raw === '0' ? false : fallback);

export const createSiteAccess = ({ query, cacheMs = CACHE_MS, now = Date.now }) => {
  let cached = null;
  let at = 0;
  const read = async () => {
    if (cached && now() - at < cacheMs) return cached;
    const rows = await query(`SELECT \`name\`, \`value\` FROM settings WHERE \`name\` IN (${KEYS.map(() => '?').join(', ')})`, KEYS);
    cached = new Map([...rows].map((r) => [String(r.name), String(r.value).trim()]));
    at = now();
    return cached;
  };
  return {
    on: async (name) => {
      const s = SWITCHES[name];
      if (!s) return false;
      try { return valueOf((await read()).get(s.key), s.on); } catch { return s.on; }
    },
    place: async () => {
      try { return placeForms((await read()).get(PLACE.key)); } catch { return placeForms(PLACE.word); }
    },
    helpTickets: async () => {
      try { return helpTicketsMode((await read()).get(HELP_TICKETS.key)); } catch { return HELP_TICKETS.mode; }
    },
    accounts: async () => {
      try {
        const rows = await read();
        return accountRules(rows.get(EMAIL.key), rows.get(AVATARS.key));
      } catch { return DEFAULT_ACCOUNT_RULES; }
    },
    extensions: async () => (await read()).get(EXTENSIONS_KEY),
  };
};

export const helpTicketsMode = (raw) => (HELP_TICKETS.modes.includes(String(raw ?? '')) ? String(raw) : HELP_TICKETS.mode);

export const DEFAULT_ACCESS = Object.freeze({
  on: async (name) => SWITCHES[name]?.on ?? false,
  place: async () => placeForms(PLACE.word),
  helpTickets: async () => HELP_TICKETS.mode,
  accounts: async () => DEFAULT_ACCOUNT_RULES,
  extensions: async () => undefined,
});

export const accountRulesOf = async (access) => (typeof access?.accounts === 'function' ? access.accounts() : DEFAULT_ACCOUNT_RULES);

export const helpTicketsOf = async (access) => (typeof access?.helpTickets === 'function' ? access.helpTickets() : HELP_TICKETS.mode);

export const placeOf = async (access) => (typeof access?.place === 'function' ? access.place() : placeForms(PLACE.word));
