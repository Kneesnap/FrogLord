package net.highwayfrogs.editor.games.konami.greatquest.entity;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityFlag.kcEntityInstanceFlag;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.entity.GreatQuestMapEditorEntityDisplay;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.MathUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Represents the 'kcEntity3DInst' struct.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcEntity3DInst extends kcEntityInst {
    @Setter private int flags = DEFAULT_FLAGS;
    private kcAxisType billboardAxis = DEFAULT_BILLBOARD_AXIS;
    private final kcVector4 position = DEFAULT_POSITION.clone();
    private final kcVector4 rotation = DEFAULT_ROTATION.clone();
    private final kcVector4 scale = DEFAULT_SCALE.clone();
    private final int[] reservedValues = new int[RESERVE_VALUE_COUNT];
    private final int[] padding = new int[PADDING_VALUE_COUNT];

    public static final int SIZE_IN_BYTES = 240;
    public static final int SIZE_IN_BYTES_WITHOUT_PADDING = 180;
    private static final int RESERVE_VALUE_COUNT = 7;
    private static final int PADDING_VALUE_COUNT = 32;
    public static final int ACTUAL_PADDING_BYTE_SIZE = (RESERVE_VALUE_COUNT + PADDING_VALUE_COUNT) * Constants.INTEGER_SIZE;
    private static final UUID GIZMO_ID = UUID.randomUUID();
    private static final kcAxisType DEFAULT_BILLBOARD_AXIS = kcAxisType.Y;
    private static final kcVector4 DEFAULT_POSITION = new kcVector4(0, 0, 0, 1);
    private static final kcVector4 DEFAULT_ROTATION = new kcVector4(0, 0, 0, 1);
    private static final kcVector4 DEFAULT_SCALE = new kcVector4(1, 1, 1, 1);
    private static final int DEFAULT_FLAGS = 0;

    public kcEntity3DInst(kcCResourceEntityInst entity) {
        super(entity);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.flags = reader.readInt();
        this.billboardAxis = kcAxisType.getType(reader.readInt(), false);
        this.position.load(reader);
        this.rotation.load(reader);
        this.scale.load(reader);
        if (reader.getRemaining() == ACTUAL_PADDING_BYTE_SIZE) {
            for (int i = 0; i < this.reservedValues.length; i++)
                this.reservedValues[i] = reader.readInt();
            for (int i = 0; i < this.padding.length; i++)
                this.padding[i] = reader.readInt();
        } else {
            // A handful of coins & gems found in the PC version's extra Dark Trail level seem to be semi-broken.
            // The data reports the wrong byte-size, and is missing the padding fields but not the reserved fields.
            reader.skipBytes(reader.getRemaining()); // Seems to be garbage. (Part of another entity instance?)
            Arrays.fill(this.reservedValues, 0);
            Arrays.fill(this.padding, 0);
        }
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.flags);
        writer.writeInt(this.billboardAxis.ordinal());
        this.position.save(writer);
        this.rotation.save(writer);
        this.scale.save(writer);
        for (int i = 0; i < this.reservedValues.length; i++)
            writer.writeInt(this.reservedValues[i]);
        for (int i = 0; i < this.padding.length; i++)
            writer.writeInt(this.padding[i]);
    }

    @Override
    protected void setupMainEditor(GreatQuestEntityManager manager, GUIEditorGrid grid, GreatQuestMapEditorEntityDisplay entityDisplay) {
        super.setupMainEditor(manager, grid, entityDisplay);
        grid.addLabel("Flags", NumberUtils.toHexString(this.flags));
        grid.addEnumSelector("Billboard Axis", this.billboardAxis, kcAxisType.values(), false, this::setBillboardAxis);

        // Position Editor
        grid.addPositionEditor(manager.getController(), GIZMO_ID, "Position", this.position.getX(), this.position.getY(), this.position.getZ(), .02, (meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> {
            this.position.setX((float) newX);
            this.position.setY((float) newY);
            this.position.setZ((float) newZ);
            entityDisplay.setPosition(newX, newY, newZ);
        });

        // Scale Editor
        grid.addScaleEditor(manager.getController(), GIZMO_ID, "Scale", this.position.getX(), this.position.getY(), this.position.getZ(), this.scale.getX(), this.scale.getY(), this.scale.getZ(), .02, (meshView, oldX, oldY, oldZ, newX, newY, newZ) -> {
            this.scale.setX((float) newX);
            this.scale.setY((float) newY);
            this.scale.setZ((float) newZ);
            entityDisplay.setScale(newX, newY, newZ);
        });

        // Rotation
        addRotationSlider(getGameInstance(), grid, entityDisplay, "Rotation X", this.rotation.getX(), this.rotation::setX);
        addRotationSlider(getGameInstance(), grid, entityDisplay, "Rotation Y", this.rotation.getY(), this.rotation::setY);
        addRotationSlider(getGameInstance(), grid, entityDisplay, "Rotation Z", this.rotation.getZ(), this.rotation::setZ);
    }

    private static final DecimalFormat ROTATION_ANGLE_FORMATTER = new DecimalFormat("##0.###");
    private static void addRotationSlider(GreatQuestInstance instance, GUIEditorGrid grid, GreatQuestMapEditorEntityDisplay entityDisplay, String labelText, float angleInRadians, Consumer<Float> setter) {
        double rotation = MathUtils.clampAngleInDegrees(Math.toDegrees(angleInRadians));
        AtomicReference<Label> labelRef = new AtomicReference<>();
        Slider slider = grid.addDoubleSlider(labelText + " (" + (int) rotation + "°)", rotation, newValue -> {
            setter.accept((float) Math.toRadians(newValue));
            entityDisplay.updateRotation();

            Label label = labelRef.get();
            if (label != null)
                label.setText(labelText + " (" + ((int) (double) newValue) + "°)");
        }, -180, 180, false, labelRef);
        slider.setMajorTickUnit(90);

        Label label = labelRef.get();
        if (label != null) {
            label.setOnMouseClicked(event -> {
                event.consume();
                double startRotation = slider.getValue();
                InputMenu.promptInputBlocking(instance, "Please enter the new " + labelText + ".", ROTATION_ANGLE_FORMATTER.format(startRotation), newAngleText -> {
                    if (!NumberUtils.isNumber(newAngleText)) {
                        FXUtils.showPopup(AlertType.WARNING, "Invalid number.", "The value '" + newAngleText + "' cannot be interpreted as a number!");
                        return;
                    }

                    float newAngle = Float.parseFloat(newAngleText);
                    if (!Float.isFinite(newAngle)) {
                        FXUtils.showPopup(AlertType.WARNING, "Invalid angle.", "The value '" + newAngleText + "' cannot be used as an angle!");
                        return;
                    }

                    setter.accept((float) Math.toRadians(newAngle));
                    entityDisplay.updateRotation();
                    label.setText(labelText + " (" + ((int) newAngle) + "°)");
                    slider.setValue(newAngle);
                });
            });
        }
    }

    /**
     * Gets the entity rotation in degrees.
     * @param output the output storage vector for the angles
     * @return rotationAnglesInDegrees
     */
    public Vector3f getRotationAnglesInDegrees(Vector3f output) {
        if (output == null)
            output = new Vector3f();

        output.setX((float) Math.toDegrees(this.rotation.getX()));
        output.setY((float) Math.toDegrees(this.rotation.getY()));
        output.setZ((float) Math.toDegrees(this.rotation.getZ()));
        return output;
    }

    /**
     * Sets the entity rotation based on a vector containing the angles in degrees.
     * @param anglesInDegrees the vector containing angles in degrees
     */
    public void setRotationAnglesInDegrees(Vector3f anglesInDegrees) {
        if (anglesInDegrees == null)
            throw new NullPointerException("anglesInDegrees");

        this.rotation.setX((float) Math.toRadians(anglesInDegrees.getX()));
        this.rotation.setY((float) Math.toRadians(anglesInDegrees.getY()));
        this.rotation.setZ((float) Math.toRadians(anglesInDegrees.getZ()));
        this.rotation.setW(1F);
    }

    /**
     * Test if the entity has a flag set.
     * @param instanceFlag the flag to test
     * @return entityFlags
     */
    public boolean hasFlag(kcEntityInstanceFlag instanceFlag) {
        if (instanceFlag == null)
            throw new NullPointerException("instanceFlag");

        return (this.flags & instanceFlag.getInstanceBitFlagMask()) == instanceFlag.getInstanceBitFlagMask();
    }

    /**
     * Sets the billboard axis.
     * @param newAxis The axis to apply
     */
    @SuppressWarnings("unused") // Available to Noodle scripts.
    public void setBillboardAxis(kcAxisType newAxis) {
        if (newAxis == null)
            throw new NullPointerException("newAxis");

        this.billboardAxis = newAxis;
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Flags: ").append(kcEntityInstanceFlag.getAsOptionalArguments(this.flags).getNamedArgumentsAsCommaSeparatedString()).append(Constants.NEWLINE);
        if (this.billboardAxis != DEFAULT_BILLBOARD_AXIS)
            builder.append(padding).append("Billboard Axis: ").append(this.billboardAxis).append(Constants.NEWLINE);
        this.position.writePrefixedInfoLine(builder, "Position", padding);
        if (!DEFAULT_ROTATION.equals(this.rotation))
            this.rotation.writePrefixedInfoLine(builder, "Rotation", padding);
        if (!DEFAULT_SCALE.equals(this.scale))
            this.scale.writePrefixedInfoLine(builder, "Scale", padding);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Position", this.position.toParseableString(1F));
        propertyList.add("Rotation", getRotationAnglesInDegrees(null).toParseableString());
        propertyList.add("Scale", this.scale.toParseableString(1F));
        propertyList.add("Flags", kcEntityInstanceFlag.getAsOptionalArguments(this.flags).getNamedArgumentsAsCommaSeparatedString());
        propertyList.add("Billboard Axis", this.billboardAxis);
        return propertyList;
    }

    private static final String CONFIG_KEY_FLAGS = "flags";
    private static final String CONFIG_KEY_BILLBOARD_AXIS = "billboardAxis";
    private static final String CONFIG_KEY_POSITION = "position";
    private static final String CONFIG_KEY_ROTATION = "rotation";
    private static final String CONFIG_KEY_SCALE = "scale";

    @Override
    public void fromConfig(ILogger logger, Config input) {
        super.fromConfig(logger, input);

        ConfigValueNode flagNode = input.getOptionalKeyValueNode(CONFIG_KEY_FLAGS);
        if (flagNode != null) {
            OptionalArguments flagArguments = OptionalArguments.parseCommaSeparatedNamedArguments(flagNode.getAsString());
            this.flags = kcEntityInstanceFlag.getValueFromArguments(flagArguments);
            flagArguments.warnAboutUnusedArguments(getResource().getLogger());
        } else {
            kcEntity3DDesc description = getDescription();
            this.flags = description != null ? description.getDefaultFlags() : DEFAULT_FLAGS;
        }

        this.billboardAxis = input.getOrDefaultKeyValueNode(CONFIG_KEY_BILLBOARD_AXIS).getAsEnum(DEFAULT_BILLBOARD_AXIS);
        this.position.parse(input.getKeyValueNodeOrError(CONFIG_KEY_POSITION).getAsString(), 1F);

        ConfigValueNode rotationNode = input.getOptionalKeyValueNode(CONFIG_KEY_ROTATION);
        if (rotationNode != null) {
            Vector3f degrees = new Vector3f();
            degrees.parse(rotationNode.getAsString());
            setRotationAnglesInDegrees(degrees);
        } else {
            this.rotation.setXYZW(DEFAULT_ROTATION);
        }

        ConfigValueNode scaleNode = input.getOptionalKeyValueNode(CONFIG_KEY_SCALE);
        if (scaleNode != null) {
            this.scale.parse(scaleNode.getAsString(), 1F);
        } else {
            this.scale.setXYZW(DEFAULT_SCALE);
        }
    }

    @Override
    public void toConfig(Config output, kcScriptList scriptList, kcScriptDisplaySettings settings) {
        super.toConfig(output, scriptList, settings);

        output.getOrCreateKeyValueNode(CONFIG_KEY_FLAGS)
                .setComment("For a full list of flags, refer to the GQS scripting documentation.")
                .setAsString(kcEntityInstanceFlag.getAsOptionalArguments(this.flags).getNamedArgumentsAsCommaSeparatedString());

        if (this.billboardAxis != DEFAULT_BILLBOARD_AXIS)
            output.getOrCreateKeyValueNode(CONFIG_KEY_BILLBOARD_AXIS).setAsEnum(this.billboardAxis);
        output.getOrCreateKeyValueNode(CONFIG_KEY_POSITION).setAsString(this.position.toParseableString(1F));
        if (!DEFAULT_ROTATION.equals(this.rotation))
            output.getOrCreateKeyValueNode(CONFIG_KEY_ROTATION).setAsString(getRotationAnglesInDegrees(null).toParseableString());
        if (!DEFAULT_SCALE.equals(this.scale))
            output.getOrCreateKeyValueNode(CONFIG_KEY_SCALE).setAsString(this.scale.toParseableString(1F));
    }
}