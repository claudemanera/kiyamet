"""TWD 1.9 wild animals: detailed GeckoLib quadrupeds (legs, neck, head, jaw, tail, ears, mane/antlers/tusks),
painted fur textures (stripes, mane, scales, spots), walk/run/idle/attack animations, rotting walker variants,
meat + pelt items, fur equipment assets, spawn eggs and lang."""
import json, math, os, random, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from draw_farm import C, dark, light, mix
R = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources')
A = R + '/assets/kiyamet'
D = R + '/data/kiyamet'


class Beast:
    def __init__(s, name, tw=256, th=256):
        s.name, s.tw, s.th = name, tw, th
        s.bones, s.order, s.uvs = {}, [], []
        s.u = s.v = s.rowh = 0

    def bone(s, n, parent=None, pivot=(0, 0, 0), rot=None):
        b = {"name": n, "pivot": [round(x, 2) for x in pivot], "cubes": []}
        if parent: b["parent"] = parent
        if rot: b["rotation"] = list(rot)
        s.bones[n] = b; s.order.append(n); return b

    def cube(s, bone, o, sz, mat, rot=None, pivot=None, inflate=0):
        w, h, d = [max(0.3, v) for v in sz]
        uw, uh = int(2 * (w + d) + 0.999) + 1, int(h + d + 0.999) + 1
        if s.u + uw > s.tw: s.u = 0; s.v += s.rowh; s.rowh = 0
        uv = [s.u, s.v]; s.u += uw; s.rowh = max(s.rowh, uh)
        c = {"origin": [round(x, 2) for x in o], "size": [round(x, 2) for x in (w, h, d)], "uv": uv}
        if rot: c["rotation"] = list(rot); c["pivot"] = [round(x, 2) for x in (pivot or o)]
        if inflate: c["inflate"] = inflate
        s.bones[bone]["cubes"].append(c); s.uvs.append((uv, (w, h, d), mat, (uw, uh)))

    # ------------------------------------------------------------------ texture
    def paint(s, mats, zombie=False):
        im = Image.new('RGBA', (s.tw, s.th), (0, 0, 0, 0)); r = random.Random(s.name)
        for (u, v), (w, h, d), mat, (uw, uh) in s.uvs:
            base, kind = mats[mat]
            belly = mats.get('belly', (base, ''))[0]
            W, H, Dd = max(1, math.ceil(w)), max(1, math.ceil(h)), max(1, math.ceil(d))
            for yy in range(v, min(s.th, v + uh)):
                for xx in range(u, min(s.tw, u + uw)): im.putpixel((xx, yy), tuple(base) + (255,))

            def fill(x0, y0, ww, hh, face):
                for y in range(y0, y0 + hh):
                    for x in range(x0, x0 + ww):
                        if not (0 <= x < s.tw and 0 <= y < s.th): continue
                        c = list(base); n = r.randint(-7, 7); ly = (y - y0) / max(1, hh - 1)
                        if kind in ('fur', 'stripe', 'spots', 'mane', 'shag'):
                            if r.random() < 0.18: n += r.choice((-18, 16))            # strands
                            if face == 'top': n += 10
                            elif face == 'bottom': c = list(belly); n -= 4
                            elif face in ('side', 'front', 'back'):
                                n += int(10 - 22 * ly)                                    # light back, dark flank
                                if ly > 0.78 and kind != 'mane' and mat != 'leg': c = list(mix(base, belly, 0.6))
                        if kind == 'stripe' and face != 'bottom':
                            k = (x - x0) + int(2.2 * math.sin((y - y0) * 0.9 + x0)) if face != 'top' else (y - y0) + int(1.6 * math.sin(x * 0.8))
                            if k % 5 in (0,) or (k % 9 == 4 and r.random() < 0.7): c = list(mats.get('stripec', ((26, 20, 18), ''))[0]); n = r.randint(-3, 3)
                        elif kind == 'spots' and face in ('side', 'top') and r.random() < 0.06: c = list(light(base, 0.65))
                        elif kind == 'mane': n += r.randint(-16, 16); c = list(mix(base, (60, 30, 12), 0.25 * ((x * 7 + y * 3) % 4) / 3))
                        elif kind == 'scale':
                            cx, cy = (x - x0) % 3, (y - y0 + ((x - x0) // 3) % 2) % 3
                            if cx == 0 or cy == 0: n -= 22
                            elif cx == 1 and cy == 1: n += 14
                            if face == 'bottom': c = list(belly); n = r.randint(-5, 5) - (12 if (y - y0) % 2 else 0)
                            if face == 'top' and (x - x0) % 4 == 1 and (y - y0) % 2 == 0: n += 24
                        elif kind == 'horn':
                            n = r.randint(-5, 5) + int(18 * (1 - ly)) - (14 if (y - y0) % 3 == 0 else 0)
                        elif kind == 'eye':
                            c = [16, 12, 10] if not zombie else [196, 236, 120]
                            if (x - x0, y - y0) == (0, 0) and ww > 0: c = [250, 250, 240] if not zombie else [240, 255, 200]
                            n = 0
                        elif kind == 'nose': n = r.randint(-3, 3) - (10 if (x + y) % 2 else 0)
                        elif kind == 'teeth': n = r.randint(-6, 4)
                        elif kind == 'mouth': n = r.randint(-4, 4)
                        elif kind == 'metal':
                            n = r.randint(-4, 4) + (22 if (y - y0) == 0 else -16 if (y - y0) == hh - 1 else 0) + (10 if (x - x0) == 0 else -8 if (x - x0) == ww - 1 else 0)
                            if ww > 3 and hh > 3 and (x - x0, y - y0) in ((1, 1), (ww - 2, 1), (1, hh - 2), (ww - 2, hh - 2)): n += 30
                        elif kind == 'cloth': n = r.randint(-5, 5) + (-9 if (x + y) % 2 else 5) + (-10 if (x - x0) % 4 == 0 and face in ('front', 'back', 'side') else 0)
                        elif kind == 'glow': n = r.randint(-6, 30)
                        elif kind == 'skin': n = r.randint(-4, 4)
                        elif kind == 'hoof': n = r.randint(-4, 4) - (8 if (x - x0) == ww // 2 else 0)
                        col = [max(0, min(255, k + n)) for k in c[:3]]
                        if zombie and kind not in ('eye',): col = rot_px(col, r, x, y, kind)
                        im.putpixel((x, y), tuple(col) + (255,))
            fill(u + Dd, v, W, Dd, 'top'); fill(u + Dd + W, v, W, Dd, 'bottom')
            fill(u, v + Dd, Dd, H, 'side'); fill(u + Dd, v + Dd, W, H, 'front'); fill(u + Dd + W, v + Dd, Dd, H, 'side'); fill(u + 2 * Dd + W, v + Dd, W, H, 'back')
        if zombie:   # wounds: torn patches with blood and bone across the whole skin
            for _ in range(int(s.tw * s.th / 260)):
                cx, cy, rr = r.randrange(s.tw), r.randrange(s.th), r.uniform(1.2, 3.4)
                for y in range(int(cy - rr), int(cy + rr) + 1):
                    for x in range(int(cx - rr), int(cx + rr) + 1):
                        if not (0 <= x < s.tw and 0 <= y < s.th) or im.getpixel((x, y))[3] == 0: continue
                        dd = math.hypot(x - cx, y - cy)
                        if dd > rr: continue
                        old = im.getpixel((x, y))
                        if old[1] > old[0] + 30 and old[2] < 150 and old[0] > 150: continue  # keep glowing eyes
                        col = (226, 214, 190) if dd < rr * 0.3 and r.random() < 0.6 else (98 + r.randint(-14, 14), 22, 20) if dd < rr * 0.75 else (60, 30, 26)
                        im.putpixel((x, y), col + (255,))
        return im

    def save(s, mats, anims, scale_bounds=4):
        assert s.v + s.rowh <= s.th, (s.name, s.v + s.rowh)
        geo = lambda ident: {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": f"geometry.kiyamet.{ident}", "texture_width": s.tw,
               "texture_height": s.th, "visible_bounds_width": scale_bounds, "visible_bounds_height": scale_bounds, "visible_bounds_offset": [0, 1.5, 0]},
               "bones": [s.bones[n] for n in s.order]}]}
        for ident, zombie in ((s.name, False), (ZNAME.get(s.name), True)):
            if not ident or (not zombie and s.name in ONLY_ZOMBIE): continue
            json.dump(geo(ident), open(f'{A}/geckolib/models/entity/{ident}.geo.json', 'w'), indent=1)
            s.paint(mats, zombie).save(f'{A}/textures/entity/{ident}.png')
            json.dump({"format_version": "1.8.0", "animations": anims if not zombie else zanims(anims)},
                      open(f'{A}/geckolib/animations/entity/{ident}.animation.json', 'w'), indent=1)


