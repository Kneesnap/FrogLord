package net.highwayfrogs.editor.file.map.animation;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.AnimationManager;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents the MAP_ANIM struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
@Setter
public class MAPAnimation extends GameObject {
    private MAPAnimationType type = MAPAnimationType.UV;
    private short uChange; // Delta U (Each frame)
    private short vChange;
    private int uvDuration; // Frames before resetting.
    private int texDuration; // Also known as celPeriod.
    private List<Short> textures = new ArrayList<>(); // Non-remapped texture id array.
    private List<MAPUVInfo> mapUVs = new ArrayList<>();

    private transient MAPFile parentMap;
    private transient int texturePointerAddress;
    private transient int uvPointerAddress;

    public static final int BYTE_SIZE = 2 + (7 * Constants.SHORT_SIZE) + (4 * Constants.INTEGER_SIZE);

    public MAPAnimation(MAPFile mapFile) {
        this.parentMap = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        this.uChange = reader.readUnsignedByteAsShort();
        this.vChange = reader.readUnsignedByteAsShort();
        this.uvDuration = reader.readUnsignedShortAsInt();
        reader.skipBytes(4); // Four run-time bytes.

        // Texture information.
        int celCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Run-time short.
        int celListPointer = reader.readInt();
        this.texDuration = reader.readUnsignedShortAsInt(); // Frames before resetting.
        reader.skipShort(); // Run-time variable.
        this.type = MAPAnimationType.getType(reader.readUnsignedShortAsInt());
        int polygonCount = reader.readUnsignedShortAsInt();
        reader.skipInt(); // Texture pointer. Generated at run-time.

        if (getType() == MAPAnimationType.TEXTURE || getType() == MAPAnimationType.BOTH) {
            reader.jumpTemp(celListPointer);
            for (int i = 0; i < celCount; i++)
                textures.add(reader.readShort());
            reader.jumpReturn();
        }

        reader.jumpTemp(reader.readInt()); // Map UV Pointer.
        for (int i = 0; i < polygonCount; i++) {
            MAPUVInfo info = new MAPUVInfo(getParentMap());
            info.load(reader);
            mapUVs.add(info);
        }

        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte(this.uChange);
        writer.writeUnsignedByte(this.vChange);
        writer.writeUnsignedShort(this.uvDuration);
        writer.writeNull(4); // Run-time.
        writer.writeUnsignedShort(this.textures.size());
        writer.writeNull(Constants.SHORT_SIZE); // Run-time.
        this.texturePointerAddress = writer.writeNullPointer();
        writer.writeUnsignedShort(this.texDuration);
        writer.writeShort((short) 0); // Runtime.
        writer.writeUnsignedShort(getType().getFlag());
        writer.writeUnsignedShort(getMapUVs().size());
        writer.writeInt(0); // Run-time.
        this.uvPointerAddress = writer.writeNullPointer();
    }

    /**
     * Called after animations are saved, this saves texture ids.
     * @param writer The writer to write to.
     */
    public void writeTextures(DataWriter writer) {
        Utils.verify(getTexturePointerAddress() > 0, "There is no saved address to write the texture pointer at.");

        int textureLocation = writer.getIndex();
        writer.jumpTemp(this.texturePointerAddress);
        writer.writeInt(textureLocation);
        writer.jumpReturn();

        for (short texId : getTextures())
            writer.writeShort(texId);

        this.texturePointerAddress = 0;
    }

    /**
     * Called after textures are written, this saves Map UVs.
     * @param writer The writer to write to.
     */
    public void writeMapUVs(DataWriter writer) {
        Utils.verify(getUvPointerAddress() > 0, "There is no saved address to write the uv pointer at.");

        int uvLocation = writer.getIndex();
        writer.jumpTemp(this.uvPointerAddress);
        writer.writeInt(uvLocation);
        writer.jumpReturn();

        for (MAPUVInfo mapUV : getMapUVs())
            mapUV.save(writer);

        this.uvPointerAddress = 0;
    }

