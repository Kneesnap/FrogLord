package net.highwayfrogs.editor.games.psx.utils;

import jpsxdec.modules.video.save.MdecDecodeQuality;
import jpsxdec.psxvideo.bitstreams.BitStreamCompressor;
import jpsxdec.psxvideo.bitstreams.BitStreamDebugging;
import jpsxdec.psxvideo.bitstreams.BitStreamUncompressor;
import jpsxdec.psxvideo.bitstreams.IBitStreamUncompressor;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.encode.PsxYCbCrImage;
import jpsxdec.psxvideo.mdec.*;
import jpsxdec.psxvideo.mdec.MdecException.EndOfStream;
import jpsxdec.psxvideo.mdec.MdecException.ReadCorruption;
import jpsxdec.util.BinaryDataNotRecognized;
import jpsxdec.util.IncompatibleException;
import net.highwayfrogs.editor.utils.DataSizeUnit;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Contains static utilities mimicking the PSX's MDEC chip. (Used for image/video encoding)
 * Utilizes a heavily stripped down version of jPSXdec.
 * If .MDEC files ever need to be supported, know that they are just uncompressed .BS files. Refer to ReplaceFrameFull.java/readMdecAndCompress in jPSXdec.
 * Created by Kneesnap on 9/23/2025.
 */
public class PsxMdecUtils {
    /**
     * Enables/disables debug mode for mdec decoding.
     * @param debug iff true, debug mode will be enabled
     */
    public static void setDebugMode(boolean debug) {
        BitStreamDebugging.DEBUG = debug;
        MdecDecoder.DEBUG = debug;
        if (debug) {
            boolean blnAssertsEnabled = false;
            assert blnAssertsEnabled = true;
            if (!blnAssertsEnabled) {
                System.err.println("[Warning] Unable to enable decoding debug because asserts are disabled.");
                System.err.println("Start java using the -ea option.");
            }
        }
    }

    /**
     * Decodes a .BS image from the provided file bytes.
     * @param width the width of the image
     * @param height the height of the image
     * @param fileData the file data
     * @param outputQuality the output quality to use when decoding the image
     * @param upsampleQuality the upsample quality to use if the image will be upsampled. (Only occurs if the output quality is high)
     * @return decodedImage
     */
    @SuppressWarnings({"AssertWithSideEffects", "ConstantValue"})
    public static BufferedImage decodeBsImage(int width, int height, byte[] fileData, MdecDecodeQuality outputQuality, ChromaUpsample upsampleQuality) {
        if (width <= 0 || width >= 2000)
            throw new IllegalArgumentException("Invalid width: " + width);
        if (height <= 0 || height >= 2000)
            throw new IllegalArgumentException("Invalid height: " + height);
        if (fileData == null)
            throw new NullPointerException("fileData");
        if (outputQuality == null)
            outputQuality = MdecDecodeQuality.HIGH;
        if (upsampleQuality == null)
            upsampleQuality = ChromaUpsample.NearestNeighbor;

        // made decoder, verify decoding quality
        MdecDecoder vidDecoder = outputQuality.makeDecoder(width, height);

        // verify upsample quality
        if (vidDecoder instanceof MdecDecoder_double)
            ((MdecDecoder_double) vidDecoder).setUpsampler(upsampleQuality);

        try {
            IBitStreamUncompressor uncompressor = BitStreamUncompressor.identifyUncompressor(fileData, fileData.length);

            try {
                vidDecoder.decode(uncompressor);
            } catch (MdecException.ReadCorruption ex) {
                throw new RuntimeException("Frame data appears to be corrupted.", ex);
            } catch (MdecException.EndOfStream ex) {
                throw new RuntimeException("Frame data appears to be incomplete.", ex);
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            vidDecoder.readDecodedRgb(image.getWidth(), image.getHeight(),
                    ((DataBufferInt) image.getRaster().getDataBuffer()).getData());

            return image;
        } catch (BinaryDataNotRecognized ex) {
            throw new RuntimeException("Could not determine stream frame type (uncompressor).", ex);
        }
    }

    /**
     * Replaces a pre-existing .BS image with a BufferedImage.
     * Based on ReplaceFrameFull.java/encodeJavaImage
     * @param oldBsImageData the previous .BS image to copy encoding settings from
     * @param image the image to replace the previous image with
     * @return newBsImageData
     */
    @SuppressWarnings({"AssertWithSideEffects", "ConstantValue"})
    public static byte[] encodeBsImage(byte[] oldBsImageData, BufferedImage image) {
        if (oldBsImageData == null)
            throw new NullPointerException("oldBsImageData");
        if (image == null)
            throw new NullPointerException("image");
        if (Calc.fullDimension(image.getWidth()) != image.getWidth() || Calc.fullDimension(image.getHeight()) != image.getHeight())
            throw new IllegalArgumentException("The provided image was " + image.getWidth() + "x" + image.getHeight()
                    + " pixels large: it was not padded to a multiple of 16! (Eg: "
                    + Calc.fullDimension(image.getWidth()) + "x" + Calc.fullDimension(image.getHeight()) + ")");

        // Determine decompressor from old image data.
        IBitStreamUncompressor decompressor;
        try {
            decompressor = BitStreamUncompressor.identifyUncompressor(oldBsImageData);
        } catch (BinaryDataNotRecognized ex) {
            throw new RuntimeException("Could not determine stream frame type (uncompressor).", ex);
        }

        return encodeBsImage(image, decompressor.makeCompressor());
    }

    /**
     * Encodes an image to the .BS (bitstream) file format.
     * @param image the image to encode
     * @param compressor the compressor to use when compressing the image
     * @return encodedBsImage
     */
    @SuppressWarnings({"AssertWithSideEffects", "ConstantValue"})
    public static byte[] encodeBsImage(BufferedImage image, BitStreamCompressor compressor) {
        if (image == null)
            throw new NullPointerException("image");
        if (compressor == null)
            throw new NullPointerException("compressor");
        if (Calc.fullDimension(image.getWidth()) != image.getWidth() || Calc.fullDimension(image.getHeight()) != image.getHeight())
            throw new IllegalArgumentException("The provided image was " + image.getWidth() + "x" + image.getHeight()
                    + " pixels large: it was not padded to a multiple of 16! (Eg: "
                    + Calc.fullDimension(image.getWidth()) + "x" + Calc.fullDimension(image.getHeight()) + ")");

        // No image conversion to a particular color model needs to occur here, since PsxYCbCrImage does not rely on any particular image type.
        MdecEncoder imageEncoder = new MdecEncoder(new PsxYCbCrImage(image), image.getWidth(), image.getHeight());

        try {
            // This only worked due to a chance I made to the compressor class, where I changed it to get the macro block count from the encoder object, instead of the uncompressor.
            // The uncompressor doesn't have the correct macro block count because it never actually uncompressed any data.
            return compressor.compressFull((int) DataSizeUnit.MEGABYTE.getIncrement(), "BsImage", imageEncoder);
        } catch (EndOfStream ex) {
            throw new RuntimeException("Reached end of stream while compressing image.", ex);
        } catch (ReadCorruption ex) {
            throw new RuntimeException("Read corrupted data while compressing image.", ex);
        } catch (IncompatibleException ex) {
            throw new RuntimeException("Encountered incompatible data while compressing image.", ex);
        }
    }
}
