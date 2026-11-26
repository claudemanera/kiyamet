"""TWD 1.6 farming art: 10 fruits/vegetables, seeds, crop stages, 2 fruit trees (log, leaves x3, sapling), 4 pelts, fur armor overlays."""
import os, random, math
from PIL import Image
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet/textures')
os.makedirs(f'{A}/item', exist_ok=True); os.makedirs(f'{A}/block', exist_ok=True)
os.makedirs(f'{A}/entity/equipment/humanoid', exist_ok=True); os.makedirs(f'{A}/entity/equipment/humanoid_leggings', exist_ok=True)

def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
def dark(c, k=0.6): return tuple(int(v * k) for v in c)
def light(c, k=0.35): return mix(c, (255, 255, 255), k)

class C:
    def __init__(s, w=16, h=16): s.im = Image.new('RGBA', (w, h), (0, 0, 0, 0)); s.w, s.h = w, h
    def px(s, x, y, c, a=255):
        if 0 <= x < s.w and 0 <= y < s.h: s.im.putpixel((int(x), int(y)), tuple(c[:3]) + (a,))
    def get(s, x, y): return s.im.getpixel((x, y)) if 0 <= x < s.w and 0 <= y < s.h else (0, 0, 0, 0)
    def blob(s, cx, cy, rx, ry, col, rot=0.0):
        """shaded ellipse lit from the top-left"""
        for y in range(s.h):
            for x in range(s.w):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                ux = dx * math.cos(rot) + dy * math.sin(rot); uy = -dx * math.sin(rot) + dy * math.cos(rot)
                d = (ux / rx) ** 2 + (uy / ry) ** 2
                if d <= 1:
                    l = (-ux / rx - uy / ry) * 0.5          # light direction
                    c = mix(dark(col, 0.55), col, min(1, max(0, 0.55 + l * 0.6 + (1 - d) * 0.3)))
                    if d > 0.75: c = dark(c, 0.8)
                    s.px(x, y, c)
    def outline(s, k=0.35):
        src = s.im.copy()
        for y in range(s.h):
            for x in range(s.w):
                if src.getpixel((x, y))[3]: continue
                ns = [src.getpixel((x + a, y + b)) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + a < s.w and 0 <= y + b < s.h and src.getpixel((x + a, y + b))[3]]
                if ns: s.px(x, y, dark(ns[0], k))
    def save(s, path): s.im.save(path)

GREEN = (70, 150, 50); LEAF = (58, 128, 44); STEM = (90, 70, 40)

def leaf(c, x, y, col=LEAF, n=3, dx=1, dy=-1):
    for i in range(n): c.px(x + dx * i, y + dy * i, col if i else dark(col, 0.8)); c.px(x + dx * i + (1 if dy else 0), y + dy * i, light(col, 0.15))

# ------------------------------------------------------------------ produce
def tomato(c):
    c.blob(8, 9.5, 5.6, 5, (214, 40, 30)); c.px(5, 7, (255, 170, 150)); c.px(6, 6, (255, 200, 180))
    for (x, y) in ((8, 4), (6, 4), (10, 4), (7, 5), (9, 5), (8, 3)): c.px(x, y, GREEN)
    c.px(8, 2, STEM)
