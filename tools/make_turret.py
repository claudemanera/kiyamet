"""3D auto turret: GeckoLib geo model + painted 64x64 box-UV texture + scan animation."""
import json, os, random
from PIL import Image
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')
cubes = []   # (bone, origin, size, mat, rotation(optional pivot,rot))
bones = {}
def bone(name, parent=None, pivot=(0, 0, 0), rot=None):
    b = {"name": name, "pivot": list(pivot), "cubes": []}
    if parent: b["parent"] = parent
    if rot: b["rotation"] = list(rot)
    bones[name] = b
    return b
MAT = {'steel': ((96, 102, 112), (140, 148, 160), (64, 68, 76)), 'dark': ((46, 48, 54), (70, 74, 82), (28, 30, 34)),
       'olive': ((86, 96, 58), (116, 128, 80), (60, 68, 40)), 'brass': ((176, 136, 52), (218, 182, 84), (120, 88, 30)),
       'red': ((180, 40, 36), (230, 80, 70), (110, 20, 20)), 'glass': ((60, 160, 200), (130, 220, 250), (30, 90, 120)), 'black': ((22, 22, 26), (40, 40, 46), (12, 12, 14))}
U = [0, 0]; ROWH = [0]
uvs = []
def cube(bn, origin, size, mat, inflate=0):
    w, h, d = size
    uw, uh = int(2 * (w + d) + 0.999), int(h + d + 0.999)
    if U[0] + uw > 64: U[0] = 0; U[1] += ROWH[0]; ROWH[0] = 0
    uv = [U[0], U[1]]; U[0] += uw; ROWH[0] = max(ROWH[0], uh)
    c = {"origin": list(origin), "size": list(size), "uv": uv}
    if inflate: c["inflate"] = inflate
    bones[bn]["cubes"].append(c); uvs.append((uv, size, mat))

bone('root')
bone('legs', 'root', (0, 6, 0))
for i, ang in enumerate((0, 120, 240)):
    b = bone(f'leg{i}', 'legs', (0, 6, 0), (0, ang, 0))
    lb = bone(f'leg{i}_s', f'leg{i}', (0, 6, 0), (35, 0, 0))
    cube(f'leg{i}_s', (-0.5, 0, -0.5), (1, 7, 1), 'dark')
    cube(f'leg{i}', (-1, 0, 4.5), (2, 1, 2), 'black')
bone('mount', 'root', (0, 6, 0))
cube('mount', (-3, 5, -3), (6, 2, 6), 'olive')
cube('mount', (-1, 7, -1), (2, 3, 2), 'steel')
bone('head', 'mount', (0, 11, 0))
cube('head', (-3, 10, -4), (6, 5, 8), 'olive')
cube('head', (-4, 9.5, 3), (8, 6, 1), 'dark')           # shield
cube('head', (3, 10, -2), (2, 4, 4), 'brass')           # ammo box
cube('head', (-1, 15, -2), (2, 2, 3), 'black')          # sight
cube('head', (-0.5, 15.5, 1), (1, 1, 1), 'red')         # sight lens
bone('barrels', 'head', (0, 12.5, 4))
cube('barrels', (-2, 11.5, 4), (1.5, 1.5, 9), 'steel')
cube('barrels', (0.5, 11.5, 4), (1.5, 1.5, 9), 'steel')
cube('barrels', (-2.5, 11, 12), (5, 2.5, 1.5), 'black')  # muzzle brake
cube('head', (-3, 10, -5), (6, 4, 1), 'black')          # back plate
geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": "geometry.kiyamet.auto_turret", "texture_width": 64, "texture_height": 64,
       "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]}, "bones": list(bones.values())}]}
json.dump(geo, open(f'{A}/geckolib/models/entity/auto_turret.geo.json', 'w'), indent=1)

im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); r = random.Random(9)
def fill(x0, y0, w, h, col, face):
    base, hi, lo = col
    for y in range(int(y0), int(y0 + max(1, round(h)))):
        for x in range(int(x0), int(x0 + max(1, round(w)))):
            if not (0 <= x < 64 and 0 <= y < 64): continue
            c = base
            if face == 'top': c = hi
            elif face == 'bottom': c = lo
            if y == int(y0) and face not in ('top', 'bottom'): c = hi
            if y == int(y0 + max(1, round(h))) - 1 and face not in ('top', 'bottom'): c = lo
            v = r.randint(-6, 6)
            im.putpixel((x, y), tuple(max(0, min(255, k + v)) for k in c) + (255,))
for (u, v), (w, h, d), mat in uvs:
    col = MAT[mat]; w, h, d = max(1, round(w)), max(1, round(h)), max(1, round(d))
    fill(u + d, v, w, d, col, 'top'); fill(u + d + w, v, w, d, col, 'bottom')
    fill(u, v + d, d, h, col, 'side'); fill(u + d, v + d, w, h, col, 'front'); fill(u + d + w, v + d, d, h, col, 'side'); fill(u + 2 * d + w, v + d, w, h, col, 'back')
im.save(f'{A}/textures/entity/auto_turret.png')
anim = {"format_version": "1.8.0", "animations": {
    "misc.idle": {"loop": True, "animation_length": 6.0, "bones": {"head": {"rotation": {"0.0": [0, 0, 0], "1.5": [0, 35, 0], "3.0": [0, 0, 0], "4.5": [0, -35, 0], "6.0": [0, 0, 0]}}}},
    "move.walk": {"loop": True, "animation_length": 0.3, "bones": {"barrels": {"position": {"0.0": [0, 0, 0], "0.08": [0, 0, -1.5], "0.3": [0, 0, 0]}}}},
    "attack.swing": {"loop": False, "animation_length": 0.3, "bones": {"barrels": {"position": {"0.0": [0, 0, 0], "0.08": [0, 0, -1.5], "0.3": [0, 0, 0]}}}}}}
json.dump(anim, open(f'{A}/geckolib/animations/entity/auto_turret.animation.json', 'w'), indent=1)
print('turret', len(uvs), 'cubes, uv rows end at', U[1] + ROWH[0])
