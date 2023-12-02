package frostbird347.fletchingadditions.screenHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import frostbird347.fletchingadditions.recipe.FletchingRecipe;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FletchingTableScreenHandler extends ScreenHandler {
	public static final int ARROW_TIP_SLOT_INDEX = 0;
	private ArrayList<Ingredient> arrowTipIngredients = new ArrayList<Ingredient>();
	public static final int ARROW_STICK_SLOT_INDEX = 1;
	private ArrayList<Ingredient> arrowStickIngredients = new ArrayList<Ingredient>();
	public static final int ARROW_FINS_SLOT_INDEX = 2;
	private ArrayList<Ingredient> arrowFinsIngredients = new ArrayList<Ingredient>();
	public static final int EFFECT_SLOT_INDEX = 3;
	private ArrayList<Ingredient> effectIngredients = new ArrayList<Ingredient>();
	public static final int RESULT_SLOT_INDEX = 0;
	private long lastCraftTime;
	private final World world;

	private final ScreenHandlerContext context;
	private final Inventory inputInventory = new SimpleInventory(4){

		@Override
		public void markDirty() {
			FletchingTableScreenHandler.this.onContentChanged(this);
			super.markDirty();
		}
	};
	private final CraftingResultInventory outputInventory = new CraftingResultInventory(){};

	public static boolean isValidIngredient(ArrayList<Ingredient> ingredients, ItemStack stack) {
		for (int i = 0; i < ingredients.size(); i++) {
			if (ingredients.get(i).test(stack)) {
				return true;
			}
		}
		return false;
	}

	//Client
	public FletchingTableScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	//Server
	public FletchingTableScreenHandler(int syncId, PlayerInventory playerInventory, final ScreenHandlerContext context) {
		super(ScreenHandlerManager.FLETCHING_TABLE, syncId);
		this.context = context;
		this.world = playerInventory.player.getWorld();

		//Prevent players from putting invalid ingredients in the wrong slots
		List<FletchingRecipe> allRecipes = this.world.getRecipeManager().listAllOfType(FletchingRecipe.Type.INSTANCE);
		for (int i = 0; i < allRecipes.size(); i++) {
			arrowTipIngredients.add(allRecipes.get(i).getInputTip());
			arrowStickIngredients.add(allRecipes.get(i).getInputStick());
			arrowFinsIngredients.add(allRecipes.get(i).getInputFins());

			Ingredient inputEffect = allRecipes.get(i).getInputEffect();
			if (!inputEffect.isEmpty()) {
				effectIngredients.add(allRecipes.get(i).getInputEffect());
			}
		}

		this.addSlot(new Slot(inputInventory, ARROW_TIP_SLOT_INDEX, 44, 17) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return isValidIngredient(arrowTipIngredients, stack);
			}
		});
		this.addSlot(new Slot(inputInventory, ARROW_STICK_SLOT_INDEX, 44, 35) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return isValidIngredient(arrowStickIngredients, stack);
			}
		});
		this.addSlot(new Slot(inputInventory, ARROW_FINS_SLOT_INDEX, 44, 53) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return isValidIngredient(arrowFinsIngredients, stack);
			}
		});
		this.addSlot(new Slot(inputInventory, EFFECT_SLOT_INDEX, 80, 35) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return isValidIngredient(effectIngredients, stack);
			}
		});
		
		this.addSlot(new Slot(this.outputInventory, RESULT_SLOT_INDEX, 134, 35) {

			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public void onTakeItem(PlayerEntity player, ItemStack stack) {
				((Slot)FletchingTableScreenHandler.this.slots.get(ARROW_TIP_SLOT_INDEX)).takeStack(1);
				((Slot)FletchingTableScreenHandler.this.slots.get(ARROW_STICK_SLOT_INDEX)).takeStack(1);
				((Slot)FletchingTableScreenHandler.this.slots.get(ARROW_FINS_SLOT_INDEX)).takeStack(1);
				Slot effectSlot = (Slot)FletchingTableScreenHandler.this.slots.get(EFFECT_SLOT_INDEX);
				if (effectSlot.hasStack()) {
					effectSlot.takeStack(1);
				}

				stack.onCraft(world, player, stack.getCount());
				context.run((world, pos) -> {
					long l = world.getTime();
					if (FletchingTableScreenHandler.this.lastCraftTime != l) {
						world.playSound(null, (BlockPos)pos, SoundEvents.BLOCK_PACKED_MUD_HIT, SoundCategory.BLOCKS, 1.0f, 1.0f);
						FletchingTableScreenHandler.this.lastCraftTime = l;
					}
				});
				super.onTakeItem(player, stack);
			}

		});

		addPlayerInventory(playerInventory);
		addPlayerHotbar(playerInventory);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return FletchingTableScreenHandler.canUse(this.context, player, Blocks.FLETCHING_TABLE);
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		ItemStack tipStack = this.inputInventory.getStack(ARROW_TIP_SLOT_INDEX);
		ItemStack stickStack = this.inputInventory.getStack(ARROW_STICK_SLOT_INDEX);
		ItemStack finsStack = this.inputInventory.getStack(ARROW_FINS_SLOT_INDEX);
		ItemStack effectStack = this.inputInventory.getStack(EFFECT_SLOT_INDEX);
		ItemStack outputStack = this.outputInventory.getStack(RESULT_SLOT_INDEX);

		//If ingredients were removed
		if (!outputStack.isEmpty() && (tipStack.isEmpty() || stickStack.isEmpty() || finsStack.isEmpty())) {
			this.outputInventory.removeStack(RESULT_SLOT_INDEX);
			this.sendContentUpdates();
		} else if (!tipStack.isEmpty() && !stickStack.isEmpty() && !finsStack.isEmpty()) {
			this.updateOutput(tipStack, stickStack, finsStack, effectStack, outputStack);
		}
	}

	private void updateOutput(ItemStack tipStack, ItemStack stickStack, ItemStack finsStack, ItemStack effectStack, ItemStack oldOutput) {
		this.context.run((world, pos) -> {
			Optional<FletchingRecipe> currentRecipe = this.world.getRecipeManager().getFirstMatch(FletchingRecipe.Type.INSTANCE, this.inputInventory, this.world);
			if (currentRecipe.isPresent()) {
				this.outputInventory.setStack(RESULT_SLOT_INDEX, currentRecipe.get().craft(this.outputInventory));
			} else {
				this.outputInventory.removeStack(RESULT_SLOT_INDEX);
			}
			this.sendContentUpdates();
		});
	}

	//Shift clicking
	@Override
	public ItemStack transferSlot(PlayerEntity player, int invSlot) {
		ItemStack newStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(invSlot);
		if (slot != null && slot.hasStack()) {
			ItemStack originalStack = slot.getStack();

			//Relevant if shift clicking output slot
			int craftCount = 1;
			ItemStack arrowTipStack = this.slots.get(ARROW_TIP_SLOT_INDEX).getStack();
			ItemStack arrowStickStack = this.slots.get(ARROW_STICK_SLOT_INDEX).getStack();
			ItemStack arrowFinsStack = this.slots.get(ARROW_FINS_SLOT_INDEX).getStack();
			ItemStack effectStack = this.slots.get(EFFECT_SLOT_INDEX).getStack();
			if (invSlot == 4) {
				craftCount = Math.min(arrowTipStack.getCount(), arrowStickStack.getCount());
				craftCount = Math.min(craftCount, arrowFinsStack.getCount());
				if (!effectStack.isEmpty()) {
					craftCount = Math.min(craftCount, effectStack.getCount());
				}
			}

			newStack = originalStack.copy();
			ItemStack initialStack = originalStack.copy();
			for (int i = 0; i < craftCount; i++) {

				//Use the original stack for the first loop, so behaviour is identical to vanilla when not crafting
				ItemStack currentStack = originalStack;
				if (i != 0) {
					currentStack = initialStack.copy();
				}

				if (invSlot < this.inputInventory.size() + this.outputInventory.size()) {
					if (!this.insertItem(currentStack, this.inputInventory.size() + this.outputInventory.size(), this.slots.size(), true)) {
						return ItemStack.EMPTY;
					}
				} else if (!this.insertItem(currentStack, 0, this.inputInventory.size() + this.outputInventory.size(), false)) {
					return ItemStack.EMPTY;
				}

				if (invSlot == 4) {
					if (!effectStack.isEmpty()) {
						effectStack.decrement(1);
					}
					arrowTipStack.decrement(1);
					arrowStickStack.decrement(1);
					arrowFinsStack.decrement(1);

					//newStack starts off with the correct count, so we don't increment it for the first loop
					if (i != 0) {
						newStack.setCount(newStack.getCount() + currentStack.getCount());

					//play a sound on the first loop, if the user if crafting
					} else {
						context.run((world, pos) -> {
							long l = world.getTime();
							if (FletchingTableScreenHandler.this.lastCraftTime != l) {
								world.playSound(null, (BlockPos)pos, SoundEvents.BLOCK_PACKED_MUD_HIT, SoundCategory.BLOCKS, 1.0f, 1.0f);
								FletchingTableScreenHandler.this.lastCraftTime = l;
							}
						});
					}
				}

				if (currentStack.isEmpty()) {
					slot.setStack(ItemStack.EMPTY);
				} else {
					slot.markDirty();
				}
			}

			//Refresh crafting output, inventory argument is unfortunately required but unused so it can be anything
			if (invSlot == 4) {
				this.onContentChanged(this.inputInventory);
			}
		}
 
		return newStack;
	}

	private void addPlayerInventory(PlayerInventory playerInventory) {
		for (int r = 0; r < 3; r++) {
			for (int s = 0; s < 9; s++) {
				this.addSlot(new Slot(playerInventory, s + r * 9 + 9, s * 18 + 8, r * 18 + 84));
			}
		}
	}

	private void addPlayerHotbar(PlayerInventory playerInventory) {
		for (int i = 0; i < 9; i++) {
			this.addSlot(new Slot(playerInventory, i, i * 18 + 8, 142));
		}
	}

	@Override
	public void close(PlayerEntity player) {
		super.close(player);

		for (int i = 0; i < inputInventory.size(); i++) {
			ItemStack currentSlot = inputInventory.getStack(i);
			if (!currentSlot.isEmpty()) {
				player.getInventory().offerOrDrop(currentSlot);
			}
		}
	}
}