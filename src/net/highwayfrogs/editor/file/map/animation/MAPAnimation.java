package net.highwayfrogs.editor.file.map.animation;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
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
    private int uvFrameCount; // Frames before resetting.
    private int texFrameDuration; // Also known as celPeriod.
    private List<Short> textures = new ArrayList<>(); // Non-remapped texture id array.
    private List<MAPUVInfo> mapUVs = new ArrayList<>();

    private transient MAPFile parentMap;
    private transient int texturePointerAddress;
    private transient int uvPointerAddress;

    public static final ImageFilterSettings PREVIEW_SETTINGS = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(true);

    public static final int BYTE_SIZE = 2 + (7 * Constants.SHORT_SIZE) + (4 * Constants.INTEGER_SIZE);

    public MAPAnimation(MAPFile mapFile) {
        this.parentMap = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        this.uChange = reader.readUnsignedByteAsShort();
        this.vChange = reader.readUnsignedByteAsShort();
        this.uvFrameCount = reader.readUnsignedShortAsInt();
        reader.skipBytes(4); // Four run-time bytes.

        // Texture information.
        int celCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Run-time short.
        int celListPointer = reader.readInt();
        this.texFrameDuration = reader.readUnsignedShortAsInt(); // Frames before resetting.
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
        writer.writeUnsignedShort(this.uvFrameCount);
        writer.writeNull(4); // Run-time.
        writer.writeUnsignedShort(this.textures.size());
        writer.writeNull(Constants.SHORT_SIZE); // Run-time.
        this.texturePointerAddress = writer.writeNullPointer();
        writer.writeUnsignedShort(this.texFrameDuration);
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
        // Common controls
        if (isUV) {
            editor.addDoubleSlider("x Change", toPercent(getUChange()), (value) -> {
                setUChange(toShort(value));
                manager.setupEditor();
            }, 0, 1, true);
            editor.addDoubleSlider("y Change", toPercent(getVChange()), (value) -> {
                setVChange(toShort(value));
                manager.setupEditor();
            }, 0, 1, true);
        }

        TextField uvFrameField = isUV ? editor.addIntegerField("UV Frame Count", getUvFrameCount(), (value) -> {
            setUvFrameCount(value);
            manager.setupEditor();
        }, null) : null;

        TextField speedField = isTexture ? editor.addIntegerField("Speed", getTexFrameDuration(), newVal -> {
            setTexFrameDuration(newVal);
            manager.setupEditor();
        }, null) : null;

        editor.addCheckBox("Map Tool", this.equals(manager.getEditAnimation()), newState -> {
            manager.editAnimation(this);
            manager.setupEditor();
        });

        // Find the base image used to preview.
        VLOArchive vlo = getParentMap().getVlo();
        List<Short> remap = getConfig().getRemapTable(getParentMap().getFileEntry());

        GameImage gameImage = null;
        List<GameImage> images = null;
        boolean hasTextures = isTexture && getTextures().size() > 0;
        boolean hasUV = isUV && getMapUVs().size() > 0;
        boolean hasBoth = (hasTextures && hasUV);

        if (hasTextures) {
            List<GameImage> finalImages = new ArrayList<>(getTextures().size());
            getTextures().forEach(toRemap -> finalImages.add(vlo.getImageByTextureId(remap.get(toRemap))));
            images = finalImages;
            gameImage = images.get(0);
        } else if (hasUV) {
            for (MAPUVInfo uvInfo : getMapUVs()) {
                gameImage = vlo.getImageByTextureId(remap.get(((MAPPolyTexture) uvInfo.getPolygon()).getTextureId()), false);
                if (gameImage != null)
                    break;
            }
        }

        // Create the animation preview.
        if (gameImage != null) {
            editor.addBoldLabel("Preview:");
            AtomicBoolean isAnimating = new AtomicBoolean(false);
            ImageView imagePreview = editor.addCenteredImage(gameImage.toFXImage(PREVIEW_SETTINGS), 150);

            int maxValidTexture = getTextures().size() - 1;
            int maxValidUV = getUvFrameCount() - 1;
            int maxValidBoth = Math.min(maxValidTexture, maxValidUV);
            int usedMax = hasBoth ? maxValidBoth : (hasTextures ? maxValidTexture : maxValidUV);

            final GameImage finalImage = gameImage;
            final List<GameImage> finalImages = images;
            Slider frameSlider = editor.addIntegerSlider("Animation Frame", 0, newFrame -> {
                GameImage useImage = hasTextures ? finalImages.get(newFrame) : finalImage;

                if (hasTextures) { // Apply texture animation.
                    imagePreview.setImage(useImage.toFXImage(PREVIEW_SETTINGS));
                }

                if (hasUV) { // Apply UV animation.
                    int width = useImage.getIngameWidth();
                    int height = useImage.getIngameHeight();
                    FloatMap floatMap = new FloatMap(width, height);

                    double percent = ((newFrame + 1D) / (double) getUvFrameCount());
                    float u = (float) (percent * ((float) this.uChange / 255));
                    float v = (float) (percent * ((float) this.vChange / 255));
                    for (int i = 0; i < width; i++)
                        for (int j = 0; j < height; j++)
                            floatMap.setSamples(i, j, u, v);

                    DisplacementMap displacementMap = new DisplacementMap(floatMap);
                    displacementMap.setWrap(true);
                    imagePreview.setEffect(displacementMap);
                } else {
                    imagePreview.setEffect(null);
                }
            }, 0, usedMax);

            double millisInterval = 1000D / getMWD().getFPS();
            if (isTexture)
                millisInterval *= getTexFrameDuration(); // Apply the duration of each texture.

            Timeline animationTimeline = new Timeline(new KeyFrame(Duration.millis(millisInterval), evt -> {
                int i = (int) frameSlider.getValue() + 1;
                if (i == usedMax + 1)
                    i = 0;
                frameSlider.setValue(i);
            }));
            animationTimeline.setCycleCount(Timeline.INDEFINITE);

            imagePreview.setOnMouseClicked(evt -> {
                isAnimating.set(!isAnimating.get());
                boolean playNow = isAnimating.get();
                if (playNow) {
                    animationTimeline.play();
                } else {
                    animationTimeline.pause();
                }

                frameSlider.setDisable(playNow);
                if (speedField != null)
                    speedField.setDisable(playNow);
                if (uvFrameField != null)
                    uvFrameField.setDisable(playNow);
            });

            if (hasUV && !hasTextures) { // Allow changing the texture of all polygons affected by a UV animation.
                editor.addBoldLabel("UV Texture:");
                editor.addButton("Change Texture", () -> {
                    vlo.promptImageSelection(newImage -> {
                        int newIndex = remap.indexOf(newImage.getTextureId());
                        if (newIndex == -1) {
                            Utils.makePopUp("This texture is not present in the remap.", AlertType.ERROR);
                            return;
                        }

                        for (MAPUVInfo mapuvInfo : getMapUVs())
                            ((MAPPolyTexture) mapuvInfo.getPolygon()).setTextureId((short) newIndex);
                        manager.setupEditor();
                        manager.getBaseController().getMapUIController().getGeometryManager().refreshView();
                    }, false);
                });
            }
        }

        // Setup editor.
        if (isTexture && images != null) {
            editor.addBoldLabel("Textures:");
            for (int i = 0; i < images.size(); i++) {
                final int tempIndex = i;
                GameImage image = images.get(i);

                Image scaledImage = Utils.toFXImage(image.toBufferedImage(VLOArchive.ICON_EXPORT), false);
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
                getTextures().add(getTextures().isEmpty() ? (short) 0 : getTextures().get(getTextures().size() - 1));
                manager.setupEditor();
            });
        }
    }

    private static double toPercent(int value) {
        return value / 255.0;
    }

    private static short toShort(double percent) {
        return (short) Math.round(percent * 255);
    }
}
