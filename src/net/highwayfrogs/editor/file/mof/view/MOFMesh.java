package net.highwayfrogs.editor.file.mof.view;

import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.FrogMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntry;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.standard.SVector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a MOF Mesh.
 * Created by Kneesnap on 2/13/2019.
 */
@Getter
public class MOFMesh extends FrogMesh<MOFPolygon> {
    private MOFFile mofFile;
    private int animationId;
    private int frameCount;
    private List<SVector> verticeCache = new ArrayList<>();

    public MOFMesh(MOFFile mofFile, TextureMap map) {
        super(map, VertexFormat.POINT_TEXCOORD);
        this.mofFile = mofFile;
        updateData();
    }

    @Override
    public void onUpdatePolygonData() {
        AtomicInteger texId = new AtomicInteger();

        for (MOFPart part : getMofFile().getParts()) {
            part.getMofPolygons().values().forEach(list -> list.forEach(poly -> addPolygon(poly, texId)));
            setVerticeStart(getVerticeStart() + part.getCel(this.animationId, this.frameCount).getVertices().size());

            for (MOFPartPolyAnim partPolyAnim : part.getPartPolyAnims()) {
                MOFPolygon mofPolygon = partPolyAnim.getMofPolygon();
                if (!(mofPolygon instanceof MOFPolyTexture))
                    throw new RuntimeException("PartPolyAnim polygon was not a textured polygon! Type: " + partPolyAnim.getPrimType());

                MOFPolyTexture polyTex = (MOFPolyTexture) mofPolygon;

                int texFrame = (this.frameCount % partPolyAnim.getTotalFrames());
                List<MOFPartPolyAnimEntry> entries = partPolyAnim.getEntryList().getEntries();
                for (MOFPartPolyAnimEntry entry : entries) {
                    if (entry.getDuration() > texFrame) {
                        polyTex.setViewImageId((short) entry.getImageId());
                        break;
                    } else {
                        texFrame -= entry.getDuration();
                    }
                }
            }
        }
    }

    @Override
    public List<SVector> getVertices() {
        this.verticeCache.clear();
        for (MOFPart part : getMofFile().getParts())
            this.verticeCache.addAll(part.getCel(this.animationId, this.frameCount).getVertices());
        return this.verticeCache;
    }

    /**
     * Set the animation frame.
     * @param newFrame The frame to use.
     */
    public void setFrame(int newFrame) {
        if (newFrame < 0 || getMofFile().getParts().stream().anyMatch(part -> part.getFlipbook() == null))
            return;

        System.out.println("New Frame: " + newFrame);
        this.frameCount = newFrame;
        updateData();
    }

    /**
     * Set the animation id.
     * @param actionId The frame to use.
     */
    public void setAction(int actionId) {
        if (actionId < 0)
            return;

        for (MOFPart part : getMofFile().getParts()) // Don't go too high.
            if (part.getFlipbook() == null || part.getFlipbook().getActions().size() <= actionId)
                return;

        System.out.println("New Action: " + actionId);
        this.animationId = actionId;
        setFrame(0);
    }
}
