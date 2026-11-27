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
    r = random.Random(seed + sum(map(ord, name))); im = Image.new('RGBA', (16, 16))
    def put(x, y, c, v=0): im.putpixel((x, y), tuple(max(0, min(255, int(k) + v)) for k in c[:3]) + (255,))
    for y in range(16):
        for x in range(16):
            v = r.randint(-5, 5); c = list(base)
            if kind == 'wood':      # planks: 4 boards, grain streaks, knots, board seams
                board = y // 4
                v += (board % 2) * 8 - 4
                if y % 4 == 3: v -= 26
                if (x + board * 5) % 16 == 0: v -= 22
                g = math.sin((x + board * 3) * 0.9 + y * 0.35) * 6; v += int(g)
                if (x * 7 + y * 13 + board) % 37 == 0: v -= 18
            elif kind == 'fabric':  # woven cloth with a stitched border
                v += 5 if (x + y) % 2 == 0 else -4
                if x in (0, 15) or y in (0, 15): v -= 22
                if (x in (1, 14) or y in (1, 14)) and (x + y) % 2 == 0: v += 18
            elif kind == 'metal':   # brushed, bevelled panel
                v = r.randint(-2, 2) + (x % 3 == 0) * 3
                if y == 0 or x == 0: v += 24
                if y == 15 or x == 15: v -= 24
            elif kind == 'glass':
                c = [base[0] - 10 + y * 2, base[1] - 10 + y * 2, base[2]]
                if x == y or x == y + 1 or x == y - 7: v += 45
                if x in (0, 15) or y in (0, 15): c = [200, 205, 210]
            elif kind == 'screen':
                v = r.randint(-3, 3); c = [18 + y, 30 + y * 3, 70 + y * 5] if 1 <= x <= 14 and 1 <= y <= 14 else [16, 16, 20]
                if 2 <= x <= 5 and 2 <= y <= 3: c = [200, 230, 255]
            elif kind == 'books':
                cols = [(150, 40, 40), (40, 80, 150), (50, 120, 60), (180, 140, 50), (100, 60, 120), (70, 70, 70), (160, 100, 60)]
                ci = (x // 2 + (y // 8) * 3) % len(cols); c = list(cols[ci])
                if y % 8 == 7: c = [90, 60, 32]
                elif x % 2 == 1: v -= 20
                elif y % 8 in (2, 5): v += 30
            elif kind == 'burner':
                d = math.hypot(x - 7.5, y - 7.5); c = [30, 30, 32] if d > 6 else [80, 80, 86] if d > 5 else [40, 40, 44] if d > 2.5 else [90, 40, 30] if d > 1 else [60, 60, 64]
            elif kind == 'water':
                c = [50, 110 + (x * 3 + y) % 4 * 8, 200]; v = 30 if (x + y * 2) % 9 == 0 else 0
            put(x, y, c, v)
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
    w(f'{A}/models/block/{name}.json', {"parent": "minecraft:block/block", "ambientocclusion": True,
        "textures": dict({m: (f"kiyamet:block/{m}" if m.startswith('f2_') else f"kiyamet:block/furn_{m}") for m in used},
                         particle=(f"kiyamet:block/{particle}" if particle.startswith('f2_') else f"kiyamet:block/furn_{particle}")), "elements": els})
def legs(x1, z1, x2, z2, h, m, t=2):
    return [box((x1, 0, z1), (x1 + t, h, z1 + t), m), box((x2 - t, 0, z1), (x2, h, z1 + t), m), box((x1, 0, z2 - t), (x1 + t, h, z2), m), box((x2 - t, 0, z2 - t), (x2, h, z2), m)]

# ------------------------------------------------------------------ 2.1: rebuilt furniture (see furn_designs.py)
import furn_designs
furn_designs.make_textures()
M = furn_designs.build(box, model, w)
for n, els in M.items(): model(n, els, 'f2_white' if n in ('fridge', 'bathtub', 'toilet', 'stove') else 'f2_oak')

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
(lambda *a: None)(f'{D}/recipe/furniture_table.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SPS", "PCP", "PPP"],
    "key": {"S": "minecraft:shears", "P": "#minecraft:planks", "C": "minecraft:crafting_table"}, "result": {"id": "kiyamet:furniture_table", "count": 1}})
L['block.kiyamet.furniture_table'] = 'Furniture Table'

# ------------------------------------------------------------------ placed bags (2 per block) - 2.1: real backpacks and money sacks
furn_designs.bags(box, model)
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
TITLES = {'vegetable_stew': 'Vegetable Stew', 'tomato_soup': 'Tomato Soup', 'stuffed_peppers': 'Stuffed Peppers', 'ratatouille': 'Baked Vegetables', 'cabbage_rolls': 'Cabbage Rolls',
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
for k, (ings, n) in ([] if True else REC.items()):        # 1.9: meals are cooked at the Cooking Table
    w(f'{D}/recipe/{k}.json', {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ings, "result": {"id": f"kiyamet:{k}", "count": n}})
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('furniture', len(NAMES), 'meals', len(TITLES))
