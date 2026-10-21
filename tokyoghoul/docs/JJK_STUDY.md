# How Jujutsu Craft works, and how Tokyo Ghoul Craft rebuilds it

Notes from decompiling **Jujutsu Craft v51** (Forge 1.20.1, by *orca*, built with MCreator) and the
design decisions they led to. No Jujutsu Craft code is copied; this mod re-implements the same *systems*
in hand-written Java for the Tokyo Ghoul universe.

## 1. What the jar is

| | |
|---|---|
| Tool | MCreator (generated code, `net.mcreator.jujutsucraft`) |
| Size | ~6,500 classes: ~1,200 `*Procedure` classes, ~920 entities, ~440 items |
| Libraries | GeckoLib (animated mob models), PlayerAnimator (player animations) |
| Data | ~210 advancements (most are invisible unlock flags), lang, models, sounds |

Every behaviour is a static `XxxProcedure.execute(world, x, y, z, entity)` method. State lives in
`entity.getPersistentData()` doubles (`skill`, `cnt1`…`cnt6`, `PRESS_Z`) and in one big player capability
(`JujutsucraftModVariables.PlayerVariables`).

## 2. The systems, one by one

### First join: the Insect and the book
`WhenPlayerJointheWorldProcedure` → `WhenRespawnProcedure`: grants the hidden `update` advancement, rolls a
random technique if none is set, sets cursed energy to max, and the starter items include the
**Insect** (right-click: infinite satiety) and the technique-changer book.

**Tokyo Ghoul Craft:** first join gives the **Ghoul Registry** (book) and the **Anteiku Thermos**
(right-click toggles infinite satiety). `PlayerEvents.onLogin`, game rule `tgStarterKit`.

### Picking a character
The book opens `SelectTechniqueScreen` (two pages, labelled Sorcerers / Cursed Spirits / Curse Users).
Each button runs `SelectGojoProcedure` & co.: set `PlayerCurseTechnique = 2`, grant `skill_gojo`, give the
costume (uniform, blindfold or sunglasses).

**TGC:** `SelectScreen` with Anteiku / Aogiri Tree / CCG / Quinx Squad / Others tabs, a live 3D preview and
the full moveset. `PlayerActions.select` sets the character and gives their quinques, mask and CCG coat.

### The R / Z keys and the technique rotation
* `KeyChangeTechniqueOnKeyPressed` (**R**, shift = backwards) walks `PlayerSelectCurseTechnique` through
  slots 0–21: 0–2 are the universal *Attack / Knockback Attack / Barrage Attack*, 3–19 the character's
  techniques, 20 the domain, 21 "cancel domain". Each slot has a name and a cost; locked slots show
  `-----` and are skipped (`ChangeTechniqueTestProcedure`).
