package frostbird347.fletchingadditions.item;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class ModelPlaceholderItem extends Item {
	public ModelPlaceholderItem(Item.Settings settings) {
		super(settings);
	}

	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.literal("An unobtainable item used to store custom models for arrow parts").formatted(Formatting.GRAY));
	}
}