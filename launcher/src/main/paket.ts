import { BrowserWindow } from 'electron'
import { getProfile } from './profiles'
import { installMod, listInstalledContent } from './modrinth'
import { VError } from './errors'

/**
 * Das grosse Mod-Paket: dieselbe Auswahl, die OneClient mitbringt, dazu
 * Essential.
 *
 * Die Liste stammt nicht aus einer Werbeseite, sondern aus einer echten
 * OneClient-Installation: aus jeder Jar wurde die Mod-Kennung gelesen und
 * gegen Modrinth aufgeloest. Installiert wird ueber Modrinth, damit fuer
 * jede Minecraft-Version die passende Fassung kommt und Abhaengigkeiten
 * von selbst mitgezogen werden.
 *
 * Was NICHT dazugehoert: unsere eigene Oberflaeche. Startbildschirm,
 * Client-Menue und HUD bleiben unsere — die Mods hier bringen Funktionen
 * mit, nicht das Aussehen. Unsere HUD-Anzeigen sind ab Werk aus, deshalb
 * doppelt sich auch nichts, wenn EvergreenHUD mitkommt.
 */
// waveycapes fehlt bewusst: fuer 1.21.11 gibt es auf Modrinth keine
// Fabric-Fassung. Aufgenommen wird nur, was sich von dort beziehen laesst.
// polyplus fehlt bewusst: es beschreibt sich selbst als "Various
// expansions for the OneClient 'modpack'" und bringt OneClients
// Titelbildschirm mit - eigene Klasse, nicht die von Minecraft,
// weshalb unser TitleScreen-Mixin nie feuerte. Im Spiel stand dann
// ONECLIENT statt VISUAL CLIENT. Die einzelnen poly*-Mods
// (polyblur, polysprint, polytime ...) bleiben, die sind Funktion
// ohne Marke.
// animatium fehlt bewusst: es schreibt denselben Mixin-Punkt um wie
// viewmodel (HeldItemRenderer.renderFirstPersonItem). Zusammen stuerzt
// Minecraft beim Start ab; viewmodel gewinnt und bleibt.
const PAKET: string[] = [
  '3dskinlayers',
  'animaticarefabricated',
  'appleskin',
  'architectury-api',
  'autogg-fabric',
  'badoptimizations',
  'behindyou',
  'bettercommandblockui',
  'better-mipmaps',
  'better-mount-hud',
  'better-narrator-error',
  'betternightvision',
  'better-screens',
  'better-selection',
  'better-stats',
  'black-bar-concealer',
  'blur-plus',
  // 'c2me-fabric' entfernt: sein Teil c2me-opts-natives-math verlangt
  // Java 25, wir liefern Java 21 mit. Der Start bricht sonst ab.
  'centered-crosshair',
  'chatting',
  'chattweaks',
  'cloth-config',
  'collective',
  'colorsaturation',
  'compose-multiplatform',
  'confirm-disconnect',
  'continuity',
  'controlling',
  'crashpatch',
  'crosshairtweaks',
  'cubes-without-borders',
  // 'cull-fewer-leaves' entfernt: erklaert sich selbst fuer unvereinbar
  // mit More Culling, und das kommt als Abhaengigkeit anderer Mods mit.
  // Beide zusammen lassen das Spiel gar nicht erst starten.
  'damagetint',
  'dark-graph',
  'dark-loading-screen',
  'debugify',
  'detail-armor-bar-reconstructed',
  'droppeditemtweaks',
  'dynamic-fps',
  'e4mc',
  'effecttimerplus',
  'entityculling',
  'entity-model-features',
  'entitytexturefeatures',
  'esf',
  'essential',
  'evergreenhud',
  'fabric-api',
  'fabric-language-kotlin',
  'fastquit',
  'fastserverpings',
  'ferrite-core',
  'firmament-packet-fix',
  'fix-keyboard-on-linux',
  'forcecloseworldloadingscreen',
  'forge-config-api-port',
  'fovchanger',
  'fzzy-config',
  'gamma-utils',
  'hitbox',
  'hypixel-mod-api',
  'hytils',
  'immediatelyfast',
  'in-game-account-switcher',
  'iris',
  'ixeris',
  'lambdynamiclights',
  'language-reload',
  'libjf',
  'lithium',
  'log-cleaner',
  'midnightlib',
  'mixintrace-reloaded',
  'modernfix-mvus',
  'modmenu',
  'mountopacity',
  'nbt-autocomplete',
  'no-chat-reports',
  'no-chat-restrictions',
  'notenoughcrashes',
  'noxesium',
  'numerical-enchantments',
  'obe',
  'oneconfig',
  'optigui',
  'optipainting-reloaded',
  'overflowparticles',
  'overlaytweaks',
  'packed-packs',
  'paginatedadvancements',
  'particle-core',
  'polyblur',
  'polynametag',
  'polysprint',
  'polytime',
  'polytone',
  'polyweather',
  'quick-pack',
  'redaction',
  'rendertweaks',
  'resourcify',
  'respackopts',
  'reward-claim',
  'rrls',
  'rsls',
  'scalablelux',
  'sciophobia',
  'screencopy',
  'screenshot-compression',
  'scrolltweaks',
  'searchables',
  'secureskins',
  'server-pack-unlocker',
  'shaketweaks',
  'shulkerboxtooltip',
  'simple-block-overlay',
  'simple-nick-hider',
  'simple-voice-chat',
  'skyboxify',
  'smooth-scrolling-refurbished',
  'smooth-skies',
  'smuc',
  'sodium',
  'sodium-extra',
  'soundtweaks',
  'spark',
  'stackdeobf',
  'status-effect-bars',
  'symbol-chat',
  'tcdcommons',
  'tooltip-scroll',
  'unilib',
  'vanillahud',
  'viewmodel',
  'vignettetweaks',
  'yacl',
  'yosbr',
  'zoomify'

]

