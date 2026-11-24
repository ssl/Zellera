import { bridgeSecret, config } from './config.mjs';

const BRIDGE_HEADER = 'X-Bridge-Secret';
const BRIDGE_VERSION = 1;

export class BridgeDown extends Error {}

export const callBridge = async (op, args = {}, { timeoutMs = 5000, url = config.bridgeUrl, secret = bridgeSecret() } = {}) => {
  let res;
  try {
    res = await fetch(url, {
      method: 'POST',
      headers: { 'content-type': 'application/json', [BRIDGE_HEADER]: secret },
      body: JSON.stringify({ v: BRIDGE_VERSION, op, ...args }),
      signal: AbortSignal.timeout(timeoutMs),
    });
  } catch (e) {
    throw new BridgeDown(`bridge ${op}: ${e.name ?? 'error'}`);
  }
  if (res.status !== 200) {
    await res.body?.cancel().catch(() => {});
    throw new BridgeDown(`bridge ${op}: HTTP ${res.status}`);
  }
  let out;
  try { out = await res.json(); } catch { throw new BridgeDown(`bridge ${op}: body is not JSON`); }
  if (out?.ok === true) return { ok: true, data: out.data ?? null };
  if (out?.ok === false && Number.isInteger(out.status) && typeof out.error === 'string') {
    return { ok: false, status: out.status, error: out.error };
  }
  throw new BridgeDown(`bridge ${op}: unexpected answer`);
};
