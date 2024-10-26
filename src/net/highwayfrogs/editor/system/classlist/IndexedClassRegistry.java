package net.highwayfrogs.editor.system.classlist;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.BitReader;
import net.highwayfrogs.editor.file.writer.BitWriter;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.system.classlist.GlobalClassRegistry.ClassEntry;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A list of classes which are accessible by an index.
 * Primarily used to allow space-efficient specification of which child class to create when loading an object from binary data.
 * Created by Kneesnap on 7/16/2024.
 */
public class IndexedClassRegistry<TBase> implements IBinarySerializable {
    @Getter private final GlobalClassRegistry<TBase> globalClassRegistry;
    private final List<ClassEntry<TBase>> entries = new ArrayList<>();
    private final Map<ClassEntry<TBase>, Integer> idsByEntry = new HashMap<>();
    private int bitCount = 0;

    private static final short NULL_CLASS_ENTRY_INDICATOR = 0xFF;
    public static final short MAXIMUM_IDENTIFIER_LENGTH = NULL_CLASS_ENTRY_INDICATOR - 1;

    public IndexedClassRegistry(GlobalClassRegistry<TBase> globalClassRegistry) {
        this.globalClassRegistry = globalClassRegistry;
    }

    /**
     * Clears the tracked entries.
     */
    public void clear() {
        this.entries.clear();
        this.idsByEntry.clear();
        this.bitCount = 0;
    }

    @Override
    public void load(DataReader reader) {
        clear();

        int entryCount = reader.readInt();
        for (int i = 0; i < entryCount; i++) {
            short identifierLength = reader.readUnsignedByteAsShort();
            if (identifierLength == NULL_CLASS_ENTRY_INDICATOR) {
                this.entries.add(null);
                this.idsByEntry.put(null, this.entries.size());
                continue;
            }

            // Read & lookup global class data.
            String identifier = reader.readString(identifierLength + 1);
            int fullClassNameLength = reader.readUnsignedShortAsInt();
            String fullClassName = reader.readString(fullClassNameLength);
            ClassEntry<TBase> classEntry = this.globalClassRegistry.getClassEntry(identifier, fullClassName);
            if (classEntry == null)
                throw new RuntimeException("Failed to resolve class registry entry for identifier: '" + identifier + "', fullClassName: '" + fullClassName + "'.");

            // Register.
            this.idsByEntry.put(classEntry, this.entries.size());
            this.entries.add(classEntry);
        }

        ensureBitCountLargeEnough();
    }

    @Override
    public void save(DataWriter writer) {
        ensureBitCountLargeEnough();
        writer.writeInt(this.entries.size());
        for (int i = 0; i < this.entries.size(); i++) {
            ClassEntry<TBase> classEntry = this.entries.get(i);
            if (classEntry == null) {
                writer.writeUnsignedByte(NULL_CLASS_ENTRY_INDICATOR);
                continue;
            }

            // Write identifier.
            String identifier = classEntry.getIdentifier();
            writer.writeUnsignedByte((short) (identifier.length() - 1));
            writer.writeStringBytes(identifier);

            // Write full class name as fallback.
            String className = classEntry.getClass().getName();
            writer.writeUnsignedShort(className.length());
            writer.writeStringBytes(className);
        }
    }

    private void ensureBitCountLargeEnough() {
        while (this.bitCount < 31 && this.entries.size() > (1 << this.bitCount) - 1)
            this.bitCount++;
    }

    /**
     * Reads a list of class ref IDs and creates corresponding objects with the given constructor arguments.
     * @param reader the reader to read the class IDs from
     * @param handler the callback run for each new object
     */
    public <TParam1> void readClassRefIdBitArray(DataReader reader, Consumer<TBase> handler, Class<? extends TParam1> typeParam1, TParam1 param1) {
        int valueCount = reader.readInt();
        int byteArrayLength = reader.readInt();
        byte[] rawBitArrayBytes = reader.readBytes(byteArrayLength);

        BitReader bitReader = new BitReader(rawBitArrayBytes, 0);
        for (int i = 0; i < valueCount; i++) {
            TBase newInstance = constructNextClassReference(bitReader, typeParam1, param1);
            handler.accept(newInstance);
        }
    }

    /**
     * Reads a list of class ref IDs and creates corresponding objects with the given constructor arguments.
     * @param reader the reader to read the class IDs from
     * @param handler the callback run for each new object
     */
    public <TParam1, TParam2> void readClassRefIdBitArray(DataReader reader, Consumer<TBase> handler, Class<? extends TParam1> typeParam1, TParam1 param1, Class<? extends TParam2> typeParam2, TParam2 param2) {
        int valueCount = reader.readInt();
        int byteArrayLength = reader.readInt();
        byte[] rawBitArrayBytes = reader.readBytes(byteArrayLength);

        BitReader bitReader = new BitReader(rawBitArrayBytes, 0);
        for (int i = 0; i < valueCount; i++) {
            TBase newInstance = constructNextClassReference(bitReader, typeParam1, param1, typeParam2, param2);
            handler.accept(newInstance);
        }
    }

