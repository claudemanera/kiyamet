"""Tiny software renderer for GeckoLib geo.json + box-UV texture -> 3/4 view PNG (for previews and the guide)."""
import json, math, sys
import numpy as np
from PIL import Image, ImageDraw

def rmat(rx, ry, rz):
    # GeckoLib negates X and Y; applied Z, then Y, then X
    x, y, z = math.radians(-rx), math.radians(-ry), math.radians(rz)
    Rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    Ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    Rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return Rx @ Ry @ Rz

def render(geo, tex, out, size=512, yaw=35, pitch=24, pose=None):
    g = json.load(open(geo))["minecraft:geometry"][0]; T = Image.open(tex).convert('RGBA'); tw, th = T.size
    bones = {b["name"]: b for b in g["bones"]}
    pose = pose or {}
    def chain(name):
        out = []
        while name: out.append(bones[name]); name = bones[name].get("parent")
        return out
    faces = []
    for b in g["bones"]:
        for c in b.get("cubes", []):
            o, s = np.array(c["origin"], float), np.array(c["size"], float); u, v = c["uv"]; inf = c.get("inflate", 0)
            o -= inf; s2 = s + 2 * inf
            w, h, d = s
            P = lambda x, y, z: np.array([o[0] + x * s2[0], o[1] + y * s2[1], o[2] + z * s2[2]])
            # (corners, uv rect) ; front = -z
            W, H, D = [max(1, math.ceil(k)) for k in s]
            quads = [([P(0, 1, 0), P(1, 1, 0), P(1, 1, 1), P(0, 1, 1)], (u + D, v, W, D)),            # top
                     ([P(0, 0, 1), P(1, 0, 1), P(1, 0, 0), P(0, 0, 0)], (u + D + W, v, W, D)),        # bottom
                     ([P(1, 1, 0), P(0, 1, 0), P(0, 0, 0), P(1, 0, 0)], (u + D, v + D, W, H)),        # front (-z)
                     ([P(0, 1, 1), P(1, 1, 1), P(1, 0, 1), P(0, 0, 1)], (u + 2 * D + W, v + D, W, H)),  # back
                     ([P(1, 1, 1), P(1, 1, 0), P(1, 0, 0), P(1, 0, 1)], (u, v + D, D, H)),            # +x side
                     ([P(0, 1, 0), P(0, 1, 1), P(0, 0, 1), P(0, 0, 0)], (u + D + W, v + D, D, H))]    # -x side
            def xf(p):
                if "rotation" in c:
                    pv = np.array(c.get("pivot", c["origin"]), float); p = rmat(*c["rotation"]) @ (p - pv) + pv
                for bb in chain(b["name"]):
                    r = list(bb.get("rotation", [0, 0, 0])); extra = pose.get(bb["name"])
                    if extra: r = [r[i] + extra[i] for i in range(3)]
                    if any(r):
                        pv = np.array(bb["pivot"], float); p = rmat(*r) @ (p - pv) + pv
                return p
            for corners, rect in quads:
                faces.append(([xf(p) for p in corners], rect))
    V = rmat(pitch, 0, 0) @ rmat(0, yaw, 0)   # view from the front-left, slightly above
    proj = [([V @ p for p in cs], r) for cs, r in faces]
    allp = np.array([p for cs, _ in proj for p in cs])
    mn, mx = allp.min(0), allp.max(0); sc = (size * 0.86) / max(mx[0] - mn[0], mx[1] - mn[1]); cx, cy = (mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2
    S = 2; N = size * S
    col = np.zeros((N, N, 4), np.uint8); zb = np.full((N, N), np.inf)
    Tn = np.array(T)
    def scr(p): return np.array([(p[0] - cx) * sc * S + N / 2, N / 2 - (p[1] - cy) * sc * S, p[2]])
    for cs, (fu, fv, fw, fh) in proj:
        n = np.cross(cs[1] - cs[0], cs[3] - cs[0]); ln = np.linalg.norm(n)
        if ln == 0: continue
        n /= ln
        pass
        shade = 0.62 + 0.38 * max(0.0, min(1.0, n[1] * 0.9 - n[0] * 0.25 + 0.3))
        a, b_, _, d4 = [scr(p) for p in cs]
        e1, e2 = b_ - a, d4 - a
        M = np.array([[e1[0], e2[0]], [e1[1], e2[1]]])
        if abs(np.linalg.det(M)) < 1e-9: continue
        Mi = np.linalg.inv(M)
        c4 = a + e1 + e2
        xs = [a[0], b_[0], d4[0], c4[0]]; ys = [a[1], b_[1], d4[1], c4[1]]
        x0, x1 = max(0, int(min(xs))), min(N - 1, int(max(xs)) + 1); y0, y1 = max(0, int(min(ys))), min(N - 1, int(max(ys)) + 1)
        if x1 < x0 or y1 < y0: continue
        X, Y = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        st = np.tensordot(Mi, np.stack([X - a[0], Y - a[1]]), axes=1)
        sS, tT = st[0], st[1]
        inside = (sS >= 0) & (sS < 1) & (tT >= 0) & (tT < 1)
        if not inside.any(): continue
        z = a[2] + e1[2] * sS + e2[2] * tT
        tu = np.clip((fu + np.floor(sS * fw)).astype(int), 0, tw - 1); tv = np.clip((fv + np.floor(tT * fh)).astype(int), 0, th - 1)
        px = Tn[tv, tu]
        sub = zb[y0:y1 + 1, x0:x1 + 1]
        m = inside & (z < sub - 1e-4) & (px[..., 3] > 20)
        sub[m] = z[m]
        cc = col[y0:y1 + 1, x0:x1 + 1]
        cc[m, :3] = np.clip(px[m, :3] * shade, 0, 255).astype(np.uint8); cc[m, 3] = 255
    Image.fromarray(col).resize((size, size), Image.LANCZOS).save(out)
if __name__ == '__main__':
    render(*sys.argv[1:4])


def render_block(model_path, out, size=256, yaw=35, pitch=28, assets='/home/claude/repo/src/main/resources/assets'):
    """render a Java block model (elements + per-face uv) at 3/4 view"""
    m = json.load(open(model_path))
    texs = m.get("textures", {})
    def tex(ref):
        while ref.startswith('#'): ref = texs.get(ref[1:], '')
        ns, p = ref.split(':') if ':' in ref else ('minecraft', ref)
        return Image.open(f'{assets}/{ns}/textures/{p}.png').convert('RGBA')
    cache = {}
    faces = []
    for e in m["elements"]:
        f0, t0 = np.array(e["from"], float), np.array(e["to"], float)
        P = lambda x, y, z: np.array([f0[0] + (t0[0] - f0[0]) * x, f0[1] + (t0[1] - f0[1]) * y, f0[2] + (t0[2] - f0[2]) * z])
        quads = {"up": [P(0, 1, 0), P(1, 1, 0), P(1, 1, 1), P(0, 1, 1)], "down": [P(0, 0, 1), P(1, 0, 1), P(1, 0, 0), P(0, 0, 0)],
                 "north": [P(1, 1, 0), P(0, 1, 0), P(0, 0, 0), P(1, 0, 0)], "south": [P(0, 1, 1), P(1, 1, 1), P(1, 0, 1), P(0, 0, 1)],
                 "east": [P(1, 1, 1), P(1, 1, 0), P(1, 0, 0), P(1, 0, 1)], "west": [P(0, 1, 0), P(0, 1, 1), P(0, 0, 1), P(0, 0, 0)]}
        for d, fc in e["faces"].items():
            ref = fc["texture"]
            if ref not in cache: cache[ref] = np.array(tex(ref))
            uv = fc.get("uv")
            if uv is None:
                if d in ("up", "down"): uv = [f0[0], f0[2], t0[0], t0[2]]
                elif d in ("north", "south"): uv = [f0[0], 16 - t0[1], t0[0], 16 - f0[1]]
                else: uv = [f0[2], 16 - t0[1], t0[2], 16 - f0[1]]
            faces.append(([p - 8 for p in quads[d]], cache[ref], uv))
    V = rmat(pitch, 0, 0) @ rmat(0, yaw, 0)
    proj = [([V @ p for p in cs], t, uv) for cs, t, uv in faces]
    allp = np.array([p for cs, _, _ in proj for p in cs]); mn, mx = allp.min(0), allp.max(0)
    sc = size * 0.86 / max(mx[0] - mn[0], mx[1] - mn[1]); cx, cy = (mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2
    S = 2; N = size * S; col = np.zeros((N, N, 4), np.uint8); zb = np.full((N, N), np.inf)
    def scr(p): return np.array([(p[0] - cx) * sc * S + N / 2, N / 2 - (p[1] - cy) * sc * S, p[2]])
    for cs, T, (u0, v0, u1, v1) in proj:
        n = np.cross(cs[1] - cs[0], cs[3] - cs[0]); ln = np.linalg.norm(n)
        if ln == 0: continue
        n /= ln
        shade = 0.62 + 0.38 * max(0.0, min(1.0, n[1] * 0.9 - n[0] * 0.25 + 0.3))
        a, b_, _, d4 = [scr(p) for p in cs]; e1, e2 = b_ - a, d4 - a
        M = np.array([[e1[0], e2[0]], [e1[1], e2[1]]])
        if abs(np.linalg.det(M)) < 1e-9: continue
        Mi = np.linalg.inv(M); c4 = a + e1 + e2
        xs = [a[0], b_[0], d4[0], c4[0]]; ys = [a[1], b_[1], d4[1], c4[1]]
        x0, x1 = max(0, int(min(xs))), min(N - 1, int(max(xs)) + 1); y0, y1 = max(0, int(min(ys))), min(N - 1, int(max(ys)) + 1)
        if x1 < x0 or y1 < y0: continue
        X, Y = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        st = np.tensordot(Mi, np.stack([X - a[0], Y - a[1]]), axes=1); sS, tT = st[0], st[1]
        inside = (sS >= 0) & (sS < 1) & (tT >= 0) & (tT < 1)
        if not inside.any(): continue
        z = a[2] + e1[2] * sS + e2[2] * tT
        th, tw = T.shape[:2]
        tu = np.clip(((u0 + (u1 - u0) * sS) / 16 * tw).astype(int), 0, tw - 1); tv = np.clip(((v0 + (v1 - v0) * tT) / 16 * th).astype(int), 0, th - 1)
        px = T[tv, tu]; sub = zb[y0:y1 + 1, x0:x1 + 1]
        mm = inside & (z < sub - 1e-4) & (px[..., 3] > 20); sub[mm] = z[mm]
        cc = col[y0:y1 + 1, x0:x1 + 1]; cc[mm, :3] = np.clip(px[mm, :3] * shade, 0, 255).astype(np.uint8); cc[mm, 3] = 255
    Image.fromarray(col).resize((size, size), Image.LANCZOS).save(out)
