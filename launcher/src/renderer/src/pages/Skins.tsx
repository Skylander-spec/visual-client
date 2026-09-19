import { useEffect, useRef, useState } from 'react'
import type { Account, CosmeticFile } from '../env'
import { useT } from '../lib/i18n'

const SKIN_SITES = [
  { label: 'NameMC', url: 'https://de.namemc.com/minecraft-skins' },
  { label: 'MinecraftSkins', url: 'https://www.minecraftskins.com/' },
  { label: 'Planet MC', url: 'https://www.planetminecraft.com/skins/' }
]

/** Zeichnet Kopf+Körper-Frontansicht aus einer 64x64-Skin-Datei. */
/** Schmale Arme (Alex) erkennt man an den leeren Pixeln neben dem Arm. */
function isSlim(img: HTMLImageElement): boolean {
  const c = document.createElement('canvas')
  c.width = img.width
  c.height = img.height
  const g = c.getContext('2d')!
  g.drawImage(img, 0, 0)
  try {
    return g.getImageData(46, 52, 1, 1).data[3] === 0
  } catch {
    return false
  }
}

function SkinPreview({ dataUrl, size = 132 }: { dataUrl: string; size?: number }): JSX.Element {
  const ref = useRef<HTMLCanvasElement>(null)
  useEffect(() => {
    const canvas = ref.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')!
    ctx.imageSmoothingEnabled = false
    const img = new Image()
    img.onload = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      // Ganze Figur statt nur Kopf + Rumpf: Beine, Arme, Kopf und jeweils die
      // zweite Ebene (Hut/Jacke), damit der Skin so aussieht wie im Spiel.
      // Figur ist 32 Einheiten hoch (8 Kopf + 12 Rumpf + 12 Beine) und 16 breit.
      // Mit size/20 lagen die Beine ausserhalb der Flaeche und fehlten.
      const u = size / 32
      const px = (sx: number, sy: number, sw: number, sh: number,
                  dx: number, dy: number): void =>
        ctx.drawImage(img, sx, sy, sw, sh, dx * u, dy * u, sw * u, sh * u)
      // Alex-Skins haben 3 statt 4 Pixel breite Arme
      const aw = img.width >= 64 && isSlim(img) ? 3 : 4

      // Körper (Ursprung x=6 ist die linke Rumpfkante)
      px(20, 20, 8, 12, 6, 8) // Rumpf
      px(44, 20, aw, 12, 6 - aw, 8) // rechter Arm
      px(36, 52, aw, 12, 14, 8) // linker Arm
      px(4, 20, 4, 12, 6, 20) // rechtes Bein
      px(20, 52, 4, 12, 10, 20) // linkes Bein
      px(8, 8, 8, 8, 6, 0) // Kopf
      // zweite Ebene (Jacke, Ärmel, Hosen, Hut)
      px(20, 36, 8, 12, 6, 8)
      px(44, 36, aw, 12, 6 - aw, 8)
      px(52, 52, aw, 12, 14, 8)
      px(4, 36, 4, 12, 6, 20)
      px(4, 52, 4, 12, 10, 20)
      px(40, 8, 8, 8, 6, 0)
    }
    img.src = dataUrl
  }, [dataUrl, size])
  // 16 breit zu 32 hoch — Fläche im selben Verhältnis, sonst wird beschnitten
  return (
    <div className="skinstage">
      <canvas
        ref={ref}
        width={size * 0.5}
        height={size}
        style={{ imageRendering: 'pixelated' }}
      />
    </div>
  )
}

export default function Skins({ account }: { account: Account }): JSX.Element {
  const t = useT()
  const [skins, setSkins] = useState<CosmeticFile[]>([])
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState<string | null>(null)
  const [renaming, setRenaming] = useState<string | null>(null)
  const [draft, setDraft] = useState('')

  const reload = (): void => {
    window.visual.skins.list().then(setSkins)
  }
  useEffect(reload, [])

  async function add(): Promise<void> {
    const added = await window.visual.skins.add(t('skins.pickFile'))
    if (added) {
      setMsg(t('skins.added', { name: added.name }))
      reload()
    }
  }

  async function apply(name: string, variant: 'classic' | 'slim'): Promise<void> {
    setBusy(name)
    setErr('')
    setMsg('')
    try {
      setMsg(await window.visual.skins.apply(name, variant))
    } catch (e) {
      setErr(e instanceof Error ? e.message : t('skins.failed'))
    } finally {
      setBusy(null)
    }
  }

  async function commitRename(oldName: string): Promise<void> {
    const newName = draft.trim()
    setRenaming(null)
    if (newName && newName !== oldName) {
      await window.visual.skins.rename(oldName, newName)
      reload()
    }
  }

  return (
    <>
      <div className="row" style={{ marginBottom: 18, flexWrap: 'wrap' }}>
        <div className="grow">
          <h1>{t('skins.title')}</h1>
          <p className="sub" style={{ marginBottom: 0 }}>
            {t('skins.sub')}
          </p>
        </div>
        <div className="searchdrop">
          <button className="btn primary tabchip">{'🔍 ' + t('skins.search')}</button>
          <div className="searchmenu">
            {SKIN_SITES.map((s) => (
              <button key={s.url} onClick={() => window.visual.openExternal(s.url)}>
                {t('skins.openSite', { name: s.label })}
              </button>
            ))}
          </div>
        </div>
        <button className="btn tabchip" onClick={add}>
          {'+ ' + t('skins.import')}
        </button>
      </div>

      {account.type !== 'microsoft' && (
        <div className="muted" style={{ marginBottom: 14 }}>
          {t('skins.mojangHint')}
        </div>
      )}
      {msg && <div className="ok" style={{ marginBottom: 10 }}>{msg}</div>}
      {err && <div className="error" style={{ marginBottom: 10 }}>{err}</div>}

      <div className="cards">
        {skins.map((s) => (
          <div key={s.name} className="pcard" style={{ alignItems: 'center' }}>
            <SkinPreview dataUrl={s.dataUrl} />
            {renaming === s.name ? (
              <input
                autoFocus
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                onBlur={() => commitRename(s.name)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') commitRename(s.name)
                  if (e.key === 'Escape') setRenaming(null)
                }}
                style={{ width: '100%', textAlign: 'center' }}
              />
            ) : (
              <div
                className="pname editable"
                title={t('skins.rename')}
                onClick={() => {
                  setRenaming(s.name)
                  setDraft(s.name)
                }}
              >
                {s.name} <span className="pencil">✎</span>
              </div>
            )}
            <div className="row" style={{ width: '100%' }}>
              <button
                className="btn primary grow"
                disabled={busy !== null}
                onClick={() => apply(s.name, 'slim')}
              >
                {busy === s.name ? '…' : t('skins.slim')}
              </button>
              <button
                className="btn grow"
                disabled={busy !== null}
                onClick={() => apply(s.name, 'classic')}
              >
                {t('skins.classic')}
              </button>
              <button
                className="btn danger"
                title={t('common.delete')}
                onClick={async () => {
                  await window.visual.skins.remove(s.name)
                  reload()
                }}
              >
                ✕
              </button>
            </div>
          </div>
        ))}
        <div className="pcard newcard" onClick={add}>
          {t('skins.importHint')}
        </div>
      </div>
    </>
  )
}
