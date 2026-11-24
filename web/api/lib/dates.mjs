const iso = (ms) => new Date(ms).toISOString();

export const userTime = (sec) => (Number(sec) > 0 ? iso(Number(sec) * 1000).replace('Z', '+0000') : null);
export const colonTime = (sec) => iso(Math.max(0, Number(sec) || 0) * 1000).replace('Z', '+00:00');
export const day = (sec) => iso(Math.max(0, Number(sec) || 0) * 1000).slice(0, 10);
export const isoMs = (ms) => iso(Math.max(0, Number(ms) || 0));
export const today = (nowMs = Date.now()) => iso(nowMs).slice(0, 10);
