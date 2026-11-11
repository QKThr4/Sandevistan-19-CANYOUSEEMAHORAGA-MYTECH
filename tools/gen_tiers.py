"""Gera as texturas e modelos dos tiers II e III: a textura do tier I com marcas (barras) no canto inferior direito."""
import json
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "sandevistan")
TEX = os.path.join(ROOT, "textures", "item")
MOD = os.path.join(ROOT, "models", "item")
BASES = ["braco_ferro", "pernas_ciberneticas", "estrutura_ossea", "sistema_nervoso", "coracao_ferro",
         "chip_artes_marciais", "chip_supressao_dor", "chip_andarilho", "chip_ram"]
COLORS = {2: (255, 200, 40, 255), 3: (80, 230, 255, 255)}


def mark(img, tier):
    img = img.convert("RGBA")
    px = img.load()
    for x in range(16 - (2 * tier + 2) + 0, 16):
        for y in range(11, 16):
            px[x, y] = (10, 10, 14, 235)
    for i in range(tier):
        x = 16 - 2 * (tier - i)
        for y in range(12, 15):
            px[x, y] = COLORS[tier]
    return img


for base in BASES:
    src = Image.open(os.path.join(TEX, base + ".png"))
    for tier in (2, 3):
        name = "%s_%d" % (base, tier)
        mark(src, tier).save(os.path.join(TEX, name + ".png"))
        with open(os.path.join(MOD, name + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "sandevistan:item/" + name}}, f, separators=(",", ":"))
print("ok")
