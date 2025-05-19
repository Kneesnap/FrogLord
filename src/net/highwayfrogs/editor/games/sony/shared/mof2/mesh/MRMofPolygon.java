package net.highwayfrogs.editor.games.sony.shared.mof2.mesh;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXTextureShader;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimationEntry;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh.MRMofShadedTextureManager;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.Arrays;

/**
 * Represents a map polygon seen in the MOF file format.
 * The polygons use consistent ordering between colors, uvs, and vertices. Textures are consistent too except for FT4, which reorders the data.
 * Assuming viewing from the default perspective, a quad is ordered like this:
 * 1---2
 * |  /|
 * | / |
 * |/  |
 * 3---4
 * This order matches how the PSX GPU processes a quad, first using vertices 1-2-3, then 2-3-4, according to <a href="https://psx-spx.consoledev.net/graphicsprocessingunitgpu/">this link</a>.
 * UVs are based on a different corner though. UVs start from the bottom left corner, instead of the top corner. Meaning 0, 0 (Origin) is the bottom left corner.
 * To counteract this, we can do 1.0 - v to get the texture coordinates relative to the top left corner.
 * Created by Kneesnap on 2/19/2025.
 */
@Getter
public class MRMofPolygon extends SCGameData<SCGameInstance> {
    private final MRMofPart mofPart;
    private final MRMofPolygonType polygonType;
    private final int[] vertices;
    private final int[] environmentNormals;
    private final int[] normals;
    private final SCByteTextureUV[] textureUvs;
    @Setter private short textureId = -1;
    private final CVector color = CVector.makeColorFromRGB(PSXTextureShader.UNSHADED_COLOR_ARGB);

    // The last address which the polygon was written to.
    private transient int lastReadAddress = -1;
    private transient int lastWriteAddress = -1;

    private static final SCByteTextureUV[] EMPTY_UV_ARRAY = new SCByteTextureUV[0];
    private static final int[] EMPTY_NORMAL_ARRAY = new int[0];

    public MRMofPolygon(MRMofPart mofPart, MRMofPolygonType polygonType) {
        super(mofPart != null ? mofPart.getGameInstance() : null);
        this.mofPart = mofPart;
        this.polygonType = polygonType;
        this.vertices = new int[polygonType.getVertexCount()];
        this.environmentNormals = polygonType.getEnvironmentNormalCount() > 0 ? new int[polygonType.getEnvironmentNormalCount()] : EMPTY_NORMAL_ARRAY;
        this.normals = polygonType.getNormalCount() > 0 ? new int[polygonType.getNormalCount()] : EMPTY_NORMAL_ARRAY;
        this.textureUvs = polygonType.isTextured() ? new SCByteTextureUV[polygonType.getVertexCount()] : EMPTY_UV_ARRAY;
        Arrays.fill(this.vertices, -1);
        Arrays.fill(this.environmentNormals, -1);
        Arrays.fill(this.normals, -1);
        for (int i = 0; i < this.textureUvs.length; i++)
            this.textureUvs[i] = new SCByteTextureUV();
    }

