package net.highwayfrogs.editor.file.mof.view;

import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.FrogMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.MOFPartcel;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject.TransformData;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbook;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntry;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private List<Vector> verticeCache = new ArrayList<>();

    public MOFMesh(MOFFile mofFile, TextureMap map) {
        super(map, VertexFormat.POINT_TEXCOORD);
        this.mofFile = mofFile;

        int animCount = mofFile.getParts().stream().map(MOFPart::getFlipbook).filter(Objects::nonNull).map(MOFFlipbook::getActions).mapToInt(List::size).max().orElse(0);
        System.out.println("Animation Count: " + animCount + ", Texture Animation: " + mofFile.getParts().stream().anyMatch(part -> !part.getPartPolyAnims().isEmpty()));
        updateData();
    }

    @Override
    public void onUpdatePolygonData() {
        AtomicInteger texId = new AtomicInteger();

        for (MOFPart part : getMofFile().getParts()) {
            if (shouldSkip(part))
                continue;

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
    public List<Vector> getVertices() {
        this.verticeCache.clear();
        for (MOFPart part : getMofFile().getParts()) {
            MOFPartcel partcel = part.getCel(this.animationId, this.frameCount);
            if (shouldSkip(part))
                continue;

            if (getMofFile().getAnimation() != null) {
                TransformObject transform = getMofFile().getAnimation().getTransform(part, this.animationId, this.frameCount);

                for (SVector vertex : partcel.getVertices()) {
                    TransformData result = transform.calculatePartTransform();
                    PSXMatrix mtx = result.getTempMatrix();

                    IVector newPos = new IVector();
                    PSXMatrix.MRApplyMatrix(mtx, vertex, newPos);
                    this.verticeCache.add(newPos);
                }
            } else {
                this.verticeCache.addAll(partcel.getVertices());
            }
        }
        return this.verticeCache;
    }

    /**
     * Set the animation frame.
     * @param newFrame The frame to use.
     */
    public void setFrame(int newFrame) {
        if (newFrame < 0)
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
            if (part.getFlipbook() != null && part.getFlipbook().getActions().size() <= actionId)
                return;

        if (getMofFile().getAnimation() != null && getMofFile().getAnimation().getModelSet().getCelSet().getCels().size() <= actionId)
            return;

        System.out.println("New Action: " + actionId);
        this.animationId = actionId;
        setFrame(0);
    }

    private boolean shouldSkip(MOFPart part) { // Skip the croak for now. In the future we should make something non-hardcoded.
        return getMofFile().getFileEntry().getDisplayName().contains("GEN_FROG") && part.getPartID() == 15;
    }
}
