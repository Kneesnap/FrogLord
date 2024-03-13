package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.medievil.data.MediEvilMapCollprim;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MediEvil map format collprim packet.
 * Created by RampantSpirit on 3/13/2024.
 */
@Getter
public class MediEvilMapCollprimsPacket extends MediEvilMapPacket {
    public static final String IDENTIFIER = "PLOC"; // 'COLP'.
    private final List<MediEvilMapCollprim> collprims = new ArrayList<>();

    public MediEvilMapCollprimsPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int collprimCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Padding (-1)
        int collprimListPtr = reader.readInt();

        // Read collprims
        this.collprims.clear();
        reader.jumpTemp(collprimListPtr);
        for (int i = 0; i < collprimCount; i++) {
            MediEvilMapCollprim collprim = new MediEvilMapCollprim(getParentFile());
            collprim.load(reader);
            this.collprims.add(collprim);
        }

        reader.setIndex(endIndex);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        // TODO: Implement.
    }
}