package frostbird347.fletchingadditions.entity;

import java.util.Map;
import frostbird347.fletchingadditions.MainMod;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.text.Text;

public class CustomArrowEntityRenderPart {
	public enum Type {
		TIP,
		STICK,
		FIN,
		EFFECT
	}
	public enum RenderMode {
		TEXTURE,
		MODEL,
		NONE
	}
	private static final Map<Character, Type> TYPE_MAP = Map.of(Character.valueOf('t'), Type.TIP, Character.valueOf('s'), Type.STICK, Character.valueOf('f'), Type.FIN, Character.valueOf('e'), Type.EFFECT);
	private static final Map<String, RenderMode> MODE_MAP = Map.of("texture", RenderMode.TEXTURE, "model", RenderMode.MODEL, "none", RenderMode.NONE);

	public int textureId;
	public String modelId;
	public Type type;
	public RenderMode mode;
	private byte size;
	private int lastId;

	/*
	Index:	0,1	2,1	2,3	0,3
	Path:
		0    3
		|    |
		1----2	
	first group is always used, second is used for the fin/stick, third is used by the stick
	*/
	private float[][] cachedTexturePoints = {{-1f, -1f, -1f, -1f}, {-1f, -1f, -1f, -1f}, {-1f, -1f, -1f, -1f}};
	private final byte[] POINT_LOOKUP_X = {0, 2, 2, 0};
	private final byte[] POINT_LOOKUP_Y = {1, 1, 3, 3};
	//TODO: Add model support
	private ModelIdentifier cachedModel = null;
	private final boolean IS_SERVER_SIDE;

	public CustomArrowEntityRenderPart(CustomArrowEntity arrow, char _type, String _mode, String data) {
		this(arrow);
		type = TYPE_MAP.getOrDefault(_type, Type.EFFECT);
		mode = MODE_MAP.getOrDefault(_mode, RenderMode.NONE);
		switch (mode) {
			case TEXTURE:
				textureId = Integer.parseInt(data, 10);
				break;
			case MODEL:
				modelId = data;
				break;
			default:
				break;
		}
	}

	public CustomArrowEntityRenderPart(CustomArrowEntity arrow) {
		size = -1;
		textureId = 0;
		lastId = -1;
		modelId = "";
		cachedModel = null;
		IS_SERVER_SIDE = !arrow.world.isClient;

		//Only load the missing model after
		if (!IS_SERVER_SIDE) {
			cachedModel = ModelLoader.MISSING_ID;
		}
	}

