package net.highwayfrogs.editor.games.tgq.script.interim;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.script.effect.kcScriptEffect;
import net.highwayfrogs.editor.games.tgq.script.kcParam;
import net.highwayfrogs.editor.games.tgq.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.tgq.script.kcScriptEffectType;

/**
 * Represents a script effect in an interim stage between raw bytes and the final in-memory representation of a script effect.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
public class kcInterimScriptEffect extends GameObject {
    @Setter private transient long dataOffset;
    private kcScriptEffectType effectType;
    private int effectID;
    private int destObjectHash;
    private final kcParam[] parameters = new kcParam[4];
    private byte[] unhandledRawBytes;

    public static final int SIZE_IN_BYTES = 0x20; // 32 bytes. This in theory could differ with other versions.

    /**
     * Load instance data from a kcScriptEffect object.
     * @param effect The script to load data from.
     */
    public void load(kcScriptEffect effect) {
        this.effectType = effect.getEffectType();
        this.effectID = effect.getEffectID();
        this.destObjectHash = effect.getTargetEntityHash();
        this.unhandledRawBytes = null;

        kcParamWriter writer = new kcParamWriter(this.parameters);
        effect.save(writer);
        writer.clearRemaining();
    }

    @Override
    public void load(DataReader reader) {
        int startIndex = reader.getIndex();
        int storedSize = reader.readInt();
        if (storedSize < 0x20)
            throw new RuntimeException("Expected a script effect to be at least 32 bytes, but was " + storedSize + ".");

        this.effectType = kcScriptEffectType.getEffectType(reader.readInt(), false);
        this.effectID = reader.readInt();
        this.destObjectHash = reader.readInt();
        for (int i = 0; i < this.parameters.length; i++)
            this.parameters[i] = kcParam.readParam(reader);

        // Handle any remaining bytes.
        int readSize = reader.getIndex() - startIndex;
        if (readSize != storedSize) {
            this.unhandledRawBytes = reader.readBytes(storedSize - readSize);
            System.out.println("Script Effect [" + this + "] was loaded from " + readSize + " bytes, but " + storedSize + " were supposed to be read.");
        } else {
            this.unhandledRawBytes = null;
        }
    }

    @Override
    public void save(DataWriter writer) {
        int startIndex = writer.writeNullPointer();
        writer.writeInt(this.effectType.getValue());
        writer.writeInt(this.effectID);
        writer.writeInt(this.destObjectHash);
        for (int i = 0; i < this.parameters.length; i++)
            kcParam.writeParam(writer, this.parameters[i]);

        if (this.unhandledRawBytes != null && this.unhandledRawBytes.length > 0)
            writer.writeBytes(this.unhandledRawBytes);

        // Write the length.
        writer.writeAddressAt(startIndex, writer.getIndex() - startIndex);
    }

    /**
     * Writes the script to a string builder.
     * @param builder  The builder to write the script to.
     * @param settings The settings for displaying the output.
     */
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("[Effect: ").append(this.effectType).append('/').append(this.effectID).append(", Target: ");
        builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.destObjectHash, true));
        builder.append("] Data:");

        // Write raw data.
        for (int i = 0; i < this.parameters.length; i++) {
            kcParam param = this.parameters[i];
            builder.append(' ');
            builder.append(kcScriptDisplaySettings.getHashDisplay(settings, param.getAsInteger(), false));
        }
    }

    /**
     * Converts this interim container to the object we'll use instead.
     */
    public kcScriptEffect toScriptEffect() {
        kcScriptEffect newEffect = this.effectType.newInstance(this.effectID);
        newEffect.setTargetEntityHash(this.destObjectHash);
        kcParamReader reader = new kcParamReader(this.parameters);
        newEffect.load(reader);

        // Find unused arguments.
        int lastReadArgument = reader.getCurrentIndex();
        int lastUnusedArgument = -1;
        while (reader.hasMore()) {
            kcParam param = reader.next();
            if (param.getAsInteger() != 0)
                lastUnusedArgument = reader.getCurrentIndex();
        }

        // Store unused arguments.
        if (lastUnusedArgument >= 0) {
            int unusedCount = lastUnusedArgument - lastReadArgument;
            System.out.println("There was a " + this.effectType + "/" + this.effectID + " effect with at least " + unusedCount + " unread arguments.");
        }

        return newEffect;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        this.toString(builder, kcScriptDisplaySettings.DEFAULT_SETTINGS);
        return builder.toString();
    }
}

