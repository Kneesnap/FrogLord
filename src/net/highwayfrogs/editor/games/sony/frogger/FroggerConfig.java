package net.highwayfrogs.editor.games.sony.frogger;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.games.sony.SCGameConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configures a version of Frogger 1997.
 * Created by Kneesnap on 9/7/2023.
 */
@Getter
public class FroggerConfig extends SCGameConfig {
    private final List<Short> islandRemap = new ArrayList<>();
    private int build;
    private int mapBookAddress;
    private int themeBookAddress;
    private int arcadeLevelAddress;
    private int musicAddress;
    @Setter private int demoTableAddress;
    private int pickupDataAddress;
    private int scriptArrayAddress;
    private int skyLandTextureAddress;
    private NameBank formBank;
    private NameBank entityBank;
    private NameBank scriptBank; // Name of scripts.
    private NameBank scriptCallbackBank; // Name of scripts.
    private final Map<String, FroggerMapConfig> mapConfigs = new HashMap<>();
    private final FroggerMapConfig defaultMapConfig = new FroggerMapConfig();

    public FroggerConfig(String internalName) {
        super(internalName);
    }

    @Override
    protected void readConfigData(Config config) {
        this.build = config.getInt("build", -1);
        this.skyLandTextureAddress = config.getInt("txl_sky_land", 0); // Get this by searching for the hex texture ids of sky images as shorts. The textures in the PSX US Demo are textures #98, #97, #96. -> 1723, 1722, 1753 -> "BB 06 BA 06 D9 06".

        super.readConfigData(config);
        loadBanks(config);
        readBasicConfigData(config);
        readMapConfigs(config);
    }

    private void readBasicConfigData(Config config) {
        this.pickupDataAddress = config.getInt("pickupData", 0); // Pointer to Pickup_data[] in ent_gen. If this is not set, bugs will not have textures in the viewer. On PSX, search for 63 63 63 00 then after this entries image pointers, there's Pickup_data.
        this.themeBookAddress = config.getInt("themeBook", -1);
        this.mapBookAddress = config.getInt("mapBook", -1);
        this.demoTableAddress = config.getInt("demoTable", -1);
        // Get this by searching for "07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 1e 00 00 00 03 00 00 00 01 00 00 00 07 00 00 00".
        // Search for the pointer that points to this (Don't forget to include ramOffset)
        this.scriptArrayAddress = config.getInt("scripts", 0);
        // Music is generally always the same data, so you can find it with a search.
        this.musicAddress = config.getInt("musicAddress", -1);
        this.arcadeLevelAddress = config.getInt("arcadeLevelAddress", 0);
    }

    private void readMapConfigs(Config config) {
        this.mapConfigs.clear();
        if (!config.hasChild("MapConfig"))
            return;

        Config defaultMapConfig = config.getChild("MapConfig");
        this.defaultMapConfig.load(defaultMapConfig, this.defaultMapConfig);

        // Read other configs, if there are any.
        for (Config mapConfig : defaultMapConfig.getOrderedChildren()) {
            FroggerMapConfig newMapConfig = new FroggerMapConfig();
            newMapConfig.load(mapConfig, this.defaultMapConfig);
            for (String mapFileName : newMapConfig.getApplicableMaps())
                this.mapConfigs.put(mapFileName, newMapConfig);
        }
    }

    private void loadBanks(Config config) {
        this.formBank = loadBank(config, "formList", "1997-09-12-psx-build57", "forms", "Form", true);
        this.entityBank = loadBank(config, "entityList", "1997-09-12-psx-build57", "entities", "Entity", true);
        this.scriptBank = loadBank(config, "scriptList", "1997-09-18-psx-build63", "scripts", "Script", false);
        this.scriptCallbackBank = this.scriptBank.getChildBank("CallbackNames");
    }

    /**
     * Checks if this build is the PSX alpha build or not.
     */
    public boolean isPSXAlpha() {
        return "psx-1997-06-02-alpha".equalsIgnoreCase(getInternalName());
    }

    /**
     * Checks if this build is the first E3 build or not.
     */
    public boolean isE3Build1() {
        return "psx-1997-06-12-e3".equalsIgnoreCase(getInternalName());
    }

    /**
     * Checks if this build is the second E3 build or not.
     */
    public boolean isE3Build2() {
        return "psx-1997-06-13-e3".equalsIgnoreCase(getInternalName());
    }

    /**
     * Test if this build is the windows alpha build from June 29, 1997.
     */
    public boolean isWindowsAlpha() {
        return "pc-1997-06-29-alpha".equalsIgnoreCase(getInternalName());
    }

    /**
     * Test if this build is the windows beta build from July 21, 1997.
     */
    public boolean isWindowsBeta() {
        return "pc-1997-07-21".equalsIgnoreCase(getInternalName());
    }

