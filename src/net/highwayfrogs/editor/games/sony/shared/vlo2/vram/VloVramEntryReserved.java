package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.psx.image.PsxVramBox;

import java.awt.*;

/**
 * Represents a portion of VRAM space which has been reserved.
 * Created by Kneesnap on 1/16/2026.
 */
public class VloVramEntryReserved extends VloVramEntry {
    private final PsxVramBox vramBox;
    private final Color color;
    private final boolean psxMode;

    public VloVramEntryReserved(PsxVramBox vramBox, Color color, boolean psxMode) {
        super(vramBox.getWidth(), vramBox.getHeight());
        this.color = color;
        this.vramBox = vramBox;
        this.psxMode = psxMode;
    }

    @Override
    public int getX() {
        return this.vramBox.getX();
    }

    @Override
    public int getY() {
        return this.vramBox.getY();
    }

    @Override
    public void setPosition(int x, int y) {
        throw new UnsupportedOperationException("Reserved Vram entries cannot be moved.");
    }

    @Override
    public void draw(Graphics2D graphics, boolean selected) {
        graphics.setColor(this.color); // Next frame.
        if (this.psxMode) {
            graphics.fillRect(this.vramBox.getX() * PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT, this.vramBox.getY(),
                    this.vramBox.getWidth() * PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT, this.vramBox.getHeight());
        } else {
            graphics.fillRect(this.vramBox.getX(), this.vramBox.getY(),
                    this.vramBox.getWidth(), this.vramBox.getHeight());
        }
    }
}
