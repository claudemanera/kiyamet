"""TWD 2.0 elements: 14 ancient weapons (32x32 pixel art -> thick 3D voxel hand models, glowing 'power on' variants),
14 element cores, the ancient rod, the sage map; GeckoLib models for the 14 guardians, 14 sages and 14 element lords;
spawn eggs, lang, and the sage maps in city loot."""
import json, math, os, random, sys, glob
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from make_animals import Beast, dark, light, mix
import make_3d_items as V
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')
D = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/data/kiyamet')

EL = [('flame', 'Flame', 'flame_sword', 'Flame Sword', (255, 90, 30)), ('lightning', 'Lightning', 'lightning_mace', 'Lightning Mace', (255, 226, 74)),
      ('earth', 'Earth', 'earth_axe', 'Earth Axe', (123, 162, 63)), ('shadow', 'Shadow', 'shadow_katana', 'Shadow Katana', (107, 47, 181)),
      ('smoke', 'Smoke', 'smoke_sai', 'Smoke Sai', (154, 154, 154)), ('spirit', 'Spirit', 'spirit_spear', 'Spirit Spear', (127, 240, 232)),
      ('electric', 'Electric', 'electric_scythe', 'Electric Scythe', (53, 200, 255)), ('thunder', 'Thunder', 'thunder_hammer', 'Thunder Hammer', (77, 107, 255)),
      ('ice', 'Ice', 'ice_nunchaku', 'Ice Nunchaku', (168, 232, 255)), ('fire', 'Fire', 'fire_club', 'Fire Club', (255, 154, 26)),
      ('water', 'Water', 'water_shuriken', 'Water Shuriken', (46, 123, 255)), ('air', 'Air', 'air_kunai', 'Air Kunai', (232, 246, 255)),
      ('wind', 'Wind', 'wind_triple_scythe', 'Wind Triple Scythe', (142, 240, 160)), ('metal', 'Metal', 'metal_claws', 'Metal Claws', (184, 194, 204))]
POWER = {'flame': 'Flamethrower', 'lightning': 'Lightning strikes where you look', 'earth': 'Shatters a 6x6 area', 'shadow': 'Invisibility', 'smoke': 'Choking smoke cloud',
         'spirit': 'Flight at 4x speed', 'electric': 'Exploding lightning', 'thunder': 'Super speed, time at half speed', 'ice': 'Freezes time', 'fire': 'Fireballs',
         'water': 'Super jumps, full speed in water', 'air': 'Rise into the sky and fly', 'wind': 'Time runs twice as fast', 'metal': 'You take no damage'}
T = 32
AX, AY, BX, BY = 3.5, 28.5, 28.5, 3.5          # handle end -> tip (diagonal)
LEN = math.hypot(BX - AX, BY - AY)
UX, UY = (BX - AX) / LEN, (BY - AY) / LEN
VX, VY = -UY, UX                                # perpendicular (towards top-left)


def uv(x, y):
    px, py = x + 0.5 - AX, y + 0.5 - AY
    return px * UX + py * UY, px * VX + py * VY


def shade(c, k): return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def draw(shape):
    """shape(u, v, x, y) -> rgb or None"""
    im = Image.new('RGBA', (T, T), (0, 0, 0, 0))
    for y in range(T):
        for x in range(T):
            u, v = uv(x, y)
            c = shape(u, v, x, y)
            if c: im.putpixel((x, y), tuple(int(k) for k in c[:3]) + (255,))
    return im


def outline(im, col):
    out = im.copy()
    for y in range(T):
        for x in range(T):
            if im.getpixel((x, y))[3]: continue
            nb = [im.getpixel((x + dx, y + dy)) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < T and 0 <= y + dy < T and im.getpixel((x + dx, y + dy))[3]]
            if nb:
                if col is None: out.putpixel((x, y), tuple(int(k * 0.42) for k in nb[0][:3]) + (255,))
                else: out.putpixel((x, y), col)
    return out


GOLD, GOLD_D, WOOD, WOOD_D, STEEL, STEEL_D, LEATHER = (232, 186, 60), (160, 116, 30), (128, 84, 46), (86, 54, 28), (196, 204, 212), (110, 118, 128), (70, 40, 28)


def grip(u, v, a, b, w, c1, c2):
    if a <= u <= b and abs(v) <= w: return c1 if int(u * 1.4 + v) % 2 == 0 else c2


