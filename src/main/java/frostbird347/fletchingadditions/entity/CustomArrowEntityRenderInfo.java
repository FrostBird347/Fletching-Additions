package frostbird347.fletchingadditions.entity;

import java.util.ArrayList;

public class CustomArrowEntityRenderInfo {

	public ArrayList<CustomArrowEntityRenderPart> renderList;

	public CustomArrowEntityRenderInfo(CustomArrowEntity arrow) {
		renderList = new ArrayList<CustomArrowEntityRenderPart>();
		String dataTest = "160";
		double rand = Math.random();
		if (rand > 0.75) {
			dataTest = "161";
		} else if (rand > 0.5) {
			dataTest = "0";
		}  else if (rand > 0.25) {
			dataTest = "1";
		} 
		renderList.add(new CustomArrowEntityRenderPart(arrow, 't', "texture", dataTest, "b"));
		renderList.add(new CustomArrowEntityRenderPart(arrow, 's', "texture", dataTest, "b"));
		renderList.add(new CustomArrowEntityRenderPart(arrow, 'f', "texture", dataTest, "b"));
	}
}