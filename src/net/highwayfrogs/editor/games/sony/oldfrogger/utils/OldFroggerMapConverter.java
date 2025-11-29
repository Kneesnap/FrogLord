package net.highwayfrogs.editor.games.sony.oldfrogger.utils;

import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapLight;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityData;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert.FroggerEntityDataFallingRock;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest.FroggerEntityDataBreakingBranch;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest.FroggerEntityDataFallingLeaf;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerMapFormSquareReaction;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareReaction;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo.FroggerPathMotionType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegmentSpline;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygonType;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGeneral;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerUtils;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerReactionType;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapForm;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert.FallingRockEntityData.FallingRockDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.BreakingBranchEntityData.BreakingBranchDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.FallingLeavesEntityData.FallingLeavesDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.*;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapLightPacket.OldFroggerMapLight;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapPathPacket.OldFroggerMapPath;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path.OldFroggerPathData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path.OldFroggerPathData.MotionType;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path.OldFroggerSpline;
import net.highwayfrogs.editor.games.sony.shared.misc.MRLightType;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.IntegerCounter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains utilities for converting old Frogger map files.
 * Created by Kneesnap on 6/1/2025.
 */
public class OldFroggerMapConverter {
    /**
     * Converts the old Frogger map file to the new Frogger map format.
     * @param oldMapFile the map file to convert
     */
    public static void convertToNewFormatUI(OldFroggerMapFile oldMapFile) {
        FroggerGameInstance instance = FroggerUtils.getOtherFroggerInstanceOrWarnUser(oldMapFile.getGameInstance());
        if (instance == null)
            return;

        SelectionMenu.promptSelection(instance, "Select the map file to import over.", mapFile -> {
            FroggerMapFile newMapFile = convertToNewFormat(mapFile.getIndexEntry(), oldMapFile);
            mapFile.getArchive().replaceFile(oldMapFile.getFileDisplayName(), mapFile.getIndexEntry(), mapFile, newMapFile, true);
        }, instance.getMainArchive().getAllFiles(FroggerMapFile.class), SCGameFile::getFileDisplayName, FroggerMapFile::getCollectionViewIcon);
    }

