package net.highwayfrogs.editor.file.mof.poly_anim;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.prims.MOFPrimType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents "MR_PART_POLY_ANIM".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
@Setter
public class MOFPartPolyAnim extends GameObject {
    private MOFPrimType primType;
    private MOFPolygon mofPolygon;
    private MOFPartPolyAnimEntryList entryList;

    private transient MOFPart parentPart;
    private transient int tempAnimAddress;

    public MOFPartPolyAnim(MOFPart part) {
        this.parentPart = part;
    }

    public MOFPartPolyAnim(MOFPart part, MOFPolygon polygon, MOFPartPolyAnimEntryList entryList) {
        this(part);
        this.primType = polygon.getType();
        this.mofPolygon = polygon;
        this.entryList = entryList;
    }

    @Override
    public void load(DataReader reader) {
        this.primType = MOFPrimType.values()[reader.readInt()];

        int primId = reader.readInt();
        this.mofPolygon = getParentPart().getPolygon(primId);
        Utils.verify(getMofPolygon().getType() == getPrimType(), "Expected a polygon type %s, but got a %s.", getPrimType(), getMofPolygon().getType());
        Utils.verify(getMofPolygon() != null, "Failed to load MOF Polygon. (%s, %d)", getPrimType(), primId);

        reader.skipPointer(); // Runtime.
        int animPointer = reader.readInt(); // Animation pointer.
        if (getParentPart().getLoadAnimEntryListMap().containsKey(animPointer)) {
            this.entryList = getParentPart().getLoadAnimEntryListMap().get(animPointer);
        } else {
            reader.jumpTemp(animPointer);
            this.entryList = new MOFPartPolyAnimEntryList(getParentPart());
            this.entryList.load(reader);
            reader.jumpReturn();
        }
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
     * @param writer The writer to write data to.
     */
    public void saveExtra(DataWriter writer) {
        Utils.verify(this.tempAnimAddress > 0, "save() was not called first.");
        writer.writeAddressAt(this.tempAnimAddress, this.entryList.getTempSavePointer());
        this.tempAnimAddress = 0;
    }

    /**
     * Gets the frame count of this animation.
     * @return totalFrames
     */
    public int getTotalFrames() {
        int frame = 0;
        for (MOFPartPolyAnimEntry entry : getEntryList().getEntries())
            frame += entry.getDuration();
        return frame;
    }
}
