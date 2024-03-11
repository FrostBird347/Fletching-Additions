package frostbird347.fletchingadditions.entity;

import java.util.Map;
import frostbird347.fletchingadditions.MainMod;
import frostbird347.fletchingadditions.entityRenderer.ItemModelRenderInfo;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;

public class CustomArrowEntityRenderPart {
	public enum Type {
		TIP,
		STICK,
		FIN,
		EFFECT
	}
	public enum RenderMode {
		TEXTURE,
		ITEM,
		NONE
	}
	public enum RenderSide {
		BOTH,
		FLAT_HORIZONTAL,
		FLAT_VERTICAL
	}
	private static final Map<String, Type> TYPE_MAP = Map.of("t", Type.TIP, "s", Type.STICK, "f", Type.FIN, "e", Type.EFFECT);
	private static final Map<String, RenderMode> MODE_MAP = Map.of("texture", RenderMode.TEXTURE, "item", RenderMode.ITEM, "none", RenderMode.NONE);
	private static final Map<String, RenderSide> SIDE_MAP = Map.of("b", RenderSide.BOTH, "h", RenderSide.FLAT_HORIZONTAL, "v", RenderSide.FLAT_VERTICAL);

	public int textureId;
	public String itemId;
	public NbtCompound itemNbt;
	public Type type;
	public RenderMode mode;
	public RenderSide side;
	private float size;
	private int lastTextureId;
	private String lastItemId;
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
	private ItemStack cachedItemStack = null;
	private ItemModelRenderInfo cachedRenderInfo = null;
	private byte langReloadCache = MainMod.langReloads;
	private final boolean IS_SERVER_SIDE;

	public CustomArrowEntityRenderPart(CustomArrowEntity arrow, String _type, String _mode, NbtCompound data) {
		this(arrow);
		type = TYPE_MAP.getOrDefault(_type, Type.EFFECT);
		mode = MODE_MAP.getOrDefault(_mode, RenderMode.NONE);
		switch (mode) {
			case TEXTURE:
				try {
					textureId = Integer.parseInt(data.get("id").asString(), 10);
				} catch(Exception err) {
					MainMod.LOGGER.error("Failed to parse texture ID \"" + data.getString("id") + "\": " + err.getLocalizedMessage());
					textureId = 0;
				}
				side = SIDE_MAP.getOrDefault(data.getString("side"), RenderSide.BOTH);
				break;
			case ITEM:
				itemId = data.getString("id");
				itemNbt = data.getCompound("nbt");
				break;
			default:
				break;
		}
	}

	public CustomArrowEntityRenderPart(CustomArrowEntity arrow) {
		size = -1;
		textureId = 0;
		lastTextureId = -1;
		itemId = "";
		lastItemId = "_";
		cachedItemStack = null;
		IS_SERVER_SIDE = !arrow.world.isClient;
	}