def rot_px(c, r, x, y, kind):
    """walker skin: drained, grey-green, blotchy"""
    g = sum(c) / 3
    c = [g * 0.45 + c[0] * 0.42 - 6, g * 0.5 + c[1] * 0.38 + 8, g * 0.42 + c[2] * 0.36]
    if kind in ('teeth', 'horn'): c = [c[0] + 20, c[1] + 16, c[2]]
    if r.random() < 0.08: c = [k * 0.7 for k in c]
    return [max(0, min(255, int(k))) for k in c]


LAST_LEG = 8
ZNAME = {'lion': 'zombie_lion', 'tiger': 'zombie_tiger', 'brown_bear': 'zombie_bear', 'deer': 'zombie_deer', 'crocodile': 'zombie_crocodile', 'grey_wolf': 'zombie_wolf'}
ONLY_ZOMBIE = {'grey_wolf'}


def zanims(anims):
    """walkers limp: same clips, slower and with a lurch"""
    out = json.loads(json.dumps(anims))
    for n, a in out.items():
        if n.startswith('move'):
            a["animation_length"] = round(a["animation_length"] * 1.3, 2)
            for b in a["bones"].values():
                for ch in b.values():
                    for t in list(ch.keys()):
                        v = ch.pop(t); ch[f'{float(t) * 1.3:.2f}'] = v
            a["bones"].setdefault("head", {})["rotation"] = {"0.00": [10, 0, -12], f'{a["animation_length"] / 2:.2f}': [16, 0, 10], f'{a["animation_length"]:.2f}': [10, 0, -12]}
    return out


