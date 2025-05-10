package net.highwayfrogs.editor.games.sony.shared.mof2.mesh;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.shared.mof2.hilite.MRMofHilite;
import net.highwayfrogs.editor.games.sony.shared.mof2.hilite.MRMofHilite.HiliteAttachType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the 'MR_MPRIM_HEADER' struct, along with the accompanying primitive data.
 * This holds any number of MOF polygons/primitives, as long as they are of the same type.
 * Created by Kneesnap on 2/19/2025.
 */
public class MRMofPolygonBlock {
    @NonNull @Getter private final MRMofPart parentPart;
    @NonNull @Getter private final MRMofPolygonType polygonType;
    private final List<MRMofPolygon> polygons;
    private final List<MRMofPolygon> immutablePolygons;

    public MRMofPolygonBlock(MRMofPart parentPart, MRMofPolygonType polygonType) {
        this(parentPart, polygonType, null);
    }

    private MRMofPolygonBlock(MRMofPart parentPart, MRMofPolygonType polygonType, List<MRMofPolygon> polygons) {
        this.parentPart = parentPart;
        this.polygonType = polygonType;
        this.polygons = polygons != null ? new ArrayList<>(polygons) : new ArrayList<>();
        this.immutablePolygons = Collections.unmodifiableList(this.polygons);
    }

    /**
     * Saves the polygon block data to the DataWriter.
     * @param writer The writer to save the polygon data to.
     */
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.polygonType.ordinal());
        writer.writeUnsignedShort(this.polygons.size());
        for (int i = 0; i < this.polygons.size(); i++)
            this.polygons.get(i).save(writer);
    }

    /**
     * Returns the polygons included in this block.
     * @return polygons
     */
    public List<MRMofPolygon> getPolygons() {
        return this.immutablePolygons;
    }

    /**
     * Adds a polygon to the block.
     * @param polygon The polygon to add.
     * @return If the polygon was already tracked, false will be returned. Otherwise, true indicates successful registration.
     */
    public boolean addPolygon(MRMofPolygon polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (polygon.getPolygonType() != this.polygonType)
            throw new RuntimeException("Cannot add a polygon of type " + polygon.getPolygonType() + " to a block which only holds " + this.polygonType + " polygons.");

        if (this.polygons.contains(polygon))
            return false; // Already registered.

        this.parentPart.markPolygonListDirty();
        this.polygons.add(polygon);
        return true;
    }

    /**
     * Removes a polygon from the block.
     * @param polygon The polygon to remove.
     * @return true Iff the polygon was successfully removed
     */
    public boolean removePolygon(MRMofPolygon polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");
        if (polygon.getPolygonType() != this.polygonType)
            throw new RuntimeException("Cannot remove a polygon of type " + polygon.getPolygonType() + " from a block which only holds " + this.polygonType + " polygons.");

        if (!this.polygons.remove(polygon))
            return false; // Wasn't registered.

        this.parentPart.markPolygonListDirty();

        // Remove polygon animation targets.
        this.parentPart.setTextureAnimation(polygon, null);

        // Remove polygon MOF hilites.
        List<MRMofHilite> hilites = this.parentPart.getHilites();
        for (int i = 0; i < hilites.size(); i++) {
            MRMofHilite hilite = hilites.get(i);
            if (hilite.getAttachType() == HiliteAttachType.PRIM && polygon == hilite.getPolygon())
                hilites.remove(i--);
        }

        return true;
    }

    /**
     * Loads the polygon block data from the DataReader.
     * @param mofPart The MOF model part which the block is part of.
     * @param reader The reader to read the polygon data from.
     */
    public static MRMofPolygonBlock readBlockFromReader(MRMofPart mofPart, DataReader reader) {
        if (mofPart == null)
            throw new NullPointerException("mofPart");
        if (reader == null)
            throw new NullPointerException("reader");

        // Read header.
        int primitiveTypeInt = reader.readUnsignedShortAsInt();
        if (primitiveTypeInt < 0 || primitiveTypeInt >= MRMofPolygonType.values().length)
            throw new RuntimeException("Invalid MRMofPolygonType ID: " + primitiveTypeInt);

        MRMofPolygonType polygonType = MRMofPolygonType.values()[primitiveTypeInt];
        int polygonCount = reader.readUnsignedShortAsInt();

        // Read polygons.
        List<MRMofPolygon> polygons = new ArrayList<>();
        for (int i = 0; i < polygonCount; i++) {
            MRMofPolygon newPolygon = new MRMofPolygon(mofPart, polygonType);
            newPolygon.load(reader);
            polygons.add(newPolygon);
        }

        return new MRMofPolygonBlock(mofPart, polygonType, polygons);
    }
}
