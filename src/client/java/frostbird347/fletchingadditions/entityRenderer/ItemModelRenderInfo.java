package frostbird347.fletchingadditions.entityRenderer;

import frostbird347.fletchingadditions.entity.CustomArrowEntityRenderPart.TextureSide;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

//Used by CustomArrowEntityRenderer to position and render all the item models after everything else has been rendered (otherwise the game will crash)
public class ItemModelRenderInfo  {
	public ItemStack item;
	public Vec3d translate;
	public float scale;
	public Vec3f rotation;
	public TextureSide renderSide;
	public double partOffset;

	public ItemModelRenderInfo(ItemStack item, Vec3d translate, float scale, Vec3f rotation, TextureSide renderSide, float partOffset) {
		this.item = item;
		this.translate = translate;
		this.scale = scale;
		this.rotation = rotation;
		this.renderSide = renderSide;
		this.partOffset = partOffset;
	}
}