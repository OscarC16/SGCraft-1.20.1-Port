package gcewing.sgcraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Global registry for all Stargates in the world.
 * Persisted using Minecraft's SavedData system.
 */
public class SGNetwork extends SavedData {

    private static final String DATA_NAME = "sgcraft_network";
    private final Map<String, StargateLocation> stargates = new HashMap<>();

    public static SGNetwork get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new RuntimeException("SGNetwork can only be accessed on the server side!");
        }
        return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(
                SGNetwork::load,
                SGNetwork::new,
                DATA_NAME
        );
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

    // --- Persistence ---

    public SGNetwork() {}

    public static SGNetwork load(CompoundTag tag) {
        SGNetwork network = new SGNetwork();
        ListTag list = tag.getList("stargates", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String address = entry.getString("address");
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(entry.getString("dim")));
            network.stargates.put(address, new StargateLocation(dim, pos));
        }
        return network;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, StargateLocation> entry : stargates.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putString("address", entry.getKey());
            e.putLong("pos", entry.getValue().pos.asLong());
            e.putString("dim", entry.getValue().dimension.location().toString());
            list.add(e);
        }
        tag.put("stargates", list);
        return tag;
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
