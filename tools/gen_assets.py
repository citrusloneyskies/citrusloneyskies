#!/usr/bin/env python3
"""Generates Last Bastion's static assets: pixel-art item/block/effect textures, models, blockstates,
loot tables, the core recipe and the English language file. Entity textures are generated at runtime
by the procedural model system, so none are needed here.

Run from the repository root:  python3 tools/gen_assets.py
"""
import json
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'lastbastion')
D = os.path.join(ROOT, 'data', 'lastbastion')
MOD = 'lastbastion'


def path(*p):
    full = os.path.join(*p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    return full


def write_json(p, obj):
    with open(path(p), 'w') as f:
        json.dump(obj, f, indent=2)


def rgb(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


def shade(h, f):
    r, g, b, _ = rgb(h)
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))), max(0, min(255, int(b * f))), 255)


class Canvas:
    def __init__(self, size=16, seed=0):
        self.img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        self.px = self.img.load()
        self.size = size
        self.rnd = random.Random(seed)

    def set(self, x, y, c):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[x, y] = c if isinstance(c, tuple) else rgb(c)

    def rect(self, x0, y0, x1, y1, c, noise=0.0):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if noise:
                    self.set(x, y, shade(c, 1 + (self.rnd.random() - 0.5) * 2 * noise))
                else:
                    self.set(x, y, c)

    def line(self, x0, y0, x1, y1, c):
        n = int(max(abs(x1 - x0), abs(y1 - y0), 1))
        for i in range(n + 1):
            self.set(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)

    def circle(self, cx, cy, r, c, fill=True, noise=0.0):
        for y in range(self.size):
            for x in range(self.size):
                d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
                if (fill and d <= r) or (not fill and abs(d - r) < 0.6):
                    self.set(x, y, shade(c, 1 + (self.rnd.random() - 0.5) * 2 * noise) if noise else c)

    def outline(self, c=0x1A1A1A):
        src = self.img.copy().load()
        for y in range(self.size):
            for x in range(self.size):
                if src[x, y][3] == 0:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < self.size and 0 <= ny < self.size and src[nx, ny][3] > 0:
                            self.set(x, y, rgb(c))
                            break

    def save(self, p):
        self.img.save(path(p))


# ---------------------------------------------------------------------------- data

TURRETS = [('ballista', 'Ballista', 0x8A6A3A), ('flame', 'Inferno Turret', 0xFF6A00), ('frost', 'Cryo Spire', 0x8FE3FF),
           ('tide', 'Tidecaller', 0x2F6BFF), ('tesla', 'Tesla Coil', 0xF4F06A), ('venom', 'Venom Lobber', 0x7BD940),
           ('mortar', 'Siege Mortar', 0x5A5A5A), ('prism', 'Arcane Prism', 0xC86BFF), ('gale', 'Gale Tower', 0xCFFFF0),
           ('sanctum', 'Sanctum Obelisk', 0xFFF2B0)]
HEROES = [('ashfang', 'Ashfang the Dire Wolf', 0x8A8F99), ('sylva', 'Sylva the Huntress', 0x3F8F4A),
          ('bastion', 'Bastion, the Stone Colossus', 0x8C8577), ('aldric', 'Sir Aldric, Knight of Dawn', 0xFFD966),
          ('morgana', 'Morgana the Stormweaver', 0x7A4AE0), ('kage', 'Kage the Ronin', 0xC01030),
          ('aegis7', 'AEGIS-7 War Mech', 0x5B6E7A), ('asterion', 'Asterion, the Minotaur King', 0x9A5A2E),
          ('anubis', 'Anubis, Judge of the Dead', 0xE0B040), ('zeus', 'Zeus, King of Olympus', 0xF4F06A)]
ZOMBIES = [('shambler', 'Shambler'), ('runner', 'Runner'), ('spitter', 'Spitter'), ('brute', 'Brute'), ('bloater', 'Bloater'),
           ('leaper', 'Leaper'), ('bulwark', 'Bulwark'), ('burrower', 'Burrower'), ('shrieker', 'Shrieker'), ('wraith', 'Wraith'),
           ('wrecker', 'Wrecker'), ('necromancer', 'Necromancer'), ('crawler', 'Crawler'), ('incinerator', 'Incinerator'),
           ('abomination', 'Abomination')]
