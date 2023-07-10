package net.highwayfrogs.editor.games.tgq.model;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQBinFile;
import net.highwayfrogs.editor.games.tgq.TGQFile;
import net.highwayfrogs.editor.games.tgq.TGQImageFile;
import net.highwayfrogs.editor.games.tgq.toc.TGQChunkTextureReference;
import net.highwayfrogs.editor.utils.Utils;

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
            System.out.println("The model '" + getExportName() + "' was supposed to have " + size + " bytes, but actually has " + reader.getRemaining() + " bytes.");

        this.model.load(reader);
        if (reader.hasMore())
            System.out.println("The model '" + getExportName() + "' has " + reader.getRemaining() + " unread bytes.");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE_STR);
        int sizePos = writer.writeNullPointer();
        this.model.save(writer);
        writer.writeAddressAt(sizePos, writer.getIndex() - sizePos - Constants.INTEGER_SIZE);
    }

    @Override
    public void afterLoad2() {
        super.afterLoad2();

        // Apply file names to all materials.
        // We need to do this both when a texture reference loads and when the model loads, so regardless of if this particular model loads before or after the texture it will still get the texture names.
        kcMaterial.resolveMaterialTextures(getMainArchive(), this.model.getMaterials());
    }

    /**
     * Loads material textures by searching for textures in a chunked file.
     * This should be called by texture references in the same chunk as a model reference, because it will overwrite any existing textures if a match is found.
     * @param imageFile The chunk to search.
     */
    public void resolveMaterialTextures(TGQChunkTextureReference texRef, TGQImageFile imageFile) {
        if (imageFile == null)
            return;

        // Find material(s).
        // We need to do this both when a texture reference loads and when the model loads, so regardless of if this particular model loads before or after the texture it will still get the texture names.
        // Images are fully loaded before afterLoad() is called, so it's OK to skip
        String strippedName = Utils.stripExtension(texRef.getName());
        for (kcMaterial material : this.model.getMaterials())
            if (Utils.stripExtension(material.getTextureFileName()).equals(strippedName))
                material.setTexture(imageFile);
    }
}
