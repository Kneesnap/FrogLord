package net.highwayfrogs.editor.games.sony.shared.overlay;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.utils.Utils;

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
        if (SCUtils.isValidLookingPointer(getGameInstance().getPlatform(), pathPointer)) {
            reader.jumpTemp((int) (pathPointer - getGameInstance().getRamOffset()));
            this.filePath = reader.readNullTerminatedString();
            reader.jumpReturn();
        } else {
            this.filePath = null;
        }

        long overlayPointer = reader.readUnsignedIntAsLong();
        if (SCUtils.isValidLookingPointer(getGameInstance().getPlatform(), overlayPointer)) {
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
            getLogger().fine(" - Overlay Entry '" + this.filePath + "', Address: " + Utils.toHexString(this.overlayStartPointer));
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

        String[] path = new String[]{this.filePath};
        File overlayFile;
        do {
            overlayFile = new File(getGameInstance().getExeFile().getParentFile(), path[path.length - 1]);
            if (overlayFile.exists() && overlayFile.isFile())
                break;
            path = path[path.length - 1].split("\\\\", 2);
        } while(path.length > 1);

        if (!overlayFile.exists() || !overlayFile.isFile())
            throw new IllegalArgumentException("Unable to read overlay from '" + overlayFile.getPath() + "'.");

        try {
            return new DataReader(new FileSource(overlayFile));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to open overlay file '" + overlayFile.getPath() + "'.", ex);
        }
    }
}