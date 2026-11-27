"""Draws the ten original power stones as original pixel art (16x16 cut gems, each with its own symbol inside).
Replaces the old build step that recoloured a vanilla texture: nothing here comes from Minecraft's files."""
import os
from PIL import Image, ImageDraw
OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/kiyamet/textures/item/')


def light(c, k): return tuple(int(v + (255 - v) * k) for v in c)
def dark(c, k): return tuple(int(v * k) for v in c)


SHAPES = {   # outline of the cut stone
    'hex':   [(8, 1), (13, 4), (14, 10), (8, 15), (2, 10), (3, 4)],
    'tall':  [(8, 0), (12, 5), (11, 12), (8, 15), (5, 12), (4, 5)],
    'round': [(5, 2), (11, 2), (14, 5), (14, 11), (11, 14), (5, 14), (2, 11), (2, 5)],
    'shard': [(9, 0), (14, 6), (11, 15), (4, 13), (2, 6)],
    'drop':  [(8, 0), (12, 6), (13, 10), (11, 14), (5, 14), (3, 10), (4, 6)],
}
SYMBOLS = {
    'flame':  [(8, 5), (7, 6), (8, 6), (7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9), (9, 9), (8, 10)],
    'flake':  [(8, 4), (8, 5), (8, 6), (8, 7), (8, 8), (8, 9), (8, 10), (5, 7), (6, 7), (7, 7), (9, 7), (10, 7), (11, 7), (6, 5), (10, 5), (6, 9), (10, 9)],
    'bolt':   [(9, 3), (8, 4), (8, 5), (7, 6), (7, 7), (8, 7), (9, 7), (9, 8), (8, 9), (8, 10), (7, 11)],
    'leaf':   [(9, 4), (10, 4), (8, 5), (9, 5), (10, 5), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (6, 8), (7, 8), (6, 9), (5, 10)],
    'drop':   [(8, 4), (8, 5), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8), (9, 8), (8, 9)],
    'moon':   [(7, 4), (6, 5), (5, 6), (5, 7), (5, 8), (5, 9), (6, 10), (7, 11), (8, 11), (6, 6), (6, 9)],
    'spiral': [(8, 5), (9, 5), (10, 6), (10, 7), (10, 8), (9, 9), (8, 9), (7, 8), (7, 7), (8, 7), (6, 6), (6, 9), (6, 10), (7, 11)],
    'glass':  [(6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (7, 5), (9, 5), (8, 6), (8, 7), (8, 8), (7, 9), (9, 9), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10)],
    'ring':   [(7, 5), (8, 5), (9, 5), (6, 6), (10, 6), (6, 7), (10, 7), (6, 8), (10, 8), (7, 9), (8, 9), (9, 9)],
    'peak':   [(8, 5), (7, 6), (9, 6), (6, 7), (10, 7), (5, 8), (11, 8), (8, 7), (8, 8), (7, 8), (9, 8), (4, 9), (12, 9)],
}
STONES = {   # id: shape, symbol, body colour, symbol colour
    'fire_stone':   ('hex', 'flame', (220, 70, 30), (255, 220, 90)),
    'frost_stone':  ('shard', 'flake', (90, 180, 240), (240, 252, 255)),
    'storm_stone':  ('tall', 'bolt', (230, 200, 50), (255, 255, 220)),
    'nature_stone': ('drop', 'leaf', (60, 170, 70), (200, 255, 150)),
    'blood_stone':  ('round', 'drop', (150, 20, 40), (255, 90, 100)),
    'shadow_stone': ('shard', 'moon', (60, 40, 90), (180, 140, 255)),
    'soul_stone':   ('round', 'spiral', (190, 205, 230), (100, 230, 255)),
    'time_stone':   ('tall', 'glass', (220, 80, 170), (255, 220, 120)),
    'void_stone':   ('hex', 'ring', (90, 30, 150), (10, 0, 20)),
    'earth_stone':  ('round', 'peak', (140, 100, 60), (120, 200, 90)),
}


def stone(shape, sym, col, scol):
    im = Image.new('RGBA', (16, 16))
    dr = ImageDraw.Draw(im)
    pts = SHAPES[shape]
    dr.polygon(pts, fill=col + (255,))
    # facets: light upper left, dark lower right
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y))
            if not p[3]: continue
            k = (x + y) / 30
            c = light(col, 0.35 * (1 - k * 1.6)) if k < 0.5 else dark(col, 1 - (k - 0.5) * 0.6)
            if (x - y) % 7 == 0: c = light(c, 0.12)
            im.putpixel((x, y), c + (255,))
    for (x, y) in SYMBOLS[sym]:
        if im.getpixel((x, y))[3]: im.putpixel((x, y), scol + (255,))
    # rim and highlight
    out = im.copy()
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3]: continue
            if any(0 <= x + dx < 16 and 0 <= y + dy < 16 and im.getpixel((x + dx, y + dy))[3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), dark(col, 0.3) + (255,))
    xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
    hx, hy = min(xs) + 2, min(ys) + 2
    for (x, y) in ((hx, hy), (hx + 1, hy), (hx, hy + 1)):
        if im.getpixel((x, y))[3]: out.putpixel((x, y), (255, 255, 255, 230))
    return out


if __name__ == '__main__':
    for sid, (sh, sy, c, sc) in STONES.items(): stone(sh, sy, c, sc).save(OUT + sid + '.png')
    print('10 stones drawn')
