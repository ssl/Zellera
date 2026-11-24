import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const SPEC_FILE = fileURLToPath(new URL('../api.json', import.meta.url));
export const specData = JSON.parse(readFileSync(SPEC_FILE, 'utf8'));
