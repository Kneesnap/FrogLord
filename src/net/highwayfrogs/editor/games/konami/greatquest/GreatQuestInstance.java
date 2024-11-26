package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.LargeFileReceiver;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcAnimState;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyBezier.kcTrackKeyBezierPosition;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyDummy;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyTcb.kcTrackKeyTcbPosition;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyTcb.kcTrackKeyTcbRotation;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyVector.kcTrackKeyLinearPosition;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyVector.kcTrackKeyLinearRotation;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyVector.kcTrackKeyLinearScale;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.*;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SoundChunkFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SoundChunkFile.SoundChunkEntry;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.*;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr.kcVtxBufFileStruct;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceNamedHash.HashTableEntry;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh.kcCFace;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh.kcCTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.entity.*;
import net.highwayfrogs.editor.games.konami.greatquest.file.*;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourcePath;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceString;
import net.highwayfrogs.editor.games.konami.greatquest.map.*;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctBranch;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctLeaf;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctTree;
import net.highwayfrogs.editor.games.konami.greatquest.math.*;
import net.highwayfrogs.editor.games.konami.greatquest.model.*;
import net.highwayfrogs.editor.games.konami.greatquest.noodle.GreatQuestInstanceNoodleTemplate;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcEmitterDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyCapsuleDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyTriMeshDesc;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.*;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestMainMenuUIController;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents an instance of 'Frogger: The Great Quest'.
 * TODO Immediate:
 *  -> Fix transparency for the bone icon.
 *  -> Add PS2 PAL TOC support.
 *  -> Scripting Engine
 *   -> Strings should be an object with a template.
 *   -> Enums should be an object with a template too.
 *   -> Support Arrays
 *   -> Consider rigid primitives, upsides & downsides.
 *   -> Register public static fields as well.
 *   -> Fix interfaces.
 *   -> throw "" keyword.
 *   -> I believe config is not properly handling escapes, either that or NoodleCompiler isn't properly handling escapes in strings.
 *   -> Are for loops actually evaluating the condition each time?
 *  -> Import/Export Models/Animations/Animation Sets/Skeletons/Collision Meshes/OctTree building
 *  -> GQS:
 *   -> Documentation of the non-script GQS portion.
 *   -> Action Sequence Support
 *   -> Collision Proxy Support
 *   -> I think it'd be a good idea to consider if we want to allow partial-configs, for just applying changes to existing things. Eg: CharacterParams has a lot of stuff.
 *   -> Better script warnings. [Go over all actions]
 *   ->
 *  -> Transparent stuff has been broken again.
 *  -> Go over TODOs in the tutorial gqs script example.
 *
 * TODO Future:
 *  -> Flesh out the PropertyList behavior. (Nesting!)
 *  -> Further support previewing & editing generic data.
 *  -> Preview texture references in chunk file viewer.
 *  -> Improve how the scripting UI feels to use. (Eg: the UI shouldn't be completely blocked)
 *  -> Config
 *   -> How do we handle comments in the key-value-pair section? (Multi-line)
 *   -> Phase out the old Config class.
 * Created by Kneesnap on 4/13/2024.
 */
@Getter
public class GreatQuestInstance extends GameInstance {
    private final List<GreatQuestGameFile> allFiles = new ArrayList<>();
    private final List<GreatQuestGameFile> looseFiles = new ArrayList<>();
    private GreatQuestAssetBinFile mainArchive;
    private SoundChunkFile soundChunkFile;
    private File mainArchiveBinFile;
    private int nextFreeSoundId;
    private final Map<Integer, String> soundPathsById = new HashMap<>();
    private final Map<String, Integer> soundIdsByPath = new HashMap<>();

    public static final float JUMP_SLOPE_THRESHOLD = .8F;

    // Padding data.
    public static final byte PADDING_BYTE_DEFAULT = (byte) 0xCC;
    public static final byte PADDING_BYTE_CD = (byte) 0xCD;
    private static final byte[] PADDING_DEFAULT_INT_BYTES = {PADDING_BYTE_DEFAULT, PADDING_BYTE_DEFAULT, PADDING_BYTE_DEFAULT, PADDING_BYTE_DEFAULT};
    public static final int PADDING_DEFAULT_INT = DataUtils.readIntFromBytes(PADDING_DEFAULT_INT_BYTES, 0);
    private static final byte[] PADDING_CD_INT_BYTES = {PADDING_BYTE_CD, PADDING_BYTE_CD, PADDING_BYTE_CD, PADDING_BYTE_CD};
    public static final int PADDING_CD_INT = DataUtils.readIntFromBytes(PADDING_CD_INT_BYTES, 0);

    public GreatQuestInstance() {
        super(GreatQuestGameType.INSTANCE);
    }

    /**
     * Load and setup all instance data relating to the game such as version configuration and game files.
     * @param gameVersionConfigName The name of the version configuration file to load.
     * @param instanceConfig the configuration stored for the user on a per-game-version basis.
     * @param binFile the main archive file to read
     * @param progressBar the progress bar to display load progress on, if it exists
     */
    public void loadGame(String gameVersionConfigName, Config instanceConfig, File binFile, ProgressBarComponent progressBar) {
        if (this.mainArchive != null)
            throw new RuntimeException("The game instance has already been loaded.");

        if (binFile == null || !binFile.exists())
            throw new RuntimeException("The main archive file '" + binFile + "' does not exist.");

        this.mainArchiveBinFile = binFile;
        loadGameConfig(gameVersionConfigName, instanceConfig);
        loadSoundFilePaths();

        // Load the sound files.
        loadSoundFolder();

        // Load the main file.
        try {
            DataReader reader = new DataReader(new FileSource(binFile));
            this.mainArchive = new GreatQuestAssetBinFile(this);
            this.mainArchive.load(reader, progressBar);
            this.allFiles.addAll(this.mainArchive.getFiles());
        } catch (IOException ex) {
            Utils.handleError(getLogger(), ex, true, "Failed to load the bin file.");
        }
    }

    private void loadSoundFilePaths() {
        Config config = Config.loadTextConfigFromInputStream(getGameType().getEmbeddedResourceStream("sound-list.cfg"), "sound-list");

        this.soundIdsByPath.clear();
        this.soundPathsById.clear();
        for (Entry<String, ConfigValueNode> keyValuePair : config.getKeyValuePairs().entrySet()) {
            if (!NumberUtils.isInteger(keyValuePair.getKey())) {
                getLogger().warning("sound-list key '" + keyValuePair.getKey() + "' is not an integer, skipping!");
                continue;
            }

            int soundId = Integer.parseInt(keyValuePair.getKey());
            String soundPath = keyValuePair.getValue().getAsString();
            if (soundPath == null || soundPath.trim().isEmpty()) {
                getLogger().warning("sound-list key '" + keyValuePair.getKey() + "' has not associated value, skipping!");
                continue;
            }

            this.soundPathsById.put(soundId, soundPath);
            Integer oldSoundId = this.soundIdsByPath.put(soundPath, soundId);

            // Warn if there are any duplicate file paths.
            if (oldSoundId != null)
                getLogger().warning("Both SFX ID " + oldSoundId + " & " + soundId + " share the path '" + soundPath + "'! This will cause issues trying to export all sounds at once!");
        }
    }

    private void loadSoundFolder() {
        this.nextFreeSoundId = 0;
        this.soundChunkFile = null;
        File soundFolder = new File(getMainGameFolder(), "SOUND");
        if (!soundFolder.exists() || !soundFolder.isDirectory())
            return;

        File idxFile = null, sckFile = null;
        for (File sndFile : soundFolder.listFiles()) {
            String soundFileName = sndFile.getName().toLowerCase();

            GreatQuestGameFile gameFile;
            if (soundFileName.endsWith(".sbr")) {
                gameFile = new SBRFile(this, sndFile);
            } else if (soundFileName.endsWith(".idx")) {
                if (idxFile != null) {
                    getLogger().warning("There was more than one index file! '" + idxFile + "', '" + sndFile.getName() + "'");
                    continue;
                }

                if (sckFile != null) {
                    gameFile = this.soundChunkFile = new SoundChunkFile(this, sndFile, sckFile);
                } else {
                    idxFile = sndFile;
                    continue;
                }
            } else if (soundFileName.endsWith("sck")) {
                if (sckFile != null) {
                    getLogger().warning("There was more than sound chunk file! '" + sckFile + "', '" + sndFile.getName() + "'");
                    continue;
                }

                if (idxFile != null) {
                    gameFile = this.soundChunkFile = new SoundChunkFile(this, idxFile, sndFile);
                } else {
                    sckFile = sndFile;
                    continue;
                }
            } else { // Unrecognized file.
                if (sndFile.isFile())
                    getLogger().warning("Skipping unrecognized file in the sound folder: '" + sndFile.getName() + "'");
                continue;
            }

            try {
                DataReader reader = new DataReader(new FileSource(sndFile));
                this.looseFiles.add(gameFile);
                this.allFiles.add(gameFile);
                gameFile.load(reader);
            } catch (IOException ex) {
                Utils.handleError(getLogger(), ex, true, "Failed to load the file in the sound folder '%s'.", sndFile.getName());
            }
        }
    }

    private File transformLocalFile(File outputBinFile, File relativeFile) {
        String path = FileUtils.toLocalPath(getMainGameFolder(), relativeFile, false);
        return new File(outputBinFile.getParentFile(), path);
    }

    /**
     * Saves the game.
     * @param outputBinFile The file to save the .bin to
     * @param progressBar the progress bar to display progress for
     */
    public void saveGame(File outputBinFile, ProgressBarComponent progressBar) {
        // Save SBRs.
        if (progressBar != null)
            progressBar.setTotalProgress(this.looseFiles.size());

        for (GreatQuestGameFile file : this.looseFiles) {
            if (file instanceof SBRFile) {
                if (progressBar != null)
                    progressBar.setStatusMessage("Saving '" + file.getFileName() + "'");
                file.saveToFile(transformLocalFile(outputBinFile, ((SBRFile) file).getFile()));
            }

            if (progressBar != null)
                progressBar.addCompletedProgress(1);
        }

        // Save SCK/IDK.
        if (this.soundChunkFile != null)
            this.soundChunkFile.saveFileContentsToNewFolder(getMainGameFolder(), outputBinFile.getParentFile(), progressBar);

        DataWriter writer = new DataWriter(new LargeFileReceiver(outputBinFile));

        try {
            getMainArchive().save(writer, progressBar);
        } catch (Throwable th) {
            // Bubble the error upwards.
            throw new RuntimeException("Failed to save game data.", th);
        } finally {
            writer.closeReceiver();
        }
    }

    @Override
    public GreatQuestMainMenuUIController getMainMenuController() {
        return (GreatQuestMainMenuUIController) super.getMainMenuController();
    }

    @Override
    protected GreatQuestMainMenuUIController makeMainMenuController() {
        return new GreatQuestMainMenuUIController(this);
    }

    @Override
    protected void setupScriptEngine(NoodleScriptEngine engine) {
        super.setupScriptEngine(engine);
        engine.addTemplate(GreatQuestInstanceNoodleTemplate.INSTANCE);
        engine.addWrapperTemplates(kcTrack.class, kcAnimState.class, kcTrackKeyDummy.class,
                kcTrackKeyBezierPosition.class, kcTrackKeyTcbPosition.class,
                kcTrackKeyTcbRotation.class, kcTrackKeyLinearRotation.class, kcTrackKeyLinearPosition.class,
                kcTrackKeyLinearScale.class);
        engine.addWrapperTemplates(SBRFile.class, SfxWavePC.class, SfxWavePS2.class, SfxEntry.class,
                SfxEntryStreamAttributes.class,SfxEntrySimpleAttributes.class, SoundChunkFile.class, SoundChunkEntry.class);
        engine.addWrapperTemplates(kcCResource.class, GreatQuestChunkTextureReference.class, GreatQuestDummyFileChunk.class,
                kcCResOctTreeSceneMgr.class, kcCResourceAnimSet.class, kcCResourceEntityInst.class,
                kcCResourceModel.class, kcCResourceNamedHash.class, kcCResourceSkeleton.class,
                kcCResourceTOC.class, kcCResourceTrack.class, kcCResourceTriMesh.class,
                HashTableEntry.class, kcNode.class);
        engine.addWrapperTemplates(CCoinDesc.class, CGemDesc.class, CharacterParams.class, CHoneyPotDesc.class,
                CItemDesc.class, CMagicStoneDesc.class, CObjKeyDesc.class, CPropDesc.class, CUniqueItemDesc.class,
                kcActorBaseDesc.class, kcActorDesc.class, kcAnimSetDesc.class, kcEntity3DDesc.class,
                kcEntity3DInst.class, kcEntityInst.class, kcHealthDesc.class, kcParticleEmitterParam.class,
                kcParticleParam.class, kcProjectileParams.class, kcWaypointDesc.class, LauncherParams.class);
        engine.addWrapperTemplates(GreatQuestArchiveFile.class, GreatQuestAssetBinFile.class,
                GreatQuestChunkedFile.class, GreatQuestDummyArchiveFile.class, GreatQuestImageFile.class,
                GreatQuestLooseGameFile.class);
        engine.addWrapperTemplates(kcCResourceGeneric.class, kcCResourcePath.class, kcCResourceString.class, kcOctTree.class,
                kcOctBranch.class, kcOctLeaf.class, kcVtxBufFileStruct.class, kcCTriMesh.class);
        engine.addWrapperTemplates(kcColor3.class, kcColor4.class, kcMatrix.class, kcQuat.class,
                kcBox4.class, kcSphere.class, kcVector3.class, kcVector4.class, kcCFace.class);
        engine.addWrapperTemplates(kcEnvironment.class, kcFogParams.class, kcLight.class, kcPerspective.class);
        engine.addWrapperTemplates(kcMaterial.class, kcModel.class, kcModelNode.class, kcModelPrim.class,
                kcModelWrapper.class, kcVertex.class);
        engine.addWrapperTemplates(kcEmitterDesc.class, kcProxyCapsuleDesc.class, kcProxyDesc.class, kcProxyTriMeshDesc.class);
        engine.addWrapperTemplates(kcScriptList.class, kcScript.class, kcScriptDisplaySettings.class,
                kcArgument.class, kcCActionSequence.class, kcParam.class, kcInterimScriptEffect.class, kcParamReader.class,
                kcParamWriter.class, kcScriptListInterim.class, kcScriptTOC.class);
        engine.addWrapperTemplates(GreatQuestHash.class, GreatQuestUtils.class, GreatQuestConfig.class);
    }

    @Override
    public GreatQuestConfig getVersionConfig() {
        return (GreatQuestConfig) super.getVersionConfig();
    }

    @Override
    public File getMainGameFolder() {
        if (this.mainArchiveBinFile != null) {
            return this.mainArchiveBinFile.getParentFile();
        } else {
            throw new IllegalStateException("The folder is not known since the game has not been loaded yet.");
        }
    }

    /**
     * Gets the full sound file path for the given sound ID.
     * @param soundId the sound ID to resolve.
     * @return fullSoundPath, or the ID as a string if there is none.
     */
    public String getFullSoundPath(int soundId) {
        String soundPath = this.soundPathsById.get(soundId);
        if (soundPath != null)
            return soundPath;

        return String.valueOf(soundId);
    }

    /**
     * Gets the shorted sound file path for the given sound ID.
     * @param soundId the sound ID to resolve.
     * @param includeId if true, the sound id will be guaranteed to be included as part of the name
     * @return shortenedSoundPath
     */
    public String getShortenedSoundPath(int soundId, boolean includeId) {
        String soundPath = this.soundPathsById.get(soundId);
        if (soundPath == null)
            return NumberUtils.padNumberString(soundId, 4);

        int lastSlashFound = soundPath.lastIndexOf('/');
        if (lastSlashFound >= 0) {
            int secondToLastSlashFound = soundPath.lastIndexOf('/', lastSlashFound - 1);
            if (secondToLastSlashFound >= 0)
                soundPath = soundPath.substring(secondToLastSlashFound + 1);
        }

        return (includeId ? "[" + NumberUtils.padNumberString(soundId, 4) + "] " : "") + soundPath;
    }

    /**
     * Gets the sound file name for the given sound ID.
     * @param soundId the sound ID to resolve.
     * @param includeId if true, the sound id will be guaranteed to be included as part of the name
     * @return soundFileName
     */
    public String getSoundFileName(int soundId, boolean includeId) {
        String soundPath = this.soundPathsById.get(soundId);
        if (soundPath == null)
            return NumberUtils.padNumberString(soundId, 4);

        int lastSlashFound = soundPath.lastIndexOf('/');
        if (lastSlashFound >= 0)
            return soundPath.substring(lastSlashFound + 1);

        return (includeId ? "[" + NumberUtils.padNumberString(soundId, 4) + "] " : "") + soundPath;
    }

    /**
     * Returns true if there is a sound path linked for the given SFX ID.
     * @param sfxId the SFX id to lookup
     * @return true iff there is a corresponding sound path.
     */
    public boolean hasFullSoundPathFor(int sfxId) {
        return !StringUtils.isNullOrWhiteSpace(this.soundPathsById.get(sfxId));
    }

    /**
     * Gets the full sound file path for the given sound ID.
     * @param fullPath the path to resolve.
     * @return sfxId, or -1 if the sound effect ID could not be found.
     */
    public int getSfxIdFromFullSoundPath(String fullPath) {
        if (NumberUtils.isInteger(fullPath))
            return Integer.parseInt(fullPath);

        Integer sfxId = this.soundIdsByPath.get(fullPath);
        return sfxId != null ? sfxId : -1;
    }

    /**
     * Marks the given SFX id as used.
     * @param sfxId the sfx id to mark
     */
    public void markSfxIdAsUsed(int sfxId) {
        if (sfxId >= this.nextFreeSoundId)
            this.nextFreeSoundId = sfxId + 1;
    }

    /**
     * Fill the next free sound ID slot, and get that ID.
     * @return nowUsedSoundIdSlot
     */
    public int useNextFreeSoundIdSlot() {
        return this.nextFreeSoundId++;
    }
}