    /**
     * Test if this build is the September 3 PC build from Kao.
     */
    public boolean isKaosPrototype() {
        return "pc-1997-09-03".equalsIgnoreCase(getInternalName());
    }

    /**
     * Test if this is the US PSX demo from September 1997.
     */
    public boolean isSeptemberUsDemo() {
        return "psx-demo-ntsc".equalsIgnoreCase(getInternalName());
    }

    /**
     * Test if the build is at/after the retail Windows build.
     */
    public boolean isAtLeastRetailWindows() {
        return "pc-retail-v1.0".equalsIgnoreCase(getInternalName())
                || "pc-retail-v3.0e".equalsIgnoreCase(getInternalName())
                || "pc-retail-v3.0e-vogons".equalsIgnoreCase(getInternalName())
                || "pc-demo".equalsIgnoreCase(getInternalName());
    }

    /**
     * Tests if this is the sony presentation April 28th build.
     */
    public boolean isSonyPresentation() {
        return "psx-1997-04-28-sony".equalsIgnoreCase(getInternalName());
    }

    /**
     * Test if the configuration is for a build before build 01.
     */
    public boolean isBeforeBuild1() {
        return isSonyPresentation() || isPSXAlpha() || isE3Build1() || isE3Build2();
    }

    /**
     * Tests if the build is at/before build 1.
     * @return isBuildAtOrBeforeBuild1
     */
    public boolean isAtOrBeforeBuild1() {
        return this.build == 1 || isBeforeBuild1();
    }

    /**
     * Tests if the build is at/before build 4.
     * @return isBuildAtOrBeforeBuild4
     */
    public boolean isAtOrBeforeBuild4() {
        return (this.build > 0 && this.build <= 4) || isBeforeBuild1() || isWindowsAlpha();
    }

    /**
     * Tests if the build is at/before build 11.
     * @return isBuildAtOrBeforeBuild11
     */
    public boolean isAtOrBeforeBuild11() {
        return (this.build >= 0 && this.build <= 11) || isBeforeBuild1();
    }

    /**
     * Tests if the build is at/before build 20.
     * @return isBuildAtOrBeforeBuild20
     */
    public boolean isAtOrBeforeBuild20() {
        return (this.build >= 0 && this.build <= 20) || isBeforeBuild1() || isWindowsAlpha() || isWindowsBeta();
    }

    /**
     * Tests if the build is at/before build 21.
     * @return isBuildAtOrBeforeBuild21
     */
    public boolean isAtOrBeforeBuild21() {
        return (this.build >= 0 && this.build <= 21) || isBeforeBuild1() || isWindowsAlpha() || isWindowsBeta();
    }

    /**
     * Tests if the build is at/before build 23.
     * @return isBuildAtOrBeforeBuild23
     */
    public boolean isAtOrBeforeBuild23() {
        return (this.build >= 0 && this.build <= 23) || isBeforeBuild1() || isWindowsAlpha() || isWindowsBeta();
    }

    /**
     * Tests if the build is at/before build 24.
     * @return isBuildAtOrBeforeBuild24
     */
    public boolean isAtOrBeforeBuild24() {
        return (this.build >= 0 && this.build <= 24) || isBeforeBuild1() || isWindowsAlpha() || isWindowsBeta();
    }

    /**
     * Tests if the build is at/before build 28.
     * @return isBuildAtOrBeforeBuild28
     */
    public boolean isAtOrBeforeBuild28() {
        return (this.build >= 0 && this.build <= 28) || isBeforeBuild1() || isWindowsAlpha() || isWindowsBeta();
    }

    /**
     * Tests if the build is at/before build 29.
     * @return isBuildAtOrBeforeBuild29
     */
    public boolean isAtOrBeforeBuild29() {
        return (this.build >= 0 && this.build <= 29) || isBeforeBuild1() || isWindowsAlpha() || isWindowsBeta();
    }

    /**
     * Tests if the build is at/before build 33
     * @return isBuildAtOrBeforeBuild33
     */
    public boolean isAtOrBeforeBuild33() {
        return (this.build >= 0 && this.build <= 38) || isBeforeBuild1() || isWindowsAlpha() || isWindowsBeta();
    }

    /**
     * Tests if the build is at/before build 38
     * @return isBuildAtOrBeforeBuild38
     */
    public boolean isAtOrBeforeBuild38() {
        return (this.build >= 0 && this.build <= 38) || isBeforeBuild1() || isWindowsAlpha() || isWindowsBeta();
    }

    /**
     * Tests if the build is at/before build 51
     * @return isBuildAtOrBeforeBuild51
     */
    public boolean isAtOrBeforeBuild51() {
        return (this.build >= 0 && this.build <= 51) || isBeforeBuild1() || isWindowsAlpha() || isWindowsBeta()
                || isKaosPrototype() || isSeptemberUsDemo();
    }
}