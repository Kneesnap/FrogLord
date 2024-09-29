package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;

/**
 * A reference to a 3D model.
 * Created by Kneesnap on 6/28/2023.
 */
@Getter
public class kcCResourceModel extends kcCResource {
    private String fullPath; // Full file path to the real model file.

    public static final int FULL_PATH_SIZE = 260;

    public kcCResourceModel(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.MODEL);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.fullPath = reader.readNullTerminatedFixedSizeString(FULL_PATH_SIZE); // Don't read with the padding byte, as the padding bytes are only valid when the buffer is initially created, if the is shrunk (Such as GOOBER.VTX in 00.dat), after the null byte, the old bytes will still be there.

        GreatQuestAssetBinFile mainArchive = getMainArchive();
        if (mainArchive != null)
            mainArchive.applyFileName(this.fullPath, false);

    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeNullTerminatedFixedSizeString(this.fullPath, FULL_PATH_SIZE, GreatQuestInstance.PADDING_BYTE_CD);
    }

    @Override
    public void afterLoad1(kcLoadContext context) {
        super.afterLoad1(context);
        // We must wait until afterLoad1() because the file object won't exist for files found later in the file if we don't.
        // But, this must run before afterLoad2() because that's when we start doing lookups based on file paths.
        kcModelWrapper wrapper = getModelWrapper();
        if (wrapper != null)
            context.getMaterialLoadContext().applyLevelTextureFileNames(getParentFile(), this.fullPath, wrapper.getModel().getMaterials());
    }

    @Override
    public void afterLoad2(kcLoadContext context) {
        super.afterLoad2(context);
        // Now we'll resolve the textures for this model using the textures found in the chunked file.
        // We don't need to print a warning if the model doesn't exist, because the applyFileName() call would have already caught it.
        kcModelWrapper wrapper = getModelWrapper();
        if (wrapper != null)
            context.getMaterialLoadContext().resolveMaterialTexturesInChunk(getParentFile(), wrapper, wrapper.getModel().getMaterials());
    }

    /**
     * Gets the file name without the full path.
     */
    public String getFileName() {
        int lastBackslash = this.fullPath.lastIndexOf('\\');
        if (lastBackslash == -1)
            return this.fullPath;

        return this.fullPath.substring(lastBackslash + 1);
    }

    /**
     * Gets the 3D this model stands in for.
     * @return model
     */
    public kcModel getModel() {
        GreatQuestArchiveFile file = getOptionalFileByName(this.fullPath);
        return (file instanceof kcModelWrapper) ? ((kcModelWrapper) file).getModel() : null;
    }

    /**
     * Gets the wrapper around the 3D model.
     * @return modelWrapper
     */
    public kcModelWrapper getModelWrapper() {
        GreatQuestArchiveFile file = getOptionalFileByName(this.fullPath);
        return (file instanceof kcModelWrapper) ? ((kcModelWrapper) file) : null;
    }
}