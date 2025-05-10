package net.highwayfrogs.editor.games.sony;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.generic.GameConfig;

import java.util.*;


/**
 * Represents configuration data for a particular version of a Sony Cambridge / Millennium Interactive game.
 * Created by Kneesnap on 9/7/2023.
 */
@Getter
public class SCGameConfig extends GameConfig {
    private long[] executableChecksums = EMPTY_LONG_ARRAY;
    private long overrideRamOffset;
    private long overlayTableOffset;
    private int MWIOffset;
    private int MWILength;
    private int bmpPointerAddress;
    private boolean mwdLooseFiles;
    private SCGameRegion region;
    private NameBank soundBank;
    private NameBank animationBank;
    private final Map<String, int[]> hiddenPartIds = new HashMap<>();
    private final Map<String, String> mofRenderOverrides = new HashMap<>();
    private final Map<String, String> mofParentOverrides = new HashMap<>();
    private final List<String> fallbackFileNames = new ArrayList<>();
    private final Map<Short, String> imageNames = new HashMap<>();

    private static final String CFG_FILE_NAMES = "Files";
    private static final String CFG_CHILD_IMAGE_NAMES = "ImageNames";
    private static final String CFG_CHILD_MOF_FORCE_VLO = "ForceVLO";
    private static final long[] EMPTY_LONG_ARRAY = new long[0];

    public SCGameConfig(String internalName) {
        super(internalName);
    }

    @Override
    protected void readConfigData(Config config) {
        super.readConfigData(config);
        readBasicConfigData(config);
        loadBanks(config);
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
        String checksumsString = config.getString("exeChecksum", null);
        if (checksumsString == null || checksumsString.trim().isEmpty()) {
            this.executableChecksums = EMPTY_LONG_ARRAY;
        } else {
            String[] split = checksumsString.split("\\s*,\\s*");
            this.executableChecksums = new long[split.length];
            for (int i = 0; i < split.length; i++)
                this.executableChecksums[i] = Long.parseLong(split[i]);
        }

        this.mwdLooseFiles = config.getBoolean("mwdLooseFiles", false);
        this.region = config.getEnum("region", SCGameRegion.UNSPECIFIED);
        this.MWIOffset = config.getInt("mwiOffset");
        this.MWILength = config.getInt("mwiLength");
        this.overrideRamOffset = config.getLong("ramOffset", 0); // If I have an offset in a file, adding this number will give its pointer.
        this.overlayTableOffset = config.getLong("overlayTable", 0);
        this.bmpPointerAddress = config.getInt("bmpPointerAddress", 0); // Generally right at the start of the 'data' section.
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
                getLogger().warning("Skipping non-integer key '" + key + "' as texture ID / image file name pair in version config '" + getInternalName() + "'.");
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
}