"""TWD 2.1: the fourteen sages, each one a different person - own face, hair, beard, headwear, robe, staff -
painted at 4x resolution (512x512 sheets) with faces, robe trims, hem patterns and the element logo on the chest,
plus the spinning element logo floating above their heads."""
import json, math, os, random, sys
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(__file__))
from make_animals import Beast, dark, light, mix
from hires import face, upscale, paste, logo_on
from make_logos import LOGOS, COL
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')

# id: name, female, skin, hair, eyes, robe, trim, hat, beard, hair style, staff, width, old
SAGES = {
    'flame':     ('Ignar the Ember-Keeper',   False, (196, 140, 110), (236, 236, 230), (200, 90, 30),  (150, 40, 26),  (240, 170, 60),  'hood',      'long',     'none',  'orb',     1.0, True),
    'lightning': ('Voltra Skyborn',           True,  (226, 188, 160), (246, 222, 140), (90, 130, 220), (236, 226, 190), (230, 190, 50), 'circlet',   'none',     'long',  'orb',     0.95, False),
    'earth':     ('Old Brannoc Stonebeard',   False, (176, 128, 92),  (150, 140, 128), (90, 120, 60),  (96, 78, 52),   (120, 160, 70),  'leafcrown', 'braided',  'none',  'crystal', 1.15, True),
    'shadow':    ('Nyxa the Veiled',          True,  (60, 50, 70),    (30, 24, 40),    (200, 120, 255), (40, 26, 60),   (150, 90, 220),  'deephood',  'none',     'none',  'crescent', 0.95, False),
    'smoke':     ('Ashwin Greycloak',         False, (200, 160, 130), (170, 170, 170), (110, 110, 110), (96, 96, 100),  (160, 160, 164), 'widebrim',  'long',     'short', 'lantern', 1.0, True),
    'spirit':    ('Elowen Soulwhisper',       True,  (236, 230, 236), (250, 250, 255), (120, 240, 230), (226, 240, 240), (120, 230, 220), 'halo',      'none',     'long',  'lantern', 0.95, False),
    'electric':  ('Tesmir Coilhand',          False, (210, 160, 130), (90, 60, 40),    (60, 170, 240), (40, 70, 120),  (210, 120, 50),  'goggles',   'mustache', 'short', 'coil',    1.0, False),
    'thunder':   ('Thorvald Stormcaller',     False, (220, 170, 140), (200, 80, 40),   (80, 110, 240), (70, 80, 110),  (180, 180, 190), 'horned',    'braided',  'short', 'hammer',  1.12, True),
    'ice':       ('Frida Rimeheart',          True,  (236, 220, 220), (240, 246, 255), (120, 200, 255), (150, 200, 236), (250, 252, 255), 'icecrown',  'none',     'long',  'crystal', 0.95, False),
    'fire':      ('Kaelen Pyreborn',          False, (120, 80, 56),   (30, 24, 22),    (230, 140, 40), (200, 100, 30), (60, 40, 30),    'turban',    'short',    'none',  'brazier', 1.05, False),
    'water':     ('Marisol Tidesinger',       True,  (184, 136, 104), (40, 30, 34),    (40, 160, 200), (30, 120, 140), (230, 210, 170), 'shellcrown', 'none',    'long',  'trident', 0.95, False),
    'air':       ('Aeris the Unbound',        True,  (210, 160, 120), (60, 44, 36),    (130, 190, 230), (200, 226, 246), (255, 255, 255), 'feathers',  'none',     'long',  'cloud',   0.95, False),
    'wind':      ('Zephyrin Gale-Walker',     False, (216, 176, 140), (40, 40, 40),    (90, 170, 110), (80, 150, 90),  (230, 240, 220), 'straw',     'mustache', 'short', 'feather', 1.0, True),
    'metal':     ('Ferros Ironmind',          False, (200, 160, 136), (120, 120, 120), (200, 60, 50),  (90, 96, 104),  (200, 206, 214), 'bald',      'short',    'none',  'gear',    1.08, True),
}


