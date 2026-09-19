import { useEffect, useMemo, useState } from 'react'
import type { CosmeticFile } from '../env'
import { generateCape, generateAnimatedCape, STYLES, type CapeStyle } from '../lib/capeGen'
import { useT } from '../lib/i18n'
import { errorText } from '../lib/fehler'

/** Eingebaute Cape-Designs — werden clientseitig als 64x32-PNG gerendert. */
const PRESETS: { name: string; draw: (ctx: CanvasRenderingContext2D) => void }[] = [
  {
    name: 'Visual Teal',
    draw: (ctx) => {
      grad(ctx, '#0a1514', '#14b8a6')
      vStripe(ctx, '#2dd4bf')
    }
  },
  {
    name: 'Nachthimmel',
    draw: (ctx) => {
      grad(ctx, '#050514', '#1e1b4b')
      for (let i = 0; i < 40; i++) {
        ctx.fillStyle = i % 3 ? '#e2e8f0' : '#a5b4fc'
        ctx.fillRect(Math.floor(Math.random() * 64), Math.floor(Math.random() * 32), 1, 1)
      }
    }
  },
  {
    name: 'Blutmond',
    draw: (ctx) => {
      grad(ctx, '#180505', '#7f1d1d')
      ctx.fillStyle = '#ef4444'
      ctx.beginPath()
      ctx.arc(16, 12, 5, 0, Math.PI * 2)
      ctx.fill()
    }
  },
  {
    name: 'Sakura',
    draw: (ctx) => {
      grad(ctx, '#1c0f14', '#4c1d34')
      for (let i = 0; i < 25; i++) {
        ctx.fillStyle = i % 2 ? '#f9a8d4' : '#fbcfe8'
        ctx.fillRect(Math.floor(Math.random() * 64), Math.floor(Math.random() * 32), 1, 1)
      }
    }
  },
  {
    name: 'Gold',
    draw: (ctx) => {
      grad(ctx, '#1a1205', '#a16207')
      vStripe(ctx, '#facc15')
    }
  },
  {
    name: 'Schwarz',
    draw: (ctx) => {
      grad(ctx, '#000000', '#18181b')
    }
  }
]

function grad(ctx: CanvasRenderingContext2D, a: string, b: string): void {
  const g = ctx.createLinearGradient(0, 0, 0, 32)
  g.addColorStop(0, a)
  g.addColorStop(1, b)
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 64, 32)
}

function vStripe(ctx: CanvasRenderingContext2D, color: string): void {
  ctx.fillStyle = color
  ctx.fillRect(14, 1, 2, 30)
}

function renderPreset(draw: (ctx: CanvasRenderingContext2D) => void): string {
  const c = document.createElement('canvas')
  c.width = 64
  c.height = 32
  draw(c.getContext('2d')!)
  return c.toDataURL('image/png')
}

/** Cape-Vorschau: der sichtbare Rückenteil (1,1 bis 11,17 im 64x32-Layout). */
function CapePreview({ dataUrl }: { dataUrl: string }): JSX.Element {
  return (
    <div
      style={{
        width: 80,
        height: 128,
        borderRadius: 8,
        border: '1px solid var(--border-hi)',
        backgroundImage: `url(${dataUrl})`,
        backgroundPosition: '-8px -8px',
        backgroundSize: '512px 256px',
        imageRendering: 'pixelated'
      }}
    />
  )
}

