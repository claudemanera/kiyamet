"""Logos for the 20 skills and 53 powers: 32x32 badges with a white glyph, written as item textures + models."""
import math, os, re, json
from PIL import Image, ImageDraw, ImageFilter, ImageChops

S = 256
REPO = '/home/claude/repo'
TEX = REPO + '/src/main/resources/assets/kiyamet/textures/item/icon'
MOD = REPO + '/src/main/resources/assets/kiyamet/models/item/icon'
ITM = REPO + '/src/main/resources/assets/kiyamet/items/icon'
for d in (TEX, MOD, ITM): os.makedirs(d, exist_ok=True)


def poly(d, pts, f=255): d.polygon([(x, y) for x, y in pts], fill=f)
def circ(d, cx, cy, r, f=255): d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=f)
def line(d, pts, w, f=255):
    d.line(pts, fill=f, width=w, joint='curve')
    for p in (pts[0], pts[-1]): circ(d, p[0], p[1], w / 2, f)
def rect(d, x0, y0, x1, y1, r=0, f=255): d.rounded_rectangle([x0, y0, x1, y1], radius=r, fill=f)


# ---------------------------------------------------------------- glyphs (drawn white on a 256 mask, centre 128)
def heart(d, s=1.0, cx=128, cy=132):
    r = 38 * s
    circ(d, cx - r * 0.95, cy - r * 0.55, r); circ(d, cx + r * 0.95, cy - r * 0.55, r)
    poly(d, [(cx - r * 1.9, cy - r * 0.35), (cx + r * 1.9, cy - r * 0.35), (cx, cy + r * 1.75)])
def boot(d):
    poly(d, [(92, 56), (140, 56), (140, 140), (196, 162), (204, 196), (70, 196), (70, 170), (92, 150)])
    for y in (84, 108): line(d, [(140, y), (168, y - 14)], 10)  # wing strokes
    line(d, [(140, 132), (182, 112)], 10)
def pickaxe(d):
    t = Image.new('L', (S, S), 0); td = ImageDraw.Draw(t)
    line(td, [(128, 226), (128, 70)], 22)                       # handle
    td.arc([20, 50, 236, 266], 218, 322, fill=255, width=30)    # head
    poly(td, [(40, 118), (56, 96), (28, 150)]); poly(td, [(216, 118), (200, 96), (228, 150)])
    t = t.rotate(-38, resample=Image.BICUBIC, center=(128, 128))
    d._image.paste(255, (0, 0), t)
def sword(d):
    poly(d, [(184, 52), (196, 64), (112, 160), (96, 144)]); poly(d, [(184, 52), (204, 44), (196, 64)])
    line(d, [(78, 128), (128, 178)], 18); line(d, [(98, 158), (66, 190)], 16); circ(d, 62, 194, 14)
def bow(d):
    d.arc([40, 40, 200, 200], 200, 340, fill=255, width=20)
    line(d, [(58, 98), (182, 98)], 6)
    line(d, [(120, 210), (120, 70)], 10); poly(d, [(120, 44), (140, 78), (100, 78)])
    poly(d, [(120, 200), (136, 222), (104, 222)])
def shield(d, s=1.0, cx=128, cy=128):
    k = s
    poly(d, [(cx - 66 * k, cy - 74 * k), (cx + 66 * k, cy - 74 * k), (cx + 66 * k, cy - 6 * k), (cx, cy + 82 * k), (cx - 66 * k, cy - 6 * k)])
def shield_hollow(d):
    shield(d); shield(d, 0.62, 128, 122); d2 = d  # inner cut done by caller
def loop(d):
    d.arc([62, 48, 194, 180], 30, 330, fill=255, width=24)
    poly(d, [(196, 70), (206, 128), (156, 108)])
    circ(d, 128, 210, 18)
def waves(d):
    for y in (92, 136, 180):
        pts = [(48 + i * 4, y + 14 * math.sin(i / 5.0 * math.pi)) for i in range(41)]
        line(d, pts, 18)
