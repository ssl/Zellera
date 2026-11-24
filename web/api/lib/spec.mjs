import { config, gameNameNow } from './config.mjs';
import { specData } from './spec-data.mjs';

export const spec = specData;

export const withSite = (value) => JSON.parse(JSON.stringify(value)
  .replaceAll('${ZEL_SITE}', config.site)
  .replaceAll('${ZEL_SERVER_ID}', JSON.stringify(config.serverId ?? '').slice(1, -1))
  .replaceAll('${ZEL_GAME_NAME}', JSON.stringify(gameNameNow()).slice(1, -1)));
