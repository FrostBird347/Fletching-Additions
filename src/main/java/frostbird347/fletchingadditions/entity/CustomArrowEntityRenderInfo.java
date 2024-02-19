package frostbird347.fletchingadditions.entity;

import java.util.ArrayList;

public class CustomArrowEntityRenderInfo {

	public ArrayList<CustomArrowEntityRenderPart> renderList;

	public CustomArrowEntityRenderInfo(CustomArrowEntity arrow) {
		renderList = new ArrayList<CustomArrowEntityRenderPart>();
		renderList.add(new CustomArrowEntityRenderPart(arrow, 't', "texture", "0"));
		renderList.add(new CustomArrowEntityRenderPart(arrow, 's', "texture", "0"));
		renderList.add(new CustomArrowEntityRenderPart(arrow, 'f', "texture", "0"));
	}
}