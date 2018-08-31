package net.highwayfrogs.editor.file.vlo;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * VLOArchive - Image archive format created by VorgPC/Vorg2.
 * ABGR 8888 = 24 + 8.
 * TODO: Is clutId still zero on PSX vlos?
 * Created by Kneesnap on 8/17/2018.
 */
@Getter
public class VLOArchive extends GameFile {
    private List<GameImage> images = new ArrayList<>();
    private List<ClutEntry> clutEntries = new ArrayList<>();
    private boolean psxMode;

    public static final int TYPE_ID = 1;
    public static final int FLAG_TRANSLUCENT = 1;
    public static final int FLAG_ROTATED = 2; // Unused.
    public static final int FLAG_HIT_X = 4; //Appears to decrease width by 1?
    public static final int FLAG_HIT_Y = 8; //Appears to decrease height by 1?
    public static final int FLAG_REFERENCED_BY_NAME = 16; // Unsure.
    public static final int FLAG_BLACK_IS_TRANSPARENT = 32; // Seems like it may not be used. Would be weird if that were the case.
    public static final int FLAG_2D_SPRITE = 32768;

    private static final String PC_SIGNATURE = "2GRP";
    private static final String PSX_SIGNATURE = "2GRV";
    private static final int SIGNATURE_LENGTH = 4;

    private static final int IMAGE_INFO_BYTES = 24;
    private static final int HEADER_SIZE = SIGNATURE_LENGTH + (2 * Constants.INTEGER_SIZE);

    @Override
    public void load(DataReader reader) {
        String readSignature = reader.readString(SIGNATURE_LENGTH);
        if (readSignature.equals(PSX_SIGNATURE)) {
            this.psxMode = true;
        } else {
            Utils.verify(readSignature.equals(PC_SIGNATURE), "Invalid VLO signature: %s.", readSignature);
        }

        int fileCount = reader.readInt();
        int textureOffset = reader.readInt();

        // Load image data.
        reader.jumpTemp(textureOffset);
        for (int i = 0; i < fileCount; i++) {
            GameImage image = new GameImage(this);
            image.load(reader);
            this.images.add(image);
        }
        reader.jumpReturn();

        // Load clut data.
        if (isPsxMode()) { // GRV file has clut data.
            int clutCount = reader.readInt();
            int clutOffset = reader.readInt();

            reader.jumpTemp(clutOffset);
            for (int i = 0; i < clutCount; i++) {
                ClutEntry clut = new ClutEntry();
                clut.load(reader);
                clutEntries.add(clut);
            }
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        int imageCount = getImages().size();
        writer.writeStringBytes(isPsxMode() ? PSX_SIGNATURE : PC_SIGNATURE);
        writer.writeInt(imageCount);
        writer.writeInt(writer.getIndex() + Constants.INTEGER_SIZE); // Offset to the VLO table info.

        int clutSetupStart = 0; //TODO: Calculate this right.
        if (isPsxMode()) {
            writer.writeInt(clutEntries.size());
            writer.writeInt(clutSetupStart);
        }

        int offset = (IMAGE_INFO_BYTES * imageCount) + HEADER_SIZE;
        for (GameImage image : getImages())
            offset += image.save(writer, offset);

        getImages().stream()
                .map(GameImage::getImageBytes)
                .forEach(writer::writeBytes);


        if (isPsxMode()) { // Save CLUT data.
            int clutOffset = writer.getIndex() + (clutEntries.size() * ClutEntry.BYTE_SIZE);
            for (ClutEntry entry : clutEntries)
                clutOffset += entry.save(writer, clutOffset);
        }
    }
}
