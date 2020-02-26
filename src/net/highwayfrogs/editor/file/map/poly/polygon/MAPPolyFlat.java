package net.highwayfrogs.editor.file.map.poly.polygon;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Flat shaded polygon.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public class MAPPolyFlat extends MAPPolygon implements VertexColor {
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
        addColorEditor(editor);
    }

    @Override
    public TextureTreeNode getNode(TextureMap map) {
        return getTreeNode(map);
    }

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        graphics.setColor(getColor().toColor());
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    protected void addColorEditor(GUIEditorGrid editor) {
        // Color Editor.
        if (getColor() != null) {
            editor.addBoldLabel("Color:");
            editor.addColorPicker(SINGLE_COLOR_NAME[0], getColor().toRGB(), getColor()::fromRGB);
            //TODO: Update map display when color is updated. (Update texture map.)
        }
    }
}
