import { clientIp, createLimiter, ipLimitKey, isLoopbackIp } from '../lib/limits.mjs';
import { DEFAULT_ACCOUNT_RULES } from '../lib/site-access.mjs';
import { BETA_CODE, BETA_MODE, PROD } from './config.mjs';
import * as dbModule from './db.mjs';
import { gameRules } from '../lib/game-rules.mjs';
import { fmtDateTime } from './lib/html.mjs';
import { originOk, readBody, sameSiteFetch } from './lib/http.mjs';
import { SITE_API } from '../site-api/index.mjs';
import { PERMANENT_FROM } from '../hk-api/users-shared.mjs';
import { randomBytes, randomInt } from 'node:crypto';
import {
  GUEST_SESSION_TTL, hashIsOutdated, hashPassword, newSession, newTicket, readSession, safeEqual, sessionMatchesUser, verifyPassword,
} from './auth.mjs';

const usernameRule = () => {
  const { 'player.name.min_length': min, 'player.name.max_length': max, 'player.name.punctuation': punctuation } = gameRules();
  const escaped = punctuation.replace(/[\\\]^-]/g, '\\$&');
  return { min, max, punctuation, re: new RegExp(`^[A-Za-z0-9${escaped}]{${min},${max}}$`) };
};
const TICKET_SHAPE = /^[\x21-\x7e]{1,256}$/;
const EMAIL_RE = /^[^\s@<>()[\]\\,;:"]{1,64}@[^\s@<>()[\]\\,;:"]+\.[^\s@<>()[\]\\,;:".]{2,}$/;
const EMAIL_MAX = 254;
const PASSWORD_MIN = 8;
// One answer whichever of the two is taken, so registering does not say whose e-mail address has an account.
const NOT_AVAILABLE = 'That username or e-mail address can not be used. Try another username, or log in if you have an account already.';
const EMAIL_NOT_AVAILABLE = 'That e-mail address can not be used. Enter another one.';
const NAME_UNFIT = 'That name can not be used. Choose another one.';

export const LOGIN_CLIENT_LIMIT = Object.freeze([10, 600]);
const LOGIN_SLOWDOWN = Object.freeze({ failures: 50, windowSec: 600, delayMs: 2000 });
const AVATAR_LIMIT = Object.freeze([20, 600]);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const banNotice = ({ reason, expires_at: end, own }) => {
  const until = Number(end) >= PERMANENT_FROM ? 'permanently' : `until ${fmtDateTime(end)}`;
  return `${own ? 'This account' : 'This connection'} is banned ${until}.${own && reason ? ` Reason: ${reason}` : ''}`;
};

export const emailOf = (typed) => {
  const v = String(typed ?? '').trim();
  if (!v) return { email: null };
  if (v.length > EMAIL_MAX || !EMAIL_RE.test(v)) return { problem: 'That does not look like an e-mail address (like name@example.com).' };
  return { email: v };
};

export const createCms = ({
  SITE, db = dbModule, loginSlowdown = LOGIN_SLOWDOWN, limiter = createLimiter(), accountRules = async () => DEFAULT_ACCOUNT_RULES,
  guestsOn = async () => false,
}) => {

  const isHttps = (req) => PROD || req.headers['x-forwarded-proto'] === 'https';

  const cookies = (req) => Object.fromEntries(String(req.headers.cookie ?? '').split(';')
    .map((c) => c.trim().split('=')).filter(([k, v]) => k && v !== undefined).map(([k, ...v]) => [k, v.join('=')]));
  const sessionCookie = (req, value, maxAge) => {
    const name = isHttps(req) ? '__Host-zel_session' : 'zel_session';
    return `${name}=${value}; Path=/; Max-Age=${maxAge}; HttpOnly; SameSite=Lax${isHttps(req) ? '; Secure' : ''}`;
  };
  const loginCookie = (req, account, avatarId, ttl) => {
    const s = newSession({ accountId: account.id, avatarId, password: account.password, ttl });
    return sessionCookie(req, s.value, s.maxAge);
  };

  const sessionValue = (req) => { const c = cookies(req); return isHttps(req) ? c['__Host-zel_session'] : c.zel_session; };

  const currentAccount = async (req) => {
    const session = readSession(sessionValue(req));
    if (!session) return null;
    const account = await db.sessionAccount(session.accountId, session.nonce);
    if (!sessionMatchesUser(session, account)) return null;
    const user = session.avatarId > 0 ? await db.accountAvatar(account.id, session.avatarId) : null;
    if (user && await db.activeBan(user.id, clientIp(req))) return null;
    return { session, account, user: user && { ...user, password: account.password } };
  };

  const currentUser = async (req) => {
    const me = await currentAccount(req);
    return me?.user ? { session: me.session, user: me.user, account: me.account } : null;
  };

  const endedByBan = async (req) => {
    const session = readSession(sessionValue(req));
    if (!session || !(session.avatarId > 0)) return null;
    const ban = await db.sessionBan(session.accountId, session.avatarId, session.nonce);
    if (ban) return sessionMatchesUser(session, ban) ? banNotice(ban) : null;
    const account = await db.sessionAccount(session.accountId, session.nonce);
    if (!sessionMatchesUser(session, account)) return null;
    const user = await db.accountAvatar(account.id, session.avatarId);
    const barred = user && await db.activeBan(user.id, clientIp(req));
    return barred ? banNotice(barred) : null;
  };

  const sessionEnd = async (req) => {
    if (sessionValue(req) === undefined) return null;
    const me = await currentAccount(req);
    if (me?.user || (me && (await accountRules()).avatars > 1)) return null;
    return { cookie: sessionCookie(req, '', 0), notice: await endedByBan(req) };
  };

  const json = (res, status, body, headers = {}) => {
    const s = JSON.stringify(body);
    res.writeHead(status, {
      'content-type': 'application/json; charset=utf-8', 'content-length': Buffer.byteLength(s),
      'cache-control': 'no-store', 'x-content-type-options': 'nosniff', ...headers,
    });
    res.end(s);
  };
  const fail = (res, status, error, field, headers) => json(res, status, { ok: false, error, ...(field ? { field } : {}) }, headers);
  const sessionOver = (res) => fail(res, 401, 'Your session ended. Log in again.');

  const readJson = async (req) => {
    const body = await readBody(req, 4096);
    if (!body) return null;
    try {
      const v = JSON.parse(body.toString('utf8') || '{}');
      return v && typeof v === 'object' && !Array.isArray(v) ? v : null;
    } catch { return null; }
  };
  const str = (v, max = 256) => (typeof v === 'string' ? v.slice(0, max) : '');

  const limited = (res, key, max, windowSec) => {
    const r = limiter.hit(key, max, windowSec);
    if (r.ok) return false;
    fail(res, 429, 'Too many attempts. Wait a moment and try again.', null, { 'retry-after': String(r.retryAfter) });
    return true;
  };

  const newAvatar = async (username, ip) => {
    const cfg = await db.settings();
    return {
      username, ip, look: gameRules()['new_player.look'], gender: 'M', motto: cfg['site.registration.motto'],
      credits: gameRules()['new_player.credits'], homeRoom: Math.max(0, Number(cfg['site.registration.home_room_id']) || 0),
    };
  };
  const usernameProblem = (username) => {
    const { min, max, punctuation, re } = usernameRule();
    return re.test(username) ? null : `Names are ${min} to ${max} characters: letters, numbers and ${[...punctuation].join(' ')}`;
  };

  const login = async (req, res, body) => {
    const rules = await accountRules();
    const many = rules.avatars > 1;
    const loginName = str(body.login).trim();
    const password = str(body.password, 128);
    if (limited(res, `login:ip:${ipLimitKey(clientIp(req))}`, ...LOGIN_CLIENT_LIMIT)) return;
    const what = many ? 'your e-mail address' : rules.email === 'off' ? 'your username' : 'your username or e-mail address';
    if (!loginName || !password) return fail(res, 400, `Enter ${what} and your password.`, loginName ? 'password' : 'login');
    const byEmail = rules.email !== 'off' && loginName.includes('@') ? await db.accountByEmail(loginName) : null;
    const candidates = [
      byEmail && (many ? byEmail : { ...byEmail, avatarId: await db.firstAvatar(byEmail.id) }),
      many ? null : await db.accountByAvatarName(loginName),
    ].filter(Boolean);
    const nameKey = `login:fail:${candidates[0] ? `id:${candidates[0].id}` : `name:${loginName.toLowerCase()}`}`;
    if (limiter.count(nameKey) >= loginSlowdown.failures) await sleep(loginSlowdown.delayMs);
    let account = null;
    for (const candidate of candidates.length ? candidates : [null]) {
      if (await verifyPassword(password, candidate?.password ?? null)) { account = candidate; break; }
    }
    if (!account) {
      limiter.hit(nameKey, loginSlowdown.failures, loginSlowdown.windowSec);
      return fail(res, 401, `That ${many ? 'e-mail address' : 'name'} or password is not right. Check both and try again.`, 'password');
    }
    limiter.reset(nameKey);
    if (hashIsOutdated(account.password)) {
      try {
        const hash = await hashPassword(password);
        await db.setPassword(account.id, hash);
        account = { ...account, password: hash };
      } catch (e) {
        console.error(`web: a password hash was not renewed at login: ${e?.message ?? e}`);
      }
    }
    if (many) {
      const ban = await db.addressBan(clientIp(req));
      if (ban) return fail(res, 403, banNotice(ban));
      return json(res, 200, { ok: true, next: '/avatars' }, { 'set-cookie': loginCookie(req, account, 0) });
    }
    if (!(account.avatarId > 0)) return fail(res, 403, 'This account has no avatar.');
    const ban = await db.activeBan(account.avatarId, clientIp(req));
    if (ban) return fail(res, 403, banNotice(ban));
    return json(res, 200, { ok: true, next: '/me' }, { 'set-cookie': loginCookie(req, account, account.avatarId) });
  };

  const newPasswordProblem = (password, password2) => {
    if (password.length < PASSWORD_MIN) return ['password', `Use at least ${PASSWORD_MIN} characters for your password.`];
    if (password.length > 128) return ['password', 'Use at most 128 characters for your password.'];
    if (password !== password2) return ['password2', 'The two passwords do not match.'];
    return null;
  };

  const register = async (req, res, body) => {
    const rules = await accountRules();
    const ip = clientIp(req);
    if (limited(res, `register:ip:${ipLimitKey(ip)}`, 5, 3600)) return;
    const ban = await db.addressBan(ip);
    if (ban) return fail(res, 403, banNotice(ban));
    const username = str(body.username, 64).trim();
    const password = str(body.password, 256);
    const password2 = str(body.password2, 256);
    if (BETA_MODE) {
      if (!BETA_CODE) return fail(res, 403, 'Registration is closed during the beta.', 'beta');
      if (!safeEqual(str(body.beta, 128).trim(), BETA_CODE)) return fail(res, 403, 'That beta code is not valid.', 'beta');
    }
    let email = null;
    if (rules.email !== 'off') {
      const typed = emailOf(str(body.email, 512));
      if (typed.problem) return fail(res, 400, typed.problem, 'email');
      if (!typed.email && rules.email === 'required') return fail(res, 400, 'Enter your e-mail address: you log in with it.', 'email');
      email = typed.email;
    }
    const nameProblem = usernameProblem(username);
    if (nameProblem) return fail(res, 400, nameProblem, 'username');
    if (!(await db.nameAllowed(username))) return fail(res, 400, NAME_UNFIT, 'username');
    const weak = newPasswordProblem(password, password2);
    if (weak) return fail(res, 400, weak[1], weak[0]);

    const cfg = await db.settings();
    const maxPerIp = Number(cfg['site.registration.accounts_per_address']) || 0;
    if (maxPerIp > 0 && !isLoopbackIp(ip) && await db.accountsForIp(ip) >= maxPerIp) {
      return fail(res, 429, `This connection already has ${maxPerIp} accounts.`);
    }
    const hash = await hashPassword(password);
    const avatar = await newAvatar(username, ip);
    let made;
    try {
      made = await db.withRegisterLock(async () => {
        if (await db.usernameTaken(username)) return 'username';
        if (email && await db.emailTaken(email)) return 'email';
        return db.createUser({ password: hash, mail: email, ...avatar });
      });
    } catch (e) {
      const key = dbModule.duplicateKey(e);
      if (key === null) throw e;
      made = key === dbModule.EMAIL_KEY ? 'email' : 'username';
    }
    if (made === 'username' && !email) return fail(res, 409, 'That username is taken. Try another one.', 'username');
    if (made === 'username' || made === 'email') return fail(res, 409, NOT_AVAILABLE);
    return json(res, 201, { ok: true, next: '/me' }, {
      'set-cookie': loginCookie(req, { id: made.accountId, password: hash }, made.userId),
    });
  };

  // A guest is an account nobody typed anything for: a numbered name, a password nobody knows, one free rename in the
  // game, and a year-long session cookie that is the only way back in.
  const guest = async (req, res) => {
    if (!(await guestsOn())) return fail(res, 403, 'Guest accounts are not available here.');
    if (BETA_MODE) return fail(res, 403, 'Registration is closed during the beta.');
    const ip = clientIp(req);
    if (limited(res, `register:ip:${ipLimitKey(ip)}`, 5, 3600)) return;
    const ban = await db.addressBan(ip);
    if (ban) return fail(res, 403, banNotice(ban));
    const cfg = await db.settings();
    const maxPerIp = Number(cfg['site.registration.accounts_per_address']) || 0;
    if (maxPerIp > 0 && !isLoopbackIp(ip) && await db.accountsForIp(ip) >= maxPerIp) {
      return fail(res, 429, `This connection already has ${maxPerIp} accounts.`);
    }
    const password = randomBytes(24).toString('base64url');
    const hash = await hashPassword(password);
    let made = null;
    let username = '';
    for (let tries = 0; tries < 8 && !made; tries++) {
      username = `Guest${randomInt(1000, 1000000)}`;
      if (usernameProblem(username)) return fail(res, 503, 'Guest accounts are not possible with these name rules.');
      const avatar = await newAvatar(username, ip);
      try {
        made = await db.withRegisterLock(async () => (await db.usernameTaken(username)
          ? null : db.createUser({ password: hash, mail: null, mayChangeName: true, ...avatar })));
      } catch (e) {
        if (dbModule.duplicateKey(e) === null) throw e;
      }
    }
    if (!made) return fail(res, 503, 'Could not make a guest account. Try again.');
    return json(res, 201, { ok: true, username, password }, {
      'set-cookie': loginCookie(req, { id: made.accountId, password: hash }, made.userId, GUEST_SESSION_TTL),
    });
  };

  const logout = async (req, res) => {
    const me = await currentAccount(req);
    if (me) await db.revokeSession(me.session.nonce, me.session.exp);
    return json(res, 200, { ok: true }, { 'set-cookie': sessionCookie(req, '', 0) });
  };

  const ssoTicket = async (req, res) => {
    const me = await currentUser(req);
    if (!me) return sessionOver(res);
    if (limited(res, `ticket:${me.user.id}`, 6, 60)) return;
    const ban = await db.activeBan(me.user.id, clientIp(req)) ?? await db.accountBan(me.account.id);
    if (ban) return fail(res, 403, banNotice(ban));
    const ticket = newTicket();
    await db.issueTicket(me.user.id, ticket, clientIp(req));
    return json(res, 200, { ok: true, ticket });
  };

  const changePassword = async (req, res, body) => {
    const me = await currentAccount(req);
    if (!me) return sessionOver(res);
    if (limited(res, `password:${me.account.id}`, ...LOGIN_CLIENT_LIMIT)) return;
    const current = str(body.current, 128);
    if (!current) return fail(res, 400, 'Enter your current password.', 'current');
    if (!(await verifyPassword(current, me.account.password))) return fail(res, 401, 'That is not your current password.', 'current');
    const password = str(body.password, 256);
    const weak = newPasswordProblem(password, str(body.password2, 256));
    if (weak) return fail(res, 400, weak[1], weak[0]);
    const hash = await hashPassword(password);
    await db.setPassword(me.account.id, hash);
    return json(res, 200, { ok: true }, { 'set-cookie': loginCookie(req, { id: me.account.id, password: hash }, me.session.avatarId) });
  };

  const changeEmail = async (req, res, body) => {
    const rules = await accountRules();
    if (rules.email === 'off') return fail(res, 404, 'This site does not use e-mail addresses.');
    const me = await currentAccount(req);
    if (!me) return sessionOver(res);
    if (limited(res, `password:${me.account.id}`, ...LOGIN_CLIENT_LIMIT)) return;
    const typed = emailOf(str(body.email, 512));
    if (typed.problem) return fail(res, 400, typed.problem, 'email');
    if (!typed.email && rules.email === 'required') return fail(res, 400, 'Enter an e-mail address: you log in with it.', 'email');
    const current = str(body.current, 128);
    if (!current) return fail(res, 400, 'Enter your current password.', 'current');
    if (!(await verifyPassword(current, me.account.password))) return fail(res, 401, 'That is not your current password.', 'current');
    if ((typed.email ?? '').toLowerCase() === String(me.account.mail ?? '').toLowerCase()) return json(res, 200, { ok: true, email: me.account.mail ?? null });
    try {
      await db.setEmail(me.account.id, typed.email);
    } catch (e) {
      if (e.errno === 1062) return fail(res, 409, EMAIL_NOT_AVAILABLE, 'email');
      throw e;
    }
    return json(res, 200, { ok: true, email: typed.email });
  };

  const accountFacts = async (req, res) => {
    const me = await currentAccount(req);
    if (!me) return sessionOver(res);
    const rules = await accountRules();
    return json(res, 200, {
      ok: true, rules, email: rules.email === 'off' ? null : me.account.mail ?? null,
      avatars: { count: await db.avatarCount(me.account.id), max: rules.avatars },
    });
  };

  const avatarAnswer = (a, chosen) => {
    const banned = a.ban_end != null;
    return {
      id: Number(a.id), name: String(a.name), figure: String(a.figure ?? ''), gender: String(a.gender ?? 'M'), motto: String(a.motto ?? ''),
      memberSince: Number(a.created_at) || 0, lastOnline: Number(a.last_online) || 0, current: Number(a.id) === chosen,
      banned, ban: banned ? { reason: String(a.ban_reason ?? ''), until: Number(a.ban_end) >= PERMANENT_FROM ? null : Number(a.ban_end) } : null,
    };
  };

  const manyAvatars = async (req, res) => {
    const rules = await accountRules();
    if (rules.avatars <= 1) { fail(res, 404, 'One account holds one avatar here.'); return null; }
    const me = await currentAccount(req);
    if (!me) { sessionOver(res); return null; }
    return { me, rules };
  };

  const listAvatars = async (req, res) => {
    const got = await manyAvatars(req, res);
    if (!got) return;
    const rows = await db.avatarsOf(got.me.account.id);
    const chosen = got.me.user ? Number(got.me.user.id) : 0;
    return json(res, 200, {
      ok: true, max: got.rules.avatars, canCreate: rows.length < got.rules.avatars, avatars: rows.map((a) => avatarAnswer(a, chosen)),
    });
  };

  const selectAvatar = async (req, res, body) => {
    const got = await manyAvatars(req, res);
    if (!got) return;
    const { me } = got;
    if (limited(res, `avatar:${me.account.id}`, ...AVATAR_LIMIT)) return;
    const id = Number.isInteger(body.id) && body.id > 0 ? body.id : 0;
    const row = (await db.avatarsOf(me.account.id)).find((a) => Number(a.id) === id);
    if (!row) return fail(res, 404, 'That avatar is not one of yours.', 'id');
    const ban = await db.activeBan(id, clientIp(req));
    if (ban) return fail(res, 403, banNotice(ban), 'id');
    await db.revokeSession(me.session.nonce, me.session.exp);
    return json(res, 200, { ok: true, next: '/me' }, { 'set-cookie': loginCookie(req, me.account, id) });
  };

  const checkName = async (req, res, body) => {
    const got = await manyAvatars(req, res);
    if (!got) return;
    if (limited(res, `avatar-name:${got.me.account.id}`, 60, 600)) return;
    const name = str(body.name, 64).trim();
    const problem = usernameProblem(name);
    if (problem) return json(res, 200, { ok: true, available: false, reason: problem });
    if (!(await db.nameAllowed(name))) return json(res, 200, { ok: true, available: false, reason: NAME_UNFIT });
    const taken = await db.usernameTaken(name);
    return json(res, 200, { ok: true, available: !taken, reason: taken ? 'That name is taken. Try another one.' : null });
  };

  const createAvatar = async (req, res, body) => {
    const got = await manyAvatars(req, res);
    if (!got) return;
    const { me, rules } = got;
    if (limited(res, `avatar:${me.account.id}`, ...AVATAR_LIMIT)) return;
    const name = str(body.name, 64).trim();
    const problem = usernameProblem(name);
    if (problem) return fail(res, 400, problem, 'name');
    if (!(await db.nameAllowed(name))) return fail(res, 400, NAME_UNFIT, 'name');
    const ban = await db.addressBan(clientIp(req)) ?? await db.accountBan(me.account.id);
    if (ban) return fail(res, 403, banNotice(ban));
    const avatar = await newAvatar(name, clientIp(req));
    let id;
    try {
      id = await db.withRegisterLock(async () => {
        if (await db.avatarCount(me.account.id) >= rules.avatars) return 'full';
        if (await db.usernameTaken(name)) return 'name';
        return db.createAvatar(me.account.id, avatar);
      });
    } catch (e) {
      if (e.errno !== 1062) throw e;
      id = 'name';
    }
    if (id === 'full') return fail(res, 409, `An account holds at most ${rules.avatars} avatars.`);
    if (id === 'name') return fail(res, 409, 'That name is taken. Try another one.', 'name');
    await db.revokeSession(me.session.nonce, me.session.exp);
    return json(res, 201, { ok: true, id, next: '/me' }, { 'set-cookie': loginCookie(req, me.account, id) });
  };

  const API = {
    [`${SITE_API}/login`]: login, [`${SITE_API}/register`]: register, [`${SITE_API}/guest`]: guest, [`${SITE_API}/logout`]: logout,
    [`${SITE_API}/me/ticket`]: ssoTicket, [`${SITE_API}/me/password`]: changePassword, [`${SITE_API}/me/email`]: changeEmail,
    [`${SITE_API}/avatars`]: createAvatar, [`${SITE_API}/avatars/select`]: selectAvatar, [`${SITE_API}/avatars/check-name`]: checkName,
  };
  const READS = { [`${SITE_API}/avatars`]: listAvatars, [`${SITE_API}/me/account`]: accountFacts };

  const guarded = async (res, path, run) => {
    try {
      await run();
    } catch (e) {
      if (e?.code === 'EBUSY') {
        if (!res.headersSent) fail(res, 503, 'Too many people are logging in right now. Try again in a moment.', null, { 'retry-after': '2' });
        return;
      }
      console.error(`cms ${path}: ${e.stack ?? e}`);
      if (!res.headersSent) fail(res, 503, 'Something went wrong on our side. Try again in a moment.');
    }
  };

  const handle = async (req, res, path) => {
    if (req.method === 'GET' && READS[path]) {
      await guarded(res, path, () => READS[path](req, res));
      return true;
    }
    if (req.method === 'POST' && API[path]) {
      if (!/^application\/json(;|$)/i.test(String(req.headers['content-type'] ?? ''))) return fail(res, 415, 'JSON only.'), true;
      if (!originOk(req, SITE, PROD) || !sameSiteFetch(req)) return fail(res, 403, 'Cross-site request refused.'), true;
      const body = await readJson(req);
      if (!body) return fail(res, 400, 'Bad request.'), true;
      await guarded(res, path, () => API[path](req, res, body));
      return true;
    }
    return false;
  };

  const checkPlayTicket = async (ticket) => TICKET_SHAPE.test(ticket) && db.ticketValid(ticket);
  const bindPlayDevice = (ticket, device) => db.bindTicketDevice(ticket, device);
  // A reload of the game page is a plain GET /play (the page drops its POST from the history): a logged-in player
  // gets a fresh ticket, under the same rules as the site's ticket button; null sends them to /me.
  const freshPlayTicket = async (req) => {
    const me = await currentUser(req);
    if (!me || !limiter.hit(`ticket:${me.user.id}`, 6, 60).ok) return null;
    if (await db.activeBan(me.user.id, clientIp(req)) || await db.accountBan(me.account.id)) return null;
    const ticket = newTicket();
    await db.issueTicket(me.user.id, ticket, clientIp(req));
    return ticket;
  };

  return { handle, checkPlayTicket, bindPlayDevice, freshPlayTicket, currentAccount, currentUser, endedByBan, sessionEnd };
};
