package net.highwayfrogs.editor.games.sony.shared.mof2.mesh;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.shared.collprim.MRCollprim;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook.MRMofFlipbookAnimationList;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimationPolygonTarget;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofBoundingBox;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofCollprim;
import net.highwayfrogs.editor.games.sony.shared.mof2.hilite.MRMofHilite;
import net.highwayfrogs.editor.games.sony.shared.mof2.hilite.MRMofHilite.HiliteAttachType;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.*;

/**
 * Represents the MR_PART struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MRMofPart extends SCSharedGameData {
    @NonNull @Getter private final transient MRStaticMof parentMof;
    private final Map<MRMofPolygonType, List<MRMofPolygonBlock>> polygonBlocksByType = new HashMap<>();
    private final List<MRMofPolygonBlock> polygonBlocks  = new ArrayList<>(); // In the order they are loaded/saved.
    private final List<MRMofPolygon> orderedPolygons  = new ArrayList<>(); // In the order they are loaded/saved.
    @Getter private List<MRMofPartCel> partCels = new ArrayList<>();
    @Getter private List<MRMofHilite> hilites = new ArrayList<>(); // Safe to let the user modify.
    @Getter private List<MRMofCollprim> collPrims = new ArrayList<>();
    @Getter private List<PSXMatrix> collPrimMatrices = new ArrayList<>();
    private final List<MRMofTextureAnimationPolygonTarget> textureAnimationPolygonTargets = new ArrayList<>(); // "Part Poly Animations"
    private final List<MRMofTextureAnimation> textureAnimations = new ArrayList<>(); // "Part Poly Animations"
    @Getter private MRMofFlipbookAnimationList flipbook = new MRMofFlipbookAnimationList();;
    private boolean polygonListDirty;

    private final List<MRMofPolygon> immutableOrderedPolygons = Collections.unmodifiableList(this.orderedPolygons);
    private final List<MRMofPolygonBlock> immutablePolygonBlocks = Collections.unmodifiableList(this.polygonBlocks);
    private final List<MRMofTextureAnimationPolygonTarget> immutableTextureAnimationPolygonTargets = Collections.unmodifiableList(this.textureAnimationPolygonTargets);
    private final List<MRMofTextureAnimation> immutableTextureAnimations = Collections.unmodifiableList(this.textureAnimations);

    public static final int FLAG_PART_HAS_ANIMATED_POLYGONS = Constants.BIT_FLAG_0;
    private static final int FLAG_VALIDATION_MASK = FLAG_PART_HAS_ANIMATED_POLYGONS;

    public MRMofPart(@NonNull MRStaticMof parent) {
        this(parent.getGameInstance(), parent);
    }

    public MRMofPart(SCGameInstance instance, MRStaticMof parent) {
        super(instance);
        this.parentMof = parent;

    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.parentMof.getLogger(), "part=" + getPartID(), AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void load(DataReader reader) {
        throw new UnsupportedOperationException("Directly saving/loading MRMofPart is unsupported, call loadHeader() instead.");
    }

    @Override
    public void save(DataWriter writer) {
        throw new UnsupportedOperationException("Directly saving/loading MRMofPart is unsupported, call saveHeader() instead.");
    }

    /**
     * Generate a bit flag mask for this MOF, based on the data in this MOF.
     */
    public int generateBitFlags() {
        return this.textureAnimationPolygonTargets.size() > 0 ? FLAG_PART_HAS_ANIMATED_POLYGONS : 0;
    }

    /**
     * Get ordered polygons of the given type.
     * @param polygonType The type of polygon to get
     * @return polygonsOfType
     */
    public List<MRMofPolygonBlock> getPolygonBlocksByType(MRMofPolygonType polygonType) {
        List<MRMofPolygonBlock> polygons = this.polygonBlocksByType.get(polygonType);
        return polygons != null ? Collections.unmodifiableList(polygons) : Collections.emptyList();
    }

    /**
     * Marks the polygon list as dirty/changed.
     */
    public void markPolygonListDirty() {
        this.polygonListDirty = true;
    }

    /**
     * Gets all polygons sorted in the order which they are saved/loaded.
     * @return orderedPolygons
     */
    public List<MRMofPolygon> getOrderedPolygons() {
        if (this.polygonListDirty) {
            this.polygonListDirty = false;

            this.orderedPolygons.clear();
            for (int i = 0; i < this.polygonBlocks.size(); i++) {
                MRMofPolygonBlock block = this.polygonBlocks.get(i);
                this.orderedPolygons.addAll(block.getPolygons());
            }
        }

        return this.immutableOrderedPolygons;
    }


    /**
     * Get all polygon blocks in order.
     */
    public List<MRMofPolygonBlock> getPolygonBlocks() {
        return this.immutablePolygonBlocks;
    }

    /**
     * Gets a polygon by its index into the polygon list.
     * @param polygonIndex The index of the polygon to get.
     * @return mofPolygon
     */
    public MRMofPolygon getPolygon(int polygonIndex) {
        List<MRMofPolygon> allPolygons = getOrderedPolygons();
        return polygonIndex >= 0 && polygonIndex < allPolygons.size() ? allPolygons.get(polygonIndex) : null;
    }

    /**
     * Gets the index that a given MOFPolygon would be saved as if a save were to occur right now.
     * Such an index can become invalid if polygon data were to change.
     * @param polygon The polygon to get the index of.
     * @return polygonIndex
     */
    public int getPolygonIndex(MRMofPolygon polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");

        int blockIndex = 0;
        for (int i = 0; i < this.polygonBlocks.size(); i++) {
            MRMofPolygonBlock block = this.polygonBlocks.get(i);
            if (block.getPolygonType() == polygon.getPolygonType()) {
                int localIndex = block.getPolygons().indexOf(polygon);
                if (localIndex >= 0)
                    return blockIndex + localIndex;
            }

            blockIndex += block.getPolygons().size();
        }

        throw new IllegalArgumentException("The specified MRMofPolygon is not registered, and therefore does not have an index!");
    }

    /**
     * Adds a polygon block to this part.
     * This method will fail (return false) if the polygon block is already registered.
     * @param block The block to add.
     * @return If the block was added successfully.
     */
    public boolean addPolygonBlock(MRMofPolygonBlock block) {
        if (block == null)
            throw new NullPointerException("block");
        if (block.getParentPart() != this)
            throw new RuntimeException("Cannot add polygon block which has another MRMofPart as its parent!");

        if (this.polygonBlocks.contains(block))
            return false;

        this.polygonBlocks.add(block);
        this.polygonBlocksByType.computeIfAbsent(block.getPolygonType(), key -> new ArrayList<>()).add(block);
        if (!this.polygonListDirty)
            this.orderedPolygons.addAll(block.getPolygons());

        return true;
    }

    /**
     * Removes a polygon block from this part.
     * This method will fail (return false) if the polygon block was not registered.
     * Any data (such as animation targets or MOF hilites) reliant upon the removed polygons will also be removed.
     * @param block The block to remove.
     * @return If the block was removed successfully.
     */
    public boolean removePolygonBlock(MRMofPolygonBlock block) {
        if (block == null)
            throw new NullPointerException("block");
        if (block.getParentPart() != this)
            return false;

        if (!this.polygonBlocks.remove(block))
            return false;

        List<MRMofPolygonBlock> polygonBlocksOfType = this.polygonBlocksByType.get(block.getPolygonType());
        if (polygonBlocksOfType != null && (polygonBlocksOfType.remove(block) || polygonBlocksOfType.isEmpty()))
            this.polygonBlocksByType.remove(block.getPolygonType(), polygonBlocksOfType);

        if (!this.polygonListDirty)
            this.orderedPolygons.removeAll(block.getPolygons());

        // Remove polygon animation targets.
        for (int i = 0; i < this.textureAnimationPolygonTargets.size(); i++) {
            MRMofTextureAnimationPolygonTarget polygonTarget = this.textureAnimationPolygonTargets.get(i);
            if (block.getPolygons().contains(polygonTarget.getPolygon()))
                this.textureAnimationPolygonTargets.remove(i--);
        }

        // Remove polygon MOF hilites.
        for (int i = 0; i < this.hilites.size(); i++) {
            MRMofHilite hilite = this.hilites.get(i);
            if (hilite.getAttachType() == HiliteAttachType.PRIM && block.getPolygons().contains(hilite.getPolygon()))
                this.hilites.remove(i--);
        }

        return true;
    }

    /**
     * Gets a list of all the texture animations available.
     * @return textureAnimations
     */
    public List<MRMofTextureAnimation> getTextureAnimations() {
        return this.immutableTextureAnimations;
    }

    /**
     * Gets a list of all the texture animation target polygons.
     */
    public List<MRMofTextureAnimationPolygonTarget> getTextureAnimationPolygonTargets() {
        return this.immutableTextureAnimationPolygonTargets;
    }

    /**
     * Registers a new texture animation.
     * @param textureAnimation The texture animation to register.
     * @return Was the registration successful?
     */
    public boolean addTextureAnimation(MRMofTextureAnimation textureAnimation) {
        if (textureAnimation == null)
            throw new NullPointerException("textureAnimation");
        if (textureAnimation.getParentPart() != this)
            throw new RuntimeException("Cannot add texture animation, it is registered to another MRMofPart.");

        if (this.textureAnimations.contains(textureAnimation))
            return false;

        this.textureAnimations.add(textureAnimation);
        return true;
    }

    /**
     * Registers a new texture animation. All usages of that texture animation will be removed too.
     * Any animation polygon targets reliant upon the removed animation will also be removed.
     * @param textureAnimation The texture animation to register.
     * @return Was the registration successful?
     */
    public boolean removeTextureAnimation(MRMofTextureAnimation textureAnimation) {
        if (textureAnimation == null)
            throw new NullPointerException("textureAnimation");
        if (textureAnimation.getParentPart() != this)
            return false;

        if (!this.textureAnimations.remove(textureAnimation))
            return false;

        // After removal, remove all usages in the texture animation list.
        for (int i = 0; i < this.textureAnimationPolygonTargets.size(); i++) {
            MRMofTextureAnimationPolygonTarget polygonTarget = this.textureAnimationPolygonTargets.get(i);
            if (polygonTarget.getAnimation() == textureAnimation)
                this.textureAnimationPolygonTargets.remove(i--);
        }

        return true;
    }

    /**
     * Sets the texture animation for the given polygon.
     * If null is supplied, any texture animation will be removed.
     * Any existing texture animation will be replaced.
     * @param polygon The polygon to apply an animation to
     * @param textureAnimation the animation to apply
     */
    public void setTextureAnimation(MRMofPolygon polygon, MRMofTextureAnimation textureAnimation) {
        if (polygon == null)
            throw new NullPointerException("polygon");

        // I didn't actually verify this can't happen, and tbh I bet it would work...
        // But then it'd break FrogLord's ability to load that data without rewriting some stuff.
        // There's no real reason to support this, so I guess it's fine.
        if (textureAnimation != null && textureAnimation.getParentPart() != this)
            throw new RuntimeException("Cannot apply a texture animation for a different MRMofPart!");

        // Register the texture animation if it's not already registered.
        if (textureAnimation != null && !this.textureAnimations.contains(textureAnimation))
            addTextureAnimation(textureAnimation);

        // Find polygon if it's already there.
        int existingPolygonTargetIndex = -1;
        for (int i = 0; i < this.textureAnimationPolygonTargets.size(); i++) {
            MRMofTextureAnimationPolygonTarget polygonTarget = this.textureAnimationPolygonTargets.get(i);
            if (polygonTarget.getPolygon() == polygon) {
                if (polygonTarget.getAnimation() == textureAnimation)
                    return; // The animation is already set.

                existingPolygonTargetIndex = i;
                break;
            }
        }

        // Remove/replace existing one.
        if (textureAnimation != null) {
            MRMofTextureAnimationPolygonTarget newTarget = new MRMofTextureAnimationPolygonTarget(this, polygon, textureAnimation);
            if (existingPolygonTargetIndex >= 0) {
                this.textureAnimationPolygonTargets.set(existingPolygonTargetIndex, newTarget);
            } else {
                this.textureAnimationPolygonTargets.add(newTarget);
            }
        } else if (existingPolygonTargetIndex >= 0) {
            this.textureAnimationPolygonTargets.remove(existingPolygonTargetIndex);
        }
    }

    /**
     * Gets a static partcel, the one which in the default state of the model.
     * Flipbook animations will flip through the list of partcels, but the partCel returned here is the default.
     * @return mofPartcel
     */
    public MRMofPartCel getStaticPartcel() {
        if (this.partCels.isEmpty())
            throw new RuntimeException("Cannot get the model part's static partCel because there are no partCels!");

        return this.partCels.get(0);
    }

    /**
     * Gets the id of this part.
     * @return partId
     */
    public int getPartID() {
        return Utils.getLoadingIndex(this.parentMof.getParts(), this);
    }

    /**
     * Gets the part-cel id.
     * @param flipbookId The animation id.
     * @param frame      The global frame count.
     * @return celId
     */
    public int getCelId(int flipbookId, int frame) {
        return this.flipbook.getPartCelIndex(flipbookId, frame);
    }

    /**
     * Gets the flipbook part-cel.
     * @param flipbookId The animation id.
     * @param frame      The global frame count.
     * @return cel
     */
    public MRMofPartCel getCel(int flipbookId, int frame) {
        return this.partCels.get(Math.min(this.partCels.size() - 1, getCelId(flipbookId, frame)));
    }

    /**
     * Copy data in this mof to the incomplete mof.
     * @param incompletePart incompleteMof
     */
    public void copyToIncompletePart(MRMofPart incompletePart) {
        incompletePart.partCels = this.partCels;
        incompletePart.hilites = this.hilites;
        incompletePart.collPrims = this.collPrims;
        incompletePart.collPrimMatrices = this.collPrimMatrices;
        incompletePart.flipbook = this.flipbook;
        SCGameInstance instance = getGameInstance();

        // I believe this wouldn't work in the retail game either due to referencing polygons (POLYGONS ARE NOT COPIED TO/FROM INCOMPLETE MODELS).
        if (this.parentMof.getTextureAnimationCount() > 0 && (!instance.isFrogger() || !((FroggerConfig) instance.getVersionConfig()).isAtOrBeforeBuild23()))
            getLogger().warning("Texture animation cannot be copied to an incomplete MOF (from %s) due to polygon data not being shared!", incompletePart.getParentMof().getModel().getFileDisplayName());
    }

    /**
     * Generate a bounding box for this part.
     * In practice (at least in Frogger, and potentially more), the same bounding box is shared amongst all partCels.
     * That bounding box is the smallest box capable of fully including all vertex positions within all partCels.
     * This method is slightly inaccurate to the original data, but only by a little.
     * The current hypothesis is that there was likely full 32-bit floating point coordinates in the original models, which were used to make these boxes.
     * However, we can only use 16 bit fixed point coordinates to calculate these, which means ours are slightly lower resolution/slightly differ from the original.
     * @return boundingBox
     */
    public MRMofBoundingBox makeBoundingBox() {
        MRMofBoundingBox box = new MRMofBoundingBox();
        if (getPartCels().isEmpty())
            return box;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        float maxZ = Float.MIN_VALUE;
        boolean anyVertices = false;

        for (MRMofPartCel partCel : getPartCels()) {
            for (SVector vertex : partCel.getVertices()) {
                minX = Math.min(minX, vertex.getFloatX());
                minY = Math.min(minY, vertex.getFloatY());
                minZ = Math.min(minZ, vertex.getFloatZ());
                maxX = Math.max(maxX, vertex.getFloatX());
                maxY = Math.max(maxY, vertex.getFloatY());
                maxZ = Math.max(maxZ, vertex.getFloatZ());
                anyVertices = true;
            }
        }

        if (anyVertices) {
            box.getVertices()[0].setValues(minX, minY, minZ, 4);
            box.getVertices()[1].setValues(minX, minY, maxZ, 4);
            box.getVertices()[2].setValues(minX, maxY, minZ, 4);
            box.getVertices()[3].setValues(minX, maxY, maxZ, 4);
            box.getVertices()[4].setValues(maxX, minY, minZ, 4);
            box.getVertices()[5].setValues(maxX, minY, maxZ, 4);
            box.getVertices()[6].setValues(maxX, maxY, minZ, 4);
            box.getVertices()[7].setValues(maxX, maxY, maxZ, 4);
        }

        return box;
    }

    /**
     * Tests if this part is hidden by default by the configuration.
     */
    public boolean isHiddenByConfiguration() {
        int[] hiddenParts = this.parentMof.getConfiguredPartsHiddenByDefault();
        return hiddenParts != null && Arrays.binarySearch(hiddenParts, getPartID()) >= 0;
    }

    /**
     * Loads the header of this MOF part from a DataReader, returning a header context object.
     * @param reader the reader to read the header from
     * @param context the shared context data to use when loading/saving data
     * @return mofPartHeader
     */
    MRMofPartHeader loadHeader(DataReader reader, MRStaticMofDataContext context) {
        MRMofPartHeader newHeader = new MRMofPartHeader(this, context);
        newHeader.load(reader);
        return newHeader;
    }

    /**
     * Saves the header of this MOF part to a DataWriter, returning a header context object.
     * @param writer the writer to write the header to
     * @param context the shared context data to use when loading/saving data
     * @return mofPartHeader
     */
    MRMofPartHeader saveHeader(DataWriter writer, MRStaticMofDataContext context) {
        MRMofPartHeader newHeader = new MRMofPartHeader(this, context);
        newHeader.save(writer);
        return newHeader;
    }

    /**
     * Represents a MRMofPart's header data.
     */
    @RequiredArgsConstructor
    public static class MRMofPartHeader implements IBinarySerializable {
        private final MRMofPart mofPart;
        @NonNull private final MRStaticMofDataContext context; // Shared across all parts.
        private int flags;
        private int partCelCount;
        private int vertexCount;
        private int normalCount;
        private int primitiveCount;
        private int hiliteCount;
        private int partCelPointer;
        private int primitivePointer;
        private int hilitePointer;
        private int collPrimPointer;
        private int collPrimMatrixPointer;
        private int animatedTexturePointer;
        private int flipbookPointer;


        @Override
        public void load(DataReader reader) {
            this.flags = reader.readShort();
            this.mofPart.warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);

            this.partCelCount = reader.readUnsignedShortAsInt();
            this.vertexCount = reader.readUnsignedShortAsInt();
            this.normalCount = reader.readUnsignedShortAsInt();
            this.primitiveCount = reader.readUnsignedShortAsInt();
            this.hiliteCount = reader.readUnsignedShortAsInt();

            this.partCelPointer = reader.readInt();
            this.primitivePointer = reader.readInt();
            this.hilitePointer = reader.readInt(); // May be null
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Run-time value.
            this.collPrimPointer = reader.readInt(); // May be null.
            this.collPrimMatrixPointer = reader.readInt(); // May be null.
            this.animatedTexturePointer = reader.readInt(); // (Point to integer which is count.) Followed by: MR_PART_POLY_ANIM
            this.flipbookPointer = reader.readInt(); // MR_PART_FLIPBOOK (MR_PART_FLIPBOOK_ACTION may follow?)
        }

        @Override
        @SuppressWarnings("ExtractMethodRecommender")
        public void save(DataWriter writer) {
            int verticeCount = this.mofPart.getStaticPartcel().getVertices().size();
            int normalCount = this.mofPart.getStaticPartcel().getNormals().size();
            for (MRMofPartCel partCel : this.mofPart.getPartCels()) { // Extra safety check, so if this somehow happens we won't be baffled by the in-game results.
                if (verticeCount != partCel.getVertices().size())
                    throw new RuntimeException("Not all of the partcels in part #" + this.mofPart.getPartID() + " had the same number of vertices! (" + verticeCount + ", " + partCel.getVertices().size() + ")");
                if (normalCount != partCel.getNormals().size())
                    throw new RuntimeException("Not all of the partcels in part #" + this.mofPart.getPartID() + " had the same number of normals! (" + normalCount + ", " + partCel.getNormals().size() + ")");
            }

            writer.writeUnsignedShort(this.mofPart.generateBitFlags());
            writer.writeUnsignedShort(this.mofPart.partCels.size());
            writer.writeUnsignedShort(verticeCount);
            writer.writeUnsignedShort(normalCount);
            writer.writeUnsignedShort(this.mofPart.getOrderedPolygons().size()); // PrimitiveCount.
            writer.writeUnsignedShort(this.mofPart.getHilites().size());

            this.partCelPointer = writer.writeNullPointer();
            this.primitivePointer = writer.writeNullPointer();
            this.hilitePointer = writer.writeNullPointer();
            writer.writeInt(0); // Runtime value.
            this.collPrimPointer = writer.writeNullPointer();
            this.collPrimMatrixPointer = writer.writeNullPointer();
            this.animatedTexturePointer = writer.writeNullPointer();
            this.flipbookPointer = writer.writeNullPointer();
        }

        /**
         * Loads the body of the MOF part.
         * @param reader The reader to load the body from.
         */
        public void loadBody(DataReader reader) {
            MRMofPart counterpart;
            boolean isIncomplete = this.mofPart.getParentMof().getModel().isIncomplete();
            if (isIncomplete) {
                if (this.mofPart.getParentMof().getModel().getCompleteCounterpart() == null)
                    throw new RuntimeException("Incomplete model is missing a counterpart!!");

                counterpart = this.mofPart.getParentMof().getModel().getCompleteCounterpart().asStaticFile().getParts().get(this.mofPart.getPartID());
            } else {
                counterpart = null;
            }

            this.mofPart.readPartCels(reader, this.partCelPointer, this.partCelCount, this.vertexCount, this.normalCount, this.context, counterpart); // Incomplete changes this.

            // Skip the bounding boxes of incomplete mofs.
            if (isIncomplete)
                while (reader.getIndex() != this.hilitePointer && reader.getIndex() != this.collPrimPointer && reader.getIndex() != this.animatedTexturePointer && reader.getIndex() != this.flipbookPointer && reader.getIndex() != this.primitivePointer)
                    new MRMofBoundingBox().load(reader);

            this.mofPart.readHilites(reader, this.hilitePointer, this.hiliteCount, counterpart); // Appears to be the same even when incomplete.
            this.mofPart.readCollPrims(reader, this.collPrimPointer, counterpart);  // It is unknown if being incomplete impacts this.
            int matrixDataEndPointer = calculateStartOfSectionAfterMatrixData(this.collPrimMatrixPointer, this.animatedTexturePointer, this.flipbookPointer, this.primitivePointer);
            this.mofPart.readCollPrimMatrices(reader, this.collPrimMatrixPointer, matrixDataEndPointer, counterpart); // It is unknown if being incomplete impacts this.
            int textureAnimationDataEndPointer = calculateStartOfSectionAfterTextureAnimationData(this.animatedTexturePointer, this.flipbookPointer, this.primitivePointer);
            this.mofPart.readTextureAnimations(reader, this.animatedTexturePointer, textureAnimationDataEndPointer, counterpart); // It is unknown if being incomplete impacts this.
            this.mofPart.readFlipbook(reader, this.flipbookPointer, counterpart); // Appears to be the same even when incomplete.
            this.mofPart.readPrimitives(reader, this.primitivePointer, this.primitiveCount); // Appears to be the same even when incomplete.

            int generatedFlags = this.mofPart.generateBitFlags();
            if (this.flags != generatedFlags)
                throw new RuntimeException("Expected Flags do not match the flags actually seen. (Read: " + this.flags + ", Expected: " + generatedFlags + ")");

            clear();
        }

        /**
         * Saves the body of the MOF part.
         * @param writer The writer to save the body to.
         */
        public void saveBody(DataWriter writer) {
            boolean incompleteMof = this.mofPart.getParentMof().getModel().isIncomplete();
            this.mofPart.writePartCels(writer, this.partCelPointer, this.context, incompleteMof);
            this.mofPart.writeHilites(writer, this.hilitePointer); // Appears to be the same even when incomplete.
            this.mofPart.writeCollPrims(writer, this.collPrimPointer); // It is unknown if being incomplete impacts this.
            this.mofPart.writeCollPrimMatrices(writer, this.collPrimMatrixPointer); // It is unknown if being incomplete impacts this.
            this.mofPart.writeTextureAnimations(writer, this.animatedTexturePointer); // It is unknown if being incomplete impacts this.
            this.mofPart.writeFlipbook(writer, this.flipbookPointer); // Appears to be the same even when incomplete.
            this.mofPart.writePrimitives(writer, this.primitivePointer); // Appears to be the same even when incomplete.
        }

        /**
         * Clears the data.
         */
        public void clear() {
            this.flags = 0;
            this.partCelCount = 0;
            this.vertexCount = 0;
            this.normalCount = 0;
            this.primitiveCount = 0;
            this.hiliteCount = 0;
            this.partCelPointer = 0;
            this.primitivePointer = 0;
            this.hilitePointer = 0;
            this.collPrimPointer = 0;
            this.collPrimMatrixPointer = 0;
            this.animatedTexturePointer = 0;
            this.flipbookPointer = 0;
        }
    }

    private void readPartCels(DataReader reader, int partCelPointer, int partCelCount, int vertexCount, int normalCount, MRStaticMofDataContext context, MRMofPart completeMofPart) {
        if (completeMofPart != null) {
            // Incomplete mofs use partCels from their counterpart.
            this.partCels = completeMofPart.partCels;
            requireReaderIndex(reader, partCelPointer, "Expected incomplete/empty partCel data to start");
            reader.skipBytes(partCelCount * MRMofPartCel.SIZE_IN_BYTES);
            if (this.partCels.size() != partCelCount)
                getLogger().warning("Incomplete mof expected %d partCels, but %d were actually linked.", partCelCount, this.partCels.size());

            return;
        }

        this.partCels.clear();
        if (partCelCount <= 0)
            return;

        // Read PartCel headers. (They're placed after the vertex/normal buffer).
        reader.jumpTemp(partCelPointer);
        for (int i = 0; i < partCelCount; i++) {
            MRMofPartCel partCel = new MRMofPartCel(this);
            partCel.load(reader);
            this.partCels.add(partCel);
        }
        int partCelDataEndIndex = reader.getIndex();
        reader.jumpReturn();

        // PartCel data for ALL parts can be found in the first part.
        // Incomplete MOFs completely omit partCel data.
        boolean incompleteMof = this.parentMof.getModel().isIncomplete();
        if (!incompleteMof && this.parentMof.getParts().size() > 0 && this == this.parentMof.getParts().get(0)) {
            if (this.partCels.size() > 0) { // Perform sanity check when feasible.
                int vertexPointer = this.partCels.get(0).getVertexPointer();
                requireReaderIndex(reader, vertexPointer, "Expected start vertexPointer (for all parts)");
            }

            // Load all remaining vertices.
            while (partCelPointer > reader.getIndex())
                context.getPartCelVectors().readElement(reader);
        }

        // Load vertices & normals for the active partCels.
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).loadVertices(vertexCount, context);
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).loadNormals(normalCount, context);

        requireReaderIndex(reader, partCelPointer, "Expected partCel data to start");
        reader.setIndex(partCelDataEndIndex);

        // Read PartCel bounding boxes.
        Map<Integer, MRMofBoundingBox> previousBoundingBoxes = new HashMap<>();
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).loadBoundingBox(reader, previousBoundingBoxes);
    }

    private void writePartCels(DataWriter writer, int partcelPointer, MRStaticMofDataContext context, boolean incompleteMof) {
        if (this.partCels.isEmpty())
            return;

        // Write ALL PartCel buffers to the first part to mimic the official game.
        if (!incompleteMof && this.parentMof.getParts().size() > 0 && this == this.parentMof.getParts().get(0)) {
            // Save vertices.
            for (int partIndex = 0; partIndex < this.parentMof.getParts().size(); partIndex++) {
                MRMofPart mofPart = this.parentMof.getParts().get(partIndex);
                for (int i = 0; i < mofPart.getPartCels().size(); i++)
                    mofPart.getPartCels().get(i).saveVertices(writer, context);
            }

            // Save normals.
            for (int partIndex = 0; partIndex < this.parentMof.getParts().size(); partIndex++) {
                MRMofPart mofPart = this.parentMof.getParts().get(partIndex);
                for (int i = 0; i < mofPart.getPartCels().size(); i++)
                    mofPart.getPartCels().get(i).saveNormals(writer, context);
            }
        }

        // Write PartCel headers.
        writer.writeAddressTo(partcelPointer);
        if (incompleteMof) {
            writer.writeNull(this.partCels.size() * MRMofPartCel.SIZE_IN_BYTES);
        } else {
            for (int i = 0; i < this.partCels.size(); i++)
                this.partCels.get(i).save(writer);
        }

        // Write PartCel bounding boxes.
        Map<MRMofBoundingBox, Integer> previousBoundingBoxes = new HashMap<>();
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).saveBoundingBox(writer, previousBoundingBoxes, incompleteMof);
    }

    private void readHilites(DataReader reader, int hilitePointer, int hiliteCount, MRMofPart completeMofPart) {
        if (completeMofPart != null) {
            // Incomplete mofs use hilites from their counterpart.
            this.hilites = completeMofPart.hilites;
            if (this.hilites.size() != hiliteCount)
                getLogger().warning("Incomplete mof expected %d hilites, but %d were actually linked.", hiliteCount, this.hilites.size());

            if (hilitePointer > 0) {
                requireReaderIndex(reader, hilitePointer, "Expected incomplete/empty hilite data to start");
                reader.skipBytes(hiliteCount * MRMofHilite.SIZE_IN_BYTES);
            }

            return;
        }

        this.hilites.clear();
        if (hiliteCount <= 0)
            return;

        requireReaderIndex(reader, hilitePointer, "Expected Hilites");
        for (int i = 0; i < hiliteCount; i++) {
            MRMofHilite hilite = new MRMofHilite(this);
            hilite.load(reader);
            this.hilites.add(hilite);
        }
    }

    private void writeHilites(DataWriter writer, int hilitePointer) {
        if (this.hilites.isEmpty())
            return;

        writer.writeAddressTo(hilitePointer);
        for (int i = 0; i < this.hilites.size(); i++)
            this.hilites.get(i).save(writer);
    }

    private void readCollPrims(DataReader reader, int collPrimPointer, MRMofPart completeMofPart) {
        if (completeMofPart != null) {
            // Incomplete mofs use collPrims from their counterpart.
            this.collPrims = completeMofPart.collPrims;
            if (collPrimPointer > 0)
                requireReaderIndex(reader, collPrimPointer, "Expected empty/incomplete CollPrims");
            reader.skipBytes(this.collPrims.size() * MRMofCollprim.SIZE_IN_BYTES);
            return;
        }

        this.collPrims.clear();
        if (collPrimPointer <= 0)
            return;

        requireReaderIndex(reader, collPrimPointer, "Expected CollPrims");
        MRMofCollprim newCollPrim;
        do {
            newCollPrim = new MRMofCollprim(this);
            newCollPrim.load(reader);
            this.collPrims.add(newCollPrim);
        } while (!newCollPrim.testFlag(MRCollprim.FLAG_LAST_IN_LIST));
    }

    private void writeCollPrims(DataWriter writer, int collPrimPointer) {
        if (this.collPrims.isEmpty())
            return;

        writer.writeAddressTo(collPrimPointer);
        for (int i = 0; i < this.collPrims.size(); i++)
            this.collPrims.get(i).save(writer);
    }

    private static int calculateStartOfSectionAfterMatrixData(int matrixPointer, int animatedTexturesPointer, int flipbookPointer, int primitivePointer) {
        // Calculate the next section after the matrix one, so we can read all the matrices.
        int startOfSectionAfterMatrixPtr = Integer.MAX_VALUE;
        if (animatedTexturesPointer > matrixPointer)
            startOfSectionAfterMatrixPtr = animatedTexturesPointer;
        if (flipbookPointer > matrixPointer && flipbookPointer < startOfSectionAfterMatrixPtr)
            startOfSectionAfterMatrixPtr = flipbookPointer;
        if (primitivePointer > matrixPointer && primitivePointer < startOfSectionAfterMatrixPtr)
            startOfSectionAfterMatrixPtr = primitivePointer;

        if (startOfSectionAfterMatrixPtr == Integer.MAX_VALUE)
            throw new RuntimeException("Failed to calculate the end of MOF Part Matrix Data. (No data section found afterwards.)");

        return startOfSectionAfterMatrixPtr;
    }

    private void readCollPrimMatrices(DataReader reader, int matrixPointer, int startOfSectionAfterMatrixPtr, MRMofPart completeMofPart) {
        if (completeMofPart != null) {
            // Incomplete mofs use collPrim matrices from their counterpart.
            this.collPrimMatrices = completeMofPart.collPrimMatrices;
            if (matrixPointer > 0)
                requireReaderIndex(reader, matrixPointer, "Expected empty/incomplete CollPrim Matrices");
            reader.setIndex(startOfSectionAfterMatrixPtr);
            return;
        }

        this.collPrimMatrices.clear();
        if (matrixPointer <= 0)
            return;

        // Verify alignment to new section.
        requireReaderIndex(reader, matrixPointer, "Expected CollPrim Matrices");
        int remainderBytes = ((startOfSectionAfterMatrixPtr - matrixPointer) % PSXMatrix.BYTE_SIZE);
        if (remainderBytes != 0) {
            if (remainderBytes == 4 && getGameInstance().isBeastWars()) { // It is unclear what this data is for.
                reader.jumpTemp(startOfSectionAfterMatrixPtr - 4);
                int endValue = reader.readInt();
                reader.jumpReturn();

                if (endValue != 0)
                    throw new RuntimeException("Didn't properly calculate the end of matrix data. (Start: " + NumberUtils.toHexString(matrixPointer) + ", End: " + NumberUtils.toHexString(startOfSectionAfterMatrixPtr) + ", Remainder: " + remainderBytes + ", End Value: " + endValue + ")");
            } else {
                throw new RuntimeException("Didn't properly calculate the end of matrix data. (Start: " + NumberUtils.toHexString(matrixPointer) + ", End: " + NumberUtils.toHexString(startOfSectionAfterMatrixPtr) + ", Remainder: " + remainderBytes + ")");
            }
        }


        // Read matrix data.
        while (startOfSectionAfterMatrixPtr > reader.getIndex()) {
            PSXMatrix matrix = new PSXMatrix();
            matrix.load(reader);
            this.collPrimMatrices.add(matrix);
        }

        // Ensure placement is right
        reader.setIndex(startOfSectionAfterMatrixPtr);
    }

    private void writeCollPrimMatrices(DataWriter writer, int matrixPointer) {
        if (this.collPrimMatrices.isEmpty())
            return;

        writer.writeAddressTo(matrixPointer);
        for (int i = 0; i < this.collPrims.size(); i++)
            this.collPrimMatrices.get(i).save(writer);
    }

    private static int calculateStartOfSectionAfterTextureAnimationData(int animatedTexturesPointer, int flipbookPointer, int primitivePointer) {
        // Calculate the next section after the texture animation one, so we can read all the matrices.
        int startOfSectionAfterMatrixPtr = Integer.MAX_VALUE;
        if (flipbookPointer > animatedTexturesPointer)
            startOfSectionAfterMatrixPtr = flipbookPointer;
        if (primitivePointer > animatedTexturesPointer && primitivePointer < startOfSectionAfterMatrixPtr)
            startOfSectionAfterMatrixPtr = primitivePointer;

        if (startOfSectionAfterMatrixPtr == Integer.MAX_VALUE)
            throw new RuntimeException("Failed to calculate the end of MOF Part Texture Animation Data. (No data section found afterwards.)");

        return startOfSectionAfterMatrixPtr;
    }

    private void readTextureAnimations(DataReader reader, int partPolyAnimPointer, int startOfSectionAfterTextureAnimationData, MRMofPart completeMofPart) {
        if (completeMofPart != null) {
            // Incomplete mofs use texture animations from their counterpart.
            if (completeMofPart.getTextureAnimations().size() > 0 && (!getGameInstance().isFrogger() || !((FroggerConfig) getGameInstance().getVersionConfig()).isAtOrBeforeBuild23()))
                getLogger().warning("Texture animation cannot be copied to an incomplete MOF (from %s) due to polygon data not being shared!", completeMofPart.getParentMof().getModel().getFileDisplayName());

            /*
            requireReaderIndex(reader, partPolyAnimPointer, "Expected empty/incomplete texture animations");
            reader.skipBytesRequireEmpty(this.textureAnimations.size() * MRMofTextureAnimationPolygonTarget.SIZE_IN_BYTES);
            requireReaderIndex(reader, startOfSectionAfterTextureAnimationData, "Expected end of empty/incomplete texture animations");
             */
            reader.setIndex(startOfSectionAfterTextureAnimationData);
            return;
        }

        this.textureAnimationPolygonTargets.clear();
        this.textureAnimations.clear();
        if (partPolyAnimPointer <= 0)
            return;

        requireReaderIndex(reader, partPolyAnimPointer, "Expected texture animations");
        int animationTargetCount = reader.readInt();

        // Read texture animation targets.
        for (int i = 0; i < animationTargetCount; i++) {
            MRMofTextureAnimationPolygonTarget newTarget = new MRMofTextureAnimationPolygonTarget(this);
            newTarget.load(reader);
            this.textureAnimationPolygonTargets.add(newTarget);
        }

        // Read texture animations.
        Map<Integer, MRMofTextureAnimation> animationsByPointer = new HashMap<>();
        while (startOfSectionAfterTextureAnimationData > reader.getIndex()) {
            MRMofTextureAnimation newAnimation = new MRMofTextureAnimation(this);
            animationsByPointer.put(reader.getIndex(), newAnimation);
            newAnimation.load(reader);
            this.textureAnimations.add(newAnimation);
        }

        // Apply texture animations to the target polygons.
        for (int i = 0; i < this.textureAnimationPolygonTargets.size(); i++)
            this.textureAnimationPolygonTargets.get(i).resolveTextureAnimationPointer(animationsByPointer);
    }

    private void writeTextureAnimations(DataWriter writer, int partPolyAnimPointer) {
        if (this.textureAnimations.isEmpty() && this.textureAnimationPolygonTargets.isEmpty())
            return;

        writer.writeAddressTo(partPolyAnimPointer);
        writer.writeInt(this.textureAnimationPolygonTargets.size());

        // Write texture animation targets.
        for (int i = 0; i < this.textureAnimationPolygonTargets.size(); i++)
            this.textureAnimationPolygonTargets.get(i).save(writer);

        // Write texture animations.
        Map<MRMofTextureAnimation, Integer> pointersByAnimation = new HashMap<>();
        for (int i = 0; i < this.textureAnimations.size(); i++) {
            MRMofTextureAnimation animation = this.textureAnimations.get(i);
            pointersByAnimation.put(animation, writer.getIndex());
            animation.save(writer);
        }

        // Apply texture animations to the target polygons.
        for (int i = 0; i < this.textureAnimationPolygonTargets.size(); i++)
            this.textureAnimationPolygonTargets.get(i).saveTextureAnimationPointer(writer, pointersByAnimation);
    }

    private void readFlipbook(DataReader reader, int flipbookPointer, MRMofPart completeMofPart) {
        if (completeMofPart != null) {
            // Incomplete mofs use flipbook from their counterpart.
            this.flipbook = completeMofPart.flipbook;
            requireReaderIndex(reader, flipbookPointer, "Expected incomplete/empty flipbook data");
            new MRMofFlipbookAnimationList().load(reader); // Skip the area covered by the flipbook.
            return;
        }

        this.flipbook.getAnimations().clear();
        if (flipbookPointer <= 0)
            return;

        requireReaderIndex(reader, flipbookPointer, "Expected flipbook data");
        this.flipbook.load(reader);
    }

    private void writeFlipbook(DataWriter writer, int flipbookPointer) {
        if (this.flipbook.getAnimations().isEmpty())
            return;

        writer.writeAddressTo(flipbookPointer);
        this.flipbook.save(writer);
    }

    private void readPrimitives(DataReader reader, int primitivePointer, int primitiveCount) {
        this.polygonBlocks.clear();
        this.polygonBlocksByType.clear();
        this.orderedPolygons.clear();
        this.polygonListDirty = false;
        if (primitiveCount <= 0 && primitivePointer <= 0)
            return;

        // Read Primitives.
        // Beast Wars retail has this data, but it is guaranteed to be zero.
        if (doesVersionFormatHaveExtraZeroBeforePolygonData() && primitivePointer > reader.getIndex())
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE);

        requireReaderIndex(reader, primitivePointer, "Expected polygon data");
        while (primitiveCount > 0) {
            MRMofPolygonBlock newBlock = MRMofPolygonBlock.readBlockFromReader(this, reader);
            primitiveCount -= newBlock.getPolygons().size();
            addPolygonBlock(newBlock);
        }

        if (!this.parentMof.getModel().isIncomplete()) {
            // Resolve polygon pointers in texture animation targets.
            for (int i = 0; i < this.textureAnimationPolygonTargets.size(); i++)
                this.textureAnimationPolygonTargets.get(i).resolvePolygonID();

            // Resolve polygon pointers in MOF Hilites.
            for (int i = 0; i < this.hilites.size(); i++)
                this.hilites.get(i).resolveAttachment();
        }
    }

    private void writePrimitives(DataWriter writer, int primitivePointer) {
        /*if (doesVersionFormatHaveExtraZeroBeforePolygonData())
            writer.writeInt(0);*/ // I don't think this is necessary.

        writer.writeAddressTo(primitivePointer);

        // Write polygons.
        for (int i = 0; i < this.polygonBlocks.size(); i++)
            this.polygonBlocks.get(i).save(writer);
    }

    /**
     * Returns true iff the active format version supports interpolation flags.
     * Beast Wars PSX has it, and is guaranteed to be zero.
     */
    public boolean doesVersionFormatHaveExtraZeroBeforePolygonData() {
        if (getGameInstance().isFrogger()) {
            FroggerConfig config = (FroggerConfig) getConfig();
            return config.isAtOrBeforeBuild1(); // Doesn't seem related to MR API version, but it might be, since it jumps from 1.11a to 1.30 after this.
        } else {
            // Despite Frogger not having this beyond Build 1, Beast Wars seems to have this even in the retail version.
            return getGameInstance().getGameType().isAtOrBefore(SCGameType.BEAST_WARS);
        }
    }
}