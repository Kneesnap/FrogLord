package net.highwayfrogs.editor.games.sony.frogger.map.data;

import javafx.scene.control.Alert.AlertType;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketLight;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapLightManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapLightManager.FroggerMapLightPreview;
import net.highwayfrogs.editor.games.sony.shared.misc.MRLightType;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

/**
 * Holds lighting data, or the "LIGHT" struct in mapdisp.H
 * Created by Kneesnap on 8/24/2018.
 */
public class FroggerMapLight extends SCGameData<FroggerGameInstance> {
    @Getter private final FroggerMapFile mapFile;
    @Setter private int parentId; // I believe this value was originally intended for use outside of LIGHT_TYPE_STATIC, for example, specifying which entity the light is attached to. However, this value seems to be complete garbage (unallocated data?) as I've been unable to find any meaningful pattern.
    @Getter @Setter @NonNull private MRLightType lightType = MRLightType.AMBIENT;
    @Getter @Setter private int color; // BbGgRr
    @Getter private final SVector position = new SVector(); // This is probably the place in the world the light is placed in the editor.
    @Getter private final SVector direction = new SVector(); // When this is AMBIENT, I think this is arbitrary. When this is parallel, it seems to be a 12bit normalized direction vector. When this is point it is unused.
    @Getter @Setter private int attribute0; // Umbra angle is this. Larger value seems to incidate a larger circular radius (If shined directly at a flat plane)
    @Getter @Setter private int attribute1; // Seems unused in all cases?
    private boolean enabled = true;

    // attribute0 seems to be zero for PARALLEL.
    // attribute1 seems to be zero for PARALLEL.

    // attribute0 is sometimes zero for SPOT, other times not.
    // attribute1 is usually zero for SPOT, other times not.
    // attribute1 is zero for AMBIENT, PARALLEL.

    // AMBIENT -> NO EXTRA DATA NECESSARY
    // PARALLEL -> I DON'T THINK THERE'S ANY MORE DATA NECESSARY.
    //

    // AMBIENT:
    // priority = 255, parentId = GARBAGE?
    // position = INVALID, direction = INVALID
    // attribute0 = ??? (Usually 20000k ish), attribute1 = ??? (Near 100)
    // NOTE!!! I believe all data for AMBIENT is garbage except color & priority.

    // PARALLEL:
    // priority = 131, parentId = GARBAGE?
    // position = INVALID, direction = VALID (Seems normalized) (Position may not actually be invalid, since different parallel lights have different but close-by positions.)
    // attribute0 = ??? (Usually 20000k ish), attribute1 = ??? (Near 100)
    // NOTE!!!! CAV3.MAP has all zeros except color, direction, and priority. I believe ALL the other data is garbage for this light type.

    // POINT:
    // priority = 130, parentId = GARBAGE?
    // position = VALID, direction = ?
    // attribute0 = falloff min?, attribute1 = falloff max?

    // SPOT:
    // priority = 128, parentId = Garbage?
    // position = VALID (VERY HIGH UP), direction = VALID (MAY NOT BE NORMALIZED? IT USUALY IS THO Does this suggest it's something other than direction?)
    // attribute0 = Umbra Angle (Near 300 usually. Above 256), attribute1 = ??? (Around 80) ->> IDEA: This could be outerAngle, or falloff.

