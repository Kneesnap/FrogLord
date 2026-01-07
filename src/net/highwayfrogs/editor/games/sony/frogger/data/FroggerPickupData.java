package net.highwayfrogs.editor.games.sony.frogger.data;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameObject;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents pickup data. (Definitions of fly/bug sprite textures/sizes)
 * Created by Kneesnap on 3/26/2019.
 */
@Getter
public class FroggerPickupData extends SCGameData<FroggerGameInstance> {
    private final FroggerFlyScoreType flyScoreType;
    private int sphereGlowColor = 0x7F7F7F; // BGR 24 bit color.
    private int spriteSize = 1 << 16; // Fixed point multiple. (1 << 16 === 0x10000 == 1.0F). Applies to both X and Y.
    private final List<PickupAnimationFrame> frames = new ArrayList<>();

    public FroggerPickupData(FroggerGameInstance instance, FroggerFlyScoreType flyScoreType) {
        super(instance);
        this.flyScoreType = flyScoreType;
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    public void load(DataReader reader) {
        if (isOldPickupFormat()) {
            // TODO: Properly support the format. (This doesn't work on PC... is the issue that the PC version doesn't properly identify pointers?)
            long nextTexture = reader.readUnsignedIntAsLong();
            while (reader.hasMore() && !getGameInstance().getBmpTexturePointers().contains(nextTexture)) // This intends to skip the first texture.
                nextTexture = reader.readUnsignedIntAsLong();

            reader.setIndex(reader.getIndex() - Constants.INTEGER_SIZE);
        } else {
            this.sphereGlowColor = reader.readInt();
            this.spriteSize = reader.readInt();
        }

        // Read animation frames. (Image pointers)
        this.frames.clear();
        long imagePointer;
        while (reader.hasMore() && (imagePointer = reader.readUnsignedIntAsLong()) != 0)
            if (!isOldPickupFormat() || getGameInstance().getBmpTexturePointers().contains(imagePointer))
                this.frames.add(new PickupAnimationFrame(this, imagePointer));
    }

    @Override
    public void save(DataWriter writer) {
        if (isOldPickupFormat())
            return; // We didn't track the data, so we shouldn't save invalid data.

        writer.writeInt(this.sphereGlowColor);
        writer.writeInt(this.spriteSize);

        // Write texture pointers (animation frames).
        for (int i = 0; i < this.frames.size(); i++)
            writer.writeUnsignedInt(this.frames.get(i).texturePointer);
        writer.writeNullPointer(); // Ends with an empty.
    }

    /**
     * Gets the sprite scale size as a floating point value.
     */
    public float getSpriteSizeAsFloat() {
        return DataUtils.fixedPointIntToFloatNBits(this.spriteSize, 16);
    }

    /**
     * Test if this contains the old pickup data format.
     */
    public boolean isOldPickupFormat() {
        return getConfig().isAtOrBeforeBuild24();
    }

    /**
     * Represents an animated frame of a pickup.
     */
    public static class PickupAnimationFrame extends SCGameObject<FroggerGameInstance> {
        private final FroggerPickupData pickupData;
        private final long texturePointer;
        private boolean resolvedTextures;
        private VloImage resolvedImage;
        private BufferedImage awtPreviewImage;
        private Image fxPreviewImage;
        private PhongMaterial highlightedMaterial;
        private PhongMaterial normalMaterial;

        private static final int FLY_SPRITE_IMAGE_OPTIONS = VloImage.DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS;
        public static final float ENTITY_FLY_SPRITE_SIZE = 4F;
        public static final float ENTITY_FLY_SPRITE_SCALE_SIZE = 4F * ENTITY_FLY_SPRITE_SIZE; // Chosen by experimenting until I found one I was happy with.
        public static final TriangleMesh ENTITY_FLY_SPRITE_MESH = Scene3DUtils.createSpriteMesh(ENTITY_FLY_SPRITE_SIZE);

        public PickupAnimationFrame(FroggerPickupData pickupData, long texturePointer) {
            super(pickupData.getGameInstance());
            this.pickupData = pickupData;
            this.texturePointer = texturePointer;
        }

        private void tryResolveTextures() {
            if (this.resolvedTextures)
                return;

            this.resolvedTextures = true;

            // Identify the image used for this frame.
            this.resolvedImage = getGameInstance().getImageFromPointer(this.texturePointer);
            if (this.resolvedImage == null)
                return; // This can be null in the EU PS1 demo. (It may not have properly been setup when compiled.)

            // Create a preview image from the resolved image.
            // This attempts to apply glow effect, transparency, etc.
            this.awtPreviewImage = this.resolvedImage.toBufferedImage(FLY_SPRITE_IMAGE_OPTIONS);

            // Scale the image to avoid transparency problems at the edges.
            int newWidth = (int) (this.awtPreviewImage.getWidth() * ENTITY_FLY_SPRITE_SCALE_SIZE);
            int newHeight = (int) (this.awtPreviewImage.getHeight() * ENTITY_FLY_SPRITE_SCALE_SIZE);
            BufferedImage scaledPreviewImage = ImageUtils.resizeImage(this.awtPreviewImage, newWidth, newHeight, true);
            this.fxPreviewImage = FXUtils.toFXImage(scaledPreviewImage, false);
        }

        /**
         * Gets or creates a material for the frame.
         * @param highlight whether we want the highlighted material or not
         * @return fxMaterial
         */
        public PhongMaterial getFxMaterial(boolean highlight) {
            tryResolveTextures();
            if (this.resolvedImage == null)
                throw new RuntimeException("Cannot get material, since the texture could not be resolved.");

            if (highlight) {
                if (this.highlightedMaterial == null)
                    this.highlightedMaterial = Scene3DUtils.updateHighlightMaterial(null, this.awtPreviewImage);

                return this.highlightedMaterial;
            } else {
                if (this.normalMaterial == null)
                    this.normalMaterial = Scene3DUtils.makeUnlitSharpMaterial(this.fxPreviewImage);

                return this.normalMaterial;
            }
        }

        /**
         * Applies the animation frame to the provided mesh view.
         * @param meshView the mesh view to apply the animation frame display settings to
         * @param highlight whether the mesh view should use the highlighted material
         * @return appliedSuccessfully
         */
        public boolean applyToMeshView(MeshView meshView, boolean highlight) {
            tryResolveTextures();
            if (this.resolvedImage == null)
                return false; // There was no image, so we must abort.

            meshView.setMesh(ENTITY_FLY_SPRITE_MESH);
            meshView.setMaterial(getFxMaterial(highlight));

            // Properly size via scaling.
            float scaleSize = this.pickupData.getSpriteSizeAsFloat();
            double flySpriteSizeSq = (ENTITY_FLY_SPRITE_SIZE * ENTITY_FLY_SPRITE_SIZE);
            Scene3DUtils.setNodeScale(meshView, (double) (scaleSize * this.resolvedImage.getUnpaddedWidth()) / flySpriteSizeSq, (double) (scaleSize * this.resolvedImage.getUnpaddedHeight()) / flySpriteSizeSq, 1D);
            return true;
        }

        /**
         * Gets the image used for this frame, if there is one.
         */
        public VloImage getImage() {
            tryResolveTextures();
            return this.resolvedImage;
        }
    }
}