BOSSES = [('gorehound', 'Gorehound the Butcher'), ('plague_mother', 'The Plague Mother'), ('malgrave', 'Malgrave the Dread Knight'),
          ('ossuary_colossus', 'The Ossuary Colossus'), ('varkhul', 'Varkhul the Lich King'), ('morvathrax', 'Morvathrax the Rotwing')]
GUNS = [('ravager', "M60 'Ravager' Machine Gun"), ('thunderboom', "'Thunderboom' Scattergun"), ('hellfire', "'Hellfire' Rocket Launcher"),
        ('starpiercer', "'Starpiercer' Railgun"), ('dragonbreath', "'Dragon's Breath' Flamethrower"), ('stormfist', "'Stormfist' Tesla Gauntlet"),
        ('glacier', "'Glacier' Cryo Cannon"), ('sunlance', "'Sunlance' Holy Repeater")]
GRENADES = [('frag', 'Frag Grenade', 0x4A5A3A), ('incendiary', 'Incendiary Grenade', 0xC04010), ('cryo', 'Cryo Grenade', 0x6FC8FF),
            ('holy', 'Holy Hand Grenade', 0xE8C848), ('singularity', 'Singularity Grenade', 0x7A3AAA)]
CONSUMABLES = [('bandage', 'Field Bandage'), ('medkit', 'Combat Medkit'), ('stim', 'Berserker Stim'), ('core_repair', 'Core Repair Kit')]
SUPPORT = [('orbital', 'Orbital Strike Remote'), ('airstrike', 'Airstrike Flare'), ('drone', 'Sentry Drone'), ('exosuit', 'Titan Exosuit Beacon')]
BLOCKS = [('siege_core', 'Siege Core'), ('barricade_wood', 'Wooden Barricade'), ('barricade_stone', 'Stone Rampart'),
          ('barricade_iron', 'Iron Bulwark'), ('barricade_runic', 'Runic Ward'), ('spike_trap', 'Spike Trap'), ('landmine', 'Landmine'),
          ('medic_shrine', 'Medic Shrine'), ('soul_harvester', 'Soul Harvester'), ('repair_pylon', 'Repair Pylon')]
EFFECTS = [('bleeding', 'Bleeding', 0x9B0E0E), ('scorched', 'Scorched', 0xFF6A00), ('chilled', 'Chilled', 0x9FE6FF),
           ('frozen', 'Frozen', 0xD8F8FF), ('soaked', 'Soaked', 0x2F6BFF), ('shocked', 'Shocked', 0xF4F06A),
           ('corroded', 'Corroded', 0x7BD940), ('concussed', 'Concussed', 0xB08A5A), ('marked', 'Marked', 0xE04AE0),
           ('disoriented', 'Disoriented', 0xCFFFF0), ('purified', 'Purified', 0xFFF2B0), ('jammed', 'Jammed', 0x5A5A5A),
           ('feared', 'Feared', 0x3A1A4A), ('aegis', 'Aegis', 0xFFD966), ('rally', 'Rally', 0xFF4040)]

# ---------------------------------------------------------------------------- item art


def turret_kit(color, seed):
    c = Canvas(seed=seed)
    c.rect(2, 4, 13, 14, 0x7A5230, 0.12)
    c.rect(2, 4, 13, 5, 0x5A3A20)
    c.rect(2, 9, 13, 9, 0x5A3A20)
    for x, y in ((2, 4), (13, 4), (2, 14), (13, 14)):
        c.set(x, y, 0xB0B0B8)
    c.circle(8, 9.5, 2.6, color)
    c.circle(8, 9.5, 2.6, shade(color, 0.6), fill=False)
    c.rect(6, 1, 9, 3, 0x8A8A90)
    c.set(7, 0, color)
    c.set(8, 0, color)
    c.outline()
    return c


