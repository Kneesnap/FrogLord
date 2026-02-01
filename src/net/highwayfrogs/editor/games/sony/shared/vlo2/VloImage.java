package net.highwayfrogs.editor.games.sony.shared.vlo2;

import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.PSXClutColor;
import net.highwayfrogs.editor.games.psx.image.PsxAbrTransparency;
import net.highwayfrogs.editor.games.psx.image.PsxImageBitDepth;
import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygonSortMode;
import net.highwayfrogs.editor.games.sony.medievil.map.misc.MediEvilMapFrictionLevel;
import net.highwayfrogs.editor.games.sony.medievil.map.misc.MediEvilMapInteractionType;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.image.ImageUtils;
import net.highwayfrogs.editor.utils.image.quantization.octree.OctreeQuantizer;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * A singular image in a VLO file. MR_TXSETUP struct.
 * This file is an utter mess (because Vorg was a mess).
 * <p/>
 * In mid-1996 when the MR API was created, the original VLO format was rewritten to become VLO2 (the format supported by this file).
 * The program which could create these .VLO files was named "Vorg", likely for "Visual organizer" or "VRAM organizer".
 * While the VLO2 file format would be used from 1996-2001, Vorg was rewritten twice (once for PC support in 1997, and later for Med2/C-12 at an unknown time).
 * And even between rewrites, it seems like Vorg had different versions or per-game tweaks made for each game during the existence of Vorg.
 * Despite the differences between games, the file format itself remained consistent.
 * <p/>
 * Because the file format remained consistent from 1996-2001 (the core era of Millennium's games which FrogLord intends to support),
 *  having a single implementation/object for .VLO files is very feasible.
 * The complexities arise once we start to import textures and make changes to .VLO files.
 * This is because Vorg was responsible for many complex behaviors which changed over time such as:
 *  - Generating texture padding
 *  - Placing textures in VRAM without overlap, when multiple VLO files may be loaded at once.
 *  - Avoiding integer overflow in PSX texture displays
 *  - Image quantization (Reducing images from arbitrary colors down to a 16 or 256 color palette)
 *
 * Because no source code, binary, or documentation of Vorg has ever been found, the original behavior of Vorg
 *  has been determined by analyzing .VLO files from the many different games by this company.
 * Many of the implementations chosen in .VLO files showcase baffling algorithm choices,
 *  or use manually configured data on a per-texture basis, which are omitted from the resulting .VLO files.
 * This implementation of VLO files does not aim to perfectly recreate original Vorg behavior.
 * But it does aim to mimic the patterns seen in the original .VLO files as closely as possible,
 *  in order to minimize the risk of FrogLord breaking existing textures while also keeping texture editing as simple as possible for a FrogLord user.
 * <p/>
 * TODO Remaining Tasks:
 *  1) Rewrite the VLO file UI
 *   -> The "Clone Image" button should be removed to "Copy image to other VLO file.", and moved to right-click menu.
 *   -> Update the import all/export all feature to be on right-click of the VLO itself
 *    -> It should also use image file names.
 *     -> For images with unrecognized non-numeric names, add them to the VLO.
 *     -> For images numeric names, add them with that as their texture ID, replacing any existing image with that texture ID.
 *  2) Text generation
 *   -> Integrate with new system to become seamless
 *   -> Remove usage of SCImageUtils.TransparencyFilter, and then delete that class,
 * Created by Kneesnap on 8/30/2018.
 */
public class VloImage extends SCSharedGameData implements Cloneable, ITextureSource, ICollectionViewEntry {
    @Getter private final List<Consumer<BufferedImage>> imageChangeListeners;
    @Getter private final VloFile parent;
    @Getter private short vramX;
    @Getter private short vramY;
    @Getter private short paddedWidth;
    @Getter private short paddedHeight;
    @Getter private short textureId = -1;
    @Getter private short flags;
    @Getter private short clutId; // Seems to be garbage data on PC. The VLO load function doesn't copy this value anywhere, so it is provably unused by the PC version.
    private short unpaddedWidth; // Contains the width of the original image, without padding. Overridden getter is getUnpaddedWidth(), getUnpaddedHeight(), which will return padding as seen by the game, instead of the padding to the underlying image.
    private short unpaddedHeight; // Contains the height of the original image, without padding.
    @Getter private int[] pixelBuffer;
    @NonNull @Getter private PsxImageBitDepth bitDepth = PsxImageBitDepth.CLUT4; // This is consistent across versions on PC.
    @NonNull @Getter private PsxAbrTransparency abr = PsxAbrTransparency.DEFAULT; // ABR.
    @Getter private VloClut clut;
    @Getter private boolean clutFogEnabled;
    @Getter private boolean paddingTransparent;
    private int paddingAmount;
    private String customName; // The custom name applied by FrogLord. (This may override the original name)
    private boolean anyStpPixelInversionPsx;
    private boolean anyFullyTransparentPixelPresentPsx;
    @Getter private boolean anyFullyBlackPixelPresentPC;
    private boolean expectedStpNonBlackBitPsx; // Pre-MediEvil II: Used to calculate the CLUT STP bit state.
    private boolean expectedStpBlackBitPsx; // Pre-MediEvil II: Used to calculate the CLUT STP bit state.

    // Temporary data.
    private final transient BufferedImage[] cachedImages = new BufferedImage[IMAGE_EXPORT_CACHE_SIZE];
    private transient int tempImageDataPointer = -1;

    public static final int MAX_IMAGE_DIMENSION = 256;
    public static final int PC_VRAM_TOTAL_PAGES = 14; // It appears the PC version rendering dlls only create 14 pages.
    public static final int PC_VRAM_PAGE_WIDTH = 256;
    public static final int PC_VRAM_PAGE_HEIGHT = 256;
    public static final int PC_VRAM_MAX_POSITION_X = PC_VRAM_PAGE_WIDTH;
    public static final int PC_VRAM_MAX_POSITION_Y = PC_VRAM_PAGE_HEIGHT * PC_VRAM_TOTAL_PAGES;

    public static final int FLAG_TRANSLUCENT = Constants.BIT_FLAG_0; // Used by sprites in the MR API to enable semi-transparent rendering mode.
    // Even though there is code to patch MOFs to render based on the translucent flag, the function (MRPatchMOFTranslucency) is only be called by Frogger, not MediEvil. (Other games untested)
    // Terrain Transparency:
    //  - MediEvil has a transparency flag on the polygon itself, but also seems to check the transparency flag here.

    //public static final int FLAG_ROTATED = Constants.BIT_FLAG_1; // Unused in MR API + Frogger, does not appear to be set.
    private static final int FLAG_HIT_X = Constants.BIT_FLAG_2; // Appears to decrease width by 1?
    private static final int FLAG_HIT_Y = Constants.BIT_FLAG_3; // Appears to decrease height by 1?
    public static final int FLAG_REFERENCED_BY_NAME = Constants.BIT_FLAG_4; // It means it has an entry in bmp_pointers. Images without this flag can be dynamically loaded/unloaded without having a fixed memory location which the code can access the texture info from.
    // Used on PC only.
    // Upon loading ANY texture with this flag set, its texture page is permanently marked as transparent, until the process is stopped.
    // Therefore, ALL textures on a texture page with a BLACK_IS_TRANSPARENT option were marked with this flag by Vorg, even if no black pixels are present.
    // Additionally, this flag is checked when determining which VRAM pages to place a texture.
    public static final int FLAG_BLACK_IS_TRANSPARENT = Constants.BIT_FLAG_5; // Used exclusively by the PC version during image loading.
    public static final int FLAG_2D_SPRITE = Constants.BIT_FLAG_15; // Indicates that an animation list should be used when the image is used to create a sprite. I dunno, it seems like every single texture in frogger has this flag set. (Though this is not confirmed, let alone confirmed for all versions)
    private static final int VALIDATION_FLAGS = FLAG_2D_SPRITE | FLAG_BLACK_IS_TRANSPARENT | FLAG_REFERENCED_BY_NAME | FLAG_HIT_Y | FLAG_HIT_X | FLAG_TRANSLUCENT;

    // PT Toolkit Era: (C-12, ??, ??)
    public static final int PT_FLAG_PARTLY_TRANSPARENT = Constants.BIT_FLAG_6; // Used in C-12 & MediEvil II.
    private static final int PT_VALIDATION_FLAGS = VALIDATION_FLAGS | PT_FLAG_PARTLY_TRANSPARENT;

    private static final int FLAG_MEDIEVIL_SORT_MASK = Constants.BIT_FLAG_9 | Constants.BIT_FLAG_8;
    private static final int FLAG_MEDIEVIL_SORT_SHIFT = 8;
    private static final int FLAG_MEDIEVIL_FRICTION_MASK = Constants.BIT_FLAG_11 | Constants.BIT_FLAG_10; // Controls how much friction this surface has for entities walking upon it.
    private static final int FLAG_MEDIEVIL_FRICTION_SHIFT = 10;
    private static final int FLAG_MEDIEVIL_INTERACTION_MASK = Constants.BIT_FLAG_14 | Constants.BIT_FLAG_13 | Constants.BIT_FLAG_12; // This is an enum value for the surface type. 0 - NONE, 1 - WATER, 2 - MUD, 3 - DEADLY MUD, 4 - NOT GROUND, 5 - CORN, 6 - SPECIAL1 (unknown), 7 - SPECIAL2
    private static final int FLAG_MEDIEVIL_INTERACTION_SHIFT = 12;
    private static final int MEDIEVIL_VALIDATION_FLAGS = VALIDATION_FLAGS | FLAG_MEDIEVIL_SORT_MASK | FLAG_MEDIEVIL_FRICTION_MASK | FLAG_MEDIEVIL_INTERACTION_MASK;

    // 0 -> Used for a pixel which is expected to respect the automatically generated STP value.
    // 127 -> The STP bit is flipped from the default state for this pixel specifically.
    // 255 -> Used for a pixel which is expected to respect the automatically generated STP value.
    private static final byte IMAGE_ALPHA_REGULAR_STP_BIT_TRANS = 0x00;
    private static final byte IMAGE_ALPHA_INVERTED_STP_BIT = 0x7F;
    private static final byte IMAGE_ALPHA_REGULAR_STP_BIT_OPAQUE = (byte) 0xFF;
    private static final int COLOR_CLOSEST_TO_BLACK = 0xFF080000; // This value seems to have been used by Vorg in the place of true black when BLACK_IS_TRANSPARENT is set.
    private static final int COLOR_FULL_ALPHA = 0xFF000000;
    private static final int COLOR_TRUE_BLACK = 0xFF000000;

    private static final int PADDING_TRANSPARENT_PIXEL_PC = 0xFF000000;
    private static final int PADDING_TRANSPARENT_PIXEL_PSX = 0x00000000;

    public VloImage(VloFile parentFile) {
        super(parentFile != null ? parentFile.getGameInstance() : null);
        this.imageChangeListeners = new ArrayList<>();
        this.parent = parentFile;
    }

