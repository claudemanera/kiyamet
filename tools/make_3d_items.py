"""TWD 1.9: real 3D hand models. Every handheld TWD item (all guns, their attachments, swords, tools, gadgets) is
turned from a flat sprite into a thick voxel model (one element per run of pixels, textured from the sprite), and guns
get a recoil animation: while the gun is cooling down after a shot the model kicks up and back in your hand."""
import glob, json, os
from PIL import Image
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')

DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}
RECOIL = json.loads(json.dumps(DISPLAY))
RECOIL["firstperson_righthand"] = {"rotation": [0, -90, 38], "translation": [1.13, 3.6, 2.6], "scale": [0.68, 0.68, 0.68]}
RECOIL["firstperson_lefthand"] = {"rotation": [0, 90, -38], "translation": [1.13, 3.6, 2.6], "scale": [0.68, 0.68, 0.68]}
RECOIL["thirdperson_righthand"] = {"rotation": [0, -90, 65], "translation": [0, 4.6, 1.5], "scale": [0.85, 0.85, 0.85]}
RECOIL["thirdperson_lefthand"] = {"rotation": [0, 90, -65], "translation": [0, 4.6, 1.5], "scale": [0.85, 0.85, 0.85]}


def voxel(tex_id, z0, z1, tint):
    im = Image.open(f'{A}/textures/{tex_id.split(":")[1]}.png').convert('RGBA')
    W, H = im.size
    px = 16 / W
    op = lambda x, y: 0 <= x < W and 0 <= y < H and im.getpixel((x, y))[3] > 16
    els = []
    for y in range(H):
        x = 0
        while x < W:
            if not op(x, y): x += 1; continue
            x1 = x
            while x1 < W and op(x1, y): x1 += 1
            u0, u1, v0, v1 = x * px, x1 * px, y * px, (y + 1) * px
            f = lambda uv: dict({"uv": [round(k, 4) for k in uv], "texture": "#0"}, **({"tintindex": 0} if tint else {}))
            faces = {"south": f([u0, v0, u1, v1]), "north": f([u1, v0, u0, v1])}
            if not all(op(i, y - 1) for i in range(x, x1)): faces["up"] = f([u0, v0, u1, v0 + px * 0.5])
            if not all(op(i, y + 1) for i in range(x, x1)): faces["down"] = f([u0, v1 - px * 0.5, u1, v1])
            faces["west"] = f([u0, v0, u0 + px, v1]); faces["east"] = f([u1 - px, v0, u1, v1])
            els.append({"from": [round(x * px, 4), round(16 - (y + 1) * px, 4), z0], "to": [round(x1 * px, 4), round(16 - y * px, 4), z1], "faces": faces})
            x = x1
    return els


def write(path, tex_id, z0, z1, tint, display):
    m = {"gui_light": "front", "textures": {"0": tex_id, "particle": tex_id}, "elements": voxel(tex_id, z0, z1, tint), "display": display}
    json.dump(m, open(path, 'w'), separators=(',', ':'))


n_models = 0
guns = set()
for f in sorted(glob.glob(f'{A}/models/item/*.json')):
    m = json.load(open(f))
    name = os.path.basename(f)[:-5]
    if m.get("parent") not in ("minecraft:item/handheld", "item/handheld"): continue
    if "layer1" in m.get("textures", {}) or "layer0" not in m.get("textures", {}): continue
    tex = m["textures"]["layer0"]
    if not os.path.exists(f'{A}/textures/{tex.split(":")[1]}.png'): continue
    defn = json.load(open(f'{A}/items/{name}.json')) if os.path.exists(f'{A}/items/{name}.json') else None
    is_gun = defn is not None and defn["model"].get("type") == "minecraft:composite"
    big = Image.open(f'{A}/textures/{tex.split(":")[1]}.png').size[0] >= 32
    z0, z1 = (7.0, 9.0) if is_gun or big else (7.25, 8.75)
    write(f, tex, z0, z1, is_gun, DISPLAY); n_models += 1
    if is_gun:
        guns.add(name)
        json.dump({"parent": f"kiyamet:item/{name}", "display": RECOIL}, open(f'{A}/models/item/{name}_recoil.json', 'w'))

# attachments: a little thicker so they sit on the gun's surface
for f in sorted(glob.glob(f'{A}/models/item/att/*.json')):
    name = os.path.basename(f)[:-5]
    if name.endswith('_recoil'): continue
    m = json.load(open(f))
    tex = m.get("textures", {}).get("layer0") or m.get("textures", {}).get("0")
    if not tex: continue
    write(f, tex, 6.8, 9.2, False, DISPLAY); n_models += 1
    json.dump({"parent": f"kiyamet:item/att/{name}", "display": RECOIL}, open(f'{A}/models/item/att/{name}_recoil.json', 'w'))

# gun item definitions: the composite as before, swapped for a recoiling copy while the gun cools down after a shot
def recoiled(node):
    node = json.loads(json.dumps(node))
    def walk(n):
        if isinstance(n, dict):
            if n.get("type") == "minecraft:model" and n.get("model", "").startswith("kiyamet:item/") and not n["model"].endswith("_recoil"):
                n["model"] += "_recoil"
            for v in n.values(): walk(v)
        elif isinstance(n, list):
            for v in n: walk(v)
    walk(node); return node

for name in sorted(guns):
    p = f'{A}/items/{name}.json'
    d = json.load(open(p))
    base = d["model"]
    if base.get("type") == "minecraft:range_dispatch": base = base["fallback"]
    d["model"] = {"type": "minecraft:range_dispatch", "property": "minecraft:cooldown", "scale": 1.0,
                  "entries": [{"threshold": 0.6, "model": recoiled(base)}], "fallback": base}
    json.dump(d, open(p, 'w'), indent=1)
print('3d models:', n_models, 'guns with recoil:', len(guns))
