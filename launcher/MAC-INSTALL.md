# Visual Client auf dem MacBook installieren

## Kurzfassung

1. Ordner aus dem ZIP irgendwohin entpacken (z. B. nach `Dokumente`)
2. **Node.js** installieren, falls noch nicht vorhanden: https://nodejs.org (LTS)
3. Doppelklick auf **`install-mac.command`**
4. Am Ende öffnet sich die fertige `.dmg` — Visual Client in den Programme-Ordner ziehen

Beim ersten Start meldet macOS „unbekannter Entwickler", weil die App nicht bei
Apple signiert ist. Dann Rechtsklick auf die App → **Öffnen** → **Öffnen**.

## Falls der Doppelklick nichts tut

macOS blockiert manchmal heruntergeladene Skripte. Terminal öffnen, in den
Ordner wechseln und die Ausführung erlauben:

```bash
cd /Pfad/zum/Ordner
chmod +x install-mac.command
./install-mac.command
```

## Was das Skript macht

- prüft, ob Node.js 18+ vorhanden ist
- lädt die Abhängigkeiten (`npm install`)
- baut die App (`electron-vite build`)
- schnürt ein `.dmg` passend zu deinem Mac — Apple Silicon (arm64) oder Intel (x64)

## Warum kein fertiges DMG im ZIP liegt

Ein macOS-Paket lässt sich nur **auf** macOS bauen; electron-builder bricht auf
Windows mit „Build for macOS is supported only on macOS" ab. Deshalb liegt hier
der Quellcode plus Bauskript — auf dem Mac ist es dann ein Doppelklick.

## Was schon mit drin ist

- der Launcher selbst (Profile, Microsoft-/Offline-Login, Modrinth-Browser, Skins, Capes)
- die In-Game-Mods für **Minecraft 1.21.2 bis 1.21.9** (je Version eine eigene Jar)
- die Branding-Mod (Fenstertitel + Icon)
- Icons und das Visual-Client-Design

## Daten und Speicherorte

Der Launcher legt seine Daten unter macOS hier ab:

```
~/Library/Application Support/.visualclient/
```

Darin: `profiles.json`, `accounts.json`, `instances/`, `capes/`, `skins/`.
Profile und Capes von Windows kannst du einfach in diesen Ordner kopieren.

## Bekannte Einschränkungen auf dem Mac

- Die App ist **nicht signiert und nicht notarisiert** — daher die Meldung beim ersten Start.
- Für **Minecraft 26.2** gibt es noch keine Mod-Builds; der Launcher startet solche
  Profile ohne unsere Mods (statt sie mit einem Absturz zu quittieren).
