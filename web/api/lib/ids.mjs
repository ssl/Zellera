import { createCipheriv, createDecipheriv } from 'node:crypto';
import { config, idKey } from './config.mjs';

const MAGIC = Buffer.from('zel1', 'latin1');
const KINDS = { user: 0x75, room: 0x72, group: 0x67 };
const prefixOf = (kind) => ({ user: `${config.serverId}-`, room: `r-${config.serverId}-`, group: `g-${config.serverId}-` }[kind]);

const encrypt = (block) => {
  const c = createCipheriv('aes-128-ecb', idKey(), null);
  c.setAutoPadding(false);
  return Buffer.concat([c.update(block), c.final()]);
};
const decrypt = (block) => {
  const d = createDecipheriv('aes-128-ecb', idKey(), null);
  d.setAutoPadding(false);
  return Buffer.concat([d.update(block), d.final()]);
};

const encode = (kind, id) => {
  const n = Number(id);
  if (!Number.isSafeInteger(n) || n <= 0) throw new RangeError(`bad ${kind} id ${id}`);
  const block = Buffer.alloc(16);
  MAGIC.copy(block, 0);
  block[4] = KINDS[kind];
  block.writeBigUInt64BE(BigInt(n), 8);
  return prefixOf(kind) + encrypt(block).toString('hex');
};

const decode = (kind, uid) => {
  const prefix = prefixOf(kind);
  if (typeof uid !== 'string' || !uid.toLowerCase().startsWith(prefix)) return null;
  const hex = uid.slice(prefix.length);
  if (!/^[0-9a-fA-F]{32}$/.test(hex)) return null;
  const block = decrypt(Buffer.from(hex, 'hex'));
  if (!block.subarray(0, 4).equals(MAGIC) || block[4] !== KINDS[kind] || block[5] || block[6] || block[7]) return null;
  const n = block.readBigUInt64BE(8);
  return n > 0n && n <= 2147483647n ? Number(n) : null;
};

export const userUid = (id) => encode('user', id);
export const roomUid = (id) => encode('room', id);
export const groupUid = (id) => encode('group', id);

export const userIdOf = (uid) => decode('user', uid);
export const groupIdOf = (uid) => decode('group', uid);

export const isUserIdFormat = (s) => typeof s === 'string' && /^[a-z0-9]{2,12}-[0-9a-fA-F]{32}$/.test(s);
