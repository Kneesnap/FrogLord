package net.highwayfrogs.editor.file.vlo;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXClutColor;
import net.highwayfrogs.editor.file.writer.DataWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VLOArchive - Image archive format created by VorgPC/Vorg2.
 * ABGR 8888 = 24 + 8.
 * Created by Kneesnap on 8/17/2018.
 */
@Getter
public class VLOArchive extends GameFile {
    private List<GameImage> images = new ArrayList<>();
    private List<ClutEntry> clutEntries = new ArrayList<>();
    private BufferedImage clutImage;
    private boolean psxMode;

    private static final String PC_SIGNATURE = "2GRP";
    private static final String PSX_SIGNATURE = "2GRV";
    private static final int SIGNATURE_LENGTH = 4;

    private static final int IMAGE_INFO_BYTES = 24;
    private static final int HEADER_SIZE = SIGNATURE_LENGTH + (2 * Constants.INTEGER_SIZE);
    public static final int TYPE_ID = 1;

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
            makeCLUTImage();
        }

        // Load image data.
        reader.jumpTemp(textureOffset);
        for (int i = 0; i < fileCount; i++) {
            GameImage image = new GameImage(this);
            image.load(reader);
            this.images.add(image);
        }
        reader.jumpReturn();
    }

    private void makeCLUTImage() { //TODO: Remove this once PSX is fully supported.
        this.clutImage = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_ARGB);

        for (ClutEntry entry : clutEntries) {
            int startX = entry.getClutRect().getX();
            int startY = entry.getClutRect().getY();
            int width = entry.getClutRect().getWidth();
            int height = entry.getClutRect().getHeight();

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    PSXClutColor color = entry.getColors().get((y * width) + x);
                    this.clutImage.setRGB(x + startX, y + startY, toBGRA(color));
                    System.out.println("[" + (x + startX) + ", " + (y + startY) + "] (" + color.getUnsignedScaledRed() + ", " + color.getUnsignedScaledGreen() + ", " + color.getUnsignedScaledBlue() + ") " + Integer.toHexString(color.getUnsignedScaledRed()) + " " + Integer.toHexString(color.getUnsignedScaledGreen()) + " " + Integer.toHexString(color.getUnsignedScaledBlue()));
                }
            }
        }

        try {
            ImageIO.write(this.clutImage, "png", new File("debug/clut.png"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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


        if (isPsxMode()) { // Save CLUT data.
            writer.jumpTemp(clutFirstSetupIndex);
            writer.writeInt(imageBytesOffset.get());
            writer.jumpReturn();

            for (ClutEntry entry : clutEntries)
                entry.save(writer, imageBytesOffset);
        }
    }

    public static int toBGRA(PSXClutColor color) {
        byte[] bytes = new byte[4];
        bytes[0] = Utils.unsignedShortToByte(color.getUnsignedScaledBlue());
        bytes[1] = Utils.unsignedShortToByte(color.getUnsignedScaledGreen());
        bytes[2] = Utils.unsignedShortToByte(color.getUnsignedScaledRed());
        bytes[3] = color.getAlpha(false); // FF = solid. 0 = Invisible.
        return Utils.readNumberFromBytes(bytes);
    }
}
