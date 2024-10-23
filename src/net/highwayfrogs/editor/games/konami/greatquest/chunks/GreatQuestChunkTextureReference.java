package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a texture reference.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@Setter
public class GreatQuestChunkTextureReference extends kcCResource {
    private String path;

    private static final int PATH_SIZE = 260;

    public GreatQuestChunkTextureReference(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.TEXTURE);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.path = reader.readNullTerminatedFixedSizeString(PATH_SIZE); // Don't read with the padding byte, as the padding bytes are only valid when the buffer is initially created, if the is shrunk (Such as shadow.img in 00.dat), after the null byte, the old bytes will still be there.

        // Apply the file name.
        GreatQuestAssetBinFile mainArchive = getMainArchive();
        if (mainArchive != null)
            mainArchive.applyFileName(this.path, false);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeNullTerminatedFixedSizeString(this.path, PATH_SIZE, GreatQuestInstance.PADDING_BYTE_CD);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("File Path", this.path); // TODO: Allow changing. (Make sure to validate the hash though!)
        return propertyList;
    }

    /**
     * Gets the images referenced by this chunk.
     */
    public GreatQuestImageFile getReferencedImage() {
        GreatQuestArchiveFile texRefFile = getGameInstance().getMainArchive().getFileByName(getParentFile(), this.path);
        if (texRefFile != null && !(texRefFile instanceof GreatQuestImageFile)) {
            getLogger().warning(Utils.getSimpleName(this) + " pointed to path '" + this.path + "', which yielded a " + Utils.getSimpleName(texRefFile) + " instead of an image???");
            return null;
        }

        return (GreatQuestImageFile) texRefFile;
    }
}