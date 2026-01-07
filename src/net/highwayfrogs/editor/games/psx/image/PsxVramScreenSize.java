package net.highwayfrogs.editor.games.psx.image;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;

/**
 * Represents a framebuffer window in psx VRAM.
 * As a string, this is represented by "{width}x{height}@{x},{y}"
 * Created by Kneesnap on 02/01/2026.
 */
@Getter
@AllArgsConstructor
public class PsxVramScreenSize {
    private int x;
    private int y;
    private int width;
    private int height;

    private static final String FORMAT_STR = "{width}x{height}@{x},{y}";

    @Override
    public String toString() {
        return this.width + "x" + this.height + "@" + this.x + "," + this.y;
    }

    /**
     * Creates a clone of this screen size, offset by the given amount.
     * @param x the x offset
     * @param y the y offset
     * @return clonedSize
     */
    public PsxVramScreenSize add(int x, int y) {
        return new PsxVramScreenSize(this.x + x, this.y + y, this.width, this.height);
    }

    /**
     * Create a clone of this screen size, but below the current area.
     * @return newArea
     */
    public PsxVramScreenSize cloneBelow() {
        return add(0, this.height);
    }

    public static PsxVramScreenSize parse(String input) {
        if (StringUtils.isNullOrWhiteSpace(input))
            throw new NullPointerException("input");

        String[] atSplit = input.trim().split("@");
        if (atSplit.length != 2)
            throw new IllegalArgumentException("Input '" + input + "' is not formatted as '" + FORMAT_STR + "'.");

        String[] dimensionSplit = atSplit[0].split("x");
        String[] positionSplit = atSplit[1].split(",");
        if (dimensionSplit.length != 2 || !NumberUtils.isInteger(dimensionSplit[0]) || !NumberUtils.isInteger(dimensionSplit[1])
                || positionSplit.length != 2 || !NumberUtils.isInteger(positionSplit[0]) || !NumberUtils.isInteger(positionSplit[1]))
            throw new IllegalArgumentException("Input '" + input + "' is not formatted as '" + FORMAT_STR + "'.");

        int x = Integer.parseInt(positionSplit[0]);
        int y = Integer.parseInt(positionSplit[1]);
        int width = Integer.parseInt(dimensionSplit[0]);
        int height = Integer.parseInt(dimensionSplit[1]);
        return new PsxVramScreenSize(x, y, width, height);
    }
}