def sigil(color, seed, letter):
    c = Canvas(seed=seed)
    c.circle(8, 8, 7, 0xD8B040, noise=0.08)
    c.circle(8, 8, 5.2, color, noise=0.1)
    c.circle(8, 8, 7, 0x8A6A20, fill=False)
    glyphs = {
        0: [(8, 4), (8, 5), (7, 6), (9, 6), (6, 7), (10, 7), (8, 8), (8, 9), (8, 10), (8, 11)],
        1: [(5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (10, 5), (9, 6), (6, 9), (5, 10)],
        2: [(6, 5), (7, 5), (8, 5), (9, 5), (10, 5), (8, 6), (8, 7), (8, 8), (8, 9), (6, 10), (7, 10), (8, 10), (9, 10), (10, 10)],
    }
    for x, y in glyphs[letter % 3]:
        c.set(x, y, 0xFFFFFF)
    c.outline()
    return c


GUN_ART = {
    'ravager': [((1, 7, 12, 8), 0x3A3A40), ((12, 6, 15, 9), 0x2A2A2A), ((3, 9, 5, 12), 0x5A3A20), ((6, 9, 7, 10), 0x2A2A2A),
                ((8, 9, 10, 13), 0x6A6A70), ((0, 6, 2, 9), 0x5A3A20)],
    'thunderboom': [((2, 7, 14, 8), 0x2A2A2A), ((2, 9, 9, 9), 0x6A4A2A), ((0, 8, 3, 11), 0x6A4A2A), ((10, 9, 12, 9), 0x8A6A3A)],
    'hellfire': [((1, 6, 14, 9), 0x4A5A3A), ((14, 5, 15, 10), 0x2A2A2A), ((5, 10, 6, 13), 0x2A2A2A), ((9, 10, 10, 12), 0x3A3A3A),
                 ((0, 7, 1, 8), 0xA02020)],
    'starpiercer': [((1, 7, 15, 8), 0xD0D8E0), ((3, 6, 12, 6), 0x7FE8FF), ((3, 9, 12, 9), 0x7FE8FF), ((4, 10, 6, 13), 0x3A3A48),
                    ((0, 6, 2, 9), 0x3A3A48)],
    'dragonbreath': [((1, 7, 11, 8), 0x3A3A40), ((11, 6, 14, 9), 0x2A2A2A), ((2, 3, 5, 6), 0xA02A1A), ((4, 9, 6, 12), 0x5A5A5A),
                     ((15, 7, 15, 8), 0xFF9020)],
    'stormfist': [((4, 4, 11, 12), 0xB87333), ((5, 5, 10, 6), 0x7A4A20), ((6, 8, 9, 10), 0xF4F06A), ((5, 13, 10, 15), 0x5A5A60),
                  ((7, 1, 8, 3), 0xF4F06A)],
    'glacier': [((1, 6, 12, 9), 0x6A8AA0), ((12, 5, 15, 10), 0x9FE8FF), ((4, 10, 6, 13), 0x3A4A58), ((2, 5, 9, 5), 0xD0F4FF)],
    'sunlance': [((1, 7, 13, 8), 0xF0EEE8), ((13, 6, 15, 9), 0xFFD966), ((5, 9, 7, 12), 0xD8B040), ((3, 6, 10, 6), 0xFFF2B0)],
}


def gun(name, seed):
    c = Canvas(seed=seed)
    for (x0, y0, x1, y1), col in GUN_ART[name]:
        c.rect(x0, y0, x1, y1, col, 0.08)
    c.outline()
    # rotate 45 degrees feel by shearing? keep horizontal but tilt for handheld look
    c.img = c.img.rotate(35, resample=Image.NEAREST)
    return c


def voidedge(seed):
    c = Canvas(seed=seed)
    for i in range(10):
        c.set(4 + i, 11 - i, 0xC07AFF)
        c.set(5 + i, 11 - i, 0x7A3ACF)
    c.line(1, 14, 4, 11, 0x2A1A2A)
    c.set(3, 11, 0xD8B040)
    c.set(4, 12, 0xD8B040)
    c.set(5, 10, 0xD8B040)
    c.outline()
    return c


def grenade(color, seed):
    c = Canvas(seed=seed)
    c.circle(8, 9.5, 5, color, noise=0.12)
    c.rect(6, 3, 9, 5, 0x5A5A5A)
    c.circle(11, 3, 1.5, 0xC0C0C0, fill=False)
    c.set(6, 8, 0xFFFFFF)
    c.set(6, 7, shade(color, 1.4))
    c.outline()
    return c


def consumable(name, seed):
    c = Canvas(seed=seed)
    if name == 'bandage':
        c.circle(8, 8, 5, 0xF0EEE8, noise=0.05)
        c.circle(8, 8, 2, 0xC8C0B0)
        c.rect(10, 9, 15, 11, 0xF0EEE8)
    elif name == 'medkit':
        c.rect(2, 4, 13, 13, 0xF0F0F0, 0.04)
        c.rect(6, 6, 9, 11, 0xD02020)
        c.rect(4, 7, 11, 10, 0xD02020)
        c.rect(6, 2, 9, 3, 0x5A5A5A)
    elif name == 'stim':
        c.line(3, 12, 12, 3, 0xD0E0F0)
        c.line(4, 12, 12, 4, 0xFF3030)
        c.line(2, 14, 4, 12, 0xA0A0A0)
        c.rect(11, 2, 13, 4, 0x5A5A5A)
    else:
        c.circle(7, 8, 4, 0x6FE3FF, noise=0.1)
        c.set(6, 6, 0xFFFFFF)
        c.line(10, 13, 14, 9, 0x8A8A90)
        c.rect(13, 8, 14, 9, 0xB0B0B8)
    c.outline()
    return c


def support(name, seed):
    c = Canvas(seed=seed)
    if name == 'orbital':
        c.rect(4, 5, 11, 14, 0x3A3A48, 0.06)
        c.rect(5, 6, 10, 9, 0x7FE8FF)
        c.set(6, 11, 0xFF3030)
        c.set(9, 11, 0x30FF30)
        c.line(10, 5, 13, 1, 0xA0A0A0)
        c.set(13, 0, 0xFF3030)
    elif name == 'airstrike':
        c.line(4, 14, 11, 4, 0xC02020)
        c.line(5, 14, 12, 4, 0xA01010)
        c.circle(12, 3, 2, 0xFFC040)
        c.set(13, 1, 0xFFFFA0)
    elif name == 'drone':
        c.rect(5, 7, 10, 9, 0xE0E4E8)
        c.set(7, 9, 0x7FE8FF)
        c.set(8, 9, 0x7FE8FF)
        for x, y in ((2, 5), (13, 5), (2, 11), (13, 11)):
            c.rect(x - 1, y, x + 1, y, 0x2A2A2A)
            c.line(x, y, 7.5, 8, 0x5A5A60)
    else:
        c.rect(5, 6, 10, 14, 0xE8B020, 0.06)
        for y in range(6, 15, 2):
            c.rect(5, y, 10, y, 0x2A2A2A)
        c.rect(6, 2, 9, 5, 0xFF3020)
        c.set(7, 1, 0xFFA080)
    c.outline()
    return c


def block_tex(name, seed):
    c = Canvas(seed=seed)
    if name == 'barricade_wood':
        c.rect(0, 0, 15, 15, 0x8A6034, 0.1)
        for y in (0, 5, 10, 15):
            c.rect(0, y, 15, y, 0x5A3A1A)
        c.line(0, 0, 15, 15, 0x5A3A1A)
        c.line(0, 15, 15, 0, 0x5A3A1A)
        c.line(1, 0, 15, 14, 0x6A4A26)
    elif name == 'barricade_stone':
        c.rect(0, 0, 15, 15, 0x8A8A8A, 0.1)
        for y in range(0, 16, 4):
            c.rect(0, y, 15, y, 0x5A5A5A)
            off = 0 if (y // 4) % 2 == 0 else 4
            for x in range(off, 16, 8):
                c.rect(x, y, x, y + 3, 0x5A5A5A)
    elif name == 'barricade_iron':
        c.rect(0, 0, 15, 15, 0x8A8A90, 0.06)
        c.rect(0, 7, 15, 8, 0x5A5A60)
        c.rect(7, 0, 8, 15, 0x5A5A60)
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (5, 5), (10, 10), (10, 5), (5, 10)):
            c.set(x, y, 0xC8C8D0)
    elif name == 'barricade_runic':
        c.rect(0, 0, 15, 15, 0x1A1424, 0.12)
        for x, y in ((3, 3), (4, 4), (5, 3), (4, 2), (11, 11), (12, 12), (10, 12), (12, 10), (7, 8), (8, 7), (8, 9), (9, 8), (3, 12), (12, 3)):
            c.set(x, y, 0xB050FF)
    elif name == 'spike_trap':
        c.rect(0, 0, 15, 15, 0x5A5A60, 0.08)
        for y in range(1, 16, 4):
            for x in range(1, 16, 4):
                c.rect(x, y, x + 1, y + 1, 0xC8C8D0)
                c.set(x, y, 0xFFFFFF)
    elif name == 'landmine':
        c.rect(0, 0, 15, 15, 0x3A3A30, 0.06)
        c.circle(8, 8, 3, 0x5A5A50)
        c.set(7, 7, 0xFF2020)
        c.set(8, 8, 0xFF2020)
    elif name == 'medic_shrine':
        c.rect(0, 0, 15, 15, 0xF0EEE8, 0.05)
        c.rect(6, 3, 9, 12, 0xD02020)
        c.rect(3, 6, 12, 9, 0xD02020)
    elif name == 'soul_harvester':
        c.rect(0, 0, 15, 15, 0x1A3A40, 0.1)
        c.circle(8, 8, 5, 0x5AD8FF, fill=False)
        c.circle(8, 8, 2, 0xC0FFFF)
    elif name == 'repair_pylon':
        c.rect(0, 0, 15, 15, 0xA06020, 0.08)
        c.rect(0, 0, 15, 1, 0x2A2A2A)
        c.rect(0, 14, 15, 15, 0x2A2A2A)
        c.line(4, 12, 11, 4, 0xD8D8D8)
        c.rect(10, 3, 12, 5, 0xD8D8D8)
    elif name == 'siege_core':
        c.rect(0, 0, 15, 15, 0x2A3038, 0.08)
        c.rect(0, 0, 15, 0, 0x6FE3FF)
        c.rect(0, 15, 15, 15, 0x6FE3FF)
        c.rect(6, 6, 9, 9, 0x6FE3FF)
        c.set(7, 7, 0xFFFFFF)
    return c


def effect_icon(color, seed, idx):
    c = Canvas(18, seed)
    c.circle(9, 9, 8, shade(color, 0.45))
    c.circle(9, 9, 6.5, color, noise=0.1)
    c.circle(9, 9, 8, 0x101010, fill=False)
    marks = [(9, 5), (9, 6), (9, 7), (9, 8), (9, 9), (9, 11), (9, 12)]
    sym = idx % 4
    if sym == 0:
        pts = marks
    elif sym == 1:
        pts = [(6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11), (12, 12), (12, 6), (11, 7), (7, 11), (6, 12)]
    elif sym == 2:
        pts = [(5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9), (11, 9), (12, 9), (9, 5), (9, 6), (9, 7), (9, 8), (9, 10), (9, 11), (9, 12)]
    else:
        pts = [(7, 5), (8, 5), (9, 5), (10, 5), (11, 6), (11, 7), (10, 8), (9, 9), (9, 10), (9, 12)]
    for x, y in pts:
        c.set(x, y, 0xFFFFFF)
    return c


# ---------------------------------------------------------------------------- main


def main():
    lang = {'itemGroup.lastbastion': 'Last Bastion: Zombie Siege',
            'key.categories.lastbastion': 'Last Bastion',
            'key.lastbastion.hero_command': 'Order All Heroes',
            'msg.lastbastion.core_exists': 'A Siege Core already stands at %s. Only one core may exist.'}
    seed = 1
    items = []

    for i, (tid, name, col) in enumerate(TURRETS):
        n = 'turret_kit_' + tid
        turret_kit(col, seed + i).save(os.path.join(A, 'textures', 'item', n + '.png'))
        lang['item.%s.%s' % (MOD, n)] = name + ' Kit'
        lang['entity.%s.turret_%s' % (MOD, tid)] = name
        items.append((n, 'generated'))
    for i, (hid, name, col) in enumerate(HEROES):
        n = 'sigil_' + hid
        sigil(col, 100 + i, i).save(os.path.join(A, 'textures', 'item', n + '.png'))
        lang['item.%s.%s' % (MOD, n)] = 'Sigil of ' + name
        lang['entity.%s.%s' % (MOD, hid)] = name
        items.append((n, 'generated'))
    lang['entity.%s.kage_clone' % MOD] = 'Shadow of Kage'
    for i, (gid, name) in enumerate(GUNS):
        lang['item.%s.gun_%s' % (MOD, gid)] = name
    # guns and melee weapons (models, baked icons) are generated by tools/gen_arsenal.py
    for i, (gid, name, col) in enumerate(GRENADES):
        n = 'grenade_' + gid
        grenade(col, 400 + i).save(os.path.join(A, 'textures', 'item', n + '.png'))
        lang['item.%s.%s' % (MOD, n)] = name
        items.append((n, 'generated'))
    for i, (cid, name) in enumerate(CONSUMABLES):
        consumable(cid, 500 + i).save(os.path.join(A, 'textures', 'item', cid + '.png'))
        lang['item.%s.%s' % (MOD, cid)] = name
        items.append((cid, 'generated'))
    for i, (sid, name) in enumerate(SUPPORT):
        n = 'call_' + sid
        support(sid, 600 + i).save(os.path.join(A, 'textures', 'item', n + '.png'))
        lang['item.%s.%s' % (MOD, n)] = name
        items.append((n, 'generated'))
    for zid, name in ZOMBIES:
        lang['entity.%s.%s' % (MOD, zid)] = name
    for bid, name in BOSSES:
        lang['entity.%s.%s' % (MOD, bid)] = name
    for k, v in {'projectile': 'Siege Projectile', 'hazard_zone': 'Hazard', 'phylactery': 'Phylactery Crystal',
                 'sentry_drone': 'Sentry Drone', 'drop_pod': 'Drop Pod', 'titan_exosuit': 'Titan Exosuit'}.items():
        lang['entity.%s.%s' % (MOD, k)] = v
    for i, (eid, name, col) in enumerate(EFFECTS):
        effect_icon(col, 700 + i, i).save(os.path.join(A, 'textures', 'mob_effect', eid + '.png'))
        lang['effect.%s.%s' % (MOD, eid)] = name

    for n, parent in items:
        write_json(os.path.join(A, 'models', 'item', n + '.json'),
                   {'parent': 'minecraft:item/' + parent, 'textures': {'layer0': '%s:item/%s' % (MOD, n)}})

    # blocks
    for i, (bid, name) in enumerate(BLOCKS):
        lang['block.%s.%s' % (MOD, bid)] = name
        block_tex(bid, 800 + i).save(os.path.join(A, 'textures', 'block', bid + '.png'))
        write_json(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'': {'model': '%s:block/%s' % (MOD, bid)}}})
        write_json(os.path.join(A, 'models', 'item', bid + '.json'), {'parent': '%s:block/%s' % (MOD, bid)})
        write_json(os.path.join(D, 'loot_tables', 'blocks', bid + '.json'), {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': '%s:%s' % (MOD, bid)}],
                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    tex = lambda b: '%s:block/%s' % (MOD, b)
    for b in ('barricade_wood', 'barricade_stone', 'barricade_iron', 'barricade_runic'):
        write_json(os.path.join(A, 'models', 'block', b + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex(b)}})

    def model(name, elements, particle):
        write_json(os.path.join(A, 'models', 'block', name + '.json'), {
            'parent': 'minecraft:block/block', 'textures': {'t': tex(name), 'particle': tex(particle)}, 'elements': elements})

    def el(frm, to, faces='all'):
        f = {d: {'texture': '#t'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}
        return {'from': frm, 'to': to, 'faces': f}

    model('spike_trap', [el([0, 0, 0], [16, 2, 16])] + [el([x, 2, z], [x + 2, 5, z + 2]) for x in (1, 5, 9, 13) for z in (1, 5, 9, 13)], 'spike_trap')
    model('landmine', [el([4, 0, 4], [12, 1.5, 12]), el([7, 1.5, 7], [9, 2.5, 9])], 'landmine')
    model('medic_shrine', [el([2, 0, 2], [14, 3, 14]), el([5, 3, 5], [11, 13, 11]), el([3, 13, 3], [13, 15, 13])], 'medic_shrine')
    model('soul_harvester', [el([1, 0, 1], [15, 4, 15]), el([4, 4, 4], [12, 12, 12]), el([6, 12, 6], [10, 16, 10])], 'soul_harvester')
    model('repair_pylon', [el([3, 0, 3], [13, 2, 13]), el([6, 2, 6], [10, 14, 10]), el([4, 14, 4], [12, 16, 12])], 'repair_pylon')
    model('siege_core', [el([1, 0, 1], [15, 4, 15]), el([3, 4, 3], [13, 8, 13]), el([5, 8, 5], [11, 10, 11]),
                         el([1, 4, 1], [3, 12, 3]), el([13, 4, 1], [15, 12, 3]), el([1, 4, 13], [3, 12, 15]), el([13, 4, 13], [15, 12, 15])], 'siege_core')

    write_json(os.path.join(D, 'recipes', 'siege_core.json'), {
        'type': 'minecraft:crafting_shaped',
        'pattern': ['IGI', 'RDR', 'IGI'],
        'key': {'I': {'item': 'minecraft:iron_block'}, 'G': {'item': 'minecraft:gold_ingot'},
                'R': {'item': 'minecraft:redstone'}, 'D': {'item': 'minecraft:diamond'}},
        'result': {'item': '%s:siege_core' % MOD, 'count': 1}})

    extra = os.path.join(os.path.dirname(__file__), 'lang_extra.json')
    if os.path.exists(extra):
        with open(extra) as f:
            lang.update(json.load(f))
    write_json(os.path.join(A, 'lang', 'en_us.json'), dict(sorted(lang.items())))
    print('generated', len(items), 'items,', len(BLOCKS), 'blocks,', len(EFFECTS), 'effects,', len(lang), 'lang keys')


if __name__ == '__main__':
    main()


# ---------------------------------------------------------------------------- gametest arena

def write_empty_structure(p, sx, sy, sz):
    """Writes an all-air structure template (gzipped NBT) used by the GameTest smoke tests."""
    import gzip
    import struct

    def name(n):
        b = n.encode('utf-8')
        return struct.pack('>H', len(b)) + b

    def tag_int(n, v):
        return b'\x03' + name(n) + struct.pack('>i', v)

    def tag_list_int(n, vals):
        return b'\x09' + name(n) + b'\x03' + struct.pack('>i', len(vals)) + b''.join(struct.pack('>i', v) for v in vals)

    def tag_list_empty(n):
        return b'\x09' + name(n) + b'\x00' + struct.pack('>i', 0)

    body = tag_int('DataVersion', 3465) + tag_list_int('size', [sx, sy, sz]) + tag_list_empty('palette') \
        + tag_list_empty('blocks') + tag_list_empty('entities') + b'\x00'
    data = b'\x0a' + name('') + body
    with gzip.open(path(p), 'wb') as f:
        f.write(data)


write_empty_structure(os.path.join(D, 'structures', 'arena.nbt'), 32, 10, 32)
