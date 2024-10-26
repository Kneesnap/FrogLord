package net.highwayfrogs.editor.games.konami.greatquest.map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;

import java.util.UUID;

/**
 * Represents the '_kcLight' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
public class kcLight implements IMultiLineInfoWriter, IBinarySerializable {
    private kcLightType lightType;
    private final kcColor4 diffuseColor = new kcColor4();
    private final kcColor4 ambientColor = new kcColor4();
    private final kcColor4 specularColor = new kcColor4();
    private final kcVector4 position = new kcVector4();
    private final kcVector4 direction = new kcVector4();
    private float range = 1000F;
    private float falloff;
    private float attenuation0;
    private float attenuation1;
    private float attenuation2;
    private float theta;
    private float phi;

    private static final UUID POSITION_ID = UUID.randomUUID();

    @Override
    public void load(DataReader reader) {
        this.lightType = kcLightType.readLightType(reader);
        this.diffuseColor.load(reader);
        this.ambientColor.load(reader);
        this.specularColor.load(reader);
        this.position.load(reader);
        this.direction.load(reader);
        this.range = reader.readFloat();
        this.falloff = reader.readFloat();
        this.attenuation0 = reader.readFloat();
        this.attenuation1 = reader.readFloat();
        this.attenuation2 = reader.readFloat();
        this.theta = reader.readFloat();
        this.phi = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        kcLightType.writeLightType(writer, this.lightType);
        this.diffuseColor.save(writer);
        this.ambientColor.save(writer);
        this.specularColor.save(writer);
        this.position.save(writer);
        this.direction.save(writer);
        writer.writeFloat(this.range);
        writer.writeFloat(this.falloff);
        writer.writeFloat(this.attenuation0);
        writer.writeFloat(this.attenuation1);
        writer.writeFloat(this.attenuation2);
        writer.writeFloat(this.theta);
        writer.writeFloat(this.phi);
    }

    public void setupEditor(GUIEditorGrid editorGrid, MeshViewController<?> viewController) {
        editorGrid.addEnumSelector("Light Type", this.lightType, kcLightType.values(), false, newValue -> this.lightType = newValue);
        editorGrid.addColorPickerWithAlpha("Diffuse Color", this.diffuseColor.toColor().getRGB(), this.diffuseColor::fromARGB);
        editorGrid.addColorPickerWithAlpha("Ambient Color", this.ambientColor.toColor().getRGB(), this.ambientColor::fromARGB);
        editorGrid.addColorPickerWithAlpha("Specular Color", this.specularColor.toColor().getRGB(), this.specularColor::fromARGB);

        // TODO: Position & direction need editors, but I'm not happy with the current situation for 3D integration.
        editorGrid.addPositionEditor(viewController, POSITION_ID, "Position", this.position.getX(), this.position.getY(), this.position.getZ(), (meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> {
            if ((flags & TranslationGizmo.X_CHANGED_FLAG) == TranslationGizmo.X_CHANGED_FLAG)
                this.position.setX((float) newX);
            if ((flags & TranslationGizmo.Y_CHANGED_FLAG) == TranslationGizmo.Y_CHANGED_FLAG)
                this.position.setY((float) newY);
            if ((flags & TranslationGizmo.Z_CHANGED_FLAG) == TranslationGizmo.Z_CHANGED_FLAG)
                this.position.setZ((float) newZ);
        });

        editorGrid.addButton("Show Direction", () ->
                viewController.getMarkerManager().updateArrow(this.position, this.direction));
        /*editorGrid.addFloatVector("Direction", this.direction, () -> {
            viewController.getMarkerManager().updateArrow(this.position, this.direction);
        }, viewController);*/

        editorGrid.addFloatField("Range", this.range, newValue -> this.range = newValue, null);
        editorGrid.addFloatField("Falloff", this.falloff, newValue -> this.falloff = newValue, null);
        editorGrid.addFloatField("Attenuation 0", this.attenuation0, newValue -> this.attenuation0 = newValue, null);
        editorGrid.addFloatField("Attenuation 1", this.attenuation1, newValue -> this.attenuation1 = newValue, null);
        editorGrid.addFloatField("Attenuation 2", this.attenuation2, newValue -> this.attenuation2 = newValue, null);
        editorGrid.addFloatField("Theta", this.theta, newValue -> this.theta = newValue, null);
        editorGrid.addFloatField("Phi", this.phi, newValue -> this.phi = newValue, null);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Light Type: ").append(this.lightType).append(Constants.NEWLINE);
        this.diffuseColor.writePrefixedInfoLine(builder, "Diffuse Color", padding);
        this.ambientColor.writePrefixedInfoLine(builder, "Ambient Color", padding);
        this.specularColor.writePrefixedInfoLine(builder, "Specular Color", padding);
        this.position.writePrefixedInfoLine(builder, "Position", padding);
        this.direction.writePrefixedInfoLine(builder, "Direction", padding);

        builder.append(padding).append("Range: ").append(this.range).append(Constants.NEWLINE);
        builder.append(padding).append("Falloff: ").append(this.falloff).append(Constants.NEWLINE);
        builder.append(padding).append("Attenuation 0: ").append(this.attenuation0).append(Constants.NEWLINE);
        builder.append(padding).append("Attenuation 1: ").append(this.attenuation1).append(Constants.NEWLINE);
        builder.append(padding).append("Attenuation 2: ").append(this.attenuation2).append(Constants.NEWLINE);
        builder.append(padding).append("Theta: ").append(this.theta).append(Constants.NEWLINE);
        builder.append(padding).append("Phi: ").append(this.phi).append(Constants.NEWLINE);
    }
}