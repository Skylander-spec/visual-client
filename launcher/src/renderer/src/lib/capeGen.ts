/**
 * Cape-Generator: erzeugt aus Stichwörtern + Zufallszahl ein Motiv.
 *
 * Bewusst lokal und ohne externen Dienst — es steckt kein Bildmodell dahinter,
 * sondern ein Satz Muster, deren Farben und Parameter aus dem Text abgeleitet
 * werden. Gleicher Text + gleiche Zahl ergibt immer dasselbe Bild.
 */

/** Zeichenfläche in Cape-Einheiten; K = Vergrößerung für HD. */
const K = 8

export const STYLES = [
  'Verlauf',
  'Sternenhimmel',
  'Streifen',
  'Flammen',
  'Nebel',
  'Geometrie',
  'Aurora',
  'Regen'
] as const

export type CapeStyle = (typeof STYLES)[number]

/** Kleiner, schneller Zufallsgenerator mit festem Startwert. */
function rng(seed: number): () => number {
  let s = seed >>> 0 || 1
  return () => {
    s ^= s << 13
    s ^= s >>> 17
    s ^= s << 5
    return ((s >>> 0) % 100000) / 100000
  }
}

/** Text -> Zahl, damit gleiche Eingabe gleiche Farben ergibt. */
function hash(text: string): number {
  let h = 2166136261
  for (let i = 0; i < text.length; i++) {
    h ^= text.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

/** Farbwörter schlagen den Zufall — „rot" soll auch rot werden. */
const WORD_HUES: [RegExp, number][] = [
  [/rot|feuer|flamme|blut|lava/i, 0],
  [/orange|sonne|herbst/i, 28],
  [/gelb|gold|blitz|honig/i, 48],
  [/gr[üu]n|wald|gift|natur|smaragd/i, 130],
  [/t[üu]rkis|cyan|eis|aqua|wasser/i, 180],
  [/blau|meer|ozean|himmel|frost/i, 210],
  [/lila|violett|magie|amethyst/i, 275],
  [/pink|rosa|sakura|kirsch/i, 320],
  [/schwarz|dunkel|nacht|schatten|void/i, -1],
  [/wei[sß]|silber|schnee|licht/i, -2]
]

/**
 * HSL -> Hex. Nötig, weil an mehreren Stellen Alpha als Hex-Suffix angehängt
 * wird ("#rrggbb" + "aa") — mit hsl()-Strings wirft das einen Fehler.
 */
function hslHex(h: number, s: number, l: number): string {
  const sat = s / 100
  const lig = l / 100
  const k = (n: number): number => (n + h / 30) % 12
  const a = sat * Math.min(lig, 1 - lig)
  const f = (n: number): number =>
    lig - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)))
  const to = (v: number): string =>
    Math.round(v * 255)
      .toString(16)
      .padStart(2, '0')
  return `#${to(f(0))}${to(f(8))}${to(f(4))}`
}

function paletteFor(text: string, rand: () => number): { a: string; b: string; c: string } {
  let hue = -3
  for (const [re, h] of WORD_HUES) {
    if (re.test(text)) {
      hue = h
      break
    }
  }
  if (hue === -1) return { a: '#05060a', b: '#141821', c: '#3b4354' } // dunkel
  if (hue === -2) return { a: '#e8eef5', b: '#aab8c7', c: '#ffffff' } // hell
  if (hue === -3) hue = Math.floor(rand() * 360)
  const h2 = (hue + 35 + Math.floor(rand() * 40)) % 360
  return {
    a: hslHex(hue, 70, 8),
    b: hslHex(hue, 78, 34),
    c: hslHex(h2, 90, 62)
  }
}

/**
 * Erzeugt die fertige Cape-Textur als Daten-URL (512x256, HD-Layout).
 * Gezeichnet wird nur der sichtbare Rückenteil; Kanten und Innenseite
 * leitet die Funktion daraus ab.
 */