    /**
     * Converts a map in the old Frogger map format to a file in the new format.
     * @param mwiEntry the mwiEntry of the map file to create the new Frogger map file as.
     * @param oldMapFile the old map file to convert
     * @return newMapFile
     */
    public static FroggerMapFile convertToNewFormat(MWIResourceEntry mwiEntry, OldFroggerMapFile oldMapFile) {
        if (mwiEntry == null)
            throw new NullPointerException("mwiEntry");
        if (!(mwiEntry.getGameInstance() instanceof FroggerGameInstance))
            throw new ClassCastException("The provided mwiEntry was expected to be for " + FroggerGameInstance.class.getSimpleName() + ", but was actually for " + Utils.getSimpleName(mwiEntry.getGameInstance()) + ".");
        if (oldMapFile == null)
            throw new NullPointerException("oldMapFile");

        OldFroggerMapLevelSpecificPacket levelSpecificPacket = oldMapFile.getLevelSpecificDataPacket();
        OldFroggerMapPathPacket pathPacket = oldMapFile.getPathPacket();
        OldFroggerMapEntityMarkerPacket entityMarkerPacket = oldMapFile.getEntityMarkerPacket();
        OldFroggerMapLightPacket lightPacket = oldMapFile.getLightPacket();
        OldFroggerMapGridHeaderPacket gridPacket = oldMapFile.getGridPacket();
        OldFroggerMapVertexPacket vertexPacket = oldMapFile.getVertexPacket();
        // QUAD and graphical packets don't contain much of interest to convert.
        // Camera height-field packet doesn't translate very well to new Frogger. Luckily, this feature isn't really used beyond what appear to be tests, so we can ignore it.
        ILogger logger = oldMapFile.getLogger();

        int uvAnimationCount = oldMapFile.getAnimPacket().getUvAnimations().size();
        if (uvAnimationCount > 0)
            logger.warning("%d UV animation(s) will be skipped during conversion.", uvAnimationCount);

        FroggerGameInstance instance = (FroggerGameInstance) mwiEntry.getGameInstance();
        FroggerMapFile newMapFile = new FroggerMapFile(instance, mwiEntry);
        FroggerMapFilePacketGeneral newGeneralPacket = newMapFile.getGeneralPacket();
        newGeneralPacket.setStartingTimeLimit(Math.max(25, getHighestStartingTimerValue(levelSpecificPacket)));
        newGeneralPacket.getDefaultCameraSourceOffset().setValues(oldMapFile.getStandardPacket().getCameraOffset());
        newGeneralPacket.setMapTheme(mwiEntry.getGameFile() instanceof FroggerMapFile ? ((FroggerMapFile) mwiEntry.getGameFile()).getMapTheme() : FroggerMapTheme.getTheme(oldMapFile.getFileDisplayName()));

        // Calculate the world position offset to allow alignment with the post-recode Frogger grid alignment.
        FroggerMapFilePacketGrid newGridPacket = newMapFile.getGridPacket();
        newGridPacket.resizeGrid(Math.min(FroggerMapFilePacketGrid.MAX_GRID_SQUARE_COUNT_X, (gridPacket.getXSize() / FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH) * gridPacket.getXCount()),
                Math.min(FroggerMapFilePacketGrid.MAX_SAFE_GRID_SQUARE_COUNT_Z, (gridPacket.getZSize() / FroggerMapFilePacketGrid.GRID_STACK_WORLD_LENGTH) * gridPacket.getZCount()));
        SVector basePoint = gridPacket.getBasePoint();
        SVector worldOffset = new SVector(newGridPacket.getBaseGridX() - basePoint.getX(),
                -basePoint.getY(),
                newGridPacket.getBaseGridZ() - basePoint.getZ());

        // Create new vertices, but offset to align to the post-recode Frogger grid alignment.
        for (int i = 0; i < vertexPacket.getVertices().size(); i++)
            newMapFile.getVertexPacket().getVertices().add(vertexPacket.getVertices().get(i).clone().add(worldOffset));

        // Copy polygons.
        int oldGridXCount = gridPacket.getXCount();
        int oldGridZCount = gridPacket.getZCount();
        int oldGridTotal = oldGridXCount * oldGridZCount;
        if (oldGridTotal != gridPacket.getGrids().size())
            throw new RuntimeException("Grid dimensions were [" + oldGridXCount + "x" + oldGridZCount + " -> " + oldGridTotal + "] is not the expected " + gridPacket.getGrids().size() + "!");

        Vector3f tempVector = new Vector3f();
        int defaultGridFlags = convertReactionToGridSquareFlags(logger, levelSpecificPacket.getDefaultReactionType(), false);
        for (int z = 0; z < gridPacket.getZCount(); z++) {
            for (int x = 0; x < gridPacket.getXCount(); x++) {
                OldFroggerMapGrid oldGrid = gridPacket.getGrids().get((z * gridPacket.getXCount()) + x);
                List<OldFroggerMapPolygon> polygons = oldGrid.getPolygons();
                for (int i = 0; i < polygons.size(); i++) {
                    OldFroggerMapPolygon polygon = polygons.get(i);
                    FroggerMapPolygon newPolygon = convertToNewPolygonType(oldMapFile, polygon, newMapFile);
                    newMapFile.getPolygonPacket().addPolygon(newPolygon);

                    try {
                        FroggerGridSquare newGridSquare = newMapFile.getGridPacket().getOrAddGridSquare(newPolygon, tempVector);
                        newGridSquare.setFlags(defaultGridFlags);
                    } catch (Throwable th) {
                        // Some maps such as SKY.MAP don't really have a real solution to this problem.
                        // So we'll leave it alone and leave it to the user to deal with however they like.
                    }
                }
            }
        }

        // Setup start position.
        IVector oldFroggerStartPos = levelSpecificPacket.getFroggerStartPosition().clone().add(worldOffset);
        newGeneralPacket.setStartGridCoordX(newGridPacket.getGridXFromWorldX(oldFroggerStartPos.getX()));
        newGeneralPacket.setStartGridCoordZ(newGridPacket.getGridZFromWorldZ(oldFroggerStartPos.getZ()));

        // Copy paths.
        for (int i = 0; i < pathPacket.getPaths().size(); i++) {
            OldFroggerMapPath oldPath = pathPacket.getPaths().get(i);
            FroggerPath newPath = convertToNewPath(oldPath, newMapFile, worldOffset);
            newMapFile.getPathPacket().getPaths().add(newPath);
        }

        // Convert entities.
        Config formCfg = SCGameType.OLD_FROGGER.loadConfigFromEmbeddedResourcePath("form-conversion.cfg", false);
        for (int i = 0; i < entityMarkerPacket.getEntities().size(); i++) {
            OldFroggerMapEntity entity = entityMarkerPacket.getEntities().get(i);
            FroggerMapEntity newEntity = convertToNewEntity(entity, newMapFile, worldOffset, formCfg);
            if (newEntity != null)
                newMapFile.getEntityPacket().addEntity(newEntity);
        }

        // Convert lights.
        for (int i = 0; i < lightPacket.getLights().size(); i++) {
            OldFroggerMapLight oldLight = lightPacket.getLights().get(i);
            newMapFile.getLightPacket().addLight(convertToNewMapLight(oldLight, newMapFile));
        }

        newGridPacket.recalculateAllCliffHeights();
        newMapFile.getGroupPacket().generateMapGroups(oldMapFile.getLogger(), ProblemResponse.CREATE_POPUP, true);
        return newMapFile;
    }

