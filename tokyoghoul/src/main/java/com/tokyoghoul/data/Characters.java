package com.tokyoghoul.data;

import com.tokyoghoul.data.CharacterDef.Group;
import com.tokyoghoul.data.Look.Build;
import com.tokyoghoul.data.Look.Hair;
import com.tokyoghoul.data.Look.Mask;
import com.tokyoghoul.data.Look.Top;
import com.tokyoghoul.move.Anims;
import com.tokyoghoul.move.MoveDef.Kind;
import com.tokyoghoul.move.MoveDef.Shot;
import com.tokyoghoul.move.MoveDef.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tokyoghoul.data.Moves.b;
import static com.tokyoghoul.data.Moves.k;
import static com.tokyoghoul.data.Moves.kaguneSweep;
import static com.tokyoghoul.data.Moves.kakuja;
import static com.tokyoghoul.data.Moves.q;
import static com.tokyoghoul.data.Moves.rinkakuPierce;
import static com.tokyoghoul.data.Moves.tailStrike;
import static com.tokyoghoul.data.Moves.ukakuVolley;

/**
 * The roster: every major Tokyo Ghoul / :re character with their canon kagune or quinques and named
 * techniques, plus the generic ghouls, investigators and civilians that populate the world. Moves unlock
 * with rank in the order listed (the {@code rank} on each move), like Jujutsu Craft's skill advancements.
 */
public final class Characters {
    private static final Map<String, CharacterDef> ALL = new LinkedHashMap<>();

    public static final CharacterDef KANEKI, TOUKA, NISHIKI, YOMO, YOSHIMURA, TSUKIYAMA, HINAMI, RIZE;
    public static final CharacterDef AYATO, ETO, TATARA, JASON, TAKIZAWA, KURONA, NORO, FURUTA;
    public static final CharacterDef ARIMA, AMON, JUUZOU, SHINOHARA, KUREO, AKIRA;
    public static final CharacterDef HAISE, URIE, SAIKO, SHIRAZU, MUTSUKI;
    public static final CharacterDef UKAKU_GHOUL, KOUKAKU_GHOUL, RINKAKU_GHOUL, BIKAKU_GHOUL, AOGIRI_GRUNT, INVESTIGATOR, HUMAN, CIVILIAN;

