package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a MOF file.
 * Some files are off by 4-16 bytes. This is caused by us merging prim banks of matching types, meaning the header that was there before is removed.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFFile extends MOFBase {
    private byte[] bytes;
    private List<MOFPart> parts = new ArrayList<>();
    private int unknownValue;

    private static final int INCOMPLETE_TEST_ADDRESS = 0x1C;
    private static final int INCOMPLETE_TEST_VALUE = 0x40;
    public static final ImageFilterSettings MOF_EXPORT_FILTER = new ImageFilterSettings(ImageState.EXPORT)
            .setTrimEdges(true).setAllowTransparency(true).setAllowFlip(true);

    public MOFFile(MOFHolder holder) {
        super(holder);
    }

    @Override
    public void onLoad(DataReader reader) {
        int partCount = reader.readInt();

        reader.jumpTemp(INCOMPLETE_TEST_ADDRESS);
        boolean isIncomplete = (reader.readInt() == INCOMPLETE_TEST_VALUE);
        reader.jumpReturn();

        if (isIncomplete) { // Just copy the MOF directly.
            getHolder().setIncomplete(true);
            reader.jumpTemp(0);
            this.bytes = reader.readBytes(reader.getRemaining());
            reader.jumpReturn();

            String oldName = Utils.stripExtensionWin95(getHolder().getCompleteMOF().getFileEntry().getDisplayName());
            String newName = Utils.stripExtensionWin95(getFileEntry().getDisplayName());
            getConfig().getAnimationBank().linkChildBank(oldName, newName); // Link animation names.
        }

        for (int i = 0; i < partCount; i++) {
            MOFPart part = new MOFPart(this);
            part.load(reader);
            if (isIncomplete)
                getHolder().getCompleteMOF().getStaticFile().getParts().get(i).copyToIncompletePart(part);

            parts.add(part);
        }

        this.unknownValue = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        if (getHolder().isIncomplete() && this.bytes != null) { // If the MOF is incomplete, save the incomplete mof.
            writer.writeBytes(this.bytes);
            return;
        }

        super.save(writer);
    }

    @Override
    public void onSave(DataWriter writer) {
        writer.writeInt(getParts().size());
        getParts().forEach(part -> part.save(writer));
        writer.writeInt(this.unknownValue);
        getParts().forEach(part -> part.saveExtra(writer));
    }

    /**
     * Run some behavior on each mof polygon.
     * @param handler The behavior to run.
     */
    public void forEachPolygon(Consumer<MOFPolygon> handler) {
        getParts().forEach(part -> part.getMofPolygons().values().forEach(list -> list.forEach(handler)));
    }

    /**
     * Create a map of textures which were generated
     * @return texMap
     */
    public Map<VertexColor, BufferedImage> makeVertexColorTextures() {
        Map<VertexColor, BufferedImage> texMap = new HashMap<>();

        forEachPolygon(prim -> {
            if (!(prim instanceof VertexColor))
                return;

            VertexColor vertexColor = (VertexColor) prim;
            BufferedImage image = vertexColor.makeTexture();
            texMap.put(vertexColor, image);
        });

        return texMap;
    }

    /**
     * Check if this file has any texture animations.
     * @return hasTextureAnimation
     */
    public boolean hasTextureAnimation() {
        for (MOFPart part : getParts())
            if (!part.getPartPolyAnims().isEmpty())
                return true;
        return false;
    }

    /**
     * Gets the number of hilites this model has.
     * @return hiliteCount
     */
    public int getHiliteCount() {
        int totalHilites = 0;
        for (int i = 0; i < getParts().size(); i++)
            totalHilites += getParts().get(i).getHilites().size();
        return totalHilites;
    }

    /**
     * Gets the number of collprims in this model.
     * @return collprimCount
     */
    public int getCollprimCount() {
        int totalCollprim = 0;
        for (int i = 0; i < getParts().size(); i++)
            if (getParts().get(i).getCollprim() != null)
                totalCollprim++;
        return totalCollprim;
    }
}