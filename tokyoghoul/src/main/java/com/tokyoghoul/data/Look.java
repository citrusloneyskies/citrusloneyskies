package com.tokyoghoul.data;

/**
 * Appearance of a character. The client turns it into a procedural 3D model (hair, outfit, mask,
 * kakugan, build) so every character, playable or NPC, gets a distinct anime look without hand-made
 * model files. Kept free of client classes so the roster can live in common code.
 */
public final class Look {
    public enum Hair {SHORT, MESSY, SPIKY, LONG, BOB, SIDE_SWEPT, SLICKED, BUZZ, BALD, PONYTAIL, TWINTAILS, WILD}

    public enum Top {HOODIE, TRENCH_COAT, SUIT, SHIRT, JACKET, BODYSUIT, BANDAGES, ROBE, CLOAK, DRESS, VEST, QUINX}

    public enum Build {SLIM, AVERAGE, ATHLETIC, BULKY, HUGE}

    public enum Mask {NONE, KANEKI, RABBIT, GOURMET, NISHIKI, CROW, OWL, BLACK_RABBIT, JASON, TATARA, NORO, CLOWN, BANDAGE, KURONA, PLAIN, HANNYA}

    /** Accessory bit flags. */
    public static final int GLASSES = 1, EYEPATCH = 2, HAIRCLIPS = 4, STITCHES = 8, SCARF = 16, HOOD = 32, BEARD = 64, TIE = 128,
            GLOVES = 256, BELT = 512, SUSPENDERS = 1024, ROOTS = 2048;

    public int skin = 0xF1D2B6, hair = 0x1A1A1A, hairAccent = -1, eyes = 0x3A2A20;
    public Hair hairStyle = Hair.SHORT;
    public Top top = Top.SHIRT;
    public int topColor = 0x2A2A30, topAccent = 0xE8E8E8, pants = 0x22222A, shoes = 0x151515;
    public Build build = Build.AVERAGE;
    public float height = 1f;
    public Mask mask = Mask.NONE;
    public int maskColor = 0x111111, maskAccent = 0xE8E8E8;
    public int accessories;
    /** Mask is worn all the time (Aogiri, Jason) rather than only when the kagune is out. */
    public boolean alwaysMasked;
    public boolean female;

    public static Look of() {
        return new Look();
    }

    public Look skin(int c) {
        skin = c;
        return this;
    }

    public Look hair(Hair style, int color) {
        hairStyle = style;
        hair = color;
        return this;
    }

    public Look hairAccent(int c) {
        hairAccent = c;
        return this;
    }

    public Look eyes(int c) {
        eyes = c;
        return this;
    }

    public Look top(Top t, int color, int accent) {
        top = t;
        topColor = color;
        topAccent = accent;
        return this;
    }

    public Look pants(int c) {
        pants = c;
        return this;
    }

    public Look shoes(int c) {
        shoes = c;
        return this;
    }

    public Look build(Build b) {
        build = b;
        return this;
    }

    public Look height(float h) {
        height = h;
        return this;
    }

    public Look mask(Mask m, int color, int accent) {
        mask = m;
        maskColor = color;
        maskAccent = accent;
        return this;
    }

    public Look with(int accessoryFlags) {
        accessories |= accessoryFlags;
        return this;
    }

    public Look masked() {
        alwaysMasked = true;
        return this;
    }

    public Look female() {
        female = true;
        return this;
    }

    public boolean has(int flag) {
        return (accessories & flag) != 0;
    }
}
