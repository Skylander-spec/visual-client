"""Visual Pack — Textur-Pipeline.

Erzeugt alle Item-Texturen (32x), GUI-Sprites, Armor-Layer und Emissive-Maps
prozedural. Stil: cleaner PvP-Look, dunkle Outlines, Cyan-Akzente.

Aufruf:  py tools/generate_textures.py   (aus resourcepack/)
"""
from __future__ import annotations

import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "pack")
ITEMS = os.path.join(ROOT, "assets", "minecraft", "textures", "item")
GUI = os.path.join(ROOT, "assets", "minecraft", "textures", "gui")
ARMOR = os.path.join(ROOT, "assets", "minecraft", "textures", "models", "armor")
OPTIFINE = os.path.join(ROOT, "assets", "minecraft", "optifine")

S = 32
OUTLINE = (12, 17, 28, 255)
ACCENT = (34, 211, 238, 255)
ACCENT_DIM = (14, 116, 144, 255)

RAMPS = {
    "diamond":   [(23, 106, 128), (45, 178, 205), (118, 231, 250), (206, 250, 255)],
    "iron":      [(84, 92, 104), (140, 149, 162), (196, 204, 216), (238, 242, 248)],
    "gold":      [(158, 106, 18), (222, 158, 34), (250, 204, 66), (255, 240, 160)],
    "netherite": [(38, 34, 44), (68, 62, 76), (104, 96, 116), (148, 140, 160)],
    "wood":      [(66, 44, 24), (104, 72, 40), (146, 104, 60), (186, 142, 92)],
    "red":       [(120, 22, 36), (200, 44, 66), (244, 84, 104), (255, 160, 172)],
    "purple":    [(74, 32, 116), (128, 62, 186), (176, 112, 232), (222, 182, 255)],
    "pearl":     [(16, 84, 92), (28, 140, 148), (74, 202, 202), (170, 245, 238)],
}


def canvas(w=S, h=S):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((int(x), int(y)), tuple(c) if len(c) == 4 else (*c, 255))


def opaque(img, x, y):
    if 0 <= x < img.width and 0 <= y < img.height:
        return img.getpixel((x, y))[3] > 0
    return False


def shade(img, ramp):
    """Pixel-Shading: Kanten oben/links aufhellen, unten/rechts abdunkeln."""
    src = img.copy()
    hi, lo = (*ramp[3], 255), (*ramp[0], 255)
    for y in range(img.height):
        for x in range(img.width):
            p = src.getpixel((x, y))
            if p[3] == 0 or p[:3] in ((c[:3]) for c in (ACCENT, ACCENT_DIM)):
                continue
            if not opaque(src, x - 1, y - 1) and not opaque(src, x, y - 1):
                px(img, x, y, hi)
            elif not opaque(src, x + 1, y + 1) and not opaque(src, x, y + 1):
                px(img, x, y, lo)


