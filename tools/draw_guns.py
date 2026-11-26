"""Hand-designed 32x32 arrow guns with material shading: every part has a material (4-tone palette),
top edges get light, bottom edges shadow, metal gets a specular line, and the whole silhouette gets a dark outline."""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet/textures/item')
N = 32

MAT = {  # dark, mid, light, highlight
    'gunmetal': [(40, 43, 50), (64, 69, 80), (96, 103, 116), (146, 154, 168)],
    'steel':    [(74, 79, 90), (118, 124, 137), (162, 169, 182), (214, 220, 230)],
    'black':    [(24, 24, 28), (40, 41, 47), (60, 62, 70), (92, 95, 106)],
    'wood':     [(78, 44, 22), (118, 70, 36), (152, 96, 52), (184, 126, 76)],
    'darkwood': [(56, 32, 18), (86, 52, 28), (112, 70, 40), (140, 94, 58)],
    'olive':    [(52, 60, 32), (78, 90, 48), (104, 118, 64), (138, 152, 92)],
    'tan':      [(120, 96, 62), (164, 136, 92), (196, 170, 122), (226, 204, 160)],
    'white':    [(132, 146, 164), (186, 198, 214), (222, 230, 240), (255, 255, 255)],
    'copper':   [(104, 52, 26), (156, 86, 44), (200, 122, 66), (240, 172, 112)],
    'brass':    [(120, 88, 30), (176, 136, 52), (218, 182, 84), (250, 228, 150)],
    'red':      [(100, 20, 20), (156, 34, 34), (200, 58, 52), (240, 110, 100)],
    'purple':   [(38, 18, 66), (66, 34, 116), (104, 58, 172), (164, 118, 236)],
    'navy':     [(24, 30, 52), (40, 50, 84), (62, 76, 120), (100, 118, 168)],
    'rubber':   [(30, 28, 28), (48, 44, 44), (66, 62, 62), (88, 84, 84)],
}
GLOW = {'green': (110, 255, 110), 'cyan': (110, 230, 255), 'orange': (255, 150, 40), 'purple': (200, 120, 255), 'red': (255, 60, 50),
        'ice': (200, 245, 255), 'gold': (255, 214, 90), 'white': (255, 255, 255)}


class G:
    def __init__(self):
        self.m = [[None] * N for _ in range(N)]   # material name per pixel
        self.fx = {}                                # (x,y) -> fixed colour drawn on top (glows, details)

    def rect(self, x0, y0, x1, y1, mat):
        for y in range(max(0, y0), min(N, y1 + 1)):
            for x in range(max(0, x0), min(N, x1 + 1)):
                self.m[y][x] = mat
                self.fx.pop((x, y), None)
        return self

    def poly(self, pts, mat):
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        for y in range(min(ys), max(ys) + 1):
            for x in range(min(xs), max(xs) + 1):
                if inside(x + 0.5, y + 0.5, pts): self.rect(x, y, x, y, mat)
        return self

    def px(self, x, y, col):
        if 0 <= x < N and 0 <= y < N: self.fx[(x, y)] = col
        return self

    def line(self, x0, y0, x1, y1, col):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1): self.px(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), col)
        return self

    def glowline(self, x0, y, x1, glow):
        c = GLOW[glow]
        for x in range(x0, x1 + 1): self.px(x, y, c)
        return self

    def render(self):
        im = Image.new('RGBA', (N, N), (0, 0, 0, 0))
        filled = lambda x, y: 0 <= x < N and 0 <= y < N and self.m[y][x] is not None
        for y in range(N):
            for x in range(N):
                mat = self.m[y][x]
                if mat is None: continue
                pal = MAT[mat]
                up = filled(x, y - 1) and self.m[y - 1][x] == mat
                down = filled(x, y + 1) and self.m[y + 1][x] == mat
                up2 = filled(x, y - 2) and self.m[y - 2][x] == mat
                if not up: c = pal[2]
                elif not down: c = pal[0]
                elif not up2 and mat in ('steel', 'gunmetal', 'white', 'copper', 'brass', 'black'): c = pal[3]  # specular line
                else: c = pal[1]
                if mat in ('wood', 'darkwood') and up and down and (x * 7 + y * 3) % 11 == 0: c = pal[0]   # grain
                im.putpixel((x, y), c + (255,))
        for (x, y), c in self.fx.items():
            im.putpixel((x, y), (c + (255,)) if len(c) == 3 else c)
        # outline
        out = im.copy()
        for y in range(N):
            for x in range(N):
                if im.getpixel((x, y))[3]: continue
                ns = [im.getpixel((x + dx, y + dy)) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < N and 0 <= y + dy < N and im.getpixel((x + dx, y + dy))[3]]
                if ns:
                    r = sum(c[0] for c in ns) // len(ns); g = sum(c[1] for c in ns) // len(ns); b = sum(c[2] for c in ns) // len(ns)
                    out.putpixel((x, y), (int(r * 0.28) + 8, int(g * 0.28) + 8, int(b * 0.3) + 10, 255))
        return out


