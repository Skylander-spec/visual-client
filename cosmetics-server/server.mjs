import http from 'node:http';
import crypto from 'node:crypto';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const UUID = /^[a-f0-9]{32}$/;
const MAX_BODY = 750_000;
const MAX_IMAGE = 256_000;
const cleanId = id => String(id ?? '').replaceAll('-', '').toLowerCase();

function png(value) {
  if (value == null || value === '') return null;
  if (typeof value !== 'string' || value.length > MAX_IMAGE * 4 / 3 + 8 || !/^[A-Za-z0-9+/]+={0,2}$/.test(value))
    throw new Error('Invalid image');
  const bytes = Buffer.from(value, 'base64');
  if (bytes.length < 33 || bytes.length > MAX_IMAGE ||
      bytes.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a' ||
      bytes.toString('ascii', 12, 16) !== 'IHDR') throw new Error('Expected PNG');
  const width = bytes.readUInt32BE(16), height = bytes.readUInt32BE(20);
  if (width < 1 || height < 1 || width > 1024 || height > 4096 || width * height > 1_048_576)
    throw new Error('Image dimensions exceed limit');
  return value;
}

export function validateAppearance(input) {
  if (!input || typeof input !== 'object') throw new Error('Invalid appearance');
  const integer = (key, min, max, fallback) => {
    const n = input[key] ?? fallback;
    if (!Number.isInteger(n) || n < min || n > max) throw new Error(`Invalid ${key}`);
    return n;
  };
  const flag = (key, fallback = false) => {
    const v = input[key] ?? fallback;
    if (typeof v !== 'boolean') throw new Error(`Invalid ${key}`);
    return v;
  };
  return {
    miniMe: flag('miniMe'), miniMeArt: integer('miniMeArt', 0, 16, 0),
    miniMePos: integer('miniMePos', 0, 3, 1), miniMeSize: integer('miniMeSize', 15, 80, 35),
    miniMeSitzt: flag('miniMeSitzt', true), miniMeHut: integer('miniMeHut', 0, 9, 0),
    miniMeFluegel: flag('miniMeFluegel'), miniMeFlight: integer('miniMeFlight', 0, 2, 0),
    miniMeScarf: integer('miniMeScarf', 0, 3, 0), miniMeShoes: integer('miniMeShoes', 0, 3, 0), cape: png(input.cape), skin: png(input.skin)
  };
}

async function verifyMicrosoft({ username, serverId, uuid }) {
  const query = new URLSearchParams({ username, serverId });
  const response = await fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?${query}`,
    { signal: AbortSignal.timeout(8000) });
  if (!response.ok || response.status === 204) return false;
  const profile = await response.json();
  return cleanId(profile.id) === uuid;
}

export async function createService({ directory = './data', verify = verifyMicrosoft } = {}) {
  await fs.mkdir(directory, { recursive: true });
  const challenges = new Map(), sessions = new Map(), limits = new Map();
  const server = http.createServer(async (req, res) => {
    const reply = (status, data) => {
      res.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
      res.end(JSON.stringify(data));
    };
    try {
      const now = Date.now();
      const ip = req.socket.remoteAddress;
      let rate = limits.get(ip);
      if (!rate || now - rate.start > 60_000) limits.set(ip, rate = { start: now, count: 0 });
      if (++rate.count > 240) return reply(429, { error: 'Too many requests' });
      const url = new URL(req.url, 'http://localhost');
      if (req.method === 'GET' && url.pathname === '/health') return reply(200, { ok: true });
      if (req.method === 'GET' && url.pathname === '/challenge') {
        const uuid = cleanId(url.searchParams.get('uuid'));
        if (!UUID.test(uuid)) return reply(400, { error: 'Invalid UUID' });
        if (challenges.size > 10_000) return reply(503, { error: 'Busy' });
        const serverId = crypto.randomBytes(20).toString('hex');
        challenges.set(serverId, { uuid, expires: now + 60_000 });
        return reply(200, { serverId });
      }
      if (req.method === 'GET' && url.pathname === '/appearances') {
        const ids = [...new Set((url.searchParams.get('ids') ?? '').split(',').map(cleanId))];
        if (ids.length > 32 || ids.some(id => !UUID.test(id))) return reply(400, { error: 'Invalid IDs' });
        const result = {};
        for (const id of ids) {
          try { result[id] = JSON.parse(await fs.readFile(path.join(directory, `${id}.json`), 'utf8')); }
          catch (error) { if (error.code !== 'ENOENT') throw error; }
        }
        return reply(200, result);
      }
      if (req.method !== 'POST' && req.method !== 'PUT') return reply(404, { error: 'Not found' });
      if (!['/session', '/appearance'].includes(url.pathname)) return reply(404, { error: 'Not found' });
      let body = '';
      for await (const chunk of req) {
        body += chunk;
        if (Buffer.byteLength(body) > MAX_BODY) { reply(413, { error: 'Payload too large' }); req.destroy(); return; }
      }
      const input = JSON.parse(body);
      if (req.method === 'POST' && url.pathname === '/session') {
        const uuid = cleanId(input.uuid);
        const challenge = challenges.get(input.serverId);
        challenges.delete(input.serverId); // a challenge may only be consumed once
        if (!challenge || challenge.expires < now || challenge.uuid !== uuid ||
            !/^[A-Za-z0-9_]{1,16}$/.test(input.username ?? '')) return reply(401, { error: 'Invalid challenge' });
        if (!await verify({ uuid, username: input.username, serverId: input.serverId }))
          return reply(401, { error: 'Minecraft ownership could not be verified' });
        const token = crypto.randomBytes(32).toString('hex');
        sessions.set(token, { uuid, expires: now + 24 * 3600_000 });
        return reply(200, { token });
      }
      if (req.method === 'PUT' && url.pathname === '/appearance') {
        const token = req.headers.authorization?.replace(/^Bearer /, '');
        const session = sessions.get(token);
        if (!session || session.expires < now) return reply(401, { error: 'Authentication required' });
        const appearance = validateAppearance(input);
        const dest = path.join(directory, `${session.uuid}.json`);
        const temp = `${dest}.${crypto.randomBytes(8).toString('hex')}.tmp`;
        await fs.writeFile(temp, JSON.stringify(appearance));
        await fs.rename(temp, dest);
        return reply(200, { ok: true });
      }
      reply(404, { error: 'Not found' });
    } catch (error) {
      // No tokens, request bodies or account data in logs or error responses.
      if (!res.headersSent) reply(error instanceof SyntaxError || /Invalid|image|PNG|limit/i.test(error.message) ? 400 : 500,
        { error: 'Request failed' });
    }
  });
  server.requestTimeout = 15_000;
  const cleanup = setInterval(() => {
    const now = Date.now();
    for (const map of [challenges, sessions]) for (const [key, value] of map)
      if (value.expires < now) map.delete(key);
    for (const [key, value] of limits) if (now - value.start > 60_000) limits.delete(key);
  }, 60_000).unref();
  server.on('close', () => clearInterval(cleanup));
  return server;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const server = await createService({ directory: process.env.DATA_DIR ?? './data' });
  server.listen(Number(process.env.PORT ?? 8787), process.env.HOST ?? '127.0.0.1', () =>
    console.log('Visual cosmetics service ready'));
}
