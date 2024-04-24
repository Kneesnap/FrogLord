package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.entity.GreatQuestMapEditorEntityDisplay;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

import java.util.UUID;

/**
 * Represents the 'kcEntity3DInst' struct.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
@Setter
public class kcEntity3DInst extends kcEntityInst {
    private int flags; // TODO: InitFlags() is called.
    private kcAxisType billboardAxis = kcAxisType.Y;
    private final kcVector4 position = new kcVector4();
    private final kcVector4 rotation = new kcVector4();
    private final kcVector4 scale = new kcVector4();
    private final int[] reservedValues = new int[7];
    private final int[] padding = new int[32];

    public static final int SIZE_IN_BYTES = 240;
    private static final UUID GIZMO_ID = UUID.randomUUID();

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
        for (int i = 0; i < this.reservedValues.length; i++)
            this.reservedValues[i] = reader.readInt();
        for (int i = 0; i < this.padding.length; i++)
            this.padding[i] = reader.readInt();
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
        grid.addLabel("Flags", Utils.toHexString(this.flags));
        grid.addEnumSelector("Billboard Axis", this.billboardAxis, kcAxisType.values(), false, newType -> this.billboardAxis = newType);

        // Position Editor
        grid.addPositionEditor(manager.getController(), GIZMO_ID, "Position", this.position.getX(), this.position.getY(), this.position.getZ(), (meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> {
            this.position.setX((float) newX);
            this.position.setY((float) newY);
            this.position.setZ((float) newZ);
            entityDisplay.setPosition(newX, newY, newZ);
        });

        // Scale Editor TODO: I think we want to make these gizmos managed by the entity editor for maximum flexibility.
        grid.addScaleEditor(manager.getController(), GIZMO_ID, "Scale", this.position.getX(), this.position.getY(), this.position.getZ(), this.scale.getX(), this.scale.getY(), this.scale.getZ(), (meshView, oldX, oldY, oldZ, newX, newY, newZ) -> {
            this.scale.setX((float) newX);
            this.scale.setY((float) newY);
            this.scale.setZ((float) newZ);
            entityDisplay.setScale(newX, newY, newZ);
        });

        // Rotation
        grid.addDoubleSlider("Rotation X", this.rotation.getX(), newValue -> {
            this.rotation.setX((float) (double) newValue);
            entityDisplay.updateRotation();
        }, -Math.PI, Math.PI);

        grid.addDoubleSlider("Rotation Y", this.rotation.getY(), newValue -> {
            this.rotation.setY((float) (double) newValue);
            entityDisplay.updateRotation();
        }, -Math.PI, Math.PI);

        grid.addDoubleSlider("Rotation Z", this.rotation.getZ(), newValue -> {
            this.rotation.setZ((float) (double) newValue);
            entityDisplay.updateRotation();
        }, -Math.PI, Math.PI);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Flags: ").append(Utils.toHexString(this.flags)).append(Constants.NEWLINE);
        builder.append(padding).append("Billboard Axis: ").append(this.billboardAxis).append(Constants.NEWLINE);
        this.position.writePrefixedInfoLine(builder, "Position", padding);
        this.rotation.writePrefixedInfoLine(builder, "Rotation", padding);
        this.scale.writePrefixedInfoLine(builder, "Scale", padding);
    }
}