package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.image.ImageView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.GeometryManager;
import net.highwayfrogs.editor.system.TexturedPoly;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents PSX polygons with a texture.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public class MAPPolyTexture extends MAPPolygon implements TexturedPoly {
    private short flags;
    private ByteUV[] uvs;
    private short textureId;
    private PSXColorVector[] vectors;

    private static final ImageFilterSettings SHOW_SETTINGS = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(true).setAllowFlip(true);
    private static final int SHOW_SIZE = 150;

    public MAPPolyTexture(MAPPolygonType type, int verticeCount, int colorCount) {
        super(type, verticeCount);
        this.uvs = new ByteUV[verticeCount];
        this.vectors = new PSXColorVector[colorCount];
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        this.flags = reader.readShort();
        reader.skipShort(); // Padding

        loadUV(0, reader);
        reader.skipShort(); // Runtime clut-id.
        loadUV(1, reader);
        this.textureId = reader.readShort();

        for (int i = 2; i < this.uvs.length; i++)
            loadUV(i, reader);

        if (this.uvs.length == MAPPolygon.TRI_SIZE)
            reader.skipShort(); // Padding.

        for (int i = 0; i < this.vectors.length; i++) {
            PSXColorVector vector = new PSXColorVector();
            vector.load(reader);
            this.vectors[i] = vector;
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);

        writer.writeShort(this.flags);
        writer.writeNull(Constants.SHORT_SIZE);
        this.uvs[0].save(writer);
        writer.writeShort((short) 0); // Runtime value.
        this.uvs[1].save(writer);
        writer.writeShort(this.textureId);

        for (int i = 2; i < this.uvs.length; i++)
            this.uvs[i].save(writer);

        if (this.uvs.length == 3)
            writer.writeNull(Constants.SHORT_SIZE);

        for (PSXColorVector colorVector : this.vectors)
            colorVector.save(writer);
    }

    private void loadUV(int id, DataReader reader) {
        this.uvs[id] = new ByteUV();
        this.uvs[id].load(reader);
    }

    /**
     * Get the nth obj UV string.
     * @param index The index to get.
     * @return objUvString
     */
    public String getObjUVString(int index) {
        boolean shouldSwap = (this.uvs.length == MAPPolygon.QUAD_SIZE);

        if (shouldSwap) { // I believe we have to swap it due to the .obj format.
            ByteUV temp = this.uvs[2];
            this.uvs[2] = this.uvs[3];
            this.uvs[3] = temp;
        }

        String uvString = this.uvs[index].toObjTextureString();

        if (shouldSwap) {
            ByteUV temp = this.uvs[2];
            this.uvs[2] = this.uvs[3];
            this.uvs[3] = temp;
        }

        return uvString;
    }

    @Override
    public int getOrderId() {
        return getTextureId();
    }

    @Override
    public TextureEntry getEntry(TextureMap map) {
        return map.getEntry(getTextureId());
    }

    @Override
    public void setupEditor(GeometryManager manager, GUIEditorGrid editor) {
        super.setupEditor(manager, editor);

        TextureMap texMap = manager.getMesh().getTextureMap();
        VLOArchive suppliedVLO = manager.getMap().getVlo();
        GameImage image = suppliedVLO.getImageByTextureId(texMap.getRemap(getTextureId()));

        ImageView view = editor.addCenteredImage(image.toFXImage(SHOW_SETTINGS), 150);
        view.setOnMouseClicked(evt -> suppliedVLO.promptImageSelection(newImage -> {
            short newValue = newImage.getTextureId();
            if (texMap.getRemapList() != null)
                newValue = (short) texMap.getRemapList().indexOf(newValue);

            if (newValue == (short) -1) {
                System.out.println("This image is not part of the remap! It can't be used!"); // Show this as a popup maybe.
                return;
            }

            this.textureId = newValue;
            view.setImage(newImage.toFXImage(SHOW_SETTINGS));
            manager.getController().getGeometryManager().refreshView();
        }, false));

        for (PolyTextureFlag flag : PolyTextureFlag.values())
            editor.addCheckBox(Utils.capitalize(flag.name()), testFlag(flag), newState -> setFlag(flag, newState));

        int id = 0;
        for (PSXColorVector colorVec : getVectors())
            editor.addColorPicker("Color #" + (++id), colorVec.toRGB(), colorVec::fromRGB);

        id = 0;
        for (ByteUV byteUV : getUvs())
            byteUV.setupEditor("UV #" + (++id), editor);
    }

    /**
     * Test if a flag is present.
     * @param flag The flag in question.
     * @return flagPresent
     */
    public boolean testFlag(PolyTextureFlag flag) {
        return (this.flags & flag.getFlag()) == flag.getFlag();
    }

    /**
     * Set a flag state.
     * @param flag     The flag to set.
     * @param newState The new flag state.
     */
    public void setFlag(PolyTextureFlag flag, boolean newState) {
        boolean currentState = testFlag(flag);
        if (currentState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.flags |= flag.getFlag();
        } else {
            this.flags ^= flag.getFlag();
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PolyTextureFlag {
        SEMI_TRANSPARENT(Constants.BIT_FLAG_0), // setSemiTrans(true)
        ENVIRONMENT_IMAGE(Constants.BIT_FLAG_1), // Show the solid environment bitmap. (For instance, how water appears as a solid body, or sludge in the sewer levels.)
        MAX_ORDER_TABLE(Constants.BIT_FLAG_2); // Puts at the back of the order table. Either the very lowest rendering priority, or the very highest.

        private final int flag;
    }
}
