package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.SwarmEntityData.SwarmDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a bee swarm entity.
 * TODO: This seems to contain data extremely similar to CAV_SLIME. This seems to me like there's a similarity between these entities. Both entities are created dynamically.
 * - Do they serve as some kind of template for the entities which are made?
 * - Do they serve as some kind of generator which manages them? Dunno.
 * Created by Kneesnap on 12/17/2023.
 */
public class SwarmEntityData extends OldFroggerEntityData<SwarmDifficultyData> {
    private int unknown1;
    private int unknown2;
    private int unknown3;
    private int unknown4;

    /*
FLOAT MR_USHORT "Critical Distance" 0 32767 256 256
        FLOAT MR_USHORT "Frog Delay" 0 300 60 30
        FLOAT MR_USHORT "Swarm Speed" 0 32767 2184.5 2184.5
        FLOAT MR_USHORT "Interest Time" 0 3000 150 30

struct __forest_rt_swarm
	{
	MR_VOID*				sw_frog;				// Ptr to frog being chased (void* to avoid header include probs)
	MR_VOID*				sw_api_item0;			// Ptr to PGEN
	MR_VOID*				sw_api_insts[4];		// Ptr to api insts
	MR_LONG					sw_voice_id;			// Voice id
	MR_LONG					sw_speed;				// Speed at which swarm moves
	MR_ULONG				sw_delay;				// Delay count
	MR_MAT					sw_matrix;				// Matrix
	MR_SVEC					sw_positions[FOR_NUM_SWARM_SPRITES];
//	MR_ULONG				sw_curr_offset[FOR_NUM_SWARM_SPRITES];
//	MR_VEC*					sw_offset_table[FOR_NUM_SWARM_SPRITES];
	MR_LONG					sw_ofs_angle[FOR_NUM_SWARM_SPRITES];

	};	//FOREST_RT_SWARM

struct __forest_hive
	{
	MR_MAT					hv_matrix;				// matrix of entity
	MR_LONG					hv_release_distance;	// How close does Frogger get before swarm comes out.
	MR_LONG					hv_swarm_speed;			// How fast when released?
	};	//FOREST_HIVE

struct __forest_rt_hive
	{
	FOREST_RT_SWARM			hv_swarm;				// swarm entity when its released
	MR_LONG					hv_voice_id;			// Voice id
	MR_USHORT				hv_state;				// State
	MR_USHORT				hv_pad;					// Pad
	};	//FOREST_RT_HIVE
     */

    // TODO: !

    public SwarmEntityData(OldFroggerMapEntity entity) {
        super(entity, SwarmDifficultyData::new);
    }

    @Override
    protected void loadMainEntityData(DataReader reader) {
        this.unknown1 = reader.readUnsignedShortAsInt();
        this.unknown2 = reader.readUnsignedShortAsInt();
        this.unknown3 = reader.readUnsignedShortAsInt();
        this.unknown4 = reader.readUnsignedShortAsInt();
        // TODO: !
    }

    @Override
    protected void saveMainEntityData(DataWriter writer) {
        writer.writeUnsignedShort(this.unknown1);
        writer.writeUnsignedShort(this.unknown2);
        writer.writeUnsignedShort(this.unknown3);
        writer.writeUnsignedShort(this.unknown4);
        // TODO: !
    }

    @Override
    public float[] getPosition(float[] position) {
        return new float[6];// TODO: !
    }

    @Override
    public void setupMainEntityDataEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        editor.addUnsignedFixedShort("Value 1", this.unknown1, newValue -> this.unknown1 = newValue, 1);
        editor.addUnsignedFixedShort("Value 2", this.unknown2, newValue -> this.unknown2 = newValue, 1);
        editor.addUnsignedFixedShort("Value 3", this.unknown3, newValue -> this.unknown3 = newValue, 1);
        editor.addUnsignedFixedShort("Value 4", this.unknown4, newValue -> this.unknown4 = newValue, 1);
// TODO: !
    }

    @Getter
    public static class SwarmDifficultyData extends OldFroggerDifficultyData {
        private int unknown1 = 2184; // TODO: PROBABLY SPEED

        public SwarmDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.unknown1 = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.unknown1);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.unknown1, newValue -> this.unknown1 = newValue, 2184);
        }
    }
}