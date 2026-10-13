package com.lastbastion.siege;

public enum SiegeState {
    /** No core placed. */
    INACTIVE,
    /** Daytime: build, upgrade, shop. The wave begins at nightfall. */
    PREPARING,
    /** A wave is underway. */
    WAVE;

    public static SiegeState byId(int i) {
        return i >= 0 && i < values().length ? values()[i] : INACTIVE;
    }
}
