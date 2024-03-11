package frostbird347.fletchingadditions.entity.entityRendering;

import java.util.ArrayList;

import frostbird347.fletchingadditions.entity.CustomArrowEntity;
import net.minecraft.nbt.NbtCompound;

public class CustomArrowEntityRenderInfo {

	public ArrayList<CustomArrowEntityRenderPart> renderList;

	public CustomArrowEntityRenderInfo(CustomArrowEntity arrow) {
		renderList = new ArrayList<CustomArrowEntityRenderPart>();

		NbtCompound arrowData = new NbtCompound();
		arrowData.putInt("id", 0);
		arrowData.putString("side", "b");
		
		renderList.add(new CustomArrowEntityRenderPart(arrow, "t", "texture", arrowData));
		renderList.add(new CustomArrowEntityRenderPart(arrow, "s", "texture", arrowData));
		renderList.add(new CustomArrowEntityRenderPart(arrow, "f", "texture", arrowData));
	}
}