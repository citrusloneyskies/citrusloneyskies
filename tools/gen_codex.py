#!/usr/bin/env python3
"""Assets for the Siege Codex: a 32x32 sprite of an iron-bound tome with the core's emblem, its item model and its
language entry (merged into tools/lang_extra.json so the other generators keep it).

Usage:  python3 tools/gen_codex.py
"""
import json
import os

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'lastbastion')
LANG = {'item.lastbastion.siege_codex': 'Siege Codex'}
S = 256


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def sprite(path):
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # pages (seen at the fore-edge) and the cover, drawn slightly turned
    d.polygon([(62, 58), (206, 40), (214, 214), (70, 230)], fill=(232, 220, 190, 255))
    for i in range(6):
        y = 70 + i * 26
        d.line([(196, y - 14), (206, y - 16)], fill=(180, 166, 136, 255), width=3)
    d.polygon([(40, 48), (190, 30), (198, 206), (48, 224)], fill=(70, 24, 22, 255))
    d.polygon([(52, 58), (180, 42), (186, 196), (58, 212)], fill=(104, 34, 28, 255))
    # spine
    d.polygon([(40, 48), (58, 46), (66, 222), (48, 224)], fill=(46, 16, 14, 255))
    for y in (70, 110, 150, 190):
        d.line([(42, y), (60, y - 2)], fill=(200, 160, 80, 255), width=5)
    # iron corners
    for (cx, cy) in ((180, 42), (186, 196), (58, 58), (58, 212)):
        d.polygon([(cx - 14, cy - 12), (cx + 12, cy - 14), (cx + 14, cy + 12), (cx - 12, cy + 14)], fill=(150, 150, 160, 255),
                  outline=(70, 70, 80, 255))
    # the core's emblem: a glowing gem in a gold ring with four spikes
    c = (120, 128)
    glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([c[0] - 34, c[1] - 34, c[0] + 34, c[1] + 34], fill=(110, 227, 255, 160))
    im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(10)))
    d.ellipse([c[0] - 30, c[1] - 30, c[0] + 30, c[1] + 30], outline=(216, 168, 64, 255), width=8)
    for dx, dy in ((0, -1), (1, 0), (0, 1), (-1, 0)):
        tip = (c[0] + dx * 52, c[1] + dy * 52)
        base1 = (c[0] + dx * 30 - dy * 10, c[1] + dy * 30 - dx * 10)
        base2 = (c[0] + dx * 30 + dy * 10, c[1] + dy * 30 + dx * 10)
        d.polygon([tip, base1, base2], fill=(216, 168, 64, 255))
    d.polygon([(c[0], c[1] - 18), (c[0] + 16, c[1]), (c[0], c[1] + 18), (c[0] - 16, c[1])], fill=(140, 236, 255, 255))
    d.polygon([(c[0], c[1] - 18), (c[0] + 16, c[1]), (c[0], c[1])], fill=(220, 250, 255, 255))
    # bookmark ribbon
    d.polygon([(150, 206), (166, 204), (170, 244), (160, 234), (152, 246)], fill=(200, 30, 36, 255))
    small = im.resize((32, 32), Image.LANCZOS)
    alpha = small.split()[3]
    out = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    out.paste(Image.new('RGBA', (32, 32), (20, 8, 6, 255)), (0, 0), alpha.filter(ImageFilter.MaxFilter(3)))
    out.alpha_composite(small)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)


def main():
    sprite(os.path.join(ROOT, 'textures', 'item', 'siege_codex.png'))
    write_json(os.path.join(ROOT, 'models', 'item', 'siege_codex.json'), {
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': 'lastbastion:item/siege_codex'},
    })
    extra_path = os.path.join(HERE, 'lang_extra.json')
    extra = json.load(open(extra_path)) if os.path.exists(extra_path) else {}
    extra.update(LANG)
    write_json(extra_path, dict(sorted(extra.items())))
    lang_path = os.path.join(ROOT, 'lang', 'en_us.json')
    lang = json.load(open(lang_path))
    lang.update(LANG)
    write_json(lang_path, dict(sorted(lang.items())))
    print('codex assets written')


if __name__ == '__main__':
    main()
