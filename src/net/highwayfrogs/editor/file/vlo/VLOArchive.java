package net.highwayfrogs.editor.file.vlo;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VLOArchive - Image archive format created by VorgPC/Vorg2.
 * TODO: PSX VLOs don't properly read images. Once that's fixed, test exporting valid PSX MWDs.
 * ABGR 8888 = 24 + 8.
 * Created by Kneesnap on 8/17/2018.
 */
@Getter
public class VLOArchive extends GameFile {
    private List<GameImage> images = new ArrayList<>();
    private List<ClutEntry> clutEntries = new ArrayList<>();
    private boolean psxMode;

    private static final String PC_SIGNATURE = "2GRP";
    private static final String PSX_SIGNATURE = "2GRV";
    private static final int SIGNATURE_LENGTH = 4;

    private static final int IMAGE_INFO_BYTES = 24;
    private static final int HEADER_SIZE = SIGNATURE_LENGTH + (2 * Constants.INTEGER_SIZE);
    public static final int TYPE_ID = 1;
    public static final int WAD_TYPE = 0;
    public static final Image ICON = loadIcon("image");

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

        // Load image data.
        reader.jumpTemp(textureOffset);
        for (int i = 0; i < fileCount; i++) {
            GameImage image = new GameImage(this);
            image.load(reader);
            this.images.add(image);
        }
        reader.jumpReturn();

        GameImage.PACK_ID++;
        GameImage.IMAGE_ID = 0;
    }

    @Override
    public void save(DataWriter writer) {
        int imageCount = getImages().size();
        writer.writeStringBytes(isPsxMode() ? PSX_SIGNATURE : PC_SIGNATURE);
        writer.writeInt(imageCount);
        writer.writeInt(writer.getIndex() + ((isPsxMode() ? 3 : 1) * Constants.INTEGER_SIZE)); // Offset to the VLO table info.

        int clutFirstSetupIndex = -1;
        if (isPsxMode()) {
            writer.writeInt(this.clutEntries.size());
            clutFirstSetupIndex = writer.getIndex();
            writer.writeNull(Constants.INTEGER_SIZE); // This will be written later.
        }


        AtomicInteger imageBytesOffset = new AtomicInteger((IMAGE_INFO_BYTES * imageCount) + HEADER_SIZE);
        if (isPsxMode()) // Add room for clut setup data.
            imageBytesOffset.addAndGet(getClutEntries().stream().mapToInt(ClutEntry::getByteSize).sum());

        for (GameImage image : getImages())
            image.save(writer, imageBytesOffset);

        // Save CLUT data. CLUT should be saved after images are saved, because changed images will generate a new CLUT.
        if (isPsxMode()) {
            writer.jumpTemp(clutFirstSetupIndex);
            writer.writeInt(imageBytesOffset.get());
            writer.jumpReturn();

            for (ClutEntry entry : clutEntries)
                entry.save(writer, imageBytesOffset);
        }
    }

    @Override
    public Image getIcon() {
        return ICON;
    }
}