def corn(c):
    c.blob(8.5, 8, 3.3, 6.5, (240, 196, 60), rot=-0.45)
    for y in range(3, 14):
        for x in range(4, 14):
            p = c.get(x, y)
            if p[3] and (x + y) % 2 == 0: c.px(x, y, dark(p[:3], 0.85))
    for i in range(7): c.px(3 + i // 2, 15 - i, (110, 160, 60)); c.px(12 - i // 3, 15 - i // 2, (90, 140, 50))
    c.px(12, 1, (200, 170, 90)); c.px(13, 0, (200, 170, 90))
def cucumber(c):
    c.blob(8, 8, 2.8, 6.8, (60, 140, 50), rot=0.7)
    for (x, y) in ((6, 6), (9, 8), (8, 11), (11, 5), (5, 10)): c.px(x, y, (150, 210, 110))
    c.px(12, 3, (190, 170, 60))
def eggplant(c):
    c.blob(7.5, 9.5, 4.2, 5.4, (90, 40, 120), rot=0.45)
    c.px(5, 8, (190, 140, 220)); c.px(6, 7, (160, 110, 200))
    for (x, y) in ((10, 4), (11, 4), (9, 5), (11, 5), (12, 5), (10, 5)): c.px(x, y, GREEN)
    c.px(12, 3, STEM); c.px(13, 2, STEM)
def bell_pepper(c):
    c.blob(6.3, 10, 3.2, 4.8, (220, 50, 30)); c.blob(9.7, 10, 3.2, 4.8, (200, 40, 26)); c.blob(8, 9.5, 2.6, 4.8, (232, 62, 40))
    c.px(5, 8, (255, 160, 140)); c.px(5, 9, (255, 130, 110))
    for (x, y) in ((7, 4), (8, 4), (9, 4)): c.px(x, y, GREEN)
    c.px(8, 3, (70, 130, 40)); c.px(9, 2, (70, 130, 40))
def onion(c):
    c.blob(8, 10, 5, 4.6, (196, 140, 70))
    for x in (6, 8, 10): 
        for y in range(7, 14): 
            if c.get(x, y)[3]: c.px(x, y, dark(c.get(x, y)[:3], 0.82))
    c.px(5, 8, (240, 200, 140)); c.px(8, 5, (150, 100, 50)); c.px(8, 4, (120, 170, 70)); c.px(7, 3, (110, 160, 60)); c.px(9, 2, (120, 180, 70)); c.px(7, 2, (100, 150, 60))
    for x in range(6, 11): c.px(x, 15, (220, 200, 170) if x % 2 else (0, 0, 0)) if False else None
def cabbage(c):
    c.blob(8, 9, 6, 5.6, (120, 186, 80))
    c.blob(8, 8.5, 3.6, 3.4, (160, 210, 110))
    for (x0, y0, x1, y1) in ((8, 8, 3, 12), (8, 8, 13, 12), (8, 8, 8, 14), (8, 8, 4, 5), (8, 8, 12, 5)):
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1): c.px(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), (205, 235, 170))
def strawberry(c):
    for y in range(5, 15):
        w = int(5.5 - (y - 5) * 0.48) if y > 6 else 4 + (y - 5)
        for x in range(8 - w, 8 + w):
            t = (x - (8 - w)) / max(1, 2 * w)
            c.px(x, y, mix((235, 50, 50), (150, 20, 30), t * 0.8 + (y - 5) / 25))
    for (x, y) in ((5, 7), (8, 8), (10, 7), (6, 10), (9, 11), (7, 13), (11, 9), (4, 9)): c.px(x, y, (250, 230, 120))
    for (x, y) in ((5, 4), (6, 4), (7, 3), (8, 4), (9, 3), (10, 4), (11, 4), (8, 2)): c.px(x, y, GREEN)
def orange(c):
    c.blob(8, 9, 5.8, 5.8, (240, 140, 30))
    for (x, y) in ((6, 9), (9, 11), (11, 8), (7, 12), (10, 6), (5, 11)): c.px(x, y, (215, 115, 20))
    c.px(5, 6, (255, 210, 150)); c.px(6, 5, (255, 200, 130))
    c.px(8, 3, STEM); c.px(9, 2, LEAF); c.px(10, 2, LEAF); c.px(11, 1, light(LEAF)); c.px(10, 1, LEAF)
def peach(c):
    c.blob(8, 9.5, 5.6, 5.4, (250, 170, 120))
    for y in range(5, 15):
        for x in range(9, 15):
            p = c.get(x, y)
            if p[3]: c.px(x, y, mix(p[:3], (230, 80, 80), 0.45))
    for y in range(5, 13): c.px(8 + (y > 9), y, dark(c.get(8, y)[:3] if c.get(8, y)[3] else (220, 120, 90), 0.8))
    c.px(5, 7, (255, 225, 190)); c.px(8, 3, STEM); c.px(6, 2, LEAF); c.px(7, 2, LEAF); c.px(5, 1, light(LEAF))