def flame_sword(c):
    def f(u, v, x, y):
        if u < 1.8 and abs(v) <= 1.6: return GOLD
        g = grip(u, v, 1.8, 7.5, 1.05, LEATHER, (110, 60, 40))
        if g: return g
        if 7.5 <= u <= 9.2 and abs(v) <= 4.6 - abs(u - 8.3): return GOLD if abs(v) < 3.5 else GOLD_D
        if 9.2 <= u <= 31:
            t = (u - 9.2) / 21.8
            wdt = 2.4 * (1 - t ** 3) + 0.4 + (0.9 * max(0, math.sin(u * 1.25)) if v > 0 else 0)
            if abs(v) <= wdt:
                core = abs(v) < 0.8
                base = mix((190, 30, 10), (255, 170, 40), t)
                return (255, 240, 170) if core and t > 0.2 else mix(base, (255, 220, 120), 0.4) if v > wdt - 0.9 else base
    return f


def lightning_mace(c):
    def f(u, v, x, y):
        if u < 1.5 and abs(v) <= 1.5: return GOLD
        if 1.5 <= u <= 17.5 and abs(v) <= 1.0: return GOLD if int(u) % 5 == 0 else (60, 50, 70)
        d = math.hypot(u - 23, v)
        if d <= 4.6: return (255, 255, 230) if abs(v - (math.sin(u * 2) * 1.5)) < 0.6 else mix((240, 190, 40), (255, 240, 120), (4.6 - d) / 4.6)
        for su in (19.5, 23, 26.5):
            for sv in (-1, 1):
                if abs(u - su) <= 0.8 and 4.3 <= v * sv <= 6.4: return (250, 250, 250)
        if 27.5 <= u <= 30 and abs(v) <= 0.8: return (250, 250, 250)
    return f


def earth_axe(c):
    def f(u, v, x, y):
        if 0 <= u <= 29 and abs(v) <= 1.0: return WOOD if int(u * 1.7) % 3 else WOOD_D
        if 17 <= u <= 28 and 0.5 <= v <= 9 - abs(u - 22.5) * 0.55:
            edge = v > 7.6 - abs(u - 22.5) * 0.55
            moss = (x * 7 + y * 3) % 5 == 0
            return (110, 230, 120) if edge else (90, 150, 60) if moss else (128, 124, 116) if (x + y) % 3 else (104, 100, 94)
        if 19 <= u <= 25 and -3.2 <= v <= -0.5: return (100, 96, 90)
        if 28 <= u <= 30 and abs(v) <= 1.3: return (90, 150, 60)
    return f


def shadow_katana(c):
    def f(u, v, x, y):
        if u < 1.2 and abs(v) <= 1.3: return (40, 30, 50)
        g = grip(u, v, 1.2, 8.5, 1.0, (120, 60, 180), (30, 20, 40))
        if g: return g
        if 8.5 <= u <= 9.8 and abs(v) <= 2.6: return (20, 16, 24)
        if 9.8 <= u <= 31:
            cv = 0.010 * (u - 9.8) ** 2
            if -0.4 <= v - cv <= 1.4 * (1 - ((u - 9.8) / 21.2) ** 4) + 0.2:
                return (220, 170, 255) if v - cv > 0.75 else (90, 40, 140) if v - cv > 0 else (30, 20, 40)
    return f


def smoke_sai(c):
    def f(u, v, x, y):
        if u < 1.4 and abs(v) <= 1.4: return STEEL_D
        g = grip(u, v, 1.4, 8, 1.0, (90, 90, 96), (50, 50, 56))
        if g: return g
        if 8 <= u <= 9.2 and abs(v) <= 4: return STEEL
        for sv in (-1, 1):
            if 9.2 <= u <= 14 and abs(v * sv - (4 - (u - 9.2) * 0.25)) <= 0.7: return STEEL
        if 9.2 <= u <= 27 and abs(v) <= 0.9 * (1 - ((u - 9.2) / 18) ** 3) + 0.2: return mix((230, 230, 236), (120, 120, 128), (u - 9.2) / 18) if v > 0 else (150, 150, 156)
    return f


def spirit_spear(c):
    def f(u, v, x, y):
        if 0 <= u <= 23 and abs(v) <= 0.85: return (226, 214, 180) if int(u) % 6 else (127, 240, 232)
        if 21 <= u <= 22.5 and -2.5 <= v <= -0.8: return (120, 220, 210)
        if 23 <= u <= 31:
            w = 2.8 * math.sin(math.pi * (u - 23) / 8)
            if abs(v) <= w + 0.3: return (230, 255, 252) if abs(v) < 0.6 else mix((60, 200, 200), (160, 255, 245), (u - 23) / 8)
    return f


def electric_scythe(c):
    def f(u, v, x, y):
        if 0 <= u <= 29 and abs(v) <= 0.85: return (200, 110, 50) if int(u) % 5 == 0 else (50, 56, 70)
        if 25 <= u <= 31 and -1 <= v <= 1.2: return (50, 56, 70)
        for vv in range(1, 13):
            pass
        if 1 <= v <= 12.5:
            top = 29.5 - (v ** 1.35) * 0.55
            if top - 2.6 + v * 0.05 <= u <= top:
                edge = u < top - 1.8 + v * 0.05
                return (240, 255, 255) if edge else mix((40, 150, 230), (110, 230, 255), v / 12)
    return f


