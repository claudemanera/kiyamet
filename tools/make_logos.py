"""TWD 2.1: the 14 element logos - round badges with each element's symbol. Saved at 32x32 (for models) and 128x128 (for the guide)."""
import math, os
from PIL import Image, ImageDraw
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet')
COL = {'flame': (255, 90, 30), 'lightning': (255, 226, 74), 'earth': (123, 162, 63), 'shadow': (107, 47, 181), 'smoke': (154, 154, 154), 'spirit': (127, 240, 232),
       'electric': (53, 200, 255), 'thunder': (77, 107, 255), 'ice': (168, 232, 255), 'fire': (255, 154, 26), 'water': (46, 123, 255), 'air': (232, 246, 255),
       'wind': (142, 240, 160), 'metal': (184, 194, 204)}
S = 256


def dk(c, k): return tuple(int(v * k) for v in c)
def lt(c, k): return tuple(int(v + (255 - v) * k) for v in c)


def badge(e):
    c = COL[e]
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.ellipse([6, 6, S - 6, S - 6], fill=dk(c, 0.25) + (255,))
    for r in range(110, 0, -2):           # radial gradient
        t = r / 110
        d.ellipse([S / 2 - r, S / 2 - r, S / 2 + r, S / 2 + r], fill=tuple(int(dk(c, 0.45)[i] * t + lt(c, 0.15)[i] * (1 - t)) for i in range(3)) + (255,))
    d.ellipse([14, 14, S - 14, S - 14], outline=lt(c, 0.6) + (255,), width=8)
    for k in range(12):                    # rim studs
        a = k * math.pi / 6; x, y = S / 2 + math.cos(a) * 118, S / 2 + math.sin(a) * 118
        d.ellipse([x - 6, y - 6, x + 6, y + 6], fill=lt(c, 0.7) + (255,))
    W = (255, 255, 255, 255); SH = dk(c, 0.2) + (255,)
    cx = cy = S / 2

    def poly(pts, fill=W, off=6):
        d.polygon([(x + off, y + off) for x, y in pts], fill=SH); d.polygon(pts, fill=fill)

    if e == 'flame':
        pts = [(cx, 40), (cx + 34, 100), (cx + 50, 150), (cx + 30, 200), (cx, 214), (cx - 30, 200), (cx - 50, 150), (cx - 30, 110), (cx - 18, 140), (cx - 8, 90)]
        poly(pts); poly([(cx, 120), (cx + 20, 160), (cx + 12, 196), (cx, 202), (cx - 12, 196), (cx - 20, 165)], fill=lt(c, 0.3) + (255,), off=0)
    elif e == 'fire':
        for k in range(8):
            a = k * math.pi / 4; poly([(cx + math.cos(a - 0.22) * 46, cy + math.sin(a - 0.22) * 46), (cx + math.cos(a) * 96, cy + math.sin(a) * 96), (cx + math.cos(a + 0.22) * 46, cy + math.sin(a + 0.22) * 46)])
        d.ellipse([cx - 44, cy - 44, cx + 44, cy + 44], fill=W); d.ellipse([cx - 26, cy - 26, cx + 26, cy + 26], fill=lt(c, 0.2) + (255,))
    elif e == 'lightning':
        poly([(cx + 20, 34), (cx - 40, 140), (cx - 2, 140), (cx - 22, 222), (cx + 46, 110), (cx + 6, 110), (cx + 34, 34)])
    elif e == 'electric':
        for a in (0, 60, 120):
            box = [cx - 90, cy - 34, cx + 90, cy + 34]
            ell = Image.new('RGBA', (S, S)); ImageDraw.Draw(ell).ellipse(box, outline=W, width=12)
            im.alpha_composite(ell.rotate(a, center=(cx, cy)))
        d.ellipse([cx - 22, cy - 22, cx + 22, cy + 22], fill=W)
    elif e == 'thunder':
        poly([(cx - 70, 120), (cx - 50, 86), (cx - 10, 76), (cx + 20, 60), (cx + 60, 80), (cx + 76, 116), (cx + 56, 134), (cx - 60, 134)])
        poly([(cx + 10, 130), (cx - 26, 186), (cx, 186), (cx - 14, 230), (cx + 34, 170), (cx + 8, 170), (cx + 28, 130)], fill=lt(c, 0.5) + (255,))
    elif e == 'earth':
        poly([(cx - 90, 186), (cx - 30, 80), (cx, 120), (cx + 30, 64), (cx + 92, 186)])
        poly([(cx + 30, 64), (cx + 48, 96), (cx + 14, 96)], fill=lt(c, 0.6) + (255,), off=0)
        d.ellipse([cx - 14, 186, cx + 14, 214], fill=lt(c, 0.4) + (255,))
    elif e == 'shadow':
        d.ellipse([cx - 80, cy - 80, cx + 80, cy + 80], fill=W); d.ellipse([cx - 50, cy - 96, cx + 106, cy + 60], fill=dk(c, 0.45) + (255,))
        d.ellipse([cx - 40, cy - 12, cx - 12, cy + 16], fill=lt(c, 0.4) + (255,))
    elif e == 'smoke':
        for k, (r, w) in enumerate(((84, 14), (58, 14), (32, 14))):
            d.arc([cx - r, cy - r, cx + r, cy + r], start=200 - k * 40, end=520 - k * 40, fill=W, width=w)
        d.ellipse([cx - 10, cy - 10, cx + 10, cy + 10], fill=W)
    elif e == 'spirit':
        poly([(cx, 46), (cx + 30, 110), (cx + 44, 160), (cx + 24, 200), (cx, 208), (cx - 24, 200), (cx - 44, 160), (cx - 30, 110)])
        d.ellipse([cx - 22, 130, cx - 6, 150], fill=dk(c, 0.4) + (255,)); d.ellipse([cx + 6, 130, cx + 22, 150], fill=dk(c, 0.4) + (255,))
        d.ellipse([cx - 60, 30, cx + 60, 62], outline=W, width=8)
    elif e == 'ice':
        for k in range(6):
            a = k * math.pi / 3; x1, y1 = cx + math.cos(a) * 92, cy + math.sin(a) * 92
            d.line([(cx, cy), (x1, y1)], fill=W, width=14)
            for t, L in ((0.5, 30), (0.75, 22)):
                px, py = cx + math.cos(a) * 92 * t, cy + math.sin(a) * 92 * t
                for s in (-1, 1): d.line([(px, py), (px + math.cos(a + s * 0.8) * L, py + math.sin(a + s * 0.8) * L)], fill=W, width=10)
        d.regular_polygon((cx, cy, 20), 6, fill=lt(c, 0.5) + (255,))
    elif e == 'water':
        poly([(cx, 40), (cx + 30, 100), (cx + 56, 150), (cx + 50, 190), (cx, 220), (cx - 50, 190), (cx - 56, 150), (cx - 30, 100)])
        d.arc([cx - 34, 130, cx + 20, 196], start=100, end=200, fill=lt(c, 0.3) + (255,), width=10)
    elif e == 'air':
        for k, (y, L) in enumerate(((90, 140), (128, 170), (166, 120))):
            x0 = cx - L / 2
            d.line([(x0, y), (x0 + L - 20, y)], fill=W, width=14)
            d.arc([x0 + L - 46, y - 30, x0 + L + 4, y + 2], start=300, end=180 + 270, fill=W, width=14)
    elif e == 'wind':
        for k in range(5):
            w = 100 - k * 18; y = 60 + k * 32
            d.arc([cx - w + k * 6, y - 14, cx + w + k * 6, y + 18], start=0, end=180, fill=W, width=12)
        d.line([(cx + 30, 200), (cx + 16, 222)], fill=W, width=12)
    elif e == 'metal':
        for k in range(10):
            a = k * math.pi / 5; poly([(cx + math.cos(a - 0.2) * 70, cy + math.sin(a - 0.2) * 70), (cx + math.cos(a - 0.14) * 96, cy + math.sin(a - 0.14) * 96),
                                       (cx + math.cos(a + 0.14) * 96, cy + math.sin(a + 0.14) * 96), (cx + math.cos(a + 0.2) * 70, cy + math.sin(a + 0.2) * 70)], off=0)
        d.ellipse([cx - 74, cy - 74, cx + 74, cy + 74], fill=W); d.ellipse([cx - 30, cy - 30, cx + 30, cy + 30], fill=dk(c, 0.45) + (255,))
    return im


def save_all():
    os.makedirs(f'{A}/textures/gui/logo', exist_ok=True)
    out = {}
    for e in COL:
        big = badge(e)
        big.resize((128, 128), Image.LANCZOS).save(f'{A}/textures/gui/logo/{e}.png')
        small = big.resize((32, 32), Image.LANCZOS)
        px = small.load()
        for y in range(32):
            for x in range(32):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, 255 if a > 110 else 0)
        out[e] = small
    return out


LOGOS = save_all()
if __name__ == '__main__':
    sheet = Image.new('RGBA', (7 * 136, 2 * 136))
    for i, e in enumerate(COL):
        t = Image.open(f'{A}/textures/gui/logo/{e}.png'); sheet.paste(t, ((i % 7) * 136 + 4, (i // 7) * 136 + 4), t)
    sheet.save('/tmp/prev/logos.png'); print('logos ok')
