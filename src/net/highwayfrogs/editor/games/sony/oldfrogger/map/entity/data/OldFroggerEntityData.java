package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.DefaultMatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.DefaultPathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.suburbia.DogEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.suburbia.LillypadEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.suburbia.SwanEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents arbitrary entity data.
 * Created by Kneesnap on 12/11/2023.
 */
@Getter
public abstract class OldFroggerEntityData<TDifficultyData extends OldFroggerDifficultyData> extends SCGameData<OldFroggerGameInstance> {
    private static final Map<String, OldFroggerEntityDataFactory> ENTITY_DATA_TEMPLATES = new HashMap<>();
    private final OldFroggerMapEntity entity;
    private final OldFroggerDifficultyWrapper<TDifficultyData> difficultyData;

    public OldFroggerEntityData(OldFroggerMapEntity entity, Function<OldFroggerMapEntity, TDifficultyData> difficultyDataMaker) {
        super(entity.getGameInstance());
        this.entity = entity;
        this.difficultyData = difficultyDataMaker != null ? new OldFroggerDifficultyWrapper<>(entity, difficultyDataMaker) : null;
    }

    @Override
    public void load(DataReader reader) {
        this.loadMainEntityData(reader);
        reader.alignRequireEmpty(4);
        if (this.difficultyData != null) {
            if (this.entity != null && this.entity.isDifficultyLevelEnabled(OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL))
                getLogger().warning("Loading entity difficulty data despite the difficulty flag marking this as a static model entity.");

            this.difficultyData.load(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.saveMainEntityData(writer);
        writer.align(4);
        if (this.difficultyData != null) {
            if (this.entity != null && this.entity.isDifficultyLevelEnabled(OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL))
                getLogger().warning("Saving entity difficulty data despite the difficulty flag marking this as a static model entity.");

            this.difficultyData.save(writer);
        }
    }

    @Override
    public ILogger getLogger() {
        return this.entity != null ? this.entity.getLogger() : super.getLogger();
    }

    /**
     * Load the main entity data (non-difficulty data) from the reader.
     * @param reader The reader to read data from.
     */
    protected abstract void loadMainEntityData(DataReader reader);

    /**
     * Save the main entity data (non-difficulty data) to the writer.
     * @param writer The writer to save the data to.
     */
    protected abstract void saveMainEntityData(DataWriter writer);

    @Override
    public OldFroggerConfig getConfig() {
        return (OldFroggerConfig) super.getConfig();
    }

    /**
     * Get the x, y, z position of this entity.
     * @param position A cached position array to write output to.
     * @return position
     */
    public abstract float[] getPosition(float[] position);

    /**
     * Setup the editor for the entity data.
     * @param editor The editor grid to setup the entity data under.
     */
    public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        this.setupMainEntityDataEditor(manager, editor);
        if (this.difficultyData != null && !this.entity.isDifficultyLevelEnabled(OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL))
            this.difficultyData.setupEditor(manager, editor);
    }

    /**
     * Setup the editor for the main entity data (non-difficulty).
     * @param editor The editor grid to setup the entity data under.
     */
    public abstract void setupMainEntityDataEditor(OldFroggerEntityManager manager, GUIEditorGrid editor);

    /**
     * A factory which can create entity data.
     */
    public interface OldFroggerEntityDataFactory {
        /**
         * Get the name of this factory.
         * @return factoryName
         */
        String getName();

        /**
         * Creates entity data for the given entity.
         * @param entity The entity to create data for.
         * @return entityData
         */
        OldFroggerEntityData<?> createEntityData(OldFroggerMapEntity entity);
    }

    @Getter
    private static class FroggerEntityDataReflectionFactory<TEntityData extends OldFroggerEntityData<?>> implements OldFroggerEntityDataFactory {
        private final Class<TEntityData> entityDataClass;
        private final String name;
        private final Constructor<TEntityData> constructor;

        public FroggerEntityDataReflectionFactory(Class<TEntityData> entityDataClass) {
            this.entityDataClass = entityDataClass;
            this.name = this.entityDataClass.getSimpleName();
            try {
                this.constructor = this.entityDataClass.getConstructor(OldFroggerMapEntity.class);
            } catch (NoSuchMethodException nsme) {
                throw new RuntimeException("Couldn't find constructor for '" + this.name + "'.", nsme);
            }
        }

        @Override
        public OldFroggerEntityData<?> createEntityData(OldFroggerMapEntity entity) {
            try {
                return this.constructor.newInstance(entity);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to construct new instance of '" + this.name + "'.", e);
            }
        }
    }

    /**
     * Gets the entity data factory by its name, if it exists.
     * @param factoryName The factory name to lookup.
     * @return The entity data factory to return, if any.
     */
    public static OldFroggerEntityDataFactory getEntityDataFactory(String factoryName) {
        return ENTITY_DATA_TEMPLATES.get(factoryName);
    }

    private static <TEntityData extends OldFroggerEntityData<?>> void registerDataTemplate(Class<TEntityData> entityDataClass) {
        FroggerEntityDataReflectionFactory<TEntityData> newFactory = new FroggerEntityDataReflectionFactory<>(entityDataClass);
        ENTITY_DATA_TEMPLATES.put(newFactory.getName(), newFactory);
    }

    static {
        // General:
        registerDataTemplate(DefaultMatrixEntityData.class);
        registerDataTemplate(DefaultPathEntityData.class);
        registerDataTemplate(TriggerPointEntityData.class);
        registerDataTemplate(BonusTimeEntityData.class);
        registerDataTemplate(BonusScoreEntityData.class);
        registerDataTemplate(BonusLifeEntityData.class);
        registerDataTemplate(WaterEntityData.class);

        // Swamp:
        registerDataTemplate(RatEntityData.class);
        registerDataTemplate(SquirtEntityData.class);
        registerDataTemplate(TurtleEntityData.class);
        registerDataTemplate(CrocodileEntityData.class);
        registerDataTemplate(WasteBarrelEntityData.class);
        registerDataTemplate(StaticWasteBarrelEntityData.class);
        registerDataTemplate(NuclearBarrelEntityData.class);
        registerDataTemplate(OilDrumEntityData.class);
        registerDataTemplate(SinkingBoxEntityData.class);

        // Suburbia
        registerDataTemplate(DogEntityData.class);
        registerDataTemplate(SwanEntityData.class);
        registerDataTemplate(LillypadEntityData.class);

        // Original
        registerDataTemplate(LogSnakeEntityData.class);
        registerDataTemplate(FlyEntityData.class);
        registerDataTemplate(CrocHeadEntityData.class);
        registerDataTemplate(BabyFrogEntityData.class);
        registerDataTemplate(BeaverEntityData.class);

        // Cave:
        registerDataTemplate(RopeBridgeEntityData.class);
        registerDataTemplate(RockBlockEntityData.class);
        registerDataTemplate(HedgehogEntityData.class);
        registerDataTemplate(SpiderEntityData.class);
        registerDataTemplate(RockFallFloorEntityData.class);
        registerDataTemplate(SlimeEntityData.class);
        registerDataTemplate(SnailEntityData.class);
        registerDataTemplate(CobwebEntityData.class);
        registerDataTemplate(LightBugEntityData.class);
        registerDataTemplate(CaveLightEntityData.class);
        registerDataTemplate(RopeBridgeEntityData.class);
        registerDataTemplate(BreakingFloorEntityData.class);

        // Forest:
        registerDataTemplate(HiveEntityData.class);
        registerDataTemplate(FallingLeavesEntityData.class);
        registerDataTemplate(SwayingBranchEntityData.class);
        registerDataTemplate(BreakingBranchEntityData.class);
        registerDataTemplate(SquirrelEntityData.class);
        registerDataTemplate(SwarmEntityData.class);

        // Sky:
        registerDataTemplate(JetEntityData.class);
        registerDataTemplate(HelicopterEntityData.class);
        registerDataTemplate(SmallBirdEntityData.class);
        registerDataTemplate(BalloonEntityData.class);
        registerDataTemplate(CloudEntityData.class);
        registerDataTemplate(HawkEntityData.class);
        registerDataTemplate(TornadoObjectEntityData.class);
        registerDataTemplate(BiplaneBannerEntityData.class);

        // Desert:
        registerDataTemplate(SnakeEntityData.class);
        registerDataTemplate(TumbleweedEntityData.class);
        registerDataTemplate(FallingRockEntityData.class);
        registerDataTemplate(EarthquakeEntityData.class);
        registerDataTemplate(BisonEntityData.class);
        registerDataTemplate(HoleEntityData.class);
    }
}