def thunder_hammer(c):
    def f(u, v, x, y):
        if u < 1.5 and abs(v) <= 1.5: return GOLD
        g = grip(u, v, 1.5, 19.5, 1.0, (60, 60, 90), (40, 40, 60))
        if g: return g
        if 19.5 <= u <= 29 and abs(v) <= 6:
            rim = abs(v) > 5 or u < 20.5 or u > 28
            bolt = abs(v - (u - 24) * 1.2) < 0.7 and abs(u - 24.2) < 3.5
            return GOLD if rim else (255, 240, 120) if bolt else (70, 100, 230) if (x + y) % 4 else (90, 120, 250)
    return f


def ice_nunchaku(c):
    segs = [((4, 27), (12, 19), 1.4, 'stick'), ((12.5, 18.5), (15.5, 12), 0.6, 'chain'), ((15.5, 12), (27, 5), 1.4, 'stick')]

    def f(u, v, x, y):
        for (x0, y0), (x1, y1), w, kind in segs:
            dx, dy = x1 - x0, y1 - y0; L = math.hypot(dx, dy)
            t = ((x + 0.5 - x0) * dx + (y + 0.5 - y0) * dy) / (L * L)
            if 0 <= t <= 1:
                d = abs((x + 0.5 - x0) * dy - (y + 0.5 - y0) * dx) / L
                if d <= w:
                    if kind == 'chain': return (200, 210, 220) if (x + y) % 2 else (120, 130, 140)
                    band = t < 0.1 or t > 0.9
                    return (90, 110, 130) if band else mix((150, 220, 255), (240, 250, 255), 0.5 + 0.5 * math.sin(t * 9)) if d < w - 0.5 else (110, 190, 240)
    return f


def fire_club(c):
    def f(u, v, x, y):
        w = 0.9 + (u / 26) * 2.4
        if 0 <= u <= 26 and abs(v) <= w:
            band = int(u) in (8, 9, 16, 17, 23)
            return (230, 120, 30) if band else WOOD if (int(u * 2) + int(v * 2)) % 3 else WOOD_D
        if 25 <= u <= 31 and abs(v) <= 3.2 - (u - 25) * 0.45 + 0.8 * math.sin(u * 3 + v):
            return (255, 230, 120) if abs(v) < 1 else (255, 120, 20)
    return f


def water_shuriken(c):
    def f(u, v, x, y):
        cx, cy = x + 0.5 - 16, y + 0.5 - 16
        r, a = math.hypot(cx, cy), math.atan2(cy, cx)
        if r < 2.2: return None
        k = abs(math.cos(2 * a)) ** 6
        if r <= 3.5 + 10.5 * k: return (230, 245, 255) if r > 3.5 + 10.5 * k - 1.2 else mix((20, 80, 200), (90, 170, 255), r / 14)
        if r < 4.6: return (30, 70, 160)
    return f


def air_kunai(c):
    def f(u, v, x, y):
        if math.hypot(u - 1.2, v) <= 2.4 and math.hypot(u - 1.2, v) >= 1.1: return (230, 240, 250)
        g = grip(u, v, 3.4, 12, 1.0, (240, 245, 250), (150, 200, 230))
        if g: return g
        if 12 <= u <= 13 and abs(v) <= 1.8: return STEEL_D
        if 13 <= u <= 30 and abs(v) <= min((u - 13) * 0.75 + 0.6, (30 - u) * 0.3 + 0.2, 3.2):
            return (255, 255, 255) if abs(v) < 0.5 else (190, 225, 245) if v > 0 else (150, 190, 220)
    return f


def wind_triple_scythe(c):
    def f(u, v, x, y):
        if 0 <= u <= 25 and abs(v) <= 0.85: return (230, 240, 230) if int(u) % 4 else (100, 200, 120)
        if 23 <= u <= 25.5 and abs(v) <= 3.8: return (90, 170, 110)
        if 25 <= u <= 31 and abs(v) <= 0.7 * (1 - (u - 25) / 7) + 0.3: return (220, 255, 225)
        for s in (-1, 1):
            vv = v * s
            if 24 <= u <= 31 and abs(vv - (3.2 + (u - 24) ** 1.4 * 0.35)) <= 0.8 * (1 - (u - 24) / 8) + 0.25:
                return (240, 255, 245) if u > 28 else (140, 230, 160)
    return f


def metal_claws(c):
    def f(u, v, x, y):
        if 0 <= u <= 9.5 and abs(v) <= 3.4:
            return STEEL_D if abs(v) > 2.8 or u < 1 else (150, 158, 168) if (int(u) + int(v + 4)) % 2 else (130, 138, 148)
        for k in (-2.6, 0, 2.6):
            L = 30 - abs(k) * 0.8
            if 9.5 <= u <= L and abs(v - k - (u - 9.5) * 0.03 * k) <= 0.75 * (1 - ((u - 9.5) / (L - 9.5)) ** 2) + 0.2:
                return (245, 248, 252) if v - k > 0 else (170, 178, 188)
    return f


