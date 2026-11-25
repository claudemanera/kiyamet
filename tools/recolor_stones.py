"""Build step: takes the amethyst shard texture from the player's own Minecraft
client jar and recolours it into the 10 power stones. The vanilla texture is
never stored in this repository.
Usage: python3 tools/recolor_stones.py <minecraft-client.jar>"""
import sys, zipfile, io, colorsys
from PIL import Image

OUT = 'src/main/resources/assets/kiyamet/textures/item/'
# id: (hue degrees, saturation multiplier, brightness multiplier)
STONES = {
    'fire_stone':   (18, 1.25, 1.10), 'frost_stone':  (195, 0.95, 1.20),
    'storm_stone':  (52, 1.20, 1.25), 'nature_stone': (140, 1.05, 1.00),
    'blood_stone':  (352, 1.30, 0.85), 'shadow_stone': (265, 0.55, 0.55),
    'soul_stone':   (230, 0.18, 1.30), 'time_stone':   (325, 1.10, 1.15),
    'void_stone':   (275, 1.35, 1.00), 'earth_stone':  (28, 0.70, 0.80),
}

def main(jar):
    with zipfile.ZipFile(jar) as z:
        src = Image.open(io.BytesIO(z.read('assets/minecraft/textures/item/amethyst_shard.png'))).convert('RGBA')
    for sid, (hue, sm, vm) in STONES.items():
        img = src.copy(); px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = px[x, y]
                if a == 0: continue
                h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                r2, g2, b2 = colorsys.hsv_to_rgb(hue / 360, min(1, s * sm), min(1, v * vm))
                px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
        img.save(OUT + sid + '.png')
    print('10 stones recoloured')

if __name__ == '__main__':
    main(sys.argv[1])
