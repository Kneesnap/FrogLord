package net.highwayfrogs.editor.games.tgq.script;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.tgq.toc.KCResourceID;
import net.highwayfrogs.editor.games.tgq.toc.kcCResource;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a list of scripts.
 * The logic has been determined by reading kcCScriptMgr::Load() from the PS2 PAL version.
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
public class kcScriptList extends kcCResource {
    private final List<kcScript> scripts = new ArrayList<>();
    private byte[] rawUnhandledData;

    public static final String GLOBAL_SCRIPT_NAME = "scriptdata";

    public kcScriptList(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.RAW);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Read interim list.
        kcScriptListInterim interim = new kcScriptListInterim();
        interim.load(reader);

        // Read unhandled data.
        this.rawUnhandledData = null;
        if (reader.hasMore()) {
            this.rawUnhandledData = reader.readBytes(reader.getRemaining());
            System.out.println("The kcScriptList '" + getName() + "' "
                    + (getParentFile() != null ? "' " + getParentFile().getFilePath() : "")
                    + " has " + this.rawUnhandledData.length + " unread/unhandled bytes.");
        }

        // Convert interim list to script list.
        this.scripts.clear();
        for (int i = 0; i < interim.getEntries().size(); i++)
            this.scripts.add(kcScript.loadScript(interim, interim.getEntries().get(i)));
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);

        // Convert scripts to interim data.
        List<kcScriptTOC> entries = new ArrayList<>();
        List<Long> statusData = new ArrayList<>();
        List<kcScriptEffect> effects = new ArrayList<>();
        for (int i = 0; i < this.scripts.size(); i++) {
            kcScript script = this.scripts.get(i);

            // Create new entry. TODO: "hasScriptType" is not yet understood, and hardcoding 32 may not be a good long-term practice.
            kcScriptTOC newEntry = new kcScriptTOC(32, statusData.size(), script.getFunctions().size(), script.getEffectCount());
            entries.add(newEntry);

            // Add 'cause' & 'effect' data.
            for (int j = 0; j < script.getFunctions().size(); j++) {
                kcScriptFunction function = script.getFunctions().get(j);
                function.saveCauseData(statusData, (long) effects.size() * kcScriptEffect.SIZE_IN_BYTES);
                effects.addAll(function.getEffects());
            }
        }

        // Write interim data.
        kcScriptListInterim interim = new kcScriptListInterim(entries, statusData, effects);
        interim.save(writer);

        // Write unhandled data.
        if (this.rawUnhandledData != null && this.rawUnhandledData.length > 0)
            writer.writeBytes(this.rawUnhandledData);
    }

    /**
     * Writes the script list to a string builder.
     * @param builder  The builder to write the script to.
     * @param settings The settings used to build the output.
     */
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        for (int i = 0; i < this.scripts.size(); i++) {
            builder.append("// Script #").append(i + 1).append(":\n");
            this.scripts.get(i).toString(builder, settings);
            builder.append('\n');
        }
    }

    @Getter
    public static class kcScriptEffect extends GameObject {
        @Setter private transient long dataOffset;
        private long scriptType;
        private kcActionID action;
        private int destObjectHash;
        private final kcParam[] parameters = new kcParam[4];
        private byte[] unhandledRawBytes;

        public static final int SIZE_IN_BYTES = 0x20; // 32 bytes. This in theory could differ with other versions.

        @Override
        public void load(DataReader reader) {
            int startIndex = reader.getIndex();
            int storedSize = reader.readInt();
            if (storedSize < 0x20)
                throw new RuntimeException("Expected a script effect to be at least 32 bytes, but was " + storedSize + ".");

            this.scriptType = reader.readUnsignedIntAsLong();
            this.action = kcActionID.getActionByOpcode(reader.readInt());
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
            writer.writeUnsignedInt(this.scriptType);
            writer.writeUnsignedInt(this.action.ordinal());
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


            builder.append("[Script Type: ").append(this.scriptType).append(", Target: ");

            String hashName;
            if (settings != null && settings.getNamesByHash() != null && (hashName = settings.getNamesByHash().get(this.destObjectHash)) != null) {
                builder.append('"').append(hashName.replace("\"", "\\\"")).append('"');
            } else {
                builder.append(Utils.toHexString(this.destObjectHash));
            }

            builder.append("] ");
            kcAction.writeAction(builder, this.action, this.parameters, settings);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            this.toString(builder, new kcScriptDisplaySettings(null, true, true));
            return builder.toString();
        }
    }

    /**
     * Reimplementation of the 'kcScriptTOC' class.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class kcScriptTOC extends GameObject {
        private long hasScriptType; // TODO: Not sure. Seems to be a flag.
        private long causeStartIndex;
        private long causeCount;
        private long effectCount;

        @Override
        public void load(DataReader reader) {
            this.hasScriptType = reader.readUnsignedIntAsLong();
            this.causeStartIndex = reader.readUnsignedIntAsLong();
            this.causeCount = reader.readUnsignedIntAsLong();
            this.effectCount = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedInt(this.hasScriptType);
            writer.writeUnsignedInt(this.causeStartIndex);
            writer.writeUnsignedInt(this.causeCount);
            writer.writeUnsignedInt(this.effectCount);
        }
    }
}
