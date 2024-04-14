package net.highwayfrogs.editor.file.map.poly.polygon;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.ui.mapeditor.MapUIController;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * Flat shaded polygon.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public class MAPPolyFlat extends MAPPolygon {
    private PSXColorVector color = new PSXColorVector();

    public MAPPolyFlat(MAPPolygonType type, int verticeCount) {
        super(type, verticeCount);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.color.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        this.color.save(writer);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, MapUIController controller) {
        super.setupEditor(editor, controller);
        getColor().setupEditor(editor, "Color", null, null, true);
    }

    @Override
    public BufferedImage makeTexture(TextureMap map) {
        BufferedImage image = new BufferedImage(MAPFile.VERTEX_COLOR_IMAGE_SIZE, MAPFile.VERTEX_COLOR_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(getColor().toColor());
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        graphics.dispose();
        return image;
    }

    @Override
    public boolean isOverlay(TextureMap map) {
        return true;
    }

    @Override
    public BigInteger makeIdentifier(TextureMap map) {
        return makeIdentifier(0xF1A7C010, this.color.toRGB());
    }

    @Override
    public GameImage getGameImage(TextureMap map) {
        return null;
    }
}