# ---------------------------------------------------------------------- the builder
def quad(name, *, L, W, H, legH, legT, clear=None, head, snout, neck=(0, 0), neck_len=0, tail, ears=(2, 2, 1), paws='paw', tw=256, th=256,
         body_mat='fur', head_mat=None, leg_mat=None, hump=0, splay=0, low_head=0):
    """L,W,H body; legH leg length; head (w,h,d); snout (w,h,d); tail (len, thick, droop deg)"""
    global LAST_LEG
    LAST_LEG = legH
    b = Beast(name, tw, th)
    y0 = legH if clear is None else clear
    z0 = -L / 2
    b.bone('root', pivot=(0, 0, 0))
    b.bone('body', 'root', pivot=(0, y0 + H / 2, 0))
    bm, hm, lm = body_mat, head_mat or body_mat, leg_mat or ('leg' if body_mat == 'fur' else body_mat)
    # torso: chest (front), belly, haunch (back) -> a shaped body instead of one box
    b.cube('body', (-W / 2, y0, z0), (W, H, L * 0.45), bm)                                         # chest
    b.cube('body', (-W / 2 + 0.5, y0 + 0.5, z0 + L * 0.42), (W - 1, H - 1.2, L * 0.25), bm)        # waist
    b.cube('body', (-W / 2 + 0.2, y0 + 0.2, z0 + L * 0.64), (W - 0.4, H - 0.4, L * 0.36), bm)     # haunch
    b.cube('body', (-W / 2 + 1, y0 - 0.6, z0 + 2), (W - 2, 0.8, L - 4), 'belly')                   # underside
    if hump: b.cube('body', (-W / 2 + 1.5, y0 + H, z0 + 1), (W - 3, hump, L * 0.4), bm)              # shoulder hump (bears, moose, boar ridge)
    # legs with upper/lower parts and paws/hooves
    for i, (side, front) in enumerate(((-1, True), (1, True), (-1, False), (1, False))):
        n = ('fl', 'fr', 'bl', 'br')[i]
        x = side * (W / 2 - legT / 2 - 0.3 + splay)
        z = z0 + (legT / 2 + 1.5 if front else L - legT / 2 - 1.5)
        top = y0 + H * 0.55
        b.bone(n, 'body', pivot=(x, top, z))
        ut = legT + (0.8 if not front else 0.4)
        b.cube(n, (x - ut / 2, y0 - 1.5, z - ut / 2), (ut, top - y0 + 1.5, ut), lm)                  # thigh / shoulder
        b.cube(n, (x - legT / 2, 1.2 if paws else 0, z - legT / 2), (legT, legH - 0.2, legT), lm)          # shin
        if paws == 'hoof': b.cube(n, (x - legT / 2 - 0.1, 0, z - legT / 2 - 0.1), (legT + 0.2, 1.4, legT + 0.2), 'hoof')
        elif paws == 'paw':
            b.cube(n, (x - legT / 2 - 0.4, 0, z - legT / 2 - 1), (legT + 0.8, 1.4, legT + 1.2), lm)
            for c in range(3): b.cube(n, (x - legT / 2 + c * (legT / 3) + 0.2, 0, z - legT / 2 - 1.4), (0.6, 0.6, 0.5), 'claw')
        elif paws == 'reptile':
            b.cube(n, (x - legT / 2 - 0.8, 0, z - legT / 2 - 1.6), (legT + 1.6, 0.9, legT + 1.8), 'scale')
            for c in range(4): b.cube(n, (x - legT / 2 - 0.6 + c * (legT + 0.8) / 3, 0, z - legT / 2 - 2.4), (0.5, 0.5, 0.9), 'claw')
    # neck + head
    ny = y0 + H * 0.7 + neck[1]
    b.bone('neck', 'body', pivot=(0, ny, z0 + 1))
    if neck_len:
        b.cube('neck', (-neck[0] / 2, ny - 1, z0 - neck_len + 1.5), (neck[0], neck[0] + 1, neck_len), hm, rot=(-35 + low_head, 0, 0), pivot=(0, ny, z0 + 1))
    hz = z0 - (neck_len * 0.8 if neck_len else 0) + 1
    hy = ny + (neck_len * 0.55 if neck_len else 0) - low_head * 0.12
    hw, hh, hd = head
    b.bone('head', 'neck', pivot=(0, hy, hz))
    b.cube('head', (-hw / 2, hy - hh / 2, hz - hd), (hw, hh, hd), hm)
    sw, sh, sd = snout
    b.cube('head', (-sw / 2, hy - hh / 2, hz - hd - sd), (sw, sh * 0.6, sd), hm)                   # upper muzzle
    b.cube('head', (-sw / 2 + 0.6, hy - hh / 2 + sh * 0.6 - 0.2, hz - hd - sd + 0.2), (sw - 1.2, sh * 0.4, sd - 0.4), hm)  # nose bridge
    b.cube('head', (-1.2, hy - hh / 2 + sh * 0.35, hz - hd - sd - 0.3), (2.4, 1.2, 0.5), 'nose')
    b.bone('jaw', 'head', pivot=(0, hy - hh / 2 + 0.2, hz - hd + 1))
    b.cube('jaw', (-sw / 2 + 0.3, hy - hh / 2 - 1.4, hz - hd - sd + 0.4), (sw - 0.6, 1.5, sd + 0.6), hm)
    b.cube('jaw', (-sw / 2 + 0.8, hy - hh / 2 + 0.05, hz - hd - sd + 0.6), (sw - 1.6, 0.3, sd - 0.4), 'mouth')
    for xx in (-sw / 2 + 0.8, sw / 2 - 1.3):
        b.cube('jaw', (xx, hy - hh / 2 + 0.1, hz - hd - sd + 0.5), (0.5, 0.8, 0.5), 'teeth')
    for side in (-1, 1):                                                                            # eyes + ears
        b.cube('head', (side * (hw / 2 - 1.6) - 0.6 + (0.6 if side > 0 else 0), hy + hh * 0.12, hz - hd - 0.1), (1.2, 1.0, 0.2), 'eye')
        ew, eh, ed = ears
        b.cube('head', (side * (hw / 2 - ew / 2 - 0.4) - ew / 2, hy + hh / 2, hz - hd * 0.45), (ew, eh, ed), hm,
               rot=(0, 0, side * -12), pivot=(side * (hw / 2 - 1), hy + hh / 2, hz - hd * 0.4))
    # tail in two segments
    tl, tt, droop = tail
    if tl:
        tz = -z0 - 0.5
        b.bone('tail', 'body', pivot=(0, y0 + H - 1.5, tz), rot=(-droop, 0, 0))
        b.cube('tail', (-tt / 2, y0 + H - 1.5 - tt / 2, tz), (tt, tt, tl * 0.55), bm)
        b.bone('tail2', 'tail', pivot=(0, y0 + H - 1.5, tz + tl * 0.55), rot=(-droop * 0.5, 0, 0))
        b.cube('tail2', (-tt / 2 + 0.15, y0 + H - 1.5 - tt / 2 + 0.15, tz + tl * 0.55 - 0.2), (tt - 0.3, tt - 0.3, tl * 0.45), bm)
    return b, (hw, hh, hd, hy, hz, sd, y0, z0)


