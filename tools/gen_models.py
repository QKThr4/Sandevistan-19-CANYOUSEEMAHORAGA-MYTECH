#!/usr/bin/env python3
"""Converte os modelos .glb (jetpack e Lamina Mantis, enviados pelo usuario) em malhas compactas para o mod.
Saida: assets/sandevistan/models/gear/*.bin + texturas reduzidas em textures/entity/.
Formato .bin: int nPartes; por parte: UTF textura, byte brilho(0/1), int nTris, depois 3 vertices por tri com 8 floats (x y z u v nx ny nz),
posicoes ja em unidades de modelo do Minecraft (pixels/16) no espaco da parte do corpo (costas = corpo; lamina = braco)."""
import io, os, struct, sys
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import glb_reader as gl

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "sandevistan")
JET = sys.argv[1]
MANTIS = sys.argv[2]


def utf(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def write(name, parts):
    os.makedirs(os.path.join(ROOT, "models", "gear"), exist_ok=True)
    out = struct.pack(">i", len(parts))
    for tex, glow, tri in parts:
        v = np.concatenate([tri[0], tri[1], tri[2]], axis=2).astype(">f4")  # (n,3,8)
        out += utf(tex) + struct.pack(">Bi", glow, len(v)) + v.tobytes()
    open(os.path.join(ROOT, "models", "gear", name + ".bin"), "wb").write(out)
    print(name, sum(len(p[2][0]) for p in parts), "tris")


def save_tex(img, name, size):
    img = img.copy()
    if max(img.size) > size:
        r = size / max(img.size)
        img = img.resize((max(1, int(img.width * r)), max(1, int(img.height * r))), Image.LANCZOS)
    img.save(os.path.join(ROOT, "textures", "entity", name + ".png"))


# ---------------- jetpack ----------------
j, b = gl.load(JET)
tris = gl.triangles(j, b)
S = 4.2           # pixels por unidade do modelo
ZOFF = 0.0        # a camada soma o recuo (costas) em Java
groups = {}       # (material, brilho) -> listas
for pos, uv, nrm, mat, ni in tris:
    p = pos.copy()
    X = -p[..., 0] * S
    Y = (2.75 - p[..., 1]) * S
    Z = (p[..., 2] + 0.38) * S
    P = np.stack([X, Y, Z], -1) / 16.0
    N = np.stack([-nrm[..., 0], -nrm[..., 1], nrm[..., 2]], -1)
    n = np.linalg.norm(N, axis=-1, keepdims=True); n[n == 0] = 1; N = N / n
    groups.setdefault(mat, []).append((P, uv, N))
parts = []
tex_of = {0: "jet_0", 1: "jet_1", 2: "jet_2"}
for mat, lst in sorted(groups.items()):
    P = np.concatenate([a for a, _, _ in lst]); U = np.concatenate([a for _, a, _ in lst]); N = np.concatenate([a for _, _, a in lst])
    src = j["textures"][j["materials"][mat]["pbrMetallicRoughness"]["baseColorTexture"]["index"]]["source"]
    save_tex(gl.image(j, b, src), tex_of[mat], 256)
    parts.append(("sandevistan:textures/entity/%s.png" % tex_of[mat], 0, (P, U, N)))
    em = j["materials"][mat].get("emissiveTexture")
    if em:
        esrc = j["textures"][em["index"]]["source"]
        save_tex(gl.image(j, b, esrc), tex_of[mat] + "e", 256)
        parts.append(("sandevistan:textures/entity/%se.png" % tex_of[mat], 1, (P, U, N)))
write("jetpack", parts)

# ---------------- lamina mantis (so o mecanismo; sem o bloco do braco) ----------------
j, b = gl.load(MANTIS)
tris = gl.triangles(j, b)
keep = []
for pos, uv, nrm, mat, ni in tris:
    ext = pos.reshape(-1, 3).max(0) - pos.reshape(-1, 3).min(0)
    if ext[1] >= 0.6 and ext[0] >= 0.15:
        continue   # bloco tan do braco do modelo original
    keep.append((pos, uv, nrm))
allp = np.concatenate([k[0].reshape(-1, 3) for k in keep])
M = 12.0
for side, sel in (("r", lambda x: x > -0.3), ("l", lambda x: x <= -0.3)):
    sub = [k for k in keep if sel(k[0][..., 0].mean())]
    pp = np.concatenate([k[0].reshape(-1, 3) for k in sub])
    cx = (pp[:, 0].min() + pp[:, 0].max()) / 2
    Ps, Us, Ns = [], [], []
    for pos, uv, nrm in sub:
        xm = pos[..., 0] - cx; zm = pos[..., 2]
        # o modelo original tem a lamina inclinada para cima; endireita (paralela ao antebraco)
        ym = pos[..., 1] - np.clip(0.1 - zm, 0, None) * 0.55
        ym = np.maximum(ym, 0.27)
        if side == "r":
            X = -2.0 - (ym - 0.27) * M; Z = xm * M
            N = np.stack([-nrm[..., 1], -nrm[..., 2], nrm[..., 0]], -1)
        else:
            X = 2.0 + (ym - 0.27) * M; Z = -xm * M
            N = np.stack([nrm[..., 1], -nrm[..., 2], -nrm[..., 0]], -1)
        Y = 3.0 + (0.69 - zm) * M
        Ps.append(np.stack([X, Y, Z], -1) / 16.0); Us.append(uv); Ns.append(N)
    save_tex(gl.image(j, b, 0), "mantis", 128)
    write("mantis_" + side, [("sandevistan:textures/entity/mantis.png", 0,
                              (np.concatenate(Ps), np.concatenate(Us), np.concatenate(Ns)))])
