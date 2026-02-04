package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.psx.image.PsxVramBox;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameObject.SCSharedGameObject;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloClut;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

/**
 * A snapshot of Vram texture/entries and their positions.
 * The behavior seen here is not perfectly consistent with the original Vorg program, it packs textures more efficiently.
 * Unfortunately, many of the questionable design choices of Vorg must be maintained for engine compatibility.
 * Created by Kneesnap on 1/15/2026.
 */
public class VloVramSnapshot extends SCSharedGameObject {
    @Getter private final VloTreeNode node;
    private final int width;
    private final int height;
    private final VloVramEntry[][] cachedTextureLocations;
    private final int[][] cachedTextureStartXPositions;
    private final List<VloVramEntry> entries = new ArrayList<>();

    public static final int PSX_VRAM_BOTTOM_PAGE_BIT_MASK = 0xFFFF0000; // Bits 16-31 represent pages on the bottom of Vram on PSX, which is where Cluts are preferred to be placed.

    public VloVramSnapshot(SCGameInstance instance, VloTreeNode node) {
        super(instance);
        this.node = node;

        boolean psxMode = instance.isPSX();
        int pageCount = VloUtils.getPageCount(psxMode);
        this.width = VloUtils.getVramUnitMaxPositionX(psxMode);
        this.height = VloUtils.getVramMaxPositionY(psxMode);

        int pageHeight = VloUtils.getPageHeight(psxMode);
        this.cachedTextureStartXPositions = new int[pageCount][pageHeight];
        this.cachedTextureLocations = new VloVramEntry[this.height][this.width];
    }

    /**
     * Clears the snapshot to a default state.
     */
    void clear() {
        for (int i = 0; i < this.cachedTextureStartXPositions.length; i++)
            Arrays.fill(this.cachedTextureStartXPositions[i], 0);
        for (int i = 0; i < this.cachedTextureLocations.length; i++)
            Arrays.fill(this.cachedTextureLocations[i], null);
        this.entries.clear();
    }

    /**
     * Copy the contents of this snapshot to another.
     * @param otherSnapshot the snapshot to copy to
     */
    void copyTo(VloVramSnapshot otherSnapshot) {
        if (otherSnapshot == null)
            throw new NullPointerException("otherSnapshot");

        for (int i = 0; i < this.cachedTextureStartXPositions.length; i++)
            System.arraycopy(this.cachedTextureStartXPositions[i], 0, otherSnapshot.cachedTextureStartXPositions[i], 0, this.cachedTextureStartXPositions[i].length);
        for (int i = 0; i < this.cachedTextureLocations.length; i++)
            System.arraycopy(this.cachedTextureLocations[i], 0, otherSnapshot.cachedTextureLocations[i], 0, this.cachedTextureLocations[i].length);
        otherSnapshot.entries.clear();
        otherSnapshot.entries.addAll(this.entries);
    }

    /**
     * Draws the vram snapshot to a BufferedImage.
     * Contains all textures found in the snapshot, and all tree parent snapshots.
     * @return snapshotImage
     */
    public BufferedImage toBufferedImage() {
        return toBufferedImage(null, null);
    }

