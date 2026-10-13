# Last Bastion: Zombie Siege

A Forge mod for **Minecraft 1.20.1** (Forge 47.x). Craft a Siege Core, place it, and hold it against a
zombie siege that comes every night from every direction and gets harder each wave. Kills earn credits.
You spend them at the core on turrets, heroes, weapons, call-ins and fortifications. If the core falls,
the siege starts again from wave 1.

## Getting started

1. Craft a **Siege Core**:
   ```
   [Iron Block] [Gold Ingot] [Iron Block]
   [ Redstone ] [ Diamond  ] [ Redstone ]
   [Iron Block] [Gold Ingot] [Iron Block]
   ```
2. Place it. Only one core can exist per world. Right-click it to open the **War Room** shop.
3. Use the daytime to build. Wave 1 starts at nightfall. You can also *Sound the War Horn* to start the next wave early for a +25% bonus.
4. Every player starts with **400 credits**. Kills pay out to whoever landed the blow; kills by turrets and heroes pay their owner.
   Each cleared wave pays `50 + 20 × wave` to every defender.

The HUD in the top-left shows the wave number, core health and barrier, the enemies remaining or the time until nightfall, and your credits.
Floating `+N` numbers appear over every kill.

## The siege

* Zombies rise out of the ground in a ring around the core (default 44 blocks away). Wave 1 comes from one side.
  By wave 15 they come from all directions, with flanking groups.
* Zombies march on the core and **breach anything in their way**, with cracking animations. Breach speed depends on block
  hardness and the zombie type. They divert to fight defenders who get close or hurt them, but a leash pulls them back to the core.
* The siege only runs while a player is within 160 blocks of the core.
* **Defeat:** when core HP reaches 0, the core explodes. All turrets and heroes are lost, and you keep 25% of your credits.
  The siege resets to wave 1, and a new core item drops so you can start again. (Configurable.)

### Difficulty model
| Mechanic | Scaling |
|---|---|
| Threat budget | `18 × 1.12^(w-1) + 8(w-1)`, × difficulty, × (1 + 0.5 per extra player) |
| Enemy HP / damage | +7% / +3.5% per wave |
| Zombie rank | centre rank = (w-1)/7 → Rotten → Feral → Armored → Corrupted → Abyssal |
| Champions | from wave 4: 2% + 0.5%/wave (max 15%), with 1-3 affixes |
| Spawn fronts | 1 → 2 → 3 → 4 → all-round with flankers |
| Bosses | every 5th wave, cycling through all 6. After wave 30, several bosses come at once |
| Special nights | from wave 6, 35% chance: Blood Moon, Graveyard Fog, Thunderstorm, Horde Night, Elite Vanguard, Necrotic Tide (each pays bonus credits) |

## Enemies

**12 zombie types × 5 ranks.** Higher ranks get tougher and look different: rusted helmets and pauldrons, glowing void crystals,
and finally horns, burning eyes and a hellfire crown.

| Zombie | Role |
|---|---|
| Shambler | Slow, endless foot soldier |
| Runner | Sprints and pounces |
| Spitter | Corrosive bile from range, prefers turrets |
| Brute | Tank with a charge; breaches walls 3× faster and smashes turrets |
| Bloater | Swells and explodes into a toxic cloud. Kill it far from your walls |
| Leaper | Vaults over walls |
| Bulwark | Tower shield blocks 80% of frontal projectiles |
| Burrower | Tunnels under walls and erupts beside the core |
| Shrieker | Screams to enrage zombies and jam turrets |
| Wraith | Phases through walls; resists physical, weak to holy and arcane |
| Wrecker | Hunts turrets with a siege hammer (3× damage to turrets) |
| Necromancer | Raises shamblers and heals the horde |

**Champion affixes:** Vampiric, Molten, Frostbound, Shielded, Hasted, Splitting, Blinking, Reflective, Warded, Juggernaut.

**Bosses** (each has a boss bar, phases and its own moveset):
* **Gorehound the Butcher**: cleaver combo, meat-hook pull, rampage charge, Blood Frenzy at 50%.
* **The Plague Mother**: toxic spew, brood birth, acid rain, crushing leap in phase 2.
* **Malgrave the Dread Knight**: greatsword combo, shadow charge, dark wave, summons death knights, Soulfire Ascension.
* **The Ossuary Colossus**: boulder throws (targets turrets), quake stomp, bone spikes, core smash, ribcage breach.
* **Varkhul the Lich King**: soul volleys, frost nova, death coil. Phylactery crystals make him invulnerable until they are destroyed. Raises an army of the dead.
* **Morvathrax the Rotwing**: undead dragon. Strafes, dives and rains bone, then crashes down at 40% and fights on the ground.

## Turrets (10 types, levels 1–5)
Each level raises damage (×1.5/2.2/3.1/4.4), range, fire rate and hull, and changes the model.
Level 3 and level 5 each unlock an ability. Turrets can be repaired, sold (60% refund) and switched between targeting modes
(closest to core, nearest, strongest, weakest).