* `KeyStartTechnique` (**Z**) sets `PRESS_Z = true` and calls `StartCursedTechniqueProcedure`, which spends
  cursed energy, writes `skill = technique * 100 + slot` (so Gojo's Blue is `206`) and applies the
  `CURSED_TECHNIQUE` effect. Releasing Z clears `PRESS_Z`, which ends held moves such as barrages.
* Every tick the effect calls `CursedTechniqueOnPotionActiveTickProcedure`, a long `if (skill >= …)` chain
  that dispatches to `CursedTechniqueGojoProcedure`, which dispatches again by slot.
* Cooldowns are effects: `COOLDOWN_TIME` (technique) and `COOLDOWN_TIME_COMBAT` (melee), with length
  `max(cost / 2, 20)` ticks.

**TGC:** same keys (R / Shift+R / Z hold / X reset) plus C (kagune), G (guard), B (dodge), M (regenerate).
Instead of `skill == 206` chains, each move is a `MoveDef` (kind + tuned parameters) executed by one
`MoveRunner` shared by players and NPCs. `CharacterDef.rotation()` = universal attacks + character moves +
ultimate, the same layout as JJK's slots.

### Hits: RangeAttackProcedure
The core of combat. Each hit: check `LogicAttackProcedure` (factions), apply guard reduction (the `GUARD`
effect or a defending technique), roll **Black Flash**, damage with a custom damage type, apply velocity
knockback scaled by the `cnt6` charge, then spawn hit particles.

**TGC:** `combat/Combat.hit` does faction checks, guard (frontal, 80% reduction, Rc drain, guard break),
counter stances, dodge frames, hit-stun that **interrupts the victim's move** (attack trading), knockback,
and per-source damage types (`kagune`, `quinque`, `strike`). Ghouls take half damage from ordinary
weapons; quinques hit them harder.

### Factions
`LogicAttackProcedure` compares the persistent-data tags `JujutsuSorcerer`, `CursedSpirit` and
`CurseUser`, plus owner chains for shikigami. Same tag means no damage, except when one is targeting the
other (retaliation). `LogicAttackTargetStartProcedure` picks targets for NPCs.

**TGC:** `data/Faction` (Human, CCG, Anteiku, Aogiri, Wild, Clowns) with `hunts()` / `canHurt()`. A
player's faction is their character's. CCG NPCs hunt ghoul players; Aogiri and wild ghouls hunt humans
and investigators; Anteiku only fights back.

### Progression: fame → grade → level → unlocks
* `WhenEntityDieProcedure`: an enemy's level is its **Strength effect amplifier**. Killing it is worth
  `(level+1)^2 + 1` fame to the killer ("MVP") and a tenth to everyone nearby who fought.
* Fame thresholds (50 / 200 / 500 / 1000 / 1750 / 2750 / 4000, times a difficulty game rule) award the
  grade advancements (Grade 4 … Special Grade). The upper grades also require having beaten a
  strong-enough enemy.
* `CompletedAdvancementProcedure` maps grade → `PlayerLevel`; max cursed energy is
  `former * round((2 + level) / 1.1)`.
* Each technique slot is unlocked by an advancement granted by `AdvancementGojoProcedure` & co. when the
  player reaches a grade. The domain needs a "mastery" advancement.

**TGC:** `player/Progression` uses the same formula and an MVP split. Ranks are **ghoul ratings**
(Unrated → SSS) or **CCG ranks** (Academy → Special Class) depending on race (`data/Rank`). Rank raises
max Rc, health and damage; every move has a `minRank`. Ultimates that are a kakuja need **kakuja
progress** (eat kakuhou, devour ghouls), the counterpart of JJK's mastery gate.

### Domain expansion
Slot 20. It builds a barrier sphere, applies sure-hit, and amplification and simple domain counter it.

**TGC:** the Tokyo Ghoul equivalent is the **ultimate**: kakuja transformations (Centipede, One-Eyed Owl,
Owl, Jason's armour, Tatara's blazing serpent), Shinohara's Arata armour, Arima's White Reaper, or
signature finishers (Shard Storm, Chimera Kagune…). Transformations scale the model up, add kakuja/Arata
armour, multiply damage, cut damage taken, regenerate, and resist hit-stun.

### NPC AI ("anime combat")
* Entities extend `Monster` with a `MeleeAttackGoal` whose reach is **0**, so vanilla melee never fires.
  The goal only closes distance; all fighting happens in `baseTick → AIxxxProcedure`.
* `AIActiveProcedure`: level-scaled Speed/Jump boosts when far (amplifier = level / 4), sprinting, and
  simple-domain reactions.
* The per-character AI (e.g. `AIGojoSchoolDaysProcedure`) runs every 10 ticks when idle:
  desperation moves at low health (Hollow Purple below 30% health), domain logic, otherwise a random
  technique pick `rnd = 4 + random * 16` validated against distance, retried up to 128 times.
* `CalculateAttackProcedure`: if no technique fits, choose melee by distance band (`2.5 / 5 / 10 / 20 ×
  size`): jab / knockback / barrage close up, jump or run attacks far away. Defensive choices are made
  when "danger" (incoming projectiles or enemy techniques) is detected.
* Spawning is gated by `STRONGEST_PLAYER`: `SpawnLevel1..5` probabilities rise with the strongest
  player's level, so Gojo and Sukuna show up once someone can face them.

**TGC:** `entity/ai/CombatBrain` (technique pick with retries, distance bands, desperation ultimate,
**guard/dodge reactions** to wind-ups and projectiles), `CombatMovementGoal` (sprint in with level-scaled
boosts, circle-strafe, hop, keep range for Ukaku users) and `world/Encounters` (night ghoul raids for
humans/CCG, CCG squads for ghoul players, named bosses gated by the strongest player's rank).

### Models
JJK uses GeckoLib `.geo.json` models with animation controllers (movement / attacking / procedure); the
procedure controller plays a synced `ANIMATION` string.

**TGC:** no model files. Characters are procedural (`client/model/CharacterModels`), built from a
`Look` (build, hair style, outfit, mask, glasses, stitches…) on a jointed skeleton, then posed by
`CharacterAnimator` from a synced animation id. Kagune (`KaguneModels`) are chained segments that bend
and extend per move. Quinques and masks are procedural too. Textures are painted at runtime. Players who
pick a character are rendered as that character.