    @Override
    public void load(DataReader reader) {
        this.vramX = reader.readShort();
        this.vramY = reader.readShort();
        this.paddedWidth = reader.readShort();
        this.paddedHeight = reader.readShort();

        this.tempImageDataPointer = reader.readInt();
        this.textureId = reader.readShort();

        short readPage = reader.readShort();
        if (isPsxMode()) {
            this.bitDepth = PsxImageBitDepth.values()[(readPage & 0b110000000) >> 7];
            this.abr = PsxAbrTransparency.values()[(readPage & 0b1100000) >> 5];
            this.clutId = reader.readShort();
            this.flags = reader.readShort();
        } else {
            this.bitDepth = PsxImageBitDepth.CLUT4;
            this.abr = PsxAbrTransparency.values()[(readPage & 0b1100000000) >> 8];
            this.flags = reader.readShort();
            this.clutId = reader.readShort(); // Provably unused. Probably garbage data.
        }

        // Can do this before texturePage is set, but after bitDepth is set.
        this.paddedWidth = (short) (this.paddedWidth * getWidthMultiplier());

        // Validate page short.
        if (getTexturePageShort() != readPage) // Verify this is both read and calculated properly.
            throw new RuntimeException("Calculated tpage short as " + getTexturePageShort() + ", Real: " + readPage + "!");

        if (isPtToolkitFlags()) {
            warnAboutInvalidBitFlags(this.flags & 0xFFFF, PT_VALIDATION_FLAGS, toString());
        } else if (isMediEvilFlags()) {
            warnAboutInvalidBitFlags(this.flags & 0xFFFF, MEDIEVIL_VALIDATION_FLAGS, toString());
        } else {
            warnAboutInvalidBitFlags(this.flags & 0xFFFF, VALIDATION_FLAGS, toString());
        }

        short readU = reader.readUnsignedByteAsShort();
        short readV = reader.readUnsignedByteAsShort();
        this.unpaddedWidth = getUnpaddedSize(reader.readByte());
        this.unpaddedHeight = getUnpaddedSize(reader.readByte());

        int paddingX = this.paddedWidth - this.unpaddedWidth;
        int paddingY = this.paddedHeight - this.unpaddedHeight;
        this.paddingAmount = paddingY;
        if (paddingX != paddingY && getGameInstance().isPC()) // This is never known to happen.
            getLogger().warning("Padding XY mismatch! [%d vs %d]", paddingX, paddingY);

        // The PlayStation API technically supports images > 256x256, although they will just be chopped off in-game.
        // This means we want to roughly approximate their padding.
        // This is definitely not perfect, but it's good enough.
        if (isPsxMode()) {
            if (this.paddedHeight > VloImage.MAX_IMAGE_DIMENSION && this.unpaddedHeight == VloImage.MAX_IMAGE_DIMENSION) {
                this.unpaddedHeight = (short) (this.paddedHeight - Math.max(0, calculatePaddingY()));
                this.paddingAmount = paddingY = this.paddedHeight - this.unpaddedHeight; // Update to reflect new changes.
            }

            if (this.paddedWidth > VloImage.MAX_IMAGE_DIMENSION && this.unpaddedWidth == VloImage.MAX_IMAGE_DIMENSION) {
                this.unpaddedWidth = (short) (this.paddedWidth - Math.max(0, calculatePaddingX()));
                paddingX = this.paddedWidth - this.unpaddedWidth; // Update to reflect new changes.
            }
        }


        // Validate calculated data.
        int testPadX = calculatePaddingX();
        if (paddingX != testPadX && testPadX != -1)
            getLogger().warning("Calculated paddingX (%d) did not match the expected paddingX (%d)! [%d, %d|%d, UV: %d, %d] [%s]", testPadX, paddingX, this.unpaddedWidth, getExpandedVramX(), this.vramX, getU(), getV(), getFlagDisplay());

        int testPadY = calculatePaddingY();
        if (paddingY != testPadY && testPadY != -1)
            getLogger().warning("Calculated paddingY (%d) did not match the expected paddingY (%d)! [%dx%d, %dx%d, (VRAM: %d/%d, %d), UV: %d, %d] [%s]", testPadY, paddingY, this.unpaddedWidth, this.unpaddedHeight, this.paddedWidth, this.paddedHeight, getExpandedVramX(), this.vramX, this.vramY, getU(), getV(), getFlagDisplay());

        boolean hitXTest = calculateHitX();
        if (testFlag(FLAG_HIT_X) != hitXTest)
            getLogger().warning("HitX Mismatch! [%b, %b], Width (Unpadded: %d, Padded: %d Expanded: %d), VramX: %d (%d), UV: %d, %d", testFlag(FLAG_HIT_X), hitXTest, this.unpaddedWidth, this.paddedWidth, getUnitWidth(), this.vramX, getExpandedVramX(), getU(), getV());

        boolean hitYTest = calculateHitY();
        if (testFlag(FLAG_HIT_Y) != hitYTest)
            getLogger().warning("HitY Mismatch! [%b, %b], Size: %dx%d, Vram Pos: (%d, %d), UV: %d, %d", testFlag(FLAG_HIT_Y), hitYTest, this.unpaddedHeight, this.paddedHeight, this.vramX, this.vramY, getU(), getV());

        if (readU != getU() || readV != getV())
            getLogger().warning("UV Mismatch at image %d! [%d,%d] [%d,%d] -> %dx%d, %dx%d, %04X [%d, %d]",
                    Utils.getLoadingIndex(this.parent.getImages(), this), readU, readV, getU(), getV(),
                    this.unpaddedWidth, this.unpaddedHeight, this.paddedWidth, this.paddedHeight, getFlags(),
                    (this.paddedWidth - this.unpaddedWidth), (this.paddedHeight - this.unpaddedHeight));
    }

    /**
     * Returns true iff the bit flags enabled for this image are PT_TOOLKIT era flags.
     */
    public boolean isPtToolkitFlags() {
        return getGameInstance().getGameType().isAtLeast(SCGameType.MEDIEVIL2);
    }

    /**
     * Returns true iff the bit flags enabled for this image are MediEvil 1 flags.
     */
    public boolean isMediEvilFlags() {
        return getGameInstance().isMediEvil();
    }



    private static short getUnpaddedSize(byte value) {
        return (short) (value == 0 ? 256 : (value & 0xFF));
    }

    /**
     * Load image data.
     * @param reader The reader to read image data from.
     */
    void readImageData(DataReader reader, VloClutList clutList) {
        if (this.tempImageDataPointer < 0)
            throw new RuntimeException("Cannot read image data, the image data pointer is invalid.");

        requireReaderIndex(reader, this.tempImageDataPointer, "Expected image data");
        this.tempImageDataPointer = -1;

        // Setup pixel buffer.
        int pixelCount = this.paddedWidth * this.paddedHeight;
        if (this.pixelBuffer == null || this.pixelBuffer.length != pixelCount)
            this.pixelBuffer = new int[pixelCount];

        // Read image.
        setClut(null);
        this.expectedStpBlackBitPsx = false; // Updated by loadClut(), okay if bitDepth is 15bit.
        this.expectedStpNonBlackBitPsx = false; // Updated by loadClut(), okay if bitDepth is 15bit.
        this.anyFullyBlackPixelPresentPC = false;
        if (isPsxMode()) {
            if (this.bitDepth == PsxImageBitDepth.SBGR1555) { // Used heavily in pre-recode Frogger, and occasionally seen in other places. (Example: Frogger USA Demo, Frogger Sony Presentation, etc.)
                read15BitImage(reader);
            } else if (this.bitDepth == PsxImageBitDepth.CLUT8) { // Used in Frogger PSX Retail NTSC, such as in STARTNTSC.VLO or the level select vlo files.
                loadClut(clutList);
                for (int i = 0; i < pixelCount; i++)
                    this.pixelBuffer[i] = loadPaletteColor(this.clut, reader.readByte() & 0xFF);
            } else { // 4bit (normal) mode.
                if ((pixelCount % 2) > 0) // Indicates a problem, but shouldn't be fatal/prevent the image from loading.
                    getLogger().warning("The calculated number of pixels was odd! This suggests a 4-bit image which may not be properly encoded!");

                loadClut(clutList);
                for (int i = 0; i < pixelCount; i += 2) { // We read two pixels per byte.
                    byte value = reader.readByte();
                    int lowPixel = value & 0x0F;
                    int highPixel = (value & 0xFF) >>> 4;

                    this.pixelBuffer[i] = loadPaletteColor(this.clut, lowPixel);
                    this.pixelBuffer[i + 1] = loadPaletteColor(this.clut, highPixel);
                }
            }
        } else { // PC
            // PC version needs full black to be set to zero.
            boolean nonZeroAlphaWarningShown = false;
            byte lastNonZeroAlphaWarningShown = 0x00;
            for (int i = 0; i < pixelCount; i++) {
                byte alpha = reader.readByte();
                byte blue = reader.readByte();
                byte green = reader.readByte();
                byte red = reader.readByte();

                // Reverse engineering Frogger v3.0e shows that the alpha byte is NEVER accessed. It goes completely unused.
                // What a shame, they easily could have used it to control transparency, instead of the "black is transparent" hack.
                if (alpha != 0 && (!nonZeroAlphaWarningShown || lastNonZeroAlphaWarningShown != alpha)) {
                    getLogger().severe("Found non-zero alpha: %08X, %b, %b", alpha, testFlag(FLAG_TRANSLUCENT), testFlag(FLAG_BLACK_IS_TRANSPARENT));
                    nonZeroAlphaWarningShown = true;
                    lastNonZeroAlphaWarningShown = alpha;
                } else { // Alpha is treated as 0xFF by the game, unless the pixel is full black.
                    alpha = (byte) 0xFF;
                }

                this.pixelBuffer[i] = ColorUtils.toARGB(red, green, blue, alpha);
                if ((this.pixelBuffer[i] & PSXClutColor.ARGB8888_TO5BIT_COLOR_MASK) == 0)
                    this.anyFullyBlackPixelPresentPC = true;
            }
        }

        updatePsxPixelInfo();
        this.clutFogEnabled = this.clut != null && this.clut.isFogEnabled();

        int firstClutColor = this.clut != null ? loadClutColor(this.clut.getColor(0)) : getTransparentPaddingPixel();
        generatePadding(this.pixelBuffer, PaddingOperation.VALIDATE, firstClutColor);
    }

    private void updatePsxPixelInfo() {
        this.anyStpPixelInversionPsx = false;
        this.anyFullyTransparentPixelPresentPsx = false;
        if (!isPsxMode())
            return;

        // Test for transparent pixels.
        for (int i = 0; i < this.pixelBuffer.length; i++) {
            int pixelColor = this.pixelBuffer[i];
            int alpha = ColorUtils.getAlphaInt(pixelColor);
            boolean fullBlack = ((pixelColor & PSXClutColor.ARGB8888_TO5BIT_COLOR_MASK) == 0);
            boolean stpBit = (fullBlack ? this.expectedStpBlackBitPsx : this.expectedStpNonBlackBitPsx) ^ (alpha == IMAGE_ALPHA_INVERTED_STP_BIT);
            if (alpha == IMAGE_ALPHA_INVERTED_STP_BIT) // stpBit being true is a transparent enabler, and fullBlack yields transparency.
                this.anyStpPixelInversionPsx = true;
            if (fullBlack && !stpBit) // Full black and non-STP -> this pixel is fully transparent regardless of psxSemiTransparency mode enablement.
                this.anyFullyTransparentPixelPresentPsx = true;

            if (this.anyStpPixelInversionPsx && this.anyFullyTransparentPixelPresentPsx)
                break;
        }
    }

    private int getTransparentPaddingPixel() {
        return isPsxMode() ? PADDING_TRANSPARENT_PIXEL_PSX : PADDING_TRANSPARENT_PIXEL_PC;
    }

    private void loadClut(VloClutList clutList) {
        setClut(clutList.getClutFromId(this.clutId, true));
        validateClut();
    }

