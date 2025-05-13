package net.highwayfrogs.editor.games.sony.frogger.map.data.animation;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerUIMapAnimationManager;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.MathUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a map animation in Frogger.
 * Created by Kneesnap on 8/27/2018.
 */
public class FroggerMapAnimation extends SCGameData<FroggerGameInstance> {
    @Getter private final FroggerMapFile mapFile;
    @Getter @Setter @NonNull private FroggerMapAnimationType type = FroggerMapAnimationType.UV;
    @Getter @Setter private byte deltaU; // Change in texture U (Each frame)
    @Getter @Setter private byte deltaV; // Change in texture V (Each frame)
    @Getter private int uvFrameCount = 1; // Frames before resetting.
    @Getter private int framesPerTexture = 1; // Also known as celPeriod, this is the number of frames before an animated texture advances to the next texture.
    @Getter private final List<Short> unusedTextureIds = new ArrayList<>(); // Contains texture ids which aren't used by any animation.
    @Getter private final List<Short> textureIds = new ArrayList<>(); // Non-remapped texture id array.
    @Getter private final List<FroggerMapAnimationTargetPolygon> targetPolygons = new ArrayList<>();
    private transient int textureIdListPointerAddress = -1;
    @Getter private transient int targetPolygonListPointerAddress = -1;

    private static final Short DEFAULT_TEXTURE_ID = (short) -1;
    public static final int BYTE_SIZE = 2 + (7 * Constants.SHORT_SIZE) + (4 * Constants.INTEGER_SIZE);
    public static final ITextureSource UNKNOWN_TEXTURE_SOURCE = UnknownTextureSource.YELLOW_INSTANCE; // Indicates couldn't find an animated texture.

    public FroggerMapAnimation(FroggerMapFile mapFile) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        this.deltaU = reader.readByte();
        this.deltaV = reader.readByte();
        this.uvFrameCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Runtime.

