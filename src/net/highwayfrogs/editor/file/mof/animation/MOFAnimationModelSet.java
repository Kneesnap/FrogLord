package net.highwayfrogs.editor.file.mof.animation;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFBBox;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MR_ANIM_MODEL_SET struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MOFAnimationModelSet extends GameObject {
    private int type;
    private List<MOFAnimationModel> models = new ArrayList<>();
    private List<MOFAnimationCelSet> celSets = new ArrayList<>();
    private List<MOFBBox> bboxes = new ArrayList<>();

    @Override
    public void load(DataReader reader) {
        this.type = reader.readInt();

        byte modelCount = reader.readByte();
        byte celsetCount = reader.readByte();
        byte bboxCount = reader.readByte();
        reader.readByte(); // Padding.

        int modelPointer = reader.readInt();
        int celsetPointer = reader.readInt();
        int bboxPointer = reader.readInt();

        reader.jumpTemp(modelPointer);
        for (int i = 0; i < modelCount; i++) {
            MOFAnimationModel model = new MOFAnimationModel();
            model.load(reader);
        }
        reader.jumpReturn();

        //TODO: Load.
    }

    @Override
    public void save(DataWriter writer) {
        //TODO: save.
    }
}
