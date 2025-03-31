package net.highwayfrogs.editor.games.sony.medievil;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.io.File;
import java.util.*;
import java.util.function.Supplier;

/**
 * Parses MediEvil per-level scripts into a more readable/usable form.
 * TODO: Generalize to potentially other kinds of MediEvil scripts.
 * TODO: Make scripts accessible within FrogLord UI (maybe, depends on if there's a pointer to them easily accessible)
 * Created in 2022 by Kneesnap.
 */
@SuppressWarnings("SpellCheckingInspection")
public class MediEvilScripter {
    private static final int MM_COLP_TYPE_CAMERA = 32768;
    private static final int MM_COLP_TYPE_WARP = 16384;
    private static final int MM_COLP_TYPE_COLANDEVENT = 49152;

    public static void main(String[] args) throws Throwable {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter the path to the overlay .bin file: ");
        File file = new File(scanner.nextLine());
        String overlayCode = FileUtils.stripExtension(file.getName()).toUpperCase();
        if (!file.isFile() || !file.exists()) {
            System.out.println("Overlay file not found.");
            return;
        }

        System.out.print("What is the address of the script (In the file): ");
        String addressStr = scanner.nextLine();
        if (!NumberUtils.isHexInteger(addressStr)) {
            System.out.print("Invalid hex integer '" + addressStr + "'.");
            return;
        }

        int address = Integer.parseInt(addressStr.substring(2), 16);
        DataReader reader = new DataReader(new FileSource(file));
        reader.setIndex(address);
        System.out.println();
        System.out.println("MR_LONG gal" + overlayCode + "Events[] =");
        System.out.println("{");

        for (EventBlock block : readScript(reader))
            System.out.println("\t" + block + ",");

        System.out.println("}");
        System.out.println();
    }

    public static List<EventBlock> readScript(DataReader reader) {
        List<EventBlock> blocks = new ArrayList<>();

        EventBlock lastBlock;
        do {
            lastBlock = readEventBlock(reader);
            blocks.add(lastBlock);
        } while (lastBlock.getType() != EventType.ECON_END_CONDITION_LIST);

        return blocks;
    }

    private static EventBlock readEventBlock(DataReader reader) {
        int startIndex = reader.getIndex();
        short commandId = reader.readShort();
        EventType eventType = EventType.getEventTypeById(commandId);
        if (eventType == null) {
            throw new RuntimeException("Unknown Command ID: '" + commandId + "'.");
        } else {
            reader.jumpTemp(reader.getIndex());
            reader.skipByte();
            int expectedSize = reader.readUnsignedByte();
            reader.jumpReturn();
            EventBlock newBlock = eventType.getBlockCreator() != null ? eventType.getBlockCreator().get() : new DummyBlock(eventType);
            if (newBlock.getType() != eventType) {
                throw new RuntimeException("Block " + newBlock.getClass().getSimpleName() + "'s type is " + newBlock.getType() + ", when it should be " + eventType + ".");
            } else {
                newBlock.load(reader);
                int bytesRead = reader.getIndex() - startIndex;
                if (bytesRead != expectedSize) {
                    throw new RuntimeException("The number of bytes read for the " + newBlock.getClass().getSimpleName() + "/" + eventType + " was " + bytesRead + ", when it was expected to be " + expectedSize + ".");
                } else {
                    return newBlock;
                }
            }
        }
    }

    private static void writeFixedDecimal(StringBuilder sb, short value) {
        sb.append(value).append(" ").append("/* ").append(DataUtils.fixedPointShortToFloat4Bit(value)).append(" */");
    }

    private static void writeFixedVector(StringBuilder builder, short x, short y, short z) {
        writeFixedDecimal(builder, x);
        builder.append(", ");
        writeFixedDecimal(builder, y);
        builder.append(", ");
        writeFixedDecimal(builder, z);
    }

    private static void writeFixedVector(StringBuilder builder, short x, short y, short z, short pad) {
        writeFixedVector(builder, x, y, z);
        builder.append(", ");
        writeFixedDecimal(builder, pad);
    }

    public static <E extends Enum<E>> String getEnumName(E[] enumValues, int ordinal) {
        return ordinal >= 0 && ordinal < enumValues.length ? enumValues[ordinal].name() : "/*[BAD ENUM]*/ " + NumberUtils.toHexString(ordinal);
    }

    public static String getBitFlagsString(int bitValue, BitFlagEnum[] values) {
        StringBuilder builder = new StringBuilder();
        appendBitFlagsString(builder, bitValue, values);
        return builder.toString();
    }

    public static void appendBitFlagsString(StringBuilder builder, int bitValue, BitFlagEnum[] values) {
        int numberAdded = 0;

        for (int i = values.length - 1; i >= 0; i--) {
            BitFlagEnum enumValue = values[i];
            if (((long)bitValue & enumValue.getBitMask()) == enumValue.getBitMask()) {
                bitValue = (int)((long)bitValue ^ enumValue.getBitMask());
                if (numberAdded++ > 0)
                    builder.append(" | ");

                builder.append(enumValue.name());
            }
        }

        if (bitValue != 0) {
            if (numberAdded++ > 0)
                builder.append(" | ");

            builder.append("0x").append(Integer.toHexString(bitValue).toUpperCase());
        }

        if (numberAdded == 0)
            builder.append("NULL");
    }

    public enum VelocityMode {
        ACC,
        DEC,
        ACCDEC,
        CONST
    }

    public enum InventoryItemType {
        INV_ITEM_SMALL_SWORD,
        INV_ITEM_BIG_SWORD,
        INV_ITEM_MAGIC_SWORD,
        INV_ITEM_CLUB,
        INV_ITEM_ARM,
        INV_ITEM_DAGGER,
        INV_ITEM_AXE,
        INV_ITEM_CROSSBOW,
        INV_ITEM_CHICKEN_DRUMSTICK,
        INV_ITEM_LONGBOW,
        INV_ITEM_SPEAR,
        INV_ITEM_LIGHTNING,
        INV_ITEM_SHIELD_COPPER,
        INV_ITEM_SHIELD_SILVER,
        INV_ITEM_SHIELD_GOLD,
        INV_ITEM_CHAOS_RUNE,
        INV_ITEM_EARTH_RUNE,
        INV_ITEM_MOON_RUNE,
        INV_ITEM_STAR_RUNE,
        INV_ITEM_TIME_RUNE,
        INV_ITEM_DC_SHEET_MUSIC,
        INV_ITEM_SKULL_KEY,
        INV_ITEM_LAKE_KEY,
        INV_ITEM_SF_HARVESTER_PART,
        INV_ITEM_AC_AMBER,
        INV_ITEM_PD_HELMET,
        INV_ITEM_MULTI_SHADOW_ARTIFACT,
        INV_ITEM_EE_SHADOW_TALISMAN,
        INV_ITEM_MULTI_WITCH_TALISMAN,
        INV_ITEM_SV_CRUCIFIX,
        INV_ITEM_SV_CRUCIFIX_CAST,
        INV_ITEM_SV_SAFE_KEY,
        INV_ITEM_SV_METAL_BUST,
        INV_ITEM_HAMMER,
        INV_ITEM_FLAMINGBOW,
        INV_ITEM_MAGICBOW,
        INV_ITEM_HR_CROWN,
        INV_ITEM_CHALICE_OF_SOULS,
        INV_ITEM_GOLD_PIECES,
        INV_ITEM_DRAGON_GEM,
        INV_ITEM_GOOD_LIGHTNING,
        INV_ITEM_DRAGON_ARMOUR,
        INV_ITEM_LIFE_BOTTLE,
        INV_ITEM_MAX
    }

    public enum EventSetPosFlag implements BitFlagEnum {
        SETP_ENABLE_SKY_B,
        SETP_DISABLE_SKY_B,
        SETP_MAX
    }

