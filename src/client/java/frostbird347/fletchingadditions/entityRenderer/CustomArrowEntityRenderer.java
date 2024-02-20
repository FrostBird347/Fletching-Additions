package frostbird347.fletchingadditions.entityRenderer;

import frostbird347.fletchingadditions.MainMod;
import frostbird347.fletchingadditions.entity.CustomArrowEntity;
import frostbird347.fletchingadditions.entity.CustomArrowEntityRenderInfo;
import frostbird347.fletchingadditions.entity.CustomArrowEntityRenderPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
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

	public CustomArrowEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
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
		matrices.scale(0.05625f, 0.05625f, 0.05625f);
		matrices.translate(0.0, 0.0, 0.0);

		//Setup some stuff I don't fully understand
		VertexConsumer renderBuffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(this.getTexture(arrow)));
		MatrixStack.Entry topMatrixEntry = matrices.peek();
		Matrix4f positionMatrix = topMatrixEntry.getPositionMatrix();
		Matrix3f normalMatrix = topMatrixEntry.getNormalMatrix();

		int currentOffset = 0;
		for (byte i = 0; i < arrow.renderInfo.renderList.size(); i++) {
			CustomArrowEntityRenderPart currentPart = arrow.renderInfo.renderList.get(i);

			switch (currentPart.mode) {
				case TEXTURE:
					switch (currentPart.type) {
						case TIP:
							for (byte _i = 0; _i < 4; _i++) {
								//Rotate in a loop for all 4 sides
								matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0f));

								//I literally just spawned a regular and custom arrow at the same spot and then manually changed values until the tip matched up with the arrow (z-fighting was surprisingly helpful)
								renderRectangle(new Vec3i(1 - currentOffset, -2, 0), new Vec3i(5 - currentOffset, 2, 0), new Vec3i(1, 0, 1), currentPart, 0, positionMatrix, normalMatrix, renderBuffer, light);
							}
							currentOffset += 4;
							break;
						default:
							break;
					}
					break;
				//TODO: Add model support
				case MODEL:
					break;
				case NONE:
					break;
				default:
					MainMod.LOGGER.error("Unknown mode \"" + currentPart.mode.toString() +"\"!");
			}
		}

		//The dead code here will be removed soon
		if (false) {
		//Back of the fins
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -7, -2, -2, 0.0f, 0.15625f, -1, 0, 0, light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -7, -2, 2, 0.15625f, 0.15625f, -1, 0, 0, light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -7, 2, 2, 0.15625f, 0.3125f, -1, 0, 0, light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -7, 2, -2, 0.0f, 0.3125f, -1, 0, 0, light);
		//Front of the fins
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -7, 2, -2, 0.0f, 0.15625f, 1, 0, 0, light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -7, 2, 2, 0.15625f, 0.15625f, 1, 0, 0, light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -7, -2, 2, 0.15625f, 0.3125f, 1, 0, 0, light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -7, -2, -2, 0.0f, 0.3125f, 1, 0, 0, light);

		//Main arrow part
		for (int u = 0; u < 4; ++u) {
			//Rotate in a loop for all 4 sides
			matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0f));

			//A single rectangle for each side
			//x, y, z: pos
			//u, v: x, y percentage of the texture to render at this corner?
			//	^ So something like 0,0 0,1 1,0 1,1 would render the full texture
			//	^ While 1,1 1,0 0,1 0,0 should render the same as the one above, but with the x and y flipped?
			this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -8, -2, 0, 0.0f, 0.0f, 0, 1, 0, light);
			this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, 8, -2, 0, 0.5f, 0.0f, 0, 1, 0, light);
			this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, 8, 2, 0, 0.5f, 0.015625f, 0, 1, 0, light);
			this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, -8, 2, 0, 0.0f, 0.015625f, 0, 1, 0, light);
		}
		}

		//Finally render all the vertex data
		matrices.pop();
		super.render(arrow, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	//This abomination works somehow
	//The z point min/max order might need to be changed in the future though
	private void renderRectangle(Vec3i minPoint, Vec3i maxPoint, Vec3i normals, CustomArrowEntityRenderPart part, int rectIndex, Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer renderBuffer, int light) {
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, maxPoint.getX(), minPoint.getY(),  minPoint.getZ(), part.getCoord(0, false, rectIndex), part.getCoord(0, true, rectIndex), normals.getX(), normals.getZ(), normals.getY(), light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, minPoint.getX(), minPoint.getY(),  minPoint.getZ(), part.getCoord(1, false, rectIndex), part.getCoord(1, true, rectIndex), normals.getX(), normals.getZ(), normals.getY(), light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, minPoint.getX(), maxPoint.getY(),  maxPoint.getZ(), part.getCoord(2, false, rectIndex), part.getCoord(2, true, rectIndex), normals.getX(), normals.getZ(), normals.getY(), light);
		this.addVertexPoint(positionMatrix, normalMatrix, renderBuffer, maxPoint.getX(), maxPoint.getY(),  maxPoint.getZ(), part.getCoord(3, false, rectIndex), part.getCoord(3, true, rectIndex), normals.getX(), normals.getZ(), normals.getY(), light);
	}

	//Renders a vertex point of a rectangle
	//Every 4 times this is called a new rectangle is rendered
	public void addVertexPoint(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer renderBuffer, int x, int y, int z, float u, float v, int normalX, int normalZ, int normalY, int light) {
		renderBuffer.vertex(positionMatrix, x, y, z).color(255, 255, 255, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, normalX, normalY, normalZ).next();
	}
}