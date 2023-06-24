package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.TGQFile;
import net.highwayfrogs.editor.games.tgq.TGQImageFile;
import net.highwayfrogs.editor.games.tgq.model.kcMaterial;
import net.highwayfrogs.editor.games.tgq.model.kcModel;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a 3D model.
 * This file was fully understood by looking at the debug symbols on the PS2 PAL version, specifically the "kcModelPrepare" function.
 * Created by Kneesnap on 3/23/2020.
 */
@Getter
public class TGQChunk3DModel extends kcCResource {
    private String referenceName; // If this is a reference.
    private String fullReferenceName;
    private kcModel model;

    public static final int NAME_SIZE = 32;
    public static final int FULL_NAME_SIZE = 260;
    public static final byte FULL_NAME_PADDING = (byte) 0xCD;

    public TGQChunk3DModel(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.MODEL);
    }

    @Override
    public void load(DataReader reader) {
        readRawData(reader);

        if (!isRootChunk()) { // Reference. (TODO: Probably make a separate class for text reference and the real one.)
            this.referenceName = reader.readTerminatedStringOfLength(NAME_SIZE);
            this.fullReferenceName = reader.readTerminatedStringOfLength(FULL_NAME_SIZE);
            return;
        }

        this.model = new kcModel();
        this.model.load(reader);
    }

    @Override
    public void afterLoad() {
        super.afterLoad();

        if (getParentFile() == null)
            return;

        if (this.model == null) {
            if (this.fullReferenceName != null)
                getParentFile().getMainArchive().applyFileName(this.fullReferenceName);
            return;
        }

        // Find materials.
        Map<String, kcMaterial> materialsByName = new HashMap<>();
        for (kcMaterial material : this.model.getMaterials())
            materialsByName.put(Utils.stripExtension(material.getTextureFileName()), material);

        // Apply file names to all materials.
        for (TGQFile file : getParentFile().getMainArchive().getFiles()) {
            if (file instanceof TGQChunkedFile) {
                for (kcCResource resource : ((TGQChunkedFile) file).getChunks()) {
                    if (!(resource instanceof TGQChunkTextureReference))
                        continue;

                    TGQChunkTextureReference texRef = (TGQChunkTextureReference) resource;
                    TGQFile texRefFile = getParentFile().getMainArchive().getFileByName(texRef.getPath());
                    if (texRefFile != null && texRefFile.getRawName() == null)
                        texRefFile.setRawName(texRef.getPath());

                    kcMaterial material = materialsByName.get(Utils.stripExtension(texRef.getName()));
                    if (material != null && texRefFile instanceof TGQImageFile)
                        material.setTexture((TGQImageFile) texRefFile);
                }
            }
        }
    }

    /**
     * Loads material textures by searching for textures in a chunked file.
     * @param imageFile The chunk to search.
     */
    public void load(TGQChunkTextureReference texRef, TGQImageFile imageFile) {
        if (this.model == null || imageFile == null)
            return;

        // Find material(s).
        String strippedName = Utils.stripExtension(texRef.getName());
        for (kcMaterial material : this.model.getMaterials())
            if (material.getTexture() == null && Utils.stripExtension(material.getTextureFileName()).equals(strippedName))
                material.setTexture(imageFile);
    }

    @Override
    public void save(DataWriter writer) {
        if (isRootChunk()) {
            this.model.save(writer);
        } else {
            writer.writeTerminatedStringOfLength(this.referenceName, NAME_SIZE);
            writer.writeTerminatedStringOfLength(this.fullReferenceName, FULL_NAME_SIZE);
        }
    }
}