PRODUCE = dict(tomato=tomato, corn=corn, cucumber=cucumber, eggplant=eggplant, bell_pepper=bell_pepper, onion=onion, cabbage=cabbage, strawberry=strawberry, orange=orange, peach=peach)
COL = dict(tomato=(214, 40, 30), corn=(240, 196, 60), cucumber=(60, 140, 50), eggplant=(90, 40, 120), bell_pepper=(220, 50, 30), onion=(196, 140, 70),
           cabbage=(140, 200, 90), strawberry=(225, 40, 45), orange=(240, 140, 30), peach=(250, 160, 120))
SEED = dict(tomato=(236, 214, 150), corn=(245, 205, 80), cucumber=(240, 236, 200), eggplant=(220, 196, 140), bell_pepper=(246, 226, 170), onion=(40, 36, 36),
            cabbage=(110, 60, 50), strawberry=(240, 220, 110))

for n, f in PRODUCE.items():
    c = C(); f(c); c.outline(); c.save(f'{A}/item/{n}.png')

# ------------------------------------------------------------------ seeds (a little paper packet + seeds)
for n, col in SEED.items():
    c = C(); r = random.Random(n)
    for y in range(3, 13):
        for x in range(3, 11): c.px(x, y, (226, 212, 180) if y > 4 else (196, 180, 150))
    for y in range(6, 11):
        for x in range(4, 10): c.px(x, y, COL[n])
    c.px(5, 7, light(COL[n], 0.4)); c.px(4, 6, light(COL[n], 0.2))
    for (x, y) in ((11, 11), (12, 13), (10, 14), (13, 10), (12, 12), (14, 12)): c.px(x, y, col); c.px(x, y - 1 if (x + y) % 2 else y, light(col, 0.3))
    c.outline(); c.save(f'{A}/item/{n}_seeds.png')

# ------------------------------------------------------------------ crop stages 0..3
def plant(c, h, col, stage, produce, tall=False):
    r = random.Random(stage * 7 + len(produce))
    for sx in (3, 8, 12):
        hh = h + r.randint(-1, 1)
        for y in range(15, 15 - hh, -1): c.px(sx, y, dark(GREEN, 0.85) if y % 3 else GREEN)
        for y in range(15 - hh + 1, 15, 2):
            side = 1 if (y + sx) % 4 else -1
            c.px(sx + side, y, (86, 168, 60)); c.px(sx + 2 * side, y - 1, (110, 190, 70)) if stage > 1 else None
    if stage == 3:
        for sx in (3, 8, 12):
            y = 15 - h + 3 + r.randint(0, 3)
            if produce == 'corn':
                for yy in range(y, y + 4): c.px(sx + 1, yy, (240, 200, 70)); c.px(sx + 1, y - 1, (210, 190, 120))
            elif produce in ('cabbage',):
                for (a, b) in ((0, 0), (1, 0), (-1, 0), (0, -1), (1, -1), (0, 1)): c.px(sx + a, 13 + b, (150, 205, 100) if (a + b) % 2 else (120, 180, 80))
            elif produce == 'onion':
                for (a, b) in ((0, 0), (1, 0), (0, -1), (1, -1)): c.px(sx + a, 14 + b, (200, 150, 80))
            else:
                big = produce in ('eggplant', 'cucumber')
                c.px(sx + 1, y, col); c.px(sx + 1, y + 1, dark(col, 0.8)); c.px(sx + 2, y, light(col, 0.2))
                if big: c.px(sx + 1, y + 2, dark(col, 0.7))