    private void validateClut() {
        // Determine the STP Bit state.

        // These games have images with varying STP bits, and ARE supported despite it.
        //  - MediEvil II
        //  - C-12 Final Resistance 0.03a

        // Tested against:
        //  - Old Frogger PSX Milestone 3
        //  - Frogger PSX Alpha
        //  - Frogger PSX Build 02
        //  - Frogger PSX Build 20
        //  - Frogger PSX Build 49
        //  - Frogger PSX Build 71
        //  - Beast Wars NTSC Prototype
        //  - Beast Wars NTSC Release
        //  - Beast Wars PAL Release
        //  - MediEvil 0.31
        //  - MediEvil NTSC Release
        //  - MoonWarrior
        VloClut clut = this.clut;
        this.expectedStpBlackBitPsx = false;
        this.expectedStpNonBlackBitPsx = false;
        if (clut == null || clut.getColorCount() == 0)
            return;

        boolean foundFirstBlackStpBit = false;
        boolean foundFirstNonBlackStpBit = false;
        int colorCount = clut.getColorCount();
        int blackMatches = 0, nonBlackMatches = 0;
        int blackMismatches = 0, nonBlackMismatches = 0;
        for (int i = 0; i < colorCount; i++) {
            PSXClutColor color = clut.getColor(i);
            if (color.isFullBlack()) {
                if (!foundFirstBlackStpBit) {
                    foundFirstBlackStpBit = true;
                    this.expectedStpBlackBitPsx = color.isStp();
                    blackMatches++;
                } else if (color.isStp() ^ this.expectedStpBlackBitPsx) {
                    blackMismatches++;
                } else {
                    blackMatches++;
                }
            } else if (!foundFirstNonBlackStpBit) {
                foundFirstNonBlackStpBit = true;
                this.expectedStpNonBlackBitPsx = color.isStp();
                nonBlackMatches++;
            } else if (color.isStp() ^ this.expectedStpNonBlackBitPsx) {
                nonBlackMismatches++;
            } else {
                nonBlackMatches++;
            }
        }

        // Pick a desirable default STP bit based on which bit is more common.
        if (!foundFirstBlackStpBit)
            this.expectedStpBlackBitPsx = !testFlag(FLAG_BLACK_IS_TRANSPARENT);
        if (!foundFirstNonBlackStpBit)
            this.expectedStpNonBlackBitPsx = testFlag(FLAG_TRANSLUCENT);

        if (getGameInstance().isPreviouslySavedByFrogLord() || isPixelStpBitFlipAllowed()) {
            if (blackMismatches > blackMatches /*&& foundFirstNonBlackStpBit*/)
                this.expectedStpBlackBitPsx = !this.expectedStpBlackBitPsx;
            if (nonBlackMismatches > nonBlackMatches /*&& foundFirstNonBlackStpBit*/)
                this.expectedStpNonBlackBitPsx = !this.expectedStpNonBlackBitPsx;
        } else if (blackMismatches > 0 || nonBlackMismatches > 0) {
            // Warn if we find any situation which breaks the assumptions we believe to be true.
            getLogger().warning("STP Bit Mismatch(es) in %s. Matches: (Black: %d/%d, Non-Black: %d/%d)", clut, blackMatches, blackMismatches, nonBlackMatches, nonBlackMismatches);
            for (int i = 0; i < colorCount; i++) {
                PSXClutColor color = clut.getColor(i);
                getLogger().warning(" CLUT Color %d: [Full Black: %b], %s", i, color.isFullBlack(), color);
            }
        }

        printStpTests();
    }

    private void read15BitImage(DataReader reader) {
        this.expectedStpBlackBitPsx = false;
        this.expectedStpNonBlackBitPsx = false;
        boolean foundFirstBlackStpBit = false;
        boolean foundFirstNonBlackStpBit = false;
        int blackMatches = 0, nonBlackMatches = 0;
        int blackMismatches = 0, nonBlackMismatches = 0;
        PSXClutColor tempColor = new PSXClutColor();
        for (int i = 0; i < this.pixelBuffer.length; i++) {
            PSXClutColor color = tempColor.fromShort(reader.readShort()); // Read the next pixel.

            if (color.isFullBlack()) {
                if (!foundFirstBlackStpBit) {
                    foundFirstBlackStpBit = true;
                    this.expectedStpBlackBitPsx = color.isStp();
                    blackMatches++;
                } else if (color.isStp() ^ this.expectedStpBlackBitPsx) {
                    blackMismatches++;
                } else {
                    blackMatches++;
                }
            } else if (!foundFirstNonBlackStpBit) {
                foundFirstNonBlackStpBit = true;
                this.expectedStpNonBlackBitPsx = color.isStp();
                nonBlackMatches++;
            } else if (color.isStp() ^ this.expectedStpNonBlackBitPsx) {
                nonBlackMismatches++;
            } else {
                nonBlackMatches++;
            }

            this.pixelBuffer[i] = loadClutColor(color);
        }

        // Pick a desirable default STP bit based on which bit is more common.
        if (!foundFirstBlackStpBit)
            this.expectedStpBlackBitPsx = !testFlag(FLAG_BLACK_IS_TRANSPARENT);
        if (!foundFirstNonBlackStpBit)
            this.expectedStpNonBlackBitPsx = testFlag(FLAG_TRANSLUCENT);

        if (getGameInstance().isPreviouslySavedByFrogLord()) { // This appears to be fine on MediEvil II and C-12, even if non-15BPP images needed special handling.
            // Pick a desirable default STP bit based on which bit is more common.
            if (blackMismatches > blackMatches)
                this.expectedStpBlackBitPsx = !this.expectedStpBlackBitPsx;
            if (nonBlackMismatches > nonBlackMatches)
                this.expectedStpNonBlackBitPsx = !this.expectedStpNonBlackBitPsx;
        } else if (blackMismatches > 0 || nonBlackMismatches > 0) {
            // Warn if we find any situation which breaks the assumptions we believe to be true.
            getLogger().warning("STP Bit Mismatch. (Black Mismatches: %d, Non-Black Mismatches: %b) %s", blackMismatches, nonBlackMismatches);
        }

        printStpTests();
    }

    @SuppressWarnings({"UnnecessaryReturnStatement", "CommentedOutCode"})
    private void printStpTests() {
        // The following flag behavior has been tested in:
        // - Old Frogger (0 mismatches)
        // - Frogger NTSC (10 mismatches, 1 non-black mismatch)
        // - MediEvil NTSC (34 black mismatches, 37 non-black mismatches)
        // - Beast Wars PAL (64 black mismatches, 0 non-black mismatches)
        // - Moon Warrior (0 mismatches)

        // The following games are ignored/do not come down this code-path because these games do not appear to use a default stp bit in the original data.
        // - MediEvil II NTSC
        // - C-12 Final Resistance NTSC
        if (isPixelStpBitFlipAllowed())
            return;

        // The mismatches are likely unimportant/do not actually suggest this check is wrong per-say.
        // For an explanation of why this is, see the documentation in setFlag().
        // These warnings are disabled for general use of FrogLord, because they serve no purpose for anyone who is not a FrogLord developer.
        /*
        if (this.expectedStpBlackBitPsx == testFlag(FLAG_BLACK_IS_TRANSPARENT))
            getLogger().warning("Expected Black STP: %b, Translucent: %b [%s]", this.expectedStpBlackBitPsx, testFlag(FLAG_BLACK_IS_TRANSPARENT), this.abr);
        if (this.expectedStpNonBlackBitPsx != testFlag(FLAG_TRANSLUCENT))
            getLogger().warning("Expected Non-Black STP: %b, Translucent: %b [%s]", this.expectedStpNonBlackBitPsx, testFlag(FLAG_TRANSLUCENT), this.abr);
         */
    }

    @Override
    public String getCollectionViewDisplayName() {
        String originalName = getName();
        return getLocalImageID() + ": " + (originalName != null ? originalName : "")
                + " (ID: " + this.textureId + ")";
    }

    @Override
    public String getCollectionViewDisplayStyle() {
        return null;
    }

    @Override
    public Image getCollectionViewIcon() {
        return toFXImage(DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS);
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        // TODO: Implement Import.
        //  -> "Replace Image": Imports with the image's existing settings.
        //  -> on the image list add icon, -> "Import Images" -> Show a menu where you can pick individual images or full directories. Perhaps allow selecting multiple at once.
        // TODO: Implement Export.
        // TODO: Implement Change Bit Depth (Have window previewing the images using the previous padding window template) (PSX Only)
    }

    private enum PaddingOperation {
        VALIDATE, APPLY
    }

    // NOTE: unpaddedHeight and paddedHeight should be up-to-date when calling this function.
    private void generatePadding(int[] pixelBuffer, PaddingOperation operation, int firstClutColor) {
        int padMinX = getLeftPadding();
        int padMaxX = Math.max(padMinX, padMinX + this.unpaddedWidth - 1); // DO NOT USE getRightPadding()! That includes the hitX calculation, which should NOT impact padding.
        int padMinY = getUpPadding();
        int padMaxY = Math.max(padMinY, padMinY + this.unpaddedHeight - 1); // DO NOT USE getDownPadding()! That includes the hitX calculation, which should NOT impact padding.

        // Determine padding right boundary (which part uses clut color 0)
        int paddingX = (this.paddedWidth - this.unpaddedWidth);
        int paddingY = (this.paddedHeight - this.unpaddedHeight);
        int emptyRightPaddingX = Math.max(0, paddingX - paddingY); // paddingY = paddingX - alignmentPaddingX, so this calculates the alignmentPadding.

        // Set to false so the padding transparency can be determined.
        if (operation == PaddingOperation.VALIDATE)
            this.paddingTransparent = true;

        // Generate padding.
        boolean firstPixelMismatch = false;
        for (int i = 0; i < pixelBuffer.length; i++) {
            int paddedColor = getPaddingColor(pixelBuffer, i, this.paddedWidth, padMinX, padMaxX, padMinY, padMaxY, paddingX, paddingY, emptyRightPaddingX, firstClutColor);
            if (operation == PaddingOperation.VALIDATE && paddedColor != pixelBuffer[i]) {
                if (!firstPixelMismatch) {
                    firstPixelMismatch = true;

                    // Start over with transparency disabled.
                    if (pixelBuffer[i] != getTransparentPaddingPixel()) {
                        this.paddingTransparent = false;
                        i = -1;
                        continue;
                    }
                }

                // This warning will be hit by images which have width > 256.
                // These images are not valid, never rendered/used, and do not report accurate original dimensions to FrogLord, due to being represented in an 8-bit number.
                // We try our best to validate these images, but ultimately, they are difficult to work with due to the lack of accurate information.
                // As such, we ignore padding failures on these images specifically, because these warnings are not helpful / do not actually indicate the algorithm is inaccurate.
                if (this.paddedWidth <= MAX_IMAGE_DIMENSION)
                    getLogger().warning("Pixel[%d,%d] padding was expected to be %08X, but was calculated to be %08X. (PadX: %d, PadY: %d, Stripped PadX: %d, CLUT Index: %d)", i % this.paddedWidth, i / this.paddedWidth, this.pixelBuffer[i], paddedColor, paddingX, paddingY, emptyRightPaddingX, this.clut != null ? this.clut.getColorIndex(getClutColor(new PSXClutColor(), i), false) : Integer.MAX_VALUE);
            } else if (operation == PaddingOperation.APPLY) {
                if ((paddedColor & PSXClutColor.ARGB8888_TO5BIT_COLOR_MASK) == 0 && !isPsxMode())
                    this.anyFullyBlackPixelPresentPC = true;

                pixelBuffer[i] = paddedColor;
            }
        }

        if (operation == PaddingOperation.APPLY)
            updatePsxPixelInfo();
    }

    private boolean isPaddingPixel(int pixelIndex) {
        int x = (pixelIndex % this.paddedWidth);
        int y = (pixelIndex / this.paddedWidth);
        int padMinX = getLeftPadding();
        int padMaxX = padMinX + this.unpaddedWidth; // DO NOT USE getRightPadding()! That includes the hitX calculation, which should NOT impact padding.
        int padMinY = getUpPadding();
        int padMaxY = padMinY + this.unpaddedHeight; // DO NOT USE getDownPadding()! That includes the hitX calculation, which should NOT impact padding.
        return x < padMinX || x > padMaxX || y < padMinY || y > padMaxY;
    }

    @Override
    public void save(DataWriter writer) {
        int widthMultiplier = getWidthMultiplier();
        if ((this.paddedWidth % widthMultiplier) != 0) // Shouldn't be possible to trigger this anymore.
            getGameInstance().showWarning(getLogger(), "Image skew detected.", "%s has a width of %d. Because it is mode %s, and the width is not a multiple of %d, the image will be skewed!", getIdentifier(), this.paddedWidth, this.bitDepth, widthMultiplier);

        writer.writeShort(this.vramX);
        writer.writeShort(this.vramY);

        writer.writeShort((short) (this.paddedWidth / widthMultiplier));
        writer.writeShort(this.paddedHeight);
        this.tempImageDataPointer = writer.writeNullPointer();
        writer.writeShort(this.textureId);
        writer.writeShort(getTexturePageShort());

        updateFlags(); // Ensure flags which get saved are up-to-date.
        short clutId = this.clut != null ? this.clut.getClutID() : this.clutId;
        if (isPsxMode()) {
            writer.writeShort(clutId);
            writer.writeShort(this.flags);
        } else {
            writer.writeShort(this.flags);
            writer.writeShort(clutId);
        }

        writer.writeUnsignedByte(getU());
        writer.writeUnsignedByte(getV());
        writer.writeUnsignedByte(getUnpaddedSizeByte(this.unpaddedWidth));
        writer.writeUnsignedByte(getUnpaddedSizeByte(this.unpaddedHeight));
    }

    private void updateFlags() {
        setFlag(FLAG_HIT_X, calculateHitX());
        setFlag(FLAG_HIT_Y, calculateHitY());
    }

