package frostbird347.fletchingadditions.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.FletchingTableBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FletchingTableBlock.class)
public class FletchingTableBlockMixin extends CraftingTableBlock {

	public FletchingTableBlockMixin(Settings settings) {
		super(settings);
	}

	//can't see any reason to avoid overwriting stuff in this class,
	//since any other mod attempting to add functionality to this block wouldn't be compatible anyway
	@Overwrite
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		} else {
			player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
			player.incrementStat(Stats.INTERACT_WITH_SMITHING_TABLE);
			return ActionResult.CONSUME;
		}
	}

	public NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
		return new SimpleNamedScreenHandlerFactory((syncId, inventory, player) -> {
			return new SmithingScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, pos));
		}, Text.translatable("container.fletching_craft"));
	}
}