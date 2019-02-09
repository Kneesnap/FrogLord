package net.highwayfrogs.editor.file.map.zone;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

/**
 * Holds data for a CameraZone.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
@Setter
public class CameraZone extends GameObject {
    private int flags;
    private short direction; // Force camera rotation to this direction. -1 is none.
    private SVector northSourceOffset;
    private SVector northTargetOffset;
    private SVector eastSourceOffset;
    private SVector eastTargetOffset;
    private SVector southSourceOffset;
    private SVector southTargetOffset;
    private SVector westSourceOffset;
    private SVector westTargetOffset;

    public static final int FLAG_ABSOLUTE_Y = Constants.BIT_FLAG_3; // Use y offsets as world y position.

    public static final int BYTE_SIZE = (2 * Constants.SHORT_SIZE) + (8 * SVector.PADDED_BYTE_SIZE);

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.direction = reader.readShort();
        this.northSourceOffset = SVector.readWithPadding(reader);
        this.northTargetOffset = SVector.readWithPadding(reader);
        this.eastSourceOffset = SVector.readWithPadding(reader);
        this.eastTargetOffset = SVector.readWithPadding(reader);
        this.southSourceOffset = SVector.readWithPadding(reader);
        this.southTargetOffset = SVector.readWithPadding(reader);
        this.westSourceOffset = SVector.readWithPadding(reader);
        this.westTargetOffset = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(getFlags());
        writer.writeShort(getDirection());
        this.northSourceOffset.saveWithPadding(writer);
        this.northTargetOffset.saveWithPadding(writer);
        this.eastSourceOffset.saveWithPadding(writer);
        this.eastTargetOffset.saveWithPadding(writer);
        this.southSourceOffset.saveWithPadding(writer);
        this.southTargetOffset.saveWithPadding(writer);
        this.westSourceOffset.saveWithPadding(writer);
        this.westTargetOffset.saveWithPadding(writer);
    }

    /**
     * Setup the camera zone editor.
     * @param controller The controller controlling this.
     * @param editor     The editor to create an interface under.
     */
    public void setupEditor(MapUIController controller, GUIEditorGrid editor) {
        editor.addIntegerField("Flags", getFlags(), this::setFlags, null);
        editor.addShortField("Direction", getDirection(), this::setDirection, null);
        editor.addSVector("North Source", getNorthSourceOffset());
        editor.addSVector("North Target", getNorthTargetOffset());
        editor.addSVector("East Source", getEastSourceOffset());
        editor.addSVector("East Target", getEastTargetOffset());
        editor.addSVector("South Source", getSouthSourceOffset());
        editor.addSVector("South Target", getSouthTargetOffset());
        editor.addSVector("West Source", getWestSourceOffset());
        editor.addSVector("West Target", getWestTargetOffset());
    }
}
