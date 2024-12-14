package net.highwayfrogs.editor.games.konami.greatquest.generic;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Represents a resource path.
 * This appears fully unused by the game as there is no code to handle this data, but perhaps it was used by an editor.
 * This appears to be how the game specifies where an animation file came from. Eg:
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
public class kcCResourcePath extends GameData<GreatQuestInstance> implements IPropertyListCreator, kcIGenericResourceData {
    private final kcCResourceGeneric resource;
    private String filePath = "";

    private static final int PATH_LENGTH = 260;

    public kcCResourcePath(@NonNull kcCResourceGeneric resource) {
        super(resource.getGameInstance());
        this.resource = resource;
    }

    @Override
    public void load(DataReader reader) {
        // NOTE: This file when looked at in a hex editor may appear to have extra data. Remember that this data is actually the header of the kcCResourceGeneric.
        int fileNameHash = reader.readInt();
        this.filePath = reader.readNullTerminatedFixedSizeString(PATH_LENGTH, GreatQuestInstance.PADDING_BYTE_DEFAULT);

        // Validations:
        if (fileNameHash != getFileNameHash() && Objects.equals(this.resource.getName(), getResourceName(this.resource.getHash())))
            throw new RuntimeException("The resource path '" + this.filePath + "' was paired with hash " + NumberUtils.to0PrefixedHexString(fileNameHash) + " but FrogLord thinks it should be " + NumberUtils.to0PrefixedHexString(getFileNameHash()));
        String expectedResourceName = getResourceName(getFileName());
        if (expectedResourceName != null && !expectedResourceName.equals(this.resource.getName()))
            throw new RuntimeException("Expected kcCResourcePath to be named '" + expectedResourceName + "', but was actually named '" + this.resource.getName() + "'.");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getFileNameHash());
        writer.writeNullTerminatedFixedSizeString(this.filePath, PATH_LENGTH, GreatQuestInstance.PADDING_BYTE_DEFAULT);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Path", this.filePath,
                () -> InputMenu.promptInputBlocking(getGameInstance(), "Please enter the new path.", this.filePath, this::setFilePath));
        return propertyList;
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.RESOURCE_PATH;
    }

    /**
     * Get the file name from the file path.
     * This can often be null if the file name was not included as part of the file path. (It would be determinable by finding the file by its hash)
     */
    public String getFileName() {
        return GreatQuestUtils.getFileNameFromPath(this.filePath);
    }

    /**
     * Get the hash of the file name.
     */
    public int getFileNameHash() {
        String fileName = getFileName();
        return fileName != null && fileName.length() > 0 ? GreatQuestUtils.hash(fileName) : this.resource.getHash();
    }

    /**
     * Sets the file path.
     * @param newFilePath The new file path to apply
     */
    public void setFilePath(String newFilePath) {
        if (newFilePath == null)
            throw new NullPointerException("newFilePath");

        String newFileName = GreatQuestUtils.getFileNameFromPath(newFilePath);
        if (newFileName == null || newFileName.trim().isEmpty())
            throw new IllegalArgumentException("The file path '" + newFilePath + "' does not appear to have a valid file name.");

        if (newFilePath.getBytes(StandardCharsets.US_ASCII).length >= PATH_LENGTH) // We can conflate characters with bytes here as the charset is US_ASCII.
            throw new IllegalArgumentException("The file path is too long, the game only has room for " + (PATH_LENGTH - 1) + " characters!");

        this.filePath = newFilePath;
        if (this.resource.isHashBasedOnName()) {
            String newResourceName = getResourceName(newFileName);
            if (newResourceName != null && Objects.equals(this.resource.getName(), getResourceName(this.resource.getHash())))
                this.resource.setName(newResourceName, true);
        }
    }

    private static String getResourceName(String fileName) {
        if (fileName == null || fileName.isEmpty())
            return null;

        int fileNameHash = GreatQuestUtils.hash(fileName);
        return getResourceName(fileNameHash);
    }

    private static String getResourceName(int fileNameHash) {
        return "respath-0x" + NumberUtils.to0PrefixedHexStringLower(fileNameHash); // The padding is confirmed to occur by referencing Level18JoyTowers 'respath-0x0307826a'.
    }
}