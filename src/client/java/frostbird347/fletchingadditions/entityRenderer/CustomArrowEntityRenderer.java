package frostbird347.fletchingadditions.entityRenderer;

import frostbird347.fletchingadditions.entity.CustomArrowEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

public class CustomArrowEntityRenderer extends ProjectileEntityRenderer<CustomArrowEntity> {
    public static final Identifier TEXTURE = new Identifier("textures/entity/projectiles/spectral_arrow.png");

    public CustomArrowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CustomArrowEntity spectralArrowEntity) {
        return TEXTURE;
    }
}