package frostbird347.fletchingadditions.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class EntityManager {

	public static final EntityType<CustomArrowEntity> CUSTOM_ARROW =  FabricEntityTypeBuilder.<CustomArrowEntity>create(SpawnGroup.MISC, CustomArrowEntity::new).dimensions(EntityDimensions.fixed(0.5f, 0.5f)).trackRangeBlocks(4).trackedUpdateRate(20).build();

	public static void registerEntities() {
		 Registry.register(Registry.ENTITY_TYPE, new Identifier("fletching-additions", "custom_arrow"), CUSTOM_ARROW);
	}
}