def inside(x, y, pts):
    c = False
    for i in range(len(pts)):
        x1, y1 = pts[i]; x2, y2 = pts[(i + 1) % len(pts)]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1 + 1e-9) + x1: c = not c
    return c


def arrow(g, x, y, metal='brass'):
    """Loaded arrow poking out of the muzzle."""
    g.rect(x, y, x + 1, y, 'tan')
    pal = MAT[metal]
    g.px(x + 2, y - 1, pal[1]).px(x + 2, y, pal[3]).px(x + 2, y + 1, pal[1]).px(x + 3, y, pal[2])


def grip(g, x, y, h=7, mat='black', slant=2):
    g.poly([(x, y), (x + 4, y), (x + 4 - slant, y + h), (x - slant, y + h)], mat)
    for i in range(2, h, 2): g.px(x + 1 - i * slant // h, y + i, MAT[mat][0])   # texture lines


def guard(g, x, y):
    col = MAT['black'][0]
    g.line(x, y, x, y + 3, col).line(x, y + 3, x + 4, y + 3, col)
    g.px(x + 2, y + 1, MAT['steel'][2])   # trigger


def sights(g, x0, x1, y):
    g.px(x0, y, MAT['black'][1]).px(x1, y, MAT['black'][1])


D = {}

def gun(f):
    D[f.__name__] = f
    return f

@gun
def arrow_pistol(g):
    g.rect(8, 10, 22, 13, 'gunmetal')                 # slide
    for x in range(10, 15, 2): g.px(x, 11, MAT['gunmetal'][0])   # serrations
    g.rect(22, 11, 25, 12, 'steel')                    # barrel
    g.rect(8, 14, 20, 15, 'black')                     # frame
    grip(g, 9, 15, 7, 'black', 2)
    g.rect(9, 18, 11, 20, 'rubber')
    guard(g, 13, 15)
    sights(g, 9, 21, 9)
    arrow(g, 26, 11)

@gun
def silenced_arrow_pistol(g):
    arrow_pistol(g)
    g.rect(22, 10, 30, 13, 'black')                    # integrated suppressor
    for x in range(23, 30, 2): g.px(x, 10, MAT['black'][3])
    g.px(30, 11, MAT['steel'][2])

@gun
def arrow_rifle(g):
    g.poly([(1, 12), (9, 12), (9, 16), (5, 17), (1, 18)], 'wood')     # stock
    g.rect(1, 12, 1, 18, 'rubber')                     # butt pad
    g.rect(9, 10, 19, 15, 'gunmetal')                  # receiver
    g.rect(10, 8, 18, 9, 'black')                      # top rail
    for x in range(10, 19, 2): g.px(x, 8, MAT['black'][3])
    g.rect(19, 11, 24, 15, 'black')                    # handguard
    for x in range(20, 24, 2): g.px(x, 13, MAT['gunmetal'][1])
    g.rect(24, 12, 28, 13, 'steel')                    # barrel
    g.px(28, 11, MAT['black'][1])                      # front sight
    g.poly([(13, 16), (16, 16), (17, 21), (14, 21)], 'black')   # curved mag
    grip(g, 10, 16, 5, 'darkwood', 1)
    arrow(g, 28, 12)

@gun
def double_arrow_rifle(g):
    g.poly([(1, 13), (8, 12), (8, 17), (3, 18), (1, 19)], 'wood')
    g.rect(8, 11, 13, 16, 'steel')                     # action
    for y in (11, 14):
        g.rect(13, y, 28, y + 1, 'gunmetal')           # two barrels
    g.rect(13, 13, 28, 13, 'black')
    g.rect(15, 15, 22, 17, 'wood')                     # forend
    grip(g, 8, 16, 4, 'wood', 1)
    guard(g, 11, 16)
    arrow(g, 28, 11); arrow(g, 28, 14)

@gun
def arrow_shotgun(g):
    g.poly([(0, 13), (8, 12), (9, 16), (3, 17), (0, 19)], 'wood')
    g.rect(8, 11, 15, 15, 'gunmetal')
    g.rect(15, 11, 29, 12, 'steel')                    # barrel
    g.rect(15, 14, 26, 15, 'gunmetal')                 # magazine tube
    g.rect(17, 13, 23, 16, 'wood')                     # pump
    for x in range(18, 23, 2): g.px(x, 16, MAT['wood'][0])
    grip(g, 9, 16, 4, 'wood', 1)
    guard(g, 12, 16)
    g.px(28, 10, MAT['brass'][2])                      # bead sight
    arrow(g, 29, 11)

@gun
def arrow_repeater(g):
    g.line(1, 12, 1, 17, MAT['steel'][1]).line(1, 12, 7, 12, MAT['steel'][1]).line(1, 17, 7, 16, MAT['steel'][1])   # wire stock
    g.rect(7, 10, 20, 15, 'black')
    g.rect(8, 9, 18, 9, 'gunmetal')
    g.rect(20, 11, 26, 13, 'gunmetal')
    g.rect(13, 16, 15, 23, 'gunmetal')                 # straight mag
    g.px(14, 22, MAT['steel'][2])
    grip(g, 8, 16, 6, 'black', 1)
    g.rect(21, 14, 22, 17, 'black')                    # front grip
    arrow(g, 26, 12)

@gun
def toxic_arrow_rifle(g):
    arrow_rifle(g)
    g.rect(19, 11, 24, 15, 'olive')
    g.rect(11, 5, 17, 8, 'olive')                      # toxin canister
    for x in range(12, 17): g.px(x, 6, GLOW['green'])
    g.px(12, 7, GLOW['green']).px(16, 7, (60, 160, 60))
    g.glowline(10, 13, 18, 'green')
    g.px(30, 12, GLOW['green']).px(29, 11, GLOW['green']).px(29, 13, GLOW['green'])

@gun
def frost_arrow_gun(g):
    g.rect(6, 10, 24, 15, 'white')
    g.rect(9, 6, 13, 9, 'white')                       # coolant tank
    g.rect(10, 7, 12, 8, 'navy'); g.px(11, 7, GLOW['ice'])
    g.rect(24, 11, 28, 13, 'steel')
    g.glowline(8, 13, 22, 'cyan')
    for x in (14, 18): g.rect(x, 9, x + 1, 9, 'navy')
    grip(g, 8, 16, 6, 'navy', 1)
    g.px(29, 12, GLOW['ice']).px(30, 12, GLOW['cyan']).px(29, 11, GLOW['ice']).px(29, 13, GLOW['ice'])

@gun
def arrow_sniper(g):
    g.poly([(0, 12), (9, 13), (9, 17), (4, 18), (0, 20)], 'olive')
    g.rect(3, 12, 7, 13, 'olive')                      # cheek rest
    g.rect(9, 12, 17, 16, 'gunmetal')
    g.rect(17, 13, 30, 14, 'steel')                    # long barrel
    g.rect(28, 12, 30, 15, 'black')                    # muzzle brake
    g.rect(8, 7, 19, 9, 'black')                       # scope
    g.rect(7, 6, 9, 10, 'black'); g.rect(18, 6, 20, 10, 'black')
    g.px(7, 8, GLOW['cyan']).px(20, 8, GLOW['cyan'])
    g.rect(11, 10, 12, 11, 'gunmetal'); g.rect(15, 10, 16, 11, 'gunmetal')
    g.line(22, 15, 20, 20, MAT['steel'][1]).line(23, 15, 25, 20, MAT['steel'][1])   # bipod
    grip(g, 10, 17, 5, 'olive', 1)
    g.rect(14, 17, 16, 19, 'black')
    arrow(g, 30, 13) if False else None

@gun
def incendiary_repeater(g):
    g.rect(7, 10, 22, 15, 'black')
    for x in range(14, 21, 2): g.rect(x, 10, x, 12, 'red')   # heat vents
    for x in range(14, 21, 2): g.px(x, 11, GLOW['orange'])
    g.rect(22, 11, 27, 14, 'gunmetal')
    g.rect(8, 16, 13, 20, 'red')                       # fuel tank
    g.px(9, 17, MAT['red'][3]).rect(10, 21, 11, 21, 'steel')
    grip(g, 15, 16, 5, 'black', 1)
    g.px(28, 12, GLOW['orange']).px(29, 12, GLOW['red']).px(28, 11, GLOW['gold']).px(28, 13, GLOW['orange']).px(30, 12, (255, 220, 120))

@gun
def tesla_arrow_gun(g):
    g.rect(5, 11, 16, 15, 'gunmetal')
    g.rect(16, 12, 27, 13, 'steel')
    for x in (17, 20, 23):                             # copper coils
        g.rect(x, 10, x + 1, 15, 'copper')
    g.rect(7, 7, 12, 10, 'navy')                       # capacitor
    g.px(8, 8, GLOW['cyan']).px(10, 8, GLOW['cyan']).px(9, 9, GLOW['white'])
    grip(g, 7, 16, 6, 'black', 1)
    for (x, y) in ((28, 11), (29, 13), (27, 14), (30, 12), (26, 9)): g.px(x, y, GLOW['cyan'])

@gun
def arrow_gatling(g):
    g.rect(4, 9, 13, 18, 'black')                      # motor housing
    g.rect(5, 10, 6, 17, 'gunmetal')
    g.rect(13, 10, 15, 17, 'gunmetal')                 # barrel clamp
    for y in (10, 13, 16): g.rect(15, y, 29, y + 1, 'steel')   # three barrels
    g.rect(24, 9, 25, 18, 'gunmetal')
    g.rect(6, 5, 11, 6, 'black'); g.rect(6, 6, 6, 8, 'black'); g.rect(11, 6, 11, 8, 'black')   # carry handle
    g.rect(6, 19, 9, 24, 'brass')                      # ammo belt
    for y in range(19, 25, 2): g.px(7, y, MAT['brass'][0])
    grip(g, 10, 19, 4, 'black', 1)

@gun
def harpoon_gun(g):
    g.poly([(1, 13), (8, 12), (8, 16), (2, 17)], 'darkwood')
    g.rect(8, 11, 18, 16, 'steel')
    g.rect(10, 17, 15, 20, 'tan')                      # rope coil
    for x in range(10, 16, 2): g.px(x, 18, MAT['tan'][0])
    g.rect(18, 12, 24, 14, 'gunmetal')
    g.rect(24, 13, 28, 13, 'steel')                    # harpoon shaft
    g.px(29, 13, MAT['steel'][3]).px(30, 13, MAT['steel'][2]).px(28, 11, MAT['steel'][1]).px(29, 12, MAT['steel'][2]).px(28, 15, MAT['steel'][1]).px(29, 14, MAT['steel'][2])
    grip(g, 9, 17, 4, 'darkwood', 1)

@gun
def arrow_railgun(g):
    g.rect(4, 11, 15, 16, 'white')
    g.rect(15, 10, 30, 11, 'white')                    # upper rail
    g.rect(15, 15, 30, 16, 'white')                    # lower rail
    g.glowline(15, 13, 30, 'purple')
    g.glowline(16, 12, 29, 'purple')
    g.px(17, 14, GLOW['purple']).px(23, 14, GLOW['purple'])
    g.rect(6, 7, 12, 10, 'purple')                     # battery
    g.px(8, 8, GLOW['purple']).px(10, 8, GLOW['white'])
    grip(g, 6, 17, 6, 'navy', 1)

@gun
def arrow_launcher(g):
    g.rect(3, 10, 26, 16, 'olive')                     # tube
    g.rect(3, 10, 4, 16, 'black'); g.rect(25, 9, 27, 17, 'black')
    g.rect(9, 10, 10, 16, 'tan'); g.rect(19, 10, 20, 16, 'tan')   # bands
    g.rect(13, 7, 16, 9, 'black')                      # sight
    g.px(13, 8, GLOW['red'])
    g.rect(28, 11, 29, 15, 'red'); g.px(30, 13, MAT['red'][3]); g.px(28, 12, MAT['red'][3])   # warhead
    grip(g, 10, 17, 5, 'black', 1)
    g.rect(17, 17, 18, 20, 'black')

@gun
def void_arrow_launcher(g):
    g.rect(3, 10, 24, 16, 'black')
    g.rect(6, 11, 20, 11, 'purple')
    g.glowline(6, 13, 20, 'purple')
    g.rect(11, 7, 15, 9, 'purple'); g.px(13, 8, GLOW['white'])
    g.rect(24, 9, 26, 17, 'purple')
    for (x, y) in ((28, 12), (28, 13), (28, 14), (29, 12), (29, 13), (29, 14), (27, 13), (30, 13)): g.px(x, y, (30, 0, 50))
    g.px(28, 13, GLOW['purple']).px(31, 11, GLOW['purple']).px(30, 16, GLOW['purple'])
    grip(g, 9, 17, 6, 'black', 1)

@gun
def homing_arrow_gun(g):
    g.rect(6, 10, 22, 15, 'white')
    g.rect(22, 11, 27, 13, 'steel')
    g.rect(12, 4, 12, 9, 'steel')                      # antenna
    g.rect(10, 3, 14, 4, 'white'); g.px(12, 2, GLOW['cyan'])   # dish
    g.glowline(8, 12, 20, 'cyan')
    g.rect(16, 7, 20, 9, 'navy'); g.px(18, 8, GLOW['red'])     # target screen
    grip(g, 8, 16, 6, 'navy', 1)
    arrow(g, 27, 12, 'steel')
    g.px(31, 12, GLOW['cyan'])

@gun
def apocalypse_cannon(g):
    g.rect(1, 8, 22, 19, 'black')
    g.rect(1, 8, 22, 9, 'red'); g.rect(1, 18, 22, 19, 'red')
    for y in (10, 13, 16): g.rect(22, y, 30, y + 1, 'gunmetal')   # three barrels
    for y in (10, 13, 16): g.px(30, y, (20, 20, 20))
    g.rect(26, 9, 27, 18, 'red')
    # skull emblem
    sk = ["0111110", "1111111", "1001001", "1111111", "0110110", "0101010"]
    for j, row in enumerate(sk):
        for i, ch in enumerate(row):
            if ch == '1': g.px(8 + i, 10 + j, (235, 230, 220))
    g.rect(4, 20, 8, 25, 'black'); g.rect(5, 20, 7, 24, 'rubber')
    g.rect(14, 20, 16, 23, 'gunmetal')


# ---------------------------------------------------------------- 1.5 armoury: pistols
def pistol_base(g, slide='gunmetal', frame='black', gripm='black', x=7, length=14, barrel=3):
    g.rect(x, 10, x + length, 13, slide)
    for xx in range(x + 1, x + 6, 2): g.px(xx, 11, MAT[slide][0])          # rear serrations
    g.px(x + length - 3, 12, MAT[slide][0])                                 # ejection port
    g.rect(x + 2, 14, x + length - 2, 15, frame)
    g.rect(x + length - 5, 14, x + length - 2, 14, MAT and frame)            # dust cover rail
    g.rect(x + length + 1, 11, x + length + barrel, 12, 'steel')
    grip(g, x + 1, 15, 7, gripm, 2)
    guard(g, x + 6, 15)
    sights(g, x + 1, x + length - 1, 9)

@gun
def service_pistol(g):
    pistol_base(g)
    g.px(9, 18, MAT['black'][3]).px(9, 20, MAT['black'][3])

@gun
def compact_pistol(g):
    pistol_base(g, 'steel', 'black', 'rubber', x=9, length=10, barrel=2)

@gun
def tactical_pistol(g):
    pistol_base(g, 'tan', 'black', 'tan', x=6, length=15, barrel=3)
    for xx in range(14, 19, 2): g.px(xx, 15, MAT['black'][3])               # rail notches
    g.rect(23, 10, 26, 13, 'black'); g.px(26, 11, MAT['black'][3])          # compensator

@gun
def hand_cannon(g):
    g.rect(5, 9, 24, 13, 'steel')                                           # big slab slide
    g.rect(5, 9, 24, 9, 'white')
    for xx in range(6, 11, 2): g.rect(xx, 10, xx, 12, MAT['steel'][0] and 'gunmetal')
    g.rect(15, 11, 18, 11, 'gunmetal')
    g.rect(24, 10, 27, 12, 'gunmetal'); g.px(27, 11, (14, 14, 16))
    g.rect(7, 14, 21, 15, 'steel')
    grip(g, 7, 16, 8, 'darkwood', 2)
    g.px(7, 19, MAT['brass'][2]).px(6, 22, MAT['brass'][2])
    guard(g, 13, 16)
    sights(g, 6, 23, 8)

@gun
def revolver(g):
    g.rect(14, 10, 27, 11, 'steel')                                         # barrel
    g.rect(14, 12, 25, 12, 'gunmetal')                                      # ejector rod
    g.px(26, 9, MAT['black'][1])
    g.rect(8, 9, 14, 14, 'gunmetal')                                        # frame
    g.rect(10, 10, 14, 14, 'steel')                                         # cylinder
    for xx in (11, 13): g.rect(xx, 10, xx, 14, MAT['steel'][0] and 'gunmetal')
    g.px(7, 9, MAT['black'][1]).px(6, 10, MAT['black'][1])                  # hammer
    g.poly([(5, 14), (10, 14), (9, 21), (4, 22), (3, 20)], 'wood')
    g.px(6, 17, MAT['brass'][3])
    guard(g, 10, 15)

@gun
def magnum_revolver(g):
    revolver(g)
    g.rect(14, 9, 29, 12, 'steel'); g.rect(14, 9, 29, 9, 'black')          # vent rib + full lug
    for xx in range(16, 29, 3): g.px(xx, 9, MAT['black'][3])
    g.px(29, 10, (14, 14, 16))
    g.poly([(5, 14), (10, 14), (9, 21), (4, 22), (3, 20)], 'rubber')
    for yy in (16, 18, 20): g.px(5, yy, MAT['rubber'][0])

@gun
def machine_pistol(g):
    pistol_base(g, 'black', 'black', 'black', x=7, length=13, barrel=2)
    g.rect(13, 16, 15, 25, 'gunmetal')                                      # long stick mag
    g.rect(13, 25, 16, 25, 'black'); g.px(14, 22, MAT['steel'][2])
    g.rect(1, 12, 6, 12, 'steel'); g.rect(1, 12, 1, 15, 'steel')            # folding wire stock
    g.px(20, 15, GLOW['red'])                                               # select-fire dot

@gun
def golden_pistol(g):
    hand_cannon(g)
    g.rect(5, 9, 24, 13, 'brass'); g.rect(7, 14, 21, 15, 'brass')
    for xx in range(6, 11, 2): g.rect(xx, 10, xx, 12, 'copper')
    g.rect(24, 10, 27, 12, 'brass'); g.px(27, 11, (40, 24, 6))
    for (x, y) in ((14, 11), (17, 11), (20, 11)): g.px(x, y, GLOW['white'])  # engraving glints
    grip(g, 7, 16, 8, 'black', 2)
    g.px(7, 19, GLOW['gold']).px(6, 22, GLOW['gold'])

@gun
def flare_pistol(g):
    g.rect(9, 10, 18, 14, 'red')
    g.rect(18, 9, 27, 14, 'red'); g.rect(26, 9, 27, 14, 'black')            # fat barrel
    g.rect(19, 10, 25, 10, MAT['red'][3] and 'red')
    g.px(27, 11, GLOW['orange']).px(27, 12, GLOW['gold']).px(28, 11, GLOW['orange']).px(29, 12, (255, 220, 120))
    grip(g, 10, 15, 7, 'black', 2)
    guard(g, 14, 15)
    g.px(8, 10, MAT['black'][1])

# ---------------------------------------------------------------- AK family ("keleş")
def ak(g, wood='wood', metal='gunmetal', stock='full', mag='curved', barrel_end=29, hand='wood'):
    if stock == 'full':
        g.poly([(0, 13), (8, 11), (8, 15), (3, 18), (0, 19)], wood)
        g.rect(0, 13, 0, 19, 'rubber')
    elif stock == 'fold':
        g.line(1, 11, 8, 11, MAT['steel'][1]).line(1, 16, 8, 14, MAT['steel'][1]).line(1, 11, 1, 16, MAT['steel'][1])
    elif stock == 'poly':
        g.poly([(0, 12), (8, 11), (8, 15), (4, 16), (0, 18)], 'black')
        g.rect(2, 13, 5, 14, 'rubber'); g.rect(0, 12, 0, 18, 'rubber')
    g.rect(8, 10, 17, 14, metal)                                            # receiver
    g.rect(8, 9, 16, 9, metal)                                              # dust cover
    g.px(10, 12, MAT[metal][0]).px(15, 12, MAT[metal][3])                   # selector & rivet
    g.line(9, 11, 13, 11, MAT[metal][0])
    g.rect(17, 11, 23, 14, hand)                                            # lower handguard
    g.rect(17, 9, 23, 10, hand)                                             # upper handguard/gas tube
    g.rect(23, 9, 24, 9, metal)
    g.rect(23, 11, barrel_end, 12, 'steel')                                 # barrel
    g.px(barrel_end - 3, 10, MAT['black'][1]).px(barrel_end - 3, 9, MAT['black'][1])   # front sight post
    g.rect(barrel_end - 1, 11, barrel_end, 12, 'black')                     # slant brake
    g.px(14, 8, MAT['black'][1])                                            # rear sight
    if mag == 'curved':
        g.poly([(13, 15), (17, 15), (20, 22), (16, 23)], 'gunmetal' if metal != 'gunmetal' else 'black')
        for t in (17, 19, 21): g.px(13 + (t - 15) // 2 + 2, t, MAT['black'][3])
    elif mag == 'drum':
        g.rect(13, 15, 16, 16, 'black')
        g.poly([(11, 17), (18, 17), (19, 20), (18, 23), (11, 23), (10, 20)], 'black')
        g.rect(13, 19, 16, 21, 'gunmetal'); g.px(14, 20, MAT['steel'][3])
    grip(g, 9, 15, 5, wood if stock != 'poly' else 'black', 1)
    guard(g, 11, 15)

@gun
def ak47(g):
    ak(g)

@gun
def akm_classic(g):
    ak(g, 'darkwood', 'black', hand='darkwood')
    for xx in (18, 20, 22): g.px(xx, 13, MAT['darkwood'][0])                # finger grooves

@gun
def ak74(g):
    ak(g, 'wood', 'gunmetal', stock='poly', hand='red' if False else 'black')
    g.poly([(13, 15), (17, 15), (20, 22), (16, 23)], 'copper')              # plum polymer mag
    g.rect(28, 10, 30, 13, 'black'); g.px(29, 10, MAT['black'][3])         # chamber brake

@gun
def aks74u(g):
    ak(g, stock='fold', barrel_end=25, hand='wood')
    g.rect(23, 10, 26, 13, 'black'); g.px(26, 11, (14, 14, 16))            # booster
    g.rect(17, 9, 21, 10, 'wood')

@gun
def ak_tactical(g):
    ak(g, stock='poly', hand='black', metal='black')
    g.rect(16, 7, 23, 8, 'gunmetal')                                        # top rail
    for xx in range(16, 24, 2): g.px(xx, 7, MAT['gunmetal'][3])
    g.poly([(13, 15), (17, 15), (20, 22), (16, 23)], 'tan')
    g.rect(19, 15, 20, 18, 'black')                                         # vertical grip

@gun
def rpk(g):
    ak(g, barrel_end=30, mag='drum')
    g.rect(23, 11, 30, 12, 'steel')
    g.line(24, 13, 22, 19, MAT['steel'][1]).line(25, 13, 27, 19, MAT['steel'][1])   # bipod
    g.poly([(0, 12), (8, 11), (8, 15), (4, 16), (2, 19), (0, 19)], 'wood')         # club-foot stock

@gun
def golden_ak(g):
    ak(g, 'darkwood', 'brass', hand='darkwood')
    g.poly([(13, 15), (17, 15), (20, 22), (16, 23)], 'brass')
    for (x, y) in ((10, 11), (12, 13), (15, 10), (19, 22)): g.px(x, y, GLOW['white'])
    g.rect(28, 11, 29, 12, 'brass')

@gun
def ak12(g):
    ak(g, stock='poly', hand='black', metal='black')
    g.rect(8, 8, 23, 8, 'gunmetal')                                         # full-length rail
    for xx in range(8, 24, 2): g.px(xx, 8, MAT['gunmetal'][3])
    g.rect(27, 10, 30, 13, 'gunmetal'); g.px(30, 11, (14, 14, 16))
    g.poly([(13, 15), (17, 15), (20, 22), (16, 23)], 'olive')

@gun
def ak_grenadier(g):
    ak(g)
    g.rect(17, 14, 26, 16, 'gunmetal')                                      # underbarrel launcher
    g.rect(25, 14, 26, 16, 'black'); g.px(26, 15, (14, 14, 16))
    g.rect(19, 17, 20, 19, 'black')
    g.px(24, 15, MAT['red'][2])

# ---------------------------------------------------------------- sniper rifles
def bolt(g, x, y, mat='steel'):
    g.px(x, y, MAT[mat][2]).px(x, y + 1, MAT[mat][1]).px(x - 1, y + 2, MAT['black'][1])

def long_scope(g, x0, x1, y, lens='cyan'):
    g.rect(x0 + 2, y, x1 - 2, y + 2, 'black')
    g.rect(x0, y - 1, x0 + 2, y + 3, 'black'); g.rect(x1 - 2, y - 1, x1, y + 3, 'black')
    g.rect((x0 + x1) // 2, y - 2, (x0 + x1) // 2 + 1, y - 1, 'gunmetal')
    g.px(x1, y + 1, GLOW[lens]).px(x1, y, (170, 240, 255)).px(x0, y + 1, (70, 130, 170))
    g.rect(x0 + 3, y + 3, x0 + 4, y + 4, 'gunmetal'); g.rect(x1 - 4, y + 3, x1 - 3, y + 4, 'gunmetal')

@gun
def bolt_sniper(g):
    g.poly([(0, 13), (10, 13), (12, 16), (6, 17), (2, 18), (0, 20)], 'darkwood')
    g.rect(0, 13, 0, 20, 'rubber')
    g.rect(10, 12, 18, 15, 'darkwood')
    g.rect(10, 11, 16, 12, 'steel')
    bolt(g, 13, 13)
    g.rect(18, 12, 31, 13, 'steel')
    long_scope(g, 7, 20, 7)
    grip(g, 9, 16, 4, 'darkwood', 1)
    guard(g, 12, 16)

@gun
def hunting_rifle(g):
    g.poly([(0, 14), (9, 13), (11, 16), (5, 17), (0, 20)], 'wood')
    g.rect(9, 12, 22, 15, 'wood')
    g.rect(10, 11, 17, 12, 'gunmetal')
    bolt(g, 14, 13, 'gunmetal')
    g.rect(22, 12, 30, 13, 'gunmetal')
    g.px(29, 11, MAT['brass'][2])
    long_scope(g, 9, 19, 8, 'gold')
    for xx in (19, 21): g.px(xx, 14, MAT['wood'][0])
    g.px(3, 16, MAT['tan'][2]).px(26, 14, MAT['tan'][2])                    # sling swivels
    grip(g, 9, 16, 3, 'wood', 1)

@gun
def marksman_rifle(g):
    g.poly([(0, 12), (8, 11), (8, 16), (4, 16), (0, 19)], 'black')
    g.rect(2, 12, 6, 13, 'rubber')
    g.rect(8, 10, 18, 14, 'tan')
    g.rect(18, 10, 25, 14, 'tan')
    for xx in range(19, 25, 2): g.px(xx, 12, MAT['tan'][0])
    g.rect(25, 11, 30, 12, 'black'); g.rect(29, 10, 31, 13, 'black')
    g.rect(8, 8, 24, 8, 'black')
    long_scope(g, 9, 19, 4)
    g.poly([(12, 15), (15, 15), (16, 20), (13, 20)], 'black')
    grip(g, 9, 15, 5, 'black', 1)

@gun
def anti_materiel_rifle(g):
    g.poly([(0, 11), (7, 11), (7, 17), (3, 17), (0, 20)], 'black')
    g.rect(0, 11, 0, 20, 'rubber'); g.rect(2, 13, 5, 14, 'gunmetal')
    g.rect(7, 10, 19, 15, 'gunmetal')
    g.rect(7, 9, 19, 9, 'black')
    g.rect(19, 11, 28, 13, 'steel')
    g.rect(27, 9, 31, 15, 'black')                                          # huge brake
    for yy in (10, 12, 14): g.px(29, yy, (14, 14, 16))
    long_scope(g, 6, 19, 4, 'red')
    g.rect(12, 16, 16, 21, 'black'); g.px(14, 19, MAT['black'][3])         # box mag
    g.line(22, 14, 19, 22, MAT['steel'][1]).line(23, 14, 26, 22, MAT['steel'][1])
    grip(g, 8, 16, 5, 'black', 1)

@gun
def ghillie_sniper(g):
    bolt_sniper(g)
    leaf = [MAT['olive'][1], MAT['olive'][2], (70, 96, 40), (96, 84, 46)]
    for i, (x, y) in enumerate(((2, 12), (4, 12), (6, 11), (9, 11), (12, 10), (15, 10), (18, 11), (21, 11), (24, 11), (3, 19), (7, 17), (1, 15), (11, 16), (26, 14), (22, 14))):
        g.px(x, y, leaf[i % 4]); g.px(x + 1, y + (i % 2), leaf[(i + 1) % 4])

@gun
def silent_sniper(g):
    g.poly([(0, 12), (8, 11), (8, 16), (4, 16), (0, 19)], 'navy')
    g.rect(8, 10, 17, 15, 'navy')
    bolt(g, 12, 12)
    g.rect(17, 11, 21, 13, 'steel')
    g.rect(21, 10, 31, 14, 'black')                                         # integral suppressor
    for xx in range(22, 31, 2): g.px(xx, 10, MAT['black'][3])
    long_scope(g, 7, 18, 5, 'ice')
    grip(g, 9, 16, 5, 'navy', 1)
    g.rect(12, 16, 14, 19, 'black')

@gun
def lever_rifle(g):
    g.poly([(0, 14), (8, 12), (9, 16), (4, 17), (0, 20)], 'wood')
    g.rect(8, 11, 14, 15, 'brass')                                          # brass receiver
    g.rect(14, 11, 30, 12, 'gunmetal')
    g.rect(14, 13, 27, 14, 'gunmetal')                                      # tube mag
    g.rect(15, 13, 22, 15, 'wood')
    g.px(29, 10, MAT['brass'][2]).px(12, 10, MAT['black'][1])
    g.line(9, 16, 9, 19, MAT['gunmetal'][1]).line(9, 19, 13, 19, MAT['gunmetal'][1]).line(13, 16, 13, 19, MAT['gunmetal'][1])   # lever loop
    g.px(11, 17, MAT['steel'][2])

@gun
def longshot_rifle(g):
    g.poly([(0, 12), (9, 12), (9, 17), (4, 17), (0, 20)], 'white')
    g.rect(3, 11, 7, 12, 'white')
    g.rect(9, 11, 18, 16, 'white')
    g.glowline(10, 14, 17, 'cyan')
    g.rect(18, 12, 31, 13, 'steel')
    for xx in range(20, 30, 3): g.rect(xx, 11, xx, 14, 'navy')              # cooling rings
    long_scope(g, 7, 22, 6, 'cyan')
    grip(g, 10, 17, 5, 'navy', 1)
    g.rect(14, 17, 16, 20, 'navy'); g.px(15, 18, GLOW['cyan'])

# ---------------------------------------------------------------- extras
@gun
def survivor_smg(g):
    g.line(1, 12, 6, 12, MAT['steel'][1]).line(1, 12, 1, 16, MAT['steel'][1]).line(1, 16, 6, 15, MAT['steel'][1])
    g.rect(6, 10, 19, 14, 'gunmetal')
    g.rect(19, 10, 25, 13, 'black')
    for xx in range(20, 25, 2): g.px(xx, 10, MAT['black'][3])
    g.rect(25, 11, 27, 12, 'steel')
    g.rect(13, 15, 15, 23, 'black'); g.px(14, 21, MAT['black'][3])
    g.rect(6, 15, 9, 15, 'tan')                                             # duct tape repair
    grip(g, 8, 15, 6, 'rubber', 1)
    guard(g, 10, 15)


if __name__ == '__main__':
    for name, f in D.items():
        g = G(); f(g); g.render().save(f'{OUT}/{name}.png')
    print(len(D), 'guns drawn')
