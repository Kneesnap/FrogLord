package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbook;
import net.highwayfrogs.editor.file.mof.hilite.MOFHilite;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntryList;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.prims.MOFPrimType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the MR_PART struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFPart extends GameObject {
    private final Map<MOFPrimType, List<MOFPolygon>> mofPolygons = new HashMap<>();
    private List<MOFPartcel> partcels = new ArrayList<>();
    private List<MOFHilite> hilites = new ArrayList<>();
    @Setter private PSXMatrix matrix; // Seems to usually be null. JUN_PLANT is the only model that uses this, to add a slight roll rotation.
    @Setter private MOFCollprim collprim;
    private final List<MOFPartPolyAnim> partPolyAnims = new ArrayList<>();
    private final List<MOFPartPolyAnimEntryList> partPolyAnimLists = new ArrayList<>();
    @Setter private MOFFlipbook flipbook;

    private final transient MOFFile parent;
    private final transient Map<Integer, MOFPartPolyAnimEntryList> loadAnimEntryListMap = new HashMap<>();
    private final transient Map<List<SVector>, Integer> saveNormalMap = new HashMap<>();
    @Setter private transient int saveBboxPointer;
    private final transient List<MOFPolygon> orderedByLoadPolygons = new ArrayList<>();
    private transient int tempPartcelPointer;
    private transient int tempPrimitivePointer;
    private transient int tempHilitePointer;
    private transient int tempCollPrimPointer;
    private transient int tempMatrixPointer;
    private transient int tempAnimatedTexturesPointer;
    private transient int tempFlipbookPointer;
    @Setter private transient int tempVertexStart;

    public MOFPart(MOFFile parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        int flags = reader.readUnsignedShortAsInt();

        int partcelCount = reader.readUnsignedShortAsInt();
        int verticeCount = reader.readUnsignedShortAsInt();
        int normalCount = reader.readUnsignedShortAsInt();
        int primitiveCount = reader.readUnsignedShortAsInt();
        int hiliteCount = reader.readUnsignedShortAsInt();

        int partcelPointer = reader.readInt();
        int primitivePointer = reader.readInt();
        int hilitePointer = reader.readInt(); // May be null
        reader.skipPointer(); // Run-time value.

        int collprimPointer = reader.readInt(); // May be null.
        int matrixPointer = reader.readInt(); // May be null.
        int animatedTexturesPointer = reader.readInt(); // (Point to integer which is count.) Followed by: MR_PART_POLY_ANIM
        int flipbookPointer = reader.readInt(); // MR_PART_FLIPBOOK (MR_PART_FLIPBOOK_ACTION may follow?)

        boolean incompleteMOF = getParent().getHolder().isIncomplete();

        // Read Partcels.
        if (partcelCount > 0 && !incompleteMOF) {
            reader.jumpTemp(partcelPointer);
            for (int i = 0; i < partcelCount; i++) {
                MOFPartcel partcel = new MOFPartcel(this, verticeCount, normalCount);
                partcel.load(reader);
                partcels.add(partcel);
            }
            reader.jumpReturn();
        }

        // Read matrix.
        if (matrixPointer > 0 && !incompleteMOF) {
            reader.jumpTemp(matrixPointer);
            this.matrix = new PSXMatrix();
            this.matrix.load(reader);
            reader.jumpReturn();
        }

        // Read collprim.
        if (collprimPointer > 0 && !incompleteMOF) {
            reader.jumpTemp(collprimPointer);
            this.collprim = new MOFCollprim(this);
            this.collprim.load(reader);
            reader.jumpReturn();
        }

        // Read Primitives:
        orderedByLoadPolygons.clear();
        reader.jumpTemp(primitivePointer);
        while (primitiveCount > 0) {
            short primType = reader.readShort(); // MR_MPRIM_HEADER
            short primCount = reader.readShort();

            Utils.verify(MOFPrimType.values().length > primType, "Unknown prim-type: %d", primType);
            MOFPrimType mofPrimType = MOFPrimType.values()[primType];

            List<MOFPolygon> prims = mofPolygons.computeIfAbsent(mofPrimType, type -> new ArrayList<>());
            for (int i = 0; i < primCount; i++) {
                MOFPolygon newPoly = mofPrimType.makeNew(this);
                newPoly.load(reader);
                prims.add(newPoly);
                orderedByLoadPolygons.add(newPoly);
            }

            primitiveCount -= primCount;
        }
        reader.jumpReturn();

        // Read Hilites
        if (!incompleteMOF) {
            reader.jumpTemp(hilitePointer);
            for (int i = 0; i < hiliteCount; i++) {
                MOFHilite hilite = new MOFHilite(this);
                hilite.load(reader);
                hilites.add(hilite);
            }
            reader.jumpReturn();
        }

        if (animatedTexturesPointer > 0 && !incompleteMOF) {
            loadAnimEntryListMap.clear();
            reader.jumpTemp(animatedTexturesPointer);
            int count = reader.readInt();
            for (int i = 0; i < count; i++) {
                MOFPartPolyAnim partPolyAnim = new MOFPartPolyAnim(this);
                partPolyAnim.load(reader);
                this.partPolyAnims.add(partPolyAnim);
            }
            reader.jumpReturn();
        }

        if (flipbookPointer > 0 && !incompleteMOF) {
            reader.jumpTemp(flipbookPointer);
            this.flipbook = new MOFFlipbook();
            this.flipbook.load(reader);
            reader.jumpReturn();
        }

        if (flags != buildFlags())
            throw new RuntimeException("Generated Flags do not match real flags. (" + flags + ", " + buildFlags() + ")");
    }

    /**
     * Build flags for this MOF, based on the data in this MOF.
     */
    public int buildFlags() {
        return getPartPolyAnims().size() > 0 ? 1 : 0;
    }

    @Override
    public void save(DataWriter writer) {
        int verticeCount = getStaticPartcel().getVertices().size();
        int normalCount = getStaticPartcel().getNormals().size();
        for (MOFPartcel partcel : getPartcels()) { // Extra safety check, so if this somehow happens we won't be baffled by the in-game results.
            if (verticeCount != partcel.getVertices().size())
                throw new RuntimeException("Not all of the partcels in part #" + getPartID() + " had the same number of vertices! (" + verticeCount + ", " + partcel.getVertices().size() + ")");
            if (normalCount != partcel.getNormals().size())
                throw new RuntimeException("Not all of the partcels in part #" + getPartID() + " had the same number of normals! (" + normalCount + ", " + partcel.getNormals().size() + ")");
        }

        writer.writeUnsignedShort(buildFlags());
        writer.writeUnsignedShort(getPartcels().size());
        writer.writeUnsignedShort(verticeCount);
        writer.writeUnsignedShort(normalCount);
        writer.writeUnsignedShort(getMofPolygons().values().stream().mapToInt(List::size).sum()); // PrimitiveCount.
        writer.writeUnsignedShort(getHilites().size());

        this.tempPartcelPointer = writer.writeNullPointer();
        this.tempPrimitivePointer = writer.writeNullPointer();
        this.tempHilitePointer = writer.writeNullPointer();
        writer.writeInt(0); // Runtime value.
        this.tempCollPrimPointer = writer.writeNullPointer();
        this.tempMatrixPointer = writer.writeNullPointer();
        this.tempAnimatedTexturesPointer = writer.writeNullPointer();
        this.tempFlipbookPointer = writer.writeNullPointer();
    }

    /**
     * Save extra data.
     * @param writer Save extra data to a writer.
     */
    public void saveExtra(DataWriter writer) {
        this.saveBboxPointer = 0;

        // Write Partcels.
        if (getPartcels().size() > 0) {
            getSaveNormalMap().clear();
            getPartcels().forEach(partcel -> partcel.save(writer));
            getPartcels().forEach(partcel -> partcel.saveNormalData(writer));
            writer.writeAddressTo(getTempPartcelPointer());
            getPartcels().forEach(partcel -> partcel.savePointerData(writer));
            getPartcels().forEach(partcel -> partcel.saveBboxData(writer));
        }

        // Read Hilites
        if (getHilites().size() > 0) {
            writer.writeAddressTo(getTempHilitePointer());
            getHilites().forEach(hilite -> hilite.save(writer));
        }

        // Write collprim.
        if (getCollprim() != null) {
            writer.writeAddressTo(getTempCollPrimPointer());
            getCollprim().save(writer);
        }

        // Write Matrix.
        if (getMatrix() != null) {
            writer.writeAddressTo(getTempMatrixPointer());
            getMatrix().save(writer);
        }

        if (getPartPolyAnims().size() > 0) {
            writer.writeAddressTo(getTempAnimatedTexturesPointer());
            writer.writeInt(getPartPolyAnims().size());
            getPartPolyAnims().forEach(partPolyAnim -> partPolyAnim.save(writer));
            getPartPolyAnimLists().forEach(list -> list.save(writer));
            getPartPolyAnims().forEach(mofPartPolyAnim -> mofPartPolyAnim.saveExtra(writer));
        }

        if (getFlipbook() != null) {
            writer.writeAddressTo(getTempFlipbookPointer());
            getFlipbook().save(writer);
        }

        // Write Primitives.
        writer.writeAddressTo(getTempPrimitivePointer());
        for (MOFPrimType primType : MOFPrimType.values()) { // NOTE: This erases the order which seems to not be consistent.
            List<MOFPolygon> polygons = getMofPolygons().get(primType);
            if (polygons == null)
                continue;

            writer.writeUnsignedShort(primType.ordinal()); // Write type.
            writer.writeUnsignedShort(polygons.size());
            polygons.forEach(prim -> prim.save(writer));
        }
    }

    /**
     * Gets a polygon id, by the loaded order. (The way it turns out Frogger does it.)
     * @param primId The id to get.
     * @return mofPolygon
     */
    public MOFPolygon getPolygon(int primId) {
        return getOrderedByLoadPolygons().get(primId);
    }

    /**
     * Gets the ID that a given MOFPolygon should be saved as.
     * @param poly The polygon to save.
     * @return primId
     */
    public int getPolygonSaveID(MOFPolygon poly) {
        int primId = 0;
        for (MOFPrimType type : MOFPrimType.values()) {
            List<MOFPolygon> polygons = getMofPolygons().get(type);
            if (polygons == null)
                continue;

            int index = polygons.indexOf(poly);
            if (index >= 0) {
                return primId + index;
            } else {
                primId += polygons.size();
            }
        }

        throw new IllegalArgumentException("This MOFPolygon is not registered, and therefore does not have an id!");
    }

    /**
     * Gets a static partcel, the one which will not be affected by flipbook animations.
     * @return mofPartcel
     */
    public MOFPartcel getStaticPartcel() {
        return getPartcels().get(0);
    }

    /**
     * Gets the id of this part.
     * @return partId
     */
    public int getPartID() {
        int partId = getParent().getParts().indexOf(this);
        Utils.verify(partId >= 0, "MOFPart is not registered!");
        return partId;
    }

    /**
     * Gets the part-cel id.
     * @param flipbookId The animation id.
     * @param frame      The global frame count.
     * @return celId
     */
    public int getCelId(int flipbookId, int frame) {
        return getFlipbook() != null ? getFlipbook().getPartCelIndex(flipbookId, frame) : 0;
    }

    /**
     * Gets the flipbook part-cel.
     * @param flipbookId The animation id.
     * @param frame      The global frame count.
     * @return cel
     */
    public MOFPartcel getCel(int flipbookId, int frame) {
        return getPartcels().get(Math.min(getPartcels().size() - 1, getCelId(flipbookId, frame)));
    }

    /**
     * Copy data in this mof to the incomplete mof.
     * @param incompletePart incompleteMof
     */
    public void copyToIncompletePart(MOFPart incompletePart) {
        incompletePart.partcels = this.partcels;
        incompletePart.hilites = this.hilites;
        incompletePart.collprim = this.collprim;
        incompletePart.matrix = this.matrix;
        incompletePart.flipbook = this.flipbook;
        if (getParent().hasTextureAnimation() && !getConfig().isAtOrBeforeBuild20()) // TODO: Replace with some kind of warning system, where instead of throwing exceptions, we can have warnings per-file, etc.
            throw new RuntimeException("Texture animation cannot be copied to an incomplete MOF right now!"); // It is believed this wouldn't work in the retail game either.
    }

    /**
     * Generate a bounding box for this partcel.
     * This is slightly inaccurate, but only by a little, there was likely some information lost when the original models were converted to MOF.
     * @return boundingBox
     */
    public MOFBBox makeBoundingBox() {
        if (getPartcels().isEmpty())
            return new MOFBBox();

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        float maxZ = Float.MIN_VALUE;

        for (MOFPartcel partcel : getPartcels()) {
            for (SVector vertex : partcel.getVertices()) {
                minX = Math.min(minX, vertex.getFloatX());
                minY = Math.min(minY, vertex.getFloatY());
                minZ = Math.min(minZ, vertex.getFloatZ());
                maxX = Math.max(maxX, vertex.getFloatX());
                maxY = Math.max(maxY, vertex.getFloatY());
                maxZ = Math.max(maxZ, vertex.getFloatZ());
            }
        }

        MOFBBox box = new MOFBBox();
        box.getVertices()[0].setValues(minX, minY, minZ, 4);
        box.getVertices()[1].setValues(minX, minY, maxZ, 4);
        box.getVertices()[2].setValues(minX, maxY, minZ, 4);
        box.getVertices()[3].setValues(minX, maxY, maxZ, 4);
        box.getVertices()[4].setValues(maxX, minY, minZ, 4);
        box.getVertices()[5].setValues(maxX, minY, maxZ, 4);
        box.getVertices()[6].setValues(maxX, maxY, minZ, 4);
        box.getVertices()[7].setValues(maxX, maxY, maxZ, 4);
        return box;
    }

    /**
     * Gets all of the polygons within this model.
     */
    public List<MOFPolygon> getAllPolygons() {
        List<MOFPolygon> polygons = new ArrayList<>();
        for (List<MOFPolygon> list : getMofPolygons().values())
            polygons.addAll(list);
        return polygons;
    }

    /**
     * Tests if this part should be hidden.
     * @return shouldHide
     */
    public boolean shouldHide() {
        return "GEN_FROG.XAR".equals(getParent().getFileEntry().getDisplayName()) && getPartID() == 15;
    }
}