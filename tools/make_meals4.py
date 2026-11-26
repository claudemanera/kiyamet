"""TWD 1.9: 10 wild-game meals, the Cooking Table (all meals are cooked there now), and its Java recipe list."""
import json, os, sys, glob
sys.path.insert(0, os.path.dirname(__file__))
import make_furniture as F           # re-runs furniture art (deterministic); gives us box/model/tex
from draw_farm import C, dark, light, mix
A, D = F.A, F.D
L = json.load(open(A + '/lang/en_us.json'))

def bowl(c, col, bits):
    for y in range(9, 14):
        w2 = 7 - (y - 9)
        for x in range(8 - w2, 8 + w2): c.px(x, y, (140, 96, 56) if y > 9 else (110, 76, 44))
    for x in range(2, 14): c.px(x, 8, col); c.px(x, 7, light(col, 0.15))
    for (x, y, cc) in bits: c.px(x, y, cc)
def plate(c):
    for x in range(1, 15): c.px(x, 12, (236, 236, 232)); c.px(x, 13, (196, 196, 192))
    for x in range(3, 13): c.px(x, 14, (170, 170, 166))
def steak(c, col, y0=6):
    c.blob(8, y0 + 3, 5.5, 3, col)
    for x in range(4, 13, 3): c.px(x, y0 + 2, dark(col, 0.6)); c.px(x + 1, y0 + 3, dark(col, 0.6))
def venison_stew(c): bowl(c, (120, 60, 34), [(4, 7, (150, 40, 40)), (7, 7, (240, 140, 40)), (10, 7, (150, 40, 40)), (12, 8, (230, 210, 140))])
def bear_steak(c): plate(c); steak(c, (110, 56, 34)); [c.px(x, 11, (220, 190, 120)) for x in (2, 3, 12, 13)]
def boar_sausages(c):
    plate(c)
    for row in (7, 9):
        for x in range(2, 14): c.px(x, row, (150, 70, 46)); c.px(x, row + 1, (120, 52, 34))
        c.px(2, row, (100, 40, 26)); c.px(13, row, (100, 40, 26))
def moose_roast(c):
    plate(c); c.blob(8, 8, 6, 4, (100, 50, 30))
    for (x, y) in ((5, 6), (8, 5), (11, 6)): c.px(x, y, (150, 90, 50))
    for x in (2, 13): c.px(x, 11, (240, 140, 40))
def skewers(c):
    for i in range(14): c.px(1 + i, 14 - i, (170, 130, 80))
    for k, col in enumerate(((150, 60, 40), (90, 160, 60), (150, 60, 40), (220, 50, 40), (150, 60, 40))):
        cx, cy = 3 + k * 2.4, 12 - k * 2.4; c.blob(cx, cy, 1.4, 1.4, col)
def croc_wrap(c):
    c.blob(8, 8, 6.5, 4, (226, 196, 140), rot=-0.6)
    for (x, y, cc) in ((6, 8, (90, 170, 60)), (8, 7, (210, 50, 40)), (10, 7, (230, 200, 180)), (7, 6, (90, 170, 60))): c.px(x, y, cc)
def hunters_pie(c):
    for y in range(8, 14):
        for x in range(2, 14): c.px(x, y, (180, 120, 60) if y > 8 else (220, 170, 90))
    for x in range(3, 13, 2): c.px(x, 9, (120, 60, 40))
    c.px(7, 8, (110, 50, 30)); c.px(8, 8, (110, 50, 30))
def bear_chili(c): bowl(c, (170, 50, 30), [(4, 7, (110, 50, 30)), (7, 7, (240, 200, 80)), (10, 7, (110, 50, 30)), (12, 7, (90, 160, 60))])
def jerky(c):
    for k in range(3):
        for i in range(10): c.px(3 + i + k, 4 + k * 3 + (i % 3 == 0), (110, 50, 34)); c.px(3 + i + k, 5 + k * 3 + (i % 3 == 0), (80, 36, 24))
def meatballs(c): bowl(c, (200, 60, 40), [(5, 7, (110, 60, 40)), (6, 7, (130, 70, 46)), (9, 7, (110, 60, 40)), (10, 7, (130, 70, 46)), (7, 6, (110, 60, 40))])

# id, title, food, sat, days, fedMinutes, bowl, count, ingredients, icon
NEW = [("venison_stew", "Venison Stew", 14, 1.4, 6, 8, True, 1, ["minecraft:bowl", "kiyamet:venison", "minecraft:potato", "minecraft:carrot", "kiyamet:onion"], venison_stew),
       ("bear_steak", "Bear Steak and Potatoes", 16, 1.5, 5, 9, False, 1, ["kiyamet:cooked_bear_meat", "minecraft:baked_potato", "minecraft:baked_potato", "kiyamet:onion"], bear_steak),
       ("boar_sausages", "Boar Sausages", 12, 1.3, 10, 6, False, 2, ["kiyamet:boar_meat", "kiyamet:boar_meat", "kiyamet:onion", "kiyamet:bell_pepper"], boar_sausages),
       ("moose_roast", "Moose Roast", 18, 1.5, 6, 10, False, 2, ["kiyamet:moose_meat", "minecraft:carrot", "minecraft:potato", "kiyamet:onion", "kiyamet:tomato"], moose_roast),
       ("meat_skewers", "Meat Skewers", 12, 1.3, 4, 6, False, 2, ["kiyamet:big_cat_meat", "kiyamet:bell_pepper", "kiyamet:onion", "minecraft:stick", "minecraft:stick"], skewers),
       ("croc_wrap", "Crocodile Wrap", 12, 1.3, 4, 6, False, 1, ["kiyamet:cooked_croc_meat", "minecraft:bread", "kiyamet:tomato", "kiyamet:cabbage"], croc_wrap),
       ("hunters_pie", "Hunter's Pie", 16, 1.4, 8, 9, False, 2, ["kiyamet:venison", "minecraft:wheat", "minecraft:wheat", "minecraft:egg", "minecraft:brown_mushroom"], hunters_pie),
       ("bear_chili", "Bear Chili", 14, 1.4, 6, 8, True, 1, ["minecraft:bowl", "kiyamet:bear_meat", "kiyamet:tomato", "kiyamet:bell_pepper", "kiyamet:onion"], bear_chili),
       ("jerky", "Jerky", 8, 1.1, 120, 4, False, 3, ["kiyamet:venison", "kiyamet:moose_meat", "kiyamet:boar_meat"], jerky),
       ("meatballs", "Meatballs in Tomato Sauce", 14, 1.4, 5, 8, True, 1, ["minecraft:bowl", "kiyamet:boar_meat", "minecraft:bread", "minecraft:egg", "kiyamet:tomato"], meatballs)]
