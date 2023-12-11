package frostbird347.fletchingadditions.entityRenderer;

import frostbird347.fletchingadditions.entity.EntityManager;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class EntityRendererManager {

	public static void registerEntityRenderers() {
		EntityRendererRegistry.register(EntityManager.CUSTOM_ARROW, (context) -> new CustomArrowEntityRenderer(context));
	}
}