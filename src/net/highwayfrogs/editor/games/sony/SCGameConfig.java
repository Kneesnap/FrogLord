package net.highwayfrogs.editor.games.sony;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.vlo.VLOArchive;

import java.util.*;
import java.util.function.BiFunction;


/**
 * Represents configuration data for a particular version of a Sony Cambridge / Millennium Interactive game.
 * Created by Kneesnap on 9/7/2023.
 */
@Getter
public class SCGameConfig {
    private final String internalName;
    private Config config;

    // Loaded configuration data.
    private String displayName;
    private long ramPointerOffset;
    private int MWIOffset;
    private int MWILength;
    private int bmpPointerAddress;
    private SCGamePlatform platform;
    private NameBank soundBank;
    private NameBank animationBank;
    private final Map<String, int[]> hiddenPartIds = new HashMap<>();
    private final Map<String, String> mofRenderOverrides = new HashMap<>();
    private final Map<String, String> mofParentOverrides = new HashMap<>();
    private final List<String> fallbackFileNames = new ArrayList<>();
    private final Map<Short, String> imageNames = new HashMap<>();

    public static final String CFG_DISPLAY_NAME = "name";
    public static final String CFG_GAME_TYPE = "game";
    private static final String CFG_FILE_NAMES = "Files";
    private static final String CFG_CHILD_IMAGE_NAMES = "ImageNames";
    private static final String CFG_CHILD_MOF_FORCE_VLO = "ForceVLO";

    public SCGameConfig(String internalName) {
        this.internalName = internalName;
    }

    /**
     * Load data from the specified config.
     * @param config The config to load data from.
     */
    public void loadData(Config config) {
        if (this.config != null)
            throw new RuntimeException("The config " + this.internalName + " already has its data loaded.");

        if (config == null)
            return;

        this.config = config;
        loadBanks(config);
        readBasicConfigData(config);
        readFallbackFileNames(config);
        readHiddenParts(config);
        readMofOverrides(config);
        readMofParentOverrides(config);
        readConfiguredImageNames(config);
    }

    private void loadBanks(Config config) {
        this.soundBank = loadBank(config, "soundList", null, "sounds", "Sound", true);
        this.animationBank = loadBank(config, "animList", null, "anims", true, (bank, index) -> bank.size() <= 1 ? "Default Animation" : "Animation " + index);
    }

    private void readBasicConfigData(Config config) {
        this.displayName = config.getString(CFG_DISPLAY_NAME);
        this.platform = config.getEnum("platform", SCGamePlatform.class);
        this.MWIOffset = config.getInt("mwiOffset");
        this.MWILength = config.getInt("mwiLength");
        this.ramPointerOffset = config.getLong("ramOffset"); // If I have an offset in a file, adding this number will give its pointer.
        this.bmpPointerAddress = config.getInt("bmpPointerAddress", 0); // Generally right at the start of the 'data' section.
    }

    protected NameBank loadBank(Config config, String configKey, String defaultBank, String bankName, String unknownName, boolean addChildrenToMainBank) {
        return loadBank(config, configKey, defaultBank, bankName, addChildrenToMainBank, (bank, index) -> "Unknown " + unknownName + " [" + index + "]");
    }

    protected NameBank loadBank(Config config, String configKey, String defaultBank, String bankName, boolean addChildrenToMainBank, BiFunction<NameBank, Integer, String> nameHandler) {
        String animBankName = config.getString(configKey, defaultBank);
        if (animBankName == null)
            return NameBank.EMPTY_BANK;

        return NameBank.readBank(bankName, animBankName, addChildrenToMainBank, nameHandler);
    }

    /**
     * Reads a list of hardcoded file names from the config.
     */
    private void readFallbackFileNames(Config config) {
        this.fallbackFileNames.clear();
        if (config.hasChild(CFG_FILE_NAMES))
            this.fallbackFileNames.addAll(config.getChild(CFG_FILE_NAMES).getText());
    }

