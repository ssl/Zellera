export const excerpt = (text, max, { atWord = true } = {}) => {
  const flat = String(text ?? '').replace(/\s+/g, ' ').trim();
  if (flat.length <= max) return flat;
  if (!atWord) return `${flat.slice(0, max - 1)}…`;
  const cut = flat.slice(0, max);
  const at = cut.lastIndexOf(' ');
  return `${(at > max * 0.6 ? cut.slice(0, at) : cut).replace(/[\s.,;:!?-]+$/, '')}…`;
};
