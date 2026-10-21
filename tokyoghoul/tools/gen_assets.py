#!/usr/bin/env python3
"""Generates Tokyo Ghoul Craft's static assets: pixel-art item icons, item models, the English lang
file, damage types and recipes. Entity, kagune and quinque models are procedural (built in Java and
painted at runtime), so they need no files here.

Character and spawn-egg names are read from Characters.java so the roster stays the single source of
truth. Run from the tokyoghoul/ directory:  python3 tools/gen_assets.py
"""
import json
import os
import random
import re

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources')
MOD = 'tokyoghoul'
A = os.path.join(ROOT, 'assets', MOD)
D = os.path.join(ROOT, 'data', MOD)
JAVA = os.path.join(HERE, '..', 'src', 'main', 'java', 'com', 'tokyoghoul')


def path(*p):
    full = os.path.join(*p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    return full


def write_json(p, obj):
    with open(path(p), 'w') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def rgb(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


def shade(h, f):
    r, g, b, _ = rgb(h)
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))), max(0, min(255, int(b * f))), 255)


class Canvas:
    def __init__(self, seed=0):
        self.img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)

    def set(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[x, y] = c if isinstance(c, tuple) else rgb(c)

    def rect(self, x0, y0, x1, y1, c, noise=0.0):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if noise:
                    self.set(x, y, shade(c, 1 + (self.rnd.random() - 0.5) * 2 * noise))
                else:
                    self.set(x, y, c)

    def ellipse(self, cx, cy, rx, ry, c, noise=0.0):
        for y in range(16):
            for x in range(16):
                if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                    self.set(x, y, shade(c, 1 + (self.rnd.random() - 0.5) * 2 * noise) if noise else c)

    def line(self, x0, y0, x1, y1, c):
        n = int(max(abs(x1 - x0), abs(y1 - y0), 1))
        for i in range(n + 1):
            self.set(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)

    def outline(self, c=0x140A0A):
        src = self.img.copy().load()
        for y in range(16):
            for x in range(16):
                if src[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and src[x + dx, y + dy][3] > 0
                                             for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    self.set(x, y, c)

    def save(self, name):
        self.img.save(path(A, 'textures', 'item', name + '.png'))


# ---------------------------------------------------------------------------- icons

def book():
    c = Canvas(1)
    c.rect(3, 2, 12, 13, 0x6A0F1A, 0.08)
    c.rect(3, 2, 4, 13, 0x3A0810)
    c.rect(12, 3, 12, 13, 0xE8E0D0)
    c.ellipse(8.5, 7.5, 2.6, 1.6, 0x0A0A0A)
    c.ellipse(8.5, 7.5, 1.0, 1.0, 0xFF2020)
    c.line(6, 11, 10, 11, 0xC8A060)
    c.outline()
    c.save('ghoul_registry')


def thermos():
    c = Canvas(2)
    c.rect(5, 3, 10, 14, 0x6A4A2E, 0.1)
    c.rect(5, 2, 10, 3, 0x2A2A2A)
    c.rect(6, 1, 9, 1, 0x1A1A1A)
    c.rect(5, 7, 10, 9, 0xE8D8B8)
    c.set(7, 8, 0x6A4A2E)
    c.set(8, 8, 0x6A4A2E)
    c.outline()
    c.save('anteiku_thermos')


def coffee():
    c = Canvas(3)
    c.rect(4, 6, 10, 13, 0xF0EEE8)
    c.rect(5, 6, 9, 7, 0x3A2010)
    c.rect(11, 8, 12, 10, 0xF0EEE8)
    c.rect(3, 13, 12, 14, 0xD8D4CC)
    c.set(6, 3, 0xB8B8B8)
    c.set(7, 2, 0xB8B8B8)
    c.set(8, 4, 0xB8B8B8)
    c.outline()
    c.save('anteiku_coffee')


def flesh():
    c = Canvas(4)
    c.ellipse(8, 9, 5.5, 4.2, 0xB03A3A, 0.12)
    c.ellipse(7, 8, 3, 2, 0xD86A6A, 0.1)
    c.rect(11, 3, 12, 8, 0xEDE6D6)
    c.rect(10, 2, 13, 3, 0xEDE6D6)
    c.outline()
    c.save('human_flesh')


def rc_cells():
    c = Canvas(5)
    c.rect(6, 4, 9, 13, 0xD8E0E8)
    c.rect(6, 7, 9, 13, 0xD01020, 0.1)
    c.rect(5, 3, 10, 4, 0x3A3A3A)
    c.set(7, 9, 0xFF6060)
    c.outline()
    c.save('rc_cells')


def suppressant():
    c = Canvas(6)
    c.line(3, 12, 11, 4, 0xD8E0E8)
    c.line(4, 12, 12, 4, 0xD8E0E8)
    c.line(5, 11, 10, 6, 0x4A8AD0)
    c.line(12, 3, 14, 1, 0xA0A0A0)
    c.line(2, 13, 3, 14, 0x606060)
    c.outline()
    c.save('rc_suppressant')


KAKUHOU = {'ukaku': 0x8E3AD8, 'koukaku': 0x4060C0, 'rinkaku': 0xC0102A, 'bikaku': 0xD06020}


def kakuhou(t, col):
    c = Canvas(hash(t) & 0xFFFF)
    c.ellipse(8, 8.5, 5, 4.5, shade(col, 0.6), 0.0)
    c.ellipse(7.5, 8, 3.2, 2.8, col, 0.12)
    c.ellipse(6.5, 7, 1.2, 1, 0xFFFFFF)
    for i in range(4):
        c.line(8, 8, 8 + c.rnd.randint(-5, 5), 8 + c.rnd.randint(-5, 5), shade(col, 1.2))
    c.outline()
    c.save('kakuhou_' + t)


MASKS = {
    'mask_kaneki': (0x121214, 0xE8E2D6, 'teeth'),
    'mask_rabbit': (0x2A2140, 0x9B7BD8, 'ears'),
    'mask_gourmet': (0xE8E0F0, 0x6A3AB0, 'moon'),
    'mask_nishiki': (0x2A2D52, 0x8A8FD0, 'lower'),
    'mask_crow': (0x141418, 0x6A6A70, 'beak'),
    'mask_owl': (0x2A2A2E, 0xD8D0B8, 'owl'),
    'mask_black_rabbit': (0x101014, 0xC0C0D0, 'ears'),
    'mask_jason': (0xF0EEE6, 0x8A1A1A, 'holes'),
    'mask_tatara': (0xA01818, 0x2A0A0A, 'lower'),
    'mask_bandage': (0xD8D0C0, 0x7A1A2A, 'wrap'),
    'mask_clown': (0xF0F0F0, 0xC23B8E, 'grin'),
    'mask_hannya': (0x8A1A1A, 0xE8E8E8, 'horns'),
}


def mask(name, base, acc, style):
    c = Canvas(hash(name) & 0xFFFF)
    if style == 'lower':
        c.ellipse(8, 10, 5.5, 3.5, base, 0.06)
    else:
        c.ellipse(8, 8.5, 5.5, 6, base, 0.06)
    eye = 0x0A0A0A
    if style not in ('lower',):
        c.rect(5, 7, 6, 8, eye)
        c.rect(10, 7, 11, 8, eye)
    if style == 'teeth':
        c.rect(5, 11, 11, 12, acc)
        for x in range(5, 12, 2):
            c.set(x, 11, eye)
        c.rect(4, 6, 7, 9, 0x050505)
        c.set(10, 7, 0xFF2020)
    elif style == 'ears':
        c.rect(5, 0, 6, 4, base)
        c.rect(10, 0, 11, 4, base)
        c.set(5, 1, acc)
        c.set(10, 1, acc)
    elif style == 'moon':
        c.ellipse(10, 8, 3, 4, acc)
        c.ellipse(11, 8, 2.4, 3.4, base)
    elif style == 'lower':
        for x in range(4, 13, 2):
            c.set(x, 10, acc)
    elif style == 'beak':
        c.rect(7, 9, 9, 11, acc)
        c.set(8, 12, acc)
    elif style == 'owl':
        c.set(8, 10, acc)
        c.set(8, 11, acc)
        c.rect(3, 2, 4, 4, base)
        c.rect(12, 2, 13, 4, base)
    elif style == 'holes':
        for (x, y) in ((6, 11), (8, 12), (10, 11), (7, 4), (9, 4)):
            c.set(x, y, 0x5A5A5A)
        c.set(5, 5, acc)
        c.set(11, 5, acc)
    elif style == 'wrap':
        for y in range(3, 15, 2):
            c.line(3, y, 13, y - 1, shade(base, 0.8))
        c.rect(5, 7, 6, 8, eye)
    elif style == 'grin':
        c.line(4, 10, 8, 12, acc)
        c.line(8, 12, 12, 10, acc)
        c.set(5, 6, acc)
    elif style == 'horns':
        c.line(4, 4, 2, 0, acc)
        c.line(12, 4, 14, 0, acc)
        c.rect(5, 11, 11, 11, acc)
    c.outline()
    c.save(name)


def coat():
    c = Canvas(9)
    c.rect(3, 2, 12, 14, 0xE8E4DA, 0.05)
    c.rect(1, 3, 3, 10, 0xE8E4DA, 0.05)
    c.rect(12, 3, 14, 10, 0xE8E4DA, 0.05)
    c.rect(7, 2, 8, 14, 0xC8C4BA)
    c.rect(6, 2, 9, 3, 0x1A1A1A)
    c.outline(0x3A3A3A)
    c.save('ccg_coat')


# ---------------------------------------------------------------------------- roster

def roster():
    src = open(os.path.join(JAVA, 'data', 'Characters.java')).read()
    return re.findall(r'new CharacterDef\("([a-z0-9_]+)", "([^"]+)"', src)


QUINQUES = re.findall(r'^\s+[A-Z0-9]+\("([a-z0-9_]+)", "([^"]+)"', open(os.path.join(JAVA, 'item', 'QuinqueKind.java')).read(), re.M)


def models(chars):
    gen = ['ghoul_registry', 'anteiku_thermos', 'anteiku_coffee', 'human_flesh', 'rc_cells', 'rc_suppressant', 'ccg_coat']
    gen += ['kakuhou_' + t for t in KAKUHOU] + list(MASKS)
    for g in gen:
        write_json(os.path.join(A, 'models', 'item', g + '.json'), {'parent': 'item/generated', 'textures': {'layer0': f'{MOD}:item/{g}'}})
    for qid, _ in QUINQUES:
        write_json(os.path.join(A, 'models', 'item', qid + '.json'), {
            'parent': 'builtin/entity',
            'gui_light': 'front',
            'display': {
                'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                'firstperson_righthand': {'rotation': [0, -10, 25], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                'firstperson_lefthand': {'rotation': [0, 10, -25], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                'ground': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
            }})
    for cid, _ in chars:
        write_json(os.path.join(A, 'models', 'item', cid + '_spawn_egg.json'), {'parent': 'item/template_spawn_egg'})


def lang(chars):
    L = {
        'itemGroup.tokyoghoul': 'Tokyo Ghoul',
        'itemGroup.tokyoghoul.characters': 'Tokyo Ghoul: Characters',
        'item.tokyoghoul.ghoul_registry': 'Ghoul Registry',
        'item.tokyoghoul.anteiku_thermos': 'Anteiku Thermos',
        'item.tokyoghoul.anteiku_coffee': 'Anteiku Coffee',
        'item.tokyoghoul.human_flesh': 'Human Flesh',
        'item.tokyoghoul.rc_cells': 'Rc Cell Pack',
        'item.tokyoghoul.rc_suppressant': 'Rc Suppressant',
        'item.tokyoghoul.ccg_coat': 'CCG Investigator Coat',
        'item.tokyoghoul.kakuhou_ukaku': 'Ukaku Kakuhou',
        'item.tokyoghoul.kakuhou_koukaku': 'Koukaku Kakuhou',
        'item.tokyoghoul.kakuhou_rinkaku': 'Rinkaku Kakuhou',
        'item.tokyoghoul.kakuhou_bikaku': 'Bikaku Kakuhou',
        'item.tokyoghoul.mask_kaneki': "Eyepatch's Mask",
        'item.tokyoghoul.mask_rabbit': "Rabbit's Mask",
        'item.tokyoghoul.mask_gourmet': "Gourmet's Mask",
        'item.tokyoghoul.mask_nishiki': "Nishiki's Mask",
        'item.tokyoghoul.mask_crow': "Crow's Mask",
        'item.tokyoghoul.mask_owl': "Owl's Mask",
        'item.tokyoghoul.mask_black_rabbit': "Black Rabbit's Mask",
        'item.tokyoghoul.mask_jason': "Jason's Hockey Mask",
        'item.tokyoghoul.mask_tatara': "Tatara's Mask",
        'item.tokyoghoul.mask_bandage': "One-Eyed Owl's Bandages",
        'item.tokyoghoul.mask_clown': "Clown's Mask",
        'item.tokyoghoul.mask_hannya': 'Hannya Mask',
        'effect.tokyoghoul.rc_suppressed': 'Rc Suppressed',
        'key.categories.tokyoghoul': 'Tokyo Ghoul',
        'key.tokyoghoul.cycle': 'Change technique (Shift: back)',
        'key.tokyoghoul.use': 'Use selected technique (hold)',
        'key.tokyoghoul.reset': 'Reset technique selection',
        'key.tokyoghoul.kagune': 'Release / retract kagune',
        'key.tokyoghoul.guard': 'Guard (hold)',
        'key.tokyoghoul.dodge': 'Dodge / backstep',
        'key.tokyoghoul.regen': 'Rc regeneration (hold)',
        'faction.tokyoghoul.human': 'Human',
        'faction.tokyoghoul.ccg': 'CCG',
        'faction.tokyoghoul.anteiku': 'Anteiku',
        'faction.tokyoghoul.aogiri': 'Aogiri Tree',
        'faction.tokyoghoul.wild': 'Ghoul',
        'faction.tokyoghoul.clowns': 'Clowns',
        'kagune.tokyoghoul.none': 'No kagune',
        'kagune.tokyoghoul.ukaku': 'Ukaku',
        'kagune.tokyoghoul.koukaku': 'Koukaku',
        'kagune.tokyoghoul.rinkaku': 'Rinkaku',
        'kagune.tokyoghoul.bikaku': 'Bikaku',
        'screen.tokyoghoul.registry': 'Ghoul Registry',
        'screen.tokyoghoul.become': 'Become %s',
        'screen.tokyoghoul.pick': 'Choose a character on the left.',
        'screen.tokyoghoul.moves': 'Techniques',
        'screen.tokyoghoul.show_kagune': 'Show kagune',
        'screen.tokyoghoul.hide_kagune': 'Hide kagune',
        'screen.tokyoghoul.keys': 'R change  Z use  X reset  C kagune  G guard  B dodge  M regenerate',
        'hud.tokyoghoul.pick': 'Open the Ghoul Registry to choose who you are.',
        'tooltip.tokyoghoul.registry': 'Right-click to become any character.',
        'tooltip.tokyoghoul.thermos': "Right-click: never go hungry (toggle).",
        'tooltip.tokyoghoul.suppressant': 'Use on a ghoul to shut down its kagune.',
        'tooltip.tokyoghoul.mask': 'Hides your face from the CCG.',
        'tooltip.tokyoghoul.kakuhou': 'Ghouls: eat to grow toward a kakuja. Anyone: forge a quinque.',
        'tooltip.tokyoghoul.kakuhou_owner': 'Taken from %s',
        'tooltip.tokyoghoul.quinque_move': 'Right-click: %s',
        'tooltip.tokyoghoul.flesh': 'The only real food for a ghoul. Humans get sick.',
        'tooltip.tokyoghoul.coffee': "The one drink a ghoul's tongue can stand.",
        'tooltip.tokyoghoul.rc_cells': 'Restores Rc.',
        'msg.tokyoghoul.welcome': 'Tokyo is full of ghouls. Open the Ghoul Registry to choose who you are.',
        'msg.tokyoghoul.no_character': 'You have not chosen a character yet. Use the Ghoul Registry.',
        'msg.tokyoghoul.selected': 'You are now %s (%s).',
        'msg.tokyoghoul.cooldown': '%s is recovering.',
        'msg.tokyoghoul.rank_locked': 'Unlocks at a higher rank (level %s).',
        'msg.tokyoghoul.kakuja_locked': 'Your kakuja has not awakened. Devour kakuhou.',
        'msg.tokyoghoul.no_rc': 'Not enough Rc.',
        'msg.tokyoghoul.suppressed': 'Your Rc cells are suppressed!',
        'msg.tokyoghoul.stunned': 'Stunned!',
        'msg.tokyoghoul.fame': 'Defeated %s: +%s fame',
        'msg.tokyoghoul.rank_up': 'Promoted: %s',
        'msg.tokyoghoul.promoted': 'Your reputation grows',
        'msg.tokyoghoul.unlocked': 'New technique: %s',
        'msg.tokyoghoul.kakuja_progress': 'Kakuja %s%%',
        'msg.tokyoghoul.kakuja_ready': 'Your kakuja has awakened. Your ultimate is ready.',
        'msg.tokyoghoul.satiated_on': 'Satiated: you will not go hungry.',
        'msg.tokyoghoul.satiated_off': 'Satiation off.',
        'msg.tokyoghoul.disgusting': 'It tastes like rotten clay...',
        'msg.tokyoghoul.boss_near': '%s is nearby.',
        'death.attack.tokyoghoul.kagune': "%1$s was torn apart by a kagune",
        'death.attack.tokyoghoul.kagune.player': "%1$s was torn apart by %2$s's kagune",
        'death.attack.tokyoghoul.quinque': '%1$s was cut down by a quinque',
        'death.attack.tokyoghoul.quinque.player': "%1$s was cut down by %2$s's quinque",
        'death.attack.tokyoghoul.strike': '%1$s was beaten to death',
        'death.attack.tokyoghoul.strike.player': '%1$s was beaten to death by %2$s',
        'gamerule.tgPvp': 'Tokyo Ghoul: player techniques hurt players',
        'gamerule.tgFameDifficulty': 'Tokyo Ghoul: fame needed per rank (multiplier)',
        'gamerule.tgEncounters': 'Tokyo Ghoul: ghoul raids and CCG patrols',
        'gamerule.tgGhoulDiet': 'Tokyo Ghoul: ghouls cannot digest human food',
        'gamerule.tgStarterKit': 'Tokyo Ghoul: give the Ghoul Registry and thermos on first join',
    }
    for cid, name in chars:
        L[f'entity.{MOD}.{cid}'] = name
        L[f'character.{MOD}.{cid}'] = name
        L[f'item.{MOD}.{cid}_spawn_egg'] = name + ' Spawn Egg'
    L['entity.tokyoghoul.rc_projectile'] = 'Rc Projectile'
    L['entity.tokyoghoul.ground_spike'] = 'Kagune Spike'
    for qid, name in QUINQUES:
        L[f'item.{MOD}.{qid}'] = name
    write_json(os.path.join(A, 'lang', 'en_us.json'), L)


def data():
    for t, msg in (('kagune', 'tokyoghoul.kagune'), ('quinque', 'tokyoghoul.quinque'), ('strike', 'tokyoghoul.strike')):
        write_json(os.path.join(D, 'damage_type', t + '.json'), {'message_id': msg, 'exhaustion': 0.1, 'scaling': 'never'})

    def shaped(name, pattern, keys, result, count=1):
        write_json(os.path.join(D, 'recipes', name + '.json'), {
            'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern,
            'key': {k: {'item': v} for k, v in keys.items()}, 'result': {'item': result, 'count': count}})

    def shapeless(name, items, result, count=1):
        write_json(os.path.join(D, 'recipes', name + '.json'), {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'item': i} for i in items],
            'result': {'item': result, 'count': count}})

    for t in KAKUHOU:
        shaped('quinque_' + t, [' K ', ' I ', ' S '], {'K': f'{MOD}:kakuhou_{t}', 'I': 'minecraft:iron_ingot', 'S': 'minecraft:stick'},
               f'{MOD}:quinque_{t}')
        shapeless('rc_cells_from_' + t, [f'{MOD}:kakuhou_{t}'], f'{MOD}:rc_cells', 4)
    shapeless('quinque_blade', ['minecraft:iron_sword', f'{MOD}:rc_cells', f'{MOD}:rc_cells'], f'{MOD}:quinque_blade')
    shapeless('rc_suppressant', ['minecraft:glass_bottle', f'{MOD}:rc_cells', 'minecraft:iron_nugget'], f'{MOD}:rc_suppressant', 2)
    shapeless('anteiku_coffee', ['minecraft:cocoa_beans', 'minecraft:glass_bottle', 'minecraft:sugar'], f'{MOD}:anteiku_coffee')
    shaped('anteiku_thermos', [' I ', 'ICI', ' I '], {'I': 'minecraft:iron_ingot', 'C': f'{MOD}:anteiku_coffee'}, f'{MOD}:anteiku_thermos')
    shapeless('ghoul_registry', ['minecraft:book', f'{MOD}:rc_cells', 'minecraft:ink_sac'], f'{MOD}:ghoul_registry')
    shaped('ccg_coat', ['W W', 'WBW', 'WWW'], {'W': 'minecraft:white_wool', 'B': 'minecraft:black_dye'}, f'{MOD}:ccg_coat')
    extra = {
        'mask_kaneki': ['minecraft:black_dye', 'minecraft:bone_meal'],
        'mask_rabbit': ['minecraft:purple_dye', 'minecraft:rabbit_hide'],
        'mask_gourmet': ['minecraft:white_dye', 'minecraft:magenta_dye'],
        'mask_nishiki': ['minecraft:blue_dye', 'minecraft:black_dye'],
        'mask_crow': ['minecraft:black_dye', 'minecraft:feather'],
        'mask_owl': ['minecraft:brown_dye', 'minecraft:feather'],
        'mask_black_rabbit': ['minecraft:black_dye', 'minecraft:rabbit_hide'],
        'mask_jason': ['minecraft:white_dye', 'minecraft:red_dye'],
        'mask_tatara': ['minecraft:red_dye', 'minecraft:blaze_powder'],
        'mask_bandage': ['minecraft:paper', 'minecraft:paper'],
        'mask_clown': ['minecraft:white_dye', 'minecraft:pink_dye'],
        'mask_hannya': ['minecraft:red_dye', 'minecraft:bone'],
    }
    for m, items in extra.items():
        shapeless(m, ['minecraft:leather', 'minecraft:string'] + items, f'{MOD}:{m}')


def main():
    chars = roster()
    book()
    thermos()
    coffee()
    flesh()
    rc_cells()
    suppressant()
    coat()
    for t, col in KAKUHOU.items():
        kakuhou(t, col)
    for name, (base, acc, style) in MASKS.items():
        mask(name, base, acc, style)
    models(chars)
    lang(chars)
    data()
    print(f'{len(chars)} characters, {len(QUINQUES)} quinques, {len(MASKS)} masks')


if __name__ == '__main__':
    main()
