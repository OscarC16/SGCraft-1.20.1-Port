package net.minecraft.client.model.geom.builders;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record UVPair(float u, float v) {
    @Override
    public String toString() {
        return "(" + this.u + "," + this.v + ")";
    }

    public static long pack(float p_470665_, float p_470783_) {
        long i = Float.floatToIntBits(p_470665_) & 4294967295L;
        long j = Float.floatToIntBits(p_470783_) & 4294967295L;
        return i << 32 | j;
    }

    public static float unpackU(long p_470792_) {
        int i = (int)(p_470792_ >> 32);
        return Float.intBitsToFloat(i);
    }

    public static float unpackV(long p_470804_) {
        return Float.intBitsToFloat((int)p_470804_);
    }
}
