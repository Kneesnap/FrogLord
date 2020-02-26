package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Represents polygons with gouraud shading.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public class MAPPolyGouraud extends MAPPolygon implements VertexColor {
    private PSXColorVector[] colors;

    // NOTE: Changing MAPFile.VERTEX_COLOR_IMAGE_SIZE to a higher value will improve the shading quality, but may
    //       also break texture related stuff elsewhere (I haven't done much in the way of testing).
    //       Try changing the default value from 8 to 32 for example.
    //       I don't like the fact we are relying on texture generation and resolution to shade the polygons. We should
    //       really just be setting vertex color values and letting the hardware do the shading work. This just seems
    //       very, very wrong - but I don't know how we can get around it right now due to the crappy limitations of
    //       JavaFx. It's a problem for sure.
    private static final int FULL_SIZE = MAPFile.VERTEX_COLOR_IMAGE_SIZE;

    public MAPPolyGouraud(MAPPolygonType type, int verticeCount) {
        super(type, verticeCount);
        this.colors = new PSXColorVector[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Read colors
        for (int i = 0; i < this.colors.length; i++) {
            PSXColorVector vector = new PSXColorVector();
            vector.load(reader);
            this.colors[i] = vector;
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (PSXColorVector vector : colors)
            vector.save(writer);
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
        final Color c0 = Utils.fromRGB(this.colors[0].toRGB());
        final Color c1 = Utils.fromRGB(this.colors[1].toRGB());
        final Color c2 = Utils.fromRGB(this.colors[2].toRGB());
        final Color c3 = (this.colors.length > 3 ? Utils.fromRGB(this.colors[3].toRGB()) : c2);

        float tx, ty;

        for (int x = 0; x < FULL_SIZE; x++) {
            tx = (x == FULL_SIZE - 1) ? 1F : (float) x / (float) FULL_SIZE;
            for (int y = 0; y < FULL_SIZE; y++) {
                ty = (y == FULL_SIZE - 1) ? 1F : (float) y / (float) FULL_SIZE;
                final Color newColor = Utils.calculateBilinearInterpolatedColour(c0, c2, c1, c3, tx, ty);
                graphics.setColor(Utils.toAWTColor(newColor));
                graphics.fillRect(x, y, x + 1, y + 1);
            }
        }
    }

    protected void addColorEditor(GUIEditorGrid editor) {
        // Color Editor.
        if (getColors() != null) {
            editor.addBoldLabel("Colors:");
            String[] nameArray = COLOR_BANK[getColors().length - 1];
            for (int i = 0; i < getColors().length; i++)
                editor.addColorPicker(nameArray[i], getColors()[i].toRGB(), getColors()[i]::fromRGB);
            //TODO: Update map display when color is updated. (Update texture map.)
        }
    }
}
