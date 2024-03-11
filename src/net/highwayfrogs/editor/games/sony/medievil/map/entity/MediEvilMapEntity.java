package net.highwayfrogs.editor.games.sony.medievil.map.entity;

import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;

/**
 * Represents an entity on a MediEvil map.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilMapEntity extends SCGameData<MediEvilGameInstance> {
    private final MediEvilMapFile map;

    private long entityId = -1;

    private int formId = -1;
    private int subFormId = -1;

    private int rotationX;
    private int rotationY;
    private int rotationZ;

    private SVector initialPosition;
    private SVector currentPosition;

    private int mofId;

    public MediEvilMapEntity(MediEvilMapFile map) {
        super(map.getGameInstance());
        this.map = map;
    }

    @Override
    public void load(DataReader reader) {
        this.entityId = reader.readInt();
        this.formId = reader.readUnsignedShortAsInt();
        this.subFormId = reader.readUnsignedByte();
        reader.skipBytes(1); //Unused?

        reader.skipBytes(4); //Pointer
        reader.skipBytes(4); //Pointer

        this.rotationX = reader.readUnsignedByte();
        this.rotationY = reader.readUnsignedByte();
        this.rotationZ = reader.readUnsignedByte();

        reader.skipBytes(1); //Padding?

        this.initialPosition = new SVector();
        this.initialPosition.loadWithPadding(reader);

        this.currentPosition = new SVector();
        this.currentPosition.loadWithPadding(reader);

        reader.skipBytes(4); //Flags
        reader.skipBytes(4); //Flags

        reader.skipBytes(4); //Idk
        reader.skipBytes(4);
        reader.skipBytes(4);
        reader.skipBytes(4);
        reader.skipBytes(4);
    }

    @Override
    public void save(DataWriter writer) {
        // TODO: Make saving work.
    }

    /**
     * Gets the mof file associated with the form, if it can be found.
     */
    public WADFile.WADEntry getMofFileEntry() {
        if (this.map == null)
            return null;

        MediEvilLevelTableEntry levelTableEntry = this.map.getLevelTableEntry();
        if (levelTableEntry == null) {
            getLogger().warning("Couldn't get level table entry, which prevents getting the mof file for a form.");
            return null;
        }

        WADFile wadFile = levelTableEntry.getWadFile();
        if (wadFile == null) {
            getLogger().warning("Couldn't get WAD from the level table entry, which prevents getting the mof file for a form.");
            return null;
        }

        if (this.mofId < 0 || this.mofId >= wadFile.getFiles().size()) {
            getLogger().warning("Couldn't get file " + this.mofId + " from the WAD file '" + wadFile.getFileDisplayName() + "', which prevents getting the mof file for a form.");
            return null;
        }

        return wadFile.getFiles().get(this.mofId);
    }

    public MOFHolder getMof() {
        WADFile.WADEntry wadEntry = getMofFileEntry();
        if (wadEntry == null)
            return null;

        if (!(wadEntry.getFile() instanceof MOFHolder)) {
            getLogger().warning("The form specified file '" + wadEntry.getDisplayName() + "' as its MOF, but this seems to not actually be a MOF.");
            return null;
        }

        return (MOFHolder) wadEntry.getFile();
    }

    @Override
    public MediEvilConfig getConfig() {
        return (MediEvilConfig) super.getConfig();
    }

    /**
     * Setup the editor UI for the entity.
     */
    public void setupEditor(/*MediEvilEntityManager manager, GUIEditorGrid editor*/) {
        // TODO: Implement this.
    }
}