	public float getCoord(int index, boolean getY, int rectIndex, boolean flipYAxis) {
		if (IS_SERVER_SIDE) {
			MainMod.LOGGER.error("getCoord was called on the server!");
			MainMod.LOGGER.error("This should never happen!");
			return -1;
		}

		if (lastTextureId != textureId) {
			lastTextureId = textureId;
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

						//Third face (which is the second flipped vertically)
						cachedTexturePoints[2][2] = ((currentPixelX) / 480f);
						cachedTexturePoints[2][1] = ((currentPixelY) / 112f);

						currentPixelX -= 10;
						currentPixelY -= 9;

						cachedTexturePoints[2][0] = ((currentPixelX) / 480f);
						cachedTexturePoints[2][3] = ((currentPixelY) / 112f);
						break;
					default:
						MainMod.LOGGER.error("Unknown part type \"" + type.toString() + "\" with an id of " + textureId + "!");
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

						//Third face (which is the second flipped vertically)
						cachedTexturePoints[2][2] = ((currentPixelX) / 480f);
						cachedTexturePoints[2][1] = ((currentPixelY) / 112f);

						currentPixelX -= 6;
						currentPixelY -= 5;

						cachedTexturePoints[2][0] = ((currentPixelX) / 480f);
						cachedTexturePoints[2][3] = ((currentPixelY) / 112f);
						break;
					default:
						MainMod.LOGGER.error("Unknown part type \"" + type.toString() + "\" with an id of " + textureId + "!");
						break;
				}
			}
		}

		if (getY) {
			if (side == RenderSide.BOTH || !flipYAxis) {
				return cachedTexturePoints[rectIndex][POINT_LOOKUP_Y[index]];
			} else {
				//Flip the y axis
				return cachedTexturePoints[rectIndex][POINT_LOOKUP_Y[(index + 2) % 4]];
			}
		}
		return cachedTexturePoints[rectIndex][POINT_LOOKUP_X[index]];
	}

	public float getSize() {
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

				size = getLangData("entity.fletching-additions.custom_arrow.texture.offset." + type.toString().toLowerCase() + "." + textureId, Float.valueOf(size)).floatValue();
				break;
			case ITEM:
				NbtCompound itemNbt = this.getItem().getNbt();
				String itemModelId = ".0";
				if (itemNbt != null) {
					itemModelId = "." + Integer.valueOf(itemNbt.getInt("CustomModelData")).toString().replaceAll("-", "_");
				}
				size = getLangData("entity.fletching-additions.custom_arrow.model.offset." + itemId.replaceAll(":", ".") + itemModelId, Float.valueOf(16)).floatValue();
				break;

			case NONE:
			default:
				size = 0;
				break;
		}
		return size;
	}

	public ItemStack getItem() {
		//Ensure this isn't run server side
		if (IS_SERVER_SIDE) {
			return null;
		}
		
		if (!itemId.equals(lastItemId)) {
			lastItemId = itemId;
			cachedRenderInfo = null;
			Item basicItem = Registry.ITEM.get(new Identifier(itemId));
			if (basicItem == null) {
				cachedItemStack = Items.BARRIER.getDefaultStack();
			} else {
				cachedItemStack = basicItem.getDefaultStack();

				if (itemNbt == null) {
					itemNbt = new NbtCompound();
				}
				cachedItemStack.setNbt(itemNbt);
			}
		}

		return cachedItemStack;
	}

	public ItemModelRenderInfo getModelInfo(float offset) {
		if (cachedRenderInfo == null) {
			ItemStack thisItem = this.getItem();
			NbtCompound itemNbt = thisItem.getNbt();
			String itemModelId = ".0";
			if (itemNbt != null) {
				itemModelId = "." + Integer.valueOf(itemNbt.getInt("CustomModelData")).toString().replaceAll("-", "_");
			}
			Vec3d translate = new Vec3d(
				getLangData("entity.fletching-additions.custom_arrow.model.translate_x." + itemId.replaceAll(":", ".") + itemModelId, 0.0).doubleValue(),
				getLangData("entity.fletching-additions.custom_arrow.model.translate_y." + itemId.replaceAll(":", ".") + itemModelId, 0.0).doubleValue(),
				getLangData("entity.fletching-additions.custom_arrow.model.translate_z." + itemId.replaceAll(":", ".") + itemModelId, 0.0).doubleValue()
			);
			float scale = getLangData("entity.fletching-additions.custom_arrow.model.resize." + itemId.replaceAll(":", ".") + itemModelId, 1f).floatValue();
			Vec3f rotate = new Vec3f(
				getLangData("entity.fletching-additions.custom_arrow.model.rotate_x." + itemId.replaceAll(":", ".") + itemModelId, 0f).floatValue(),
				getLangData("entity.fletching-additions.custom_arrow.model.rotate_y." + itemId.replaceAll(":", ".") + itemModelId, 0f).floatValue(),
				getLangData("entity.fletching-additions.custom_arrow.model.rotate_z." + itemId.replaceAll(":", ".") + itemModelId, 0f).floatValue()
			);
			RenderSide renderSide = getLangData("entity.fletching-additions.custom_arrow.model.side" + itemId.replaceAll(":", ".") + itemModelId, RenderSide.FLAT_HORIZONTAL);

			cachedRenderInfo = new ItemModelRenderInfo(thisItem, translate, scale, rotate, renderSide, offset);
		}

		return cachedRenderInfo;
	}

	private <T> T getLangData(String key, T defaultValue) {
		MainMod.LOGGER.info(key);
		try {
			String rawValue = Text.translatable(key).getString();
			if (rawValue.equals(key)) {
				return defaultValue;
			}

			if (defaultValue instanceof Byte) {
				return (T)Byte.valueOf(Byte.parseByte(rawValue, 10));
			}
			if (defaultValue instanceof Float) {
				return (T)Float.valueOf(Float.parseFloat(rawValue));
			}
			if (defaultValue instanceof Double) {
				return (T)Double.valueOf(Double.parseDouble(rawValue));
			}
			if (defaultValue instanceof RenderSide) {
				return (T)SIDE_MAP.getOrDefault(rawValue, (RenderSide)defaultValue);
			}
			
			MainMod.LOGGER.error("Unsupported type for key " + key);
			return defaultValue;
		} catch(Exception err) {
			//if (err)
			MainMod.LOGGER.error(err.toString());
			return defaultValue;
		}
	}

	public void checkForLangReload() {
		if (langReloadCache != MainMod.langReloads) {
			langReloadCache = MainMod.langReloads;
			size = -1;
			cachedRenderInfo = null;
		}
	}
	
}