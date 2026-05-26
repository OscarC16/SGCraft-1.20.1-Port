package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

/**
 * @param hasAmbientOcclusion {@code false} to force-disable AO for this quad
 *                            or {@code true} to use the behavior dictated by
 *                            {@link net.neoforged.neoforge.client.extensions.BlockModelPartExtension#ambientOcclusion}.
 */
@OnlyIn(Dist.CLIENT)
public record BakedQuad(
    Vector3fc position0,
    Vector3fc position1,
    Vector3fc position2,
    Vector3fc position3,
    long packedUV0,
    long packedUV1,
    long packedUV2,
    long packedUV3,
    int tintIndex,
    Direction direction,
    TextureAtlasSprite sprite,
    boolean shade,
    int lightEmission,
    net.neoforged.neoforge.client.model.quad.BakedNormals bakedNormals,
    net.neoforged.neoforge.client.model.quad.BakedColors bakedColors,
    boolean hasAmbientOcclusion
) {
    public BakedQuad(Vector3fc position0, Vector3fc position1, Vector3fc position2, Vector3fc position3, long packedUV0, long packedUV1, long packedUV2, long packedUV3, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade, int lightEmission) {
        this(position0, position1, position2, position3, packedUV0, packedUV1, packedUV2, packedUV3, tintIndex, direction, sprite, shade, lightEmission, net.neoforged.neoforge.client.model.quad.BakedNormals.UNSPECIFIED, net.neoforged.neoforge.client.model.quad.BakedColors.DEFAULT, true);
    }

    public static final int VERTEX_COUNT = 4;

    public boolean isTinted() {
        return this.tintIndex != -1;
    }

    public Vector3fc position(int p_470820_) {
        return switch (p_470820_) {
            case 0 -> this.position0;
            case 1 -> this.position1;
            case 2 -> this.position2;
            case 3 -> this.position3;
            default -> throw new IndexOutOfBoundsException(p_470820_);
        };
    }

    public long packedUV(int p_470743_) {
        return switch (p_470743_) {
            case 0 -> this.packedUV0;
            case 1 -> this.packedUV1;
            case 2 -> this.packedUV2;
            case 3 -> this.packedUV3;
            default -> throw new IndexOutOfBoundsException(p_470743_);
        };
    }
}
