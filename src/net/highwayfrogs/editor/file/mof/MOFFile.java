package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a MOF file.
 * Some files are off by 4-16 bytes. This is caused by us merging prim banks of matching types, meaning the header that was there before is removed.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFFile extends MOFBase {
    private byte[] bytes;
    private final List<MOFPart> parts = new ArrayList<>();
    private int unknownValue; // As far as I can tell, this value is unused. It might be a checksum of some kind, really I have no information to go off. Keeping it at zero should be fine for most purposes.

    public static final String SIGNATURE = "\2FOM";
    private static final int INCOMPLETE_TEST_ADDRESS = 0x1C;
    private static final int INCOMPLETE_TEST_VALUE = 0x40;
    public static final ImageFilterSettings MOF_EXPORT_FILTER = new ImageFilterSettings(ImageState.EXPORT)
            .setTrimEdges(true).setAllowTransparency(true).setAllowFlip(true);

    public MOFFile(SCGameInstance instance, MOFHolder holder) {
        super(instance, holder);
    }

    @Override
    public void onLoad(DataReader reader, byte[] signature) {
        int partCount = reader.readInt();

        reader.jumpTemp(INCOMPLETE_TEST_ADDRESS);
        boolean isIncomplete = (reader.readInt() == INCOMPLETE_TEST_VALUE);
        reader.jumpReturn();

        if (isIncomplete) { // Just copy the MOF directly.
            getHolder().setIncomplete(true);
            reader.jumpTemp(0);
            this.bytes = reader.readBytes(reader.getRemaining());
            reader.jumpReturn();

            String oldName = Utils.stripExtensionWin95(getHolder().getCompleteMOF().getIndexEntry().getDisplayName());
            String newName = Utils.stripExtensionWin95(getFileEntry().getDisplayName());
            getConfig().getAnimationBank().linkChildBank(oldName, newName); // Link animation names.
        }

        for (int i = 0; i < partCount; i++) {
            MOFPart part = new MOFPart(this);
            part.load(reader);
            if (isIncomplete)
                getHolder().getCompleteMOF().asStaticFile().getParts().get(i).copyToIncompletePart(part);

            parts.add(part);
        }

        this.unknownValue = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        if (getHolder().isIncomplete() && this.bytes != null) { // If the MOF is incomplete, save the incomplete mof.
            writer.writeBytes(this.bytes);
            return;
        }

        super.save(writer);
    }

    @Override
    public void onSave(DataWriter writer) {
        writer.writeInt(getParts().size());
        getParts().forEach(part -> part.save(writer));
        writer.writeInt(this.unknownValue);
        getParts().forEach(part -> part.saveExtra(writer));
    }

    /**
     * Run some behavior on each mof polygon.
     * @param handler The behavior to run.
     */
    public void forEachPolygon(Consumer<MOFPolygon> handler) {
        getParts().forEach(part -> part.getMofPolygons().values().forEach(list -> list.forEach(handler)));
    }

    /**
     * Gets all of the polygons within this model.
     */
    public List<MOFPolygon> getAllPolygons() {
        List<MOFPolygon> polygons = new ArrayList<>();
        for (MOFPart part : getParts())
            for (List<MOFPolygon> list : part.getMofPolygons().values())
                polygons.addAll(list);
        return polygons;
    }

    /**
     * Check if this file has any texture animations.
     * @return hasTextureAnimation
     */
    public boolean hasTextureAnimation() {
        for (MOFPart part : getParts())
            if (!part.getPartPolyAnims().isEmpty())
                return true;
        return false;
    }

    /**
     * Gets the number of hilites this model has.
     * @return hiliteCount
     */
    public int getHiliteCount() {
        int totalHilites = 0;
        for (int i = 0; i < getParts().size(); i++)
            totalHilites += getParts().get(i).getHilites().size();
        return totalHilites;
    }

    /**
     * Gets the number of collprims in this model.
     * @return collprimCount
     */
    public int getCollprimCount() {
        int totalCollprim = 0;
        for (int i = 0; i < getParts().size(); i++)
            if (getParts().get(i).getCollprim() != null)
                totalCollprim++;
        return totalCollprim;
    }

    @Override
    public int buildFlags() {
        int flags = 0; // Bits 0, 1, 2 are runtime only.
        if (getParts().stream().anyMatch(part -> part.getPartPolyAnims().size() > 0))
            flags |= Constants.BIT_FLAG_4; // Bit 4 is texture animation.
        if (getParts().stream().anyMatch(part -> part.getFlipbook() != null))
            flags |= Constants.BIT_FLAG_5; // Bit 5 is flipbook animation.
        return flags;
    }

    @Override
    public String makeSignature() {
        return SIGNATURE; // Seems to be constant.
    }
}