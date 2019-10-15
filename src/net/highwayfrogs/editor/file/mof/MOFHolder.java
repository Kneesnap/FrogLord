package net.highwayfrogs.editor.file.mof;

import javafx.scene.Node;
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
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
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
public class MOFHolder extends GameFile {
    private boolean dummy; // Is this dummied data?
    @Setter private boolean incomplete; // Some mofs are changed at run-time to share information. This attempts to handle that.
    private boolean staticMOF;
    private boolean animatedMOF;

    private MOFFile staticFile;
    private MOFAnimation animatedFile;

    private transient MAPTheme theme;
    @Setter private transient VLOArchive vloFile;
    private MOFHolder completeMOF; // This is the last MOF which was not incomplete.
    private transient TextureMap textureMap;
    private transient MOFMesh mofMesh;

    public static final int MOF_ID = 3;
    public static final int MAP_MOF_ID = 4;

    public static final int FLAG_ANIMATION_FILE = Constants.BIT_FLAG_3; // This is an animation MOF file.

    private static final Image ICON = loadIcon("swampy");
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
        this.staticMOF = true;
        this.staticFile = new MOFFile(this);
        this.staticFile.load(reader);
        if (!isIncomplete()) // We're not incomplete, we don't need to hold onto this value.
            this.completeMOF = null;
    }

    private void resolveAnimatedMOF(DataReader reader) {
        this.animatedMOF = true;
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
        return null;
    }

    @Override
    public List<Tuple2<String, String>> showWadProperties(WADFile wadFile, WADEntry wadEntry) {
        List<Tuple2<String, String>> list = new ArrayList<>();
        list.add(new Tuple2<>("Type", isDummy() ? "Dummy" : (isIncomplete() ? "Incomplete" : (isAnimatedMOF() ? "Animated" : "Static"))));

        if (!isDummy()) {
            MOFFile staticMof = asStaticFile();
            list.add(new Tuple2<>("Parts", String.valueOf(staticMof.getParts().size())));
            list.add(new Tuple2<>("Texture Animation", String.valueOf(staticMof.hasTextureAnimation())));
            list.add(new Tuple2<>("Hilites", String.valueOf(staticMof.getHiliteCount())));
            list.add(new Tuple2<>("Collprims", String.valueOf(staticMof.getCollprimCount())));
        }

        return list;
    }

    @Override
    public void handleWadEdit(WADFile parent) {
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
     * @return maxFrame
     */
    public int getMaxFrame(int animationId) {
        if (isAnimatedMOF())
            return getAnimatedFile().getAnimationById(animationId).getFrameCount();

        // Flipbook.
        int maxFrame = 0;
        for (MOFPart part : getStaticFile().getParts()) {
            MOFFlipbook flipbook = part.getFlipbook();

            if (flipbook != null) {
                MOFFlipbookAction action = flipbook.getAction(animationId);
                if (action.getFrameCount() > maxFrame)
                    maxFrame = action.getFrameCount();
            }

            for (MOFPartPolyAnim anim : part.getPartPolyAnims()) {
                int frameCount = anim.getTotalFrames();
                if (frameCount > maxFrame)
                    maxFrame = frameCount;
            }
        }

        return maxFrame;
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
     * Gets this MOF's TextureMap.
     * @return textureMap
     */
    public TextureMap getTextureMap() {
        if (this.textureMap == null)
            this.textureMap = TextureMap.newTextureMap(this);
        return this.textureMap;
    }

    /**
     * Get this MOF's mesh.
     * @return mofMesh
     */
    public MOFMesh getMofMesh() {
        if (this.mofMesh == null)
            this.mofMesh = new MOFMesh(this);
        return this.mofMesh;
    }
}