    /**
     * Save extra data.
     * @param writer The writer to save data to.
     */
    void writeImageData(DataWriter writer, VloClutList clutList) {
        if (this.tempImageDataPointer < 0)
            throw new RuntimeException("Cannot write image data, the image data pointer is invalid.");

        writer.writeAddressTo(this.tempImageDataPointer);
        writeImageBytes(writer, clutList);

        this.tempImageDataPointer = -1;
    }

    private boolean getExpectedStpBit(PSXClutColor color) {
        return (color.isFullBlack() ? this.expectedStpBlackBitPsx : this.expectedStpNonBlackBitPsx);
    }

    private PSXClutColor getClutColor(PSXClutColor tempColor, int pixelIndex) {
        return getClutColor(this.pixelBuffer, tempColor, pixelIndex);
    }

    private PSXClutColor getClutColor(int[] pixelBuffer, PSXClutColor tempColor, int pixelIndex) {
        int rawColor = pixelBuffer[pixelIndex];
        PSXClutColor color = tempColor.fromRGB(rawColor, false);
        boolean stpBit = getExpectedStpBit(color);

        // stp bit flip!
        // Handle colors properly.
        // The purpose of this is to allow flipping the STP bit on individual pixels, while having very solid import capabilities.
        byte alpha = ColorUtils.getAlpha(rawColor);
        if (alpha == IMAGE_ALPHA_INVERTED_STP_BIT) {
            stpBit = !stpBit;
        } else if (alpha != IMAGE_ALPHA_REGULAR_STP_BIT_TRANS && alpha != IMAGE_ALPHA_REGULAR_STP_BIT_OPAQUE) {
            throw new UnsupportedOperationException("Unsupported alpha value: " + alpha);
        }

        color.setStp(stpBit);
        return color;
    }

    private void regenerateClut(VloClutList clutList, PSXClutColor tempColor, boolean ignorePadding) {
        if (!isPsxMode() || this.bitDepth == PsxImageBitDepth.SBGR1555) {
            setClut(null);
            return; // No need to refresh a clut.
        }

        // Quantize down to the desired image bit depth.
        // This method should only be called after padding has been generated/applied.
        // Padding is capable of adding new colors, so we must quantize only after padding.
        // Colors of different alpha must not be merged together as it would break STP bits.
        int clutWidth = getPaletteWidth(this.bitDepth);
        if (clutWidth > 0)
            OctreeQuantizer.quantizeImage(this.pixelBuffer, clutWidth, false);
            //this.pixelBuffer = MedianCutQuantizer.quantizeARGB8888Buffer(this.pixelBuffer, colorCount);

        if (tempColor == null)
            tempColor = new PSXClutColor();

        // Find unique clut colors.
        List<PSXClutColor> newColors = new ArrayList<>();
        for (int i = 0; i < this.pixelBuffer.length; i++) {
            PSXClutColor color = getClutColor(tempColor, i);
            if ((!ignorePadding || !isPaddingPixel(i)) && !newColors.contains(color))
                newColors.add(color.clone());
        }

        // Sort colors so we can equality-test clut colors.
        // Do NOT change this sorting order without also fixing replaceImage()'s firstClutColor calculation.
        newColors.sort(Comparator.comparingInt(this::loadClutColor));

        // For any unfilled part of the clut, fill it with black.
        PSXClutColor unused = new PSXClutColor();
        unused.setStp(this.expectedStpBlackBitPsx);
        while (clutWidth > newColors.size())
            newColors.add(unused.clone());

        // Generate fog.
        int clutHeight = 1;
        if (this.clutFogEnabled) {
            clutHeight = VloClut.CLUT_FOG_HEIGHT;

            // Validate there are not too many colors!
            if (newColors.size() > clutWidth) // This should not happen, it means an invalid image has been applied.
                throw new RuntimeException("Tried to save invalid image data (" + this + ") which contained too many colors for its bitDepth (" + this.bitDepth + "). [Max Allowed Colors: " + clutWidth + ", Actual Colors: " + newColors.size() + "]");

            // Generate fog colors.
            for (int row = 1; row < VloClut.CLUT_FOG_HEIGHT; row++)
                for (int column = 0; column < clutWidth; column++)
                    newColors.add(VloClut.calculateFogColor(newColors.get(column), row, null));
        }

        // Validate there are not too many colors!
        int maxColors = (clutWidth * clutHeight);
        if (newColors.size() > maxColors) // This should not happen, it means an invalid image has been applied.
            throw new RuntimeException("Tried to save invalid image data (" + this + ") which contained too many colors for its bitDepth (" + this.bitDepth + "). [Max Allowed Colors: " + maxColors + ", Actual Colors: " + newColors.size() + "]");

        VloClut foundClut = clutList.getClut(newColors);
        if (foundClut != null) {
            setClut(foundClut);
        } else {
            VloClut newClut = this.clut; // Use previous clut if possible
            if (newClut == null || newClut.getImages().size() > 1 || clutWidth > newClut.getWidth() || clutHeight > newClut.getHeight() || this.clutFogEnabled != newClut.isFogEnabled())
                newClut = new VloClut(this.parent);
            newClut.loadColors(clutWidth, clutHeight, newColors, this.clutFogEnabled);
            setClut(newClut); // Registering the clut should generate its position.
        }
    }

    private void writeImageBytes(DataWriter writer, VloClutList clutList) {
        // Write 32-bit color PC image.
        if (!isPsxMode()) {
            // Byte Order ARGB -> Byte Order RGBA
            for (int i = 0; i < this.pixelBuffer.length; i++)
                writer.writeInt(this.pixelBuffer[i] << 8); // Alpha is always 0 at rest, so only shifting by 8 is necessary.

            return;
        }

        // Write non-CLUT 15-bit image.
        PSXClutColor tempColor = new PSXClutColor();
        if (this.bitDepth == PsxImageBitDepth.SBGR1555) {
            for (int i = 0; i < this.pixelBuffer.length; i++)
                writer.writeShort(getClutColor(tempColor, i).toShort());

            return;
        }

        // this.clut is up to date, having been updated at any point that an image is imported.
        clutList.addClut(this.clut);
        if (this.bitDepth == PsxImageBitDepth.CLUT8) {
            for (int i = 0; i < this.pixelBuffer.length; i++)
                writer.writeByte((byte) this.clut.getColorIndex(getClutColor(tempColor, i), true));
        } else if (this.bitDepth == PsxImageBitDepth.CLUT4) {
            for (int i = 0; i < this.pixelBuffer.length; i += 2) {
                PSXClutColor color = getClutColor(tempColor, i);
                int color1Index = this.clut.getColorIndex(color, true);
                color = getClutColor(tempColor, i + 1);
                int color2Index = this.clut.getColorIndex(color, true);
                writer.writeByte((byte) (color1Index | (color2Index << 4)));
            }
        } else {
            throw new RuntimeException("Could not handle clut mode: " + this.bitDepth);
        }
    }

    private static short getUnpaddedSizeByte(short value) {
        if (value <= 0)
            throw new IllegalArgumentException("value must not be less than zero! (Value: " + value + ")");

        return (short) (value >= VloImage.MAX_IMAGE_DIMENSION ? 0 : (value & 0xFF));
    }

