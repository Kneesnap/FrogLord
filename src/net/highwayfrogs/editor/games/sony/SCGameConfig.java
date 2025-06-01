package net.highwayfrogs.editor.games.sony;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    private final Map<Integer, SCBssSymbol> bssSymbols = new HashMap<>();

    private static final String CFG_FILE_NAMES = "Files";
    private static final String CFG_CHILD_IMAGE_NAMES = "ImageNames";
    private static final String CFG_CHILD_BSS_SYMBOLS = "BssSymbols";
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
        readBssSymbols(config);
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

    private void readBssSymbols(Config config) {
        this.bssSymbols.clear();
        if (!config.hasChild(CFG_CHILD_BSS_SYMBOLS))
            return;

        Config hiddenPartsCfg = config.getChild(CFG_CHILD_BSS_SYMBOLS);
        for (String line : hiddenPartsCfg.getText()) {
            if (StringUtils.isNullOrWhiteSpace(line))
                continue;

            SCBssSymbol newSymbol = SCBssSymbol.parseBssSymbol(line);
            if (newSymbol == null) {
                getLogger().warning("Could not interpret '%s' as a BSS symbol.", line);
                continue;
            }

            this.bssSymbols.put(newSymbol.getAddress(), newSymbol);
        }
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

    @Getter
    @RequiredArgsConstructor
    public static class SCBssSymbol {
        private final int address;
        private final String name;
        private final int size;

        private static final Pattern REGEX_PATTERN = Pattern.compile("0x([a-fA-F0-9]{8}),([a-zA-Z_][a-zA-Z0-9_]+),([0-9]+)");

        /**
         * Parses the bss symbol from a line of text.
         * @param input the line of text to read
         * @return bssSymbol
         */
        public static SCBssSymbol parseBssSymbol(String input) {
            if (StringUtils.isNullOrEmpty(input))
                throw new NullPointerException("input");

            Matcher matcher = REGEX_PATTERN.matcher(input);
            if (!matcher.matches())
                return null;

            int address = (int) Long.parseLong(matcher.group(1), 16);
            String name = matcher.group(2);
            int size = Integer.parseInt(matcher.group(3));
            return new SCBssSymbol(address, name, size);
        }
    }
}