def outline(img):
    """Dunkle 1px-Outline um alle opaken Bereiche."""
    src = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            if src.getpixel((x, y))[3] == 0:
                if any(opaque(src, x + dx, y + dy)
                       for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                    px(img, x, y, OUTLINE)


def emissive_map(img):
    """Nur die Accent-Pixel behalten -> _e.png fuer OptiFine-Glow."""
    e = canvas(img.width, img.height)
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p[:3] in (ACCENT[:3], ACCENT_DIM[:3]):
                px(e, x, y, p)
    return e


def save(img, folder, name, glow=False):
    os.makedirs(folder, exist_ok=True)
    img.save(os.path.join(folder, name + ".png"))
    if glow:
        emissive_map(img).save(os.path.join(folder, name + "_e.png"))
    print("  +", name)


# ---------------------------------------------------------------- Waffen

def draw_sword(ramp):
    img = canvas()
    for i in range(17):                       # Klinge diagonal
        x, y = 9 + i, 22 - i
        px(img, x, y, ramp[2])
        px(img, x + 1, y, ramp[1])
        px(img, x, y - 1, ramp[3])
        if i % 2 == 0 and 2 < i < 15:         # Energie-Linie
            px(img, x + 1, y - 1, ACCENT)
    px(img, 27, 4, ramp[3])                   # Spitze
    for d in range(-3, 4):                    # Parierstange
        px(img, 8 + d, 24 - d, RAMPS["netherite"][1])
        px(img, 9 + d, 25 - d, RAMPS["netherite"][0])
    px(img, 8, 24, ACCENT)
    for i in range(5):                        # Griff
        x, y = 6 - i, 25 + i
        px(img, x, y, RAMPS["wood"][1] if i % 2 else RAMPS["wood"][0])
        px(img, x + 1, y, RAMPS["wood"][2])
    px(img, 2, 29, ramp[1])                   # Knauf
    px(img, 1, 29, ramp[0])
    px(img, 2, 30, ramp[0])
    shade(img, ramp)
    outline(img)
    return img


def draw_axe(ramp):
    img = canvas()
    for i in range(16):                       # Stiel
        x, y = 8 + i, 25 - i
        px(img, x, y, RAMPS["wood"][1] if i % 3 else RAMPS["wood"][0])
        px(img, x + 1, y, RAMPS["wood"][2])
    d = ImageDraw.Draw(img)
    d.polygon([(17, 4), (25, 6), (27, 12), (22, 15), (16, 10)], fill=(*ramp[1], 255))
    d.polygon([(18, 5), (24, 7), (25, 11)], fill=(*ramp[2], 255))
    for i in range(4):                        # Schneide
        px(img, 26 - i, 13 + 0, ramp[3])
        px(img, 27, 8 + i, ramp[3])
    px(img, 20, 8, ACCENT)
    px(img, 21, 10, ACCENT)
    shade(img, ramp)
    outline(img)
    return img


def draw_bow(stage):
    img = canvas()
    wood = RAMPS["wood"]
    arc = [(22, 3), (25, 5), (27, 8), (28, 12), (28, 16), (27, 20),
           (25, 23), (22, 26), (18, 28), (14, 28)]
    d0 = ImageDraw.Draw(img)
    for (x1, y1), (x2, y2) in zip(arc, arc[1:]):
        d0.line([(x1, y1), (x2, y2)], fill=(*wood[1], 255), width=2)
    for i, (x, y) in enumerate(arc):
        px(img, x, y, wood[2])
        if i % 3 == 0:
            px(img, x - 1, y - 1, ACCENT_DIM)
    pull = [0, 3, 6][stage] if stage >= 0 else 0
    sx, sy = 22 - pull, 3 + 0
    ex, ey = 14 - 0, 28 - 0
    d = ImageDraw.Draw(img)
    if stage < 0:
        d.line([(22, 3), (14, 28)], fill=(226, 232, 240, 255))
    else:
        mx, my = 12 - pull, 14 + pull // 2
        d.line([(22, 3), (mx, my)], fill=(226, 232, 240, 255))
        d.line([(mx, my), (14, 28)], fill=(226, 232, 240, 255))
        for i in range(10):                   # eingelegter Pfeil
            px(img, mx + i, my - i // 3, RAMPS["iron"][2] if i > 7 else wood[2])
    shade(img, wood)
    outline(img)
    return img


def draw_arrow():
    img = canvas()
    for i in range(18):
        x, y = 7 + i, 24 - i
        px(img, x, y, RAMPS["wood"][2])
    for i in range(4):                        # Spitze
        px(img, 25 + i // 2, 6 - i // 2, RAMPS["iron"][2 + (i % 2)])
    px(img, 26, 5, ACCENT)
    for i in range(3):                        # Federn
        px(img, 6 + i, 26 - i, ACCENT_DIM)
        px(img, 5 + i, 27 - i, ACCENT)
        px(img, 7 + i, 27 - i, (226, 232, 240, 255))
    shade(img, RAMPS["wood"])
    outline(img)
    return img


# ---------------------------------------------------------------- Items

def draw_apple(gold=True, enchanted=False):
    img = canvas()
    ramp = RAMPS["purple"] if enchanted else RAMPS["gold"]
    d = ImageDraw.Draw(img)
    d.ellipse([7, 10, 24, 27], fill=(*ramp[1], 255))
    d.ellipse([9, 12, 20, 22], fill=(*ramp[2], 255))
    d.ellipse([11, 13, 16, 18], fill=(*ramp[3], 255))
    px(img, 15, 9, RAMPS["wood"][0])          # Stiel
    px(img, 15, 8, RAMPS["wood"][0])
    px(img, 16, 7, RAMPS["wood"][1])
    for dx, dy in ((18, 7), (19, 7), (20, 8), (18, 8)):   # Blatt
        px(img, dx, dy, (74, 222, 128, 255))
    if enchanted:
        for x, y in ((8, 20), (14, 25), (21, 14), (19, 23), (11, 11)):
            px(img, x, y, ACCENT)
    shade(img, ramp)
    outline(img)
    return img


def draw_pearl():
    img = canvas()
    ramp = RAMPS["pearl"]
    d = ImageDraw.Draw(img)
    d.ellipse([7, 7, 25, 25], fill=(*ramp[1], 255))
    d.ellipse([10, 10, 21, 21], fill=(*ramp[2], 255))
    for i in range(8):                        # Wirbel
        x = 16 + int(4.5 * __import__("math").cos(i * 0.9))
        y = 16 + int(4.5 * __import__("math").sin(i * 0.9))
        px(img, x, y, ramp[0])
    px(img, 12, 11, ramp[3])
    px(img, 13, 11, ramp[3])
    px(img, 12, 12, ramp[3])
    for x, y in ((9, 18), (20, 9), (22, 19)):
        px(img, x, y, ACCENT)
    shade(img, ramp)
    outline(img)
    return img


def draw_rod():
    img = canvas()
    for i in range(20):
        x, y = 5 + i, 27 - i
        px(img, x, y, RAMPS["wood"][2] if i % 4 else ACCENT_DIM)
    d = ImageDraw.Draw(img)
    d.line([(25, 7), (28, 10), (28, 16)], fill=(203, 213, 225, 255))
    px(img, 28, 17, RAMPS["iron"][3])         # Haken
    px(img, 27, 18, RAMPS["iron"][3])
    px(img, 28, 19, ACCENT)
    shade(img, RAMPS["wood"])
    outline(img)
    return img


# ---------------------------------------------------------------- Ruestung (Icons)

def draw_helmet(ramp):
    img = canvas()
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([8, 10, 23, 19], radius=4, fill=(*ramp[1], 255))
    d.rectangle([8, 17, 23, 21], fill=(*ramp[1], 255))
    d.rectangle([10, 12, 21, 14], fill=(*ramp[2], 255))
    for x in range(10, 22):
        px(img, x, 18, ACCENT if x % 3 == 0 else ramp[0])
    shade(img, ramp)
    outline(img)
    return img


def draw_chestplate(ramp):
    img = canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(9, 9), (22, 9), (24, 13), (23, 24), (8, 24), (7, 13)], fill=(*ramp[1], 255))
    d.rectangle([12, 9, 19, 12], fill=(0, 0, 0, 0))
    d.rectangle([11, 14, 20, 17], fill=(*ramp[2], 255))
    px(img, 15, 19, ACCENT)
    px(img, 16, 19, ACCENT)
    for x in range(9, 23):
        px(img, x, 23, ramp[0])
    shade(img, ramp)
    outline(img)
    return img


def draw_leggings(ramp):
    img = canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([9, 9, 22, 13], fill=(*ramp[2], 255))
    d.rectangle([9, 13, 14, 24], fill=(*ramp[1], 255))
    d.rectangle([17, 13, 22, 24], fill=(*ramp[1], 255))
    for x in range(9, 23):
        px(img, x, 10, ACCENT if x % 4 == 0 else ramp[3])
    shade(img, ramp)
    outline(img)
    return img


def draw_boots(ramp):
    img = canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([8, 12, 13, 20], fill=(*ramp[1], 255))
    d.rectangle([8, 18, 16, 22], fill=(*ramp[1], 255))
    d.rectangle([18, 12, 23, 20], fill=(*ramp[1], 255))
    d.rectangle([18, 18, 26, 22], fill=(*ramp[1], 255))
    px(img, 9, 13, ramp[3])
    px(img, 19, 13, ramp[3])
    for x in (10, 20):
        px(img, x, 17, ACCENT)
    shade(img, ramp)
    outline(img)
    return img


# ---------------------------------------------------------------- Armor-Layer (getragen)

def draw_armor_layer(ramp, layer):
    img = canvas(64, 32)
    rnd = random.Random(7 * layer)
    base, mid, hi = ramp[1], ramp[2], ramp[3]
    for y in range(32):
        for x in range(64):
            v = rnd.random()
            c = mid if v > 0.85 else base
            if v > 0.97:
                c = hi
            px(img, x, y, c)
    for y in (0, 15, 31):                     # Segment-Linien
        for x in range(64):
            px(img, x, y, ramp[0])
    for x in range(0, 64, 8):
        for y in range(32):
            if y % 2 == 0:
                px(img, x, y, ramp[0])
    for x in range(8, 16):                    # Cyan-Trim Stirn (Helm-Front)
        px(img, x, 10, ACCENT_DIM)
    for x in range(20, 28):                   # Brust-Trim
        px(img, x, 22, ACCENT_DIM)
    return img


# ---------------------------------------------------------------- GUI

def draw_widgets():
    img = canvas(256, 256)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 181, 21], fill=(11, 15, 20, 205))          # Hotbar
    d.rectangle([0, 0, 181, 21], outline=(40, 52, 66, 255))
    for i in range(1, 9):
        x = i * 20
        d.line([(x, 2), (x, 19)], fill=(30, 40, 52, 255))
    d.rectangle([0, 22, 23, 45], outline=ACCENT, width=2)          # Auswahlrahmen
    d.rectangle([2, 24, 21, 43], outline=(14, 116, 144, 140))
    for y0, bg, border in ((46, (16, 20, 26, 230), (40, 52, 66, 255)),
                           (66, (17, 24, 32, 235), (52, 70, 88, 255)),
                           (86, (20, 32, 42, 240), ACCENT)):
        d.rectangle([0, y0, 199, y0 + 19], fill=bg)
        d.rectangle([0, y0, 199, y0 + 19], outline=border)
    return img


