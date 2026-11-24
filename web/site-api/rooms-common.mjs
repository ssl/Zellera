export const LOOKUP_LIMIT = ['pub-lookup', 120, 60];

export const resolveText = (ctx, value) => {
  let s = String(value ?? '');
  for (let i = 0; i < 4; i++) {
    const m = /^\$\{([^}]+)\}$/.exec(s) ?? /^\$([^$\s]+)\$$/.exec(s);
    if (!m) return s;
    const next = ctx.texts.get(m[1], null);
    if (next == null) return '';
    s = next;
  }
  return /^\$/.test(s) ? '' : s;
};

export const doorLabel = (ctx, state) => {
  switch (state) {
    case 'locked': return ctx.texts.get('navigator.door.mode.closed', 'Doorbell');
    case 'password': return ctx.texts.get('navigator.door.mode.password', 'Password');
    case 'invisible': return ctx.texts.get('navigator.door.mode.invisible', 'Invisible');
    default: return ctx.texts.get('navigator.door.mode.open', 'Open');
  }
};

export const groupTypeLabel = (ctx, state) => {
  switch (Number(state)) {
    case 1: return ctx.texts.get('group.edit.settings.type.exclusive.label', 'Exclusive');
    case 2: case 4: return ctx.texts.get('group.edit.settings.type.private.label', 'Private');
    default: return ctx.texts.get('group.edit.settings.type.regular.label', 'Regular');
  }
};
