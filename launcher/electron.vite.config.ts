import { defineConfig, externalizeDepsPlugin } from 'electron-vite'
import react from '@vitejs/plugin-react'

/**
 * Electron 31 bringt Chrome 126 mit — deshalb direkt darauf übersetzen statt
 * auf alte Browser Rücksicht zu nehmen: weniger Hilfscode, kleineres Bündel.
 * Die Konsolenausgaben fliegen im Paket raus.
 */
const shared = {
  target: 'chrome126',
  minify: 'esbuild' as const,
  cssMinify: true,
  reportCompressedSize: false
}

export default defineConfig({
  main: {
    plugins: [externalizeDepsPlugin()],
    build: { ...shared, target: 'node20' }
  },
  preload: {
    plugins: [externalizeDepsPlugin()],
    build: { ...shared, target: 'node20' }
  },
  renderer: {
    plugins: [react()],
    esbuild: { drop: ['console', 'debugger'], legalComments: 'none' },
    build: {
      ...shared,
      chunkSizeWarningLimit: 900,
      rollupOptions: {
        output: {
          // React getrennt halten: ändert sich selten, lädt einmal und bleibt
          manualChunks: (id: string) =>
            id.includes('node_modules/react') || id.includes('node_modules/scheduler')
              ? 'react'
              : undefined
        }
      }
    }
  }
})
