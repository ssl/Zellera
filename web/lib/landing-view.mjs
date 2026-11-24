export const ARTICLE_SLOTS = Object.freeze([2, 3, 4, 5]);
const IMAGE_SLOTS = Object.freeze([2, 4]);
export const shownAt = (i) => ({ shown: i < ARTICLE_SLOTS.length, image: IMAGE_SLOTS.includes(ARTICLE_SLOTS[i]) });
const REFRESH_MS = 60_000;

export const ACTIONS = Object.freeze([
  Object.freeze({ type: 'catalog', label: 'Open a catalog page', hint: 'The page\'s name, like wired_new (the name in catalog/open/<name> links).' }),
  Object.freeze({ type: 'room', label: 'Go to a room', hint: 'The room\'s id.' }),
  Object.freeze({ type: 'client', label: 'Open a client link', hint: 'A link the client opens itself, like navigator/tab/official_view or catalog/open.' }),
  Object.freeze({ type: 'web', label: 'Open a page of this site', hint: 'A path on this site, like /news or /help.' }),
  Object.freeze({ type: 'none', label: 'No button', hint: '' }),
]);
const TYPES = new Set(ACTIONS.map((a) => a.type));

export const CLIENT_LINKS = Object.freeze(['navigator/tab/official_view', 'navigator/goto/home', 'catalog/open']);

export const LIMITS = Object.freeze({ title: 100, text: 500, button: 50, link: 200, image: 200 });

const CATALOG_PAGE = /^[A-Za-z0-9_-]{1,64}$/;
const ROOM_ID = /^[1-9]\d{0,9}$/;
const CLIENT_LINK = /^[A-Za-z0-9_][A-Za-z0-9_/.-]{0,199}$/;
const SITE_PATH = /^\/[^\s,;"'<>\\]{0,199}$/;

const oneLine = (s) => String(s ?? '').replace(/\r\n?/g, '\n').trim();

export const articleRefusal = (a) => {
  const title = oneLine(a.title);
  const text = oneLine(a.text);
  const button = oneLine(a.button_text);
  const link = String(a.button_link ?? '').trim();
  const image = String(a.image ?? '').trim();
  if (!title) return 'An article needs a title.';
  if (title.includes('\n')) return 'The title is one line.';
  if (title.length > LIMITS.title) return `The title is at most ${LIMITS.title} characters.`;
  if (!text) return 'An article needs a text.';
  if (text.length > LIMITS.text) return `The text is at most ${LIMITS.text} characters.`;
  if (!TYPES.has(a.button_type)) return 'Choose what the button does.';
  if (a.button_type !== 'none') {
    if (!button) return 'The button needs a text.';
    if (button.includes('\n') || button.length > LIMITS.button) return `The button text is one line of at most ${LIMITS.button} characters.`;
    if (a.button_type === 'catalog' && !CATALOG_PAGE.test(link)) return 'A catalog page name is letters, digits, _ and -, like wired_new.';
    if (a.button_type === 'room' && !ROOM_ID.test(link)) return 'A room is its id, a whole number.';
    if (a.button_type === 'client' && !CLIENT_LINK.test(link)) return 'A client link is letters, digits and / . _ -, like navigator/tab/official_view.';
    if (a.button_type === 'web' && !SITE_PATH.test(link)) return 'A page of this site is a path that starts with /, like /news (no spaces, commas or semicolons).';
  }
  if (image && !SITE_PATH.test(image)) return 'The image is a path on this site that starts with / (no spaces, commas or semicolons).';
  return null;
};

export const cleanArticle = (a) => {
  const type = a.button_type;
  return {
    title: oneLine(a.title),
    text: oneLine(a.text),
    button_text: type === 'none' ? '' : oneLine(a.button_text),
    button_type: type,
    button_link: type === 'none' ? '' : String(a.button_link ?? '').trim(),
    image: String(a.image ?? '').trim(),
  };
};

const key = (row) => `landing.view.news_article_${Number(row.id)}`;

const listValue = (s) => String(s ?? '').replace(/[,;\r\n]/g, '');

const buttonElement = (row, SITE) => {
  const k = `${key(row)}.button`;
  const link = listValue(row.button_link);
  switch (row.button_type) {
    case 'catalog': return `catalogbutton,${k},${link}`;
    case 'room': return `internallinkbutton,${k},navigator/goto/${link}`;
    case 'client': return `internallinkbutton,${k},${link}`;
    case 'web': return `link,${k},${SITE}${link}`;
    default: return null;
  }
};

export const articleVariables = (rows, SITE) => {
  const list = (rows ?? []).slice(0, ARTICLE_SLOTS.length);
  const lines = [];
  ARTICLE_SLOTS.forEach((slot, i) => {
    const row = list[i];
    const s = `landing.view.dynamic.slot.${slot}`;
    if (!row) {
      lines.push(`${s}.widget=`, `${s}.conf=`, `${s}.layout=`);
      return;
    }
    const k = key(row);
    const conf = [`caption,${k}.header`, `bodytext,${k}.body`, buttonElement(row, SITE)].filter(Boolean).join(';');
    const image = IMAGE_SLOTS.includes(slot) ? listValue(row.image) : '';
    lines.push(
      `${s}.widget=generic`, `${s}.conf=news_article_${Number(row.id)}`, `${s}.layout=`,
      `${k}.widget=generic`, `${k}.conf=${conf}`,
      `${k}.layout=${image ? `bitmap.uri,${SITE}${image};bitmap.x,50;bitmap.y,0;container.height,250` : ''}`,
    );
  });
  return lines.join('\n');
};

const textValue = (s) => oneLine(s).replace(/\n/g, '\\n');

export const articleTexts = (rows) => (rows ?? []).slice(0, ARTICLE_SLOTS.length).flatMap((row) => [
  `${key(row)}.header=${textValue(row.title)}`,
  `${key(row)}.body=${textValue(row.text)}`,
  `${key(row)}.button=${textValue(row.button_text)}`,
]).join('\n');

let articles = null;
let version = 0;
let changedAt = 0;
let reader = null;

export const landingViewArticles = () => articles;
export const landingViewVersion = () => version;
export const landingViewChangedAt = () => changedAt;
const setArticles = (rows) => {
  const next = rows == null ? null : rows.map((r) => ({
    id: Number(r.id), title: String(r.title ?? ''), text: String(r.text ?? ''), button_text: String(r.button_text ?? ''),
    button_type: String(r.button_type ?? 'none'), button_link: String(r.button_link ?? ''), image: String(r.image ?? ''),
  }));
  if (JSON.stringify(next) === JSON.stringify(articles)) return;
  articles = next;
  version++;
  changedAt = Date.now();
};

export const refreshLandingView = async () => {
  if (!reader) return;
  try { setArticles(await reader()); } catch (e) { console.error(`web: the landing view's articles were not read: ${e?.message ?? e}`); }
};

export const startLandingView = async (read, { everyMs = REFRESH_MS } = {}) => {
  reader = read;
  await refreshLandingView();
  const timer = setInterval(refreshLandingView, everyMs);
  timer.unref?.();
  return () => { clearInterval(timer); reader = null; };
};
