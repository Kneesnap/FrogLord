package net.highwayfrogs.editor.file.mof;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShaderMode;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimationCels;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbook;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbookAction;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.gui.editor.MOFController;
import net.highwayfrogs.editor.gui.editor.MOFMainController;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.utils.FileUtils3D;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Holds MOF files.
 * Created by Kneesnap on 2/25/2019.
 */
@Getter
@Setter
public class MOFHolder extends GameFile {
    private boolean dummy; // Is this dummied data?
    private boolean incomplete; // Some mofs are changed at run-time to share information. This attempts to handle that.

    private MOFFile staticFile;
    private MOFAnimation animatedFile;

    private transient MAPTheme theme;
    private transient VLOArchive vloFile;
    private MOFHolder completeMOF; // This is the last MOF which was not incomplete.

    public static final int MOF_ID = 3;
    public static final int MAP_MOF_ID = 4;

    public static final int FLAG_ANIMATION_FILE = Constants.BIT_FLAG_3; // This is an animation MOF file.

    private static final Image ICON = loadIcon("model");
    public static final byte[] DUMMY_DATA = "DUMY".getBytes();

    public MOFHolder(MAPTheme theme, MOFHolder lastCompleteMOF) {
        this.theme = theme;
        this.completeMOF = lastCompleteMOF;
    }

    @Override
    public void load(DataReader reader) {
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

        if (getMaxAnimation() > 1) //TODO: TOSS
            System.out.println(getFileEntry().getDisplayName() + ": " + getMaxAnimation());
    }

    @Override
    public void save(DataWriter writer) {
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
        this.staticFile = new MOFFile(this);
        this.staticFile.load(reader);
        if (!isIncomplete()) // We're not incomplete, we don't need to hold onto this value.
            this.completeMOF = null;
    }

    private void resolveAnimatedMOF(DataReader reader) {
        this.animatedFile = new MOFAnimation(this);
        this.animatedFile.load(reader);
        if (!isIncomplete()) // We're not incomplete, we don't need to hold onto this value.
            this.completeMOF = null;
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new MOFMainController(), "mofmain", this);
    }

    @Override
    public List<Tuple2<String, String>> showWadProperties(WADFile wadFile, WADEntry wadEntry) {
        List<Tuple2<String, String>> list = new ArrayList<>();
        list.add(new Tuple2<>("Type", isDummy() ? "Dummy" : (isIncomplete() ? "Incomplete" : (isAnimatedMOF() ? "Animated" : "Static"))));

        if (!isDummy()) {
            MOFFile staticMof = asStaticFile();
            list.add(new Tuple2<>("Parts", String.valueOf(staticMof.getParts().size())));
            list.add(new Tuple2<>("Parts", String.valueOf(staticMof.getParts().size())));
            list.add(new Tuple2<>("Texture Animation", String.valueOf(staticMof.hasTextureAnimation())));
            list.add(new Tuple2<>("Hilites", String.valueOf(staticMof.getHiliteCount())));
            list.add(new Tuple2<>("Collprims", String.valueOf(staticMof.getCollprimCount())));
            if (isAnimatedMOF()) {
                list.add(new Tuple2<>("Animation Count", String.valueOf(getAnimatedFile().getModelSet().getCelSet().getCels().size())));
                list.add(new Tuple2<>("Interpolation Enabled", String.valueOf(getAnimatedFile().getModelSet().getCelSet().getCels().stream().filter(MOFAnimationCels::isInterpolationEnabled).count())));
                list.add(new Tuple2<>("Translation Type", getAnimatedFile().getTransformType().name()));
            }
        }

        return list;
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            Utils.makePopUp("Your version of JavaFX does not support 3D, so models cannot be previewed.", AlertType.WARNING);
            return;
        }

        if (getVloFile() != null) {
            MainController.MAIN_WINDOW.openEditor(new MOFController(), this);
            return;
        }

        getMWD().promptVLOSelection(getTheme(), vlo -> {
            setVloFile(vlo);
            MainController.MAIN_WINDOW.openEditor(new MOFController(), this);
        }, false);
    }

    /**
     * Get the maximum animation action id.
     * @return maxAnimation
     */
    public int getMaxAnimation() {
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

        String bankName = Utils.stripWin95(Utils.stripExtension(getFileEntry().getDisplayName()));
        NameBank childBank = getConfig().getAnimationBank().getChildBank(bankName);
        return childBank != null ? childBank.getName(animationId) : getConfig().getAnimationBank().getEmptyChildNameFor(animationId, getMaxAnimation());
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
        File saveTo = new File(folder, Utils.stripExtension(getFileEntry().getDisplayName()) + ".mm3d");
        Utils.deleteFile(saveTo);

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
        return TextureMap.newTextureMap(this, ShaderMode.NO_SHADING);
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
     * Get whether or not this is an animated (XAR) MOF.
     */
    public boolean isAnimatedMOF() {
        return this.animatedFile != null;
    }
}