for n in SEED:
    tall = n == 'corn'
    for st, h in enumerate((3, 6, 9, 12) if not tall else (4, 8, 12, 15)):
        c = C()
        if n == 'cabbage': h = (2, 4, 5, 6)[st]
        if n == 'onion': h = (2, 4, 6, 8)[st]
        if n == 'strawberry': h = (2, 3, 5, 6)[st]
        plant(c, h, COL[n], st, n, tall)
        c.save(f'{A}/block/{n}_crop_stage{st}.png')

# ------------------------------------------------------------------ trees
BARK = dict(orange=((112, 84, 56), (86, 62, 40), (140, 110, 76)), peach=((104, 62, 52), (76, 44, 38), (134, 88, 74)))
for n, (b, d, l) in BARK.items():
    c = C(); r = random.Random(n)
    for x in range(16):
        base = [b, d, l][0 if x % 4 else 1]
        for y in range(16):
            cc = base
            if (x * 3 + y * 5 + r.randint(0, 3)) % 9 == 0: cc = d
            if (x + y * 2) % 13 == 0: cc = l
            c.px(x, y, cc)
    c.save(f'{A}/block/{n}_log.png')
    c = C()
    for y in range(16):
        for x in range(16):
            d2 = math.hypot(x - 7.5, y - 7.5)
            if d2 > 7.2: c.px(x, y, d)
            else: c.px(x, y, mix((214, 176, 120), (170, 130, 86), (math.sin(d2 * 2.1) + 1) / 2) if n == 'orange' else mix((200, 140, 110), (160, 100, 80), (math.sin(d2 * 2.1) + 1) / 2))
    c.save(f'{A}/block/{n}_log_top.png')

LEAVES = dict(orange=((46, 112, 40), (66, 140, 52), (30, 84, 30)), peach=((70, 132, 52), (96, 160, 68), (48, 100, 38)))
BLOSSOM = dict(orange=(250, 250, 240), peach=(250, 170, 200))
for n, (m, l, d) in LEAVES.items():
    r = random.Random(n + 'leaf')
    base = C()
    for y in range(16):
        for x in range(16):
            v = r.random()
            if v < 0.1: continue                       # little gaps like vanilla leaves
            base.px(x, y, l if v > 0.8 else d if v < 0.3 else m)
    base.save(f'{A}/block/{n}_leaves_0.png')
    b1 = C(); b1.im = base.im.copy()
    for (x, y) in ((3, 2), (10, 4), (6, 9), (13, 12), (2, 13), (9, 14), (12, 7)):
        b1.px(x, y, BLOSSOM[n]); b1.px(x + 1, y, light(BLOSSOM[n], 0.3)); b1.px(x, y + 1, dark(BLOSSOM[n], 0.85))
    b1.save(f'{A}/block/{n}_leaves_1.png')
    b2 = C(); b2.im = base.im.copy()
    fc = COL[n]
    for (x, y) in ((2, 3), (10, 2), (6, 8), (12, 10), (3, 12), (9, 13)):
        b2.px(x, y, light(fc, 0.25)); b2.px(x + 1, y, fc); b2.px(x, y + 1, fc); b2.px(x + 1, y + 1, dark(fc, 0.75))
    b2.save(f'{A}/block/{n}_leaves_2.png')
    s = C()
    for y in range(8, 16): s.px(7, y, BARK[n][0]); s.px(8, y, BARK[n][1])
    for (x, y) in ((4, 3), (5, 2), (6, 2), (7, 1), (8, 1), (9, 2), (10, 2), (11, 3), (3, 5), (12, 5), (5, 6), (10, 6), (7, 4), (8, 5), (6, 4), (9, 4),
                   (4, 4), (11, 4), (5, 3), (10, 3), (6, 5), (9, 5), (7, 6), (8, 6), (7, 2), (8, 2), (6, 3), (9, 3), (7, 3), (8, 3), (7, 5), (8, 4)):
        s.px(x, y, [m, l, d][(x + y) % 3])
    s.px(6, 3, fc); s.px(10, 4, fc)
    s.outline(0.5); s.save(f'{A}/block/{n}_sapling.png')

