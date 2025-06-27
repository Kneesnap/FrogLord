package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerGeneralDataManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Arrays;
import java.util.UUID;

/**
 * Contains the "STANDARD" chunk data.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapStandardPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "STND";
    private static final UUID CAMERA_OFFSET_IDENTIFIER = UUID.randomUUID();

    private final SVector cameraOffset = new SVector();
    private short halfHeight;
    private short frontClipPlane;
    private short backClipPlane;
    private short horizClipPlaneOffset;
    private short cameraMaximumLeft;
    private short cameraMaximumBottom;
    private short cameraMaximumRight;
    private short cameraMaximumTop;
    private final int[] racePathID = new int[5];

    public OldFroggerMapStandardPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
        Arrays.fill(this.racePathID, -1);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.cameraOffset.loadWithPadding(reader);
        this.halfHeight = reader.readShort();
        this.frontClipPlane = reader.readShort();
        this.backClipPlane = reader.readShort();
        this.horizClipPlaneOffset = reader.readShort();
        this.cameraMaximumLeft = reader.readShort();
        this.cameraMaximumBottom = reader.readShort();
        this.cameraMaximumRight = reader.readShort();
        this.cameraMaximumTop = reader.readShort();
        for (int i = 0; i < this.racePathID.length; i++)
            this.racePathID[i] = reader.readInt();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        this.cameraOffset.saveWithPadding(writer);
        writer.writeShort(this.halfHeight);
        writer.writeShort(this.frontClipPlane);
        writer.writeShort(this.backClipPlane);
        writer.writeShort(this.horizClipPlaneOffset);
        writer.writeShort(this.cameraMaximumLeft);
        writer.writeShort(this.cameraMaximumBottom);
        writer.writeShort(this.cameraMaximumRight);
        writer.writeShort(this.cameraMaximumTop);
        for (int i = 0; i < this.racePathID.length; i++)
            writer.writeInt(this.racePathID[i]);
    }

    @Override
    public void clear() {
        this.cameraOffset.setValues((short) 0, (short) 0, (short) 0);
        this.halfHeight = 0;
        this.frontClipPlane = 0;
        this.backClipPlane = 0;
        this.horizClipPlaneOffset = 0;
        this.cameraMaximumLeft = 0;
        this.cameraMaximumBottom = 0;
        this.cameraMaximumRight = 0;
        this.cameraMaximumTop = 0;
        Arrays.fill(this.racePathID, 0);
    }

    /**
     * Create the editor UI for editing the data in this packet.
     * @param manager The manager to create the editor UI for.
     * @param editor  Creating the editor
     */
    public void setupEditor(OldFroggerGeneralDataManager manager, GUIEditorGrid editor) {
        IVector froggerStartPosition = getParentFile().getLevelSpecificDataPacket().getFroggerStartPosition();

        editor.addPositionOffsetEditor(manager.getController(), CAMERA_OFFSET_IDENTIFIER, "Camera Offset", this.cameraOffset, froggerStartPosition, null);
        editor.addFixedShort("Half Height", this.halfHeight, newValue -> this.halfHeight = newValue);
        editor.addFixedShort("Camera Max Left", this.cameraMaximumLeft, newValue -> this.cameraMaximumLeft = newValue);
        editor.addFixedShort("Camera Max Bottom", this.cameraMaximumBottom, newValue -> this.cameraMaximumBottom = newValue); // 95.5625, -2.6875, -592.0
        editor.addFixedShort("Camera Max Right", this.cameraMaximumRight, newValue -> this.cameraMaximumRight = newValue);
        editor.addFixedShort("Camera Max Top", this.cameraMaximumTop, newValue -> this.cameraMaximumTop = newValue);
        editor.addFixedShort("Front Clip Plane", this.frontClipPlane, newValue -> this.frontClipPlane = newValue);
        editor.addFixedShort("Back Clip Plane", this.backClipPlane, newValue -> this.backClipPlane = newValue);
        editor.addFixedShort("Horizontal Clip Plane", this.horizClipPlaneOffset, newValue -> this.horizClipPlaneOffset = newValue);

        // Race Paths
        for (int i = 0; i < this.racePathID.length; i++) {
            final int index = i;
            editor.addSignedIntegerField("Race Path ID #" + (i + 1), this.racePathID[i],
                    newValue -> this.racePathID[index] = newValue);
        }
    }
}