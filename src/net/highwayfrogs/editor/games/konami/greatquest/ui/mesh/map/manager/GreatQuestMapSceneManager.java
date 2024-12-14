package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Scale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr.kcVtxBufFileStruct;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh.kcCTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctLeaf;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctTree;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctTree.kcOctTreeStatus;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctTreeTraversalInfo;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestMapUIManager.GreatQuestMapListManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.system.math.Box.BoxBoundaryMode;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Allows poking around in the map kcCResOctTreeSceneManager.
 * Created by Kneesnap on 11/17/2024.
 */
public class GreatQuestMapSceneManager extends GreatQuestMapListManager<kcOctLeaf, Box> {
    @NonNull private final kcCResOctTreeSceneMgr sceneManager;
    private final GreatQuestOctTreeState[] states = new GreatQuestOctTreeState[kcEntityTreeDisplayType.values().length];
    private final Scale boxScale = new Scale(1, 1, 1);

    private final Vector3f tempPosition = new Vector3f();
    private ComboBox<kcEntityTreeDisplayType> treeViewComboBox;
    private DisplayList previewDisplayList;
    private Slider detailViewSlider;

    private static final PhongMaterial BOUNDING_FLAGGED_MATERIAL = Scene3DUtils.makeHighlightOverlayMaterial(Color.RED);
    private static final PhongMaterial BOUNDING_NORMAL_MATERIAL = Scene3DUtils.makeHighlightOverlayMaterial(Color.YELLOW);

    public GreatQuestMapSceneManager(MeshViewController<GreatQuestMapMesh> controller, kcCResOctTreeSceneMgr sceneManager) {
        super(controller);
        this.sceneManager = sceneManager;
        this.disableRemoveButton = true;
    }

    @Override
    public void onSetup() {
        this.states[kcEntityTreeDisplayType.ENTITY.ordinal()] = new GreatQuestOctTreeState(this, kcEntityTreeDisplayType.ENTITY, this.sceneManager.getEntityTree());
        this.states[kcEntityTreeDisplayType.VISUAL.ordinal()] = new GreatQuestOctTreeState(this, kcEntityTreeDisplayType.VISUAL, this.sceneManager.getVisualTree());

        this.previewDisplayList = getRenderManager().createDisplayListWithNewGroup(); // Run before adding values.
        super.onSetup();
        getAddValueButton().setDisable(true);
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        getMainGrid().addBoldLabel("This view is for debugging purposes only.");
        getMainGrid().addNormalLabel("The game world is tracked as an Octree by the kcCGameSystem.");
        getMainGrid().addNormalLabel("This allows the game to avoid drawing off-screen terrain/entities.");
        getMainGrid().addNormalLabel("As a user of FrogLord, you do not need to modify this data as it is automatically generated.");
        getMainGrid().addSeparator();

        super.setupMainGridEditor(sidePanel);
        this.treeViewComboBox = getMainGrid().addEnumSelector("Tree Type", kcEntityTreeDisplayType.VISUAL, kcEntityTreeDisplayType.values(), false, newType -> {
            refreshList();

            // Update detail view slider.
            int smallestNodeDepth = getState().getTree().getSmallestNodeDepth();
            if (this.detailViewSlider.getValue() > smallestNodeDepth)
                this.detailViewSlider.setValue(smallestNodeDepth);
            this.detailViewSlider.setMax(smallestNodeDepth);
        });

        int smallestNodeDepth = getState().getTree().getSmallestNodeDepth();
        this.detailViewSlider = getMainGrid().addIntegerSlider("Tree Height Filter", smallestNodeDepth,
                newTreeHeightFilter -> updateValueVisibility(), 0, smallestNodeDepth);
        this.detailViewSlider.setMinorTickCount(0);
        this.detailViewSlider.setMajorTickUnit(1);
        this.detailViewSlider.setSnapToTicks(true);
        this.detailViewSlider.setShowTickMarks(true);
        this.detailViewSlider.setShowTickLabels(false);
        this.detailViewSlider.setDisable(getValueDisplaySetting().getValue() != ListDisplayType.ALL);

        Slider boxScaleSlider = getMainGrid().addDoubleSlider("Preview Scale", .5, null, 0, 1);
        boxScaleSlider.setDisable(false);
        this.boxScale.xProperty().bind(boxScaleSlider.valueProperty());
        this.boxScale.yProperty().bind(boxScaleSlider.valueProperty());
        this.boxScale.zProperty().bind(boxScaleSlider.valueProperty());

        getValueDisplaySetting().valueProperty().addListener((observable, oldValue, newValue) -> this.detailViewSlider.setDisable(newValue != ListDisplayType.ALL));
    }

