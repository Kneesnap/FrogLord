package net.highwayfrogs.editor.games.sony.shared.landscaper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.gui.texture.ITextureSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a texture which can be applied to the landscape.
 * Created by Kneesnap on 7/16/2024.
 */
public abstract class LandscapeTexture extends SCSharedGameData implements ILandscapeComponent {
    @Getter private final Landscape landscape;
    private final List<LandscapeMaterial> materials = new ArrayList<>();
    private final List<LandscapeMaterial> immutableMaterials = Collections.unmodifiableList(this.materials);
    private final List<LandscapePolygon> polygons = new ArrayList<>();
    private final List<LandscapePolygon> immutablePolygons = Collections.unmodifiableList(this.polygons);
    @Getter @Setter(AccessLevel.PACKAGE) private int textureId = -1;

    public LandscapeTexture(Landscape landscape) {
        super(landscape.getGameInstance());
        this.landscape = landscape;
    }

    @Override
    public boolean isRegistered() {
        return this.landscape != null && this.textureId >= 0;
    }

    /**
     * Get a list of all the materials which have this texture active.
     */
    public List<LandscapeMaterial> getMaterials() {
        return this.immutableMaterials;
    }

    /**
     * Get a list of all the polygons which have this texture active.
     * NOTE: Textures which have this texture through the material do not count.
     */
    public List<LandscapePolygon> getPolygons() {
        return this.immutablePolygons;
    }

    /**
     * Gets the texture source which provides the texture seen here.
     * Often times a LandscapeTexture will implement ITextureSource itself and return "this".
     */
    public abstract ITextureSource getTextureSource();

    /**
     * Get the internal material list for editing purposes.
     */
    List<LandscapeMaterial> getInternalMaterialList() {
        return this.materials;
    }

    /**
     * Get the internal polygon list for editing purposes.
     */
    List<LandscapePolygon> getInternalPolygonList() {
        return this.polygons;
    }
}