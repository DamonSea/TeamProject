#!/usr/bin/env python3
"""Generates the Rat-Man app icon (packaging/ratman.png/.ico/.icns) from the rat sprite. Needs Pillow."""
import os
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
S = 1024

img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rounded_rectangle([40, 40, S - 40, S - 40], radius=200, fill=(255, 200, 45, 255), outline=(70, 45, 10, 255), width=36)
for (x, y, r) in [(220, 230, 70), (800, 260, 55), (250, 820, 60), (810, 800, 80), (560, 160, 40)]:
    d.ellipse([x - r, y - r, x + r, y + r], fill=(230, 165, 25, 255))

sheet = Image.open(os.path.join(ROOT, 'src/main/resources/images/Rat.png')).convert('RGBA')
frame = sheet.crop((0, 0, 64, 64))
rat = frame.crop(frame.getbbox())
scale = 560 // rat.width
rat = rat.resize((rat.width * scale, rat.height * scale), Image.NEAREST)
pad = 20
outline_alpha = Image.new('L', (rat.width + 2 * pad, rat.height + 2 * pad), 0)
outline_alpha.paste(rat.split()[3], (pad, pad))
outline_alpha = outline_alpha.filter(ImageFilter.MaxFilter(25))
art = Image.new('RGBA', outline_alpha.size, (40, 25, 5, 255))
art.putalpha(outline_alpha)
art.alpha_composite(rat, (pad, pad))
img.alpha_composite(art, ((S - art.width) // 2, (S - art.height) // 2 + 20))

img.save(os.path.join(HERE, 'ratman.png'))
img.save(os.path.join(HERE, 'ratman.ico'), sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
img.save(os.path.join(HERE, 'ratman.icns'))
print('wrote ratman.png/.ico/.icns')
