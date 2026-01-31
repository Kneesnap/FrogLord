package net.highwayfrogs.editor.games.sony.shared.overlay;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.reader.FileSource;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;
import java.io.IOException;

/**
 * Represents an entry in an overlay table.
 * Created by Kneesnap on 3/11/2024.
 */
@Getter
public class SCOverlayTableEntry extends SCSharedGameData {
    private String filePath;
    private long overlayDoublePointer;
    private long overlayStartPointer;

    private static final int EMPTY_BYTES = 24;

    public SCOverlayTableEntry(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        long pathPointer = reader.readUnsignedIntAsLong();
        if (getGameInstance().isValidLookingPointer(pathPointer)) {
            reader.jumpTemp((int) (pathPointer - getGameInstance().getRamOffset()));
            this.filePath = reader.readNullTerminatedString();
            reader.jumpReturn();
        } else {
            this.filePath = null;
        }

        long overlayPointer = reader.readUnsignedIntAsLong();
        if (getGameInstance().isValidLookingPointer(overlayPointer)) {
            this.overlayDoublePointer = overlayPointer;
            reader.jumpTemp((int) (overlayPointer - getGameInstance().getRamOffset()));
            this.overlayStartPointer = reader.readUnsignedIntAsLong();
            reader.jumpReturn();
        } else {
            this.overlayDoublePointer = 0;
            this.overlayStartPointer = 0;
        }

        reader.skipBytesRequireEmpty(EMPTY_BYTES);
        if (this.filePath != null || this.overlayStartPointer != 0)
            getLogger().info(" - Overlay Entry '%s', Address: 0x%X", this.filePath, this.overlayStartPointer);
    }

    @Override
    public void save(DataWriter writer) {
        writer.skipBytes(Constants.POINTER_SIZE); // We do not have the capabilities to modify this right now.
        writer.writeUnsignedInt(this.overlayDoublePointer);
        writer.writeNull(EMPTY_BYTES);
    }

    /**
     * Gets a reader which can read the overlay file.
     */
    public DataReader getReader() {
        File overlayFile = new File(getGameInstance().getMainGameFolder(), this.filePath);

        // Try stripping directories until the overlays are found.
        String tempFilePath = this.filePath;
        while (!overlayFile.exists() || !overlayFile.isFile()) {
            int nextSplitIndex = tempFilePath.indexOf('\\');
            if (nextSplitIndex >= 0) {
                tempFilePath = tempFilePath.substring(nextSplitIndex + 1);
                overlayFile = new File(getGameInstance().getMainGameFolder(), tempFilePath);
            } else {
                throw new IllegalArgumentException("Unable to read overlay from file path: '" + this.filePath + "'.");
            }
        }

        try {
            return new DataReader(new FileSource(overlayFile));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to open overlay file '" + overlayFile.getPath() + "'.", ex);
        }
    }
}