def anims(gait=24, run=40, rear=False, jaw=34, tail_sway=12, croc=False, tl=1.0):
    def legs(amp, T):
        a, h = f'{T:.2f}', f'{T / 2:.2f}'
        q = f'{T / 4:.2f}'; q3 = f'{3 * T / 4:.2f}'
        fw = {"0.00": [amp, 0, 0], h: [-amp, 0, 0], a: [amp, 0, 0]}
        bw = {"0.00": [-amp, 0, 0], h: [amp, 0, 0], a: [-amp, 0, 0]}
        if croc:
            fw = {"0.00": [0, amp, 0], h: [0, -amp, 0], a: [0, amp, 0]}; bw = {"0.00": [0, -amp, 0], h: [0, amp, 0], a: [0, -amp, 0]}
        return {"fl": {"rotation": fw}, "br": {"rotation": fw}, "fr": {"rotation": bw}, "bl": {"rotation": bw},
                "body": {"position": {"0.00": [0, 0, 0], q: [0, 0.4 if not croc else 0, 0], h: [0, 0, 0], q3: [0, 0.4 if not croc else 0, 0], a: [0, 0, 0]},
                         "rotation": {"0.00": [0, 0, 0], q: [1.5, (6 if croc else 0), 0], h: [0, 0, 0], q3: [-1.5, (-6 if croc else 0), 0], a: [0, 0, 0]}},
                "tail": {"rotation": {"0.00": [0, -tail_sway, 0], h: [0, tail_sway, 0], a: [0, -tail_sway, 0]}},
                "tail2": {"rotation": {"0.00": [0, -tail_sway * 0.8, 0], h: [0, tail_sway * 0.8, 0], a: [0, -tail_sway * 0.8, 0]}},
                "head": {"rotation": {"0.00": [0, 0, 0], q: [3, 0, 0], h: [0, 0, 0], q3: [3, 0, 0], a: [0, 0, 0]}}}
    run_b = legs(run, 0.5)
    run_b["body"]["rotation"] = {"0.00": [-5, 0, 0], "0.25": [5, 0, 0], "0.50": [-5, 0, 0]}
    run_b["body"]["position"] = {"0.00": [0, 0, 0], "0.12": [0, 1.2, 0], "0.25": [0, 0, 0], "0.38": [0, 1.2, 0], "0.50": [0, 0, 0]}
    idle = {"body": {"position": {"0.00": [0, 0, 0], "1.50": [0, 0.25, 0], "3.00": [0, 0, 0]}},
            "head": {"rotation": {"0.00": [0, 0, 0], "1.00": [4, 10, 0], "2.00": [-2, -8, 0], "3.00": [0, 0, 0]}},
            "jaw": {"rotation": {"0.00": [0, 0, 0], "2.40": [0, 0, 0], "2.60": [-6, 0, 0], "3.00": [0, 0, 0]}},
            "tail": {"rotation": {"0.00": [0, -tail_sway * 0.6, 0], "1.50": [0, tail_sway * 0.6, 0], "3.00": [0, -tail_sway * 0.6, 0]}},
            "tail2": {"rotation": {"0.00": [0, -tail_sway, 0], "1.50": [0, tail_sway, 0], "3.00": [0, -tail_sway, 0]}}}
    atk = {"jaw": {"rotation": {"0.00": [0, 0, 0], "0.15": [jaw, 0, 0], "0.40": [0, 0, 0], "0.60": [0, 0, 0]}},
           "head": {"rotation": {"0.00": [0, 0, 0], "0.15": [-14, 0, 0], "0.35": [16, 0, 0], "0.60": [0, 0, 0]},
                    "position": {"0.00": [0, 0, 0], "0.20": [0, 0, -3], "0.60": [0, 0, 0]}}}
    if rear:   # bears: rear up and swipe
        atk["body"] = {"rotation": {"0.00": [0, 0, 0], "0.20": [-28, 0, 0], "0.45": [6, 0, 0], "0.70": [0, 0, 0]}}
        atk["fl"] = {"rotation": {"0.00": [0, 0, 0], "0.20": [-80, 0, -15], "0.45": [20, 0, 0], "0.70": [0, 0, 0]}}
        atk["fr"] = {"rotation": {"0.00": [0, 0, 0], "0.25": [-90, 0, 15], "0.50": [25, 0, 0], "0.70": [0, 0, 0]}}
    elif not croc:  # cats/boar: pounce
        atk["body"] = {"position": {"0.00": [0, 0, 0], "0.20": [0, 2, -4], "0.50": [0, 0, 0]}, "rotation": {"0.00": [0, 0, 0], "0.15": [-12, 0, 0], "0.40": [8, 0, 0], "0.60": [0, 0, 0]}}
        atk["fl"] = {"rotation": {"0.00": [0, 0, 0], "0.15": [-60, 0, 0], "0.40": [30, 0, 0], "0.60": [0, 0, 0]}}
        atk["fr"] = {"rotation": {"0.00": [0, 0, 0], "0.15": [-60, 0, 0], "0.40": [30, 0, 0], "0.60": [0, 0, 0]}}
    else:  # croc: death-roll snap
        atk["body"] = {"rotation": {"0.00": [0, 0, 0], "0.20": [0, 0, 25], "0.45": [0, 0, -20], "0.70": [0, 0, 0]}}
        atk["jaw"]["rotation"] = {"0.00": [0, 0, 0], "0.12": [jaw, 0, 0], "0.25": [0, 0, 0], "0.70": [0, 0, 0]}
    lie = max(1.0, LAST_LEG - 1.2)
    sit = {"body": {"position": {"0.00": [0, -lie, 0]}}, "fl": {"rotation": {"0.00": [-82, 0, 0]}}, "fr": {"rotation": {"0.00": [-82, 0, 0]}},
           "bl": {"rotation": {"0.00": [82, 0, 0]}}, "br": {"rotation": {"0.00": [82, 0, 0]}},
           "head": {"rotation": {"0.00": [8, 0, 0], "2.00": [10, 12, 0], "4.00": [8, 0, 0]}}, "tail": {"rotation": {"0.00": [0, 25, 0]}}}
    return {"misc.sit": {"loop": True, "animation_length": 4.0, "bones": sit},
            "misc.idle": {"loop": True, "animation_length": 3.0, "bones": idle},
            "move.walk": {"loop": True, "animation_length": 1.0, "bones": legs(gait, 1.0)},
            "move.run": {"loop": True, "animation_length": 0.5, "bones": run_b},
            "attack.swing": {"loop": False, "animation_length": 0.7, "bones": atk}}


