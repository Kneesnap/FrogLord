package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

/**
 * A struct found in the frogger exe.
 * TODO: Rewrite this at some point.
 * Created by Kneesnap on 1/27/2019.
 */
public abstract class ExeStruct extends SCGameData<FroggerGameInstance> {
    public ExeStruct(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    /**
     * Handle a manual correction.
     * @param args The arguments supplied.
     */
    public void handleCorrection(String[] args) {
        throw new UnsupportedOperationException("This struct does not currently support manual correction! (" + getClass().getSimpleName() + ")");
    }

    /**
     * Handle a manual correction.
     * @param str The string containing the arguments.
     */
    public void handleCorrection(String str) {
        handleCorrection(str.split(","));
    }

    /**
     * Test if a file is held by this struct.
     * @param file The file to test.
     * @return isEntry
     */
    public abstract boolean isEntry(SCGameFile<?> file);
}