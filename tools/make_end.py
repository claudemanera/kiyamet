"""TWD 2.2: Creation and the End.
- GeckoLib models (4x painted sheets) for the zombie enderman and the five End bosses
- the Creation Axe (double-bladed battle axe, animated white shimmer, glowing 'powers on' variant), the Creation Rod,
  the Creation Element, the Star Stone and the Dragon Stone
- the Temple of Wisdom blocks (wisdom stone / glass / light), the Table of Creation and the Altar of Creation
- spawn eggs, lang, tooltips"""
import json, math, os, random, sys
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(__file__))
from make_animals import Beast, dark, light, mix
from hires import face, upscale
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')
for d in ('geckolib/models/entity', 'geckolib/animations/entity', 'textures/entity', 'textures/item', 'textures/block', 'models/item', 'models/block', 'items', 'blockstates'):
    os.makedirs(f'{A}/{d}', exist_ok=True)
L = json.load(open(A + '/lang/en_us.json'))
WHITE = (250, 250, 255)


def jd(obj, path): json.dump(obj, open(f'{A}/{path}', 'w'), indent=1)


def hires(b, mats, anims, seed, extra=None):
    b.save(mats, anims, scale_bounds=6)
    big = upscale(f'{A}/textures/entity/{b.name}.png', seed=seed)
    if extra: extra(big)
    big.save(f'{A}/textures/entity/{b.name}.png')


