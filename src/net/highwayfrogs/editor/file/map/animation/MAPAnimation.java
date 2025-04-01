package net.highwayfrogs.editor.file.map.animation;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationTargetPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationType;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the MAP_ANIM struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
@Setter
public class MAPAnimation extends SCGameData<FroggerGameInstance> {
    private FroggerMapAnimationType type = FroggerMapAnimationType.UV;
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
        super(mapFile.getGameInstance());
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
        this.type = FroggerMapAnimationType.getType(reader.readUnsignedShortAsInt());
        int polygonCount = reader.readUnsignedShortAsInt();
        reader.skipInt(); // Texture pointer. Generated at run-time.

        if (getType() == FroggerMapAnimationType.TEXTURE || getType() == FroggerMapAnimationType.BOTH) {
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
        writer.writeUnsignedShort(getType().getFlagBitMask());
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
     * Converts the animation from the old format to the new format.
     * @param newMapFile the new map file to use
     * @param convertedPolygons the converted polygon lookup table
     * @return convertedAnimation
     */
    public FroggerMapAnimation convertToNewFormat(@NonNull FroggerMapFile newMapFile, @NonNull Map<MAPPrimitive, FroggerMapPolygon> convertedPolygons) {
        FroggerMapAnimation newAnimation = new FroggerMapAnimation(newMapFile);
        newAnimation.setType(this.type);
        newAnimation.setDeltaU(DataUtils.unsignedShortToByte(this.uChange));
        newAnimation.setDeltaV(DataUtils.unsignedShortToByte(this.vChange));
        newAnimation.setUvFrameCount(this.uvFrameCount);
        newAnimation.setFramesPerTexture(this.texFrameDuration);
        newAnimation.getTextureIds().addAll(this.textures);

        for (MAPUVInfo mapuvInfo : this.mapUVs) {
            FroggerMapPolygon convertedPolygon = convertedPolygons.get(mapuvInfo.getPolygon());
            if (convertedPolygon == null)
                throw new RuntimeException("Could not find the polygon attached to the animation target. (Was it actually converted to the new format?)");

            newAnimation.getTargetPolygons().add(new FroggerMapAnimationTargetPolygon(newAnimation, convertedPolygon));
        }

        return newAnimation;
    }
}