    @Override
    public boolean isValueVisibleByUI(kcOctLeaf leaf) {
        return super.isValueVisibleByUI(leaf)
                && (getValueDisplaySetting().getValue() != ListDisplayType.ALL || (leaf != null && leaf.getNodeDepth() == (int) this.detailViewSlider.getValue()));
    }

    private GreatQuestOctTreeState getState() {
        kcEntityTreeDisplayType type = this.treeViewComboBox != null ? this.treeViewComboBox.getValue() : null;
        if (type == null)
            type = kcEntityTreeDisplayType.VISUAL;

        return this.states[type.ordinal()];
    }

    @Override
    public String getTitle() {
        return "Scene Manager (DEBUG ONLY)";
    }

    @Override
    public String getValueName() {
        return "kcOctLeaf";
    }

    @Override
    protected String getListDisplayName(int index, kcOctLeaf leaf) {
        if (leaf == null)
            return super.getListDisplayName(index, null);

        GreatQuestOctTreeState state = getState();
        String displayName = state.nameCache.get(leaf);
        if (displayName == null)
            state.nameCache.put(leaf, displayName = "Leaf " + getState().getTree().getLeaves().indexOf(leaf));

        return displayName;
    }

    @Override
    public List<kcOctLeaf> getValues() {
        return getState().getLeaves();
    }

    @Override
    protected Box setupDisplay(kcOctLeaf leaf) {
        if (leaf == null || leaf.isEmpty())
            return null;

        kcOctTree tree = getState().getTree();
        float size = leaf.getCubeDimensions(tree);
        Vector3f position = leaf.getWorldCenterPosition(tree, this.tempPosition);
        Box newPreview = new Box(size, size, size);
        newPreview.setMouseTransparent(true);
        newPreview.setMaterial(leaf.isEnabled() ? BOUNDING_NORMAL_MATERIAL : BOUNDING_FLAGGED_MATERIAL);
        Scene3DUtils.setNodePosition(newPreview, position.getX(), position.getY(), position.getZ());
        newPreview.getTransforms().add(this.boxScale);
        getController().getMainLight().getScope().add(newPreview);
        this.previewDisplayList.add(newPreview);
        return newPreview;
    }

    @Override
    protected void updateEditor(kcOctLeaf leaf) {
        getEditorGrid().addLabel("Enabled?", String.valueOf(leaf.isEnabled()));
        getEditorGrid().addLabel("Depth", String.valueOf(leaf.getNodeDepth()));
        getEditorGrid().addLabel("Position", String.valueOf(leaf.getWorldCenterPosition(getState().getTree(), this.tempPosition)));
        getEditorGrid().addLabel("Parent ID", String.valueOf(leaf.getParent()));
        getEditorGrid().addLabel("Index in Parent", String.valueOf(leaf.getLocalIndexWithinParent()));

        StringBuilder builder = new StringBuilder();
        kcOctLeaf.appendIds(builder, leaf.getSideNumbers());
        getEditorGrid().addLabel("Side IDs", builder.toString());

        kcOctTreeLeafData leafData = getState().getOrCreateLeafData(leaf);
        getEditorGrid().addLabel("Vertex Buffers", String.valueOf(leafData.getVertexBuffers().size()));
        getEditorGrid().addLabel("Collision Meshes", String.valueOf(leafData.getCollisionMeshes().size()));
    }

    @Override
    protected void setVisible(kcOctLeaf leaf, Box preview, boolean visible) {
        if (preview != null)
            preview.setVisible(visible);
    }

    @Override
    protected void onSelectedValueChange(kcOctLeaf oldLeaf, Box oldPreview, kcOctLeaf newLeaf, Box newPreview) {
        // Do nothing.
    }

    @Override
    protected kcOctLeaf createNewValue() {
        return null;
    }

    @Override
    protected void onDelegateRemoved(kcOctLeaf leaf, Box preview) {
        if (preview != null) {
            getController().getMainLight().getScope().remove(preview);
            this.previewDisplayList.remove(preview);
        }
    }

