package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.TGQFile;
import net.highwayfrogs.editor.games.tgq.TGQImageFile;
import net.highwayfrogs.editor.games.tgq.model.kcModel;
import net.highwayfrogs.editor.games.tgq.model.kcModelWrapper;

/**
 * A reference to a 3D model.
 * Created by Kneesnap on 6/28/2023.
 */
@Getter
public class kcCResourceModel extends kcCResource {
    private String fullPath; // Full file path to the real model file.

    public static final int FULL_PATH_SIZE = 260;
    public static final byte FULL_NAME_PADDING = (byte) 0xCD;

    public kcCResourceModel(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.MODEL);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.fullPath = reader.readTerminatedStringOfLength(FULL_PATH_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeTerminatedStringOfLength(this.fullPath, FULL_PATH_SIZE);
    }

    @Override
    public void afterLoad1() {
        super.afterLoad1();
        // We must wait until afterLoad1() because the file object won't exist for files found later in the file if we don't.
        // But, this must run before afterLoad2() because that's when we start doing lookups based on file paths.
        if (getParentFile() != null) {
            getParentFile().getMainArchive().applyFileName(this.fullPath);
            getFileByName(this.fullPath); // Show file missing if it's not here.
        }
    }

    /**
     * Gets the 3D this model stands in for.
     * @return model
     */
    public kcModel getModel() {
        TGQFile file = getOptionalFileByName(this.fullPath);
        return (file instanceof kcModelWrapper) ? ((kcModelWrapper) file).getModel() : null;
    }

    /**
     * Gets the wrapper around the 3D model.
     * @return modelWrapper
     */
    public kcModelWrapper getModelWrapper() {
        TGQFile file = getOptionalFileByName(this.fullPath);
        return (file instanceof kcModelWrapper) ? ((kcModelWrapper) file) : null;
    }

    /**
     * Loads material textures by searching for textures in a chunked file.
     * This should be called by texture references in the same chunk as a model reference, because it will overwrite any existing textures if a match is found.
     * @param imageFile The chunk to search.
     */
    public void resolveMaterialTextures(TGQChunkTextureReference texRef, TGQImageFile imageFile) {
        if (imageFile == null)
            return;

        kcModelWrapper wrapper = getModelWrapper();
        if (wrapper != null)
            wrapper.resolveMaterialTextures(texRef, imageFile);
    }
}