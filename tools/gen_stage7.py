#!/usr/bin/env python3
"""v5.0.0 etapa 7: texturas dos modelos 3D do corpo (gear/solar/reator) e icones tier II/III dos remedios."""
import json, os, random
from PIL import Image, ImageDraw
BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "sandevistan")
for base in ("estabilizador_neural", "pilula_sanidade", "imunossupressor"):
    src = Image.open(os.path.join(BASE, "textures", "item", base + ".png")).convert("RGBA")
    for t in (2, 3):
        im = src.copy(); d = ImageDraw.Draw(im)
        for k in range(t):
            d.point((1 + k * 2, 14), fill=(255, 230, 90, 255))
        n = "%s_%d" % (base, t)
        im.save(os.path.join(BASE, "textures", "item", n + ".png"))
        with open(os.path.join(BASE, "models", "item", n + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "sandevistan:item/" + n}}, f, indent=2)
print("ok")
