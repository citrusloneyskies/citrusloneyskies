#!/usr/bin/env python3
"""The siege's advancement tree: one JSON per advancement (a single impossible criterion; the mod grants them from
code, see siege/Milestones.java) and their titles and descriptions in the lang file (merged into tools/lang_extra.json).

Usage:  python3 tools/gen_advancements.py
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
ADV = os.path.join(RES, 'data', 'lastbastion', 'advancements')
LANG_PATH = os.path.join(RES, 'assets', 'lastbastion', 'lang', 'en_us.json')

# id, parent, icon, frame, title, description, hidden
TREE = [
    ('root', None, 'lastbastion:siege_core', 'task', 'Last Bastion', 'Place a Siege Core and prepare for the night', False),
    ('first_night', 'root', 'lastbastion:turret_kit_ballista', 'task', 'First Night', 'Survive your first wave', False),
    ('perfect_defence', 'first_night', 'minecraft:shield', 'goal', 'Not a Scratch', 'Clear a wave without the core taking any damage', False),
    ('wave_10', 'first_night', 'lastbastion:warforged_token', 'goal', 'Seasoned Defender', 'Hold the core through wave 10', False),
    ('wave_25', 'wave_10', 'lastbastion:sigil_zeus', 'challenge', 'Bastion Eternal', 'Hold the core through wave 25', False),
    ('wave_50', 'wave_25', 'lastbastion:pact_lucifer', 'challenge', 'The Last Bastion', 'Hold the core through wave 50', False),
    ('victory', 'wave_10', 'minecraft:nether_star', 'challenge', 'Victory', "Hold out to your siege's victory wave", False),
    ('kingslayer', 'first_night', 'minecraft:wither_skeleton_skull', 'goal', 'Kingslayer', 'Defeat a siege boss', False),
    ('warlords_bane', 'first_night', 'minecraft:golden_sword', 'goal', "Warlord's Bane", 'Slay a warlord', False),
    ('seal_the_rift', 'first_night', 'minecraft:ender_eye', 'goal', 'Seal the Rift', 'Destroy a corruption rift', False),
    ('special_delivery', 'first_night', 'minecraft:chest', 'task', 'Special Delivery', 'Claim a supply drop before it scuttles itself', False),
    ('not_today', 'first_night', 'minecraft:totem_of_undying', 'task', 'Not Today', 'Revive a downed teammate', False),
    ('specialist', 'root', 'lastbastion:turret_kit_tesla', 'goal', 'Specialist', 'Raise a turret to level 5 and choose its specialisation', False),
    ('answer_the_call', 'root', 'lastbastion:sigil_aldric', 'task', 'Answer the Call', 'Summon a hero from its sigil', False),
    ('legion', 'answer_the_call', 'minecraft:golden_helmet', 'goal', 'Legion', 'Command six heroes at once', False),
    ('pact_in_blood', 'answer_the_call', 'lastbastion:pact_lucifer', 'challenge', 'Pact in Blood', 'Bind a devil to your soul', False),
    ('masterwork', 'root', 'lastbastion:dawnbreaker', 'goal', 'Masterwork', 'Upgrade a weapon with a Warforged Token', False),
]


def main():
    os.makedirs(ADV, exist_ok=True)
    lang = {}
    for aid, parent, icon, frame, title, desc, hidden in TREE:
        key = 'advancements.lastbastion.' + aid
        lang[key + '.title'] = title
        lang[key + '.description'] = desc
        display = {
            'icon': {'item': icon},
            'title': {'translate': key + '.title'},
            'description': {'translate': key + '.description'},
            'frame': frame,
            'show_toast': True,
            'announce_to_chat': True,
            'hidden': hidden,
        }
        if parent is None:
            display['background'] = 'minecraft:textures/block/polished_blackstone_bricks.png'
        adv = {'display': display, 'criteria': {'earned': {'trigger': 'minecraft:impossible'}}}
        if parent is not None:
            adv = {'parent': 'lastbastion:' + parent, **adv}
        with open(os.path.join(ADV, aid + '.json'), 'w') as f:
            json.dump(adv, f, indent=2)
            f.write('\n')
    extra_path = os.path.join(HERE, 'lang_extra.json')
    extra = json.load(open(extra_path)) if os.path.exists(extra_path) else {}
    extra.update(lang)
    with open(extra_path, 'w') as f:
        json.dump(dict(sorted(extra.items())), f, indent=2)
        f.write('\n')
    full = json.load(open(LANG_PATH))
    full.update(lang)
    with open(LANG_PATH, 'w') as f:
        json.dump(dict(sorted(full.items())), f, indent=2)
        f.write('\n')
    print('%d advancements written' % len(TREE))


if __name__ == '__main__':
    main()