    public enum EntityState2Flags implements BitFlagEnum {
        ENT_STATE2_NO_DISPLAY_B,
        ENT_STATE2_HIT_FLASH_OVERRIDE_B,
        ENT_STATE2_INVULNERABLE_FLASH_B,
        ENT_STATE2_NO_GROUND_LIGHTING_B,
        ENT_STATE2_SHADOW_DISC_B,
        ENT_STATE2_SHADOW_SQUARE_B,
        ENT_STATE2_SUBSPECIES_ANIM_RES_B,
        ENT_STATE2_NO_STICK_IN_B,
        ENT_STATE2_FOOTFALL_SOUND_B,
        ENT_STATE2_CAN_FLOAT_B,
        ENT_STATE2_AMPHIBIOUS_B,
        ENT_STATE2_NO_SINKING_B,
        ENT_STATE2_NO_FALLING_DEATH_B,
        ENT_STATE2_SUBSPECIES_BFLAGS_B,
        ENT_STATE2_VINE_BASED_B,
        ENT_STATE2_NO_INVOLUNTARY_MOVE_B,
        ENT_STATE2_NO_FOOTCOL_ALIGN_B,
        ENT_STATE2_ANIM_USER0_B,
        ENT_STATE2_ANIM_USER1_B,
        ENT_STATE2_ANIM_USER2_B,
        ENT_STATE2_ANIM_USER3_B,
        ENT_STATE2_IN_EVENT_ZONE_B,
        ENT_STATE2_IN_WARP_ZONE_B,
        ENT_STATE2_IN_WATER_B,
        ENT_STATE2_UNDER_WATER_B,
        ENT_STATE2_IN_MUD_B,
        ENT_STATE2_CREATE_TRANSLUCENT_B,
        ENT_STATE2_NO_BURN_B,
        ENT_STATE2_LIE_DOWN_WHEN_DEAD_B,
        ENT_STATE2_SHADOW_PARTS_B,
        ENT_STATE2_USE_BG_SOUND_B,
        ENT_STATE2_LIMIT_B
    }

    public enum EntityState1Flags implements BitFlagEnum {
        ENT_STATE1_NOT_INITED_B,
        ENT_STATE1_IMMORTAL_B,
        ENT_STATE1_NO_DEBUG_PLACE_B,
        ENT_STATE1_FORCE_DEBUG_PLACE_B,
        ENT_STATE1_NO_DETECT_B,
        ENT_STATE1_NO_GROUND_DETECT_B,
        ENT_STATE1_NO_GROUND_ALIGN_B,
        ENT_STATE1_NO_GRAVITY_B,
        ENT_STATE1_NO_AVOID_SOLID_B,
        ENT_STATE1_NO_COLLIDE_EJECT_B,
        ENT_STATE1_WARP_DETECT_B,
        ENT_STATE1_NO_BG_COLLIDE_B,
        ENT_STATE1_NO_RANGE_DEACTIVATE_B,
        ENT_STATE1_PREVENT_FALL_B,
        ENT_STATE1_PHYSICS_OVERRIDE_B,
        ENT_STATE1_LANDING_BOUNCE_B,
        ENT_STATE1_STATIC_B,
        ENT_STATE1_OFFSCREEN_FREEZE_B,
        ENT_STATE1_CHILD_B,
        ENT_STATE1_CHILD_INACTIVE_B,
        ENT_STATE1_IN_AIR_B,
        ENT_STATE1_DUCK_B,
        ENT_STATE1_SKIDDING_B,
        ENT_STATE1_RUNNING_B,
        ENT_STATE1_TIPTOE_B,
        ENT_STATE1_RECOILING_B,
        ENT_STATE1_SHRUNK_B,
        ENT_STATE1_HEAVY_LANDING_B,
        ENT_STATE1_PROPELLANT_MOVE_OVERRIDE_B,
        ENT_STATE1_ON_PLATFORM_B,
        ENT_STATE1_OVER_ACTIVE_RANGE_B,
        ENT_STATE1_OVER_KILL_RANGE_B,
        ENT_STATE1_LIMIT_B
    }

    public enum EntityDestroyInfoFlags implements BitFlagEnum {
        ENT_DEST_CHAOS_RUNE_B,
        ENT_DEST_EARTH_RUNE_B,
        ENT_DEST_MOON_RUNE_B,
        ENT_DEST_STAR_RUNE_B,
        ENT_DEST_TIME_RUNE_B,
        ENT_DEST_PLAYER_KILLED_B,
        ENT_DEST_NO_RANGE_KILL_B,
        ENT_DEST_RANGE_REMAP_TO_SOURCE_B,
        ENT_DEST_REMAP_ON_KILL_B,
        ENT_DEST_REMAP_ON_RESTART_B,
        ENT_DEST_USER_1_B,
        ENT_DEST_USER_2_B,
        ENT_DEST_USER_3_B,
        ENT_DEST_USER_4_B,
        ENT_DEST_CREATE_BONUS_B,
        ENT_DEST_STAY_DEAD_B,
        ENT_DEST_ROT_SAVED_B,
        ENT_DEST_STAY_ALIVE_B,
        ENT_DEST_INFO_LIMIT_B
    }

    public enum EntityInitInfoFlags implements BitFlagEnum {
        ENT_INIT_PREVENT_CREATE_B,
        ENT_INIT_CREATE_ON_PREPROCESS_B,
        ENT_INIT_START_ACTIVE_B,
        ENT_INIT_REMAIN_INACTIVE_B,
        ENT_INIT_START_AS_HERO_B,
        ENT_INIT_NO_STARTUP_ALIGN_B,
        ENT_INIT_FDIR_HERO_B,
        ENT_INIT_FDIR_RANDOM_B,
        ENT_INIT_FDIR_MAPPED_B,
        ENT_INIT_USER_1_B,
        ENT_INIT_USER_2_B,
        ENT_INIT_USER_3_B,
        ENT_INIT_USER_4_B,
        ENT_INIT_AWAIT_EVENT_TRIGGER_B,
        ENT_INIT_AWAIT_PLAYER_SEEN_B,
        ENT_INIT_GENERATED_B,
        ENT_INIT_ODD_B,
        ENT_INIT_FLAGS_RESOLVED_B,
        ENT_INIT_INFO_LIMIT_B
    }

