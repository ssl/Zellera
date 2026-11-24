import { clientString } from '../lib/client-strings.mjs';
import { HELP_PAGES, fallback, render } from './client-help-pages.mjs';

export const owner = 'help';
export const grants = [];

const pageName = (name) => (name === clientString('help.clubPage') ? 'club' : name);

const NAME = /^[a-z0-9][a-z0-9_.\/-]{0,99}$/i;
const HEADERS = { 'cache-control': 'public, max-age=300' };

export const routes = [
  {
    method: 'GET', path: '/gamedata/pages/*page',
    handler: (req, res, ctx) => {
      const name = String(ctx.params.page);
      const known = NAME.test(name) ? (HELP_PAGES.get(pageName(name)) ?? HELP_PAGES.get(pageName(name).toLowerCase())) : null;
      return ctx.send.text(res, 200, render(known ?? fallback(ctx.SITE)), HEADERS);
    },
  },
];
