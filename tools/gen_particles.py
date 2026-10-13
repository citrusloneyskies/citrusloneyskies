#!/usr/bin/env python3
"""Generates the white, tintable sprites for the lastbastion:fx particle (one per FxStyle, in enum order).

Run from the repository root:  python3 tools/gen_particles.py
"""
import json
import math
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'lastbastion')
S = 32
STYLES = ['glow', 'spark', 'ember', 'smoke', 'shard', 'ring', 'soul', 'star', 'drop', 'leaf', 'rune', 'bolt', 'feather',
          'bubble', 'flake', 'skull', 'debris', 'flame']


def clamp(v, a=0.0, b=1.0):
    return max(a, min(b, v))


def smooth(a, b, x):
    t = clamp((x - a) / (b - a))
    return t * t * (3 - 2 * t)


def img_from(fn, seed=0):
    rnd = random.Random(seed)
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for y in range(S):
        for x in range(S):
            u = (x + 0.5) / S * 2 - 1
            v = (y + 0.5) / S * 2 - 1
            r = fn(u, v, rnd)
            if r is None:
                continue
            lum, a = r
            lum = int(clamp(lum) * 255)
            a = int(clamp(a) * 255)
            if a > 0:
                px[x, y] = (lum, lum, lum, a)
    return im


def noise2(seed):
    rnd = random.Random(seed)
    grid = [[rnd.random() for _ in range(9)] for _ in range(9)]

    def n(u, v):
        x = (u + 1) * 4
        y = (v + 1) * 4
        ix, iy = int(min(7, max(0, math.floor(x)))), int(min(7, max(0, math.floor(y))))
        fx, fy = x - ix, y - iy
        fx = fx * fx * (3 - 2 * fx)
        fy = fy * fy * (3 - 2 * fy)
        a = grid[iy][ix] * (1 - fx) + grid[iy][ix + 1] * fx
        b = grid[iy + 1][ix] * (1 - fx) + grid[iy + 1][ix + 1] * fx
        return a * (1 - fy) + b * fy
    return n


def glow(u, v, r):
    d = math.hypot(u, v)
    a = math.exp(-d * d * 4.5)
    return (1.0, a * smooth(1.0, 0.85, d) if d < 1 else 0)


def spark(u, v, r):
    # bright head at u=-1 (left), fading tail to the right
    along = (u + 1) / 2
    w = 0.32 * (1 - along * 0.7)
    a = math.exp(-(v * v) / (w * w * 0.5)) * (1 - along) ** 0.8
    return (1.0, a)


def ember(u, v, r):
    d = math.hypot(u, v)
    core = smooth(0.35, 0.15, d)
    halo = math.exp(-d * d * 6) * 0.6
    return (1.0, clamp(core + halo))


smoke_n = noise2(11)


def smoke(u, v, r):
    d = math.hypot(u, v)
    n = smoke_n(u * 0.9, v * 0.9)
    edge = 0.75 + (n - 0.5) * 0.5
    a = smooth(edge, edge - 0.45, d) * (0.65 + 0.35 * n)
    lum = 0.75 + 0.25 * n - max(0, v) * 0.15
    return (lum, a)


def shard(u, v, r):
    # elongated diamond with a lit facet
    x, y = u * 1.0, v * 1.0
    inside = abs(x) * 1.9 + abs(y) * 1.05 < 0.95
    if not inside:
        return None
    lum = 0.95 if x < 0 else 0.7
    if y < 0 and x < 0:
        lum = 1.0
    edge = abs(x) * 1.9 + abs(y) * 1.05 > 0.8
    return (lum - (0.15 if edge else 0), 1.0)


def ring(u, v, r):
    d = math.hypot(u, v)
    a = math.exp(-((d - 0.72) ** 2) / 0.008)
    return (1.0, a)


soul_n = noise2(23)


