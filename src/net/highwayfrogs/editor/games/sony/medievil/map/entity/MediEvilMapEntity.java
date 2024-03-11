package net.highwayfrogs.editor.games.sony.medievil.map.entity;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;

/**
 * Represents an entity on a MediEvil map.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilMapEntity extends SCGameData<MediEvilGameInstance> {
    private final MediEvilMapFile map;

    private long entityId;

    private int formId;
    private int subFormId;

    private int rotationX;
    private int rotationY;
    private int rotationZ;

    private SVector initialPosition;
    private SVector currentPosition;

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