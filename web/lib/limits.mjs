import { BlockList, isIP } from 'node:net';

export const isLoopbackIp = (ip) => ip === '127.0.0.1' || ip === '::1' || ip === '::ffff:127.0.0.1';

const family = (ip) => (isIP(ip) === 6 ? 'ipv6' : 'ipv4');

export const trustedProxies = (list) => {
  const block = new BlockList();
  const prefixes = [];
  for (const entry of String(list ?? '').split(',').map((s) => s.trim()).filter(Boolean)) {
    const [address, bits, ...rest] = entry.split('/');
    if (entry.endsWith('.') || entry.endsWith(':')) prefixes.push(entry);
    else if (isIP(address) && bits === undefined) block.addAddress(address, family(address));
    else if (isIP(address) && !rest.length && /^\d{1,3}$/.test(bits) && Number(bits) <= (isIP(address) === 6 ? 128 : 32)) block.addSubnet(address, Number(bits), family(address));
    else throw new Error(`ZEL_TRUSTED_PROXIES: ${JSON.stringify(entry)} is not an address, a CIDR block or a prefix ending in . or :`);
  }
  return (ip) => isLoopbackIp(ip) || (isIP(ip) !== 0 && block.check(ip, family(ip))) || prefixes.some((p) => ip.startsWith(p));
};

export const createClientIp = ({ trusted = '127.0.0.1', header = 'X-Forwarded-For' } = {}) => {
  const isTrusted = trustedProxies(trusted);
  const name = header.toLowerCase();
  return (req) => {
    const remote = String(req.socket?.remoteAddress ?? '').replace(/^::ffff:/, '');
    const forwarded = String(req.headers[name] ?? '').split(',')[0].trim();
    return (isTrusted(remote) && isIP(forwarded) ? forwarded : remote).replace(/^::ffff:/, '').slice(0, 45);
  };
};

export const clientIp = createClientIp({
  trusted: process.env.ZEL_TRUSTED_PROXIES || '127.0.0.1', header: process.env.ZEL_CLIENT_IP_HEADER || 'X-Forwarded-For',
});

export const ipLimitKey = (ip) => {
  const s = String(ip ?? '');
  if (isIP(s) !== 6) return s;
  let addr = s.split('%')[0].toLowerCase();
  const v4 = /(\d+)\.(\d+)\.(\d+)\.(\d+)$/.exec(addr);
  if (v4) {
    const [a, b, c, d] = v4.slice(1).map(Number);
    addr = `${addr.slice(0, v4.index)}${((a << 8) | b).toString(16)}:${((c << 8) | d).toString(16)}`;
  }
  const double = addr.indexOf('::');
  const head = (double < 0 ? addr : addr.slice(0, double)).split(':').filter((g) => g !== '');
  const tail = double < 0 ? [] : addr.slice(double + 2).split(':').filter((g) => g !== '');
  const groups = double < 0 ? head : [...head, ...Array(8 - head.length - tail.length).fill('0'), ...tail];
  const nums = groups.map((g) => parseInt(g, 16));
  if (nums.slice(0, 5).every((g) => g === 0) && nums[5] === 0xffff) {
    return `${nums[6] >> 8}.${nums[6] & 255}.${nums[7] >> 8}.${nums[7] & 255}`;
  }
  return `${nums.slice(0, 4).map((g) => g.toString(16)).join(':')}::/64`;
};

export const createGate = (max, maxQueue) => {
  let running = 0;
  const waiting = [];
  const next = () => {
    if (running >= max || waiting.length === 0) return;
    running += 1;
    const { job, resolve, reject } = waiting.shift();
    Promise.resolve().then(job).then(resolve, reject).finally(() => { running -= 1; next(); });
  };
  const run = (job) => new Promise((resolve, reject) => {
    if (running >= max && waiting.length >= maxQueue) {
      reject(Object.assign(new Error('busy'), { code: 'EBUSY' }));
      return;
    }
    waiting.push({ job, resolve, reject });
    next();
  });
  return { run, get running() { return running; }, get queued() { return waiting.length; } };
};

export const createLimiter = ({ now = () => Date.now(), maxKeys = 100_000, sweepEveryMs = 1_000 } = {}) => {
  maxKeys = Math.max(1, maxKeys);
  const windows = new Map();
  let lru = null;
  const evictOldest = () => {
    let r = lru?.next();
    if (!r || r.done) { lru = windows.keys(); r = lru.next(); }
    if (!r.done) windows.delete(r.value);
  };
  let lastSweep = -Infinity;
  const makeRoom = (t) => {
    if (t - lastSweep >= sweepEveryMs || t < lastSweep) {
      lastSweep = t;
      for (const [k, list] of windows) if (list.every((w) => t - w.start >= w.windowMs)) windows.delete(k);
    }
    while (windows.size >= maxKeys) evictOldest();
  };
  const take = (key, rules, cost = 1) => {
    const t = now();
    let list = windows.get(key);
    if (list) windows.delete(key);
    if (!list || list.length !== rules.length) {
      if (windows.size >= maxKeys) makeRoom(t);
      list = rules.map((r) => ({ start: t, count: 0, windowMs: r.windowMs }));
    }
    windows.set(key, list);
    let wait = 0;
    rules.forEach((r, i) => {
      const w = list[i];
      if (t - w.start >= r.windowMs) { w.start = t; w.count = 0; }
      if (w.count + cost > r.max) wait = Math.max(wait, w.start + r.windowMs - t);
    });
    if (wait > 0) return { ok: false, retryAfter: Math.max(1, Math.ceil(wait / 1000)) };
    for (const w of list) w.count += cost;
    return { ok: true };
  };
  return {
    take,
    hit: (key, max, windowSec) => take(key, [{ windowMs: windowSec * 1000, max }]),
    count: (key) => {
      const w = windows.get(key)?.[0];
      return w && now() - w.start < w.windowMs ? w.count : 0;
    },
    reset: (key) => { windows.delete(key); },
    size: () => windows.size,
  };
};
