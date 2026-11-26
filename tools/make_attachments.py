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

import sys
sys.path.insert(0, os.path.dirname(__file__))
from draw_guns import G, MAT, GLOW

def scope(kind, a, s):
    g = G()
    cx = a['left'] + int((a['mx'] - a['left']) * 0.45)
    t = min(a['top'](x) for x in range(cx - 3, cx + 4))
    if kind == 'red_dot':
        g.rect(cx - 2, t - 1, cx + 2, t - 1, 'gunmetal')                  # mount
        g.rect(cx - 2, t - 4, cx + 2, t - 2, 'black')
        g.px(cx + 2, t - 3, GLOW['red']).px(cx - 2, t - 3, (120, 200, 230)).px(cx, t - 4, MAT['black'][3])
    elif kind == 'holo':
        g.rect(cx - 3, t - 1, cx + 3, t - 1, 'gunmetal')
        g.rect(cx - 3, t - 6, cx + 3, t - 2, 'black')
        g.rect(cx - 2, t - 5, cx + 2, t - 3, 'navy')
        for (x, y) in ((cx, t - 5), (cx, t - 3), (cx - 1, t - 4), (cx + 1, t - 4)): g.px(x, y, GLOW['cyan'])
        g.px(cx, t - 4, GLOW['red'])
    elif kind == 'acog':
        g.rect(cx - 1, t - 1, cx + 1, t - 1, 'gunmetal'); g.rect(cx - 4, t - 1, cx - 3, t - 1, 'gunmetal')
        g.rect(cx - 5, t - 4, cx + 5, t - 2, 'gunmetal')
        g.rect(cx - 6, t - 5, cx - 5, t - 1, 'black'); g.rect(cx + 5, t - 5, cx + 6, t - 1, 'black')
        g.px(cx + 6, t - 3, GLOW['cyan']).px(cx - 6, t - 3, (90, 160, 200)).px(cx, t - 5, MAT['tan'][2])
    elif kind == 'sniper':
        g.rect(cx - 3, t - 1, cx - 2, t - 1, 'gunmetal'); g.rect(cx + 2, t - 1, cx + 3, t - 1, 'gunmetal')
        g.rect(cx - 7, t - 4, cx + 7, t - 2, 'black')
        g.rect(cx - 10, t - 5, cx - 7, t - 1, 'black'); g.rect(cx + 7, t - 5, cx + 10, t - 1, 'black')
        g.rect(cx - 1, t - 6, cx + 1, t - 5, 'gunmetal')                  # elevation turret
        g.px(cx + 10, t - 3, GLOW['cyan']).px(cx + 10, t - 4, (170, 240, 255)).px(cx - 10, t - 3, (80, 150, 190))
    return g.render()

def silencer(kind, a, s):
    g = G()
    n = 5 if kind == 'short' else 8
    x0 = a['mx'] - n + 1
    g.rect(x0, a['by'] - 1, a['mx'], a['by'] + 1, 'black')
    for x in range(x0 + 1, a['mx'], 3): g.rect(x, a['by'] - 1, x, a['by'] + 1, 'gunmetal')
    g.px(a['mx'], a['by'], (10, 10, 12))
    return g.render()

def light(kind, a, s):
    g = G()
    x = a['mx'] - 12
    y = min(a['bot_b'] + 1, max(a['bottom'](xx) for xx in range(x, x + 5)) + 1) if a['bottom'](x) < a['by'] + 6 else a['bot_b'] + 1
    if kind == 'flashlight':
        g.rect(x, y, x + 1, y, 'black')                    # mount
        g.rect(x - 1, y + 1, x + 4, y + 2, 'gunmetal'); g.rect(x + 4, y + 1, x + 5, y + 2, 'black')
        g.px(x + 5, y + 1, GLOW['gold']).px(x + 5, y + 2, (255, 240, 170)).px(x + 6, y + 1, (255, 240, 170)).px(x + 7, y + 1, (255, 245, 200))
    else:
        g.rect(x + 1, y, x + 2, y, 'black')
        g.rect(x, y + 1, x + 3, y + 2, 'black'); g.px(x + 3, y + 1, GLOW['red']).px(x + 1, y + 2, (90, 30, 30))
        for i in range(4, 10): g.px(x + i, y + 1, (255, 60, 60) if i < 6 else (200, 50, 50))
    return g.render()

def mag(kind, a, s):
    g = G()
    x = a['grip'] + 5
    y = a['bottom'](x) + 1 if a['bottom'](x) <= a['bot_b'] + 3 else a['bot_b'] + 1
    if kind == 'extended':
        g.poly([(x, y), (x + 3, y), (x + 4, y + 8), (x + 1, y + 8)], 'black')
        g.rect(x + 1, y + 8, x + 4, y + 8, 'steel')
        for yy in range(y + 2, y + 7, 2): g.px(x + 1 + (yy - y) // 8, yy, MAT['black'][3])
    else:
        g.rect(x, y, x + 3, y, 'black')
        g.poly([(x - 2, y + 1), (x + 5, y + 1), (x + 6, y + 4), (x + 5, y + 7), (x - 2, y + 7), (x - 3, y + 4)], 'black')
        g.rect(x, y + 3, x + 3, y + 5, 'gunmetal'); g.px(x + 1, y + 4, MAT['steel'][3])
    return g.render()

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