    private static FroggerMapPolygon convertToNewPolygonType(OldFroggerMapFile oldMapFile, OldFroggerMapPolygon oldPolygon, FroggerMapFile newMapFile) {
        FroggerMapPolygon newPolygon = new FroggerMapPolygon(newMapFile, FroggerMapPolygonType.getByInternalType(oldPolygon.getPolygonType()));
        if (oldPolygon.getVertices().length != newPolygon.getVertexCount())
            throw new RuntimeException("Polygon vertex mismatch for " + oldPolygon.getPolygonType() + " [" + oldPolygon.getVertices().length + " vs " + newPolygon.getVertexCount() + "]");
        if (oldPolygon.getColors().length != newPolygon.getColors().length)
            throw new RuntimeException("Polygon colors mismatch for " + oldPolygon.getPolygonType() + " [" + oldPolygon.getColors().length + " vs " + newPolygon.getColors().length + "]");
        if (oldPolygon.getTextureUvs().length != newPolygon.getTextureUvs().length)
            throw new RuntimeException("Polygon textureUv mismatch for " + oldPolygon.getPolygonType() + " [" + oldPolygon.getTextureUvs().length + " vs " + newPolygon.getTextureUvs().length + "]");

        newPolygon.setVisible(true);
        newPolygon.setTextureId((short) oldPolygon.getTextureId());
        System.arraycopy(oldPolygon.getVertices(), 0, newPolygon.getVertices(), 0, newPolygon.getVertexCount());
        OldFroggerMapPolygon.copyColors(oldMapFile, oldPolygon.getColors(), newPolygon.getColors());
        for (int i = 0; i < newPolygon.getTextureUvs().length; i++)
            newPolygon.getTextureUvs()[i] = oldPolygon.getTextureUvs()[i].clone();

        return newPolygon;
    }

    private static int getHighestStartingTimerValue(OldFroggerMapLevelSpecificPacket packet) {
        // Copy starting timer value.
        int highestCount = -1;
        Map<Integer, IntegerCounter> timerCountMap = new HashMap<>();
        int bestTimer = -1;
        for (int i = 0; i < packet.getLevelTimers().length; i++) {
            int timerValue = packet.getLevelTimers()[i];
            if (timerValue <= 1)
                continue;

            IntegerCounter counter = timerCountMap.computeIfAbsent(timerValue, key -> new IntegerCounter());
            if (counter.increment() > highestCount) {
                bestTimer = timerValue;
                highestCount = counter.getCounter();
            }
        }

        return bestTimer;
    }

