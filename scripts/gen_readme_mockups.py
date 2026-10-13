#!/usr/bin/env python3
"""Generate README UI mockups that mirror the real blueberry component tokens.

These are honest schematic reproductions of the launcher's own UI (the values below
are the same ones in `UiTokens.java`), not device screenshots. Device screenshots are
tracked in docs/UI_CHECKLIST.md; this script exists so the README has clean visuals
without pretending a mockup is a photo.

Run:  python3 scripts/gen_readme_mockups.py
Output: docs/assets/mockups/*.png
"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "..", "docs", "assets", "mockups")
FONT_DIR = "/usr/share/fonts/truetype/dejavu"

# --- UiTokens palette (mirrors app/src/main/java/.../ui/components/UiTokens.java) ---
BG_WINDOW = (0x11, 0x12, 0x14)
BG_SURFACE = (0x17, 0x19, 0x1D)
BG_SURFACE_HOVER = (0x1E, 0x21, 0x26)
BG_TITLEBAR = (0x14, 0x14, 0x18)
ACCENT_500 = (0x0F, 0x5B, 0xE4)
ACCENT_400 = (0x36, 0x82, 0xE2)
ACCENT_300 = (0x71, 0xAC, 0xEF)
ACCENT_900 = (0x10, 0x25, 0x4F)
TEXT_PRIMARY = (0xE8, 0xEA, 0xED)
TEXT_SECONDARY = (0x9A, 0xA0, 0xA6)
TEXT_DISABLED = (0x5F, 0x63, 0x68)
TOGGLE_OFF = (0x2A, 0x2D, 0x33)
HAIRLINE = (0x2A, 0x2E, 0x34)
SUCCESS = (0x3D, 0xDC, 0x84)
DANGER = (0xFF, 0x5C, 0x5C)
GOLD = (0xFF, 0xC9, 0x4D)
GREEN = (0x2F, 0xA8, 0x5A)

W, H = 1280, 720


def font(size, bold=False):
    name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    return ImageFont.truetype(os.path.join(FONT_DIR, name), size)


def rrect(d, box, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)


def text(d, xy, s, f, fill, anchor="la"):
    d.text(xy, s, font=f, fill=fill, anchor=anchor)


def glyph_cube(d, cx, cy, s, color):
    d.polygon([(cx - s, cy - s * 0.3), (cx - s * 0.1, cy - s * 0.3),
               (cx - s * 0.1, cy + s), (cx - s, cy + s)], outline=color, width=2)
    d.polygon([(cx - s, cy - s * 0.3), (cx - s * 0.5, cy - s * 0.8),
               (cx + s * 0.4, cy - s * 0.8), (cx - s * 0.1, cy - s * 0.3)], outline=color, width=2)
    d.polygon([(cx - s * 0.1, cy - s * 0.3), (cx + s * 0.4, cy - s * 0.8),
               (cx + s * 0.4, cy + s * 0.5), (cx - s * 0.1, cy + s)], outline=color, width=2)


def glyph_pencil(d, cx, cy, s, color):
    d.line([(cx - s, cy + s), (cx + s * 0.6, cy - s * 0.6)], fill=color, width=2)
    d.polygon([(cx + s * 0.3, cy - s * 0.9), (cx + s * 0.9, cy - s * 0.3),
               (cx + s * 0.6, cy - s * 0.6), (cx + s * 0.3, cy - s * 0.3)], outline=color)


def glyph_hanger(d, cx, cy, s, color):
    d.ellipse([cx - s * 0.25, cy - s * 0.9, cx + s * 0.25, cy - s * 0.4], outline=color, width=2)
    d.line([(cx - s, cy + s * 0.5), (cx, cy - s * 0.2), (cx + s, cy + s * 0.5)], fill=color, width=2)


def glyph_gear(d, cx, cy, s, color):
    import math
    d.ellipse([cx - s * 0.45, cy - s * 0.45, cx + s * 0.45, cy + s * 0.45], outline=color, width=2)
    for i in range(8):
        a = math.radians(i * 45)
        d.line([(cx + math.cos(a) * s * 0.45, cy + math.sin(a) * s * 0.45),
                (cx + math.cos(a) * s * 0.9, cy + math.sin(a) * s * 0.9)], fill=color, width=2)


def toggle(d, x, y, on, w=54, h=30):
    rrect(d, [x, y, x + w, y + h], h // 2, fill=ACCENT_500 if on else TOGGLE_OFF)
    cx = x + (w - h // 2 - 3) if on else x + h // 2 + 3
    d.ellipse([cx - h // 2 + 3, y + 3, cx + h // 2 - 3, y + h - 3], fill=(255, 255, 255))


# --- 1. Radial wheel -------------------------------------------------------------------------
def radial_wheel():
    im = Image.new("RGB", (W, H), (18, 20, 26))
    d = ImageDraw.Draw(im)
    # game backdrop
    d.rectangle([0, 0, W, H], fill=(28, 34, 30))
    d.rectangle([0, H - 120, W, H], fill=(34, 44, 36))
    # scrim
    ov = Image.new("RGBA", (W, H), (6, 6, 12, 115))
    im = Image.alpha_composite(im.convert("RGBA"), ov).convert("RGB")
    d = ImageDraw.Draw(im)

    cx, cy = W // 2, H // 2
    S = min(W, H)
    outer = 0.40 * S
    inner = 0.52 * outer
    hub = 0.46 * outer

    def sector(a0, a1, fill, label, glyph):
        d.pieslice([cx - outer, cy - outer, cx + outer, cy + outer], a0, a1,
                   fill=fill, outline=HAIRLINE, width=2)
        d.pieslice([cx - inner, cy - inner, cx + inner, cy + inner], a0, a1, fill=(18, 20, 26))
        mid = (a0 + a1) / 2
        import math
        mx = cx + math.cos(math.radians(mid)) * (inner + outer) / 2
        my = cy + math.sin(math.radians(mid)) * (inner + outer) / 2
        glyph(d, mx, my - 16, 16, TEXT_PRIMARY)
        text(d, (mx, my + 22), label, font(15, True), TEXT_PRIMARY, anchor="mm")

    base = (96, 82, 110)
    sector(-135, -45, base, "MODS", glyph_cube)
    sector(-45, 45, base, "CLOSET", glyph_hanger)
    sector(45, 135, base, "SETTINGS", glyph_gear)
    sector(135, 225, base, "HUD", glyph_pencil)

    # hub
    d.ellipse([cx - hub, cy - hub, cx + hub, cy + hub], fill=(24, 20, 32), outline=HAIRLINE, width=2)
    # stylised head (real skin renders here)
    hw = hub * 0.62
    d.rounded_rectangle([cx - hw, cy - hw, cx + hw, cy + hw], radius=10, fill=(0xC8, 0x9A, 0x6A))
    d.rectangle([cx - hw, cy - hw, cx + hw, cy - hw * 0.35], fill=(0x4A, 0x30, 0x1E))
    d.rectangle([cx - hw * 0.55, cy - hw * 0.1, cx - hw * 0.2, cy + hw * 0.15], fill=(0x2A, 0x2A, 0x2A))
    d.rectangle([cx + hw * 0.2, cy - hw * 0.1, cx + hw * 0.55, cy + hw * 0.15], fill=(0x2A, 0x2A, 0x2A))

    # chips (three clusters)
    def chip(x, y, label, on):
        w = 26 + len(label) * 8
        rrect(d, [x, y, x + w, y + 30], 15, fill=(22, 22, 30), outline=(60, 60, 72), width=1)
        d.ellipse([x + 10, y + 12, x + 18, y + 20], fill=ACCENT_500 if on else TEXT_DISABLED)
        text(d, (x + 26, y + 15), label, font(13), TEXT_PRIMARY, anchor="lm")
    chip(120, 110, "Zoom", True)
    chip(120, 150, "Fullbright", False)
    chip(W - 300, 110, "CPS", True)
    chip(W - 300, 150, "Sprint", True)
    chip(W - 300, 190, "Nametag", False)
    chip(W - 210, 470, "Replay", True)

    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, "radial_wheel.png"))


# --- 2. Mod Menu (Screen A) ------------------------------------------------------------------
MODULES = [
    ("Sprint", True, True), ("Fullbright", True, False), ("Zoom", True, True),
    ("Time Changer", False, False), ("FPS Display", True, True), ("CPS Display", True, False),
    ("Aim Settings", False, True), ("Hit Registration", True, False), ("Snaplook", False, False),
    ("Armor HUD", True, True), ("Hitboxes", True, True), ("Replay", False, True),
]


def mod_menu():
    im = Image.new("RGB", (W, H), (10, 10, 14))
    d = ImageDraw.Draw(im)
    # scrim behind the window
    d.rectangle([0, 0, W, H], fill=(8, 8, 12))
    win_w = int(min(W * 0.66, 900))
    win_h = int(H * 0.78)
    x0 = (W - win_w) // 2
    y0 = (H - win_h) // 2
    x1, y1 = x0 + win_w, y0 + win_h
    rrect(d, [x0, y0, x1, y1], 18, fill=BG_WINDOW, outline=HAIRLINE, width=2)

    # titlebar
    rrect(d, [x0, y0, x1, y0 + 56], 18, fill=BG_TITLEBAR)
    d.rectangle([x0, y0 + 40, x1, y0 + 56], fill=BG_TITLEBAR)
    text(d, (x0 + 20, y0 + 28), "Glowberry", font(20, True), TEXT_PRIMARY, anchor="lm")
    # search field
    sf = [x0 + 180, y0 + 12, x1 - 140, y0 + 46]
    rrect(d, sf, 17, fill=BG_SURFACE, outline=HAIRLINE, width=1)
    d.ellipse([sf[0] + 12, y0 + 23, sf[0] + 24, y0 + 35], outline=TEXT_SECONDARY, width=2)
    d.line([(sf[0] + 22, y0 + 33), (sf[0] + 28, y0 + 39)], fill=TEXT_SECONDARY, width=2)
    text(d, (sf[0] + 40, y0 + 29), "Search modules...", font(14), TEXT_SECONDARY, anchor="lm")
    text(d, (x1 - 90, y0 + 29), "60 FPS", font(14, True), SUCCESS, anchor="lm")
    # close
    d.ellipse([x1 - 44, y0 + 15, x1 - 16, y0 + 43], fill=BG_SURFACE_HOVER)
    d.line([(x1 - 36, y0 + 23), (x1 - 24, y0 + 35)], fill=TEXT_PRIMARY, width=2)
    d.line([(x1 - 24, y0 + 23), (x1 - 36, y0 + 35)], fill=TEXT_PRIMARY, width=2)
    d.line([(x0, y0 + 56), (x1, y0 + 56)], fill=HAIRLINE, width=1)

    # left rail (categories)
    rail_x = x0 + 12
    cats = ["ALL", "PVP", "VIS", "MOVE", "UTIL", "HUD", "VOICE", "COSM"]
    for i, c in enumerate(cats):
        yy = y0 + 72 + i * 60
        sel = i == 0
        rrect(d, [rail_x, yy, rail_x + 48, yy + 48], 12, fill=ACCENT_500 if sel else BG_SURFACE,
              outline=None if sel else HAIRLINE, width=1)
        text(d, (rail_x + 24, yy + 24), c, font(11, True),
             (255, 255, 255) if sel else TEXT_SECONDARY, anchor="mm")

    # two columns of module rows
    list_x = rail_x + 64
    col_w = (x1 - 20 - list_x) // 2
    for i, (name, on, available) in enumerate(MODULES):
        col = i % 2
        rowi = i // 2
        rx = list_x + col * (col_w + 12)
        ry = y0 + 74 + rowi * 56
        rrect(d, [rx, ry, rx + col_w, ry + 46], 12,
              fill=(30, 33, 38) if not available else (ACCENT_900 if on else BG_SURFACE),
              outline=HAIRLINE, width=1)
        glyph_cube(d, rx + 26, ry + 23, 11, ACCENT_300 if available else TEXT_DISABLED)
        text(d, (rx + 48, ry + 23), name, font(15, on and available),
             TEXT_PRIMARY if available else TEXT_DISABLED, anchor="lm")
        if available:
            toggle(d, rx + col_w - 66, ry + 8, on)
        else:
            text(d, (rx + col_w - 16, ry + 23), "N/A", font(13, True), DANGER, anchor="rm")

    im.save(os.path.join(OUT, "mod_menu.png"))


# --- 3. .AntEgg import workflow --------------------------------------------------------------
def ant_egg():
    im = Image.new("RGB", (W, H), BG_WINDOW)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, W, H], fill=BG_WINDOW)
    text(d, (60, 56), "Mods", font(30, True), TEXT_PRIMARY)
    text(d, (60, 100), "Import a .AntEgg package", font(16), TEXT_SECONDARY)

    # drop target
    dz = [60, 150, W - 60, 300]
    rrect(d, dz, 16, fill=BG_SURFACE, outline=ACCENT_400, width=2)
    # egg icon
    ex, ey = dz[0] + 90, (dz[1] + dz[3]) // 2
    d.ellipse([ex - 34, ey - 44, ex + 34, ey + 44], fill=(0x6B, 0x4A, 0x2E))
    d.ellipse([ex - 34, ey - 44, ex + 34, ey + 44], outline=GOLD, width=3)
    d.arc([ex - 20, ey - 30, ex + 20, ey + 10], 200, 340, fill=GOLD, width=3)
    text(d, (dz[0] + 170, ey - 24), "Drop a .AntEgg file here", font(22, True), TEXT_PRIMARY)
    text(d, (dz[0] + 170, ey + 8), "or tap to browse  ·  native (.so) or script (.lua)",
         font(15), TEXT_SECONDARY)
    text(d, (dz[0] + 170, ey + 38), "Validated manifest · sandboxed extraction · atomic install",
         font(13), TEXT_DISABLED)

    # installed list
    text(d, (60, 340), "INSTALLED", font(13, True), TEXT_SECONDARY)
    rows = [
        ("Chimera Tweaks", "native", "1.4.0", True),
        ("Auto-Sort Chests", "script", "0.9.2", True),
        ("Sky HUD Pack", "script", "2.0.1", False),
    ]
    for i, (name, kind, ver, on) in enumerate(rows):
        ry = 370 + i * 62
        rrect(d, [60, ry, W - 60, ry + 52], 12, fill=BG_SURFACE, outline=HAIRLINE, width=1)
        # egg chip
        d.ellipse([82, ry + 14, 106, ry + 38], fill=(0x6B, 0x4A, 0x2E), outline=GOLD, width=2)
        text(d, (120, ry + 26), name, font(17, True), TEXT_PRIMARY, anchor="lm")
        text(d, (330, ry + 26), f"{kind}  ·  v{ver}", font(14), TEXT_SECONDARY, anchor="lm")
        if on:
            rrect(d, [W - 220, ry + 12, W - 110, ry + 40], 14, fill=ACCENT_500)
            text(d, (W - 165, ry + 26), "ENABLED", font(12, True), (255, 255, 255), anchor="mm")
        else:
            rrect(d, [W - 220, ry + 12, W - 110, ry + 40], 14, fill=BG_SURFACE_HOVER,
                  outline=HAIRLINE, width=1)
            text(d, (W - 165, ry + 26), "DISABLED", font(12, True), TEXT_SECONDARY, anchor="mm")
        toggle(d, W - 90, ry + 11, on)

    im.save(os.path.join(OUT, "antegg_import.png"))


# --- 4. Closet -------------------------------------------------------------------------------
def closet():
    im = Image.new("RGB", (W, H), BG_WINDOW)
    d = ImageDraw.Draw(im)
    # left rail
    rail = ["Capes", "Hats", "Pets", "Back", "Offhand", "Effects"]
    for i, c in enumerate(rail):
        yy = 24 + i * 62
        sel = i == 1
        rrect(d, [20, yy, 20 + 92, yy + 50], 12, fill=ACCENT_500 if sel else BG_SURFACE,
              outline=None if sel else HAIRLINE, width=1)
        text(d, (66, yy + 25), c, font(14, sel), (255, 255, 255) if sel else TEXT_SECONDARY,
             anchor="mm")

    # card grid
    text(d, (140, 34), "HATS", font(14, True), TEXT_SECONDARY)
    names = ["Top Hat", "Wizard Hat", "Crown", "Beanie", "Halo", "Pumpkin", "Chef Hat", "Cap"]
    for i, n in enumerate(names):
        col, rowi = i % 3, i // 3
        cx = 140 + col * 180
        cy = 60 + rowi * 150
        equipped = i == 2
        rrect(d, [cx, cy, cx + 168, cy + 138], 12,
              fill=ACCENT_900 if equipped else BG_SURFACE,
              outline=ACCENT_500 if equipped else HAIRLINE, width=2 if equipped else 1)
        # little preview block
        rrect(d, [cx + 40, cy + 16, cx + 128, cy + 88], 8, fill=BG_SURFACE_HOVER)
        glyph_hanger(d, cx + 84, cy + 52, 20, ACCENT_300)
        text(d, (cx + 84, cy + 102), n, font(14, True), TEXT_PRIMARY, anchor="mm")
        if equipped:
            text(d, (cx + 84, cy + 122), "EQUIPPED", font(10, True), ACCENT_300, anchor="mm")
        else:
            text(d, (cx + 84, cy + 122), "OWNED", font(10), TEXT_SECONDARY, anchor="mm")

    # right preview panel
    px = 140 + 3 * 180 + 10
    rrect(d, [px, 60, W - 24, H - 24], 14, fill=BG_SURFACE, outline=HAIRLINE, width=1)
    # soft shadow ellipse
    d.ellipse([px + 60, H - 120, W - 84, H - 50], fill=(12, 12, 16))
    # stylised character
    ccx = (px + W - 24) // 2
    d.rounded_rectangle([ccx - 26, 130, ccx + 26, 200], radius=8, fill=(0x2E, 0x7A, 0xC0))  # torso
    d.rounded_rectangle([ccx - 22, 96, ccx + 22, 138], radius=8, fill=(0xC8, 0x9A, 0x6A))   # head
    d.rectangle([ccx - 22, 96, ccx + 22, 112], fill=(0x4A, 0x30, 0x1E))                     # hair
    d.rounded_rectangle([ccx - 44, 132, ccx - 28, 190], radius=6, fill=(0x2E, 0x7A, 0xC0))  # arms
    d.rounded_rectangle([ccx + 28, 132, ccx + 44, 190], radius=6, fill=(0x2E, 0x7A, 0xC0))
    d.rounded_rectangle([ccx - 22, 200, ccx - 4, 250], radius=6, fill=(0x2A, 0x3A, 0x5A))   # legs
    d.rounded_rectangle([ccx + 4, 200, ccx + 22, 250], radius=6, fill=(0x2A, 0x3A, 0x5A))
    # worn hat
    d.polygon([(ccx - 34, 100), (ccx + 34, 100), (ccx + 24, 66), (ccx - 24, 66)],
              fill=(0x6D, 0x4A, 0xFF))
    # cape behind
    d.polygon([(ccx - 40, 140), (ccx + 40, 140), (ccx + 30, 232), (ccx - 30, 232)],
              fill=(0x0F, 0x5B, 0xE4))
    # equip button
    rrect(d, [px + 60, H - 92, W - 84, H - 44], 14, fill=ACCENT_500)
    text(d, ((px + W - 24) // 2, H - 68), "EQUIP", font(18, True), (255, 255, 255), anchor="mm")
    text(d, ((px + W - 24) // 2, 108), "Live preview · drag to rotate", font(13), TEXT_SECONDARY,
         anchor="mm")

    im.save(os.path.join(OUT, "closet.png"))


# --- 5. Launch tab ---------------------------------------------------------------------------
def launch_tab():
    im = Image.new("RGB", (W, H), BG_WINDOW)
    d = ImageDraw.Draw(im)
    # top nav bar
    d.rectangle([0, 0, W, 56], fill=BG_TITLEBAR)
    tabs = ["Launch", "Versions", "Installations", "Mods", "Customize", "Settings"]
    tx = 40
    for i, t in enumerate(tabs):
        f = font(15, i == 0)
        text(d, (tx, 28), t, f, ACCENT_300 if i == 0 else TEXT_SECONDARY, anchor="lm")
        tx += d.textlength(t, font=font(15, i == 0)) + 46
    text(d, (W - 40, 28), "Sign in", font(14, True), TEXT_PRIMARY, anchor="rm")
    d.line([(0, 56), (W, 56)], fill=HAIRLINE, width=1)

    # hero card
    rrect(d, [40, 80, W - 40, 420], 18, fill=BG_SURFACE, outline=HAIRLINE, width=1)
    # avatar on ellipse shadow
    acx = 260
    d.ellipse([acx - 110, 350, acx + 110, 396], fill=(12, 12, 16))
    d.rounded_rectangle([acx - 30, 170, acx + 30, 250], radius=10, fill=(0x2E, 0x7A, 0xC0))
    d.rounded_rectangle([acx - 26, 120, acx + 26, 172], radius=10, fill=(0xC8, 0x9A, 0x6A))
    d.rectangle([acx - 26, 120, acx + 26, 138], fill=(0x4A, 0x30, 0x1E))
    d.rounded_rectangle([acx - 52, 174, acx - 32, 244], radius=8, fill=(0x2E, 0x7A, 0xC0))
    d.rounded_rectangle([acx + 32, 174, acx + 52, 244], radius=8, fill=(0x2E, 0x7A, 0xC0))
    d.rounded_rectangle([acx - 26, 250, acx - 4, 344], radius=8, fill=(0x2A, 0x3A, 0x5A))
    d.rounded_rectangle([acx + 4, 250, acx + 26, 344], radius=8, fill=(0x2A, 0x3A, 0x5A))
    d.polygon([(acx - 42, 178), (acx + 42, 178), (acx + 32, 330), (acx - 32, 330)],
              fill=(0x0F, 0x5B, 0xE4))

    # instance identity + stats
    text(d, (440, 130), "Latest Release", font(13, True), TEXT_SECONDARY)
    text(d, (440, 168), "1.21.132", font(34, True), TEXT_PRIMARY)
    text(d, (440, 210), "Instance · Default", font(15), TEXT_SECONDARY)
    stats = [("12", "mods"), ("3", "voice"), ("8h 20m", "playtime")]
    sx = 440
    for value, label in stats:
        text(d, (sx, 268), value, font(24, True), ACCENT_300)
        text(d, (sx, 300), label, font(13), TEXT_SECONDARY)
        sx += 150

    # section header + info cards
    text(d, (40, 452), "MORE", font(13, True), TEXT_SECONDARY)
    cards = [("Mods", "12 active"), ("Voice", "LAN · world"), ("Replays", "8 clips saved")]
    for i, (title, sub) in enumerate(cards):
        cx = 40 + i * 400
        rrect(d, [cx, 480, cx + 380, 580], 14, fill=BG_SURFACE, outline=HAIRLINE, width=1)
        text(d, (cx + 24, 508), title, font(18, True), TEXT_PRIMARY)
        text(d, (cx + 24, 538), sub, font(14), TEXT_SECONDARY)

    # dock
    rrect(d, [40, 610, W - 40, 696], 20, fill=BG_WINDOW, outline=HAIRLINE, width=2)
    rrect(d, [60, 626, 560, 680], 14, fill=BG_SURFACE, outline=HAIRLINE, width=1)
    text(d, (84, 646), "Version", font(13), TEXT_SECONDARY)
    text(d, (84, 666), "1.21.132", font(15, True), TEXT_PRIMARY)
    # play button gradient
    pb = [600, 626, W - 60, 680]
    for x in range(pb[0], pb[2]):
        t = (x - pb[0]) / (pb[2] - pb[0])
        col = tuple(int(ACCENT_500[i] * (1 - t) + ACCENT_400[i] * t) for i in range(3))
        d.line([(x, pb[1]), (x, pb[3])], fill=col)
    rrect(d, pb, 14, fill=None, outline=None)
    text(d, ((pb[0] + pb[2]) // 2, (pb[1] + pb[3]) // 2), "▶  LAUNCH", font(24, True),
         (255, 255, 255), anchor="mm")

    im.save(os.path.join(OUT, "launch_tab.png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    radial_wheel()
    mod_menu()
    ant_egg()
    closet()
    launch_tab()
    print("wrote mockups to", os.path.abspath(OUT))


if __name__ == "__main__":
    main()
