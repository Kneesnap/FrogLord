package net.highwayfrogs.editor.games.sony.shared.mof2.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRBaseModelData;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart.MRMofPartHeader;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the contents of a static MOF.
 * Created by Kneesnap on 2/18/2025.
 */
@Getter
public class MRStaticMof extends MRBaseModelData {
    private final List<MRMofPart> parts = new ArrayList<>();
    // TODO: WHAT IS UNKNOWN VALUE??
    private int unknownValue; // As far as I can tell, this value is unused. Highest bytes are always 0x0A 00, but the others are idk. It might be a checksum of some kind, really I have no information to go off. Keeping it at zero should be fine for most purposes.

    public static final String OLD_SIGNATURE = "\0\0\0\0";
    public static final byte[] OLD_SIGNATURE_BYTES = OLD_SIGNATURE.getBytes(StandardCharsets.US_ASCII);
    public static final String SIGNATURE = "\2FOM"; // MOF v2
    public static final byte[] SIGNATURE_BYTES = SIGNATURE.getBytes(StandardCharsets.US_ASCII);
    private static final int INCOMPLETE_TEST_ADDRESS = 0x1C;
    private static final int INCOMPLETE_TEST_VALUE = 0x40;

    public static final int FLAG_HAS_ANIMATED_TEXTURES = Constants.BIT_FLAG_4;
    public static final int FLAG_HAS_FLIPBOOK_ANIMATION = Constants.BIT_FLAG_5;

    public MRStaticMof(MRModel model) {
        super(model);
    }

    @Override
    protected void loadModelData(DataReader reader, byte[] signature) {
        int partCount = reader.readInt();

        reader.jumpTemp(INCOMPLETE_TEST_ADDRESS);
        boolean isIncomplete = (reader.readInt() == INCOMPLETE_TEST_VALUE);
        reader.jumpReturn();

        if (isIncomplete) { // Just copy the MOF directly.
            getModel().setIncomplete(true);
            String oldName = SCUtils.stripExtensionWin95(getModel().getCompleteCounterpart().getFileDisplayName());
            String newName = SCUtils.stripExtensionWin95(getFileDisplayName());
            getConfig().getAnimationBank().linkChildBank(oldName, newName); // Link animation names.
        }

        // Read part headers.
        this.parts.clear();
        MRMofPartHeader[] headers = new MRMofPartHeader[partCount];
        MRStaticMofDataContext context = new MRStaticMofDataContext();
        for (int i = 0; i < partCount; i++) {
            MRMofPart part = new MRMofPart(this);
            headers[i] = part.loadHeader(reader, context);
            if (isIncomplete)
                getModel().getCompleteCounterpart().asStaticFile().getParts().get(i).copyToIncompletePart(part);

            this.parts.add(part);
        }

        // Read unknown value.
        this.unknownValue = reader.readInt();

        // Read part bodies.
        for (int i = 0; i < headers.length; i++)
            headers[i].loadBody(reader);

        // Validate context.
        context.getPartCelVectors().printLoadWarningsForUnusedData(getLogger());
    }

    @Override
    protected void saveModelData(DataWriter writer) {
        // Read header data.
        writer.writeInt(this.parts.size());
        MRMofPartHeader[] headers = new MRMofPartHeader[this.parts.size()];
        MRStaticMofDataContext context = new MRStaticMofDataContext();
        for (int i = 0; i < this.parts.size(); i++)
            headers[i] = this.parts.get(i).saveHeader(writer, context);

        // Read body data.
        writer.writeInt(this.unknownValue);
        for (int i = 0; i < headers.length; i++)
            headers[i].saveBody(writer);
    }

    /**
     * Gets all polygons within this model.
     */
    public List<MRMofPolygon> getAllPolygons() {
        List<MRMofPolygon> polygons = new ArrayList<>();
        for (MRMofPart part : this.parts)
            polygons.addAll(part.getOrderedPolygons());
        return polygons;
    }

    /**
     * Get the number of flipbook animations this model has.
     * @return flipbookAnimationCount
     */
    public int getFlipbookAnimationCount() {
        int animationCount = 0;
        for (int i = 0; i < this.parts.size(); i++)
            animationCount += this.parts.get(i).getFlipbook().getAnimations().size();
        return animationCount;
    }

    /**
     * Get the number of texture animations this model has.
     * @return textureAnimationCount
     */
    public int getTextureAnimationCount() {
        int animationCount = 0;
        for (int i = 0; i < this.parts.size(); i++)
            animationCount += this.parts.get(i).getTextureAnimations().size();
        return animationCount;
    }

    /**
     * Gets the number of hilites this model has.
     * @return hiliteCount
     */
    public int getHiliteCount() {
        int totalHilites = 0;
        for (int i = 0; i < this.parts.size(); i++)
            totalHilites += this.parts.get(i).getHilites().size();
        return totalHilites;
    }

    /**
     * Gets the number of collprims in this model.
     * @return collprimCount
     */
    public int getCollprimCount() {
        int totalCollprims = 0;
        for (int i = 0; i < this.parts.size(); i++)
            totalCollprims += this.parts.get(i).getCollPrims().size();

        return totalCollprims;
    }

    @Override
    public int generateBitFlags() {
        int flags = 0; // Bits 0, 1, 2 are runtime only.
        if (this.parts.stream().anyMatch(part -> part.getTextureAnimations().size() > 0))
            flags |= FLAG_HAS_ANIMATED_TEXTURES;
        if (this.parts.stream().anyMatch(part -> part.getFlipbook() != null && part.getFlipbook().getAnimations().size() > 0))
            flags |= FLAG_HAS_FLIPBOOK_ANIMATION;
        return flags;
    }

    @Override
    public String generateSignature() {
        return SIGNATURE; // Seems to be constant. I like keeping this for old versions, even ones which sometimes use OLD_SIGNATURE.
    }

    /**
     * Gets an array of parts configured to be hidden by default.
     * If there are none configured, null is returned.
     */
    public int[] getConfiguredPartsHiddenByDefault() {
        return getConfig().getHiddenPartIds().get(getFileDisplayName());
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Parts", this.parts.size());
        propertyList.add("Partcels", this.parts.stream().mapToInt(part -> part.getPartCels().size()).sum());
        propertyList.add("Flipbook Animations", getFlipbookAnimationCount());
        propertyList.add("Texture Animations", getTextureAnimationCount());
        propertyList.add("Hilites", getHiliteCount());
        propertyList.add("Collprims", getCollprimCount());

        // TODO: Recursively go through and add properties.
        // TODO: I'd like to finally add the sub-category properly-list building API. I'd like to have some stats here about the static MOF as a whole, then break down into individual parts.
        return propertyList;
    }

    /**
     * Returns true iff the active format version supports interpolation flags.
     */
    public boolean canVersionFormatHaveNullSignature() {
        return canVersionFormatHaveNullSignature(getGameInstance());
    }

    /**
     * Returns true iff the active format version supports interpolation flags.
     */
    public static boolean canVersionFormatHaveNullSignature(SCGameInstance gameInstance) {
        if (gameInstance.isFrogger()) {
            FroggerConfig config = (FroggerConfig) gameInstance.getVersionConfig();
            return config.isAtOrBeforeBuild1(); // MR API version 1.30. (Build 1 is the last still using 1.11a, upgrading directly to 1.30)
        } else {
            return gameInstance.getGameType().isBefore(SCGameType.FROGGER);
        }
    }
}
