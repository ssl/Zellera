async function measureDevice(options = {}) {
  const VERSION = '1';
  const NONE = '00000000';
  const UNSTABLE = 'ffffffff';
  const STORE_KEY = options.storeKey ?? 'zel.device';
  const SEEN_KEY = `${STORE_KEY}.seen`;
  const NOISY = Symbol('unstable');
  if (!globalThis.crypto?.subtle) throw new Error('the device cannot be measured on a page without https');

  const hex = (bytes) => Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
  const hash = async (value) => {
    if (value === NOISY) return UNSTABLE;
    if (value == null || value === '') return NONE;
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(String(value)));
    return hex(new Uint8Array(digest).subarray(0, 4));
  };
  const attempt = async (measure) => {
    await new Promise((resolve) => setTimeout(resolve));
    try {
      return await measure();
    } catch {
      return NOISY;
    }
  };
  const twice = async (measure) => {
    const first = await attempt(measure);
    return first === NOISY || first === (await attempt(measure)) ? first : NOISY;
  };
  const stored = (key) => {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  };
  const store = (key, value) => {
    try {
      localStorage.setItem(key, value);
      return localStorage.getItem(key) === value;
    } catch {
      return false;
    }
  };

  const id = () => {
    const kept = stored(STORE_KEY);
    if (kept && /^[0-9a-f]{16}$/.test(kept) && kept !== UNSTABLE + UNSTABLE) return kept;
    const made = hex(crypto.getRandomValues(new Uint8Array(8)));
    return store(STORE_KEY, made) ? made : UNSTABLE + UNSTABLE;
  };

  const ua = navigator.userAgent;
  const firefox = /Firefox\//.test(ua);
  const shuffled = navigator.brave != null;

  const canvas = document.createElement('canvas');
  const gl = canvas.getContext('webgl') ?? canvas.getContext('experimental-webgl');
  const lost = !gl || gl.isContextLost();
  const gpu = () => {
    if (lost) return null;
    const info = gl.getExtension('WEBGL_debug_renderer_info');
    const vendor = gl.getParameter(info ? info.UNMASKED_VENDOR_WEBGL : gl.VENDOR);
    const renderer = gl.getParameter(info ? info.UNMASKED_RENDERER_WEBGL : gl.RENDERER);
    return vendor == null && renderer == null ? null : `${vendor}|${renderer}`;
  };
  const glFeatures = () => {
    if (lost) return null;
    const names = ['MAX_TEXTURE_SIZE', 'MAX_CUBE_MAP_TEXTURE_SIZE', 'MAX_RENDERBUFFER_SIZE', 'MAX_VERTEX_ATTRIBS',
      'MAX_VERTEX_UNIFORM_VECTORS', 'MAX_FRAGMENT_UNIFORM_VECTORS', 'MAX_VARYING_VECTORS',
      'MAX_COMBINED_TEXTURE_IMAGE_UNITS', 'MAX_TEXTURE_IMAGE_UNITS', 'MAX_VERTEX_TEXTURE_IMAGE_UNITS',
      'ALIASED_LINE_WIDTH_RANGE', 'ALIASED_POINT_SIZE_RANGE', 'MAX_VIEWPORT_DIMS'];
    const values = names.map((n) => String(Array.from([gl.getParameter(gl[n])].flat())));
    const precision = [gl.VERTEX_SHADER, gl.FRAGMENT_SHADER].flatMap((shader) =>
      [gl.LOW_FLOAT, gl.MEDIUM_FLOAT, gl.HIGH_FLOAT, gl.HIGH_INT].map((kind) => {
        const p = gl.getShaderPrecisionFormat(shader, kind);
        return p ? `${p.rangeMin},${p.rangeMax},${p.precision}` : '';
      }));
    return [...values, ...precision, ...(gl.getSupportedExtensions() ?? []).sort()].join('|');
  };

  const screenSize = () => {
    if (screen.width === innerWidth && screen.height === innerHeight) return NOISY;
    const scale = firefox ? devicePixelRatio : 1;
    return [screen.width, screen.height].map((n) => Math.round(n * scale)).sort((a, b) => a - b)
      .concat(screen.colorDepth).join('x');
  };

  const cpu = () => (shuffled ? NOISY : [navigator.hardwareConcurrency ?? '', navigator.deviceMemory ?? '',
    navigator.platform ?? '', navigator.maxTouchPoints ?? ''].join('|'));

  const zone = () => [Intl.DateTimeFormat().resolvedOptions().timeZone, (navigator.languages ?? [navigator.language]).join(',')]
    .join('|');

  const FONTS = ['Arial', 'Arial Black', 'Arial Narrow', 'Bahnschrift', 'Calibri', 'Cambria', 'Candara', 'Century Gothic',
    'Comic Sans MS', 'Consolas', 'Constantia', 'Corbel', 'Courier New', 'DejaVu Sans', 'Franklin Gothic Medium',
    'Futura', 'Gadugi', 'Geneva', 'Georgia', 'Gill Sans', 'Helvetica', 'Helvetica Neue', 'Impact', 'Liberation Sans',
    'Lucida Console', 'Lucida Grande', 'Menlo', 'Microsoft Sans Serif', 'Monaco', 'MS Gothic', 'Optima', 'Palatino',
    'Segoe UI', 'Tahoma', 'Times New Roman', 'Trebuchet MS', 'Verdana'];
  const fonts = () => {
    if (shuffled) return NOISY;
    const context = document.createElement('canvas').getContext('2d');
    const text = 'mmmmmmmmmmlli10OQ@#';
    const width = (family) => {
      context.font = `72px ${family}`;
      return context.measureText(text).width;
    };
    const bases = ['monospace', 'sans-serif', 'serif'];
    const plain = bases.map(width);
    const ofThePage = new Set(Array.from(document.fonts ?? [], (face) => face.family.replace(/["']/g, '').toLowerCase()));
    return FONTS.filter((font) => !ofThePage.has(font.toLowerCase())
      && bases.some((base, i) => width(`"${font}",${base}`) !== plain[i])).join(',');
  };

  const canvasIsExact = () => {
    const probe = document.createElement('canvas');
    probe.width = probe.height = 8;
    const c = probe.getContext('2d');
    c.fillStyle = 'rgb(18, 52, 86)';
    c.fillRect(0, 0, 8, 8);
    const pixels = c.getImageData(0, 0, 8, 8).data;
    return pixels.every((v, i) => v === [18, 52, 86, 255][i % 4]);
  };
  const canvasDrawing = () => {
    if (shuffled || !canvasIsExact()) return NOISY;
    const drawing = document.createElement('canvas');
    drawing.width = 240;
    drawing.height = 60;
    const c = drawing.getContext('2d');
    c.textBaseline = 'alphabetic';
    c.fillStyle = '#f60';
    c.fillRect(100, 1, 62, 20);
    c.fillStyle = '#069';
    c.font = '11pt "Times New Roman"';
    c.fillText('Cwm fjordbank glyphs vext quiz', 2, 15);
    c.fillStyle = 'rgba(102, 204, 0, 0.7)';
    c.font = '18pt Arial';
    c.fillText('Cwm fjordbank glyphs vext quiz', 4, 45);
    c.globalCompositeOperation = 'multiply';
    for (const [colour, x] of [['#f2f', 40], ['#2ff', 80], ['#ff2', 60]]) {
      c.fillStyle = colour;
      c.beginPath();
      c.arc(x, 30, 25, 0, Math.PI * 2, true);
      c.closePath();
      c.fill();
    }
    return drawing.toDataURL();
  };

  const AUDIO_MILLIS = 1000;
  const audio = async () => {
    if (shuffled) return NOISY;
    const Offline = globalThis.OfflineAudioContext ?? globalThis.webkitOfflineAudioContext;
    if (!Offline) return null;
    const context = new Offline(1, 5000, 44100);
    const oscillator = context.createOscillator();
    oscillator.type = 'triangle';
    oscillator.frequency.value = 10000;
    const compressor = context.createDynamicsCompressor();
    compressor.threshold.value = -50;
    compressor.knee.value = 40;
    compressor.ratio.value = 12;
    compressor.attack.value = 0;
    compressor.release.value = 0.25;
    oscillator.connect(compressor);
    compressor.connect(context.destination);
    oscillator.start(0);
    const late = new Promise((_, reject) => setTimeout(() => reject(new Error('audio took too long')), AUDIO_MILLIS));
    const samples = (await Promise.race([context.startRendering(), late])).getChannelData(0);
    let sum = 0;
    for (let i = 4500; i < samples.length; i++) sum += Math.abs(samples[i]);
    return String(sum);
  };

  const agent = () => {
    const found = /(Firefox|FxiOS|EdgiOS|EdgA|Edg|OPR|OPT|SamsungBrowser)\/(\d+)/.exec(ua) ?? /(Chrome|CriOS)\/(\d+)/.exec(ua);
    const safari = /Version\/(\d+).*Safari/.exec(ua);
    const [name, major] = found ? [found[1], found[2]] : safari ? ['Safari', safari[1]] : ['other', ''];
    const system = ['Android', 'iPhone', 'iPad', 'CrOS', 'Windows NT', 'Mac OS X', 'Linux'].find((s) => ua.includes(s)) ?? 'other';
    return `${name}|${major}|${system}`;
  };

  const raw = {
    gpu: await attempt(gpu),
    scr: await attempt(screenSize),
    cpu: await attempt(cpu),
    tz: await attempt(zone),
    fnt: await attempt(fonts),
    cnv: await twice(canvasDrawing),
    aud: await twice(audio),
    gl: await attempt(glFeatures),
    ua: await attempt(agent),
  };
  gl?.getExtension('WEBGL_lose_context')?.loseContext();
  canvas.width = canvas.height = 0;

  const pieces = { id: id() };
  for (const [name, value] of Object.entries(raw)) pieces[name] = await hash(value);

  let seen = {};
  try {
    seen = JSON.parse(stored(SEEN_KEY) ?? '{}') ?? {};
  } catch {
    seen = {};
  }
  const now = {};
  for (const name of ['cnv', 'aud']) {
    now[name] = pieces[name];
    if (pieces[name] !== NONE && pieces[name] !== UNSTABLE && seen[name] !== pieces[name]
      && seen[name] && seen[name] !== NONE && seen[name] !== UNSTABLE) pieces[name] = UNSTABLE;
  }
  store(SEEN_KEY, JSON.stringify(now));

  const device = [VERSION, ...Object.values(pieces)].join('-');
  if (!options.raw) return { device, pieces };
  return { device, pieces, raw: Object.fromEntries(Object.entries(raw).map(([k, v]) => [k, v === NOISY ? 'unstable' : v])) };
}
