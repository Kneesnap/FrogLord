package net.highwayfrogs.editor.file.config;

import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Holds a bunch of names,
 * Created by Kneesnap on 2/24/2019.
 */
public class NameBank {
    private List<String> names = new ArrayList<>();
    private Map<String, NameBank> subBanks = new HashMap<>();
    private BiFunction<NameBank, Integer, String> unknownMaker;
    private int spoofSize;

    private static final NameBank EMPTY_BANK = new NameBank(new ArrayList<>(), null);

    private NameBank(Collection<String> loadValues, BiFunction<NameBank, Integer, String> unknownMaker) {
        this.unknownMaker = unknownMaker;
        this.names.addAll(loadValues);
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
        return unknownMaker != null ? unknownMaker.apply(this, id) : "????????";
    }

    /**
     * Gets the default name for the given id, even if it's present.
     * @param id The id to get the default name for.
     * @return defaultName
     */
    public String getEmptyChildNameFor(int id, int size) {
        EMPTY_BANK.unknownMaker = unknownMaker;
        EMPTY_BANK.spoofSize = size;
        return EMPTY_BANK.getDefaultNameFor(id);
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
        return this == EMPTY_BANK;
    }

    /**
     * Loads a name bank from a config.
     * @param folder       The folder to read the name bank from.
     * @param configName   The name of the bank to read.
     * @param unknownMaker What to return when an id wasn't found. Null is allowed.
     * @return newBank
     */
    @SneakyThrows
    public static NameBank readBank(String folder, String configName, BiFunction<NameBank, Integer, String> unknownMaker) {
        Config config = new Config(Utils.getResourceStream("banks/" + folder + "/" + configName + ".cfg"));
        NameBank bank = new NameBank(config.getText(), unknownMaker);

        for (String childName : config.getOrderedChildren()) {
            List<String> childData = config.getChild(childName).getText();
            bank.subBanks.put(childName, new NameBank(childData, unknownMaker));
            bank.names.addAll(childData);
        }

        return bank;
    }
}
