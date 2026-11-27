"""TWD 2.1 furniture, rebuilt: 64x64 material textures (planks with grain and nails, marble with veins, tufted leather,
enamel, brushed steel, brass, quilted fabric, woven canvas, burlap...) and far more detailed block models - rounded
edges, inset panels, handles on standoffs, individual books, pillows, folded blankets, faucets, grates, clawfeet."""
import json, math, os, random
from PIL import Image, ImageDraw

A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')
N = 64


def clamp(v): return max(0, min(255, int(v)))
def sh(c, k): return tuple(clamp(x * k) for x in c[:3])


def noise2(r, w=N):
    """cheap smooth noise: sum of random sines"""
    waves = [(r.uniform(0.05, 0.4), r.uniform(0.05, 0.4), r.uniform(0, 6.28), r.uniform(0.3, 1)) for _ in range(5)]
    return lambda x, y: sum(a * math.sin(x * fx + y * fy + p) for fx, fy, p, a in waves) / 2.5


def T(name, kind, base, base2=None, seed=0):
    r = random.Random(seed + sum(map(ord, name)))
    im = Image.new('RGBA', (N, N)); px = im.load(); nz = noise2(r)
    b2 = base2 or sh(base, 0.7)
    for y in range(N):
        for x in range(N):
            c = list(base); v = r.uniform(-3, 3)
            if kind == 'planks':
                board = y // 16; yy = y % 16
                g = math.sin((x + board * 23) * 0.35 + math.sin(y * 0.4 + board) * 2.2) * 7 + nz(x, y + board * 50) * 5
                v += g + (board % 2) * 6 - 3
                if yy == 15: v -= 34
                if yy == 0: v += 10
                if (x + board * 19) % 64 == 0: v -= 30
                if (x + board * 19) % 64 in (3, 60) and yy in (4, 11): c = [60, 50, 40]
            elif kind == 'woodpanel':
                v += math.sin(x * 0.5 + nz(x, y) * 3) * 6 + nz(x * 2, y) * 6
            elif kind == 'fabric':
                v += (4 if (x + y) % 2 == 0 else -3) + nz(x, y) * 4
            elif kind == 'quilt':
                v += (3 if (x + y) % 2 == 0 else -3) + nz(x, y) * 3
                d = min((x + y) % 16, (x - y) % 16)
                if d == 0: v -= 26
                elif d == 1: v += 10
            elif kind == 'leather':
                v += nz(x, y) * 10
                if (x * 3 + y * 7) % 41 == 0: v -= 18
                if x % 16 in (0,) or y % 16 == 0: v -= 22
                if (x % 16 in (2,) or y % 16 == 2) and (x + y) % 3 == 0: v += 30
                if x % 16 == 8 and y % 16 == 8: c = list(sh(base, 0.5))
            elif kind == 'enamel':
                v += -y * 0.25 + 8 + (14 if 6 <= (x + y * 0.3) % 64 <= 9 else 0) + nz(x, y)
            elif kind == 'brushed':
                v += math.sin(y * 2.7 + r.random()) * 4 + nz(x * 4, y) * 3
            elif kind == 'brass':
                v += math.sin((x - y) * 0.2) * 16 + nz(x, y) * 4
            elif kind == 'marble':
                vein = abs(math.sin(x * 0.12 + y * 0.05 + nz(x, y) * 3))
                v += 6
                if vein < 0.06: c = [150, 150, 158]
                elif vein < 0.15: v -= 18
            elif kind == 'porcelain':
                v += 6 - y * 0.12 + (12 if (x - y) % 64 in (10, 11, 12) else 0)
            elif kind == 'glass':
                c = [base[0] - 20 + y, base[1] - 10 + y, base[2] + 10]
                if (x + y) % 64 in range(10, 14) or (x + y) % 64 in range(20, 22): v += 50
            elif kind == 'canvas':
                v += (5 if x % 2 == 0 else -2) + (4 if y % 2 == 0 else -2) + nz(x, y) * 5
                if x % 32 in (0, 31) or y % 32 in (0, 31): v -= 20
                if (x % 32 in (2, 29) or y % 32 in (2, 29)) and (x + y) % 4 < 2: v += 22
            elif kind == 'webbing':
                v += (10 if y % 4 < 2 else -8) + (x % 2) * 3
            elif kind == 'burlap':
                v += (12 if (x // 2 + y // 2) % 2 == 0 else -10) + nz(x, y) * 8
                if r.random() < 0.02: v -= 30
            elif kind == 'grate':
                c = list(base)
                v = 0 if (x % 16 < 3 or y % 16 < 3) else -200
            elif kind == 'water':
                c = [40, 110, 200]; v = nz(x * 2, y * 2) * 14
                if 0.92 < math.sin(x * 0.35 + nz(x, y) * 4) : c = [180, 220, 255]
            elif kind == 'lampshade':
                c = list(base); v += -abs(y - 32) * 0.6 + 20 + (x % 8 == 0) * -10
            elif kind == 'tile':
                v += nz(x, y) * 3
                if x % 16 == 0 or y % 16 == 0: c = [180, 180, 176]
            px[x, y] = tuple(clamp(k + v) for k in c) + (255,)
    im.save(f'{A}/textures/block/f2_{name}.png')
    return im


from PIL import ImageFont
F = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 40)


def decal(name, draw):
    im = Image.new('RGBA', (N, N)); d = ImageDraw.Draw(im); draw(im, d); im.save(f'{A}/textures/block/f2_{name}.png')


def make_textures():
    for n, k, c in (('oak', 'planks', (172, 128, 80)), ('dark', 'planks', (92, 60, 36)), ('birch', 'planks', (210, 192, 146)), ('spruce', 'planks', (118, 86, 52)),
                    ('oakp', 'woodpanel', (176, 132, 84)), ('darkp', 'woodpanel', (96, 62, 38)), ('birchp', 'woodpanel', (214, 196, 150)), ('walnut', 'woodpanel', (110, 72, 44)),
                    ('red', 'fabric', (172, 40, 46)), ('blue', 'fabric', (52, 80, 156)), ('gray', 'fabric', (112, 114, 120)), ('cream', 'fabric', (226, 210, 174)),
                    ('sheet', 'fabric', (238, 238, 232)), ('redq', 'quilt', (176, 44, 50)), ('blueq', 'quilt', (56, 84, 160)), ('grayq', 'quilt', (118, 120, 126)),
                    ('leather', 'leather', (124, 72, 40)), ('white', 'enamel', (232, 234, 236)), ('black', 'enamel', (38, 38, 42)), ('steel', 'brushed', (168, 174, 184)),
                    ('dsteel', 'brushed', (90, 94, 102)), ('brass', 'brass', (214, 172, 70)), ('marble', 'marble', (232, 230, 226)), ('porcelain', 'porcelain', (238, 240, 242)),
                    ('glass', 'glass', (170, 210, 232)), ('water', 'water', (0, 0, 0)), ('grate', 'grate', (40, 40, 44)), ('lampshade', 'lampshade', (246, 226, 170)),
                    ('tile', 'tile', (220, 226, 230)), ('rubber', 'fabric', (30, 30, 32))):
        T(n, k, c)
    for i, (c, acc) in enumerate(BAGS):
        T(f'bag{i + 1}', 'canvas', c); T(f'bag{i + 1}w', 'webbing', acc)
    T('sack', 'burlap', (168, 136, 92)); T('sackg', 'burlap', (226, 184, 70)); T('rope', 'webbing', (150, 116, 70))

    def dollar(im, d):
        im.paste(Image.open(f'{A}/textures/block/f2_sack.png'), (0, 0))
        d.ellipse([12, 12, 52, 52], outline=(40, 110, 50, 255), width=4)
        d.text((19, 8), "$", fill=(40, 110, 50, 255), font=F)
    decal('dollar', dollar)

    def gdollar(im, d):
        im.paste(Image.open(f'{A}/textures/block/f2_sackg.png'), (0, 0))
        d.ellipse([10, 10, 54, 54], outline=(120, 80, 10, 255), width=4)
        d.text((19, 8), "$", fill=(120, 80, 10, 255), font=F)
    decal('gdollar', gdollar)

    def coin(im, d):
        d.ellipse([2, 2, 62, 62], fill=(236, 190, 60, 255)); d.ellipse([10, 10, 54, 54], outline=(180, 130, 30, 255), width=4)
        d.text((20, 8), "$", fill=(170, 120, 20, 255), font=F)
    decal('coin', coin)

    def screen(im, d):          # a quiet landscape on the TV
        for y in range(N):
            c = (int(40 + y * 2.5), int(60 + y * 1.5), int(140 - y)) if y < 36 else (40, 90 - (y - 36), 50)
            d.line([(0, y), (N, y)], fill=c + (255,))
        d.ellipse([40, 14, 52, 26], fill=(255, 220, 150, 255))
        d.polygon([(0, 40), (14, 26), (28, 38), (44, 24), (64, 40), (64, 44), (0, 44)], fill=(50, 70, 90, 255))
    decal('screen', screen)

    def laptop(im, d):
        d.rectangle([0, 0, 63, 63], fill=(20, 24, 34, 255))
        r = random.Random(3)
        for k in range(9):
            y = 6 + k * 6; x0 = 6 + r.randint(0, 3) * 4
            d.line([(x0, y), (x0 + r.randint(10, 44), y)], fill=r.choice([(120, 200, 255), (200, 160, 255), (160, 230, 140)]) + (255,), width=2)
    decal('laptop', laptop)

    def keyboard(im, d):
        d.rectangle([0, 0, 63, 63], fill=(46, 46, 52, 255))
        for y in range(4, 60, 10):
            for x in range(4, 60, 8): d.rectangle([x, y, x + 5, y + 6], fill=(80, 80, 88, 255))
    decal('keyboard', keyboard)

    def books(im, d):
        r = random.Random(7); x = 0
        cols = [(150, 40, 40), (40, 80, 150), (50, 120, 60), (180, 140, 50), (100, 60, 120), (70, 70, 70), (160, 100, 60), (30, 90, 90)]
        while x < N:
            w = r.randint(5, 9); c = r.choice(cols); top = r.randint(0, 10)
            d.rectangle([x, top, x + w - 1, 63], fill=c + (255,))
            d.rectangle([x, top + 6, x + w - 1, top + 8], fill=(220, 190, 90, 255)); d.rectangle([x, 54, x + w - 1, 56], fill=(220, 190, 90, 255))
            d.line([(x + w - 1, top), (x + w - 1, 63)], fill=sh(c, 0.6) + (255,))
            x += w
    decal('books', books)

    def burner(im, d):
        d.rectangle([0, 0, 63, 63], fill=(30, 30, 34, 255))
        for rr, c in ((28, (70, 70, 76)), (22, (36, 36, 40)), (16, (110, 40, 30)), (10, (36, 36, 40)), (5, (90, 40, 30))): d.ellipse([32 - rr, 32 - rr, 32 + rr, 32 + rr], outline=c + (255,), width=3)
    decal('burner', burner)

    def clock(im, d):
        d.rectangle([0, 0, 63, 63], fill=(20, 20, 22, 255)); d.text((8, 18), "12:00", fill=(255, 60, 50, 255), font_size=20)
    decal('clock', clock)

    def notes(im, d):
        d.rectangle([0, 0, 63, 63], fill=(250, 240, 150, 255))
        for y in range(10, 60, 8): d.line([(6, y), (56, y)], fill=(120, 120, 200, 255), width=2)
    decal('notes', notes)

    def mirror(im, d):
        for y in range(N): d.line([(0, y), (N, y)], fill=(180 + y // 4, 200 + y // 5, 215 + y // 8, 255))
        for k in (10, 14, 40): d.line([(k, 0), (k + 30, 63)], fill=(240, 248, 255, 255), width=3)
    decal('mirror', mirror)

    def rug(im, d):
        d.rectangle([0, 0, 63, 63], fill=(140, 40, 40, 255)); d.rectangle([4, 4, 59, 59], outline=(220, 180, 90, 255), width=3)
        d.regular_polygon((32, 32, 16), 4, fill=(220, 180, 90, 255)); d.regular_polygon((32, 32, 8), 4, fill=(60, 30, 90, 255))
    decal('rug', rug)


BAGS = [((138, 106, 68), (90, 64, 40)), ((80, 140, 60), (40, 84, 32)), ((48, 102, 178), (26, 58, 110)), ((226, 124, 32), (140, 72, 16)), ((108, 118, 54), (60, 66, 30)),
        ((234, 234, 230), (190, 40, 40)), ((44, 44, 48), (20, 20, 26)), ((92, 100, 54), (56, 60, 30)), ((124, 60, 178), (72, 30, 112)), ((178, 32, 44), (230, 190, 60)),
        ((168, 136, 92), (110, 80, 40)), ((226, 184, 70), (140, 100, 30))]


def build(box, model, w):
    """box(f, t, mat, faces=None, uv=None, rot=None) with materials named f2_*"""
    M = {}
    P = 'f2_'

    def B(f, t, m, faces=None, full=False, rot=None):
        e = box(f, t, P + m, faces)
        if full:
            for fc in e['faces'].values(): fc['uv'] = [0, 0, 16, 16]
        if rot: e['rotation'] = {"origin": list(rot[0]), "axis": rot[1], "angle": rot[2]}
        return e

    def rounded(x0, y0, z0, x1, y1, z1, m, r=0.6):
        """a box with softened vertical edges: core + inset skins"""
        return [B((x0 + r, y0, z0), (x1 - r, y1, z1), m), B((x0, y0 + r, z0 + r), (x1, y1 - r, z1 - r), m)]

    def legs(x0, z0, x1, z1, h, m, t=2):
        return [B((x0, 0, z0), (x0 + t, h, z0 + t), m), B((x1 - t, 0, z0), (x1, h, z0 + t), m), B((x0, 0, z1 - t), (x0 + t, h, z1), m), B((x1 - t, 0, z1 - t), (x1, h, z1), m)]

    def turned(x, z, h, m, t=2):
        """a turned table leg: bulb, ring, taper, foot"""
        c = t / 2
        return [B((x - c * 0.7, 0, z - c * 0.7), (x + c * 0.7, 1.2, z + c * 0.7), m), B((x - c * 0.55, 1.2, z - c * 0.55), (x + c * 0.55, h * 0.55, z + c * 0.55), m),
                B((x - c, h * 0.55, z - c), (x + c, h * 0.7, z + c), m), B((x - c * 0.7, h * 0.7, z - c * 0.7), (x + c * 0.7, h, z + c * 0.7), m)]

    def handle(x0, y0, x1, y1, z, m='steel'):
        """bar handle on two standoffs, in front of a door at depth z"""
        vertical = (y1 - y0) > (x1 - x0)
        els = [B((x0, y0, z - 0.6), (x1, y1, z - 0.2), m)]
        if vertical: els += [B((x0, y0, z - 0.2), (x1, y0 + 0.6, z), m), B((x0, y1 - 0.6, z - 0.2), (x1, y1, z), m)]
        else: els += [B((x0, y0, z - 0.2), (x0 + 0.6, y1, z), m), B((x1 - 0.6, y0, z - 0.2), (x1, y1, z), m)]
        return els

    def door(x0, y0, x1, y1, z, m, frame):
        """raised-panel door: frame + inset panel"""
        return [B((x0, y0, z - 0.5), (x1, y1, z), frame), B((x0 + 1, y0 + 1, z - 0.8), (x1 - 1, y1 - 1, z - 0.5), m)]

    # ------------------------------------------------------------ beds
    def bed(name, blanket, frame, post=None):
        sheet, pil = 'sheet', 'cream' if name == 'royal_bed' else 'sheet'
        head = [B((-8, 0, 0), (24, 3, 2), frame), B((-8, 3, 0), (24, 20, 1.5), frame), B((-7, 5, 1.4), (23, 18, 2), blanket.replace('q', '')),
                B((-8, 20, -0.3), (24, 21.5, 2.2), frame)]
        for x in (-6, -2, 2, 6, 10, 14, 18):                                       # tufted headboard buttons
            for y in (8, 13): head.append(B((x, y, 1.9), (x + 0.6, y + 0.6, 2.2), 'brass'))
        head += [B((-8, 3, 2), (24, 6, 16), frame), B((-7.5, 6, 2), (23.5, 10, 16), 'sheet'),
                 B((-6, 10, 3), (6.5, 12.5, 8.5), pil), B((-5.5, 12.5, 3.5), (6, 13, 8), pil), B((9.5, 10, 3), (22, 12.5, 8.5), pil), B((10, 12.5, 3.5), (21.5, 13, 8), pil),
                 B((-7.7, 10, 10), (23.7, 10.8, 16), blanket), B((-7.7, 10.8, 10), (23.7, 11.4, 11.2), 'sheet'), B((-7.8, 6, 10), (-7.5, 10.8, 16), blanket), B((23.5, 6, 10), (23.8, 10.8, 16), blanket)]
        head += legs(-8, 0, 24, 16, 3, frame, 2)
        foot = [B((-8, 3, 0), (24, 6, 14), frame), B((-7.5, 6, 0), (23.5, 10, 14), 'sheet'), B((-7.7, 10, 0), (23.7, 10.8, 13.5), blanket),
                B((-7.8, 5, 0), (-7.5, 10.8, 13.5), blanket), B((23.5, 5, 0), (23.8, 10.8, 13.5), blanket), B((-7, 10.8, 8), (23, 11.6, 12), blanket.replace('q', '')),
                B((-8, 0, 14), (24, 13, 16), frame), B((-8, 13, 13.7), (24, 14, 16.3), frame), B((-6, 4, 15.9), (22, 11, 16.2), blanket.replace('q', ''))]
        foot += legs(-8, 0, 24, 16, 3, frame, 2)
        if post:
            for x in (-8, 22):
                head += [B((x, 0, 0), (x + 2, 31, 2), post), B((x - 0.4, 31, -0.4), (x + 2.4, 32, 2.4), 'brass')]
                foot += [B((x, 0, 14), (x + 2, 31, 16), post), B((x - 0.4, 31, 13.6), (x + 2.4, 32, 16.4), 'brass')]
            head += [B((-8, 30, 0), (24, 31, 2), post), B((-8, 30, 2), (-6, 31, 16), post), B((22, 30, 2), (24, 31, 16), post),
                     B((-7.5, 22, 0.2), (-6.5, 30, 1.8), 'red'), B((22.5, 22, 0.2), (23.5, 30, 1.8), 'red')]
            foot += [B((-8, 30, 14), (24, 31, 16), post), B((-8, 30, 0), (-6, 31, 14), post), B((22, 30, 0), (24, 31, 14), post)]
        model(name + '_head', head, P + frame); model(name + '_foot', foot, P + frame)
    bed('double_bed', 'redq', 'oak'); bed('royal_bed', 'blueq', 'darkp', 'darkp')

    # ------------------------------------------------------------ storage
    M['wardrobe'] = [B((0, 1, 2), (16, 30, 16), 'darkp'), B((-0.5, 0, 1.5), (16.5, 1.2, 16.5), 'dark'), B((-0.8, 30, 1.2), (16.8, 31, 16.8), 'dark'), B((-0.4, 31, 1.6), (16.4, 31.8, 16.4), 'darkp'),
                     *door(0.6, 1.8, 7.9, 29.4, 2, 'oakp', 'darkp'), *door(8.1, 1.8, 15.4, 29.4, 2, 'mirror', 'darkp'),
                     B((6.6, 14, 0.7), (7.3, 16.5, 1.2), 'brass'), B((8.7, 14, 0.7), (9.4, 16.5, 1.2), 'brass'),
                     B((0.4, 0, 2.3), (1.6, 1, 3.5), 'dark'), B((14.4, 0, 2.3), (15.6, 1, 3.5), 'dark')]
    M['wardrobe'][7]['faces']['north']['uv'] = [0, 0, 16, 16] if 'north' in M['wardrobe'][7]['faces'] else None
    fr = [B((1, 0.8, 1.5), (15, 30, 15), 'white'), B((1.4, 0, 2), (14.6, 0.8, 14.6), 'dsteel'),
          *rounded(1, 19.4, 0.8, 15, 29.8, 1.6, 'white', 0.4), *rounded(1, 1, 0.8, 15, 19, 1.6, 'white', 0.4),
          B((1, 19, 1), (15, 19.4, 1.6), 'rubber'), B((1, 29.8, 1), (15, 30, 1.6), 'rubber'),
          *handle(12.4, 21, 13.2, 28, 0.8), *handle(12.4, 9, 13.2, 17.5, 0.8),
          B((2.5, 22, 0.6), (7, 27.5, 0.8), 'dsteel'), B((3, 24.5, 0.5), (6.5, 27, 0.6), 'glass'), B((3.6, 22.5, 0.4), (5.9, 24, 0.6), 'black'),
          B((3, 12, 0.6), (6, 15, 0.7), 'notes', ['north'], full=True), B((7, 13, 0.6), (9, 15.5, 0.7), 'notes', ['north'], full=True), B((4, 15, 0.5), (5, 15.6, 0.6), 'red'),
          B((2, 29.9, 2.5), (14, 30.4, 13.5), 'grate', ['up'])]
    M['fridge'] = fr
    M['kitchen_cabinet'] = [B((0.5, 1, 2), (15.5, 14, 16), 'spruce'), B((0.5, 0, 3), (15.5, 1, 16), 'black'),
                            *door(0.8, 1.4, 7.8, 10.5, 2, 'oakp', 'spruce'), *door(8.2, 1.4, 15.2, 10.5, 2, 'oakp', 'spruce'),
                            B((0.8, 10.9, 1.4), (15.2, 13.6, 2), 'spruce'), *handle(5, 12, 11, 12.6, 1.4), *handle(6.2, 7, 6.9, 9.5, 1.5), *handle(9.1, 7, 9.8, 9.5, 1.5),
                            B((-0.3, 14, -0.3), (16.3, 15.6, 16.3), 'marble'), B((0, 15.6, 15), (16, 18, 16), 'tile')]
    M['kitchen_sink'] = [B((0.5, 1, 2), (15.5, 14, 16), 'spruce'), B((0.5, 0, 3), (15.5, 1, 16), 'black'), *door(0.8, 1.4, 7.8, 13.2, 2, 'oakp', 'spruce'), *door(8.2, 1.4, 15.2, 13.2, 2, 'oakp', 'spruce'),
                         *handle(6.2, 9, 6.9, 12, 1.5), *handle(9.1, 9, 9.8, 12, 1.5),
                         B((-0.3, 14, -0.3), (16.3, 15.6, 3), 'marble'), B((-0.3, 14, 13), (16.3, 15.6, 16.3), 'marble'), B((-0.3, 14, 3), (2, 15.6, 13), 'marble'), B((14, 14, 3), (16.3, 15.6, 13), 'marble'),
                         B((2, 10.5, 3), (14, 11, 13), 'steel'), B((2, 11, 3), (14, 15.6, 3.3), 'steel'), B((2, 11, 12.7), (14, 15.6, 13), 'steel'), B((2, 11, 3), (2.3, 15.6, 13), 'steel'), B((13.7, 11, 3), (14, 15.6, 13), 'steel'),
                         B((3, 11, 4), (13, 11.3, 12), 'water', ['up']), B((7.5, 10.9, 7.5), (8.5, 11.05, 8.5), 'black'),
                         B((7.2, 15.6, 13.4), (8.8, 16.2, 15), 'steel'), B((7.5, 16.2, 13.6), (8.5, 21, 14.6), 'steel'), B((7.5, 20, 9.5), (8.5, 21, 14.6), 'steel'), B((7.6, 18.6, 9.6), (8.4, 20, 10.4), 'steel'),
                         B((4.5, 15.6, 13.6), (6, 16.8, 15), 'steel'), B((10, 15.6, 13.6), (11.5, 16.8, 15), 'steel'), B((4.6, 16.8, 13.8), (5.9, 17.2, 14.8), 'red'), B((10.1, 16.8, 13.8), (11.4, 17.2, 14.8), 'blue'),
                         B((0, 15.6, 15), (16, 20, 16), 'tile')]
    st = [B((0, 1, 1), (16, 14, 16), 'black'), B((0.5, 0, 1.5), (15.5, 1, 15.5), 'dsteel'), B((1.5, 2.5, 0.6), (14.5, 10, 1), 'black'), B((3, 4, 0.5), (13, 9, 0.6), 'glass'),
          *handle(2.5, 10.8, 13.5, 11.5, 0.6), B((0, 12, 0.7), (16, 14, 1), 'steel')]
    for k, x in enumerate((2, 5, 8.5, 11.5)): st.append(B((x, 12.5, 0.3), (x + 1.4, 13.6, 0.7), 'black'))
    st += [B((-0.2, 14, 0.8), (16.2, 14.8, 16), 'steel')]
    for (x, z) in ((1.5, 2.5), (8.5, 2.5), (1.5, 9), (8.5, 9)):
        st += [B((x, 14.8, z), (x + 6, 14.9, z + 6), 'burner', ['up'], full=True), B((x, 14.9, z + 2.8), (x + 6, 15.5, z + 3.2), 'black'), B((x + 2.8, 14.9, z), (x + 3.2, 15.5, z + 6), 'black')]
    st += [B((0, 14.8, 15), (16, 21, 16), 'black'), B((5, 18, 14.8), (11, 20, 15), 'clock', ['north'], full=True), B((0, 21, 14.6), (16, 21.6, 16.2), 'steel')]
    M['stove'] = st
    # ------------------------------------------------------------ tables & seats
    M['dining_table'] = [B((-0.5, 13.5, -0.5), (16.5, 15, 16.5), 'oakp'), B((-0.8, 13, -0.8), (16.8, 13.6, 16.8), 'oak'), B((1.5, 11, 1.5), (14.5, 13, 14.5), 'oak'),
                         *turned(2.5, 2.5, 11, 'darkp', 2.4), *turned(13.5, 2.5, 11, 'darkp', 2.4), *turned(2.5, 13.5, 11, 'darkp', 2.4), *turned(13.5, 13.5, 11, 'darkp', 2.4),
                         B((5, 15, -0.6), (11, 15.15, 16.6), 'red'), B((5, 13.6, -0.65), (11, 15.15, -0.5), 'red'), B((5, 13.6, 16.5), (11, 15.15, 16.65), 'red'),
                         B((7, 15.15, 7), (9, 15.6, 9), 'brass'), B((7.6, 15.6, 7.6), (8.4, 19, 8.4), 'cream'), B((7.9, 19, 7.9), (8.1, 19.6, 8.1), 'black'),
                         B((2, 15.15, 3), (5, 15.3, 6), 'porcelain'), B((11, 15.15, 10), (14, 15.3, 13), 'porcelain')]
    ch = [B((2.5, 7, 2.5), (13.5, 8.2, 13.5), 'oakp'), B((3, 8.2, 3), (13, 9, 12), 'red'), *turned(3.5, 3.5, 7, 'oak', 1.6), *turned(12.5, 3.5, 7, 'oak', 1.6), *turned(3.5, 12.5, 7, 'oak', 1.6), *turned(12.5, 12.5, 7, 'oak', 1.6),
          B((3.5, 2.5, 3.2), (12.5, 3.2, 3.8), 'oak'), B((3.5, 2.5, 12.2), (12.5, 3.2, 12.8), 'oak'),
          B((2.8, 8.2, 12), (4.2, 21, 13.4), 'oak'), B((11.8, 8.2, 12), (13.2, 21, 13.4), 'oak'), B((2.5, 19, 11.8), (13.5, 21.5, 13.6), 'oakp')]
    for x in (5.5, 7.5, 9.5): ch.append(B((x, 9, 12.3), (x + 1, 19, 13.1), 'oak'))
    M['wooden_chair'] = ch
    M['armchair'] = [B((1, 0, 1.5), (2.2, 2, 2.7), 'dark'), B((13.8, 0, 1.5), (15, 2, 2.7), 'dark'), B((1, 0, 13.3), (2.2, 2, 14.5), 'dark'), B((13.8, 0, 13.3), (15, 2, 14.5), 'dark'),
                     B((1, 2, 1.5), (15, 6, 15), 'leather'), B((3.5, 6, 1.8), (12.5, 8.5, 12), 'leather'), B((3.3, 8.3, 2), (12.7, 8.6, 11.8), 'leather'),
                     B((1, 6, 12), (15, 18, 15), 'leather'), B((1.5, 18, 12.4), (14.5, 19, 14.8), 'leather'), B((3.5, 8.5, 11), (12.5, 17, 12.2), 'leather'),
                     B((0.5, 6, 1.5), (3.5, 11, 12), 'leather'), B((0.3, 10.5, 1.2), (3.7, 12, 12.2), 'leather'), B((12.5, 6, 1.5), (15.5, 11, 12), 'leather'), B((12.3, 10.5, 1.2), (15.7, 12, 12.2), 'leather'),
                     B((5, 9, 10.2), (11, 14, 11.2), 'cream', rot=((8, 11, 10.7), 'x', -22.5))]
    so = [B((0.5, 0, 1.5), (1.7, 2, 2.7), 'dark'), B((14.3, 0, 1.5), (15.5, 2, 2.7), 'dark'), B((0.5, 0, 13.3), (1.7, 2, 14.5), 'dark'), B((14.3, 0, 13.3), (15.5, 2, 14.5), 'dark'),
          B((0, 2, 1), (16, 6, 15), 'grayq'), B((0.5, 6, 1.3), (7.9, 8.6, 11), 'grayq'), B((8.1, 6, 1.3), (15.5, 8.6, 11), 'grayq'),
          B((0, 6, 11), (16, 17, 15), 'grayq'), B((0.6, 8.6, 9), (7.9, 16, 11.2), 'grayq'), B((8.1, 8.6, 9), (15.4, 16, 11.2), 'grayq'), B((0, 17, 11.5), (16, 18, 15), 'gray'),
          B((1.5, 9, 7.5), (6, 13, 9), 'red', rot=((3.75, 11, 8.25), 'z', 22.5)), B((10, 9, 7.5), (14.5, 13, 9), 'cream', rot=((12.25, 11, 8.25), 'z', -22.5))]
    M['sofa'] = so
    M['coffee_table'] = [B((1, 5.5, 3), (15, 6.5, 13), 'walnut'), B((2, 6.5, 4), (14, 6.8, 12), 'glass', ['up']), B((1, 6.5, 3), (15, 7, 4), 'walnut'), B((1, 6.5, 12), (15, 7, 13), 'walnut'),
                         B((1, 6.5, 4), (2, 7, 12), 'walnut'), B((14, 6.5, 4), (15, 7, 12), 'walnut'), *legs(1.5, 3.5, 14.5, 12.5, 5.5, 'walnut', 1.4),
                         B((2, 1.5, 4), (14, 2.3, 12), 'walnut'), B((3, 2.3, 5), (8, 3.1, 9), 'red'), B((3.3, 3.1, 5.3), (7.7, 3.6, 8.7), 'blue'), B((9, 2.3, 6), (13, 2.5, 11), 'notes', ['up'], full=True),
                         B((9.5, 7, 6.5), (11, 9, 8), 'porcelain'), B((10, 9, 7), (10.5, 9.1, 7.5), 'dark')]
    bc = [B((0, 0, 15), (16, 16, 16), 'oakp'), B((0, 0, 3), (1, 16, 15), 'oak'), B((15, 0, 3), (16, 16, 15), 'oak'), B((-0.3, 15.5, 2.7), (16.3, 16.5, 16.3), 'oak'),
          B((0, 0, 2.8), (16, 1, 15), 'oak'), B((1, 7.6, 3), (15, 8.6, 15), 'oak')]
    r = random.Random(4); cols = ['red', 'blue', 'leather', 'cream', 'darkp', 'gray']
    for y0, y1 in ((1, 7.6), (8.6, 15.5)):
        x = 1.2
        while x < 14.2:
            wdt = r.choice((0.9, 1.1, 1.3)); h = y1 - y0 - r.choice((0.3, 0.8, 1.4, 2))
            if r.random() < 0.12 and x < 12:
                bc.append(B((x, y0, 6), (x + wdt, y0 + h, 14.5), r.choice(cols), rot=((x, y0, 10), 'z', -22.5))); x += 2.6; continue
            bc.append(B((x, y0, 5.5), (x + wdt, y0 + h, 14.8), r.choice(cols)))
            bc.append(B((x, y0 + h * 0.7, 5.45), (x + wdt, y0 + h * 0.75, 5.5), 'brass', ['north']))
            x += wdt + 0.05
    
    M['bookcase'] = bc
    M['office_desk'] = [B((-0.3, 12.5, 1.7), (16.3, 13.8, 16.3), 'birchp'), B((10, 0, 2), (16, 12.5, 16), 'birchp'), B((0.3, 0, 2.3), (1.7, 12.5, 15.7), 'birchp'), B((1.7, 6, 15), (10, 12.5, 15.7), 'birchp'),
                        *door(10.4, 7, 15.6, 12, 2, 'birchp', 'birch'), *door(10.4, 1, 15.6, 6.6, 2, 'birchp', 'birch'), *handle(12, 9.2, 14, 9.8, 1.4), *handle(12, 3.6, 14, 4.2, 1.4),
                        B((2.5, 13.8, 6), (8.5, 14.1, 10.5), 'dsteel'), B((2.8, 14.1, 6.5), (8.2, 14.15, 10), 'keyboard', ['up'], full=True),
                        B((2.5, 13.8, 10.5), (8.5, 18.3, 10.8), 'dsteel', rot=((5.5, 13.8, 10.65), 'x', 22.5)), B((2.8, 14.1, 10.45), (8.2, 18, 10.5), 'laptop', ['north'], full=True, rot=((5.5, 13.8, 10.65), 'x', 22.5)),
                        B((11, 13.8, 4), (12.6, 16, 5.6), 'porcelain'), B((12.6, 14.5, 4.5), (13.2, 15.5, 5.1), 'porcelain'), B((11.3, 15.6, 4.3), (12.3, 15.9, 5.3), 'dark'),
                        B((12, 13.8, 9), (15, 14, 13), 'notes', ['up'], full=True, rot=((13.5, 13.9, 11), 'y', 22.5)),
                        B((13.5, 13.8, 13.5), (15, 14.2, 15), 'black'), B((14, 14.2, 14), (14.5, 18, 14.5), 'steel'), B((12.5, 17.5, 12.5), (15.5, 19, 15.5), 'lampshade')]
    M['floor_lamp'] = [B((4.5, 0, 4.5), (11.5, 0.8, 11.5), 'brass'), B((5.5, 0.8, 5.5), (10.5, 1.4, 10.5), 'black'), B((7.5, 1.4, 7.5), (8.5, 22, 8.5), 'brass'),
                       B((7.2, 11, 7.2), (8.8, 12, 8.8), 'brass'), B((7.2, 21, 7.2), (8.8, 22, 8.8), 'brass'),
                       B((4, 22, 4), (12, 23, 12), 'lampshade'), B((4.5, 23, 4.5), (11.5, 26, 11.5), 'lampshade'), B((5, 26, 5), (11, 28.5, 11), 'lampshade'), B((5.5, 28.5, 5.5), (10.5, 29, 10.5), 'lampshade'),
                       B((7, 22.5, 7), (9, 24.5, 9), 'cream'), B((7.8, 18, 7.4), (8.2, 22, 7.6), 'black')]
    M['television'] = [B((2, 0, 5), (14, 3, 12), 'walnut'), B((2.5, 0.5, 4.8), (13.5, 2.5, 5), 'black'), B((5, 3, 7.5), (11, 3.4, 10.5), 'black'), B((7.2, 3.4, 8.4), (8.8, 5, 9.4), 'black'),
                       B((-0.5, 5, 8), (16.5, 15, 9), 'black'), B((-0.2, 5.3, 7.9), (16.2, 14.7, 8), 'screen', ['north'], full=True), B((7.5, 5.05, 7.85), (8.5, 5.25, 7.95), 'red'),
                       B((3, 0.5, 6), (6, 2.5, 6.2), 'dsteel'), B((10, 3, 6), (12.5, 3.4, 7.5), 'black')]
    bt = [B((0, 2, 2), (16, 9, 3.5), 'porcelain'), B((0, 2, 12.5), (16, 9, 14), 'porcelain'), B((0, 2, 2), (1.5, 9, 14), 'porcelain'), B((14.5, 2, 2), (16, 9, 14), 'porcelain'),
          B((1.5, 2, 3.5), (14.5, 2.5, 12.5), 'porcelain'), B((-0.3, 9, 1.7), (16.3, 10, 3.7), 'porcelain'), B((-0.3, 9, 12.3), (16.3, 10, 14.3), 'porcelain'), B((-0.3, 9, 3.7), (1.7, 10, 12.3), 'porcelain'), B((14.3, 9, 3.7), (16.3, 10, 12.3), 'porcelain'), B((1.5, 2.5, 3.5), (14.5, 7.5, 12.5), 'water', ['up']),
          B((0.5, 1, 2.5), (15.5, 2, 13.5), 'porcelain')]
    for x, z in ((1, 2.5), (13.5, 2.5), (1, 12), (13.5, 12)): bt += [B((x, 0, z), (x + 1.5, 1, z + 1.5), 'brass'), B((x + 0.2, 1, z + 0.2), (x + 1.3, 2, z + 1.3), 'brass')]
    bt += [B((13.2, 10, 7.3), (14.4, 13, 8.7), 'brass'), B((11.5, 12.4, 7.6), (14.4, 13.4, 8.4), 'brass'), B((12.3, 10, 6), (13, 11, 6.7), 'brass'), B((12.3, 10, 9.3), (13, 11, 10),'brass'),
           B((4, 7.5, 6), (6, 8.8, 7.5), 'brass' if False else 'lampshade'), B((5.4, 8.8, 6.3), (6.4, 9.8, 7.2), 'lampshade'), B((6.4, 9.1, 6.6), (6.9, 9.4, 6.9), 'red'),
           B((2.5, 7.5, 9), (12, 7.6, 11.5), 'sheet', ['up'])]
    M['bathtub'] = bt
    M['toilet'] = [B((5, 0, 3), (11, 2, 10), 'porcelain'), B((4.5, 2, 2.5), (11.5, 6, 10.5), 'porcelain'), B((4, 6, 1.8), (12, 7.4, 11), 'porcelain'),
                   B((5.2, 7.2, 3), (10.8, 7.35, 9.5), 'water', ['up']), B((3.8, 7.4, 1.6), (12.2, 8, 11.2), 'white'), B((3.8, 8, 10.6), (12.2, 15.5, 11.2), 'white', rot=((8, 8, 10.9), 'x', 0)),
                   B((3.2, 7, 11), (12.8, 15, 14.5), 'porcelain'), B((2.9, 15, 10.7), (13.1, 16, 14.8), 'porcelain'), B((7, 16, 12), (9, 16.3, 13.5), 'steel'),
                   B((11.6, 13, 10.6), (12.6, 13.6, 11), 'steel'), B((7.5, 2, 14.5), (8.5, 6, 16), 'steel'), B((2, 2, 15.2), (5, 6, 15.6), 'sheet')]
    gr = [B((0, 0, 14.5), (16, 16, 16), 'darkp'), B((0, 0, 14), (16, 1, 14.5), 'dark'), B((0, 15, 14), (16, 16, 14.5), 'dark')]
    for y in (3, 9):
        gr += [B((1, y, 12), (2.5, y + 1, 14.5), 'brass'), B((13.5, y, 12), (15, y + 1, 14.5), 'brass')]
    gr += [B((-1, 4, 12.6), (14, 5.2, 13.6), 'black'), B((10, 2.2, 12.6), (16.5, 4.2, 13.6), 'walnut'), B((3, 3.2, 12.6), (5, 4, 13.6), 'black'), B((-1.5, 4.3, 12.7), (0, 4.9, 13.5), 'dsteel'),
           B((0, 10, 12.6), (15, 11.2, 13.6), 'dsteel'), B((11, 8, 12.6), (17, 10, 13.6), 'walnut'), B((2, 11.2, 12.7), (5, 12.2, 13.5), 'black'), B((-1, 10.3, 12.7), (0, 10.9, 13.5), 'dsteel'),
           B((2, 0, 10), (6, 3, 13), 'leather'), B((2.2, 3, 10.2), (5.8, 3.3, 12.8), 'brass'), B((8, 0, 11), (11, 2, 13.5), 'dsteel'), B((8.2, 2, 11.2), (10.8, 2.1, 13.3), 'red')]
    M['gun_rack'] = gr
    M['nightstand'] = [B((2, 1, 2), (14, 11, 14), 'walnut'), B((1.7, 11, 1.7), (14.3, 11.8, 14.3), 'walnut'), *legs(2, 2, 14, 14, 1, 'dark', 1.4),
                       *door(2.3, 6.2, 13.7, 10.6, 2, 'darkp', 'walnut'), *door(2.3, 1.4, 13.7, 5.8, 2, 'darkp', 'walnut'),
                       B((7.3, 8, 1), (8.7, 9, 1.5), 'brass'), B((7.3, 3.2, 1), (8.7, 4.2, 1.5), 'brass'),
                       B((4, 11.8, 7), (7, 12.4, 10), 'brass'), B((5.2, 12.4, 8.2), (5.8, 15.5, 8.8), 'brass'), B((3.5, 15.5, 6.5), (7.5, 18.5, 10.5), 'lampshade'),
                       B((9, 11.8, 4), (13, 12.6, 9), 'red'), B((9.2, 12.6, 4.2), (12.8, 13.2, 8.8), 'blue'),
                       B((9.5, 11.8, 10), (12.5, 13.5, 11.5), 'black'), B((9.7, 12, 9.9), (12.3, 13.3, 10), 'clock', ['north'], full=True)]
    return M


def bags(box, model):
    P = 'f2_'

    def B(f, t, m, faces=None, full=False):
        e = box(f, t, P + m, faces)
        if full:
            for fc in e['faces'].values(): fc['uv'] = [0, 0, 16, 16]
        return e
    out = {}
    for i in range(1, 13):
        c, w = f'bag{i}', f'bag{i}w'
        for half, y in (('lower', 0), ('upper', 8)):
            if i <= 10:
                big = i >= 6
                els = [B((4.4, y, 5), (11.6, y + 6.6, 11), c), B((4, y + 0.5, 5.4), (12, y + 6.2, 10.6), c), B((4.6, y + 6.6, 5.4), (11.4, y + 7.1, 10.6), c),     # rounded body
                       B((5, y + 0.6, 3.8), (11, y + 4.6, 5), c), B((5.4, y + 4.6, 4.1), (10.6, y + 4.9, 5), c),                                               # front pocket
                       B((5, y + 4.4, 3.75), (11, y + 4.6, 3.85), 'dsteel', ['north']), B((10.2, y + 4, 3.6), (10.6, y + 4.6, 3.8), 'dsteel'),                # zip + pull
                       B((4.3, y + 4.2, 4.6), (11.7, y + 7.4, 9), c),                                                                                         # top flap
                       B((5.4, y + 1.5, 3.6), (6.4, y + 7.5, 3.9), w), B((9.6, y + 1.5, 3.6), (10.6, y + 7.5, 3.9), w),                                         # buckle straps
                       B((5.3, y + 2.3, 3.4), (6.5, y + 3.1, 3.7), 'brass'), B((9.5, y + 2.3, 3.4), (10.7, y + 3.1, 3.7), 'brass'),
                       B((5, y + 0.6, 11), (6.4, y + 6.6, 11.8), w), B((9.6, y + 0.6, 11), (11, y + 6.6, 11.8), w),                                            # shoulder straps
                       B((7, y + 7.1, 7.4), (9, y + 7.6, 7.8), w), B((7, y + 7.1, 8.6), (9, y + 7.6, 9), w), B((7, y + 7.6, 7.4), (9, y + 7.9, 9), w),          # top handle
                       B((3.2, y + 0.5, 6.5), (4, y + 4, 9.5), c), B((3.1, y + 3.6, 6.4), (4.1, y + 4, 9.6), w)]                                              # side pocket
                if big: els += [B((2.6, y + 0.5, 6.8), (3.2, y + 5, 9.2), 'steel'), B((2.7, y + 5, 7.2), (3.1, y + 5.6, 8.8), 'black')]                     # water bottle
                if i >= 8: els += [B((12, y + 0.8, 6), (13, y + 5, 10), c), B((12.9, y + 2, 7.5), (13.1, y + 3, 8.5), 'brass')]                              # second side pocket
                if i == 10 or i == 9: els += [B((3.5, y + 7.1, 9.2), (12.5, y + 8, 11), 'redq' if i == 10 else 'grayq'), B((5, y + 7, 9), (5.6, y + 8.1, 11.2), w), B((10.4, y + 7, 9), (11, y + 8.1, 11.2), w)]  # bedroll
            else:
                gold = i == 12
                s = 'sackg' if gold else 'sack'
                def octa(y0, y1, h, c=8.0):
                    r = h * 0.3
                    return [B((c - h + r, y + y0, c - h), (c + h - r, y + y1, c + h), s), B((c - h, y + y0, c - h + r), (c + h, y + y1, c + h - r), s)]
                els = octa(0, 0.6, 4.2) + octa(0.6, 2, 5) + octa(2, 4.2, 5.3) + octa(4.2, 5.2, 4.6) + octa(5.2, 5.9, 3.4) + octa(5.9, 6.4, 2.2) + \
                      [B((5.7, y + 6.3, 5.7), (10.3, y + 6.9, 10.3), 'rope')] + octa(6.9, 7.3, 2.0) + octa(7.3, 8, 2.8) + \
                      [B((10.1, y + 5.8, 7.4), (11.2, y + 6.4, 8.2), 'rope'), B((10.6, y + 4.6, 7.6), (11, y + 5.8, 8), 'rope'),
                       B((5, y + 1.2, 2.65), (11, y + 4.6, 2.7), 'gdollar' if gold else 'dollar', ['north'], full=True)]
                if gold:
                    for (x, z, k) in ((1.5, 2, 0), (12.5, 11.5, 0.2), (2.5, 12.6, 0.1), (13, 3, 0.15)): els.append(B((x, y, z), (x + 1.6, y + 0.35 + k, z + 1.6), 'coin', full=True))
                else:
                    els.append(B((12, y, 2.5), (14, y + 0.3, 3.5), 'sheet'))
            model(f'bag{i}_{half}', els, P + ('sackg' if i == 12 else 'sack' if i == 11 else c))
    return out
