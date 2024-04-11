package net.highwayfrogs.editor.games.greatquest.model;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.IFileExport;
import net.highwayfrogs.editor.games.greatquest.TGQBinFile;
import net.highwayfrogs.editor.games.greatquest.TGQFile;
import net.highwayfrogs.editor.games.greatquest.loading.kcLoadContext;

import java.io.File;
import java.io.IOException;

/**
 * Represents a file containing a kcModel.
 * Created by Kneesnap on 6/28/2023.
 */
@Getter
public class kcModelWrapper extends TGQFile implements IFileExport {
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

        // Generate texture file names. This is done in afterLoad2() to wait for our own file path to be set.
        context.getMaterialLoadContext().applyLevelTextureFileNames(this, getFilePath(), this.model.getMaterials());

        // Apply file names to all materials.
        // We need to do this both when a texture reference loads and when the model loads, so regardless of if this particular model loads before or after the texture it will still get the texture names.
        context.getMaterialLoadContext().resolveMaterialTexturesGlobally(this, this.model.getMaterials());
    }

    @Override
    public String getDefaultFolderName() {
        return "Models";
    }

    @Override
    public void exportToFolder(File folder) throws IOException {
        if (this.model == null)
            return;

        this.model.saveToFile(folder, getExportName());
    }
}