/** Zeichnet nur den sichtbaren Rückenteil — die Grundlage jedes Einzelbildes. */
function panelZeichnen(text: string, style: CapeStyle, seed: number): HTMLCanvasElement {
  const rand = rng(hash(text) ^ (seed * 2654435761))
  const pal = paletteFor(text, rand)

  const w = 10 * K
  const h = 16 * K
  const panel = document.createElement('canvas')
  panel.width = w
  panel.height = h
  const g = panel.getContext('2d')!

  // Grundverlauf
  const base = g.createLinearGradient(0, 0, 0, h)
  base.addColorStop(0, pal.a)
  base.addColorStop(1, pal.b)
  g.fillStyle = base
  g.fillRect(0, 0, w, h)

  switch (style) {
    case 'Sternenhimmel': {
      for (let i = 0; i < 90; i++) {
        const s = rand() < 0.15 ? 3 : 2
        g.globalAlpha = 0.4 + rand() * 0.6
        g.fillStyle = rand() < 0.3 ? pal.c : '#ffffff'
        g.fillRect(Math.floor(rand() * w), Math.floor(rand() * h), s, s)
      }
      g.globalAlpha = 1
      break
    }
    case 'Streifen': {
      const count = 3 + Math.floor(rand() * 4)
      for (let i = 0; i < count; i++) {
        g.fillStyle = i % 2 ? pal.c : '#ffffff22'
        const x = Math.floor(rand() * w)
        g.fillRect(x, 0, Math.max(K, Math.floor(rand() * 2 * K)), h)
      }
      break
    }
    case 'Flammen': {
      for (let y = h; y > h * 0.25; y -= 2) {
        const t = 1 - y / h
        const width = w * (0.2 + t * 0.55) * (0.7 + rand() * 0.6)
        g.fillStyle = hslHex(20 + t * 35, 95, 45 + t * 25)
        g.globalAlpha = 0.35 + t * 0.5
        g.fillRect((w - width) / 2 + (rand() - 0.5) * K, y, width, 3)
      }
      g.globalAlpha = 1
      break
    }
    case 'Nebel': {
      for (let i = 0; i < 26; i++) {
        const r = (0.15 + rand() * 0.45) * w
        const grd = g.createRadialGradient(rand() * w, rand() * h, 0, rand() * w, rand() * h, r)
        grd.addColorStop(0, pal.c + '55')
        grd.addColorStop(1, 'transparent')
        g.fillStyle = grd
        g.fillRect(0, 0, w, h)
      }
      break
    }
    case 'Geometrie': {
      for (let i = 0; i < 16; i++) {
        g.save()
        g.translate(rand() * w, rand() * h)
        g.rotate(rand() * Math.PI)
        g.fillStyle = i % 3 ? pal.c + '99' : '#ffffff33'
        const s = (0.1 + rand() * 0.28) * w
        g.fillRect(-s / 2, -s / 2, s, s)
        g.restore()
      }
      break
    }
    case 'Aurora': {
      for (let i = 0; i < 7; i++) {
        const grd = g.createLinearGradient(0, i * (h / 7), w, (i + 2) * (h / 7))
        grd.addColorStop(0, 'transparent')
        grd.addColorStop(0.5, i % 2 ? pal.c + 'aa' : '#ffffff44')
        grd.addColorStop(1, 'transparent')
        g.fillStyle = grd
        g.beginPath()
        const y = (i + rand()) * (h / 7)
        g.moveTo(0, y)
        g.bezierCurveTo(w * 0.3, y - h * 0.1, w * 0.7, y + h * 0.12, w, y)
        g.lineTo(w, y + h * 0.1)
        g.bezierCurveTo(w * 0.7, y + h * 0.2, w * 0.3, y, 0, y + h * 0.08)
        g.closePath()
        g.fill()
      }
      break
    }
    case 'Regen': {
      for (let i = 0; i < 70; i++) {
        g.strokeStyle = rand() < 0.25 ? pal.c : '#ffffff55'
        g.lineWidth = 1 + rand()
        const x = rand() * w
        const y = rand() * h
        g.beginPath()
        g.moveTo(x, y)
        g.lineTo(x + K * 0.3, y + K * (1 + rand()))
        g.stroke()
      }
      break
    }
    default: {
      // „Verlauf": zusätzlicher Schimmer diagonal
      const shine = g.createLinearGradient(0, 0, w, h)
      shine.addColorStop(0, 'transparent')
      shine.addColorStop(0.5, pal.c + '66')
      shine.addColorStop(1, 'transparent')
      g.fillStyle = shine
      g.fillRect(0, 0, w, h)
    }
  }

  // dezente Abdunklung an den Rändern — wirkt weniger flach
  const vig = g.createRadialGradient(w / 2, h / 2, 0, w / 2, h / 2, Math.max(w, h) * 0.75)
  vig.addColorStop(0, 'transparent')
  vig.addColorStop(1, '#00000066')
  g.fillStyle = vig
  g.fillRect(0, 0, w, h)

  return panel
}

