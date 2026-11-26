"""TWD 1.9: solar panel, wind turbine, settlement bell block models; electric drill + chainsaw icons; lang + tooltips."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
import make_furniture as F
from draw_farm import C, dark, light
from PIL import Image
A, D = F.A, F.D
L = json.load(open(A + '/lang/en_us.json'))
# solar cells texture
im = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = (24, 40 + y, 96 + y * 3)
        if x % 5 == 0 or y % 5 == 0: c = (180, 186, 196)
        if (x - y) % 7 == 0 and x % 5 and y % 5: c = (90, 130, 200)
        im.putpixel((x, y), c + (255,))
im.save(f'{A}/textures/block/furn_solar.png')
F.tex('blade', (236, 236, 232), 'metal', 5)
F.model('solar_panel', [F.box((0, 4, 0), (16, 5, 16), 'solar', ['up']), F.box((0, 4, 0), (16, 5, 16), 'steel', ['north', 'south', 'east', 'west', 'down']),
                        F.box((7, 0, 7), (9, 4, 9), 'steel'), F.box((3, 0, 3), (13, 1, 13), 'dark'), F.box((0, 5, 0), (16, 5.5, 1), 'steel'), F.box((0, 5, 15), (16, 5.5, 16), 'steel')], particle='steel')
F.model('wind_turbine', [F.box((7, 0, 7), (9, 14, 9), 'blade'), F.box((5, 0, 5), (11, 1, 11), 'dark'), F.box((6.5, 13, 5), (9.5, 16, 10), 'blade'),
                         F.box((7.5, 14, 4), (8.5, 15, 5), 'dark'),
                         F.box((7.6, 15, 3.6), (8.4, 24, 4.2), 'blade'), F.box((-1, 13.6, 3.6), (8, 14.4, 4.2), 'blade'), F.box((8, 13.6, 3.6), (17, 14.4, 4.2), 'blade')], particle='blade')
F.model('settlement_bell', [F.box((4, 0, 4), (12, 1, 12), 'dark'), F.box((7, 1, 7), (9, 14, 9), 'oak'), F.box((5, 12, 5), (11, 13, 11), 'oak'),
                            F.box((5.5, 6, 5.5), (10.5, 11, 10.5), 'gold'), F.box((5, 6, 5), (11, 7, 11), 'gold'), F.box((7.5, 5.5, 7.5), (8.5, 6.5, 8.5), 'dark')], particle='gold')
F.tex('gold', (230, 180, 50), 'metal', 9)
for n in ('solar_panel', 'wind_turbine', 'settlement_bell'):
    F.w(f'{A}/blockstates/{n}.json', {"variants": {"": {"model": f"kiyamet:block/{n}"}}})
    F.w(f'{A}/items/{n}.json', {"model": {"type": "minecraft:model", "model": f"kiyamet:block/{n}"}})
    F.w(f'{D}/loot_table/blocks/{n}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"kiyamet:{n}"}]}]})
# tool icons
def drill(c):
    for x in range(3, 10):
        for y in range(5, 10): c.px(x, y, (230, 170, 30) if y > 5 else (250, 200, 60))
    for y in range(10, 15): c.px(5, y, (40, 40, 44)); c.px(6, y, (60, 60, 64)); c.px(7, y, (40, 40, 44))
    for x in range(10, 15): c.px(x, 7, (170, 176, 186)); c.px(x, 6 if x % 2 else 8, (130, 136, 146))
    c.px(15, 7, (210, 214, 222)); c.px(4, 6, (30, 30, 30)); c.px(8, 8, (60, 200, 90))
def chainsaw(c):
    for x in range(1, 7):
        for y in range(6, 12): c.px(x, y, (220, 90, 30) if y > 6 else (240, 120, 50))
    for x in range(7, 15):
        c.px(x, 7, (170, 176, 186)); c.px(x, 8, (150, 156, 166)); c.px(x, 9, (170, 176, 186))
        c.px(x, 6, (60, 60, 64) if x % 2 else (0, 0, 0, 0)) if x % 2 else None; c.px(x, 10, (60, 60, 64)) if x % 2 == 0 else None
    for x in range(2, 6): c.px(x, 5, (40, 40, 44))
    c.px(2, 12, (40, 40, 44)); c.px(5, 12, (40, 40, 44)); c.px(3, 9, (30, 30, 30))
for n, f in (('electric_drill', drill), ('chainsaw', chainsaw)):
    c = C(); f(c); c.outline(); c.save(f'{A}/textures/item/{n}.png')
    json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": f"kiyamet:item/{n}"}}, open(f'{A}/models/item/{n}.json', 'w'))
    json.dump({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{n}"}}, open(f'{A}/items/{n}.json', 'w'))
L.update({'block.kiyamet.solar_panel': 'Solar Panel', 'block.kiyamet.wind_turbine': 'Wind Turbine', 'block.kiyamet.settlement_bell': 'Settlement Bell',
          'item.kiyamet.electric_drill': 'Electric Drill', 'item.kiyamet.chainsaw': 'Chainsaw',
          'tooltip.kiyamet.solar_panel': 'Powers everything within 8 blocks|while the sun is up and it sees the sky|No fuel needed',
          'tooltip.kiyamet.wind_turbine': 'Powers everything within 8 blocks|day and night (needs open sky or height)',
          'tooltip.kiyamet.electric_drill': 'Mines 3x3 · sneak to mine one block|Charges while you stand in power',
          'tooltip.kiyamet.chainsaw': 'Fells a whole tree at once · sneak for one log|Charges while you stand in power',
          'tooltip.kiyamet.settlement_bell': 'Found your own settlement|Companion panel → Settle here · chests within 12 blocks are the stockpile|Settlers eat 1 food each morning and need beds',
          'tooltip.kiyamet.main_table': 'Builds every TWD station: furniture, cooking, guns,|lab, vehicles, power, the settlement bell and mission boards',
          'tooltip.kiyamet.cooking_table': 'All 30 meals are cooked here|Shift-click a dish to cook many',
          'tooltip.kiyamet.mission_board': 'Place your own mission board: 3 new jobs every day'})
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('power done')