    private void readHiddenParts(Config config) {
        this.hiddenPartIds.clear();
        if (!config.hasChild("HiddenParts"))
            return;

        Config hiddenPartsCfg = config.getChild("HiddenParts");
        for (String key : hiddenPartsCfg.keySet()) {
            int[] hiddenParts = hiddenPartsCfg.getIntArray(key);
            Arrays.sort(hiddenParts);
            this.hiddenPartIds.put(key, hiddenParts);
        }
    }

    private void readMofOverrides(Config config) {
        this.mofRenderOverrides.clear();
        if (!config.hasChild("MofOverride"))
            return;

        Config mofOverridesCfg = config.getChild("MofOverride");
        for (String key : mofOverridesCfg.keySet())
            this.mofRenderOverrides.put(key, mofOverridesCfg.getString(key));
    }

    private void readMofParentOverrides(Config config) {
        this.mofParentOverrides.clear();
        if (!config.hasChild("MofParentOverride"))
            return;

        Config mofParentOverridesCfg = config.getChild("MofParentOverride");
        for (String key : mofParentOverridesCfg.keySet())
            this.mofParentOverrides.put(key, mofParentOverridesCfg.getString(key));
    }

    private void readConfiguredImageNames(Config config) {
        this.imageNames.clear();
        if (!config.hasChild(CFG_CHILD_IMAGE_NAMES))
            return;

        Config imageNameCfg = config.getChild(CFG_CHILD_IMAGE_NAMES);
        for (String key : imageNameCfg.keySet()) {
            short textureId;
            try {
                textureId = Short.parseShort(key);
            } catch (NumberFormatException nfe) {
                System.out.println("Skipping non-integer key '" + key + "' as texture ID / image file name pair in version config '" + this.internalName + "'.");
                continue;
            }

            this.imageNames.put(textureId, imageNameCfg.getString(key));
        }
    }

    /**
     * Get the forced VLO file for a given string.
     * @param name The name to get the vlo for.
     * @return forcedVLO
     */
    public VLOArchive getForcedVLO(SCGameInstance instance, String name) {
        if (instance == null)
            throw new RuntimeException("Cannot find the overridden VLO file for '" + name + "' since a null instance was given.");
        if (instance.getMainArchive() == null)
            throw new RuntimeException("Cannot find the overridden VLO file for '" + name + "' since the file archive has not been loaded yet.");

        if (!getConfig().hasChild(CFG_CHILD_MOF_FORCE_VLO))
            return null;

        Config childConfig = getConfig().getChild(CFG_CHILD_MOF_FORCE_VLO);
        if (!childConfig.has(name))
            return null;

        String vloName = childConfig.getString(name);
        return instance.getMainArchive().resolveForEachFile(VLOArchive.class, vlo -> vlo.getFileDisplayName().startsWith(vloName) ? vlo : null);
    }

    /**
     * Write the config identifier to the end of the executable, so it will automatically know which config to use when this is loaded.
     */
    public byte[] applyConfigIdentifier(byte[] oldExeBytes) {
        if (this.internalName == null)
            return oldExeBytes;

        // Test if the executable already has the identifier at the end.
        byte[] identifierBytes = this.internalName.getBytes();
        boolean hasIdentifierAlready = oldExeBytes.length >= identifierBytes.length
                && Arrays.equals(Arrays.copyOfRange(oldExeBytes, oldExeBytes.length - identifierBytes.length, oldExeBytes.length), identifierBytes);
        if (hasIdentifierAlready)
            return oldExeBytes;

        // Write the identifier to a new array.
        byte[] newExeBytes = new byte[oldExeBytes.length + identifierBytes.length];
        System.arraycopy(oldExeBytes, 0, newExeBytes, 0, oldExeBytes.length);
        System.arraycopy(identifierBytes, 0, newExeBytes, oldExeBytes.length, identifierBytes.length);
        return newExeBytes;
    }
}