def wheat(d):
    line(d, [(128, 220), (128, 60)], 12)
    for i, y in enumerate((76, 108, 140, 172)):
        d.ellipse([90, y - 14, 126, y + 14], fill=255); d.ellipse([130, y - 14, 166, y + 14], fill=255)
    d.ellipse([114, 36, 142, 74], fill=255)
def axe(d):
    line(d, [(84, 208), (160, 64)], 20)
    poly(d, [(132, 70), (196, 44), (212, 118), (162, 120)])
def paw(d):
    d.ellipse([84, 120, 172, 200], fill=255)
    for x, y in ((70, 92), (106, 62), (150, 62), (186, 92)): circ(d, x, y, 20)
def book(d):
    poly(d, [(40, 84), (124, 98), (124, 206), (40, 192)]); poly(d, [(132, 98), (216, 84), (216, 192), (132, 206)])
    star(d, 128, 54, 30, 12)
def star(d, cx, cy, ro, ri, n=4, f=255):
    pts = []
    for i in range(n * 2):
        a = math.pi * i / n - math.pi / 2; r = ro if i % 2 == 0 else ri
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    poly(d, pts, f)
def flask(d):
    rect(d, 108, 40, 148, 108); poly(d, [(108, 100), (148, 100), (206, 200), (50, 200)])
    rect(d, 96, 36, 160, 52, 6)
def flask_cut(d): circ(d, 104, 168, 12, 0); circ(d, 146, 152, 9, 0)
def compass(d):
    d.ellipse([40, 40, 216, 216], outline=255, width=18)
    poly(d, [(128, 56), (150, 128), (128, 200), (106, 128)])
def gear(d, cx=128, cy=128, r=70, f=255):
    for i in range(8):
        a = i * math.pi / 4
        x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
        poly(d, [(x + math.cos(a + 1.57) * 18 - math.cos(a) * 20, y + math.sin(a + 1.57) * 18 - math.sin(a) * 20),
                 (x - math.cos(a + 1.57) * 18 - math.cos(a) * 20, y - math.sin(a + 1.57) * 18 - math.sin(a) * 20),
                 (x - math.cos(a + 1.57) * 14 + math.cos(a) * 14, y - math.sin(a + 1.57) * 14 + math.sin(a) * 14),
                 (x + math.cos(a + 1.57) * 14 + math.cos(a) * 14, y + math.sin(a + 1.57) * 14 + math.sin(a) * 14)], f)
    circ(d, cx, cy, r * 0.82, f)
def flame(d, s=1.0, cx=128, cy=128):
    k = s
    poly(d, [(cx, cy - 92 * k), (cx + 30 * k, cy - 40 * k), (cx + 60 * k, cy - 50 * k), (cx + 66 * k, cy + 20 * k), (cx + 50 * k, cy + 70 * k),
             (cx, cy + 88 * k), (cx - 50 * k, cy + 70 * k), (cx - 66 * k, cy + 20 * k), (cx - 46 * k, cy - 34 * k), (cx - 20 * k, cy - 10 * k)])
def campfire(d):
    flame(d, 0.8, 128, 108)
    line(d, [(52, 206), (204, 178)], 18); line(d, [(52, 178), (204, 206)], 18)
def eye(d, f=255):
    d.chord([28, 70, 228, 190], 180, 360, fill=f); d.chord([28, 66, 228, 186], 0, 180, fill=f)
def eye_cut(d):
    circ(d, 128, 128, 36, 0); circ(d, 128, 128, 18, 255)
def hood(d):
    poly(d, [(128, 36), (204, 110), (196, 214), (60, 214), (52, 110)])
def hood_cut(d):
    d.ellipse([78, 104, 178, 196], fill=0)
    circ(d, 108, 146, 11); circ(d, 148, 146, 11)
def cross(d):
    rect(d, 102, 44, 154, 212, 10); rect(d, 44, 102, 212, 154, 10)
def crosshair(d):
    d.ellipse([46, 46, 210, 210], outline=255, width=18)
    for a, b in (((128, 26), (128, 92)), ((128, 164), (128, 230)), ((26, 128), (92, 128)), ((164, 128), (230, 128))): line(d, [a, b], 16)
    circ(d, 128, 128, 14)