	public float getCoord(int index, boolean getY, int rectIndex) {
		if (IS_SERVER_SIDE) {
			MainMod.LOGGER.error("getCoord was called on the server!");
			MainMod.LOGGER.error("This should never happen!");
			return -1;
		}

		if (lastId != textureId) {
			lastId = textureId;
			size = -1;

			int currentPixelX = (textureId * 24) % 480;
			int currentPixelY = (int)(textureId / 20f) * 5;

			if (textureId >= 160) {
				currentPixelX = (((textureId - 160) * 40)) % 480;
				currentPixelY = 40 + ((int)((textureId - 160) / 12f) * 9);

				switch (type) {
					case TIP:
						//Top left
						cachedTexturePoints[0][0] = (currentPixelX / 480f);
						cachedTexturePoints[0][1] = (currentPixelY / 112f);
						//Shift to bottom right
						currentPixelX += 8;
						currentPixelY += 9;
						cachedTexturePoints[0][2] = (currentPixelX / 480f);
						cachedTexturePoints[0][3] = (currentPixelY / 112f);
						break;
					case STICK:
						//First face
						currentPixelX += 8;
						cachedTexturePoints[0][0] = (currentPixelX / 480f);
						cachedTexturePoints[0][1] = (currentPixelY / 112f);

						currentPixelX += 17;
						currentPixelY += 9;
						cachedTexturePoints[0][2] = (currentPixelX / 480f);
						cachedTexturePoints[0][3] = (currentPixelY / 112f);

						//Second face
						currentPixelY -= 1;
						cachedTexturePoints[1][0] = (currentPixelX / 480f);
						cachedTexturePoints[1][1] = (currentPixelY / 112f);

						currentPixelX += 4;
						currentPixelY += 1;
						cachedTexturePoints[1][2] = (currentPixelX / 480f);
						cachedTexturePoints[1][3] = (currentPixelY / 112f);

						//Third face
						currentPixelY -= 1;
						cachedTexturePoints[2][0] = (currentPixelX / 480f);
						cachedTexturePoints[2][1] = (currentPixelY / 112f);

						currentPixelX += 1;
						currentPixelY += 1;
						cachedTexturePoints[2][2] = (currentPixelX / 480f);
						cachedTexturePoints[2][3] = (currentPixelY / 112f);
						break;
					case FIN:
						//First face
						currentPixelX += 25;
						cachedTexturePoints[0][0] = (currentPixelX / 480f);
						cachedTexturePoints[0][1] = (currentPixelY / 112f);

						currentPixelX += 5;
						currentPixelY += 8;
						cachedTexturePoints[0][2] = (currentPixelX / 480f);
						cachedTexturePoints[0][3] = (currentPixelY / 112f);

						//Second face
						currentPixelY -= 8;
						cachedTexturePoints[1][0] = (currentPixelX / 480f);
						cachedTexturePoints[1][1] = (currentPixelY / 112f);

						currentPixelX += 10;
						currentPixelY += 9;
						cachedTexturePoints[1][2] = (currentPixelX / 480f);
						cachedTexturePoints[1][3] = (currentPixelY / 112f);
						break;
					default:
						MainMod.LOGGER.error("Unknown part type \"" + type.toString() + "\" with an id of " + modelId + "!");
						break;
				}
			} else {
				switch (type) {
					case TIP:
						//Top left
						cachedTexturePoints[0][0] = (currentPixelX / 480f);
						cachedTexturePoints[0][1] = (currentPixelY / 112f);
						//Shift to bottom right
						currentPixelX += 4;
						currentPixelY += 5;
						cachedTexturePoints[0][2] = (currentPixelX / 480f);
						cachedTexturePoints[0][3] = (currentPixelY / 112f);
						break;
					case STICK:
						//First face
						currentPixelX += 4;
						cachedTexturePoints[0][0] = (currentPixelX / 480f);
						cachedTexturePoints[0][1] = (currentPixelY / 112f);

						currentPixelX += 9;
						currentPixelY += 5;
						cachedTexturePoints[0][2] = (currentPixelX / 480f);
						cachedTexturePoints[0][3] = (currentPixelY / 112f);

						//Second face
						currentPixelY -= 1;
						cachedTexturePoints[1][0] = (currentPixelX / 480f);
						cachedTexturePoints[1][1] = (currentPixelY / 112f);

						currentPixelX += 4;
						currentPixelY += 1;
						cachedTexturePoints[1][2] = (currentPixelX / 480f);
						cachedTexturePoints[1][3] = (currentPixelY / 112f);

						//Third face
						currentPixelY -= 1;
						cachedTexturePoints[2][0] = (currentPixelX / 480f);
						cachedTexturePoints[2][1] = (currentPixelY / 112f);

						currentPixelX += 1;
						currentPixelY += 1;
						cachedTexturePoints[2][2] = (currentPixelX / 480f);
						cachedTexturePoints[2][3] = (currentPixelY / 112f);
						break;
					case FIN:
						//First face
						currentPixelX += 13;
						cachedTexturePoints[0][0] = (currentPixelX / 480f);
						cachedTexturePoints[0][1] = (currentPixelY / 112f);

						currentPixelX += 5;
						currentPixelY += 4;
						cachedTexturePoints[0][2] = (currentPixelX / 480f);
						cachedTexturePoints[0][3] = (currentPixelY / 112f);

						//Second face
						currentPixelY -= 4;
						cachedTexturePoints[1][0] = (currentPixelX / 480f);
						cachedTexturePoints[1][1] = (currentPixelY / 112f);

						currentPixelX += 6;
						currentPixelY += 5;
						cachedTexturePoints[1][2] = (currentPixelX / 480f);
						cachedTexturePoints[1][3] = (currentPixelY / 112f);
						break;
					default:
						MainMod.LOGGER.error("Unknown part type \"" + type.toString() + "\" with an id of " + modelId + "!");
						break;
				}
			}
		}

		if (getY) {
			return cachedTexturePoints[rectIndex][POINT_LOOKUP_Y[index]];
		}
		return cachedTexturePoints[rectIndex][POINT_LOOKUP_X[index]];
	}

	public byte getSize() {
		if (size != -1) {
			return size;
		}

		//Don't calculate the size server side
		if (IS_SERVER_SIDE) {
			MainMod.LOGGER.error("getSize was called on the server!");
			MainMod.LOGGER.error("This should never happen!");
			return 0;
		}

		switch (mode) {
			case TEXTURE:
				try {
					size = Byte.parseByte(Text.translatable("entity.fletching-additions.custom_arrow.render_" + type.toString().toLowerCase() + "_size.texture." + textureId).getString(), 10);
				
				} catch(Exception err) {
					MainMod.LOGGER.debug(err.toString());

					if (textureId < 160) {
						if (type == Type.STICK) {
							size = 9;
						} else {
							size = 4;
						}
					} else {
						if (type == Type.STICK) {
							size = 17;
						} else {
							size = 8;
						}
					}
				}
				break;

				case MODEL:
					try {
						size = Byte.parseByte(Text.translatable("entity.fletching-additions.custom_arrow.render_" + type.toString().toLowerCase() + "_size.model." + modelId.replaceAll(":", ".")).getString(), 10);
						
					} catch(Exception err) {
						MainMod.LOGGER.debug(err.toString());

						size = 16;
					}
					break;

				case NONE:
				default:
					size = 0;
					break;
			}

		return size;
	}
}