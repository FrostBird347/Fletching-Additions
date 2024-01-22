package frostbird347.fletchingadditions.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import frostbird347.fletchingadditions.screenHandler.FletchingTableScreenHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FletchingTableScreen extends HandledScreen<FletchingTableScreenHandler> {
	private static final Identifier GUI_TEXTURE = new Identifier("fletching-additions", "textures/gui/container/fletching.png");
	//Coordinates of each slot to cover when an item is present
	private static final int[] slotHidePos = new int[] {44, 17, 44, 35, 44, 53, 80, 35, 134, 35};

	public FletchingTableScreen(FletchingTableScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Override
	protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, GUI_TEXTURE);
		int x = (width - backgroundWidth) / 2;
		int y = (height - backgroundHeight) / 2;
		drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);
		
		//Display invalid output
		if (this.getScreenHandler() != null && this.getScreenHandler().displayOutputWarning()) {
			drawTexture(matrices, x + 99, y + 33, 176, 0, 28, 21);
		}

		//Hide slot texture when it isn't empty
		boolean[] slotsToHide = this.getScreenHandler().slotGraphicsToHide();
		for (int i = 0; i < slotsToHide.length; i++) {
			if (slotsToHide[i]) {
				drawTexture(matrices, x + slotHidePos[i * 2], y + slotHidePos[(i * 2) + 1], 176, 21, 16, 16);
			}
		}
	}
 
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		renderBackground(matrices);
		super.render(matrices, mouseX, mouseY, delta);
		drawMouseoverTooltip(matrices, mouseX, mouseY);
	}

	@Override
	protected void init() {
		super.init();
		// Center the title
		titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
	}
}