    /**
     * Reads a list of class ref IDs and creates corresponding objects with the given constructor arguments.
     * @param reader the reader to read the class IDs from
     * @param handler the callback run for each new object
     */
    public <TParam1, TParam2, TParam3> void readClassRefIdBitArray(DataReader reader, Consumer<TBase> handler, Class<? extends TParam1> typeParam1, TParam1 param1, Class<? extends TParam2> typeParam2, TParam2 param2, Class<? extends TParam3> typeParam3, TParam3 param3) {
        int valueCount = reader.readInt();
        int byteArrayLength = reader.readInt();
        byte[] rawBitArrayBytes = reader.readBytes(byteArrayLength);

        BitReader bitReader = new BitReader(rawBitArrayBytes, 0);
        for (int i = 0; i < valueCount; i++) {
            TBase newInstance = constructNextClassReference(bitReader, typeParam1, param1, typeParam2, param2, typeParam3, param3);
            handler.accept(newInstance);
        }
    }

    /**
     * Writes the class ref IDs of the provided values into the bit array form.
     * @param values the objects to write the class IDs for
     */
    public void writeClassRefIdsAsBitArray(DataWriter writer, List<? extends TBase> values) {
        byte[] bitArray = writeClassRefIdsToBitArray(values);
        writer.writeInt(values.size()); // Value count.
        writer.writeInt(bitArray.length); // Number of bytes taken up by the bit array.
        writer.writeBytes(bitArray);
    }

    /**
     * Writes the class ref IDs of the provided values into a bit array
     * @param values the objects to write the class IDs for
     * @return bitArrayAsByteArray
     */
    private byte[] writeClassRefIdsToBitArray(List<? extends TBase> values) {
        BitWriter bitWriter = new BitWriter();
        clear();
        for (int i = 0; i < values.size(); i++)
            register(values.get(i));

        ensureBitCountLargeEnough();
        for (int i = 0; i < values.size(); i++) {
            TBase value = values.get(i);
            writeNextClassReference(bitWriter, value);
        }

        return bitWriter.toByteArray();
    }

    /**
     * Registers the class used by the specific object in the registry, if it is not already registered.
     * @param object the object to register
     */
    @SuppressWarnings("unchecked")
    private void register(TBase object) {
        ClassEntry<TBase> classEntry;
        if (object != null) {
            classEntry = this.globalClassRegistry.getClassEntry((Class<? extends TBase>) object.getClass());
            if (classEntry == null)
                throw new RuntimeException("Object " + Utils.getSimpleName(object) + " could not be saved, as its class is not registered in the " + Utils.getSimpleName(this.globalClassRegistry) + ".");
        } else {
            classEntry = null;
        }

        // Register the entries.
        if (!this.idsByEntry.containsKey(classEntry)) {
            this.idsByEntry.put(classEntry, this.entries.size());
            this.entries.add(classEntry);
        }
    }

    /**
     * Reads the next class entry reference from the reader.
     * The method which runs on bits is more space-efficient than the one which does not.
     * @param reader the reader to read the next class entry reference from
     * @return nextClassReference
     */
    public ClassEntry<TBase> readNextClassReference(BitReader reader) {
        int classId = reader.readBits(this.bitCount);
        return this.entries.get(classId);
    }

    /**
     * Constructs the next class entry reference from the reader using the provided constructor arguments.
     * The method which runs on bits is more space-efficient than the one which does not.
     * @param reader the reader to read the next class entry reference from
     * @return nextClassReference
     */
    public <TParam> TBase constructNextClassReference(BitReader reader, Class<? extends TParam> paramType1, TParam param1) {
        ClassEntry<TBase> classEntry = readNextClassReference(reader);
        return classEntry != null ? classEntry.newInstance(paramType1, param1) : null;
    }

    /**
     * Constructs the next class entry reference from the reader using the provided constructor arguments.
     * The method which runs on bits is more space-efficient than the one which does not.
     * @param reader the reader to read the next class entry reference from
     * @return nextClassReference
     */
    public <TParam1, TParam2> TBase constructNextClassReference(BitReader reader, Class<? extends TParam1> paramType1, TParam1 param1, Class<? extends TParam2> paramType2, TParam2 param2) {
        ClassEntry<TBase> classEntry = readNextClassReference(reader);
        return classEntry != null ? classEntry.newInstance(paramType1, param1, paramType2, param2) : null;
    }

