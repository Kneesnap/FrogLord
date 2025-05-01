package net.highwayfrogs.editor.file.mof;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShadingMode;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimationCels;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbook;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbookAction;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.ISCFileDefinition;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.ui.file.MOFController;
import net.highwayfrogs.editor.games.sony.shared.ui.file.MOFMainController;
import net.highwayfrogs.editor.games.sony.shared.utils.FileUtils3D;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FileReceiver;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Holds MOF files.
 * Created by Kneesnap on 2/25/2019.
 */
@Getter
@Setter
public class MOFHolder extends SCSharedGameFile {
    private final MRModel newModel;
    private byte[] rawBytes; // Raw file data. // TODO: TOSS
    private boolean dummy; // Is this dummied data?
    private boolean incomplete; // Some mofs are changed at run-time to share information. This attempts to handle that.

    private MOFFile staticFile;
    private MOFAnimation animatedFile;

    private transient FroggerMapTheme theme; // TODO: We may want to change how we track this to instead maybe know the parent WAD file and calculate it from that. This is in the interest of supporting other games.
    private transient VLOArchive vloFile; // TODO: Change this later, I think we want to change how this is tracked.
    private MOFHolder completeMOF; // This is the last MOF which was not incomplete.

    public static final int FLAG_ANIMATION_FILE = Constants.BIT_FLAG_3; // This is an animation MOF file.

    public static final byte[] DUMMY_DATA = "DUMY".getBytes();

    public MOFHolder(SCGameInstance instance, FroggerMapTheme theme, MOFHolder lastCompleteMOF) {
        super(instance);
        this.theme = theme;
        this.completeMOF = lastCompleteMOF;
        this.newModel = new MRModel(instance, theme, lastCompleteMOF != null ? lastCompleteMOF.getNewModel() : null);
    }

    @Override
    public void setFileDefinition(ISCFileDefinition newFileDefinition) {
        super.setFileDefinition(newFileDefinition);
        this.newModel.setFileDefinition(newFileDefinition);
    }

    @Override
    @SneakyThrows
    public void exportAlternateFormat() {
        // TODO: TOSS
        File outputFile = new File(FrogLordApplication.getWorkingDirectory(), getFileDisplayName());
        if (this.rawBytes != null)
            FileUtils.writeBytesToFile(getLogger(), outputFile, this.rawBytes, true);
    }

