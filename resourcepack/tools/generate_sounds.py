"""Visual Pack — Sound-Pipeline.

Synthetisiert Hit-/Kill-/UI-Sounds (Originale, keine kopierten Assets).
Minecraft braucht .ogg — bevorzugt wird `soundfile` (schreibt Vorbis direkt),
Fallback: WAV schreiben und mit ffmpeg konvertieren, falls vorhanden.

Aufruf:  py tools/generate_sounds.py   (aus resourcepack/)
"""
from __future__ import annotations

import os
import shutil
import subprocess
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "pack", "assets",
                   "visualpack", "sounds")


def env_exp(n, decay):
    return np.exp(-np.linspace(0, decay, n))


def tone(freq, dur, decay=8.0, harmonics=((1, 1.0),)):
    n = int(SR * dur)
    t = np.linspace(0, dur, n, endpoint=False)
    sig = np.zeros(n)
    for mult, amp in harmonics:
        sig += amp * np.sin(2 * np.pi * freq * mult * t)
    return sig * env_exp(n, decay)


def noise_burst(dur, decay=40.0, lowpass=0.3):
    n = int(SR * dur)
    sig = np.random.default_rng(4).uniform(-1, 1, n)
    for _ in range(3):  # simpler Tiefpass
        sig = lowpass * sig + (1 - lowpass) * np.concatenate([[0], sig[:-1]])
    return sig * env_exp(n, decay)


def normalize(sig, peak=0.7):
    m = np.max(np.abs(sig))
    return sig * (peak / m) if m > 0 else sig


def mix(a, b):
    n = max(len(a), len(b))
    out = np.zeros(n)
    out[: len(a)] += a
    out[: len(b)] += b
    return out


def hit_sound():
    """Heller 'Tink' — klassischer PvP-Hitsound-Charakter."""
    s = tone(1245, 0.14, decay=26, harmonics=((1, 1.0), (2.76, 0.4), (5.4, 0.15)))
    s = mix(s, 0.35 * noise_burst(0.03, decay=90))
    return normalize(s)


def kill_sound():
    """Zweiton-Chime aufsteigend — kurz, befriedigend, nicht aufdringlich."""
    a = tone(660, 0.16, decay=14, harmonics=((1, 1.0), (2, 0.3)))
    b = tone(990, 0.30, decay=10, harmonics=((1, 1.0), (2, 0.25), (3, 0.1)))
    s = np.concatenate([a, np.zeros(int(SR * 0.02))])
    out = np.zeros(len(s) + len(b))
    out[: len(s)] += s
    out[len(s):] += b
    return normalize(out, 0.75)


def ui_click():
    """Weicher, kurzer Klick fuers Launcher-/Menue-UI."""
    s = tone(2200, 0.05, decay=60, harmonics=((1, 1.0), (0.5, 0.5)))
    s = mix(s, 0.4 * noise_burst(0.015, decay=160, lowpass=0.6))
    return normalize(s, 0.5)


def write_wav(path, sig):
    data = (sig * 32767).astype(np.int16)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())


def export(name, sig):
    os.makedirs(OUT, exist_ok=True)
    ogg = os.path.join(OUT, name + ".ogg")
    try:
        import soundfile as sf
        sf.write(ogg, sig, SR, format="OGG", subtype="VORBIS")
        print("  +", name + ".ogg  (soundfile)")
        return
    except Exception:
        pass
    wav = os.path.join(OUT, name + ".wav")
    write_wav(wav, sig)
    if shutil.which("ffmpeg"):
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav,
                        "-c:a", "libvorbis", "-q:a", "4", ogg], check=True)
        os.remove(wav)
        print("  +", name + ".ogg  (ffmpeg)")
    else:
        print("  !", name + ".wav geschrieben — Minecraft braucht .ogg!")
        print("    Fix:  py -m pip install soundfile   und erneut ausfuehren.")


def main():
    print("Visual Pack Sounds werden generiert...")
    export("hit", hit_sound())
    export("kill", kill_sound())
    export("ui_click", ui_click())
    print("Fertig.")


if __name__ == "__main__":
    main()
