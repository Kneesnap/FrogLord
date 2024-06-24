package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import javafx.scene.AmbientLight;
import javafx.scene.LightBase;
import javafx.scene.PointLight;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerLightManager;
import net.highwayfrogs.editor.games.sony.shared.misc.MRLightType;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds light sources for an old Frogger map.
 * Created by Kneesnap on 12/12/2023.
 */
@Getter
public class OldFroggerMapLightPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "LITE";
    private final List<OldFroggerMapLight> lights = new ArrayList<>();

    public OldFroggerMapLightPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.lights.clear();
        int lightCount = reader.readInt();
        for (int i = 0; i < lightCount; i++) {
            OldFroggerMapLight newLight = new OldFroggerMapLight(getParentFile().getGameInstance());
            newLight.load(reader);
            this.lights.add(newLight);
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.lights.size());
        for (int i = 0; i < this.lights.size(); i++)
            this.lights.get(i).save(writer);
    }

    @Override
    public int getKnownStartAddress() {
        OldFroggerMapGraphicalHeaderPacket graphicalPacket = getParentFile().getGraphicalHeaderPacket();
        return graphicalPacket != null ? graphicalPacket.getLightChunkAddress() : -1;
    }

    /**
     * Represents a map light in an old Frogger map.
     */
    @Getter
    public static class OldFroggerMapLight extends SCGameData<OldFroggerGameInstance> {
        private OldFroggerMapLightType type = OldFroggerMapLightType.STATIC;
        private short priority; // can bin low priority lights (detail) top bit is ON/OFF
        private int parentId; // (depends on above)
        private MRLightType apiType = MRLightType.AMBIENT; // (point/parallel/etc.)
        private final CVector color = new CVector();
        private final SVector position = new SVector();
        private final SVector direction = new SVector();
        private int attrib1; // eg falloff if point, umbra angle if
        private int attrib2; // spot

        public OldFroggerMapLight(OldFroggerGameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.type = OldFroggerMapLightType.values()[reader.readUnsignedByteAsShort()];
            this.priority = reader.readUnsignedByteAsShort();
            this.parentId = reader.readUnsignedShortAsInt();
            this.apiType = MRLightType.getType(reader.readUnsignedByteAsShort());
            reader.alignRequireEmpty(4);
            this.color.load(reader);
            this.position.loadWithPadding(reader);
            this.direction.loadWithPadding(reader);
            this.attrib1 = reader.readUnsignedShortAsInt();
            this.attrib2 = reader.readUnsignedShortAsInt();
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE * 2);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedByte((short) (this.type != null ? this.type.ordinal() : 0));
            writer.writeUnsignedByte(this.priority);
            writer.writeUnsignedShort(this.parentId);
            writer.writeUnsignedByte((short) (this.apiType != null ? this.apiType.getBitFlagMask() : 0));
            writer.align(4);
            this.color.save(writer);
            this.position.saveWithPadding(writer);
            this.direction.saveWithPadding(writer);
            writer.writeUnsignedShort(this.attrib1);
            writer.writeUnsignedShort(this.attrib2);
            writer.writeNullPointer(); // Runtime pointer.
            writer.writeNullPointer(); // Runtime pointer.
        }

        /**
         * Creates the editor UI for the light.
         * @param manager The manager to create the UI for.
         * @param editor  The editor to use to create the UI.
         */
        public void setupEditor(OldFroggerLightManager manager, GUIEditorGrid editor) {
            editor.addEnumSelector("Light Type", this.type, OldFroggerMapLightType.values(), false, newValue -> this.type = newValue).setDisable(true); // Disabled since there's no reason to change this.
            editor.addUnsignedByteField("Priority:", this.priority, newPriority -> this.priority = newPriority);
            editor.addUnsignedFixedShort("Parent ID", this.parentId, newValue -> this.parentId = newValue, 1);
            editor.addEnumSelector("API Light", this.apiType, MRLightType.values(), false, newValue -> {
                this.apiType = newValue;
                manager.createDisplay(this); // Create a new display for the light.
            });

            this.color.setupUnmodulatedEditor(editor, "Light Color", null, () -> manager.createDisplay(this));
            editor.addFloatVector("Position", this.position, () -> manager.createDisplay(this), manager.getController());
            editor.addButton("Show Direction", () ->
                    manager.getController().getMarkerManager().updateArrow(this.position, this.direction, this.position.defaultBits()));
            editor.addFloatVector("Direction", this.direction, () -> {
                manager.createDisplay(this);
                manager.getController().getMarkerManager().updateArrow(this.position, this.direction, this.position.defaultBits());
            }, manager.getController(), 12);

            editor.addUnsignedFixedShort("Attribute 0:", this.attrib1, newValue -> this.attrib1 = newValue, 1);
            editor.addUnsignedFixedShort("Attribute 1:", this.attrib2, newValue -> this.attrib2 = newValue, 1);
        }

        /**
         * Create a javafx light.
         */
        public LightBase createJavaFxLight() {
            switch (this.apiType) {
                case AMBIENT:
                    AmbientLight ambLight = new AmbientLight();
                    ambLight.setColor(Utils.fromRGB(this.color.toRGB()));
                    return ambLight;
                case PARALLEL:
                    // IMPORTANT! JavaFX does NOT support parallel (directional) lights [AndyEder]
                    PointLight parallelLight = new PointLight();
                    parallelLight.setColor(Utils.fromRGB(this.color.toRGB()));
                    // Use direction as a vector to set a position to simulate a parallel light with JavaFX.
                    // This has 12 decimal bits, so by only using 2 we place the light very far away, which approximates a parallel light, albeit inaccurately.
                    parallelLight.setTranslateX(-this.direction.getFloatX(2));
                    parallelLight.setTranslateY(-this.direction.getFloatY(2));
                    parallelLight.setTranslateZ(-this.direction.getFloatZ(2));
                    return parallelLight;
                case POINT:
                    PointLight pointLight = new PointLight();
                    pointLight.setColor(Utils.fromRGB(this.color.toRGB()));

                    // Assuming direction is position? Are POINT lights ever used? [AndyEder]
                    // I recall point lights getting used somewhere, but I can't recall and could be wrong. (~Knee)
                    pointLight.setTranslateX(this.direction.getFloatX());
                    pointLight.setTranslateY(this.direction.getFloatY());
                    pointLight.setTranslateZ(this.direction.getFloatZ());
                    return pointLight;
                default:
                    // Unknown / unsupported light type.
                    return null;
            }
        }
    }

    public enum OldFroggerMapLightType {
        DUMMY,
        STATIC,
        ENTITY
    }
}