    /**
     * Constructs the next class entry reference from the reader using the provided constructor arguments.
     * The method which runs on bits is more space-efficient than the one which does not.
     * @param reader the reader to read the next class entry reference from
     * @return nextClassReference
     */
    public <TParam1, TParam2, TParam3> TBase constructNextClassReference(BitReader reader, Class<? extends TParam1> paramType1, TParam1 param1, Class<? extends TParam2> paramType2, TParam2 param2, Class<? extends TParam3> paramType3, TParam3 param3) {
        ClassEntry<TBase> classEntry = readNextClassReference(reader);
        return classEntry != null ? classEntry.newInstance(paramType1, param1, paramType2, param2, paramType3, param3) : null;
    }

    /**
     * Reads the next class entry reference from the reader.
     * The other one which works with the bit reader more space-efficient.
     * @param reader the reader to read the next class from.
     * @return nextClassReference
     */
    public ClassEntry<TBase> readNextClassReference(DataReader reader) {
        int classId;
        if (this.entries.size() > 0x10000) {
            classId = reader.readInt();
        } else if (this.entries.size() > 0x100) {
            classId = reader.readUnsignedShortAsInt();
        } else {
            classId = reader.readUnsignedByte();
        }

        return this.entries.get(classId);
    }

    /**
     * Constructs the next class entry reference from the reader using the provided constructor arguments.
     * The other one which works with the bit reader more space-efficient.
     * @param reader the reader to read the next class entry reference from
     * @return nextClassReference
     */
    public <TParam> TBase constructNextClassReference(DataReader reader, Class<? extends TParam> paramType1, TParam param1) {
        ClassEntry<TBase> classEntry = readNextClassReference(reader);
        return classEntry != null ? classEntry.newInstance(paramType1, param1) : null;
    }

    /**
     * Constructs the next class entry reference from the reader using the provided constructor arguments.
     * The other one which works with the bit reader more space-efficient.
     * @param reader the reader to read the next class entry reference from
     * @return nextClassReference
     */
    public <TParam1, TParam2> TBase constructNextClassReference(DataReader reader, Class<? extends TParam1> paramType1, TParam1 param1, Class<? extends TParam2> paramType2, TParam2 param2) {
        ClassEntry<TBase> classEntry = readNextClassReference(reader);
        return classEntry != null ? classEntry.newInstance(paramType1, param1, paramType2, param2) : null;
    }

    /**
     * Constructs the next class entry reference from the reader using the provided constructor arguments.
     * The other one which works with the bit reader more space-efficient.
     * @param reader the reader to read the next class entry reference from
     * @return nextClassReference
     */
    public <TParam1, TParam2, TParam3> TBase constructNextClassReference(DataReader reader, Class<? extends TParam1> paramType1, TParam1 param1, Class<? extends TParam2> paramType2, TParam2 param2, Class<? extends TParam3> paramType3, TParam3 param3) {
        ClassEntry<TBase> classEntry = readNextClassReference(reader);
        return classEntry != null ? classEntry.newInstance(paramType1, param1, paramType2, param2, paramType3, param3) : null;
    }

    /**
     * Writes the next object reference to the writer.
     * The method which runs on bits is more space-efficient than the one which does not.
     * @param writer the writer to write the next class entry reference to
     * @param object the object to get the class ID from to write
     */
    public void writeNextClassReference(BitWriter writer, TBase object) {
        writeNextClassReference(writer, this.globalClassRegistry.requireClassEntry(object));
    }

    /**
     * Writes the next class entry reference to the writer.
     * The other one which works with the bit writer more space-efficient.
     * @param writer the writer to write the next class entry reference to.
     * @param object the object to get the class ID from to write
     */
    public void writeNextClassReference(DataWriter writer, TBase object) {
        writeNextClassReference(writer, this.globalClassRegistry.requireClassEntry(object));
    }

    /**
     * Writes the next class entry reference to the writer.
     * The method which runs on bits is more space-efficient than the one which does not.
     * @param writer the writer to write the next class entry reference to
     */
    public void writeNextClassReference(BitWriter writer, ClassEntry<TBase> classEntry) {
        Integer classId = this.idsByEntry.get(classEntry);
        if (classId == null)
            throw new RuntimeException("The class reference " + classEntry + "' was not registered, so the class reference cannot be written!");

        writer.writeBits(classId, this.bitCount);
    }

    /**
     * Writes the next class entry reference to the writer.
     * The other one which works with the bit writer more space-efficient.
     * @param writer the writer to write the next class entry reference to.
     */
    public void writeNextClassReference(DataWriter writer, ClassEntry<TBase> classEntry) {
        Integer classId = this.idsByEntry.get(classEntry);
        if (classId == null)
            throw new RuntimeException("The class reference " + classEntry + "' was not registered, so the class reference cannot be written!");

        if (this.entries.size() > 0x10000) {
            writer.writeInt(classId);
        } else if (this.entries.size() > 0x100) {
            writer.writeUnsignedShort(classId);
        } else {
            writer.writeUnsignedByte((short) (int) classId);
        }
    }
}