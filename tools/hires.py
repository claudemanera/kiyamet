"""Helpers for 4x-resolution GeckoLib textures: find a cube's face on the box-UV sheet, upscale with fine grain, paste logos."""
import math, random
from PIL import Image


def face(b, idx, which, k=4):
    """pixel rect (x, y, w, h) of a face of cube #idx, at k-times resolution"""
    (u, v), (w, h, d), mat, _ = b.uvs[idx]
    W, H, D = max(1, math.ceil(w)), max(1, math.ceil(h)), max(1, math.ceil(d))
    r = {'top': (u + D, v, W, D), 'bottom': (u + D + W, v, W, D), 'right': (u, v + D, D, H), 'front': (u + D, v + D, W, H),
         'left': (u + D + W, v + D, D, H), 'back': (u + 2 * D + W, v + D, W, H)}[which]
    return tuple(x * k for x in r)


def upscale(path, k=4, grain=6, seed=1):
    im = Image.open(path).convert('RGBA')
    big = im.resize((im.width * k, im.height * k), Image.NEAREST)
    px = big.load(); r = random.Random(seed)
    for y in range(big.height):
        for x in range(big.width):
            p = px[x, y]
            if p[3] == 0: continue
            n = r.randint(-grain, grain)
            px[x, y] = (max(0, min(255, p[0] + n)), max(0, min(255, p[1] + n)), max(0, min(255, p[2] + n)), p[3])
    return big


def paste(img, rect, src, mirror=False):
    x, y, w, h = rect
    s = src.resize((w, h), Image.LANCZOS if src.width > w else Image.NEAREST)
    if mirror: s = s.transpose(Image.FLIP_LEFT_RIGHT)
    img.alpha_composite(s, (x, y))


def logo_on(img, b, idx, logo):
    """an emblem cube shows the logo on both sides; its rim gets the logo's edge colour"""
    for f in ('top', 'bottom', 'left', 'right', 'front', 'back'):          # clear: only the round badge shows
        x, y, w, h = face(b, idx, f)
        img.paste((0, 0, 0, 0), (x, y, x + w, y + h))
    paste(img, face(b, idx, 'front'), logo)
    paste(img, face(b, idx, 'back'), logo, mirror=True)