    public enum EntitySpecies {
        ENT_SPECIES_DAN,
        ENT_SPECIES_GY1ZOMBIE1,
        ENT_SPECIES_GY1ZOMBIE2,
        ENT_SPECIES_ZLSKELETON,
        ENT_SPECIES_CHEST,
        ENT_SPECIES_WEAPON,
        ENT_SPECIES_SHIELD,
        ENT_SPECIES_POTION,
        ENT_SPECIES_GENERATOR,
        ENT_SPECIES_DOOR1,
        ENT_SPECIES_SWORD,
        ENT_SPECIES_THUMPER,
        ENT_SPECIES_LIGHTNING,
        ENT_SPECIES_PROJECTILE,
        ENT_SPECIES_PSPUMPKIN,
        ENT_SPECIES_SMARTBOMB,
        ENT_SPECIES_DRAGONDAN,
        ENT_SPECIES_RUNEKEY,
        ENT_SPECIES_GY2WOLF1,
        ENT_SPECIES_GY2HEADLESS1,
        ENT_SPECIES_TUMBLING1,
        ENT_SPECIES_SEVHAND1,
        ENT_SPECIES_SFCROW,
        ENT_SPECIES_ZAROKTRAIL,
        ENT_SPECIES_SFSCREW,
        ENT_SPECIES_PARTICLEENT,
        ENT_SPECIES_STATICENT,
        ENT_SPECIES_SFWICKERMANHEAD,
        ENT_SPECIES_SFDISCOFDEATH,
        ENT_SPECIES_SFWINDMILL,
        ENT_SPECIES_FIREBALL,
        ENT_SPECIES_SFINOUTCTRL,
        ENT_SPECIES_SFINOUTPOLE,
        ENT_SPECIES_SFSPINNER,
        ENT_SPECIES_SFSPINNYBOWL,
        ENT_SPECIES_SFTOPGRINDER,
        ENT_SPECIES_AGCHESSPIECE,
        ENT_SPECIES_DCIMP1,
        ENT_SPECIES_DC_COFFINLID,
        ENT_SPECIES_TORCH,
        ENT_SPECIES_AGHEAD,
        ENT_SPECIES_GY1COFFIN1,
        ENT_SPECIES_PDCHARIOT,
        ENT_SPECIES_MADMONK,
        ENT_SPECIES_IAPATIENT,
        ENT_SPECIES_STAINEDGLASSDEMON,
        ENT_SPECIES_MUDKNIGHT,
        ENT_SPECIES_NMEPROJ,
        ENT_SPECIES_FATKNIGHT,
        ENT_SPECIES_PICKUP,
        ENT_SPECIES_MRORGAN,
        ENT_SPECIES_GHOUL,
        ENT_SPECIES_SVBELLOWS,
        ENT_SPECIES_VINE,
        ENT_SPECIES_MRSMAD,
        ENT_SPECIES_PGMAGGOT,
        ENT_SPECIES_PGPPLANT,
        ENT_SPECIES_PGPUMPKIN,
        ENT_SPECIES_MRMAD,
        ENT_SPECIES_HRCRAWLDEMON1,
        ENT_SPECIES_NELLIEMAD,
        ENT_SPECIES_ANT,
        ENT_SPECIES_SVSTOREPAD,
        ENT_SPECIES_QUEENANT,
        ENT_SPECIES_HRCYCLOPS,
        ENT_SPECIES_HRCANNON,
        ENT_SPECIES_CHICKEN,
        ENT_SPECIES_ANTLARVAE,
        ENT_SPECIES_HRCORNPILE,
        ENT_SPECIES_SVCRUCIFIXHOLDER,
        ENT_SPECIES_PGMONSTER,
        ENT_SPECIES_PGPODBALL,
        ENT_SPECIES_HRCHAIN,
        ENT_SPECIES_EETOMBLID,
        ENT_SPECIES_HRCATAPULT,
        ENT_SPECIES_HRBOULDER,
        ENT_SPECIES_HRBLOCK,
        ENT_SPECIES_SVFOUNTAIN,
        ENT_SPECIES_RHINOTAUR,
        ENT_SPECIES_SERPENT,
        ENT_SPECIES_PGBARNSCREW,
        ENT_SPECIES_SVDUCKY,
        ENT_SPECIES_SVDUCKSHOOT,
        ENT_SPECIES_IAHEADLESS1,
        ENT_SPECIES_CCLARGELIFT,
        ENT_SPECIES_EHIMP1,
        ENT_SPECIES_CCCRYSTAL,
        ENT_SPECIES_ACFIREFLY,
        ENT_SPECIES_GSPIRATECAPTAIN,
        ENT_SPECIES_GSTHINPIRATE,
        ENT_SPECIES_SFSCARECROW,
        ENT_SPECIES_GSBARREL,
        ENT_SPECIES_GSSPINDLE,
        ENT_SPECIES_GSCANNON,
        ENT_SPECIES_CCSCOUTDEMON1,
        ENT_SPECIES_GSCANNONDOORCONT,
        ENT_SPECIES_GSNETDAEMON,
        ENT_SPECIES_PGMUSHYTOP,
        ENT_SPECIES_SFCORNKILLER,
        ENT_SPECIES_GSCAPSTAN,
        ENT_SPECIES_GSCRANE,
        ENT_SPECIES_GSLIFT,
        ENT_SPECIES_CCDRAGON,
        ENT_SPECIES_SFFARMER,
        ENT_SPECIES_TDBOILERGUARD,
        ENT_SPECIES_GSPIRATEFRED,
        ENT_SPECIES_ZARHEAD,
        ENT_SPECIES_SFCROWTREE,
        ENT_SPECIES_GSCAMDAEMON,
        ENT_SPECIES_SKEWEREDMAN,
        ENT_SPECIES_ACBOULDER,
        ENT_SPECIES_TDCOG,
        ENT_SPECIES_TDFINGER,
        ENT_SPECIES_TDEYE,
        ENT_SPECIES_TDGATE,
        ENT_SPECIES_TDEGGTIMER,
        ENT_SPECIES_KEYHOLE,
        ENT_SPECIES_SVWOLF1,
        ENT_SPECIES_MARKER,
        ENT_SPECIES_SFBAT1,
        ENT_SPECIES_HRLAVATAP,
        ENT_SPECIES_SFSAWBLADE,
        ENT_SPECIES_TDTURNTABLE,
        ENT_SPECIES_TDCIRCUIT,
        ENT_SPECIES_SFCHAFFBLOWER,
        ENT_SPECIES_TDGLOBE,
        ENT_SPECIES_DCDEMONHEART,
        ENT_SPECIES_CCBAT1,
        ENT_SPECIES_HRMACEKNIGHT1,
        ENT_SPECIES_TDCRYSTAL,
        ENT_SPECIES_PSFLAPPYFISH,
        ENT_SPECIES_LATOADDEMON,
        ENT_SPECIES_LAELEDRAGON,
        ENT_SPECIES_PGTUNNELWALL,
        ENT_SPECIES_TDSWITCH1,
        ENT_SPECIES_TDSIMPLELIFT,
        ENT_SPECIES_SWJABBER,
        ENT_SPECIES_SWSPIDER,
        ENT_SPECIES_BIGPLANET,
        ENT_SPECIES_HANGEDMAN,
        ENT_SPECIES_LAFISH,
        ENT_SPECIES_LABARNACLE,
        ENT_SPECIES_LACRYSTAL,
        ENT_SPECIES_LAJUNCTION,
        ENT_SPECIES_EECABLECAR,
        ENT_SPECIES_IAZOMBIE3,
        ENT_SPECIES_GGFALLINGSLAB,
        ENT_SPECIES_CCDEATHRAY,
        ENT_SPECIES_CCBLOCK,
        ENT_SPECIES_CCIMP1,
        ENT_SPECIES_PGSPIRALHILLGEN,
        ENT_SPECIES_LASCOUTDEMON1,
        ENT_SPECIES_GMTRAIN,
        ENT_SPECIES_DCSPIRITS,
        ENT_SPECIES_GMSWITCH1,
        ENT_SPECIES_EEGOBBA,
        ENT_SPECIES_EEPODSPITTA,
        ENT_SPECIES_GMCONDUIT,
        ENT_SPECIES_GMLIFT,
        ENT_SPECIES_EEPODBOMB,
        ENT_SPECIES_GMHUBCUPBOARD,
        ENT_SPECIES_CHBOULDER,
        ENT_SPECIES_GMFLAMER,
        ENT_SPECIES_GMSTEAMER,
        ENT_SPECIES_GMEYE1,
        ENT_SPECIES_TDEYE1,
        ENT_SPECIES_TDWATCH,
        ENT_SPECIES_TDCLOCK,
        ENT_SPECIES_GMCOG,
        ENT_SPECIES_GMHAMMER,
        ENT_SPECIES_GMHANDWHEEL,
        ENT_SPECIES_GMHANDSTER,
        ENT_SPECIES_EECRAWLDEMON1,
        ENT_SPECIES_EESCOUTDEMON1,
        ENT_SPECIES_EEGODDESSES,
        ENT_SPECIES_TDCLOCKSWITCH,
        ENT_SPECIES_ZLKNIGHT,
        ENT_SPECIES_TDELECTRICGLOBE,
        ENT_SPECIES_EEPLATFORM,
        ENT_SPECIES_TDCLOCKTELLER,
        ENT_SPECIES_EEBADSPIDER,
        ENT_SPECIES_EEROCKBLOCK,
        ENT_SPECIES_EEBIRDLEGS,
        ENT_SPECIES_IABOMB,
        ENT_SPECIES_TDKEYGLASS,
        ENT_SPECIES_EEEGG,
        ENT_SPECIES_IACANNON,
        ENT_SPECIES_TDNETDAEMON,
        ENT_SPECIES_TDTRAIN,
        ENT_SPECIES_FREESLOT,
        ENT_SPECIES_FREESLOT2,
        ENT_SPECIES_IABAT1,
        ENT_SPECIES_GMBOILER,
        ENT_SPECIES_GMCHIMNEY1,
        ENT_SPECIES_GMCHIMNEY2,
        ENT_SPECIES_CHHEADLESS1,
        ENT_SPECIES_GY1ANGEL1,
        ENT_SPECIES_GY2SHIFTYBLOCK,
        ENT_SPECIES_CHZOMBIE1,
        ENT_SPECIES_GY2ZOMBIE2,
        ENT_SPECIES_CHGARGOYLE,
        ENT_SPECIES_GY2STONEWOLF,
        ENT_SPECIES_SMASHROCK,
        ENT_SPECIES_GY2COFFIN1,
        ENT_SPECIES_PGBOULDER,
        ENT_SPECIES_DCSMASHGLASS,
        ENT_SPECIES_DCFIREBLOCK,
        ENT_SPECIES_DCSPIRITHEART,
        ENT_SPECIES_HELP_GARGOYLE,
        ENT_SPECIES_GY2ANGEL1,
        ENT_SPECIES_CHZAROK,
        ENT_SPECIES_GY2WARPTWINKLE,
        ENT_SPECIES_SFWICKERMAN,
        ENT_SPECIES_SFHAYCART,
        ENT_SPECIES_LAEYE,
        ENT_SPECIES_LASEAWEED,
        ENT_SPECIES_LALEVER,
        ENT_SPECIES_SFPUSHCRATE,
        ENT_SPECIES_SFHAYSTACK,
        ENT_SPECIES_SFLEVER,
        ENT_SPECIES_SFHARVESTER,
        ENT_SPECIES_SFBARNWALL,
        ENT_SPECIES_ACROCKNROLL,
        ENT_SPECIES_AGSTAR,
        ENT_SPECIES_AGBELL,
        ENT_SPECIES_AGFACE,
        ENT_SPECIES_AGGRILL,
        ENT_SPECIES_ACSHATTERWALL,
        ENT_SPECIES_AGCLOWN,
        ENT_SPECIES_AGFIREBLOCK,
        ENT_SPECIES_ACFAIRY,
        ENT_SPECIES_AGHEDGE,
        ENT_SPECIES_ACFORCEFIELD,
        ENT_SPECIES_AGRODENT,
        ENT_SPECIES_AGHEDGECAT,
        ENT_SPECIES_AGELEPHANTDAEMON,
        ENT_SPECIES_ACWITCH,
        ENT_SPECIES_AGELEPHANTWALL,
        ENT_SPECIES_PDBOAT,
        ENT_SPECIES_EEDRAGONTOAD,
        ENT_SPECIES_PDBOATMAN,
        ENT_SPECIES_EEWINGEDDEMON,
        ENT_SPECIES_EEFLAME,
        ENT_SPECIES_EEMOLTENROCK,
        ENT_SPECIES_HHSTATUE,
        ENT_SPECIES_EEWITCH,
        ENT_SPECIES_HRGOLEM,
        ENT_SPECIES_HRFLYDEMON,
        ENT_SPECIES_HRFARMER,
        ENT_SPECIES_HRFLAME,
        ENT_SPECIES_SVLEVER,
        ENT_SPECIES_HRLEVER,
        ENT_SPECIES_SVPUSHABLE,
        ENT_SPECIES_SVBOILERGUARD,
        ENT_SPECIES_HRTHEKING,
        ENT_SPECIES_PSPPLANT,
        ENT_SPECIES_PSWITCH,
        ENT_SPECIES_SVSAFE,
        ENT_SPECIES_CCROCK,
        ENT_SPECIES_GSLAMPWRAPPER,
        ENT_SPECIES_TDTRAINPLATFORM,
        ENT_SPECIES_GSEXPLODEBARREL,
        ENT_SPECIES_GSCANNONBALL,
        ENT_SPECIES_TDFLAME,
        ENT_SPECIES_TDSTEAM,
        ENT_SPECIES_TDLEVER,
        ENT_SPECIES_GSCLOUDDAEMON,
        ENT_SPECIES_TDROLLCLOCK,
        ENT_SPECIES_TDGLOBECONDUIT,
        ENT_SPECIES_IAMAYOR,
        ENT_SPECIES_IAOBJECTDROPPER,
        ENT_SPECIES_ZLZAROK,
        ENT_SPECIES_ZLCHALICE,
        ENT_SPECIES_EHSPHERE,
        ENT_SPECIES_CHBOOKCASE,
        ENT_SPECIES_CHMAGICFIRE,
        ENT_SPECIES_PSRAT1,
        ENT_SPECIES_SVRAT1,
        ENT_SPECIES_GY1RAT1,
        ENT_SPECIES_GY2RAT1,
        ENT_SPECIES_GGZOMBIE3,
        ENT_SPECIES_CRSMASHWALL,
        ENT_SPECIES_ZLZARHEAD,
        ENT_SPECIES_LABOAT,
        ENT_SPECIES_LABOATMAN,
        ENT_SPECIES_ZLTRAIN,
        ENT_SPECIES_GY2FLAME,
        ENT_SPECIES_ZLOBJECTDROPPER,
        ENT_SPECIES_ZLDROPBLOCK,
        ENT_SPECIES_EEFALLAWAY,
        ENT_SPECIES_DCSMASHSPIKES,
        ENT_SPECIES_MAX
    }

