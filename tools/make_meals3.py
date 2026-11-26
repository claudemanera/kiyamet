import json, sys
sys.path.insert(0, '/home/claude/repo/tools')
from draw_farm import C, dark, light
A = '/home/claude/repo/src/main/resources/assets/kiyamet'; D = '/home/claude/repo/src/main/resources/data/kiyamet'
def bun(c, top_y=5, bot_y=11):
    for x in range(2, 14):
        for y in range(top_y, top_y + 3): c.px(x, y, (210, 150, 70) if y > top_y else (230, 175, 90))
        c.px(x, bot_y + 2, (200, 140, 64)); c.px(x, bot_y + 3, (170, 110, 50))
    for (x, y) in ((5, top_y), (8, top_y), (11, top_y)): c.px(x, y, (250, 240, 200))
def fish_sandwich(c):
    bun(c)
    for x in range(1, 15): c.px(x, 8, (110, 180, 70)); c.px(x, 9, (230, 170, 90)); c.px(x, 10, (220, 150, 70)); c.px(x, 11, (110, 180, 70))
def burger(c):
    bun(c)
    for x in range(1, 15): c.px(x, 8, (90, 170, 60)); c.px(x, 9, (210, 50, 40)); c.px(x, 10, (110, 60, 40)); c.px(x, 11, (90, 50, 32)); c.px(x, 12, (240, 200, 60) if x % 3 else (90, 50, 32))
def soup(c, col, bits):
    for y in range(9, 14):
        w2 = 7 - (y - 9)
        for x in range(8 - w2, 8 + w2): c.px(x, y, (140, 96, 56) if y > 9 else (110, 76, 44))
    for x in range(2, 14): c.px(x, 8, col); c.px(x, 7, light(col, 0.15))
    for (x, y, cc) in bits: c.px(x, y, cc)
def omelette(c):
    for x in range(1, 15):
        for y in (12, 13): c.px(x, y, (230, 230, 226) if y == 12 else (190, 190, 186))
    c.blob(8, 9.5, 6, 3, (246, 214, 90))
    for (x, y) in ((5, 9), (9, 8), (11, 10)): c.px(x, y, (220, 50, 40))
    for (x, y) in ((7, 10), (10, 9)): c.px(x, y, (90, 170, 60))
F = {'fish_sandwich': (fish_sandwich, 'Fish Sandwich', ["minecraft:bread", "minecraft:cooked_cod", "kiyamet:cabbage", "kiyamet:tomato"], 1),
     'chicken_soup': (lambda c: soup(c, (236, 200, 120), [(4, 7, (240, 140, 40)), (7, 7, (240, 230, 200)), (10, 7, (240, 140, 40)), (12, 8, (90, 160, 60))]), 'Chicken Soup',
                      ["minecraft:bowl", "minecraft:cooked_chicken", "minecraft:carrot", "kiyamet:onion", "minecraft:potato"], 1),
     'burger': (burger, 'Burger', ["minecraft:bread", "minecraft:cooked_beef", "kiyamet:tomato", "kiyamet:cabbage", "kiyamet:onion"], 1),
     'cabbage_salad': (lambda c: soup(c, (150, 210, 110), [(4, 7, (220, 50, 40)), (7, 6, (110, 180, 70)), (10, 7, (240, 240, 230)), (12, 7, (220, 50, 40))]), 'Cabbage Salad',
                       ["minecraft:bowl", "kiyamet:cabbage", "kiyamet:tomato", "kiyamet:cucumber", "kiyamet:onion"], 1),
     'omelette': (omelette, 'Omelette', ["minecraft:egg", "minecraft:egg", "kiyamet:onion", "kiyamet:bell_pepper", "kiyamet:tomato"], 1)}
L = json.load(open(A + '/lang/en_us.json'))
for k, (f, t, ings, n) in F.items():
    c = C(); f(c); c.outline(); c.save(f'{A}/textures/item/{k}.png')
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{k}"}}, open(f'{A}/models/item/{k}.json', 'w'))
    json.dump({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{k}"}}, open(f'{A}/items/{k}.json', 'w'))
    json.dump({"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ings, "result": {"id": f"kiyamet:{k}", "count": n}}, open(f'{D}/recipe/{k}.json', 'w'), indent=1)
    L['item.kiyamet.' + k] = t
L['item.kiyamet.ratatouille'] = 'Baked Vegetables'; L['item.kiyamet.mushroom_risotto'] = 'Chicken and Mushroom Rice'
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('ok')
