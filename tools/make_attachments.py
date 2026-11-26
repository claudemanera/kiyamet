"""Generates gun attachment overlays (scopes, silencers, lights, magazines) fitted to each gun sprite,
plus composite item model definitions that show them and tint the gun with its paint."""
import json, os, re
from PIL import Image
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')
src = open(os.path.join(os.path.dirname(__file__), '..', 'src/main/java/com/kiyamet/registry/ModItems.java')).read()
W = re.findall(r'"(\w+)"', src[src.index('WEAPONS = {'):src.index('};', src.index('WEAPONS = {'))])
os.makedirs(f'{A}/textures/item/att', exist_ok=True); os.makedirs(f'{A}/models/item/att', exist_ok=True)

K = (28, 28, 32, 255); D = (52, 54, 60, 255); G = (92, 96, 104, 255); L = (140, 146, 156, 255)
RED = (230, 40, 40, 255); CY = (90, 230, 255, 255); GL = (120, 190, 255, 255); YE = (255, 240, 150, 255)

def put(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height: im.putpixel((x, y), c)

def rect(im, x0, y0, x1, y1, c, outline=None):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = outline and (x in (x0, x1) or y in (y0, y1))
            put(im, x, y, outline if edge else c)

def analyse(img):
    w, h = img.size
    op = lambda x, y: img.getpixel((x, y))[3] > 0
    xs = [x for y in range(h) for x in range(w) if op(x, y)]
    mx = max(xs)
    barrel = [y for y in range(h) if any(op(x, y) for x in range(mx - 1, mx + 1))]
    by = barrel[len(barrel) // 2]
    def top(x): return min([y for y in range(h) if op(x, y)] or [by])
    def bottom(x): return max([y for y in range(h) if op(x, y)] or [by])
    left = min(xs)
    grip = max(range(left, left + (mx - left) // 2 + 1), key=bottom)
    return dict(mx=mx, by=by, top=top, bottom=bottom, left=left, grip=grip, top_b=min(barrel), bot_b=max(barrel))

def scope(kind, a, s):
    im = Image.new('RGBA', s.size, (0, 0, 0, 0))
    cx = a['left'] + int((a['mx'] - a['left']) * 0.45)
    t = min(a['top'](x) for x in range(cx - 3, cx + 4))
    if kind == 'red_dot':
        rect(im, cx - 2, t - 4, cx + 2, t - 1, D, K); put(im, cx, t - 3, RED); put(im, cx - 1, t - 3, (120, 20, 20, 255))
    elif kind == 'holo':
        rect(im, cx - 3, t - 5, cx + 3, t - 1, (0, 0, 0, 0), K); rect(im, cx - 2, t - 4, cx + 2, t - 2, (40, 90, 110, 140)); put(im, cx, t - 3, CY); rect(im, cx - 3, t - 1, cx + 3, t - 1, D)
    elif kind == 'acog':
        rect(im, cx - 4, t - 4, cx + 4, t - 2, D, K); put(im, cx - 4, t - 3, GL); put(im, cx + 4, t - 3, GL); rect(im, cx - 1, t - 1, cx + 1, t - 1, G)
    elif kind == 'sniper':
        rect(im, cx - 7, t - 5, cx + 7, t - 3, D, K); rect(im, cx - 8, t - 6, cx - 6, t - 2, G, K); rect(im, cx + 6, t - 6, cx + 8, t - 2, G, K)
        put(im, cx - 7, t - 4, GL); put(im, cx + 7, t - 4, GL); put(im, cx, t - 6, L); rect(im, cx - 3, t - 2, cx - 2, t - 1, G); rect(im, cx + 2, t - 2, cx + 3, t - 1, G)
    return im

def silencer(kind, a, s):
    im = Image.new('RGBA', s.size, (0, 0, 0, 0))
    n = 5 if kind == 'short' else 8
    y0, y1 = a['by'] - 1, a['by'] + 1
    rect(im, a['mx'] - n + 1, y0, a['mx'], y1, D, K)
    for x in range(a['mx'] - n + 2, a['mx'], 2): put(im, x, y0, G)
    return im

def light(kind, a, s):
    im = Image.new('RGBA', s.size, (0, 0, 0, 0))
    x = a['mx'] - 11
    y = max(a['bottom'](xx) for xx in range(x, x + 4)) if a['bottom'](x) < a['by'] + 6 else a['bot_b']
    y = min(y, a['bot_b'] + 2) + 1
    if kind == 'flashlight':
        rect(im, x, y, x + 4, y + 2, G, K); put(im, x + 4, y + 1, YE); put(im, x + 5, y + 1, (255, 250, 200, 160))
    else:
        rect(im, x + 1, y, x + 3, y + 1, D, K); put(im, x + 3, y, RED)
        for i in range(4, 9): put(im, x + i, y, (255, 50, 50, 110))
    return im

def mag(kind, a, s):
    im = Image.new('RGBA', s.size, (0, 0, 0, 0))
    x = a['grip'] + 4
    y = a['bottom'](x) if a['bottom'](x) <= a['bot_b'] + 3 else a['bot_b'] + 1
    if kind == 'extended':
        rect(im, x, y, x + 2, y + 7, D, K); put(im, x + 1, y + 6, L)
    else:
        rect(im, x - 1, y, x + 3, y + 4, G, K); rect(im, x, y + 1, x + 2, y + 3, D); put(im, x + 1, y + 2, L)
    return im

PARTS = {0: ('scope', ['red_dot', 'holo', 'acog', 'sniper'], scope), 1: ('silencer', ['short', 'long'], silencer),
         2: ('light', ['flashlight', 'laser'], light), 3: ('mag', ['extended', 'drum'], mag)}

for g in W:
    base = Image.open(f'{A}/textures/item/{g}.png').convert('RGBA')
    a = analyse(base)
    models = [{"type": "minecraft:model", "model": f"kiyamet:item/{g}", "tints": [{"type": "minecraft:custom_model_data", "index": 0, "default": -1}]}]
    for idx, (part, kinds, fn) in PARTS.items():
        cases = []
        for k in kinds:
            name = f'{g}_{k}'
            fn(k, a, base).save(f'{A}/textures/item/att/{name}.png')
            json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": f"kiyamet:item/att/{name}"}}, open(f'{A}/models/item/att/{name}.json', 'w'))
            cases.append({"when": k, "model": {"type": "minecraft:model", "model": f"kiyamet:item/att/{name}"}})
        models.append({"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": idx, "cases": cases, "fallback": {"type": "minecraft:empty"}})
    json.dump({"model": {"type": "minecraft:composite", "models": models}}, open(f'{A}/items/{g}.json', 'w'), indent=1)
print('guns', len(W))
