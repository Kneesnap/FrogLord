package net.highwayfrogs.editor.games.sony.shared.mof2;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ISCTextureUser;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimationEntry;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelFileUIController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelMeshController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.games.sony.shared.mof2.utils.MRModelUtils;
import net.highwayfrogs.editor.games.sony.shared.mof2.utils.MRMofAndMisfitModelConverter;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.utils.DynamicMeshObjExporter;
import net.highwayfrogs.editor.games.sony.shared.utils.SCNameBank;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.gui.DefaultFileUIController.IExtraUISupplier;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.mesh.Embedded3DViewComponent;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.MessageTrackingLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Millennium Interactive 3D model (MOF or Animated MOF) file.
 * Because the data contained in these files can vary and be quite odd, this serves as something of a frontend for a consistent API to access of the underlying data types.
 * Created by Kneesnap on 2/18/2025.
 */
@Getter
public class MRModel extends SCSharedGameFile implements ISCTextureUser, IExtraUISupplier {
    private MRStaticMof staticMof;
    private MRAnimatedMof animatedMof;

    // Frogger is the only game I'm aware of that uses this.
    // This is an EXTREMELY hacky/risky feature, one which manually stitches together two 3D models.
    // The "incomplete" model is the one which the data is stitched onto.
    // The purpose of this feature is to decrease duplicate data in memory/RAM, ie: to save memory.
    // And this is important in Frogger since it often has extremely similar variants of the same model (5 differently colored baby froglets, 4 multiplayer frog characters, etc)
    // So at runtime, the game has a hardcoded list of models which it will overwrite pointers within in order to avoid needing to include a full 3D model.
    // This boolean indicates whether the current model is one of those "incomplete" models.
    // There is no official term for these models that I'm aware of.
    @Setter private boolean incomplete;
    private MRModel completeCounterpart;

    @Setter private transient VloFile vloFile;

    public static final int FLAG_ANIMATION_FILE = Constants.BIT_FLAG_3; // This is an animation MOF file.

    public static final byte[] DUMMY_DATA = "DUMY".getBytes();

    public MRModel(SCGameInstance instance, MRModel lastCompleteMOF) {
        super(instance);
        this.completeCounterpart = lastCompleteMOF;
    }

    @Override
    public void load(DataReader reader) {
        int dataStartIndex = reader.getIndex();
        byte[] signature = reader.readBytes(DUMMY_DATA.length);

        this.staticMof = null;
        this.animatedMof = null;
        if (Arrays.equals(DUMMY_DATA, signature))
            return;

        // Read only after verifying it isn't a dummy file.
        reader.skipInt(); // File length, including header.
        int flags = reader.readInt();

        reader.setIndex(dataStartIndex);
        if ((flags & FLAG_ANIMATION_FILE) == FLAG_ANIMATION_FILE) {
            if (!MRAnimatedMof.testSignature(signature))
                throw new RuntimeException("Expected an animated MOF file, but that's not what we found!");

            this.animatedMof = new MRAnimatedMof(this);
            this.animatedMof.load(reader);
        } else {
            boolean oldMofFormat = MRStaticMof.canVersionFormatHaveNullSignature(getGameInstance());
            if (!Arrays.equals(MRStaticMof.SIGNATURE_BYTES, signature) && !(oldMofFormat && Arrays.equals(MRStaticMof.OLD_SIGNATURE_BYTES, signature)))
                throw new RuntimeException("Expected a static MOF file, but that's not what we found!");

            this.staticMof = new MRStaticMof(this);
            this.staticMof.load(reader);
        }

        if (!this.incomplete) // Only incomplete models have a complete counterpart.
            this.completeCounterpart = null;
    }

    @Override
    public void save(DataWriter writer) {
        switch (getModelType()) {
            case DUMMY:
                writer.writeBytes(DUMMY_DATA);
                break;
            case ANIMATED:
                this.animatedMof.save(writer);
                break;
            case STATIC:
                this.staticMof.save(writer);
                break;
            default:
                throw new RuntimeException("Cannot save unsupported ModelType: " + getModelType());
        }
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.GEOMETRIC_SHAPES_32.getFxImage();
    }