        // Texture information.
        int celCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Runtime.
        this.textureIdListPointerAddress = reader.readInt();
        this.framesPerTexture = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Runtime.
        this.type = FroggerMapAnimationType.getType(reader.readUnsignedShortAsInt());
        int polygonCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.POINTER_SIZE); // Runtime texture pointer.
        this.targetPolygonListPointerAddress = reader.readInt();

        // Populate texture ID list for future reading.
        this.textureIds.clear();
        this.unusedTextureIds.clear();
        if (this.type == FroggerMapAnimationType.BOTH || this.type == FroggerMapAnimationType.TEXTURE)
            for (int i = 0; i < celCount; i++)
                this.textureIds.add(DEFAULT_TEXTURE_ID);

        // Populate polygon target list for future reading.
        this.targetPolygons.clear();
        for (int i = 0; i < polygonCount; i++)
            this.targetPolygons.add(new FroggerMapAnimationTargetPolygon(this));
    }

    /**
     * Reads the texture list from the current position.
     * @param reader the reader to read it from
     */
    @SuppressWarnings("Java8ListReplaceAll")
    public void readTextureIdList(DataReader reader) {
        if (this.textureIdListPointerAddress == 0 || this.textureIds.isEmpty() || this.type == FroggerMapAnimationType.UV) {
            this.textureIdListPointerAddress = -1;
            return; // Empty.
        }

        if (this.textureIdListPointerAddress <= 0)
            throw new RuntimeException("Cannot read texture id list, the pointer " + NumberUtils.toHexString(this.textureIdListPointerAddress) + " is invalid.");

        // Read up to the desired texture id list.
        while (this.textureIdListPointerAddress > reader.getIndex())
            this.unusedTextureIds.add(reader.readShort());

        // Validate index.
        if (this.mapFile.isExtremelyEarlyMapFormat()) {
            reader.setIndex(this.textureIdListPointerAddress);
        } else {
            requireReaderIndex(reader, this.textureIdListPointerAddress, "Expected texture id list");
        }

        // Read the texture id list.
        for (int i = 0; i < this.textureIds.size(); i++)
            this.textureIds.set(i, reader.readShort());

        this.textureIdListPointerAddress = -1;
    }

    /**
     * Reads the target polygon list from the current position.
     * @param reader the reader to read it from
     */
    public void readTargetPolygonList(DataReader reader) {
        if (this.targetPolygonListPointerAddress == 0 || this.targetPolygons.isEmpty()) {
            this.targetPolygonListPointerAddress = -1;
            return;
        }

        if (this.targetPolygonListPointerAddress <= 0)
            throw new RuntimeException("Cannot target polygon list, the pointer " + NumberUtils.toHexString(this.targetPolygonListPointerAddress) + " is invalid.");

        requireReaderIndex(reader, this.targetPolygonListPointerAddress, "Expected target polygon list");
        for (int i = 0; i < this.targetPolygons.size(); i++)
            this.targetPolygons.get(i).load(reader);

        this.targetPolygonListPointerAddress = -1;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.deltaU);
        writer.writeByte(this.deltaV);
        writer.writeUnsignedShort(this.uvFrameCount);
        writer.writeNull(Constants.INTEGER_SIZE); // Runtime.
        writer.writeUnsignedShort(this.textureIds.size());
        writer.writeNull(Constants.SHORT_SIZE); // Runtime.
        this.textureIdListPointerAddress = writer.writeNullPointer();
        writer.writeUnsignedShort(this.framesPerTexture);
        writer.writeNull(Constants.SHORT_SIZE); // Runtime.
        writer.writeUnsignedShort((this.type != null ? this.type : FroggerMapAnimationType.UV).getFlagBitMask());
        writer.writeUnsignedShort(this.targetPolygons.size());
        writer.writeNullPointer(); // Runtime texture pointer.
        this.targetPolygonListPointerAddress = writer.writeNullPointer();
    }

    /**
     * Writes the texture list to the current position.
     * @param writer The writer to write to.
     */
    public void writeTextureIdList(DataWriter writer) {
        if (this.type == FroggerMapAnimationType.UV) { // No texture.
            this.textureIdListPointerAddress = -1;
            return;
        }

        if (this.textureIdListPointerAddress <= 0)
            throw new RuntimeException("Cannot write texture id list, the pointer " + NumberUtils.toHexString(this.textureIdListPointerAddress) + " is invalid.");

        // Write texture IDs.
        writer.writeAddressTo(this.textureIdListPointerAddress);
        for (int i = 0; i < this.textureIds.size(); i++)
            writer.writeShort(this.textureIds.get(i));
        for (int i = 0; i < this.unusedTextureIds.size(); i++)
            writer.writeShort(this.unusedTextureIds.get(i));

        this.textureIdListPointerAddress = -1;
    }

    /**
     * Writes the target polygon list to the current position.
     * @param writer the writer to write it to
     */
    public void writeTargetPolygonList(DataWriter writer) {
        if (this.targetPolygonListPointerAddress <= 0)
            throw new RuntimeException("Cannot write target polygon list, the pointer " + NumberUtils.toHexString(this.targetPolygonListPointerAddress) + " is invalid.");

        // Write target polygons.
        writer.writeAddressTo(this.targetPolygonListPointerAddress);
        for (int i = 0; i < this.targetPolygons.size(); i++)
            this.targetPolygons.get(i).save(writer);

        this.targetPolygonListPointerAddress = -1;
    }

    /**
     * Gets the index of this animation into the animation list.
     */
    public int getAnimationIndex() {
        return this.mapFile != null && this.mapFile.getAnimationPacket().isActive() ? this.mapFile.getAnimationPacket().getAnimations().lastIndexOf(this) : -1;
    }

    /**
     * Gets logger information.
     */
    public String getLoggerInfo() {
        return this.mapFile != null ? this.mapFile.getFileDisplayName() + "|Animation{" + getAnimationIndex() + "}" : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerMapAnimation::getLoggerInfo, this);
    }

    /**
     * Gets the x pixel offset for the given frame of animation.
     * @param frame the frame to get the offset from
     * @return xPixelOffset
     */
    public int getOffsetX(int frame) {
        return this.type.hasUVAnimation() && this.uvFrameCount > 0 && frame >= 0 ? (((frame + 1) % this.uvFrameCount) * this.deltaU) : 0;
    }

    /**
     * Gets the y pixel offset for the given frame of animation.
     * @param frame the frame to get the offset from
     * @return yPixelOffset
     */
    public int getOffsetY(int frame) {
        return this.type.hasUVAnimation() && this.uvFrameCount > 0 && frame >= 0 ? (((frame + 1) % this.uvFrameCount) * this.deltaV) : 0;
    }

    /**
     * Gets the game image at the given frame.
     * Returns null if there is no texture at the given frame.
     * @param frame the frame to get the game image from
     * @return textureAtFrame, if there is one
     */
    public GameImage getTextureAtFrame(int frame) {
        if (frame < 0 || !this.type.hasTextureAnimation() || this.textureIds.isEmpty() || this.framesPerTexture <= 0)
            return null;

        // Resolve global texture ID.
        int frameTexture = ((frame + 1) / this.framesPerTexture) % this.textureIds.size();
        short localTextureId = this.textureIds.get(frameTexture);
        return getImageByLocalID(localTextureId);
    }

    private GameImage getImageByLocalID(short localTextureId) {
        TextureRemapArray remap = this.mapFile.getTextureRemap();
        Short remappedTextureId = remap != null ? remap.getRemappedTextureId(localTextureId) : null;
        if (remappedTextureId == null)
            return null;

        // Find image by the ID.
        VLOArchive vlo = this.mapFile.getVloFile();
        GameImage gameImage = vlo != null ? vlo.getImageByTextureId(remappedTextureId) : null;
        if (gameImage == null) // If it wasn't found in the
            gameImage = getArchive().getImageByTextureId(remappedTextureId);

        return gameImage;
    }

    /**
     * Gets the image representing the animation at a given frame.
     * @param textureSource the default texture source. Used when the animation doesn't have a texture to apply itself
     * @param frame the frame of animation to get
     * @param applyTextureUvOffset whether the resulting texture should be shifted by the texture uv offset
     * @return animationFrameImage
     */
    public BufferedImage getAnimationFrame(ITextureSource textureSource, int frame, boolean applyTextureUvOffset) {
        return getAnimationFrame(null, textureSource, frame, false, applyTextureUvOffset);
    }

    /**
     * Gets the image representing the animation at a given frame.
     * @param polygon the polygon to apply the animation to. If null is supplied, shading must be disabled.
     * @param frame the frame of animation to get
     * @param shadingEnabled whether shading should be enabled
     * @param applyTextureUvOffset whether the resulting texture should be shifted by the texture uv offset
     * @return animationFrameImage
     */
    public BufferedImage getAnimationFrame(FroggerMapPolygon polygon, int frame, boolean shadingEnabled, boolean applyTextureUvOffset) {
        return getAnimationFrame(polygon, polygon != null ? polygon.getTexture() : null, frame, shadingEnabled, applyTextureUvOffset);
    }

    /**
     * Gets the image representing the animation at a given frame.
     * @param polygon the polygon to apply the animation to. If null is supplied, shading must be disabled.
     * @param textureSource the default texture source. Used when the animation doesn't have a texture to apply itself
     * @param frame the frame of animation to get
     * @param shadingEnabled whether shading should be enabled
     * @param applyTextureUvOffset whether the resulting texture should be shifted by the texture uv offset
     * @return animationFrameImage
     */
    private BufferedImage getAnimationFrame(FroggerMapPolygon polygon, ITextureSource textureSource, int frame, boolean shadingEnabled, boolean applyTextureUvOffset) {
        if (polygon == null && shadingEnabled)
            throw new IllegalArgumentException("Shading cannot be enabled when no polygon is provided, since the polygon is where the shading data is obtained from.");

        // Find the base image used to preview.
        GameImage gameImage = null;
        if (this.type.hasTextureAnimation() && this.textureIds.size() > 0 && this.framesPerTexture > 0) {
            TextureRemapArray remap = this.mapFile.getTextureRemap();

            // Resolve global texture ID.
            int frameTexture = ((frame + 1) / this.framesPerTexture) % this.textureIds.size();
            short localTextureId = this.textureIds.get(frameTexture);
            Short remappedTextureId = remap != null ? remap.getRemappedTextureId(localTextureId) : null;

            // Find image by the ID.
            if (remappedTextureId != null) {
                VLOArchive vlo = this.mapFile.getVloFile();
                textureSource = gameImage = vlo != null ? vlo.getImageByTextureId(remappedTextureId) : null;
                if (textureSource == null) // If it wasn't found in the
                    textureSource = gameImage = getArchive().getImageByTextureId(remappedTextureId);
            }
        } else if (this.type.hasUVAnimation()) {
            if (textureSource == null && this.targetPolygons.size() > 0) {
                textureSource = gameImage = this.targetPolygons.stream()
                        .map(testPolygon -> testPolygon.getPolygon() != null ? testPolygon.getPolygon().getTexture() : null)
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
            }
        } else {
            // It's not textured, nor does it have UVs, return the base polygon image.
            if (textureSource != null) {
                return textureSource.makeImage();
            } else if (polygon != null) {
                return polygon.createPolygonShadeDefinition(null, shadingEnabled, null, -1).makeImage();
            } else {
                return UNKNOWN_TEXTURE_SOURCE.makeImage();
            }
        }

        // Apply the texture UV offset.
        BufferedImage resultImage = (textureSource != null ? textureSource : UNKNOWN_TEXTURE_SOURCE).makeImage();
        if (applyTextureUvOffset && this.type.hasUVAnimation() && textureSource != null && this.uvFrameCount > 0) { // Apply UV animation.
            BufferedImage baseImage = resultImage;
            int uvFrame = ((frame + 1) % this.uvFrameCount);
            int xOffset = (this.deltaU * uvFrame);
            int yOffset = (this.deltaV * uvFrame);

            // Apply the image shift.
            if (xOffset != 0 || yOffset != 0) {
                int width = baseImage.getWidth();
                int height = baseImage.getHeight();
                resultImage = new BufferedImage(width, height, baseImage.getType());
                Graphics2D graphics = resultImage.createGraphics();

                // Draw the main image shifted.
                graphics.drawImage(baseImage, -xOffset, -yOffset, width, height, null);

                // Draw the supplemental images.
                // At maximum there can be four images drawn to a single position.
                int adjustedWidth = xOffset > 0 ? width : -width;
                int adjustedHeight = yOffset > 0 ? height : -height;
                if (xOffset != 0) // Draw the left/right image.
                    graphics.drawImage(baseImage, adjustedWidth - xOffset, -yOffset, width, height, null);
                if (yOffset != 0) // Draw the up/down image.
                    graphics.drawImage(baseImage, -xOffset, adjustedHeight - yOffset, width, height, null);
                if (xOffset != 0 && yOffset != 0) // Draw the diagonal image.
                    graphics.drawImage(baseImage, adjustedWidth - xOffset, adjustedHeight - yOffset, width, height, null);

                // Done!
                graphics.dispose();
            }
        }

        // Apply shading. (If enabled)
        if (shadingEnabled)
            resultImage = polygon.createPolygonShadeDefinition(null, true, null, -1).makeImage(resultImage, null);

        return gameImage != null && resultImage != null ? ImageWorkHorse.trimEdges(gameImage, resultImage) : resultImage;
    }

    /**
     * Setup an animation editor.
     * @param editor The editor to setup under.
     */
    public void setupEditor(FroggerUIMapAnimationManager manager, GUIEditorGrid editor) {
        // Animation Type:
        editor.addEnumSelector("Type", getType(), FroggerMapAnimationType.values(), false, newType -> {
            setType(newType);
            manager.updateEditor(); // Refresh UI, since different UI elements may be visible now.
        });

        // Common controls.
        if (this.type.hasUVAnimation()) {
            editor.addSignedByteField("Pixels Per Frame (X)", this.deltaU, newDeltaU -> Math.abs(newDeltaU) <= 32, newDeltaU -> {
                setDeltaU(newDeltaU);
                manager.updatePreviewImage(); // Update the preview image.
            });
            editor.addSignedByteField("Pixels Per Frame (Y)", this.deltaV, newDeltaV -> Math.abs(newDeltaV) <= 32, newDeltaV -> {
                setDeltaV(newDeltaV);
                manager.updatePreviewImage(); // Update the preview image.
            });

            editor.addSignedIntegerField("UV Frame Count", this.uvFrameCount,
                    newUvFrameCount -> newUvFrameCount > 0 && Math.abs(newUvFrameCount * this.deltaU) < Byte.MAX_VALUE && Math.abs(newUvFrameCount * this.deltaV) < Byte.MAX_VALUE,
                    newUvFrameCount -> {
                setUvFrameCount(newUvFrameCount);
                manager.updatePreviewUI(); // Update the preview UI, since the UV frame count may have changed.
                manager.updateEditor(); // Need to refresh the editor to change the other uv stuff.
            });
        }

        if (this.type.hasTextureAnimation()) {
            editor.addSignedIntegerField("Frames per Texture", this.framesPerTexture, newFrameCount -> newFrameCount > 0, newFrameCount -> {
                setFramesPerTexture(newFrameCount);
                manager.updatePreviewUI(); // Update the preview UI, since the texture frame count may have changed.
                manager.updatePreviewImage(); // Update the preview (but don't refresh editor).
            });
        }

        // Find the base image used to preview.
        VLOArchive vlo = this.mapFile.getVloFile();
        TextureRemapArray remap = this.mapFile.getTextureRemap();

        // Setup editor.
        if (this.type.hasTextureAnimation()) {
            editor.addBoldLabel("Texture List:");
            for (int i = 0; i < this.textureIds.size(); i++) {
                final int tempIndex = i;
                short textureId = this.textureIds.get(i);
                GameImage image = getImageByLocalID(textureId);

                Image scaledImage = FXUtils.toFXImage(image != null ? image.toBufferedImage(VLOArchive.ICON_EXPORT) : UnknownTextureSource.MAGENTA_INSTANCE.makeImage(), false);
                ImageView view = editor.setupNode(new ImageView(scaledImage));
                view.setFitWidth(20);
                view.setFitHeight(20);

                view.setOnMouseClicked(evt -> remap.askUserToSelectImage(vlo, false, newImage -> {
                    int newIndex = remap.getRemapIndex(newImage.getTextureId());
                    if (newIndex < 0) {
                        FXUtils.makePopUp("The selected image is not part of the map's texture remap! (" + newImage.getTextureId() + ")", AlertType.ERROR);
                        return;
                    }

                    this.textureIds.set(tempIndex, (short) newIndex);
                    view.setImage(FXUtils.toFXImage(newImage.toBufferedImage(VLOArchive.ICON_EXPORT), false)); // Update the texture displayed in the UI.
                    manager.updatePreviewImage(); // Update the animation preview.
                    if (tempIndex == 0) // Refresh the texture displayed in the animation list.
                        manager.refreshList();
                }));

                editor.setupSecondNode(new Button("Remove " + i + (image != null ? " (" + image.getLocalImageID() + "/" + image.getTextureId() + ")" : "")), false).setOnAction(evt -> {
                    this.textureIds.remove(tempIndex);
                    manager.updatePreviewUI(); // Update the preview UI, since the texture frame count may have changed.
                    manager.updateEditor(); // Refresh the editor to remove the animation.
                    if (tempIndex == 0) // Refresh the texture displayed in the animation list.
                        manager.refreshList();
                });

                editor.addRow(25);
            }

            editor.addButton("Add Texture", () -> remap.askUserToSelectImage(vlo, false, newImage -> {
                int newIndex = remap.getRemapIndex(newImage.getTextureId());
                if (newIndex < 0) {
                    FXUtils.makePopUp("The selected image is not part of the map's texture remap! (" + newImage.getTextureId() + ")", AlertType.ERROR);
                    return;
                }

                this.textureIds.add((short) newIndex);
                manager.updatePreviewUI(); // Update the preview UI, since the texture frame count may have changed.
                manager.updateEditor(); // Refresh the editor to show the new texture.
                if (this.textureIds.size() == 1) // Refresh the texture displayed in the animation list.
                    manager.refreshList();
            })).setDisable(vlo == null);
        } else if (this.type.hasUVAnimation()) { // Allow changing the texture of all polygons affected by a UV animation.
            editor.addBoldLabel("UV Texture:");
            editor.addButton("Change Texture", () -> remap.askUserToSelectImage(vlo, false, newImage -> {
                int newIndex = remap.getRemapIndex(newImage.getTextureId());
                if (newIndex < 0) {
                    FXUtils.makePopUp("The selected image is not part of the map's texture remap! (" + newImage.getTextureId() + ")", AlertType.ERROR);
                    return;
                }

                manager.updatePreviewImage(); // Refresh 3D window.

                // Apply texture ID to all targetted polygons.
                FroggerMapMesh mapMesh = manager.getMesh();
                mapMesh.pushBatchOperations();
                mapMesh.getTextureAtlas().startBulkOperations();
                for (FroggerMapAnimationTargetPolygon targetPolygon : this.targetPolygons) {
                    FroggerMapPolygon polygon = targetPolygon.getPolygon();
                    if (polygon != null && polygon.getTextureId() != newIndex) {
                        polygon.setTextureId((short) newIndex);
                        mapMesh.getShadedTextureManager().updatePolygon(polygon);
                    }
                }
                mapMesh.getTextureAtlas().endBulkOperations();
                mapMesh.pushBatchOperations();
            }));
        }
    }

    /**
     * Sets the UV animation frame count.
     * @param newUvFrameCount the new UV animation frame count.
     */
    public void setUvFrameCount(int newUvFrameCount) {
        if (this.type.hasUVAnimation() && (Math.abs(newUvFrameCount * this.deltaU) >= Byte.MAX_VALUE || Math.abs(newUvFrameCount * this.deltaV) >= Byte.MAX_VALUE || Math.abs(newUvFrameCount) > Byte.MAX_VALUE))
            throw new IllegalArgumentException("The provided uvFrameCount (" + newUvFrameCount + ") would cause overflow!");

        this.uvFrameCount = newUvFrameCount;
    }

    /**
     * Set the duration (in frames) each texture in the texture animation will be shown for
     * @param framesPerTexture the new frames per texture value
     */
    public void setFramesPerTexture(int framesPerTexture) {
        if (framesPerTexture <= 0 && this.type.hasTextureAnimation())
            throw new IllegalArgumentException("framesPerTexture must be >= zero. (Got: " + framesPerTexture + ")");

        this.framesPerTexture = framesPerTexture;
    }

    /**
     * Gets the number of frames it takes for the animation to complete.
     * This can return 0 if the animation is instant.
     */
    public int getFrameCount() {
        if (this.type.hasUVAnimation() && this.type.hasTextureAnimation()) {
            int textureFrameCount = this.framesPerTexture * this.textureIds.size();
            int greatestCommonFactor = MathUtils.gcd(this.uvFrameCount, textureFrameCount);

            // Calculate the smallest number divisible by both the UV frame count and texture frame count.
            // Eg: Get the minimum number of frames necessary to complete an animation with both types of animations smoothly.
            return greatestCommonFactor != 0 ? ((this.uvFrameCount * textureFrameCount) / greatestCommonFactor) : 0;
        } else if (this.type.hasTextureAnimation()) {
            return this.framesPerTexture * this.textureIds.size();
        } else if (this.type.hasUVAnimation()) {
            return this.uvFrameCount;
        } else {
            throw new RuntimeException("Unsupported animation type: " + this.type);
        }
    }
}