    public enum EventConditionFlag implements BitFlagEnum {
        EF_IS_COMMAND_B,
        EF_SIMPLE_CONDITIONAL_B,
        EF_SWITCH_B,
        EF_ALLOC_EVENT_B,
        ECONF_PROC_CMD_WHEN_TRUE_B,
        ECONF_PROC_CMD_WHEN_FALSE_B,
        ECONF_PROC_CMD_ALWAYS_B,
        ECONF_PROC_CMD_WHILE_B,
        ECONF_ADVANCE_B,
        ECONF_RETREAT_B,
        ECONF_SKIP1_B,
        ECONF_SKIP2_B,
        ECONF_RESET_B,
        ECONF_KILL_BRANCH_WHEN_MET_B,
        ECONF_MUST_BE_MET_B,
        ECONF_MET_B,
        ECMDF_REPEATING_B,
        ECMDF_AWAIT_COMPLETE_B,
        ECMDF_COMPLETED_B
    }

    public interface BitFlagEnum {
        int ordinal();
        String name();

        /**
         * Gets the bit mask of the enum value.
         */
        default int getBitMask() {
            return 1 << ordinal();
        }
    }

    @Getter
    public static class EventCommandGidSetInitUser1 extends CommandStandard {
        private long gid;

        public EventCommandGidSetInitUser1() {
            super(EventType.ECMD_GID_SET_INIT_USER_1);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_GID_SET_INIT_USER_1(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
        }
    }

    @Getter
    public static class EventCommandWakeUpInactiveEntity extends CommandStandard {
        private long gid;

        public EventCommandWakeUpInactiveEntity() {
            super(EventType.ECMD_WAKE_UP_INACTIVE_ENT);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_WAKE_UP_INACTIVE_ENT(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
        }
    }

    @Getter
    public static class EventCommandSetBranchTimer extends CommandStandard {
        private long value;

        public EventCommandSetBranchTimer() {
            super(EventType.ECMD_SET_BRANCH_TIMER);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_SET_BRANCH_TIMER(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.value).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.value = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.value);
        }
    }

    @Getter
    public static class EventCommandSetEventValue extends CommandStandard {
        private long event;
        private long value;

        public EventCommandSetEventValue() {
            super(EventType.ECMD_EVENT_SET_VALUE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_EVENT_SET_VALUE(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.event).append(", ")
                    .append(this.value).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.event = reader.readUnsignedIntAsLong();
            this.value = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.event);
            writer.writeUnsignedInt(this.value);
        }
    }

    @Getter
    public static class EventCommandChangePolyHeight extends CommandStandard {
        private short x;
        private short y;
        private short z;
        private short distance;
        private short updateCount;
        private short velMode;

        public EventCommandChangePolyHeight() {
            super(EventType.ECMD_CHANGE_POLY_HEIGHT);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_CHANGE_POLY_HEIGHT(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ");

            writeFixedVector(builder, this.x, this.y, this.z, this.distance);
            builder.append(", ").append(this.updateCount)
                    .append(", ").append(getEnumName(VelocityMode.values(), this.velMode))
                    .append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.x = reader.readShort();
            this.y = reader.readShort();
            this.z = reader.readShort();
            this.distance = reader.readShort();
            this.updateCount = reader.readShort();
            this.velMode = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeShort(this.x);
            writer.writeShort(this.y);
            writer.writeShort(this.z);
            writer.writeShort(this.distance);
            writer.writeShort(this.updateCount);
            writer.writeShort(this.velMode);
        }
    }

    @Getter
    public static class EventCommandDisableBoth extends CommandStandard {
        private long entGid;
        private long colpGid;

        public EventCommandDisableBoth() {
            super(EventType.ECMD_GID_AND_COLP_DISABLE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_GID_AND_COLP_DISABLE(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.entGid).append(", ")
                    .append(this.colpGid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.entGid = reader.readUnsignedIntAsLong();
            this.colpGid = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.entGid);
            writer.writeUnsignedInt(this.colpGid);
        }
    }

