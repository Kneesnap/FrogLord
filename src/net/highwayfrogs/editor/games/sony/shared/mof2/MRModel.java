package net.highwayfrogs.editor.games.sony.shared.mof2;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelFileUIController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelMeshController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.utils.DynamicMeshObjExporter;
import net.highwayfrogs.editor.games.sony.shared.utils.FileUtils3D;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FileReceiver;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Millennium Interactive 3D model (MOF or Animated MOF) file.
 * Because the data contained in these files can vary and be quite odd, this serves as something of a frontend for a consistent API to access of the underlying data types.
 * Created by Kneesnap on 2/18/2025.
 */
@Getter
public class MRModel extends SCSharedGameFile {
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

    private transient FroggerMapTheme theme; // TODO: We may want to change how we track this to instead maybe know the parent WAD file and calculate it from that. This is in the interest of supporting other games.
    @Setter private transient VLOArchive vloFile; // TODO: Change this later, I think we want to change how this is tracked.

    public static final int FLAG_ANIMATION_FILE = Constants.BIT_FLAG_3; // This is an animation MOF file.

    public static final byte[] DUMMY_DATA = "DUMY".getBytes();

    public MRModel(SCGameInstance instance, FroggerMapTheme theme, MRModel lastCompleteMOF) {
        super(instance);
        this.theme = theme;
        this.completeCounterpart = lastCompleteMOF;
    }

    @Override
    @SneakyThrows
    public void exportAlternateFormat() {
        // Nothing?
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
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);

        propertyList.add("Type", getModelType() + (this.incomplete ? " (Incomplete)" : ""));
        if (this.incomplete)
            propertyList.add("Complete Counterpart", this.completeCounterpart.getFileDisplayName());

        if (this.animatedMof != null)
            propertyList = this.animatedMof.addToPropertyList(propertyList);
        if (this.staticMof != null)
            propertyList = this.staticMof.addToPropertyList(propertyList);

        return propertyList;
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        showEditor3D();
    }

    /**
     * Show the 3D editor.
     */
    public void showEditor3D() {
        if (getVloFile() == null) {
            // Just grab the first VLO.
            VLOArchive firstVLO = getArchive().findFirstVLO();
            if (firstVLO != null) {
                setVloFile(firstVLO);
            } else {
                getArchive().promptVLOSelection(getTheme(), vlo -> {
                    setVloFile(vlo);
                    showEditor3D();
                }, false);
                return;
            }
        }

        MeshViewController.setupMeshViewer(getGameInstance(), new MRModelMeshController(getGameInstance()), createMesh());
    }

    /**
     * Export this model to .obj
     * @param folder The folder to export to.
     * @param vlo    The graphics pack to export.
     */
    @SneakyThrows
    public void exportObject(File folder, VLOArchive vlo) {
        if (getModelType() == MRModelType.DUMMY) {
            System.out.println("Cannot export dummy MOF.");
            return;
        }

        setVloFile(vlo);
        DynamicMeshObjExporter.exportMeshToObj(getLogger(), createMesh(), folder, FileUtils.stripExtension(getFileDisplayName()), true);

        // Export mm3d too.
        File saveTo = new File(folder, FileUtils.stripExtension(getFileDisplayName()) + ".mm3d");
        FileUtils.deleteFile(saveTo);

        MisfitModel3DObject model = FileUtils3D.convertMofToMisfitModel(this);
        DataWriter writer = new DataWriter(new FileReceiver(saveTo));
        if (model != null)
            model.save(writer);
        writer.closeReceiver();
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
}
