const NAME_KEY = 'game.name';
const NAME_MAX = 40;
const REFRESH_MS = 60_000;

let fromDb = null;

export const cleanName = (value) => String(value ?? '')
  .replace(/[\u0000-\u001f\u007f-\u009f]+/g, ' ').replace(/\s+/g, ' ').trim().slice(0, NAME_MAX).trim();

export const FALLBACK_NAME = cleanName(process.env.ZEL_GAME_NAME) || 'My Game';

export const settingName = () => fromDb;
export const gameName = () => fromDb ?? FALLBACK_NAME;
const setSettingName = (value) => { fromDb = value == null ? null : (cleanName(value) || null); };

const refresh = async (read) => {
  try { setSettingName((await read())?.[NAME_KEY]); } catch (e) { console.error(`web: the game's name was not read from the settings: ${e?.message ?? e}`); }
};

export const startGameName = async (read, { everyMs = REFRESH_MS } = {}) => {
  await refresh(read);
  const timer = setInterval(() => refresh(read), everyMs);
  timer.unref?.();
  return () => clearInterval(timer);
};
