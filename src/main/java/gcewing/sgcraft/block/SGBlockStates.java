package gcewing.sgcraft.block;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

public class SGBlockStates {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty MERGED = BooleanProperty.create("merged");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
}