    /**
     * Setup an animation editor.
     * @param editor The editor to setup under.
     */
    public void setupEditor(AnimationManager manager, GUIEditorGrid editor) {
        boolean isBoth = getType() == MAPAnimationType.BOTH;
        boolean isTexture = getType() == MAPAnimationType.TEXTURE || isBoth;
        boolean isUV = getType() == MAPAnimationType.UV || isBoth;

        editor.addEnumSelector("Type", getType(), MAPAnimationType.values(), false, newValue -> {
            setType(newValue);
            manager.setupEditor(); // Change what's visible.
        });

        if (isUV) {
            editor.addShortField("u Frame Change", getUChange(), this::setUChange, null);
            editor.addShortField("v Frame Change", getVChange(), this::setVChange, null);
            editor.addIntegerField("Total Frames", getUvDuration(), this::setUvDuration, null);
        }

        TextField speedField = isTexture ? editor.addIntegerField("Speed", getTexDuration(), newVal -> {
            setTexDuration(newVal);
            manager.setupEditor();
        }, null) : null;

        editor.addCheckBox("Map Tool", this.equals(manager.getEditAnimation()), newState -> {
            manager.editAnimation(this);
            manager.setupEditor();
        });

        if (!isTexture)
            return; // I'd love to have a UV viewer, but... it isn't linked to specific textures so we could only describe the motion of the texture. Unless, the user could choose the texture, or it went with the texture used in the most places.

        // Setup.
        VLOArchive vlo = getParentMap().getVlo();
        List<Short> remap = getConfig().getRemapTable(getParentMap().getFileEntry());
        List<GameImage> images = new ArrayList<>(getTextures().size());
        getTextures().forEach(toRemap -> images.add(vlo.getImageByTextureId(remap.get(toRemap))));

        // Setup viewer.
        if (getTextures().size() > 0) {
            editor.addBoldLabel("Preview:");
            AtomicBoolean isAnimating = new AtomicBoolean(false);
            ImageView animView = editor.addCenteredImage(images.get(0).toFXImage(MWDFile.VLO_ICON_SETTING), 150);
            Slider frameSlider = editor.addIntegerSlider("Texture", 0, newFrame ->
                    animView.setImage(images.get(newFrame).toFXImage(MWDFile.VLO_ICON_SETTING)), 0, getTextures().size() - 1);

            Timeline animationTimeline = new Timeline(new KeyFrame(Duration.millis(getTexDuration() * 1000D / getMWD().getFPS()), evt -> {
                int i = (int) frameSlider.getValue() + 1;
                if (i == images.size())
                    i = 0;
                frameSlider.setValue(i);
            }));
            animationTimeline.setCycleCount(Timeline.INDEFINITE);

            animView.setOnMouseClicked(evt -> {
                isAnimating.set(!isAnimating.get());
                boolean playNow = isAnimating.get();
                if (playNow) {
                    animationTimeline.play();
                } else {
                    animationTimeline.pause();
                }

                frameSlider.setDisable(playNow);
                speedField.setDisable(playNow);
            });
        }

        // Setup editor.
        editor.addBoldLabel("Textures:");
        for (int i = 0; i < images.size(); i++) {
            final int tempIndex = i;
            GameImage image = images.get(i);

            Image scaledImage = Utils.toFXImage(image.toBufferedImage(VLOArchive.ICON_EXPORT), true);
            ImageView view = editor.setupNode(new ImageView(scaledImage));
            view.setFitWidth(20);
            view.setFitHeight(20);

            view.setOnMouseClicked(evt -> vlo.promptImageSelection(newImage -> {
                int newIndex = remap.indexOf(newImage.getTextureId());
                Utils.verify(newIndex >= 0, "Failed to find remap for texture id: %d!", newImage.getTextureId());
                getTextures().set(tempIndex, (short) newIndex);
                manager.setupEditor();
            }, false));

            editor.setupSecondNode(new Button("Remove #" + image.getLocalImageID() + " (" + image.getTextureId() + ")"), false).setOnAction(evt -> {
                getTextures().remove(tempIndex);
                manager.setupEditor();
            });

            editor.addRow(25);
        }

        editor.addButton("Add Texture", () -> {
            getTextures().add(getTextures().isEmpty() ? 0 : getTextures().get(getTextures().size() - 1));
            manager.setupEditor();
        });
    }
}