def draw_icons():
    img = canvas(256, 256)
    d = ImageDraw.Draw(img)
    for i in range(4, 12):                    # Crosshair 15x15
        px(img, i if i < 8 else i + 0, 7, ACCENT)
    for i in range(4, 12):
        px(img, 7, i, ACCENT)
    px(img, 7, 7, (255, 255, 255, 255))

    def heart(x0, y0, ramp, fill=True, half=False):
        pts = [(1, 1), (2, 0), (3, 0), (4, 1), (5, 1), (6, 0), (7, 0), (8, 1)]
        d.polygon([(x0 + 1, y0 + 2), (x0 + 4, y0 + 2), (x0 + 4, y0 + 7),
                   (x0 + 1, y0 + 5)] if half else
                  [(x0 + 1, y0 + 2), (x0 + 7, y0 + 2), (x0 + 7, y0 + 4),
                   (x0 + 4, y0 + 7), (x0 + 1, y0 + 4)],
                  fill=(*ramp[1], 255) if fill else (24, 30, 38, 255))
        for xx, yy in pts:
            c = (*ramp[2], 255) if fill else (44, 54, 66, 255)
            px(img, x0 + xx, y0 + yy, c)
        if fill:
            px(img, x0 + 2, y0 + 2, (*ramp[3], 255))

    heart(16, 0, RAMPS["red"], fill=False)                    # Container
    heart(52, 0, RAMPS["red"], fill=True)                     # Voll
    heart(61, 0, RAMPS["red"], fill=True, half=True)          # Halb

    def armor_icon(x0, amount):
        d.rectangle([x0 + 1, 1 + 9, x0 + 7, 7 + 9], outline=(60, 74, 92, 255))
        if amount > 0:
            w = 3 if amount == 1 else 6
            d.rectangle([x0 + 1, 10, x0 + 1 + w, 16], fill=(*RAMPS["diamond"][2], 220))

    armor_icon(16, 0)
    armor_icon(25, 1)
    armor_icon(34, 2)

    def food(x0, fill=True, half=False):
        col = (*RAMPS["gold"][2], 255) if fill else (44, 54, 66, 255)
        d.ellipse([x0 + 1, 27 + 1, x0 + 7, 27 + 7], fill=col)
        if fill:
            px(img, x0 + 3, 29, (*RAMPS["gold"][3], 255))
        if half:
            d.rectangle([x0 + 1, 27, x0 + 3, 27 + 8], fill=(0, 0, 0, 0))

    food(16, fill=False)
    food(52, fill=True)
    food(61, fill=True, half=True)

    d.rectangle([0, 64, 181, 68], fill=(20, 26, 34, 220))      # XP leer
    d.rectangle([0, 64, 181, 68], outline=(40, 52, 66, 255))
    d.rectangle([0, 69, 181, 73], fill=(*RAMPS["pearl"][2], 255))  # XP voll
    for x in range(0, 182, 6):
        px(img, x, 71, (*RAMPS["pearl"][3], 255))
    return img