    @Override
    public void load(DataReader reader) {
        reader.jumpTemp(reader.getIndex()); // TODO: TOSS
        this.rawBytes = reader.readBytes(reader.getRemaining());
        reader.jumpReturn();

        DataReader tempReader = new DataReader(new ArraySource(this.rawBytes));
        try {
            this.newModel.load(tempReader);
        } catch (Throwable th) {
            Utils.handleError(this.newModel.getLogger(), th, false, "Failed to load as MRModel.");
        }

        reader.jumpTemp(reader.getIndex());
        byte[] temp = reader.readBytes(DUMMY_DATA.length);
        reader.jumpReturn();

        if (Arrays.equals(DUMMY_DATA, temp)) {
            this.dummy = true;
            return;
        }

        reader.jumpTemp(reader.getIndex() + temp.length);
        reader.skipInt(); // File length, including header.
        int flags = reader.readInt();
        reader.jumpReturn();

        if ((flags & FLAG_ANIMATION_FILE) == FLAG_ANIMATION_FILE) {
            resolveAnimatedMOF(reader);
        } else {
            resolveStaticMOF(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        if (this.newModel != null) { // TODO: Toss later!
            try {
                byte[] newModelData = this.newModel.writeDataToByteArray();
                if (newModelData != null) {
                    writer.writeBytes(newModelData);
                    return;
                }
            } catch (Throwable th) {
                Utils.handleError(this.newModel.getLogger(), th, false, "Failed to save as MRModel.");
            }
        }

        if (isDummy()) { // Save dummy mofs.
            writer.writeBytes(DUMMY_DATA);
            return;
        }

        // Save normal mofs.
        if (isAnimatedMOF()) { // If this is an animation, save the animation.
            getAnimatedFile().save(writer);
        } else {
            getStaticFile().save(writer);
        }
    }

    private void resolveStaticMOF(DataReader reader) {
        this.staticFile = new MOFFile(getGameInstance(), this);
        this.staticFile.load(reader);
        if (!isIncomplete()) // We're not incomplete, we don't need to hold onto this value.
            this.completeMOF = null;
    }

    private void resolveAnimatedMOF(DataReader reader) {
        this.animatedFile = new MOFAnimation(getGameInstance(), this);
        this.animatedFile.load(reader);
        if (!isIncomplete()) // We're not incomplete, we don't need to hold onto this value.
            this.completeMOF = null;
    }

    @Override
    public boolean warnIfEndNotReached() {
        return false;
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.GEOMETRIC_SHAPES_32.getFxImage();
    }

    @Override
    public MOFMainController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-mof", new MOFMainController(getGameInstance()), this);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);

        propertyList.add("Type", isDummy() ? "Dummy" : (isIncomplete() ? "Incomplete" : (isAnimatedMOF() ? "Animated" : "Static")));

        if (!isDummy()) {
            MOFFile staticMof = asStaticFile();
            propertyList.add("Parts", staticMof.getParts().size());
            propertyList.add("Animations", getAnimationCount());
            propertyList.add("Texture Animation", staticMof.hasTextureAnimation());
            propertyList.add("Hilites", staticMof.getHiliteCount());
            propertyList.add("Collprims", staticMof.getCollprimCount());
            if (isAnimatedMOF()) {
                MOFAnimation animMof = getAnimatedFile();
                propertyList.add("MOF Count", animMof.getMofCount());
                propertyList.add("Model Set Count", animMof.getModelSetCount());
                propertyList.add("Animation Count", animMof.getModelSet().getCelSet().getCels().size());
                propertyList.add("Interpolation Enabled", animMof.getModelSet().getCelSet().getCels().stream().filter(MOFAnimationCels::isInterpolationEnabled).count());
                propertyList.add("Translation Type", animMof.getTransformType());
                propertyList.add("Start at Frame Zero?", animMof.isStartAtFrameZero());
            }
        }

        return this.newModel.addToPropertyList(propertyList);
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            FXUtils.makePopUp("Your version of JavaFX does not support 3D, so models cannot be previewed.", AlertType.WARNING);
            return;
        }

        if (getVloFile() != null) {
            showEditor3D();
            return;
        }

        // Just grab the first VLO.
        VLOArchive firstVLO = getArchive().findFirstVLO();
        if (firstVLO != null) {
            setVloFile(firstVLO);
            showEditor3D();
        } else {
            getArchive().promptVLOSelection(getTheme(), vlo -> {
                setVloFile(vlo);
                showEditor3D();
            }, false);
        }
    }

    /**
     * Show the 3D editor.
     */
    public void showEditor3D() {
        MOFController controller = new MOFController(getGameInstance(), this);
        Stage stage = getGameInstance().getMainStage();
        if (stage != null)
            controller.setupMofViewer(stage);
    }

    /**
     * Gets the number of animations in this mof. (Does not include texture animation).
     * Get the maximum animation action id.
     * @return maxAnimation
     */
    public int getAnimationCount() {
        if (isAnimatedMOF())
            return getAnimatedFile().getAnimationCount();

        // Flipbook.
        return getStaticFile().getParts().stream()
                .map(MOFPart::getFlipbook)
                .filter(Objects::nonNull)
                .map(MOFFlipbook::getActions)
                .mapToInt(List::size)
                .max().orElse(0);
    }