for (k, t, *_r, icon) in NEW:
    c = C(); icon(c); c.outline(); c.save(f'{A}/textures/item/{k}.png')
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{k}"}}, open(f'{A}/models/item/{k}.json', 'w'))
    json.dump({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{k}"}}, open(f'{A}/items/{k}.json', 'w'))
    L['item.kiyamet.' + k] = t

# ---- every meal moves to the Cooking Table: read the old shapeless recipes, then delete them
J = '/home/claude/repo/src/main/java/com/kiyamet/food/Recipes.java'
meal_ids = __import__('re').findall(r'new Meal\("(\w+)"', open(J).read())
cook = []
for m in meal_ids:
    p = f'{D}/recipe/{m}.json'
    if os.path.exists(p):
        d = json.load(open(p)); cook.append((m, d['result'].get('count', 1), d['ingredients'])); os.remove(p)
for (k, t, food, sat, days, fed, bw, n, ings, _i) in NEW:
    if k not in meal_ids: cook.append((k, n, ings))
json.dump([{"meal": m, "count": n, "ingredients": i} for m, n, i in cook], open(f'{D}/cooking.json', 'w'), indent=1)
src = open(J).read()
add = ''.join(f',\n            new Meal("{k}", "{t}", {food}, {sat}f, {days}, {fed}, {"true" if bw else "false"})' for (k, t, food, sat, days, fed, bw, *_r) in NEW if k not in meal_ids)
if add: src = src.replace('new Meal("omelette", "Omelette", 10, 1.2f, 3, 5, false));', 'new Meal("omelette", "Omelette", 10, 1.2f, 3, 5, false)' + add + ');'); open(J, 'w').write(src)
java = ',\n'.join(f'        new Cook("{m}", {n}, ' + ', '.join(f'"{x}"' for x in i) + ')' for m, n, i in cook)
open('/home/claude/repo/src/main/java/com/kiyamet/food/CookList.java', 'w').write(
    'package com.kiyamet.food;\n\n/** Generated by tools/make_meals4.py - what each meal needs at the Cooking Table. */\npublic final class CookList {\n'
    '    public record Cook(String meal, int count, String... ingredients) {}\n    public static final java.util.List<Cook> ALL = java.util.List.of(\n' + java + ');\n    private CookList() {}\n}\n')

# ---- Cooking Table block: counter with a stove top, a pot and a cutting board
F.tex('counter_top', (200, 200, 196), 'metal', 11)
F.model('cooking_table', [F.box((0, 0, 0), (16, 13, 16), 'oak', ['north', 'south', 'east', 'west', 'down']), F.box((0, 13, 0), (16, 14, 16), 'counter_top'),
                          F.box((1, 2, -0.1), (7, 11, 0.2), 'dark', ['north']), F.box((9, 2, -0.1), (15, 11, 0.2), 'dark', ['north']),
                          F.box((6, 9, -0.4), (7, 10, 0.1), 'steel'), F.box((9, 9, -0.4), (10, 10, 0.1), 'steel'),
                          F.box((2, 14, 2), (8, 18, 8), 'steel'), F.box((1.5, 18, 1.5), (8.5, 18.6, 8.5), 'dark'), F.box((4.5, 18.6, 4.5), (5.5, 19.4, 5.5), 'dark'),
                          F.box((9, 14, 8), (15, 14.8, 14), 'oak'), F.box((10, 14.8, 10), (13, 15.1, 11), 'steel'), F.box((10, 14.8, 12), (12, 15.4, 13), 'red')], particle='oak')
F.w(f'{A}/blockstates/cooking_table.json', {"variants": {"": {"model": "kiyamet:block/cooking_table"}}})
F.w(f'{A}/items/cooking_table.json', {"model": {"type": "minecraft:model", "model": "kiyamet:block/cooking_table"}})
F.w(f'{D}/loot_table/blocks/cooking_table.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "kiyamet:cooking_table"}]}]})
F.w(f'{D}/recipe/cooking_table.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["IBI", "PFP", "PPP"],
    "key": {"I": "minecraft:iron_ingot", "B": "minecraft:bowl", "F": "minecraft:furnace", "P": "#minecraft:planks"}, "result": {"id": "kiyamet:cooking_table", "count": 1}})
L['block.kiyamet.cooking_table'] = 'Cooking Table'
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('meals4 done', len(cook))