    @Override
    public GameUIController<SCGameInstance> makeEditorUI() {
        return loadEditor(getGameInstance(), new MRModelFileUIController(getGameInstance()), this);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);

        propertyList.add("Type", getModelType() + (this.incomplete ? " (Incomplete)" : ""));
        propertyList.add("Main VLO", this.vloFile != null ? this.vloFile.getFileDisplayName() : "None");
        if (this.incomplete)
            propertyList.add("Complete Counterpart", this.completeCounterpart != null ? this.completeCounterpart.getFileDisplayName() : "None");

        if (this.animatedMof != null)
            this.animatedMof.addToPropertyList(propertyList);
        if (this.staticMof != null)
            this.staticMof.addToPropertyList(propertyList);
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportAsObjFile = new MenuItem("Export as .obj file.");
        contextMenu.getItems().add(exportAsObjFile);
        exportAsObjFile.setOnAction(event ->
                DynamicMeshObjExporter.askUserToMeshToObj(getGameInstance(), getLogger(), createMeshWithDefaultAnimation(), FileUtils.stripExtension(getFileDisplayName()), true));

        if (isAnimatedMof()) {
            MenuItem bakeToStaticMof = new MenuItem("Bake to Static MOF.");
            contextMenu.getItems().add(bakeToStaticMof);
            bakeToStaticMof.setOnAction(event -> {
                MRModel newModel = MRModelUtils.bakeAndReplaceAnimatedMof(this);
                FXUtils.showPopup(AlertType.INFORMATION, "Successfully imported the model.", "Successfully baked '" + newModel.getFileDisplayName() + "' into a staticMof!");
            });
        }
    }

    @Override
    public void performDefaultUIAction() {
        showEditor3D();
    }

    /**
     * Show the 3D editor.
     */
    public void showEditor3D() {
        MeshViewController.setupMeshViewer(getGameInstance(), new MRModelMeshController(getGameInstance()), createMesh());
    }

    /**
     * Sets the static mof represented by this model.
     * @param staticMof the static mof to apply
     */
    public void setStaticMof(MRStaticMof staticMof) {
        if (staticMof == null)
            throw new NullPointerException("staticMof");

        this.animatedMof = null;
        this.staticMof = staticMof;
    }

    /**
     * Sets the animated mof represented by this model.
     * @param animatedMof the animated mof to apply
     */
    @SuppressWarnings("unused")
    public void setAnimatedMof(MRAnimatedMof animatedMof) {
        if (animatedMof == null)
            throw new NullPointerException("animatedMof");

        this.animatedMof = animatedMof;
        this.staticMof = null;
    }

    @Override
    public void askUserToImportFile() {
        File inputFile = FileUtils.askUserToOpenFile(getGameInstance(), SINGLE_FILE_IMPORT_PATH);
        if (inputFile == null)
            return;

        String fileName = inputFile.getName().toLowerCase();
        if (fileName.endsWith(".mm3d")) {
            MessageTrackingLogger importLogger = new MessageTrackingLogger(getLogger());
            importLogger.info("Importing '%s'...", inputFile.getName());

            byte[] rawFileBytes;
            try {
                rawFileBytes = Files.readAllBytes(inputFile.toPath());
            } catch (IOException ex) {
                Utils.handleError(getLogger(), ex, true, "Failed to read contents of '%s'.", inputFile.getName());
                return;
            }

            MisfitModel3DObject newObject = new MisfitModel3DObject();
            DataReader reader = new DataReader(new ArraySource(rawFileBytes));

            try {
                newObject.load(reader);
            } catch (Exception ex) {
                Utils.handleError(importLogger, ex, true, "An error occurred loading '%s'.", inputFile.getName());
                return;
            }

            try {
                MRMofAndMisfitModelConverter.importMofFromModel(importLogger, newObject, this);
            } catch (Exception ex) {
                Utils.handleError(importLogger, ex, true, "An error occurred while importing '%s'.", inputFile.getName());
                return;
            }

            importLogger.showImportPopup(inputFile.getName());
        } else if (fileName.endsWith(".vlo") || fileName.endsWith(".xar") || fileName.endsWith(".xmr")) {
            importFile(inputFile);
        } else {
            FXUtils.showPopup(AlertType.WARNING, "Unrecognized/unsupported file type.", "Don't know how to import this file type. Aborted.");
        }
    }

    /**
     * Export this model to .obj
     * @param folder The folder to export to.
     */
    @SneakyThrows
    public void exportObject(File folder) {
        if (getModelType() == MRModelType.DUMMY)
            return;

        DynamicMeshObjExporter.exportMeshToObj(getLogger(), createMeshWithDefaultAnimation(), folder, FileUtils.stripExtension(getFileDisplayName()), true);
    }

    /**
     * Export this model to maverick model 3d format (.mm3d).
     * @param folder The folder to export to.
     * @param textureExportFolder if not null, any required textures will be exported to this folder
     */
    public void exportMaverickModel(File folder, String relativeMofTexturePath, File textureExportFolder) {
        if (getModelType() == MRModelType.DUMMY)
            return;

        // Export mm3d too.
        List<MRStaticMof> staticMofs = getStaticMofs();
        for (int i = 0; i < staticMofs.size(); i++) {
            String suffix = (staticMofs.size() > 1 ? "-" + i : "");
            File saveTo = new File(folder, FileUtils.stripExtension(getFileDisplayName()) + suffix + ".mm3d");
            FileUtils.deleteFile(saveTo);

            MisfitModel3DObject model = MRMofAndMisfitModelConverter.convertMofToMisfitModel(staticMofs.get(i), relativeMofTexturePath, textureExportFolder);
            model.writeDataToFile(getLogger(), saveTo, true);
        }
    }

    /**
     * Gets the override of this MRModel, if there is one.
     * @return override
     */
    public MRModel getOverride() {
        String mofOverride = getConfig().getMofRenderOverrides().get(getFileDisplayName());
        if (mofOverride != null) {
            MWIResourceEntry entry = getGameInstance().getResourceEntryByName(mofOverride);
            if (entry != null) {
                SCGameFile<?> file = getGameInstance().getGameFile(entry);
                if (file instanceof MRModel)
                    return (MRModel) file;
            }
        }

        return this;
    }

    /**
     * Clears this model to make it a dummy.
     */
    public void setDummy() {
        this.animatedMof = null;
        this.staticMof = null;
        this.incomplete = false;
    }

    /**
     * Create a JavaFX mesh representation of this model.
     */
    public MRModelMesh createMesh() {
        return new MRModelMesh(this);
    }

    /**
     * Create a JavaFX mesh representation of this model.
     */
    public MRModelMesh createMeshWithDefaultAnimation() {
        MRModelMesh newMesh = new MRModelMesh(this);
        newMesh.getAnimationPlayer().setDefaultAnimation();
        return newMesh;
    }

    /**
     * Gets the FrogLord model behavior type.
     * @return modelType
     */
    public MRModelType getModelType() {
        if (this.animatedMof != null) {
            return MRModelType.ANIMATED;
        } else if (this.staticMof != null) {
            return MRModelType.STATIC;
        } else {
            return MRModelType.DUMMY;
        }
    }

    /**
     * Test whether this is an animated (XAR) MOF file.
     */
    public boolean isAnimatedMof() {
        return this.animatedMof != null;
    }

    /**
     * Test whether this model is a single static (non-XAR) MOF file.
     * NOTE: This has no relation to the static Mofs seen within an animated (XAR) MOF file.
     */
    public boolean isStaticMof() {
        return this.staticMof != null;
    }

    /**
     * Test if this is a dummy MOF.
     */
    public boolean isDummy() {
        return this.animatedMof == null && this.staticMof == null;
    }

    /**
     * Get a list containing all static mofs available.
     * The returned list should not be modified.
     */
    public List<MRStaticMof> getStaticMofs() {
        if (this.animatedMof != null) {
            return this.animatedMof.getStaticMofs();
        } else if (this.staticMof != null) {
            return Collections.singletonList(this.staticMof);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Many prototype builds contain froglets with models that are slightly broken.
     * This lets us test if this model meets those conditions.
     * New FrogLord should fully investigate what's actually going on, and develop a proper fix.
     * GEN_FROG2.XMR/3/4 are also broken, but don't seem to be fixed with the froglet fix. Hmm.
     * TODO: REVIEW THIS
     */
    public boolean isWeirdFrogMOF() {
        if (!getGameInstance().isFrogger())
            return false;

        FroggerGameInstance frogger = (FroggerGameInstance) getGameInstance();
        if (frogger.getVersionConfig().isAtOrBeforeBuild4() || frogger.getVersionConfig().getBuild() >= 50)
            return false; // Note: Build 5 may or may not be included. Build 50 is also probably not the correct build to test against here.

        String name = getFileDisplayName();
        boolean isFroglet = "GEN_CHECKPOINT_1.XMR".equals(name)
                || "GEN_CHECKPOINT_2.XMR".equals(name)
                || "GEN_CHECKPOINT_3.XMR".equals(name)
                || "GEN_CHECKPOINT_4.XMR".equals(name)
                || "GEN_CHECKPOINT_5.XMR".equals(name);
        boolean isGoldenFrog = "GEN_GOLD_FROG.XMR".equals(name);

        return isFroglet || (isGoldenFrog && !frogger.getVersionConfig().isAtOrBeforeBuild20());
    }

    /**
     * Gets the name of an animation from a preconfigured list by its ID, if there is one
     * @param animationNameId The ID of the animation name to get.
     * @return name
     */
    public String getConfiguredAnimationName(int animationNameId) {
        if (animationNameId < 0)
            return null;

        SCNameBank bank = getGameInstance().getVersionConfig().getAnimationBank();
        if (bank == null)
            return null;

        String bankName = SCUtils.stripWin95(FileUtils.stripExtension(getFileDisplayName()));
        SCNameBank childBank = bank.getChildBank(bankName);
        return childBank != null ? childBank.getName(animationNameId) : null;
    }

    @Override
    public void onImport(SCGameFile<?> oldFile, String oldFileName, String importedFileName) {
        if (oldFile instanceof MRModel)
            ((MRModel) oldFile).updateIncompleteReferences(this);
    }

    /**
     * Update incomplete models which reference this one to reference a new model instead.
     * @param newModel the new model to reference
     */
    private void updateIncompleteReferences(MRModel newModel) {
        if (newModel == null)
            throw new NullPointerException("newModel");
        if (this.incomplete || isDummy() || this == newModel)
            return;

        for (MRModel otherModel : getGameInstance().getMainArchive().getAllFiles(MRModel.class)) {
            if (!otherModel.isIncomplete() || otherModel.getCompleteCounterpart() != this)
                continue;

            otherModel.completeCounterpart = newModel;
            List<MRStaticMof> staticMofs = newModel.getStaticMofs();
            List<MRStaticMof> otherStaticMofs = otherModel.getStaticMofs();
            if (staticMofs.size() != otherStaticMofs.size())
                throw new RuntimeException("The new incomplete model had " + otherStaticMofs.size() + " static mofs, but " + staticMofs.size() + " were expected to match the new counterpart model.");

            for (int i = 0; i < staticMofs.size(); i++) {
                MRStaticMof staticMof = staticMofs.get(i);
                MRStaticMof otherStaticMof = otherStaticMofs.get(i);
                if (staticMof.getParts().size() != otherStaticMof.getParts().size())
                    throw new RuntimeException("The new incomplete model had " + otherStaticMof.getParts().size() + " parts in staticMof " + i + ", but " + staticMof.getParts().size() + " were expected to match the new counterpart model.");

                for (int j = 0; j < staticMof.getParts().size(); j++) {
                    MRMofPart mofPart = staticMof.getParts().get(j);
                    MRMofPart otherPart = staticMof.getParts().get(j);
                    mofPart.copyToIncompletePart(otherPart);
                }
            }
        }
    }

    @Override
    public List<Short> getUsedTextureIds() {
        List<Short> textures = new ArrayList<>();
        List<MRStaticMof> staticMofs = getStaticMofs();
        for (int i = 0; i < staticMofs.size(); i++) {
            MRStaticMof staticMof = staticMofs.get(i);
            for (int j = 0; j < staticMof.getParts().size(); j++) {
                MRMofPart mofPart = staticMof.getParts().get(j);
                List<MRMofPolygon> polygons = mofPart.getOrderedPolygons();
                for (int k = 0; k < polygons.size(); k++) {
                    MRMofPolygon polygon = polygons.get(k);
                    if (polygon.getPolygonType().isTextured() && polygon.getTextureId() >= 0 && !textures.contains(polygon.getTextureId()))
                        textures.add(polygon.getTextureId());
                }

                // Add animated textures.
                for (int k = 0; k < mofPart.getTextureAnimations().size(); k++) {
                    MRMofTextureAnimation textureAnimation = mofPart.getTextureAnimations().get(k);
                    for (int l = 0; l < textureAnimation.getEntries().size(); l++) {
                        MRMofTextureAnimationEntry animationEntry = textureAnimation.getEntries().get(l);
                        if (animationEntry.getGlobalImageId() >= 0 && !textures.contains(animationEntry.getGlobalImageId()))
                            textures.add(animationEntry.getGlobalImageId());
                    }
                }
            }
        }

        return textures;
    }

    @Override
    public String getTextureUserName() {
        // Include the parent WAD to ensure files with the same name can be distinguished.
        WADFile parentWadFile = getParentWadFile();
        return getFileDisplayName() + (parentWadFile != null ? "[" + parentWadFile.getFileDisplayName() + "]" : "");
    }

    @Override
    public GameUIController<?> createExtraUIController() {
        if (isDummy())
            return null;

        Embedded3DViewComponent<?> component = new Embedded3DViewComponent<>(getGameInstance(), createMeshWithDefaultAnimation());
        component.getCamera().setFarClip(MRModelMeshController.MAP_VIEW_FAR_CLIP);
        component.getCamera().setTranslateZ(MRModelMeshController.CAMERA_DEFAULT_TRANSLATE_Z * 2); // smaller view -> larger distance to fit within smaller view.
        component.getCamera().setTranslateY(MRModelMeshController.CAMERA_DEFAULT_TRANSLATE_Y);
        component.getRotationCamera().getRotationX().setAngle(MRModelMeshController.CAMERA_DEFAULT_ROTATION_X);
        component.getRotationCamera().getRotationY().setAngle(MRModelMeshController.CAMERA_DEFAULT_ROTATION_Y);
        component.getRootNode().setHeight(200);

        return component;
    }

    /**
     * Change all usages of a particular texture ID to a new texture ID.
     * @param oldTextureId the texture ID to change
     * @param newTextureId the new texture ID to apply
     */
    public void replaceTextureIdUsages(short oldTextureId, short newTextureId) {
        List<MRStaticMof> staticMofs = getStaticMofs();
        for (int i = 0; i < staticMofs.size(); i++) {
            MRStaticMof staticMof = staticMofs.get(i);
            for (int j = 0; j < staticMof.getParts().size(); j++) {
                MRMofPart mofPart = staticMof.getParts().get(j);

                // Replace texture IDs in texture animations.
                List<MRMofTextureAnimation> textureAnimations = mofPart.getTextureAnimations();
                for (int k = 0; k < textureAnimations.size(); k++) {
                    MRMofTextureAnimation textureAnimation = textureAnimations.get(k);
                    List<MRMofTextureAnimationEntry> animationEntries = textureAnimation.getEntries();
                    for (int l = 0; l < animationEntries.size(); l++) {
                        MRMofTextureAnimationEntry animationEntry = animationEntries.get(l);
                        if (animationEntry.getGlobalImageId() == oldTextureId)
                            animationEntry.setGlobalImageId(newTextureId);
                    }
                }

                // Replace texture IDs directly on the polygons.
                List<MRMofPolygon> mofPolygons = mofPart.getOrderedPolygons();
                for (int k = 0; k < mofPolygons.size(); k++) {
                    MRMofPolygon mofPolygon = mofPolygons.get(k);
                    if (mofPolygon.getTextureId() == oldTextureId)
                        mofPolygon.setTextureId(newTextureId);
                }
            }
        }
    }
}
