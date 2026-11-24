export const signedIn = async (ctx) => {
  const s = await ctx.session();
  return s && Number(s.userId) > 0 ? s : null;
};

export const loginFirst = (ctx, res) => ctx.send.error(res, 401, 'Log in first', 'Your session ended. Log in again.', { reason: 'login-required' });
