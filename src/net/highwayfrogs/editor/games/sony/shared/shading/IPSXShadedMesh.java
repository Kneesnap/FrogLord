package net.highwayfrogs.editor.games.sony.shared.shading;

/**
 * Represents a mesh which has toggleable PSX shading.
 * Created by Kneesnap on 4/8/2024.
 */
public interface IPSXShadedMesh {
    /**
     * Gives access to the shaded texture manager.
     */
    PSXShadedTextureManager<?> getShadedTextureManager();

    /**
     * Returns true if the mesh has shading enabled.
     */
    boolean isShadingEnabled();

    /**
     * Makes shading active or inactive.
     * @param newState the new shading state
     */
    void setShadingEnabled(boolean newState);
}