def soul(u, v, r):
    # head at bottom, tail streaming upward with a wave
    y = (v + 1) / 2  # 0 top .. 1 bottom
    cx = math.sin(y * 7) * 0.12 * (1 - y)
    w = 0.12 + 0.3 * y ** 1.5
    if y > 0.85:
        w = max(0, 0.36 - (y - 0.85) * 2.4)
    d = abs(u - cx) / max(0.01, w)
    a = smooth(1.0, 0.4, d) * smooth(0.0, 0.5, y)
    return (1.0, a * (0.6 + 0.4 * soul_n(u, v)))


def star(u, v, r):
    au, av = abs(u), abs(v)
    a = max(math.exp(-(av * av) / 0.004) * (1 - au), math.exp(-(au * au) / 0.004) * (1 - av))
    d = math.hypot(u, v)
    a = max(a, math.exp(-d * d * 18))
    return (1.0, clamp(a * 1.2))


def drop(u, v, r):
    # teardrop pointing up
    y = v
    if y < -0.75 or y > 0.8:
        return None
    w = 0.55 * math.sqrt(max(0, 1 - ((y - 0.25) / 0.55) ** 2)) if y > -0.1 else 0.55 * (y + 0.75) / 0.65 * 0.55 / 0.55 * 0.8
    if abs(u) > w:
        return None
    lum = 0.8 + 0.2 * (-u)
    if -0.3 < u < -0.1 and 0.05 < y < 0.35:
        lum = 1.0
    return (lum, 0.95)


def leaf(u, v, r):
    # pointed oval rotated 30 degrees with a mid vein
    c, s = math.cos(0.5), math.sin(0.5)
    x, y = u * c - v * s, u * s + v * c
    w = 0.42 * (1 - y * y) if abs(y) < 0.95 else 0
    if abs(x) > w:
        return None
    lum = 0.85 + 0.15 * (x / max(w, 0.01))
    if abs(x) < 0.05:
        lum = 1.0
    if abs((y * 6) % 1 - abs(x) * 3) < 0.15 and abs(x) > 0.08:
        lum -= 0.12
    return (lum, 1.0)


def rune(u, v, r):
    # a single angular glyph inside a circle
    d = math.hypot(u, v)
    a = 0
    if abs(d - 0.8) < 0.07:
        a = 1
    lines = [((0, -0.6), (0, 0.6)), ((0, -0.2), (0.4, -0.55)), ((0, 0.1), (-0.4, -0.25)), ((0, 0.6), (0.35, 0.3))]
    for (x0, y0), (x1, y1) in lines:
        dx, dy = x1 - x0, y1 - y0
        t = clamp(((u - x0) * dx + (v - y0) * dy) / (dx * dx + dy * dy))
        if math.hypot(u - (x0 + dx * t), v - (y0 + dy * t)) < 0.07:
            a = 1
    glow_a = math.exp(-d * d * 3) * 0.25
    return (1.0, max(a, glow_a)) if max(a, glow_a) > 0.01 else None


def bolt(u, v, r):
    pts = [(-0.1, -0.95), (0.25, -0.35), (-0.15, -0.05), (0.3, 0.45), (0.0, 0.95)]
    best = 9
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        dx, dy = x1 - x0, y1 - y0
        t = clamp(((u - x0) * dx + (v - y0) * dy) / (dx * dx + dy * dy))
        best = min(best, math.hypot(u - (x0 + dx * t), v - (y0 + dy * t)))
    a = smooth(0.16, 0.05, best) + math.exp(-best * best / 0.03) * 0.5
    return (1.0, clamp(a)) if a > 0.01 else None


def feather(u, v, r):
    c, s = math.cos(-0.6), math.sin(-0.6)
    x, y = u * c - v * s, u * s + v * c
    if abs(y) > 0.95:
        return None
    w = 0.32 * math.sqrt(max(0, 1 - y * y)) * (1.0 if y < 0.6 else (0.95 - y) / 0.35)
    if abs(x) > w + (0.03 if y > 0.6 else 0):
        return None
    lum = 0.9 - abs(x) * 0.6
    if abs(x) < 0.035:
        lum = 1.0
    if (y * 10 + abs(x) * 8) % 1 < 0.2:
        lum -= 0.1
    return (lum, 1.0)


