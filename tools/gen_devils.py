#!/usr/bin/env python3
"""Assets for the Devils: a 32x32 infernal pact sprite per devil (a burnt contract with the devil's emblem
branded into it and a blood seal), the item models, and the language entries for the devils, their pacts and
the hellspawn they call up. Entries are merged into tools/lang_extra.json so the other generators keep them.

Usage:  python3 tools/gen_devils.py
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'lastbastion')
MOD = 'lastbastion'
S = 256

DEVILS = [
    # id, title, short name, emblem colour (r, g, b)
    ('lucifer', 'Lucifer, King of Hell', 'Lucifer', (225, 30, 36)),
    ('beelzebub', 'Beelzebub, Lord of the Flies', 'Beelzebub', (150, 205, 60)),
    ('abaddon', 'Abaddon, the Destroyer', 'Abaddon', (160, 90, 255)),
    ('leviathan', 'Leviathan, Serpent of the Abyss', 'Leviathan', (50, 200, 215)),
    ('moloch', 'Moloch, the Furnace King', 'Moloch', (255, 130, 30)),
]

LANG = {
    'entity.lastbastion.imp': 'Imp',
    'entity.lastbastion.hellhound': 'Hellhound',
    'entity.lastbastion.fiend': 'Fiend',
}
for did, title, short, _ in DEVILS:
    LANG['entity.lastbastion.' + did] = title
    LANG['item.lastbastion.pact_' + did] = 'Infernal Pact: ' + short


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def parchment(d, rnd):
    """A slightly skewed sheet of old vellum with scorched, ragged edges and two rolled ends."""
    x0, y0, x1, y1 = 40, 34, 216, 222
    pts = []
    for i in range(24):
        t = i / 24
        pts.append((x0 + (x1 - x0) * t, y0 + rnd.uniform(-6, 6)))
    for i in range(24):
        t = i / 24
        pts.append((x1 + rnd.uniform(-8, 4), y0 + (y1 - y0) * t))
    for i in range(24):
        t = i / 24
        pts.append((x1 - (x1 - x0) * t, y1 + rnd.uniform(-6, 6)))
    for i in range(24):
        t = i / 24
        pts.append((x0 + rnd.uniform(-4, 8), y1 - (y1 - y0) * t))
    d.polygon(pts, fill=(58, 30, 18, 255))
    inner = [(x + (128 - x) * 0.07, y + (128 - y) * 0.07) for x, y in pts]
    d.polygon(inner, fill=(196, 160, 112, 255))
    inner2 = [(x + (128 - x) * 0.16, y + (128 - y) * 0.16) for x, y in pts]
    d.polygon(inner2, fill=(222, 192, 140, 255))
    # rolled ends
    for y in (y0 - 4, y1 - 10):
        d.rounded_rectangle([x0 - 14, y, x1 + 14, y + 16], 8, fill=(170, 130, 86, 255), outline=(70, 40, 22, 255), width=4)
    # lines of infernal script
    for i in range(6):
        yy = 74 + i * 22
        xa = 70 + rnd.randint(0, 10)
        xb = 186 - rnd.randint(0, 24)
        d.line([xa, yy, xb, yy], fill=(110, 70, 50, 255), width=4)


def emblem(d, kind, col):
    c = 128
    dark = tuple(int(v * 0.35) for v in col) + (255,)
    colA = col + (255,)
    if kind == 'lucifer':
        # a horned crown of thorns over a flame
        d.polygon([(c - 40, c + 30), (c - 30, c - 10), (c - 14, c + 10), (c, c - 26), (c + 14, c + 10), (c + 30, c - 10), (c + 40, c + 30)], fill=dark)
        d.polygon([(c - 34, c + 24), (c - 27, c - 2), (c - 13, c + 16), (c, c - 16), (c + 13, c + 16), (c + 27, c - 2), (c + 34, c + 24)], fill=colA)
        for s in (-1, 1):
            d.line([c + s * 40, c + 4, c + s * 62, c - 30, c + s * 52, c - 50], fill=dark, width=12)
            d.line([c + s * 40, c + 4, c + s * 62, c - 30, c + s * 52, c - 50], fill=colA, width=6)
        d.polygon([(c - 12, c + 52), (c, c + 26), (c + 12, c + 52)], fill=(255, 170, 60, 255))
    elif kind == 'beelzebub':
        # a fly: body, two big eyes, veined wings
        for s in (-1, 1):
            d.ellipse([c + s * 8 - 34 + (s > 0) * 0, c - 44, c + s * 8 + 34, c - 4], fill=(230, 240, 220, 200), outline=dark, width=4)
        d.ellipse([c - 18, c - 20, c + 18, c + 50], fill=dark)
        d.ellipse([c - 14, c - 16, c + 14, c + 44], fill=colA)
        for i in range(3):
            d.line([c - 14, c + i * 14, c + 14, c + i * 14], fill=dark, width=4)
        d.ellipse([c - 22, c - 40, c + 22, c - 10], fill=dark)
        for s in (-1, 1):
            d.ellipse([c + s * 12 - 10, c - 38, c + s * 12 + 10, c - 18], fill=(220, 30, 20, 255))
    elif kind == 'abaddon':
        # the Key of the Abyss
        d.ellipse([c - 30, c - 64, c + 30, c - 4], outline=dark, width=18)
        d.ellipse([c - 30, c - 64, c + 30, c - 4], outline=colA, width=8)
        d.rectangle([c - 8, c - 8, c + 8, c + 56], fill=dark)
        d.rectangle([c - 4, c - 8, c + 4, c + 56], fill=colA)
        for yy, w in ((c + 30, 28), (c + 46, 20)):
            d.rectangle([c, yy - 6, c + w, yy + 6], fill=dark)
            d.rectangle([c, yy - 3, c + w - 3, yy + 3], fill=colA)
    elif kind == 'leviathan':
        # a coiled serpent with a burning eye
        pts = []
        for i in range(60):
            t = i / 59
            a = t * math.tau * 1.25
            r = 46 - 30 * t
            pts.append((c + math.cos(a) * r, c + 10 + math.sin(a) * r * 0.8))
        d.line(pts, fill=dark, width=22, joint='curve')
        d.line(pts, fill=colA, width=12, joint='curve')
        hx, hy = pts[0]
        d.ellipse([hx - 16, hy - 12, hx + 16, hy + 12], fill=dark)
        d.ellipse([hx - 12, hy - 8, hx + 12, hy + 8], fill=colA)
        d.ellipse([hx - 2, hy - 6, hx + 6, hy + 2], fill=(255, 255, 220, 255))
    else:
        # Moloch: bull horns over a furnace mouth
        for s in (-1, 1):
            d.line([c + s * 18, c - 8, c + s * 50, c - 20, c + s * 54, c - 52], fill=(40, 20, 10, 255), width=20)
            d.line([c + s * 18, c - 8, c + s * 50, c - 20, c + s * 54, c - 52], fill=(200, 160, 110, 255), width=9)
        d.rounded_rectangle([c - 30, c - 16, c + 30, c + 46], 10, fill=dark)
        d.rounded_rectangle([c - 22, c - 4, c + 22, c + 38], 6, fill=(255, 190, 70, 255))
        for i in range(4):
            x = c - 16 + i * 11
            d.line([x, c - 4, x, c + 38], fill=dark, width=4)


def seal(d, rnd):
    cx, cy = 186, 196
    pts = []
    for i in range(18):
        a = i / 18 * math.tau
        r = 26 + rnd.uniform(-4, 4)
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    d.polygon(pts, fill=(96, 6, 10, 255))
    d.ellipse([cx - 18, cy - 18, cx + 18, cy + 18], fill=(170, 16, 20, 255))
    d.ellipse([cx - 9, cy - 9, cx + 9, cy + 9], outline=(96, 6, 10, 255), width=4)
    d.line([cx - 30, cy + 20, cx - 36, cy + 42], fill=(140, 10, 14, 255), width=6)


def pact_sprite(did, col, path):
    rnd = random.Random(did)
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    parchment(d, rnd)
    # the emblem glows: blurred halo of the devil's colour under the brand
    glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    emblem(gd, did, col)
    halo = glow.filter(ImageFilter.GaussianBlur(10))
    im.alpha_composite(halo)
    im.alpha_composite(glow)
    seal(d, rnd)
    small = im.resize((32, 32), Image.LANCZOS)
    alpha = small.split()[3]
    grown = alpha.filter(ImageFilter.MaxFilter(3))
    out = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    out.paste(Image.new('RGBA', (32, 32), (24, 8, 6, 255)), (0, 0), grown)
    out.alpha_composite(small)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)


def main():
    for did, _, _, col in DEVILS:
        n = 'pact_' + did
        pact_sprite(did, col, os.path.join(ROOT, 'textures', 'item', n + '.png'))
        write_json(os.path.join(ROOT, 'models', 'item', n + '.json'), {
            'parent': 'minecraft:item/generated',
            'textures': {'layer0': '%s:item/%s' % (MOD, n)},
        })
    extra_path = os.path.join(HERE, 'lang_extra.json')
    extra = json.load(open(extra_path)) if os.path.exists(extra_path) else {}
    extra.update(LANG)
    write_json(extra_path, dict(sorted(extra.items())))
    lang_path = os.path.join(ROOT, 'lang', 'en_us.json')
    lang = json.load(open(lang_path))
    lang.update(LANG)
    write_json(lang_path, dict(sorted(lang.items())))
    print('devil assets written')


if __name__ == '__main__':
    main()
