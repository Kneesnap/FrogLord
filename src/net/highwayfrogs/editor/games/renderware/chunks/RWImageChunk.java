package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RWSChunk;
import net.highwayfrogs.editor.games.renderware.RWSChunkManager;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;

/**
 * Created by Kneesnap on 6/9/2020.
 */
@Getter
public class RWImageChunk extends RWSChunk {
    private BufferedImage image;

    public RWImageChunk(int renderwareVersion, RWSChunk parentChunk) {
        super(0x18, renderwareVersion, parentChunk);
    }

    @Override
    public void loadChunkData(DataReader reader) {
        RWStructChunk structChunk = (RWStructChunk) RWSChunkManager.readChunk(reader, this);

        DataReader structReader = structChunk.getReader();
        int width = structReader.readInt();
        int height = structReader.readInt();
        int bitDepth = structReader.readInt();
        int lineStride = structReader.readInt(); // Number of bytes to go from the first pixel in a row to the address of the pixel in the next row. (https://medium.com/@oleg.shipitko/what-does-stride-mean-in-image-processing-bba158a72bcd)

        if (height == 0)
            height = width;

        //if (bitDepth == 0)
        //    bitDepth =
        // C:\Program Files (x86)\Konami\Frogger's Adventures\dvd\win\area07.hfs
        System.err.println("Image (Depth: " + bitDepth + ", Dimensions: [" + width + ", " + height + "], Stride: " + lineStride + ", Remaining: " + reader.getRemaining() + ")");

        if (bitDepth != 0 && bitDepth != 8 && bitDepth != 4 && bitDepth != 32)
            throw new UnsupportedOperationException("Bit depth not supported! (Depth: " + bitDepth + ", Dimensions: [" + width + ", " + height + "], Stride: " + lineStride + ", Remaining: " + reader.getRemaining() + ")");

        // TODO: ?
        //int calculatedLineStride = (int) (((double) bitDepth / Constants.BITS_PER_BYTE) * width);
        //if (lineStride != calculatedLineStride)
        //    throw new AssertionError("The calculated stride (" + calculatedLineStride + ") does not match the one which was read. (" + lineStride + ")");

        reader.jumpTemp(reader.getIndex() + (lineStride * height));
        int[] colorPalette = new int[reader.getRemaining() / Constants.INTEGER_SIZE];
        for (int i = 0; i < colorPalette.length; i++)
            colorPalette[i] = reader.readInt();
        reader.jumpReturn();

        short lastReadByte = Constants.NULL_BYTE;
        boolean readHighBits = true;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitDepth == 4) {
                    if (readHighBits)
                        lastReadByte = reader.readUnsignedByteAsShort();

                    this.image.setRGB(x, y, Utils.swapRedBlue(colorPalette[(lastReadByte >> (readHighBits ? 0 : 4)) & 0x0F]));
                    readHighBits = !readHighBits;
                } else if (bitDepth == 8 || bitDepth == 0) {
                    this.image.setRGB(x, y, Utils.swapRedBlue(colorPalette[reader.readUnsignedByteAsShort()]));
                } else if (bitDepth == 32) {
                    this.image.setRGB(x, y, Utils.swapRedBlue(reader.readInt()));
                }
            }
        }
    }

    @Override
    public void saveChunkData(DataWriter writer) {

    }
}
