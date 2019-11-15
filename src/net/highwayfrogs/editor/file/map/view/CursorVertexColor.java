package net.highwayfrogs.editor.file.map.view;

import lombok.Getter;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * The texture for the cursor.
 * Created by Kneesnap on 12/4/2018.
 */
@Getter
public class CursorVertexColor implements VertexColor {
    private Color bodyColor;
    private Color outlineColor;

    public CursorVertexColor(Color bodyColor, Color outlineColor) {
        this.bodyColor = bodyColor;
        this.outlineColor = outlineColor;
    }

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        graphics.setColor(getBodyColor());
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        graphics.setColor(getOutlineColor());
        graphics.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);
    }
}
