"""TWD 2.1: all 19 vehicles rebuilt from scratch.
Real shapes instead of boxes: sloped windshields and rear glass, pillars, wheel arches, octagonal tyres with spoked rims
and brake discs, headlight housings with lenses, chrome grilles, bumpers with fog lights, plates, mirrors on stalks,
door seams and handles, side skirts, exhausts, a full interior (dash, steering wheel, seats), and per-type gear
(pickup bed, light bars, taxi sign, ambulance box, armour, roll cage, bus windows, fire ladder, turret...).
Painted on a 2x sheet: glossy paint with reflections, tinted glass, tyre tread, plates with text."""
import json, math, os, random, sys
from PIL import Image, ImageDraw, ImageFont
sys.path.insert(0, os.path.dirname(__file__))
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')
K = 2   # texture resolution multiplier
FONT = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 9)


def lt(c, k): return tuple(int(v + (255 - v) * k) for v in c[:3])
def dk(c, k): return tuple(int(v * k) for v in c[:3])


class V:
    def __init__(s, name, tw=256, th=256):
        s.name, s.tw, s.th = name, tw, th
        s.bones, s.order, s.uvs = {}, [], []
        s.u = s.v = s.rowh = 0
        s.bone('body', pivot=(0, 0, 0))

    def bone(s, n, parent=None, pivot=(0, 0, 0)):
        b = {"name": n, "pivot": [round(x, 2) for x in pivot], "cubes": []}
        if parent: b["parent"] = parent
        s.bones[n] = b; s.order.append(n)

    def c(s, o, sz, mat, bone='body', rot=None, pivot=None, deco=None):
        w, h, d = [max(0.15, v) for v in sz]
        uw, uh = int(2 * (math.ceil(w) + math.ceil(d))) + 1, int(math.ceil(h) + math.ceil(d)) + 1
        if s.u + uw > s.tw: s.u = 0; s.v += s.rowh; s.rowh = 0
        if s.v + uh > s.th: s.th *= 2
        uv = [s.u, s.v]; s.u += uw; s.rowh = max(s.rowh, uh)
        cb = {"origin": [round(x, 3) for x in o], "size": [round(x, 3) for x in (w, h, d)], "uv": uv}
        if rot: cb["rotation"] = list(rot); cb["pivot"] = [round(x, 3) for x in (pivot or o)]
        s.bones[bone]["cubes"].append(cb)
        s.uvs.append((uv, (w, h, d), mat, deco))

    # ------------------------------------------------------------ painting
    def paint(s, M):
        W, H = s.tw * K, s.th * K
        im = Image.new('RGBA', (W, H), (0, 0, 0, 0)); px = im.load(); r = random.Random(s.name)
        d = ImageDraw.Draw(im)
        for (u, v), (w, h, dd), mat, deco in s.uvs:
            base, kind = M[mat]
            cw, ch, cd = math.ceil(w), math.ceil(h), math.ceil(dd)
            faces = {'top': (u + cd, v, cw, cd), 'bottom': (u + cd + cw, v, cw, cd), 'side1': (u, v + cd, cd, ch), 'front': (u + cd, v + cd, cw, ch),
                     'side2': (u + cd + cw, v + cd, cd, ch), 'back': (u + 2 * cd + cw, v + cd, cw, ch)}
            for f, (fx, fy, fw, fh) in faces.items():
                X0, Y0, FW, FH = fx * K, fy * K, max(1, fw * K), max(1, fh * K)
                for y in range(Y0, min(H, Y0 + FH)):
                    ty = (y - Y0) / max(1, FH - 1)
                    for x in range(X0, min(W, X0 + FW)):
                        tx = (x - X0) / max(1, FW - 1)
                        c = list(base); n = r.randint(-3, 3)
                        if kind == 'paint':
                            if f == 'top': n += 16 + (10 if 0.25 < tx < 0.35 else 0)
                            elif f == 'bottom': n -= 40
                            else:
                                n += int(18 - 34 * ty)
                                if 0.12 < ty < 0.2: n += 22          # shoulder reflection line
                                if ty > 0.93: n -= 18
                        elif kind == 'glass':
                            c = [int(base[0] + 30 * ty), int(base[1] + 30 * ty), int(base[2] + 25 * ty)]
                            if abs((tx - ty * 0.6) % 1 - 0.3) < 0.05 or abs((tx - ty * 0.6) % 1 - 0.42) < 0.02: n += 60
                            if f in ('top', 'bottom'): n -= 10
                        elif kind == 'tire':
                            n = r.randint(-2, 2) + (-14 if (y // 2 + (x // 3)) % 3 == 0 else 6)
                            if f in ('side1', 'side2') and (0.12 < tx < 0.2 or 0.8 < tx < 0.88 or 0.12 < ty < 0.2 or 0.8 < ty < 0.88): n += 18
                        elif kind == 'rim':
                            cx, cy = tx - 0.5, ty - 0.5; rr = math.hypot(cx, cy); a = math.atan2(cy, cx)
                            if rr > 0.48: c = [24, 24, 26]
                            elif rr < 0.1: c = lt(base, 0.4)
                            elif rr < 0.16: c = dk(base, 0.5)
                            elif (math.cos(a * deco_spokes(deco)) > 0.55) or rr > 0.42: n += 18
                            else: c = [36, 36, 40]
                        elif kind == 'chrome':
                            n += int(40 * math.sin(ty * 6.3) + 10)
                        elif kind == 'light':
                            n += (30 if (x // 2 + y // 2) % 3 == 0 else 0) + (20 if ty < 0.3 else 0)
                        elif kind == 'grille':
                            c = [44, 44, 48] if ((x // 2) + (y // 2)) % 2 == 0 else [12, 12, 14]
                        elif kind == 'fabric':
                            n += (5 if (x + y) % 2 else -4) + (-12 if (y // 3) % 4 == 0 else 0)
                        elif kind == 'stripe':
                            c = list(base) if ((x + y) // 5) % 2 == 0 else [20, 20, 22]
                        elif kind == 'matte':
                            n += int(6 - 14 * ty)
                        elif kind == 'rubber':
                            n = r.randint(-2, 2)
                        elif kind == 'plate':
                            n = 0
                        px[x, y] = tuple(max(0, min(255, int(k) + n)) for k in c[:3]) + (255,)
            # decals painted on the front face
            fx, fy, fw, fh = faces['front']
            X0, Y0, FW, FH = fx * K, fy * K, fw * K, fh * K
            if deco and isinstance(deco, str):
                if deco.startswith('plate:'):
                    d.rectangle([X0, Y0, X0 + FW - 1, Y0 + FH - 1], outline=(40, 40, 60, 255))
                    d.text((X0 + 2, Y0 + max(0, FH // 2 - 6)), deco[6:], fill=(30, 30, 60, 255), font=FONT)
                elif deco.startswith('text:'):
                    _, txt, col = deco.split(':')
                    d.text((X0 + 2, Y0 + max(0, FH // 2 - 6)), txt, fill=tuple(int(col[i:i + 2], 16) for i in (0, 2, 4)) + (255,), font=FONT)
                elif deco.startswith('sidetext:'):
                    _, txt, col = deco.split(':')
                    for side in ('side1', 'side2'):
                        sx, sy, sw, sh_ = faces[side]
                        d.text((sx * K + 2, sy * K + max(0, sh_ * K // 2 - 6)), txt, fill=tuple(int(col[i:i + 2], 16) for i in (0, 2, 4)) + (255,), font=FONT)
                elif deco == 'sidecross':
                    for side in ('side1', 'side2'):
                        sx, sy, sw, sh_ = faces[side]; X, Y, Wd, Hd = sx * K, sy * K, sw * K, sh_ * K
                        cx, cy, sz = X + Wd // 2, Y + Hd // 2, min(Wd, Hd) // 3
                        d.rectangle([cx - sz, cy - sz // 3, cx + sz, cy + sz // 3], fill=(210, 30, 30, 255)); d.rectangle([cx - sz // 3, cy - sz, cx + sz // 3, cy + sz], fill=(210, 30, 30, 255))
                elif deco == 'cross':
                    cx, cy, sz = X0 + FW // 2, Y0 + FH // 2, min(FW, FH) // 3
                    d.rectangle([cx - sz, cy - sz // 3, cx + sz, cy + sz // 3], fill=(210, 30, 30, 255)); d.rectangle([cx - sz // 3, cy - sz, cx + sz // 3, cy + sz], fill=(210, 30, 30, 255))
                elif deco == 'seams':       # door seams + handle shadow on a body side
                    for side in ('side1', 'side2', 'front', 'back'):
                        sx, sy, sw, sh_ = faces[side]
                        if side in ('front', 'back') and cw < 6: continue
                        X, Y, Wd, Hd = sx * K, sy * K, sw * K, sh_ * K
                        if Wd < 30: continue
                        for t in (0.36, 0.62):
                            xx = X + int(Wd * t); d.line([(xx, Y + 2), (xx, Y + Hd - 3)], fill=(20, 20, 22, 255))
                        d.line([(X + 2, Y + int(Hd * 0.18)), (X + Wd - 3, Y + int(Hd * 0.18))], fill=lt(base, 0.25) + (255,))
        return im

    def save(s, M, wheels, length=1.0, spin=-360):
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": f"geometry.kiyamet.{s.name}", "texture_width": s.tw, "texture_height": s.th,
               "visible_bounds_width": 10, "visible_bounds_height": 6, "visible_bounds_offset": [0, 1.5, 0]}, "bones": [s.bones[n] for n in s.order]}]}
        json.dump(geo, open(f'{A}/geckolib/models/entity/{s.name}.geo.json', 'w'), indent=1)
        s.paint(M).save(f'{A}/textures/entity/{s.name}.png', optimize=True)
        rot = {"0.00": [0, 0, 0], "0.50": [spin / 2, 0, 0], "1.00": [spin, 0, 0]}
        anim = {"format_version": "1.8.0", "animations": {
            "misc.idle": {"loop": True, "animation_length": 2.0, "bones": {"body": {"position": {"0.00": [0, 0, 0], "1.00": [0, -0.06, 0], "2.00": [0, 0, 0]}}}},
            "move.walk": {"loop": True, "animation_length": 1.0, "bones": {"body": {"position": {"0.00": [0, 0, 0], "0.50": [0, 0.2, 0], "1.00": [0, 0, 0]}}}},
            "attack.swing": {"loop": False, "animation_length": 0.5, "bones": {}},
            "move.drive": {"loop": True, "animation_length": 0.5 * length, "bones": dict({wb: {"rotation": {k2.replace('1.00', f'{0.5 * length:.2f}').replace('0.50', f'{0.25 * length:.2f}'): v for k2, v in rot.items()}} for wb in wheels},
                           body={"position": {"0.00": [0, 0, 0], f'{0.125 * length:.2f}': [0, 0.12, 0], f'{0.25 * length:.2f}': [0, 0, 0], f'{0.375 * length:.2f}': [0, 0.08, 0], f'{0.5 * length:.2f}': [0, 0, 0]}})}}}
        json.dump(anim, open(f'{A}/geckolib/animations/entity/{s.name}.animation.json', 'w'), indent=1)


def deco_spokes(deco): return deco if isinstance(deco, int) else 5


def mats(paint, accent=None, rim=(178, 184, 194), interior=(46, 44, 48), seat=(70, 50, 40)):
    accent = accent or dk(paint, 0.6)
    return {'paint': (paint, 'paint'), 'accent': (accent, 'paint'), 'glass': ((40, 64, 86), 'glass'), 'black': ((28, 28, 32), 'matte'), 'tire': ((24, 24, 26), 'tire'),
            'rim': (rim, 'rim'), 'chrome': ((200, 206, 214), 'chrome'), 'head': ((255, 246, 210), 'light'), 'tail': ((200, 24, 28), 'light'), 'amber': ((240, 150, 30), 'light'),
            'grille': ((30, 30, 30), 'grille'), 'plate': ((236, 232, 214), 'plate'), 'interior': (interior, 'matte'), 'seat': (seat, 'fabric'), 'red': ((220, 30, 30), 'light'),
            'blue': ((40, 90, 240), 'light'), 'white': ((236, 236, 236), 'paint'), 'disc': ((120, 122, 128), 'chrome'), 'caliper': ((200, 30, 30), 'paint'),
            'stripe': ((240, 196, 40), 'stripe'), 'rubber': ((30, 30, 32), 'rubber'), 'yellow': ((240, 196, 40), 'paint'), 'olive': ((92, 100, 60), 'matte'), 'steel': ((130, 134, 140), 'chrome')}


# ------------------------------------------------------------ parts
def wheel(v, i, x, z, r, tw, outer, spokes=5, caliper=False, name=None):
    bn = name or f'w{i}'
    v.bone(bn, pivot=(x, r, z))
    x0 = x - tw / 2
    v.c((x0, 0, z - r * 0.42), (tw, 2 * r, r * 0.84), 'tire', bn)                                     # octagonal tyre: 2 slabs + 2 rotated
    v.c((x0, r - r * 0.42, z - r), (tw, r * 0.84, 2 * r), 'tire', bn)
    v.c((x0 + 0.02, 0.3, z - r * 0.42), (tw - 0.04, 2 * r - 0.6, r * 0.84), 'tire', bn, rot=(45, 0, 0), pivot=(x, r, z))
    rr = r * 0.66
    xo = x0 + tw - 0.25 if outer > 0 else x0 - 0.05
    v.c((xo, r - rr, z - rr), (0.3, 2 * rr, 2 * rr), 'rim', bn, deco=spokes)                         # spoked rim face
    v.c((xo + (0.2 if outer > 0 else -0.2), r - 0.6, z - 0.6), (0.3, 1.2, 1.2), 'chrome', bn)       # hub cap
    if caliper: v.c((x - outer * 0.1 - 0.4, r + rr * 0.3, z - rr * 0.6), (0.8, rr * 0.6, rr * 0.9), 'caliper')


def lights_front(v, W, y, z, h=2.2, w=5.5, drl=True):
    for s in (-1, 1):
        x = s * (W / 2 - 1.2) - (w if s > 0 else 0)
        v.c((x - 0.2, y - 0.2, z - 0.5), (w + 0.4, h + 0.4, 0.5), 'chrome')
        v.c((x, y, z - 0.8), (w, h, 0.4), 'head')
        if drl: v.c((x + 0.4, y - 0.9, z - 0.6), (w - 0.8, 0.5, 0.3), 'head')
        v.c((x + (0 if s < 0 else w - 1.6), y + h + 0.2, z - 0.6), (1.6, 0.7, 0.3), 'amber')


def lights_rear(v, W, y, z, h=2, w=5, wrap=True):
    for s in (-1, 1):
        x = s * (W / 2 - 1) - (w if s > 0 else 0)
        v.c((x, y, z + 0.1), (w, h, 0.5), 'tail')
        if wrap: v.c((s * (W / 2) - (0.4 if s > 0 else 0.1), y, z - 2.5), (0.5, h, 2.6), 'tail')


def grille(v, w, h, y, z, chrome=True):
    if chrome: v.c((-w / 2 - 0.5, y - 0.5, z - 0.4), (w + 1, h + 1, 0.4), 'chrome')
    v.c((-w / 2, y, z - 0.7), (w, h, 0.4), 'grille')
    v.c((-0.8, y + h / 2 - 0.6, z - 1), (1.6, 1.2, 0.3), 'chrome')                                   # badge


def bumper(v, W, y, z, front, chromed=False, fog=True, plate='TWD 2027'):
    s = -1 if front else 1
    m = 'chrome' if chromed else 'black'
    v.c((-W / 2 - 0.3, y, z + (s * 1.6 if s > 0 else -1.8) - (0 if s > 0 else 0)), (W + 0.6, 2.6, 1.8), m)
    v.c((-W / 2 + 1.5, y - 0.6, z + (0.6 if s > 0 else -1.6)), (W - 3, 0.7, 1.2), 'black')            # lip
    if fog and front:
        for x in (-W / 2 + 2.5, W / 2 - 4.5): v.c((x, y + 0.6, z - 2), (2, 1.2, 0.3), 'head')
    v.c((-3.5, y + (3 if not front else 0.3), z + (s * 0.4 if s > 0 else -2.1)), (7, 2, 0.3), 'plate', deco='plate:' + plate)


def mirrors(v, W, y, z):
    for s in (-1, 1):
        x = s * (W / 2) - (0 if s > 0 else 0.6)
        v.c((x, y, z), (0.6, 0.6, 1.4), 'black')
        v.c((x + s * 0.6 - (0 if s > 0 else 0.6) + (0 if s > 0 else 0), y - 0.3, z - 0.2), (1.8 if False else 1.6, 1.6, 2), 'paint')
        v.c((x + s * 0.6 + (0.2 if s > 0 else -0.4) - (0 if s > 0 else 1.0), y - 0.1, z + 1.8), (1.2, 1.2, 0.2), 'glass')


def interior(v, W, y, zf, zr, rear=True, wheel_side=-1):
    v.c((-W / 2 + 1.5, y, zf), (W - 3, 3, 3), 'interior')                                              # dashboard
    sx = wheel_side * (W / 4)
    v.c((sx - 2, y + 2.6, zf + 3.2), (4, 0.6, 0.6), 'black', rot=(-25, 0, 0), pivot=(sx, y + 2.9, zf + 3.5))      # steering wheel rim
    v.c((sx - 0.3, y + 1.2, zf + 3.2), (0.6, 3.4, 0.6), 'black', rot=(-25, 0, 0), pivot=(sx, y + 2.9, zf + 3.5))
    v.c((sx - 0.4, y + 1, zf + 3), (0.8, 0.8, 2), 'black')
    for s in (-1, 1):
        x = s * W / 4 - 2.5
        v.c((x, y - 1.5, zf + 6), (5, 1.6, 5), 'seat'); v.c((x, y - 0.5, zf + 10.5), (5, 6, 1.4), 'seat', rot=(-10, 0, 0), pivot=(x, y, zf + 11)); v.c((x + 1, y + 5.5, zf + 10.8), (3, 1.8, 1), 'seat')
    if rear: v.c((-W / 2 + 2, y - 1.5, zr - 6), (W - 4, 1.6, 5), 'seat')
    if rear: v.c((-W / 2 + 2, y - 0.5, zr - 1.6), (W - 4, 5.5, 1.4), 'seat')


def cabin(v, W, y, zf, zr, h, inset=1.5, ws=35, rs=30, roof='paint', pillar='paint'):
    """sloped windshield + roof + rear glass + side glass + pillars"""
    wi = W - 2 * inset
    run_f = h / math.tan(math.radians(90 - ws)) if ws else 0
    run_r = h / math.tan(math.radians(90 - rs)) if rs else 0
    v.c((-wi / 2, y + h - 0.8, zf + run_f), (wi, 0.8, zr - zf - run_f - run_r), roof)                 # roof
    # windshield / rear glass (rotated slabs around their bottom edge)
    Lf = h / math.cos(math.radians(ws)); Lr = h / math.cos(math.radians(rs))
    v.c((-wi / 2 + 0.6, y, zf), (wi - 1.2, Lf, 0.3), 'glass', rot=(-ws, 0, 0), pivot=(0, y, zf))
    v.c((-wi / 2 + 0.6, y, zr - 0.3), (wi - 1.2, Lr, 0.3), 'glass', rot=(rs, 0, 0), pivot=(0, y, zr))
    for s in (-1, 1):
        x = s * wi / 2 - (0.6 if s > 0 else 0)
        v.c((x, y, zf), (0.6, Lf, 0.6), pillar, rot=(-ws, 0, 0), pivot=(x, y, zf))                    # A pillar
        v.c((x, y, zr - 0.6), (0.6, Lr, 0.6), pillar, rot=(rs, 0, 0), pivot=(x, y, zr))              # C pillar
        mid = (zf + run_f + zr - run_r) / 2
        v.c((x, y, mid - 0.4), (0.6, h - 0.6, 0.8), 'black')                                           # B pillar
        xg = s * wi / 2 - (0.3 if s > 0 else -0.05)
        v.c((xg - 0.05, y + 0.2, zf + run_f * 0.25), (0.3, h - 1, zr - zf - run_f * 0.25 - run_r * 0.25), 'glass')   # side glass
    return run_f, run_r


def body_with_arches(v, W, y0, top, z1, z2, wheels_z, r, mat='paint', arch_gap=1.0):
    """lower body that leaves room for the wheels: segments between arches + panels above arches"""
    cuts = sorted(wheels_z)
    edges = [z1]
    for wz in cuts: edges += [wz - r - arch_gap, wz + r + arch_gap]
    edges.append(z2)
    for i in range(0, len(edges), 2):
        a, b = edges[i], edges[i + 1]
        if b - a > 0.2: v.c((-W / 2, y0, a), (W, top - y0, b - a), mat, deco='seams' if b - a > 20 else None)
    for wz in cuts:
        a, b = wz - r - arch_gap, wz + r + arch_gap
        ya = 2 * r + 0.6
        v.c((-W / 2, ya, a), (W, top - ya, b - a), mat)
        v.c((-W / 2 + 0.3, ya - 0.6, a + 0.5), (W - 0.6, 0.6, b - a - 1), 'black')                   # inner fender
        for s in (-1, 1): v.c((s * W / 2 - (0.6 if s > 0 else -0.0) - (0 if s > 0 else 0.6) + (0 if s < 0 else 0.6) - 0.3, ya - 0.2, a), (0.6, 1, b - a), 'black')   # arch trim


def side_details(v, W, y, z1, z2, handles=(), skirt=True):
    for s in (-1, 1):
        x = s * W / 2 - (0.2 if s > 0 else 0.1)
        for hz in handles: v.c((x + s * 0.15 - (0 if s > 0 else 0.05), y, hz), (0.3, 0.6, 2), 'chrome')
        if skirt: v.c((x - (0.2 if s < 0 else 0), y - 7.5 if False else 0, 0), (0.1, 0.1, 0.1), 'black')


# ------------------------------------------------------------ vehicle types
def car(name, paint, accent=None, L=62, W=30, clr=3.5, H=9, r=5.2, tw=3.6, wb=21, cab=(-7, 15), ch=9.5, ws=38, rs=32, kind='sedan', spokes=5, rimc=(178, 184, 194),
        plate='TWD 2027', chrome=False, caliper=False, hood_rise=0.0):
    v = V(name, 256, 256)
    M = mats(paint, accent, rimc)
    z1, z2 = -L / 2, L / 2
    top = clr + H
    wzs = [-wb, wb]
    body_with_arches(v, W, clr, top, z1 + 2, z2 - 2, wzs, r)
    v.c((-W / 2 + 0.8, clr - 1.2, z1 + 6), (W - 1.6, 1.2, L - 12), 'black')                            # floor pan
    # nose + tail tapers
    v.c((-W / 2 + 1, clr + 1, z1), (W - 2, H - 2.5, 2), 'paint'); v.c((-W / 2 + 1, clr + 1, z2 - 2), (W - 2, H - 2.5, 2), 'paint')
    # hood + trunk skins (slight slope)
    v.c((-W / 2 + 0.8, top, z1 + 1.5), (W - 1.6, 0.6 + hood_rise, cab[0] - z1 - 1.5), 'paint', rot=(-3, 0, 0), pivot=(0, top, cab[0]))
    v.c((-W / 2 + 0.8, top, cab[1]), (W - 1.6, 0.6, z2 - 1.5 - cab[1]), 'paint', rot=(2, 0, 0), pivot=(0, top, cab[1]))
    v.c((-W / 2 + 6, top + 0.55, z1 + 6), (W - 12, 0.2, 9), 'accent')                                  # hood bulge / vent
    run_f, run_r = cabin(v, W, top + 0.5, cab[0], cab[1], ch, ws=ws, rs=rs)
    interior(v, W, top - 1.5, cab[0] - 1, cab[1])
    lights_front(v, W, top - 3.8, z1)
    lights_rear(v, W, top - 3.2, z2 - 0.4)
    grille(v, W * 0.42, 3.2, clr + 2.4, z1, chrome or kind in ('muscle', 'taxi', 'sedan'))
    bumper(v, W, clr - 0.3, z1, True, chrome, plate=plate)
    bumper(v, W, clr - 0.3, z2, False, chrome, fog=False, plate=plate)
    mirrors(v, W, top + 1.2, cab[0] + 2)
    for s in (-1, 1):
        x = s * W / 2 - (0.15 if s > 0 else 0.15)
        for hz in (cab[0] + run_f + 4, (cab[0] + cab[1]) / 2 + 3): v.c((x, top - 2, hz), (0.3, 0.6, 2.2), 'chrome')
        v.c((x - (0 if s > 0 else 0.2), clr - 0.4, z1 + 9 + r), (0.5, 1.2, L - 18 - 2 * r), 'black')        # skirt
    v.c((W / 2 - 5, clr - 0.8, z2 - 0.5), (1.6, 1.6, 1.6), 'chrome'); v.c((W / 2 - 7.5, clr - 0.8, z2 - 0.5), (1.6, 1.6, 1.6), 'chrome')   # twin exhaust
    v.c((-W / 2 - 0.05, top - 3, cab[1] + 4), (0.2, 1.6, 1.6), 'black')                                # fuel cap
    i = 0
    for wz in wzs:
        for s in (-1, 1):
            wheel(v, i, s * (W / 2 - tw / 2 - 0.2), wz, r, tw, s, spokes, caliper); i += 1
    return v, M, [f'w{k}' for k in range(i)], top


def sedan():
    v, M, w, top = car('car_sedan', (170, 30, 34), chrome=True)
    v.save(M, w)


def police():
    v, M, w, top = car('car_police', (236, 236, 236), (20, 20, 26), plate='POLICE')
    roof = top + 0.5 + 9.5
    v.c((-8, roof, -1), (16, 1.6, 3), 'black'); v.c((-7.5, roof + 1.2, -0.6), (7, 1.4, 2.2), 'red'); v.c((0.5, roof + 1.2, -0.6), (7, 1.4, 2.2), 'blue')
    v.c((-15.2, 4, -8), (0.2, 4, 20), 'black'); v.c((15, 4, -8), (0.2, 4, 20), 'black')                # door livery
    v.c((-15.3, 5, -4), (0.2, 2, 12), 'white', deco='sidetext:POLICE:1a1a40'); v.c((15.1, 5, -4), (0.2, 2, 12), 'white', deco='sidetext:POLICE:1a1a40')
    v.c((-12, 2, -33.6), (24, 6, 1.2), 'black'); v.c((-12, 7, -34), (24, 1, 1), 'black')                # push bar
    v.c((10, roof, 6), (0.4, 6, 0.4), 'black')                                                          # antenna
    v.save(M, w)


def taxi():
    v, M, w, top = car('taxi', (240, 196, 30), (20, 20, 20), plate='NYC TAXI', chrome=True)
    roof = top + 0.5 + 9.5
    v.c((-4, roof, 0), (8, 2.6, 4), 'white', deco='text:TAXI:202020'); v.c((-4.2, roof, -0.2), (8.4, 0.4, 4.4), 'black')
    for s in (-1, 1): v.c((s * 15 - (0.2 if s > 0 else 0), 6, -14), (0.3, 1.2, 34), 'stripe')
    v.save(M, w)


def muscle():
    v, M, w, top = car('muscle_car', (24, 24, 28), (230, 110, 20), L=66, W=31, H=8.5, cab=(-4, 15), ch=8, ws=45, rs=40, plate='V8 HEMI', chrome=True, spokes=6, rimc=(200, 200, 205))
    v.c((-2.5, top + 0.4, -33), (5, 3, 16), 'black'); v.c((-2, top + 3.2, -24), (4, 0.6, 3), 'grille')  # hood scoop
    for x in (-5, 3): v.c((x, top + 0.62, -33), (2, 0.1, 66), 'accent')
    v.c((-15, top + 0.4, 31), (30, 1.2, 1.4), 'black')                                                 # ducktail
    v.save(M, w)


def sports(name, paint, accent, wing=True, stripes=False, spokes=10):
    v, M, w, top = car(name, paint, accent, L=68, W=32, clr=2.6, H=7, r=5.4, tw=4.4, wb=22, cab=(-5, 13), ch=7.2, ws=58, rs=50, plate='TURBO', spokes=spokes,
                       rimc=(40, 40, 44) if name != 'sports_rs' else (220, 190, 80), caliper=True)
    v.c((-16, 2, -34.6), (32, 1, 2.5), 'black')                                                         # splitter
    for s in (-1, 1):
        v.c((s * 16 - (0.3 if s > 0 else 0), 4, 1), (0.4, 3, 8), 'grille')                              # side intakes
        v.c((s * 9 - 3, 2.2, 34), (6, 2, 0.6), 'grille')                                                # diffuser fins
    if wing:
        v.c((-15, top + 6, 30), (30, 0.8, 4), 'accent')
        for s in (-1, 1): v.c((s * 10 - 0.5, top + 0.5, 31), (1, 5.6, 2), 'black'); v.c((s * 15 - (0.4 if s > 0 else 0), top + 3.5, 29.6), (0.4, 4, 4.8), 'accent')
    if stripes:
        for x in (-4.5, 1.5): v.c((x, top + 0.62, -34), (3, 0.1, 50), 'accent')
    v.save(M, w)


def pickup():
    v = V('car_pickup', 256, 256); M = mats((50, 90, 160), (30, 30, 34))
    W, L, clr, H, r, tw = 34, 70, 4.5, 10, 6.4, 4.4
    top = clr + H; wzs = [-23, 21]
    body_with_arches(v, W, clr, top, -33, 33, wzs, r)
    v.c((-W / 2 + 1, clr - 1.4, -28), (W - 2, 1.4, 56), 'black')
    v.c((-W / 2 + 0.8, top, -33), (W - 1.6, 0.7, 14), 'paint', rot=(-3, 0, 0), pivot=(0, top, -19))
    run_f, run_r = cabin(v, W, top + 0.5, -19, -1, 11, ws=32, rs=4)
    interior(v, W, top - 1.5, -20, -1, rear=False)
    # bed
    v.c((-W / 2, top, 0), (1.2, 4, 33), 'paint'); v.c((W / 2 - 1.2, top, 0), (1.2, 4, 33), 'paint'); v.c((-W / 2 + 1.2, top - 1, 0), (W - 2.4, 1, 33), 'black')
    v.c((-W / 2 + 1.2, top, 32), (W - 2.4, 4, 1.2), 'paint'); v.c((-W / 2 - 0.2, top + 3.6, 0), (1.6, 0.5, 33), 'chrome'); v.c((W / 2 - 1.4, top + 3.6, 0), (1.6, 0.5, 33), 'chrome')
    v.c((-10, top, 6), (10, 4, 8), 'olive'); v.c((3, top, 20), (5, 6, 4), 'red')                       # cargo: crate + fuel can
    v.c((-W / 2 + 2, top + 11.5, -16), (W - 4, 1.2, 1.2), 'black'); v.c((-W / 2 + 3, top + 11.8, -16), (W - 6, 1.6, 1.6), 'head')   # light bar
    lights_front(v, W, top - 4.5, -35, h=3, w=6); lights_rear(v, W, top - 1, 33.3, h=4, w=2, wrap=False)
    grille(v, W * 0.6, 5, clr + 2.4, -35, True); bumper(v, W, clr - 1, -35, True, True); bumper(v, W, clr - 1, 35, False, True, fog=False)
    mirrors(v, W, top + 1.2, -16)
    i = 0
    for wz in wzs:
        for s in (-1, 1): wheel(v, i, s * (W / 2 - tw / 2 - 0.2), wz, r, tw, s, 6, False); i += 1
    v.save(M, [f'w{k}' for k in range(i)])


def boxy(name, paint, accent, L, W, H, box_from, box_h, r=6, tw=4.4, wbs=(-20, 20), clr=4.5, cab_len=16, extra=None, six=False):
    """vans, ambulances, buses, trucks: cab + tall box"""
    v = V(name, 256, 256); M = mats(paint, accent)
    top = clr + H
    wzs = list(wbs)
    body_with_arches(v, W, clr, top, -L / 2 + 1, L / 2 - 1, wzs, r)
    v.c((-W / 2 + 1, clr - 1.4, -L / 2 + 5), (W - 2, 1.4, L - 10), 'black')
    zf = -L / 2 + 1
    v.c((-W / 2, top, zf + cab_len * 0.55), (W, box_h, L - cab_len * 0.55 - 1), 'paint', deco='seams')  # tall box
    v.c((-W / 2 + 1, top, zf), (W - 2, 0.6, cab_len * 0.55), 'paint', rot=(-6, 0, 0), pivot=(0, top, zf + cab_len * 0.55))
    zb = zf + cab_len * 0.55
    v.c((-W / 2 + 1.4, top + box_h * 0.18, zb - 0.3), (W - 2.8, box_h * 0.5, 0.4), 'glass')                                   # windshield
    v.c((-W / 2 + 0.6, top + box_h * 0.18, zb - 0.5), (0.8, box_h * 0.5, 0.6), 'black'); v.c((W / 2 - 1.4, top + box_h * 0.18, zb - 0.5), (0.8, box_h * 0.5, 0.6), 'black')
    v.c((-W / 2 + 3, top + box_h * 0.7, zb - 0.6), (W - 6, 0.8, 0.4), 'black')                                                # wiper / visor
    for s in (-1, 1):
        v.c((s * W / 2 - (0.1 if s > 0 else 0.2), top + 1, zf + cab_len * 0.6), (0.3, box_h * 0.45, cab_len * 0.4), 'glass')   # door windows
    v.c((-W / 2 + 0.2, top + box_h, zf + cab_len * 0.55 + 0.5), (W - 0.4, 0.6, L - cab_len * 0.55 - 2), 'white')            # roof
    interior(v, W, top - 1.5, zf + cab_len * 0.55 + 1, zf + cab_len + 8, rear=False)
    lights_front(v, W, top - 4, -L / 2, h=3, w=5); lights_rear(v, W, top + 1, L / 2 - 0.4, h=4, w=2, wrap=False)
    grille(v, W * 0.5, 4, clr + 2.5, -L / 2, True); bumper(v, W, clr - 1, -L / 2, True); bumper(v, W, clr - 1, L / 2, False, fog=False)
    mirrors(v, W, top + 3, zf + cab_len * 0.6)
    v.c((-W / 2 + 2, top + box_h * 0.15, L / 2 - 0.6), (W / 2 - 2.5, box_h * 0.75, 0.4), 'paint'); v.c((0.5, top + box_h * 0.15, L / 2 - 0.6), (W / 2 - 2.5, box_h * 0.75, 0.4), 'paint')  # rear doors
    v.c((-W / 2 + 4, top + box_h * 0.5, L / 2 - 0.3), (W / 2 - 6, box_h * 0.3, 0.4), 'glass'); v.c((2, top + box_h * 0.5, L / 2 - 0.3), (W / 2 - 6, box_h * 0.3, 0.4), 'glass')
    if extra: extra(v, W, L, top, box_h)
    i = 0
    for wz in wzs:
        for s in (-1, 1): wheel(v, i, s * (W / 2 - tw / 2 - 0.2), wz, r, tw, s, 6); i += 1
    v.save(M, [f'w{k}' for k in range(i)])


def ambulance_extra(v, W, L, top, bh):
    for s in (-1, 1):
        x = s * W / 2 - (0.05 if s > 0 else 0.25)
        v.c((x, top + 3, -6), (0.3, 2, 40), 'red'); v.c((x, top + 7, 6), (0.3, 8, 8), 'white', deco='sidecross')
    v.c((-W / 2 + 2, top + bh + 0.6, -10), (W - 4, 1.4, 2.4), 'black'); v.c((-W / 2 + 2.5, top + bh + 1.2, -9.7), (6, 1.2, 1.8), 'red'); v.c((W / 2 - 8.5, top + bh + 1.2, -9.7), (6, 1.2, 1.8), 'blue')
    v.c((-4, top + bh * 0.62, L / 2 - 0.15), (8, 6, 0.2), 'white', deco='cross')


def van_extra(v, W, L, top, bh):
    for s in (-1, 1): v.c((s * W / 2 - (0.05 if s > 0 else 0.25), top + 2, -4), (0.3, 1.4, 44), 'accent')
    v.c((-6, top + bh + 0.6, 0), (12, 1, 20), 'black')                                                 # roof rack


def bus_extra(v, W, L, top, bh):
    for s in (-1, 1):
        x = s * W / 2 - (0.05 if s > 0 else 0.25)
        for k in range(9): v.c((x, top + 4, -36 + k * 9.5), (0.3, 7, 8), 'glass')
        v.c((x, top + 2.4, -42), (0.3, 1, 90), 'black'); v.c((x, top + 12, -42), (0.3, 1, 90), 'black')
    v.c((-W / 2 - 3.5, top + 3, -32), (3.5, 3.5, 0.4), 'red', deco='text:STOP:ffffff')                # stop arm
    v.c((-10, top + bh - 4, -L / 2 + 8.5), (20, 3, 0.3), 'black', deco='text:SCHOOL BUS:f0c428')
    for x in (-W / 2 + 2, W / 2 - 4): v.c((x, top + bh - 2, -L / 2 + 8.4), (2, 1.4, 0.3), 'red')


def fire_extra(v, W, L, top, bh):
    for s in (-1, 1):
        x = s * W / 2 - (0.05 if s > 0 else 0.25)
        for k in range(4): v.c((x, top + 2, -10 + k * 13), (0.3, bh - 5, 11), 'steel')               # compartment doors
        v.c((x, top + bh - 2.5, -14), (0.3, 1.5, 62), 'white')
    v.bone('ladder', 'body', pivot=(0, top + bh + 2, 0))
    for s in (-1, 1): v.c((s * 5 - 0.6, top + bh + 1.5, -30), (1.2, 1.2, 76), 'steel', 'ladder')
    for k in range(18): v.c((-4.4, top + bh + 1.7, -28 + k * 4.2), (8.8, 0.6, 0.6), 'steel', 'ladder')
    v.c((-W / 2 + 2, top + bh + 0.6, -24), (W - 4, 1.4, 2.4), 'black'); v.c((-W / 2 + 3, top + bh + 1.2, -23.7), (W - 6, 1.2, 1.8), 'red')


def armored_extra(v, W, L, top, bh):
    for s in (-1, 1):
        x = s * W / 2 - (0.2 if s > 0 else 0.4)
        for k in range(6): v.c((x, top + 1, -18 + k * 8), (0.6, bh - 2, 7.4), 'olive')
        for k in range(5): v.c((x - s * 0.2, top + bh * 0.6, -16 + k * 9), (0.6, 1.4, 5), 'black')   # vision slits
    v.bone('turret', 'body', pivot=(0, top + bh + 3, 6))
    v.c((-5, top + bh + 0.6, 1), (10, 4, 10), 'olive', 'turret'); v.c((-3.5, top + bh + 4.6, 2.5), (7, 1, 7), 'olive', 'turret')
    v.c((-0.8, top + bh + 2, -12), (1.6, 1.6, 14), 'black', 'turret'); v.c((-1.1, top + bh + 1.7, -13), (2.2, 2.2, 2), 'black', 'turret')
    v.c((-W / 2 - 1, 2, -L / 2 - 4), (W + 2, 7, 1.2), 'stripe', rot=(-20, 0, 0), pivot=(0, 2, -L / 2 - 4))        # plow


def armored_suv():
    v, M, w, top = car('car_armored_suv', (70, 76, 50), (40, 44, 30), L=68, W=36, clr=6, H=11, r=7, tw=5, wb=22, cab=(-12, 26), ch=11, ws=26, rs=6, plate='ARMOR', spokes=6, rimc=(80, 84, 70))
    for s in (-1, 1):
        x = s * 18 - (0.3 if s > 0 else 0.3)
        v.c((x, 9, -10), (0.6, 5, 30), 'olive')
        v.c((s * 13 - 1, top + 11.5, -6), (2, 1.2, 28), 'black')                                       # roof rails
    v.c((-17, 3, -36.5), (34, 8, 1.2), 'stripe', rot=(-15, 0, 0), pivot=(0, 3, -36))                     # plow
    v.c((-8, top + 12, -4), (16, 3, 10), 'olive'); v.c((-3, top + 10.4, 31), (6, 6, 1.5), 'tire')       # crate + spare
    v.c((12, top + 12, 6), (3, 4, 2), 'red'); v.c((-15, top + 12, 6), (3, 4, 2), 'red')
    v.save(M, w)


def jeep():
    v = V('jeep', 256, 256); M = mats((84, 104, 60), (40, 50, 30), (90, 90, 96), seat=(60, 44, 30))
    W, clr, H, r, tw = 30, 5, 9, 6.6, 4.6
    top = clr + H; wzs = [-18, 18]
    body_with_arches(v, W, clr, top, -29, 29, wzs, r)
    v.c((-W / 2 + 0.8, top, -29), (W - 1.6, 0.7, 16), 'paint')
    v.c((-W / 2 + 1, top, -13), (W - 2, 7, 0.6), 'glass', rot=(-12, 0, 0), pivot=(0, top, -13)); v.c((-W / 2 + 0.6, top + 6.8, -14.5), (W - 1.2, 0.8, 0.8), 'black')
    for s in (-1, 1):                                                                                    # roll cage
        x = s * (W / 2 - 1.5) - 0.5
        v.c((x, top, 4), (1, 11, 1), 'black'); v.c((x, top + 10, -12), (1, 1, 17), 'black'); v.c((x, top, 26), (1, 9, 1), 'black')
        v.c((x, top + 8.5, 5), (1, 1, 21.5), 'black', rot=(-5, 0, 0), pivot=(x, top + 9, 5))
    v.c((-W / 2 + 1, top + 10, 4), (W - 2, 1, 1), 'black')
    interior(v, W, top - 1.5, -14, 26)
    for x in range(-9, 10, 3): v.c((x - 0.6, clr + 2.5, -29.4), (1.2, 4, 0.4), 'black')                 # 7-slot grille
    for s in (-1, 1): v.c((s * 8 - 2, top - 4, -29.6), (4, 4, 0.5), 'head', rot=(0, 0, 45), pivot=(s * 8, top - 2, -29.4))
    bumper(v, W, clr - 1, -29.5, True, fog=True, plate='4X4'); bumper(v, W, clr - 1, 29.5, False, fog=False, plate='4X4')
    v.bone('spare', 'body', pivot=(0, top, 31)); v.c((-5.5, clr + 2, 30), (11, 11, 3), 'tire', 'spare'); v.c((-3.5, clr + 4, 33), (7, 7, 0.4), 'rim', 'spare', deco=6)
    lights_rear(v, W, top - 3, 29, h=2.4, w=2.4, wrap=False); mirrors(v, W, top + 2, -12)
    i = 0
    for wz in wzs:
        for s in (-1, 1): wheel(v, i, s * (W / 2 - tw / 2 + 0.8), wz, r, tw, s, 6); i += 1
    v.save(M, [f'w{k}' for k in range(i)])


def atv():
    v = V('atv', 128, 128); M = mats((200, 60, 30), (30, 30, 30), (60, 60, 64))
    v.c((-6, 4, -9), (12, 3, 18), 'black'); v.c((-8, 7, -12), (16, 2, 7), 'paint'); v.c((-8, 7, 5), (16, 2, 7), 'paint')            # fenders
    v.c((-4, 7, -5), (8, 5, 10), 'paint'); v.c((-3.5, 12, -1), (7, 1.5, 8), 'seat'); v.c((-4, 9.5, -8), (8, 1, 4), 'grille')
    v.c((-0.6, 9, -7), (1.2, 7, 1.2), 'black', rot=(-25, 0, 0), pivot=(0, 9, -6.4)); v.c((-6, 15, -9.5), (12, 0.8, 0.8), 'black')
    for s in (-1, 1): v.c((s * 6 - 0.7, 14.6, -10), (1.4, 1.6, 1.6), 'rubber')
    v.c((-7, 9.2, 7), (14, 0.6, 5), 'steel'); v.c((-7, 9.2, -13), (14, 0.6, 3), 'steel')                # racks
    for s in (-1, 1): v.c((s * 3 - 1, 8, -12.6), (2, 1.2, 0.4), 'head')
    i = 0
    for wz in (-8, 8):
        for s in (-1, 1): wheel(v, i, s * 7.5, wz, 3.6, 3.4, s, 5); i += 1
    v.save(M, [f'w{k}' for k in range(i)])


def bike(name, paint, accent, sport=False):
    v = V(name, 128, 128); M = mats(paint, accent, (60, 60, 64) if sport else (190, 194, 200), seat=(30, 30, 32))
    r = 4.6
    v.c((-0.6, 5.5, -6), (1.2, 1.2, 14), 'black', rot=(-12, 0, 0), pivot=(0, 6, 1))                     # frame spine
    v.c((-2.2, 3.4, -2), (4.4, 5.6, 6), 'steel'); v.c((-1.8, 2.6, -1), (3.6, 1, 4), 'black')             # engine
    v.c((-0.7, 6, -8), (1.4, 1.4, 7), 'black', rot=(35, 0, 0), pivot=(0, 6.5, -6)); v.c((-0.6, 8.6, 0.5), (1.2, 1.2, 9), 'black')   # down tube + rails
    v.c((-2.6, 9, -6), (5.2, 3.4, 7), 'paint'); v.c((-2.2, 12.2, -5.5), (4.4, 0.6, 6), 'paint')          # tank
    v.c((-2, 10, 1), (4, 1.6, 7), 'seat'); v.c((-1.6, 11, 7.5), (3.2, 1.2, 4), 'paint' if sport else 'seat')
    v.c((-1.4, 8.5, 8), (2.8, 1.4, 6), 'paint')                                                          # tail
    v.c((-1, 9, 13.4), (2, 1, 0.4), 'tail')
    v.c((-0.4, 2.5, 4), (0.8, 0.8, 7), 'black', rot=(-8, 0, 0), pivot=(0, 3, 4)); v.c((1.8, 2, 4), (1, 1, 8), 'chrome', rot=(-8, 0, 0), pivot=(2, 2, 4))   # swingarm + exhaust
    for s in (-1, 1): v.c((s * 1.6 - 0.4, 3.5, -11.5), (0.8, 11, 0.8), 'chrome', rot=(-24, 0, 0), pivot=(s * 1.6, 9, -10))   # forks
    v.c((-4.5, 13.5, -9.5), (9, 0.7, 0.7), 'black'); v.c((-5, 13.2, -9.7), (1.4, 1.2, 1.2), 'rubber'); v.c((3.6, 13.2, -9.7), (1.4, 1.2, 1.2), 'rubber')
    if sport:
        v.c((-2.8, 7, -12), (5.6, 6.5, 4), 'paint', rot=(-20, 0, 0), pivot=(0, 7, -10)); v.c((-2, 12, -12.8), (4, 3.5, 0.5), 'glass', rot=(-30, 0, 0), pivot=(0, 12, -12))
        v.c((-1.5, 9.5, -13), (3, 1.4, 0.5), 'head'); v.c((-3, 4, -6), (6, 5, 6), 'accent')                # fairing
    else:
        v.c((-1.6, 11, -12.5), (3.2, 3, 2), 'chrome'); v.c((-1.2, 11.4, -12.8), (2.4, 2.2, 0.4), 'head')
    v.c((-0.7, 9.3, -15), (1.4, 0.6, 6), 'paint'); v.c((-0.7, 9.3, 6.5), (1.4, 0.6, 6), 'paint')          # fenders
    wheel(v, 0, 0, -13, r, 1.6, 1, 10 if sport else 12, sport, name='wf'); wheel(v, 0, 0, 10, r, 2.2, 1, 10 if sport else 12, sport, name='wr')
    v.save(M, ['wf', 'wr'])


if __name__ == '__main__':
    sedan(); police(); taxi(); muscle()
    sports('sports_gt', (200, 20, 24), (20, 20, 22)); sports('sports_rs', (20, 60, 170), (230, 230, 230), stripes=True); sports('sports_turbo', (240, 200, 30), (20, 20, 22), spokes=6)
    pickup(); armored_suv(); jeep(); atv()
    boxy('car_ambulance', (240, 240, 240), (200, 30, 30), L=70, W=34, H=9, box_from=0, box_h=17, extra=ambulance_extra)
    boxy('van', (236, 236, 236), (40, 80, 160), L=76, W=32, H=8, box_from=0, box_h=15, wbs=(-24, 24), extra=van_extra)
    boxy('school_bus', (244, 190, 20), (20, 20, 20), L=112, W=34, H=8, box_from=0, box_h=19, r=6.6, wbs=(-40, 38), cab_len=14, extra=bus_extra)
    boxy('fire_truck', (200, 24, 28), (236, 236, 236), L=100, W=34, H=9, box_from=0, box_h=17, r=6.6, wbs=(-34, 22, 36), cab_len=20, extra=fire_extra)
    boxy('armored_truck', (84, 92, 56), (40, 44, 30), L=58, W=36, H=9, box_from=0, box_h=17, r=7, wbs=(-18, 4, 20), cab_len=18, extra=armored_extra)
    bike('motorcycle', (20, 20, 24), (200, 200, 205)); bike('superbike_vortex', (40, 200, 60), (20, 20, 22), True); bike('superbike_blaze', (210, 30, 30), (240, 240, 240), True)
    print('vehicles done')
