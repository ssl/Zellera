import { serverId } from './client-profile.mjs';

export const photoDir = () => `servercamera/photos/${serverId()}`;
export const PHOTO_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