def draw_pack_icon():
    img = canvas(64, 64)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2, 2, 61, 61], radius=12, fill=(11, 15, 20, 255))
    d.rounded_rectangle([2, 2, 61, 61], radius=12, outline=ACCENT, width=2)
    d.line([(18, 16), (32, 48)], fill=ACCENT, width=5)
    d.line([(46, 16), (32, 48)], fill=(120, 235, 250, 255), width=5)
    return img


# ---------------------------------------------------------------- Main

def main():
    print("Visual Pack Texturen werden generiert...")
    print("[Waffen]")
    for mat in ("iron", "gold", "diamond", "netherite"):
        name = {"gold": "golden"}.get(mat, mat)
        save(draw_sword(RAMPS[mat]), ITEMS, f"{name}_sword", glow=True)
        save(draw_axe(RAMPS[mat]), ITEMS, f"{name}_axe", glow=True)
    save(draw_bow(-1), ITEMS, "bow")
    for s in range(3):
        save(draw_bow(s), ITEMS, f"bow_pulling_{s}")
    save(draw_arrow(), ITEMS, "arrow")
    print("[Items]")
    save(draw_apple(), ITEMS, "golden_apple", glow=True)
    save(draw_apple(enchanted=True), ITEMS, "enchanted_golden_apple", glow=True)
    save(draw_pearl(), ITEMS, "ender_pearl", glow=True)
    save(draw_rod(), ITEMS, "fishing_rod")
    print("[Ruestung]")
    for mat in ("diamond", "netherite"):
        r = RAMPS[mat]
        save(draw_helmet(r), ITEMS, f"{mat}_helmet", glow=True)
        save(draw_chestplate(r), ITEMS, f"{mat}_chestplate", glow=True)
        save(draw_leggings(r), ITEMS, f"{mat}_leggings", glow=True)
        save(draw_boots(r), ITEMS, f"{mat}_boots", glow=True)
        save(draw_armor_layer(r, 1), ARMOR, f"{mat}_layer_1")
        save(draw_armor_layer(r, 2), ARMOR, f"{mat}_layer_2")
    print("[GUI]")
    save(draw_widgets(), GUI, "widgets")
    save(draw_icons(), GUI, "icons")
    draw_pack_icon().save(os.path.join(ROOT, "pack.png"))
    print("  + pack.png")
    os.makedirs(OPTIFINE, exist_ok=True)
    with open(os.path.join(OPTIFINE, "emissive.properties"), "w") as f:
        f.write("suffix.emissive=_e\n")
    print("  + optifine/emissive.properties")
    print("Fertig.")


if __name__ == "__main__":
    main()