    @Getter
    public static class EventCommandEnableBoth extends CommandStandard {
        private long entGid;
        private long colpGid;

        public EventCommandEnableBoth() {
            super(EventType.ECMD_GID_AND_COLP_ENABLE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_GID_AND_COLP_ENABLE(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.entGid).append(", ")
                    .append(this.colpGid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.entGid = reader.readUnsignedIntAsLong();
            this.colpGid = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.entGid);
            writer.writeUnsignedInt(this.colpGid);
        }
    }

    @Getter
    public static class EventCommandColpDisable extends CommandStandard {
        private final List<Long> gids = new ArrayList<>();
        private int colprimType;

        public EventCommandColpDisable() {
            super(EventType.ECMD_COL_PRIM_DISABLE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            if (this.colprimType == MM_COLP_TYPE_CAMERA) {
                builder.append("EVCMD_CAMERA_DISABLE(");
            } else if (this.colprimType == MM_COLP_TYPE_WARP) {
                builder.append("EVCMD_WARP_DISABLE(");
            } else {
                if (this.colprimType != MM_COLP_TYPE_COLANDEVENT)
                    throw new RuntimeException("Invalid colprim type '" + this.colprimType + "'.");

                builder.append("EVCMD_COL_DISABLE(");
            }

            builder.append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gids.size()).append(")");

            if (this.gids.size() > 0) {
                builder.append(", /* Gids: */ ");

                for (int i = 0; i < this.gids.size(); i++) {
                    if (i > 0)
                        builder.append(", ");

                    builder.append(this.gids.get(i));
                }
            }
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            long numGids = reader.readUnsignedIntAsLong();
            this.colprimType = reader.readInt();
            if (this.colprimType != MM_COLP_TYPE_CAMERA && this.colprimType != MM_COLP_TYPE_COLANDEVENT && this.colprimType != MM_COLP_TYPE_WARP)
                throw new RuntimeException("Invalid colprim type '" + this.colprimType + "'.");

            this.gids.clear();
            for (int i = 0; i < numGids; i++)
                this.gids.add(reader.readUnsignedIntAsLong());
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeInt(this.gids.size());
            writer.writeInt(this.colprimType);

            for (int i = 0; i < this.gids.size(); i++)
                writer.writeUnsignedInt(this.gids.get(i));
        }
    }

    @Getter
    public static class EventCommandColpEnable extends CommandStandard {
        private final List<Long> gids = new ArrayList<>();
        private int colprimType;

        public EventCommandColpEnable() {
            super(EventType.ECMD_COL_PRIM_ENABLE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            if (this.colprimType == MM_COLP_TYPE_CAMERA) {
                builder.append("EVCMD_CAMERA_ENABLE(");
            } else if (this.colprimType == MM_COLP_TYPE_WARP) {
                builder.append("EVCMD_WARP_ENABLE(");
            } else {
                if (this.colprimType != MM_COLP_TYPE_COLANDEVENT)
                    throw new RuntimeException("Invalid colprim type '" + this.colprimType + "'.");

                builder.append("EVCMD_COL_ENABLE(");
            }

            builder.append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gids.size()).append(")");

            if (this.gids.size() > 0) {
                builder.append(", /* Gids: */ ");

                for (int i = 0; i < this.gids.size(); i++) {
                    if (i > 0)
                        builder.append(", ");

                    builder.append(this.gids.get(i));
                }
            }
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            long numGids = reader.readUnsignedIntAsLong();
            this.colprimType = reader.readInt();
            if (this.colprimType != MM_COLP_TYPE_CAMERA && this.colprimType != MM_COLP_TYPE_COLANDEVENT && this.colprimType != MM_COLP_TYPE_WARP) {
                throw new RuntimeException("Invalid colprim type '" + this.colprimType + "'.");
            } else {
                this.gids.clear();

                for (int i = 0; i < numGids; i++)
                    this.gids.add(reader.readUnsignedIntAsLong());
            }
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeInt(this.gids.size());
            writer.writeInt(this.colprimType);

            for (int i = 0; i < this.gids.size(); i++)
                writer.writeUnsignedInt(this.gids.get(i));
        }
    }

    @Getter
    public static class EventCommandDisableCreate extends CommandStandard {
        private final List<Long> gids = new ArrayList<>();

        public EventCommandDisableCreate() {
            super(EventType.ECMD_GID_DISABLE_CREATE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_GID_DISABLE_CREATE(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gids.size()).append(")");

            if (this.gids.size() > 0) {
                builder.append(", /* Gids: */ ");

                for (int i = 0; i < this.gids.size(); i++) {
                    if (i > 0)
                        builder.append(", ");

                    builder.append(this.gids.get(i));
                }
            }
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            long numGids = reader.readUnsignedIntAsLong();
            reader.skipInt();
            this.gids.clear();

            for (int i = 0; i < numGids; i++)
                this.gids.add(reader.readUnsignedIntAsLong());
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeInt(this.gids.size());
            writer.writeInt(0);

            for (int i = 0; i < this.gids.size(); i++)
                writer.writeUnsignedInt(this.gids.get(i));
        }
    }

    @Getter
    public static class EventCommandEnableCreate extends CommandStandard {
        private final List<Long> gids = new ArrayList<>();

        public EventCommandEnableCreate() {
            super(EventType.ECMD_GID_ENABLE_CREATE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_GID_ENABLE_CREATE(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gids.size()).append(")");

            if (this.gids.size() > 0) {
                builder.append(", /* Gids: */ ");

                for (int i = 0; i < this.gids.size(); i++) {
                    if (i > 0)
                        builder.append(", ");

                    builder.append(this.gids.get(i));
                }
            }
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            long numGids = reader.readUnsignedIntAsLong();
            reader.skipInt();
            this.gids.clear();

            for (int i = 0; i < numGids; i++)
                this.gids.add(reader.readUnsignedIntAsLong());
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeInt(this.gids.size());
            writer.writeInt(0);

            for (int i = 0; i < this.gids.size(); i++)
                writer.writeUnsignedInt(this.gids.get(i));
        }
    }

    @Getter
    public static class EventCommandColprimDisableSingle extends CommandStandard {
        private long gid;

        public EventCommandColprimDisableSingle() {
            super(EventType.ECMD_COL_PRIM_DISABLE_S);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_COL_DISABLE_S(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
            reader.skipInt();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
            writer.writeInt(MM_COLP_TYPE_COLANDEVENT);
        }
    }

    @Getter
    public static class EventCommandColprimEnableSingle extends CommandStandard {
        private long gid;

        public EventCommandColprimEnableSingle() {
            super(EventType.ECMD_COL_PRIM_ENABLE_S);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_COL_ENABLE_S(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
            reader.skipInt();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
            writer.writeInt(MM_COLP_TYPE_COLANDEVENT);
        }
    }

    @Getter
    public static class EventCommandSetBehaviour extends CommandStandard {
        private long gid;
        private long behaviour;

        public EventCommandSetBehaviour() {
            super(EventType.ECMD_GID_SET_BEHAVIOUR);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_GID_SET_BEHAVIOUR(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(", ")
                    .append(this.behaviour).append("/*TODO: Behavior name.*/")  // The name of the behavior is not realistic to automatically determine in FrogLord, so we leave it to the user.
                    .append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
            this.behaviour = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
            writer.writeUnsignedInt(this.behaviour);
        }
    }

    @Getter
    public static class EventCommandSetPositionRotation extends CommandStandard {
        private long gid;
        private short x;
        private short y;
        private short z;
        private short yRotation;
        private int cmdFlags;

        public EventCommandSetPositionRotation() {
            super(EventType.ECMD_GID_SET_POS_AND_ROT);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_GID_SET_POS_AND_ROT(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(", ");

            writeFixedVector(builder, this.x, this.y, this.z, this.yRotation);
            builder.append(", ").append(getBitFlagsString(this.cmdFlags, EventSetPosFlag.values())).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
            this.x = reader.readShort();
            this.y = reader.readShort();
            this.z = reader.readShort();
            this.yRotation = reader.readShort();
            this.cmdFlags = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
            writer.writeShort(this.x);
            writer.writeShort(this.y);
            writer.writeShort(this.z);
            writer.writeShort(this.yRotation);
            writer.writeInt(this.cmdFlags);
        }
    }