export interface PaketStand {
  gesamt: number
  fertig: number
  installiert: number
  uebersprungen: number
  aktuell: string
}

/**
 * Installiert das Paket in ein Profil. Ein Mod, den es fuer diese
 * Minecraft-Version nicht gibt, wird uebersprungen statt den ganzen Lauf
 * abzubrechen — bei ueber 140 Eintraegen ist immer einer dabei, der auf
 * einer bestimmten Version fehlt.
 */
/** Wie viele Eintraege das Paket hat - fuer den Gesamtbalken beim Start. */
export function paketGroesse(): number {
  return PAKET.length
}

export async function installPaket(
  win: BrowserWindow | null,
  profileId: string,
  melden?: (s: PaketStand) => void
): Promise<PaketStand> {
  const profil = getProfile(profileId)
  if (!profil) throw new VError('error.profileMissing')

  // Schon vorhandene ueberspringen, damit ein zweiter Lauf schnell ist
  const da = new Set<string>()
  try {
    for (const e of listInstalledContent(profileId)) {
      if (e.projectId) da.add(e.projectId)
    }
  } catch {
    /* leeres Profil */
  }

  const stand: PaketStand = {
    gesamt: PAKET.length,
    fertig: 0,
    installiert: 0,
    uebersprungen: 0,
    aktuell: ''
  }
  for (const slug of PAKET) {
    stand.aktuell = slug
    win?.webContents.send('pack:status', { ...stand })
    melden?.({ ...stand })
    if (da.has(slug)) {
      stand.uebersprungen++
    } else {
      try {
        await installMod(profileId, slug, 'mod')
        stand.installiert++
      } catch {
        // Kein Build fuer diese Version, oder Modrinth kennt den Slug nicht
        stand.uebersprungen++
      }
    }
    stand.fertig++
    win?.webContents.send('pack:status', { ...stand })
    melden?.({ ...stand })
  }
  stand.aktuell = ''
  win?.webContents.send('pack:status', { ...stand })
  melden?.({ ...stand })
  return stand
}