    @Override
    public ILogger getLogger() {
        String name = "MRMofPolygon{" + this.polygonType + "," + NumberUtils.toHexString(this.lastReadAddress) + "}";
        if (this.mofPart != null) {
            return new AppendInfoLoggerWrapper(this.mofPart.getLogger(), name, AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
        } else {
            return new LazyInstanceLogger(getGameInstance(), name);
        }
    }

    @Override
    public void load(DataReader reader) {
        this.lastReadAddress = reader.getIndex();

        // Read core data.
        readSwappedArray(reader, this.vertices);
        readSwappedArray(reader, this.environmentNormals);
        readSwappedArray(reader, this.normals);

        // Read texture data.
        if (this.polygonType.isTextured()) {
            if (this.polygonType == MRMofPolygonType.FT4) { // Not sure why this is different.
                readTextureFormatFT4(reader);
            } else {
                readTextureFormat(reader);
            }
        } else {
            this.textureId = (short) -1;
        }

        // Read color.
        reader.alignRequireEmpty(Constants.INTEGER_SIZE); // Padding.
        this.color.load(reader);
    }

    private void readTextureFormat(DataReader reader) {
        this.textureUvs[0].load(reader);
        short clutId = reader.readShort();
        if (clutId != 0)
            throw new RuntimeException("MRMofPolygon had clut id which was not zero! (" + clutId + ").");

        this.textureUvs[1].load(reader);
        short textureId = reader.readShort();
        if (textureId != 0)
            throw new RuntimeException("MOFPolyTexture had texture id which was not zero! (" + textureId + ").");

        if (this.textureUvs.length > 3) {
            this.textureUvs[3].load(reader); // This swap can be seen in MR_MESH.C/MRUpdateViewportMeshInstancesAnimatedPolys.
            this.textureUvs[2].load(reader);
        } else {
            this.textureUvs[2].load(reader);
        }

        this.textureId = reader.readShort();
    }

    private void readTextureFormatFT4(DataReader reader) {
        this.textureId = reader.readShort();
        this.textureUvs[0].load(reader);
        short clutId = reader.readShort();
        this.textureUvs[1].load(reader);
        short zeroTextureId = reader.readShort();
        this.textureUvs[3].load(reader); // This swap can be seen in MR_MESH.C/MRUpdateViewportMeshInstancesAnimatedPolys.
        this.textureUvs[2].load(reader);

        if (clutId != 0)
            throw new RuntimeException("MRMofPolygon(FT4) had clut id which was not zero! (" + clutId + ").");
        if (zeroTextureId != 0)
            throw new RuntimeException("MRMofPolygon(FT4) had unused texture id which was not zero! (" + zeroTextureId + ").");
    }

    @Override
    public void save(DataWriter writer) {
        this.lastWriteAddress = writer.getIndex();
        writeSwappedArray(writer, this.vertices);
        writeSwappedArray(writer, this.environmentNormals);
        writeSwappedArray(writer, this.normals);

        // Write texture data.
        if (this.polygonType.isTextured()) {
            if (this.textureId < 0) {
                getLogger().severe("A textured MRMofPolygon had an invalid texture ID! (%d) This polygon will not render correctly in-game and may even cause crashes!");
            } else {
                GameImage image = getTexture(null, 0); // Get the texture without any animation.
                if (image == null)
                    getLogger().severe("A textured MRMofPolygon had an unresolvable texture ID! (%d) This polygon will not render correctly in-game and may even cause crashes!");
            }

            if (this.polygonType == MRMofPolygonType.FT4) { // Not sure why this is different.
                writeTextureFormatFT4(writer);
            } else {
                writeTextureFormat(writer);
            }
        }

        // Write color.
        writer.align(Constants.INTEGER_SIZE); // Padding.
        this.color.save(writer);
    }

    private void writeTextureFormat(DataWriter writer) {
        this.textureUvs[0].save(writer);
        writer.writeUnsignedShort(0); // The Clut ID is always zero.
        this.textureUvs[1].save(writer);
        writer.writeUnsignedShort(0); // The "Texture ID" is always zero.

        if (this.textureUvs.length > 3) {
            this.textureUvs[3].save(writer); // This swap can be seen in MR_MESH.C/MRUpdateViewportMeshInstancesAnimatedPolys.
            this.textureUvs[2].save(writer);
        } else {
            this.textureUvs[2].save(writer);
        }

        writer.writeShort(this.textureId); // Image ID
    }

    private void writeTextureFormatFT4(DataWriter writer) {
        writer.writeShort(this.textureId); // This is technically the "image ID".
        this.textureUvs[0].save(writer);
        writer.writeUnsignedShort(0); // Clut ID is always zero.
        this.textureUvs[1].save(writer);
        writer.writeUnsignedShort(0); // The "texture ID" is always zero.
        this.textureUvs[3].save(writer); // This swap can be seen in MR_MESH.C/MRUpdateViewportMeshInstancesAnimatedPolys.
        this.textureUvs[2].save(writer);
    }

    /**
     * Gets the active texture on this polygon.
     */
    public GameImage getTexture(MRMofTextureAnimation animation, int animationTick) {
        if (!this.polygonType.isTextured())
            return null;

        short globalTextureId = this.textureId;
        if (this.mofPart != null && animation != null) {
            MRMofTextureAnimationEntry animationEntry = animation.getEntryForFrame(animationTick);
            if (animationEntry != null)
                globalTextureId = animationEntry.getGlobalImageId();
        }

        // If the mof has a VLO associated, first try getting the image from that VLO.
        VLOArchive vlo;
        if (this.mofPart != null && (vlo = this.mofPart.getParentMof().getModel().getVloFile()) != null) {
            GameImage image = vlo.getImageByTextureId(globalTextureId, false);
            if (image != null)
                return image;
        }

        return getArchive().getImageByTextureId(globalTextureId);
    }

    /**
     * Get the number of vertices active for this polygon.
     */
    public int getVertexCount() {
        return this.polygonType.getVertexCount();
    }

    /**
     * Test if the image is semi-transparent.
     * Equivalent to the behavior seen in MRWritePartPrimCodes@MR_MOF.C
     */
    public boolean isSemiTransparent() {
        if (getGameInstance().isMediEvil())
            return false; // MediEvil seems not to call 'MRPatchMOFTranslucency',
        if (!this.polygonType.isTextured())
            return false;

        // TODO: Some terrain is transparent which shouldn't be (Think sky zone) Actually, in prototypes, these do actually appear transparent, just of very high brightness.
        // TODO: crap, I think we broke stuff under water.
        // Frogger is the only other game confirmed to call 'MRPatchMOFTranslucency', although I did not test which build that was added.
        // However, even in Frogger, it has been observed that in builds such as PSX Alpha (slugs in SWP1.MAP), the transparency is not always respected.
        // The first build seen with semi-transparent texture patching is PSX Build 11. PSX Build 8 is the most recently seen build before that, so we'll check from there.
        if (getGameInstance().isFrogger() && !((FroggerGameInstance) getGameInstance()).getVersionConfig().isAtOrBeforeBuild8())
            return false;

        GameImage image = getTexture(null, 0);
        return image != null && image.testFlag(GameImage.FLAG_TRANSLUCENT);
    }

    /**
     * Creates a texture shade definition for this polygon.
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(MRModelMesh mesh, boolean shadingEnabled, MRMofTextureAnimation animation, int animationTick) {
        SCByteTextureUV[] uvs = null;
        if (this.polygonType.isTextured()) {
            uvs = new SCByteTextureUV[this.textureUvs.length];
            for (int i = 0; i < uvs.length; i++)
                uvs[i] = this.textureUvs[i].clone();
        }

        // Determine the texture.
        ITextureSource textureSource = getTexture(animation, animationTick);

        // Clone colors.
        CVector[] colors = new CVector[1];
        colors[0] = shadingEnabled || !this.polygonType.isTextured() ? this.color.clone() : PSXTextureShader.UNSHADED_COLOR;

        MRMofShadedTextureManager shadedTextureManager = mesh != null ? mesh.getShadedTextureManager() : null;
        return new PSXShadeTextureDefinition(shadedTextureManager, this.polygonType.getInternalType(), textureSource, colors, uvs, isSemiTransparent(), true);
    }

    private static void readSwappedArray(DataReader reader, int[] array) {
        if (array.length == 4) {
            array[0] = reader.readUnsignedShortAsInt();
            array[1] = reader.readUnsignedShortAsInt();
            array[3] = reader.readUnsignedShortAsInt();
            array[2] = reader.readUnsignedShortAsInt();
        } else {
            for (int i = 0; i < array.length; i++)
                array[i] = reader.readUnsignedShortAsInt();
        }
    }

    private static void writeSwappedArray(DataWriter writer, int[] array) {
        if (array.length == 4) {
            writer.writeUnsignedShort(array[0]);
            writer.writeUnsignedShort(array[1]);
            writer.writeUnsignedShort(array[3]);
            writer.writeUnsignedShort(array[2]);
        } else {
            for (int i = 0; i < array.length; i++)
                writer.writeUnsignedShort(array[i]);
        }
    }
}
