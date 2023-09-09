package net.highwayfrogs.editor.file.map.entity.script;

import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.map.entity.script.jungle.ScriptFloatingTreeData;
import net.highwayfrogs.editor.file.map.entity.script.sky.ScriptBalloonData;
import net.highwayfrogs.editor.file.map.entity.script.sky.ScriptHeliumBalloon;
import net.highwayfrogs.editor.file.map.entity.script.swamp.ScriptBobbingWasteData;
import net.highwayfrogs.editor.file.map.entity.script.swamp.ScriptNuclearBarrelData;
import net.highwayfrogs.editor.file.map.entity.script.swamp.ScriptSpecialWaterNoiseData;
import net.highwayfrogs.editor.file.map.entity.script.volcano.ScriptHawkData;
import net.highwayfrogs.editor.file.map.entity.script.volcano.ScriptHelicopterData;
import net.highwayfrogs.editor.file.map.entity.script.volcano.ScriptMechanismData;
import net.highwayfrogs.editor.file.map.entity.script.volcano.ScriptPlatform2Data;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.Tuple2;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens to entity script data.
 * Created by Kneesnap on 11/27/2018.
 */
public abstract class EntityScriptData extends GameObject {
    private static final Map<String, Tuple2<Class<? extends EntityScriptData>, Constructor<? extends EntityScriptData>>> CACHE_MAP = new HashMap<>();
    private static final List<Class<? extends EntityScriptData>> REGISTERED_SCRIPT_TYPES = Arrays.asList(ScriptBobbingWasteData.class, ScriptPlatform2Data.class,
            ScriptNoiseData.class, ScriptButterflyData.class, ScriptNuclearBarrelData.class, ScriptHelicopterData.class,
            ScriptHawkData.class, ScriptMechanismData.class, ScriptHeliumBalloon.class, ScriptBalloonData.class, ScriptSpecialWaterNoiseData.class,
            ScriptFloatingTreeData.class);


    /**
     * Add script data.
     * @param editor The editor to build on top of.
     */
    public abstract void addData(GUIEditorGrid editor);

    /**
     * Make script data for the given form.
     * @param instance The game instance to make data for.
     * @param entry    The form.
     * @return entityData
     */
    @SneakyThrows
    public static EntityScriptData makeData(FroggerGameInstance instance, FormEntry entry) {
        if (entry == null)
            return null;

        String dataClassName = instance.getConfig().getFormBank().getConfig().getString(entry.getFormName(), null);
        if (dataClassName == null)
            return null;

        if (!CACHE_MAP.containsKey(dataClassName))
            throw new RuntimeException("Failed to find form class for the type: " + entry.getFormName() + ", " + dataClassName);
        return CACHE_MAP.get(dataClassName).getB().newInstance();
    }

    /**
     * Get the script class for the given form.
     * @param instance The game instance to find the script data class for.
     * @param entry    The form to get.
     * @return entityData
     */
    public static Class<? extends EntityScriptData> getScriptDataClass(FroggerGameInstance instance, FormEntry entry) {
        if (entry == null)
            return null;

        String dataClassName = instance.getConfig().getFormBank().getConfig().getString(entry.getFormName(), null);
        if (dataClassName == null)
            return null;

        if (!CACHE_MAP.containsKey(dataClassName))
            throw new RuntimeException("Failed to find form class for the type: " + entry.getFormName() + ", " + dataClassName);
        return CACHE_MAP.get(dataClassName).getA();
    }

    static {
        for (Class<? extends EntityScriptData> dataClass : REGISTERED_SCRIPT_TYPES) {
            try {
                CACHE_MAP.put(dataClass.getSimpleName(), new Tuple2<>(dataClass, dataClass.getConstructor()));
            } catch (NoSuchMethodException e) {
                System.out.println(dataClass.getSimpleName() + " probably does not have a no-args constructor.");
            }
        }
    }
}