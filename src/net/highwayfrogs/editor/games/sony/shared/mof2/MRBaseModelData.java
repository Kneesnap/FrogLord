package net.highwayfrogs.editor.games.sony.shared.mof2;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents the shared data structure between animated/non-animated MOFs.
 * Created by Kneesnap on 2/18/2025.
 */
@Getter
public abstract class MRBaseModelData extends SCSharedGameData implements IPropertyListCreator {
    @NonNull private final transient MRModel model;

    public MRBaseModelData(@NonNull MRModel model) {
        super(model.getGameInstance());
        this.model = model;
    }

    @Override
    public ILogger getLogger() {
        return this.model.getLogger();
    }

    @Override
    public final void load(DataReader reader) {
        int readStartIndex = reader.getIndex();
        byte[] signature = reader.readBytes(MOFHolder.DUMMY_DATA.length);
        int modelSizeInBytes = reader.readInt();
        int flags = reader.readInt();
        loadModelData(reader, signature);

        // Validations.
        int readBytes = reader.getIndex() - readStartIndex;
        if (readBytes != modelSizeInBytes)
            throw new RuntimeException("MOF File was " + modelSizeInBytes + " bytes large, but FrogLord actually read " + readBytes + " bytes.");

        int generatedFlags = generateBitFlags();
        if (flags != generatedFlags)
            throw new RuntimeException("Expected Flags (" + NumberUtils.toHexString(generatedFlags) + ") do not match the flags actually seen (" + NumberUtils.toHexString(flags) + ").");

        String expectedSignature = generateSignature();
        if (!expectedSignature.equals(new String(signature)) && (!(this instanceof MRStaticMof) || !((MRStaticMof) this).canVersionFormatHaveNullSignature())) // Build 1 seems to skip on the signature (but only some models).
            throw new RuntimeException("The expected MOF Signature (" + expectedSignature + ") does not match the actual MOF signature (" + new String(signature) + "/" + DataUtils.toByteString(signature) + ").");
    }

    @Override
    public final void save(DataWriter writer) {
        int writeStartIndex = writer.getIndex();
        writer.writeStringBytes(generateSignature());
        int sizeAddress = writer.writeNullPointer();
        writer.writeInt(generateBitFlags());
        saveModelData(writer);
        writer.writeIntAtPos(sizeAddress, writer.getIndex() - writeStartIndex);
    }

    /**
     * Gets this file's display name.
     */
    public String getFileDisplayName() {
        return this.model.getFileDisplayName();
    }

    /**
     * Loads the format-specific model data from the reader.
     * @param reader The reader to read data from.
     */
    protected abstract void loadModelData(DataReader reader, byte[] signature);

    /**
     * Saves the format-specific model data from to the writer.
     * @param writer The writer to write data to.
     */
    protected abstract void saveModelData(DataWriter writer);

    /**
     * Calculates the bit flags used to configure the MOF data.
     */
    public abstract int generateBitFlags();

    /**
     * Generates the signature that goes at the start of this file identifying the file contents.
     */
    public abstract String generateSignature();
}
