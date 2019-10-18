package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.PickupData;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry.FormLibFlag;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.entity.Entity.EntityFlag;
import net.highwayfrogs.editor.file.map.entity.FlyScoreType;
import net.highwayfrogs.editor.file.map.entity.data.cave.EntityFatFireFly;
import net.highwayfrogs.editor.file.map.entity.data.general.BonusFlyEntity;
import net.highwayfrogs.editor.file.map.entity.script.ScriptButterflyData;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.AbstractGUIEditorGrid;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages map entities.
 * Created by Kneesnap on 8/19/2019.
 */
public class EntityManager extends MapManager {
    private GUIEditorGrid entityEditor;
    private List<MeshView> entityModelViews = new ArrayList<>();
    private List<FormEntry> entityTypes = new ArrayList<>();
    private Set<Integer> entitiesToUpdate = new HashSet<>();

    private static final Image ENTITY_ICON_IMAGE = GameFile.loadIcon("entity");
    private static final PhongMaterial MATERIAL_ENTITY_ICON = Utils.makeSpecialMaterial(ENTITY_ICON_IMAGE);

    public EntityManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        getRenderManager().addMissingDisplayList("entityModelViews");
        updateEntities();
        MapUIController.getPropertyEntityIconSize().addListener((observable, old, newVal) -> updateEntities());
    }

    @Override
    public void setupEditor() {
        showEntityInfo(null);
    }

    /**
     * Gets the entities this will work with.
     * @return entityList
     */
    public List<Entity> getEntities() {
        return getMap().getEntities();
    }

    /**
     * Show entity information.
     * @param entity The entity to show information for.
     */
    public void showEntityInfo(Entity entity) {
        FormEntry[] entries = getMap().getConfig().getAllowedForms(getMap().getTheme());
        if (entity != null && !Utils.contains(entries, entity.getFormEntry())) // This wasn't found in this
            entries = getMap().getConfig().getFullFormBook().toArray(new FormEntry[0]);

        // Setup Editor:
        if (this.entityEditor == null)
            this.entityEditor = new AbstractGUIEditorGrid(getController().getEntityGridPane(), this::updateEntities);

        entityEditor.clearEditor();
        if (entity == null) {
            entityEditor.addBoldLabel("There is no entity selected.");
            entityEditor.addButtonWithEnumSelection("Add Entity", this::addNewEntity, entries, entries[0])
                    .setConverter(new AbstractStringConverter<>(FormEntry::getFormName));
            return;
        }

        this.entityEditor.addButtonWithEnumSelection("Add Entity", this::addNewEntity, entries, entity.getFormEntry())
                .setConverter(new AbstractStringConverter<>(FormEntry::getFormName));

        entityEditor.addBoldLabel("General Information:");
        entityEditor.addLabel("Entity Type", entity.getFormEntry().getEntityName());

        entityEditor.addEnumSelector("Form Type", entity.getFormEntry(), entries, false, newEntry -> {
            entity.setFormEntry(newEntry);
            showEntityInfo(entity);
            updateEntities();
        }).setConverter(new AbstractStringConverter<>(FormEntry::getFormName));

        entityEditor.addIntegerField("Entity ID", entity.getUniqueId(), entity::setUniqueId, null);

        if (entity.getFormGridId() >= 0 && getMap().getForms().size() > entity.getFormGridId()) {
            entityEditor.addSelectionBox("Form", getMap().getForms().get(entity.getFormGridId()), getMap().getForms(),
                    newForm -> entity.setFormGridId(getMap().getForms().indexOf(newForm)))
                    .setConverter(new AbstractIndexStringConverter<>(getMap().getForms(), (index, form) -> "Form #" + index + " (" + form.getXGridSquareCount() + "," + form.getZGridSquareCount() + ")"));
        } else { // This form is invalid, so show this as a text box.
            entityEditor.addIntegerField("Form ID", entity.getFormGridId(), entity::setFormGridId, null);
        }

        entityEditor.addBoldLabel("Flags:");
        for (EntityFlag flag : EntityFlag.values())
            entityEditor.addCheckBox(Utils.capitalize(flag.name()), entity.testFlag(flag), newState -> entity.setFlag(flag, newState));

        // Populate Entity Data.
        if (entity.getEntityData() != null) {
            this.entityEditor.addSeparator(25);
            entityEditor.addBoldLabel("Entity Data:");
            entity.getEntityData().addData(this, this.entityEditor);
        }

        // Populate Script Data.
        if (entity.getScriptData() != null) {
            this.entityEditor.addSeparator(25);
            this.entityEditor.addBoldLabel("Script Data:");
            entity.getScriptData().addData(this.entityEditor);
        }

        this.entityEditor.addSeparator(25);
        this.entityEditor.addButton("Remove Entity", () -> {
            getMap().getEntities().remove(entity);
            updateEntities();
            showEntityInfo(null); // Don't show the entity we just deleted.
        });

        getController().getEntityPane().setExpanded(true);
    }

    private void addNewEntity(FormEntry entry) {
        Entity newEntity = new Entity(getMap(), entry);

        if (newEntity.getMatrixInfo() != null) { // Lets you select a polygon to place the new entity on.
            for (GridStack stack : getMap().getGridStacks())
                for (GridSquare square : stack.getGridSquares())
                    getController().renderOverPolygon(square.getPolygon(), MapMesh.GENERAL_SELECTION);
            MeshData data = getMesh().getManager().addMesh();

            getController().getGeometryManager().selectPolygon(poly -> {
                getMesh().getManager().removeMesh(data);

                // Set entity position to the clicked polygon.
                PSXMatrix matrix = newEntity.getMatrixInfo();
                SVector pos = MAPPolygon.getCenterOfPolygon(getMesh(), poly);
                matrix.getTransform()[0] = Utils.floatToFixedPointInt20Bit(pos.getFloatX());
                matrix.getTransform()[1] = Utils.floatToFixedPointInt20Bit(pos.getFloatY());
                matrix.getTransform()[2] = Utils.floatToFixedPointInt20Bit(pos.getFloatZ());

                // Add entity.
                addEntityToMap(newEntity);
            }, () -> getMesh().getManager().removeMesh(data));
            return;
        }

        if (newEntity.getPathInfo() != null) {
            if (getMap().getPaths().isEmpty()) {
                Utils.makePopUp("Path entities cannot be added if there are no paths present! Add a path.", AlertType.WARNING);
                return;
            }

            // User selects the path.
            getController().getPathManager().promptPath((path, segment, segDistance) -> {
                newEntity.getPathInfo().setPath(getMap(), path, segment);
                newEntity.getPathInfo().setSegmentDistance(segDistance);
                newEntity.getPathInfo().setSpeed(10); // Default speed.
                addEntityToMap(newEntity);
            }, null);
            return;
        }

        addEntityToMap(newEntity);
    }

    private void addEntityToMap(Entity entity) {
        if (entity.getUniqueId() == -1) { // Default entity id, update it to something new.
            boolean isPath = entity.getMatrixInfo() != null;

            // Use the largest entity id + 1.
            for (Entity tempEntity : getMap().getEntities())
                if (tempEntity.getUniqueId() >= entity.getUniqueId() && (isPath == (tempEntity.getMatrixInfo() != null)))
                    entity.setUniqueId(tempEntity.getUniqueId() + 1);
        }

        if (entity.getFormGridId() == -1) { // Default form id, make it something.
            int[] formCounts = new int[getMap().getForms().size()];
            for (Entity testEntity : getMap().getEntities())
                if (testEntity.getFormEntry() == entity.getFormEntry())
                    formCounts[testEntity.getFormGridId()]++;

            int maxCount = -1;
            for (int i = 0; i < formCounts.length; i++) {
                if (formCounts[i] > maxCount) {
                    maxCount = formCounts[i];
                    entity.setFormGridId(i);
                }
            }
        }

        //TODO: New matrix entities don't show up in-game, but path entities work fine.
        getMap().getEntities().add(entity);
        showEntityInfo(entity);
        updateEntities();
    }

    /**
     * Updates displayed entities.
     */
    public void updateEntities() {
        List<Entity> entities = getEntities();

        // Add new entity models.
        while (entities.size() > entityModelViews.size()) {
            this.entityTypes.add(null);

            MeshView newView = new MeshView();
            newView.setCullFace(CullFace.NONE);
            newView.setDrawMode(DrawMode.FILL);
            this.entityModelViews.add(newView);
            getRenderManager().addNode("entityModelViews", newView);

            newView.setOnMouseClicked(evt -> { // Handle being clicked.
                MeshView safeView = (MeshView) evt.getSource();
                showEntityInfo(getEntities().get(this.entityModelViews.indexOf(safeView)));
            });
        }

        // Update entity models.
        for (int i = 0; i < getEntities().size(); i++)
            updateEntityMesh(i);

        // Update entity positions.
        float[] pos = new float[6];
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            entity.getPosition(pos, getMap());

            float yaw = pos[3];
            float pitch = pos[4];
            float roll = pos[5];
            MeshView view = this.entityModelViews.get(i);
            boolean hasModel = !entity.getFormEntry().testFlag(FormLibFlag.NO_MODEL);
            if (hasModel) {
                int foundRotations = 0;
                for (Transform transform : view.getTransforms()) { // Update existing rotations.
                    if (!(transform instanceof Rotate))
                        continue;

                    foundRotations++;
                    double value = pos[2 + foundRotations];

                    ((Rotate) transform).setAngle(Math.toDegrees(value));
                    if (foundRotations == 3)
                        break;
                }

                if (foundRotations == 0) { // There are no rotations, so add rotations.
                    view.getTransforms().add(new Rotate(Math.toDegrees(yaw), Rotate.X_AXIS));
                    view.getTransforms().add(new Rotate(Math.toDegrees(pitch), Rotate.Y_AXIS));
                    view.getTransforms().add(new Rotate(Math.toDegrees(roll), Rotate.Z_AXIS));
                }
            }

            view.setTranslateX(pos[0]);
            view.setTranslateY(pos[1]);
            view.setTranslateZ(pos[2]);
        }


        // Update visibility.
        for (int i = 0; i < this.entityModelViews.size(); i++)
            this.entityModelViews.get(i).setVisible(entities.size() > i); // Update visibility.

        entitiesToUpdate.clear();
    }

    /**
     * Updates the mesh of a given entity.
     * @param entity The entity to update.
     */
    public void updateEntity(Entity entity) {
        this.entitiesToUpdate.add(getEntities().indexOf(entity));
        updateEntities();
    }

    private void updateEntityMesh(int entityIndex) {
        Entity entity = getEntities().get(entityIndex);
        MeshView entityMesh = this.entityModelViews.get(entityIndex);
        FormEntry oldForm = this.entityTypes.get(entityIndex);
        FormEntry newForm = entity.getFormEntry();

        if (oldForm == newForm && !entitiesToUpdate.contains(entityIndex))
            return; // The entity form has not changed, so we shouldn't change the model.

        this.entityTypes.set(entityIndex, newForm);
        boolean hasModel = !newForm.testFlag(FormLibFlag.NO_MODEL);

        if (hasModel) {
            boolean isGeneralTheme = newForm.getTheme() == MAPTheme.GENERAL;
            ThemeBook themeBook = getMap().getConfig().getThemeBook(newForm.getTheme());

            WADFile wadFile = null;
            if (isGeneralTheme) {
                wadFile = themeBook.getWAD(getMap());
            } else {
                MapBook mapBook = getMap().getFileEntry().getMapBook();
                if (mapBook != null)
                    wadFile = mapBook.getWad(getMap());
            }

            int wadIndex = newForm.getWadIndex();
            if (wadFile != null && wadFile.getFiles().size() > wadIndex && wadIndex >= 0) { // Test if there's an associated WAD.
                WADEntry wadEntry = wadFile.getFiles().get(wadIndex);

                if (!wadEntry.isDummy() && wadEntry.getFile() instanceof MOFHolder) {
                    MOFHolder holder = (MOFHolder) wadEntry.getFile();

                    // Setup VLO.
                    VLOArchive vlo = getMap().getConfig().getForcedVLO(wadEntry.getDisplayName());
                    if (vlo == null)
                        vlo = themeBook.getVLO(getMap());
                    holder.setVloFile(vlo);

                    // Update MeshView.
                    entityMesh.setMesh(holder.getMofMesh());
                    entityMesh.setMaterial(holder.getTextureMap().getPhongMaterial());
                    return;
                }
            }
        }

        // Couldn't find a model to use, so instead we'll display as a 2D sprite.
        float entityIconSize = MapUIController.getPropertyEntityIconSize().getValue();

        // Attempt to apply 2d textures, instead of the default texture.
        PhongMaterial material = MATERIAL_ENTITY_ICON;
        FroggerEXEInfo config = getMap().getConfig();
        if (config.getPickupData() != null) {
            FlyScoreType flyType = null;
            if (entity.getEntityData() instanceof BonusFlyEntity)
                flyType = ((BonusFlyEntity) entity.getEntityData()).getType();
            if (entity.getScriptData() instanceof ScriptButterflyData)
                flyType = ((ScriptButterflyData) entity.getScriptData()).getType();
            if (entity.getEntityData() instanceof EntityFatFireFly)
                flyType = ((EntityFatFireFly) entity.getEntityData()).getType();

            if (flyType != null) {
                PickupData pickupData = config.getPickupData()[flyType.ordinal()];
                GameImage flyImage = config.getImageFromPointer(pickupData.getImagePointers().get(0));
                if (flyImage != null) { // This can be null in the EU PS1 demo. (It may not have properly been setup when compiled.)
                    material = Utils.makeSpecialMaterial(flyImage.toFXImage());
                    entityIconSize /= 2;
                }
            }
        }

        // NOTE: Maybe this could be a single tri mesh, local to this manager, and we just update its points in updateEntities().
        TriangleMesh triMesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        triMesh.getPoints().addAll(-entityIconSize * 0.5f, entityIconSize * 0.5f, 0, -entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, entityIconSize * 0.5f, 0);
        triMesh.getTexCoords().addAll(0, 1, 0, 0, 1, 0, 1, 1);
        triMesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 0, 0);

        // Update mesh.
        entityMesh.setMesh(triMesh);
        entityMesh.setMaterial(material);
    }
}
