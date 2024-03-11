package frostbird347.fletchingadditions.entityRenderer;

import java.util.ArrayList;
import frostbird347.fletchingadditions.entity.CustomArrowEntity;
import frostbird347.fletchingadditions.entity.CustomArrowEntityRenderPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vec3i;

//Heavily modified vanilla arrows that have interchangable components
public class CustomArrowEntityRenderer extends EntityRenderer<CustomArrowEntity> {
	public static final Identifier TEXTURE = new Identifier("fletching-additions:textures/entity/projectiles/custom_arrow.png");
	public static final float SCALE_MULT = 0.05625f;
	private final ItemRenderer itemRenderer;

	public CustomArrowEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	public Identifier getTexture(CustomArrowEntity arrow) {
		return TEXTURE;
	}

	@Override
	public void render(CustomArrowEntity arrow, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		//Apply the arrow's current pitch and yaw
		matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(MathHelper.lerp(tickDelta, arrow.prevYaw, arrow.getYaw()) - 90.0f));
		matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(MathHelper.lerp(tickDelta, arrow.prevPitch, arrow.getPitch())));

		//Shake the arrow a little when it impacts with the ground
		float shake = (float)arrow.shake - tickDelta;
		if (shake > 0.0f) {
			float shakeAngle = -MathHelper.sin(shake * 3.0f) * shake;
			matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(shakeAngle));
		}

		//Rotate arrow by 45°
		//Change it's scale down to something more reasonable
		//and finally move it so the tip will only just be embedded within the ground
		matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(45f));
		matrices.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
		matrices.translate(0.0, 0.0, 0.0);

		//Setup some stuff I don't fully understand
		VertexConsumer renderBuffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(this.getTexture(arrow)));
		MatrixStack.Entry topMatrixEntry = matrices.peek();
		Matrix4f positionMatrix = topMatrixEntry.getPositionMatrix();
		Matrix3f normalMatrix = topMatrixEntry.getNormalMatrix();

		float currentOffset = 0;
		ArrayList<ItemModelRenderInfo> itemsToRender = new ArrayList<ItemModelRenderInfo>();
		for (byte i = 0; i < arrow.renderInfo.renderList.size(); i++) {
			CustomArrowEntityRenderPart currentPart = arrow.renderInfo.renderList.get(i);
			currentPart.checkForLangReload();

			switch (currentPart.mode) {
				case TEXTURE:

					//Make sure flat horizontal/vertical parts are actually horizontal/vertical
					if (currentPart.side != CustomArrowEntityRenderPart.RenderSide.BOTH) {
						matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-45f));
					}

					switch (currentPart.type) {
						case TIP:
							for (byte _i = 0; _i < 4; _i++) {
								//Rotate 4 times for all 4 sides
								matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0f));

								//Check if the side should be rendered
								if (currentPart.side == CustomArrowEntityRenderPart.RenderSide.BOTH || (currentPart.side == CustomArrowEntityRenderPart.RenderSide.FLAT_VERTICAL && _i % 2 == 1) || (currentPart.side == CustomArrowEntityRenderPart.RenderSide.FLAT_HORIZONTAL && _i % 2 == 0)) {
									
									//I literally just spawned a spectral and custom arrow at the same spot and then manually changed values until the tip matched up with the arrow (z-fighting was surprisingly helpful)
									if (currentPart.textureId < 160) {
										renderRectangle(new Vec3f(1 - currentOffset, -2, 0), new Vec3f(5 - currentOffset, 2, 0), new Vec3i(0, 0, 1), currentPart, 0, (_i > 2), positionMatrix, normalMatrix, renderBuffer, light);
									} else {
										renderRectangle(new Vec3f(1 - currentOffset, -3.6f, 0), new Vec3f(9 - currentOffset, 3.6f, 0), new Vec3i(0, 0, 1), currentPart, 0, (_i > 2), positionMatrix, normalMatrix, renderBuffer, light);
									}
								}
							}

							currentOffset += 4;
							break;
						case STICK:
							for (byte _i = 0; _i < 4; _i++) {
								matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0f));

								if (currentPart.side == CustomArrowEntityRenderPart.RenderSide.BOTH || (currentPart.side == CustomArrowEntityRenderPart.RenderSide.FLAT_VERTICAL && _i % 2 == 1) || (currentPart.side == CustomArrowEntityRenderPart.RenderSide.FLAT_HORIZONTAL && _i % 2 == 0)) {

									if (currentPart.textureId < 160) {
										//Variable width part
										renderRectangle(new Vec3f(-4 - currentOffset + (9 - currentPart.getSize()), -2, 0), new Vec3f(5 - currentOffset + (9 - currentPart.getSize()), 2, 0), new Vec3i(0, 0, 1), currentPart, 0, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);
										//Part that the fins are connected to
										renderRectangle(new Vec3f(1 - currentOffset - currentPart.getSize(), -0.4f, 0), new Vec3f(5 - currentOffset - currentPart.getSize(), 0.4f, 0), new Vec3i(0, 0, 1), currentPart, 1, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);

									} else {
										renderRectangle(new Vec3f(-12 - currentOffset + (17 - currentPart.getSize()), -3.6f, 0), new Vec3f(5 - currentOffset + (17 - currentPart.getSize()), 3.6f, 0), new Vec3i(0, 0, 1), currentPart, 0, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);
										
										renderRectangle(new Vec3f(1f - currentOffset - currentPart.getSize(), -0.4f, 0), new Vec3f(5f - currentOffset - currentPart.getSize(), 0.4f, 0), new Vec3i(0, 0, 1), currentPart, 1, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);
									}
								}
							}

							currentOffset += currentPart.getSize();

							//Back of the tip
							renderRectangle(new Vec3f(2 - currentOffset, -0.4f, -0.4f), new Vec3f(2 - currentOffset, 0.4f, 0.4f), new Vec3i(-1, 0, 0), currentPart, 2, false, positionMatrix, normalMatrix, renderBuffer, light);
							renderRectangle(new Vec3f(2 - currentOffset, 0.4f, -0.4f), new Vec3f(2 - currentOffset, -0.4f, 0.4f), new Vec3i(1, 0, 0), currentPart, 2, true, positionMatrix, normalMatrix, renderBuffer, light);
							break;
						case FIN:
							for (byte _i = 0; _i < 4; _i++) {
								matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0f));	
								
								if (currentPart.side == CustomArrowEntityRenderPart.RenderSide.BOTH || (currentPart.side == CustomArrowEntityRenderPart.RenderSide.FLAT_VERTICAL && _i % 2 == 1) || (currentPart.side == CustomArrowEntityRenderPart.RenderSide.FLAT_HORIZONTAL && _i % 2 == 0)) {

									if (currentPart.textureId < 160) {
										//Same as the back of the tip, but more than one pixel and offset to a corner
										renderRectangle(new Vec3f(2 - currentOffset, -3.6f, -3.6f), new Vec3f(2 - currentOffset, -0.4f, 0.4f), new Vec3i(-1, 0, 0), currentPart, 0, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);
										renderRectangle(new Vec3f(2 - currentOffset, 3.6f, -3.6f), new Vec3f(2 - currentOffset, 0.4f, 0.4f), new Vec3i(1, 0, 0), currentPart, 0, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);

										renderRectangle(new Vec3f(3 - currentOffset - currentPart.getSize(), -4.4f, 0), new Vec3f(9 - currentOffset - currentPart.getSize(), -0.4f, 0), new Vec3i(0, 0, 1), currentPart, 1, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);
										renderRectangle(new Vec3f(3 - currentOffset - currentPart.getSize(), 0.4f, 0), new Vec3f(9 - currentOffset - currentPart.getSize(), 4.4f, 0), new Vec3i(0, 0, 1), currentPart, 2, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);
										
									} else {
										renderRectangle(new Vec3f(2 - currentOffset, -6.8f, -3.6f), new Vec3f(2 - currentOffset, -0.4f, 0.4f), new Vec3i(-1, 0, 0), currentPart, 0, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);
										renderRectangle(new Vec3f(2 - currentOffset, 6.8f, -3.6f), new Vec3f(2 - currentOffset, 0.4f, 0.4f), new Vec3i(1, 0, 0), currentPart, 0, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);

										renderRectangle(new Vec3f(3 - currentOffset - currentPart.getSize(), -7.6f, 0), new Vec3f(13f - currentOffset - currentPart.getSize(), -0.4f, 0), new Vec3i(0, 0, 1), currentPart, 1, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);
										renderRectangle(new Vec3f(3 - currentOffset - currentPart.getSize(), 0.4f, 0), new Vec3f(13f - currentOffset - currentPart.getSize(), 7.6f, 0), new Vec3i(0, 0, 1), currentPart, 2, (_i < 2), positionMatrix, normalMatrix, renderBuffer, light);
									}
								}
							}
							break;
						default:
							break;
					}

					//Make sure flat horizontal/vertical parts are actually horizontal/vertical
					if (currentPart.side != CustomArrowEntityRenderPart.RenderSide.BOTH) {
						matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(45f));
					}
					break;
				//Items can't be rendered yet, so we store the info needed to render them later;
				case ITEM:
					itemsToRender.add(currentPart.getModelInfo(currentOffset));
					currentOffset += currentPart.getSize();
					break;
				case NONE:
				default:
					break;
			}
		}

		//Render the items at the end so we don't crash the game
		for (int i = 0; i < itemsToRender.size(); i++) {
			ItemModelRenderInfo currentItem = itemsToRender.get(i);
			switch (currentItem.renderSide) {
				case BOTH:
					matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-90f));
					renderItem(currentItem, matrices, vertexConsumers, light);
					matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90f));
					renderItem(currentItem, matrices, vertexConsumers, light);
					break;
				case FLAT_HORIZONTAL:
					matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-45f));
					matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-90f));
					renderItem(currentItem, matrices, vertexConsumers, light);
					matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90f));
					matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(45f));
					break;
				case FLAT_VERTICAL:
					matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-45f));
					renderItem(currentItem, matrices, vertexConsumers, light);
					matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(45f));
					break;
			}
		}

		//Finally render all the vertex data
		matrices.pop();
		super.render(arrow, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	private void renderItem(ItemModelRenderInfo currentItem, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		//Move model to the correct position
		matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(currentItem.rotation.getX()));
		matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(currentItem.rotation.getY()));
		matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(currentItem.rotation.getZ()));
		matrices.translate(currentItem.translate.getX(), currentItem.translate.getY(), currentItem.translate.getZ());
		matrices.scale(currentItem.scale, currentItem.scale, currentItem.scale);

		matrices.translate(-currentItem.partOffset, 0, 0);
		matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-90f));
		matrices.scale(1f/SCALE_MULT, 1f/SCALE_MULT, 1f/SCALE_MULT);
		matrices.translate(0.0, -0.006, 0.0);

		//Finally render the item
		itemRenderer.renderItem(currentItem.item, net.minecraft.client.render.model.json.ModelTransformation.Mode.GROUND, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, 0);
		
		//Move the model back
		matrices.translate(0.0, 0.006, 0.0);
		matrices.scale(SCALE_MULT, SCALE_MULT, SCALE_MULT);
		matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90f));
		matrices.translate(currentItem.partOffset, 0, 0);

		matrices.scale(1f/currentItem.scale, 1f/currentItem.scale, 1f/currentItem.scale);
		matrices.translate(-currentItem.translate.getX(), -currentItem.translate.getY(), -currentItem.translate.getZ());
		matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-currentItem.rotation.getX()));
		matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-currentItem.rotation.getY()));
		matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-currentItem.rotation.getZ()));
	}

	//This abomination works somehow
	//The z point min/max order might need to be changed in the future though
	private void renderRectangle(Vec3f minPoint, Vec3f maxPoint, Vec3i normals, CustomArrowEntityRenderPart part, int rectIndex, boolean flipYAxis, Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer renderBuffer, int light) {
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, maxPoint.getX(), minPoint.getY(),  minPoint.getZ(), part.getCoord(0, false, rectIndex, flipYAxis), part.getCoord(0, true, rectIndex, flipYAxis), normals.getX(), normals.getZ(), normals.getY(), light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, minPoint.getX(), minPoint.getY(),  maxPoint.getZ(), part.getCoord(1, false, rectIndex, flipYAxis), part.getCoord(1, true, rectIndex, flipYAxis), normals.getX(), normals.getZ(), normals.getY(), light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, minPoint.getX(), maxPoint.getY(),  maxPoint.getZ(), part.getCoord(2, false, rectIndex, flipYAxis), part.getCoord(2, true, rectIndex, flipYAxis), normals.getX(), normals.getZ(), normals.getY(), light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, maxPoint.getX(), maxPoint.getY(),  minPoint.getZ(), part.getCoord(3, false, rectIndex, flipYAxis), part.getCoord(3, true, rectIndex, flipYAxis), normals.getX(), normals.getZ(), normals.getY(), light);
	}

	//Renders a vertex point of a rectangle
	//Every 4 times this is called a new rectangle is rendered
	public void addVertexPoint(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer renderBuffer, float x, float y, float z, float u, float v, int normalX, int normalZ, int normalY, int light) {
		renderBuffer.vertex(positionMatrix, x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, normalX, normalY, normalZ).next();
	}
}