export const DEV_SETTINGS = Object.freeze({
  'login.ticket.is_reusable': [false, false],
  'server.log.is_debug': [false, false],
  'login.ticket.expires': [true, true],
});
const DEV_PATH = /^\/_dev(\/|$)/;

export const settingOn = (value, fallback) => {
  const word = String(value ?? '').trim().toLowerCase();
  if (word === 'true' || word === '1') return true;
  if (word === 'false' || word === '0') return false;
  return fallback;
};

export const productionProblems = ({ settings, routes }) => {
  const problems = [];
  for (const [name, [wanted, fallback]] of Object.entries(DEV_SETTINGS)) {
    const value = settings[name];
    if (settingOn(value, fallback) !== wanted) {
      problems.push(`setting ${name} is ${value ?? 'missing'}; production needs ${wanted ? 1 : 0} (a dev stack set it)`);
    }
  }
  for (const r of routes) if (DEV_PATH.test(r.path)) problems.push(`${r.method} ${r.path} is a dev page`);
  return problems;
};

export const readDevSettings = async (query) => {
  const names = Object.keys(DEV_SETTINGS);
  const rows = await query(`SELECT \`name\`, \`value\` FROM settings WHERE \`name\` IN (${names.map(() => '?').join(',')})`, names);
  return Object.fromEntries(rows.map((r) => [r.name, r.value]));
};
