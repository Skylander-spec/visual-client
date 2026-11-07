import fs from 'node:fs'
import path from 'node:path'
import os from 'node:os'
import crypto from 'node:crypto'
import { spawnSync } from 'node:child_process'
const root = path.resolve(import.meta.dirname, '..')
const source = process.env.VISUAL_RUST_SOURCE || path.join(os.homedir(), 'visual-rust-build', 'horizon')
const commit = 'a3acb5f0d2f59d2d5ff21a5aa000809ba1e35e2f'
const patch = path.join(root, '../rust-engine/visual-client.patch')
function run(bin, args, cwd = source, capture = false) {
  const r = spawnSync(bin, args, { cwd, stdio: capture ? 'pipe' : 'inherit', encoding: 'utf8', shell: false })
  if (r.error || r.status !== 0) throw new Error(`${bin} ${args[0]} failed: ${r.error?.message || r.stderr || r.status}`)
  return r.stdout?.trim()
}
if (!fs.existsSync(source)) {
  fs.mkdirSync(path.dirname(source), { recursive: true })
  run('git', ['clone', '--depth', '1', '--branch', 'v0.2.97', 'https://github.com/horizon-rust/horizon.git', source], path.dirname(source))
}
if (run('git', ['rev-parse', 'HEAD'], source, true) !== commit) throw new Error('Unexpected source revision; preserving checkout')
const applied = spawnSync('git', ['apply', '--reverse', '--check', patch], { cwd: source, stdio: 'ignore' }).status === 0
if (!applied) { run('git', ['apply', '--check', patch]); run('git', ['apply', patch]) }
const cargo = path.join(os.homedir(), '.cargo', 'bin', process.platform === 'win32' ? 'cargo.exe' : 'cargo')
run(fs.existsSync(cargo) ? cargo : 'cargo', ['build', '--locked', '--profile', 'fast', '-p', 'horizon-client', '--bin', 'horizon-client', '--features', 'visual-client'])
const ext = process.platform === 'win32' ? '.exe' : ''
const dir = path.join(root, 'build', 'rust')
fs.mkdirSync(dir, { recursive: true })
fs.cpSync(path.join(root, '../rust-engine/packs'), path.join(dir, 'shaderpacks'), { recursive: true })
const dest = path.join(dir, `visual-client${ext}`)
fs.copyFileSync(path.join(source, 'target', 'fast', `horizon-client${ext}`), dest)
fs.copyFileSync(path.join(source, 'LICENSE'), path.join(dir, 'UPSTREAM-LICENSE.txt'))
fs.writeFileSync(path.join(dir, 'build.json'), JSON.stringify({ base: commit, feature: 'visual-client', sha256: crypto.createHash('sha256').update(fs.readFileSync(dest)).digest('hex') }, null, 2))
console.log('Visual Rust build staged:', dest)
