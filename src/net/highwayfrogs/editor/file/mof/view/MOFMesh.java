package net.highwayfrogs.editor.file.mof.view;

import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.FrogMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureSource;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.MOFPartcel;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a MOF Mesh.
 * Created by Kneesnap on 2/13/2019.
 */
@Getter
public class MOFMesh extends FrogMesh<MOFPolygon> {
    private MOFHolder mofHolder;
    private int animationId;
    private int frameCount;
    private List<Vector> verticeCache = new ArrayList<>();
    @Setter private boolean showOverlay;

    public MOFMesh(MOFHolder holder) {
        super(holder.makeTextureMap(), VertexFormat.POINT_TEXCOORD);
        this.mofHolder = holder;
        updateData();
    }

    @Override
    public void onUpdatePolygonData() {
        AtomicInteger texId = new AtomicInteger();

        for (MOFPart part : getMofHolder().asStaticFile().getParts()) {
            if (part.shouldHide())
                continue;

            part.getMofPolygons().values().forEach(list -> list.forEach(poly -> addPolygon(poly, texId)));
            setVerticeStart(getVerticeStart() + part.getCel(getAction(), getFrame()).getVertices().size());

            for (MOFPartPolyAnim partPolyAnim : part.getPartPolyAnims()) {
                MOFPolygon mofPolygon = partPolyAnim.getMofPolygon();
                if (!(mofPolygon instanceof MOFPolyTexture))
                    throw new RuntimeException("PartPolyAnim polygon was not a textured polygon! Type: " + partPolyAnim.getPrimType());

                MOFPolyTexture polyTex = (MOFPolyTexture) mofPolygon;

                int texFrame = (this.frameCount % partPolyAnim.getTotalFrames()); // Don't use getFrame() here, it prevents texture animation from showing if the flipbook or XAR animation isn't showing.
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

        // Setup the mesh for all of the polygons that have been drawn..
        for (MOFPolygon poly : getMofHolder().asStaticFile().getAllPolygons())
            poly.onMeshSetup(this);
    }

    @Override
    public List<Vector> getVertices() {
        this.verticeCache.clear();
        for (MOFPart part : getMofHolder().asStaticFile().getParts()) {
            if (part.shouldHide())
                continue;

            MOFPartcel partcel = hasEnabledAnimation() ? part.getCel(getAction(), getFrame()) : part.getStaticPartcel();
            if (getMofHolder().isAnimatedMOF() && hasEnabledAnimation()) {
                TransformObject transform = getMofHolder().getAnimatedFile().getTransform(part, getAction(), getFrame());
                for (SVector vertex : partcel.getVertices())
                    this.verticeCache.add(PSXMatrix.MRApplyMatrix(transform.calculatePartTransform(), vertex, new IVector()));
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
            newFrame = getMofHolder().getFrameCount(this.animationId) + newFrame;

        this.frameCount = (newFrame % getMofHolder().getFrameCount(this.animationId));
        updateData();
    }

    /**
     * Advance to the next frame.
     */
    public void nextFrame() {
        setFrame(this.frameCount + 1);
    }

    /**
     * Advance to the next frame.
     */
    public void previousFrame() {
        setFrame(this.frameCount - 1);
    }

    /**
     * Update this frame.
     */
    public void updateFrame() {
        setFrame(this.frameCount);
    }

    /**
     * Resets the current frame to the starting frame.
     */
    public void resetFrame() {
        setFrame(0);
    }

    /**
     * Set the animation id.
     * @param actionId The frame to use.
     */
    public void setAction(int actionId) {
        if (actionId < -1)
            return;

        for (MOFPart part : getMofHolder().asStaticFile().getParts()) // Don't go too high.
            if (part.getFlipbook() != null && part.getFlipbook().getActions().size() <= actionId)
                return;

        if (actionId >= getMofHolder().getMaxAnimation())
            return;

        this.animationId = actionId;
        resetFrame();
    }

    /**
     * Test if there is an active animation right now.
     * @return hasAnimation
     */
    public boolean hasEnabledAnimation() {
        return this.animationId != -1;
    }

    /**
     * Gets the frame if there is an animation enabled.
     */
    public int getFrame() {
        return hasEnabledAnimation() ? this.frameCount : 0;
    }

    /**
     * Gets the animation action.
     */
    public int getAction() {
        return Math.max(0, this.animationId);
    }

    /**
     * Render over an existing polygon.
     * @param targetPoly The polygon to render over.
     * @param source     The source to render.
     */
    public void renderOverPolygon(MOFPolygon targetPoly, TextureSource source) {
        setVerticeStart(0);
        int increment = getVertexFormat().getVertexIndexSize();
        boolean isQuad = (targetPoly.getVerticeCount() == MAPPolygon.QUAD_SIZE);

        int face = getPolyFaceMap().get(targetPoly) * getFaceElementSize();
        int v1 = getFaces().get(face);
        int v2 = getFaces().get(face + increment);
        int v3 = getFaces().get(face + (2 * increment));

        TextureTreeNode node = source.getTreeNode(getTextureMap());
        if (isQuad) {
            int v4 = getFaces().get(face + (3 * increment));
            int v5 = getFaces().get(face + (4 * increment));
            int v6 = getFaces().get(face + (5 * increment));
            addRectangle(node, v1, v2, v3, v4, v5, v6);
        } else {
            addTriangle(node, v1, v2, v3);
        }
    }
}
