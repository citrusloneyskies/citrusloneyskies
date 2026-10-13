package com.lastbastion.fx;

import com.lastbastion.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Data for the tintable {@code lastbastion:fx} particle.
 *
 * @param style   sprite + behaviour preset
 * @param color   RGB tint
 * @param size    quad size in blocks
 * @param life    lifetime in ticks (0 = style default)
 */
public record FxParticleOptions(FxStyle style, int color, float size, int life) implements ParticleOptions {
    @SuppressWarnings("deprecation")
    public static final Deserializer<FxParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public FxParticleOptions fromCommand(ParticleType<FxParticleOptions> type, StringReader r) throws CommandSyntaxException {
            r.expect(' ');
            int style = r.readInt();
            r.expect(' ');
            int color = r.readInt();
            r.expect(' ');
            float size = r.readFloat();
            r.expect(' ');
            int life = r.readInt();
            return new FxParticleOptions(FxStyle.byId(style), color, size, life);
        }

        @Override
        public FxParticleOptions fromNetwork(ParticleType<FxParticleOptions> type, FriendlyByteBuf b) {
            return new FxParticleOptions(FxStyle.byId(b.readByte()), b.readInt(), b.readFloat(), b.readVarInt());
        }
    };

    public static final Codec<FxParticleOptions> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("style").forGetter(o -> o.style.ordinal()),
            Codec.INT.fieldOf("color").forGetter(FxParticleOptions::color),
            Codec.FLOAT.fieldOf("size").forGetter(FxParticleOptions::size),
            Codec.INT.fieldOf("life").forGetter(FxParticleOptions::life)
    ).apply(i, (s, c, sz, l) -> new FxParticleOptions(FxStyle.byId(s), c, sz, l)));

    public static FxParticleOptions of(FxStyle style, int color, float size) {
        return new FxParticleOptions(style, color, size, 0);
    }

    public static FxParticleOptions of(FxStyle style, int color, float size, int life) {
        return new FxParticleOptions(style, color, size, life);
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.FX.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf b) {
        b.writeByte(style.ordinal());
        b.writeInt(color);
        b.writeFloat(size);
        b.writeVarInt(life);
    }

    @Override
    public String writeToString() {
        return "lastbastion:fx " + style.ordinal() + " " + color + " " + size + " " + life;
    }
}