    private static int convertReactionToGridSquareFlags(ILogger logger, OldFroggerReactionType reactionType, boolean isForm) {
        switch (reactionType) {
            case Nothing:
                return isForm ? FroggerMapFormSquareReaction.NONE.getGridSquareFlagBitMask() : FroggerMapFormSquareReaction.SAFE.getGridSquareFlagBitMask();
            case Sand: // Seen in SWAMP.MAP, doesn't seem to do anything. Might be overridden by 'Nothing' though.
            case Slide: // Seen in SWAMP.MAP, doesn't seem to do anything. Might be overridden by 'Nothing' though.
            case EntityTrigger: // Seen in SKY.MAP, this will activate a particular entity.
            case EntityPause: // Seen in SKY.MAP, this will deactivate a particular entity.
            case EntityUnpause: // Seen in SKY.MAP, this will reactivate a particular entity.
            case Bonus: // This is seen on the FLY in ORIGINAL.MAP.
            case HitLand: // Seen in ORIGINAL.MAP for ORG_STAT_MIDDLE_BANK. Not entirely sure what this does.
            case Safe:
                return FroggerGridSquareReaction.SAFE.getGridSquareFlagBitMask();
            case FrogHitFrog: // Never seen.
            case Sticky: // Never seen.
            case Org_hittrigger: // Never seen
            case BounceAndDie: // Never seen.
            case Sinking: // Seen in SUBURBIA.MAP, functionality unknown.
            case Eject: // Never seen.
            case FreezeAndDie: // Seen in VOLCANO.MAP, functionality unknown.
                if (logger != null)
                    logger.warning("Don't know how to handle the reaction type: %s.", reactionType);
                return FroggerGridSquareReaction.SAFE.getGridSquareFlagBitMask();
            case DisappearAndDie: // Seen as SUB_DOG in MULTIPLAYER1.MAP. Seems to make the frog disappear and die.
            case Die:
                return FroggerGridSquareReaction.DEADLY.getGridSquareFlagBitMask();
            case Jump:
                return FroggerGridSquareReaction.BOUNCY.getGridSquareFlagBitMask();
            case FroggerSwim: // There is no swimming mechanic in Frogger, we'll just treat it as drowning.
            case Water:
                return FroggerGridSquareReaction.WATER_DEATH.getGridSquareFlagBitMask();
            case HitTrigger:
                return isForm ? FroggerMapFormSquareReaction.CHECKPOINT.getGridSquareFlagBitMask() : FroggerGridSquareReaction.SAFE.getGridSquareFlagBitMask();
            case Stop: // Seen on the bee hives & trees in FOREST.MAP. It might be what allows climbing an entity? I think the closest matching behavior is preventing movement.
                return FroggerGridSquareReaction.NONE.getGridSquareFlagBitMask();
            case FallingDeath: // Causes the player to fall through the map. Closest match is free-fall.
            case SkyMapZone: // This seems like it enables free-fall, but I haven't confirmed that for sure.
                return FroggerGridSquareReaction.FREE_FALL.getGridSquareFlagBitMask();
            default:
                throw new UnsupportedOperationException("Unsupported reaction type: " + reactionType);
        }
    }

    private static FroggerMapLight convertToNewMapLight(OldFroggerMapLight oldLight, FroggerMapFile newMapFile) {
        FroggerMapLight newLight = new FroggerMapLight(newMapFile);
        newLight.setParentId(oldLight.getParentId());
        newLight.setLightType(oldLight.getApiType());
        newLight.getPosition().setValues(oldLight.getPosition());
        newLight.getDirection().setValues(oldLight.getDirection());
        newLight.setColor(oldLight.getColor().toRGB());
        newLight.setAttribute0(oldLight.getAttrib1());
        newLight.setAttribute1(oldLight.getAttrib2());
        if (newMapFile.getMapTheme() == FroggerMapTheme.CAVE && newLight.getLightType() == MRLightType.AMBIENT)
            newLight.setColor(0xFFFFFFFF); // Max light.

        return newLight;
    }

