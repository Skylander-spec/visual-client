const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

function fixture(initial = []) {
  let stored = JSON.stringify(initial)
  let writes = 0
  const copies = []
  const fakeFs = {
    readFileSync: () => stored,
    writeFileSync: (_file, value) => { stored = value; writes++ },
    cpSync: (from, to) => { copies.push([from, to]) },
    mkdirSync: () => {},
    existsSync: () => true
  }
  const module = { exports: {} }
  const code = ts.transpileModule(fs.readFileSync(path.join(__dirname, '../src/main/profiles.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true }
  }).outputText
  const imports = { fs: fakeFs, path, crypto: require('node:crypto'), './paths': { PROFILES_FILE: 'profiles.json', instanceDir: id => `/isolated/${id}` } }
  vm.runInNewContext(code, { module, exports: module.exports, require: id => { if (!(id in imports)) throw Error(id); return imports[id] } })
  return { api: module.exports, writes: () => writes, copies }
}

test('legacy profiles remain Java without migrating their files', () => {
  const f = fixture([{ id: 'old', name: 'Bedwars', mcVersion: '1.21.11', loader: 'fabric', mods: [] }])
  const saved = f.api.saveProfile({ id: 'old', name: 'Bedwars renamed', mcVersion: '1.21.11' })
  assert.equal(saved.engine ?? 'java', 'java')
  assert.equal(saved.loader, 'fabric')
})
test('Rust profile disables Java loader, auto-install and Java mod metadata', () => {
  const f = fixture()
  const p = f.api.saveProfile({ name: 'Rust', mcVersion: '26.3', engine: 'horizon', loader: 'fabric', autoVisuals: true, mods: [{ projectId: 'fabric-api' }] })
  assert.equal(p.loader, 'vanilla')
  assert.equal(p.autoVisuals, false)
  assert.equal(p.mods.length, 0)
})
test('unsupported Rust version is rejected without saving', () => {
  const f = fixture()
  assert.throws(() => f.api.saveProfile({ name: 'Rust', mcVersion: '1.21.11', engine: 'horizon' }), /26.3/)
  assert.equal(f.writes(), 0)
})
test('existing Java world cannot be switched to Rust in place', () => {
  const f = fixture([{ id: 'old', name: 'World', mcVersion: '1.21.11', mods: [] }])
  assert.throws(() => f.api.saveProfile({ id: 'old', name: 'World', mcVersion: '26.3', engine: 'horizon' }), /new profile/)
  assert.equal(f.writes(), 0)
})
test('existing Rust world cannot be switched to Java in place', () => {
  const f = fixture([{ id: 'rust', name: 'World', engine: 'horizon', mcVersion: '26.3', mods: [] }])
  assert.throws(() => f.api.saveProfile({ id: 'rust', name: 'World', mcVersion: '1.21.11', engine: 'java' }), /new profile/)
  assert.equal(f.writes(), 0)
})
test('duplicate keeps Rust engine and receives a distinct profile identity', () => {
  const f = fixture([{ id: 'rust', name: 'World', engine: 'horizon', mcVersion: '26.3', mods: [] }])
  const copy = f.api.duplicateProfile('rust')
  assert.equal(copy.engine, 'horizon')
  assert.notEqual(copy.id, 'rust')
  assert.equal(f.copies.length, 4)
  assert.deepEqual(f.copies.map(([from]) => path.basename(from)).sort(), ['options.txt', 'resourcepacks', 'saves', 'shaderpacks'])
  assert.ok(f.copies.every(([from]) => !from.includes('horizon-launcher')))
})
