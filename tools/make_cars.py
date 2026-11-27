# NOTE (2.1): the vehicles made here are superseded by make_vehicles2.py - run that one after this file (this one still makes the mission board).
"""TWD 1.7 vehicles: detailed GeckoLib models + painted box-UV textures + animations for 9 cars and 2 superbikes."""
import json, os, random
from PIL import Image
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')

class Car:
    def __init__(s, name, tw=512, th=512):
        s.name, s.tw, s.th = name, tw, th; s.bones = {}; s.order = []; s.uvs = []; s.u = s.v = s.rowh = 0
        s.bone('body', pivot=(0, 4, 0))
    def bone(s, n, parent=None, pivot=(0, 0, 0)):
        b = {"name": n, "pivot": list(pivot), "cubes": []}
        if parent: b["parent"] = parent
        s.bones[n] = b; s.order.append(n); return b
    def cube(s, o, sz, mat, bone='body', inflate=0):
        w, h, d = [max(0.2, v) for v in sz]
        uw, uh = int(2 * (w + d) + 0.999) + 1, int(h + d + 0.999) + 1
        if s.u + uw > s.tw: s.u = 0; s.v += s.rowh; s.rowh = 0
        uv = [s.u, s.v]; s.u += uw; s.rowh = max(s.rowh, uh)
        c = {"origin": [round(x, 2) for x in o], "size": [round(x, 2) for x in (w, h, d)], "uv": uv}
        if inflate: c["inflate"] = inflate
        s.bones[bone]["cubes"].append(c); s.uvs.append((uv, (w, h, d), mat, (uw, uh)))
    def save(s, mats, wheels):
        assert s.v + s.rowh <= s.th, (s.name, s.v + s.rowh)
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": f"geometry.kiyamet.{s.name}", "texture_width": s.tw, "texture_height": s.th,
               "visible_bounds_width": 8, "visible_bounds_height": 5, "visible_bounds_offset": [0, 1.5, 0]}, "bones": [s.bones[n] for n in s.order]}]}
        json.dump(geo, open(f'{A}/geckolib/models/entity/{s.name}.geo.json', 'w'), indent=1)
        im = Image.new('RGBA', (s.tw, s.th), (0, 0, 0, 0)); r = random.Random(s.name)
        import math
        for (u, v), (w, h, d), mat, (uw, uh) in s.uvs:
            base, kind = mats[mat]
            for yy in range(v, min(s.th, v + uh)):          # whole box first: no transparent gaps when sizes are fractional
                for xx in range(u, min(s.tw, u + uw)):
                    if kind != 'ladder': im.putpixel((xx, yy), tuple(max(0, min(255, k + r.randint(-3, 3))) for k in base) + (255,))
            w, h, d = max(1, math.ceil(w)), max(1, math.ceil(h)), max(1, math.ceil(d))
            def fill(x0, y0, ww, hh, face):
                for y in range(y0, y0 + hh):
                    for x in range(x0, x0 + ww):
                        if not (0 <= x < s.tw and 0 <= y < s.th): continue
                        c = list(base); n = r.randint(-4, 4)
                        if kind == 'paint':
                            if face == 'top': n += 14
                            elif face == 'bottom': n -= 30
                            elif y == y0: n += 18
                            elif y == y0 + hh - 1: n -= 14
                        elif kind == 'glass':
                            c = [int(k + (y - y0) * 4) for k in base]
                            if (x - x0) == (y - y0) + 1 or (x - x0) == (y - y0) + 2: n += 40
                        elif kind == 'tire':
                            if (x + y) % 2 == 0: n -= 6
                        elif kind == 'rim':
                            cx, cy = x0 + ww / 2 - 0.5, y0 + hh / 2 - 0.5
                            dd = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                            if face in ('side',) and dd > min(ww, hh) / 2 - 0.6: c = [30, 30, 34]
                            elif dd < 1.2: c = [90, 90, 96]
                            elif int(dd * 2 + (x - cx) * 0.7) % 3 == 0: n -= 30
                        elif kind == 'light': n = r.randint(-2, 2) + (20 if y == y0 else 0)
                        elif kind == 'checker': c = list(base) if ((x - x0) // 2 + (y - y0) // 2) % 2 == 0 else [20, 20, 20]
                        elif kind == 'grille': c = [40, 40, 44] if (y - y0) % 2 == 0 else [14, 14, 16]
                        elif kind == 'ladder': c = list(base) if (x - x0) % 3 == 0 or y in (y0, y0 + hh - 1) else [0, 0, 0, 0][:3]
                        im.putpixel((x, y), tuple(max(0, min(255, k + n)) for k in c) + ((0,) if kind == 'ladder' and c == [0, 0, 0] else (255,)))
            fill(u + d, v, w, d, 'top'); fill(u + d + w, v, w, d, 'bottom')
            fill(u, v + d, d, h, 'side'); fill(u + d, v + d, w, h, 'front'); fill(u + d + w, v + d, d, h, 'side'); fill(u + 2 * d + w, v + d, w, h, 'back')
        im.save(f'{A}/textures/entity/{s.name}.png')
        rot = {"0.00": [0, 0, 0], "0.50": [-180, 0, 0], "1.00": [-360, 0, 0]}
        anim = {"format_version": "1.8.0", "animations": {
            "misc.idle": {"loop": True, "animation_length": 3.0, "bones": {"body": {"rotation": {"0.00": [0, 0, 0], "1.50": [-0.6, 0, 0], "3.00": [0, 0, 0]}}}},
            "move.walk": {"loop": True, "animation_length": 1.0, "bones": {"body": {"position": {"0.00": [0, 0, 0], "0.50": [0, 0.3, 0], "1.00": [0, 0, 0]}}}},
            "attack.swing": {"loop": False, "animation_length": 0.5, "bones": {}},
            "move.drive": {"loop": True, "animation_length": 0.6, "bones": dict({wb: {"rotation": rot} for wb in wheels}, body={"position": {"0.00": [0, 0, 0], "0.30": [0, 0.15, 0], "0.60": [0, 0, 0]}})}}}
        json.dump(anim, open(f'{A}/geckolib/animations/entity/{s.name}.animation.json', 'w'), indent=1)

def mats(color, accent):
    return {'paint': (color, 'paint'), 'accent': (accent, 'paint'), 'glass': ((70, 110, 140), 'glass'), 'black': ((26, 26, 30), 'paint'), 'tire': ((22, 22, 24), 'tire'),
            'rim': ((170, 176, 186), 'rim'), 'chrome': ((196, 202, 212), 'paint'), 'head': ((255, 244, 190), 'light'), 'tail': ((210, 30, 30), 'light'), 'orange': ((240, 140, 30), 'light'),
            'grille': ((30, 30, 30), 'grille'), 'checker': ((240, 196, 40), 'checker'), 'seat': ((60, 44, 36), 'paint'), 'interior': ((40, 40, 44), 'paint'),
            'redlight': ((230, 30, 30), 'light'), 'bluelight': ((40, 90, 240), 'light'), 'white': ((236, 236, 236), 'paint'), 'ladder': ((200, 200, 206), 'ladder'), 'plate': ((230, 226, 200), 'paint')}

def wheel(c, i, x, z, r=5, tw=4, clr=0):
    c.bone(f'w{i}', pivot=(x, r, z))
    side = 1 if x > 0 else -1
    x0 = x - tw / 2
    c.cube((x0, 0, z - r), (tw, 2 * r, 2 * r), 'tire', f'w{i}')
    c.cube((x0 + (tw - 0.4 if side > 0 else -0.01), 1.5, z - r + 1.5), (0.45, 2 * r - 3, 2 * r - 3), 'rim', f'w{i}')

def car(name, color, accent, L=64, W=28, H=9, clr=4, cabin=(-10, 14), ch=9, inset=2, wr=5, kind='sedan'):
    c = Car(name)
    x1, z1, y0 = -W / 2, -L / 2, clr
    # chassis + body panels
    c.cube((x1 + 1, y0 - 1.5, z1 + 6), (W - 2, 1.5, L - 12), 'black')
    c.cube((x1, y0, z1), (W, H, L), 'paint')
    c.cube((x1 + 1, y0 + H, z1 + 1), (W - 2, 0.6, cabin[0] - z1 - 1), 'paint')        # hood skin
    c.cube((x1 + 1, y0 + H, cabin[1]), (W - 2, 0.6, L / 2 - cabin[1] - 1), 'paint')   # trunk lid
    # cabin: glass box + roof + pillars
    cz0, cz1 = cabin
    c.cube((x1 + inset, y0 + H, cz0), (W - 2 * inset, ch, cz1 - cz0), 'glass')
    c.cube((x1 + inset - 0.3, y0 + H + ch, cz0 + 1.5), (W - 2 * inset + 0.6, 1.2, cz1 - cz0 - 3), 'paint')
    for zz in (cz0, (cz0 + cz1) / 2 - 0.5, cz1 - 1):
        for xx in (x1 + inset - 0.3, -x1 - inset - 0.7):
            c.cube((xx, y0 + H, zz), (1, ch, 1), 'paint')
    c.cube((x1 + inset + 1, y0 + H, cz0 + 3), (W - 2 * inset - 2, 3, cz1 - cz0 - 6), 'interior')    # seats seen through glass
    # front: grille, lights, bumper, plate
    c.cube((-6, y0 + 2, z1 - 0.4), (12, 4, 0.5), 'grille')
    for xx in (x1 + 1.5, -x1 - 6.5):
        c.cube((xx, y0 + H - 3.5, z1 - 0.4), (5, 2.5, 0.5), 'head')
        c.cube((xx, y0 + H - 3, -z1 - 0.1), (5, 2, 0.5), 'tail')
        c.cube((xx + (0 if xx < 0 else 3), y0 + H - 5.2, z1 - 0.3), (2, 1, 0.4), 'orange')
    c.cube((x1 - 0.5, y0 - 0.5, z1 - 2), (W + 1, 3, 2.5), 'chrome' if kind in ('muscle', 'taxi') else 'black')
    c.cube((x1 - 0.5, y0 - 0.5, -z1 - 0.5), (W + 1, 3, 2.5), 'chrome' if kind in ('muscle', 'taxi') else 'black')
    c.cube((-3, y0 + 0.6, -z1 + 1.9), (6, 2, 0.4), 'plate')
    # sides: door seams, handles, mirrors, skirts
    for side in (-1, 1):
        xs = side * W / 2 - (0.15 if side > 0 else 0.05)
        for zz in (cz0 + 0.5, (cz0 + cz1) / 2, cz1 - 0.5):
            c.cube((xs, y0 + 1.5, zz), (0.2, H - 2, 0.3), 'black')
        for zz in (cz0 + 4, (cz0 + cz1) / 2 + 3):
            c.cube((xs + (0.1 if side > 0 else -0.2), y0 + H - 3, zz), (0.3, 0.6, 2), 'chrome')
        c.cube((side * (W / 2 + 0.6) - 1, y0 + H + 1, cz0 + 1), (2, 1.6, 1.6), 'paint')
        c.cube((xs - (0.3 if side < 0 else 0), y0 - 0.5, z1 + 12), (0.6, 1.2, L - 24), 'black')
    # wheel arches + exhaust
    for zz in (z1 + 12, -z1 - 12):
        for side in (-1, 1):
            c.cube((side * W / 2 - (1 if side > 0 else 0), y0 + wr * 2 - 4, zz - wr - 1), (1.1, 1.2, 2 * wr + 2), 'black')
    c.cube((x1 + 4, y0 - 0.8, -z1 - 0.5), (1.5, 1.5, 2), 'chrome')
    # wheels
    i = 0
    for zz in (z1 + 12, -z1 - 12):
        for side in (-1, 1):
            wheel(c, i, side * (W / 2 - 1.5), zz, wr); i += 1
    return c, i

def save(c, n, color, accent):
    c.save(mats(color, accent), [f'w{i}' for i in range(n)])

# ---- sports cars: low, wide, spoiler, intakes
def sports(name, color, accent, stripes=False, wing=True):
    c, n = car(name, color, accent, L=66, W=30, H=7, clr=3, cabin=(-6, 12), ch=7, inset=3, wr=5, kind='sport')
    c.cube((-15, 3, -33.4), (30, 2, 0.6), 'black')                   # splitter
    c.cube((-10, 10.4, -28), (20, 0.4, 14), 'accent' if stripes else 'paint')
    if stripes:
        for xx in (-4, 1.5): c.cube((xx, 10.4, -33), (2.5, 0.7, 66), 'accent'); c.cube((xx, 17.6, -6), (2.5, 0.6, 18), 'accent')
    for side in (-1, 1): c.cube((side * 15 - (0.2 if side > 0 else 0.1), 5, 8), (0.3, 3, 6), 'black')   # side intakes
    if wing:
        for xx in (-9, 8): c.cube((xx, 10, 28), (1, 4, 2), 'black')
        c.cube((-14, 14, 26.5), (28, 1, 5), 'accent' if not stripes else 'black')
        for side in (-1, 1): c.cube((side * 14 - (1 if side > 0 else 0), 12.5, 26.5), (1, 3, 5), 'black')
    c.cube((-12, 4, 32.4), (24, 2, 1), 'black'); c.cube((-6, 4.5, 33), (2, 1.5, 1), 'chrome'); c.cube((4, 4.5, 33), (2, 1.5, 1), 'chrome')   # diffuser + twin exhaust
    save(c, n, color, accent)

sports('sports_gt', (196, 22, 26), (24, 24, 28))
sports('sports_rs', (240, 196, 20), (24, 24, 28))
sports('sports_turbo', (30, 80, 190), (240, 240, 244), stripes=True)

c, n = car('taxi', (242, 196, 30), (20, 20, 20), kind='taxi')
c.cube((-4, 23.2, -2), (8, 3, 4), 'white'); c.cube((-4.3, 23.2, -2.3), (8.6, 0.5, 4.6), 'black')
for side in (-1, 1): c.cube((side * 14 - (0.2 if side > 0 else 0.1), 7.5, -24), (0.35, 1.6, 48), 'checker')
save(c, n, (242, 196, 30), (20, 20, 20))

c, n = car('muscle_car', (28, 28, 32), (240, 120, 20), L=66, H=9, cabin=(-6, 14), ch=8, kind='muscle')
c.cube((-4, 13.6, -26), (8, 2, 10), 'black'); c.cube((-3, 14, -26.3), (6, 1.2, 0.4), 'grille')
for xx in (-6, 3): c.cube((xx, 13.4, -33), (3, 0.6, 66), 'accent')
for side in (-1, 1): c.cube((side * 15 - (0.4 if side > 0 else 0.2), 5, -8), (0.6, 1, 10), 'chrome')
save(c, n, (28, 28, 32), (240, 120, 20))

# jeep: open top, roll bar, spare tire, high, big wheels
c = Car('jeep'); col, acc = (86, 98, 56), (40, 44, 30)
c.cube((-13, 8, -28), (26, 8, 56), 'paint'); c.cube((-12, 6.5, -24), (24, 1.5, 48), 'black')
c.cube((-12, 16, -28), (24, 0.6, 14), 'paint'); c.cube((-6, 9, -28.4), (12, 6, 0.5), 'grille')
for xx in (-11, 7): c.cube((xx, 12, -28.4), (4, 3, 0.5), 'head'); c.cube((xx, 12, 27.9), (4, 2, 0.5), 'tail')
c.cube((-12, 16, -14), (24, 7, 1), 'glass'); c.cube((-12.5, 16, -14.5), (1, 8, 1.5), 'black'); c.cube((11.5, 16, -14.5), (1, 8, 1.5), 'black')
for zz in (2, 14):
    for xx in (-12, 11): c.cube((xx, 16, zz), (1, 11, 1), 'black')
    c.cube((-12, 26, zz), (24, 1, 1), 'black')
c.cube((-12, 26, 2), (1, 1, 13), 'black'); c.cube((11, 26, 2), (1, 1, 13), 'black')
c.cube((-10, 16, -8), (8, 6, 6), 'seat'); c.cube((2, 16, -8), (8, 6, 6), 'seat'); c.cube((-10, 16, 6), (20, 6, 6), 'seat')
c.cube((-14, 7, -30), (28, 3, 2), 'black'); c.cube((-14, 7, 28), (28, 3, 2), 'black')
c.bone('spare', 'body', (0, 14, 30)); c.cube((-5, 9, 28.5), (10, 10, 3), 'tire', 'spare'); c.cube((-3, 11, 31.4), (6, 6, 0.4), 'rim', 'spare')
for zz in (-17, 17):
    for side in (-1, 1): c.cube((side * 13 - (1.5 if side > 0 else 0), 13, zz - 7), (1.5, 1.5, 14), 'black')
i = 0
for zz in (-17, 17):
    for side in (-1, 1): wheel(c, i, side * 12.5, zz, 7, 5); i += 1
save(c, i, col, acc)

# panel van
c = Car('van', 1024, 512); col = (236, 236, 236)
c.cube((-14, 4, -34), (28, 22, 68), 'paint'); c.cube((-13, 4, -40), (26, 10, 6), 'paint'); c.cube((-12, 14, -38), (24, 0.6, 4), 'paint')
c.cube((-13, 14, -34.4), (26, 8, 0.5), 'glass'); c.cube((-6, 5, -40.4), (12, 5, 0.5), 'grille')
for xx in (-12, 7): c.cube((xx, 10, -40.4), (5, 3, 0.5), 'head'); c.cube((xx, 8, 33.9), (5, 4, 0.5), 'tail')
for side in (-1, 1):
    c.cube((side * 14 - (0.2 if side > 0 else 0.1), 14, -32), (0.3, 7, 8), 'glass')
    c.cube((side * 14 - (0.2 if side > 0 else 0.1), 8, -8), (0.3, 0.4, 36), 'accent')
    c.cube((side * 15 - 1, 15, -33), (2, 2, 1.5), 'paint')
c.cube((-0.2, 5, 33.9), (0.4, 20, 0.6), 'black'); c.cube((-15, 3, -41), (30, 3, 2), 'black'); c.cube((-15, 3, 33), (30, 3, 2), 'black')
i = 0
for zz in (-26, 24):
    for side in (-1, 1): wheel(c, i, side * 13, zz, 5); i += 1
save(c, i, col, (40, 100, 170))

# school bus
c = Car('school_bus', 1024, 1024); col = (244, 186, 20)
c.cube((-15, 5, -48), (30, 26, 98), 'paint'); c.cube((-14, 5, -60), (28, 13, 12), 'paint'); c.cube((-6, 6, -60.4), (12, 6, 0.5), 'grille')
c.cube((-14, 18, -48.4), (28, 10, 0.5), 'glass')
for xx in (-13, 8): c.cube((xx, 12, -60.4), (5, 3, 0.5), 'head'); c.cube((xx, 9, 49.9), (5, 4, 0.5), 'tail')
for side in (-1, 1):
    xs = side * 15 - (0.2 if side > 0 else 0.1)
    for k in range(9): c.cube((xs, 18, -42 + k * 10), (0.3, 8, 8), 'glass')
    c.cube((xs, 15, -46), (0.3, 1, 94), 'black'); c.cube((xs, 9, -46), (0.3, 1, 94), 'black')
    c.cube((xs - side * 0.3, 31, -46), (0.6, 1, 94), 'redlight' if side < 0 else 'orange')
c.cube((-17, 16, -40), (2, 4, 4), 'redlight'); c.cube((-15.5, 4, -62), (31, 3, 2), 'black'); c.cube((-15.5, 4, 49), (31, 3, 2), 'black')
c.cube((-14, 31, -46), (28, 1, 94), 'white')
i = 0
for zz in (-44, 36):
    for side in (-1, 1): wheel(c, i, side * 14, zz, 6, 5); i += 1
save(c, i, col, (20, 20, 20))

# fire truck
c = Car('fire_truck', 1024, 1024); col = (196, 24, 24)
c.cube((-15, 6, -26), (30, 18, 76), 'paint'); c.cube((-15, 6, -48), (30, 26, 22), 'paint')
c.cube((-14, 20, -48.4), (28, 10, 0.5), 'glass'); c.cube((-7, 7, -48.4), (14, 9, 0.5), 'grille')
for xx in (-14, 9): c.cube((xx, 16, -48.4), (5, 3, 0.5), 'head'); c.cube((xx, 10, 49.9), (5, 4, 0.5), 'tail')
c.cube((-12, 32, -44), (11, 2, 4), 'redlight'); c.cube((1, 32, -44), (11, 2, 4), 'bluelight')
c.bone('ladder', 'body', (0, 25, 0)); c.cube((-6, 25, -30), (12, 1.5, 76), 'ladder', 'ladder'); c.cube((-1, 24, 40), (2, 2, 2), 'chrome', 'ladder')
for side in (-1, 1):
    xs = side * 15 - (0.2 if side > 0 else 0.1)
    for k in range(4): c.cube((xs, 9, -22 + k * 17), (0.3, 12, 15), 'chrome')
    c.cube((xs, 7, -24), (0.3, 1.5, 72), 'white')
    c.cube((side * 16 - 1, 22, -40), (2, 3, 2), 'chrome')
c.cube((-15.5, 5, -50), (31, 3, 2), 'chrome'); c.cube((-4, 8, 49.8), (8, 8, 1), 'tire')
i = 0
for zz in (-38, 26, 40):
    for side in (-1, 1): wheel(c, i, side * 14, zz, 6, 5); i += 1
save(c, i, col, (236, 236, 236))

# ---- superbikes
def bike(name, color, accent):
    c = Car(name, 128, 128)
    c.cube((-1.5, 5, -9), (3, 5, 18), 'black')                                  # frame
    c.cube((-3, 7, -13), (6, 7, 8), 'paint')                                    # front fairing
    c.cube((-2.5, 9, -15), (5, 4, 2), 'paint'); c.cube((-2, 13, -13.5), (4, 4, 1), 'glass')   # nose + windscreen
    c.cube((-1.5, 11, -15.3), (3, 1.5, 0.5), 'head'); c.cube((-1, 9.5, -15.3), (2, 1, 0.5), 'head')
    c.cube((-2.8, 11, -5), (5.6, 4, 6), 'paint'); c.cube((-2.6, 14.6, -4.5), (5.2, 0.6, 5), 'accent')   # tank
    c.cube((-2, 12, 1), (4, 2, 6), 'seat'); c.cube((-2.2, 12.5, 6), (4.4, 3, 6), 'paint'); c.cube((-1, 13, 11.5), (2, 1, 0.5), 'tail')   # seat + tail
    for side in (-1, 1):
        c.cube((side * 3 - (0.2 if side > 0 else 0), 6, -11), (0.2, 6, 14), 'accent')
        c.cube((side * 2.6 - (0.8 if side > 0 else 0), 6, -14), (0.8, 7, 0.8), 'chrome')      # forks
        c.cube((side * 4 - (1 if side > 0 else 0), 15, -11), (2, 0.6, 0.6), 'black')           # bars
    c.cube((2, 5, 2), (1.5, 1.5, 10), 'chrome'); c.cube((2.2, 6, 11), (1.2, 1.2, 2), 'black')  # exhaust
    c.bone('wf', pivot=(0, 5, -14)); c.cube((-1.2, 0, -19), (2.4, 10, 10), 'tire', 'wf'); c.cube((-1.3, 1.5, -17.5), (2.6, 7, 7), 'rim', 'wf')
    c.bone('wr', pivot=(0, 5, 10)); c.cube((-1.6, 0, 5), (3.2, 10, 10), 'tire', 'wr'); c.cube((-1.7, 1.5, 6.5), (3.4, 7, 7), 'rim', 'wr')
    c.save(mats(color, accent), ['wf', 'wr'])
bike('superbike_vortex', (40, 190, 60), (20, 20, 24))
bike('superbike_blaze', (210, 30, 30), (240, 240, 240))
print('cars done')

# ---- mission board: a 2-block-wide notice board on two legs
c = Car('mission_board', 256, 256)
bm = {'paint': ((112, 78, 46), 'paint'), 'dark': ((70, 46, 26), 'paint'), 'cork': ((176, 132, 86), 'paint'), 'paper': ((236, 230, 210), 'paint'), 'paper2': ((240, 220, 160), 'paint'),
      'red': ((200, 40, 40), 'light'), 'roof': ((90, 58, 34), 'paint'), 'black': ((26, 26, 30), 'paint'), 'tire': ((22, 22, 24), 'tire'), 'rim': ((170, 176, 186), 'rim')}
for x in (-16, 13):
    c.cube((x, 0, -1.5), (3, 34, 3), 'dark'); c.cube((x - 1, 0, -2.5), (5, 1.5, 5), 'dark')
c.cube((-15, 12, -0.5), (30, 20, 1.5), 'paint'); c.cube((-14, 13, -1), (28, 18, 0.6), 'cork')
c.cube((-18, 33, -3), (36, 2, 6), 'roof'); c.cube((-17, 32, -2.5), (34, 1, 5), 'dark')
for (x, y, w, h, m) in ((-13, 23, 7, 7, 'paper'), (-5, 24, 6, 6, 'paper2'), (2, 22, 8, 8, 'paper'), (-12, 14, 8, 7, 'paper2'), (-3, 15, 6, 7, 'paper'), (5, 14, 8, 6, 'paper2')):
    c.cube((x, y, -1.2), (w, h, 0.3), m); c.cube((x + w / 2 - 0.5, y + h - 1.2, -1.5), (1, 1, 0.4), 'red')
c.cube((-15, 11, -1.5), (30, 1, 3), 'dark')
c.save(bm, [])
