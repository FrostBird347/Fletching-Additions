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