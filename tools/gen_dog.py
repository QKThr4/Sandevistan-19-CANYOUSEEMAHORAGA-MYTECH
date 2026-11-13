#!/usr/bin/env python3
"""Textura do Cao Robo (64x64, layout do DogModel)."""
import os, random
from PIL import Image, ImageDraw
random.seed(9)
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "sandevistan", "textures", "entity", "robo_cao.png")
im = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); p = im.load(); d = ImageDraw.Draw(im)

def plate(x0, y0, x1, y1, base, n=12):
    for x in range(x0, x1):
        for y in range(y0, y1):
            v = random.randint(-n, n)
            p[x, y] = tuple(max(0, min(255, c + v)) for c in base) + (255,)

plate(0, 0, 36, 18, (108, 114, 122))      # corpo
plate(0, 20, 20, 30, (98, 104, 114))      # cabeca
plate(24, 20, 36, 25, (84, 90, 100))      # focinho
plate(40, 20, 44, 23, (60, 66, 76))       # antenas
plate(0, 32, 8, 40, (70, 76, 86), 8)      # pernas
plate(24, 32, 36, 38, (60, 66, 76), 8)    # rabo
dark = (58, 62, 72, 255)
for y in (6, 12):
    d.line([(0, y), (35, y)], fill=dark)
for x in (6, 18, 24):
    d.line([(x, 0), (x, 17)], fill=dark)
d.rectangle([12, 2, 17, 4], fill=(255, 150, 30, 255))   # luz no lombo
d.line([(0, 36), (7, 36)], fill=(40, 44, 52, 255))      # articulacao da perna
# rosto: frente da cabeca em (5..9, 25..29)
d.rectangle([5, 25, 9, 29], fill=(80, 86, 96, 255))
d.rectangle([5, 26, 6, 27], fill=(255, 170, 30, 255))
d.rectangle([8, 26, 9, 27], fill=(255, 170, 30, 255))
d.line([(5, 29), (9, 29)], fill=(30, 34, 40, 255))
# frente do focinho (27..29, 23..24) com sensor
d.rectangle([27, 23, 29, 24], fill=(30, 34, 40, 255)); d.point((28, 23), fill=(255, 60, 40, 255))
im.save(OUT)
