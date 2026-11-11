#!/usr/bin/env python3
"""v5.0.0 etapa 4: texturas dos robos, loot, spawns."""
import json, os, random
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(BASE, "assets", "sandevistan")
D = os.path.join(BASE, "data", "sandevistan")
random.seed(5)


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)


def metal(w, h, base, n=14):
    im = Image.new("RGBA", (w, h), base + (255,)); p = im.load()
    for x in range(w):
        for y in range(h):
            v = random.randint(-n, n)
            p[x, y] = tuple(max(0, min(255, c + v)) for c in base) + (255,)
    d = ImageDraw.Draw(im)
    for y in range(3, h, 8): d.line([(0, y), (w, y)], fill=tuple(max(0, c - 40) for c in base) + (255,))
    for x in range(5, w, 11): d.line([(x, 0), (x, h)], fill=tuple(max(0, c - 28) for c in base) + (255,))
    return im


def save(name, im):
    path = os.path.join(A, "textures", "entity", name + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


def eyes(im, pts, col):
    d = ImageDraw.Draw(im)
    for (x, y) in pts: d.rectangle([x, y, x + 1, y], fill=col + (255,))


# zumbi / seguranca: layout 64x64, rosto da cabeca em (8..15, 8..15)
z = metal(64, 64, (92, 104, 108)); eyes(z, [(9, 12), (13, 12)], (230, 40, 40)); save("robo_zumbi", z)
s = metal(64, 64, (52, 58, 66), 10); eyes(s, [(9, 12), (13, 12)], (255, 64, 64))
ImageDraw.Draw(s).rectangle([20, 22, 27, 25], fill=(255, 190, 40, 255)); save("robo_seguranca", s)
# creeper / esqueleto: 64x32, rosto em (8..15, 8..15)
c = metal(64, 32, (84, 100, 92)); eyes(c, [(9, 11), (13, 11)], (40, 230, 230)); ImageDraw.Draw(c).rectangle([10, 13, 13, 15], fill=(30, 40, 40, 255)); save("robo_creeper", c)
k = metal(64, 32, (176, 182, 192)); eyes(k, [(9, 11), (13, 11)], (40, 160, 255)); save("robo_esqueleto", k)
# cao (layout do porco)
dg = metal(64, 32, (108, 114, 122)); eyes(dg, [(9, 10), (13, 10)], (255, 170, 30)); save("robo_cao", dg)
# drone 64x64
dr = metal(64, 64, (130, 138, 152), 10)
ImageDraw.Draw(dr).rectangle([0, 13, 8, 17], fill=(40, 255, 176, 255)); save("robo_drone", dr)


def loot(name, scrap, extra):
    pools = [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "sandevistan:sucata", "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": scrap[0], "max": scrap[1]}},
        {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}]
    for item, chance, empty in extra:
        e = {"type": "minecraft:item", "name": "sandevistan:" + item}
        if empty:
            e["functions"] = [{"function": "minecraft:set_damage", "damage": 0.0}]
        pools.append({"rolls": 1, "bonus_rolls": 0, "entries": [e], "conditions": [{"condition": "minecraft:random_chance", "chance": chance}]})
    dump(os.path.join(D, "loot_table", "entities", name + ".json"), {"type": "minecraft:entity", "pools": pools})


loot("robo_zumbi", (1, 3), [("componente_comum", 0.35, False), ("celula_basica", 0.05, True)])
loot("robo_esqueleto", (1, 3), [("componente_comum", 0.35, False), ("fios_eletronicos", 0.3, False), ("celula_basica", 0.06, True)])
loot("robo_creeper", (1, 2), [("componente_comum", 0.3, False), ("capacitor", 0.15, False)])
loot("robo_cao", (1, 2), [("conjunto_engrenagens", 0.3, False), ("componente_comum", 0.2, False)])
loot("robo_seguranca", (3, 6), [("componente_comum", 0.8, False), ("componente_melhorado", 0.3, False), ("celula_ions", 0.1, True), ("celula_basica", 0.2, True)])
loot("robo_drone", (1, 3), [("componente_comum", 0.4, False), ("componente_melhorado", 0.08, False), ("celula_basica", 0.08, True)])

sp = lambda n, w, lo, hi: {"type": "sandevistan:" + n, "weight": w, "minCount": lo, "maxCount": hi}
dump(os.path.join(D, "neoforge", "biome_modifier", "robos.json"), {"type": "neoforge:add_spawns", "biomes": "#minecraft:is_overworld",
     "spawners": [sp("robo_zumbi", 10, 1, 2), sp("robo_esqueleto", 8, 1, 2), sp("robo_creeper", 6, 1, 1), sp("robo_cao", 6, 1, 3),
                  sp("robo_seguranca", 2, 1, 1), sp("robo_drone", 4, 1, 2)]})

# modelos dos ovos
for n in ("robo_zumbi", "robo_creeper", "robo_esqueleto", "robo_cao", "robo_seguranca", "robo_drone"):
    dump(os.path.join(A, "models", "item", n + "_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})
print("ok")
