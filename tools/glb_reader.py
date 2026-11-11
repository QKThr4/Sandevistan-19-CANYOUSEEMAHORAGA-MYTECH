import json, struct, io
import numpy as np
from PIL import Image

def load(path):
    b = open(path, 'rb').read()
    l = struct.unpack('<I', b[12:16])[0]
    j = json.loads(b[20:20+l])
    off = 20 + l
    binchunk = b''
    while off < len(b):
        cl, ct = struct.unpack('<II', b[off:off+8])
        if ct == 0x004E4942:
            binchunk = b[off+8:off+8+cl]
        off += 8 + cl
    return j, binchunk

CT = {5120: np.int8, 5121: np.uint8, 5122: np.int16, 5123: np.uint16, 5125: np.uint32, 5126: np.float32}
NC = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4, 'MAT4': 16}

def acc(j, bin_, i):
    a = j['accessors'][i]
    bv = j['bufferViews'][a['bufferView']]
    dt = np.dtype(CT[a['componentType']])
    n = NC[a['type']]
    stride = bv.get('byteStride', 0)
    start = bv.get('byteOffset', 0) + a.get('byteOffset', 0)
    if stride and stride != dt.itemsize * n:
        raw = np.frombuffer(bin_, dtype=np.uint8, count=stride * a['count'], offset=start).reshape(a['count'], stride)
        arr = np.frombuffer(raw[:, :dt.itemsize * n].tobytes(), dtype=dt).reshape(a['count'], n)
    else:
        arr = np.frombuffer(bin_, dtype=dt, count=a['count'] * n, offset=start).reshape(a['count'], n)
    if a.get('normalized'):
        info = np.iinfo(dt)
        arr = arr.astype(np.float32) / info.max
    return arr

def local(n):
    if 'matrix' in n:
        return np.array(n['matrix'], dtype=np.float64).reshape(4, 4).T
    t = np.array(n.get('translation', [0, 0, 0]), dtype=np.float64)
    q = n.get('rotation', [0, 0, 0, 1]); x, y, z, w = q
    r = np.array([[1-2*(y*y+z*z), 2*(x*y-z*w), 2*(x*z+y*w)],
                  [2*(x*y+z*w), 1-2*(x*x+z*z), 2*(y*z-x*w)],
                  [2*(x*z-y*w), 2*(y*z+x*w), 1-2*(x*x+y*y)]])
    s = np.array(n.get('scale', [1, 1, 1]), dtype=np.float64)
    m = np.eye(4); m[:3, :3] = r * s; m[:3, 3] = t
    return m

def worlds(j):
    nodes = j['nodes']
    parent = [-1] * len(nodes)
    for i, n in enumerate(nodes):
        for c in n.get('children', []): parent[c] = i
    W = [None] * len(nodes)
    def w(i):
        if W[i] is None:
            m = local(nodes[i])
            W[i] = m if parent[i] < 0 else w(parent[i]) @ m
        return W[i]
    for i in range(len(nodes)): w(i)
    return W

def triangles(j, bin_):
    """Lista de (tri_pos Nx3x3, tri_uv Nx3x2, tri_nrm Nx3x3, material) no espaco global, pose de repouso."""
    W = worlds(j)
    out = []
    for ni, n in enumerate(j['nodes']):
        if 'mesh' not in n: continue
        mesh = j['meshes'][n['mesh']]
        for p in mesh['primitives']:
            at = p['attributes']
            pos = acc(j, bin_, at['POSITION']).astype(np.float64)
            nrm = acc(j, bin_, at['NORMAL']).astype(np.float64) if 'NORMAL' in at else np.zeros_like(pos)
            uv = acc(j, bin_, at['TEXCOORD_0']).astype(np.float64) if 'TEXCOORD_0' in at else np.zeros((len(pos), 2))
            if 'skin' in n and 'JOINTS_0' in at:
                sk = j['skins'][n['skin']]
                ibm = acc(j, bin_, sk['inverseBindMatrices']).reshape(-1, 4, 4).transpose(0, 2, 1).astype(np.float64)
                jm = np.array([W[jn] @ ibm[k] for k, jn in enumerate(sk['joints'])])
                jt = acc(j, bin_, at['JOINTS_0']).astype(int); wt = acc(j, bin_, at['WEIGHTS_0']).astype(np.float64)
                hp = np.concatenate([pos, np.ones((len(pos), 1))], 1)
                res = np.zeros_like(pos); rn = np.zeros_like(pos)
                for k in range(4):
                    M = jm[jt[:, k]]
                    res += wt[:, k:k+1] * np.einsum('nij,nj->ni', M, hp)[:, :3]
                    rn += wt[:, k:k+1] * np.einsum('nij,nj->ni', M[:, :3, :3], nrm)
                pos, nrm = res, rn
            else:
                M = W[ni]
                pos = pos @ M[:3, :3].T + M[:3, 3]
                nrm = nrm @ np.linalg.inv(M[:3, :3]).T
            idx = acc(j, bin_, p['indices']).reshape(-1).astype(int) if 'indices' in p else np.arange(len(pos))
            idx = idx[:len(idx) // 3 * 3].reshape(-1, 3)
            out.append((pos[idx], uv[idx], nrm[idx], p.get('material', 0), ni))
    return out

def image(j, bin_, i):
    im = j['images'][i]
    bv = j['bufferViews'][im['bufferView']]
    s = bv.get('byteOffset', 0)
    return Image.open(io.BytesIO(bin_[s:s + bv['byteLength']])).convert('RGBA')

def mat_image(j, bin_, m):
    mt = j['materials'][m]
    bt = mt.get('pbrMetallicRoughness', {}).get('baseColorTexture')
    if not bt: return None
    return bt['index'] and j['textures'][bt['index']]['source'] if False else j['textures'][bt['index']]['source']
