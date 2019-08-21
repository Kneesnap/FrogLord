package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
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

/**
 * Manages map entities.
 * Created by Kneesnap on 8/19/2019.
 */
public class EntityManager extends MapManager {
    private GUIEditorGrid entityEditor;

    private static final Image ENTITY_ICON_IMAGE = GameFile.loadIcon("entity");
    private static final PhongMaterial MATERIAL_ENTITY_ICON = Utils.makeSpecialMaterial(ENTITY_ICON_IMAGE);
    private static final String ENTITY_LIST = "entityList";

    public EntityManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        updateEntities();
        MapUIController.getPropertyEntityIconSize().addListener((observable, old, newVal) -> updateEntities());
    }

    @Override
    public void setupEditor() {
        showEntityInfo(null); //TODO: Figure out if this is actually how we want to handle it.
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
                    getController().getController().renderOverPolygon(square.getPolygon(), MapMesh.GENERAL_SELECTION);
            MeshData data = getMesh().getManager().addMesh();

            getController().selectPolygon(poly -> {
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
     * Update entity display.
     */
    public void updateEntities() {
        getRenderManager().addMissingDisplayList(ENTITY_LIST);
        getRenderManager().clearDisplayList(ENTITY_LIST);

        float[] pos = new float[6];
        for (Entity entity : getMap().getEntities()) {
            entity.getPosition(pos, getMap());
            MeshView meshView = makeEntityIcon(entity, pos[0], pos[1], pos[2], pos[3], pos[4], pos[5]);
            meshView.setOnMouseClicked(evt -> showEntityInfo(entity));
        }
    }

    private MeshView makeEntityIcon(Entity entity, float x, float y, float z, float yaw, float pitch, float roll) {
        float entityIconSize = MapUIController.getPropertyEntityIconSize().getValue();

        FormEntry form = entity.getFormEntry();

        if (!form.testFlag(FormLibFlag.NO_MODEL)) {
            boolean isGeneralTheme = form.getTheme() == MAPTheme.GENERAL;
            ThemeBook themeBook = getMap().getConfig().getThemeBook(form.getTheme());

            WADFile wadFile = null;
            if (isGeneralTheme) {
                wadFile = themeBook.getWAD(getMap());
            } else {
                MapBook mapBook = getMap().getFileEntry().getMapBook();
                if (mapBook != null)
                    wadFile = mapBook.getWad(getMap());
            }

            int wadIndex = form.getWadIndex();
            if (wadFile != null && wadFile.getFiles().size() > wadIndex) {
                WADEntry wadEntry = wadFile.getFiles().get(wadIndex);

                if (!wadEntry.isDummy() && wadEntry.getFile() instanceof MOFHolder) {
                    MOFHolder holder = (MOFHolder) wadEntry.getFile();

                    // Setup VLO.
                    VLOArchive vlo = getMap().getConfig().getForcedVLO(wadEntry.getDisplayName());
                    if (vlo == null)
                        vlo = themeBook.getVLO(getMap());
                    holder.setVloFile(vlo);

                    // Setup MeshView.
                    MeshView view = setupNode(new MeshView(holder.getMofMesh()), x, y, z);
                    view.setMaterial(holder.getTextureMap().getPhongMaterial());
                    view.getTransforms().add(new Rotate(Math.toDegrees(yaw), Rotate.X_AXIS));
                    view.getTransforms().add(new Rotate(Math.toDegrees(pitch), Rotate.Y_AXIS));
                    view.getTransforms().add(new Rotate(Math.toDegrees(roll), Rotate.Z_AXIS));
                    return view;
                }
            }
        }

        PhongMaterial material = MATERIAL_ENTITY_ICON;

        FroggerEXEInfo config = getMap().getConfig();

        // Attempt to apply fly texture.
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

        TriangleMesh triMesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        triMesh.getPoints().addAll(-entityIconSize * 0.5f, entityIconSize * 0.5f, 0, -entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, -entityIconSize * 0.5f, 0, entityIconSize * 0.5f, entityIconSize * 0.5f, 0);
        triMesh.getTexCoords().addAll(0, 1, 0, 0, 1, 0, 1, 1);
        triMesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 0, 0);

        MeshView triMeshView = new MeshView(triMesh);
        triMeshView.setDrawMode(DrawMode.FILL);
        triMeshView.setMaterial(material);
        triMeshView.setCullFace(CullFace.NONE);

        return setupNode(triMeshView, x, y, z);
    }

    private <T extends Node> T setupNode(T node, float x, float y, float z) {
        node.setTranslateX(x);
        node.setTranslateY(y);
        node.setTranslateZ(z);
        getRenderManager().addNode(ENTITY_LIST, node);
        return node;
    }
}