SHAPES = dict(flame_sword=flame_sword, lightning_mace=lightning_mace, earth_axe=earth_axe, shadow_katana=shadow_katana, smoke_sai=smoke_sai, spirit_spear=spirit_spear,
              electric_scythe=electric_scythe, thunder_hammer=thunder_hammer, ice_nunchaku=ice_nunchaku, fire_club=fire_club, water_shuriken=water_shuriken,
              air_kunai=air_kunai, wind_triple_scythe=wind_triple_scythe, metal_claws=metal_claws)
BIG = {'earth_axe', 'thunder_hammer', 'electric_scythe', 'wind_triple_scythe', 'spirit_spear', 'lightning_mace', 'fire_club'}
L = json.load(open(A + '/lang/en_us.json'))


def scaled(disp, k):
    d = json.loads(json.dumps(disp))
    for key in ('firstperson_righthand', 'firstperson_lefthand', 'thirdperson_righthand', 'thirdperson_lefthand'):
        d[key]['scale'] = [s * k for s in d[key]['scale']]
    return d


for eid, title, wid, wtitle, col in EL:
    im = draw(SHAPES[wid](col))
    im = outline(im, None)
    im.save(f'{A}/textures/item/{wid}.png')
    # power on: brighter, with a glowing halo in the element colour
    on = Image.new('RGBA', (T, T))
    for y in range(T):
        for x in range(T):
            p = im.getpixel((x, y))
            if p[3]: on.putpixel((x, y), tuple(min(255, int(p[k] * 0.75 + col[k] * 0.45)) for k in range(3)) + (255,))
    on = outline(on, tuple(light(col, 0.35)) + (255,))
    on.save(f'{A}/textures/item/{wid}_on.png')
    k = 1.35 if wid in BIG else 1.1
    V.write(f'{A}/models/item/{wid}.json', f'kiyamet:item/{wid}', 7.0, 9.0, False, scaled(V.DISPLAY, k))
    V.write(f'{A}/models/item/{wid}_on.json', f'kiyamet:item/{wid}_on', 6.8, 9.2, False, scaled(V.DISPLAY, k))
    json.dump({"model": {"type": "minecraft:condition", "property": "minecraft:custom_model_data", "index": 0,
                         "on_true": {"type": "minecraft:model", "model": f"kiyamet:item/{wid}_on"}, "on_false": {"type": "minecraft:model", "model": f"kiyamet:item/{wid}"}}},
              open(f'{A}/items/{wid}.json', 'w'), indent=1)
    L[f'item.kiyamet.{wid}'] = wtitle
    L[f'tooltip.kiyamet.{wid}'] = f'Ancient weapon of {title}|Right-click: power on/off (no cooldown)|Power: {POWER[eid]}|Every hit carries the {title.lower()}'
    # the element core: a glowing orb
    c = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 6.6:
                sw = 0.5 + 0.5 * math.sin(math.atan2(y - 7.5, x - 7.5) * 3 + d * 0.9)
                cc = mix(dark(col, 0.5), light(col, 0.5), (1 - d / 6.6) * 0.7 + sw * 0.3)
                if d > 5.8: cc = dark(col, 0.45)
                if (x, y) in ((5, 4), (5, 5), (4, 5)): cc = (255, 255, 255)
                c.putpixel((x, y), tuple(cc) + (255,))
    c.save(f'{A}/textures/item/{eid}_core.png')
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{eid}_core"}}, open(f'{A}/models/item/{eid}_core.json', 'w'))
    json.dump({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{eid}_core"}}, open(f'{A}/items/{eid}_core.json', 'w'))
    L[f'item.kiyamet.{eid}_core'] = f'{title} Core'
    L[f'tooltip.kiyamet.{eid}_core'] = f'The heart of the {title} element|Hold it and right-click an anvil with an Ancient Rod|in your inventory to forge the {wtitle}'

# ancient rod + sage map
rod = Image.new('RGBA', (16, 16))
for i in range(13):
    x, y = 2 + i, 13 - i
    for dx, dy in ((0, 0), (1, 0), (0, 1)):
        rod.putpixel((min(15, x + dx), min(15, y + dy)), (150, 110, 60, 255) if i % 3 else (220, 190, 90, 255))
rod.putpixel((14, 1), (255, 240, 160, 255)); rod.putpixel((13, 1), (255, 220, 120, 255)); rod.putpixel((14, 2), (255, 220, 120, 255))
rod.save(f'{A}/textures/item/ancient_rod.png')
mp = Image.new('RGBA', (16, 16))
for y in range(2, 14):
    for x in range(2, 14):
        if (x + y) % 9 == 0 and x > 9: continue
        mp.putpixel((x, y), (214, 190, 140, 255) if (x * 3 + y) % 7 else (190, 160, 110, 255))
for (x, y) in ((5, 5), (6, 6), (7, 7), (8, 6), (9, 5)): mp.putpixel((x, y), (120, 80, 50, 255))
for (x, y) in ((10, 9), (11, 10), (10, 11), (9, 10), (12, 11), (11, 12), (12, 9)): mp.putpixel((x, y), (200, 40, 40, 255))
mp.save(f'{A}/textures/item/sage_map.png')
for k in ('ancient_rod', 'sage_map'):
    json.dump({"parent": "minecraft:item/handheld" if k == 'ancient_rod' else "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{k}"}}, open(f'{A}/models/item/{k}.json', 'w'))
    json.dump({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{k}"}}, open(f'{A}/items/{k}.json', 'w'))
L.update({'item.kiyamet.ancient_rod': 'Ancient Rod', 'item.kiyamet.sage_map': 'Torn City Map',
          'tooltip.kiyamet.ancient_rod': 'Dropped by the element lords|Join it with an element core on an anvil',
          'tooltip.kiyamet.sage_map': 'Found in cities. Right-click to unfold:|it shows where one of the 14 sages lives'})


# ---------------------------------------------------------------- GeckoLib humanoids
def humanoid(name, mats, kind, weapon, crest):
    b = Beast(name, 128, 128)
    b.bone('root')
    b.bone('body', 'root', pivot=(0, 24, 0))
    b.bone('head', 'body', pivot=(0, 24, 0))
    b.bone('arm_r', 'body', pivot=(-5, 22, 0)); b.bone('arm_l', 'body', pivot=(5, 22, 0))
    b.bone('leg_r', 'root', pivot=(-2, 12, 0)); b.bone('leg_l', 'root', pivot=(2, 12, 0))
    b.bone('weapon', 'arm_r', pivot=(-6, 11, -1))
    b.bone('cape', 'body', pivot=(0, 24, 2))
    sage = kind == 'sage'
    lord = kind == 'lord'
    # legs
    for n, x in (('leg_r', -4), ('leg_l', 0)):
        b.cube(n, (x, 0, -2), (4, 12, 4), 'cloth' if sage else 'armor2')
        if not sage: b.cube(n, (x - 0.2, 0, -2.4), (4.4, 2, 4.6), 'armor'); b.cube(n, (x - 0.1, 5, -2.3), (4.2, 3, 0.5), 'trim')
    # body
    b.cube('body', (-4, 12, -2), (8, 12, 4), 'cloth' if sage else 'armor')
    if sage:
        b.cube('body', (-4.5, 0.5, -2.5), (9, 12, 5), 'cloth')                  # robe skirt
        b.cube('body', (-4.6, 11, -2.6), (9.2, 1.4, 5.2), 'trim')               # belt
        b.cube('body', (-1, 12, -2.6), (2, 12, 0.4), 'trim')                     # robe band
    else:
        b.cube('body', (-4.4, 17, -2.6), (8.8, 7, 5.2), 'armor')                 # chest plate
        b.cube('body', (-1.5, 18, -2.9), (3, 4, 0.4), 'glow')                    # element sigil
        b.cube('body', (-4.6, 12, -2.5), (9.2, 1.6, 5), 'trim')                  # belt
        b.cube('cape', (-4, 3 if lord else 8, 2.1), (8, 21 if lord else 16, 0.6), 'cloth')
    # head
    if sage:
        b.cube('head', (-4, 24, -4), (8, 8, 8), 'skin')
        b.cube('head', (-4.5, 27, -4.5), (9, 6, 9), 'cloth', inflate=0.1)       # hood
        b.cube('head', (-3, 19, -4.4), (6, 6, 1), 'beard')                       # long beard
        b.cube('head', (-3.2, 28.5, -4.3), (2.4, 1, 0.3), 'eye'); b.cube('head', (0.8, 28.5, -4.3), (2.4, 1, 0.3), 'eye')
    else:
        b.cube('head', (-4, 24, -4), (8, 8, 8), 'skin')
        b.cube('head', (-4.6, 24, -4.6), (9.2, 8.6, 9.2), 'armor')               # helmet
        b.cube('head', (-3.4, 27.2, -4.9), (6.8, 1.2, 0.4), 'glow')              # visor slit / eyes
        b.cube('head', (-0.6, 24.3, -4.9), (1.2, 3, 0.4), 'trim')                # nose guard
        if crest == 'spikes':
            for i, x in enumerate((-3, -1, 1, 3)): b.cube('head', (x - 0.5, 32.6, -1), (1, 2 + (2 if x in (-1, 1) else 0) + (2 if lord else 0), 1), 'glow')
        elif crest == 'horns':
            for s in (-1, 1): b.cube('head', (s * 4.6 - (1 if s < 0 else 0), 30, -1), (1.2, 5 + (3 if lord else 0), 1.2), 'horn', rot=(0, 0, s * -30), pivot=(s * 4.6, 30, 0))
        elif crest == 'plume':
            b.cube('head', (-0.6, 32.6, -3), (1.2, 2.5, 7), 'cloth')
        elif crest == 'crystals':
            for x, h in ((-3, 3), (0, 5), (3, 3)): b.cube('head', (x - 0.6, 32.6, -0.6), (1.2, h + (2 if lord else 0), 1.2), 'glow', rot=(0, 45, 0), pivot=(x, 32.6, 0))
        elif crest == 'halo':
            b.cube('head', (-4, 34.5, -4), (8, 0.6, 8), 'glow')
        elif crest == 'fins':
            b.cube('head', (-0.4, 32.6, -4), (0.8, 3, 8), 'glow')
        if lord:
            for x in (-3.5, -1.2, 1.2, 3.5): b.cube('head', (x - 0.5, 32.4, -4.6), (1, 2.5, 1), 'trim')   # crown
    # arms
    for n, x, s in (('arm_r', -8, -1), ('arm_l', 4, 1)):
        b.cube(n, (x, 12, -2), (4, 12, 4), 'cloth' if sage else 'armor2')
        if sage: b.cube(n, (x - 0.4, 11, -2.4), (4.8, 6, 4.8), 'cloth')          # wide sleeves
        else:
            b.cube(n, (x - 0.5, 19.5, -2.5), (5, 5, 5), 'armor')                 # pauldron
            if lord: b.cube(n, (x - 1 + (0 if s < 0 else 1), 23.5, -2.6), (5.2, 1.6, 5.2), 'trim')
            b.cube(n, (x - 0.3, 12, -2.3), (4.6, 4, 4.6), 'trim')                # gauntlet
    # the weapon in the right hand, pointing forward
    W = 'weapon'
    hx, hy = -6, 11
    if sage:
        b.cube(W, (hx - 0.6, 0, -1.6), (1.2, 26, 1.2), 'wood')                    # staff
        b.cube(W, (hx - 1.6, 26, -2.6), (3.2, 3.2, 3.2), 'glow')                  # orb
    else:
        g = {'flame_sword': ('blade', 18, 1, 2.6), 'lightning_mace': ('mace', 12, 4.5, 4.5), 'earth_axe': ('axe', 16, 1, 1), 'shadow_katana': ('blade', 20, 0.8, 1.6),
             'smoke_sai': ('sai', 12, 0.8, 0.8), 'spirit_spear': ('spear', 26, 0.8, 0.8), 'electric_scythe': ('scythe', 22, 0.9, 0.9), 'thunder_hammer': ('hammer', 14, 1, 1),
             'ice_nunchaku': ('nunchaku', 8, 1.2, 1.2), 'fire_club': ('club', 15, 2.2, 2.2), 'water_shuriken': ('star', 0, 0, 0), 'air_kunai': ('blade', 9, 0.6, 1.6),
             'wind_triple_scythe': ('trident', 22, 0.8, 0.8), 'metal_claws': ('claws', 9, 0.4, 0.4)}[weapon]
        kind_, Ln, w1, w2 = g
        b.cube(W, (hx - 0.5, hy - 1.5, -1.5), (1, 3, 3), 'trim')                  # grip in hand
        if kind_ == 'blade':
            b.cube(W, (hx - w1 / 2, hy - w2 / 2, -3 - 2), (w1, w2 + 1.4, 2), 'trim') # guard
            b.cube(W, (hx - 0.3, hy - w2 / 2 + 0.2, -3 - 2 - Ln), (0.6, w2, Ln), 'blade')
            b.cube(W, (hx - 0.15, hy - w2 / 2 + 0.4, -3 - 2 - Ln), (0.3, w2 * 0.4, Ln), 'glow')
        elif kind_ in ('spear', 'scythe', 'trident'):
            b.cube(W, (hx - 0.5, hy - 0.5, -2 - Ln), (1, 1, Ln + 6), 'wood')
            if kind_ == 'spear': b.cube(W, (hx - 0.4, hy - 1.5, -2 - Ln - 5), (0.8, 3, 5), 'glow')
            if kind_ == 'scythe': b.cube(W, (hx - 0.3, hy - 9, -2 - Ln - 1), (0.6, 9, 3), 'blade'); b.cube(W, (hx - 0.2, hy - 9, -2 - Ln - 1.4), (0.4, 9, 0.6), 'glow')
            if kind_ == 'trident':
                for dy in (-3, 0, 3): b.cube(W, (hx - 0.4, hy + dy - 0.4, -2 - Ln - 5), (0.8, 0.8, 5), 'glow')
                b.cube(W, (hx - 0.6, hy - 3.6, -2 - Ln), (1.2, 7.2, 1), 'blade')
        elif kind_ in ('mace', 'hammer', 'club'):
            b.cube(W, (hx - 0.5, hy - 0.5, -2 - Ln), (1, 1, Ln), 'wood')
            if kind_ == 'mace': b.cube(W, (hx - 2.2, hy - 2.2, -2 - Ln - 4.4), (4.4, 4.4, 4.4), 'glow'); b.cube(W, (hx - 0.4, hy - 3.6, -2 - Ln - 2.6), (0.8, 7.2, 0.8), 'blade')
            if kind_ == 'hammer': b.cube(W, (hx - 2.5, hy - 4, -2 - Ln - 4), (5, 8, 4), 'blade'); b.cube(W, (hx - 2.7, hy - 1, -2 - Ln - 2.6), (5.4, 2, 1.2), 'glow')
            if kind_ == 'club': b.cube(W, (hx - 1.4, hy - 1.4, -2 - Ln - 2), (2.8, 2.8, 8), 'wood'); b.cube(W, (hx - 1.6, hy - 1.6, -2 - Ln - 3), (3.2, 3.2, 2), 'glow')
        elif kind_ == 'axe':
            b.cube(W, (hx - 0.5, hy - 0.5, -2 - Ln), (1, 1, Ln), 'wood')
            b.cube(W, (hx - 0.4, hy - 6, -2 - Ln + 1), (0.8, 6, 5), 'blade'); b.cube(W, (hx - 0.3, hy - 6.6, -2 - Ln + 0.6), (0.6, 1.2, 5.8), 'glow')
        elif kind_ == 'sai':
            b.cube(W, (hx - 0.3, hy - 0.3, -2 - Ln), (0.6, 0.6, Ln), 'blade')
            for dy in (-1, 1): b.cube(W, (hx - 0.3, hy + dy * 1.8 - 0.3, -5), (0.6, 0.6, 3), 'blade')
        elif kind_ == 'nunchaku':
            b.cube(W, (hx - 0.6, hy - 0.6, -2 - Ln), (1.2, 1.2, Ln), 'glow')
            b.cube(W, (hx - 0.6, hy - 9, -2 - Ln - 0.6), (1.2, 8, 1.2), 'glow')
        elif kind_ == 'star':
            b.cube(W, (hx - 0.3, hy - 3, -6), (0.6, 6, 6), 'glow', rot=(45, 0, 0), pivot=(hx, hy, -3))
        elif kind_ == 'claws':
            for dy in (-1.2, 0, 1.2): b.cube(W, (hx - 0.2, hy + dy - 0.2, -2 - Ln), (0.4, 0.4, Ln), 'blade')
    anims = {
        "misc.idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"position": {"0.00": [0, 0, 0], "1.50": [0, -0.25, 0], "3.00": [0, 0, 0]}},
            "arm_r": {"rotation": {"0.00": [-10, 0, 0], "1.50": [-14, 0, 2], "3.00": [-10, 0, 0]}},
            "arm_l": {"rotation": {"0.00": [0, 0, 0], "1.50": [3, 0, -3], "3.00": [0, 0, 0]}},
            "head": {"rotation": {"0.00": [0, 0, 0], "1.00": [2, 8, 0], "2.00": [0, -8, 0], "3.00": [0, 0, 0]}},
            "cape": {"rotation": {"0.00": [4, 0, 0], "1.50": [8, 0, 0], "3.00": [4, 0, 0]}}}},
        "move.walk": {"loop": True, "animation_length": 1.0, "bones": {
            "leg_r": {"rotation": {"0.00": [30, 0, 0], "0.50": [-30, 0, 0], "1.00": [30, 0, 0]}},
            "leg_l": {"rotation": {"0.00": [-30, 0, 0], "0.50": [30, 0, 0], "1.00": [-30, 0, 0]}},
            "arm_r": {"rotation": {"0.00": [-25, 0, 0], "0.50": [5, 0, 0], "1.00": [-25, 0, 0]}},
            "arm_l": {"rotation": {"0.00": [25, 0, 0], "0.50": [-25, 0, 0], "1.00": [25, 0, 0]}},
            "body": {"position": {"0.00": [0, 0, 0], "0.25": [0, 0.5, 0], "0.50": [0, 0, 0], "0.75": [0, 0.5, 0], "1.00": [0, 0, 0]}},
            "cape": {"rotation": {"0.00": [18, 0, 0], "0.50": [26, 0, 0], "1.00": [18, 0, 0]}}}},
        "attack.swing": {"loop": False, "animation_length": 0.6, "bones": {
            "arm_r": {"rotation": {"0.00": [-10, 0, 0], "0.20": [-150, 10, 0], "0.40": [20, -10, 0], "0.60": [-10, 0, 0]}},
            "body": {"rotation": {"0.00": [0, 0, 0], "0.20": [-6, 15, 0], "0.40": [8, -20, 0], "0.60": [0, 0, 0]}},
            "weapon": {"rotation": {"0.00": [0, 0, 0], "0.20": [-20, 0, 0], "0.40": [30, 0, 0], "0.60": [0, 0, 0]}}}}}
    if sage:
        anims["misc.idle"]["bones"]["arm_r"] = {"rotation": {"0.00": [-30, 0, 0], "1.50": [-34, 0, 0], "3.00": [-30, 0, 0]}}
        anims["misc.idle"]["bones"]["weapon"] = {"rotation": {"0.00": [30, 0, 0]}}
    b.save(mats, anims, scale_bounds=4)


CREST = {'flame': 'spikes', 'lightning': 'fins', 'earth': 'horns', 'shadow': 'horns', 'smoke': 'plume', 'spirit': 'halo', 'electric': 'fins', 'thunder': 'spikes',
         'ice': 'crystals', 'fire': 'spikes', 'water': 'fins', 'air': 'halo', 'wind': 'plume', 'metal': 'crystals'}
os.makedirs(f'{A}/geckolib/models/entity', exist_ok=True)
for eid, title, wid, wtitle, col in EL:
    armor = mix(col, (90, 96, 104), 0.55) if eid not in ('shadow',) else (40, 34, 52)
    base = dict(skin=((196, 150, 120), 'skin'), eye=((20, 20, 20), 'eye'), beard=((236, 236, 236), 'fur'), wood=((110, 76, 44), 'fur'), horn=((60, 50, 44), 'horn'),
                blade=(light(col, 0.55) if eid != 'shadow' else (60, 40, 90), 'metal'), glow=(light(col, 0.25), 'glow'))
    g = dict(base, armor=(armor, 'metal'), armor2=(dark(armor, 0.8), 'metal'), trim=((200, 170, 80), 'metal'), cloth=(dark(col, 0.65), 'cloth'))
    humanoid(f'{eid}_guardian', g, 'guardian', wid, CREST[eid])
    lord = dict(base, armor=(dark(col, 0.55), 'metal'), armor2=(dark(col, 0.4), 'metal'), trim=((236, 196, 70), 'metal'), cloth=(dark(col, 0.35), 'cloth'), glow=(light(col, 0.4), 'glow'))
    humanoid(f'{eid}_lord', lord, 'lord', wid, CREST[eid])
    sage = dict(base, cloth=(mix(col, (110, 100, 90), 0.45), 'cloth'), trim=(light(col, 0.3), 'cloth'), skin=((210, 170, 140), 'skin'))
    humanoid(f'{eid}_sage', sage, 'sage', wid, None)
    L[f'entity.kiyamet.{eid}_guardian'] = f'{title} Guardian'
    L[f'entity.kiyamet.{eid}_lord'] = f'{title} Lord'
    L[f'entity.kiyamet.{eid}_sage'] = f'{title} Sage'
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)

