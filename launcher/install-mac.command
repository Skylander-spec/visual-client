#!/bin/bash
# Visual Client — Installation auf macOS.
# Doppelklick genügt. Baut die App aus dem Quellcode und legt sie in /Applications.
set -e
cd "$(dirname "$0")"

BLUE='\033[1;36m'; RED='\033[1;31m'; GREEN='\033[1;32m'; OFF='\033[0m'
echo -e "${BLUE}"
echo "  VISUAL CLIENT — Installation für macOS"
echo -e "${OFF}"

if ! command -v node >/dev/null 2>&1; then
  echo -e "${RED}Node.js fehlt.${OFF}"
  echo "Installiere es einmalig von https://nodejs.org (LTS-Version), dann dieses"
  echo "Skript erneut doppelklicken."
  read -r -p "Enter zum Schließen..."
  exit 1
fi

NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
if [ "$NODE_MAJOR" -lt 18 ]; then
  echo -e "${RED}Node.js $NODE_MAJOR ist zu alt — benötigt wird 18 oder neuer.${OFF}"
  read -r -p "Enter zum Schließen..."
  exit 1
fi

echo "› Abhängigkeiten installieren (dauert beim ersten Mal einige Minuten) ..."
npm install --no-audit --no-fund

echo "› App bauen ..."
npx electron-vite build

ARCH="$(uname -m)"   # arm64 = Apple Silicon, x86_64 = Intel
if [ "$ARCH" = "arm64" ]; then TARGET_ARCH="--arm64"; else TARGET_ARCH="--x64"; fi
echo "› Paket für $ARCH schnüren ..."
npx electron-builder --mac dmg "$TARGET_ARCH"

DMG="$(find dist-app -name '*.dmg' -maxdepth 2 2>/dev/null | head -1)"
if [ -z "$DMG" ]; then
  DMG="$(find . -name 'Visual-Client-*.dmg' -maxdepth 3 2>/dev/null | head -1)"
fi

if [ -n "$DMG" ]; then
  echo -e "${GREEN}"
  echo "  Fertig! Installationsdatei:"
  echo "  $DMG"
  echo -e "${OFF}"
  echo "› Öffne sie — zieh das Visual-Client-Symbol in den Programme-Ordner."
  open "$DMG"
else
  echo -e "${RED}Kein DMG gefunden. Die Ausgabe oben zeigt, woran es lag.${OFF}"
fi

echo
echo "Hinweis: Beim ersten Start meldet macOS, dass die App von einem"
echo "unbekannten Entwickler stammt (sie ist nicht bei Apple signiert)."
echo "Dann: Rechtsklick auf die App -> Öffnen -> Öffnen bestätigen."
read -r -p "Enter zum Schließen..."
