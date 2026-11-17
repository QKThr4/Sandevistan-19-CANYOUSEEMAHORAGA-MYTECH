#!/usr/bin/env python3
"""Novo modelo do jetpack (jetpack_-_minecraft.glb, CC-BY 4.0, ollypop567) -> models/gear/jetpack.bin + textures/entity/jet_N.png.
Escala 1,2x; cerca de 30% do modelo dentro do torso; os dois tanques ficam lado a lado nas costas (modelo girado 90 graus).
Uso: gen_jetpack2.py arquivo.glb  [FLIP=1 inverte a frente/tras]"""
import os, struct, sys
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import glb_reader as gl
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "sandevistan")
S = 1.2
FLIP = os.environ.get("FLIP") == "1"
j, b = gl.load(sys.argv[1])
groups = {}
for pos, uv, nrm, mat, ni in gl.triangles(j, b):
    p = pos * 16.0
    X = (p[..., 2] - 7.5) * S
    Y = 12.6 - p[..., 1] * S
    d = (p[..., 0] - 5.0) if FLIP else (11.0 - p[..., 0])
    Z = 2.0 + d * S - 2.2
    P = np.stack([X, Y, Z], -1) / 16.0
    N = np.stack([nrm[..., 2], -nrm[..., 1], nrm[..., 0] * (1 if FLIP else -1)], -1)
    groups.setdefault(mat, []).append((P, uv, N))
parts = []
for mat, lst in sorted(groups.items()):
    P = np.concatenate([a for a, _, _ in lst]); U = np.concatenate([a for _, a, _ in lst]); N = np.concatenate([a for _, _, a in lst])
    src = j["textures"][j["materials"][mat]["pbrMetallicRoughness"]["baseColorTexture"]["index"]]["source"]
    gl.image(j, b, src).save(os.path.join(ROOT, "textures", "entity", "jet_%d.png" % mat))
    parts.append(("sandevistan:textures/entity/jet_%d.png" % mat, 0, (P, U, N)))
out = struct.pack(">i", len(parts))
for tex, glow, tri in parts:
    v = np.concatenate([tri[0], tri[1], tri[2]], axis=2).astype(">f4")
    t = tex.encode()
    out += struct.pack(">H", len(t)) + t + struct.pack(">Bi", glow, len(v)) + v.tobytes()
open(os.path.join(ROOT, "models", "gear", "jetpack.bin"), "wb").write(out)
print("ok", len(parts), "partes")
