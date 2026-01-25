package net.highwayfrogs.editor.games.sony.shared.vlo2;

import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;

/**
 * Contains static utilities for working with Vlo files.
 * Created by Kneesnap on 1/16/2026.
 */
public class VloUtils {
    /**
     * Gets the number of vram pages available.
     * @param psxMode if the VLO is PSX mode.
     * @return pageCount
     */
    public static int getPageCount(boolean psxMode) {
        return psxMode ? PsxVram.PSX_VRAM_TOTAL_PAGES : VloImage.PC_VRAM_TOTAL_PAGES;
    }

    /**
     * Gets the width of a single vram page, in vram units.
     * @param psxMode if we are working with the psx vram format
     * @return unitPageWidth
     */
    public static int getUnitPageWidth(boolean psxMode) {
        return psxMode ? PsxVram.PSX_VRAM_PAGE_UNIT_WIDTH : VloImage.PC_VRAM_PAGE_WIDTH;
    }

    /**
     * Gets the width of a vram page (in expanded form).
     * @param psxMode if we are working with the psx vram format
     * @return expandedPageWidth
     */
    public static int getExpandedPageWidth(boolean psxMode) {
        return psxMode ? PsxVram.PSX_VRAM_PAGE_EXPANDED_WIDTH : VloImage.PC_VRAM_PAGE_WIDTH;
    }

    /**
     * Gets the height of a vram page.
     * @param psxMode if we are working with the psx vram format
     * @return pageHeight
     */
    public static int getPageHeight(boolean psxMode) {
        return psxMode ? PsxVram.PSX_VRAM_PAGE_HEIGHT : VloImage.PC_VRAM_PAGE_HEIGHT;
    }

    /**
     * Gets the maximum vram X position (in expanded form).
     * @param psxMode if we are working with the psx vram format
     * @return maxVramExpandedPositionX
     */
    public static int getVramExpandedMaxPositionX(boolean psxMode) {
        return psxMode ? PsxVram.PSX_VRAM_MAX_EXPANDED_POSITION_X : VloImage.PC_VRAM_MAX_POSITION_X;
    }

    /**
     * Gets the maximum vram X position (in unit form).
     * @param psxMode if we are working with the psx vram format
     * @return maxVramUnitPositionX
     */
    public static int getVramUnitMaxPositionX(boolean psxMode) {
        return psxMode ? PsxVram.PSX_VRAM_MAX_POSITION_X : VloImage.PC_VRAM_MAX_POSITION_X;
    }

    /**
     * Gets the maximum vram Y position.
     * @param psxMode if we are working with the psx vram format
     * @return maxVramPositionY
     */
    public static int getVramMaxPositionY(boolean psxMode) {
        return psxMode ? PsxVram.PSX_VRAM_MAX_POSITION_Y : VloImage.PC_VRAM_MAX_POSITION_Y;
    }

    private static void ensurePageValid(boolean psxMode, int page) {
        int totalPages = getPageCount(psxMode);
        if (page < 0 || page >= totalPages)
            throw new IllegalArgumentException("page " + page + " is invalid, valid pages are within [0, " + totalPages + ").");
    }

    /**
     * Gets the start X coordinate of the page (in expanded form).
     * @param psxMode if we are working with the psx vram format
     * @param page the index of the vram page
     * @return pageExpandedStartX
     */
    public static int getPageExpandedStartX(boolean psxMode, int page) {
        ensurePageValid(psxMode, page);
        if (psxMode) {
            return ((page % PsxVram.PSX_VRAM_PAGE_COUNT_X) * PsxVram.PSX_VRAM_PAGE_EXPANDED_WIDTH);
        } else {
            return 0;
        }
    }

    /**
     * Gets the start X coordinate of the page (in unit form).
     * @param psxMode if we are working with the psx vram format
     * @param page the index of the vram page
     * @return pageUnitStartX
     */
    public static int getPageUnitStartX(boolean psxMode, int page) {
        ensurePageValid(psxMode, page);
        if (psxMode) {
            return ((page % PsxVram.PSX_VRAM_PAGE_COUNT_X) * PsxVram.PSX_VRAM_PAGE_UNIT_WIDTH);
        } else {
            return 0;
        }
    }