    @Override
    protected boolean tryRemoveValue(kcOctLeaf leaf) {
        return false; // Prevent removals.
    }

    @Getter
    private static class GreatQuestOctTreeState {
        private final GreatQuestMapSceneManager manager;
        private final kcEntityTreeDisplayType displayType;
        private final kcOctTree tree;
        private final List<kcOctLeaf> leaves;
        private final Map<kcOctLeaf, String> nameCache = new HashMap<>();
        private final Map<kcOctLeaf, kcOctTreeLeafData> leafData = new HashMap<>();

        public GreatQuestOctTreeState(GreatQuestMapSceneManager manager, kcEntityTreeDisplayType displayType, kcOctTree tree) {
            this.manager = manager;
            this.displayType = displayType;
            this.tree = tree;

            // Add leaves.
            this.leaves = new ArrayList<>();
            for (kcOctLeaf leaf : tree.getLeaves())
                if (leaf != null && !leaf.isEmpty())
                    this.leaves.add(leaf);

            generateOctLeafData();
        }

        /**
         * Gets or creates the leaf data for a given leaf.
         * @param leaf the leaf to get/create the data for
         * @return leafData
         */
        public kcOctTreeLeafData getOrCreateLeafData(kcOctLeaf leaf) {
            kcOctTreeLeafData leafData = this.leafData.get(leaf);
            if (leafData == null)
                this.leafData.put(leaf, leafData = new kcOctTreeLeafData(this.manager));

            return leafData;
        }

        private static kcOctTreeStatus testVertexBuffer(kcOctTreeTraversalInfo<kcVtxBufFileStruct> traversalInfo, int branchIndex) {
            Vector3f centerPos = traversalInfo.getContext().getBoundingBox().getBox(null).getCenterPosition(null);
            return traversalInfo.getCollisionBox().contains(centerPos, BoxBoundaryMode.INCLUSIVE) ? kcOctTreeStatus.IN : kcOctTreeStatus.OUT;
        }

        private void handleLeaf(kcVtxBufFileStruct vertexBuffer, kcOctLeaf leaf) {
            getOrCreateLeafData(leaf).getVertexBuffers().add(vertexBuffer);
        }

        private static kcOctTreeStatus testCollisionMesh(kcOctTreeTraversalInfo<kcCTriMesh> traversalInfo, int branchIndex) {
            Vector3f centerPos = traversalInfo.getContext().getBoundingBox().getBox(null).getCenterPosition(null);
            return traversalInfo.getCollisionBox().contains(centerPos, BoxBoundaryMode.INCLUSIVE) ? kcOctTreeStatus.IN : kcOctTreeStatus.OUT;
        }

        private void handleLeaf(kcCTriMesh triMesh, kcOctLeaf leaf) {
            getOrCreateLeafData(leaf).getCollisionMeshes().add(triMesh);
        }

        private void generateOctLeafData() {
            this.leafData.clear();

            kcCResOctTreeSceneMgr sceneManager = this.manager.sceneManager;

            // 1) Vertex Buffers
            BiConsumer<kcVtxBufFileStruct, kcOctLeaf> vertexBufferHandler = this::handleLeaf;
            for (int i = 0; i < sceneManager.getVertexBuffers().size(); i++) {
                kcVtxBufFileStruct vtxBuf = sceneManager.getVertexBuffers().get(i);
                this.tree.traverse(GreatQuestOctTreeState::testVertexBuffer, vertexBufferHandler, vtxBuf);
            }

            // 2) Collision Meshes
            BiConsumer<kcCTriMesh, kcOctLeaf> triMeshHandler = this::handleLeaf;
            for (int i = 0; i < sceneManager.getCollisionMeshes().size(); i++) {
                kcCTriMesh triMesh = sceneManager.getCollisionMeshes().get(i);
                this.tree.traverse(GreatQuestOctTreeState::testCollisionMesh, triMeshHandler, triMesh);
            }
        }
    }

    public enum kcEntityTreeDisplayType {
        VISUAL,
        ENTITY
    }

    @Getter
    @RequiredArgsConstructor
    private static class kcOctTreeLeafData {
        private final GreatQuestMapSceneManager manager;
        private final List<kcVtxBufFileStruct> vertexBuffers = new ArrayList<>();
        private final List<kcCTriMesh> collisionMeshes = new ArrayList<>();
    }
}