# ------------------------------------------------------------------ pelts + fur armor layers
FUR = dict(wolf=((150, 150, 156), (110, 110, 118), (196, 196, 204), (70, 70, 78)),
           fox=((214, 112, 40), (170, 80, 30), (244, 160, 80), (250, 246, 236)),
           polar=((236, 234, 226), (204, 200, 188), (255, 255, 250), (176, 172, 160)),
           sheep=((226, 210, 176), (190, 170, 130), (246, 236, 210), (150, 126, 90)))

def furpx(r, pal, x, y):
    v = r.random()
    return pal[2] if v > 0.82 else pal[1] if v < 0.3 else pal[0]

for n, pal in FUR.items():
    r = random.Random(n)
    c = C()
    shape = ["....xxxxxxx.....", "..xxxxxxxxxxx...", ".xxxxxxxxxxxxx..", "xxxxxxxxxxxxxxx.", ".xxxxxxxxxxxxxxx", "..xxxxxxxxxxxxx.",
             "...xxxxxxxxxxx..", "..xxxxxxxxxxxx..", ".xxxxxxxxxxxxxx.", "xxxxxxxxxxxxxxx.", ".xxx.xxxxxx.xxx.", "..x...xxxx...x.."]
    for j, row in enumerate(shape):
        for i, ch in enumerate(row):
            if ch == 'x': c.px(i, j + 2, furpx(r, pal, i, j))
    if n == 'fox':
        for i in range(10, 15): c.px(i, 11, pal[3]); c.px(i, 10, pal[3])
    c.outline(); c.save(f'{A}/item/{n}_pelt.png' if n != 'sheep' else f'{A}/item/sheepskin.png')

    # armor overlay, vanilla humanoid 64x32 UV layout
    h = C(64, 32)
    def area(x0, y0, x1, y1, p=1.0):
        for y in range(y0, y1):
            for x in range(x0, x1):
                if r.random() < p: h.px(x, y, furpx(r, pal, x, y))
    # head: fur hat (top + band around the forehead)
    area(8, 0, 16, 8); area(16, 0, 24, 8, 0.6)
    area(0, 8, 32, 11)                           # band all round
    for x in range(0, 32, 2): h.px(x, 11, furpx(r, pal, x, 11))   # fringe
    # body: big collar + open coat with fur trim + back mantle
    area(20, 16, 28, 20)                          # top of the body (shoulders seen from above)
    area(16, 20, 40, 23)                          # collar all round
    for y in range(23, 32):
        for x in (20, 21, 26, 27): h.px(x, y, furpx(r, pal, x, y))   # coat trim down the front
        for x in range(32, 40): h.px(x, y, furpx(r, pal, x, y))       # back mantle
        for x in (16, 17, 18, 19, 28, 29, 30, 31):
            if r.random() < 0.55: h.px(x, y, furpx(r, pal, x, y))
    area(16, 30, 40, 32)                          # hem
    # arms: shoulders + cuffs
    area(44, 16, 52, 20); area(40, 20, 56, 24); area(40, 29, 56, 32)
    # legs (boots): fur tops
    area(0, 26, 16, 29)
    for x in range(0, 16): 
        if r.random() < 0.6: h.px(x, 29, furpx(r, pal, x, 29))
    if n == 'fox':                                # white chest
        for y in range(23, 28):
            for x in (22, 23, 24, 25): h.px(x, y, pal[3])
    h.save(f'{A}/entity/equipment/humanoid/fur_{n}.png')
    lg = C(64, 32)
    for y in range(20, 23):
        for x in range(0, 16): lg.px(x, y, furpx(r, pal, x, y))
    for x in range(16, 24): lg.px(x, 26 if False else 20, furpx(r, pal, x, 20)) if False else None
    for y in range(16, 20):
        for x in range(4, 12): lg.px(x, y, furpx(r, pal, x, y))
    lg.save(f'{A}/entity/equipment/humanoid_leggings/fur_{n}.png')
print('farm art done')
