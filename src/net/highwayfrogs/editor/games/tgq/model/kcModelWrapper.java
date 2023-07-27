package net.highwayfrogs.editor.games.tgq.model;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQBinFile;
import net.highwayfrogs.editor.games.tgq.TGQFile;
import net.highwayfrogs.editor.games.tgq.loading.kcLoadContext;

/**
 * Represents a file containing a kcModel.
 * Created by Kneesnap on 6/28/2023.
 */
@Getter
public class kcModelWrapper extends TGQFile {
    private final kcModel model;

    public static final String SIGNATURE_STR = "6YTV";

    public kcModelWrapper(TGQBinFile mainArchive) {
        this(mainArchive, new kcModel());
    }

    public kcModelWrapper(TGQBinFile mainArchive, kcModel model) {
        super(mainArchive);
        this.model = model;
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE_STR);
        int size = reader.readInt();

        if (size != reader.getRemaining())
            System.out.println("The model '" + getDebugName() + "' was supposed to have " + size + " bytes, but actually has " + reader.getRemaining() + " bytes.");

        this.model.load(reader);
        if (reader.hasMore())
            System.out.println("The model '" + getDebugName() + "' has " + reader.getRemaining() + " unread bytes.");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE_STR);
        int sizePos = writer.writeNullPointer();
        this.model.save(writer);
        writer.writeAddressAt(sizePos, writer.getIndex() - sizePos - Constants.INTEGER_SIZE);
    }

    @Override
    public String getExtension() {
        return "vtx";
    }

    @Override
    public void afterLoad2(kcLoadContext context) {
        super.afterLoad2(context);

        // Apply file names to all materials.
        // We need to do this both when a texture reference loads and when the model loads, so regardless of if this particular model loads before or after the texture it will still get the texture names.
        context.getMaterialLoadContext().resolveMaterialTexturesGlobally(this, this.model.getMaterials());
    }
}