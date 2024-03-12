package frostbird347.fletchingadditions.item;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import com.google.common.collect.Lists;
import frostbird347.fletchingadditions.entity.CustomArrowEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.FireworkStarItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CustomArrowItem extends ArrowItem {
	public CustomArrowItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter) {
		CustomArrowEntity customArrowEntity = new CustomArrowEntity(world, shooter, stack);
		return customArrowEntity;
	}

	public PersistentProjectileEntity createArrow(World world, ItemStack stack, Position position) {
		CustomArrowEntity customArrowEntity = new CustomArrowEntity(world, position.getX(), position.getY(), position.getZ(), stack);
		customArrowEntity.serverSourcePos = new Vec3d(position.getX(), position.getY(), position.getZ());
		return customArrowEntity;
	}

	public PersistentProjectileEntity createArrow(World world, ItemStack stack, Position position, boolean isDispenser) {
		CustomArrowEntity customArrowEntity = (CustomArrowEntity)createArrow(world, stack, position);
		if (isDispenser) {
			customArrowEntity.markAsDispenserArrow();
		}
		return customArrowEntity;
	}

	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		String paddingText = "  ";
		int paddingAmount = 0;

		//Extract basic stats nbt
		NbtCompound currentNbtChunk = stack.getNbt();
		if (currentNbtChunk != null) {
			boolean firstStat = true;

			//TODO: Replace with translatable when I make the item names translatable
			String[] stats = {"flySpeed", "gravityMult", "drawSpeed", "drawMinMult", "damageMult"};
			String[] statNames = {"Airspeed", "Weight", "Draw Speed", "drawMinMult", "Damage"};

			for (int i = 0; i < stats.length; i++) {
				if (currentNbtChunk.contains(stats[i], NbtCompound.FLOAT_TYPE)) {
					if (firstStat) {
						tooltip.add(Text.literal("Basic Stats").formatted(Formatting.BLUE));
						paddingAmount = 1;
						firstStat = false;
					}

					tooltip.add(Text.literal(paddingText.repeat(paddingAmount)).append(Text.literal(statNames[i])).append(": ").append(String.valueOf(currentNbtChunk.getFloat(stats[i]))).append("x").formatted(Formatting.GRAY));
				}
			}
		}
		
		//Extract firework rocket stats
		//Just some modified vanilla firework tooltip code
		currentNbtChunk = stack.getSubNbt("inheritFireworkNBT");
		if (currentNbtChunk != null) {
			currentNbtChunk = currentNbtChunk.getCompound("Fireworks");
			
			tooltip.add(Text.literal("Firework Rocket").formatted(Formatting.RED));
			paddingAmount = 1;

			if (currentNbtChunk.contains("Flight", NbtCompound.NUMBER_TYPE)) {
				tooltip.add(Text.literal(paddingText.repeat(paddingAmount)).append(Text.translatable("item.minecraft.firework_rocket.flight")).append(" ").append(String.valueOf(currentNbtChunk.getByte("Flight"))).formatted(Formatting.GRAY));
			}

			NbtList fireworkExplosions = currentNbtChunk.getList("Explosions", NbtCompound.COMPOUND_TYPE);
			if (!fireworkExplosions.isEmpty()) {

				for (int i = 0; i < fireworkExplosions.size(); i++) {
					NbtCompound currentExplosion = fireworkExplosions.getCompound(i);
					List<Text> explosionText = Lists.newArrayList();
					FireworkStarItem.appendFireworkTooltip(currentExplosion, explosionText);

					for (int line = 0; line < explosionText.size(); line++) {
						if (line == 0) {
							paddingAmount = 1;
						} else {
							paddingAmount = 2;
						}
						explosionText.set(line, Text.literal(paddingText.repeat(paddingAmount)).append(explosionText.get(line)).formatted(Formatting.GRAY));
					}

					tooltip.addAll(explosionText);
				}
			}
		}
		
		//Extract firework star nbt
		currentNbtChunk = stack.getSubNbt("inheritFireworkStarNBT");
		if (currentNbtChunk != null) {
			currentNbtChunk = currentNbtChunk.getCompound("Explosion");

			tooltip.add(Text.literal("Firework Star").formatted(Formatting.YELLOW));
			paddingAmount = 1;

			List<Text> explosionText = Lists.newArrayList();
			FireworkStarItem.appendFireworkTooltip(currentNbtChunk, explosionText);

			for (int line = 0; line < explosionText.size(); line++) {
				if (line == 0) {
					paddingAmount = 1;
				} else {
					paddingAmount = 2;
				}
				explosionText.set(line, Text.literal(paddingText.repeat(paddingAmount)).append(explosionText.get(line)).formatted(Formatting.GRAY));
			}

			tooltip.addAll(explosionText);
		}
	}
}