    private static FroggerPath convertToNewPath(OldFroggerMapPath path, FroggerMapFile newMapFile, SVector worldOffset) {
        FroggerPath newPath = new FroggerPath(newMapFile);

        for (int i = 0; i < path.getSplines().size(); i++) {
            OldFroggerSpline oldSpline = path.getSplines().get(i);
            FroggerPathSegmentSpline newSegment = new FroggerPathSegmentSpline(newPath);
            newPath.getSegments().add(newSegment);
            newSegment.copyFrom(oldSpline, worldOffset);
        }

        return newPath;
    }

    private static IFroggerFormEntry getNewFormEntry(FroggerMapFile newMap, OldFroggerMapEntity oldEntity, Config formConversionCfg) {
        OldFroggerMapForm oldForm = oldEntity.getForm();
        if (oldForm == null) {
            oldEntity.getLogger().warning("Skipping entity due to missing form.");
            return null;
        }

        String[] testNames;
        ConfigValueNode node = formConversionCfg.getOptionalKeyValueNode(oldForm.getName());
        if (node != null) {
            testNames = node.getAsString().split(",\\s*");
        } else {
            testNames = new String[] {oldForm.getName()};
        }

        // Resolve names.
        FroggerGameInstance instance = newMap.getGameInstance();
        for (int i = 0; i < testNames.length; i++) {
            String testName = testNames[i];
            int formId = instance.getVersionConfig().getFormBank().getIndexForName(testName, true);
            if (formId < 0)
                continue;

            IFroggerFormEntry testEntry = instance.getMapFormEntry(newMap.getMapTheme(), formId);
            if (testEntry != null && testName.equalsIgnoreCase(testEntry.getFormTypeName()))
                return testEntry;

            testEntry = instance.getMapFormEntry(FroggerMapTheme.GENERAL, formId);
            if (testEntry != null && testName.equalsIgnoreCase(testEntry.getFormTypeName()))
                return testEntry;
        }

        if (testNames.length > 0)
            oldEntity.getLogger().warning("Could not resolve the new form. Names: %s", oldForm.getName(), Arrays.toString(testNames));
        return null;
    }

    private static FroggerMapEntity convertToNewEntity(OldFroggerMapEntity oldEntity, FroggerMapFile newMap, SVector worldOffset, Config formConfig) {
        IFroggerFormEntry newFormEntry = getNewFormEntry(newMap, oldEntity, formConfig);
        if (newFormEntry == null)
            return null;

        FroggerMapEntity newEntity = new FroggerMapEntity(newMap, newFormEntry);
        copyEntityData(oldEntity, newEntity, worldOffset);
        return newEntity;
    }

