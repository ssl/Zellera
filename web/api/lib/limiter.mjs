import { spec } from './spec.mjs';

export const wiredRules = (cls) => {
  const l = spec.rateLimits[cls];
  if (!l) throw new Error(`no rate limit class ${cls}`);
  const rules = [{ windowMs: 60_000, max: l.perMinute }];
  if (l.per10s) rules.push({ windowMs: 10_000, max: l.per10s });
  return rules;
};