    @Getter
    public static class EventCommandSetPosition extends CommandStandard {
        private long gid;
        private short x;
        private short y;
        private short z;

        public EventCommandSetPosition() {
            super(EventType.ECMD_GID_SET_POS);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_GID_SET_POS(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(", ");

            writeFixedVector(builder, this.x, this.y, this.z);
            builder.append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
            this.x = reader.readShort();
            this.y = reader.readShort();
            this.z = reader.readShort();
            reader.skipShort();
            reader.skipInt();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
            writer.writeShort(this.x);
            writer.writeShort(this.y);
            writer.writeShort(this.z);
            writer.writeShort((short)0);
            writer.writeInt(0);
        }
    }

    @Getter
    public static class EventCommandEntityClearTrigger extends CommandStandard {
        private long gid;

        public EventCommandEntityClearTrigger() {
            super(EventType.ECMD_GID_CLR_TRIGGER);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_ENT_CLR_TRIGGER(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
        }
    }

    @Getter
    public static class EventCommandEntitySetTrigger extends CommandStandard {
        private long gid;

        public EventCommandEntitySetTrigger() {
            super(EventType.ECMD_GID_SET_TRIGGER);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_ENT_SET_TRIGGER(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
        }
    }

    @Getter
    public static class EventCommandEntityDestroy extends CommandStandard {
        private long gid;

        public EventCommandEntityDestroy() {
            super(EventType.ECMD_ENT_DESTROY);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_ENT_DESTROY(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.gid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.gid = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.gid);
        }
    }

    @Getter
    public static class EventCommandEntityCreate extends CommandStandard {
        private int species;
        private short subSpecies;
        private short behaviour;
        private short x;
        private short y;
        private short z;
        private short yRotation;
        private int initFlags;
        private int destroyFlags;
        private int state1Flags;
        private int state2Flags;

        public EventCommandEntityCreate() {
            super(EventType.ECMD_ENT_CREATE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_ENT_CREATE(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", "
                    ).append(getEnumName(EntitySpecies.values(), this.species)).append(", ")
                    .append(this.subSpecies).append("/*TODO: Sub-Species Name from 'entdef.h'*/, ")  // The name of the subspecies is not realistic to automatically determine in FrogLord, so we leave it to the user.
                    .append(this.behaviour).append("/*TODO: Behavior name*/, "); // The name of the behavior is not realistic to automatically determine in FrogLord, so we leave it to the user.

            writeFixedVector(builder, this.x, this.y, this.z, this.yRotation);
            builder.append(", ").append(getBitFlagsString(this.initFlags, EntityInitInfoFlags.values()))
                    .append(", ").append(getBitFlagsString(this.destroyFlags, EntityDestroyInfoFlags.values()))
                    .append(", ").append(getBitFlagsString(this.state1Flags, EntityState1Flags.values()))
                    .append(", ").append(getBitFlagsString(this.state2Flags, EntityState2Flags.values()))
                    .append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.species = reader.readUnsignedShortAsInt();
            this.subSpecies = reader.readUnsignedByteAsShort();
            this.behaviour = reader.readUnsignedByteAsShort();
            this.x = reader.readShort();
            this.y = reader.readShort();
            this.z = reader.readShort();
            this.yRotation = reader.readShort();
            this.initFlags = reader.readInt();
            this.destroyFlags = reader.readInt();
            this.state1Flags = reader.readInt();
            this.state2Flags = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedShort(this.species);
            writer.writeUnsignedByte(this.subSpecies);
            writer.writeUnsignedByte(this.behaviour);
            writer.writeShort(this.x);
            writer.writeShort(this.y);
            writer.writeShort(this.z);
            writer.writeShort(this.yRotation);
            writer.writeInt(this.initFlags);
            writer.writeInt(this.destroyFlags);
            writer.writeInt(this.state1Flags);
            writer.writeInt(this.state2Flags);
        }
    }

    @Getter
    public static class EventCommandCallFunction extends CommandStandard {
        private long functionAddress;
        private long parameter;

        public EventCommandCallFunction() {
            super(EventType.ECMD_CALL_FUNC);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_CALL_FUNC(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.functionAddress))
                    .append("/*TODO: Use real function name.*/, ")  // The name of the function is not realistic to automatically determine in FrogLord, so we leave it to the user.
                    .append(NumberUtils.toHexString(this.parameter)).append("/*TODO: Determine if this needs to be named or anything This can vary per-function.*/)");  // The parameter data is not realistic to automatically determine in FrogLord, so we leave it to the user.
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.functionAddress = reader.readUnsignedIntAsLong();
            this.parameter = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.functionAddress);
            writer.writeUnsignedInt(this.parameter);
        }
    }

    @Getter
    public static class EventCommandSetUserVar extends CommandStandard {
        private short index;
        private short value;

        public EventCommandSetUserVar() {
            super(EventType.ECMD_SET_USER_VAR);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_SET_USER_VAR(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(this.index).append(", ")
                    .append(this.value).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.index = reader.readShort();
            this.value = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeShort(this.index);
            writer.writeShort(this.value);
        }
    }

    public static class EventCommandShowMe extends CommandStandard {
        public EventCommandShowMe() {
            super(EventType.ECMD_SHOWME);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_SHOWME(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(")");
        }
    }

    @Getter
    public static class EventCommandCallCommandList extends CommandStandard {
        private long commandAddress;

        public EventCommandCallCommandList() {
            super(EventType.ECMD_CALL_CMDLIST);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCMD_CALL_CMDLIST(")
                    .append(this.getSwitchCaseId()).append(", ")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
        }
    }

    @Getter
    public static class EventConditionWaitPlayerInUseZone extends EventBlock {
        private long commandAddress;