    @Override
    public ILogger getLogger() {
        String loggerInfo = getName();
        if (loggerInfo == null)
            loggerInfo = Integer.toString(getTextureId());

        int localImageId = getLocalImageID();
        if (localImageId == -1)
            localImageId = this.parent.getImages().size();

        return new AppendInfoLoggerWrapper(this.parent.getLogger(), loggerInfo + "/" + localImageId,
                AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
    }

    @Override
    public String toString() {
        String name = getName();
        return "VloImage{" + (name != null ? name + "," : "")
                + "id=" + this.textureId
                + (this.parent != null ? "@" + this.parent.getFileDisplayName() : "") + "}";
    }

    private String getFlagDisplay() {
        StringBuilder builder = new StringBuilder("ABR: ");
        builder.append(this.abr);
        builder.append(", Mode: ");
        builder.append(this.bitDepth);

        addFlag(builder, FLAG_TRANSLUCENT, "Translucent");
        addFlag(builder, FLAG_HIT_X, "HitX");
        addFlag(builder, FLAG_HIT_Y, "HitY");
        addFlag(builder, FLAG_REFERENCED_BY_NAME, "Name Reference");
        addFlag(builder, FLAG_BLACK_IS_TRANSPARENT, "Black Transparent");
        addFlag(builder, FLAG_2D_SPRITE, "2DSprite");
        return builder.toString();
    }

    private void addFlag(StringBuilder builder, int flag, String name) {
        if (testFlag(flag)) {
            if (builder.length() > 0)
                builder.append(", ");
            builder.append(name);
        }
    }

    /**
     * Tests if the game allows individual clut color entries to flip the STP bit.
     * This is extremely rare, only having been seen in MediEvil II and C-12 Final Resistance.
     * FrogLord supports this, but it might be confusing to users in exported images.
     * I don't think we have any better options.
     * @return stpBitFlipAllowed
     */
    private boolean isPixelStpBitFlipAllowed() {
        return getGameInstance().getGameType().isAtLeast(SCGameType.MEDIEVIL2);
    }

    /**
     * Calculates whether the HIT_X flag should be set if the image were to be saved now.
     * Validated to have near perfectly consistent behavior with all known original VLO files.
     * Relies upon up-to-date:
     *  - Padding dimensions
     *  - Vram X position
     *  - bitDepth
     */
    public boolean calculateHitX() {
        // The purpose of HIT_X is to prevent overflow of u8 values, so that textures render correctly.

        // Validated perfect match against:
        //  - Pre-Recode Frogger PC (Milestone 3)
        //  - Frogger PC Alpha (June 1997)
        //  - Frogger PC Beta (July 1997)
        //  - Frogger PC Prototype (September 1997)
        //  - Frogger PC v3.0e (Retail)
        //  - Frogger PC Demo (November 1997)
        //  - Frogger PC Demo (December 1997)
        //  - Beast Wars PC (Retail)
        //  - Beast Wars PC (Demo)
        if (getGameInstance().isPC())
            return (((this.vramX + this.paddedWidth) % PC_VRAM_PAGE_WIDTH) == 0);

        // Validated perfect match against:
        //  - Frogger PSX Milestone 3 (Pre-Recode)
        //  - MediEvil Rolling Demo
        //  - MediEvil ECTS
        //  - Frogger PSX Build 71 (Retail NTSC)
        //  - Beast Wars PSX NTSC
        //  - Beast Wars PSX PAL
        //  - MediEvil Retail NTSC
        //  - MoonWarrior (Build 0.05a)
        //  - MediEvil II 0.19
        //  - MediEvil II USA Retail 1.1Q
        //  - C-12 Final Resistance (E3 Build) One failure... Looks like there's supposed to be padding, so perhaps this was a problem internal to Vorg.
        //  - C-12 Final Resistance (Beta Candidate 3)
        //  - C-12 Final Resistance (Retail NTSC)

        // Don't... ask me how this works... This algorithm was made by the original devs.
        // I figured it out through quite a bit of trial and error, until it perfectly reproduced the flags seen in the builds above.
        // It is unclear to me if this is a good (simple/non-wasteful) implementation of this fix. To me, it feels like there's a simpler way, but we'll do this for consistency.
        boolean alignedToEdgeX = (((this.vramX + getUnitWidth()) % PsxVram.PSX_VRAM_PAGE_UNIT_WIDTH) == 0);
        if (!alignedToEdgeX)
            return false;

        int startU = getU();
        int endU = startU + this.unpaddedWidth;

        // This was probably to ensure pixel padding works right or something.
        // But when PT Toolkit rolled around (or sometime after Med1 but before MoonWarrior), I think they realized that this is pointless since any pixels here would be padding/not part of the image.
        if (endU == 0xFF && (this.unpaddedWidth + 1 != this.paddedWidth || getGameInstance().isMediEvil()) && !getGameInstance().getGameType().isAtLeast(SCGameType.MOONWARRIOR))
            endU++; //

        // C-12 has a handful of images which look like they should be HitX, but aren't.
        // The common thread between them is that their width is a multiple of 64, but I suspect this just isn't a large enough sample size to say definitively this is why.
        if (getGameInstance().getGameType().isAtLeast(SCGameType.C12) && (this.unpaddedWidth % 64) == 0)
            return false;

        return (endU & 0xFF) != endU;
    }

    /**
     * Calculates whether the HIT_Y flag should be set if the image were to be saved now.
     * Validated to have perfectly consistent behavior with all known original VLO files.
     * Relies upon up-to-date:
     *  - Padding dimensions
     *  - Vram Y position
     */
    public boolean calculateHitY() {
        // In C-12, it doesn't seem like HIT_Y can be triggered for some reason.
        // Tested:
        //  - C-12 Final Resistance (E3 Build)
        //  - C-12 Final Resistance (Beta Candidate 3)
        //  - C-12 Final Resistance (Retail Russian)
        //  - C-12 Final Resistance (Retail NTSC)
        if (getGameInstance().getGameType() == SCGameType.C12)
            return false;

        // Validated perfect match against:
        //  - Frogger PSX Milestone 3 (Pre-Recode)
        //  - Frogger PSX Alpha
        //  - Frogger PSX Build 20 NTSC
        //  - Frogger PSX Build 71 (Retail NTSC)
        //  - MediEvil Retail NTSC
        //  - Beast Wars PSX NTSC
        //  - Beast Wars PSX PAL
        //  - MoonWarrior (Build 0.05a)
        //  - MediEvil II 0.19
        //  - MediEvil II USA Retail 1.1Q
        if (getGameInstance().isPSX())
            return (((getV() + this.unpaddedHeight) % PsxVram.PSX_VRAM_PAGE_HEIGHT) == 0);

        // Validated perfect match against:
        //  - Pre-Recode Frogger PC (Milestone 3)
        //  - Frogger PC Alpha (June 1997)
        //  - Frogger PC Beta (July 1997)
        //  - Frogger PC Prototype (September 1997)
        //  - Frogger PC v3.0e (Retail)
        //  - Frogger PC Demo (November 1997)
        //  - Frogger PC Demo (December 1997)
        //  - Beast Wars PC (Retail)
        //  - Beast Wars PC (Demo)
        return (((this.vramY + this.unpaddedHeight) % PC_VRAM_PAGE_HEIGHT) == 0);
    }

    // Attempts to calculate the padding for the image.
    // Needs updated: bitDepth, unpaddedWidth, paddingAmount
    private int calculatePaddingX() {
        return calculatePaddingX(isPsxMode(), this.unpaddedWidth, this.paddingAmount, getWidthMultiplier());
    }

    // Attempts to calculate the padding for the image.
    private static int calculatePaddingX(boolean psxMode, int unpaddedWidth, int paddingAmount, int widthMultiplier) {
        // Note that padding configuration appears consistent between versions, suggesting it was configured manually.
        //int paddingY = calculatePaddingY();

        // On PC, it looks like padX might always = padY.
        // I suspect this is because PC doesn't have any of the restrictions/considerations that PSX VRAM has.
        // And since padding on PC seems to be about polygon edges (which is impacted by both vertical and horizontal padding the same) instead of VRAM alignment, it makes sense why padX might match padY.
        //  - Frogger PC Milestone 3
        //  - Frogger PC Alpha
        //  - Frogger PC July
        //  - Frogger PC September
        //  - Frogger PC v1.0 (Retail)
        //  - Frogger PC v3.0e (Retail)
        //  - Beast Wars PC Retail
        // NOTE: If calculatePaddingX() is incorrect, the result of this function will be incorrect too.
        // But, this indicates a problem with calculatePaddingX(), not this function.
        if (!psxMode)
            return paddingAmount;

        // Validated perfect match against:
        //  - Frogger PSX Milestone 3 (Pre-Recode)
        //  - Frogger PSX Sony Demo
        //  - Frogger PSX Alpha
        //  - Frogger PSX Build 02
        //  - Frogger PSX Build 11
        //  - Frogger PSX Build 20 (NTSC)
        //  - Frogger PSX Build 49
        //  - Frogger PSX Build 71 (NTSC Retail)
        //  - Frogger PSX Build 75 (PAL Retail)
        //  - MediEvil Rolling Demo
        //  - MediEvil ECTS (September 1997)
        //  - Beast Wars PSX NTSC (Retail)
        //  - Beast Wars PSX PAL (Retail)
        //  - MediEvil NTSC (Retail)
        //  - MoonWarrior 0.05a
        //  - MediEvil II 0.19 (This has failures, BUT they are due to width>256 erasing the true image dimensions, not because of an issue with the logic here.)
        //  - MediEvil II USA Retail 1.1Q (This has failures, BUT they are due to width>256 erasing the true image dimensions, not because of an issue with the logic here.)
        //  - C-12 Final Resistance PSX Build 0.03a
        //  - C-12 Final Resistance PSX Beta Candidate 3
        //  - C-12 Final Resistance PSX Master

        // Calculated mostly perfect against:
        //  - There is one exception which is that if the image is larger than width=256, the padding may be incorrect.
        //  - This is a limitation of us not actually knowing what the original image width/height was.
        //  - Images of this size have never been observed to actually be used/displayed by a game, it does not seem like they are valid.
        //  - Despite this, they still are observed/seen in some VLO files.
        return paddingAmount + getModuloReversed(unpaddedWidth + paddingAmount, widthMultiplier);
    }

    private int calculatePaddingY() {
        return this.paddingAmount;
    }

    private static int getModuloReversed(int value, int modulo) {
        int result = value % modulo;
        return (result != 0) ? (modulo - result) : 0;
    }

    private boolean isPsxMode() {
        return this.parent != null ? this.parent.isPsxMode() : getGameInstance().isPSX();
    }

    /**
     * Gets the padded width of the image, in vram width units.
     */
    public short getUnitWidth() {
        return (short) (this.paddedWidth / getWidthMultiplier());
    }

    /**
     * Gets the expanded VRAM x coordinate.
     */
    public int getExpandedVramX() {
        return isPsxMode() ? this.vramX * PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT : this.vramX;
    }

    /**
     * Gets a string which identifies this image.
     */
    public String getIdentifier() {
        StringBuilder builder = new StringBuilder("VloImage{");
        String name = getName();
        if (name != null)
            builder.append("'").append(name).append("'/");
        builder.append(getTextureId());
        return builder.append("}").toString();
    }

    /**
     * Sets the vramX value.
     * NOTE: This value expects newVramX to be in units, not in bytes or pixels. In other words, it must not be the "expanded vram X".
     * Divide by getWidthMultiplier() to convert to non-expanded form.
     * @param newVramX the new vramX value to apply
     */
    public void setVramX(int newVramX) {
        final int maxX = VloUtils.getVramUnitMaxPositionX(isPsxMode());
        if (newVramX < 0 || newVramX + getUnitWidth() > maxX)
            throw new IllegalArgumentException("The provided x coordinate would result in the image being placed at least partially outside of VRAM! (newVramX: " + newVramX + ", paddedWidth: " + this.paddedWidth  + ")");

        this.vramX = (short) newVramX;
    }

    /**
     * Sets the vramY value.
     * @param newVramY the new vramY value to apply
     */
    public void setVramY(int newVramY) {
        final int maxY = VloUtils.getVramMaxPositionY(isPsxMode());
        if (newVramY < 0 || newVramY + this.paddedHeight > maxY)
            throw new IllegalArgumentException("The provided y coordinate would result in the image being placed at least partially outside of VRAM! (newVramY: " + newVramY + ", paddedHeight: " + this.paddedHeight + ")");

        this.vramY = (short) newVramY;
    }

    /**
     * Sets the ID of the texture represented by this object.
     * @param textureId the texture ID to apply
     */
    public void setTextureId(short textureId) {
        if (textureId < 0 || textureId >= getGameInstance().getBmpTexturePointers().size())
            throw new IllegalArgumentException("The provided textureId is not a valid texture ID for this game. (ID: " + textureId + ")");

        short oldTextureId = this.textureId;
        this.textureId = textureId;
        if (oldTextureId >= 0 && oldTextureId != textureId && this.parent != null && this.parent.getImages().contains(this))
            getGameInstance().onVloTextureIdChange(this, oldTextureId, textureId);
    }

    /**
     * Sets the PSX texture ABR used for this texture.
     * @param textureAbr the abr to apply to this texture
     */
    public void setAbr(PsxAbrTransparency textureAbr) {
        if (textureAbr == null)
            throw new NullPointerException("textureAbr");
        if (this.abr == textureAbr)
            return;

        this.abr = textureAbr;
    }

    /**
     * Calculates the page this image lies in.
     * @return page
     */
    public short getPage() {
        return VloUtils.getPageFromVramPos(getGameInstance(), this.vramX, this.vramY);
    }

    /**
     * Gets the page the end of this image lies on.
     * @return endPage
     */
    public short getEndPage() {
        return VloUtils.getPageFromVramPos(getGameInstance(), this.vramX + ((this.paddedWidth - 1) / getWidthMultiplier()), this.vramY + this.paddedHeight - 1);
    }

    /**
     * Gets the tpage short for this image.
     * This information seems very similar to what's found on:
     * <a href="http://wiki.xentax.com/index.php/Playstation_TMD"/>
     * @return tpageShort
     */
    public short getTexturePageShort() {
        if (isPsxMode()) {
            return (short) ((getPage() & 0b11111) | (this.abr.ordinal() << 5) | (this.bitDepth.ordinal() << 7));
        } else {
            // Found by comparing data between Frogger PC and PSX, and referencing the game binary.
            return (short) ((getPage() & 0xFF) | (this.abr.ordinal() << 8));
        }
    }

    /**
     * Gets the width multiplier for the image.
     */
    public int getWidthMultiplier() {
        return isPsxMode() ? this.bitDepth.getPixelMultiple() : 1;
    }

    /**
     * Generates the U value used by this image.
     * @return uValue
     */
    public short getU() {
        int pageVramX = (this.vramX % VloUtils.getUnitPageWidth(isPsxMode())) * getWidthMultiplier();
        return (short) (pageVramX + getLeftPadding());
    }

    /**
     * Generates the V value used by this image.
     * @return vValue
     */
    public short getV() {
        int pageVramY = (this.vramY % VloUtils.getPageHeight(isPsxMode()));
        return (short) (pageVramY + getUpPadding());
    }

    private int loadClutColor(PSXClutColor color) {
        // NOTE: Because this is about loading the image, we are looking for alpha 0, 127, and 255. None others!
        int argb = color.toARGB(false, null);
        if (color.isStp() == getExpectedStpBit(color))
            return argb;

        // STP bit flip warning.
        if (!isPixelStpBitFlipAllowed())
            getLogger().warning("Clut color had unexpected STP bit! This feature works, but isn't expected to be used in this game. %s", color);

        return ColorUtils.setAlpha(argb, IMAGE_ALPHA_INVERTED_STP_BIT);
    }

    private int loadPaletteColor(VloClut clut, int clutIndex) {
        return loadClutColor(clut.getColor(clutIndex));
    }

    private void setClut(VloClut clut) {
        if (clut == this.clut)
            return;
        if (!isPsxMode() && clut != null)
            throw new RuntimeException("VLO must be PSX Mode to set VloClut.");

        // Remove old clut.
        if (this.clut != null)
            this.clut.removeImage(this);

        // Add new clut. (Generates the new position if necessary, so the clutId will use the proper position)
        if (clut != null)
            clut.addImage(this);

        this.clutId = clut != null && clut.getX() != -1 && clut.getY() != -1 ? clut.getClutID() : 0;
        this.clut = clut;
    }

    /**
     * Set a flag state for this image.
     * @param flag     The flag to set.
     * @param newState The new flag state.
     */
    public void setFlag(int flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return;

        if (oldState) {
            this.flags &= (short) ~flag;
        } else {
            this.flags |= (short) flag;
        }

        if ((flag & (FLAG_TRANSLUCENT | FLAG_BLACK_IS_TRANSPARENT)) != 0)
            invalidateCache();

        if (isPsxMode()) {
            // The flag behavior was tested using printStpTests(). See that function for details.
            // It is not a perfect match for the images seen within those games.
            // However, it is very close, and the images which don't match seem to have special situations (Empty image/Non-primary frame from a .gif file/etc), which could reasonably explain why they do not match.
            // This behavior makes intuitive sense to me, AND to the user. If they see a flag TRANSLUCENT, alpha 127 is a fairly reasonable behavior to attach to the flag.
            // In MediEvil, that flag is also what is used to make map terrain use alpha 127, which requires the STP bit set like this.
            // So while this isn't technically a perfect match to the data in the .VLO files, it matches enough and makes enough sense for this to be right.
            if ((flag & FLAG_BLACK_IS_TRANSPARENT) == FLAG_BLACK_IS_TRANSPARENT)
                this.expectedStpBlackBitPsx = !newState; // FLAG_BLACK_IS_TRANSPARENT true -> STP Bit false will give alpha zero.
            if ((flag & FLAG_TRANSLUCENT) == FLAG_TRANSLUCENT)
                this.expectedStpNonBlackBitPsx = newState; // FLAG_TRANSLUCENT true -> STP bits true give alpha 127.

            // This must be called after updating these booleans to keep this.anyFullyTransparentPixelPresentPsx up-to-date.
            if ((flag & (FLAG_BLACK_IS_TRANSPARENT | FLAG_TRANSLUCENT)) != 0)
                updatePsxPixelInfo();
        } else {
            if ((flag & FLAG_BLACK_IS_TRANSPARENT) == FLAG_BLACK_IS_TRANSPARENT)
                markNeedsVramRefresh(); // The image is on an inappropriate page (on PC) if this value changes.
        }
    }

    private void markNeedsVramRefresh() {
        if (this.parent != null)
            this.parent.markDirty();
    }

    /**
     * Replace this texture with a new one.
     * The image bit-depth will be maintained as its current bit-depth.
     * Padding will be automatically generated.
     * @param image The new image to use.
     */
    public void replaceImage(BufferedImage image, ProblemResponse response) {
        replaceImage(image, null, -1, testFlag(FLAG_TRANSLUCENT), response);
    }

    /**
     * Replace this texture with a new one.
     * @param image The new image to use.
     * @param bitDepth the bit-depth to import the image as. On PC, this value is ignored. A null value indicates that the pre-existing bit depth should be used.
     * @param padding the padding amount to apply to the image. If a negative value is provided, the previous padding value will be used.
     * @param translucent if the translucent flag should be set
     * @param response Controls how this function responds to a problem, if one occurs.
     */
    @SuppressWarnings("ExtractMethodRecommender")
    public void replaceImage(BufferedImage image, PsxImageBitDepth bitDepth, int padding, boolean translucent, ProblemResponse response) {
        if (image == null)
            throw new NullPointerException("image");
        if (bitDepth == null)
            bitDepth = this.bitDepth;

        int oldPaddedWidth = this.paddedWidth;
        int oldPaddedHeight = this.paddedHeight;

        // Now that the dimensions are finalized, grant easy access to them.
        int newInputImageWidth = image.getWidth();
        int newInputImageHeight = image.getHeight();

        // Ensure it's not too large.
        if (newInputImageWidth > MAX_IMAGE_DIMENSION || newInputImageHeight > MAX_IMAGE_DIMENSION) {
            // Technically the game does support holding images larger than 256x256 in a VLO, or at least the PSX version does.
            // See select1_f (ID 11) in OPTF_RAM.VLO of PSX Build 71.
            // But that image isn't actually ever displayed, since the US retail NTSC version only supports the English language, and that's a French image.
            // PSX Build 75 (the build to actually support multiple languages) fixes that image.
            // Other builds like PSX Build 20 have images like this too.
            // In other words, this is definitely not a thing that users need to be able to do, and it would just confuse them why it wasn't working if we let them do it.
            Utils.handleProblem(response, getLogger(), Level.SEVERE, "The imported image is too big. Images can be no larger than %dx%d.", MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION);
            return;
        }

        // We can only parse TYPE_INT_ARGB, so if it's not that, we must convert the image to that, so it can be parsed properly.
        image = ImageUtils.convertBufferedImageToFormat(image, BufferedImage.TYPE_INT_ARGB);

        // Get current padding, without PSX alignment.
        boolean hadPreviousImage = (this.pixelBuffer != null);
        int oldPadding = this.paddingAmount;

        // Calculate padding changes.
        // Padding calculation needs: bitDepth, unpaddedWidth
        if (this.bitDepth != bitDepth && isPsxMode()) {
            this.bitDepth = bitDepth;
            markNeedsVramRefresh();
        }

        // Apply new dimensions.
        this.unpaddedWidth = (short) newInputImageWidth;
        this.unpaddedHeight = (short) newInputImageHeight;
        int newPadding = padding >= 0 ? padding : oldPadding;

        // Configure new padding.
        this.paddedWidth = (short) (this.unpaddedWidth + calculatePaddingX(isPsxMode(), this.unpaddedWidth, newPadding, getWidthMultiplier()));
        this.paddedHeight = (short) (this.unpaddedHeight + newPadding);

        // Apply the image to this class.
        int[] newInputImageBuffer = ImageUtils.getReadOnlyPixelIntegerArray(image);
        this.pixelBuffer = new int[this.paddedWidth * this.paddedHeight];
        int dstOffset = getLeftPadding() + (getUpPadding() * this.paddedWidth);
        for (int y = 0, srcOffset = 0; y < this.unpaddedHeight; y++, dstOffset += this.paddedWidth, srcOffset += this.unpaddedWidth)
            System.arraycopy(newInputImageBuffer, srcOffset, this.pixelBuffer, dstOffset, this.unpaddedWidth);

        // Process/fix image alpha/transparency.
        // This should be done before the clut is generated.
        this.anyFullyBlackPixelPresentPC = false;
        setFlag(FLAG_TRANSLUCENT, translucent);
        if (isPsxMode()) {
            // On PSX, the BLACK_IS_TRANSPARENT flag is calculable.
            // This can be verified with VloFile.DEBUG_VALIDATE_IMAGE_EXPORT_IMPORT.
            boolean blackIsTransparent = false;
            int padOffset = getLeftPadding() + (getUpPadding() * this.paddedWidth);
            for (int y = 0; y < this.unpaddedHeight; y++, padOffset += this.paddedWidth) {
                for (int x = 0; x < this.unpaddedWidth; x++) {
                    if (ColorUtils.getAlphaInt(this.pixelBuffer[padOffset + x]) <= 85) {
                        blackIsTransparent = true;
                        break;
                    }
                }
            }

            // Collapse alphas down to the allowed values.
            setFlag(FLAG_BLACK_IS_TRANSPARENT, blackIsTransparent);
            for (int i = 0; i < this.pixelBuffer.length; i++) {
                int color = this.pixelBuffer[i];
                int alpha = ColorUtils.getAlphaInt(color);

                // This seems counter-intuitive, but it works. This is because the image itself has no bearing on the texture settings on PSX.
                // This may be annoying, but it truly is the simplest way to deal with this.
                if (alpha >= 170) {
                    // NOTE: This is ONLY enabled when !this.expectedStpBlackBitPsx,
                    //  because otherwise it will cause VloFile.DEBUG_VALIDATE_IMAGE_EXPORT_IMPORT to fail.
                    // We do not want a color imported as black to be possible to turn transparent.
                    // Since BLACK_IS_TRANSPARENT is calculated just above, this is safe.
                    if ((color & PSXClutColor.ARGB8888_TO5BIT_COLOR_MASK) == 0 && !this.expectedStpBlackBitPsx)
                        color = COLOR_CLOSEST_TO_BLACK; // Ensure black color stays as black if transparency is enabled.

                    alpha = IMAGE_ALPHA_REGULAR_STP_BIT_OPAQUE;
                } else if (alpha > 85) {
                    alpha = IMAGE_ALPHA_INVERTED_STP_BIT;
                } else /*if (alpha > 0)*/ { // Pick regular / inverted color based on what will yield a transparent pixel.
                    color = COLOR_TRUE_BLACK; // Transparent pixels must be true black.
                    alpha = IMAGE_ALPHA_REGULAR_STP_BIT_TRANS;
                }

                this.pixelBuffer[i] = ColorUtils.setAlpha(color, (byte) alpha);
            }
        } else { // PC texture.
            boolean enableTransparency = false;

            // Determine transparency.
            for (int i = 0; i < this.pixelBuffer.length; i++) {
                if (ColorUtils.getAlphaInt(this.pixelBuffer[i]) <= 127) {
                    enableTransparency = true;
                    break;
                }
            }

            // Apply to image.
            if (enableTransparency) // Only set flag true, don't set false, because this flag IS found on images without transparent pixels, for VRAM ordering purposes.
                setFlag(FLAG_BLACK_IS_TRANSPARENT, true);

            boolean transparencyFlag = testFlag(FLAG_BLACK_IS_TRANSPARENT);
            for (int i = 0; i < this.pixelBuffer.length; i++) {
                int color = this.pixelBuffer[i];
                if (ColorUtils.getAlphaInt(color) <= 127) {
                    this.pixelBuffer[i] = COLOR_TRUE_BLACK; // Set pixel to transparent. (Black is transparent)
                    this.anyFullyBlackPixelPresentPC = true; // Encoded as pure black.
                } else if ((color & PSXClutColor.ARGB8888_TO5BIT_COLOR_MASK) == 0 && transparencyFlag) { // Color is black.
                    this.pixelBuffer[i] = COLOR_CLOSEST_TO_BLACK; // Set pixel to as close to black as possible without being transparent.
                    // Since this value is not encoded as pure-black, this.anyFullyBlackPixelsPresent should not be updated.
                } else {
                    if ((color & PSXClutColor.ARGB8888_TO5BIT_COLOR_MASK) == 0) // Found a true black pixel, but BLACK_IS_TRANSPARENT is not set.
                        this.anyFullyBlackPixelPresentPC = true; // This will be encoded as pure black (by the game), so the bool must be true.

                    this.pixelBuffer[i] |= COLOR_FULL_ALPHA; // Ensure correct alpha.
                }
            }
        }

        // Generate padding.
        // Image must be applied before this point, and clut must be generated.
        // This should be done before image quantization.
        if (this.unpaddedWidth != this.paddedWidth || this.unpaddedHeight != this.paddedHeight)
            generatePadding(this.pixelBuffer, PaddingOperation.APPLY, getFirstClutColor());

        // Generate clut for newly imported image. (Must occur after padding is generated)
        regenerateClut(this.parent.getClutList(), null, true);
        updatePsxPixelInfo();
        invalidateCache();

        // Handle dimension change...
        if (!hadPreviousImage || this.paddedWidth != oldPaddedWidth || this.paddedHeight != oldPaddedHeight) {
            markNeedsVramRefresh();

            // Change where in VloFile.images the image is placed, since this is based on the position.
            if (this.parent.isSortingOrderKnown() && this.parent.removeImageFromList(this))
                this.parent.addImageToList(this);
        }
    }

    private int getFirstClutColor() {
        int firstClutColor = PADDING_TRANSPARENT_PIXEL_PC;
        if (isPsxMode()) {
            // Use the previous first color from the previous clut.
            // This is done to make byte-matching/image import testing checks work. (VloFile.validateImageData()).
            // If we don't care about that, we can safely remove this statement block.
            if (VloFile.DEBUG_VALIDATE_IMAGE_EXPORT_IMPORT && this.clut != null)
                return loadClutColor(this.clut.getColor(0));

            if (this.paddingTransparent) {
                firstClutColor = PADDING_TRANSPARENT_PIXEL_PSX;
            } else {
                firstClutColor = 0xFFFFFFFF;
                for (int i = 0; i < this.pixelBuffer.length; i++) {
                    int color = this.pixelBuffer[i];
                    if ((color & 0xFFFFFFFFL) < (firstClutColor & 0xFFFFFFFFL))
                        firstClutColor = color;
                }
            }
        }
        return firstClutColor;
    }

    /**
     * Invalidate the cached image.
     */
    public void invalidateCache() {
        Arrays.fill(this.cachedImages, null);
    }

    /**
     * Set whether padding is fully transparent or not.
     * @param paddingTransparent if padding should be fully transparent.
     */
    public void setPaddingTransparent(boolean paddingTransparent) {
        if (this.paddingTransparent == paddingTransparent)
            return;

        BufferedImage oldImage = toBufferedImage(DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS);
        this.paddingTransparent = paddingTransparent;
        replaceImage(oldImage, ProblemResponse.THROW_EXCEPTION);
    }

    public static final int IMAGE_EXPORT_FLAG_ENABLE_TRANSPARENCY = Constants.BIT_FLAG_0; // This will also enable ABR.
    public static final int IMAGE_EXPORT_FLAG_ENABLE_PSX_SEMI_TRANSPARENT = Constants.BIT_FLAG_1;
    public static final int IMAGE_EXPORT_FLAG_INCLUDE_PADDING = Constants.BIT_FLAG_2;
    public static final int IMAGE_EXPORT_FLAG_HIGHLIGHT_PADDING = Constants.BIT_FLAG_3;
    private static final int IMAGE_EXPORT_FLAG_MASK = IMAGE_EXPORT_FLAG_ENABLE_TRANSPARENCY
            | IMAGE_EXPORT_FLAG_ENABLE_PSX_SEMI_TRANSPARENT
            | IMAGE_EXPORT_FLAG_INCLUDE_PADDING | IMAGE_EXPORT_FLAG_HIGHLIGHT_PADDING;
    private static final int IMAGE_EXPORT_CACHE_SIZE = IMAGE_EXPORT_FLAG_MASK + 1;

    /**
     * Returns an integer with the settings specified for use when exporting an image.
     * @param enablePsxSemiTransparent if PSX (hardware specified) semi-transparent rendering is enabled for the usage of the texture.
     * @param enableTransparency if transparent pixels are permitted in the returned image
     * @param includePadding iff true, padding pixels will be included
     * @param highlightPadding if true, AND padding is included, padding pixels will be highlighted
     * @return imageSettings
     */
    public static int getImageSettingFlags(boolean enablePsxSemiTransparent, boolean enableTransparency, boolean includePadding, boolean highlightPadding) {
        int settings = 0;
        if (enablePsxSemiTransparent)
            settings |= IMAGE_EXPORT_FLAG_ENABLE_PSX_SEMI_TRANSPARENT;
        if (enableTransparency)
            settings |= IMAGE_EXPORT_FLAG_ENABLE_TRANSPARENCY;
        if (includePadding) {
            settings |= IMAGE_EXPORT_FLAG_INCLUDE_PADDING;
            if (highlightPadding)
                settings |= IMAGE_EXPORT_FLAG_HIGHLIGHT_PADDING;
        }

        return settings;
    }

    /**
     * Export this image with the given settings.
     * @return bufferedImage
     */
    public BufferedImage toBufferedImage(boolean enablePsxSemiTransparent, boolean enableTransparency, boolean includePadding, boolean hightlightPadding) {
        return toBufferedImage(getImageSettingFlags(enablePsxSemiTransparent, enableTransparency, includePadding, hightlightPadding));
    }

    /**
     * Export this image with the given settings.
     * @return bufferedImage
     */
    public BufferedImage toBufferedImage(int settings) {
        boolean includePadding = (settings & IMAGE_EXPORT_FLAG_INCLUDE_PADDING) == IMAGE_EXPORT_FLAG_INCLUDE_PADDING;
        if (!includePadding)
            settings &= ~IMAGE_EXPORT_FLAG_HIGHLIGHT_PADDING; // Prevent pointless combination.

        // Return cached image.
        BufferedImage cachedImage = this.cachedImages[settings];
        if (cachedImage != null)
            return cachedImage;

        // Get information from padding.
        boolean enableTransparency = (settings & IMAGE_EXPORT_FLAG_ENABLE_TRANSPARENCY) == IMAGE_EXPORT_FLAG_ENABLE_TRANSPARENCY;
        boolean enablePsxSemiTransparent = (settings & IMAGE_EXPORT_FLAG_ENABLE_PSX_SEMI_TRANSPARENT) == IMAGE_EXPORT_FLAG_ENABLE_PSX_SEMI_TRANSPARENT;
        boolean enablePaddingHighlight = includePadding && (settings & IMAGE_EXPORT_FLAG_HIGHLIGHT_PADDING) == IMAGE_EXPORT_FLAG_HIGHLIGHT_PADDING;

        // Setup image with/without padding.
        BufferedImage image;
        if (includePadding) {
            image = ImageUtils.createImageFromArray(this.paddedWidth, this.paddedHeight, BufferedImage.TYPE_INT_ARGB, this.pixelBuffer);
        } else {
            image = getPaddedImageTemplate();
        }

        int[] pixelArray = ImageUtils.getWritablePixelIntegerArray(image);
        if (isPsxMode()) {
            PSXClutColor tempColor = new PSXClutColor();
            if (enablePsxSemiTransparent || enableTransparency) {
                for (int i = 0; i < pixelArray.length; i++) {
                    PSXClutColor clutColor = getClutColor(pixelArray, tempColor, i);

                    byte alpha;
                    if (enablePsxSemiTransparent) {
                        alpha = PSXClutColor.getAlpha(clutColor.isFullBlack(), clutColor.isStp(), true, this.abr);
                    } else {
                        // Regular transparency, but also, use alpha 127 for stp inversions.
                        if (clutColor.isStp() ^ getExpectedStpBit(clutColor)) {
                            alpha = IMAGE_ALPHA_INVERTED_STP_BIT;
                        } else {
                            alpha = PSXClutColor.getAlpha(clutColor.isFullBlack(), clutColor.isStp(), false, null);
                        }
                    }

                    pixelArray[i] = ColorUtils.setAlpha(pixelArray[i], alpha);
                }
            } else {
                // PSX version should use full alpha if transparency is disabled.
                // Replace all alpha codes with full alpha.
                for (int i = 0; i < pixelArray.length; i++)
                    pixelArray[i] |= COLOR_FULL_ALPHA; // Make the pixel fully opaque.
            }
        } else { // PC
            // Generate alpha based on ABR.
            // PC version needs full black to be set to zero.
            // Loaded PC images have 1 possible alpha value, 0xFF, so transparency is handled here, just like in-game.
            if (enablePsxSemiTransparent || (enableTransparency && this.anyFullyBlackPixelPresentPC && testFlag(FLAG_BLACK_IS_TRANSPARENT)))
                for (int i = 0; i < pixelArray.length; i++)
                    pixelArray[i] = getDisplayPixelColorPC(pixelArray[i], enablePsxSemiTransparent);
        }

        // Padding highlight.
        if (enablePaddingHighlight) {
            int minX = getLeftPadding();
            int maxX = minX + this.unpaddedWidth;
            int minY = getUpPadding();
            int maxY = minY + this.unpaddedHeight;
            for (int i = 0; i < pixelArray.length; i++) {
                int x = i % this.paddedWidth;
                int y = i / this.paddedWidth;
                if (x < minX || x >= maxX || y < minY || y >= maxY) {
                    // Applies pink Color: [R: 255, G: 0, B: 220], Alpha: 128
                    // To combine with existing pixel color, add both colors together and divide by 2.
                    int pixelColor = pixelArray[i];
                    byte alpha = ColorUtils.getAlpha(pixelColor);
                    byte red = (byte) (((ColorUtils.getRed(pixelColor) & 0xFF) + 255) >>> 1);
                    byte green = (byte) ((ColorUtils.getGreen(pixelColor) & 0xFF) >>> 1);
                    byte blue = (byte) (((ColorUtils.getBlue(pixelColor) & 0xFF) + 220) >>> 1);
                    if ((alpha & 0xFF) < Byte.MAX_VALUE)
                        alpha = Byte.MAX_VALUE; // Make sure the color is visible.

                    pixelArray[i] = ColorUtils.toARGB(red, green, blue, alpha);
                } else /*if (x < maxX)*/ {
                    i += (maxX - x - 1);
                }
            }
        }

        this.cachedImages[settings] = image; // Cache the image.
        return image;
    }

    private int getDisplayPixelColorPC(int pixelColor, boolean enablePsxSemiTransparent) {
        boolean fullBlack = ((pixelColor & PSXClutColor.ARGB8888_TO5BIT_COLOR_MASK) == 0);
        byte newAlpha = PSXClutColor.getAlpha(fullBlack, !fullBlack, enablePsxSemiTransparent, this.abr);

        if (enablePsxSemiTransparent) {
            switch (this.abr) {
                case DEFAULT: // The PC version makes this opaque for some reason.
                case SUBTRACT: // This just seems unimplemented in the PC version, as it becomes fully opaque.
                    newAlpha = (byte) 0xFF;
                    break;
                case COMBINE: // Use the previously obtained alpha value.
                    break;
                case FAINT: // This is a rough estimation of what I see on PC. I didn't bother to figure out exactly how it worked.
                    newAlpha = (byte) 0xFF;
                    pixelColor = ColorUtils.toRGB((byte) (ColorUtils.getRedInt(pixelColor) / 2),
                            (byte) (ColorUtils.getGreenInt(pixelColor) / 2),
                            (byte) (ColorUtils.getBlueInt(pixelColor) / 2));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported abr: " + this.abr);
            }
        }

        return ColorUtils.setAlpha(pixelColor, newAlpha);
    }

    private BufferedImage getPaddedImageTemplate() {
        BufferedImage image = new BufferedImage(this.unpaddedWidth, this.unpaddedHeight, BufferedImage.TYPE_INT_ARGB);
        int[] pixelArray = ImageUtils.getWritablePixelIntegerArray(image);

        // Copy unpadded image data.
        int srcOffset = getLeftPadding() + (getUpPadding() * this.paddedWidth);
        for (int y = 0, dstOffset = 0; y < this.unpaddedHeight; y++, srcOffset += this.paddedWidth, dstOffset += this.unpaddedWidth)
            System.arraycopy(this.pixelBuffer, srcOffset, pixelArray, dstOffset, this.unpaddedWidth);

        return image;
    }

    public static final int DEFAULT_IMAGE_EXPORT_SETTINGS = getImageSettingFlags(false, true, true, false);
    public static final int DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS = getImageSettingFlags(false, true, false, false);
    public static final int DEFAULT_IMAGE_NOT_TRANSPARENT_EXPORT_SETTINGS = getImageSettingFlags(false, false, true, false);
    public static final int DEFAULT_IMAGE_STRIPPED_VIEW_SETTINGS = getImageSettingFlags(false, false, false, false);
    public static final int IMAGE_3D_EXPORT_SETTINGS = getImageSettingFlags(true, true, true, false);

    /**
     * Export this image exactly how it is saved in the database.
     * @return bufferedImage
     */
    public BufferedImage toBufferedImage() {
        return toBufferedImage(DEFAULT_IMAGE_EXPORT_SETTINGS);
    }

    /**
     * Export this game image as a JavaFX image.
     * @param settings The settings to export this image with.
     * @return fxImage
     */
    public Image toFXImage(int settings) {
        return FXUtils.toFXImage(toBufferedImage(settings), false);
    }

    /**
     * Export this game image as a JavaFX image.
     * @return fxImage
     */
    public Image toFXImage() {
        return toFXImage(DEFAULT_IMAGE_EXPORT_SETTINGS);
    }

    /**
     * Test if a flag is present.
     * @param test The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(int test) {
        return (getFlags() & test) == test;
    }

    /**
     * Gets the name configured for this image.
     */
    public String getName() {
        if (!StringUtils.isNullOrWhiteSpace(this.customName))
            return this.customName;

        String originalName = getOriginalName();
        if (!StringUtils.isNullOrWhiteSpace(originalName))
            return originalName;

        return null;
    }

    /**
     * Gets the name configured as found in the original game, or null if not configured.
     */
    public String getOriginalName() {
        return getConfig().getImageList().getImageNameFromID(this.textureId);
    }

    @Override
    @SneakyThrows
    public VloImage clone() {
        return (VloImage) super.clone();
    }

    /**
     * Gets the local VLO image id.
     */
    public int getLocalImageID() {
        return this.parent.getImages().indexOf(this);
    }

    @Override
    public boolean hasAnyTransparentPixels(BufferedImage image) {
        return isFullyOpaque(true);
    }

    /**
     * Tests if the image is fully opaque.
     * @param enableSemiTransparent is PSX semi-transparency rendering mode enabled?
     * @return fullyOpaque
     */
    public boolean isFullyOpaque(boolean enableSemiTransparent) {
        if (isPsxMode()) {
            // Having any stp inversion means that there's pixels using the non-inverted version too.
            // Thus, checking this.anyStpPixelInversionPsx does actually make sense here.
            // This assumption will not break when replaceImage() is used, unless someone really does something wrong.
            // For example, importing a fully translucent image instead of just changing the translucent flag.
            // There is a lot of implied logic here.
            return !this.anyFullyTransparentPixelPresentPsx
                    && (!enableSemiTransparent || (!this.anyStpPixelInversionPsx && !this.expectedStpNonBlackBitPsx));
        } else {
            return !this.anyFullyBlackPixelPresentPC || !testFlag(FLAG_BLACK_IS_TRANSPARENT);
        }
    }

    @Override
    public BufferedImage makeImage() { // This is for use in a texture sheet.
        // As such, this should include padding, AND include proper transparency/alpha.
        return toBufferedImage(IMAGE_3D_EXPORT_SETTINGS);
    }

    @Override
    public int getWidth() {
        return this.paddedWidth;
    }

    @Override
    public int getHeight() {
        return this.paddedHeight;
    }

    /**
     * Returns true iff the PSX version padding start position should be used. (One)
     */
    private boolean shouldUvOriginStartAtOne() {
        if (!isPsxMode())
            return false; // Only possible on PSX version.

        // It has been observed that IF there is no Y padding on the PSX version,
        // THEN, there is no X padding, EXCEPT for vram pixel alignment padding.
        // This is because the texCoord U value will always start vramX + 1 on PSX if there is padding.
        short fullHeight = this.paddedHeight;
        short ingameHeight = this.unpaddedHeight;
        return fullHeight != ingameHeight && fullHeight != ingameHeight + 1;
    }

    /**
     * Gets the internally tracked unpadded width, excluding things which impact rendering such as getUnpaddedWidth().
     */
    public int getInternalUnpaddedWidth() {
        return this.unpaddedWidth;
    }

    /**
     * Gets the internally tracked unpadded height, excluding things which impact rendering such as getUnpaddedHeight().
     */
    public int getInternalUnpaddedHeight() {
        return this.unpaddedHeight;
    }

    @Override
    public int getLeftPadding() {
        if (isPsxMode())
            return shouldUvOriginStartAtOne() ? 1 : 0;

        // PC logic.
        return ((this.paddedWidth - this.unpaddedWidth) / 2);
    }

    @Override
    public int getRightPadding() {
        return (this.paddedWidth - this.unpaddedWidth - getLeftPadding()) + (calculateHitX() ? 1 : 0);
    }

    @Override
    public int getUpPadding() {
        return (this.paddedHeight - this.unpaddedHeight) / 2;
    }

    @Override
    public int getDownPadding() {
        return (this.paddedHeight - this.unpaddedHeight - getUpPadding()) + (calculateHitY() ? 1 : 0);
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        invalidateCache();
        fireChangeEvent0(newImage);
    }

    private static int getPaletteWidth(PsxImageBitDepth bitDepth) {
        switch (bitDepth) {
            case CLUT4:
                return 16;
            case CLUT8:
                return 256;
            case SBGR1555:
                return -1;
            default:
                throw new UnsupportedOperationException("Unsupported bitDepth: " + bitDepth);
        }
    }

    // The upcoming function has been tested perfectly matching against:
    //  - Old Frogger PC Milestone 3
    //  - Old Frogger PSX Milestone 3
    //  - Frogger PSX Sony Demo
    //  - Frogger PSX Alpha
    //  - Frogger PC Alpha (June 1997)
    //  - Frogger PSX Build 02
    //  - Frogger PC Build 1 (July 1997)
    //  - Frogger PSX Build 20 NTSC
    //  - Frogger PSX Build 49
    //  - Frogger PC Prototype (September 1997)
    //  - MediEvil ECTS (Ignoring failures for width>256 images, as explained elsewhere)
    //  - Frogger PSX Build 71 (Retail NTSC)
    //  - Frogger PC v3.0e (Retail)
    //  - Beast Wars NTSC Prototype
    //  - Beast Wars NTSC Release
    //  - Beast Wars PAL Release
    //  - Beast Wars PC Retail
    //  - MoonWarrior
    //  - C-12 Final Resistance 0.03a (E3 Build)
    //  - C-12 Final Resistance (Beta Candidate 3)
    //  - C-12 Final Resistance (Retail NTSC)

    // In rare cases, it seems some images will use fully white alignment padding. All of the following mismatches seem to be this situation.
    // What causes this is not currently understood.
    // Game versions which are not perfectly matched:
    //  - MediEvil 0.31 (17 pixel failures in gargoyle_eye/149@FIXEDVRM.VLO)
    //  - MediEvil Reviewable Version (17 pixel failures in gargoyle_eye/153@FIXEDVRM.VLO)
    //  - MediEvil NTSC Release (17 pixel failures in gargoyle_eye/153@FIXEDVRM.VLO)
    //  - MediEvil II 0.19 (74 pixel failures in 13/116@FILE_0002 and 27/149@FILE_0002)
    //  - MediEvil II 0.51 (74 pixel failures in 36/103@File 10 and 50/131@File 10)
    //  - MediEvil II USA Retail 1.1Q (74 pixel failures in 36/103@File 10 and 50/131@File 10)
    private int getPaddingColor(int[] paddedImagePixels, int index, int paddedWidth, int padMinX, int padMaxX, int padMinY, int padMaxY, int paddingX, int paddingY, int emptyRightPaddingX, int firstClutColor) {
        // MediEvil II has some weird images which are mostly padding, and have a texture with crudely drawn letters: "PG".
        // At the time of writing, I have no idea what this is for, but we'll leave it alone for now.
        // One theory is that this was perhaps used to support images with dimensions larger than 256.
        if ((paddingX == 258 || paddingY == 258) && getGameInstance().isMediEvil2())
            return paddedImagePixels[index];

        int padPixelX = index % paddedWidth;
        if (padPixelX >= paddedWidth - emptyRightPaddingX)
            return firstClutColor; // The pixels created by alignment padding (some of the right-most clut pixels) use clut color zero.

        boolean pixelIsPadding = false;
        if (padPixelX < padMinX) {
            padPixelX = padMinX;
            pixelIsPadding = true;
        } else if (padPixelX > padMaxX) {
            padPixelX = padMaxX;
            pixelIsPadding = true;
        }

        int padPixelY = index / paddedWidth;
        if (padPixelY < padMinY) {
            padPixelY = padMinY;
            pixelIsPadding = true;
        } else if (padPixelY > padMaxY) {
            padPixelY = padMaxY;
            pixelIsPadding = true;
        }

        if (pixelIsPadding && this.paddingTransparent)
            return getTransparentPaddingPixel();

        return paddedImagePixels[(padPixelY * paddedWidth) + padPixelX];
    }

    //////////////////////////////////////////////////////////////
    //  ---                MEDIEVIL DATA                      ----
    //////////////////////////////////////////////////////////////

    /**
     * Gets the MediEvil polygon sort mode assigned to this texture.
     * Throws an exception if the VLO is loaded from a game other than MediEvil.
     */
    public MediEvilMapPolygonSortMode getMediEvilPolygonSortMode() {
        if (!isMediEvilFlags())
            throw new UnsupportedOperationException("Cannot get the MediEvilMapPolygonSortMode for a non-MediEvil image!");

        return MediEvilMapPolygonSortMode.values()[(this.flags & FLAG_MEDIEVIL_SORT_MASK) >>> FLAG_MEDIEVIL_SORT_SHIFT];
    }

    /**
     * Sets the MediEvil polygon sort mode assigned to this texture
     * Throws an exception if the VLO is loaded from a game other than MediEvil.
     * @param sortMode sortMode
     */
    public void setMediEvilPolygonSortMode(MediEvilMapPolygonSortMode sortMode) {
        if (!isMediEvilFlags())
            throw new UnsupportedOperationException("Cannot set the MediEvilMapPolygonSortMode for a non-MediEvil image!");
        if (sortMode == null)
            throw new NullPointerException("sortMode");

        this.flags &= ~FLAG_MEDIEVIL_SORT_MASK;
        this.flags |= (short) (sortMode.ordinal() << FLAG_MEDIEVIL_SORT_SHIFT);
    }

    /**
     * Gets the MediEvil friction level assigned to this texture.
     * Throws an exception if the VLO is loaded from a game other than MediEvil.
     * @return the friction level to apply when walking on a polygon using this texture
     */
    public MediEvilMapFrictionLevel getMediEvilFrictionLevel() {
        if (!isMediEvilFlags())
            throw new UnsupportedOperationException("Cannot get the friction level for a non-MediEvil image!");

        return MediEvilMapFrictionLevel.values()[(this.flags & FLAG_MEDIEVIL_FRICTION_MASK) >>> FLAG_MEDIEVIL_FRICTION_SHIFT];
    }

    /**
     * Sets the MediEvil friction level assigned to this texture.
     * Throws an exception if the VLO is loaded from a game other than MediEvil.
     * @param frictionLevel the friction level to apply when walking on a polygon using this texture
     */
    public void setMediEvilFrictionLevel(MediEvilMapFrictionLevel frictionLevel) {
        if (!isMediEvilFlags())
            throw new UnsupportedOperationException("Cannot set the friction level for a non-MediEvil image!");
        if (frictionLevel == null)
            throw new NullPointerException("frictionLevel");

        this.flags &= ~FLAG_MEDIEVIL_FRICTION_MASK;
        this.flags |= (short) (frictionLevel.ordinal() << FLAG_MEDIEVIL_FRICTION_SHIFT);
    }

    /**
     * Gets the MediEvil polygon interaction type assigned to this texture.
     * Throws an exception if the VLO is loaded from a game other than MediEvil.
     * @return the interaction type to apply when walking on a polygon using this texture
     */
    public MediEvilMapInteractionType getMediEvilInteractionType() {
        if (!isMediEvilFlags())
            throw new UnsupportedOperationException("Cannot get the MediEvilMapInteractionType for a non-MediEvil image!");

        return MediEvilMapInteractionType.values()[(this.flags & FLAG_MEDIEVIL_INTERACTION_MASK) >>> FLAG_MEDIEVIL_INTERACTION_SHIFT];
    }

    /**
     * Sets the MediEvil polygon interaction type assigned to this texture
     * Throws an exception if the VLO is loaded from a game other than MediEvil.
     * @param interactionType the interaction type to apply when walking on a polygon using this texture
     */
    public void setMediEvilInteractionType(MediEvilMapInteractionType interactionType) {
        if (!isMediEvilFlags())
            throw new UnsupportedOperationException("Cannot set the MediEvilMapInteractionType for a non-MediEvil image!");
        if (interactionType == null)
            throw new NullPointerException("interactionType");

        this.flags &= ~FLAG_MEDIEVIL_INTERACTION_MASK;
        this.flags |= (short) (interactionType.ordinal() << FLAG_MEDIEVIL_INTERACTION_SHIFT);
    }

    /**
     * Applies a custom name to this image.
     * @param customName the custom name to apply to the image
     */
    public void setCustomName(String customName) {
        if (customName == null) {
            this.customName = null;
            return; // Allow setting custom name to null.
        }

        if (!isValidTextureName(customName))
            throw new IllegalArgumentException("Invalid name: '" + customName + "'");
        if (this.customName != null && this.customName.equals(customName))
            return; // No change.

        String originalName = getOriginalName();
        if (originalName != null && originalName.equals(customName)) {
            this.customName = null; // Set customName to null, so we'll use the originalName.
            return;
        }

        VloImage otherImage;
        if (this.parent != null && (otherImage = this.parent.getImageByName(customName)) != null)
            throw new IllegalArgumentException("Found other image named '" + customName + "', at localIndex: " + otherImage.getLocalImageID() + ", ID: " + otherImage.getTextureId() + ".");

        this.customName = customName;
    }

    /**
     * Check if the provided string is valid to use as a texture name.
     * @param textureName the name to test
     * @return true iff the texture name is valid to use as a texture name
     */
    public static boolean isValidTextureName(String textureName) {
        if (textureName == null || textureName.isEmpty() || textureName.length() >= 256)
            return false;

        for (int i = 0; i < textureName.length(); i++) {
            char temp = textureName.charAt(i);
            if ((temp >= '0' && temp <= '9' && i > 0) || (temp >= 'A' && temp <= 'Z') || (temp >= 'a' && temp <= 'z') || temp == '_')
                continue;

            return false;
        }

        return true;
    }
}