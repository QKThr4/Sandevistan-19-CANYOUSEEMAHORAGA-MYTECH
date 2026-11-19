#!/usr/bin/env python3
"""Lamina Mantis ORIGINAL (desenhada do zero, sem nenhum modelo de terceiros): cinza metalico.
Gera models/gear/mantis_r.bin, mantis_l.bin e textures/entity/mantis.png. Unidades: pixels do modelo (y para baixo)."""
import os, struct
import numpy as np
from PIL import Image
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "sandevistan")
COLS = [(62, 65, 70), (112, 117, 124), (182, 188, 195), (228, 231, 235), (36, 38, 42), (142, 147, 154)]
DARK, MID, LIGHT, EDGE, BLACK, RIVET = range(6)
img = Image.new("RGB", (16, 16))
for i, c in enumerate(COLS):
    for x in range(4):
        for y in range(4): img.putpixel(((i % 4) * 4 + x, (i // 4) * 4 + y), c)
img.save(os.path.join(ROOT, "textures", "entity", "mantis.png"))

tris = []  # (verts[3][3], color)

def quad(a, b, c, d, col, ref):
    a, b, c, d = map(np.array, (a, b, c, d))
    n = np.cross(b - a, c - a)
    if np.dot(n, (a + b + c + d) / 4 - ref) < 0:
        a, b, c, d = d, c, b, a
    tris.append(((a, b, c), col)); tris.append(((a, c, d), col))

def loft(r0, r1, col, capend=True):
    """r = (xc, zc, hx, hz, y): retangulo na altura y. Une dois retangulos com 4 lados + tampas."""
    def corners(r):
        xc, zc, hx, hz, y = r
        return [(xc - hx, y, zc - hz), (xc + hx, y, zc - hz), (xc + hx, y, zc + hz), (xc - hx, y, zc + hz)]
    A, B = corners(r0), corners(r1)
    ref = np.mean(np.array(A + B), axis=0)
    for i in range(4):
        j = (i + 1) % 4
        quad(A[i], A[j], B[j], B[i], col, ref)
    quad(*A, col, ref)
    if capend: quad(*B, col, ref)

def box(x0, y0, z0, x1, y1, z1, col):
    loft(((x0 + x1) / 2, (z0 + z1) / 2, (x1 - x0) / 2, (z1 - z0) / 2, y0), ((x0 + x1) / 2, (z0 + z1) / 2, (x1 - x0) / 2, (z1 - z0) / 2, y1), col)

# ---- braco direito (x negativo = lado de fora; y cresce para a mao) ----
# Lamina DOBRAVEL: carcaca no antebraco -> bracos articulados que sobem para tras (cotovelo) -> pivo -> lamina que aponta para frente
box(-3.7, 4.0, -1.6, -2.0, 13.0, 1.6, DARK)               # carcaca
box(-4.0, 4.0, -1.8, -3.6, 5.2, 1.8, MID)
box(-4.0, 11.6, -1.8, -3.6, 13.0, 1.8, MID)
box(-4.2, 6.2, -0.9, -3.7, 10.4, 0.9, BLACK)              # janela do mecanismo
box(-4.2, 7.0, -1.3, -3.8, 7.4, -0.9, RIVET)
box(-4.2, 9.2, 0.9, -3.8, 9.6, 1.3, RIVET)
# dois bracos inclinados: saem da carcaca e vao para tras e para fora ate o pivo
for zc in (-1.1, 1.1):
    loft((-3.9, zc, 0.35, 0.40, 10.0), (-5.2, zc, 0.35, 0.40, 6.0), MID)
    loft((-5.2, zc, 0.35, 0.40, 6.0), (-6.3, zc, 0.35, 0.40, 2.6), MID)
# pivo (junta) no topo
box(-7.0, 1.4, -1.6, -5.6, 3.4, 1.6, DARK)
box(-7.2, 1.8, -0.9, -6.8, 3.0, 0.9, RIVET)
# atuador fino (pistao) da carcaca ate a lamina
loft((-4.0, 0.0, 0.25, 0.30, 12.4), (-5.6, 0.0, 0.25, 0.30, 9.0), BLACK)
# lamina: do pivo para FRENTE, paralela ao antebraco, passando bem da mao e curvando de leve para dentro
prof = [(-6.3, 0.0, 0.40, 1.20, 2.4), (-6.1, 0.0, 0.38, 1.35, 9.0), (-5.8, 0.0, 0.34, 1.25, 16.0), (-5.4, 0.0, 0.28, 0.85, 22.0), (-5.0, 0.0, 0.18, 0.35, 26.0), (-4.7, 0.0, 0.05, 0.05, 27.8)]
for a_, b_ in zip(prof, prof[1:]): loft(a_, b_, LIGHT)
ridge = [(x - 0.34, z, 0.06, hz * 0.6, y) for x, z, hx, hz, y in prof[:-1]]
for a_, b_ in zip(ridge, ridge[1:]): loft(a_, b_, EDGE)
# serrilha no lado de dentro (de frente para o braco)
for k in range(9):
    y = 6.0 + k * 2.0
    box(-5.95 + k * 0.1, y, -0.55, -5.55 + k * 0.1, y + 0.9, 0.55, MID)

def write(name, mirror):
    verts = []
    for (v, col) in tris:
        P = np.array(v, dtype=float) / 16.0
        if mirror: P[:, 0] *= -1
        n = np.cross(P[1] - P[0], P[2] - P[0]); n = n / (np.linalg.norm(n) or 1)
        u = ((col % 4) * 4 + 2) / 16.0; w = ((col // 4) * 4 + 2) / 16.0
        for p in P: verts.append([*p, u, w, *n])
    arr = np.array(verts, dtype=">f4")
    tex = b"sandevistan:textures/entity/mantis.png"
    out = struct.pack(">i", 1) + struct.pack(">H", len(tex)) + tex + struct.pack(">Bi", 0, len(arr) // 3) + arr.tobytes()
    open(os.path.join(ROOT, "models", "gear", name + ".bin"), "wb").write(out)
write("mantis_r", False); write("mantis_l", True)
print("ok", len(tris), "triangulos")
