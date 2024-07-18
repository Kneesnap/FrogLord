package net.highwayfrogs.editor.games.sony.shared.landscaper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.system.math.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a material used on the Landscape.
 * Created by Kneesnap on 7/16/2024.
 */
public class LandscapeMaterial extends SCSharedGameData implements ILandscapeComponent {
    @Getter private final Landscape landscape;
    private final List<LandscapePolygon> polygons = new ArrayList<>();
    private final List<LandscapePolygon> immutablePolygons = Collections.unmodifiableList(this.polygons);
    @Getter @Setter(AccessLevel.PACKAGE) private int materialId = -1;
    @Getter private LandscapeTexture texture;
    @Getter private final Vector3f diffuseColor = new Vector3f(1F, 1F, 1F);
    @Getter private final Vector3f ambientColor = new Vector3f(1F, 1F, 1F);
    @Getter private final Vector3f specularColor = new Vector3f(1F, 1F, 1F);
    @Getter private float power = 1F;


    public LandscapeMaterial(Landscape landscape) {
        super(landscape.getGameInstance());
        this.landscape = landscape;
    }

    @Override
    public boolean isRegistered() {
        return this.landscape != null && this.materialId >= 0;
    }

    @Override
    public void load(DataReader reader) {
        int textureId = reader.readInt();
        setTexture(textureId >= 0 ? this.landscape.getTextures().get(textureId) : null);
        this.diffuseColor.load(reader);
        this.ambientColor.load(reader);
        this.specularColor.load(reader);
        this.power = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.texture != null ? this.texture.getTextureId() : -1);
        this.diffuseColor.save(writer);
        this.ambientColor.save(writer);
        this.specularColor.save(writer);
        writer.writeFloat(this.power);
    }

    /**
     * Apply a landscape texture to this material.
     * @param newTexture the new texture to apply, null is allowed.
     */
    public void setTexture(LandscapeTexture newTexture) {
        if (this.texture == newTexture)
            return; // No change.
        if (newTexture != null && newTexture.getLandscape() != getLandscape())
            throw new RuntimeException("The texture belongs to a different Landscape, so it cannot be applied to this material!");
        if (newTexture != null && isRegistered() && !newTexture.isRegistered())
            throw new RuntimeException("The texture is not registered to the Landscape, so this material can't use it!");

        // Remove old tracking.
        LandscapeTexture oldTexture = this.texture;
        if (oldTexture != null)
            oldTexture.getInternalMaterialList().remove(this);

        this.texture = newTexture;

        // Apply new tracking.
        if (newTexture != null && newTexture.isRegistered() && isRegistered())
            newTexture.getInternalMaterialList().add(this);
    }

    /**
     * Get a list of all the polygons which have this texture active.
     * NOTE: Textures which have this texture through the material are not included.
     * Those textures are available in the material instead.
     */
    public List<LandscapePolygon> getPolygons() {
        return this.immutablePolygons;
    }

    /**
     * Get the internal polygon list for editing purposes.
     */
    List<LandscapePolygon> getInternalPolygonList() {
        return this.polygons;
    }
}