def bricks(d):
    for row in range(4):
        y = 52 + row * 40; off = 0 if row % 2 == 0 else 36
        for col in range(-1, 4):
            x = 40 + off + col * 72
            x0, x1 = max(40, x), min(216, x + 64)
            if x1 - x0 > 10: rect(d, x0, y, x1, y + 32, 4)
def bolt(d):
    poly(d, [(150, 30), (70, 140), (124, 140), (100, 226), (190, 106), (134, 106), (166, 30)])
def uparrow(d):
    poly(d, [(128, 36), (204, 120), (154, 120), (154, 214), (102, 214), (102, 120), (52, 120)])
def feather(d):
    poly(d, [(196, 40), (210, 60), (150, 150), (104, 196), (70, 192), (74, 160), (120, 110)])
    line(d, [(56, 214), (176, 76)], 8, 0); line(d, [(56, 214), (98, 168)], 10)
def spark_pick(d):
    pickaxe(d); star(d, 196, 172, 34, 10)
def fist(d):
    rect(d, 66, 76, 190, 172, 30)
    rect(d, 86, 160, 172, 214, 14)
    poly(d, [(56, 120), (90, 102), (100, 150), (66, 156)])
def fist_cut(d):
    for x in (100, 130, 160): line(d, [(x, 80), (x, 120)], 6, 0)
def plus(d, cx, cy, r, w, f=255):
    rect(d, cx - w, cy - r, cx + w, cy + r, 4, f); rect(d, cx - r, cy - w, cx + r, cy + w, 4, f)
def drumstick(d):
    d.ellipse([70, 50, 190, 160], fill=255); line(d, [(110, 140), (66, 196)], 22)
    circ(d, 56, 196, 16); circ(d, 70, 212, 16)
def bubbles(d):
    for x, y, r in ((96, 168, 40), (160, 118, 30), (110, 74, 22), (168, 54, 14)):
        d.ellipse([x - r, y - r, x + r, y + r], outline=255, width=14)
def dolphin(d):
    d.chord([36, 70, 220, 210], 190, 350, fill=255)
    poly(d, [(120, 82), (150, 46), (156, 90)]); poly(d, [(46, 120), (20, 90), (30, 140)])
    waves_small(d)
def waves_small(d):
    pts = [(40 + i * 4.4, 196 + 10 * math.sin(i / 5.0 * math.pi)) for i in range(41)]
    line(d, pts, 14)
def conduit(d):
    d.ellipse([36, 36, 220, 220], outline=255, width=20); eye(d)
def ghost(d):
    d.pieslice([56, 40, 200, 184], 180, 360, fill=255); rect(d, 56, 110, 200, 190)
    for i in range(4): poly(d, [(56 + i * 36, 186), (74 + i * 36, 216), (92 + i * 36, 186)])
def ghost_cut(d): circ(d, 104, 112, 13, 0); circ(d, 152, 112, 13, 0)
def clover(d):
    for dx, dy in ((0, -40), (40, 0), (0, 40), (-40, 0)): circ(d, 128 + dx, 118 + dy, 34)
    line(d, [(128, 140), (168, 218)], 12)
def banner(d):
    line(d, [(64, 36), (192, 36)], 12)
    poly(d, [(80, 44), (176, 44), (176, 212), (128, 178), (80, 212)])
def banner_cut(d): star(d, 128, 104, 30, 13, 5, 0)


