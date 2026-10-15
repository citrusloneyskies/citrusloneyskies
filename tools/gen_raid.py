#!/usr/bin/env python3
"""Assets for the raid systems: the Warforged Token sprite and item model, and the language entries for the
war-event entities (supply crate, corruption rift). Entries are merged into tools/lang_extra.json so the other
generators keep them.

Usage:  python3 tools/gen_raid.py
"""
import json
import math
import os

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'lastbastion')
MOD = 'lastbastion'

LANG = {
    'item.lastbastion.warforged_token': 'Warforged Token',
    'entity.lastbastion.supply_crate': 'Supply Drop',
    'entity.lastbastion.corruption_rift': 'Corruption Rift',
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def token_sprite(path):
    """A hammered gold coin: rim, a ring of violet runes, crossed swords over an anvil."""
    S = 256
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = S / 2
    r = S * 0.46
    # coin body with a radial gold gradient
    for i in range(60, 0, -1):
        t = i / 60
        rr = r * t
        g = (int(150 + 105 * (1 - t) ** 0.7), int(100 + 110 * (1 - t) ** 0.7), int(28 + 60 * (1 - t)))
        d.ellipse([c - rr, c - rr, c + rr, c + rr], fill=g + (255,))
    # rim
    d.ellipse([c - r, c - r, c + r, c + r], outline=(92, 58, 14, 255), width=10)
    d.ellipse([c - r * 0.86, c - r * 0.86, c + r * 0.86, c + r * 0.86], outline=(120, 76, 20, 255), width=6)
    # runes around the rim
    for k in range(12):
        a = k / 12 * math.tau
        x, y = c + math.cos(a) * r * 0.93, c + math.sin(a) * r * 0.93
        d.rectangle([x - 5, y - 5, x + 5, y + 5], fill=(196, 120, 255, 255))
    # anvil
    ax, ay = c, c + r * 0.34
    d.polygon([(ax - 52, ay - 14), (ax + 60, ay - 14), (ax + 40, ay + 6), (ax - 34, ay + 6)], fill=(60, 52, 58, 255))
    d.rectangle([ax - 16, ay + 6, ax + 16, ay + 28], fill=(52, 44, 50, 255))
    d.rectangle([ax - 30, ay + 26, ax + 30, ay + 36], fill=(60, 52, 58, 255))
    # crossed swords
    for sgn in (-1, 1):
        x0, y0 = c - sgn * 62, c + 46
        x1, y1 = c + sgn * 54, c - 70
        d.line([x0, y0, x1, y1], fill=(220, 228, 236, 255), width=14)
        d.line([x0, y0, x1, y1], fill=(250, 252, 255, 255), width=4)
        # cross-guard perpendicular to the blade, a third of the way up from the pommel
        bx, by = x1 - x0, y1 - y0
        ln = math.hypot(bx, by)
        ux, uy = bx / ln, by / ln
        gx, gy = x0 + ux * 34, y0 + uy * 34
        d.line([gx - uy * 20, gy + ux * 20, gx + uy * 20, gy - ux * 20], fill=(150, 92, 24, 255), width=10)
        d.ellipse([x0 - 9, y0 - 9, x0 + 9, y0 + 9], fill=(200, 140, 40, 255))
    # glint: a bright arc on the upper-left rim
    d.arc([c - r * 0.8, c - r * 0.8, c + r * 0.8, c + r * 0.8], 200, 250, fill=(255, 246, 200, 255), width=8)
    small = im.resize((32, 32), Image.LANCZOS)
    # dark outline for readability on any slot background
    alpha = small.split()[3]
    grown = alpha.filter(ImageFilter.MaxFilter(3))
    out = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    edge = Image.new('RGBA', (32, 32), (34, 20, 6, 255))
    out.paste(edge, (0, 0), grown)
    out.alpha_composite(small)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)


def main():
    token_sprite(os.path.join(ROOT, 'textures', 'item', 'warforged_token.png'))
    write_json(os.path.join(ROOT, 'models', 'item', 'warforged_token.json'), {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': '%s:item/warforged_token' % MOD},
    })
    extra_path = os.path.join(HERE, 'lang_extra.json')
    extra = json.load(open(extra_path)) if os.path.exists(extra_path) else {}
    extra.update(LANG)
    write_json(extra_path, dict(sorted(extra.items())))
    lang_path = os.path.join(ROOT, 'lang', 'en_us.json')
    lang = json.load(open(lang_path))
    lang.update(LANG)
    write_json(lang_path, dict(sorted(lang.items())))
    print('raid assets written')


if __name__ == '__main__':
    main()
