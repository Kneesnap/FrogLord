package net.highwayfrogs.editor.games.sony.shared.map.filesync;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.commandparser.CommandListExecutionContext;
import net.highwayfrogs.editor.utils.commandparser.CommandListParser;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.MessageTrackingLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents basic context shared across games.
 * Created by Kneesnap on 2/9/2026.
 */
public abstract class MapFileSyncLoadContext<TMapFile extends SCGameFile<?>> extends CommandListExecutionContext {
    @Getter @NonNull private final TMapFile mapFile;
    @Getter private final int maxSupportedFileFormatVersion; // Max version supported by this FrogLord version.
    @Getter int fileFormatVersion;
    @Getter SCGameConfig gameConfig;
    private TextureRemapArray cachedTextureRemap;
    private VloFile cachedVloFile;

    public MapFileSyncLoadContext(TMapFile mapFile, ILogger logger, String importedFileName, int maxSupportedFileFormatVersion) {
        super(new MessageTrackingLogger(logger), importedFileName);
        this.mapFile = mapFile;
        this.maxSupportedFileFormatVersion = maxSupportedFileFormatVersion;
    }

    @Override
    public MessageTrackingLogger getLogger() {
        return (MessageTrackingLogger) super.getLogger();
    }

    /**
     * Resolves the texture remap.
     * If null is returned, an error will be thrown on import.
     */
    protected abstract TextureRemapArray resolveTextureRemap();

    /**
     * Gets the texture remap holding textures for this context, if there is one.
     * If null is returned, an error will be thrown on import.
     */
    public TextureRemapArray getTextureRemap() {
        if (this.cachedTextureRemap != null)
            return this.cachedTextureRemap;

        return this.cachedTextureRemap = resolveTextureRemap();
    }

    /**
     * Resolves the VloFile.
     * Null may be returned by this function.
     */
    protected abstract VloFile resolveVloFile();

    /**
     * Gets the vlo file holding textures for this context.
     * Null may be returned by this function.
     */
    public VloFile getVloFile() {
        if (this.cachedVloFile != null)
            return this.cachedVloFile;

        return this.cachedVloFile = resolveVloFile();
    }

    /**
     * Executes commands from the input file using the given command parser
     * @param commandParser the command parser to execute commands with
     * @param inputFile the file to read commands from
     */
    @SuppressWarnings("unchecked")
    public <TContext extends MapFileSyncLoadContext<?>> void executeCommands(CommandListParser<TContext> commandParser, File inputFile) {
        if (commandParser == null)
            throw new NullPointerException("commandParser");
        if (inputFile == null)
            throw new NullPointerException("inputFile");

        // Resolve and validate textureRemap.
        TextureRemapArray textureRemap = getTextureRemap();
        if (textureRemap == null)
            throw new IllegalStateException("Could not resolve textureRemap for '" + getMapFile().getFileDisplayName() + "'.");

        // 1) Clear remap before running commands. (Commands must update remap.)
        List<Short> oldTextureIds = new ArrayList<>(textureRemap.getTextureIds());
        textureRemap.getTextureIds().clear();
        this.logger.info("Importing map data from '%s'.", getImportedFileName());

        // 2) Run commands from file.
        try {
            commandParser.executeCommands((TContext) this, inputFile);
        } catch (Throwable th) {
            // Restore previous texture IDs upon failure.
            textureRemap.getTextureIds().clear();
            textureRemap.getTextureIds().addAll(oldTextureIds);
            throw new RuntimeException("An error occurred while loading '" + getImportedFileName() + "'.", th);
        }

        // 3) Validate texture remap.
        // This is necessary, as even changes done in FrogLord need to be reapplied after a restart.
        if (textureRemap.getTextureIds().size() > textureRemap.getTextureIdSlotsAvailable())
            this.mapFile.getGameInstance().showWarning(getLogger(), "Overflowed the texture remap!", "The texture remap for %s has room for %d textures, but %d were imported.\nThis will likely cause graphical corruption in-game!", this.mapFile.getFileDisplayName(), textureRemap.getTextureIdSlotsAvailable(), textureRemap.getTextureIds().size());
        while (textureRemap.getTextureIdSlotsAvailable() > textureRemap.getTextureIds().size())
            textureRemap.getTextureIds().add((short) -1);
    }

    /**
     * Finishes importing the map file, and displays any information related to that.
     */
    public void finish() {
        this.logger.info("Finished importing map data from '%s'.", getImportedFileName());
        if (getLogger().hasErrorsOrWarnings())
            getLogger().showImportPopup(getImportedFileName());
    }
}
