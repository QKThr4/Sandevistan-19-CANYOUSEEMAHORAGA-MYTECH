#!/usr/bin/env python3
"""v5.0.0 etapa 5: icones dos implantes extras e tag de minerios."""
import json, os
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(BASE, "assets", "sandevistan")


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def outline(im, col=(18, 18, 26, 255)):
    sp, p = im.copy().load(), im.load()
    for x in range(16):
        for y in range(16):
            if sp[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and sp[nx, ny][3] > 0:
                        p[x, y] = col
                        break
    return im


def pips(im, tier):
    d = ImageDraw.Draw(im)
    for k in range(tier):
        d.point((2 + k * 2, 14), fill=(255, 230, 90, 255))
    return im


def boot():
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([4, 3, 8, 11], fill=(110, 116, 128, 255)); d.rectangle([4, 10, 13, 13], fill=(80, 86, 98, 255))
    d.rectangle([4, 12, 13, 13], fill=(50, 54, 64, 255)); d.polygon([(2, 8), (4, 6), (4, 11)], fill=(120, 230, 255, 255))
    d.polygon([(1, 11), (4, 10), (4, 13)], fill=(255, 190, 60, 255))
    return outline(im)


def jet():
    im = new(); d = ImageDraw.Draw(im)
    for x in (3, 9):
        d.rectangle([x, 2, x + 3, 11], fill=(120, 126, 138, 255)); d.rectangle([x, 4, x + 3, 5], fill=(60, 66, 78, 255))
        d.polygon([(x, 12), (x + 3, 12), (x + 2, 15), (x + 1, 15)], fill=(255, 170, 40, 255)); d.point((x + 1, 13), fill=(255, 240, 150, 255))
    d.rectangle([7, 5, 8, 7], fill=(60, 66, 78, 255))
    return outline(im)


def xp(tier):
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([3, 3, 12, 12], fill=(30, 60, 36, 255)); d.ellipse([5, 5, 10, 10], fill=(110, 255, 70, 255)); d.ellipse([6, 6, 8, 8], fill=(220, 255, 190, 255))
    for k in (4, 7, 10): d.point((k, 2), fill=(230, 200, 90, 255)); d.point((k, 13), fill=(230, 200, 90, 255))
    return outline(pips(im, tier))


def scanner(tier):
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([2, 4, 13, 12], fill=(40, 46, 58, 255)); d.rectangle([3, 5, 12, 11], fill=(20, 26, 36, 255))
    d.ellipse([5, 6, 10, 11], outline=(70, 200, 255, 255)); d.point((8, 8), fill=(255, 230, 90, 255)); d.point((6, 7), fill=(90, 255, 140, 255))
    return outline(pips(im, tier))


def eye(iris, extra):
    im = new(); d = ImageDraw.Draw(im)
    d.ellipse([1, 4, 14, 12], fill=(235, 235, 240, 255)); d.ellipse([5, 5, 10, 11], fill=iris); d.ellipse([7, 7, 8, 9], fill=(10, 10, 14, 255))
    d.point((6, 6), fill=(255, 255, 255, 255))
    extra(d)
    return outline(im)


def tid(base, t):
    return base if t == 1 else "%s_%d" % (base, t)


EYES = {
    "olho_zoom": ((70, 130, 230, 255), lambda d: d.rectangle([12, 2, 14, 3], fill=(200, 200, 210, 255))),
    "olho_tatico": ((230, 70, 70, 255), lambda d: (d.line([(1, 2), (4, 2)], fill=(255, 80, 80, 255)), d.line([(11, 14), (14, 14)], fill=(80, 255, 120, 255)))),
    "olho_laser": ((255, 40, 40, 255), lambda d: d.line([(11, 8), (15, 8)], fill=(255, 80, 80, 255))),
    "olho_visao": ((255, 150, 40, 255), lambda d: d.rectangle([1, 13, 4, 14], fill=(90, 255, 120, 255))),
}
ICONS = {}
for t in (1, 2, 3):
    ICONS[tid("bota_voadora", t)] = pips(boot(), t)
    ICONS[tid("jetpack", t)] = pips(jet(), t)
    ICONS[tid("implante_xp", t)] = xp(t)
    ICONS[tid("scanner_minerios", t)] = scanner(t)
    for name, (iris, extra) in EYES.items():
        ICONS[tid(name, t)] = pips(eye(iris, extra), t)
for n, im in ICONS.items():
    im.save(os.path.join(A, "textures", "item", n + ".png"))
    dump(os.path.join(A, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "sandevistan:item/" + n}})
dump(os.path.join(BASE, "data", "c", "tags", "block", "ores.json"), {"replace": False, "values": ["sandevistan:minerio_sucata"]})
print("ok")
