import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { createService, validateAppearance } from './server.mjs';

test('appearance validation rejects invalid models, huge images and non-PNG input', () => {
  assert.throws(() => validateAppearance({ miniMeArt: 99 }));
  assert.throws(() => validateAppearance({ miniMe: 'true' }));
  assert.throws(() => validateAppearance({ cape: Buffer.from('not png').toString('base64') }));
  assert.throws(() => validateAppearance({ skin: 'a'.repeat(500_000) }));
  assert.equal(validateAppearance({ miniMeArt: 8, miniMePos: 3 }).miniMeArt, 8);
  const cute = validateAppearance({ miniMeArt: 16, miniMeFlight: 2, miniMeHut: 9, miniMeScarf: 3, miniMeShoes: 3 });
  assert.equal(cute.miniMeArt, 16);
  assert.equal(cute.miniMeScarf, 3);
  assert.equal(cute.miniMeFlight, 2);
  for (const field of ['miniMeScarf', 'miniMeShoes', 'miniMeFlight'])
    assert.throws(() => validateAppearance({ [field]: 99 }));
});

test('two clients share cosmetics; authenticated identity cannot be spoofed; challenges cannot be replayed', async () => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), 'visual-cosmetics-test-'));
  const uuid = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
  const other = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb';
  const server = await createService({ directory, verify: async data => data.uuid === uuid });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const send = (route, input, token, method = 'POST') => fetch(base + route, {
    method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify(input)
  });
  try {
    assert.equal((await send('/appearance', {}, undefined, 'PUT')).status, 401);
    const challenge = await (await fetch(`${base}/challenge?uuid=${uuid}`)).json();
    const credentials = { uuid, username: 'TestPlayer', serverId: challenge.serverId };
    const login = await send('/session', credentials);
    assert.equal(login.status, 200);
    const { token } = await login.json();
    assert.equal((await send('/session', credentials)).status, 401);
    assert.equal((await send('/appearance', { uuid: other, miniMe: true, miniMeArt: 7, miniMePos: 3 }, token, 'PUT')).status, 200);
    const visible = await (await fetch(`${base}/appearances?ids=${uuid},${other}`)).json();
    assert.equal(visible[uuid].miniMeArt, 7);
    assert.equal(visible[uuid].miniMePos, 3);
    assert.equal(visible[other], undefined);
    const denied = await (await fetch(`${base}/challenge?uuid=${other}`)).json();
    assert.equal((await send('/session', { uuid: other, username: 'Impostor', serverId: denied.serverId })).status, 401);
    assert.equal((await fetch(`${base}/appearances?ids=../../accounts`)).status, 400);
  } finally {
    await new Promise(resolve => server.close(resolve));
    const resolved = path.resolve(directory);
    assert.equal(path.dirname(resolved), path.resolve(os.tmpdir()));
    assert.ok(path.basename(resolved).startsWith('visual-cosmetics-test-'));
    await fs.rm(resolved, { recursive: true });
  }
});