export default function Capes(): JSX.Element {
  const t = useT()
  const [capes, setCapes] = useState<CosmeticFile[]>([])
  const [active, setActive] = useState<string | null>(null)
  const [msg, setMsg] = useState('')
  const [renaming, setRenaming] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [genText, setGenText] = useState('')
  const [genStyle, setGenStyle] = useState<CapeStyle>('Verlauf')
  const [genSeed, setGenSeed] = useState(1)

  // Vorschau neu rechnen, sobald sich Text, Stil oder Zufallszahl ändern
  const genPreview = useMemo(
    () => generateCape(genText, genStyle, genSeed),
    [genText, genStyle, genSeed]
  )

  async function saveGenerated(): Promise<void> {
    const base = genText.trim() || genStyle
    const name = `${base} ${genSeed}`.slice(0, 32)
    await window.visual.capes.savePreset(name, genPreview)
    setMsg(t('capes.created', { name }))
    reload()
  }

  /**
   * Bewegtes Cape: zwölf Einzelbilder in einem Streifen. Der Mod erkennt das
   * am Seitenverhältnis und spielt sie ab.
   */
  async function saveAnimated(): Promise<void> {
    const base = genText.trim() || genStyle
    const name = `${base} ${genSeed} ✦`.slice(0, 32)
    await window.visual.capes.savePreset(name, generateAnimatedCape(genText, genStyle, genSeed))
    setMsg(t('capes.createdAnimated', { name }))
    reload()
  }

  const reload = (): void => {
    window.visual.capes.list().then(setCapes)
    window.visual.capes.active().then(setActive)
  }
  useEffect(reload, [])

  async function commitRename(oldName: string): Promise<void> {
    const newName = draft.trim()
    setRenaming(null)
    if (newName && newName !== oldName) {
      await window.visual.capes.rename(oldName, newName)
      reload()
    }
  }

  /**
   * Beliebiges Bild in eine gültige 64x32-Cape-Textur rechnen: Motiv passgenau
   * (Cover-Fit) auf den sichtbaren Rückenteil, Ränder aus den Kantenpixeln
   * gezogen und die Innenseite abgedunkelt — sonst sieht man beim Laufen
   * durchsichtige Nähte.
   */
  async function importImage(file: File): Promise<void> {
    const url = URL.createObjectURL(file)
    try {
      const img = new Image()
      await new Promise<void>((resolve, reject) => {
        img.onload = () => resolve()
        img.onerror = () => reject(new Error(t('capes.readFailed')))
        img.src = url
      })

      // HD: gleiches Layout, nur 8x so groß. Unser Cape-Renderer im Mod nutzt
      // proportionale UVs, dadurch bleibt das Motiv scharf statt 10x16-Brei.
      const K = 8
      const c = document.createElement('canvas')
      c.width = 64 * K
      c.height = 32 * K
      const ctx = c.getContext('2d')!

      // Ausschnitt mittig auf das Seitenverhältnis des Capes (10:16) bringen
      const ratio = 10 / 16
      let sw = img.width
      let sh = img.height
      let sx = 0
      let sy = 0
      if (img.width / img.height > ratio) {
        sw = img.height * ratio
        sx = (img.width - sw) / 2
      } else {
        sh = img.width / ratio
        sy = (img.height - sh) / 2
      }

      ctx.drawImage(img, sx, sy, sw, sh, 1 * K, 1 * K, 10 * K, 16 * K) // Rückseite (sichtbar)
      // Kanten aus den Randpixeln ziehen
      ctx.drawImage(c, 1 * K, 1 * K, 10 * K, K, 1 * K, 0, 10 * K, K)
      ctx.drawImage(c, 1 * K, 16 * K, 10 * K, K, 1 * K, 17 * K, 10 * K, K)
      ctx.drawImage(c, 1 * K, 1 * K, K, 16 * K, 0, 1 * K, K, 16 * K)
      ctx.drawImage(c, 10 * K, 1 * K, K, 16 * K, 11 * K, 1 * K, K, 16 * K)
      // Innenseite: gleiches Motiv, abgedunkelt
      ctx.drawImage(img, sx, sy, sw, sh, 12 * K, 1 * K, 10 * K, 16 * K)
      ctx.fillStyle = 'rgba(0,0,0,0.45)'
      ctx.fillRect(12 * K, 1 * K, 10 * K, 16 * K)

      const name = file.name.replace(/\.[a-z0-9]+$/i, '').slice(0, 32) || 'Cape'
      await window.visual.capes.savePreset(name, c.toDataURL('image/png'))
      setMsg(t('capes.imported', { name }))
      reload()
    } catch (e) {
      setMsg(errorText(e, t))
    } finally {
      URL.revokeObjectURL(url)
    }
  }

  async function ensurePresets(): Promise<void> {
    for (const p of PRESETS) {
      await window.visual.capes.savePreset(p.name, renderPreset(p.draw))
    }
    setMsg(t('capes.presetsAdded'))
    reload()
  }

  async function activate(name: string | null): Promise<void> {
    await window.visual.capes.setActive(name)
    setActive(name)
    setMsg(
      name
        ? `Cape „${name}" ist aktiv — sichtbar im Spiel mit VisualsMod (Forge-Profil).`
        : t('capes.deactivated')
    )
  }

  return (
    <>
      <div className="row" style={{ marginBottom: 18 }}>
        <div className="grow">
          <h1>{t('capes.title')}</h1>
          <p className="sub" style={{ marginBottom: 0 }}>
            {t('capes.sub')}
          </p>
        </div>
        <div className="searchdrop">
          <button className="btn primary tabchip">{'🔍 ' + t('capes.search')}</button>
          <div className="searchmenu">
            <button onClick={() => window.visual.openExternal('https://de.namemc.com/capes')}>
              {t('capes.nameMc')}
            </button>
            <button
              onClick={() =>
                window.visual.openExternal(
                  'https://www.google.com/search?q=minecraft+cape+texture+64x32+png&tbm=isch'
                )
              }
            >
              {t('capes.imageSearch')}
            </button>
            <button
              onClick={() =>
                window.visual.openExternal(
                  'https://www.google.com/search?q=minecraft+optifine+cape+64x32+download&tbm=isch'
                )
              }
            >
              OptiFine-Cape-{t('capes.templates')} ↗
            </button>
          </div>
        </div>
        <button className="btn tabchip" onClick={ensurePresets}>
          {t('capes.templates')}
        </button>
        <button
          className="btn tabchip"
          onClick={async () => {
            const added = await window.visual.capes.add(t('capes.pickFile'))
            if (added) reload()
          }}
        >
          {'⬆ ' + t('capes.upload')}
        </button>
        <label className="btn tabchip primary" style={{ cursor: 'pointer' }}>
          {'✦ ' + t('capes.imageAsCape')}
          <input
            type="file"
            accept="image/*"
            style={{ display: 'none' }}
            onChange={(e) => {
              const file = e.target.files?.[0]
              e.target.value = ''
              if (file) importImage(file)
            }}
          />
        </label>
      </div>

      {msg && <div className="ok" style={{ marginBottom: 12 }}>{msg}</div>}
      {active && (
        <div className="row" style={{ marginBottom: 12 }}>
          <span className="badge">Aktiv: {active}</span>
          <button className="btn" style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => activate(null)}>
            {t('capes.deactivate')}
          </button>
        </div>
      )}

      <div className="capegen">
        <CapePreview dataUrl={genPreview} />
        <div className="capegen-form">
          <div className="capegen-head">{'✦ ' + t('capes.generate')}</div>
          <p className="muted" style={{ margin: '0 0 10px' }}>
            {t('capes.genDesc')}
          </p>
          <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
            <input
              className="grow"
              placeholder={t('capes.keywords')}
              value={genText}
              onChange={(e) => setGenText(e.target.value)}
              style={{ minWidth: 200 }}
            />
            <select value={genStyle} onChange={(e) => setGenStyle(e.target.value as CapeStyle)}>
              {STYLES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>
          <div className="row" style={{ gap: 8, marginTop: 10 }}>
            <button className="btn grow" onClick={() => setGenSeed(Math.floor(Math.random() * 99999))}>
              {'🎲 ' + t('capes.roll')}
            </button>
            <button className="btn grow" onClick={saveGenerated}>
              {t('common.save')}
            </button>
            <button className="btn primary grow" onClick={saveAnimated}>
              {'✦ ' + t('capes.animated')}
            </button>
          </div>
        </div>
      </div>

      <div className="cards">
        {capes.map((c) => (
          <div key={c.name} className="pcard" style={{ alignItems: 'center' }}>
            <CapePreview dataUrl={c.dataUrl} />
            {renaming === c.name ? (
              <input
                autoFocus
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                onBlur={() => commitRename(c.name)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') commitRename(c.name)
                  if (e.key === 'Escape') setRenaming(null)
                }}
                style={{ width: '100%', textAlign: 'center' }}
              />
            ) : (
              <div
                className="pname editable"
                title={t('capes.clickRename')}
                onClick={() => {
                  setRenaming(c.name)
                  setDraft(c.name)
                }}
              >
                {c.name} <span className="pencil">✎</span>
              </div>
            )}
            <div className="row" style={{ width: '100%' }}>
              <button
                className={`btn grow ${active === c.name ? '' : 'primary'}`}
                onClick={() => activate(active === c.name ? null : c.name)}
              >
                {active === c.name ? '✓ Aktiv' : 'Anwenden'}
              </button>
              <button
                className="btn danger"
                onClick={async () => {
                  await window.visual.capes.remove(c.name)
                  reload()
                }}
              >
                ✕
              </button>
            </div>
          </div>
        ))}
        {capes.length === 0 && (
          <div className="pcard newcard" onClick={ensurePresets}>
            {t('capes.loadPresets', { kind: t('capes.templates') })}
          </div>
        )}
      </div>
    </>
  )
}
