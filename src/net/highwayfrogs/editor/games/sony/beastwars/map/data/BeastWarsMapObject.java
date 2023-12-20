package net.highwayfrogs.editor.games.sony.beastwars.map.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;

/**
 * Represents an object / entity created inside a Beast Wars map.
 * Created by Kneesnap on 9/22/2023.
 */
@Getter
public class BeastWarsMapObject extends SCGameData<BeastWarsInstance> {
    private final BeastWarsMapFile mapFile;
    private byte[] rawBytes; // TODO: Temporary, fix this once we know how it works.

    public static final int SIZE_IN_BYTES = 48; // TODO: Seems pretty similar.

    public BeastWarsMapObject(BeastWarsMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        this.rawBytes = reader.readBytes(SIZE_IN_BYTES);
        // 0x24/1 - If this value == 0xFF, an empty function is run. Seems whatever this functionality was has been stripped out.
        // 0x28/4 - If (((value >> 8) & 0xC0) != 0x80), it subtracts scaled world width and height from  0x14[4] and 0x1C[4]
        // If that value == 0x40, it will directly call a function, otherwise
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(this.rawBytes);
    }
}