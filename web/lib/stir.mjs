import { createHmac } from 'node:crypto';

const SEED = [0x76, 0x8D, 0x3C, 0xDD, 0xDC, 0x3A, 0x91, 0xDD, 0xF7, 0x46, 0x28, 0x63, 0x59, 0xC1, 0x82, 0x2E];
const key = Buffer.from(SEED.map((b) => b ^ 0x3C));

export const stirred = (text, algorithm = 'sha256') => createHmac(algorithm, key).update(text).digest();

export const tail = (body, alphabet, length) => {
  const mac = stirred(`t${body}`);
  let out = '';
  for (let i = 0; i < length; i++) out += alphabet[mac[i] % alphabet.length];
  return out;
};