def bubble(u, v, r):
    d = math.hypot(u, v)
    if d > 0.8:
        return None
    a = 0.25 + smooth(0.55, 0.78, d) * 0.7
    lum = 0.9
    if math.hypot(u + 0.3, v + 0.3) < 0.18:
        a, lum = 1.0, 1.0
    return (lum, a)


def flake(u, v, r):
    best = 9
    for k in range(6):
        ang = k * math.pi / 3
        dx, dy = math.cos(ang), math.sin(ang)
        t = clamp(u * dx + v * dy, 0, 0.85)
        best = min(best, math.hypot(u - dx * t, v - dy * t))
        for side in (-1, 1):
            bx, by = dx * 0.5, dy * 0.5
            ex, ey = bx + math.cos(ang + side * 0.8) * 0.25, by + math.sin(ang + side * 0.8) * 0.25
            ddx, ddy = ex - bx, ey - by
            tt = clamp(((u - bx) * ddx + (v - by) * ddy) / (ddx * ddx + ddy * ddy))
            best = min(best, math.hypot(u - (bx + ddx * tt), v - (by + ddy * tt)))
    a = smooth(0.1, 0.03, best)
    return (1.0, a) if a > 0.01 else None


def skull(u, v, r):
    head = (u / 0.62) ** 2 + ((v + 0.15) / 0.55) ** 2 < 1
    jaw = abs(u) < 0.32 and 0.25 < v < 0.62
    if not (head or jaw):
        g = math.exp(-(u * u + v * v) * 2.5) * 0.25
        return (1.0, g) if g > 0.02 else None
    eye = ((abs(u) - 0.24) / 0.15) ** 2 + ((v + 0.08) / 0.16) ** 2 < 1
    nose = abs(u) < 0.06 and 0.12 < v < 0.25
    teeth = jaw and v > 0.42 and int((u + 1) * 12) % 2 == 0
    if eye or nose or teeth:
        return (0.15, 0.9)
    return (1.0, 1.0)


deb_n = noise2(41)


def debris(u, v, r):
    ang = math.atan2(v, u)
    rad = 0.62 + 0.22 * math.sin(ang * 3 + 1) + 0.1 * math.sin(ang * 5)
    d = math.hypot(u, v)
    if d > rad:
        return None
    lum = 0.55 + 0.35 * deb_n(u, v) + (0.15 if u + v < -0.2 else 0)
    if d > rad - 0.1:
        lum -= 0.15
    return (lum, 1.0)


def flame(u, v, r):
    y = (v + 1) / 2  # 0 top .. 1 bottom
    w = 0.55 * math.sin(clamp(y, 0, 1) * math.pi * 0.85) ** 0.8 * (0.5 + 0.5 * y)
    cx = math.sin(y * 5) * 0.06
    d = abs(u - cx) / max(w, 0.01)
    if d > 1 or y < 0.03:
        return None
    a = smooth(1.0, 0.55, d) * smooth(0.0, 0.25, y)
    lum = 1.0
    return (lum, a)


def main():
    out = os.path.join(ROOT, 'textures', 'particle')
    os.makedirs(out, exist_ok=True)
    fns = {'glow': glow, 'spark': spark, 'ember': ember, 'smoke': smoke, 'shard': shard, 'ring': ring, 'soul': soul, 'star': star,
           'drop': drop, 'leaf': leaf, 'rune': rune, 'bolt': bolt, 'feather': feather, 'bubble': bubble, 'flake': flake,
           'skull': skull, 'debris': debris, 'flame': flame}
    for i, name in enumerate(STYLES):
        img_from(fns[name], i).save(os.path.join(out, 'fx_' + name + '.png'))
    pdir = os.path.join(ROOT, 'particles')
    os.makedirs(pdir, exist_ok=True)
    with open(os.path.join(pdir, 'fx.json'), 'w') as f:
        json.dump({'textures': ['lastbastion:fx_' + n for n in STYLES]}, f, indent=2)
    print('wrote', len(STYLES), 'particle sprites')


if __name__ == '__main__':
    main()