SKILLS = {
    'health': (heart, None, (220, 50, 60), (130, 16, 30)),
    'athletics': (boot, None, (60, 200, 120), (16, 100, 60)),
    'mining': (pickaxe, None, (150, 150, 165), (60, 60, 75)),
    'swords': (sword, None, (230, 90, 60), (120, 30, 20)),
    'archery': (bow, None, (200, 150, 70), (100, 60, 20)),
    'defense': (shield, lambda d: (shield(d, 0.55, 128, 120)), (70, 120, 220), (20, 40, 110)),
    'acrobatics': (loop, None, (130, 220, 230), (30, 100, 130)),
    'swimming': (waves, None, (50, 150, 230), (10, 60, 130)),
    'farming': (wheat, None, (230, 200, 60), (120, 90, 10)),
    'woodcutting': (axe, None, (160, 110, 60), (80, 45, 20)),
    'hunting': (paw, None, (150, 100, 70), (70, 40, 20)),
    'enchanting': (book, None, (170, 90, 230), (70, 20, 120)),
    'alchemy': (flask, flask_cut, (210, 80, 200), (100, 20, 100)),
    'exploration': (compass, None, (240, 170, 50), (130, 70, 10)),
    'engineering': (gear, lambda d: circ(d, 128, 128, 26, 0), (240, 120, 40), (120, 50, 10)),
    'survival': (campfire, None, (240, 110, 40), (110, 30, 10)),
    'stealth': (hood, hood_cut, (90, 90, 120), (25, 25, 40)),
    'medicine': (cross, None, (235, 70, 80), (130, 20, 30)),
    'heavy_weapons': (crosshair, None, (200, 60, 50), (70, 20, 20)),
    'building': (bricks, None, (210, 110, 80), (110, 45, 30)),
}

EFFECTS = {
    'NIGHT_VISION': (eye, eye_cut, (40, 70, 200), (10, 15, 70)),
    'SPEED': (bolt, None, (90, 200, 240), (20, 80, 130)),
    'JUMP_BOOST': (uparrow, None, (120, 230, 120), (20, 110, 40)),
    'SLOW_FALLING': (feather, None, (230, 230, 245), (120, 120, 150)),
    'HASTE': (spark_pick, None, (240, 210, 60), (130, 100, 10)),
    'STRENGTH': (fist, fist_cut, (220, 60, 50), (110, 15, 15)),
    'RESISTANCE': (shield, lambda d: plus(d, 128, 116, 34, 11, 0), (130, 140, 160), (50, 55, 70)),
    'ABSORPTION': (heart, lambda d: heart(d, 0.55, 128, 128), (250, 200, 50), (150, 100, 10)),
    'REGENERATION': (heart, lambda d: plus(d, 128, 118, 30, 9, 0), (240, 100, 170), (130, 30, 80)),
    'FIRE_RESISTANCE': (flame, lambda d: flame(d, 0.45, 128, 150) if False else None, (240, 130, 40), (130, 40, 10)),
    'SATURATION': (drumstick, None, (210, 140, 80), (110, 60, 20)),
    'WATER_BREATHING': (bubbles, None, (60, 140, 230), (15, 50, 120)),
    'DOLPHINS_GRACE': (dolphin, None, (90, 170, 230), (20, 70, 130)),
    'CONDUIT_POWER': (conduit, eye_cut, (40, 190, 190), (10, 80, 90)),
    'INVISIBILITY': (ghost, ghost_cut, (180, 190, 210), (60, 65, 85)),
    'LUCK': (clover, None, (60, 190, 80), (15, 90, 30)),
    'HERO_OF_THE_VILLAGE': (banner, banner_cut, (60, 180, 90), (20, 80, 40)),
    'INSTANT_HEALTH': (heart, lambda d: bolt_small(d), (250, 70, 90), (130, 15, 30)),
}
def bolt_small(d):
    poly(d, [(140, 70), (102, 132), (128, 132), (114, 180), (156, 116), (130, 116), (148, 70)], 0)


