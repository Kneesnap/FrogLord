package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.psx.image.PsxVramBox;
import net.highwayfrogs.editor.utils.StringUtils;

import java.awt.*;

/**
 * Represents a portion of VRAM space which has been reserved.
 * Created by Kneesnap on 1/16/2026.
 */
public class VloVramEntryReserved extends VloVramEntry {
    private final PsxVramBox vramBox;
    private final Color color;
    private final boolean psxMode;
    private final String titleText;

    private static final Font FONT = new Font("Dialog", 0, 12);

    public VloVramEntryReserved(PsxVramBox vramBox, Color color, boolean psxMode, String titleText) {
        super(vramBox.getWidth(), vramBox.getHeight());
        this.color = color;
        this.vramBox = vramBox;
        this.psxMode = psxMode;
        this.titleText = titleText;
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
        int x = this.psxMode ? this.vramBox.getX() * PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT : this.vramBox.getX();
        int y = this.vramBox.getY();
        int width = this.psxMode ? this.vramBox.getWidth() * PsxVram.PSX_VRAM_MAX_PIXELS_PER_UNIT : this.vramBox.getWidth();
        int height = this.vramBox.getHeight();

        graphics.setColor(this.color);
        graphics.fillRect(x, y, width, height);
        if (!StringUtils.isNullOrWhiteSpace(this.titleText)) {
            graphics.setFont(FONT);
            graphics.setColor(Color.BLACK);
            graphics.drawString(this.titleText, x + 2, y + FONT.getSize() + 2);
        }
    }
}
