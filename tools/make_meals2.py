import json, os, sys
sys.path.insert(0, '/home/claude/repo/tools')
from draw_farm import C, dark, light
A = '/home/claude/repo/src/main/resources/assets/kiyamet'; D = '/home/claude/repo/src/main/resources/data/kiyamet'
def plate(c):
    for x in range(1, 15):
        for y in (12, 13): c.px(x, y, (230, 230, 226) if y == 12 else (190, 190, 186))
def breakfast(c):
    plate(c); c.blob(5, 10, 3, 2, (200, 120, 90)); c.blob(11, 10, 3, 2.2, (250, 250, 240)); c.px(11, 10, (250, 200, 40)); c.px(11, 9, (250, 210, 60))
    for x in range(4, 9): c.px(x, 7, (200, 150, 80))
def fish(c):
    plate(c); c.blob(6, 9, 4, 2.5, (220, 160, 70))
    for x in (10, 11, 12, 13): c.px(x, 8 + x % 2, (240, 210, 110)); c.px(x, 10, (230, 196, 90))
def risotto(c):
    for y in range(9, 14):
        w2 = 7 - (y - 9)
        for x in range(8 - w2, 8 + w2): c.px(x, y, (140, 96, 56))
    for x in range(2, 14): c.px(x, 8, (236, 226, 196)); c.px(x, 7, (246, 236, 210))
    for (x, y, col) in ((4, 7, (150, 100, 70)), (7, 6, (200, 60, 50)), (10, 7, (150, 100, 70)), (12, 7, (240, 240, 240))): c.px(x, y, col)
def ham(c):
    c.blob(8, 9, 6, 4.5, (200, 90, 70))
    for (x, y) in ((5, 7), (8, 6), (11, 8), (6, 10), (10, 11)): c.px(x, y, (240, 170, 50))
    c.px(13, 4, (240, 240, 230)); c.px(12, 5, (240, 240, 230)); c.px(14, 3, (240, 240, 230))
def pancakes(c):
    for k, y in enumerate((12, 10, 8)):
        for x in range(3, 13): c.px(x, y, (220, 160, 80)); c.px(x, y + 1, (180, 120, 60))
    for (x, y) in ((5, 7), (8, 7), (10, 7), (7, 6)): c.px(x, y, (180, 30, 50))
    for x in range(6, 10): c.px(x, 7, (240, 200, 90))
F = {'hunters_breakfast': (breakfast, "Hunter's Breakfast", ["minecraft:cooked_porkchop", "minecraft:egg", "minecraft:bread", "minecraft:baked_potato"], 1),
     'fish_and_chips': (fish, 'Fish and Chips', ["minecraft:cooked_cod", "minecraft:baked_potato", "minecraft:baked_potato", "minecraft:paper"], 1),
     'mushroom_risotto': (risotto, 'Mushroom Risotto', ["minecraft:bowl", "minecraft:brown_mushroom", "minecraft:red_mushroom", "minecraft:wheat", "minecraft:wheat", "minecraft:cooked_chicken"], 1),
     'honey_glazed_ham': (ham, 'Honey Glazed Ham', ["minecraft:cooked_porkchop", "minecraft:cooked_porkchop", "minecraft:honey_bottle", "minecraft:carrot"], 1),
     'berry_pancakes': (pancakes, 'Berry Pancakes', ["minecraft:wheat", "minecraft:wheat", "minecraft:egg", "minecraft:milk_bucket", "minecraft:sweet_berries", "minecraft:sugar"], 2)}
L = json.load(open(A + '/lang/en_us.json'))
for k, (f, t, ings, n) in F.items():
    c = C(); f(c); c.outline(); c.save(f'{A}/textures/item/{k}.png')
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{k}"}}, open(f'{A}/models/item/{k}.json', 'w'))
    json.dump({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{k}"}}, open(f'{A}/items/{k}.json', 'w'))
    json.dump({"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ings, "result": {"id": f"kiyamet:{k}", "count": n}}, open(f'{D}/recipe/{k}.json', 'w'), indent=1)
    L['item.kiyamet.' + k] = t
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('ok')
