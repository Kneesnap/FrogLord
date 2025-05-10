package net.highwayfrogs.editor.games.sony.frogger.map.data.form;

import net.highwayfrogs.editor.games.generic.data.IGameObject;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;

/**
 * Represents a Frogger form definition.
 * Created by Kneesnap on 5/27/2024.
 */
public interface IFroggerFormEntry extends IGameObject {
    /**
     * Gets the name of the entity type.
     */
    String getEntityTypeName();

    /**
     * Gets the name of the form type.
     */
    String getFormTypeName();

    /**
     * Gets the form grid associated with the form book.
     */
    FroggerFormGrid getFormGrid();

    /**
     * Sets the form grid associated with the form book.
     * @param formGrid formGrid
     */
    void setFormGrid(FroggerFormGrid formGrid);

    /**
     * Gets the WAD entry for the 3D model file used to render the provided entity.
     * @param entity the provided entity
     * @return entityModel, or null if we failed to resolve it
     */
    WADEntry getEntityModelWadEntry(FroggerMapEntity entity);

    /**
     * Gets the entity model MOF file.
     * @param entity the provided entry
     * @return entityMof, or null if we failed to resolve it
     */
    default MRModel getEntityModel(FroggerMapEntity entity) {
        WADEntry entityModel = getEntityModelWadEntry(entity);
        return entityModel != null ? ((MRModel) entityModel.getFile()).getOverride() : null;
    }

    /**
     * Gets the frogger map theme.
     */
    public FroggerMapTheme getTheme();
}