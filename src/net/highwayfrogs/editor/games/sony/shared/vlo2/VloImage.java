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
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
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
 * And even between rewrites, it seems like Vorg had different versions or per-game tweaks made for each game during the existance of Vorg.
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
 * TODO Remaining Tasks before feature complete:
 *  4) Fix HitX for MediEvil prototypes.
 *  5) Later, create the VRAM texture placement system.
 *   -> Easy image management. (VloFile.addImage(String name, BufferedImage, ClutMode, abr, int padding)
 *    -> -1 will calculate padding.
 *    -> How to change padding?
 *    -> The text generation util should integrate with this new stuff.
 *    -> Name tracking.
 *    -> Update the import all/export all feature to be on right-click of the VLO itself
 *     -> It should also use image file names.
 *      -> For images with unrecognized non-numeric names, add them to the VLO.
 *      -> For images numeric names, add them with that as their texture ID, replacing any existing image with that texture ID.
 *    -> The "Clone Image" button should be removed to "Copy image to other VLO file.", and moved to right-click menu.
 *  6) Store custom file names in the .VLO file itself.
 *  8) Review to-do comments in this file.
 *  9) Text generation
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
    @Getter private PsxImageBitDepth bitDepth; // This is consistent across versions on PC, and does seem to indicate image quality. I do not think it is used for anything important.
    @NonNull @Getter private PsxAbrTransparency abr = PsxAbrTransparency.DEFAULT; // ABR. Always observed to be DEFAULT on PC.
    @Getter private VloClut clut;
    @Getter private boolean paddingTransparent; // TODO: Add a UI check box to toggle this. Disable it if BLACK_IS_TRANSPARENT is not set.
    private boolean paddingEnabled = true;
    private boolean stpNonBlackBitFlipped; // Pre-MediEvil II: Used to calculate the CLUT STP bit state.
    private boolean stpBlackBitFlipped; // Pre-MediEvil II: Used to calculate the CLUT STP bit state

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
    public static final int FLAG_BLACK_IS_TRANSPARENT = Constants.BIT_FLAG_5; // Used exclusively by the PC version for image loading.
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
        this.bitDepth = PsxImageBitDepth.values()[(readPage & 0b110000000) >> 7];
        this.abr = PsxAbrTransparency.values()[(readPage & 0b1100000) >> 5];
        if (!isPsxMode() && this.abr != PsxAbrTransparency.DEFAULT)
            throw new RuntimeException("ABR was always expected to be zero for PC builds. (Was: " + this.abr + ")");

        // Can do this before texturePage is set, but after clutMode is set.
        this.paddedWidth = (short) (this.paddedWidth * getWidthMultiplier());

        // Validate page short.
        if (getTexturePageShort() != readPage) // Verify this is both read and calculated properly.
            throw new RuntimeException("Calculated tpage short as " + getTexturePageShort() + ", Real: " + readPage + "!");

        if (isPsxMode()) {
            this.clutId = reader.readShort();
            this.flags = reader.readShort();
        } else {
            this.flags = reader.readShort();
            this.clutId = reader.readShort(); // Provably unused. Probably garbage data.
        }

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
        if (isPsxMode()) {
            this.paddingEnabled = (paddingY > 0); // If y padding is enabled, then x padding is likely to be enabled.
        } else {
            this.paddingEnabled = (paddingX > 4);
        }
        if (paddingX != paddingY && getGameInstance().isPC()) // This is never known to happen.
            getLogger().warning("Padding XY mismatch! [%d vs %d]", paddingX, paddingY);

        // The PlayStation API technically supports images > 256x256, although they will just be chopped off in-game.
        // This means we want to roughly approximate their padding.
        // This is definitely not perfect, but it's good enough.
        if (isPsxMode()) {
            if (this.paddedWidth > VloImage.MAX_IMAGE_DIMENSION && this.unpaddedWidth == VloImage.MAX_IMAGE_DIMENSION) {
                this.unpaddedWidth = (short) (this.paddedWidth - Math.max(0, calculatePaddingX()));
                paddingX = this.paddedWidth - this.unpaddedWidth; // Update to reflect new changes.
            }

            if (this.paddedHeight > VloImage.MAX_IMAGE_DIMENSION && this.unpaddedHeight == VloImage.MAX_IMAGE_DIMENSION) {
                this.unpaddedHeight = (short) (this.paddedHeight - Math.max(0, calculatePaddingY()));
                paddingY = this.paddedHeight - this.unpaddedHeight; // Update to reflect new changes.
                this.paddingEnabled = (paddingY > 0); // If y padding is enabled, then x padding is likely to be enabled.
            }
        }

        // Validate calculated data.
        int testPadX = calculatePaddingX();
        if (paddingX != testPadX && testPadX != -1)
            getLogger().warning("Calculated paddingX did not match expected paddingX! Calculated: %d, Expected: %d [%d, %d|%d, UV: %d, %d] [%s]", testPadX, paddingX, this.unpaddedWidth, getExpandedVramX(), this.vramX, getU(), getV(), getFlagDisplay());

        int testPadY = calculatePaddingY();
        if (paddingY != testPadY && testPadY != -1)
            getLogger().warning("Calculated paddingY did not match expected paddingY! Calculated: %d, Expected: %d [%dx%d, %dx%d, (VRAM: %d/%d, %d), UV: %d, %d] [%s]", testPadY, paddingY, this.unpaddedWidth, this.unpaddedHeight, this.paddedWidth, this.paddedHeight, getExpandedVramX(), this.vramX, this.vramY, getU(), getV(), getFlagDisplay());

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
        if (isPsxMode()) {
            if (this.bitDepth == PsxImageBitDepth.SBGR1555) { // Used in PS1 demo. Example: Frogger's eye, VOL@35 (The fireball texture)
                PSXClutColor tempColor = new PSXClutColor();
                for (int i = 0; i < pixelCount; i++)
                    this.pixelBuffer[i] = loadClutColor(tempColor.fromShort(reader.readShort()));
            } else if (this.bitDepth == PsxImageBitDepth.CLUT8) { // Used in PS1 release. Example: STARTNTSC.VLO
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
            }
        }

        int firstClutColor = this.clut != null ? loadClutColor(this.clut.getColor(0)) : getDefaultFirstClutColor();
        generatePadding(this.pixelBuffer, PaddingOperation.VALIDATE, firstClutColor);
    }

    private int getDefaultFirstClutColor() {
        return isPsxMode() ? PADDING_TRANSPARENT_PIXEL_PSX : PADDING_TRANSPARENT_PIXEL_PC;
    }

    private void loadClut(VloClutList clutList) {
        setClut(clutList.getClutFromId(this.clutId, true));
        validateClut();
    }

    private void validateClut() {
        // Determine the STP Bit state.
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
        //  - MediEvil II TODO: Failure. (Perhaps we can just use the regular to/from clut color for these versions, since there's no PC port to mess it up with)
        //  - C-12 Final Resistance 0.03a TODO: Failure
        VloClut clut = this.clut;
        this.stpBlackBitFlipped = false;
        this.stpNonBlackBitFlipped = false;
        if (clut == null || clut.getColorCount() == 0 || isPixelStpBitFlipAllowed())
            return;

        boolean expectedNonBlackStpBit = calculateExpectedStpBitForNonBlack();
        boolean expectedBlackStpBit = calculateExpectedStpBitForBlack();
        boolean firstNonBlackStpBit = false;
        boolean firstBlackStpBit = false;

        boolean foundMismatch = false;
        boolean foundFirstBlackStpBit = false;
        boolean foundFirstNonBlackStpBit = false;
        int colorCount = clut.getColorCount();
        for (int i = 0; i < colorCount; i++) {
            PSXClutColor color = clut.getColor(i);
            if (color.isFullBlack()) {
                if (!foundFirstBlackStpBit) {
                    foundFirstBlackStpBit = true;
                    firstBlackStpBit = color.isStp();
                    this.stpBlackBitFlipped = (expectedBlackStpBit ^ firstBlackStpBit);
                } else if (color.isStp() ^ firstBlackStpBit) {
                    foundMismatch = true;
                    break;
                }
            } else if (!foundFirstNonBlackStpBit) {
                foundFirstNonBlackStpBit = true;
                firstNonBlackStpBit = color.isStp();
                this.stpNonBlackBitFlipped = (expectedNonBlackStpBit ^ firstNonBlackStpBit);
            } else if (color.isStp() ^ firstNonBlackStpBit) {
                foundMismatch = true;
                break;
            }
        }

        // Warn if we find any situation which breaks our ability to generate Cluts.
        if (foundMismatch) {
            getLogger().warning("STP Bit Mismatch in %s. (Transparent: %b, Black Translucent: %b) %s", clut, testFlag(FLAG_TRANSLUCENT), testFlag(FLAG_BLACK_IS_TRANSPARENT), clut);
            for (int i = 0; i < colorCount; i++) {
                PSXClutColor color = clut.getColor(i);
                getLogger().warning(" CLUT Color %d: [STP: %b, Full Black: %b], %s", i, color.isStp(), color.isFullBlack(), color);
            }
        }
    }

    @Override
    public String getCollectionViewDisplayName() {
        String originalName = getOriginalName();
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
        // TODO: Implement Export.
        // TODO: Implement Change Bit Depth (Have window previewing the images using the previous padding window template)
    }

    private enum PaddingOperation {
        VALIDATE, APPLY
    }

    // NOTE: unpaddedHeight and paddedHeight should be up to date when calling this function.
    private void generatePadding(int[] pixelBuffer, PaddingOperation operation, int firstClutColor) {
        int padMinX = getLeftPadding();
        int padMaxX = Math.max(padMinX, padMinX + this.unpaddedWidth - 1); // DO NOT USE getRightPadding()! That includes the hitX calculation, which should NOT impact padding.
        int padMinY = getUpPadding();
        int padMaxY = Math.max(padMinY, padMinY + this.unpaddedHeight - 1); // DO NOT USE getDownPadding()! That includes the hitX calculation, which should NOT impact padding.

        // Determine padding right boundary (which part uses clut color 0)
        int paddingX = (this.paddedWidth - this.unpaddedWidth);
        int paddingY = (this.paddedHeight - this.unpaddedHeight);
        int emptyRightPaddingX = Math.max(0, paddingX - paddingY);

        // Set to false so the padding transparency can be determined.
        if (operation == PaddingOperation.VALIDATE)
            this.paddingTransparent = false;

        // Generate padding.
        boolean firstPixelMismatch = false;
        for (int i = 0; i < pixelBuffer.length; i++) {
            int paddedColor = getPaddingColor(pixelBuffer, i, this.paddedWidth, padMinX, padMaxX, padMinY, padMaxY, paddingX, paddingY, emptyRightPaddingX, firstClutColor);
            // boolean pixelIsClutZero = (i % this.paddedWidth) < (this.paddedWidth - emptyRightPaddingX); // true iff the pixel is
            if (operation == PaddingOperation.VALIDATE && paddedColor != pixelBuffer[i]) {
                if (!firstPixelMismatch) {
                    firstPixelMismatch = true;

                    // Try to enable transparency if that helps.
                    if (this.pixelBuffer[i] == PADDING_TRANSPARENT_PIXEL_PC
                            || this.pixelBuffer[i] == PADDING_TRANSPARENT_PIXEL_PSX) {
                        this.paddingTransparent = true;
                        i--;
                        continue;
                    }
                }

                getLogger().warning("Pixel[%d,%d] padding was expected to be %08X, but was calculated to be %08X. (PadX: %d, PadY: %d, Stripped PadX: %d, CLUT Index: %d)", i % this.paddedWidth, i / this.paddedWidth, this.pixelBuffer[i], paddedColor, paddingX, paddingY, emptyRightPaddingX, this.clut != null ? this.clut.getColorIndex(getClutColor(new PSXClutColor(), i), false) : Integer.MAX_VALUE);
            } else if (operation == PaddingOperation.APPLY) {
                pixelBuffer[i] = paddedColor;
            }
        }

        // If transparent padding was enabled, mark the image as having transparency.
        if (this.paddingTransparent && operation == PaddingOperation.APPLY)
            setFlag(FLAG_BLACK_IS_TRANSPARENT, true);
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
        return (color.isFullBlack() ? calculateExpectedStpBitForBlack() : calculateExpectedStpBitForNonBlack());
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

        if (tempColor == null)
            tempColor = new PSXClutColor();

        int clutWidth = getPaletteWidth(this.bitDepth);
        int clutHeight = 1;
        // TODO: Moon Warrior palette stuff.

        // Find unique clut colors.
        List<PSXClutColor> newColors = new ArrayList<>();
        for (int i = 0; i < this.pixelBuffer.length; i++) {
            PSXClutColor color = getClutColor(tempColor, i);
            if ((!ignorePadding || !isPaddingPixel(i)) && !newColors.contains(color))
                newColors.add(color.clone());
        }

        // Validate there are not too many colors!
        int maxColors = (clutWidth * clutHeight);
        if (newColors.size() > maxColors) // This should not happen, it means an invalid image has been applied.
            throw new RuntimeException("Tried to save invalid image data (" + this + ") which contained too many colors for its bitDepth (" + this.bitDepth + "). [Max Allowed Colors: " + maxColors + ", Actual Colors: " + newColors.size() + "]");

        // Sort colors so we can equality-test clut colors.
        // Do NOT change this sorting order without also fixing replaceImage()'s firstClutColor calculation.
        newColors.sort(Comparator.comparingInt(this::loadClutColor));

        // For any unfilled part of the clut, fill it with black.
        PSXClutColor unused = new PSXClutColor();
        unused.setStp(calculateExpectedStpBitForBlack());
        while (maxColors > newColors.size())
            newColors.add(unused.clone());

        VloClut foundClut = clutList.getClut(newColors);
        if (foundClut != null) {
            setClut(foundClut);
        } else {
            VloClut newClut = this.clut; // Use previous clut if possible
            if (newClut == null || newClut.getImages().size() > 1 || clutWidth > newClut.getWidth() || clutHeight > newClut.getHeight())
                newClut = new VloClut(this.parent); // TODO: This fails right now due to not being able to get a valid clut position.
            newClut.loadColors(clutWidth, clutHeight, newColors);
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
        String loggerInfo = getOriginalName();
        if (StringUtils.isNullOrWhiteSpace(loggerInfo))
            loggerInfo = Integer.toString(getTextureId());

        int localImageId = getLocalImageID();
        if (localImageId == -1)
            localImageId = this.parent.getImages().size();

        return new AppendInfoLoggerWrapper(this.parent.getLogger(), loggerInfo + "/" + localImageId,
                AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
    }

    @Override
    public String toString() {
        String originalName = getOriginalName();
        return "VloImage{" + (originalName != null ? originalName + "," : "")
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

    private boolean calculateExpectedStpBitForNonBlack() {
        // See the validity check in the load function for a list of games this is confirmed to work for.
        return testFlag(FLAG_TRANSLUCENT) ^ this.stpNonBlackBitFlipped;
    }

    private boolean calculateExpectedStpBitForBlack() {
        // See the validity check in the load function for a list of games this is confirmed to work for.
        return !testFlag(FLAG_BLACK_IS_TRANSPARENT) ^ this.stpBlackBitFlipped;
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
        //  - MediEvil Rolling Demo TODO: One failure
        //  - MediEvil ECTS TODO: One failure
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
        if (endU == 0xFF && this.unpaddedWidth + 1 != this.paddedWidth && !getGameInstance().getGameType().isAtLeast(SCGameType.MOONWARRIOR))
            endU++;

        // C-12 has a handful of images which look like they should be HitX, but aren't.
        // The common thread between them is that their width is a multiple of 64, but I suspect this just isn't a large enough sample size to say definitively this is why.
        if (getGameInstance().getGameType().isAtLeast(SCGameType.C12) && (this.unpaddedWidth % 64) == 0)
            return false;

        return (endU & 0xFF) != endU; // TODO: Why does this return a wrong value in MediEvil once?
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

    // Calculates the padding used to align the image to PSX Vram specifications.
    // Returns zero on PC, or if no padding is necessary.
    // Requires: unpaddedWidth, bitDepth
    private int calculatePsxAlignmentPaddingX() {
        return getModuloReversed(this.unpaddedWidth, getWidthMultiplier());
    }

    // Attempts to calculate the padding for the image.
    // Needs updated: clutMode, unpaddedWidth, paddingEnabled
    // Note that it appears on PC, padding may have been configured manually.
    private int calculatePaddingX() {
        // Note that padding configuration appears consistent between versions.
        int padX = calculatePsxAlignmentPaddingX();

        // Validated perfect match against:
        //  - Frogger PC Milestone 3
        //  - Frogger PC Alpha (One failure)
        //  - Frogger PC July (One failure, opt_ripple0)
        //  - Frogger PC September (49 failures, wanted 2 but got 4)
        //  - Frogger PC v1.0
        //  - Frogger PC v3.0e
        //  - Beast Wars PC Retail (15 cases where 2 was expected and 4 was returned)
        if (getGameInstance().isPC()) {
            if (getGameInstance().getGameType().isAtLeast(SCGameType.FROGGER)) {
                if (getGameInstance().isFrogger()) {
                    if (this.unpaddedWidth > MAX_IMAGE_DIMENSION - 2)
                        return 0; // No padding for these images.

                    if (((FroggerGameInstance) getGameInstance()).getVersionConfig().isAtLeastRetailWindows()) {
                        if (this.unpaddedWidth > MAX_IMAGE_DIMENSION - 4)
                            return 2; // No padding for these images.
                        if (this.unpaddedWidth > MAX_IMAGE_DIMENSION - 8)
                            return 4; // No padding for these images.

                        // NOTE:
                        // This mostly seems to be the case:
                        //  - In texture remap? Use 8.
                        //  - Text 2D sprite? Use 2.
                        //  - Otherwise, use 4.
                        //  - While this pattern seems to mostly be true, it is not ACTUALLY consistent.
                        // It's just a general guideline. As such, we

                        String name = getOriginalName();
                        if (this.parent.getFileDisplayName().startsWith("FIX")) {
                            if (name != null && name.startsWith("frog_shadow")) {
                                return 8;
                            } else if (name != null && name.startsWith("opt") && name.endsWith("_sec")) {
                                return 2;
                            } else {
                                return -1;
                            }
                        } else if (this.parent.getFileDisplayName().equals("OPT_VRAM.VLO")) {
                            if (name != null && name.startsWith("opt_log_")) {
                                return 8;
                            } else {
                                // NOTE: 1UP, 2UP, 3UP, 4UP, etc should be 2x2, but we don't have names yet.
                                return 4;
                            }
                        } else if (this.parent.getFileDisplayName().startsWith("LS_ALL") && name != null && name.startsWith("opt") && name.endsWith("_sec")) {
                            return 2;
                        } else if (getGameInstance().getTexturesFoundInRemap().getBit(this.textureId)) {
                            return this.paddingEnabled ? 8 : 4;
                        } else if (name != null && (name.endsWith("pic") || name.endsWith("name"))) {
                            return name.endsWith("mname") || "org3name".equals(name) ? 4 : 2;
                        } else if ("gen_frogstrip".equals(name)) {
                            return 8;
                        } else if (this.parent.getFileDisplayName().startsWith("OPT")) {
                            return -1; // Not sure how to distinguish 2x2 from 4x4.
                        } else {
                            return 4;
                        }

                        // It was also valid to return -1 here.
                    } else {

                        // Fixes Windows Build 1 (PC July).
                        if (this.unpaddedWidth >= MAX_IMAGE_DIMENSION - 4 && this.paddingEnabled)
                            return 2; // No padding for these images.

                        // Frogger PC Alpha (Okay all but once)
                        return this.paddingEnabled ? 4 : 0; // This is just PSX * 2?
                    }
                }

                return this.paddingEnabled ? 4 : 0; // This is just PSX * 2?
            } else { // Pre-recode Frogger PC.
                return this.paddingEnabled ? 2 : 0;
            }
        }

        // Validated perfect match against:
        //  - Frogger PSX Milestone 3 (Pre-Recode)
        //  - Frogger PSX Sony Demo
        //  - Frogger PSX Alpha
        //  - Frogger PSX Build 02
        //  - Frogger PSX Build 11
        //  - Frogger PSX Build 20 (NTSC)
        //  - Frogger PSX Build 71 (NTSC Retail)
        //  - Frogger PSX Build 75 (PAL Retail)
        //  - MoonWarrior 0.05a
        //  - MediEvil II 0.19
        //  - MediEvil II USA Retail 1.1Q
        //  - C-12 Final Resistance PSX Build 0.03a
        //  - C-12 Final Resistance PSX Beta Candidate 3

        // Calculated mostly perfect against:
        //  - MediEvil Rolling Demo (2 failures, expecting value: 1)
        //  - MediEvil ECTS (September 1997) (2 failures, expecting value: 1)
        //  - MediEvil NTSC (Retail) (4 failures, expecting value: 1)
        //  - Beast Wars PSX NTSC (Retail) (1 failure, expecting value: 1)
        //  - Beast Wars PSX PAL (Retail) (1 failure, expecting value: 1)
        //  - C-12 Final Resistance PSX Master (8 failures, expecting value: 1)
        //  - There is one exception which is that if the image is larger than width=256, the padding may be incorrect.
        //  - This is a limitation of us not actually knowing what the original image width/height was.
        // The following check seems equivalent to if ((padX <= 1 && shouldUvOriginStartAtOne())), but it's usable without having calculated paddingY.
        if ((padX <= 1 && this.paddingEnabled)) // "is Y padding enabled?, and the alignment padding won't cover it"
            padX += (this.bitDepth == PsxImageBitDepth.CLUT4) ? 4 : 2;

        return padX;
    }

    // Needs updated: clutMode, unpaddedWidth, paddingEnabled
    private int calculatePaddingY() {
        // On PC, it looks like padX might always = padY.
        // I suspect this is because PC doesn't have any of the restrictions/considerations that PSX VRAM has.
        // And since padding on PC seems to be about polygon edges (which is impacted by both vertical and horizontal padding the same), it makes sense why padX might match padY.
        //  - Frogger PC Milestone 3
        //  - Frogger PC Alpha
        //  - Frogger PC July
        //  - Frogger PC September
        //  - Frogger PC v1.0 (Retail)
        //  - Frogger PC v3.0e (Retail)
        //  - Beast Wars PC Retail
        // NOTE: If calculatePaddingX() is incorrect, the result of this function will be incorrect too.
        // But, this indicates a problem with calculatePaddingX(), not this function.
        if (getGameInstance().isPC())
            return calculatePaddingX();

        // Validated perfect match against:
        //  - Frogger PSX Milestone 3 (Pre-Recode)
        //  - Frogger PSX Sony Demo
        //  - Frogger PSX Alpha
        //  - Frogger PSX Build 02
        //  - Frogger PSX Build 11
        //  - Frogger PSX Build 17
        //  - Frogger PSX Build 18 -> This version marks a change in paddingY behavior.
        //  - Frogger PSX Build 20 (NTSC)
        //  - Frogger PSX Build 71 (NTSC Retail)
        //  - Frogger PSX Build 75 (PAL Retail)
        //  - MoonWarrior 0.05a
        //  - C-12 Final Resistance PSX Build 0.03a
        //  - C-12 Final Resistance PSX Beta Candidate 3

        // Validated mostly matching against:
        //  - MediEvil Rolling Demo (August 1997) (24 failures, expecting value: 1)
        //  - MediEvil ECTS (September 1997) (23 failures, expecting value: 1)
        //  - MediEvil NTSC (Retail) (19 failures, expecting value: 1)
        //  - Beast Wars PSX NTSC (Retail) (28 failures, expecting value: 1)
        //  - Beast Wars PSX PAL (Retail) (14 failures, expecting value: 1)
        //  - MediEvil II 0.19 (3 failures, expecting value: 1)
        //  - MediEvil II USA Retail 1.1Q (3 failures, expecting value: 1)
        //  - C-12 Final Resistance PSX Master (18 failures, expecting value: 1)
        if (this.paddingEnabled)
            return 2; // It's always even so there's room on the left AND right.

        return 0;
    }

    private static int getModuloReversed(int value, int modulo) {
        int result = value % modulo;
        return (result != 0) ? (modulo - result) : 0;
    }

    private boolean isPsxMode() {
        return this.parent != null ? this.parent.isPsxMode() : getGameInstance().isPSX();
    }

    private short getUnitWidth() {
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
        String originalName = getOriginalName();
        if (originalName != null)
            builder.append("'").append(originalName).append("'/");
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
        final int maxX = isPsxMode() ? PsxVram.PSX_VRAM_MAX_POSITION_X : PC_VRAM_MAX_POSITION_X;
        if (newVramX < 0 || newVramX + this.paddedWidth > maxX)
            throw new IllegalArgumentException("The provided x coordinate would result in the image being placed at least partially outside of VRAM! (newVramX: " + newVramX + ", paddedWidth: " + this.paddedWidth  + ")");

        this.vramX = (short) newVramX;
    }

    /**
     * Sets the vramY value.
     * @param newVramY the new vramY value to apply
     */
    public void setVramY(int newVramY) {
        final int maxY = isPsxMode() ? PsxVram.PSX_VRAM_MAX_POSITION_Y : PC_VRAM_MAX_POSITION_Y;
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

        this.textureId = textureId;
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
        if (!isPsxMode())
            throw new IllegalArgumentException("ABR values can only be set in PSX VLO files.");

        this.abr = textureAbr;
    }

    /**
     * Calculates the page this image lies in.
     * @return page
     */
    public short getPage() {
        return getPage(this.vramX, this.vramY);
    }

    /**
     * Gets the page the end of this image lies on.
     * @return endPage
     */
    public short getEndPage() {
        return getPage(this.vramX + ((this.paddedWidth - 1) / getWidthMultiplier()), this.vramY + this.paddedHeight - 1);
    }

    private short getPage(int vramX, int vramY) {
        if (isPsxMode()) {
            return (short) (((vramY / PsxVram.PSX_VRAM_PAGE_HEIGHT) * PsxVram.PSX_VRAM_PAGE_COUNT_X) + (vramX / PsxVram.PSX_VRAM_PAGE_UNIT_WIDTH));
        } else if (getGameInstance().getGameType().isAtLeast(SCGameType.FROGGER)) {
            return (short) (vramY / VloImage.PC_VRAM_PAGE_HEIGHT);
        } else {
            // Old Frogger PC Milestone 3 does this.
            return (short) (vramX / VloImage.PC_VRAM_PAGE_WIDTH);
        }
    }

    /**
     * Gets the tpage short for this image.
     * This information seems very similar to what's found on:
     * <a href="http://wiki.xentax.com/index.php/Playstation_TMD"/>
     * @return tpageShort
     */
    public short getTexturePageShort() {
        return (short) ((getPage() & 0b11111) | (this.abr.ordinal() << 5) | (this.bitDepth.ordinal() << 7));
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
        int pageVramX = (this.vramX % (isPsxMode() ? PsxVram.PSX_VRAM_PAGE_UNIT_WIDTH : VloImage.PC_VRAM_PAGE_WIDTH)) * getWidthMultiplier();
        return (short) (pageVramX + getLeftPadding());
    }

    /**
     * Generates the V value used by this image.
     * @return vValue
     */
    public short getV() {
        int pageVramY = (this.vramY % (isPsxMode() ? PsxVram.PSX_VRAM_PAGE_HEIGHT : VloImage.PC_VRAM_PAGE_HEIGHT));
        return (short) (pageVramY + getUpPadding());
    }

    private int loadClutColor(PSXClutColor color) {
        // NOTE: Because this is about loading the image, we are looking for alpha 0, 127, and 255. None others!
        int argb = color.toARGB(false, null);
        if (color.isStp() == getExpectedStpBit(color))
            return argb;

        // STP bit flip warning.
        if (!isPixelStpBitFlipAllowed())
            getLogger().warning("Clut color had unexpected STP bit! This feature works, but isn't expected to be used in this game.");

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

        this.clutId = clut != null ? clut.getClutID() : 0;
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
    }

    private void markNeedsVramRefresh() {
        // TODO: Implement in future.
    }

    /**
     * Replace this texture with a new one.
     * The image bit-depth will be maintained as its current bit-depth.
     * Padding will be automatically generated.
     * @param image The new image to use.
     */
    public void replaceImage(BufferedImage image, ProblemResponse response) {
        replaceImage(image, null, response);
    }

    /**
     * Replace this texture with a new one.
     * Padding will be automatically generated.
     * @param image The new image to use.
     * @param bitDepth the bit-depth to import the image as. On PC, this value is ignored. A null value indicates that the pre-existing bit depth should be used.
     * @param response Controls how this function responds to a problem, if one occurs.
     */
    public void replaceImage(BufferedImage image, PsxImageBitDepth bitDepth, ProblemResponse response) {
        replaceImage(image, bitDepth, -1, -1, response);
    }

    /**
     * Replace this texture with a new one.
     * @param image The new image to use.
     * @param bitDepth the bit-depth to import the image as. On PC, this value is ignored. A null value indicates that the pre-existing bit depth should be used.
     * @param paddingX the padding width to apply to the image. If a negative value is provided, padding width will be automatically calculated.
     * @param paddingY the padding height to apply to the image. If a negative value is provided, padding height will be automatically calculated.
     * @param response Controls how this function responds to a problem, if one occurs.
     */
    public void replaceImage(BufferedImage image, PsxImageBitDepth bitDepth, int paddingX, int paddingY, ProblemResponse response) {
        if (image == null)
            throw new NullPointerException("image");
        if (bitDepth == null)
            bitDepth = this.bitDepth;

        int oldPaddedWidth = this.paddedWidth;
        int oldPaddedHeight = this.paddedHeight;

        // We can only parse TYPE_INT_ARGB, so if it's not that, we must convert the image to that, so it can be parsed properly.
        image = ImageUtils.convertBufferedImageToFormat(image, BufferedImage.TYPE_INT_ARGB);

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

        // Get current padding, without PSX alignment.
        int oldPaddingXAlignment = calculatePsxAlignmentPaddingX();
        int oldPaddingX = this.paddedWidth - this.unpaddedWidth - oldPaddingXAlignment;
        int oldPaddingY = this.paddedHeight - this.unpaddedHeight - oldPaddingXAlignment;

        // Calculate padding changes.
        // Padding calculation needs: bitDepth, unpaddedWidth
        if (isPsxMode() && (this.bitDepth != bitDepth)) {
            this.bitDepth = bitDepth;
            markNeedsVramRefresh();
        }

        // Apply new dimensions.
        this.unpaddedWidth = (short) newInputImageWidth;
        this.unpaddedHeight = (short) newInputImageHeight;
        int newPaddingXAlignment = calculatePsxAlignmentPaddingX(); // Requires: unpaddedWidth, bitDepth

        // Calculate the new Y padding.
        // Happens before X so we can calculate paddingEnabled before calculatePaddingX() is potentially called.
        int newPaddingY;
        if (paddingY >= 0) {
            newPaddingY = paddingY;
            this.paddingEnabled = (paddingY > 0);
        } else {
            newPaddingY = calculatePaddingY();
            if (newPaddingY < 0)
                newPaddingY = newPaddingXAlignment + oldPaddingY;
        }

        // Calculate the new X padding.
        int newPaddingX;
        if (paddingX >= 0) {
            newPaddingX = paddingX;
        } else {
            newPaddingX = calculatePaddingX();
            if (newPaddingX < 0)
                newPaddingX = newPaddingXAlignment + oldPaddingX;
        }

        // Configure new padding.
        this.paddedWidth = (short) (this.unpaddedWidth + newPaddingX);
        this.paddedHeight = (short) (this.unpaddedHeight + newPaddingY);

        // Apply the image to this class.
        int[] newInputImageBuffer = ImageUtils.getReadOnlyPixelIntegerArray(image);
        this.pixelBuffer = new int[this.paddedWidth * this.paddedHeight];
        int dstOffset = getLeftPadding() + (getUpPadding() * this.paddedWidth);
        for (int y = 0, srcOffset = 0; y < this.unpaddedHeight; y++, dstOffset += this.paddedWidth, srcOffset += this.unpaddedWidth)
            System.arraycopy(newInputImageBuffer, srcOffset, this.pixelBuffer, dstOffset, this.unpaddedWidth);

        // Process/fix image alpha/transparency.
        // This should be done before the clut is generated.
        if (isPsxMode()) {
            // Collapse alphas down to the allowed values.
            boolean anyTransparentPixels = false;
            for (int i = 0; i < this.pixelBuffer.length; i++) {
                int color = this.pixelBuffer[i];
                int alpha = ColorUtils.getAlphaInt(color);
                if (alpha >= 170) {
                    alpha = IMAGE_ALPHA_REGULAR_STP_BIT_OPAQUE;
                } else if (alpha > 85) {
                    alpha = IMAGE_ALPHA_INVERTED_STP_BIT;
                } else /*if (alpha > 0)*/ {
                    alpha = IMAGE_ALPHA_REGULAR_STP_BIT_TRANS;
                    anyTransparentPixels = true;
                }

                this.pixelBuffer[i] = ColorUtils.setAlpha(color, (byte) alpha);
            }

            // This flag behavior has not been proven perfectly consistent with the original game files.
            // But, this flag is believed to never be accessed by any of the games/MR API.
            // Out of lazyness, I'll just assume this is how it is supposed to work and revisit this if problems arise.
            setFlag(FLAG_BLACK_IS_TRANSPARENT, anyTransparentPixels);
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
            setFlag(FLAG_BLACK_IS_TRANSPARENT, enableTransparency);
            for (int i = 0; i < this.pixelBuffer.length; i++) {
                int color = this.pixelBuffer[i];
                if (ColorUtils.getAlphaInt(color) <= 127) {
                    this.pixelBuffer[i] = 0xFF000000; // Set pixel to transparent. (Black is transparent)
                } else if ((color & PSXClutColor.ARGB8888_TO5BIT_COLOR_MASK) == 0 && enableTransparency) { // Color is black.
                    this.pixelBuffer[i] = 0xFF080808; // Set pixel to as close to black as possible without being transparent.
                } else {
                    this.pixelBuffer[i] |= 0xFF000000; // Ensure correct alpha.
                }
            }
        }

        // Generate padding.
        // Image must be applied before this point, and clut must be generated.
        // This should be done before image quantization.
        if (this.unpaddedWidth != this.paddedWidth || this.unpaddedHeight != this.paddedHeight) {
            int firstClutColor = PADDING_TRANSPARENT_PIXEL_PC;
            if (isPsxMode()) {
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

            generatePadding(this.pixelBuffer, PaddingOperation.APPLY, firstClutColor);
        }


        // Quantize down to the desired image bit depth.
        // Do this after padding is generated to ensure the correct number of colors are created.
        int colorCount = isPsxMode() ? getPaletteWidth(bitDepth) : -1;
        if (colorCount > 0)
            OctreeQuantizer.quantizeImage(this.pixelBuffer, colorCount);
        // ;image = MedianCutQuantizer.quantizeImageToARGB8888Image(image, colorCount);

        // Generate clut for newly imported image.
        // Generate the clut only after padding and quantization.
        regenerateClut(this.parent.getClutList(), null, true);

        // Handle dimension change...
        if (this.paddedWidth != oldPaddedWidth || this.paddedHeight != oldPaddedHeight)
            markNeedsVramRefresh();

        invalidateCache();
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
                    byte alpha = PSXClutColor.getAlpha(clutColor.isFullBlack(), clutColor.isStp(), enablePsxSemiTransparent, this.abr);
                    pixelArray[i] = ColorUtils.setAlpha(pixelArray[i], alpha);
                }
            } else {
                // PSX version should use full alpha if transparency is disabled.
                for (int i = 0; i < pixelArray.length; i++)
                    pixelArray[i] |= 0xFF000000; // Make the pixel fully opaque.
            }
        } else { // PC
            // Loaded PC images have 1 possible alpha value, 0xFF, so transparency is handled here, just like in-game.
            if (enableTransparency && testFlag(FLAG_BLACK_IS_TRANSPARENT)) {
                // PC version needs full black to be set to zero.
                for (int i = 0; i < pixelArray.length; i++)
                    if ((pixelArray[i] & PSXClutColor.ARGB8888_TO5BIT_COLOR_MASK) == 0) // If the image (in 16-bit form) is full-black...
                        pixelArray[i] &= 0x00FFFFFF; // Make the pixel transparent.
            }
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
     * Gets the name configured as the original name for this image.
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
        return testFlag(FLAG_BLACK_IS_TRANSPARENT);
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

    // This function has been tested perfectly matching against:
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

    // Game versions which are not perfectly matched.
    //  - MediEvil ECTS (107 pixel failures in 2934/2@GENTITLP.VLO and 2938/2@GENTITLN.VLO)
    //    -> Some padding pixels seem to be part of the image, like HitX/HitY.
    //  - MediEvil 0.31 (124 pixel failures in gargoyle_eye/149@FIXEDVRM.VLO, logop/2@GENTITLP.VLO, and, logon/2@GENTITLN.VLO)
    //  - MediEvil Reviewable Version (124 pixel failures)
    //  - MediEvil NTSC Release (124 pixel failures in gargoyle_eye/153@FIXEDVRM.VLO, logop/2@GENTITLP.VLO, and, logon/2@GENTITLN.VLO)
    //  - MediEvil II 0.19 (105 pixel failures in 13/116@FILE_0002 and 27/149@FILE_0002)
    //  - MediEvil II 0.51 (105 pixel failures in 36/103@File 10 and 50/131@File 10)
    //  - MediEvil II USA Retail 1.1Q (105 pixel failures in 36/103@File 10 and 50/131@File 10)
    private int getPaddingColor(int[] paddedImagePixels, int index, int paddedWidth, int padMinX, int padMaxX, int padMinY, int padMaxY, int paddingX, int paddingY, int emptyRightPaddingX, int firstClutColor) {
        // MediEvil II has some weird images which are mostly padding, and have a texture with crudely drawn letters: "PG".
        // At the time of writing, I have no idea what this is for, but we'll leave it alone for now.
        // One theory is that this was perhaps used to support images with dimensions larger than 256.
        if ((paddingX == 258 || paddingY == 258) && getGameInstance().isMediEvil2())
            return paddedImagePixels[index];

        int padPixelX = index % paddedWidth;
        if (padPixelX >= paddedWidth - emptyRightPaddingX)
            return firstClutColor; // Some of the right-most clut pixels use clut color zero.

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
            return getGameInstance().isPC() ? PADDING_TRANSPARENT_PIXEL_PC : PADDING_TRANSPARENT_PIXEL_PSX;

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
}