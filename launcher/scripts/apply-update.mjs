// Baut den aktuellen Code und spielt ihn in die INSTALLIERTE App ein,
// ohne die .exe / Verknüpfung / App-ID zu verändern — der Taskleisten-Pin
// bleibt also gültig. Aufruf:  npm run update
import { execSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import os from 'node:os'

const INSTALL_DIR = path.join(
  process.env.LOCALAPPDATA ?? path.join(os.homedir(), 'AppData', 'Local'),
  'Programs',
  'Visual Client'
)
const EXE = path.join(INSTALL_DIR, 'Visual Client.exe')
const INSTALLED_ASAR = path.join(INSTALL_DIR, 'resources', 'app.asar')
const BUILT_ASAR = 'C:/Users/maxim/VisualClientApp/win-unpacked/resources/app.asar'
const ROOT = path.resolve(import.meta.dirname, '..')

const run = (cmd) => execSync(cmd, { stdio: 'inherit', cwd: ROOT })

if (!fs.existsSync(EXE)) {
  console.error('Installierte App nicht gefunden:', INSTALL_DIR)
  console.error('Erst einmalig installieren:  npm run dist  → Setup ausführen.')
  process.exit(1)
}

console.log('› Baue Code + Paket (electron-builder) …')
run('npx electron-vite build && npx electron-builder --win --dir')

if (!fs.existsSync(BUILT_ASAR)) {
  console.error('Gebautes app.asar nicht gefunden:', BUILT_ASAR)
  process.exit(1)
}

console.log('› Laufende App schließen …')
try {
  execSync('taskkill /IM "Visual Client.exe" /F', { stdio: 'ignore' })
} catch {
  /* lief nicht */
}
// kurz warten, bis Windows die Datei freigibt
await new Promise((r) => setTimeout(r, 1500))

console.log('› Neue app.asar einspielen …')
fs.copyFileSync(BUILT_ASAR, INSTALLED_ASAR)

console.log('› Neue Version starten …')
execSync(`start "" "${EXE}"`, { stdio: 'ignore', shell: 'cmd.exe' })
console.log('✓ Fertig — installierte App läuft mit den Änderungen (Pin bleibt gültig).')
