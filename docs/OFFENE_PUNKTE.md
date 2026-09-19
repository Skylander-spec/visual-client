# Offene Punkte / nächste Schritte

1. **JDK 17 installieren** — ohne Java kann weder der Forge-Mod gebaut noch MC 1.20.x
   gestartet werden. Empfehlung: [Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17).
   Für 1.8.9 zusätzlich ein Java 8 (Temurin 8).
2. **1.8.9-Mod-Port** — VisualsMod targetet Forge 1.20.1. Die 1.8.9-Forge-API ist komplett
   anders (kein DeferredRegister, GuiIngame statt GuiGraphics); der Port ist ein eigenes
   Teilprojekt. Das Resource Pack funktioniert mit angepasstem `pack_format: 1` auch auf 1.8.9.
3. **Referenz-Screenshots** — reallyvisuals.me ist eine JS-Single-Page-App und konnte nicht
   direkt als Designvorlage gelesen werden. Für Fein-Tuning des Looks: Screenshots schicken,
   dann werden Farben/Layout im Launcher-Theme (`launcher/src/theme.css`) und in der
   Textur-Pipeline nachgezogen.
4. **Gradle Wrapper** — `mod/` enthält Build-Skripte, aber kein Wrapper-Binary
   (`gradle-wrapper.jar`). Einmalig `gradle wrapper --gradle-version 8.7` im `mod/`-Ordner
   ausführen (oder Gradle 8.x direkt nutzen).
5. **Sound-Feinschliff** — die Sounds sind synthetisch generiert (kurz, dezent). Wenn ein
   bestimmter Sound-Charakter gewünscht ist (z. B. „Bell"-Hitsound wie bei bekannten Packs):
   Referenz nennen, Synthese-Parameter in `resourcepack/tools/generate_sounds.py` anpassen.
6. **Code-Signing / Installer** — `npm run build` erzeugt eine portable App. Für einen
   „echten" Installer (NSIS) und SmartScreen-freie Verteilung wäre ein Zertifikat nötig.
7. **Cape-Sync online** — Capes werden aktuell lokal gerendert (nur du siehst sie).
   Server-seitige Cosmetics (andere sehen dein Cape) bräuchten einen eigenen Backend-Dienst.
