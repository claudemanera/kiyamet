"""TWD 1.6 spawn eggs (colours taken from each mob's own skin) and car-shaped vehicle kits, plus the Vehicle Workshop block."""
import os, sys, random, colorsys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
T = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet/textures')

def dark(c, k=0.6): return tuple(int(v * k) for v in c[:3])
def light(c, k=0.35): return tuple(int(v + (255 - v) * k) for v in c[:3])

def palette(tex):
    im = Image.open(f'{T}/entity/{tex}.png').convert('RGBA')
    q = {}
    for p in im.getdata():
        if p[3] < 200: continue
        k = (p[0] // 24, p[1] // 24, p[2] // 24)
        q.setdefault(k, []).append(p[:3])
    groups = sorted(q.values(), key=len, reverse=True)
    avg = [tuple(sum(c[i] for c in g) // len(g) for i in range(3)) for g in groups[:8]]
    base = avg[0]
    # spot colour: the most different of the common colours
    spot = max(avg[1:6], key=lambda c: sum(abs(c[i] - base[i]) for i in range(3))) if len(avg) > 1 else light(base)
    return base, spot

EGG = ["......xxxx......", ".....xxxxxx.....", "....xxxxxxxx....", "...xxxxxxxxxx...", "...xxxxxxxxxx...", "..xxxxxxxxxxxx..",
       "..xxxxxxxxxxxx..", "..xxxxxxxxxxxx..", ".xxxxxxxxxxxxxx.", ".xxxxxxxxxxxxxx.", ".xxxxxxxxxxxxxx.", ".xxxxxxxxxxxxxx.",
       "..xxxxxxxxxxxx..", "..xxxxxxxxxxxx..", "...xxxxxxxxxx...", ".....xxxxxx....."]

def egg(name, base, spot, mark=None):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); r = random.Random(name)
    for y, row in enumerate(EGG):
        for x, ch in enumerate(row):
            if ch != 'x': continue
            lx = (x - 4) / 12 + (y - 3) / 16
            c = light(base, 0.25) if lx < 0.15 else dark(base, 0.78) if lx > 0.95 else base
            im.putpixel((x, y), c + (255,))
    for _ in range(9):                                  # spots
        x, y = r.randint(3, 12), r.randint(3, 14)
        if EGG[y][x] == 'x':
            im.putpixel((x, y), spot + (255,))
            if x + 1 < 16 and EGG[y][x + 1] == 'x' and r.random() < 0.6: im.putpixel((x + 1, y), dark(spot, 0.85) + (255,))
    im.putpixel((5, 3), light(base, 0.6) + (255,)); im.putpixel((4, 5), light(base, 0.5) + (255,))
    if mark == 'boss':   # gold crown on top
        for x in (5, 7, 8, 10): im.putpixel((x, 0), (255, 214, 60, 255))
        for x in range(5, 11): im.putpixel((x, 1), (230, 180, 40, 255))
    elif mark == 'robot':
        for x in range(4, 12): im.putpixel((x, 9), (110, 230, 255, 255))
        im.putpixel((7, 8), (110, 230, 255, 255)); im.putpixel((9, 10), (110, 230, 255, 255))
    elif mark == 'zombie':  # bite mark
        for (x, y) in ((10, 6), (11, 7), (11, 8), (10, 9)): im.putpixel((x, y), (120, 20, 20, 255))
    elif mark == 'friend':
        for (x, y) in ((7, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (7, 9), (8, 9), (9, 9), (8, 10)): im.putpixel((x, y), (230, 60, 70, 255))
    elif mark == 'enemy':
        for (x, y) in ((6, 7), (10, 7), (7, 8), (9, 8), (8, 9), (7, 10), (9, 10), (6, 11), (10, 11)): im.putpixel((x, y), (20, 20, 20, 255))
    # outline
    out = im.copy()
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3]: continue
            ns = [im.getpixel((x + a, y + b)) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + a < 16 and 0 <= y + b < 16 and im.getpixel((x + a, y + b))[3]]
            if ns: out.putpixel((x, y), dark(ns[0], 0.35) + (255,))
    out.save(f'{T}/item/{name}_spawn_egg.png')

import re
J = os.path.join(os.path.dirname(__file__), '..', 'src/main/java/com/kiyamet')
zombies = re.findall(r'\("(\w+_zombie)"', open(f'{J}/entity/ZombieKind.java').read())
bosses = re.findall(r'^\s+\w+\s*\("(\w+)"', open(f'{J}/boss/BossKind.java').read(), re.M)
robots = re.findall(r'^\s+\w+\("(\w+)"', open(f'{J}/robot/RobotKind.java').read(), re.M)
for z in zombies: egg(z, *palette(z), 'zombie')
for b in bosses: egg(b, *palette(b), 'boss')
for r in robots: egg(r, *palette(r), 'robot')
HUM = {'survivor': ('human', 'friend'), 'companion': ('companion_warrior', 'friend'), 'community_member': ('base_guard', None), 'community_guard': ('base_guard', 'friend'),
       'trader': ('survivor_trader', None), 'raider': ('raider', 'enemy'), 'raider_guard': ('raider_brute', 'enemy'), 'masked_one': ('masked_one', 'enemy')}
for n, (tex, m) in HUM.items():
    base, spot = palette(tex)
    if n == 'community_member': base, spot = (110, 140, 90), (200, 180, 120)
    if n == 'survivor': base, spot = (150, 110, 70), (90, 120, 160)
    egg(n, base, spot, m)

# ------------------------------------------------------------- car kits: little side-view vehicles, not eggs
from draw_farm import C
def car(name, body, roof='sedan', extra=None):
    c = C(); b = body; d = dark(b, 0.7); l = light(b, 0.3); G = (150, 200, 230); K = (26, 26, 30); W = (90, 90, 96)
    if roof in ('sedan', 'police'):
        for x in range(1, 15):
            for y in range(8, 12): c.px(x, y, l if y == 8 else d if y == 11 else b)
        for x in range(4, 12):
            for y in range(5, 8): c.px(x, y, b if x in (4, 11) or y == 5 else G)
        c.px(7, 6, b); c.px(1, 9, (255, 240, 160)); c.px(14, 9, (220, 40, 40))
        if roof == 'police':
            c.px(7, 4, (230, 40, 40)); c.px(8, 4, (40, 90, 240))
            for x in range(5, 11): c.px(x, 10, (240, 240, 240))
    elif roof == 'pickup':
        for x in range(1, 15):
            for y in range(8, 12): c.px(x, y, l if y == 8 else d if y == 11 else b)
        for x in range(2, 8):
            for y in range(4, 8): c.px(x, y, b if x in (2, 7) or y == 4 else G)
        for x in range(9, 15): c.px(x, 7, d)
        c.px(14, 6, d); c.px(9, 6, d); c.px(1, 9, (255, 240, 160))
    elif roof == 'van':
        for x in range(1, 15):
            for y in range(3, 12): c.px(x, y, l if y == 3 else d if y == 11 else b)
        for x in range(1, 4):
            for y in range(5, 8): c.px(x, y, G)
        for (x, y) in ((9, 5), (8, 6), (9, 6), (10, 6), (9, 7)): c.px(x, y, (220, 30, 30))
        for x in range(4, 15): c.px(x, 9, (220, 40, 40))
        c.px(6, 2, (230, 40, 40)); c.px(7, 2, (40, 90, 240))
    elif roof == 'suv':
        for x in range(1, 15):
            for y in range(4, 12): c.px(x, y, l if y == 4 else d if y == 11 else b)
        for x in range(3, 12): c.px(x, 6, (60, 70, 80)) if x % 4 else None
        for x in (2, 6, 10, 13):
            c.px(x, 9, light(b, 0.5))
        c.rect = None
    elif roof == 'truck':
        for x in range(0, 16):
            for y in range(3, 12): c.px(x, y, l if y == 3 else d if y == 11 else b)
        for x in range(1, 4):
            for y in range(5, 7): c.px(x, y, (60, 70, 80))
        for x in range(6, 10): c.px(x, 1, (40, 40, 44)); c.px(9 + x - 6, 2, (40, 40, 44))
        c.px(13, 1, (40, 40, 44)); c.px(14, 1, (40, 40, 44))
        for x in (5, 9, 13): c.px(x, 8, light(b, 0.5))
    if roof == 'atv':
        for x in range(3, 13):
            for y in range(7, 10): c.px(x, y, l if y == 7 else b)
        c.px(5, 5, (60, 60, 64)); c.px(6, 6, (60, 60, 64)); c.px(10, 6, (40, 40, 40)); c.px(11, 6, (40, 40, 40))
        for (x, y) in ((2, 10), (3, 10), (4, 10), (2, 11), (3, 11), (4, 11), (2, 12), (3, 12), (4, 12), (11, 10), (12, 10), (13, 10), (11, 11), (12, 11), (13, 11), (11, 12), (12, 12), (13, 12)): c.px(x, y, K)
        c.px(3, 11, W); c.px(12, 11, W)
    elif roof == 'moto':
        for (x, y) in ((6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (7, 7), (8, 7), (9, 7), (11, 6), (12, 6), (11, 7)): c.px(x, y, b)
        c.px(5, 7, K); c.px(4, 7, K); c.px(12, 5, (200, 200, 200)); c.px(13, 5, (200, 200, 200))
        for cx in (3, 12):
            for (a, bb) in ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (-2, 0), (2, 0), (0, -2), (0, 2)): c.px(cx + a, 10 + bb, K)
            c.px(cx, 10, W)
        c.px(6, 9, (160, 160, 170)); c.px(7, 9, (160, 160, 170))
    else:
        for cx in (4, 12) if roof != 'truck' else (3, 8, 13):
            for (a, bb) in ((-1, 0), (0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (0, -1), (0, 2), (-1, -1), (1, -1), (-1, 2), (1, 2)): c.px(cx + a, 11 + bb, K)
            c.px(cx, 12, W)
    c.outline(0.3); c.save(f'{T}/item/{name}_kit.png')

car('car_sedan', (60, 110, 170)); car('car_pickup', (170, 60, 40), 'pickup'); car('car_police', (30, 30, 36), 'police'); car('car_ambulance', (236, 236, 236), 'van')
car('car_armored_suv', (50, 56, 50), 'suv');
def sport(name, body, stripe=None):
    c = C(); b = body; d = dark(b, 0.7); l = light(b, 0.3); G = (150, 200, 230); K = (26, 26, 30); W = (90, 90, 96)
    for x in range(1, 16):
        for y in range(9, 12): c.px(x, y, l if y == 9 else d if y == 11 else b)
    for x in range(5, 11): c.px(x, 8, b)
    for x in range(6, 10): c.px(x, 7, G)
    c.px(5, 7, b); c.px(10, 7, b); c.px(13, 7, K); c.px(14, 7, K); c.px(15, 7, K); c.px(14, 8, K)
    if stripe:
        for x in range(1, 16): c.px(x, 10, stripe)
    c.px(1, 10, (255, 240, 160)); c.px(15, 10, (220, 40, 40))
    for cx in (4, 12):
        for (a, bb) in ((-1, 0), (0, 0), (1, 0), (-1, 1), (0, 1), (1, 1), (0, -1), (0, 2), (-1, -1), (1, -1), (-1, 2), (1, 2)): c.px(cx + a, 12 + bb, K)
        c.px(cx, 13, W)
    c.outline(0.3); c.save(f'{T}/item/{name}_kit.png')
sport('sports_gt', (196, 22, 26)); sport('sports_rs', (240, 196, 20)); sport('sports_turbo', (30, 80, 190), (240, 240, 244))
car('taxi', (242, 196, 30), 'police'); car('muscle_car', (28, 28, 32)); car('jeep', (86, 98, 56), 'pickup'); car('van', (236, 236, 236), 'suv')
car('school_bus', (244, 186, 20), 'truck'); car('fire_truck', (196, 24, 24), 'truck')
car('superbike_vortex', (40, 190, 60), 'moto'); car('superbike_blaze', (210, 30, 30), 'moto')
car('atv', (200, 140, 30), 'atv'); car('motorcycle', (160, 30, 30), 'moto'); car('armored_truck', (80, 88, 64), 'truck')

# ------------------------------------------------------------- vehicle workshop block
r = random.Random(3)
top = Image.new('RGBA', (16, 16)); side = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        v = r.randint(-8, 8)
        edge = x in (0, 15) or y in (0, 15)
        top.putpixel((x, y), ((70 + v, 74 + v, 82 + v) if not edge else (40, 42, 48)) + (255,))
        stripe = (x + y) % 6 < 3
        side.putpixel((x, y), ((60 + v, 62 + v, 68 + v) if y > 3 else ((220, 180, 40) if stripe else (30, 30, 30))) + (255,))
for (x, y) in ((4, 4), (5, 4), (6, 5), (7, 6), (8, 7), (9, 8), (10, 9), (11, 10), (11, 11), (10, 11)):   # wrench
    top.putpixel((x, y), (190, 196, 206, 255))
for (x, y) in ((3, 3), (3, 4), (4, 3), (12, 11), (12, 12), (11, 12)): top.putpixel((x, y), (190, 196, 206, 255))
for cx in (5, 11):   # tyre on the side
    for y in range(6, 15):
        for x in range(16):
            if (x - cx) ** 2 + (y - 10) ** 2 <= 7: side.putpixel((x, y), (28, 28, 30, 255))
            if (x - cx) ** 2 + (y - 10) ** 2 <= 1: side.putpixel((x, y), (150, 150, 156, 255))
top.save(f'{T}/block/vehicle_workshop_top.png'); side.save(f'{T}/block/vehicle_workshop_side.png')
print('eggs & kits done')

# ------------------------------------------------------------- throwables
def grenade():
    c = C(); c.blob(7.5, 10, 4.6, 5, (80, 96, 56))
    for y in range(6, 15):
        for x in range(3, 13):
            p = c.get(x, y)
            if p[3] and (x % 3 == 0 or y % 3 == 0): c.px(x, y, dark(p[:3], 0.75))
    for x in range(6, 10): c.px(x, 4, (110, 112, 118)); c.px(x, 5, (90, 92, 98))
    c.px(10, 4, (150, 152, 160)); c.px(11, 5, (150, 152, 160)); c.px(12, 6, (150, 152, 160)); c.px(12, 3, (190, 190, 196)); c.px(13, 2, (190, 190, 196))
    c.outline(); c.save(f'{T}/item/frag_grenade.png')
def molotov():
    c = C()
    for y in range(7, 15):
        for x in range(4, 11): c.px(x, y, (60, 120, 60) if x > 4 else (110, 180, 110))
    for y in range(10, 15):
        for x in range(5, 10): c.px(x, y, (200, 120, 40))
    for y in range(3, 7): c.px(6, y, (70, 130, 70)); c.px(7, y, (70, 130, 70)); c.px(8, y, (50, 100, 50))
    for (x, y) in ((7, 2), (8, 1), (6, 1), (7, 0), (9, 2)): c.px(x, y, (255, 160, 40))
    c.px(7, 1, (255, 230, 120)); c.px(5, 3, (220, 210, 190)); c.px(5, 4, (220, 210, 190))
    c.outline(); c.save(f'{T}/item/molotov.png')
def smoke():
    c = C()
    for y in range(5, 15):
        for x in range(5, 11): c.px(x, y, (150, 156, 164) if x < 9 else (110, 116, 124))
    for x in range(5, 11): c.px(x, 8, (60, 60, 64)); c.px(x, 9, (230, 230, 230))
    for x in range(6, 10): c.px(x, 4, (80, 82, 88))
    for (x, y) in ((10, 3), (11, 2), (12, 2), (12, 1), (13, 0)): c.px(x, y, (200, 200, 206))
    c.outline(); c.save(f'{T}/item/smoke_grenade.png')
def flash():
    c = C()
    for y in range(5, 15):
        for x in range(5, 11): c.px(x, y, (40, 44, 50) if x < 9 else (28, 30, 36))
    for y in (7, 10, 13):
        for x in range(5, 11): c.px(x, y, (230, 200, 60))
    for x in range(6, 10): c.px(x, 4, (110, 112, 118))
    c.px(11, 4, (170, 170, 176)); c.px(12, 5, (170, 170, 176)); c.px(12, 3, (255, 255, 200)); c.px(13, 2, (255, 255, 230))
    c.outline(); c.save(f'{T}/item/flashbang.png')
grenade(); molotov(); smoke(); flash()
print('throwables done')

# ------------------------------------------------------------- dollars + money bag
FONT = {'1': ["010", "110", "010", "010", "111"], '5': ["111", "100", "110", "001", "110"], '0': ["111", "101", "101", "101", "111"], '2': ["110", "001", "010", "100", "111"]}
BAND = {1: (120, 160, 100), 5: (170, 120, 200), 10: (220, 160, 60), 20: (90, 170, 120), 50: (220, 90, 80), 100: (80, 150, 210)}
for v, band in BAND.items():
    c = C()
    for y in range(4, 12):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (4, 11)
            c.px(x, y, (70, 120, 70) if edge else (150, 196, 140) if (x + y) % 5 else (136, 182, 128))
    for y in range(5, 11): c.px(3, y, band); c.px(12, y, band)
    for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9)): c.px(x, y, (110, 150, 100))   # portrait
    c.px(7, 6, (90, 130, 84)); c.px(8, 7, (90, 130, 84))
    s = str(v)
    x0 = 15 - 4 * len(s)
    for i, ch in enumerate(s):
        for j, row in enumerate(FONT[ch]):
            for k, b in enumerate(row):
                if b == '1': c.px(x0 + i * 4 + k, 10 + j, (30, 70, 30))
    # second bill peeking behind
    for x in range(2, 15): c.px(x, 3, (110, 160, 104))
    c.outline(0.4); c.save(f'{T}/item/dollar_{v}.png')
c = C(); c.blob(8, 10, 6, 5.4, (150, 110, 60))
for x in range(5, 11): c.px(x, 4, (120, 86, 46)); c.px(x, 5, (170, 130, 76))
for (x, y) in ((6, 3), (9, 3), (7, 2), (8, 2)): c.px(x, y, (120, 86, 46))
for x in range(5, 11): c.px(x, 6, (200, 170, 80))
for (x, y) in ((8, 8), (7, 8), (6, 9), (7, 10), (8, 10), (9, 11), (8, 12), (7, 12), (6, 12), (7, 7), (7, 13)): c.px(x, y, (60, 150, 60))
c.outline(); c.save(f'{T}/item/money_bag.png')
print('money done')