def M(base, belly, kind='fur', **extra):
    m = {'fur': (base, kind), 'belly': (belly, 'fur'), 'eye': ((10, 10, 10), 'eye'), 'nose': ((40, 30, 28), 'nose'), 'teeth': ((236, 228, 208), 'teeth'),
         'mouth': ((120, 40, 44), 'mouth'), 'claw': ((40, 36, 34), 'horn'), 'hoof': ((46, 38, 32), 'hoof'), 'horn': ((214, 200, 170), 'horn'),
         'antler': ((150, 118, 80), 'horn'), 'mane': (dark(base, 0.62), 'mane'), 'scale': (base, 'scale'), 'leg': (base, kind), 'pale': (light(base, 0.55), 'fur'), 'dark': (dark(base, 0.5), 'fur')}
    m.update(extra); return m


def lion():
    b, (hw, hh, hd, hy, hz, sd, y0, z0) = quad('lion', L=28, W=11, H=11, legH=11, legT=3.6, head=(8, 8, 7), snout=(5, 4, 4), neck=(6, -1), neck_len=0, tail=(16, 1.4, 50), ears=(2, 2, 1))
    b.cube('head', (-hw / 2 - 2.5, hy - hh / 2 - 2.5, hz - hd + 2.5), (hw + 5, hh + 5, 6), 'mane')            # big mane around the face
    b.cube('head', (-hw / 2 - 2.5, hy - hh / 2 - 4, hz - hd + 3), (hw + 5, 2, 5), 'mane')                     # beard
    b.cube('body', (-6, y0 + 4, z0 - 1), (12, 8, 7), 'mane')                                              # mane over the shoulders
    b.cube('body', (-5.5, y0 + 1, z0 - 1.5), (11, 4, 6), 'mane')                                         # chest mane
    b.cube('tail2', (-1.3, y0 + 11 - 1.5 - 1.3, -z0 - 0.5 + 16 - 0.5), (2.6, 2.6, 2.6), 'mane')                 # tail tuft
    b.save(M((204, 158, 88), (232, 204, 150)), anims(gait=22, run=42, tail_sway=10))


def tiger():
    b, (hw, hh, hd, hy, hz, sd, y0, z0) = quad('tiger', L=30, W=11, H=10, legH=10, legT=3.8, head=(9, 8, 7), snout=(5.5, 4, 3.5), neck=(6, -1), tail=(18, 1.6, 30), ears=(2, 2, 1))
    b.cube('head', (-hw / 2 - 1, hy - hh / 2 - 1, hz - hd + 1), (1.2, 4, 4), 'pale')                     # cheek ruffs
    b.cube('head', (hw / 2 - 0.2, hy - hh / 2 - 1, hz - hd + 1), (1.2, 4, 4), 'pale')
    b.cube('head', (-2.5, hy - hh / 2 - 0.4, hz - hd - 0.3), (5, 2.2, 0.4), 'pale')                      # white muzzle patch
    b.save(M((228, 128, 40), (248, 240, 226), 'stripe'), anims(gait=22, run=44, tail_sway=14))


