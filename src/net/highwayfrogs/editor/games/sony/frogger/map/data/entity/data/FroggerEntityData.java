package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data;

import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.cave.*;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert.FroggerEntityDataCrack;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert.FroggerEntityDataCrocodileHead;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert.FroggerEntityDataFallingRock;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert.FroggerEntityDataThermal;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest.*;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.general.FroggerEntityDataBonusFly;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.general.FroggerEntityDataCheckpoint;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.general.FroggerEntityDataTrigger;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.jungle.*;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.retro.FroggerEntityDataBabyFrog;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.retro.FroggerEntityDataBeaver;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.retro.FroggerEntityDataLogSnake;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.rushedmap.FroggerEntityDataCrocodileOld;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.rushedmap.FroggerEntityDataSwanOld;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.rushedmap.FroggerEntityDataTurtleOld;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.suburbia.FroggerEntityDataDog;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.suburbia.FroggerEntityDataTurtle;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.swamp.*;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.volcano.FroggerEntityDataColorTrigger;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.volcano.FroggerEntityDataTriggeredPlatform;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;

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
public abstract class FroggerEntityData extends SCGameData<FroggerGameInstance> {
    private final FroggerMapFile mapFile;
    private FroggerMapEntity parentEntity;
    private static final Map<String, Tuple2<Class<? extends FroggerEntityData>, Constructor<? extends FroggerEntityData>>> CACHE_MAP = new HashMap<>();
    private static final List<Class<? extends FroggerEntityData>> REGISTERED_ENTITY_TYPES = Arrays.asList(FroggerEntityDataRopeBridge.class,
            FroggerEntityDataBreakingBranch.class, FroggerEntityDataBabyFrog.class, FroggerEntityDataCrusher.class, FroggerEntityDataFallingRock.class, FroggerEntityDataBeeHive.class,
            FroggerEntityDataSlug.class, FroggerEntityDataRat.class, FroggerEntityDataOutro.class, FroggerEntityDataCaveLight.class, FroggerEntityDataDog.class, FroggerEntityDataSquirt.class,
            FroggerEntityDataPress.class, FroggerEntityDataEvilPlant.class, FroggerEntityDataCrack.class, FroggerEntityDataFatFireFly.class, FroggerEntityDataThermal.class, FroggerEntityDataCheckpoint.class,
            FroggerEntityDataBeaver.class, FroggerEntityDataSquirrel.class, FroggerEntityDataPathInfo.class, FroggerEntityDataLogSnake.class, FroggerEntityDataBonusFly.class, FroggerEntityDataPlinthFrog.class,
            FroggerEntityDataMatrix.class, FroggerEntityDataRaceSnail.class, FroggerEntityDataColorTrigger.class, FroggerEntityDataSwayingBranch.class, FroggerEntityDataTurtle.class,
            FroggerEntityDataOutroPlinth.class, FroggerEntityDataHedgehog.class, FroggerEntityDataFallingLeaf.class, FroggerEntityDataCrocodileHead.class, FroggerEntityDataTrigger.class,
            FroggerEntityDataFatFireFlyBuild1.class, FroggerEntityDataWeb.class, FroggerEntityDataTurtleOld.class, FroggerEntityDataSwanOld.class, FroggerEntityDataCrocodileOld.class,
            FroggerEntityDataSpider.class, FroggerEntityDataTriggeredPlatform.class);

    public FroggerEntityData(FroggerMapFile mapFile) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    public ILogger getLogger() {
        return this.parentEntity != null ? this.parentEntity.getLogger() : super.getLogger();
    }

    /**
     * Sets up the entity data for editing.
     * @param editor The editor to build on.
     */
    public abstract void setupEditor(GUIEditorGrid editor);

    /**
     * Get the x, y, z position of this entity.
     * @param position A cached position array to write output to.
     * @return position
     */
    public float[] getPositionAndRotation(float[] position) {
        throw new UnsupportedOperationException("Positional data is not supported for " + Utils.getSimpleName(this) + ".");
    }

    /**
     * Sets up the entity data for editing.
     * @param manager The entity manager.
     * @param editor  The editor to apply to.
     */
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        this.setupEditor(editor);
    }

    /**
     * Make FroggerEntityData for the given form.
     * @param mapFile The map file to create data for.
     * @param entity   The entity to make data for.
     * @return entityData
     */
    @SneakyThrows
    public static FroggerEntityData makeData(FroggerMapFile mapFile, FroggerMapEntity entity) {
        if (entity == null)
            return null;

        String dataClassName = mapFile.getConfig().getEntityBank().getConfig().getString(entity.getTypeName(), null);
        if (dataClassName == null)
            return null;

        Tuple2<Class<? extends FroggerEntityData>, ? extends Constructor<? extends FroggerEntityData>> entityDataFactory = getEntityDataFactory(dataClassName);
        if (entityDataFactory == null)
            throw new RuntimeException("Failed to find FroggerEntityData class for the type: " + entity.getTypeName() + ", " + dataClassName);

        FroggerEntityData newData = entityDataFactory.getB().newInstance(mapFile);
        newData.parentEntity = entity;
        return newData;
    }

    /**
     * Gets the entity data class for the given form.
     * @param instance The game instance to create data for.
     * @param formEntry The form entry to get the entity class for.
     * @return entityData
     */
    public static Class<? extends FroggerEntityData> getEntityDataClass(FroggerGameInstance instance, IFroggerFormEntry formEntry) {
        if (formEntry == null)
            return null;

        String dataClassName = instance.getVersionConfig().getEntityBank().getConfig().getString(formEntry.getEntityTypeName(), null);
        if (dataClassName == null)
            return null;

        Tuple2<Class<? extends FroggerEntityData>, ? extends Constructor<? extends FroggerEntityData>> entityDataFactory = getEntityDataFactory(dataClassName);
       if (entityDataFactory != null)
           return entityDataFactory.getA();

        throw new RuntimeException("Failed to find FroggerEntityData class for the type: " + formEntry.getEntityTypeName() + ", " + dataClassName);
    }

    private static Tuple2<Class<? extends FroggerEntityData>, Constructor<? extends FroggerEntityData>> getEntityDataFactory(String dataClassName) {
        Tuple2<Class<? extends FroggerEntityData>, Constructor<? extends FroggerEntityData>> prefixedResult = CACHE_MAP.get(dataClassName);
        if (prefixedResult != null)
            return prefixedResult;

        prefixedResult = CACHE_MAP.get("FroggerEntityData" + dataClassName);
        return prefixedResult;
    }

    static {
        for (Class<? extends FroggerEntityData> dataClass : REGISTERED_ENTITY_TYPES) {
            try {
                CACHE_MAP.put(dataClass.getSimpleName(), new Tuple2<>(dataClass, dataClass.getConstructor(FroggerMapFile.class)));
            } catch (NoSuchMethodException e) {
                System.err.println(dataClass.getSimpleName() + " probably does not have a constructor accepting a single FroggerMapFile.");
            }
        }
    }
}