def render(glyph, cut, c1, c2, shape='circle', pips=0, mini=None, special=False):
    big = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    # badge
    m = Image.new('L', (S, S), 0); dm = ImageDraw.Draw(m)
    if shape == 'circle': dm.ellipse([6, 6, S - 6, S - 6], fill=255)
    else: dm.rounded_rectangle([6, 6, S - 6, S - 6], radius=56, fill=255)
    grad = Image.new('RGBA', (S, S))
    gp = grad.load()
    for y in range(S):
        for x in range(S):
            t = min(1, max(0, (x * 0.35 + y * 0.65) / S))
            gp[x, y] = tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(3)) + (255,)
    big.paste(grad, (0, 0), m)
    # rim
    rim = Image.new('L', (S, S), 0); dr = ImageDraw.Draw(rim)
    if shape == 'circle': dr.ellipse([6, 6, S - 6, S - 6], outline=255, width=12)
    else: dr.rounded_rectangle([6, 6, S - 6, S - 6], radius=56, outline=255, width=12)
    rimc = (255, 215, 90, 255) if special else tuple(min(255, int(v * 0.45)) for v in c2) + (255,)
    big.paste(Image.new('RGBA', (S, S), rimc), (0, 0), rim)
    # gloss
    gl = Image.new('L', (S, S), 0); ImageDraw.Draw(gl).ellipse([30, 14, S - 30, S * 0.55], fill=46)
    big = Image.alpha_composite(big, Image.merge('RGBA', [Image.new('L', (S, S), 255)] * 3 + [ImageChops.multiply(gl, m)]))
    # glyph
    g = Image.new('L', (S, S), 0); dg = ImageDraw.Draw(g)
    glyph(dg)
    if cut: cut(dg)
    g = g.resize((int(S * 0.74),) * 2, Image.LANCZOS); gg = Image.new('L', (S, S), 0); o = (S - g.width) // 2; gg.paste(g, (o, o)); g = gg
    shadow = g.filter(ImageFilter.MaxFilter(13)).filter(ImageFilter.GaussianBlur(2))
    big = Image.alpha_composite(big, Image.merge('RGBA', [Image.new('L', (S, S), int(v * 0.3)) for v in c2] + [ImageChops.multiply(shadow, m)]))
    big = Image.alpha_composite(big, Image.merge('RGBA', [Image.new('L', (S, S), 255), Image.new('L', (S, S), 250), Image.new('L', (S, S), 240), g]))
    # tier pips
    d = ImageDraw.Draw(big)
    for i in range(pips):
        x = S / 2 + (i - (pips - 1) / 2) * 40
        d.ellipse([x - 15, S - 50, x + 15, S - 20], fill=(255, 215, 80, 255), outline=(90, 60, 10, 255), width=5)
    if mini is not None:
        mg, mc, a, b = SKILLS[mini]
        sub = render(mg, mc, a, b, 'circle').resize((112, 112), Image.LANCZOS)
        big.alpha_composite(sub, (S - 116, S - 116))
    return big


def save(name, img):
    img.resize((32, 32), Image.LANCZOS).save(f'{TEX}/{name}.png')
    json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'kiyamet:item/icon/{name}'}}, open(f'{MOD}/{name}.json', 'w'))
    json.dump({'model': {'type': 'minecraft:model', 'model': f'kiyamet:item/icon/{name}'}}, open(f'{ITM}/{name}.json', 'w'))


if __name__ == '__main__':
    for k, (g, c, a, b) in SKILLS.items():
        save('skill_' + k, render(g, c, a, b, 'circle'))
    src = open(REPO + '/src/main/java/com/kiyamet/skill/Powers.java').read()
    ROMAN = {'I': 1, 'II': 2, 'III': 3, 'IV': 4}
    n = 0
    for m in re.finditer(r'^\s+([A-Z_0-9]+)\("([^"]+)", MobEffects\.([A-Z_]+), (\d+), Skill\.([A-Z_]+)', src, re.M):
        name, title, eff, amp, skill = m.groups()
        g, c, a, b = EFFECTS[eff]
        words = title.replace('(1.2)', '').split()
        plain = words[-1] in ROMAN or title.split(' (')[0].replace("'", '').upper().replace(' ', '_') == eff or name in ('NIGHT_VISION', 'SLOW_FALL', 'FIRE_RES', 'SATURATION', 'WATER_BREATH', 'DOLPHIN', 'CONDUIT', 'ABSORPTION', 'HERO')
        pips = ROMAN.get(words[-1], int(amp) + 1 if not plain else 0)
        save('power_' + name.lower(), render(g, c, a, b, 'square', pips, None if plain else skill.lower(), special=not plain))
        n += 1
    print('skills', len(SKILLS), 'powers', n)
