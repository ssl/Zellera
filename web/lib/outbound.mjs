import { clientString, clientStringsVersion } from './client-strings.mjs';

const hostOf = (SITE) => new URL(SITE).host.toLowerCase();

export const applyOverrides = (text, overridesText, SITE) => {
  const lines = text.split(/\r?\n/);
  const index = new Map(lines.map((l, i) => [l.slice(0, l.indexOf('=')), i]).filter(([k]) => k));
  for (const raw of String(overridesText ?? '').split(/\r?\n/)) {
    if (!raw || raw.startsWith('#') || !raw.includes('=')) continue;
    if (raw.includes('${ZEL_LANDING_BACKDROP}') && !process.env.ZEL_LANDING_BACKDROP) continue;
    if (raw.includes('${ZEL_SERVER_ID}') && !clientString('client.serverId')) continue;
    const line = raw.replaceAll('${ZEL_SITE}', SITE).replaceAll('${ZEL_LANDING_BACKDROP}', process.env.ZEL_LANDING_BACKDROP ?? '')
      .replaceAll('${ZEL_SERVER_ID}', clientString('client.serverId') ?? '');
    const key = line.slice(0, line.indexOf('='));
    if (index.has(key)) lines[index.get(key)] = line; else lines.push(line);
  }
  return lines.join('\n');
};

const escapeRegExp = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const VALUE_TAIL = String.raw`[^\s,;"'<|]*`;
// The linked sites come as a pattern of host names. Only these pieces are taken: letters, digits and -, an escaped dot,
// a class of those with +, a {n,m} count, (?: ... ) with an optional ? and |. Anything else is no host pattern.
const HOST_PATTERN = /^(?:[a-z0-9-]|\\\.|\[[a-z0-9.-]+\](?:\+|\{\d,?\d?\})?|\(\?:|\)\??|\|)+$/i;
const HOST_PATTERN_MAX = 200;
export const hostPattern = (value) => {
  const s = String(value ?? '');
  if (!s || s.length > HOST_PATTERN_MAX || !HOST_PATTERN.test(s)) return null;
  try { new RegExp(s); } catch { return null; }
  return s;
};
let patterns = { version: null };
const packPatterns = () => {
  if (patterns.version === clientStringsVersion()) return patterns;
  const host = clientString('asset.host');
  const given = clientString('site.linkedSites');
  const sites = hostPattern(given);
  if (given && !sites) console.error('web: the client string site.linkedSites is not a pattern of host names; shop and article links are left as they are');
  const site = sites ? `https?://(?:${sites})` : null;
  patterns = {
    version: clientStringsVersion(),
    asset: host ? new RegExp(`(?:https?:)?//${escapeRegExp(host)}/`, 'g') : null,
    shop: site ? new RegExp(`${site}/shop${VALUE_TAIL}`, 'gi') : null,
    article: site ? new RegExp(`${site}/community/article/${VALUE_TAIL}`, 'gi') : null,
  };
  return patterns;
};

export const rewriteAssetUrls = (text, SITE) => {
  const { asset } = packPatterns();
  return asset ? text.replace(asset, `${SITE}/assets/`) : text;
};

export const rewriteShopLinks = (text, SITE) => {
  const { shop } = packPatterns();
  return shop ? text.replace(shop, `${SITE}/shop`) : text;
};
export const rewriteArticleLinks = (text, SITE) => {
  const { article } = packPatterns();
  return article ? text.replace(article, `${SITE}/news`) : text;
};

const TEXT_URL_RE = /https?:\/\/([^\s/?#"'<\]|\\)]+)[^\s"'<\]|\\)]*/gi;
export const rewriteForeignToHelp = (text, SITE) => {
  const ours = hostOf(SITE);
  return text.replace(TEXT_URL_RE, (url, authority) => (authority.toLowerCase() === ours ? url : `${SITE}/help`));
};

export const clearForeignAdUrls = (data, SITE) => {
  let cleared = 0;
  for (const section of ['roomitemtypes', 'wallitemtypes']) {
    for (const f of data?.[section]?.furnitype ?? []) {
      if (typeof f.adurl === 'string' && f.adurl !== '' && !f.adurl.startsWith(`${SITE}/`)) {
        f.adurl = '';
        cleared++;
      }
    }
  }
  return cleared;
};

export const rewriteVariables = (text, SITE) => rewriteArticleLinks(rewriteShopLinks(text, SITE), SITE);

export const rewriteTexts = (text, SITE) =>
  rewriteForeignToHelp(rewriteArticleLinks(rewriteShopLinks(rewriteAssetUrls(text, SITE), SITE), SITE), SITE);

