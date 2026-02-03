package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.games.sony.shared.vlo2.ui.VloVramUIController;

import java.awt.*;

/**
 * Implements a VloVramEntry for a VloImage.
 * Created by Kneesnap on 1/16/2026.
 */
@Getter
public class VloVramEntryImage extends VloVramEntry {
    private final VloImage image;

    public VloVramEntryImage(VloImage image) {
        super(image.getUnitWidth(), image.getPaddedHeight());
        this.image = image;
    }

    @Override
    public int getX() {
        return this.image.getVramX();
    }

    @Override
    public int getY() {
        return this.image.getVramY();
    }

    @Override
    public void setPosition(int x, int y) {
        this.image.setVramX(x);
        this.image.setVramY(y);
    }

    @Override
    public void draw(Graphics2D graphics, boolean selected) {
        VloVramUIController.writeVramImage(graphics, this.image, selected);
    }
}