    public FroggerMapLight(FroggerMapFile mapFile) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
    }

    public FroggerMapLight(FroggerMapFile mapFile, MRLightType apiLightType) {
        this(mapFile);
        this.lightType = apiLightType;
    }

    @Override
    public void load(DataReader reader) {
        short lightType = reader.readUnsignedByteAsShort();
        if (lightType != 1) // These other types have never been seen in any build, and do nothing in the game code.
            throw new RuntimeException("The FroggerMapLight type was not LIGHT_TYPE_STATIC (1), instead it was " + lightType + ".");

        short priority = reader.readUnsignedByteAsShort();
        this.parentId = reader.readUnsignedShortAsInt();
        this.lightType = MRLightType.getType(reader.readUnsignedByteAsShort());
        reader.alignRequireEmpty(Constants.INTEGER_SIZE); // Padding
        this.color = reader.readInt();
        this.position.loadWithPadding(reader);
        this.direction.loadWithPadding(reader);
        this.attribute0 = reader.readUnsignedShortAsInt();
        this.attribute1 = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(2 * Constants.POINTER_SIZE); // Runtime pointers.

        // Validate priority.
        short expectedPriorityEnabled = getPriority(this.lightType, true);
        short expectedPriorityDisabled = getPriority(this.lightType, false);
        if (priority == expectedPriorityEnabled) {
            this.enabled = true;
        } else if (priority == expectedPriorityDisabled) {
            this.enabled = false;
        } else {
            getLogger().warning("Expected a priority value of %d or %d for %s, but we read a priority of %d.", expectedPriorityEnabled, expectedPriorityDisabled, this.lightType,  priority);
        }

        if (this.lightType == MRLightType.POINT)
            getLogger().warning("Found a point light! These have not been observed to exist in Frogger!");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) 1); // Light type. Always LIGHT_TYPE_STATIC.
        writer.writeUnsignedByte(getPriority(this.lightType, this.enabled));
        writer.writeUnsignedShort(this.parentId);
        writer.writeUnsignedByte((short) this.lightType.getBitFlagMask());
        writer.align(Constants.INTEGER_SIZE); // Padding
        writer.writeInt(this.color);
        this.position.saveWithPadding(writer);
        this.direction.saveWithPadding(writer);
        writer.writeUnsignedShort(this.attribute0);
        writer.writeUnsignedShort(this.attribute1);
        writer.writeNullPointer();
        writer.writeNullPointer();
    }

    /**
     * Gets the index of the light within the map.
     */
    public int getLightIndex() {
        FroggerMapFilePacketLight lightPacket = this.mapFile.getLightPacket();
        return lightPacket.getLoadingIndex(lightPacket.getLights(), this);
    }

    /**
     * Gets the logger information string for this light.
     */
    public String getLoggerInfo() {
        return this.mapFile != null ? this.mapFile.getFileDisplayName() + "|FroggerMapLight{" + getLightIndex() + "}" : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerMapLight::getLoggerInfo, this);
    }

    /**
     * Creates a copy of the map light attached to the given map file.
     * @param mapFile the map file to clone the map file for
     * @return clonedLightObject
     */
    public FroggerMapLight clone(FroggerMapFile mapFile) {
        return DataUtils.cloneSerializableObject(this, new FroggerMapLight(mapFile));
    }

    /**
     * Makes a lighting editor.
     * @param editor Lighting editor.
     */
    public void makeEditor(GUIEditorGrid editor, FroggerUIMapLightManager lightManager, FroggerMapLightPreview lightPreview) {
        if (this.lightType == MRLightType.AMBIENT || this.lightType == MRLightType.PARALLEL) {
            editor.addBoldLabel("This light only impacts entities.");
            editor.addNormalLabel("The original map editor (Mappy) would");
            editor.addNormalLabel(" change the terrain vertex colors");
            editor.addNormalLabel(" based on this light, but due to");
            editor.addNormalLabel(" technical reasons, FrogLord cannot.");
            editor.addSeparator();
        } else {
            editor.addBoldLabel("This light will do nothing ingame.");
            editor.addNormalLabel(this.lightType + " lights only worked in Mappy,");
            editor.addNormalLabel(" the original Frogger map editor.");
            editor.addNormalLabel("Mappy would color the terrain based");
            editor.addNormalLabel(" on this light, but FrogLord cannot.");
            editor.addSeparator();
        }

        // Type.
        editor.addEnumSelector("Light Type", this.lightType, MRLightType.values(), false, newType -> {
            if (this.lightType != newType) {
                if (newType == MRLightType.AMBIENT) {
                    if (lightManager.getMap().getLightPacket().hasAmbientLight()) {
                        FXUtils.makePopUp("This map already contains an ambient light, so a new one cannot be added.", AlertType.ERROR);
                        lightManager.updateEditor(); // Ensure the editor shows the previous value instead of the new one.
                        return;
                    }
                } else if (newType == MRLightType.PARALLEL) {
                    if (lightManager.getMap().getLightPacket().hasMaxNumberOfParallelLights()) {
                        FXUtils.makePopUp("This map already contains the maximum number of parallel lights.", AlertType.ERROR);
                        lightManager.updateEditor(); // Ensure the editor shows the previous value instead of the new one.
                        return;
                    }
                }
            }

            this.lightType = newType;
            lightPreview.updateLight();
            lightManager.updateEditor(); // Rebuild the editor since the light type may impact which fields are used, such as the attribute names.
        });

        // Light color.
        int rgbColor = ColorUtils.toRGB(ColorUtils.fromBGR(this.color));
        editor.addColorPicker("Color", 25, rgbColor, newColor -> {
            this.color = ColorUtils.toBGR(ColorUtils.fromRGB(newColor));
            lightPreview.updateLight();
        });

        // TODO: Restrict editor to show (but not allow edit) of stuff which isn't compatible with the given light type.
        editor.addFloatVector("Position", this.position, lightPreview::updateLight, lightManager.getController());
        editor.addFloatVector("Direction", this.direction, lightPreview::updateLight, lightManager.getController(), 12); // TODO: Proper editor. Perhaps have a mesh node which can be calculated as an arrow from one position to another.
        editor.addUnsignedShortField(getAttribute0Name(), this.attribute0, newValue -> this.attribute0 = newValue).setDisable(this.lightType != MRLightType.POINT && this.lightType != MRLightType.SPOT);
        editor.addUnsignedShortField("Attribute 1", this.attribute1, newValue -> this.attribute1 = newValue).setDisable(true);
    }

    /**
     * Gets the name of the attribute 0 value based on the type.
     */
    public String getAttribute0Name() {
        switch (this.lightType) {
            case POINT:
                return "Falloff";
            case SPOT:
                return "Umbra Angle";
            default:
                return "Attribute 0";
        }
    }

    /**
     * Gets the expected priority value for the given light type.
     * @param lightType the MRLightType to get the priority from
     * @return priority
     */
    public static short getPriority(MRLightType lightType, boolean enabled) {
        // This light data structure is unchanged from pre-recode.
        // And it has some weird design remnants such as the "priority".
        // The pre-recode game seemed to want this value as some kind of LOD option.
        // Bit 7 indicates that the light is ON. Bits 0, 1, and 2 indicate different "detail level", which I believe is based on the camera height.
        // So, it sounds like a LOD feature. However, the retail game definitely doesn't have anything like this, and it seems like we can match this value by generating it.
        // This value does not need to be exposed to FrogLord users since it's effectively useless.

        switch (lightType) {
            case AMBIENT:
                return (short) 0b11111111; // 255
            case PARALLEL:
                return (short) 0b10000011; // 131
            case POINT:
                return (short) 0b10000010; // 130
            case SPOT:
                return enabled ? (short) 0b10000000 : (short) 4; // 128 or 4 when disabled.
            default:
                throw new RuntimeException("Unsupported MRLightType: " + lightType);
        }
    }
}