def bear(name, base, belly, big=1.0, frost=False):
    s = big
    b, (hw, hh, hd, hy, hz, sd, y0, z0) = quad(name, L=26 * s, W=15 * s, H=13 * s, legH=10 * s, legT=5.5 * s, head=(10 * s, 9 * s, 8 * s), snout=(6 * s, 5 * s, 4 * s),
                                               neck=(8, 0), tail=(3, 3, 60), ears=(2.5, 2.5, 1.2), hump=3 * s, tw=256 if s <= 1 else 512, th=256)
    b.cube('body', (-7.8 * s, y0 + 2, z0 + 1), (15.6 * s, 4, 10 * s), 'shag')                                  # shaggy chest
    mats = M(base, belly, shag=(dark(base, 0.85) if not frost else (226, 234, 246), 'mane'), nose=((24, 22, 22), 'nose'))
    if frost: mats['claw'] = ((30, 30, 34), 'horn')
    b.save(mats, anims(gait=18, run=34, rear=True, jaw=30, tail_sway=4), scale_bounds=5)


def crocodile():
    b, (hw, hh, hd, hy, hz, sd, y0, z0) = quad('crocodile', L=30, W=12, H=6, legH=4, legT=3, clear=3, head=(9, 5, 6), snout=(6, 4, 13), neck=(8, -3.2), tail=(30, 6, 4),
                                               ears=(0.3, 0.3, 0.3), paws='reptile', body_mat='scale', splay=2.2)
    for z in range(-13, 14, 3):                                                                        # back ridges (osteoderms)
        for xx in (-3.5, -1, 1.5):
            b.cube('body', (xx, 9, z), (1.5, 1.1, 1.8), 'scale')
    for z in (2, 8, 14, 20, 26):
        b.cube('tail' if z < 16 else 'tail2', (-0.8, 9 - 0.1 * z + (0 if z < 16 else 0), 15 + z * 0.5), (1.6, 1.5, 2), 'scale')
    for i in range(6):                                                                               # rows of teeth along the snout
        for side in (-1, 1):
            b.cube('head', (side * 2.6 - 0.25, hy - hh / 2 - 0.6, hz - hd - 12 + i * 2), (0.5, 0.8, 0.5), 'teeth')
    b.cube('head', (-3.2, hy + hh / 2 - 0.6, hz - hd + 1), (2, 1.4, 2), 'scale')                       # raised eye bumps
    b.cube('head', (1.2, hy + hh / 2 - 0.6, hz - hd + 1), (2, 1.4, 2), 'scale')
    b.save(M((74, 94, 52), (196, 192, 140), 'scale'), anims(gait=26, run=34, croc=True, jaw=40, tail_sway=20), scale_bounds=5)


def boar():
    b, (hw, hh, hd, hy, hz, sd, y0, z0) = quad('wild_boar', L=19, W=10, H=10, legH=6, legT=3, head=(7, 7, 7), snout=(4.5, 4, 4), neck=(6, -1.5), tail=(4, 1, 70), ears=(2, 2.5, 0.8),
                                               paws='hoof', hump=1.5, low_head=10)
    b.cube('body', (-1, y0 + 11.2, z0), (2, 2, 14), 'shag')                                                # bristly ridge
    for side in (-1, 1):                                                                               # tusks
        b.cube('jaw', (side * 2.3 - 0.4, hy - hh / 2 - 0.3, hz - hd - 3.4), (0.8, 2.6, 0.8), 'horn', rot=(-20, 0, side * 15), pivot=(side * 2.3, hy - hh / 2, hz - hd - 3))
    b.cube('head', (-1.8, hy - hh / 2 + 0.2, hz - hd - 4.4), (3.6, 2.6, 0.5), 'nose')                    # flat pig snout
    b.save(M((86, 66, 54), (120, 98, 80), shag=((52, 40, 34), 'mane'), nose=((150, 104, 96), 'nose')), anims(gait=26, run=44, jaw=20, tail_sway=20))


def deer():
    b, (hw, hh, hd, hy, hz, sd, y0, z0) = quad('deer', L=19, W=8, H=8, legH=14, legT=2.2, head=(5, 5, 5), snout=(3.4, 3, 4), neck=(4, 0), neck_len=9, tail=(3, 2, 50),
                                               ears=(1.5, 3, 0.6), paws='hoof')
    ax = hy + 2.5
    for side in (-1, 1):                                                                               # antlers: beam + tines
        x = side * 1.8
        b.cube('head', (x - 0.5, ax, hz - 3), (1, 5, 1), 'antler', rot=(0, 0, side * -25), pivot=(x, ax, hz - 2.5))
        b.cube('head', (x + side * 2 - 0.4, ax + 4, hz - 3), (0.8, 4.5, 0.8), 'antler', rot=(-15, 0, side * -45), pivot=(x + side * 2, ax + 4, hz - 2.5))
        b.cube('head', (x + side * 1.6 - 0.4, ax + 3, hz - 4), (0.8, 3, 0.8), 'antler', rot=(30, 0, side * -10), pivot=(x + side * 1.6, ax + 3, hz - 3.5))
        b.cube('head', (x + side * 3.6 - 0.4, ax + 7, hz - 3), (0.8, 3, 0.8), 'antler', rot=(10, 0, side * -10), pivot=(x + side * 3.6, ax + 7, hz - 2.5))
    b.cube('tail', (-1.2, y0 + 8 - 1.5 - 0.6, -z0 + 0.5), (2.4, 1, 2.2), 'pale')                           # white flag tail
    b.save(M((156, 104, 62), (230, 214, 190), 'spots'), anims(gait=26, run=50, jaw=12, tail_sway=8))


