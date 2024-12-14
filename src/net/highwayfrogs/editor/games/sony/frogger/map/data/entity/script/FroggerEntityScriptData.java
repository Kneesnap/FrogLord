package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script;

import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.jungle.FroggerEntityScriptDataFloatingTree;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.sky.FroggerEntityScriptDataBalloon;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.sky.FroggerEntityScriptDataHawk;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.sky.FroggerEntityScriptDataHelicopter;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.sky.FroggerEntityScriptDataHeliumBalloon;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.swamp.FroggerEntityScriptDataBobbingWaste;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.swamp.FroggerEntityScriptDataNuclearBarrel;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.swamp.FroggerEntityScriptDataSpecialWaterNoise;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.volcano.FroggerEntityScriptDataMechanism;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.volcano.FroggerEntityScriptDataPlatform2;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens to entity script data.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public abstract class FroggerEntityScriptData extends SCGameData<FroggerGameInstance> {
    private final FroggerMapFile mapFile;
    private FroggerMapEntity parentEntity;
    private static final Map<String, Tuple2<Class<? extends FroggerEntityScriptData>, Constructor<? extends FroggerEntityScriptData>>> CACHE_MAP = new HashMap<>();
    private static final List<Class<? extends FroggerEntityScriptData>> REGISTERED_SCRIPT_TYPES = Arrays.asList(FroggerEntityScriptDataBobbingWaste.class, FroggerEntityScriptDataPlatform2.class,
            FroggerEntityScriptDataNoise.class, FroggerEntityScriptDataButterfly.class, FroggerEntityScriptDataNuclearBarrel.class, FroggerEntityScriptDataHelicopter.class,
            FroggerEntityScriptDataHawk.class, FroggerEntityScriptDataMechanism.class, FroggerEntityScriptDataHeliumBalloon.class, FroggerEntityScriptDataBalloon.class, FroggerEntityScriptDataSpecialWaterNoise.class,
            FroggerEntityScriptDataFloatingTree.class);

    public FroggerEntityScriptData(FroggerMapFile mapFile) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
    }

    @Override
    public ILogger getLogger() {
        return this.parentEntity != null ? this.parentEntity.getLogger() : super.getLogger();
    }

    /**
     * Add entity script data to the editor.
     * @param editor The editor to build on top of.
     * @param manager the entity manager responsible for the data
     */
    public abstract void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager);

    /**
     * Make script data for the given form.
     * @param entity The entity to make the data for.
     * @param formEntry The form entry to apply.
     * @return entityData
     */
    @SneakyThrows
    public static FroggerEntityScriptData makeData(FroggerMapEntity entity, IFroggerFormEntry formEntry) {
        if (formEntry == null)
            return null;

        String dataClassName = entity.getConfig().getFormBank().getConfig().getString(formEntry.getFormTypeName(), null);
        if (dataClassName == null)
            return null;

        Tuple2<Class<? extends FroggerEntityScriptData>, Constructor<? extends FroggerEntityScriptData>> scriptDataFactory = getScriptDataFactory(dataClassName);
        if (scriptDataFactory == null)
            throw new RuntimeException("Failed to find form class for the type: " + formEntry.getFormTypeName() + ", " + dataClassName);

        FroggerEntityScriptData scriptData = scriptDataFactory.getB().newInstance(entity.getMapFile());
        scriptData.parentEntity = entity;
        return scriptData;
    }

    /**
     * Get the script class for the given form.
     * @param instance The game instance to find the script data class for.
     * @param formEntry The form entry to create the entity data with.
     * @return entityData
     */
    public static Class<? extends FroggerEntityScriptData> getScriptDataClass(FroggerGameInstance instance, IFroggerFormEntry formEntry) {
        if (formEntry == null)
            return null;

        String dataClassName = instance.getVersionConfig().getFormBank().getConfig().getString(formEntry.getFormTypeName(), null);
        if (dataClassName == null)
            return null;

        Tuple2<Class<? extends FroggerEntityScriptData>, Constructor<? extends FroggerEntityScriptData>> scriptDataFactory = getScriptDataFactory(dataClassName);
        if (scriptDataFactory != null)
            return scriptDataFactory.getA();

        throw new RuntimeException("Failed to find FroggerEntityScriptData class for the type: " + formEntry.getEntityTypeName() + ", " + dataClassName);
    }

    private static Tuple2<Class<? extends FroggerEntityScriptData>, Constructor<? extends FroggerEntityScriptData>> getScriptDataFactory(String dataClassName) {
        Tuple2<Class<? extends FroggerEntityScriptData>, Constructor<? extends FroggerEntityScriptData>> prefixedResult = CACHE_MAP.get(dataClassName);
        if (prefixedResult != null)
            return prefixedResult;

        prefixedResult = CACHE_MAP.get("FroggerEntityScriptData" + dataClassName);
        return prefixedResult;
    }

    static {
        for (Class<? extends FroggerEntityScriptData> dataClass : REGISTERED_SCRIPT_TYPES) {
            try {
                CACHE_MAP.put(dataClass.getSimpleName(), new Tuple2<>(dataClass, dataClass.getConstructor(FroggerMapFile.class)));
            } catch (NoSuchMethodException e) {
                System.err.println(dataClass.getSimpleName() + " probably does not have a constructor accepting a single FroggerMapFile.");
            }
        }
    }
}