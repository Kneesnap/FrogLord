package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerReactionType;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEditorUtils;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerZoneManager.ZonePreview3D;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerZoneManager.ZoneRegionEditor;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerZoneManager.ZoneRegionPreview3D;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents zone data.
 * Created by Kneesnap on 12/12/2023.
 */
@Getter
public class OldFroggerMapZonePacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "ZONE";

    private final List<OldFroggerMapZone> zones = new ArrayList<>();

    public OldFroggerMapZonePacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int zoneCount = reader.readUnsignedShortAsInt();
        reader.alignRequireEmpty(4);

        this.zones.clear();
        int endPointer = reader.getIndex();
        for (int i = 0; i < zoneCount; i++) {
            int zonePointer = reader.readInt();

            reader.jumpTemp(zonePointer);
            OldFroggerMapZone newZone = new OldFroggerMapZone(getParentFile().getGameInstance());
            newZone.load(reader);
            endPointer = Math.max(endPointer, reader.getIndex());
            reader.jumpReturn();

            this.zones.add(newZone);
        }

        // Ensure reading continues after the end of the zone data.
        reader.setIndex(endPointer);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.zones.size());
        writer.writeUnsignedShort(0); // Padding

        // Zone pointer table.
        int zonePointerStartIndex = writer.getIndex();
        for (int i = 0; i < this.zones.size(); i++)
            writer.writeNullPointer();

        // Zone data.
        for (int i = 0; i < this.zones.size(); i++) {
            writer.writeAddressTo(zonePointerStartIndex + (i * Constants.INTEGER_SIZE));
            this.zones.get(i).save(writer);
        }
    }

    /**
     * Represents a single zone in an old Frogger map.
     */
    @Getter
    public static class OldFroggerMapZone extends SCGameData<OldFroggerGameInstance> {
        private short planeY; // world height of this zone
        private int objBoundAX1; // Object Aligned Bounding Box
        private int objBoundAZ1; //	(World Coords)
        private int objBoundAX2;
        private int objBoundAZ2;
        private final List<OldFroggerMapZoneRegion> regions = new ArrayList<>();
        private int difficulty; // difficulty flags, i.e. whether zone is active on particular diff levels
        private OldFroggerMapZoneType zoneType = OldFroggerMapZoneType.LANDSCAPE; // Zone type, such as SAFE/UNSAFE, TRIGGER, etc.
        private OldFroggerReactionType reactionType = OldFroggerReactionType.Nothing;
        private final int[] reactionData = new int[3];

        public OldFroggerMapZone(OldFroggerGameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.planeY = reader.readShort();
            reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Zone state. (Seems to always be zero)
            this.objBoundAX1 = reader.readInt();
            this.objBoundAZ1 = reader.readInt();
            this.objBoundAX2 = reader.readInt();
            this.objBoundAZ2 = reader.readInt();

            short regionCount = reader.readUnsignedByteAsShort();
            reader.alignRequireEmpty(4);
            this.difficulty = reader.readUnsignedShortAsInt();
            this.zoneType = OldFroggerMapZoneType.values()[reader.readUnsignedShortAsInt()];
            this.reactionType = OldFroggerReactionType.values()[reader.readUnsignedShortAsInt()];
            for (int i = 0; i < this.reactionData.length; i++)
                this.reactionData[i] = reader.readUnsignedShortAsInt();

            // Read regions.
            this.regions.clear();
            for (int i = 0; i < regionCount; i++) {
                OldFroggerMapZoneRegion newRegion = new OldFroggerMapZoneRegion(getGameInstance());
                newRegion.load(reader);
                this.regions.add(newRegion);
            }
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.planeY);
            writer.writeUnsignedShort(0); // State (Always Zero)
            writer.writeInt(this.objBoundAX1);
            writer.writeInt(this.objBoundAZ1);
            writer.writeInt(this.objBoundAX2);
            writer.writeInt(this.objBoundAZ2);
            writer.writeUnsignedByte((short) this.regions.size());
            writer.align(4);
            writer.writeUnsignedShort(this.difficulty);
            writer.writeUnsignedShort(this.zoneType != null ? this.zoneType.ordinal() : 0);
            writer.writeUnsignedShort(this.reactionType != null ? this.reactionType.ordinal() : 0);
            for (int i = 0; i < this.reactionData.length; i++)
                writer.writeUnsignedShort(this.reactionData[i]);

            // Write regions
            for (int i = 0; i < this.regions.size(); i++)
                this.regions.get(i).save(writer);
        }

        /**
         * Creates an editor UI for this zone.
         * @param editor      The editor to create the UI with.
         * @param zonePreview The 3D zone preview to update.
         */
        public void setupEditor(GUIEditorGrid editor, ZonePreview3D zonePreview) {
            // Position Updates
            editor.addFixedInt("Min X", this.objBoundAX1, newValue -> {
                this.objBoundAX1 = newValue;
                zonePreview.updateBoxPosition(this);
            });
            editor.addFixedInt("Min Z", this.objBoundAZ1, newValue -> {
                this.objBoundAZ1 = newValue;
                zonePreview.updateBoxPosition(this);
            });
            editor.addFixedInt("Max X", this.objBoundAX2, newValue -> {
                this.objBoundAX2 = newValue;
                zonePreview.updateBoxPosition(this);
            });
            editor.addFixedInt("Max Z", this.objBoundAZ2, newValue -> {
                this.objBoundAZ2 = newValue;
                zonePreview.updateBoxPosition(this);
            });
            editor.addFixedShort("Y", this.planeY, newValue -> {
                this.planeY = newValue;
                zonePreview.updateBoxPosition(this);

                // Update the Y position of the lines
                for (int i = 0; i < zonePreview.getRegionPreviews().size(); i++)
                    zonePreview.getRegionPreviews().get(i).updateRegionLineHeight(this);
            }, 1 << 4, Short.MIN_VALUE, Short.MAX_VALUE);

            // Basic Data
            OldFroggerEditorUtils.addDifficultyEditor(editor, this.difficulty, newValue -> this.difficulty = newValue);
            editor.addEnumSelector("Zone Type", this.zoneType, OldFroggerMapZoneType.values(), false, newValue -> this.zoneType = newValue);
            OldFroggerEditorUtils.setupReactionEditor(editor, this.reactionType, this.reactionData, newValue -> this.reactionType = newValue);

            // Region
            editor.addSeparator();
            OldFroggerMapZoneRegion firstRegion = this.regions.size() > 0 ? this.regions.get(0) : null;
            ZoneRegionPreview3D regionPreview = zonePreview.getPreviewsByRegion().get(firstRegion);
            new ZoneRegionEditor(this, zonePreview, firstRegion).setupEditor(editor, firstRegion, regionPreview);
        }
    }

    /**
     * Represents a single zone region in an old Frogger map.
     */
    @Getter
    @Setter
    public static class OldFroggerMapZoneRegion extends SCGameData<OldFroggerGameInstance> {
        private int worldX; // x offset for this line
        private int worldZ1; // lower Z coord
        private int worldZ2; // upper Z coord

        public OldFroggerMapZoneRegion(OldFroggerGameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.worldX = reader.readInt();
            this.worldZ1 = reader.readInt();
            this.worldZ2 = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.worldX);
            writer.writeInt(this.worldZ1);
            writer.writeInt(this.worldZ2);
        }
    }

    public enum OldFroggerMapZoneType {
        LANDSCAPE, // always effective (least when entity hits landscape)
        PLANAR, // effective when entity is below zone height
        COSMETIC, // cosmetic zone, always effective, not currently used
        TRIGGER, // always effective (designed to trigger an event or entity)
        LAUNCH, // always effective (least when entity hit landscape)
        DUMMY2, // 5
        DUMMY3, // 6
        DUMMY4, // 7
        DUMMY5, // 8
        DUMMY6, // 9
        LOCKZ, // always effective (least when entity hit landscape)
        LOCKX, // always effective (least when entity hit landscape)
        LOCKZX, // always effective (least when entity hit landscape)
        LOCK45Z, // always effective (least when entity hit landscape)
        LOCK45X, // always effective (least when entity hit landscape)
        LOCK45ZX, // always effective (least when entity hit landscape)
    }
}