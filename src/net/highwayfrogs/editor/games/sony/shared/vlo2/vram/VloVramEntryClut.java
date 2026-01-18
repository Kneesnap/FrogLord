package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloClut;

import java.awt.*;

/**
 * Allows representing a clut in PSX Vram.
 * Created by Kneesnap on 1/16/2026.
 */
public class VloVramEntryClut extends VloVramEntry {
    private final VloClut clut;

    public VloVramEntryClut(VloClut clut) {
        super(clut.getWidth(), clut.getHeight());
        this.clut = clut;
    }

    @Override
    public int getX() {
        return this.clut.getX();
    }

    @Override
    public int getY() {
        return this.clut.getY();
    }

    @Override
    public void setPosition(int x, int y) {
        this.clut.setup(x, y, this.clut.getWidth(), this.clut.getHeight());
    }

    @Override
    public void draw(Graphics2D graphics, boolean selected) {
        graphics.drawImage(this.clut.makeImage(), null, this.clut.getX() * PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT, this.clut.getY());
    }
}