        public EventConditionWaitPlayerInUseZone() {
            super(EventType.ECON_WAIT_PLAYER_IN_USE_ZONE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_PLAYER_IN_USE_ZONE(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
        }
    }

    @Getter
    public static class EventConditionWaitPlayerInHelpZone extends EventBlock {
        private long commandAddress;

        public EventConditionWaitPlayerInHelpZone() {
            super(EventType.ECON_WAIT_PLAYER_IN_HELP_ZONE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_PLAYER_IN_HELP_ZONE(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
        }
    }

    @Getter
    public static class EventConditionWaitBranchTimer extends EventBlock {
        private long commandAddress;

        public EventConditionWaitBranchTimer() {
            super(EventType.ECON_WAIT_BRANCH_TIMER);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_BRANCH_TIMER(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ").
                    append(NumberUtils.toHexString(this.commandAddress)).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
        }
    }

    @Getter
    public static class EventConditionWaitPlayerInWarp extends EventBlock {
        private long commandAddress;
        private long warpZone;

        public EventConditionWaitPlayerInWarp() {
            super(EventType.ECON_WAIT_PLAYER_IN_WARP);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_PLAYER_IN_WARP(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.warpZone).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.warpZone = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.warpZone);
        }
    }

    @Getter
    public static class EventConditionGidPermanentlyDead extends EventBlock {
        private long commandAddress;
        private long gid;

        public EventConditionGidPermanentlyDead() {
            super(EventType.ECON_GID_PERMANENTLY_DEAD);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_GID_PERMANENTLY_DEAD(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.gid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.gid = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.gid);
        }
    }

    @Getter
    public static class EventConditionGidKilledByPlayer extends EventBlock {
        private long commandAddress;
        private long gid;

        public EventConditionGidKilledByPlayer() {
            super(EventType.ECON_GID_PLAYER_KILLED);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_GID_PLAYER_KILLED(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.gid).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.gid = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.gid);
        }
    }

    @Getter
    public static class EventConditionUserFunction extends EventBlock {
        private long commandAddress;
        private long functionAddress;
        private long parameter;

        public EventConditionUserFunction() {
            super(EventType.ECON_USER_FUNC);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_USER_FUNC(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(NumberUtils.toHexString(this.functionAddress))
                    .append(" /*TODO: Use the name of this function.*/").append(", ") // The name of the function is not realistic to automatically determine in FrogLord, so we leave it to the user.
                    .append(this.parameter).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.functionAddress = reader.readUnsignedIntAsLong();
            this.parameter = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.functionAddress);
            writer.writeUnsignedInt(this.parameter);
        }
    }

    @Getter
    public static class EventConditionWaitGidBehaviourMode extends EventBlock {
        private long commandAddress;
        private long gid;
        private long mode;

        public EventConditionWaitGidBehaviourMode() {
            super(EventType.ECON_WAIT_GID_BEHAVIOUR_MODE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_GID_BEHAVIOUR_MODE(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.gid).append(", ")
                    .append(this.mode).append(" /* TODO: Apply the name of this behavior */)");  // The name of the behavior is not realistic to automatically determine in FrogLord, so we leave it to the user.
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.gid = reader.readUnsignedIntAsLong();
            this.mode = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.gid);
            writer.writeUnsignedInt(this.mode);
        }
    }

    @Getter
    public static class EventConditionWaitGidOutZone extends EventBlock {
        private long commandAddress;
        private long gid;
        private long zone;

        public EventConditionWaitGidOutZone() {
            super(EventType.ECON_WAIT_GID_OUT_ZONE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_GID_OUT_ZONE(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.gid).append(", ")
                    .append(this.zone).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.gid = reader.readUnsignedIntAsLong();
            this.zone = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.gid);
            writer.writeUnsignedInt(this.zone);
        }
    }

    @Getter
    public static class EventConditionWaitGidInZone extends EventBlock {
        private long commandAddress;
        private long gid;
        private long zone;

        public EventConditionWaitGidInZone() {
            super(EventType.ECON_WAIT_GID_IN_ZONE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_GID_IN_ZONE(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.gid).append(", ")
                    .append(this.zone).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.gid = reader.readUnsignedIntAsLong();
            this.zone = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.gid);
            writer.writeUnsignedInt(this.zone);
        }
    }

    @Getter
    public static class EventConditionWaitPlayerOutPointRadius extends EventBlock {
        private long commandAddress;
        private short x;
        private short y;
        private short z;
        private short radius;

        public EventConditionWaitPlayerOutPointRadius() {
            super(EventType.ECON_WAIT_PLAYER_OUT_POINT_RADIUS);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_PLAYER_OUT_POINT_RADIUS(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ");

            writeFixedVector(builder, this.x, this.y, this.z, this.radius);
            builder.append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.x = reader.readShort();
            this.y = reader.readShort();
            this.z = reader.readShort();
            this.radius = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeShort(this.x);
            writer.writeShort(this.y);
            writer.writeShort(this.z);
            writer.writeShort(this.radius);
        }
    }

    @Getter
    public static class EventConditionWaitPlayerInPointRadius extends EventBlock {
        private long commandAddress;
        private short x;
        private short y;
        private short z;
        private short radius;

        public EventConditionWaitPlayerInPointRadius() {
            super(EventType.ECON_WAIT_PLAYER_IN_POINT_RADIUS);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_PLAYER_IN_POINT_RADIUS(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ");

            writeFixedVector(builder, this.x, this.y, this.z, this.radius);
            builder.append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.x = reader.readShort();
            this.y = reader.readShort();
            this.z = reader.readShort();
            this.radius = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeShort(this.x);
            writer.writeShort(this.y);
            writer.writeShort(this.z);
            writer.writeShort(this.radius);
        }
    }

    @Getter
    public static class EventConditionWaitPlayerOutGidRadius extends EventBlock {
        private long commandAddress;
        private long gid;
        private long radius;

        public EventConditionWaitPlayerOutGidRadius() {
            super(EventType.ECON_WAIT_PLAYER_OUT_GID_RADIUS);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_PLAYER_OUT_GID_RADIUS(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.gid).append(", ")
                    .append(this.radius).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.gid = reader.readUnsignedIntAsLong();
            this.radius = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.gid);
            writer.writeUnsignedInt(this.radius);
        }
    }

    @Getter
    public static class EventConditionWaitPlayerInGidRadius extends EventBlock {
        private long commandAddress;
        private long gid;
        private long radius;

        public EventConditionWaitPlayerInGidRadius() {
            super(EventType.ECON_WAIT_PLAYER_IN_GID_RADIUS);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_PLAYER_IN_GID_RADIUS(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.gid).append(", ")
                    .append(this.radius).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.gid = reader.readUnsignedIntAsLong();
            this.radius = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.gid);
            writer.writeUnsignedInt(this.radius);
        }
    }

    @Getter
    public static class EventConditionWaitPlayerOutZone extends EventBlock {
        private long commandAddress;
        private long zone;

        public EventConditionWaitPlayerOutZone() {
            super(EventType.ECON_WAIT_PLAYER_OUT_ZONE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_PLAYER_OUT_ZONE(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.zone).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.zone = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.zone);
        }
    }

    @Getter
    public static class EventConditionWaitPlayerInZone extends EventBlock {
        private long commandAddress;
        private long zone;

        public EventConditionWaitPlayerInZone() {
            super(EventType.ECON_WAIT_PLAYER_IN_ZONE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_PLAYER_IN_ZONE(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.zone).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.zone = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.zone);
        }
    }

    @Getter
    public static class EventConditionTestUserVarEquality extends EventBlock {
        private long commandAddress;
        private short index;
        private short value;

        public EventConditionTestUserVarEquality() {
            super(EventType.ECON_TEST_USER_VAR_EQ);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_TEST_USER_VAR_EQ(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.index).append(", ")
                    .append(this.value).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.index = reader.readShort();
            this.value = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeShort(this.index);
            writer.writeShort(this.value);
        }
    }

    @Getter
    public static class EventConditionWaitInventoryItem extends EventBlock {
        private long commandAddress;
        private int item;
        private int quantity;

        public EventConditionWaitInventoryItem() {
            super(EventType.ECON_WAIT_INVITEM);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_INVITEM(").
                    append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(getEnumName(InventoryItemType.values(), this.item)).append(", ")
                    .append(this.quantity).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.item = reader.readInt();
            this.quantity = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeInt(this.item);
            writer.writeInt(this.quantity);
        }
    }

    @Getter
    public static class EventConditionWaitTrue extends EventBlock {
        private long commandAddress;
        private long eventId;

        public EventConditionWaitTrue() {
            super(EventType.ECON_WAIT_EVENT_TRUE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_WAIT_EVENT_TRUE(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(", ")
                    .append(NumberUtils.toHexString(this.commandAddress)).append(", ")
                    .append(this.eventId).append(")");
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandAddress = reader.readUnsignedIntAsLong();
            this.eventId = reader.readUnsignedIntAsLong();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeUnsignedInt(this.commandAddress);
            writer.writeUnsignedInt(this.eventId);
        }
    }

    public static class EventConditionAlwaysTrue extends ConditionStandard {
        public EventConditionAlwaysTrue() {
            super(EventType.ECON_ALWAYS_TRUE);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_ALWAYS_TRUE(")
                    .append(this.getBranchLevel()).append(", ")
                    .append(this.getFlagsString()).append(")");
        }
    }

    public static class EventConditionEndCommandList extends ConditionStandard {
        public EventConditionEndCommandList() {
            super(EventType.ECON_END_CMDLIST);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_END_CMDLIST()");
        }
    }

    public static class EventConditionDefineCommandList extends ConditionStandard {
        public EventConditionDefineCommandList() {
            super(EventType.ECON_DEFINE_CMDLIST);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_DEFINE_CMDLIST()");
        }
    }

    public static class EventConditionDefineBranch extends ConditionStandard {
        public EventConditionDefineBranch() {
            super(EventType.ECON_DEFINE_BRANCH);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_DEFINE_BRANCH()");
        }
    }

    public static class EventConditionEnd extends ConditionStandard {
        public EventConditionEnd() {
            super(EventType.ECON_END_CONDITION_LIST);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_END_CONDITION_LIST()");
        }
    }

    public static class EventConditionStart extends ConditionStandard {
        public EventConditionStart() {
            super(EventType.ECON_START_CONDITION_LIST);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("EVCOND_START_CONDITION_LIST()");
        }
    }

    @Getter
    public static class DummyBlock extends EventBlock {
        private byte[] dummyBytes;

        public DummyBlock(EventType type) {
            super(type);
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.dummyBytes = reader.readBytes(this.getSize() - 8);
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeBytes(this.dummyBytes);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("DUMMY ")
                    .append(this.getType()).append(" ")
                    .append(DataUtils.toByteString(this.dummyBytes));
        }
    }

    @Getter
    public abstract static class CommandStandard extends EventBlock {
        private int switchCaseId;

        public CommandStandard(EventType type) {
            super(type);
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.switchCaseId = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeInt(this.switchCaseId);
        }
    }

    @Getter
    public abstract static class ConditionStandard extends EventBlock {
        private int commandInfo;

        public ConditionStandard(EventType type) {
            super(type);
        }

        @Override
        public void load(DataReader reader) {
            super.load(reader);
            this.commandInfo = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            super.save(writer);
            writer.writeInt(this.commandInfo);
        }
    }

    @Getter
    public abstract static class EventBlock extends GameObject {
        @NonNull private final EventType type;
        private short branchLevel;
        private short size;
        private int flags;

        public EventBlock(EventType type) {
            this.type = type;
        }

        @Override
        public void load(DataReader reader) {
            this.branchLevel = reader.readUnsignedByteAsShort();
            this.size = reader.readUnsignedByteAsShort();
            this.flags = reader.readInt();
            
            boolean isCommand = this instanceof CommandStandard || this instanceof EventConditionEndCommandList;
            boolean hasCommandFlag = (this.flags & EventConditionFlag.EF_IS_COMMAND_B.getBitMask()) == EventConditionFlag.EF_IS_COMMAND_B.getBitMask();
            if (hasCommandFlag != isCommand) {
                if (isCommand) {
                    throw new RuntimeException("This " + this.getClass().getSimpleName() + " is a command, but did not have the command flag!");
                } else {
                    throw new RuntimeException("This " + this.getClass().getSimpleName() + " is not a command, but has the command flag!");
                }
            } else {
                if (hasCommandFlag)
                    this.flags ^= EventConditionFlag.EF_IS_COMMAND_B.getBitMask();
            }
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort((short)this.type.getIdentifier());
            writer.writeUnsignedByte(this.branchLevel);
            writer.writeUnsignedByte(this.size);
            int flags = this.flags;
            if (this instanceof CommandStandard || this instanceof EventConditionEndCommandList)
                flags |= EventConditionFlag.EF_IS_COMMAND_B.getBitMask();

            writer.writeInt(flags);
        }

        /**
         * Get a string containing a textual form of the bit flags.
         */
        protected String getFlagsString() {
            return getBitFlagsString(this.flags, EventConditionFlag.values());
        }

        /**
         * Write this event block as a valid string to the builder.
         * @param builder the builder to write the event block to
         */
        public abstract void writeAsText(StringBuilder builder);

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            this.writeAsText(builder);
            return builder.toString();
        }
    }

    @Getter
    public enum EventType {
        ECON_START_CONDITION_LIST(0, EventConditionStart::new),
        ECON_END_CONDITION_LIST(1, EventConditionEnd::new),
        ECON_DEFINE_BRANCH(2, EventConditionDefineBranch::new),
        ECON_DEFINE_CMDLIST(3, EventConditionDefineCommandList::new),
        ECON_END_CMDLIST(4, EventConditionEndCommandList::new),
        ECON_ALWAYS_TRUE(5, EventConditionAlwaysTrue::new),
        ECON_WAIT_EVENT_TRUE(6, EventConditionWaitTrue::new),
        ECON_WAIT_INVITEM(7, EventConditionWaitInventoryItem::new),
        ECON_TEST_USER_VAR_EQ(8, EventConditionTestUserVarEquality::new),
        ECON_WAIT_PLAYER_IN_ZONE(9, EventConditionWaitPlayerInZone::new),
        ECON_WAIT_PLAYER_OUT_ZONE(10, EventConditionWaitPlayerOutZone::new),
        ECON_WAIT_PLAYER_IN_GID_RADIUS(11, EventConditionWaitPlayerInGidRadius::new),
        ECON_WAIT_PLAYER_OUT_GID_RADIUS(12, EventConditionWaitPlayerOutGidRadius::new),
        ECON_WAIT_PLAYER_IN_POINT_RADIUS(13, EventConditionWaitPlayerInPointRadius::new),
        ECON_WAIT_PLAYER_OUT_POINT_RADIUS(14, EventConditionWaitPlayerOutPointRadius::new),
        ECON_WAIT_GID_IN_ZONE(15, EventConditionWaitGidInZone::new),
        ECON_WAIT_GID_OUT_ZONE(16, EventConditionWaitGidOutZone::new),
        ECON_WAIT_GID_BEHAVIOUR_MODE(17, EventConditionWaitGidBehaviourMode::new),
        ECON_USER_FUNC(18, EventConditionUserFunction::new),
        ECON_GID_PLAYER_KILLED(19, EventConditionGidKilledByPlayer::new),
        ECON_GID_PERMANENTLY_DEAD(20, EventConditionGidPermanentlyDead::new),
        ECON_WAIT_PLAYER_IN_WARP(21, EventConditionWaitPlayerInWarp::new),
        ECON_WAIT_BRANCH_TIMER(22, EventConditionWaitBranchTimer::new),
        ECON_WAIT_PLAYER_IN_HELP_ZONE(23, EventConditionWaitPlayerInHelpZone::new),
        ECON_WAIT_PLAYER_IN_USE_ZONE(24, EventConditionWaitPlayerInUseZone::new),
        ECON_MAX_CONDITION(25, null),
        ECMD_CALL_CMDLIST(99, EventCommandCallCommandList::new),
        ECMD_SHOWME(100, EventCommandShowMe::new),
        ECMD_SET_USER_VAR(101, EventCommandSetUserVar::new),
        ECMD_CALL_FUNC(102, EventCommandCallFunction::new),
        ECMD_ENT_CREATE(103, EventCommandEntityCreate::new),
        ECMD_ENT_DESTROY(104, EventCommandEntityDestroy::new),
        ECMD_ENT_FREEZE_ALL(105, null),
        ECMD_ENT_UNFREEZE_ALL(106, null),
        ECMD_GID_SET_TRIGGER(107, EventCommandEntitySetTrigger::new),
        ECMD_GID_CLR_TRIGGER(108, EventCommandEntityClearTrigger::new),
        ECMD_GID_SET_BEHAVIOUR(109, EventCommandSetBehaviour::new),
        ECMD_GID_SET_POS(110, EventCommandSetPosition::new),
        ECMD_GID_SET_POS_AND_ROT(111, EventCommandSetPositionRotation::new),
        ECMD_GID_ENABLE_CREATE(112, EventCommandEnableCreate::new),
        ECMD_GID_DISABLE_CREATE(113, EventCommandDisableCreate::new),
        ECMD_COL_PRIM_ENABLE(114, EventCommandColpEnable::new),
        ECMD_COL_PRIM_DISABLE(115, EventCommandColpDisable::new),
        ECMD_COL_PRIM_ENABLE_S(116, EventCommandColprimEnableSingle::new),
        ECMD_COL_PRIM_DISABLE_S(117, EventCommandColprimDisableSingle::new),
        ECMD_GID_AND_COLP_ENABLE(118, EventCommandEnableBoth::new),
        ECMD_GID_AND_COLP_DISABLE(119, EventCommandDisableBoth::new),
        ECMD_CHANGE_POLY_HEIGHT(120, EventCommandChangePolyHeight::new),
        ECMD_EVENT_SET_VALUE(121, EventCommandSetEventValue::new),
        ECMD_SET_BRANCH_TIMER(122, EventCommandSetBranchTimer::new),
        ECMD_WAKE_UP_INACTIVE_ENT(123, EventCommandWakeUpInactiveEntity::new),
        ECMD_GID_SET_INIT_USER_1(124, EventCommandGidSetInitUser1::new),
        ECMD_MAX_COMMAND(125, null);

        private final int identifier;
        private final Supplier<EventBlock> blockCreator;
        private static final Map<Integer, EventType> EVENT_TYPES_BY_ID = new HashMap<>();

        EventType(int identifier, Supplier<EventBlock> blockCreator) {
            this.identifier = identifier;
            this.blockCreator = blockCreator;
        }

        /**
         * Gets the EventType by its ID, if the provided ID is valid
         * @param id the id of the event type
         * @return eventType or null
         */
        public static EventType getEventTypeById(short id) {
            return EVENT_TYPES_BY_ID.get((int) id);
        }

        static {
            for (int i = 0; i < values().length; i++) {
                EventType type = values()[i];
                EVENT_TYPES_BY_ID.put(type.getIdentifier(), type);
            }
        }
    }
}
 