    static {
        // ================================================================== Anteiku
        KANEKI = add(new CharacterDef("kaneki", "Ken Kaneki", Group.ANTEIKU, Faction.ANTEIKU, Race.ONE_EYED,
                Look.of().hair(Hair.MESSY, 0xEDEDED).eyes(0x8A8A90).top(Top.BODYSUIT, 0x16161C, 0x2A2A33).pants(0x16161C)
                        .mask(Mask.KANEKI, 0x121214, 0xE8E2D6).with(Look.EYEPATCH)))
                .kagune(KaguneType.RINKAKU, 0xC0102A, 4).rc(120).hp(4).npc(8, 420, 0.34f, 8).mask("mask_kaneki")
                .blurb("The One-Eyed King. A half-ghoul whose Rinkaku grows into the Centipede kakuja.")
                .move(rinkakuPierce("rinkaku_pierce", "Rinkaku Pierce"))
                .move(kaguneSweep("tentacle_lash", "Tentacle Lash").rank(2))
                .move(k("kagune_grab", "Kagune Grab", Kind.GRAB).dmg(14).cost(40).rank(2))
                .move(k("rinkaku_barrage", "Rinkaku Barrage", Kind.BARRAGE).dmg(2.2f).range(5).radius(1.8f).duration(30).interval(2)
                        .anim(Anims.K_BARRAGE).cost(35).cd(80).rank(3).ai(1, 5))
                .move(k("binge", "Binge", Kind.DEVOUR).dmg(10).cost(0).rank(3))
                .move(k("centipede_kick", "Centipede Kick", Kind.DASH).dmg(12).duration(9).speed(1.5f).kb(1.6f).anim(Anims.KICK).cost(30)
                        .cd(70).rank(4))
                .ultimate(kakuja("centipede", "Kakuja: Centipede", 1.15f));

        TOUKA = add(new CharacterDef("touka", "Touka Kirishima", Group.ANTEIKU, Faction.ANTEIKU, Race.GHOUL,
                Look.of().hair(Hair.SIDE_SWEPT, 0x2C2F5A).eyes(0x4A6AD0).top(Top.HOODIE, 0x1C1C26, 0x3A2A55).pants(0x15151C).female()
                        .mask(Mask.RABBIT, 0x2A2140, 0x9B7BD8)))
                .kagune(KaguneType.UKAKU, 0x8E3AD8, 2).rc(110).hp(0).npc(6, 260, 0.38f, 4).mask("mask_rabbit")
                .blurb("Anteiku's Rabbit. Her Ukaku rains crystal shards and crackles with lightning.")
                .move(ukakuVolley("shard_barrage", "Shard Barrage"))
                .move(k("ukaku_dash", "Ukaku Dash", Kind.DASH).dmg(7).duration(8).speed(1.7f).cost(15).cd(40).rank(2))
                .move(b("rabbit_kick", "Rabbit Kick", Kind.STRIKE).dmg(9).kb(2.2f).lift(0.5f).anim(Anims.KICK).lunge().cost(10).cd(30).rank(2))
                .move(k("lightning_shards", "Lightning Shards", Kind.LIGHTNING).src(Source.LIGHTNING).dmg(6).count(5).color(0xB89BFF).cost(45)
                        .rank(4))
                .ultimate(k("shard_storm", "Ukaku: Shard Storm", Kind.STORM).ultimate(true).dmg(7).count(40).cost(140).cd(1200).rank(5));

        NISHIKI = add(new CharacterDef("nishiki", "Nishiki Nishio", Group.ANTEIKU, Faction.ANTEIKU, Race.GHOUL,
                Look.of().hair(Hair.SHORT, 0x3A2A22).eyes(0x5A3A2A).top(Top.SHIRT, 0x34506B, 0xD8D8D8).pants(0x2A2A38).with(Look.GLASSES)
                        .mask(Mask.NISHIKI, 0x2A2D52, 0x8A8FD0)))
                .kagune(KaguneType.BIKAKU, 0xB0204A, 1).rc(100).hp(2).npc(4, 160, 0.33f, 4).mask("mask_nishiki")
                .blurb("A Kamii student with a sharp tongue and a whip-fast Bikaku tail.")
                .move(tailStrike("tail_strike", "Tail Strike"))
                .move(kaguneSweep("tail_sweep", "Tail Sweep").count(1).range(5).rank(2))
                .move(k("tail_impale", "Tail Impale", Kind.PIERCE).dmg(13).range(8).count(1).windup(10).kb(0.6f).cost(30).cd(60).rank(3))
                .move(k("bikaku_spin", "Bikaku Spin", Kind.SWEEP).dmg(5).range(4).count(4).interval(4).lift(0.5f).cost(35).cd(90).rank(4))
                .ultimate(b("nishiki_frenzy", "Starving Frenzy", Kind.BUFF).ultimate(false).duration(600).cost(100).cd(1800).rank(5));

        YOMO = add(new CharacterDef("yomo", "Renji Yomo", Group.ANTEIKU, Faction.ANTEIKU, Race.GHOUL,
                Look.of().hair(Hair.SIDE_SWEPT, 0xB8BCC4).eyes(0x6A6A70).top(Top.JACKET, 0x1A1A20, 0x2E2E36).pants(0x1A1A20)
                        .build(Build.ATHLETIC).height(1.06f).mask(Mask.CROW, 0x141418, 0x6A6A70)))
                .kagune(KaguneType.UKAKU, 0x3A1F2A, 2).rc(130).hp(6).npc(7, 340, 0.36f, 8).mask("mask_crow")
                .blurb("The silent Crow of Anteiku. Ukaku shards and electric wings.")
                .move(ukakuVolley("crow_volley", "Crow Volley").color(0xD03040))
                .move(k("wing_strike", "Lightning Wing Strike", Kind.DASH).src(Source.LIGHTNING).dmg(10).duration(9).speed(1.6f).color(0xC0D0FF)
                        .cost(25).cd(60).rank(2))
                .move(b("crow_haymaker", "Crow Haymaker", Kind.STRIKE).dmg(11).kb(2.4f).windup(9).anim(Anims.HEAVY).lunge().cost(10).rank(3))
                .move(k("feather_storm", "Feather Storm", Kind.LIGHTNING).src(Source.LIGHTNING).dmg(7).count(6).color(0xD03040).cost(50).rank(4))
                .ultimate(k("crows_fury", "Crow's Fury", Kind.STORM).ultimate(false).dmg(8).count(48).color(0xD03040).cost(140).cd(1200).rank(5));

        YOSHIMURA = add(new CharacterDef("yoshimura", "Kuzen Yoshimura", Group.ANTEIKU, Faction.ANTEIKU, Race.GHOUL,
                Look.of().hair(Hair.SLICKED, 0xE6E6E6).eyes(0x4A4A4A).top(Top.VEST, 0x1A1A1A, 0xF0F0F0).pants(0x1A1A1A).with(Look.BEARD | Look.TIE)
                        .height(1.04f).mask(Mask.OWL, 0x2A2A2E, 0xD8D0B8)))
                .kagune(KaguneType.UKAKU, 0x6A2A3A, 3).rc(150).hp(6).npc(9, 600, 0.31f, 12).mask("mask_owl")
                .blurb("Anteiku's manager, the Non-Killing Owl. His kakuja is a towering owl.")
                .move(ukakuVolley("owl_feathers", "Owl Feathers"))
                .move(k("wing_slash", "Wing Slash", Kind.SWEEP).dmg(8).range(5).count(1).cost(25).cd(50).rank(2))
                .move(b("anteiku_palm", "Anteiku Palm", Kind.STRIKE).dmg(10).kb(2.6f).windup(8).cost(10).rank(2))
                .move(k("owl_dive", "Owl Dive", Kind.LEAP_SLAM).dmg(14).radius(4.5f).cost(40).rank(3))
                .ultimate(kakuja("owl_kakuja", "Kakuja: Non-Killing Owl", 1.7f));

        TSUKIYAMA = add(new CharacterDef("tsukiyama", "Shuu Tsukiyama", Group.ANTEIKU, Faction.ANTEIKU, Race.GHOUL,
                Look.of().hair(Hair.SLICKED, 0x5A3A8A).eyes(0x6A4A9A).top(Top.SUIT, 0x4A2E6E, 0xC8A8E8).pants(0x3A2458).height(1.05f)
                        .mask(Mask.GOURMET, 0xE8E0F0, 0x6A3AB0)))
                .kagune(KaguneType.KOUKAKU, 0x6A3AB0, 1).rc(110).hp(6).npc(6, 280, 0.34f, 10).mask("mask_gourmet")
                .blurb("The Gourmet. A Koukaku drill-lance and very refined taste.")
                .move(k("koukaku_lance", "Koukaku Lance", Kind.PIERCE).dmg(10).range(6).count(1).windup(6).anim(Anims.THRUST).cost(15).cd(30))
                .move(k("spiral_thrust", "Spiral Thrust", Kind.DASH).dmg(11).duration(10).speed(1.6f).anim(Anims.THRUST).cost(25).rank(2))
                .move(k("gourmet_guard", "Gourmet's Guard", Kind.GUARD_COUNTER).dmg(12).anim(Anims.K_SHIELD).cost(20).rank(2))
                .move(k("bon_appetit", "Bon Appetit!", Kind.DEVOUR).dmg(10).rank(3))
                .move(k("tres_bien", "Tres Bien Rush", Kind.COMBO).dmg(6).range(4).count(5).interval(5).anim(Anims.THRUST).cost(35).cd(80).rank(4))
                .ultimate(k("gourmet_drill", "Gourmet's Grand Drill", Kind.DASH).ultimate(false).dmg(30).duration(16).speed(2.0f).radius(2.2f)
                        .anim(Anims.THRUST).cost(130).cd(1200).rank(5));

        HINAMI = add(new CharacterDef("hinami", "Hinami Fueguchi", Group.ANTEIKU, Faction.ANTEIKU, Race.GHOUL,
                Look.of().hair(Hair.BOB, 0x6B4A2E).eyes(0x6B4A2E).top(Top.DRESS, 0xE0C8A0, 0x8A6A4A).pants(0x3A2A20).build(Build.SLIM)
                        .height(0.9f).female().mask(Mask.PLAIN, 0xF0E8E0, 0xB04040)))
                .kagune(KaguneType.KOUKAKU, 0xD03A40, 1).kagune2(KaguneType.RINKAKU, 0xE87A3A).rc(110).npc(5, 200, 0.34f, 8)
                .blurb("A chimera: her mother's Koukaku and her father's Rinkaku together.")
                .move(k("chimera_lances", "Chimera Lances", Kind.PIERCE).dmg(8).range(8).count(3).interval(4).cost(25).cd(40))
                .move(k("koukaku_shield", "Koukaku Shield", Kind.GUARD_COUNTER).dmg(8).anim(Anims.K_SHIELD).cost(15).rank(2))
                .move(k("ryouko_bloom", "Ryouko's Bloom", Kind.SPIKES).dmg(9).count(8).cost(35).rank(3))
                .move(kaguneSweep("chimera_whirl", "Chimera Whirl").rank(4))
                .ultimate(k("chimera_kagune", "Chimera Kagune", Kind.SPIKES).ultimate(true).dmg(14).count(24).range(1.4f).interval(1).cost(130)
                        .cd(1200).rank(5));

        RIZE = add(new CharacterDef("rize", "Rize Kamishiro", Group.ANTEIKU, Faction.WILD, Race.GHOUL,
                Look.of().hair(Hair.LONG, 0x6E3A8A).eyes(0x7A4A9A).top(Top.DRESS, 0x2A1A3A, 0x6E3A8A).pants(0x2A1A3A).with(Look.GLASSES)
                        .female()))
                .kagune(KaguneType.RINKAKU, 0xC0102A, 4).rc(140).hp(4).npc(7, 360, 0.35f, 6)
                .blurb("The Binge Eater. Her kakuhou made Kaneki what he is.")
                .move(rinkakuPierce("rize_pierce", "Rinkaku Crush").dmg(10))
                .move(k("binge_eater", "Binge Eater", Kind.DEVOUR).dmg(12).rank(2))
                .move(k("tentacle_storm", "Tentacle Storm", Kind.BARRAGE).dmg(2.4f).range(5).radius(2).duration(30).anim(Anims.K_BARRAGE).cost(35)
                        .cd(80).rank(3).ai(1, 5))
                .move(k("rize_grab", "Gourmand's Grip", Kind.GRAB).dmg(16).cost(40).rank(4))
                .ultimate(k("binge_frenzy", "Binge Frenzy", Kind.FORM).ultimate(true).form(1.05f, 1.8f, 0.7f).duration(700).cost(140).cd(2400)
                        .rank(5));

        // ================================================================== Aogiri Tree
        AYATO = add(new CharacterDef("ayato", "Ayato Kirishima", Group.AOGIRI, Faction.AOGIRI, Race.GHOUL,
                Look.of().hair(Hair.MESSY, 0x1E2440).eyes(0x4A6AD0).top(Top.CLOAK, 0x15151C, 0x6A0F1A).pants(0x15151C)
                        .mask(Mask.BLACK_RABBIT, 0x101014, 0xC0C0D0).masked()))
                .kagune(KaguneType.UKAKU, 0x6A2AB8, 3).rc(120).hp(2).npc(7, 300, 0.38f, 6).mask("mask_black_rabbit")
                .blurb("Aogiri's Black Rabbit, Touka's brother. Relentless Ukaku barrages.")
                .move(ukakuVolley("shard_rain", "Shard Rain").count(14))
                .move(b("black_rabbit_kick", "Black Rabbit Kick", Kind.STRIKE).dmg(10).kb(2.2f).lift(0.6f).anim(Anims.KICK).lunge().cost(10).rank(2))
                .move(k("ukaku_blade", "Ukaku Blade", Kind.SWEEP).dmg(9).range(4).count(1).anim(Anims.SLASH).cost(20).cd(40).rank(2))
                .move(k("piercing_shards", "Piercing Shards", Kind.VOLLEY).shot(Shot.SPEAR).dmg(9).count(4).interval(5).speed(2.6f).cost(40)
                        .rank(4))
                .ultimate(kakuja("semi_kakuja", "Semi-Kakuja", 1.1f));

        ETO = add(new CharacterDef("eto", "Eto Yoshimura", Group.AOGIRI, Faction.AOGIRI, Race.ONE_EYED,
                Look.of().hair(Hair.WILD, 0x5A8F5A).eyes(0x7AAF6A).top(Top.BANDAGES, 0xD8D0C0, 0x8A7A6A).pants(0xD8D0C0).build(Build.SLIM)
                        .height(0.9f).female().with(Look.HOOD).mask(Mask.BANDAGE, 0xD8D0C0, 0x7A1A2A).masked()))
                .kagune(KaguneType.UKAKU, 0x7A1A2A, 4).rc(170).hp(6).npc(10, 750, 0.36f, 10).mask("mask_bandage")
                .blurb("The One-Eyed Owl, Aogiri's true leader. Her kakuja is a living fortress.")
                .move(ukakuVolley("bandaged_shards", "Bandaged Shards").dmg(5).count(16))
                .move(k("kagune_spears", "Kagune Spears", Kind.SPIKES).dmg(10).count(10).cost(35).rank(2))
                .move(k("owl_wing_sweep", "Owl Wing Sweep", Kind.SWEEP).dmg(10).range(6).count(1).cost(30).rank(3))
                .move(k("eto_grab", "Kakuja Claw", Kind.GRAB).dmg(18).range(8).cost(45).rank(4))
                .ultimate(kakuja("one_eyed_owl", "Kakuja: One-Eyed Owl", 2.1f));

        TATARA = add(new CharacterDef("tatara", "Tatara", Group.AOGIRI, Faction.AOGIRI, Race.GHOUL,
                Look.of().hair(Hair.SLICKED, 0xEDEDED).eyes(0xB03020).top(Top.ROBE, 0x16161A, 0xA02020).pants(0x16161A).height(1.08f)
                        .build(Build.ATHLETIC).mask(Mask.TATARA, 0xA01818, 0x2A0A0A).masked()))
                .kagune(KaguneType.BIKAKU, 0xC03A10, 1).rc(150).hp(6).npc(9, 560, 0.34f, 10).mask("mask_tatara")
                .blurb("Aogiri's leader in the field. His Bikaku burns with real flame.")
                .move(tailStrike("burning_tail", "Burning Tail").src(Source.FIRE))
                .move(k("flame_breath", "Flame Breath", Kind.FIRE).src(Source.FIRE).dmg(2.5f).cost(35).rank(2))
                .move(k("inferno_sweep", "Inferno Sweep", Kind.SWEEP).src(Source.FIRE).dmg(9).range(5).count(2).cost(35).rank(3))
                .move(b("red_chop", "Red Chop", Kind.STRIKE).dmg(13).kb(1.6f).windup(8).anim(Anims.OVERHEAD).lunge().cost(10).rank(3))
                .ultimate(kakuja("tatara_kakuja", "Kakuja: Blazing Serpent", 1.25f).src(Source.FIRE));

        JASON = add(new CharacterDef("jason", "Yamori (Jason)", Group.AOGIRI, Faction.AOGIRI, Race.GHOUL,
                Look.of().hair(Hair.BUZZ, 0xD8C890).eyes(0x5A4A30).top(Top.SUIT, 0xE8E8E0, 0x6A0F1A).pants(0xE8E8E0).build(Build.HUGE)
                        .height(1.18f).mask(Mask.JASON, 0xF0EEE6, 0x8A1A1A).masked()))
                .kagune(KaguneType.RINKAKU, 0xA01020, 4).rc(140).hp(10).npc(8, 520, 0.31f, 12).mask("mask_jason")
                .blurb("Aogiri's torturer. Thousand minus seven.")
                .move(b("jason_smash", "Jason Smash", Kind.STRIKE).dmg(12).kb(2.4f).radius(1.8f).windup(9).anim(Anims.OVERHEAD).cost(5))
                .move(rinkakuPierce("rinkaku_impale", "Rinkaku Impale").rank(2))
                .move(b("thousand_minus_seven", "1000 Minus 7", Kind.BARRAGE).dmg(1.8f).duration(30).cost(20).cd(80).rank(2))
                .move(k("kakuja_arm", "Kakuja Arm", Kind.STRIKE).dmg(18).range(4).radius(2.2f).kb(2.8f).windup(10).anim(Anims.OVERHEAD).cost(40)
                        .cd(80).rank(3))
                .ultimate(kakuja("jason_kakuja", "Kakuja: Centipede Armour", 1.3f));

        TAKIZAWA = add(new CharacterDef("takizawa", "Seidou Takizawa", Group.AOGIRI, Faction.AOGIRI, Race.ONE_EYED,
                Look.of().hair(Hair.WILD, 0xDADADA).eyes(0x5A5A5A).top(Top.JACKET, 0x1A1A1E, 0x3A0A10).pants(0x1A1A1E).build(Build.SLIM)))
                .kagune(KaguneType.UKAKU, 0x8A1A1A, 3).rc(140).hp(4).npc(8, 400, 0.4f, 6)
                .blurb("A former investigator turned into an artificial One-Eyed Owl. Unhinged and fast.")
                .move(ukakuVolley("owl_volley", "Owl Volley"))
                .move(k("frenzied_rush", "Frenzied Rush", Kind.DASH).dmg(10).duration(10).speed(1.8f).cost(20).rank(2))
                .move(k("mad_owl_slash", "Mad Owl Slash", Kind.COMBO).dmg(6).range(4.5f).count(4).anim(Anims.SLASH).cost(25).cd(50).rank(3))
                .ultimate(kakuja("takizawa_kakuja", "Kakuja: Owl", 1.4f));

        KURONA = add(new CharacterDef("kurona", "Kurona Yasuhisa", Group.AOGIRI, Faction.AOGIRI, Race.ONE_EYED,
                Look.of().hair(Hair.BOB, 0xE8E8E8).eyes(0x5A5A5A).top(Top.BODYSUIT, 0x101014, 0x3A3A44).pants(0x101014).female()
                        .with(Look.STITCHES).mask(Mask.KURONA, 0xE8E8E8, 0x101014)))
                .kagune(KaguneType.RINKAKU, 0xB01030, 2).rc(110).npc(6, 240, 0.37f, 4)
                .blurb("Artificial one-eyed ghoul from Kanou's lab. Twin scything tentacles.")
                .move(rinkakuPierce("twin_rinkaku", "Twin Rinkaku").count(2))
                .move(kaguneSweep("scythe_tentacles", "Scythe Tentacles").rank(2))
                .move(k("sisters_grab", "Sister's Grab", Kind.GRAB).dmg(12).rank(3))
                .ultimate(b("sister_bond", "Sister Bond", Kind.BUFF).ultimate(false).duration(600).cost(100).cd(1800).rank(5));

        NORO = add(new CharacterDef("noro", "Noro", Group.AOGIRI, Faction.AOGIRI, Race.GHOUL,
                Look.of().hair(Hair.BALD, 0x101010).top(Top.CLOAK, 0x0C0C0E, 0x2A2A2E).pants(0x0C0C0E).build(Build.BULKY).height(1.15f)
                        .with(Look.HOOD).mask(Mask.NORO, 0x0A0A0A, 0x8A1A1A).masked()))
                .kagune(KaguneType.RINKAKU, 0x501020, 1).rc(140).hp(10).npc(8, 600, 0.29f, 10)
                .blurb("A silent Aogiri executive whose regenerating kagune is covered in mouths.")
                .move(k("maw_pierce", "Maw Pierce", Kind.PIERCE).dmg(11).range(7).count(1).cost(20).cd(40))
                .move(k("devouring_maw", "Devouring Maw", Kind.DEVOUR).dmg(14).range(4).rank(2))
                .move(k("maw_sweep", "Maw Sweep", Kind.SWEEP).dmg(9).range(5).count(1).cost(25).rank(3))
                .ultimate(kakuja("noro_kakuja", "Kakuja: Thousand Mouths", 1.5f));

        FURUTA = add(new CharacterDef("furuta", "Nimura Furuta", Group.AOGIRI, Faction.CLOWNS, Race.ONE_EYED,
                Look.of().hair(Hair.SHORT, 0x1A1A1A).eyes(0x2A2A2A).top(Top.SUIT, 0x1A1A22, 0xC23B8E).pants(0x1A1A22)
                        .mask(Mask.CLOWN, 0xF0F0F0, 0xC23B8E)))
                .kagune(KaguneType.RINKAKU, 0xA01040, 4).rc(130).hp(4).npc(8, 380, 0.36f, 6).mask("mask_clown")
                .blurb("Washuu bastard and Clown. Rize's kakuhou and a smile.")
                .move(rinkakuPierce("clown_pierce", "Clown's Pierce"))
                .move(b("cheap_shot", "Cheap Shot", Kind.BLINK).dmg(9).cost(20).rank(2))
                .move(k("clown_whirl", "Clown Whirl", Kind.SWEEP).dmg(7).range(4.5f).count(3).interval(5).cost(30).rank(3))
                .ultimate(k("dragon_seed", "Dragon Seed", Kind.SPIKES).ultimate(true).dmg(14).count(20).range(1.5f).interval(1).cost(140).cd(1500)
                        .rank(5));

        // ================================================================== CCG
        ARIMA = add(new CharacterDef("arima", "Kishou Arima", Group.CCG, Faction.CCG, Race.HUMAN,
                Look.of().hair(Hair.SHORT, 0xE0E0E0).eyes(0x6A6A70).top(Top.TRENCH_COAT, 0xEDEDED, 0x1A1A1A).pants(0x1A1A1A).height(1.04f)
                        .with(Look.GLASSES | Look.TIE)))
                .rc(160).hp(12).npc(10, 800, 0.36f, 14).quinque("ixa", "narukami")
                .blurb("The CCG's Reaper. Undefeated with IXA and Narukami.")
                .move(q("ixa_lance", "IXA: Lance", Kind.PIERCE).dmg(11).range(6).count(2).interval(6).anim(Anims.THRUST).cost(15).cd(30))
                .move(q("ixa_shield", "IXA: Shield", Kind.GUARD_COUNTER).dmg(14).anim(Anims.GUARD).cost(15).cd(80).rank(2))
                .move(q("ixa_remote", "IXA: Remote Spikes", Kind.SPIKES).dmg(11).count(10).cost(35).rank(2))
                .move(q("narukami", "Narukami", Kind.LIGHTNING).src(Source.LIGHTNING).dmg(8).count(6).color(0x8AD8FF).cost(40).rank(3))
                .move(b("reaper_step", "Reaper Step", Kind.BLINK).src(Source.QUINQUE).dmg(14).cost(30).rank(4))
                .ultimate(q("shinigami", "White Reaper", Kind.FORM).ultimate(false).form(1.0f, 1.8f, 0.4f).duration(900).cost(150).cd(2400)
                        .rank(5));

        AMON = add(new CharacterDef("amon", "Koutarou Amon", Group.CCG, Faction.CCG, Race.HUMAN,
                Look.of().hair(Hair.BUZZ, 0x1A1A1A).eyes(0x3A2A20).top(Top.TRENCH_COAT, 0xC8B08A, 0x1A1A1A).pants(0x1A1A22).build(Build.BULKY)
                        .height(1.1f).with(Look.TIE)))
                .rc(110).hp(10).npc(7, 360, 0.32f, 10).quinque("doujima", "kura")
                .blurb("First Class investigator with an iron sense of justice and a giant club.")
                .move(q("kura_smash", "Kura Smash", Kind.STRIKE).dmg(11).kb(2.0f).radius(1.8f).windup(9).anim(Anims.OVERHEAD).cost(5))
                .move(q("doujima_swing", "Doujima Swing", Kind.SWEEP).dmg(10).range(4.5f).count(1).anim(Anims.SLASH).cost(15).rank(2))
                .move(q("doujima_crush", "Doujima Crush", Kind.LEAP_SLAM).dmg(15).radius(4).cost(30).rank(3))
                .move(b("amon_tackle", "Justice Tackle", Kind.DASH).dmg(9).kb(2.2f).cost(10).rank(3))
                .ultimate(b("amon_justice", "Unbending Justice", Kind.BUFF).ultimate(false).duration(600).cost(100).cd(1800).rank(5));

        JUUZOU = add(new CharacterDef("juuzou", "Juuzou Suzuya", Group.CCG, Faction.CCG, Race.HUMAN,
                Look.of().hair(Hair.MESSY, 0xEDEDED).eyes(0xB03040).top(Top.SHIRT, 0xF0F0F0, 0x1A1A1A).pants(0x1A1A1A).build(Build.SLIM)
                        .height(0.92f).with(Look.HAIRCLIPS | Look.STITCHES | Look.SUSPENDERS)))
                .rc(110).hp(2).npc(8, 320, 0.42f, 4).quinque("jason13", "scorpion")
                .blurb("Special Class prodigy. Scorpion knives and 13's Jason.")
                .move(q("scorpion_knives", "Scorpion 1/56", Kind.VOLLEY).shot(Shot.KNIFE).dmg(4).count(8).interval(2).speed(2.2f).anim(Anims.THROW)
                        .cost(15).cd(40))
                .move(q("jason_scythe", "13's Jason", Kind.SWEEP).dmg(11).range(5).count(2).interval(6).anim(Anims.SPIN).cost(25).rank(2))
                .move(b("acrobat_dash", "Acrobatic Dash", Kind.DASH).dmg(8).duration(8).speed(1.8f).cost(10).rank(2))
                .move(q("knife_rain", "Knife Rain", Kind.STORM).shot(Shot.KNIFE).dmg(5).count(24).anim(Anims.THROW).cost(45).cd(160).rank(4))
                .ultimate(q("jasons_harvest", "Jason's Harvest", Kind.SWEEP).ultimate(false).dmg(14).range(6).count(6).interval(5).anim(Anims.SPIN)
                        .cost(130).cd(1200).rank(5));

        SHINOHARA = add(new CharacterDef("shinohara", "Yukinori Shinohara", Group.CCG, Faction.CCG, Race.HUMAN,
                Look.of().hair(Hair.BUZZ, 0x5A4A3A).eyes(0x3A2A20).top(Top.SUIT, 0x1A1A1E, 0xE8E8E8).pants(0x1A1A1E).build(Build.HUGE)
                        .height(1.12f).with(Look.TIE)))
                .rc(120).hp(12).npc(9, 600, 0.3f, 14).quinque("oniyamada", "")
                .blurb("Special Class veteran, Juuzou's mentor, master of the Arata armour.")
                .move(q("oniyamada", "Oniyamada", Kind.STRIKE).dmg(11).range(3).radius(1.8f).kb(1.8f).windup(8).anim(Anims.OVERHEAD).cost(5))
                .move(b("veteran_combo", "Veteran Combo", Kind.COMBO).dmg(5).count(4).cost(10).rank(2))
                .move(q("arata_crush", "Arata Crush", Kind.LEAP_SLAM).dmg(16).radius(4.5f).cost(30).rank(3))
                .ultimate(q("arata", "Arata Armour", Kind.FORM).ultimate(false).form(1.2f, 1.6f, 0.35f).duration(900).cost(140).cd(2400).rank(5));

        KUREO = add(new CharacterDef("kureo", "Kureo Mado", Group.CCG, Faction.CCG, Race.HUMAN,
                Look.of().hair(Hair.WILD, 0xE0E0E0).eyes(0x8A2A2A).top(Top.TRENCH_COAT, 0xBFA67A, 0x2A2A2A).pants(0x2A2A2A).build(Build.SLIM)
                        .height(1.02f)))
                .rc(110).hp(4).npc(6, 260, 0.33f, 6).quinque("fueguchi", "")
                .blurb("First Class investigator obsessed with quinques. Fueguchi One and Two.")
                .move(q("fueguchi_one", "Fueguchi One", Kind.PIERCE).dmg(9).range(9).count(2).interval(5).anim(Anims.SLASH).cost(15).cd(40))
                .move(q("fueguchi_two", "Fueguchi Two", Kind.SWEEP).dmg(8).range(5).count(2).anim(Anims.SPIN).cost(20).rank(2))
                .move(q("quinque_snare", "Quinque Snare", Kind.GRAB).dmg(10).range(8).cost(30).rank(3))
                .ultimate(b("avenging_father", "Avenging Father", Kind.BUFF).ultimate(false).duration(600).cost(100).cd(1800).rank(5));

        AKIRA = add(new CharacterDef("akira", "Akira Mado", Group.CCG, Faction.CCG, Race.HUMAN,
                Look.of().hair(Hair.LONG, 0xE8D08A).eyes(0x6A4A2A).top(Top.TRENCH_COAT, 0x1A1A22, 0x6A6A70).pants(0x1A1A22).female()))
                .rc(120).hp(4).npc(7, 300, 0.34f, 6).quinque("amatsu", "fueguchi")
                .blurb("Kureo's daughter. Cold, brilliant, and armed with Amatsu.")
                .move(q("amatsu_volley", "Amatsu", Kind.VOLLEY).shot(Shot.SPEAR).dmg(6).count(6).interval(3).speed(2.4f).anim(Anims.CAST).cost(20))
                .move(q("fueguchi_lash", "Fueguchi Lash", Kind.PIERCE).dmg(9).range(9).count(2).anim(Anims.SLASH).cost(20).rank(2))
                .move(b("mado_counter", "Mado Counter", Kind.GUARD_COUNTER).dmg(10).cost(15).rank(3))
                .ultimate(q("amatsu_barrage", "Amatsu Full Release", Kind.STORM).ultimate(false).shot(Shot.SPEAR).dmg(8).count(30).cost(130)
                        .cd(1200).rank(5));

        // ================================================================== Quinx Squad
        HAISE = add(new CharacterDef("haise", "Haise Sasaki", Group.QUINX, Faction.CCG, Race.QUINX,
                Look.of().hair(Hair.MESSY, 0x1A1A1A).hairAccent(0xEDEDED).eyes(0x6A6A70).top(Top.TRENCH_COAT, 0x1A1A22, 0x3A3A44).pants(0x1A1A22)
                        .with(Look.ROOTS | Look.TIE)))
                .kagune(KaguneType.RINKAKU, 0xC0102A, 4).rc(130).hp(6).npc(8, 420, 0.35f, 8).quinque("yukimura", "")
                .blurb("Mentor of the Quinx Squad, wielding Yukimura 1/3 and a familiar Rinkaku.")
                .move(q("yukimura_slash", "Yukimura 1/3", Kind.COMBO).dmg(5).range(3.2f).count(4).anim(Anims.SLASH).cost(5))
                .move(rinkakuPierce("haise_pierce", "Rinkaku Pierce").rank(2))
                .move(k("haise_grab", "Kagune Grab", Kind.GRAB).dmg(14).cost(40).rank(3))
                .move(kaguneSweep("haise_lash", "Tentacle Lash").rank(4))
                .ultimate(kakuja("haise_kakuja", "Kakuja: Centipede", 1.15f));

        URIE = add(new CharacterDef("urie", "Kuki Urie", Group.QUINX, Faction.CCG, Race.QUINX,
                Look.of().hair(Hair.SHORT, 0x2A2A30).eyes(0x5A5A60).top(Top.QUINX, 0x15151A, 0x3A3A44).pants(0x15151A).build(Build.ATHLETIC)))
                .kagune(KaguneType.KOUKAKU, 0xA02030, 2).rc(120).hp(4).npc(7, 320, 0.36f, 8)
                .blurb("Quinx squad leader. His Koukaku forms blades along his arms.")
                .move(k("koukaku_blades", "Koukaku Blades", Kind.COMBO).dmg(6).range(3.4f).count(3).anim(Anims.SLASH).cost(10))
                .move(k("urie_lunge", "Frenzy Lunge", Kind.DASH).dmg(10).cost(20).rank(2))
                .move(k("blade_spin", "Blade Spin", Kind.SWEEP).dmg(8).range(4).count(3).interval(4).cost(30).rank(3))
                .ultimate(kakuja("urie_kakuja", "Kakuja: Frame Breaker", 1.2f));

        SAIKO = add(new CharacterDef("saiko", "Saiko Yonebayashi", Group.QUINX, Faction.CCG, Race.QUINX,
                Look.of().hair(Hair.TWINTAILS, 0x5A7AD0).eyes(0x5A7AD0).top(Top.QUINX, 0x15151A, 0x5A7AD0).pants(0x15151A).build(Build.SLIM)
                        .height(0.86f).female()))
                .kagune(KaguneType.RINKAKU, 0x8A40D0, 6).rc(170).hp(0).npc(6, 260, 0.32f, 4)
                .blurb("The laziest Quinx with the largest Rinkaku in the squad.")
                .move(kaguneSweep("massive_rinkaku", "Massive Rinkaku").range(6).dmg(9))
                .move(k("tentacle_rain", "Tentacle Rain", Kind.SPIKES).dmg(9).count(10).cost(35).rank(2))
                .move(rinkakuPierce("saiko_pierce", "Saiko Spears").count(6).interval(3).rank(3))
                .ultimate(k("saiko_giant", "Giant Kagune", Kind.FORM).ultimate(true).form(1.0f, 2.0f, 0.6f).duration(700).cost(150).cd(2400)
                        .rank(5));

        SHIRAZU = add(new CharacterDef("shirazu", "Ginshi Shirazu", Group.QUINX, Faction.CCG, Race.QUINX,
                Look.of().hair(Hair.SPIKY, 0xE0C040).hairAccent(0x1A1A1A).eyes(0x6A4A2A).top(Top.QUINX, 0x15151A, 0xE0C040).pants(0x15151A)))
                .kagune(KaguneType.RINKAKU, 0xC03030, 2).rc(110).hp(4).npc(6, 260, 0.35f, 6)
                .blurb("Quinx with scissor-shaped Rinkaku who fights for his sister.")
                .move(k("scissor_tentacles", "Scissor Tentacles", Kind.PIERCE).dmg(9).range(7).count(2).interval(6).cost(20))
                .move(k("scissor_cut", "Scissor Cut", Kind.GRAB).dmg(13).range(6).cost(35).rank(2))
                .move(kaguneSweep("shirazu_sweep", "Pincer Sweep").rank(3))
                .ultimate(kakuja("shirazu_kakuja", "Kakuja Awakening", 1.1f));

        MUTSUKI = add(new CharacterDef("mutsuki", "Tooru Mutsuki", Group.QUINX, Faction.CCG, Race.QUINX,
                Look.of().hair(Hair.SHORT, 0x2A3460).eyes(0x6A7AA0).top(Top.QUINX, 0x15151A, 0x2A3460).pants(0x15151A).build(Build.SLIM)
                        .with(Look.EYEPATCH)))
                .kagune(KaguneType.KOUKAKU, 0x4060A0, 2).rc(110).hp(2).npc(6, 240, 0.37f, 6)
                .blurb("A Quinx with a Koukaku and a bag of knives.")
                .move(b("knife_throw", "Knife Throw", Kind.VOLLEY).shot(Shot.KNIFE).dmg(4).count(6).interval(3).anim(Anims.THROW).cost(10).cd(40))
                .move(k("koukaku_slash", "Koukaku Slash", Kind.SWEEP).dmg(9).range(4).count(1).anim(Anims.SLASH).cost(15).rank(2))
                .move(b("knife_flurry", "Knife Flurry", Kind.COMBO).dmg(5).count(6).interval(3).anim(Anims.SLASH).cost(20).rank(3))
                .ultimate(b("mutsuki_madness", "Torso's Madness", Kind.BUFF).ultimate(false).duration(600).cost(100).cd(1800).rank(5));

        // ================================================================== Others / generic
        UKAKU_GHOUL = add(new CharacterDef("ukaku_ghoul", "Ukaku Ghoul", Group.OTHERS, Faction.WILD, Race.GHOUL,
                Look.of().hair(Hair.MESSY, 0x2A2A2A).top(Top.HOODIE, 0x2A2A34, 0x4A4A55).pants(0x20202A).mask(Mask.PLAIN, 0xD8D8D8, 0x8A1A1A)))
                .kagune(KaguneType.UKAKU, 0xB02050, 2).rc(90).npc(3, 70, 0.34f, 2).generic()
                .blurb("A nameless ghoul with a wing-type kagune.")
                .move(ukakuVolley("ghoul_shards", "Shard Volley"))
                .move(k("ghoul_wing_dash", "Wing Dash", Kind.DASH).dmg(6).cost(15).rank(2))
                .move(k("ghoul_lightning", "Ukaku Spark", Kind.LIGHTNING).src(Source.LIGHTNING).dmg(5).count(3).cost(35).rank(3))
                .ultimate(kakuja("ghoul_ukaku_kakuja", "Kakuja", 1.15f));

        KOUKAKU_GHOUL = add(new CharacterDef("koukaku_ghoul", "Koukaku Ghoul", Group.OTHERS, Faction.WILD, Race.GHOUL,
                Look.of().hair(Hair.SHORT, 0x3A2A1A).top(Top.JACKET, 0x3A3020, 0x5A4A30).pants(0x2A2418).build(Build.BULKY)
                        .mask(Mask.HANNYA, 0x8A1A1A, 0xE8E8E8)))
                .kagune(KaguneType.KOUKAKU, 0x6A3A9A, 1).rc(90).hp(4).npc(3, 90, 0.3f, 6).generic()
                .blurb("A nameless ghoul with an armour-type kagune.")
                .move(k("ghoul_koukaku_slam", "Koukaku Slam", Kind.STRIKE).dmg(9).radius(1.8f).windup(9).anim(Anims.OVERHEAD).cost(10))
                .move(k("ghoul_koukaku_guard", "Koukaku Guard", Kind.GUARD_COUNTER).dmg(8).anim(Anims.K_SHIELD).cost(15).rank(2))
                .move(k("ghoul_koukaku_spikes", "Koukaku Spikes", Kind.SPIKES).dmg(8).count(6).cost(30).rank(3))
                .ultimate(kakuja("ghoul_koukaku_kakuja", "Kakuja", 1.15f));

        RINKAKU_GHOUL = add(new CharacterDef("rinkaku_ghoul", "Rinkaku Ghoul", Group.OTHERS, Faction.WILD, Race.GHOUL,
                Look.of().hair(Hair.WILD, 0x1A1A1A).top(Top.HOODIE, 0x3A1A1A, 0x5A2A2A).pants(0x201818).mask(Mask.PLAIN, 0x101010, 0xC0102A)))
                .kagune(KaguneType.RINKAKU, 0xC0102A, 2).rc(90).hp(2).npc(4, 90, 0.34f, 2).generic()
                .blurb("A nameless ghoul with tentacle-type kagune.")
                .move(rinkakuPierce("ghoul_rinkaku_pierce", "Rinkaku Pierce").count(2))
                .move(kaguneSweep("ghoul_rinkaku_lash", "Tentacle Lash").rank(2))
                .move(k("ghoul_rinkaku_grab", "Tentacle Grab", Kind.GRAB).dmg(11).cost(35).rank(3))
                .ultimate(kakuja("ghoul_rinkaku_kakuja", "Kakuja", 1.15f));

        BIKAKU_GHOUL = add(new CharacterDef("bikaku_ghoul", "Bikaku Ghoul", Group.OTHERS, Faction.WILD, Race.GHOUL,
                Look.of().hair(Hair.SPIKY, 0x4A3A2A).top(Top.JACKET, 0x2A3A2A, 0x4A5A4A).pants(0x1A241A)
                        .mask(Mask.PLAIN, 0x2A3A2A, 0xE8E8E8)))
                .kagune(KaguneType.BIKAKU, 0x9A3A2A, 1).rc(90).hp(2).npc(3, 80, 0.33f, 4).generic()
                .blurb("A nameless ghoul with a tail-type kagune.")
                .move(tailStrike("ghoul_tail_strike", "Tail Strike"))
                .move(kaguneSweep("ghoul_tail_sweep", "Tail Sweep").count(1).rank(2))
                .move(k("ghoul_tail_impale", "Tail Impale", Kind.PIERCE).dmg(11).range(7).count(1).windup(9).cost(25).rank(3))
                .ultimate(kakuja("ghoul_bikaku_kakuja", "Kakuja", 1.15f));

        AOGIRI_GRUNT = add(new CharacterDef("aogiri_grunt", "Aogiri Soldier", Group.OTHERS, Faction.AOGIRI, Race.GHOUL,
                Look.of().hair(Hair.SHORT, 0x1A1A1A).top(Top.CLOAK, 0x101014, 0x6A0F1A).pants(0x101014)
                        .mask(Mask.PLAIN, 0xE8E8E8, 0x6A0F1A).masked())).hidden()
                .kagune(KaguneType.BIKAKU, 0x8A1020, 1).rc(80).npc(3, 70, 0.34f, 4).generic()
                .move(tailStrike("grunt_tail", "Tail Strike"))
                .move(ukakuVolley("grunt_shards", "Shard Volley").count(6));

        INVESTIGATOR = add(new CharacterDef("investigator", "CCG Investigator", Group.OTHERS, Faction.CCG, Race.HUMAN,
                Look.of().hair(Hair.SHORT, 0x2A2018).top(Top.TRENCH_COAT, 0xD8D0C0, 0x1A1A1A).pants(0x1A1A22).with(Look.TIE)))
                .rc(90).hp(2).npc(3, 70, 0.32f, 6).quinque("quinque_blade", "").generic()
                .blurb("A rank-and-file investigator with a standard-issue quinque.")
                .move(q("quinque_slash", "Quinque Slash", Kind.COMBO).dmg(4).range(3).count(3).anim(Anims.SLASH).cost(5))
                .move(q("quinque_thrust", "Quinque Thrust", Kind.PIERCE).dmg(9).range(5).count(1).anim(Anims.THRUST).cost(15).rank(2))
                .move(b("investigator_guard", "Investigator Guard", Kind.GUARD_COUNTER).dmg(8).cost(10).rank(3))
                .move(q("quinque_spin", "Quinque Spin", Kind.SWEEP).dmg(8).range(4).count(2).anim(Anims.SPIN).cost(25).rank(4))
                .ultimate(b("investigator_resolve", "Investigator's Resolve", Kind.BUFF).ultimate(false).duration(600).cost(100).cd(1800).rank(5));

        HUMAN = add(new CharacterDef("human", "Ordinary Human", Group.OTHERS, Faction.HUMAN, Race.HUMAN,
                Look.of().hair(Hair.SHORT, 0x3A2A1A).top(Top.SHIRT, 0x4A6A8A, 0xE8E8E8).pants(0x2A3A5A)))
                .rc(60).npc(1, 20, 0.3f, 0).generic()
                .blurb("No kagune, no quinque. Just fists, running and a lot of courage.")
                .move(b("human_kick", "Kick", Kind.STRIKE).dmg(5).kb(1.4f).anim(Anims.KICK).cd(20))
                .move(b("human_tackle", "Tackle", Kind.DASH).dmg(5).cost(5).rank(2));

        CIVILIAN = add(new CharacterDef("civilian", "Civilian", Group.OTHERS, Faction.HUMAN, Race.HUMAN,
                Look.of().hair(Hair.SHORT, 0x4A3A2A).top(Top.SHIRT, 0x6A7A8A, 0xE8E8E8).pants(0x3A3A4A))).hidden().generic()
                .rc(20).npc(1, 20, 0.3f, 0);
    }

