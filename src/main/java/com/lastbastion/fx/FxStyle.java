package com.lastbastion.fx;

/**
 * Sprite + behaviour presets for the {@code lastbastion:fx} particle. Sprites are white so every style
 * can be tinted to any colour. Order matches {@code assets/lastbastion/particles/fx.json}.
 */
public enum FxStyle {
    /** Soft additive glow orb that shrinks out. */
    GLOW(true, true, 0f),
    /** Bright streak stretched along its velocity, falls and bounces. */
    SPARK(true, true, 0.9f),
    /** Rising flickering ember. */
    EMBER(true, true, -0.06f),
    /** Billowing smoke puff that grows and fades. */
    SMOKE(false, false, -0.02f),
    /** Spinning shard of ice / glass / bone. */
    SHARD(false, false, 1.0f),
    /** Expanding hollow ring. */
    RING(true, true, 0f),
    /** Wavy rising soul wisp. */
    SOUL(true, true, -0.05f),
    /** Twinkling four-point star. */
    STAR(true, true, 0f),
    /** Liquid droplet. */
    DROP(false, false, 1.0f),
    /** Fluttering leaf / petal. */
    LEAF(false, false, 0.08f),
    /** Rotating glowing rune glyph. */
    RUNE(true, true, -0.02f),
    /** Small crackling lightning bolt. */
    BOLT(true, true, 0f),
    /** Drifting feather. */
    FEATHER(false, false, 0.05f),
    /** Rising bubble. */
    BUBBLE(false, true, -0.04f),
    /** Snowflake / frost crystal. */
    FLAKE(true, true, 0.04f),
    /** Wispy skull spirit. */
    SKULL(true, true, -0.04f),
    /** Dark heavy debris chunk. */
    DEBRIS(false, false, 1.2f),
    /** Hot flame lick. */
    FLAME(true, true, -0.08f);

    public final boolean additive;
    public final boolean fullBright;
    public final float gravity;

    FxStyle(boolean additive, boolean fullBright, float gravity) {
        this.additive = additive;
        this.fullBright = fullBright;
        this.gravity = gravity;
    }

    private static final FxStyle[] VALUES = values();

    public static FxStyle byId(int id) {
        return VALUES[Math.floorMod(id, VALUES.length)];
    }
}
