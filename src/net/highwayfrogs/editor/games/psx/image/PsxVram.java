package net.highwayfrogs.editor.games.psx.image;

/**
 * Contains definitions for modelling PSX Vram specifications.
 * PSX Vram is really simple, it's just a megabyte of memory, which is often treated directly as one big image.
 * It can be indexed using coordinates.
 * Created by Kneesnap on 01/01/2026.
 */
public class PsxVram {
    public static final int PSX_VRAM_LOAD_FORMAT_BYTES_PER_PIXEL = 2; // The number of bytes per-pixel when using PsyQ's LoadImage().
    public static final int PSX_VRAM_MAX_PIXELS_PER_BYTE = 2;
    public static final int PSX_VRAM_MAX_PIXELS_PER_UNIT = 4; // One 'unit' is 2 bytes. It could hold one 16-bit pixel, two 8-bit pixels, or four 4-bit pixels.
    public static final int PSX_VRAM_PAGE_UNIT_WIDTH = 64; // VRAM pages are only indexable in 64 indices. Each index represents two bytes of data, which could contain 1, 2, or 4 pixels worth of data.
    public static final int PSX_VRAM_PAGE_BYTE_WIDTH = PSX_VRAM_PAGE_UNIT_WIDTH * PSX_VRAM_MAX_PIXELS_PER_BYTE; // 128
    public static final int PSX_VRAM_PAGE_EXPANDED_WIDTH = PSX_VRAM_PAGE_UNIT_WIDTH * PSX_VRAM_MAX_PIXELS_PER_UNIT; // 256
    public static final int PSX_VRAM_PAGE_HEIGHT = 256;
    public static final int PSX_VRAM_PAGE_COUNT_X = 16;
    public static final int PSX_VRAM_MAX_EXPANDED_POSITION_X = PSX_VRAM_PAGE_COUNT_X * PSX_VRAM_PAGE_EXPANDED_WIDTH; // 4096
    public static final int PSX_VRAM_MAX_POSITION_X = PSX_VRAM_PAGE_COUNT_X * PSX_VRAM_PAGE_UNIT_WIDTH; // 1024
    public static final int PSX_VRAM_PAGE_COUNT_Y = 2;
    public static final int PSX_VRAM_TOTAL_PAGES = PSX_VRAM_PAGE_COUNT_X * PSX_VRAM_PAGE_COUNT_Y; // 32. This is a hard maximum even if we made a modded emulator, as only 5 bits are available for the page ID.
    public static final int PSX_VRAM_MAX_POSITION_Y = PSX_VRAM_PAGE_COUNT_Y * PSX_VRAM_PAGE_HEIGHT; // 512
}
