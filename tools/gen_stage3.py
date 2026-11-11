#!/usr/bin/env python3
"""v5.0.0 etapa 3: texturas, modelos, blocos, loot, tags e receitas da energia."""
import json, os, random
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(BASE, "assets", "sandevistan")
D = os.path.join(BASE, "data", "sandevistan")
random.seed(11)


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


def battery(body, glow, cells):
    im = new(); d = ImageDraw.Draw(im)
    d.rectangle([5, 3, 10, 14], fill=body)
    d.rectangle([6, 2, 9, 3], fill=(190, 190, 200, 255))
    for k in range(cells):
        y = 12 - k * 2
        d.rectangle([6, y, 9, y + 1], fill=glow)
    d.line([(5, 3), (5, 14)], fill=(255, 255, 255, 60))
    return outline(im)


ITEMS = {
    "celula_basica": battery((60, 64, 72, 255), (90, 230, 120, 255), 2),
    "celula_ions": battery((40, 70, 120, 255), (90, 200, 255, 255), 3),
    "bateria_litio": battery((90, 50, 130, 255), (210, 130, 255, 255), 4),
    "bateria_alta_densidade": battery((120, 70, 20, 255), (255, 200, 60, 255), 5),
}
core = new(); d = ImageDraw.Draw(core)
d.rectangle([3, 3, 12, 12], fill=(30, 34, 44, 255)); d.ellipse([4, 4, 11, 11], fill=(20, 200, 255, 255))
d.ellipse([6, 6, 9, 9], fill=(240, 255, 255, 255))
for k in (4, 7, 10):
    d.point((k, 1), fill=(190, 160, 70, 255)); d.point((k, 14), fill=(190, 160, 70, 255))
ITEMS["nucleo_fusao"] = outline(core)
for n, im in ITEMS.items():
    im.save(os.path.join(A, "textures", "item", n + ".png"))
    dump(os.path.join(A, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "sandevistan:item/" + n}})


def noise(base, n=12):
    im = Image.new("RGBA", (16, 16), base); p = im.load()
    for x in range(16):
        for y in range(16):
            v = random.randint(-n, n)
            p[x, y] = tuple(max(0, min(255, c + v)) for c in base[:3]) + (255,)
    return im


def frame(im, col):
    ImageDraw.Draw(im).rectangle([0, 0, 15, 15], outline=col)
    return im


