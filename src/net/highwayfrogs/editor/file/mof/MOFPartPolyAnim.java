package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.prims.MOFPrimType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents "MR_PART_POLY_ANIM".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MOFPartPolyAnim extends GameObject {
    private MOFPrimType primType;
    private MOFPolygon mofPolygon;

    private transient MOFPart parentPart;
    private transient int tempAnimAddress;

    public MOFPartPolyAnim(MOFPart part) {
        this.parentPart = part;
    }

    @Override
    public void load(DataReader reader) {
        this.primType = MOFPrimType.values()[reader.readInt()];

        int primId = reader.readInt();
        this.mofPolygon = getParentPart().getPolygon(primId, this.primType);
        Utils.verify(getMofPolygon() != null, "Failed to load MOF Polygon. (%s, %d)", getPrimType(), primId);

        reader.readInt(); // Runtime.
        reader.readInt(); // Animation pointer.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.primType.ordinal());
        writer.writeInt(getParentPart().getPolygonSaveID(this.mofPolygon));
        writer.writeNullPointer(); // Runtime.
        this.tempAnimAddress = writer.writeNullPointer();
    }

    /**
     * Write extra data.
     * @param writer  The writer to write data to.
     * @param address The address to write.
     */
    public void saveExtra(DataWriter writer, int address) {
        Utils.verify(this.tempAnimAddress > 0, "save() was not called first.");
        writer.jumpTemp(this.tempAnimAddress);
        writer.writeInt(address);
        writer.jumpReturn();
        this.tempAnimAddress = 0;
    }
}
