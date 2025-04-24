package net.highwayfrogs.editor.games.sony.shared.mof2;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FileReceiver;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook.MRMofFlipbookAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook.MRMofFlipbookAnimationList;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelFileUIController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelMeshController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a Millennium Interactive 3D model (MOF or Animated MOF) file.
 * Because the data contained in these files can vary and be quite odd, this serves as something of a frontend for a consistent API to access of the underlying data types.
 * TODO: Wait, what's even the purpose of this class? If we're just going to have underlying stuff? Hrmm.
 *  -> I think the purpose is to serve as a frontend to hide away the differences of underlying things. Ah, yeah.
 *
 * TODO: Validate MediEvil models.
 * TODO: Validate the Frogger models which have the broken uvs / orange stripe. (psx-demo-ntsc)
 *
 * TODO LIST:
 *  1) Go over this file.
 *  2) Go over todos of other folder.
 *  3) When done, delete the old texture atlas system.
 * Created by Kneesnap on 2/18/2025.
 */
public class MRModel extends SCSharedGameFile {
    @Getter private MRStaticMof staticMof; // TODO: What about cases with multiple static mofs?
    @Getter private MRAnimatedMof animatedMof;

    // Frogger is the only game I'm aware of that uses this.
    // This is an EXTREMELY hacky/risky feature, one which manually stitches together two 3D models.
    // The "incomplete" model is the one which the data is stitched onto.
    // The purpose of this feature is to decrease duplicate data in memory/RAM, ie: to save memory.
    // And this is important in Frogger since it often has extremely similar variants of the same model (5 differently colored baby froglets, 4 multiplayer frog characters, etc)
    // So at runtime, the game has a hardcoded list of models which it will overwrite pointers within in order to avoid needing to include a full 3D model.
    // This boolean indicates whether the current model is one of those "incomplete" models.
    // There is no official term for these models that I'm aware of.
    @Getter @Setter private boolean incomplete;
    @Getter private MRModel completeCounterpart;

    @Getter private transient FroggerMapTheme theme; // TODO: We may want to change how we track this to instead maybe know the parent WAD file and calculate it from that. This is in the interest of supporting other games.
    @Setter @Getter private transient VLOArchive vloFile; // TODO: Change this later, I think we want to change how this is tracked.

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
        // TODO: IMPLEMENT NEW.
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
        if (this.animatedMof != null)
            propertyList = this.animatedMof.addToPropertyList(propertyList);
        if (this.staticMof != null)
            propertyList = this.staticMof.addToPropertyList(propertyList);

        return propertyList;
    }

    @Override
    public void handleWadEdit(WADFile parent) { // TODO: Needs review.
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) { // TODO: TOSS? (New 3D viewer includes this)
            FXUtils.makePopUp("Your version of JavaFX does not support 3D, so models cannot be previewed.", AlertType.WARNING);
            return;
        }

        showEditor3D();
    }

    /**
     * Show the 3D editor.
     */
    public void showEditor3D() { // TODO: Needs review.
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
     * Gets the number of animations in this mof. (Does not include texture animation).
     * Get the maximum animation action id.
     * TODO: Needs review.
     * @return maxAnimation
     */
    public int getAnimationCount() {
        if (isAnimatedMOF())
            return this.animatedMof.getModelSets().get(0).getCelSet().getAnimations().size(); // TODO: This is WRONG! I just wrote it like this for a temporary compilation aid.

        // Flipbook.
        return this.staticMof.getParts().stream()
                .map(MRMofPart::getFlipbook)
                .map(MRMofFlipbookAnimationList::getAnimations)
                .mapToInt(List::size)
                .max().orElse(0);
    }

    /**
     * Get the animation's frame count.
     * TODO: Needs review.
     * @return frameCount
     */
    public int getFrameCount(int animationId) {
        if (isAnimatedMOF() && animationId != -1) // XAR
            return this.animatedMof.getAnimationById(animationId).getFrameCount();

        // Flipbook and Texture.
        int maxFrame = 0;
        for (MRMofPart part : asStaticFile().getParts()) {
            MRMofFlipbookAnimationList flipbook = part.getFlipbook();

            if (animationId == -1) {
                for (MRMofTextureAnimation anim : part.getTextureAnimations()) {
                    int frameCount = anim.getTotalFrameCount();
                    if (frameCount > maxFrame)
                        maxFrame = frameCount;
                }
            } else if (flipbook != null) {
                MRMofFlipbookAnimation action = flipbook.getAction(animationId);
                if (action != null && action.getFrameCount() > maxFrame)
                    maxFrame = action.getFrameCount();
            }
        }

        return Math.max(maxFrame, 1);
    }

    /**
     * Get the forced static mof file.
     * @return staticMof
     */
    public MRStaticMof asStaticFile() { // TODO: Needs review.
        return isAnimatedMOF() ? this.animatedMof.getStaticMofs().get(0) : this.staticMof;
    }

    /**
     * Gets the name of a particular animation ID, if there is one.
     * @param animationId The animation ID to get.
     * @return name
     */
    public String getName(int animationId) { // TODO: Needs review.
        if (animationId == -1)
            return asStaticFile().getTextureAnimationCount() > 0 ? "Texture Animation" : "No Animation";

        NameBank bank = getConfig().getAnimationBank();
        if (bank == null)
            return (animationId != 0) ? "Animation " + animationId : "Default Animation";

        String bankName = SCUtils.stripWin95(FileUtils.stripExtension(getFileDisplayName()));
        NameBank childBank = bank.getChildBank(bankName);
        return childBank != null ? childBank.getName(animationId) : getConfig().getAnimationBank().getEmptyChildNameFor(animationId, getAnimationCount());
    }

    /**
     * Export this model to .obj
     * TODOD: Needs review.
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
        // TODO: FileUtils3D.exportMofToObj(asStaticFile(), folder, vlo);

        // Export mm3d too.
        File saveTo = new File(folder, FileUtils.stripExtension(getFileDisplayName()) + ".mm3d");
        FileUtils.deleteFile(saveTo);

        // TODO: MisfitModel3DObject model = FileUtils3D.convertMofToMisfitModel(this);
        DataWriter writer = new DataWriter(new FileReceiver(saveTo));
        // TODO: model.save(writer);
        writer.closeReceiver();
    }

    /**
     * Gets the override of this MOFHolder, if there is one.
     * TODO: Better system?
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
     * Get whether this is an animated (XAR) MOF.
     * TODO: Needs review.
     */
    public boolean isAnimatedMOF() {
        return this.animatedMof != null;
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
