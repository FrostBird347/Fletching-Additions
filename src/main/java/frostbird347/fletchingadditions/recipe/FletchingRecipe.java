package frostbird347.fletchingadditions.recipe;

import frostbird347.fletchingadditions.screenHandler.FletchingTableScreenHandler;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class FletchingRecipe implements Recipe<Inventory> {
	private final Ingredient inputTip;
	private final Ingredient inputStick;
	private final Ingredient inputFins;
	private final Ingredient inputEffect;
	private final ItemStack output;
	private final Identifier id;

	public FletchingRecipe(Identifier id, ItemStack output, Ingredient inputTip, Ingredient inputStick, Ingredient inputFins, Ingredient inputEffect) {
		this.id = id;
		this.output = output;
		this.inputTip = inputTip;
		this.inputStick = inputStick;
		this.inputFins = inputFins;
		this.inputEffect = inputEffect;
	}

	public Identifier getId() {
		return id;
	}

	public Ingredient getInputTip() {
		return inputTip;
	}

	public Ingredient getInputStick() {
		return inputStick;
	}

	public Ingredient getInputFins() {
		return inputFins;
	}

	public Ingredient getInputEffect() {
		return inputEffect;
	}

	public ItemStack getOutput() {
		return output;
	}

	@Override
	public ItemStack craft(Inventory inv) {
		return this.getOutput().copy();
	}
 
	@Override
	public boolean fits(int width, int height) {
		return true;
	}
	
	@Override 
	public boolean matches(Inventory inv, World world) {
		if(inv.size() < 4) return false;
		return inputTip.test(inv.getStack(FletchingTableScreenHandler.ARROW_TIP_SLOT_INDEX))
		&& inputStick.test(inv.getStack(FletchingTableScreenHandler.ARROW_STICK_SLOT_INDEX))
		&& inputFins.test(inv.getStack(FletchingTableScreenHandler.ARROW_FINS_SLOT_INDEX))
		&& inputEffect.test(inv.getStack(FletchingTableScreenHandler.EFFECT_SLOT_INDEX));
	}

	public static class Type implements RecipeType<FletchingRecipe> {
		private Type() {}
		public static final Type INSTANCE = new Type();
		public static final String ID = "fletching_recipe";
	}

	@Override
	public RecipeType<?> getType() {
		return Type.INSTANCE;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return FletchingRecipeSerializer.INSTANCE;
	}

}