def glow_face(big, b, idx, which, col, rows=None):
    """paint a face fully in a glowing colour with a bright centre (eyes, crystals)"""
    x, y, w, h = face(b, idx, which)
    dr = ImageDraw.Draw(big)
    dr.rectangle((x, y, x + w - 1, y + h - 1), fill=col + (255,))
    dr.rectangle((x + w // 4, y + h // 4, x + w - 1 - w // 4, y + h - 1 - h // 4), fill=light(col, 0.6) + (255,))


# ======================================================================= zombie enderman (and its two big cousins)
def ender(name, variant=None):
    b = Beast(name, 128, 128)
    T = {}
    def c(bone, o, sz, mat, key=None, **k):
        b.cube(bone, o, sz, mat, **k)
        if key: T[key] = len(b.uvs) - 1
    b.bone('root')
    b.bone('body', 'root', pivot=(0, 28, 0)); b.bone('head', 'body', pivot=(0, 40, 0)); b.bone('jaw', 'head', pivot=(0, 41, -1))
    b.bone('arm_r', 'body', pivot=(-5, 39, 0)); b.bone('arm_l', 'body', pivot=(5, 39, 0))
    b.bone('leg_r', 'root', pivot=(-2, 28, 0)); b.bone('leg_l', 'root', pivot=(2, 28, 0))
    # long thin legs, rotten in places, a bare knee bone
    c('leg_r', (-3, 0, -1), (2, 28, 2), 'skin'); c('leg_r', (-3.2, 9, -1.2), (2.4, 5, 2.4), 'rot', inflate=0.05)
    c('leg_l', (1, 0, -1), (2, 28, 2), 'skin'); c('leg_l', (1.1, 13, -1.25), (1.8, 2, 0.4), 'bone')
    c('leg_l', (0.9, 20, -1.1), (2.2, 3, 2.2), 'cloth')
    # body: torn open, ribs showing, a rag round the waist
    c('body', (-4, 28, -2), (8, 12, 4), 'skin')
    c('body', (-2.5, 30, -2.15), (5, 7, 0.3), 'flesh')
    for i in range(4): c('body', (-3, 30.6 + i * 1.7, -2.4), (6, 0.7, 0.4), 'bone')
    c('body', (-4.3, 28, -2.3), (8.6, 3.2, 4.6), 'cloth')
    c('body', (1.8, 37, -2.35), (2.4, 2.4, 0.5), 'rot')
    c('body', (-4.2, 33, 1.9), (3, 4, 0.4), 'rot')
    # head with glowing eyes, a bare patch of skull, a hanging jaw
    c('head', (-4, 41, -4), (8, 8, 8), 'skin', 'head')
    c('head', (-3.2, 44.2, -4.25), (2.4, 1, 0.3), 'eye', 'eye_r'); c('head', (0.8, 44.2, -4.25), (2.4, 1, 0.3), 'eye', 'eye_l')
    c('head', (1, 46, -4.2), (3, 2.4, 0.3), 'bone'); c('head', (-4.2, 45, -2), (0.3, 3, 3), 'rot')
    c('jaw', (-3, 39.4, -4.4), (6, 1.8, 4.2), 'skin'); c('jaw', (-2.6, 41, -4.5), (5.2, 0.6, 0.4), 'bone')
    # arms: the right one long, the left one ends in bone
    c('arm_r', (-6, 11, -1), (2, 28, 2), 'skin'); c('arm_r', (-6.2, 26, -1.2), (2.4, 4, 2.4), 'rot', inflate=0.05)
    c('arm_l', (4, 15, -1), (2, 24, 2), 'skin'); c('arm_l', (4.4, 12, -0.6), (1.2, 3, 1.2), 'bone')
    mats = dict(skin=((24, 18, 32), 'skin'), rot=((72, 98, 54), 'skin'), flesh=((128, 34, 52), 'skin'), bone=((210, 204, 180), 'teeth'),
                eye=((236, 80, 255), 'glow'), cloth=((54, 36, 64), 'cloth'))
    eyec = (236, 80, 255)
    if variant == 'tyrant':                                   # the Void Tyrant: crown, cape, shoulder spikes, a crystal in the chest
        b.bone('cape', 'body', pivot=(0, 40, 2)); b.bone('crown', 'head', pivot=(0, 49, 0))
        for i, x in enumerate((-3.5, -1.5, 0.5, 2.5)): c('crown', (x, 49, -3.6), (1, 3 + (2 if i in (1, 2) else 0), 1), 'obs')
        c('crown', (-4.2, 48.6, -4.2), (8.4, 1, 8.4), 'obs')
        for s in (-1, 1): c('body', (s * 4.4 - (1.5 if s < 0 else 0), 39, -1.5), (1.5, 3, 3), 'obs'); c('body', (s * 4.6 - (0.6 if s < 0 else 0), 41.5, -0.5), (0.8, 3, 0.8), 'obs')
        c('body', (-1, 33.5, -2.6), (2, 3, 0.5), 'crystal', 'crystal')
        c('cape', (-4.5, 6, 2.1), (9, 34, 0.6), 'cape')
        mats.update(obs=((36, 16, 52), 'metal'), crystal=((190, 80, 255), 'glow'), cape=((60, 20, 80), 'cloth'), skin=((20, 12, 30), 'skin'))
        eyec = (255, 60, 220)
    if variant == 'matriarch':                                 # the Hive Matriarch: four arms, a swollen egg sac, a crown of chorus
        b.bone('arm_r2', 'body', pivot=(-4.5, 35, 0)); b.bone('arm_l2', 'body', pivot=(4.5, 35, 0)); b.bone('sac', 'body', pivot=(0, 30, 2))
        c('arm_r2', (-5.5, 13, -1), (1.6, 22, 1.6), 'skin'); c('arm_l2', (3.9, 13, -1), (1.6, 22, 1.6), 'skin')
        c('sac', (-6, 18, 2), (12, 13, 11), 'sac'); c('sac', (-5, 19, 12.5), (10, 10, 1.5), 'sac')
        for i in range(9): c('sac', (-5 + (i % 3) * 3.6, 20 + (i // 3) * 3.5, 12.9), (1.6, 1.6, 0.5), 'crystal')
        for i in range(5): c('head', (-3.5 + i * 1.6, 49, -1), (1, 2 + (i % 2) * 2, 1), 'chorus')
        mats.update(sac=((96, 40, 110), 'skin'), crystal=((250, 140, 255), 'glow'), chorus=((150, 110, 160), 'horn'))
    anims = {
        "misc.idle": {"loop": True, "animation_length": 3.0, "bones": {
            "arm_r": {"rotation": {"0.00": [-18, 0, 4], "1.50": [-26, 0, 6], "3.00": [-18, 0, 4]}},
            "arm_l": {"rotation": {"0.00": [-12, 0, -4], "1.50": [-20, 0, -6], "3.00": [-12, 0, -4]}},
            "head": {"rotation": {"0.00": [0, 0, 0], "0.80": [6, 12, 4], "1.00": [-4, 10, 0], "2.20": [2, -14, -6], "3.00": [0, 0, 0]}},
            "jaw": {"rotation": {"0.00": [10, 0, 0], "1.50": [24, 0, 0], "3.00": [10, 0, 0]}},
            "body": {"rotation": {"0.00": [4, 0, 0], "1.50": [8, 0, 2], "3.00": [4, 0, 0]}}}},
        "move.walk": {"loop": True, "animation_length": 1.2, "bones": {
            "leg_r": {"rotation": {"0.00": [26, 0, 0], "0.60": [-26, 0, 0], "1.20": [26, 0, 0]}},
            "leg_l": {"rotation": {"0.00": [-26, 0, 0], "0.60": [26, 0, 0], "1.20": [-26, 0, 0]}},
            "arm_r": {"rotation": {"0.00": [-72, 0, 4], "0.60": [-62, 0, 8], "1.20": [-72, 0, 4]}},
            "arm_l": {"rotation": {"0.00": [-60, 0, -4], "0.60": [-74, 0, -8], "1.20": [-60, 0, -4]}},
            "body": {"rotation": {"0.00": [8, -6, 3], "0.60": [8, 6, -3], "1.20": [8, -6, 3]}, "position": {"0.00": [0, 0, 0], "0.30": [0, 0.6, 0], "0.60": [0, 0, 0], "0.90": [0, 0.6, 0], "1.20": [0, 0, 0]}},
            "jaw": {"rotation": {"0.00": [20, 0, 0], "0.60": [30, 0, 0], "1.20": [20, 0, 0]}}}},
        "attack.swing": {"loop": False, "animation_length": 0.6, "bones": {
            "arm_r": {"rotation": {"0.00": [-60, 0, 0], "0.20": [-150, 0, 10], "0.40": [-20, 0, 0], "0.60": [-60, 0, 0]}},
            "arm_l": {"rotation": {"0.00": [-60, 0, 0], "0.25": [-150, 0, -10], "0.45": [-20, 0, 0], "0.60": [-60, 0, 0]}},
            "jaw": {"rotation": {"0.00": [10, 0, 0], "0.30": [45, 0, 0], "0.60": [10, 0, 0]}},
            "body": {"rotation": {"0.00": [6, 0, 0], "0.30": [22, 0, 0], "0.60": [6, 0, 0]}}}}}
    if variant == 'tyrant':
        for k, cl in anims.items(): cl["bones"]["cape"] = {"rotation": {"0.00": [6, 0, 0], f'{cl["animation_length"] / 2:.2f}': [14, 0, 0], f'{cl["animation_length"]:.2f}': [6, 0, 0]}}
    if variant == 'matriarch':
        for k, cl in anims.items():
            n = cl["animation_length"]
            cl["bones"]["sac"] = {"scale": {"0.00": [1, 1, 1], f'{n / 2:.2f}': [1.06, 1.08, 1.06], f'{n:.2f}': [1, 1, 1]}}
            cl["bones"]["arm_r2"] = {"rotation": {"0.00": [-40, 0, 20], f'{n / 2:.2f}': [-70, 0, 30], f'{n:.2f}': [-40, 0, 20]}}
            cl["bones"]["arm_l2"] = {"rotation": {"0.00": [-70, 0, -30], f'{n / 2:.2f}': [-40, 0, -20], f'{n:.2f}': [-70, 0, -30]}}

    def extra(big):
        for k in ('eye_r', 'eye_l'): glow_face(big, b, T[k], 'front', eyec)
        if 'crystal' in T: glow_face(big, b, T['crystal'], 'front', (200, 90, 255))
        # cracks of violet light across the dark skin
        x, y, w, h = face(b, T['head'], 'front')
        dr = ImageDraw.Draw(big); r = random.Random(name)
        for _ in range(6):
            px, py = x + r.randint(2, w - 3), y + r.randint(h // 2, h - 3)
            for _ in range(5):
                nx, ny = px + r.choice((-1, 0, 1)), py + r.choice((-1, 1))
                if x <= nx < x + w and y <= ny < y + h: dr.point((nx, ny), fill=(150, 60, 200, 255)); px, py = nx, ny
    hires(b, mats, anims, seed=hash(name) & 0xffff, extra=extra)
    return b


ender('zombie_enderman')
ender('void_tyrant', 'tyrant')
ender('hive_matriarch', 'matriarch')


# ======================================================================= humanoid bosses
def human(name, th=128):
    b = Beast(name, 128, th)
    b.bone('root')
    b.bone('body', 'root', pivot=(0, 24, 0)); b.bone('head', 'body', pivot=(0, 24, 0))
    b.bone('arm_r', 'body', pivot=(-5, 22, 0)); b.bone('arm_l', 'body', pivot=(5, 22, 0))
    b.bone('leg_r', 'root', pivot=(-2, 12, 0)); b.bone('leg_l', 'root', pivot=(2, 12, 0))
    b.bone('weapon', 'arm_r', pivot=(-6, 11, -1)); b.bone('cape', 'body', pivot=(0, 24, 2))
    return b


def base_anims(extra_bones=None, slow=1.0):
    a = {
        "misc.idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"position": {"0.00": [0, 0, 0], "1.50": [0, -0.3, 0], "3.00": [0, 0, 0]}},
            "arm_r": {"rotation": {"0.00": [-14, 0, 0], "1.50": [-18, 0, 3], "3.00": [-14, 0, 0]}},
            "arm_l": {"rotation": {"0.00": [0, 0, -3], "1.50": [4, 0, -6], "3.00": [0, 0, -3]}},
            "head": {"rotation": {"0.00": [0, 0, 0], "1.00": [3, 10, 0], "2.00": [0, -10, 0], "3.00": [0, 0, 0]}},
            "cape": {"rotation": {"0.00": [5, 0, 0], "1.50": [10, 0, 0], "3.00": [5, 0, 0]}}}},
        "move.walk": {"loop": True, "animation_length": 1.0 * slow, "bones": {
            "leg_r": {"rotation": {"0.00": [30, 0, 0], f"{0.5 * slow:.2f}": [-30, 0, 0], f"{slow:.2f}": [30, 0, 0]}},
            "leg_l": {"rotation": {"0.00": [-30, 0, 0], f"{0.5 * slow:.2f}": [30, 0, 0], f"{slow:.2f}": [-30, 0, 0]}},
            "arm_r": {"rotation": {"0.00": [-25, 0, 0], f"{0.5 * slow:.2f}": [5, 0, 0], f"{slow:.2f}": [-25, 0, 0]}},
            "arm_l": {"rotation": {"0.00": [25, 0, 0], f"{0.5 * slow:.2f}": [-25, 0, 0], f"{slow:.2f}": [25, 0, 0]}},
            "cape": {"rotation": {"0.00": [20, 0, 0], f"{0.5 * slow:.2f}": [28, 0, 0], f"{slow:.2f}": [20, 0, 0]}}}},
        "attack.swing": {"loop": False, "animation_length": 0.6, "bones": {
            "arm_r": {"rotation": {"0.00": [-14, 0, 0], "0.20": [-155, 12, 0], "0.40": [25, -10, 0], "0.60": [-14, 0, 0]}},
            "body": {"rotation": {"0.00": [0, 0, 0], "0.20": [-6, 18, 0], "0.40": [8, -22, 0], "0.60": [0, 0, 0]}},
            "weapon": {"rotation": {"0.00": [0, 0, 0], "0.20": [-20, 0, 0], "0.40": [30, 0, 0], "0.60": [0, 0, 0]}}}}}
    for k, bones in (extra_bones or {}).items():
        for cl in a.values():
            n = cl["animation_length"]
            cl["bones"][k] = {kk: {kf.replace('N', f'{n:.2f}').replace('H', f'{n / 2:.2f}'): v for kf, v in vv.items()} for kk, vv in bones.items()}
    return a


# --- the Sky Herald: white robes, gold, great feathered wings, a halo, a long spear of light
b = human('sky_herald', 256); T = {}
def C(bone, o, sz, mat, key=None, **k):
    b.cube(bone, o, sz, mat, **k)
    if key: T[key] = len(b.uvs) - 1
b.bone('wing_r', 'body', pivot=(-2, 21, 2)); b.bone('wing_l', 'body', pivot=(2, 21, 2)); b.bone('halo', 'head', pivot=(0, 35, 0))
for n, x in (('leg_r', -4), ('leg_l', 0)): C(n, (x, 0, -2), (4, 12, 4), 'robe'); C(n, (x - 0.2, 0, -2.3), (4.4, 2, 4.6), 'gold')
C('body', (-4, 12, -2), (8, 12, 4), 'robe'); C('body', (-4.6, 0.5, -2.6), (9.2, 12, 5.2), 'robe')
C('body', (-4.7, 11, -2.7), (9.4, 1.4, 5.4), 'gold'); C('body', (-1, 12, -2.7), (2, 12, 0.4), 'gold'); C('body', (-4.3, 19, -2.6), (8.6, 5, 5.2), 'gold')
C('body', (-1, 20, -2.95), (2, 2, 0.3), 'light', 'gem')
C('head', (-4, 24, -4), (8, 8, 8), 'skin', 'head'); C('head', (-4.5, 28, -4.5), (9, 4.6, 9), 'hair')
C('head', (-3.2, 27.6, -4.25), (2.4, 0.9, 0.3), 'light', 'eye_r'); C('head', (0.8, 27.6, -4.25), (2.4, 0.9, 0.3), 'light', 'eye_l')
C('halo', (-4.5, 35, -4.5), (9, 0.6, 9), 'light'); C('halo', (-3.5, 35.05, -3.5), (7, 0.6, 7), 'air0')
for n, x in (('arm_r', -8), ('arm_l', 4)): C(n, (x, 12, -2), (4, 12, 4), 'robe'); C(n, (x - 0.4, 11, -2.4), (4.8, 5, 4.8), 'robe'); C(n, (x - 0.3, 20, -2.3), (4.6, 4, 4.6), 'gold')
for s, n in ((-1, 'wing_r'), (1, 'wing_l')):
    x0 = -20 if s < 0 else 2
    C(n, (x0, 6, 2.2), (18, 16, 0.6), 'feather')
    for i in range(6): C(n, (x0 + (i * 3 if s > 0 else 15 - i * 3), 1 + i * 0.8, 2.4), (2.4, 6, 0.4), 'feather2')
    C(n, (x0 + (14 if s < 0 else 0), 18, 2.0), (4, 4, 1.0), 'gold')
C('weapon', (-6.5, 10.5, -30), (1, 1, 32), 'gold'); C('weapon', (-7, 9.5, -36), (2, 3, 6), 'light', 'tip')
mats = dict(robe=((236, 236, 242), 'cloth'), gold=((232, 190, 70), 'metal'), skin=((236, 220, 206), 'skin'), hair=((250, 240, 200), 'fur'),
            light=((255, 246, 190), 'glow'), air0=((0, 0, 0), 'skin'), feather=((248, 248, 255), 'fur'), feather2=((226, 230, 246), 'fur'))
an = base_anims({"wing_r": {"rotation": {"0.00": [0, 20, 0], "H": [0, -10, 0], "N": [0, 20, 0]}},
                 "wing_l": {"rotation": {"0.00": [0, -20, 0], "H": [0, 10, 0], "N": [0, -20, 0]}},
                 "halo": {"rotation": {"0.00": [0, 0, 0], "N": [0, 180, 0]}, "position": {"0.00": [0, 0, 0], "H": [0, 0.5, 0], "N": [0, 0, 0]}}})
an["misc.idle"]["bones"]["root"] = {"position": {"0.00": [0, 2, 0], "1.50": [0, 3, 0], "3.00": [0, 2, 0]}}           # it hovers
def ex(big):
    for k in ('eye_r', 'eye_l', 'gem', 'tip'): glow_face(big, b, T[k], 'front', (255, 236, 150))
hires(b, mats, an, seed=11, extra=ex)
# the halo's inner cube is only there to be invisible: clear it
img = Image.open(f'{A}/textures/entity/sky_herald.png')
for f in ('top', 'bottom', 'left', 'right', 'front', 'back'):
    x, y, w, h = face(b, [i for i, u in enumerate(b.uvs) if u[2] == 'air0'][0], f)
    img.paste((0, 0, 0, 0), (x, y, x + w, y + h))
img.save(f'{A}/textures/entity/sky_herald.png')

# --- the Dead Captain: rotten, a long purple coat with gold, a tricorn, a peg leg, a cutlass
b = human('dead_captain', 256); T = {}
C('leg_r', (-4, 0, -2), (4, 12, 4), 'boots'); C('leg_r', (-4.2, 6, -2.2), (4.4, 6, 4.4), 'trousers')
C('leg_l', (0.8, 0, -0.8), (1.6, 7, 1.6), 'wood'); C('leg_l', (0, 7, -2), (4, 5, 4), 'trousers')
C('body', (-4, 12, -2), (8, 12, 4), 'shirt'); C('body', (-4.5, 4, -2.5), (9, 20, 5), 'coat'); C('body', (-1.6, 12, -2.6), (3.2, 12, 0.2), 'shirt')
C('body', (-4.6, 12, -2.7), (9.2, 1.4, 5.4), 'belt'); C('body', (-4.6, 4, -2.6), (0.8, 20, 0.3), 'gold'); C('body', (3.8, 4, -2.6), (0.8, 20, 0.3), 'gold')
for i in range(4): C('body', (-0.4, 15 + i * 2.2, -2.8), (0.8, 0.8, 0.3), 'gold')
C('head', (-4, 24, -4), (8, 8, 8), 'skin', 'head'); C('head', (-3.2, 21, -4.3), (6.4, 4, 1.2), 'beard')
C('head', (-3.2, 27.6, -4.25), (2.4, 1, 0.3), 'eye', 'eye_r'); C('head', (0.8, 27.6, -4.25), (2.4, 1, 0.3), 'eye', 'eye_l')
C('head', (1, 29, -4.2), (2.6, 2, 0.3), 'bone')
C('head', (-7, 31.5, -7), (14, 1, 14), 'hat'); C('head', (-4.5, 32.5, -4.5), (9, 3.5, 9), 'hat'); C('head', (-7.1, 31.4, -7.1), (14.2, 0.3, 14.2), 'gold')
C('head', (-1.5, 33, -4.7), (3, 2, 0.3), 'bone')
for n, x in (('arm_r', -8), ('arm_l', 4)): C(n, (x, 12, -2), (4, 12, 4), 'coat'); C(n, (x - 0.3, 12, -2.3), (4.6, 2.5, 4.6), 'gold'); C(n, (x + 0.5, 9.5, -1.5), (3, 2.5, 3), 'skin')
C('cape', (-4, 4, 2.6), (8, 19, 0.4), 'coat')
C('weapon', (-6.5, 9.5, -2.5), (1, 3, 3), 'gold'); C('weapon', (-6.3, 10, -18), (0.6, 2.6, 15.5), 'steel'); C('weapon', (-6.2, 12.2, -19), (0.4, 1, 4), 'steel')
mats = dict(boots=((30, 24, 22), 'metal'), trousers=((54, 44, 40), 'cloth'), wood=((110, 76, 44), 'fur'), shirt=((180, 170, 150), 'cloth'),
            coat=((84, 30, 78), 'cloth'), belt=((40, 26, 20), 'metal'), gold=((222, 180, 60), 'metal'), skin=((120, 142, 108), 'skin'),
            beard=((40, 60, 44), 'fur'), eye=((140, 255, 120), 'glow'), bone=((214, 208, 186), 'teeth'), hat=((26, 22, 28), 'cloth'), steel=((196, 206, 214), 'metal'))
an = base_anims(slow=1.3)
an["move.walk"]["bones"]["body"] = {"rotation": {"0.00": [0, 0, -6], "0.65": [0, 0, 6], "1.30": [0, 0, -6]}}                 # the peg leg limp
def ex(big):
    for k in ('eye_r', 'eye_l'): glow_face(big, b, T[k], 'front', (140, 255, 120))
hires(b, mats, an, seed=12, extra=ex)

# --- the Star Warden: night-blue armour full of stars, a star crest, a greatsword of starlight, a starry cape
b = human('star_warden', 256); T = {}
r = random.Random(5)
for n, x in (('leg_r', -4), ('leg_l', 0)): C(n, (x, 0, -2), (4, 12, 4), 'armor2'); C(n, (x - 0.2, 0, -2.4), (4.4, 2.5, 4.8), 'armor'); C(n, (x - 0.1, 5, -2.3), (4.2, 3, 0.5), 'silver')
C('body', (-4, 12, -2), (8, 12, 4), 'armor'); C('body', (-4.5, 16.5, -2.7), (9, 7.5, 5.4), 'armor'); C('body', (-4.7, 12, -2.6), (9.4, 1.6, 5.2), 'silver')
C('body', (-1.5, 18.5, -3.0), (3, 3, 0.4), 'star', 'core')
C('head', (-4, 24, -4), (8, 8, 8), 'armor2'); C('head', (-4.6, 24, -4.6), (9.2, 8.6, 9.2), 'armor', 'helm')
C('head', (-3.4, 27.2, -4.9), (6.8, 1.2, 0.4), 'star', 'visor'); C('head', (-0.6, 24.3, -4.9), (1.2, 3, 0.4), 'silver')
for a in range(5):                                                                     # the five-pointed crest
    C('head', (-0.6, 32.6, -0.6), (1.2, 5, 1.2), 'star', rot=(0, 0, a * 72 - 144), pivot=(0, 32.6, 0))
for n, x in (('arm_r', -8), ('arm_l', 4)):
    C(n, (x, 12, -2), (4, 12, 4), 'armor2'); C(n, (x - 0.5, 19.5, -2.5), (5, 5, 5), 'armor'); C(n, (x - 0.3, 12, -2.3), (4.6, 4, 4.6), 'silver')
C('cape', (-4.5, 2, 2.2), (9, 22, 0.6), 'cape')
for i in range(26):                                                                    # stars all over
    bone, o = r.choice([('body', (r.uniform(-4.2, 3.4), r.uniform(13, 23), -2.95)), ('cape', (r.uniform(-4.2, 3.8), r.uniform(3, 23), 2.85)),
                        ('arm_r', (-8.25, r.uniform(13, 23), r.uniform(-1.6, 1.2))), ('arm_l', (8.05, r.uniform(13, 23), r.uniform(-1.6, 1.2))),
                        ('leg_r', (r.uniform(-3.8, -0.8), r.uniform(1, 11), -2.45)), ('leg_l', (r.uniform(0.2, 3.2), r.uniform(1, 11), -2.45))])
    C(bone, o, (0.5, 0.5, 0.3), 'star')
C('weapon', (-6.5, 9.5, -2), (1, 3, 3), 'silver'); C('weapon', (-8, 10, -6), (4, 2, 1.5), 'silver')
C('weapon', (-6.6, 10.2, -32), (1.2, 4, 26), 'blade', 'blade'); C('weapon', (-6.4, 11.3, -32.2), (0.8, 1.8, 26), 'star')
mats = dict(armor=((32, 40, 92), 'metal'), armor2=((22, 26, 60), 'metal'), silver=((200, 210, 230), 'metal'), star=((255, 255, 240), 'glow'),
            cape=((18, 22, 56), 'cloth'), blade=((150, 200, 255), 'glow'))
an = base_anims()
an["attack.swing"]["bones"]["arm_l"] = {"rotation": {"0.00": [0, 0, 0], "0.20": [-120, -20, 0], "0.40": [20, 10, 0], "0.60": [0, 0, 0]}}
def ex(big):
    for k in ('core', 'visor', 'blade'): glow_face(big, b, T[k], 'front', (230, 240, 255))
hires(b, mats, an, seed=13, extra=ex)


# ======================================================================= item art
T32 = 32
AX, AY, BX, BY = 3.5, 28.5, 28.5, 3.5
LEN = math.hypot(BX - AX, BY - AY); UX, UY = (BX - AX) / LEN, (BY - AY) / LEN; VX, VY = -UY, UX
ELC = [(255, 90, 30), (255, 226, 74), (123, 162, 63), (107, 47, 181), (154, 154, 154), (127, 240, 232), (53, 200, 255), (77, 107, 255), (168, 232, 255),
       (255, 154, 26), (46, 123, 255), (232, 246, 255), (142, 240, 160), (184, 194, 204)]


def draw32(f, size=32):
    im = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            px, py = x + 0.5 - AX, y + 0.5 - AY
            u, v = px * UX + py * UY, px * VX + py * VY
            c = f(u, v, x, y)
            if c: im.putpixel((x, y), tuple(max(0, min(255, int(k))) for k in c[:3]) + (255,))
    return im


def outline(im, k=0.38):
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            if im.getpixel((x, y))[3]: continue
            nb = [im.getpixel((x + dx, y + dy)) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < im.width and 0 <= y + dy < im.height and im.getpixel((x + dx, y + dy))[3]]
            if nb: out.putpixel((x, y), tuple(int(c * k) for c in nb[0][:3]) + (255,))
    return out


def creation_axe(phase=0.0, on=False):
    """a double-bladed battle axe: pearl-white crescents edged in gold, a long white haft, a heart of light"""
    def f(u, v, x, y):
        sh = 0.5 + 0.5 * math.sin((u + v) * 0.55 - phase * math.pi * 2)            # the moving shimmer
        uc = 22.0
        if u < 1.6 and abs(v) <= 1.5: return (240, 200, 90)
        if 0 <= u <= 29.5 and abs(v) <= 0.95:
            if int(u * 1.3) % 4 == 0: return (236, 196, 80)
            return mix((214, 214, 228), (255, 255, 255), sh * 0.7)
        if 29.5 <= u <= 31.5 and abs(v) <= 1.6 - (u - 29.5) * 0.7: return (255, 255, 255)
        av = abs(v)
        if 1.0 <= av <= 10.2:
            hh = 2.2 + (av - 1.0) * 0.62                                              # widening towards the edge
            curve = 0.09 * (av - 1) ** 2                                                # a crescent edge
            du = u - uc + curve * 0.0
            if abs(du) <= hh and not (av > 7.5 and abs(du) < (av - 7.5) * 0.9 - 1.6):
                edge = av > 8.9 or abs(du) > hh - 0.9
                if edge: return mix((255, 220, 110), (255, 250, 200), sh) if not on else mix((255, 255, 255), ELC[int((u + av) * 0.7) % 14], 0.35)
                vein = (int(u * 2 + av * 3) % 7 == 0)
                base = mix((226, 228, 240), (255, 255, 255), sh)
                if on: base = mix(base, ELC[int(av * 1.4 + u * 0.3) % 14], 0.18)
                return mix(base, (210, 220, 255), 0.5) if vein else base
        if math.hypot(u - uc, v) <= 1.9: return (255, 255, 255) if on or sh > 0.5 else (230, 246, 255)
    return f


def axe_frames(on):
    frames = [outline(draw32(creation_axe(i / 8, on)), 0.45) for i in range(8)]
    strip = Image.new('RGBA', (32, 32 * 8))
    for i, fr in enumerate(frames): strip.paste(fr, (0, 32 * i))
    return frames[0], strip


def creation_rod():
    def f(u, v, x, y):
        if u < 1.5 and abs(v) <= 1.6: return (240, 200, 90)
        if 0 <= u <= 27 and abs(v) <= 1.0:
            band = int(u / 1.9)
            if band % 2 == 1 and band // 2 < 14: return ELC[band // 2]
            return (246, 246, 252) if abs(v) < 0.5 else (214, 214, 228)
        if math.hypot(u - 28.5, v) <= 2.6: return (255, 255, 255) if math.hypot(u - 28.5, v) < 1.4 else (220, 230, 255)
    return outline(draw32(f), 0.45)


def orb(col_in, col_out, size=16, frames=1, star=False):
    out = []
    for fi in range(frames):
        im = Image.new('RGBA', (size, size))
        ph = fi / max(1, frames)
        for y in range(size):
            for x in range(size):
                d = math.hypot(x - 7.5, y - 7.5)
                if d > 6.8: continue
                a = math.atan2(y - 7.5, x - 7.5)
                sw = 0.5 + 0.5 * math.sin(a * 3 + d * 0.9 - ph * math.pi * 2)
                c = mix(col_out, col_in, (1 - d / 6.8) * 0.75 + sw * 0.25)
                if d > 6.0: c = dark(col_out, 0.55)
                if (x, y) in ((5, 4), (5, 5), (4, 5)): c = (255, 255, 255)
                im.putpixel((x, y), tuple(int(k) for k in c[:3]) + (255,))
        out.append(im)
    return out


def gem(top, mid, bot, kind):
    """a cut gemstone, 16x16: star stone has a star of light inside, dragon stone a slit pupil"""
    im = Image.new('RGBA', (16, 16))
    pts = [(8, 1), (13, 4), (14, 9), (8, 15), (2, 9), (3, 4)]
    dr = ImageDraw.Draw(im)
    dr.polygon(pts, fill=mid + (255,))
    dr.polygon([(8, 1), (13, 4), (8, 6), (3, 4)], fill=top + (255,))
    dr.polygon([(2, 9), (8, 15), (14, 9), (8, 11)], fill=bot + (255,))
    dr.line([(3, 4), (8, 6), (13, 4)], fill=light(top, 0.4) + (255,))
    if kind == 'star':
        for (x, y) in ((8, 6), (8, 7), (8, 8), (8, 9), (8, 10), (6, 8), (7, 8), (9, 8), (10, 8), (7, 7), (9, 9), (9, 7), (7, 9)): im.putpixel((x, y), (255, 255, 240, 255))
        im.putpixel((8, 8), (255, 255, 255, 255))
    else:
        for y in range(5, 12): im.putpixel((8, y), (20, 6, 10, 255))
        for y in (7, 8, 9): im.putpixel((7, y), (40, 10, 16, 255)); im.putpixel((9, y), (40, 10, 16, 255))
    im.putpixel((5, 3), (255, 255, 255, 255)); im.putpixel((6, 3), (255, 255, 255, 220))
    return outline(im, 0.3)


import make_3d_items as V
# the axe: a thick voxel model from the first frame, but the texture it uses is the animated strip
for on in (False, True):
    nm = 'creation_axe_on' if on else 'creation_axe'
    f0, strip = axe_frames(on)
    f0.save(f'{A}/textures/item/{nm}_shape.png')
    strip.save(f'{A}/textures/item/{nm}.png')
    jd({"animation": {"frametime": 2, "interpolate": True}}, f'textures/item/{nm}.png.mcmeta')
    els = V.voxel(f'kiyamet:item/{nm}_shape', 6.6, 9.4, False)
    disp = json.loads(json.dumps(V.DISPLAY))
    for key in ('firstperson_righthand', 'firstperson_lefthand', 'thirdperson_righthand', 'thirdperson_lefthand'): disp[key]['scale'] = [s * 1.4 for s in disp[key]['scale']]
    json.dump({"gui_light": "front", "textures": {"0": f"kiyamet:item/{nm}", "particle": f"kiyamet:item/{nm}"},
               "elements": json.loads(json.dumps(els).replace(f'kiyamet:item/{nm}_shape', f'kiyamet:item/{nm}')), "display": disp},
              open(f'{A}/models/item/{nm}.json', 'w'), separators=(',', ':'))
    os.remove(f'{A}/textures/item/{nm}_shape.png')
jd({"model": {"type": "minecraft:condition", "property": "minecraft:custom_model_data", "index": 0,
              "on_true": {"type": "minecraft:model", "model": "kiyamet:item/creation_axe_on"}, "on_false": {"type": "minecraft:model", "model": "kiyamet:item/creation_axe"}}}, 'items/creation_axe.json')

creation_rod().save(f'{A}/textures/item/creation_rod.png')
disp = json.loads(json.dumps(V.DISPLAY))
V.write(f'{A}/models/item/creation_rod.json', 'kiyamet:item/creation_rod', 7.0, 9.0, False, disp)
jd({"model": {"type": "minecraft:model", "model": "kiyamet:item/creation_rod"}}, 'items/creation_rod.json')

frames = orb((255, 255, 255), (190, 200, 240), frames=8)
strip = Image.new('RGBA', (16, 128))
for i, fr in enumerate(frames): strip.paste(fr, (0, 16 * i))
strip.save(f'{A}/textures/item/creation_core.png')
jd({"animation": {"frametime": 2, "interpolate": True}}, 'textures/item/creation_core.png.mcmeta')
gem((190, 220, 255), (110, 140, 230), (50, 60, 150), 'star').save(f'{A}/textures/item/star_stone.png')
gem((255, 120, 120), (170, 30, 60), (80, 10, 40), 'dragon').save(f'{A}/textures/item/dragon_stone.png')
for k in ('creation_core', 'star_stone', 'dragon_stone'):
    jd({"parent": "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{k}"}}, f'models/item/{k}.json')
    jd({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{k}"}}, f'items/{k}.json')


# ======================================================================= blocks
def tex16(f, frames=1):
    out = []
    for fi in range(frames):
        im = Image.new('RGBA', (16, 16))
        for y in range(16):
            for x in range(16):
                c = f(x, y, fi / max(1, frames))
                if c: im.putpixel((x, y), tuple(int(k) for k in c[:3]) + ((c[3],) if len(c) > 3 else (255,)))
        out.append(im)
    if frames == 1: return out[0]
    s = Image.new('RGBA', (16, 16 * frames))
    for i, fr in enumerate(out): s.paste(fr, (0, 16 * i))
    return s


R = random.Random(22)
NOISE = [[R.random() for _ in range(16)] for _ in range(16)]
def wisdom_stone(x, y, ph):
    n = NOISE[y][x]
    c = mix((232, 232, 238), (252, 252, 255), n)
    if (x + y * 2) % 11 == 0 and n > 0.4: c = (236, 222, 170)                               # faint golden veins
    if x in (0, 15) or y in (0, 15): c = dark(c, 0.92)
    return c
def wisdom_glass(x, y, ph):
    if x in (0, 15) or y in (0, 15): return (246, 246, 252, 255)
    if (x, y) in ((3, 3), (4, 3), (3, 4), (11, 12)): return (255, 255, 255, 170)
    return (240, 244, 255, 70)
def wisdom_light(x, y, ph):
    d = math.hypot(x - 7.5, y - 7.5)
    k = 0.5 + 0.5 * math.sin(d * 0.9 - ph * math.pi * 2)
    c = mix((236, 238, 250), (255, 255, 255), k)
    if x in (0, 15) or y in (0, 15): c = (226, 222, 196)
    return c
tex16(wisdom_stone).save(f'{A}/textures/block/wisdom_stone.png')
tex16(wisdom_glass).save(f'{A}/textures/block/wisdom_glass.png')
tex16(wisdom_light, 8).save(f'{A}/textures/block/wisdom_light.png')
jd({"animation": {"frametime": 3, "interpolate": True}}, 'textures/block/wisdom_light.png.mcmeta')
for k in ('wisdom_stone', 'wisdom_glass', 'wisdom_light'):
    jd({"variants": {"": {"model": f"kiyamet:block/{k}"}}}, f'blockstates/{k}.json')
    jd({"parent": "minecraft:block/cube_all", "textures": {"all": f"kiyamet:block/{k}"}}, f'models/block/{k}.json')
    jd({"model": {"type": "minecraft:model", "model": f"kiyamet:block/{k}"}}, f'items/{k}.json')

# Table of Creation: a white pedestal, gold trim, the ring of fourteen element gems round a star of light on top
def table_top(x, y, ph):
    d = math.hypot(x - 7.5, y - 7.5)
    if x in (0, 15) or y in (0, 15): return (230, 196, 90)
    for i in range(14):
        a = i * math.pi * 2 / 14
        if math.hypot(x - (7.5 + math.cos(a) * 5.4), y - (7.5 + math.sin(a) * 5.4)) < 0.9: return ELC[i]
    if d < 2.2: return (255, 255, 255)
    if abs(x - 7.5) < 0.6 or abs(y - 7.5) < 0.6: return (250, 246, 220) if d < 4 else (240, 240, 246)
    return mix((240, 240, 246), (252, 252, 255), NOISE[y][x])
def table_side(x, y, ph):
    if y in (0, 1): return (230, 196, 90)
    if y == 15: return (210, 176, 80)
    if 4 <= y <= 11 and 3 <= x <= 12 and (x in (3, 12) or y in (4, 11)): return (230, 196, 90)
    if 5 <= y <= 10 and 4 <= x <= 11: return mix((236, 240, 255), (255, 255, 255), NOISE[y][x])
    return mix((228, 228, 236), (246, 246, 252), NOISE[y][x])
tex16(table_top).save(f'{A}/textures/block/creation_table_top.png')
tex16(table_side).save(f'{A}/textures/block/creation_table_side.png')
def box(f, t, tx, faces=None):
    e = {"from": f, "to": t, "faces": {}}
    for fc in ('north', 'south', 'east', 'west', 'up', 'down'):
        e["faces"][fc] = {"texture": f"#{(faces or {}).get(fc, tx)}"}
    return e
jd({"parent": "minecraft:block/block", "textures": {"top": "kiyamet:block/creation_table_top", "side": "kiyamet:block/creation_table_side", "stone": "kiyamet:block/wisdom_stone",
                                                     "light": "kiyamet:block/wisdom_light", "particle": "kiyamet:block/wisdom_stone"},
    "elements": [box([1, 0, 1], [15, 3, 15], 'stone'), box([4, 3, 4], [12, 10, 12], 'side', {'up': 'stone', 'down': 'stone'}),
                 box([0, 10, 0], [16, 14, 16], 'side', {'up': 'top', 'down': 'stone'}),
                 box([6.5, 14, 6.5], [9.5, 15, 9.5], 'light')]}, 'models/block/creation_table.json')
jd({"variants": {"": {"model": "kiyamet:block/creation_table"}}}, 'blockstates/creation_table.json')
jd({"model": {"type": "minecraft:model", "model": "kiyamet:block/creation_table"}}, 'items/creation_table.json')
# Altar of Creation: three white tiers, gold bands, and a crystal of light floating above
jd({"parent": "minecraft:block/block", "textures": {"side": "kiyamet:block/creation_table_side", "stone": "kiyamet:block/wisdom_stone", "top": "kiyamet:block/creation_table_top",
                                                     "light": "kiyamet:block/wisdom_light", "particle": "kiyamet:block/wisdom_stone"},
    "elements": [box([1, 0, 1], [15, 4, 15], 'stone'), box([3, 4, 3], [13, 8, 13], 'side', {'up': 'stone', 'down': 'stone'}),
                 box([5, 8, 5], [11, 10, 11], 'stone', {'up': 'top'}),
                 {"from": [6.5, 11, 6.5], "to": [9.5, 15.5, 9.5], "rotation": {"angle": 45, "axis": "y", "origin": [8, 13, 8]},
                  "faces": {fc: {"texture": "#light"} for fc in ('north', 'south', 'east', 'west', 'up', 'down')}}]}, 'models/block/creation_altar.json')
jd({"variants": {"": {"model": "kiyamet:block/creation_altar"}}}, 'blockstates/creation_altar.json')
jd({"model": {"type": "minecraft:model", "model": "kiyamet:block/creation_altar"}}, 'items/creation_altar.json')


# ======================================================================= spawn eggs
def egg(name, base, spot):
    im = Image.new('RGBA', (16, 16)); r = random.Random(name)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 5.6, (y - 8.6) / 7.2 + (0.12 if y < 8 else 0)
            if dx * dx + dy * dy > 1: continue
            c = base
            if r.random() < 0.18 or ((x * 5 + y * 3) % 9 == 0): c = spot
            if dx * dx + dy * dy > 0.78: c = dark(c, 0.62)
            if x < 7 and y < 7 and dx * dx + dy * dy < 0.5 and (x + y) % 5 == 0: c = light(c, 0.5)
            im.putpixel((x, y), tuple(c) + (255,))
    im.save(f'{A}/textures/item/{name}_spawn_egg.png')
    jd({"parent": "minecraft:item/generated", "textures": {"layer0": f"kiyamet:item/{name}_spawn_egg"}}, f'models/item/{name}_spawn_egg.json')
    jd({"model": {"type": "minecraft:model", "model": f"kiyamet:item/{name}_spawn_egg"}}, f'items/{name}_spawn_egg.json')
for n, a, s in (('zombie_enderman', (24, 18, 32), (90, 140, 70)), ('void_tyrant', (20, 10, 30), (230, 80, 255)), ('sky_herald', (240, 240, 246), (232, 190, 70)),
                ('dead_captain', (84, 30, 78), (140, 255, 120)), ('hive_matriarch', (96, 40, 110), (250, 140, 255)), ('star_warden', (32, 40, 92), (255, 255, 240))):
    egg(n, a, s)


# ======================================================================= lang
EL = ['Flame', 'Lightning', 'Earth', 'Shadow', 'Smoke', 'Spirit', 'Electric', 'Thunder', 'Ice', 'Fire', 'Water', 'Air', 'Wind', 'Metal']
POW = ['flamethrower', 'lightning', 'shatter', 'invisibility', 'smoke cloud', 'flight x4', 'exploding lightning', 'super speed', 'freeze time',
       'fireballs', 'super jump', 'sky flight', 'fast time', 'no damage']
L.update({
    'entity.kiyamet.zombie_enderman': 'Zombie Enderman', 'item.kiyamet.zombie_enderman_spawn_egg': 'Zombie Enderman Spawn Egg',
    'entity.kiyamet.void_tyrant': 'The Void Tyrant', 'entity.kiyamet.sky_herald': 'The Sky Herald', 'entity.kiyamet.dead_captain': 'The Dead Captain',
    'entity.kiyamet.hive_matriarch': 'The Hive Matriarch', 'entity.kiyamet.star_warden': 'The Star Warden',
    'item.kiyamet.void_tyrant_spawn_egg': 'Void Tyrant Spawn Egg', 'item.kiyamet.sky_herald_spawn_egg': 'Sky Herald Spawn Egg',
    'item.kiyamet.dead_captain_spawn_egg': 'Dead Captain Spawn Egg', 'item.kiyamet.hive_matriarch_spawn_egg': 'Hive Matriarch Spawn Egg',
    'item.kiyamet.star_warden_spawn_egg': 'Star Warden Spawn Egg',
    'item.kiyamet.star_stone': 'Star Stone', 'item.kiyamet.dragon_stone': 'Dragon Stone',
    'tooltip.kiyamet.star_stone': 'Passive: Luck + slow falling, your hits burn with starlight|Active: Starfall: stars crash on every enemy around|Dropped by the Star Warden (End)',
    'tooltip.kiyamet.dragon_stone': 'Passive: Resistance II + fire resistance, hits can wither|Active: Dragon\'s Breath: a dragon fireball and a blast|Dropped by the Void Tyrant (End)',
    'item.kiyamet.creation_core': 'Creation Element', 'item.kiyamet.creation_rod': 'Creation Rod', 'item.kiyamet.creation_axe': 'Creation Axe',
    'tooltip.kiyamet.creation_core': 'The greatest element, made of all twelve stones|Hold it and right-click an anvil|with the Creation Rod in your inventory',
    'tooltip.kiyamet.creation_rod': 'Made of all fourteen element weapons|at the Table of Creation',
    'tooltip.kiyamet.creation_axe': 'Double-bladed battle axe of CREATION|Holds all 14 element powers - each has its own key|(Options > Controls > The Walking Dead, numpad by default)|Right-click: selected power on/off · sneak + right-click: next power',
    'block.kiyamet.wisdom_stone': 'Wisdom Stone', 'block.kiyamet.wisdom_glass': 'Wisdom Glass', 'block.kiyamet.wisdom_light': 'Wisdom Light',
    'block.kiyamet.creation_table': 'Table of Creation', 'block.kiyamet.creation_altar': 'Altar of Creation',
    'tooltip.kiyamet.creation_table': 'Stands in the Temple of Wisdom, in the End|12 stones = Creation Element · 14 element weapons = Creation Rod',
    'tooltip.kiyamet.creation_altar': 'Given to the Sage of Creation|Place it, build your temple around it|Sneak + right-click: companions become its immortal guardians',
})
for i, e in enumerate(EL): L[f'key.kiyamet.creation_{e.lower()}'] = f'Creation Axe: {e} ({POW[i]})'
json.dump(L, open(A + '/lang/en_us.json', 'w'), indent=1, ensure_ascii=False)
print('end done')