/**
 * Verschiebt das Muster um einen Bruchteil seiner Höhe und setzt den oben
 * herauslaufenden Teil unten wieder an. Dadurch wird JEDER Stil bewegt,
 * ohne dass die einzelnen Stile davon wissen müssen: Sterne wandern,
 * Streifen laufen, Flammen steigen.
 */
function phasePanel(panel: HTMLCanvasElement, phase: number): HTMLCanvasElement {
  const out = document.createElement('canvas')
  out.width = panel.width
  out.height = panel.height
  const g = out.getContext('2d')!
  const dy = Math.round(phase * panel.height)
  g.drawImage(panel, 0, dy)
  g.drawImage(panel, 0, dy - panel.height)
  return out
}

/** Setzt einen Rückenteil in das vollständige Cape-Layout. */
function komponieren(panel: HTMLCanvasElement): HTMLCanvasElement {
  const c = document.createElement('canvas')
  c.width = 64 * K
  c.height = 32 * K
  const ctx = c.getContext('2d')!
  ctx.drawImage(panel, 1 * K, 1 * K)
  ctx.drawImage(c, 1 * K, 1 * K, 10 * K, K, 1 * K, 0, 10 * K, K)
  ctx.drawImage(c, 1 * K, 16 * K, 10 * K, K, 1 * K, 17 * K, 10 * K, K)
  ctx.drawImage(c, 1 * K, 1 * K, K, 16 * K, 0, 1 * K, K, 16 * K)
  ctx.drawImage(c, 10 * K, 1 * K, K, 16 * K, 11 * K, 1 * K, K, 16 * K)
  ctx.drawImage(panel, 12 * K, 1 * K)
  ctx.fillStyle = 'rgba(0,0,0,0.45)'
  ctx.fillRect(12 * K, 1 * K, 10 * K, 16 * K)

  return c
}

/** Ruhendes Cape als Daten-URL (512×256). */
export function generateCape(text: string, style: CapeStyle, seed: number): string {
  return komponieren(panelZeichnen(text, style, seed)).toDataURL('image/png')
}

/**
 * Bewegtes Cape: mehrere Einzelbilder übereinander in EIN Bild gestapelt.
 * Der Mod erkennt am Seitenverhältnis, dass es ein Streifen ist, zerlegt ihn
 * und spielt die Bilder ab. NoRisk verlangt für animierte Capes Geld — hier
 * entstehen sie aus demselben Generator.
 */
export function generateAnimatedCape(
  text: string,
  style: CapeStyle,
  seed: number,
  frames = 12
): string {
  const panel = panelZeichnen(text, style, seed)
  const einzel = komponieren(panel)
  const blatt = document.createElement('canvas')
  blatt.width = einzel.width
  blatt.height = einzel.height * frames
  const g = blatt.getContext('2d')!
  for (let i = 0; i < frames; i++) {
    g.drawImage(komponieren(phasePanel(panel, i / frames)), 0, i * einzel.height)
  }
  return blatt.toDataURL('image/png')
}