def save_block(name, top, side, bottom):
    for suf, im in (("top", top), ("side", side), ("bottom", bottom)):
        im.save(os.path.join(A, "textures", "block", "%s_%s.png" % (name, suf)))
    dump(os.path.join(A, "models", "block", name + ".json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "sandevistan:block/%s_top" % name, "side": "sandevistan:block/%s_side" % name, "bottom": "sandevistan:block/%s_bottom" % name}})
    dump(os.path.join(A, "blockstates", name + ".json"), {"variants": {"": {"model": "sandevistan:block/" + name}}})
    dump(os.path.join(A, "models", "item", name + ".json"), {"parent": "sandevistan:block/" + name})
    dump(os.path.join(D, "loot_table", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
         "entries": [{"type": "minecraft:item", "name": "sandevistan:" + name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


metal = (58, 62, 72)
bot = frame(noise((40, 43, 50)), (22, 24, 30))

# carregador: placa ciano com slot
top = frame(noise(metal), (26, 30, 38)); d = ImageDraw.Draw(top)
d.rectangle([5, 5, 10, 10], fill=(20, 24, 30, 255)); d.rectangle([6, 6, 9, 9], fill=(40, 200, 255, 255))
side = frame(noise(metal), (26, 30, 38)); d = ImageDraw.Draw(side)
d.rectangle([3, 4, 12, 11], fill=(24, 28, 34, 255))
for x in range(4, 12, 2): d.line([(x, 5), (x, 10)], fill=(40, 200, 255, 255))
save_block("carregador_baterias", top, side, bot)

# gerador a combustivel: grelha e chama
top = frame(noise((70, 60, 56)), (30, 26, 24)); d = ImageDraw.Draw(top)
for x in range(3, 14, 3): d.line([(x, 3), (x, 12)], fill=(20, 18, 18, 255))
side = frame(noise((78, 66, 60)), (30, 26, 24)); d = ImageDraw.Draw(side)
d.rectangle([4, 6, 11, 12], fill=(20, 16, 14, 255)); d.polygon([(8, 7), (6, 12), (10, 12)], fill=(255, 150, 30, 255)); d.polygon([(8, 9), (7, 12), (9, 12)], fill=(255, 230, 90, 255))
save_block("gerador_combustivel", top, side, bot)

# painel solar: celulas azuis
top = frame(noise((24, 44, 96), 8), (150, 156, 170)); d = ImageDraw.Draw(top)
for k in (5, 10): d.line([(k, 1), (k, 14)], fill=(150, 156, 170, 255)); d.line([(1, k), (14, k)], fill=(150, 156, 170, 255))
d.point((3, 3), fill=(140, 190, 255, 255)); d.point((8, 8), fill=(140, 190, 255, 255)); d.point((12, 12), fill=(140, 190, 255, 255))
side = frame(noise(metal), (26, 30, 38)); d = ImageDraw.Draw(side)
d.rectangle([2, 5, 13, 10], fill=(24, 44, 96, 255)); d.line([(2, 7), (13, 7)], fill=(150, 156, 170, 255))
save_block("painel_solar", top, side, bot)

# turbina eolica: helice
top = frame(noise((200, 204, 212), 8), (110, 114, 124)); d = ImageDraw.Draw(top)
d.line([(8, 1), (8, 14)], fill=(110, 114, 124, 255)); d.line([(1, 8), (14, 8)], fill=(110, 114, 124, 255)); d.ellipse([6, 6, 9, 9], fill=(40, 44, 52, 255))
side = frame(noise((190, 194, 204), 8), (110, 114, 124)); d = ImageDraw.Draw(side)
d.polygon([(8, 8), (4, 3), (6, 2)], fill=(255, 255, 255, 255)); d.polygon([(8, 8), (13, 10), (12, 12)], fill=(255, 255, 255, 255)); d.polygon([(8, 8), (3, 11), (5, 13)], fill=(255, 255, 255, 255))
d.ellipse([7, 7, 9, 9], fill=(60, 64, 72, 255))
save_block("turbina_eolica", top, side, bot)

# gerador termico: placas laranja
top = frame(noise((96, 44, 28), 8), (40, 24, 20)); d = ImageDraw.Draw(top)
for y in (4, 8, 12): d.line([(2, y), (13, y)], fill=(255, 140, 40, 255))
side = frame(noise((84, 48, 36), 8), (40, 24, 20)); d = ImageDraw.Draw(side)
d.rectangle([3, 3, 12, 12], fill=(30, 20, 18, 255))
for y in (5, 8, 11): d.line([(4, y), (11, y)], fill=(255, 120, 30, 255))
save_block("gerador_termico", top, side, bot)

# tags de mineracao: acrescenta os blocos de energia
pk = os.path.join(BASE, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json")
vals = json.load(open(pk))["values"]
for b in ("carregador_baterias", "gerador_combustivel", "painel_solar", "turbina_eolica", "gerador_termico"):
    if "sandevistan:" + b not in vals: vals.append("sandevistan:" + b)
dump(pk, {"values": vals})

# receitas comuns
RC = os.path.join(D, "recipe")


def shaped(name, pattern, key):
    dump(os.path.join(RC, name + ".json"), {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
         "key": {k: ({"item": v} if not v.startswith("#") else {"tag": v[1:]}) for k, v in key.items()}, "result": {"id": "sandevistan:" + name, "count": 1}})


I = "minecraft:iron_ingot"
shaped("carregador_baterias", ["ICI", "SBS", "III"], {"I": I, "C": "sandevistan:capacitor", "S": "sandevistan:servo_motor", "B": "sandevistan:bateria_energia"})
shaped("gerador_combustivel", ["ICI", "IFI", "ISI"], {"I": I, "C": "minecraft:copper_ingot", "F": "minecraft:furnace", "S": "sandevistan:servo_motor"})
shaped("painel_solar", ["GGG", "PCP", "III"], {"G": "minecraft:glass", "P": "sandevistan:placa_circuito", "C": "sandevistan:capacitor", "I": I})
shaped("turbina_eolica", [" V ", "VSV", "III"], {"V": "sandevistan:ventoinha", "S": "sandevistan:servo_motor", "I": I})
shaped("gerador_termico", ["ILI", "SCS", "III"], {"I": I, "L": "minecraft:lava_bucket", "S": "sandevistan:servo_motor", "C": "sandevistan:capacitor"})
print("ok")