    /**
     * Gets the start Y coordinate of the page.
     * @param psxMode if we are working with the psx vram format
     * @param page the index of the vram page
     * @return pageStartY
     */
    public static int getPageStartY(boolean psxMode, int page) {
        ensurePageValid(psxMode, page);
        if (psxMode) {
            return ((page / PsxVram.PSX_VRAM_PAGE_COUNT_X) * PsxVram.PSX_VRAM_PAGE_HEIGHT);
        } else {
            return (page * VloImage.PC_VRAM_PAGE_HEIGHT);
        }
    }

    /**
     * Gets the vram page X grid coordinate.
     * @param psxMode if we are working with the psx vram format
     * @param page the index of the vram page
     * @return pageGridX
     */
    public static int getPageGridX(boolean psxMode, int page) {
        return psxMode ? (page % PsxVram.PSX_VRAM_PAGE_COUNT_X) : 0;
    }

    /**
     * Gets the vram page Y grid coordinate.
     * @param psxMode if we are working with the psx vram format
     * @param page the index of the vram page
     * @return pageGridY
     */
    public static int getPageGridY(boolean psxMode, int page) {
        return psxMode ? (page / PsxVram.PSX_VRAM_PAGE_COUNT_X) : page;
    }

    /**
     * Gets the index of the vram page which corresponds to the given grid coordinates.
     * @param psxMode if we are working with the psx vram format
     * @param pageGridX the page grid X coordinate
     * @param pageGridY the page grid Y coordinate
     * @return the index of the vram page
     */
    public static int getPageFromGridPos(boolean psxMode, int pageGridX, int pageGridY) {
        if (psxMode) {
            if (pageGridX < 0 || pageGridX >= PsxVram.PSX_VRAM_PAGE_COUNT_X)
                throw new IllegalArgumentException("PC pageGridX must be in the range [0, " + PsxVram.PSX_VRAM_PAGE_COUNT_X + "), was: " + pageGridX);
            if (pageGridY < 0 || pageGridY >= PsxVram.PSX_VRAM_PAGE_COUNT_Y)
                throw new IllegalArgumentException("PC pageGridY must be in the range [0, " + PsxVram.PSX_VRAM_PAGE_COUNT_Y + "), was: " + pageGridY);

            return (pageGridY * PsxVram.PSX_VRAM_PAGE_COUNT_X) + pageGridX;
        } else {
            if (pageGridX != 0)
                throw new IllegalArgumentException("PC pageGridX must always be zero! (Was: " + pageGridX + ")");
            if (pageGridY < 0 || pageGridY >= VloImage.PC_VRAM_TOTAL_PAGES)
                throw new IllegalArgumentException("PC pageGridY must be in the range [0, " + VloImage.PC_VRAM_TOTAL_PAGES + "), was: " + pageGridY);

            return pageGridY;
        }
    }

    /**
     * Gets the vram page from a vram position
     * @param instance the game instance to calculate for. (Different versions operate differently)
     * @param vramUnitX the vram x position (in unit form) to calculate the page from
     * @param vramY the vram y position
     * @return vramPage
     */
    public static short getPageFromVramPos(SCGameInstance instance, int vramUnitX, int vramY) {
        if (instance.isPSX()) {
            return (short) (((vramY / PsxVram.PSX_VRAM_PAGE_HEIGHT) * PsxVram.PSX_VRAM_PAGE_COUNT_X) + (vramUnitX / PsxVram.PSX_VRAM_PAGE_UNIT_WIDTH));
        } else if (instance.getGameType().isAtLeast(SCGameType.FROGGER)) {
            return (short) (vramY / VloImage.PC_VRAM_PAGE_HEIGHT);
        } else {
            // Old Frogger PC Milestone 3 does this.
            return (short) (vramUnitX / VloImage.PC_VRAM_PAGE_WIDTH);
        }
    }
}
