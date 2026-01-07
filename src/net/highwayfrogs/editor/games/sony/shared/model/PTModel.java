package net.highwayfrogs.editor.games.sony.shared.model;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCGameObject.SCSharedGameObject;
import net.highwayfrogs.editor.games.sony.shared.collprim.PTCollprim;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTActionSetFile;
import net.highwayfrogs.editor.games.sony.shared.model.skeleton.PTSkeletonFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPart;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPartCel;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a unified 3D model, containing a static mesh, and optionally, supplemental animation files.
 * Seems to be called 'PT_MODEL' in the game by looking at the game binaries.
 * Created by Kneesnap on 5/21/2024.
 */
public class PTModel extends SCSharedGameObject {
    @Getter private final PTStaticFile staticMeshFile;
    @Getter private final PTSkeletonFile skeletonFile;
    @Getter private final PTActionSetFile actionSetFile;
    @Getter private final List<PTTransformInstanceData> transformData;
    @Getter private final List<PTPartInstanceData> partData;
    @Getter private final PSXMatrix matrix = new PSXMatrix();

    public PTModel(PTStaticFile staticMeshFile, PTSkeletonFile skeletonFile, PTActionSetFile actionSetFile) {
        super(staticMeshFile.getGameInstance());
        this.staticMeshFile = staticMeshFile;
        this.skeletonFile = skeletonFile;
        this.actionSetFile = actionSetFile;

        // Create transform data.
        this.transformData = new ArrayList<>();
        for (int i = 0; i < getTransformCount(); i++)
            this.transformData.add(new PTTransformInstanceData(this, i));

        // Create part data.
        this.partData = new ArrayList<>();
        for (int i = 0; i < staticMeshFile.getParts().size(); i++)
            this.partData.add(new PTPartInstanceData(staticMeshFile.getParts().get(i)));
    }

    /**
     * Gets the name of the model.
     */
    public String getName() {
        String name = null;
        if (this.staticMeshFile != null)
            name = FileUtils.stripExtension(this.staticMeshFile.getFileDisplayName());
        if (this.actionSetFile != null && StringUtils.isNullOrWhiteSpace(name))
            name = FileUtils.stripExtension(this.actionSetFile.getFileDisplayName());
        if (this.skeletonFile != null && StringUtils.isNullOrWhiteSpace(name))
            name = FileUtils.stripExtension(this.skeletonFile.getFileDisplayName());

        return name;
    }

    /**
     * Gets the number of static mesh parts used in the model.
     */
    public int getPartCount() {
        return this.staticMeshFile.getParts().size();
    }

    /**
     * Gets the collprim of the static mesh.
     */
    public PTCollprim getStaticCollprim() {
        return this.staticMeshFile.getCollprim();
    }

    /**
     * Get the number of transforms.
     */
    public int getTransformCount() {
        return this.skeletonFile != null ? this.skeletonFile.getBones().size() : this.staticMeshFile.getParts().size();
    }


    /**
     * Update the animation.
     */
    public void updateAnimation() {
        if (this.actionSetFile == null)
            return; // If there's no animation file, we can't update.

        // TODO: Support animations.
    }

    /**
     * Update the static model.
     */
    public void updateStaticModel() {
        int transformCount = getTransformCount();
        for (int i = 0; i < transformCount; i++)
            this.transformData.get(i).update();
    }

    /**
     * Update mime and skin vector data.
     * TODO: Support interpolation.
     * TODO: Properly get skeletal meshes working.
     */
    public void updateMimeAndSkin() {
        for (int i = 0; i < getPartCount(); i++) {
            PTStaticPart part = this.staticMeshFile.getParts().get(i);
            PTPartInstanceData partData = this.partData.get(i);
            PTStaticPartCel partCel = part.getPartCels().get(partData.getCurrentPartCel());

            // MIME
            if ((part.getFlags() & PTStaticPart.FLAG_MIME_ENABLED) == PTStaticPart.FLAG_MIME_ENABLED) {
                if ((partData.getLastInterpolationProgress() != partData.getInterpolationProgress()) && (partData.getInterpolationProgress() != 0)) {
                    // PTStaticPartCel endPartCel = part.getPartCels().get(partData.getTargetPartCel());
                    for (int j = 0; j < part.getMimeVectors(); j++)
                        partData.getVectorBlock()[j] = partCel.getMimeVectors().get(j);

                    partData.setLastInterpolationProgress(partData.getInterpolationProgress());
                }
            }

            // Skin
            if (partCel.getSkinVectorCount() != 0) {
                // Use this for calculations.
                //PTTransformInstanceData transformData = this.transformData.get(part.getTransformId());
                //PTTransformInstanceData attachedTransformData = this.transformData.get(part.getConnectedTransformId());
                for (int j = 0; j < partCel.getSkinVectorCount(); j++)
                    partData.getVectorBlock()[j] = partCel.getVectors().get(j);
            }
        }
    }
}