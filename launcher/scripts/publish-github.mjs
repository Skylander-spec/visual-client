// Veroeffentlicht eine neue Fassung als GitHub-Release.
//
// Ablauf fuer ein Update:
//   1. Version in package.json erhoehen (z. B. 1.4.2 -> 1.4.3)
//   2. npm run publish
//
// Danach sieht jeder Launcher beim naechsten Start "Update verfuegbar" und
// spielt es beim Schliessen ein. Was dabei ersetzt wird, ist ausschliesslich
// die app.asar in der Installation — Profile, Konten, Welten, selbst
// installierte Mods, Skins und Capes liegen unter %APPDATA%/.visualclient
// und bleiben unangetastet.
import { execSync, spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve(import.meta.dirname, '..')
const pkg = JSON.parse(fs.readFileSync(path.join(ROOT, 'package.json'), 'utf8'))
const version = pkg.version
const tag = `v${version}`
const REPO = 'Skylander-spec/visual-client'

const OUT = 'C:/Users/maxim/VisualClientApp'
const BUILT_ASAR = path.join(OUT, 'win-unpacked/resources/app.asar')
const INSTALLER = path.join(OUT, `Visual Client Setup ${version}.exe`)

// gh ist optional: ohne die CLI wird alles gebaut und der Rest erklaert.
const gh = spawnSync('gh', ['--version'], { shell: true }).status === 0

/**
 * Den gebauten Fabric-Mod nach launcher/build holen.
 *
 * electron-builder packt "build/visuals-fabric-*.jar" ein, gebaut wird der
 * Mod aber nach fabricmod/build/libs/visuals-fabric-<mc>-1.0.0.jar. Dazwischen
 * lag nichts. Folge: jede Fassung nahm die Jars mit, die zufaellig gerade in
 * build/ lagen — aenderte man den Mod, kam die Aenderung nie beim Spieler an,
 * und der Launcher schrieb seine alte Kopie beim Start sogar ueber ein von
 * Hand ins Profil gelegtes Jar. Genau daher kam "es hat sich nichts geaendert".
 */
function modUebernehmen() {
  const libs = path.join(ROOT, '..', 'fabricmod', 'build', 'libs')
  const ziel = path.join(ROOT, 'build')
  if (!fs.existsSync(libs)) {
    console.warn('! fabricmod/build/libs fehlt — Mod-Jars bleiben wie sie sind')
    return
  }
  let uebernommen = 0
  for (const datei of fs.readdirSync(libs)) {
    const treffer = datei.match(/^visuals-fabric-(.+)-1\.0\.0\.jar$/)
    if (!treffer || datei.includes('sources')) continue
    const von = path.join(libs, datei)
    const nach = path.join(ziel, `visuals-fabric-${treffer[1]}.jar`)
    // Nur kopieren, was sich unterscheidet - sonst rauscht bei jedem
    // Veroeffentlichen dieselbe Liste durch.
    const gleich =
      fs.existsSync(nach) && fs.readFileSync(von).equals(fs.readFileSync(nach))
    if (gleich) continue
    fs.copyFileSync(von, nach)
    console.log(`  ${treffer[1]}: Mod aktualisiert`)
    uebernommen++
  }
  console.log(uebernommen ? `> ${uebernommen} Mod-Jar(s) uebernommen` : '> Mod-Jars aktuell')
}

modUebernehmen()

console.log(`> Baue ${version} …`)
execSync('npx electron-vite build && npx electron-builder --win', {
  stdio: 'inherit',
  cwd: ROOT
})

if (!fs.existsSync(BUILT_ASAR)) {
  console.error('app.asar nicht gefunden:', BUILT_ASAR)
  process.exit(1)
}

// latest.json neben die asar legen — der Launcher prueft damit den Download
const hash = crypto.createHash('sha256').update(fs.readFileSync(BUILT_ASAR)).digest('hex')
const metaFile = path.join(OUT, 'latest.json')
fs.writeFileSync(
  metaFile,
  JSON.stringify({ version, hash, published: new Date().toISOString() }, null, 2)
)
console.log(`> app.asar  sha256 ${hash.slice(0, 16)}…`)

const dateien = [BUILT_ASAR, metaFile]
if (fs.existsSync(INSTALLER)) dateien.push(INSTALLER)
else console.log('! Installer nicht gefunden, wird nicht mit hochgeladen:', INSTALLER)

if (!gh) {
  console.log('')
  console.log('GitHub CLI (gh) fehlt — einmalig einrichten:')
  console.log('  winget install --id GitHub.cli -e')
  console.log('  gh auth login')
  console.log('')
  console.log('Danach reicht kuenftig "npm run publish". Diesmal von Hand:')
  console.log(`  gh release create ${tag} --repo ${REPO} --title "Visual Client ${version}"`)
  console.log(`    ${dateien.map((f) => `"${f}"`).join(' ')}`)
  process.exit(0)
}

console.log(`> Lege Release ${tag} an …`)
const vorhanden =
  spawnSync('gh', ['release', 'view', tag, '--repo', REPO], { shell: true }).status === 0
if (vorhanden) {
  execSync(
    `gh release upload ${tag} --repo ${REPO} --clobber ${dateien.map((f) => `"${f}"`).join(' ')}`,
    { stdio: 'inherit' }
  )
} else {
  execSync(
    `gh release create ${tag} --repo ${REPO} --title "Visual Client ${version}" ` +
      `--notes "Automatisches Update. Alle Spielerdateien bleiben erhalten." ` +
      dateien.map((f) => `"${f}"`).join(' '),
    { stdio: 'inherit' }
  )
}
console.log(`\u2713 ${tag} veroeffentlicht \u2192 https://github.com/${REPO}/releases/tag/${tag}`)