    private Characters() {
    }

    private static CharacterDef add(CharacterDef d) {
        if (ALL.put(d.id, d) != null) throw new IllegalStateException("Duplicate character " + d.id);
        return d;
    }

    public static CharacterDef get(String id) {
        return id == null ? null : ALL.get(id);
    }

    private static Map<String, com.tokyoghoul.move.MoveDef> moveIndex;

    /** Any move by id (character moves, universal attacks and AI melee). */
    public static com.tokyoghoul.move.MoveDef move(String id) {
        if (moveIndex == null) {
            Map<String, com.tokyoghoul.move.MoveDef> m = new LinkedHashMap<>();
            for (com.tokyoghoul.move.MoveDef d : Moves.AI_MELEE) m.put(d.id, d);
            for (CharacterDef c : ALL.values()) {
                for (com.tokyoghoul.move.MoveDef d : c.moves()) m.putIfAbsent(d.id, d);
                if (c.ultimate() != null) m.putIfAbsent(c.ultimate().id, c.ultimate());
            }
            moveIndex = m;
        }
        return moveIndex.get(id);
    }

    public static List<CharacterDef> all() {
        return Collections.unmodifiableList(new ArrayList<>(ALL.values()));
    }

    public static List<CharacterDef> selectable() {
        List<CharacterDef> out = new ArrayList<>();
        for (CharacterDef d : ALL.values()) if (d.selectable) out.add(d);
        return out;
    }
}