    /**
     * Draws the Vram snapshot to a BufferedImage.
     * @param oldImage the image to write the snapshot to. A new image is created if it is null or the wrong dimensions.
     * @param selectedEntry the entry which is currently selected in the UI, if there is one.
     * @return snapshotImage
     */
    public BufferedImage toBufferedImage(BufferedImage oldImage, VloVramEntry selectedEntry) {
        BufferedImage image = oldImage;
        int expandedWidth = VloUtils.getVramExpandedMaxPositionX(getGameInstance().isPSX());
        if (image == null || (expandedWidth != image.getWidth() || this.height != image.getHeight()))
            image = new BufferedImage(expandedWidth, this.height, BufferedImage.TYPE_INT_ARGB);

        // Draw on image.
        Graphics2D graphics = image.createGraphics();

        try {
            // Fill background.
            graphics.setColor(Constants.COLOR_TURQUOISE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            drawImage(graphics, selectedEntry);
        } finally {
            graphics.dispose();
        }

        return image;
    }

    /**
     * Draws the entries to the given graphics context.
     * @param graphics the graphics context to draw to
     * @param selectedEntry the entry which is currently selected
     */
    public void drawImage(Graphics2D graphics, VloVramEntry selectedEntry) {
        if (graphics == null)
            throw new NullPointerException("graphics");

        for (int i = 0; i < this.entries.size(); i++) {
            VloVramEntry entry = this.entries.get(i);
            entry.draw(graphics, entry == selectedEntry);
        }
    }

    /**
     * Adds node data to the snapshot.
     */
    public void addNodeData() {
        if (this.node == null)
            throw new NullPointerException("node");

        // If this is the tree root, add primary frame-buffers.
        boolean psxMode = getGameInstance().isPSX();
        if (this.node instanceof VloTree) {
            if (getGameInstance().getPrimaryFrameBuffer() != null)
                addEntry(new VloVramEntryReserved(getGameInstance().getPrimaryFrameBuffer(), Constants.COLOR_DEEP_GREEN, psxMode, "Primary Framebuffer"));
            if (getGameInstance().getSecondaryFrameBuffer() != null)
                addEntry(new VloVramEntryReserved(getGameInstance().getSecondaryFrameBuffer(), Constants.COLOR_DARK_YELLOW, psxMode, "Secondary Framebuffer"));
        }

        // Reserve pages.
        int pageCount = VloUtils.getPageCount(psxMode);
        int pageWidth = VloUtils.getUnitPageWidth(psxMode);
        int pageHeight = VloUtils.getPageHeight(psxMode);
        for (int i = 0; i < pageCount; i++) {
            if (!this.node.isPageReserved(i))
                continue;

            PsxVramBox reservedPageArea = new PsxVramBox(VloUtils.getPageUnitStartX(psxMode, i), VloUtils.getPageStartY(psxMode, i), pageWidth, pageHeight);
            addEntry(new VloVramEntryReserved(reservedPageArea, Constants.COLOR_LIGHT_TURQUOISE, psxMode, "Reserved Page"));
        }
    }

    /**
     * Adds a VloFile to the snapshot.
     * @param tree the tree responsible for
     * @param vloFile the vlo file to add to the snapshot
     * @param recalculatePositions if true, the vlo
     */
    void addVlo(VloTree tree, VloFile vloFile, boolean recalculatePositions) {
        if (tree == null)
            throw new NullPointerException("tree");
        if (vloFile == null)
            throw new NullPointerException("vloFile");

        VloTreeNode node = tree.getNode(vloFile);
        if (node == null)
            throw new IllegalArgumentException("The vloFile " + vloFile.getFileDisplayName() + " did not have a VloTreeNode associated with it.");

        // Add images without recalculating positions.
        List<VloImage> originalImages = vloFile.getImages();
        if (!recalculatePositions) {
            for (int i = 0; i < originalImages.size(); i++)
                addEntry(new VloVramEntryImage(originalImages.get(i)));

            if (vloFile.isPsxMode()) {
                List<VloClut> cluts = vloFile.getClutList().getCluts();
                for (int i = 0; i < cluts.size(); i++)
                    addEntry(new VloVramEntryClut(cluts.get(i)));
            }

            return;
        }

        // Add images by recalculating positions.
        List<VloImage> sortedImages = originalImages;
        if (!vloFile.isSortingOrderKnown()) {
            sortedImages = new ArrayList<>(originalImages);
            sortedImages.sort(Comparator.comparingInt((VloImage image) -> image.getPaddedWidth() * image.getPaddedHeight()).reversed());
        }

        // We must use an identity set, so that if there are multiple cluts with the same contents, they will both be added independently.
        // This happens in MediEvil, and therefore must be supported, even if FrogLord merges duplicate cluts. (The cluts here aren't rebuilt, so...)
        Set<VloClut> addedCluts = Collections.newSetFromMap(new IdentityHashMap<>());

        // Add images.
        VloTreeNodeFillMethod fillMethod = this.node.getFillMethod();
        for (int i = 0; i < sortedImages.size(); i++)
            if (!tryAddTexture(sortedImages.get(i), fillMethod, addedCluts, node.getUsablePages(), node.getExtraPages()))
                throw new RuntimeException("There was not enough space in VRAM to fit " + sortedImages.get(i) + ". (Fill Mode: " + fillMethod + ")");
    }


    /**
     * Adds a predefined entry to the vram snapshot.
     * @param entry the entry to add
     */
    public void addEntry(VloVramEntry entry) {
        if (entry == null)
            throw new NullPointerException("entry");

        this.entries.add(entry);
        applyEntryToCache(entry, true);
    }

    /**
     * Tries to add the clut.
     * @param clut the clut to add
     * @return wasAddedSuccessfully
     */
    public boolean tryAddClut(VloClut clut) {
        return clut != null && tryAddClut(clut, null, this.node.getClutPages(), this.node.getUsablePages(), this.node.getExtraPages());
    }

    /**
     * Tries to image.
     * @param image the image to add
     * @return wasAddedSuccessfully
     */
    public boolean tryAddImage(VloImage image, boolean addClut) {
        if (image == null)
            throw new NullPointerException("image");

        VloClut clut = image.getClut();
        Set<VloClut> addedCluts = null;
        if (clut != null && !addClut) {
            addedCluts = new HashSet<>();
            addedCluts.add(clut);
        }

        return tryAddTexture(image, this.node.getFillMethod(), addedCluts, this.node.getUsablePages(), this.node.getExtraPages());
    }

    private boolean tryAddTexture(VloImage image, VloTreeNodeFillMethod fillMethod, Set<VloClut> addedCluts, int usablePages, int extraPages) {
        VloVramEntryImage entry = new VloVramEntryImage(image);
        return tryAddTexture(entry, fillMethod, addedCluts, usablePages, usablePages, extraPages)
                || tryAddTexture(entry, fillMethod, addedCluts, extraPages, usablePages, extraPages);
    }

    private boolean tryAddTexture(VloVramEntryImage entry, VloTreeNodeFillMethod fillMethod, Set<VloClut> addedCluts, int currentPages, int usablePages, int extraPages) {
        boolean psxMode = getGameInstance().isPSX();
        int pageHeight = VloUtils.getPageHeight(psxMode);
        int pageCount = VloUtils.getPageCount(psxMode);

        int clutPages = this.node.getClutPages();
        if (fillMethod == VloTreeNodeFillMethod.FILL_PAGE || (fillMethod == VloTreeNodeFillMethod.AUTOMATIC && !psxMode)) {
            for (int page = 0; page < pageCount; page++)
                if ((currentPages & (1 << page)) != 0 && isPageTransparencyValid(psxMode, entry, page))
                    for (int y = 0; y < pageHeight; y++)
                        if (addEntryHorizontal(entry, y, page, currentPages))
                            return addImageClut(entry.getImage(), addedCluts, clutPages, usablePages, extraPages);
        } else if (fillMethod == VloTreeNodeFillMethod.SPREAD || (fillMethod == VloTreeNodeFillMethod.AUTOMATIC && (currentPages & PSX_VRAM_BOTTOM_PAGE_BIT_MASK) == 0)) {
            for (int y = 0; y < pageHeight; y++)
                for (int page = 0; page < pageCount; page++)
                    if ((currentPages & (1 << page)) != 0 && isPageTransparencyValid(psxMode, entry, page) && addEntryHorizontal(entry, y, page, currentPages))
                        return addImageClut(entry.getImage(), addedCluts, clutPages, usablePages, extraPages);
        } else if (fillMethod == VloTreeNodeFillMethod.AUTOMATIC) { // psxMode is true.
            // The top row of pages do not include cluts, so we can use fill page logic.
            for (int page = 0; page < PsxVram.PSX_VRAM_PAGE_COUNT_X; page++)
                if ((currentPages & (1 << page)) != 0)
                    for (int y = 0; y < pageHeight; y++)
                        if (addEntryHorizontal(entry, y, page, currentPages))
                            return addImageClut(entry.getImage(), addedCluts, clutPages, usablePages, extraPages);

            // The bottom row of pages contains cluts, so use spread logic.
            for (int y = 0; y < pageHeight; y++)
                for (int page = PsxVram.PSX_VRAM_PAGE_COUNT_X; page < pageCount; page++)
                    if ((currentPages & (1 << page)) != 0 && addEntryHorizontal(entry, y, page, currentPages))
                        return addImageClut(entry.getImage(), addedCluts, clutPages, usablePages, extraPages);
        } else {
            throw new UnsupportedOperationException("Unsupported fillMethod: " + fillMethod);
        }

        return false;
    }

    private boolean addImageClut(VloImage image, Set<VloClut> addedCluts, int clutPages, int usablePages, int extraPages) {
        VloClut clut = image.getClut();
        if (tryAddClut(clut, addedCluts, clutPages, usablePages, extraPages))
            return true; // No clut, everything is good.

        throw new RuntimeException("There was not enough VRAM space to add " + image + "'s CLUT!");
    }

    private boolean tryAddClut(VloClut clut, Set<VloClut> addedCluts, int clutPages, int usablePages, int extraPages) {
        if (clut == null || (addedCluts != null && !addedCluts.add(clut)))
            return true; // Clut has already been added.

        VloVramEntryClut entry = new VloVramEntryClut(clut);
        int pageHeight = VloUtils.getPageHeight(true); // Cluts are PSX only.

        // The behavior here has been loosely inspired by the behavior seen in the original games.
        // Or more specifically, Beast Wars NTSC. Other games were not referenced or checked,
        // Cluts are added to the bottom right-most page, right-to-left.

        // Next, try adding the CLUT to ANY usable page which has room, right to left.
        int pageCount = VloUtils.getPageCount(true);
        int passClutPages = clutPages != 0 ? clutPages : usablePages;
        return tryAddClut(entry, 0, clutPages, pageHeight, pageCount)
                || tryAddClut(entry, passClutPages, usablePages, pageHeight, pageCount)
                || tryAddClut(entry, 0, extraPages, pageHeight, pageCount);
    }

    private boolean tryAddClut(VloVramEntryClut entry, int clutPages, int currentPages, int pageHeight, int pageCount) {
        if (currentPages == 0)
            return false;

        // Cluts should only be on the bottom row of pages, if possible.
        int minClutPage = (clutPages & PSX_VRAM_BOTTOM_PAGE_BIT_MASK) != 0 ? PsxVram.PSX_VRAM_PAGE_COUNT_X : 0;
        for (int y = pageHeight - 1; y >= 0; y--)
            for (int page = pageCount - 1; page >= minClutPage; page--)
                if ((currentPages & (1 << page)) != 0 && addEntryHorizontal(entry, y, page, currentPages))
                    return true;

        return false;
    }

    // NOTE: It'd probably be better to make cluts check from maxX to minX instead of minX to maxX. However, I'm not sure if it'd be worth the effort.
    private boolean addEntryHorizontal(VloVramEntry entry, int localPageY, int page, int usablePages) {
        boolean psxMode = getGameInstance().isPSX();
        int pageWidth = VloUtils.getUnitPageWidth(psxMode);
        int pageHeight = VloUtils.getPageHeight(psxMode);

        int minPageX = VloUtils.getPageUnitStartX(psxMode, page);
        int maxPageX = minPageX + pageWidth;
        int minPageY = VloUtils.getPageStartY(psxMode, page);

        int startPageGridX = VloUtils.getPageGridX(psxMode, page);
        int startPageGridY = VloUtils.getPageGridY(psxMode, page);
        int x = minPageX + this.cachedTextureStartXPositions[page][localPageY];
        int y = minPageY + localPageY;

        // Abort if the entry would go outside of VRAM.
        int entryWidth = entry.getWidth();
        int entryHeight = entry.getHeight();
        int endY = y + entryHeight;
        if (endY > this.height)
            return false;

        x = fixClutX(entry, x);
        while (maxPageX > x && this.width >= x + entryWidth) {
            int endX = x + entryWidth;

            // In most situations, we want to avoid having textures cross page boundaries.
            // I guess technically we might not need to do this, but intuitively it feels like this will produce better texture packing.
            // If it ends up mattering I'll do some tests on it.
            int endPage = VloUtils.getPageFromVramPos(getGameInstance(), endX - 1, endY - 1);
            if (page != endPage && (entry instanceof VloVramEntryImage)) {
                VloImage image = ((VloVramEntryImage) entry).getImage();
                int endPageGridX = VloUtils.getPageGridX(psxMode, endPage);
                int endPageGridY = VloUtils.getPageGridY(psxMode, endPage);
                if (endPageGridX > startPageGridX && (!psxMode || entryWidth < ((pageWidth * image.getWidthMultiplier()) / PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT)))
                    return false; // If this check is removed on PSX, texture overflow will occur and the game will render the part of the texture page which does not have the texture.
                if (endPageGridY > startPageGridY && (!psxMode || entryHeight < pageHeight))
                    return false;
            }

            // Lazy fast checks. (Check corners, and if it's a top corner, skip past the end of the image because it's the same y level.)
            VloVramEntry temp = this.cachedTextureLocations[y][endX - 1];
            if ((temp != null) || ((temp = this.cachedTextureLocations[y][x]) != null)) {
                if (temp instanceof VloVramEntryClut && !(entry instanceof VloVramEntryClut))
                    return false; // If cluts have been reached, there's no way to fit the texture in on this page. (Requires cluts built from right to left, or kept away from images)

                x = fixClutX(entry, temp.getX() + temp.getWidth());
                continue;
            } else if ((temp = this.cachedTextureLocations[endY - 1][x]) != null || (temp = this.cachedTextureLocations[endY - 1][endX - 1]) != null) {
                if (temp instanceof VloVramEntryClut && !(entry instanceof VloVramEntryClut))
                    return false; // If cluts have been reached, there's no way to fit the texture in on this page. (Requires cluts built from right to left, or kept away from images)

                x = fixClutX(entry, temp.getX() + temp.getWidth());
                continue;
            }

            // We need to check if there's room for the texture.
            // We could iterate through every pixel and check if a texture is there, but I came up with a better idea.
            // Because of textures are always placed in the earliest upper-left pixel with enough space for the texture, it is theoretically impossible for the current entry to fully encapsulate a rectangle with a smaller area.
            // It's just not possible for one to have been placed since we don't support removing textures in this implementation.
            // Because of this, ALL rectangles which overlap with the current test position MUST cross one of the edges of the test rectangle.
            // Therefore, all we need to do is check if the borders overlap. This takes the amount of checks down from the area to the perimeter, which is a massive improvement.

            boolean foundSpot = true;

            // Vertical Checks:
            for (int searchY = y + 1; searchY < endY - 1 && foundSpot; searchY++)
                if (((temp = this.cachedTextureLocations[searchY][endX - 1]) != null) || ((temp = this.cachedTextureLocations[searchY][x]) != null))
                    foundSpot = false;

            // Horizontal Checks:
            for (int searchX = endX - 1; searchX > x && foundSpot; searchX--) // Search in backwards direction to find the furthest right position to place the texture at. (Most notable when near small textures)
                if (((temp = this.cachedTextureLocations[y][searchX]) != null) || ((temp = this.cachedTextureLocations[endY - 1][searchX]) != null))
                    foundSpot = false;

            if (!foundSpot) { // We'll just skip past it.
                if (temp instanceof VloVramEntryClut && !(entry instanceof VloVramEntryClut))
                    return false; // If cluts have been reached, there's no way to fit the texture in on this page. (Requires cluts built from right to left, or kept away from images)

                x = fixClutX(entry, temp.getX() + temp.getWidth());
                continue;
            }

            // Ensure the pages the texture would fall within are usable.
            if (page != endPage) {
                int endPageGridX = VloUtils.getPageGridX(psxMode, endPage);
                int endPageGridY = VloUtils.getPageGridY(psxMode, endPage);
                for (int testPageY = startPageGridY; testPageY <= endPageGridY; testPageY++) {
                    for (int testPageX = startPageGridX; testPageX <= endPageGridX; testPageX++) {
                        int testPage = VloUtils.getPageFromGridPos(psxMode, testPageX, testPageY);
                        if ((usablePages & (1 << testPage)) == 0 || !isPageTransparencyValid(psxMode, entry, testPage))
                            return false; // The texture bleeds into a non-usable page. It's not possible for the entry to fit in this line.
                    }
                }
            }

            entry.setPosition(x, y);
            this.entries.add(entry);
            this.applyEntryToCache(entry, true);
            return true;
        }

        return false;
    }

    private static int fixClutX(VloVramEntry entry, int x) {
        if (!(entry instanceof VloVramEntryClut))
            return x;

        int modulo = x % VloClut.X_POSITION_MODULO;
        return (modulo != 0) ? (x + (VloClut.X_POSITION_MODULO - modulo)) : x;
    }

    void applyEntryToCache(VloVramEntry entry, boolean addToParent) {
        int entryX = entry.getX();
        int entryY = entry.getY();

        // Writes information to cache.
        boolean psxMode = getGameInstance().isPSX();
        int pageWidth = VloUtils.getUnitPageWidth(psxMode);
        for (int cacheY = entryY; cacheY < entryY + entry.getHeight(); cacheY++) {
            int page = VloUtils.getPageFromVramPos(getGameInstance(), entryX, cacheY);
            int pageStartX = VloUtils.getPageUnitStartX(psxMode, page);
            int pageStartY = VloUtils.getPageStartY(psxMode, page);
            updateStartXPositions(entry, page, pageWidth, pageStartX, pageStartY, entryX, cacheY); // Fairly significant speedup.

            // Update caching of what textures are located where.
            Arrays.fill(this.cachedTextureLocations[cacheY], entryX, entryX + entry.getWidth(), entry);
        }

        if (addToParent && this.node != null && this.node.getSnapshot() != this)
            this.node.getSnapshot().applyEntryToCache(entry, false);
    }

    private void updateStartXPositions(VloVramEntry entry, int page, int pageWidth, int pageStartX, int pageStartY, int entryX, int cacheY) {
        int[] cachedStartXPositions = this.cachedTextureStartXPositions[page];

        int localY = cacheY - pageStartY;
        if (cachedStartXPositions[localY] >= entryX - pageStartX) { // Works with stuff past the entry.
            // If the entry bleeds to another page, update the positions in the other page.
            int furthestX = entryX + entry.getWidth();
            if (furthestX - pageStartX >= pageWidth) {
                cachedStartXPositions[localY] = pageWidth;
                if (furthestX - pageStartX > pageWidth)
                    updateStartXPositions(entry, page + 1, pageWidth, pageStartX + pageWidth, pageStartY, entryX, cacheY);
                return;
            }

            // Extend it further until it stops hitting textures or reaches the end of the page.
            VloVramEntry temp;
            while ((pageStartX + pageWidth) > furthestX && (temp = this.cachedTextureLocations[cacheY][furthestX]) != null)
                furthestX = temp.getX() + temp.getWidth();

            cachedStartXPositions[localY] = Math.min(furthestX - pageStartX, pageWidth);
        }
    }

    private boolean isPageTransparencyValid(boolean psxMode, VloVramEntry entry, int page) {
        if (psxMode || !(entry instanceof VloVramEntryImage))
            return true;

        VloImage image = ((VloVramEntryImage) entry).getImage();
        boolean blackIsTransparent = image.testFlag(VloImage.FLAG_BLACK_IS_TRANSPARENT); // This controls which part of VRAM this gets placed in.
        return blackIsTransparent == ((this.node.getTree().getTransparentPages() & (1 << page)) != 0);
    }
}
