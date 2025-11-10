package net.highwayfrogs.editor.games.sony.medievil;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorPropertyListUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a file containing ASCII strings.
 * Created by Kneesnap on 9/4/2025.
 */
@Getter
public class MediEvilHelpFile extends SCGameFile<MediEvilGameInstance> {
    private final List<String> strings = new ArrayList<>();

    public static final String SIGNATURE = "BATO";
    public static final byte[] SIGNATURE_BYTES = SIGNATURE.getBytes(StandardCharsets.US_ASCII);
    private static final int SIGNATURE_INTEGER = Utils.makeIdentifierInteger(SIGNATURE);

    public MediEvilHelpFile(MediEvilGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int startIndex = reader.getIndex();

        int headerIndex = -1;
        while (reader.getRemaining() >= Constants.INTEGER_SIZE) {
            if (reader.readInt() == SIGNATURE_INTEGER) {
                headerIndex = reader.getIndex() - Constants.INTEGER_SIZE;
                break;
            }
        }

        if (headerIndex < 0)
            throw new RuntimeException("Could not identify the header for the language file.");

        // Read offset table.
        int i = 0;
        int[] offsetTable = new int[reader.getRemaining() / Constants.INTEGER_SIZE];
        while (reader.getRemaining() >= Constants.INTEGER_SIZE)
            offsetTable[i++] = reader.readInt();
        int headerEndIndex = reader.getIndex();

        // Read strings.
        this.strings.clear();
        reader.setIndex(startIndex);
        Map<Integer, String> previousStrings = new HashMap<>();
        for (i = 0; i < offsetTable.length; i++) {
            int offset = offsetTable[i];
            String oldString = previousStrings.get(offset);
            if (oldString != null) {
                this.strings.add(oldString);
                continue;
            }

            requireReaderIndex(reader, offset, "Expected next string");
            String newString = reader.readNullTerminatedString();
            previousStrings.put(offset, newString);
            this.strings.add(newString);
            reader.align(Constants.INTEGER_SIZE);
        }

        requireReaderIndex(reader, headerIndex, "Expected help file header");
        reader.setIndex(headerEndIndex);
    }

    @Override
    public void save(DataWriter writer) {
        int[] offsetTable = new int[this.strings.size()];

        Map<String, Integer> stringOffsetIndices = new HashMap<>();
        stringOffsetIndices.put(null, 0);
        for (int i = 0; i < this.strings.size(); i++) {
            String currString = this.strings.get(i);
            Integer oldOffset = stringOffsetIndices.get(currString);
            if (oldOffset != null) {
                offsetTable[i] = oldOffset;
                continue;
            }

            int startIndex = writer.getIndex();
            writer.writeNullTerminatedString(currString);
            writer.align(Constants.INTEGER_SIZE);
            offsetTable[i] = startIndex;
            stringOffsetIndices.put(currString, startIndex);
        }

        // Write "header".
        writer.writeBytes(SIGNATURE_BYTES);
        for (int i = 0; i < offsetTable.length; i++)
            writer.writeInt(offsetTable[i]);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.GHIDRA_ICON_PAPER_WITH_TEXT_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-property-list-base", new SCFileEditorPropertyListUIController<>(getGameInstance(), "Help Strings"), this);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("String Entries", this.strings.size());
        for (int i = 0; i < this.strings.size(); i++) {
            final int index = i;
            propertyList.add("Entry " + i, this.strings.get(i), () -> {
                String newValue = InputMenu.promptInput(getGameInstance(), "Please enter a new value.", this.strings.get(index));
                if (newValue == null)
                    return null;

                this.strings.set(index, newValue);
                return newValue;
            });
        }
    }

    /**
     * Test if the provided bytes appear to be a help file.
     * @param bytes the bytes to test as a help file.
     * @return helpFile1
     */
    public static boolean isHelpFile(byte[] bytes) {
        if (bytes == null || bytes.length > DataSizeUnit.KILOBYTE.getIncrement() * 100)
            return false;

        for (int i = 0; i < bytes.length; i += SIGNATURE_BYTES.length)
            if (DataUtils.testSignature(bytes, i, SIGNATURE_BYTES))
                return true;

        return false;
    }
}