| Turret | Status effect | Lv3 | Lv5 |
|---|---|---|---|
| Ballista | Bleeding | Twin bolts | Piercing bolts |
| Inferno Turret | Burn / Scorched | Napalm ground | Soulfire (burns fire-immune foes) |
| Cryo Spire | Chill → Frozen | Splintering shards | Absolute Zero nova |
| Tidecaller | Soaked (+50% lightning damage taken) | Breaker waves | Maelstrom whirlpools |
| Tesla Coil | Shocked (stun), chains | 5-target arcs | Overload strikes everything in range |
| Venom Lobber | Corroded (armor shred) | Larger acid pools | Plague clouds |
| Siege Mortar | Concussed | Cluster shells | 5-shell bombardment |
| Arcane Prism | Marked (+damage taken), ramping beam | Refracts to a second target | Piercing lance |
| Gale Tower | Disoriented, knockback | Tornadoes | Cyclone wall |
| Sanctum Obelisk | Purified; heals allies and the core | Aegis aura | Divine Judgement |

Elements combine. Soaked targets take extra lightning damage and get chained further. Frozen targets shatter for +50% physical or explosive damage.

## Heroes (10, increasing in price)
Summon a hero with a **Sigil**. **Sneak + right-click** a hero to cycle its orders, or right-click it for its command screen.
Press **G** to give all your heroes the same order.
* **Guard Core**: stays by the core and attacks whatever gets closest to it.
* **Hunt Freely**: roams the field and goes after bosses, champions, necromancers and shriekers first.
* **Follow Me**: escorts you.

Heroes level up from kills (Lv1–5, +12% stats per level, extra gear on the model). If a hero dies, you can revive it at the core for half its price.

| Hero | Cost | Moveset |
|---|---|---|
| Ashfang the Dire Wolf | 350 | Rending bite, pounce, Howl of the Pack |
| Sylva the Huntress | 600 | Archer: piercing arrow, arrow rain, evasive roll, hunter's mark |
| Bastion, the Stone Colossus | 900 | Tank: taunt, earthshatter, stoneskin |
| Sir Aldric, Knight of Dawn | 1300 | Light-sword combo, radiant dash, Judgement (pillars of light), Aegis of Dawn |
| Morgana the Stormweaver | 1800 | Chain lightning, frost nova, blink, meteor |
| Kage the Ronin | 2600 | Iaido flash, shadow clones, Thousand Cuts |
| AEGIS-7 War Mech | 3600 | Rotary cannons, missile swarm, seismic stomp, overdrive |
| Ignis the Phoenix | 5000 | Flying; flame dive, healing embers, rebirth once per wave |
| Vorthax the Elder Dragon | 7000 | Flying; dragonfire, tail sweep, wing gust, terrifying roar, meteor storm |
| Zeus, King of Olympus | 10000 | Thunderbolt javelins, chain storm, Olympian Smite (boss killer), team Aegis shield, Wrath of the Sky |

## The War Room shop
* **Arsenal:** Ravager machine gun (hold to fire, overheats), Thunderboom scattergun, Hellfire rocket launcher, Starpiercer railgun (charge and pierce),
  Dragon's Breath flamethrower, Stormfist tesla gauntlet, Glacier cryo cannon, Sunlance holy repeater, Voidedge plasma katana (phase-dash),
  Vanguard armor set, tower shield.
* **Support:** Orbital Strike remote, Airstrike flare, Sentry Drone, and the **Titan Exosuit Beacon**: a drop pod falls from orbit and
  unfolds into a power suit you climb into. It has 90 seconds of rotary cannons (attack), homing missile salvos (use) and jump-jets (jump).
  The suit absorbs all damage aimed at you. Grenades: frag, incendiary, cryo, holy, singularity.
* **Supplies:** bandages, medkits, berserker stims, core repair kits, golden apples, totems, arrows.
* **Fortify:** wooden/stone/iron/runic barricades (reinforced tiers drop to the tier below when breached; runic wards zap attackers),
  spike traps, landmines, Medic Shrine, Soul Harvester (+25% credits nearby), Repair Pylon.
* **Core upgrades:** Fortified Core (+30% HP/level), Arcane Barrier (regenerating shield), Retaliation Pulse, War Treasury (+10% credits/level),
  Turret Overclock (+6% fire rate/level), Rally Beacon. Emergency core repair.

## Commands
`/siege status` · `/siege start` · `/siege setwave <n>` · `/siege credits <player> <amount>` ·
`/siege spawn <zombie> [rank 1-5] [champion]` · `/siege boss <boss>` · `/siege defeat` (all except `status` require operator permissions)

## Configuration
`config/lastbastion-common.toml`: starting credits, credit multiplier, shared team wallet, difficulty, spawn radius, build radius,
whether zombies breach blocks and the hardness cap, harsh defeat and salvage fraction, base core health, max alive enemies,
special nights, and player presence radius.

## Building
```
./gradlew build          # jar in build/libs/
./gradlew runClient      # dev client
./gradlew runGameTestServer   # server smoke tests (all turrets, heroes, zombies and bosses fighting)
```
`tools/gen_assets.py` regenerates the item/block/effect textures, models and lang file. Entity models and skins are built in code:
`client/model/*Models.java` define each model with a small modelling DSL. Their textures are painted procedurally when the game starts,
so the geometry and textures always match.
