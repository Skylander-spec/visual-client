import { useEffect, useRef, useState } from 'react'
import { SkinViewer, WalkingAnimation } from 'skinview3d'

/**
 * 3D-Skin-Viewer (skinview3d/three.js) — mit der Maus drehbar.
 * Lädt Skin + aktives Cape frisch aus dem Main-Prozess (ohne Cache),
 * damit Skin-/Cape-Wechsel sofort sichtbar sind. Fallback: mc-heads.net.
 */
export default function SkinView({
  name,
  animate,
  width = 300,
  height = 380
}: {
  name: string
  animate: boolean
  width?: number
  height?: number
}): JSX.Element {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const viewerRef = useRef<SkinViewer | null>(null)
  const [tex, setTex] = useState<{ skin: string | null; cape: string | null; slim: boolean }>({
    skin: null,
    cape: null,
    slim: false
  })

  // Texturen (Skin + Cape) aus dem Main-Prozess holen
  useEffect(() => {
    let alive = true
    window.visual.skins
      .textures()
      .then((t) => alive && setTex(t))
      .catch(() => {})
    return () => {
      alive = false
    }
  }, [name])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    let viewer: SkinViewer
    try {
      viewer = new SkinViewer({
        canvas,
        width,
        height,
        skin: tex.skin ?? `https://mc-heads.net/skin/${encodeURIComponent(name)}`,
        model: tex.slim ? 'slim' : 'default'
      })
    } catch {
      return
    }
    viewerRef.current = viewer
    viewer.controls.enableZoom = false
    viewer.controls.enablePan = false
    viewer.autoRotate = false
    viewer.camera.position.set(20, 5, 42)
    viewer.zoom = 0.9

    if (tex.cape) {
      viewer.loadCape(tex.cape).catch(() => {})
    }
    return () => {
      viewer.dispose()
      viewerRef.current = null
    }
  }, [name, width, height, tex.skin, tex.cape, tex.slim])

  useEffect(() => {
    const viewer = viewerRef.current
    if (!viewer) return
    try {
      if (animate) {
        const anim = new WalkingAnimation()
        anim.speed = 0.55
        viewer.animation = anim
      } else {
        viewer.animation = null
      }
    } catch {
      /* Viewer entsorgt (HMR) */
    }
  }, [animate, name, tex.skin])

  return <canvas ref={canvasRef} style={{ cursor: 'grab', outline: 'none' }} />
}
