"""TWD 1.7 furniture: 3D block models (elements), material textures, blockstates, loot, lang; placed bag models; meal icons."""
import json, os, random, math, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
R = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources')
A, D = R + '/assets/kiyamet', R + '/data/kiyamet'
def w(p, o): os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
L = json.load(open(A + '/lang/en_us.json'))

# ------------------------------------------------------------------ material textures
def tex(name, base, kind='plain', seed=1):
    r = random.Random(seed + hash(name) % 1000); im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = r.randint(-7, 7); c = list(base)
            if kind == 'wood':
                if y % 4 == 0: v -= 18
                if (x * 3 + y) % 11 == 0: v -= 10
            elif kind == 'fabric':
                if (x + y) % 2 == 0: v -= 6
            elif kind == 'metal':
                v = r.randint(-3, 3) + (10 if y < 2 else 0) - (8 if y > 13 else 0)
            elif kind == 'glass':
                if x == y or x == y + 1: v += 40
            elif kind == 'screen':
                v = r.randint(-4, 4); c = [20, 30 + y * 2, 60 + y * 4] if 1 <= x <= 14 and 1 <= y <= 14 else [20, 20, 24]
            elif kind == 'books':
                cols = [(150, 40, 40), (40, 80, 150), (50, 120, 60), (180, 140, 50), (100, 60, 120), (70, 70, 70)]
                c = list(cols[(x // 2 + y // 8 * 3) % len(cols)]) if y % 8 != 7 else [90, 60, 32]
                if y % 8 in (0,): c = [int(k * 0.7) for k in c]
            elif kind == 'burner':
                d = math.hypot(x - 7.5, y - 7.5); c = [30, 30, 32] if d > 6 else [60, 60, 64] if d > 4 else [40, 40, 44] if d > 2 else [70, 70, 74]
            elif kind == 'water':
                c = [60, 120 + (x + y) % 3 * 10, 200]
            im.putpixel((x, y), tuple(max(0, min(255, k + v)) for k in c) + (255,))
    im.save(f'{A}/textures/block/furn_{name}.png')
MATS = {'oak': ((168, 124, 76), 'wood'), 'dark': ((86, 56, 34), 'wood'), 'birch': ((206, 188, 140), 'wood'), 'spruce': ((116, 84, 50), 'wood'),
        'white': ((232, 234, 236), 'metal'), 'black': ((34, 34, 38), 'metal'), 'steel': ((150, 156, 166), 'metal'), 'gold': ((226, 184, 70), 'metal'),
        'red': ((170, 40, 44), 'fabric'), 'blue': ((50, 78, 150), 'fabric'), 'brown': ((120, 80, 50), 'fabric'), 'gray': ((110, 112, 118), 'fabric'),
        'sheet': ((236, 236, 228), 'fabric'), 'cream': ((222, 206, 170), 'fabric'), 'glass': ((170, 210, 230), 'glass'), 'screen': ((0, 0, 0), 'screen'),
        'books': ((0, 0, 0), 'books'), 'burner': ((0, 0, 0), 'burner'), 'water': ((0, 0, 0), 'water'), 'lamp': ((255, 236, 170), 'fabric'), 'counter': ((200, 200, 196), 'metal')}
for n, (c, k) in MATS.items(): tex(n, c, k)

def box(f, t, m, faces=None):
    fs = faces or ['north', 'south', 'east', 'west', 'up', 'down']
    return {"from": list(f), "to": list(t), "faces": {d: {"texture": "#" + m} for d in fs}}
def model(name, els, particle='oak'):
    used = sorted({e2['texture'][1:] for e in els for e2 in e['faces'].values()})
    w(f'{A}/models/block/{name}.json', {"parent": "minecraft:block/block", "ambientocclusion": False,
        "textures": dict({m: f"kiyamet:block/furn_{m}" for m in used}, particle=f"kiyamet:block/furn_{particle}"), "elements": els})
def legs(x1, z1, x2, z2, h, m, t=2):
    return [box((x1, 0, z1), (x1 + t, h, z1 + t), m), box((x2 - t, 0, z1), (x2, h, z1 + t), m), box((x1, 0, z2 - t), (x1 + t, h, z2), m), box((x2 - t, 0, z2 - t), (x2, h, z2), m)]

M = {}
# beds: head (north end) and foot; wide (double): x -8..24
def bed(name, blanket, frame, post=None):
    head = [box((-8, 0, 0), (24, 18, 2), frame), box((-8, 3, 2), (24, 7, 16), frame), box((-7, 7, 2), (23, 10, 16), 'sheet'),
            box((-6, 10, 3), (7, 12, 8), 'sheet'), box((9, 10, 3), (22, 12, 8), 'sheet'), box((-7, 10, 9), (23, 11, 16), blanket)] + legs(-8, 0, 24, 16, 3, frame)
    foot = [box((-8, 3, 0), (24, 7, 14), frame), box((-7, 7, 0), (23, 10, 14), 'sheet'), box((-7, 10, 0), (23, 11, 14), blanket), box((-7.5, 5, 0), (23.5, 10.5, 13), blanket),
            box((-8, 0, 14), (24, 11, 16), frame)] + legs(-8, 0, 24, 16, 3, frame)
    if post:
        for x in (-8, 22):
            head.append(box((x, 0, 0), (x + 2, 28, 2), post)); foot.append(box((x, 0, 14), (x + 2, 16, 16), post))
        head.append(box((-8, 16, 0), (24, 18, 2), post))
    model(name + '_head', head, frame); model(name + '_foot', foot, frame)
bed('double_bed', 'red', 'oak'); bed('royal_bed', 'blue', 'dark', 'gold')
M['wardrobe'] = [box((0, 0, 2), (16, 30, 16), 'dark'), box((0.5, 1, 1), (7.8, 29, 2), 'oak'), box((8.2, 1, 1), (15.5, 29, 2), 'oak'),
                 box((6.5, 13, 0.4), (7.3, 17, 1), 'gold'), box((8.7, 13, 0.4), (9.5, 17, 1), 'gold'), box((-0.5, 30, 1.5), (16.5, 31.5, 16.5), 'dark'), box((0, 0, 1.5), (16, 1, 2), 'dark')]
M['fridge'] = [box((1, 0, 1.5), (15, 30, 15), 'white'), box((1.2, 19.2, 1), (14.8, 29.8, 1.5), 'white'), box((1.2, 0.5, 1), (14.8, 18.8, 1.5), 'white'),
               box((12.5, 21, 0.3), (13.5, 28, 1), 'steel'), box((12.5, 9, 0.3), (13.5, 17, 1), 'steel'), box((1, 18.8, 1.2), (15, 19.2, 1.5), 'black'), box((2, 26, 0.8), (6, 28, 1), 'glass')]
M['kitchen_cabinet'] = [box((0, 0, 2), (16, 14, 16), 'spruce'), box((0.5, 1, 1), (7.8, 13, 2), 'oak'), box((8.2, 1, 1), (15.5, 13, 2), 'oak'),
                        box((6.6, 9, 0.5), (7.2, 12, 1), 'steel'), box((8.8, 9, 0.5), (9.4, 12, 1), 'steel'), box((0, 14, 0), (16, 16, 16), 'counter')]
M['kitchen_sink'] = [box((0, 0, 2), (16, 14, 16), 'spruce'), box((0.5, 1, 1), (15.5, 13, 2), 'oak'), box((0, 14, 0), (16, 16, 3), 'counter'), box((0, 14, 13), (16, 16, 16), 'counter'),
                     box((0, 14, 3), (2, 16, 13), 'counter'), box((14, 14, 3), (16, 16, 13), 'counter'), box((2, 11, 3), (14, 12, 13), 'steel'), box((2, 12, 3), (14, 14, 3.5), 'steel'),
                     box((7, 16, 13), (9, 22, 15), 'steel'), box((7, 20, 9), (9, 22, 13), 'steel'), box((3, 12, 5), (13, 12.5, 11), 'water')]
M['stove'] = [box((0, 0, 1), (16, 14, 16), 'black'), box((1, 2, 0.5), (15, 11, 1), 'glass'), box((2, 12, 0), (14, 13, 1), 'steel'), box((0, 14, 1), (16, 15, 16), 'steel'),
              box((2, 15, 3), (7, 15.3, 8), 'burner', ['up']), box((9, 15, 3), (14, 15.3, 8), 'burner', ['up']), box((2, 15, 9), (7, 15.3, 14), 'burner', ['up']), box((9, 15, 9), (14, 15.3, 14), 'burner', ['up']),
              box((0, 15, 14), (16, 20, 16), 'black')]
M['dining_table'] = [box((-0.5, 13, -0.5), (16.5, 15, 16.5), 'oak')] + legs(1, 1, 15, 15, 13, 'dark', 2) + [box((3, 4, 1.5), (13, 5, 2.5), 'dark'), box((3, 4, 13.5), (13, 5, 14.5), 'dark')]
M['wooden_chair'] = [box((3, 7, 3), (13, 8.5, 13), 'oak')] + legs(3, 3, 13, 13, 7, 'dark', 1.5) + [box((3, 8.5, 11.5), (13, 18, 13), 'oak'), box((3, 8.5, 11.5), (4.5, 20, 13), 'dark'), box((11.5, 8.5, 11.5), (13, 20, 13), 'dark')]
M['armchair'] = [box((1, 2, 1), (15, 7, 15), 'brown'), box((1, 0, 1), (15, 2, 15), 'dark'), box((1, 7, 11), (15, 18, 15), 'brown'), box((1, 7, 1), (3.5, 11, 11), 'brown'), box((12.5, 7, 1), (15, 11, 11), 'brown'),
                 box((3.5, 7, 1.5), (12.5, 8.5, 11), 'cream')]
M['sofa'] = [box((0, 2, 1), (16, 7, 15), 'gray'), box((0, 0, 1), (16, 2, 15), 'dark'), box((0, 7, 11), (16, 17, 15), 'gray'), box((0.5, 7, 1.5), (8, 8.5, 11), 'gray'), box((8, 7, 1.5), (15.5, 8.5, 11), 'gray'),
             box((1, 8.5, 9.5), (7, 13, 11), 'cream'), box((9, 8.5, 9.5), (15, 13, 11), 'red')]
M['coffee_table'] = [box((1, 5, 3), (15, 7, 13), 'dark'), box((3, 6.9, 5), (13, 7.2, 11), 'glass', ['up'])] + legs(1.5, 3.5, 14.5, 12.5, 5, 'dark', 1.5) + [box((2, 1.5, 4), (14, 2.5, 12), 'dark')]
M['bookcase'] = [box((0, 0, 4), (16, 16, 16), 'oak'), box((1, 1, 3.9), (15, 7, 4), 'books', ['north']), box((1, 9, 3.9), (15, 15, 4), 'books', ['north']), box((0, 7.5, 3), (16, 8.5, 4), 'oak'),
                 box((0, 0, 3), (1, 16, 4), 'oak'), box((15, 0, 3), (16, 16, 4), 'oak'), box((0, 15, 3), (16, 16, 4), 'oak')]
M['office_desk'] = [box((0, 12, 2), (16, 13.5, 16), 'birch'), box((10, 0, 2), (16, 12, 16), 'birch'), box((10.5, 7, 1.6), (15.5, 11, 2), 'oak'), box((10.5, 2, 1.6), (15.5, 6, 2), 'oak'),
                    box((12, 9, 1.2), (14, 9.6, 1.6), 'steel'), box((12, 4, 1.2), (14, 4.6, 1.6), 'steel'), box((0.5, 0, 2.5), (2, 12, 15.5), 'birch'),
                    box((2, 13.5, 7), (8, 13.8, 11), 'black'), box((2, 13.5, 11), (8, 17.5, 11.5), 'screen')]
M['floor_lamp'] = [box((5, 0, 5), (11, 1, 11), 'black'), box((7.5, 1, 7.5), (8.5, 22, 8.5), 'steel'), box((4, 22, 4), (12, 29, 12), 'lamp'), box((6, 21, 6), (10, 22, 10), 'gold')]
M['television'] = [box((4, 0, 6), (12, 1, 11), 'black'), box((7, 1, 8), (9, 4, 9), 'black'), box((0, 4, 7.5), (16, 14, 9), 'black'), box((0.8, 4.8, 7.4), (15.2, 13.2, 7.5), 'screen', ['north']),
                   box((7.5, 4.2, 7.4), (8.5, 4.6, 7.5), 'red', ['north'])]
M['bathtub'] = [box((0, 1, 2), (16, 9, 3.5), 'white'), box((0, 1, 12.5), (16, 9, 14), 'white'), box((0, 1, 2), (1.5, 9, 14), 'white'), box((14.5, 1, 2), (16, 9, 14), 'white'),
                box((1.5, 1, 3.5), (14.5, 2, 12.5), 'white'), box((1.5, 2, 3.5), (14.5, 6, 12.5), 'water', ['up'])] + legs(0.5, 2.5, 15.5, 13.5, 1, 'gold', 1.5) + [box((13, 9, 7), (14, 12, 8), 'steel')]
M['toilet'] = [box((4, 0, 3), (12, 7, 11), 'white'), box((3.5, 7, 2), (12.5, 8, 11), 'white'), box((5, 7.9, 3.5), (11, 8.1, 9.5), 'water', ['up']), box((3, 7, 11), (13, 15, 14), 'white'),
               box((2.8, 14.5, 10.8), (13.2, 15.5, 14.2), 'white'), box((11, 13, 10.6), (12.5, 13.6, 11), 'steel')]
M['gun_rack'] = [box((0, 0, 14), (16, 16, 16), 'dark'), box((1, 3, 12), (3, 4, 14), 'steel'), box((13, 3, 12), (15, 4, 14), 'steel'), box((1, 10, 12), (3, 11, 14), 'steel'), box((13, 10, 12), (15, 11, 14), 'steel'),
                 box((-1, 4, 12.5), (17, 5.5, 13.5), 'black'), box((10, 2, 12.5), (14, 4, 13.5), 'oak'), box((-1, 11, 12.5), (17, 12.5, 13.5), 'black'), box((1, 9, 12.5), (5, 11, 13.5), 'oak')]
M['nightstand'] = [box((2, 0, 2), (14, 11, 14), 'oak'), box((2.5, 6, 1.5), (13.5, 10, 2), 'dark'), box((2.5, 1, 1.5), (13.5, 5, 2), 'dark'), box((7, 7.5, 1), (9, 8.5, 1.5), 'gold'),
                   box((7, 2.5, 1), (9, 3.5, 1.5), 'gold'), box((5, 11, 5), (8, 12, 8), 'black'), box((6, 12, 6), (7, 15, 7), 'steel'), box((4.5, 15, 4.5), (8.5, 18, 8.5), 'lamp')]
PART = {'double_bed': 'red', 'royal_bed': 'blue'}
for n, els in M.items(): model(n, els, 'white' if n in ('fridge', 'bathtub', 'toilet') else 'oak' if n != 'stove' else 'black')

ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
NAMES = {}
import re
src = open(os.path.join(os.path.dirname(__file__), '..', 'src/main/java/com/kiyamet/furniture/Furniture.java')).read()
for fid, title in re.findall(r'new Def\("(\w+)", "([^"]+)"', src): NAMES[fid] = title
for fid, title in NAMES.items():
    L['block.kiyamet.' + fid] = title
    if fid in ('double_bed', 'royal_bed'):
        w(f'{A}/blockstates/{fid}.json', {"variants": {f"facing={f},part={p}": {"model": f"kiyamet:block/{fid}_{p}", "y": ROT[f]} for f in ROT for p in ('head', 'foot')}})
        w(f'{A}/items/{fid}.json', {"model": {"type": "minecraft:model", "model": f"kiyamet:block/{fid}_head"}})
        w(f'{D}/loot_table/blocks/{fid}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"kiyamet:{fid}",
            "conditions": [{"condition": "minecraft:block_state_property", "block": f"kiyamet:{fid}", "properties": {"part": "head"}}]}]}]})
    else:
        w(f'{A}/blockstates/{fid}.json', {"variants": {f"facing={f}": {"model": f"kiyamet:block/{fid}", "y": ROT[f]} for f in ROT}})
        w(f'{A}/items/{fid}.json', {"model": {"type": "minecraft:model", "model": f"kiyamet:block/{fid}"}})
        w(f'{D}/loot_table/blocks/{fid}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"kiyamet:{fid}"}]}]})

# furniture table
tex('table_top', (150, 110, 70), 'wood', 7)
im = Image.open(f'{A}/textures/block/furn_table_top.png')
for i in range(3, 13): im.putpixel((i, 4), (60, 60, 64, 255)); im.putpixel((4, i), (60, 60, 64, 255))
for (x, y) in ((10, 9), (11, 10), (12, 11), (9, 10), (10, 11)): im.putpixel((x, y), (180, 186, 196, 255))
im.save(f'{A}/textures/block/furn_table_top.png')
model('furniture_table', [box((0, 12, 0), (16, 16, 16), 'table_top', ['up']), box((0, 12, 0), (16, 16, 16), 'oak', ['north', 'south', 'east', 'west', 'down']),
                          box((1, 0, 1), (4, 12, 4), 'dark'), box((12, 0, 1), (15, 12, 4), 'dark'), box((1, 0, 12), (4, 12, 15), 'dark'), box((12, 0, 12), (15, 12, 15), 'dark'),
                          box((3, 4, 3), (13, 5, 13), 'oak'), box((2, 16, 10), (6, 17, 14), 'steel'), box((9, 16, 2), (14, 16.5, 4), 'red')])
w(f'{A}/blockstates/furniture_table.json', {"variants": {"": {"model": "kiyamet:block/furniture_table"}}})
w(f'{A}/items/furniture_table.json', {"model": {"type": "minecraft:model", "model": "kiyamet:block/furniture_table"}})
w(f'{D}/loot_table/blocks/furniture_table.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "kiyamet:furniture_table"}]}]})
w(f'{D}/recipe/furniture_table.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SPS", "PCP", "PPP"],
    "key": {"S": "minecraft:shears", "P": "#minecraft:planks", "C": "minecraft:crafting_table"}, "result": {"id": "kiyamet:furniture_table", "count": 1}})
L['block.kiyamet.furniture_table'] = 'Furniture Table'

# ------------------------------------------------------------------ placed bags (2 per block)
BAG = ['', (138, 106, 68), (78, 138, 58), (46, 100, 176), (224, 122, 30), (106, 116, 52), (232, 232, 228), (42, 42, 46), (90, 98, 52), (122, 58, 176), (176, 30, 42), (150, 110, 60), (230, 190, 60)]
ACC = ['', (90, 64, 40), (46, 90, 34), (28, 62, 114), (142, 74, 16), (62, 68, 32), (192, 40, 40), (20, 20, 26), (58, 62, 32), (74, 30, 114), (232, 192, 64), (60, 150, 60), (90, 60, 20)]
for i in range(1, 13):
    tex(f'bag{i}', BAG[i], 'fabric', i)
    im = Image.open(f'{A}/textures/block/furn_bag{i}.png')
    for x in range(16): im.putpixel((x, 0), ACC[i] + (255,)); im.putpixel((x, 15), ACC[i] + (255,))
    for y in range(16): im.putpixel((7, y), ACC[i] + (255,)); im.putpixel((8, y), ACC[i] + (255,))
    im.save(f'{A}/textures/block/furn_bag{i}.png')
    for half, y0 in (('lower', 0), ('upper', 8)):
        if i <= 10:   # backpack
            els = [box((3, y0, 4), (13, y0 + 7, 12), f'bag{i}'), box((4, y0 + 1, 2.5), (12, y0 + 5, 4), f'bag{i}'), box((5, y0 + 7, 7), (11, y0 + 8, 9), 'black'),
                   box((4, y0 + 2, 12), (5, y0 + 7, 13), 'black'), box((11, y0 + 2, 12), (12, y0 + 7, 13), 'black'), box((7, y0 + 3, 2.2), (9, y0 + 4, 2.5), 'steel')]
        else:          # money bag
            els = [box((3, y0, 3), (13, y0 + 6, 13), f'bag{i}'), box((4, y0 + 6, 4), (12, y0 + 7, 12), f'bag{i}'), box((6, y0 + 7, 6), (10, y0 + 8, 10), 'gold' if i == 12 else 'brown'),
                   box((6.5, y0 + 2, 2.8), (9.5, y0 + 5, 3), 'gold' if i == 12 else 'black', ['north'])]
        model(f'bag{i}_{half}', els, f'bag{i}')
mp = []
for i in range(1, 13):
    for f, r in ROT.items():
        mp.append({"when": {"lower": str(i), "facing": f}, "apply": {"model": f"kiyamet:block/bag{i}_lower", "y": r}})
        mp.append({"when": {"upper": str(i), "facing": f}, "apply": {"model": f"kiyamet:block/bag{i}_upper", "y": r}})
w(f'{A}/blockstates/placed_bag.json', {"multipart": mp})
w(f'{D}/loot_table/blocks/placed_bag.json', {"type": "minecraft:block", "pools": []})
L['block.kiyamet.placed_bag'] = 'Bag'

# ------------------------------------------------------------------ meals, spoiled food, gold bag icons
from draw_farm import C, dark, light, mix
def bowl(c, soup, bits=()):
    for y in range(9, 14):
        w2 = 7 - (y - 9)
        for x in range(8 - w2, 8 + w2): c.px(x, y, (140, 96, 56) if y > 9 else (110, 76, 44))
    for x in range(2, 14): c.px(x, 8, soup); c.px(x, 9, dark(soup, 0.85))
    for x in range(3, 13): c.px(x, 7, light(soup, 0.15))
    for (x, y, col) in bits: c.px(x, y, col)
MEALS = {
 'vegetable_stew': lambda c: bowl(c, (190, 110, 50), [(5, 7, (240, 140, 40)), (9, 7, (90, 160, 60)), (11, 8, (220, 60, 40)), (7, 8, (240, 220, 160))]),
 'tomato_soup': lambda c: bowl(c, (200, 50, 36), [(6, 7, (240, 240, 230)), (10, 7, (90, 160, 60))]),
 'ratatouille': lambda c: bowl(c, (170, 60, 40), [(4, 7, (110, 50, 140)), (7, 7, (90, 160, 60)), (10, 7, (240, 200, 60)), (12, 8, (110, 50, 140))]),
 'fruit_salad': lambda c: bowl(c, (240, 200, 160), [(4, 7, (240, 140, 30)), (6, 6, (225, 40, 45)), (9, 7, (250, 170, 120)), (11, 6, (225, 40, 45)), (8, 6, (150, 200, 80))]),
}
def stuffed(c):
    for k, x0 in enumerate((3, 9)):
        c.blob(x0 + 2, 9, 3, 4.5, (220, 50, 30) if k == 0 else (90, 170, 60))
        for x in range(x0, x0 + 5): c.px(x, 5, (200, 150, 90)); c.px(x, 6, (170, 110, 70))
def rolls(c):
    for k, (x0, y0) in enumerate(((2, 6), (8, 8), (4, 10))):
        for y in range(y0, y0 + 4):
            for x in range(x0, x0 + 7): c.px(x, y, (130, 190, 90) if (x + y) % 3 else (100, 160, 70))
        c.px(x0 + 6, y0 + 1, (180, 100, 70))
def cornbread(c):
    for y in range(6, 13):
        for x in range(2, 14): c.px(x, y, (236, 190, 70) if y > 6 else (200, 140, 50))
    for (x, y) in ((4, 8), (8, 9), (11, 8), (6, 11), (10, 11)): c.px(x, y, (250, 220, 120))
def pie(c):
    for y in range(8, 14):
        for x in range(2, 14): c.px(x, y, (210, 150, 80))
    for x in range(3, 13):
        for y in range(6, 9): c.px(x, y, (250, 170, 120) if (x + y) % 3 else (230, 120, 80))
    for x in range(3, 13, 3): c.px(x, 6, (230, 190, 120))
def jam(c):
    for y in range(4, 15):
        for x in range(4, 12): c.px(x, y, (180, 20, 40) if y > 6 else (220, 230, 240))
    for x in range(4, 12): c.px(x, 4, (220, 60, 60)); c.px(x, 3, (220, 60, 60))
    for y in range(9, 13):
        for x in range(5, 11): c.px(x, y, (240, 230, 200))
    c.px(7, 10, (220, 40, 50)); c.px(8, 11, (220, 40, 50))
def ration(c):
    for y in range(3, 14):
        for x in range(3, 13): c.px(x, y, (110, 120, 70) if 5 < y < 12 else (90, 100, 56))
    for x in range(5, 11): c.px(x, 7, (40, 40, 40)); c.px(x, 9, (40, 40, 40))
    c.px(7, 5, (230, 230, 230)); c.px(8, 5, (230, 230, 230))
def spoiled(c):
    c.blob(8, 10, 6, 4.5, (100, 110, 50))
    for (x, y) in ((5, 9), (9, 8), (11, 11), (7, 12)): c.px(x, y, (60, 80, 30))
    for (x, y) in ((6, 4), (10, 3), (8, 5)): c.px(x, y, (90, 120, 50))
MEALS.update({'stuffed_peppers': stuffed, 'cabbage_rolls': rolls, 'cornbread': cornbread, 'peach_pie': pie, 'strawberry_jam': jam, 'survivor_ration': ration, 'spoiled_food': spoiled})
for n, f in MEALS.items():
    c = C(); f(c); c.outline(); c.save(f'{A}/textures/item/{n}.png')
    w(f'{A}/models/item/{n}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{n}"}})
    w(f'{A}/items/{n}.json', {"model": {"type": "minecraft:model", "model": f"kiyamet:item/{n}"}})
gb = Image.open(f'{A}/textures/item/money_bag.png').convert('RGBA')
px = gb.load()
for y in range(16):
    for x in range(16):
        r_, g_, b_, a_ = px[x, y]
        if a_ and not (g_ > r_ and g_ > b_ + 30): px[x, y] = (min(255, int(r_ * 1.35 + 40)), min(255, int(g_ * 1.2 + 20)), int(b_ * 0.4), a_)
gb.save(f'{A}/textures/item/gold_money_bag.png')
w(f'{A}/models/item/gold_money_bag.json', {"parent": "minecraft:item/generated", "textures": {"layer0": "kiyamet:item/gold_money_bag"}})
w(f'{A}/items/gold_money_bag.json', {"model": {"type": "minecraft:model", "model": "kiyamet:item/gold_money_bag"}})
w(f'{D}/recipe/gold_money_bag.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GGG", "GBG", "GGG"], "key": {"G": "minecraft:gold_ingot", "B": "kiyamet:money_bag"},
    "result": {"id": "kiyamet:gold_money_bag", "count": 1}})
L.update({'item.kiyamet.gold_money_bag': 'Gold Money Bag', 'item.kiyamet.spoiled_food': 'Spoiled Food', 'effect.kiyamet.well_fed': 'Well Fed', 'entity.kiyamet.seat': 'Seat'})
TITLES = {'vegetable_stew': 'Vegetable Stew', 'tomato_soup': 'Tomato Soup', 'stuffed_peppers': 'Stuffed Peppers', 'ratatouille': 'Ratatouille', 'cabbage_rolls': 'Cabbage Rolls',
          'cornbread': 'Cornbread', 'fruit_salad': 'Fruit Salad', 'peach_pie': 'Peach Pie', 'strawberry_jam': 'Strawberry Jam', 'survivor_ration': "Survivor's Ration"}
for k, v in TITLES.items(): L['item.kiyamet.' + k] = v
REC = {'vegetable_stew': (["minecraft:bowl", "minecraft:potato", "minecraft:carrot", "kiyamet:onion", "kiyamet:cabbage", "kiyamet:tomato"], 1),
       'tomato_soup': (["minecraft:bowl", "kiyamet:tomato", "kiyamet:tomato", "kiyamet:tomato", "kiyamet:onion"], 1),
       'stuffed_peppers': (["kiyamet:bell_pepper", "kiyamet:bell_pepper", "minecraft:cooked_beef", "kiyamet:onion", "kiyamet:tomato"], 2),
       'ratatouille': (["minecraft:bowl", "kiyamet:eggplant", "kiyamet:tomato", "kiyamet:cucumber", "kiyamet:bell_pepper", "kiyamet:onion"], 1),
       'cabbage_rolls': (["kiyamet:cabbage", "minecraft:cooked_porkchop", "kiyamet:onion", "kiyamet:tomato"], 2),
       'cornbread': (["kiyamet:corn", "kiyamet:corn", "kiyamet:corn", "minecraft:wheat", "minecraft:wheat", "minecraft:egg"], 2),
       'fruit_salad': (["minecraft:bowl", "kiyamet:orange", "kiyamet:peach", "kiyamet:strawberry", "kiyamet:strawberry", "minecraft:apple"], 1),
       'peach_pie': (["kiyamet:peach", "kiyamet:peach", "kiyamet:peach", "minecraft:wheat", "minecraft:sugar", "minecraft:egg"], 1),
       'strawberry_jam': (["minecraft:glass_bottle", "kiyamet:strawberry", "kiyamet:strawberry", "kiyamet:strawberry", "kiyamet:strawberry", "minecraft:sugar"], 2),
       'survivor_ration': (["minecraft:cooked_beef", "kiyamet:cornbread", "minecraft:dried_kelp", "minecraft:dried_kelp", "minecraft:paper", "kiyamet:strawberry_jam"], 2)}
for k, (ings, n) in REC.items():
    w(f'{D}/recipe/{k}.json', {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ings, "result": {"id": f"kiyamet:{k}", "count": n}})
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('furniture', len(NAMES), 'meals', len(TITLES))
