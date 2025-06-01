package net.highwayfrogs.editor.file.config;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.IGameType;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Holds a bunch of names,
 * Created by Kneesnap on 2/24/2019.
 */
public class NameBank {
    @Getter private final Config config;
    @Getter private final List<String> names = new ArrayList<>();
    private final Map<String, NameBank> subBanks = new HashMap<>();
    private BiFunction<NameBank, Integer, String> unknownMaker;
    private int spoofSize;

    public static final NameBank EMPTY_BANK = new NameBank(null, new ArrayList<>(), null);
    private static final NameBank EMPTY_MODIFIABLE_BANK = new NameBank(null, new ArrayList<>(), null);

    private NameBank(Config config, Collection<String> loadValues, BiFunction<NameBank, Integer, String> unknownMaker) {
        this.config = config;
        this.unknownMaker = unknownMaker;
        this.names.addAll(loadValues);
    }

    /**
     * Gets the index safely for the given name.
     * @param name The name to get the index from.
     * @return index, or -1
     */
    public int getIndexForName(String name) {
        if (name == null)
            throw new NullPointerException("name");

        for (int i = 0; i < this.names.size(); i++) {
            String testName = this.names.get(i);
            if (name.equalsIgnoreCase(testName))
                return i;
        }

        return -1;
    }

    /**
     * Gets the name safely for the given index.
     * @param id The index to get.
     * @return name
     */
    public String getName(int id) {
        return hasName(id) ? names.get(id) : getDefaultNameFor(id);
    }

    /**
     * Gets the default name for the given id, even if it's present.
     * @param id The id to get the default name for.
     * @return defaultName
     */
    public String getDefaultNameFor(int id) {
        return unknownMaker != null ? unknownMaker.apply(this, id) : "???? (Entry #" + id + " has not been named)";
    }

    /**
     * Gets the default name for the given id, even if it's present.
     * @param id The id to get the default name for.
     * @return defaultName
     */
    public String getEmptyChildNameFor(int id, int size) {
        EMPTY_MODIFIABLE_BANK.unknownMaker = unknownMaker;
        EMPTY_MODIFIABLE_BANK.spoofSize = size;
        return EMPTY_MODIFIABLE_BANK.getDefaultNameFor(id);
    }

    /**
     * Check if we have a name stored for a given id.
     * @param id The id to test.
     * @return hasName
     */
    public boolean hasName(int id) {
        return id >= 0 && id < names.size();
    }

    /**
     * Gets a child bank. Returns null if not found.
     * @param bankName The name of the bank.
     * @return childBank
     */
    public NameBank getChildBank(String bankName) {
        return subBanks.get(bankName);
    }

    /**
     * Link a bank to another bank.
     * @param oldName The source bank.
     * @param newName The destination bank.
     */
    public void linkChildBank(String oldName, String newName) {
        if (subBanks.containsKey(newName))
            return; // Already linked.

        NameBank linkBank = getChildBank(oldName);
        if (linkBank != null)
            subBanks.put(newName, linkBank);
    }

    /**
     * Return the amount of elements this bank contains.
     * @return size
     */
    public int size() {
        return isEmpty() ? this.spoofSize : this.names.size();
    }

    private boolean isEmpty() {
        return this == EMPTY_BANK || this == EMPTY_MODIFIABLE_BANK;
    }

    /**
     * Loads a name bank from a config.
     * @param folder       The folder to read the name bank from.
     * @param configName   The name of the bank to read.
     * @param unknownMaker What to return when an id wasn't found. Null is allowed.
     * @return newBank
     */
    public static NameBank readBank(IGameType gameType, String folder, String configName, boolean addChildrenToMainBank, BiFunction<NameBank, Integer, String> unknownMaker) {
        Config config = new Config(gameType.getEmbeddedResourceStream( folder + "/" + configName + ".cfg"));
        NameBank bank = new NameBank(config, config.getText(), unknownMaker);

        for (Config childConfig : config.getOrderedChildren()) {
            List<String> childData = childConfig.getText();
            bank.subBanks.put(childConfig.getName(), new NameBank(childConfig, childData, unknownMaker));
            if (addChildrenToMainBank)
                bank.names.addAll(childData);
        }

        return bank;
    }
}