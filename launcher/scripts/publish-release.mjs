// Veröffentlicht die aktuelle Version in den lokalen Release-Kanal
// (%APPDATA%/.visualclient/release). Die installierte App erkennt das
// beim nächsten Start als Update und spielt es beim Schließen ein —
// der User muss nichts manuell kopieren.
//
// Ablauf für ein neues Update:
//   1. Version in package.json erhöhen (z. B. 1.4.0 -> 1.4.1)
//   2. npm run release
//   3. Der Launcher zeigt beim nächsten Start "Update verfügbar"
import { execSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import os from 'node:os'

const ROOT = path.resolve(import.meta.dirname, '..')
const pkg = JSON.parse(fs.readFileSync(path.join(ROOT, 'package.json'), 'utf8'))
const version = pkg.version

const RELEASE_DIR = path.join(
  process.env.APPDATA ?? path.join(os.homedir(), 'AppData', 'Roaming'),
  '.visualclient',
  'release'
)
const BUILT_ASAR = 'C:/Users/maxim/VisualClientApp/win-unpacked/resources/app.asar'
// Der Build-Ordner oben ist NICHT die Installation — die legt der NSIS-Installer
// unter Local/Programs ab. Ohne diesen zweiten Pfad landet ein Release nur im
// Kanal, und die installierte App zeigt weiter den alten Stand.
const INSTALLED_ASAR = path.join(
  process.env.LOCALAPPDATA ?? path.join(os.homedir(), 'AppData', 'Local'),
  'Programs',
  'Visual Client',
  'resources',
  'app.asar'
)

console.log(`› Baue Version ${version} …`)
execSync('npx electron-vite build && npx electron-builder --win --dir', {
  stdio: 'inherit',
  cwd: ROOT
})

if (!fs.existsSync(BUILT_ASAR)) {
  console.error('Gebautes app.asar nicht gefunden:', BUILT_ASAR)
  process.exit(1)
}

fs.mkdirSync(RELEASE_DIR, { recursive: true })
fs.copyFileSync(BUILT_ASAR, path.join(RELEASE_DIR, 'app.asar'))
const hash = crypto.createHash('sha256').update(fs.readFileSync(BUILT_ASAR)).digest('hex')
fs.writeFileSync(
  path.join(RELEASE_DIR, 'latest.json'),
  JSON.stringify({ version, hash, published: new Date().toISOString() }, null, 2)
)

console.log(`✓ Version ${version} veröffentlicht → ${RELEASE_DIR}`)

// Direkt in die Installation kopieren, damit kein Neustart-Umweg nötig ist.
// Läuft die App gerade, ist die Datei gesperrt — dann greift der eingebaute
// Updater beim Schließen.
if (fs.existsSync(INSTALLED_ASAR)) {
  try {
    fs.copyFileSync(path.join(RELEASE_DIR, 'app.asar'), INSTALLED_ASAR)
    console.log('✓ Installation direkt aktualisiert →', INSTALLED_ASAR)
  } catch {
    console.log('› Installation gesperrt (App läuft) — Update greift beim Schließen.')
  }
} else {
  console.log('! Keine Installation gefunden unter', INSTALLED_ASAR)
}
console.log('  Die installierte App zeigt beim nächsten Start "Update verfügbar".')