    /**
     * Get the animation's frame count.
     * @return frameCount
     */
    public int getFrameCount(int animationId) {
        if (isAnimatedMOF() && animationId != -1) // XAR
            return getAnimatedFile().getAnimationById(animationId).getFrameCount();

        // Flipbook and Texture.
        int maxFrame = 0;
        for (MOFPart part : asStaticFile().getParts()) {
            MOFFlipbook flipbook = part.getFlipbook();

            if (animationId == -1) {
                for (MOFPartPolyAnim anim : part.getPartPolyAnims()) {
                    int frameCount = anim.getTotalFrames();
                    if (frameCount > maxFrame)
                        maxFrame = frameCount;
                }
            } else if (flipbook != null) {
                MOFFlipbookAction action = flipbook.getAction(animationId);
                if (action.getFrameCount() > maxFrame)
                    maxFrame = action.getFrameCount();
            }
        }

        return Math.max(maxFrame, 1);
    }

    /**
     * Get the forced static mof file.
     * @return staticMof
     */
    public MOFFile asStaticFile() {
        return isAnimatedMOF() ? getAnimatedFile().getStaticMOF() : getStaticFile();
    }

    /**
     * Gets the name of a particular animation ID, if there is one.
     * @param animationId The animation ID to get.
     * @return name
     */
    public String getName(int animationId) {
        if (animationId == -1)
            return asStaticFile().hasTextureAnimation() ? "Texture Animation" : "No Animation";

        NameBank bank = getConfig().getAnimationBank();
        if (bank == null)
            return (animationId != 0) ? "Animation " + animationId : "Default Animation";

        String bankName = SCUtils.stripWin95(FileUtils.stripExtension(getFileDisplayName()));
        NameBank childBank = bank.getChildBank(bankName);
        return childBank != null ? childBank.getName(animationId) : getConfig().getAnimationBank().getEmptyChildNameFor(animationId, getAnimationCount());
    }

    /**
     * Export this model to .obj
     * @param folder The folder to export to.
     * @param vlo    The graphics pack to export.
     */
    @SneakyThrows
    public void exportObject(File folder, VLOArchive vlo) {
        if (isDummy()) {
            System.out.println("Cannot export dummy MOF.");
            return;
        }

        setVloFile(vlo);
        FileUtils3D.exportMofToObj(asStaticFile(), folder, vlo);

        // Export mm3d too.
        File saveTo = new File(folder, FileUtils.stripExtension(getFileDisplayName()) + ".mm3d");
        FileUtils.deleteFile(saveTo);

        MisfitModel3DObject model = FileUtils3D.convertMofToMisfitModel(this);
        DataWriter writer = new DataWriter(new FileReceiver(saveTo));
        model.save(writer);
        writer.closeReceiver();
    }

    /**
     * Make a TextureMap for this MOF.
     * @return textureMap
     */
    public TextureMap makeTextureMap() {
        return TextureMap.newTextureMap(this, ShadingMode.NO_SHADING);
    }

    /**
     * Gets the override of this MOFHolder, if there is one.
     * @return override
     */
    public MOFHolder getOverride() {
        String mofOverride = getConfig().getMofRenderOverrides().get(getFileDisplayName());
        if (mofOverride != null) {
            MWIResourceEntry entry = getGameInstance().getResourceEntryByName(mofOverride);
            if (entry != null) {
                SCGameFile<?> file = getGameInstance().getGameFile(entry);
                if (file instanceof MOFHolder)
                    return (MOFHolder) file;
            }
        }

        return this;
    }

    /**
     * Make a MOFMesh for this mof.
     * @return mofMesh
     */
    public MOFMesh makeMofMesh() {
        return new MOFMesh(this);
    }

    /**
     * Gets whether this is first and foremost a static MOF.
     */
    public boolean isStaticMOF() {
        return this.staticFile != null;
    }

    /**
     * Get whether this is an animated (XAR) MOF.
     */
    public boolean isAnimatedMOF() {
        return this.animatedFile != null;
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