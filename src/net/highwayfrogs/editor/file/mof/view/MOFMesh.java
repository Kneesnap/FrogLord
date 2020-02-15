package net.highwayfrogs.editor.file.mof.view;

import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.view.FrogMesh;
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
        super(holder.getTextureMap(), VertexFormat.POINT_TEXCOORD);
        this.mofHolder = holder;
        updateData();
    }

    @Override
    public void onUpdatePolygonData() {
        AtomicInteger texId = new AtomicInteger();

        for (MOFPart part : getMofHolder().asStaticFile().getParts()) {
            if (shouldSkip(part))
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
    }

    @Override
    public List<Vector> getVertices() {
        this.verticeCache.clear();
        for (MOFPart part : getMofHolder().asStaticFile().getParts()) {
            MOFPartcel partcel = part.getCel(getAction(), getFrame());
            if (shouldSkip(part))
                continue;

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
            return;

        this.frameCount = newFrame;
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

    private boolean shouldSkip(MOFPart part) { // Skip the croak for now. In the future we should make something non-hardcoded.
        return getMofHolder().getFileEntry().getDisplayName().contains("GEN_FROG") && part.getPartID() == 15;
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
}
