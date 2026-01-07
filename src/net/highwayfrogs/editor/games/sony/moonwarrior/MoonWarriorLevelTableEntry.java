package net.highwayfrogs.editor.games.sony.moonwarrior;

import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray.VLODirectTextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;

/**
 * Represents a level table entry for Moon Warrior.
 * Created by Kneesnap on 5/8/2024.
 */
public class MoonWarriorLevelTableEntry implements ISCLevelTableEntry {
    private final MoonWarriorMap mapFile; // I don't see any level table in the executable.
    private TextureRemapArray textureRemap;
    private VloFile vloArchive;

    public MoonWarriorLevelTableEntry(MoonWarriorMap mapFile) {
        this.mapFile = mapFile;
    }

    @Override
    public TextureRemapArray getRemap() {
        if (this.textureRemap != null)
            return this.textureRemap;

        VloFile vloFile = getVloFile();
        if (vloFile == null)
            return null;

        return this.textureRemap = new VLODirectTextureRemapArray(this.mapFile.getGameInstance(), vloFile);
    }

    @Override
    public VloFile getVloFile() {
        if (this.vloArchive != null)
            return this.vloArchive;

        // The game might have been so early in development that there was no level table, so we just hardcode it for now.
        switch (this.mapFile.getFileDisplayName()) {
            case "TG.ENT":
            case "TG0.MAP":
                this.vloArchive = this.mapFile.getArchive().getFileByName("KG_TRAINING.VLO");
                break;
            case "WH2.ENT":
            case "WH20.MAP":
                this.vloArchive = this.mapFile.getArchive().getFileByName("WAREHOUSE2.VLO");
                break;
            default:
                this.mapFile.getLogger().severe("Unrecognized map file to get a VLO from: '%s'.", this.mapFile.getFileDisplayName());
                return null;
        }

        if (this.vloArchive == null)
            this.mapFile.getLogger().severe("Failed to get VLO for map file '%s'.", this.mapFile.getFileDisplayName());

        return this.vloArchive;
    }
}