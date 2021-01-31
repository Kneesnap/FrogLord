package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.KCParam;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;

/**
 * Represents an action sequence definition.
 * Created by Kneesnap on 3/23/2020.
 */
@Getter
public class TGQChunkActionSequence extends kcCResource {
    @Setter private KCActionID actionID;
    private KCParam[] params = new KCParam[4];

    public TGQChunkActionSequence(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.ACTIONSEQUENCE);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.actionID = KCActionID.values()[reader.readInt() - 1];
        for (int i = 0; i < this.params.length; i++)
            this.params[i] = KCParam.readParam(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.actionID.ordinal());
        for (int i = 0; i < this.params.length; i++)
            writer.writeBytes(this.params[i].getBytes());
    }

    public enum KCActionID {
        NONE,
        STOP,
        ACTIVATE,
        ENABLE,
        TERMINATE,
        INITFLAGS,
        SETFLAGS,
        CLEARFLAGS,
        SETSTATE,
        SETTARGET,
        SETSPEED,
        SETPOSITION,
        SETPOSITION_XYZ,
        ADDPOSITION,
        ADDPOSITION_XYZ,
        SETROTATION,
        SETROTATION_XYZ,
        ADDROTATION,
        ADDROTATION_XYZ,
        ROTATE_RIGHT,
        ROTATE_LEFT,
        ROTATE_TARGET,
        SETANIMATION,
        SETSEQUENCE,
        WAIT,
        WAIT_ROTATE,
        WAIT_ROTATE_XYZ,
        WAIT_ANIMATION,
        WAIT_SEQUENCE,
        LOOP,
        IMPULSE,
        DAMAGE,
        SEEK,
        PERSUE,
        OFFSET_PERSUE,
        ARRIVAL,
        FLEE,
        EVADE,
        WANDER,
        CONTAINMENT,
        FLOCK,
        ENEMY_REACT,
        OBSTACLE_AVOID,
        WALL_FOLLOW,
        PATH_FOLLOW,
        FLOWFIELD_FOLLOW,
        LEADER_FOLLOW,
        PROMPT,
        DIALOG,
        COMPLETE,
        SETALARM,
        TRIGGER_EVENT,
        PLAYSFX, // Also sometimes called PLAYPCMSTREAM.
        VARIABLESET,
        VARIABLEADD,
        NUMBER,
        PARTICLE,
        KILLPARTICLE,
        LAUNCHER,
        WITHITEM,
        GIVETAKEITEM,
        GIVEDAMAGE,
        SAVEPOINT,
        ENABLE_UPDATE,
        AI_SETGOAL,
        ATTACH_SENSOR,
        DETACH_SENSOR,
        ATTACH,
        DETACH,
        ACTIVATE_SPECIAL,
        MAX
    }
}
