package net.highwayfrogs.editor.file.map.entity.data.rushedmap;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

/**
 * Represents the old crocodile entity data.
 * Created by Kneesnap on 2/1/2023.
 */
@Getter
public class EntityCrocodileOld extends PathData {
    private int openMouthDelay;

    public EntityCrocodileOld(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.openMouthDelay = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Padding.
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.openMouthDelay);
        writer.writeShort((short) 0); // Padding.
    }
}