def moose():
    b, (hw, hh, hd, hy, hz, sd, y0, z0) = quad('moose', L=27, W=12, H=13, legH=18, legT=3.6, head=(7, 7, 7), snout=(5.5, 5.5, 7), neck=(7, 0), neck_len=7, tail=(2, 2, 60),
                                               ears=(1.6, 3, 0.8), paws='hoof', hump=4, tw=256, th=256)
    b.cube('head', (-1.2, hy - hh / 2 - 4, hz - hd + 1), (2.4, 4, 2.5), 'dark')                          # dewlap ("bell")
    ax = hy + hh / 2 - 0.5
    for side in (-1, 1):                                                                               # broad palmate antlers
        x = side * 3.5
        b.cube('head', (x - (0 if side > 0 else 8), ax, hz - 4), (8, 1.2, 6), 'antler', rot=(0, 0, side * 18), pivot=(x, ax, hz - 1))
        b.cube('head', (x + side * 6 - (0 if side > 0 else 4), ax + 2.5, hz - 4.5), (4, 0.9, 7), 'antler', rot=(0, 0, side * 40), pivot=(x + side * 6, ax + 2.5, hz - 1))
        for t in range(4):
            b.cube('head', (x + side * (2 + t * 2) - 0.4, ax + 1.5 + t * 0.6, hz - 4.2 + (t % 2) * 4.5), (0.8, 3, 0.8), 'antler')
    b.save(M((72, 52, 36), (110, 92, 72), shag=((50, 36, 26), 'mane')), anims(gait=20, run=34, jaw=14, tail_sway=4), scale_bounds=6)


def dog(name, base, belly, saddle=None, ears_up=True, bulky=1.0, legH=8, wide_head=1.0):
    k = bulky
    b, (hw, hh, hd, hy, hz, sd, y0, z0) = quad(name, L=15 * k, W=6.5 * k, H=6.5 * k, legH=legH, legT=2.2 * k, head=(6 * k * wide_head, 5.5, 5), snout=(3.4 * k * wide_head, 3, 3.6 if wide_head == 1 else 2.8), neck=(4, 0.5), neck_len=4,
                                               tail=(9, 1.5, -30 if name == 'husky' else 35), ears=(1.6, 2.6 if ears_up else 1.4, 0.8), tw=128, th=128)
    if saddle: b.cube('body', (-3.4 * k, y0 + 6.5 * k - 0.3, z0 + 3), (6.8 * k, 0.8, 9 * k), 'saddle')                       # dark back
    if wide_head > 1:                                                                                                      # pitbull: big jaw muscles, broad chest
        b.cube('head', (-hw / 2 - 0.6, hy - hh / 2, hz - hd + 0.5), (hw + 1.2, 3, 3.5), 'fur')
        b.cube('body', (-W2 / 2, y0 - 0.5, z0 - 0.5), (W2, 5.5, 4), 'fur') if (W2 := 6.5 * k + 1.5) else None
    m = M(base, belly, saddle=(saddle or base, 'fur'), collar=((180, 40, 40), 'teeth'), tag=((230, 200, 80), 'teeth'))
    b.save(m, anims(gait=30, run=46, jaw=26, tail_sway=26), scale_bounds=3)


def cat():
    b, (hw, hh, hd, hy, hz, sd, y0, z0) = quad('stray_cat', L=10, W=4.5, H=4.5, legH=5, legT=1.5, head=(4.6, 4, 3.6), snout=(2.2, 1.6, 1.2), neck=(3, 0.6), tail=(10, 1, -40),
                                               ears=(1.3, 1.6, 0.6), tw=64, th=128)
    b.save(M((214, 140, 70), (244, 226, 200), 'stripe', stripec=((150, 84, 34), '')), anims(gait=26, run=40, jaw=14, tail_sway=18), scale_bounds=2)


