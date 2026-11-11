#!/usr/bin/env python3
"""v5.0.0 etapa 6: icones dos implantes Solar/Reator/Mantis/Injetor, tier IV e itens dificeis."""
import json, os, math
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
    col = (255, 80, 200, 255) if tier == 4 else (255, 230, 90, 255)
    for k in range(tier):
        d.point((2 + k * 2, 14), fill=col)
    return im


def tid(base, t):
    return base if t == 1 else "%s_%d" % (base, t)


def solar():
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([1, 3, 14, 12], fill=(24, 40, 90, 255))
    for x in (1, 5, 9, 13):
        d.line([(x, 3), (x, 12)], fill=(120, 150, 220, 255))
    for y in (3, 6, 9, 12):
        d.line([(1, y), (14, y)], fill=(120, 150, 220, 255))
    d.rectangle([2, 4, 4, 5], fill=(70, 120, 230, 255)); d.rectangle([10, 7, 12, 8], fill=(70, 120, 230, 255))
    d.point((13, 1), fill=(255, 230, 90, 255)); d.point((14, 2), fill=(255, 230, 90, 255))
    return outline(im)


def reactor():
    im = new(); d = ImageDraw.Draw(im)
    d.ellipse([1, 1, 14, 14], fill=(70, 78, 92, 255))
    d.ellipse([3, 3, 12, 12], fill=(20, 40, 60, 255))
    d.ellipse([4, 4, 11, 11], outline=(90, 220, 255, 255))
    d.polygon([(8, 5), (11, 10), (5, 10)], outline=(160, 245, 255, 255))
    d.ellipse([6, 7, 9, 10], fill=(220, 255, 255, 255))
    return outline(im)


def mantis():
    im = new(); d = ImageDraw.Draw(im)
    d.line([(2, 14), (3, 12)], fill=(90, 60, 40, 255), width=2)
    d.polygon([(3, 12), (4, 10), (9, 5), (14, 1), (12, 7), (7, 12), (5, 13)], fill=(190, 200, 215, 255))
    d.line([(4, 11), (13, 2)], fill=(245, 250, 255, 255))
    d.line([(5, 12), (11, 8)], fill=(120, 130, 150, 255))
    d.point((2, 13), fill=(255, 60, 60, 255))
    return outline(im)


def injector():
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([4, 3, 11, 9], fill=(150, 156, 168, 255)); d.rectangle([5, 4, 10, 8], fill=(60, 210, 255, 255))
    d.rectangle([6, 1, 9, 2], fill=(90, 96, 108, 255)); d.rectangle([7, 10, 8, 14], fill=(210, 215, 225, 255))
    d.rectangle([2, 9, 13, 9], fill=(90, 96, 108, 255)); d.point((11, 5), fill=(255, 90, 90, 255))
    return outline(im)


def jet4():
    im = new(); d = ImageDraw.Draw(im)
    for x in (3, 9):
        d.rectangle([x, 2, x + 3, 11], fill=(150, 90, 190, 255)); d.rectangle([x, 4, x + 3, 5], fill=(60, 30, 90, 255))
        d.polygon([(x, 12), (x + 3, 12), (x + 2, 15), (x + 1, 15)], fill=(90, 220, 255, 255)); d.point((x + 1, 13), fill=(240, 255, 255, 255))
    d.rectangle([7, 5, 8, 7], fill=(60, 30, 90, 255))
    return outline(im)


def bobina():
    im = new(); d = ImageDraw.Draw(im)
    d.ellipse([1, 1, 14, 14], fill=(200, 110, 40, 255)); d.ellipse([3, 3, 12, 12], fill=(60, 40, 30, 255))
    d.ellipse([4, 4, 11, 11], outline=(255, 190, 90, 255)); d.ellipse([6, 6, 9, 9], fill=(255, 245, 190, 255))
    return outline(im)


def matriz():
    im = new(); d = ImageDraw.Draw(im)
    pts = [(8 + 7 * math.cos(math.radians(60 * k + 30)), 8 + 7 * math.sin(math.radians(60 * k + 30))) for k in range(6)]
    d.polygon(pts, fill=(50, 30, 90, 255), outline=(170, 110, 255, 255))
    d.line([(8, 1), (8, 15)], fill=(120, 80, 220, 255)); d.line([(2, 5), (14, 11)], fill=(120, 80, 220, 255)); d.line([(2, 11), (14, 5)], fill=(120, 80, 220, 255))
    d.ellipse([6, 6, 9, 9], fill=(230, 210, 255, 255))
    return outline(im)


def nucleo():
    im = new(); d = ImageDraw.Draw(im)
    d.ellipse([0, 0, 15, 15], fill=(30, 80, 120, 140))
    pts = []
    for k in range(10):
        r = 7 if k % 2 == 0 else 3
        pts.append((8 + r * math.cos(math.radians(36 * k - 90)), 8 + r * math.sin(math.radians(36 * k - 90))))
    d.polygon(pts, fill=(240, 250, 255, 255), outline=(120, 220, 255, 255))
    d.ellipse([6, 6, 9, 9], fill=(120, 230, 255, 255))
    return outline(im)


ICONS = {}
for t in (1, 2, 3):
    ICONS[tid("implante_solar", t)] = pips(solar(), t)
    ICONS[tid("injetor_automatico", t)] = pips(injector(), t)
for t in (1, 2, 3, 4):
    ICONS[tid("reator_arc", t)] = pips(reactor(), t)
    ICONS[tid("lamina_mantis", t)] = pips(mantis(), t)
ICONS["jetpack_4"] = pips(jet4(), 4)
ICONS["bobina_fusao"] = bobina()
ICONS["matriz_contencao"] = matriz()
ICONS["nucleo_estelar"] = nucleo()
for n, im in ICONS.items():
    im.save(os.path.join(A, "textures", "item", n + ".png"))
    dump(os.path.join(A, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "sandevistan:item/" + n}})
print("ok", len(ICONS))
