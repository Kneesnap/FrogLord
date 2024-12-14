package net.highwayfrogs.editor.games.konami.greatquest.proxy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

import java.util.function.Function;

/**
 * A registry of different kcProxyDesc types.
 * Created by Kneesnap on 11/25/2024.
 */
@RequiredArgsConstructor
public enum kcProxyDescType {
    CAPSULE(kcClassID.PROXY_CAPSULE, "kcCProxyCapsuleDesc", kcProxyCapsuleDesc::new),
    TRIMESH(kcClassID.PROXY_TRI_MESH, "kcCTriMeshDesc", kcProxyTriMeshDesc::new),
    EMITTER(kcClassID.EMITTER, "kcEmitterDesc", kcEmitterDesc::new);

    @Getter private final kcClassID classId;
    @Getter private final String descriptionName;
    private final Function<kcCResourceGeneric, kcProxyDesc> descriptionCreator;

    /**
     * Create a new description instance.
     * @param genericResource the generic resource to create the description for
     * @return newInstance
     */
    public kcProxyDesc createNewInstance(kcCResourceGeneric genericResource) {
        if (genericResource == null)
            throw new NullPointerException("genericResource");

        if (this.descriptionCreator != null) {
            return this.descriptionCreator.apply(genericResource);
        } else {
            throw new RuntimeException("Cannot create new instance of " + this + ".");
        }
    }
}
