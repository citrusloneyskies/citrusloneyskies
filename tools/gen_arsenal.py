#!/usr/bin/env python3
"""Generates the arsenal's item assets.

* 3D weapons use a ``builtin/entity`` item model (drawn by the weapon renderer) plus an ``item/icon/<id>``
  sprite model for inventories.
* Icon sprites are baked from the 3D models by the offline previewer (tools/preview) and downsampled
  here into 32x32 item textures with a dark outline.
* Language entries are kept in tools/lang_extra.json and merged into en_us.json (gen_assets.py merges
  the same file, so regenerating everything keeps them).

Usage:  python3 tools/gen_arsenal.py [icon_render_dir]
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'lastbastion')
MOD = 'lastbastion'

MELEE = [
    ('militia_longsword', 'Militia Longsword'),
    ('frostfang', 'Frostfang'),
    ('bonecleaver', 'Bonecleaver'),
    ('voidedge', 'Voidedge'),
    ('stormpiercer', 'Stormpiercer'),
    ('earthshaker', 'Earthshaker'),
    ('dawnbreaker', 'Dawnbreaker'),
    ('soulreaper', 'Soulreaper'),
]


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def lang_extra():
    p = os.path.join(HERE, 'lang_extra.json')
    return json.load(open(p)) if os.path.exists(p) else {}


def main():
    extra = lang_extra()
    for wid, name in MELEE:
        write_json(os.path.join(ROOT, 'models', 'item', wid + '.json'), {
            'parent': 'minecraft:builtin/entity',
            'gui_light': 'front',
            'textures': {'particle': '%s:item/%s' % (MOD, wid)},
        })
        write_json(os.path.join(ROOT, 'models', 'item', 'icon', wid + '.json'), {
            'parent': 'minecraft:item/generated',
            'textures': {'layer0': '%s:item/%s' % (MOD, wid)},
        })
        extra['item.%s.%s' % (MOD, wid)] = name
    write_json(os.path.join(HERE, 'lang_extra.json'), dict(sorted(extra.items())))
    lang_path = os.path.join(ROOT, 'lang', 'en_us.json')
    lang = json.load(open(lang_path))
    lang.update(extra)
    write_json(lang_path, dict(sorted(lang.items())))
    if len(sys.argv) > 1:
        sys.path.insert(0, os.path.join(HERE, 'preview'))
        import icons
        for wid, _ in MELEE:
            src = os.path.join(sys.argv[1], wid + '_big.png')
            if os.path.exists(src):
                icons.make(src, os.path.join(ROOT, 'textures', 'item', wid + '.png'))
                print('icon', wid)
    print('arsenal assets written:', len(MELEE), 'melee weapons')


if __name__ == '__main__':
    main()
