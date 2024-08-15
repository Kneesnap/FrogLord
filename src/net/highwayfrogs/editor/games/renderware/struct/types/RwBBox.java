package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;

/**
 * Implements the 'RwBBox' struct from src/babbox.h
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public class RwBBox extends RwStruct {
    private final RwV3d maxPosition;
    private final RwV3d minPosition;

    public RwBBox(GameInstance instance) {
        super(instance, RwStructType.BOUNDING_BOX);
        this.maxPosition = new RwV3d(instance);
        this.minPosition = new RwV3d(instance);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.maxPosition.load(reader, version, byteLength);
        this.minPosition.load(reader, version, byteLength);
    }

    @Override
    public void save(DataWriter writer, int version) {
        this.maxPosition.save(writer, version);
        this.minPosition.save(writer, version);
    }

    @Override
    public String toString() {
        return "RwBBox{min=" + this.minPosition + ",max=" + this.maxPosition + "}";
    }
}