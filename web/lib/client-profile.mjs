import { clientString, clientStringsVersion } from './client-strings.mjs';

const REQUIRED = Object.freeze([
  'client.serverId', 'asset.roots', 'asset.imageRoot', 'asset.furniPath', 'asset.catalogIconPath',
  'asset.bundleExtension', 'asset.bundleMagic',
]);

export const PROFILE_MISSING = 'client profile missing on this server: it comes with a content pack (./zel import <pack.zip>)';

let missingLogged = '';
export const missingProfile = () => {
  const missing = REQUIRED.filter((name) => !clientString(name));
  const key = missing.join(',');
  if (key && key !== missingLogged) console.error(`client profile values missing: ${key} (they come with a content pack: ./zel import <pack.zip>)`);
  missingLogged = key;
  return missing;
};

/** A value worked out from the profile, kept until the profile is read again. */
export const fromProfile = (build) => {
  let version = null, value = null;
  return () => {
    if (version !== clientStringsVersion()) { version = clientStringsVersion(); value = build(); }
    return value;
  };
};

export const escapeRegExp = (text) => String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

export const serverId = () => clientString('client.serverId') ?? null;
export const assetRoots = fromProfile(() => [...(clientString('asset.roots') ?? '').split(','), clientString('asset.newsRoot')]
  .map((root) => (root ?? '').trim()).filter(Boolean));
export const imageRoot = () => clientString('asset.imageRoot') ?? null;
export const furniPath = () => clientString('asset.furniPath') ?? null;
export const catalogIconPath = () => clientString('asset.catalogIconPath') ?? null;
export const badgeImagePath = () => (imageRoot() && clientString('asset.badgeFolder') ? `${imageRoot()}/${clientString('asset.badgeFolder')}` : null);
export const bundleExtension = () => clientString('asset.bundleExtension') ?? null;
export const bundleMagic = () => clientString('asset.bundleMagic') ?? null;
