package gcewing.sgcraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Global registry for all Stargates in the world.
 * Persisted using Minecraft's SavedData system.
 */
public class SGNetwork extends SavedData {

    private static final String DATA_NAME = "sgcraft_network";
    private final Map<String, StargateLocation> stargates = new HashMap<>();

    public static final Codec<StargateLocation> STARGATE_LOCATION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(loc -> loc.dimension),
        BlockPos.CODEC.fieldOf("pos").forGetter(loc -> loc.pos)
    ).apply(instance, StargateLocation::new));

    public static final Codec<SGNetwork> CODEC = Codec.unboundedMap(Codec.STRING, STARGATE_LOCATION_CODEC).xmap(
        map -> {
            SGNetwork network = new SGNetwork();
            network.stargates.putAll(map);
            return network;
        },
        network -> network.stargates
    );

    public static final SavedDataType<SGNetwork> TYPE = new SavedDataType<>(
        DATA_NAME,
        SGNetwork::new,
        CODEC
    );

    public static SGNetwork get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new RuntimeException("SGNetwork can only be accessed on the server side!");
        }
        return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void register(String address, ResourceKey<Level> dimension, BlockPos pos) {
        stargates.put(address, new StargateLocation(dimension, pos));
        setDirty();
    }

    public void unregister(String address) {
        stargates.remove(address);
        setDirty();
    }

    public StargateLocation findStargate(String address, ResourceKey<Level> originDim) {
        // Direct match (9 symbols)
        if (stargates.containsKey(address)) {
            return stargates.get(address);
        }
        
        // 7-symbol shortcut (same dimension)
        if (address.length() == 7) {
            for (Map.Entry<String, StargateLocation> entry : stargates.entrySet()) {
                if (entry.getKey().startsWith(address) && entry.getValue().dimension.equals(originDim)) {
                    return entry.getValue();
                }
            }
        }
        
        return null;
    }

    public static class StargateLocation {
        public final ResourceKey<Level> dimension;
        public final BlockPos pos;

        public StargateLocation(ResourceKey<Level> dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }
    }
}