    private static void copyEntityData(OldFroggerMapEntity oldEntity, FroggerMapEntity newEntity, SVector worldOffset) {
        OldFroggerEntityData<?> oldEntityData = oldEntity.getEntityData();
        FroggerEntityData newEntityData = newEntity.getEntityData();

        // This data is common for basically all entity types.
        if (newEntityData instanceof FroggerEntityDataPathInfo) {
            FroggerEntityDataPathInfo newPathInfo = (FroggerEntityDataPathInfo) newEntityData;
            if (oldEntityData instanceof PathEntityData<?>) {
                OldFroggerPathData oldPathData = ((PathEntityData<?>) oldEntityData).getPathData();
                newPathInfo.getPathInfo().setPathId(oldPathData.getPathId());
                newPathInfo.getPathInfo().setSegmentId(oldPathData.getSplineId());
                newPathInfo.getPathInfo().setSegmentDistance(oldPathData.getSplinePosition());
                newPathInfo.getPathInfo().setFlag(FroggerPathMotionType.ONE_SHOT, oldPathData.getMotionType() == MotionType.Die);
                newPathInfo.getPathInfo().setFlag(FroggerPathMotionType.BACKWARDS, oldPathData.getMotionType() == MotionType.Reverse || oldPathData.getMotionType() == MotionType.Reverse_Back);
                newPathInfo.getPathInfo().setFlag(FroggerPathMotionType.REPEAT, oldPathData.getMotionType() == MotionType.Restart);
            } else {
                oldEntity.getLogger().warning("Cannot convert old data (%s) to new data type (%s)!", Utils.getSimpleName(oldEntityData), Utils.getSimpleName(newEntityData));
            }
        } else if (newEntityData instanceof FroggerEntityDataMatrix) {
            FroggerEntityDataMatrix newMatrixData = (FroggerEntityDataMatrix) newEntityData;
            PSXMatrix newMatrix = newMatrixData.getMatrix();
            if (oldEntityData instanceof MatrixEntityData<?>) {
                newMatrix.copyFrom(((MatrixEntityData<?>) oldEntityData).getMatrix());
            } else if (oldEntityData instanceof PathEntityData<?>) {
                newMatrix.copyFrom(((PathEntityData<?>) oldEntityData).calculatePosition());
            } else {
                oldEntity.getLogger().warning("Cannot convert old data (%s) to new data type (%s)!", Utils.getSimpleName(oldEntityData), Utils.getSimpleName(newEntityData));
            }

            // Apply the world offset to the picked position.
            newMatrix.getTransform()[0] += worldOffset.getX();
            newMatrix.getTransform()[1] += worldOffset.getY();
            newMatrix.getTransform()[2] += worldOffset.getZ();
        }

        // Down here we'll copy data for specifically supported entity types.
        if (newEntityData instanceof FroggerEntityDataBreakingBranch) {
            BreakingBranchDifficultyData oldData = getDifficultyData(oldEntityData);
            ((FroggerEntityDataBreakingBranch) newEntityData).setFallSpeed(oldData.getFallingSpeed());
            ((FroggerEntityDataBreakingBranch) newEntityData).setBreakDelay(oldData.getBreakingDelay());
        } else if (newEntityData instanceof FroggerEntityDataFallingLeaf) {
            FallingLeavesDifficultyData oldData = getDifficultyData(oldEntityData);
            ((FroggerEntityDataFallingLeaf) newEntityData).setFallSpeed(oldData.getSpeed());
            ((FroggerEntityDataFallingLeaf) newEntityData).setSwayAngle(oldData.getSwayAngle());
            ((FroggerEntityDataFallingLeaf) newEntityData).setSwayDuration(oldData.getSwayDuration());
        } else if (newEntityData instanceof FroggerEntityDataFallingRock) {
            FallingRockDifficultyData oldData = getDifficultyData(oldEntityData);
            FroggerEntityDataFallingRock newData = (FroggerEntityDataFallingRock) newEntityData;
            newData.setDelay(oldData.getMoveDelay());
            newData.setBounceCount(oldData.getBounceCount());
            newData.getTargets()[0].getTarget().setValues(oldData.getPositionOne().clone().add(worldOffset));
            newData.getTargets()[0].setTime(oldData.getTimeToPositionOne());
            newData.getTargets()[1].getTarget().setValues(oldData.getPositionTwo().clone().add(worldOffset));
            newData.getTargets()[1].setTime(oldData.getTimeToPositionTwo());
            newData.getTargets()[2].getTarget().setValues(oldData.getPositionThree().clone().add(worldOffset));
            newData.getTargets()[2].setTime(oldData.getTimeToPositionThree());
        }
    }

    @SuppressWarnings("unchecked")
    private static <TDifficultyData extends OldFroggerDifficultyData> TDifficultyData getDifficultyData(OldFroggerEntityData<?> oldEntityData) {
        OldFroggerDifficultyWrapper<TDifficultyData> wrapper = ((OldFroggerEntityData<TDifficultyData>) oldEntityData).getDifficultyData();
        for (int i = 0; i < OldFroggerGameInstance.DIFFICULTY_LEVELS; i++) {
            TDifficultyData difficultyData = wrapper.getDifficultyData(i);
            if (difficultyData != null)
                return difficultyData;
        }

        throw new IllegalArgumentException("There is no difficulty data present for " + Utils.getSimpleName(oldEntityData) + ".");
    }
}