package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloUtils;

import java.awt.*;

/**
 * Represents an entry in vram.
 * Created by Kneesnap on 1/16/2026.
 */
@Getter
@RequiredArgsConstructor
public abstract class VloVramEntry {
    private final int width;
    private final int height;

    /**
     * Gets the X coordinate of the entry
     * @return xPosition
     */
    public abstract int getX();

    /**
     * Gets the Y coordinate of the entry
     * @return YPosition
     */
    public abstract int getY();

    /**
     * Gets the vram page which this entry is located on
     * @param instance the instance to get the page from
     * @return vramPage
     */
    public int getVramPage(SCGameInstance instance) {
        return VloUtils.getPageFromVramPos(instance, getX(), getY());
    }

    /**
     * Sets the position of this vram entry
     * @param x the x coordinate to place the entry at
     * @param y the y coordinate to place the entry at
     */
    public abstract void setPosition(int x, int y);

    /**
     * Draws the vlo entry to the graphics object
     * @param graphics the graphics object to draw to
     * @param selected if true, the entry should be drawn as if it has been selected in some sort of UI.
     */
    public abstract void draw(Graphics2D graphics, boolean selected);
}