def build(eid, spec):
    name, fem, skin, hair, eyec, robe, trim, hat, beard, hairst, staff, wb, old = spec
    col = COL[eid]
    b = Beast(f'{eid}_sage', 128, 128)
    tag = {}
    def cube(bone, o, sz, mat, key=None, **k):
        b.cube(bone, o, sz, mat, **k)
        if key: tag[key] = len(b.uvs) - 1
    b.bone('root')
    b.bone('body', 'root', pivot=(0, 24, 0)); b.bone('head', 'body', pivot=(0, 24, 0))
    b.bone('arm_r', 'body', pivot=(-5 * wb, 22, 0)); b.bone('arm_l', 'body', pivot=(5 * wb, 22, 0))
    b.bone('leg_r', 'root', pivot=(-2, 12, 0)); b.bone('leg_l', 'root', pivot=(2, 12, 0))
    b.bone('weapon', 'arm_r', pivot=(-6 * wb, 11, -1)); b.bone('emblem', 'head', pivot=(0, 47, 0))
    W = 8 * wb
    # legs: only the boots show under the robe
    cube('leg_r', (-3.8, 0, -2), (3.6, 12, 4), 'boot'); cube('leg_l', (0.2, 0, -2), (3.6, 12, 4), 'boot')
    # torso + robe
    cube('body', (-W / 2, 12, -2), (W, 12, 4), 'robe', 'chest')
    cube('body', (-W / 2 - 0.6, 0.6, -2.7), (W + 1.2, 12, 5.4), 'robe', 'skirt')
    cube('body', (-W / 2 - 0.8, 11, -2.9), (W + 1.6, 1.6, 5.8), 'trim')                       # belt
    cube('body', (W / 2 - 1, 8.5, -3.3), (2, 2.6, 1.2), 'leather')                           # pouch
    cube('body', (-W / 2 - 0.9, 20.5, -3), (W + 1.8, 3.6, 6), 'mantle')                       # shoulder mantle
    cube('body', (-1.2, 12.5, -2.45), (2.4, 8, 0.4), 'trim')                                  # front band
    # head
    cube('head', (-4, 24, -4), (8, 8, 8), 'skin', 'head')
    if hairst == 'long': cube('head', (-4.3, 15, 1.2), (8.6, 17.4, 3.4), 'hair')
    if hairst == 'short' or (hairst == 'none' and hat in ('bald',)): pass
    if hairst != 'none' or hat not in ('bald', 'turban', 'deephood', 'hood'):
        if hat not in ('bald',): cube('head', (-4.2, 29.5, -4.2), (8.4, 2.8, 8.4), 'hair')       # hair cap
    if fem and hairst == 'long': cube('head', (-4.3, 28, -4.3), (8.6, 1.6, 0.4), 'hair')        # fringe
    # beard
    if beard in ('long', 'braided'):
        cube('head', (-3.2, 17.5 if beard == 'long' else 18.5, -4.6), (6.4, 7.5 if beard == 'long' else 6.5, 1.4), 'beard')
        cube('head', (-3.5, 25.6, -4.6), (7, 1, 0.6), 'beard')
        if beard == 'braided':
            for x in (-2, 1): cube('head', (x, 12, -4.4), (1.2, 6.5, 1.2), 'beard'); cube('head', (x - 0.1, 12, -4.5), (1.4, 0.8, 1.4), 'trim')
    elif beard == 'short': cube('head', (-3.6, 23.3, -4.5), (7.2, 3, 1.0), 'beard')
    elif beard == 'mustache':
        cube('head', (-3.6, 25.4, -4.5), (7.2, 1, 0.6), 'beard')
        if eid == 'wind': cube('head', (-4.6, 21, -4.4), (1, 4.6, 0.6), 'beard'); cube('head', (3.6, 21, -4.4), (1, 4.6, 0.6), 'beard')
    # headwear
    top = 32
    if hat == 'hood':
        cube('head', (-4.6, 25.5, -4.6), (9.2, 7.4, 9.2), 'robe'); cube('head', (-4.6, 20.5, 1.5), (9.2, 5, 3.2), 'robe'); top = 33
    elif hat == 'deephood':
        cube('head', (-4.8, 23.5, -5.2), (9.6, 9.6, 9.8), 'robe'); cube('head', (-4.8, 18, 1.5), (9.6, 6, 3.4), 'robe'); top = 33.4
    elif hat == 'circlet':
        cube('head', (-4.35, 29.4, -4.35), (8.7, 1, 8.7), 'trim')
        for x, h in ((-2.5, 2), (-0.5, 3.5), (1.5, 2)): cube('head', (x, 30.4, -4.5), (1, h, 0.6), 'glow')
    elif hat == 'leafcrown':
        cube('head', (-4.35, 30, -4.35), (8.7, 1, 8.7), 'leaf')
        for a in range(8):
            x, z = math.cos(a * math.pi / 4) * 4.2, math.sin(a * math.pi / 4) * 4.2
            cube('head', (x - 0.8, 31, z - 0.8), (1.6, 2.2, 1.6), 'leaf', rot=(0, a * 45, 0), pivot=(x, 31, z))
    elif hat == 'widebrim':
        cube('head', (-7, 32, -7), (14, 0.8, 14), 'hatc'); cube('head', (-4.2, 32.8, -4.2), (8.4, 4.2, 8.4), 'hatc'); cube('head', (-4.3, 33, -4.3), (8.6, 1, 8.6), 'trim'); top = 37
    elif hat == 'halo':
        cube('head', (-4, 34.5, -4), (8, 0.6, 8), 'glow'); top = 35
    elif hat == 'goggles':
        cube('head', (-4.4, 28.6, -4.4), (8.8, 1.2, 8.8), 'leather')
        for x in (-3.3, 0.5): cube('head', (x, 28.1, -4.9), (2.8, 2.2, 0.8), 'lens')
    elif hat == 'horned':
        cube('head', (-4.6, 28, -4.6), (9.2, 4.8, 9.2), 'metal'); cube('head', (-0.6, 25, -4.9), (1.2, 4, 0.5), 'metal')
        for s in (-1, 1): cube('head', (s * 4.6 - (1.2 if s < 0 else 0), 30, -1), (1.2, 6, 1.4), 'horn', rot=(0, 0, s * -35), pivot=(s * 4.6, 30, 0))
        top = 37
    elif hat == 'icecrown':
        cube('head', (-4.35, 29.4, -4.35), (8.7, 1, 8.7), 'glow')
        for x, h in ((-3, 3), (-1, 5), (1, 5), (3, 3)): cube('head', (x - 0.5, 30.4, -4.4), (1, h, 1), 'ice', rot=(0, 45, 0), pivot=(x, 30.4, -4))
        top = 36
    elif hat == 'turban':
        cube('head', (-4.7, 28.5, -4.7), (9.4, 4.6, 9.4), 'hatc'); cube('head', (-1.5, 30, -5), (3, 2.4, 0.6), 'glow'); cube('head', (-2.5, 33, -2.5), (5, 1.6, 5), 'hatc'); top = 35
    elif hat == 'shellcrown':
        cube('head', (-4.35, 29.6, -4.35), (8.7, 1, 8.7), 'trim')
        for x in (-3, 0, 3): cube('head', (x - 1, 30.6, -4.5), (2, 2.2, 0.8), 'shell')
    elif hat == 'feathers':
        cube('head', (-4.35, 29.4, -4.35), (8.7, 1.2, 8.7), 'trim')
        for i, x in enumerate((-3.5, -1.5, 0.5, 2.5)): cube('head', (x, 30.6, -4.4), (1, 4 + (i % 2) * 2, 0.4), 'feather', rot=(0, 0, (x + 0.5) * -6), pivot=(x, 30.6, -4.2))
        top = 37
    elif hat == 'straw':
        cube('head', (-8, 32, -8), (16, 0.8, 16), 'hatc'); cube('head', (-4.2, 32.8, -4.2), (8.4, 2.4, 8.4), 'hatc'); cube('head', (-2.2, 35.2, -2.2), (4.4, 1.6, 4.4), 'hatc'); top = 37
    elif hat == 'bald':
        cube('head', (-4.4, 27.2, -4.6), (2.6, 2.4, 0.5), 'lens')                                  # monocle
    # arms with wide sleeves
    for n, x in (('arm_r', -W / 2 - 4), ('arm_l', W / 2)):
        metal_arm = eid == 'metal' and n == 'arm_l'
        cube(n, (x, 12, -2), (4, 12, 4), 'metal' if metal_arm else 'robe')
        if not metal_arm: cube(n, (x - 0.5, 11, -2.5), (5, 6, 5), 'robe'); cube(n, (x - 0.55, 11, -2.55), (5.1, 1, 5.1), 'trim')
        else:
            for y in (14, 18, 22): cube(n, (x - 0.2, y, -2.2), (4.4, 0.8, 4.4), 'trim')
        cube(n, (x + 0.4, 9.5, -1.6), (3.2, 2, 3.2), 'metal' if metal_arm else 'skin')               # hand
    # the staff
    hx = -W / 2 - 2
    cube('weapon', (hx - 0.6, -1, -1.6), (1.2, 27, 1.2), 'wood')
    for y in (8, 16): cube('weapon', (hx - 0.75, y, -1.75), (1.5, 0.8, 1.5), 'trim')
    T = 26
    if staff == 'orb':
        cube('weapon', (hx - 1.7, T, -2.7), (3.4, 3.4, 3.4), 'glow')
        for dx, dz in ((-1.5, 0), (1.5, 0), (0, -1.5), (0, 1.5)): cube('weapon', (hx + dx - 0.3, T - 1, -1 + dz - 0.3), (0.6, 4.8, 0.6), 'trim')
    elif staff == 'crystal':
        cube('weapon', (hx - 1.2, T + 0.5, -2.2), (2.4, 4.5, 2.4), 'glow', rot=(0, 45, 0), pivot=(hx, T, -1))
        cube('weapon', (hx - 1.6, T - 0.5, -2.6), (3.2, 1.2, 3.2), 'wood')
    elif staff == 'crescent':
        cube('weapon', (hx - 0.4, T, -4), (0.8, 1, 6), 'glow'); cube('weapon', (hx - 0.4, T + 1, -4.6), (0.8, 3, 1), 'glow'); cube('weapon', (hx - 0.4, T + 1, 1.6), (0.8, 3, 1), 'glow')
    elif staff == 'lantern':
        cube('weapon', (hx - 0.3, T, -1.3), (0.6, 2, 0.6), 'metal'); cube('weapon', (hx - 1.5, T - 3.5, -2.5), (3, 3.6, 3), 'glow')
        cube('weapon', (hx - 1.7, T - 0.2, -2.7), (3.4, 0.6, 3.4), 'metal'); cube('weapon', (hx - 1.7, T - 4, -2.7), (3.4, 0.6, 3.4), 'metal')
    elif staff == 'coil':
        for y in range(4): cube('weapon', (hx - 1.1, T - 3 + y * 1.2, -2.1), (2.2, 0.6, 2.2), 'copper')
        cube('weapon', (hx - 1.4, T + 1.8, -2.4), (2.8, 2.8, 2.8), 'glow')
    elif staff == 'hammer':
        cube('weapon', (hx - 1.8, T, -4.5), (3.6, 3.6, 7), 'metal'); cube('weapon', (hx - 1.9, T + 1.2, -2), (3.8, 1.2, 2), 'glow')
    elif staff == 'trident':
        cube('weapon', (hx - 0.5, T, -3.5), (1, 1, 5), 'trim')
        for dz in (-3, -1, 1): cube('weapon', (hx - 0.4, T + 1, dz - 0.4), (0.8, 4 if dz == -1 else 3, 0.8), 'glow')
    elif staff == 'brazier':
        cube('weapon', (hx - 2, T, -3), (4, 1.5, 4), 'metal'); cube('weapon', (hx - 1.4, T + 1.5, -2.4), (2.8, 2.4, 2.8), 'glow')
    elif staff == 'cloud':
        cube('weapon', (hx - 2, T + 0.5, -2.5), (4, 2.4, 3), 'glow'); cube('weapon', (hx - 1.2, T + 2.4, -2), (2.4, 1.6, 2), 'glow')
    elif staff == 'feather':
        cube('weapon', (hx - 0.4, T, -6), (0.8, 1.2, 6), 'metal'); cube('weapon', (hx - 0.3, T - 3, -6.3), (0.6, 4.2, 1.2), 'glow')
        for i in range(3): cube('weapon', (hx - 0.2, T - 2 - i * 1.5, -0.4), (0.4, 1.4, 2.6), 'feather')
    elif staff == 'gear':
        cube('weapon', (hx - 0.4, T, -3), (0.8, 4, 4), 'metal'); cube('weapon', (hx - 0.5, T + 1.2, -1.8), (1, 1.6, 1.6), 'glow')
        for a in range(8): cube('weapon', (hx - 0.35, T + 2 + math.sin(a * math.pi / 4) * 2.6 - 0.5, -1 + math.cos(a * math.pi / 4) * 2.6 - 0.5), (0.7, 1, 1), 'metal')
    # floating logo
    cube('emblem', (-3.5, top + 7, -0.3), (7, 7, 0.6), 'emblem', 'emblem')
    b.bones['emblem']['pivot'] = [0, round(top + 10.5, 2), 0]

    mats = dict(skin=(skin, 'skin'), hair=(hair, 'mane'), beard=(hair, 'mane'), robe=(robe, 'cloth'), trim=(trim, 'metal'), mantle=(dark(robe, 0.8), 'cloth'),
                leather=((110, 74, 44), 'fur'), boot=((70, 50, 36), 'fur'), wood=((110, 76, 44), 'fur'), glow=(light(col, 0.3), 'glow'), metal=((160, 166, 176), 'metal'),
                horn=((230, 220, 196), 'horn'), leaf=((90, 160, 60), 'fur'), hatc=(dark(robe, 0.9) if hat != 'straw' else (214, 186, 120), 'cloth'), lens=((60, 200, 230), 'glow'),
                ice=((200, 240, 255), 'glow'), shell=((246, 214, 200), 'horn'), feather=((250, 250, 250), 'fur'), copper=((200, 120, 60), 'metal'), emblem=((255, 255, 255), 'skin'),
                eye=((20, 20, 20), 'eye'))
    anims = {
        "misc.idle": {"loop": True, "animation_length": 4.0, "bones": {
            "body": {"position": {"0.00": [0, 0, 0], "2.00": [0, -0.3, 0], "4.00": [0, 0, 0]}},
            "arm_r": {"rotation": {"0.00": [-28, 0, 0], "2.00": [-32, 0, 2], "4.00": [-28, 0, 0]}}, "weapon": {"rotation": {"0.00": [28, 0, 0]}},
            "arm_l": {"rotation": {"0.00": [-10, 0, -4], "2.00": [-6, 0, -8], "4.00": [-10, 0, -4]}},
            "head": {"rotation": {"0.00": [0, 0, 0], "1.30": [3, 12, 0], "2.60": [-2, -10, 0], "4.00": [0, 0, 0]}}}},
        "move.walk": {"loop": True, "animation_length": 1.2, "bones": {
            "leg_r": {"rotation": {"0.00": [24, 0, 0], "0.60": [-24, 0, 0], "1.20": [24, 0, 0]}}, "leg_l": {"rotation": {"0.00": [-24, 0, 0], "0.60": [24, 0, 0], "1.20": [-24, 0, 0]}},
            "arm_r": {"rotation": {"0.00": [-30, 0, 0], "0.60": [-18, 0, 0], "1.20": [-30, 0, 0]}}, "weapon": {"rotation": {"0.00": [28, 0, 0]}},
            "arm_l": {"rotation": {"0.00": [20, 0, 0], "0.60": [-20, 0, 0], "1.20": [20, 0, 0]}},
            "body": {"position": {"0.00": [0, 0, 0], "0.30": [0, 0.4, 0], "0.60": [0, 0, 0], "0.90": [0, 0.4, 0], "1.20": [0, 0, 0]}}}},
        "attack.swing": {"loop": False, "animation_length": 0.7, "bones": {
            "arm_r": {"rotation": {"0.00": [-28, 0, 0], "0.25": [-160, 10, 0], "0.45": [10, -10, 0], "0.70": [-28, 0, 0]}},
            "weapon": {"rotation": {"0.00": [28, 0, 0], "0.25": [0, 0, 0], "0.45": [40, 0, 0], "0.70": [28, 0, 0]}},
            "body": {"rotation": {"0.00": [0, 0, 0], "0.25": [-5, 14, 0], "0.45": [6, -18, 0], "0.70": [0, 0, 0]}}}}}
    for clip in anims.values():
        L = clip["animation_length"]
        clip["bones"]["emblem"] = {"rotation": {"0.00": [0, 0, 0], f'{L:.2f}': [0, 360 * max(1, round(L / 3)) if L >= 1 else 120, 0]},
                                   "position": {"0.00": [0, 0, 0], f'{L / 2:.2f}': [0, 0.7, 0], f'{L:.2f}': [0, 0, 0]}}
    b.save(mats, anims, scale_bounds=5)

    # ---- the 4x pass: faces, robe details, logo
    path = f'{A}/textures/entity/{eid}_sage.png'
    big = upscale(path, seed=hash(eid) & 0xffff)
    d = ImageDraw.Draw(big)
    x, y, w, h = face(b, tag['head'], 'front')
    r = random.Random(eid)
    shadowed = hat == 'deephood'
    if shadowed: d.rectangle([x, y, x + w - 1, y + h - 1], fill=(14, 10, 20, 255))
    else:
        # cheeks, nose, mouth, wrinkles
        d.rectangle([x + 14, y + 13, x + 17, y + 21], fill=dark(skin, 0.86) + (255,))
        d.rectangle([x + 13, y + 20, x + 18, y + 21], fill=dark(skin, 0.75) + (255,))
        if beard in ('none', 'mustache'): d.rectangle([x + 11, y + 24, x + 20, y + 25], fill=(140, 70, 60, 255) if fem else dark(skin, 0.6) + (255,))
        if fem:
            for cx in (x + 6, x + 22): d.rectangle([cx, y + 18, cx + 3, y + 19], fill=mix(skin, (230, 120, 120), 0.35) + (255,))
        if old:
            for k in (4, 7): d.line([(x + 8, y + k), (x + 23, y + k)], fill=dark(skin, 0.82) + (255,), width=1)
            d.line([(x + 4, y + 16), (x + 5, y + 18)], fill=dark(skin, 0.8) + (255,)); d.line([(x + 27, y + 16), (x + 26, y + 18)], fill=dark(skin, 0.8) + (255,))
    for ex in (x + 6, x + 19):
        if shadowed: d.rectangle([ex + 1, y + 13, ex + 5, y + 15], fill=light(eyec, 0.3) + (255,)); continue
        d.rectangle([ex, y + 12, ex + 6, y + 15], fill=(244, 244, 240, 255))
        d.rectangle([ex + 2, y + 12, ex + 4, y + 15], fill=eyec + (255,)); d.rectangle([ex + 3, y + 13, ex + 3, y + 14], fill=(16, 16, 16, 255))
        d.point((ex + 2, y + 12), fill=(255, 255, 255, 255))
        d.rectangle([ex - 1, y + 9, ex + 7, y + 10], fill=dark(hair, 0.85) + (255,))                    # brows
        if fem: d.line([(ex - 1, y + 11), (ex + 7, y + 11)], fill=(40, 30, 30, 255))                    # lashes
    if eid == 'fire':
        for k in range(3): d.line([(x + 2 + k * 2, y + 2), (x + 4 + k * 2, y + 8)], fill=(230, 120, 30, 255), width=1)   # ember tattoos
    # chest: robe front with trim and the logo
    cx_, cy_, cw, ch = face(b, tag['chest'], 'front')
    d.rectangle([cx_, cy_, cx_ + cw - 1, cy_ + 2], fill=trim + (255,))
    paste(big, (cx_ + cw // 2 - 8, cy_ + 8, 16, 16), LOGOS[eid])
    for k in range(5): d.point((cx_ + 4 + k * (cw - 8) // 4, cy_ + ch - 5), fill=light(col, 0.4) + (255,))
    # skirt hem: a pattern for each element
    sx, sy, sw, sh = face(b, tag['skirt'], 'front')
    for side in ('front', 'back', 'left', 'right'):
        fx, fy, fw, fh = face(b, tag['skirt'], side)
        d.rectangle([fx, fy + fh - 6, fx + fw - 1, fy + fh - 1], fill=trim + (255,))
        for i in range(0, fw, 6):
            px, py = fx + i + 2, fy + fh - 12
            c2 = light(col, 0.35) + (255,)
            if eid in ('flame', 'fire'): d.polygon([(px, py + 6), (px + 2, py), (px + 4, py + 6)], fill=c2)
            elif eid in ('water',): d.arc([px - 1, py, px + 5, py + 6], 180, 360, fill=c2)
            elif eid in ('ice', 'spirit', 'air'): d.point((px + 2, py + 3), fill=c2); d.point((px + 1, py + 3), fill=c2); d.point((px + 2, py + 2), fill=c2)
            elif eid in ('earth', 'wind'): d.ellipse([px, py + 1, px + 4, py + 5], fill=c2)
            elif eid in ('lightning', 'electric', 'thunder'): d.line([(px, py), (px + 3, py + 3), (px + 1, py + 3), (px + 4, py + 6)], fill=c2)
            else: d.rectangle([px + 1, py + 2, px + 3, py + 4], fill=c2)
        d.line([(fx + fw // 2, fy), (fx + fw // 2, fy + fh - 7)], fill=dark(robe, 0.7) + (255,), width=2)
    logo_on(big, b, tag['emblem'], LOGOS[eid])
    big.save(path)
    return name


L = json.load(open(A + '/lang/en_us.json'))
for eid, spec in SAGES.items():
    nm = build(eid, spec)
    L[f'entity.kiyamet.{eid}_sage'] = nm
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('sages done')

# ---- the Ancient Rod, now a weapon: a carved staff with a glowing stone, in 3D
T = 32
AX, AY, BX, BY = 3.5, 28.5, 28.5, 3.5
LN = math.hypot(BX - AX, BY - AY); UX, UY = (BX - AX) / LN, (BY - AY) / LN
rod = Image.new('RGBA', (T, T))
for yy in range(T):
    for xx in range(T):
        px, py = xx + 0.5 - AX, yy + 0.5 - AY
        u, v = px * UX + py * UY, px * -UY + py * UX
        c = None
        if 0 <= u <= 26 and abs(v) <= 1.1:
            c = (120, 84, 50) if int(u * 1.3 + v * 2) % 3 else (86, 58, 32)
            if int(u) in (6, 7, 14, 15, 21): c = (214, 180, 90)
        if 25 <= u <= 27.5 and abs(v) <= 2.6: c = (214, 180, 90)
        d = math.hypot(u - 29.3, v)
        if d <= 2.4: c = (250, 250, 230) if d < 0.9 else (150, 230, 255) if d < 1.7 else (70, 150, 220)
        if c: rod.putpixel((xx, yy), c + (255,))
out = rod.copy()
for yy in range(T):
    for xx in range(T):
        if rod.getpixel((xx, yy))[3]: continue
        nb = [rod.getpixel((xx + dx, yy + dy)) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= xx + dx < T and 0 <= yy + dy < T and rod.getpixel((xx + dx, yy + dy))[3]]
        if nb: out.putpixel((xx, yy), tuple(int(k * 0.42) for k in nb[0][:3]) + (255,))
out.save(f'{A}/textures/item/ancient_rod.png')
import make_3d_items as V
V.write(f'{A}/models/item/ancient_rod.json', 'kiyamet:item/ancient_rod', 7.25, 8.75, False, V.DISPLAY)
L = json.load(open(A + '/lang/en_us.json'))
L['tooltip.kiyamet.ancient_rod'] = 'Entrusted to you by a sage - fight with it!|Walkers felled with it count for that sage|With an element core, forge an element weapon on an anvil'
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('rod done')
