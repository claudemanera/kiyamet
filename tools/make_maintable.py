"""TWD 1.9 Main Table (builds every station) + mission board item icon; old station crafting recipes removed."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
import make_furniture as F
from draw_farm import C
A, D = F.A, F.D
L = json.load(open(A + '/lang/en_us.json'))
F.tex('main_top', (120, 84, 52), 'wood', 21)
from PIL import Image
im = Image.open(f'{A}/textures/block/furn_main_top.png')
for i in range(2, 14): im.putpixel((i, 2), (230, 220, 190, 255)); im.putpixel((i, 13), (230, 220, 190, 255))       # blueprint sheet
for j in range(2, 14): im.putpixel((2, j), (230, 220, 190, 255)); im.putpixel((13, j), (230, 220, 190, 255))
for (x, y) in ((5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (5, 9), (9, 5), (7, 4), (7, 10)): im.putpixel((x, y), (60, 110, 200, 255))
im.save(f'{A}/textures/block/furn_main_top.png')
F.model('main_table', [F.box((0, 12, 0), (16, 16, 16), 'main_top', ['up']), F.box((0, 12, 0), (16, 16, 16), 'dark', ['north', 'south', 'east', 'west', 'down']),
                       F.box((1, 0, 1), (4, 12, 4), 'steel'), F.box((12, 0, 1), (15, 12, 4), 'steel'), F.box((1, 0, 12), (4, 12, 15), 'steel'), F.box((12, 0, 12), (15, 12, 15), 'steel'),
                       F.box((2, 3, 2), (14, 4, 14), 'oak'), F.box((3, 4, 3), (8, 7, 8), 'red'), F.box((9, 4, 9), (13, 6, 13), 'steel'),
                       F.box((1, 16, 11), (5, 18, 15), 'steel'), F.box((11, 16, 1), (12, 22, 2), 'dark'), F.box((10, 22, 0.5), (13, 23, 3), 'steel')], particle='dark')
F.w(f'{A}/blockstates/main_table.json', {"variants": {"": {"model": "kiyamet:block/main_table"}}})
F.w(f'{A}/items/main_table.json', {"model": {"type": "minecraft:model", "model": "kiyamet:block/main_table"}})
F.w(f'{D}/loot_table/blocks/main_table.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "kiyamet:main_table"}]}]})
F.w(f'{D}/recipe/main_table.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "PCP", "PPP"],
    "key": {"I": "minecraft:iron_ingot", "P": "#minecraft:planks", "C": "minecraft:crafting_table"}, "result": {"id": "kiyamet:main_table", "count": 1}})
for r in ('cooking_table', 'furniture_table', 'gun_bench', 'lab_table', 'vehicle_workshop'):
    if os.path.exists(f'{D}/recipe/{r}.json'): os.remove(f'{D}/recipe/{r}.json')
c = C()
for x in range(2, 14):
    for y in range(2, 10): c.px(x, y, (150, 108, 64) if x in (2, 13) or y in (2, 9) else (196, 160, 110))
for (x, y, w) in ((4, 4, 3), (8, 4, 4), (4, 6, 5), (10, 6, 2)):
    for i in range(w): c.px(x + i, y, (240, 236, 220))
c.px(11, 4, (220, 50, 40))
for y in range(10, 15): c.px(4, y, (110, 76, 44)); c.px(11, y, (110, 76, 44))
c.outline(); c.save(f'{A}/textures/item/mission_board.png')
json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "kiyamet:item/mission_board"}}, open(f'{A}/models/item/mission_board.json', 'w'))
json.dump({"model": {"type": "minecraft:model", "model": "kiyamet:item/mission_board"}}, open(f'{A}/items/mission_board.json', 'w'))
L['block.kiyamet.main_table'] = 'Main Table'; L['item.kiyamet.mission_board'] = 'Mission Board'
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('main table done')