# sage maps in city loot: special rooms always, ordinary city chests sometimes
for f in glob.glob(f'{D}/loot_table/chests/city_*.json'):
    d = json.load(open(f))
    if any('sage_map' in json.dumps(pl) for pl in d.get('pools', [])): continue
    special = os.path.basename(f).count('_') >= 2 and not os.path.basename(f).startswith('city_dead_metropolis.json')
    d.setdefault('pools', []).append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": "kiyamet:sage_map"}],
                                      **({} if special else {"conditions": [{"condition": "minecraft:random_chance", "chance": 0.05}]})})
    json.dump(d, open(f, 'w'), indent=1)
print('elements done')

# spawn eggs: guardians and sages (lords get theirs from draw_eggs via BossKind)
from draw_eggs import egg as _egg
for eid, title, wid, wtitle, col in EL:
    _egg(f'{eid}_guardian', mix(col, (90, 96, 104), 0.5), (200, 170, 80), 'enemy')
    _egg(f'{eid}_sage', mix(col, (110, 100, 90), 0.45), (236, 236, 236), 'friend')
L = json.load(open(A + '/lang/en_us.json'))
for f in glob.glob(f'{A}/textures/item/*_spawn_egg.png'):
    n = os.path.basename(f)[:-4]
    if not os.path.exists(f'{A}/items/{n}.json'):
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{n}"}}, open(f'{A}/models/item/{n}.json', 'w'))
        json.dump({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{n}"}}, open(f'{A}/items/{n}.json', 'w'))
    ent = n[:-len('_spawn_egg')]
    if f'item.kiyamet.{n}' not in L and f'entity.kiyamet.{ent}' in L: L[f'item.kiyamet.{n}'] = L[f'entity.kiyamet.{ent}'] + ' Spawn Egg'
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('eggs done')
