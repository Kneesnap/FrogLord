package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a texture reference.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class GreatQuestChunkTextureReference extends kcCResource {
    private String fullPath = "";

    private static final int PATH_SIZE = 260;

    public GreatQuestChunkTextureReference(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.TEXTURE);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.fullPath = reader.readNullTerminatedFixedSizeString(PATH_SIZE); // Don't read with the padding byte, as the padding bytes are only valid when the buffer is initially created, if the is shrunk (Such as shadow.img in 00.dat), after the null byte, the old bytes will still be there.

        // Apply the file name.
        GreatQuestAssetBinFile mainArchive = getMainArchive();
        if (mainArchive != null)
            mainArchive.applyFileName(this.fullPath, false);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeNullTerminatedFixedSizeString(this.fullPath, PATH_SIZE, GreatQuestInstance.PADDING_BYTE_CD);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("File Path", this.fullPath,
                () -> InputMenu.promptInputBlocking(getGameInstance(), "Please enter the new path.", this.fullPath, newPath -> setFullPath(newPath, true)));

        return propertyList;
    }

    /**
     * Gets the images referenced by this chunk.
     */
    public GreatQuestImageFile getReferencedImage() {
        GreatQuestArchiveFile texRefFile = getGameInstance().getMainArchive().getFileByName(getParentFile(), this.fullPath);
        if (texRefFile != null && !(texRefFile instanceof GreatQuestImageFile)) {
            getLogger().warning(Utils.getSimpleName(this) + " pointed to path '" + this.fullPath + "', which yielded a " + Utils.getSimpleName(texRefFile) + " instead of an image???");
            return null;
        }

        return (GreatQuestImageFile) texRefFile;
    }

    /**
     * Sets the file path of the asset referenced here.
     * @param newPath the file path to apply
     * @param throwIfPathCannotBeResolved if the file path cannot be resolved to a valid asset and this is true, an exception will be thrown.
     */
    public void setFullPath(String newPath, boolean throwIfPathCannotBeResolved) {
        if (newPath == null)
            throw new NullPointerException("newPath");
        if (newPath.length() >= PATH_SIZE)
            throw new IllegalArgumentException("The provided path is too large! (Provided: " + newPath.length() + ", Maximum: " + (PATH_SIZE - 1) + ")");

        if (throwIfPathCannotBeResolved) {
            GreatQuestArchiveFile texRefFile = getGameInstance().getMainArchive().getFileByName(getParentFile(), newPath);
            if (!(texRefFile instanceof GreatQuestImageFile))
                throw new IllegalArgumentException("The file path could not be resolved to an image! (" + newPath + ")");
        }

        this.fullPath = newPath;
        setName(GreatQuestUtils.getFileNameFromPath(newPath));
    }
}