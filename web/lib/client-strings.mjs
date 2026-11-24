let strings = {};

export const clientString = (name) => strings[name];
export const clientStringsVersion = () => strings;
export const installClientStrings = (values) => { strings = { ...values }; };

export const startClientStrings = async (query) => {
  try {
    const rows = await query('SELECT name, value FROM client_strings');
    installClientStrings(Object.fromEntries(rows.map((r) => [r.name, r.value])));
  } catch (e) {
    console.error(`client profile not read: ${e.message}`);
  }
};