if __name__ == '__main__':
  os.makedirs(f'{A}/geckolib/models/entity', exist_ok=True)
  dog('german_shepherd', (176, 120, 64), (214, 178, 120), saddle=(40, 32, 28)); dog('husky', (150, 156, 166), (246, 246, 250), saddle=(90, 94, 104))
  dog('rottweiler', (34, 28, 26), (150, 90, 44), bulky=1.15, ears_up=False); cat()
  dog('pitbull', (30, 30, 34), (62, 62, 68), bulky=1.15, ears_up=True, legH=6.5, wide_head=1.25)
  dog('grey_wolf', (156, 152, 146), (214, 208, 198), saddle=(96, 92, 88))
  lion(); tiger(); bear('brown_bear', (104, 70, 44), (130, 94, 64)); bear('ice_bear', (236, 240, 246), (214, 222, 234), big=1.12, frost=True)
  crocodile(); boar(); deer(); moose()

  # ---------------------------------------------------------------------- items: meat, pelts, fur equipment, eggs, lang
  def meat(c, raw, fat, bone=True):
      c.blob(8, 8.5, 5.5, 4.2, raw, rot=-0.5)
      c.blob(6.5, 7.5, 2.4, 1.6, fat, rot=-0.5)
      for (x, y) in ((9, 9), (10, 7), (7, 10)): c.px(x, y, dark(raw, 0.8))
      if bone:
          for i in range(4): c.px(12 + i // 2, 4 + i, (236, 228, 206)); c.px(13 + i // 2, 4 + i, (210, 200, 176))
          c.px(14, 3, (246, 240, 224)); c.px(15, 4, (246, 240, 224))

  MEATS = {'venison': ((150, 40, 40), (210, 150, 150), 'Venison'), 'bear_meat': ((130, 34, 30), (230, 210, 190), 'Bear Meat'), 'boar_meat': ((190, 70, 70), (240, 220, 210), 'Boar Meat'),
           'moose_meat': ((120, 30, 32), (220, 190, 180), 'Moose Meat'), 'big_cat_meat': ((170, 60, 50), (236, 200, 180), 'Big Cat Meat'), 'croc_meat': ((220, 170, 160), (246, 230, 220), 'Crocodile Meat')}
  L = json.load(open(A + '/lang/en_us.json'))
  def item(k, t):
      json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{k}"}}, open(f'{A}/models/item/{k}.json', 'w'))
      json.dump({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{k}"}}, open(f'{A}/items/{k}.json', 'w'))
      L['item.kiyamet.' + k] = t
  for k, (raw, fat, t) in MEATS.items():
      c = C(); meat(c, raw, fat, k not in ('croc_meat',)); c.outline(); c.save(f'{A}/textures/item/{k}.png'); item(k, 'Raw ' + t)
      c = C(); meat(c, mix(raw, (120, 70, 40), 0.65), mix(fat, (200, 150, 90), 0.5), k not in ('croc_meat',))
      for (x, y) in ((6, 9), (8, 7), (10, 9)): c.px(x, y, (80, 46, 26))
      c.outline(); c.save(f'{A}/textures/item/cooked_{k}.png'); item('cooked_' + k, ('Grilled ' if k != 'venison' else 'Roast ') + t)
      json.dump({"type": "minecraft:smelting", "category": "food", "ingredient": f"kiyamet:{k}", "result": {"id": f"kiyamet:cooked_{k}"}, "experience": 0.35, "cookingtime": 200},
                open(f'{D}/recipe/cooked_{k}.json', 'w'), indent=1)
      for kind, time in (('smoking', 100), ('campfire_cooking', 600)):
          json.dump({"type": f"minecraft:{kind}", "category": "food", "ingredient": f"kiyamet:{k}", "result": {"id": f"kiyamet:cooked_{k}"}, "experience": 0.35, "cookingtime": time},
                    open(f'{D}/recipe/cooked_{k}_{kind}.json', 'w'), indent=1)
  PELTS = {'lion_pelt': ('lion', 'Lion Pelt'), 'tiger_pelt': ('tiger', 'Tiger Pelt'), 'bear_pelt': ('bear', 'Bear Pelt'), 'ice_bear_pelt': ('ice_bear', 'Ice Bear Pelt')}
  BASES = ["leather", "chainmail", "iron", "gold", "diamond", "netherite", "copper", "turtle_scute", "armadillo_scute"]
  for k, (fur, t) in PELTS.items():
      item(k, t)
      for base in BASES:
          lay = lambda: [{"texture": f"minecraft:{base}"}, {"texture": f"kiyamet:fur_{fur}"}]
          json.dump({"layers": {"humanoid": lay(), "humanoid_leggings": lay()}}, open(f'{A}/equipment/fur_{fur}_{base}.json', 'w'), indent=1)
  from draw_eggs import egg
  EGGC = {'lion': ((204, 158, 88), (120, 74, 34)), 'tiger': ((228, 128, 40), (24, 20, 18)), 'brown_bear': ((104, 70, 44), (60, 40, 26)), 'ice_bear': ((236, 240, 246), (150, 196, 236)),
          'crocodile': ((74, 94, 52), (196, 192, 140)), 'wild_boar': ((86, 66, 54), (210, 200, 180)), 'deer': ((156, 104, 62), (240, 230, 214)), 'moose': ((72, 52, 36), (170, 140, 100)),
          'zombie_lion': ((130, 140, 100), (110, 30, 26)), 'zombie_tiger': ((150, 130, 90), (110, 30, 26)), 'zombie_bear': ((90, 92, 70), (110, 30, 26)), 'zombie_deer': ((120, 116, 90), (110, 30, 26)),
          'german_shepherd': ((176, 120, 64), (40, 32, 28)), 'husky': ((150, 156, 166), (246, 246, 250)), 'rottweiler': ((34, 28, 26), (150, 90, 44)), 'stray_cat': ((214, 140, 70), (150, 84, 34)), 'pitbull': ((30, 30, 34), (200, 200, 206)),
          'zombie_wolf': ((120, 124, 104), (110, 30, 26)), 'zombie_crocodile': ((84, 96, 70), (110, 30, 26))}
  TITLES = {'lion': 'Lion', 'tiger': 'Tiger', 'brown_bear': 'Brown Bear', 'ice_bear': 'Ice Bear', 'crocodile': 'Crocodile', 'wild_boar': 'Wild Boar', 'deer': 'Deer', 'moose': 'Moose',
            'zombie_lion': 'Walker Lion', 'zombie_tiger': 'Walker Tiger', 'zombie_bear': 'Walker Bear', 'zombie_deer': 'Walker Deer',
            'german_shepherd': 'German Shepherd', 'husky': 'Husky', 'rottweiler': 'Rottweiler', 'stray_cat': 'Stray Cat', 'pitbull': 'Black Pitbull', 'zombie_wolf': 'Walker Wolf', 'zombie_crocodile': 'Walker Crocodile'}
  for n, (base, spot) in EGGC.items():
      egg(n, base, spot, 'zombie' if n.startswith('zombie') else None)
      item(n + '_spawn_egg', TITLES[n] + ' Spawn Egg')
      L['entity.kiyamet.' + n] = TITLES[n]
  json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
  print('animals done')
