
package net.highwayfrogs.editor.games.sony.medievil.map.entity.data;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.config.MediEvilConfig;


/**
 * Represents arbitrary entity data.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilEntityData extends SCGameData<MediEvilGameInstance> {
    private final boolean inOverlay;
    private String name = "";
    private long mofId = -1;

    public MediEvilEntityData(MediEvilGameInstance instance, boolean inOverlay) {
        super(instance);
        this.inOverlay = inOverlay;
    }

    @Override
    public void load(DataReader reader) {
        this.loadMainEntityData(reader);
    }

    @Override
    public void save(DataWriter writer) {
        //this.saveMainEntityData(writer);
    }

    /**
     * Load the main entity data from the reader.
     * @param reader The reader to read data from.
     */
    protected void loadMainEntityData(DataReader reader) {
        long offset = isInOverlay() ? getConfig().getOverlayOffset() : getGameInstance().getRamOffset();
        long entityNamePtr = reader.readUnsignedIntAsLong();
        entityNamePtr -= offset;
        reader.jumpTemp((int)entityNamePtr);
        this.name = reader.readString(Constants.NULL_BYTE);
        reader.jumpReturn();
        reader.skipBytes(172); // TODO: Read more of this
        this.mofId = reader.readUnsignedIntAsLong();

    }

    @Override
    public MediEvilConfig getConfig() {
        return (MediEvilConfig) super.getConfig();
    }
}