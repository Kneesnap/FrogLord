package net.highwayfrogs.editor.file.map.entity.data;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.entity.data.cave.*;
import net.highwayfrogs.editor.file.map.entity.data.desert.EntityCrack;
import net.highwayfrogs.editor.file.map.entity.data.desert.EntityCrocodileHead;
import net.highwayfrogs.editor.file.map.entity.data.desert.EntityFallingRock;
import net.highwayfrogs.editor.file.map.entity.data.desert.EntityThermal;
import net.highwayfrogs.editor.file.map.entity.data.forest.*;
import net.highwayfrogs.editor.file.map.entity.data.general.BonusFlyEntity;
import net.highwayfrogs.editor.file.map.entity.data.general.CheckpointEntity;
import net.highwayfrogs.editor.file.map.entity.data.general.TriggerEntity;
import net.highwayfrogs.editor.file.map.entity.data.jungle.*;
import net.highwayfrogs.editor.file.map.entity.data.retro.EntityBabyFrog;
import net.highwayfrogs.editor.file.map.entity.data.retro.EntityBeaver;
import net.highwayfrogs.editor.file.map.entity.data.retro.EntitySnake;
import net.highwayfrogs.editor.file.map.entity.data.rushedmap.EntityCrocodileOld;
import net.highwayfrogs.editor.file.map.entity.data.rushedmap.EntitySwanOld;
import net.highwayfrogs.editor.file.map.entity.data.rushedmap.EntityTurtleOld;
import net.highwayfrogs.editor.file.map.entity.data.suburbia.EntityDog;
import net.highwayfrogs.editor.file.map.entity.data.suburbia.EntityTurtle;
import net.highwayfrogs.editor.file.map.entity.data.swamp.*;
import net.highwayfrogs.editor.file.map.entity.data.volcano.EntityColorTrigger;
import net.highwayfrogs.editor.file.map.entity.data.volcano.EntityTriggeredPlatform;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.EntityManager;
import net.highwayfrogs.editor.system.Tuple2;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents game-data.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
public abstract class EntityData extends GameObject {
    @Setter private Entity parentEntity;
    private static final Map<String, Tuple2<Class<? extends EntityData>, Constructor<? extends EntityData>>> CACHE_MAP = new HashMap<>();
    private static final List<Class<? extends EntityData>> REGISTERED_ENTITY_TYPES = Arrays.asList(EntityRopeBridge.class,
            BreakingBranchEntity.class, EntityBabyFrog.class, EntityCrusher.class, EntityFallingRock.class, BeeHiveEntity.class,
            EntitySlug.class, EntityRat.class, EntityOutroEntity.class, EntityFrogLight.class, EntityDog.class, EntitySquirt.class,
            EntityPress.class, EntityEvilPlant.class, EntityCrack.class, EntityFatFireFly.class, EntityThermal.class, CheckpointEntity.class,
            EntityBeaver.class, EntitySquirrel.class, PathData.class, EntitySnake.class, BonusFlyEntity.class, EntityPlinthFrog.class,
            MatrixData.class, EntityRaceSnail.class, EntityColorTrigger.class, SwayingBranchEntity.class, EntityTurtle.class,
            EntityOutroPlinth.class, EntityHedgehog.class, FallingLeafEntity.class, EntityCrocodileHead.class, TriggerEntity.class,
            EntityFatFireFlyBuild1.class, EntityWeb.class, EntityTurtleOld.class, EntitySwanOld.class, EntityCrocodileOld.class,
            EntitySpider.class, EntityTriggeredPlatform.class);

    /**
     * Add entity data to a table.
     * @param editor The editor to build on.
     */
    public abstract void addData(GUIEditorGrid editor);

    /**
     * Add entity data to the editor.
     * @param manager The entity manager.
     * @param editor  The editor to apply to.
     */
    public void addData(EntityManager manager, GUIEditorGrid editor) {
        this.addData(editor);
    }

    /**
     * Make entity data for the given form.
     * @param config The config to read from.
     * @param entity The entity to make data for.
     * @return entityData
     */
    @SneakyThrows
    public static EntityData makeData(FroggerEXEInfo config, Entity entity, Entity entityOwner) {
        if (entity == null)
            return null;

        String dataClassName = config.getEntityBank().getConfig().getString(entity.getTypeName(), null);
        if (dataClassName == null)
            return null;

        if (!CACHE_MAP.containsKey(dataClassName))
            throw new RuntimeException("Failed to find entity class for the type: " + entity.getTypeName() + ", " + dataClassName);
        EntityData newData = CACHE_MAP.get(dataClassName).getB().newInstance();
        newData.setParentEntity(entityOwner);
        return newData;
    }

    /**
     * Make entity data for the given form.
     * @param config The config to read from.
     * @param form   The form.
     * @return entityData
     */
    public static Class<? extends EntityData> getEntityClass(FroggerEXEInfo config, FormEntry form) {
        if (form == null)
            return null;

        String dataClassName = config.getEntityBank().getConfig().getString(form.getEntityName(), null);
        if (dataClassName == null)
            return null;

        if (!CACHE_MAP.containsKey(dataClassName))
            throw new RuntimeException("Failed to find entity class for the type: " + form.getEntityName() + ", " + dataClassName);
        return CACHE_MAP.get(dataClassName).getA();
    }

    static {
        for (Class<? extends EntityData> dataClass : REGISTERED_ENTITY_TYPES) {
            try {
                CACHE_MAP.put(dataClass.getSimpleName(), new Tuple2<>(dataClass, dataClass.getConstructor()));
            } catch (NoSuchMethodException e) {
                System.out.println(dataClass.getSimpleName() + " probably does not have a no-args constructor.");
            }
        }
    }
}
