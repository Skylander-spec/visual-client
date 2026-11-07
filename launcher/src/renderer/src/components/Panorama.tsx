import { useEffect, useRef } from 'react'
import { CubeTextureLoader, PerspectiveCamera, Scene, SRGBColorSpace, WebGLRenderer } from 'three'

export default function Panorama({ animated }: { animated: boolean }): JSX.Element {
  const host = useRef<HTMLDivElement>(null)
  const moving = useRef(animated)
  moving.current = animated
  useEffect(() => {
    const container = host.current!
    let disposed = false
    let frame = 0
    let renderer: WebGLRenderer | undefined
    let texture: ReturnType<CubeTextureLoader['load']> | undefined
    let resize: ResizeObserver | undefined
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)')
    void window.visual.panorama().then(faces => {
      if (disposed || !faces || faces.length !== 6) return
      texture = new CubeTextureLoader().load([faces[1], faces[3], faces[4], faces[5], faces[0], faces[2]], cube => {
        if (disposed) { cube.dispose(); return }
        try {
          renderer = new WebGLRenderer({ antialias: false, powerPreference: 'low-power' })
          renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5))
          cube.colorSpace = SRGBColorSpace
          const scene = new Scene()
          scene.background = cube
          const camera = new PerspectiveCamera(78, 1, .1, 10)
          camera.rotation.order = 'YXZ'
          camera.rotation.x = -.08
          container.appendChild(renderer.domElement)
          resize = new ResizeObserver(() => {
            const width = container.clientWidth, height = container.clientHeight
            if (!width || !height || !renderer) return
            renderer.setSize(width, height)
            camera.aspect = width / height
            camera.updateProjectionMatrix()
            renderer.render(scene, camera)
          })
          resize.observe(container)
          let last = 0
          const draw = (time: number): void => {
            if (disposed || !renderer) return
            frame = requestAnimationFrame(draw)
            if (document.hidden || !moving.current || preference.matches) { last = time; return }
            if (time - last < 1000 / 24) return
            const elapsed = Math.min((time - last) / 1000, .1)
            last = time
            camera.rotation.y += elapsed * Math.PI / 90
            renderer.render(scene, camera)
          }
          frame = requestAnimationFrame(draw)
        } catch { renderer?.dispose(); container.replaceChildren() }
      }, undefined, () => { texture?.dispose() })
    }).catch(() => { /* Static landscape stays visible without local assets or WebGL. */ })
    return () => {
      disposed = true
      cancelAnimationFrame(frame)
      resize?.disconnect()
      texture?.dispose()
      renderer?.dispose()
      container.replaceChildren()
    }
  }, [])
  return <div className="panorama-scene" ref={host} aria-hidden="true" />
}
