package net.highwayfrogs.editor.utils;

import lombok.Getter;
import net.highwayfrogs.editor.gui.extra.hash.FroggerHashUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This tool has been created to permute variations of certain keywords, combining them together with other keywords, in order to find linker hash matches.
 * Created by Kneesnap on 11/2/2023.
 */
public class SCHashPermuter {
    /**
     * Finds variable names configured in code.
     */
    public static void findLinkerHashes() {
        // NOTE: The impact of capitalizing a letter is the same as capitalizing any other letter.
        // For example, 'Frog_drownparticle' has the same hash as 'fRog_drownparticle' or frog_drOwnparticle.
        // This means we only need to account for capital letters until we're confident there are no more. (See Game_over_Multiplayer_played_text for an example, how 'g' in game_over could be capitalized, but we never included a capital G thus lowering computation time)

        // NOTE: The order of the letters (and by extension words) has no impact on the resulting hash.
        // So, if we find a match, we can reorder words to be nicer, which means we only need to supply a list of words without caring about their order.

        // address,prev_sym_linker_hash/next_sym_linker_hash,previous_symbol/next_symbol type name

        // Frog_bubble_view_matrix
        // Result 'Frog_bubble_view_matrix', Linker Hash: 382, Assembler Hash: 103
        // Result 'Frog_splash_matrix', Linker Hash: 382', Assembler Hash: 108

        // PSX Build 71 (Retail NTSC):
        // 0x800B39D8,53/58,xa_channel_play/MRSND_sample_info_ptr MR_2DSPRITE* Game_over_press_fire
        // 0x800C0FD8,381/382,im_choose_course_f/im_fly_500 MR_MAT Frog_drownparticle
        //findLinkerHash(381, 382, f("Frog"), f("drown"), f("splash"), f("water"), f("bubble"), f("scale"), f("trans"), f("matrix"), f("particle"));
        // 0x800C61A0,430/431,_que/im_fly_50a MR_2DSPRITE*[4] Game_over_Multiplayer_played_text
        //findLinkerHash(430, 431, f("Frog"), f("multiplayer", "Multiplayer", "MUltiplayer"), f("gameover", "game_over", "end_of_game", "game_end"), f("played"), f("text"), f("sprite"), f("ptr", "ptrs"));
        // 0x800BB968,129/131,High_score_view_number_anim_env_ptr/Frog_player_data MR_2DSPRITE*[4] Game_over_Multiplayer_won_text
        //findLinkerHash(129, 131, f("Frog"), f("multiplayer", "Multiplayer", "MUltiplayer"), f("gameover", "game_over", "end_of_game", "game_end"), f("won"), f("text"), f("sprite"), f("ptr", "ptrs"));
        // 0x800C61A0,234/242,im_opt_now_formatting/im_fire_flya MR_2DSPRITE*[4] Game_over_Multiplayer_lost_text
        //findLinkerHash(234, 242, f("Frog"), f("multiplayer", "Multiplayer", "MUltiplayer"), f("gameover", "game_over", "end_of_game", "game_end"), f("lost"), f("text"), f("sprite"), f("ptr", "ptrs"));

        // 0x800BEE58,277/278,im_32x32_5/im_32x32_6 MR_MAT Rat_splash_matrix
        //findLinkerHash(277, 278, f("Swp", "Swamp"), f("rat", "Rat"), f("splash"), f("scale"), f("trans"), f("matrix"), f("particle"));

        // 0x800BF9B0,307/316,MRSND_moving_sound_root/High_score_input_lily_infos MR_USHORT[8] Cached_states
        //findLinkerHash(309, 316, f("MR", "Old", "old"), f("Cache", "cache", "Cached", "cached"), f("Input", "input"), f("controller", "Controller"), f("flag", "flags", "state", "states"));

        // 0x800B3B4C,186/189,LS_wait/LS_save_mode MR_2DSPRITE* Selection_Options_ptr
        //findLinkerHash(187, 189, f("Frog"), f("Selection", "selection"), f("Option", "Options", "option", "options"), f("text"), f("sprite"), f("ptr"));
        //findLinkerHash(187, 189, "Frog_selection_", null, f("race", "Race"), f("option", "options"), f("text"), f("sprite"), f("ptr"));

        // 0x800B3B70,211/213,High_score_view_delayed_request/Frog_selection_master_player_id MR_2DSPRITE* Options_extras_user_prompt_ptr (Options_text_ptr was close but didn't follow the checklist against Frog_selection_master_player_id)
        //findLinkerHash(211, 213, f("Options", "options", "Option", "option"), f("extras"), f("frog"), f("select", "selection", "control", "controls", "next"),  f("text"), f("sprite"), f("inst"), f("ptr"));
        //findLinkerHash(211, 213, f("Options", "options", "Option", "option"), f("extras"), f("frog"), f("user_prompt"),  f("text"), f("sprite"), f("inst"), f("ptr"));

        // 0x800B3EE0,485/490,MRSND_viewports/Option_page_current MR_2DSPRITE* Pad_user_prompt_text_instptr
        //findLinkerHash(485, 490, "Pad_", null, f("select", "selection", "control", "controls", "next", "user_prompt"),  f("text"), f("sprite"), f("inst"), f("ptr"));
        //findLinkerHash(485, 490, "High_score_view_", null, f("select", "selection", "control", "controls", "next", "user_prompt"),  f("text"), f("sprite"), f("inst"), f("ptr"));

        // 0x800B3C5C,295/298,MROT_root_ptr/Game_language MR_BOOL High_score_view_automatic_picked_flag
        //findLinkerHash(296, 297, f("HSView", "High_score", "High_score_view", "New_high_score", "New_high_score_view"), f("automatic", "manual"), f("level"), f("found", "select", "selected", "reach", "reached", "locate", "located", "picked", "current", "show", "shown"), f("flag"));

        // 0x800B3764,81/82,Map_entity_ptrs/LS_extras_matrix_ptr MR_2DSPRITE* LSunformatted_sprite
        //findLinkerHash(82, 82, f("LS", "Load", "Save", "LoadSave"), f("title", "selection", "message", "extras"), f("format", "now_formatting", "formatting", "unformatted", "not_formatted"), f("display", "status"),  f("failed"), f("text"), f("sprite"), f("inst"), f("ptr"));

        // 0x800B3A48,89/95,Sel_light_inst_2/High_score_input_frog_num MR_2DSPRITE* LS_select_sprite_inst_ptr
        //findLinkerHash(89, 94, f("LS", "Load", "Save", "LoadSave"), f("title", "selection", "message", "extras"), f("user_prompt"), f("select", "selection", "control", "controls"), f("display", "status"),  f("text"), f("sprite"), f("inst"), f2("ptr"));
        //LS_select_sprite_inst_ptr 95 70
        //LS_controls_status_sprite_inst_ptr 95 61
        //LS_control_status_text_sprite_inst 91 57
        //LS_controls_text_ptr 95 75
        // LS_user_prompt_controls_sprite_ptr 94 60
        // LS_title_status_text 91 71
        // LS_title_select_text_inst 89 64
        // LS_title_controls_status_text_inst 89 55
        // LS_title_user_prompt_status_sprite 90 56
        // LoadSave_selection_display_ptr 92

        // Image 0 - im_gen_frogeye
        //findLinkerHash(461, 462, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 1 - im_sub_lorry
        //findLinkerHash(289, 291, "im_", null, f("opt", "gen", "org", "sub"), f("frog", "frogger"), f("lorry"), f("logo"));

        // Image 2 - ?
        //findLinkerHash(313, 316, "im_", null, f("opt", "gen", "org", "sub"), f("dog"), f("kennel"), f("roof"), f("wood", "planks"), f("panel"));
        //findLinkerHash(313, 316, "im_", null, f("opt", "gen", "org", "sub"), f("start"), f("jetty"), f("wood", "wooden"), f("planks"), f("panel"));

        // Image 3 - ? Previous one but multiplayer. Seems to include "mult".
        //findLinkerHash(259, 259, "im_", null, f("optm", "genm", "orgm", "subm"), f("dog"), f("kennel"), f("roof"), f("wood", "planks"), f("panel"));
        //findLinkerHash(259, 259, "im_", null, f("optm", "genm", "orgm", "subm"), f("start"), f("jetty"), f("wood", "wooden"), f("planks"), f("panel"));

        // Image 4 - Quite close to last one im_opt_wood_roof or im_subkennelwood
        //findLinkerHash(197, 198, "im_", null, f("opt", "gen", "org", "sub"), f("dog"), f("kennel"), f("roof"), f("wood", "planks"), f("panel"));

        // Image 5 - im_sub_stopsign
        // Image 6 - im_sub_stat_tunnel
        //findLinkerHash(417, 418, "im_", null, f("opt", "gen", "sub", "org"), f("stat"), f("car"), f("tunnel"), f("cover"), f("panel"), f("side"));

        // Image 7 - Several options: im_sub_treebark_4, im_opt_palm_bark1, im_opt_tree_fat_1,
        //findLinkerHash(210, 210, "im_", null, f("opt", "gen", "sub", "org", "jun"), f("stat"), f("palm"), f("tree"), f("fat"), f("bark"), f("0", "1", "2", "3", "4"));

        // Image 8 - ?
        //findLinkerHash(197, 198, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 9 - im_des_stat_cacti

        // Image 10 - ? Cow skull
        //findLinkerHash(194, 194, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 11 - im_des_cowskull
        //findLinkerHash(83, 83, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 12 - ? beaver (im_gen_beaver - 336)
        //findLinkerHash(337, 337, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 13 - im_cav_rockfall
        //findLinkerHash(43, 44, "im_", null, f("opt", "gen", "cav"), f("rock", "stonebridge"), f("block"), f("fall"), f("floor"), f("0"));

        // Image 14 - ?
        //findLinkerHash(454, 454, "im_cav_", null, f("stat"), f("rock", "stone"), f("bridge"), f("block"), f("fall"), f("floor"), f("0"));

        // Image 15 - ?
        //findLinkerHash(?, ?, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 16 - im_cav_rope
        //findLinkerHash(143, 144, "im_cav_", null, f("rope"), f("bridge"), f("ball"), f("0"));

        // Image 17 - ? Slime
        //findLinkerHash(70, 71, "im_cav_", null, f("snail"), f("slime", "goo", "slimy"), f("trail", "path", "marker"));

        // Image 18 - ? Lava
        //findLinkerHash(146, 147, "im_cav_", null, f("deadly", "molten"), f("lava", "magma", "water"), f("flow", "flowing", "move", "moving"), f("stream", "river"));

        // Image 19 - ?
        //findLinkerHash(496, 496, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 20 - im_for_stat_tree_brach TODO: Probably wrong since image 24 seems to be incremented by 1. im_for_bark_4???
        //findLinkerHash(283, 283, "im_for_", null, f("stat"), f("tree"), f("bark"), f("branch", "brach"), f("safe"), f("swing", "swinging"), f("break", "breaking"), f("0", "1", "2", "3", "4"));

        // Image 21 - im_for_fungi
        //findLinkerHash(256, 257, "im_", null, f("for", "form"), f("stat"), f("tree"), f("branch", "brach"), f("fungi"));

        // Image 22 - im_for_fungi3
        // Image 23 - im_for_fungi2

        // Image 24 - ? Another branch
        //findLinkerHash(283, 283, "im_for_", null, f("stat"), f("tree"), f("bark"), f("branch", "brach"), f("safe"), f("swing", "swinging"), f("break", "breaking"));

        // Image 25 - ?
        //findLinkerHash(?, ?, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 26 - ?
        //findLinkerHash(?, ?, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 27 - ?
        //findLinkerHash(?, ?, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 28 - ?
        //findLinkerHash(?, ?, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 29 - ?
        //findLinkerHash(?, ?, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 30 - ?
        //findLinkerHash(?, ?, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 31 - im_for_toadstool
        //findLinkerHash(196, 197, "im_for_", null, f("stat"), f("mush", "mushroom", "mush_room", "toadstool"));

        // Image ? - ?
        //findLinkerHash(?, ?, "im_", null, f("opt", "gen"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 63-?
        //findLinkerHash(241, 241, "im_", null, f("gen", "genm", "opt", "optm"), f("multiplayer", "multi", "mult"), f("frog"), f("race"), f("stripe"), f("0", "1", "2"));

        // Image 338
        findLinkerHash(502, 502, "im_", null, f("org"), f("bull_dozer", "dozer", "bulldozer", "truck", "yellow_truck"), f("tire", "tires", "tyre", "tyres"), f("tread", "treads", "thread", "threads"), f("00", "0"));

        // Image 415-?
        //findLinkerHash(415, 415, "im_", null, f("vol"), f("lava", "magma", "fire", "wooden"), f("circle", "spin", "spinning", "spinner", "platform"), f("rings"), f("0"));

        // Image 483 - im_gen_frogeye_0
        //findLinkerHash(94, 94, "im_", null, f("opt", "gen"), f("baby"), f("frog", "frogger"), f("eye"), f("ball"), f("0"));

        // Image 622 -
        findLinkerHash(11, 11, "im_", null, f("cav"), f("floor"), f("water", "ice", "icy", "goo", "liquid"), f("puddle", "patch", "splash", "pool"), f("0"));

        // Image 1256 - ?
        findLinkerHash(316, 316, "im_des_", null, f("target", "tgt", "trgt", "targ"), f("frog"), f("start", "starting", "begin", "player"), f("bullseye"), f("spawn", "spawner", "home", "base"), f("floor", "paint", "flag"), f("circle"), f("rug", "pad", "spot", "point"), f("decal"), f("mark", "marker"));

        // Image 1441 - im_opt_rings
        //findLinkerHash(278, 278, "im_", null, f("opt", "gen", "org", "for"), f("tree", "log", "wood", "wooden"), f("end", "stump"), f("rings"), f("0")); // im_for_rings / im_opt_rings seems legit.

        // Image 1458 - ?
        findLinkerHash(284, 285, "im_", null, f("jun"), f("stone"), f("frog"), f("statue"), f("plinth"), f("door"), f("0"));

        // Image 1540 - ?
        findLinkerHash(382, 383, "im_", null, f("cav"), f("stat"), f("web", "cobweb"), f("wall"), f("0"));

        // Image 1799 - im_gold_frogeye
        //findLinkerHash(57, 59, "im_", null, f("opt", "gen"), f("baby", "gold", "golden", "checkpoint"), f("frog", "frogger"), f("eye"), f("ball"), f("0", "1"));

        // Image 1827 ? - ?
        findLinkerHash(310, 312, "im_", null, f("org", "orgm", "org_multiplayer"), f("water"), f("river", "stream"), f("low", "poly", "low_poly", "lowpoly", "lo", "win95"));

        // Image 1828 ? - ?
        findLinkerHash(313, 316, "im_", null, f("jun", "junm", "jun_multiplayer"), f("water"), f("river"), f("stream"), f("low", "poly", "low_poly", "lowpoly", "lo", "win95"));

        // Image 1829 ? - ?
        findLinkerHash(117, 123, "im_for_", null, f("leaf", "leaves"), f("tree", "treetop", "treetops"), f("0", "1", "2", "3", "4", "5", "6"));

        // Image 1830 ? - ?
        findLinkerHash(28, 30, "im_for_", null, f("leaf", "leaves"), f("tree", "treetop", "treetops"), f("0", "1", "2", "3", "4", "5", "6"));

        // Misc:
        //findLinkerHash(194, 195, "im_", null, f("opt", "gen"), f("goldfrog", "gold_frog", "golden_frog", "gold_frog"), f("flash", "anim", "stripe"), f("0"));
    }

    private static boolean shouldShowWord(String result, List<Word> words, int[] wordsIndices) {
        boolean isImage = result.startsWith("im_");

        // Require underscores.
        if (!isImage) {
            for (int i = 0; i < words.size() - 1; i++) {
                String permutationWord = words.get(i).getPermutationWord(wordsIndices[i]);
                if (!permutationWord.isEmpty() && !permutationWord.endsWith("_"))
                    return false;
            }
        }

        return !result.endsWith("_") && (Character.isUpperCase(result.charAt(0)) || isImage) && (result.length() <= 35);
    }

    // frogger string word
    private static Word f(String word) {
        return new OptionalWord(new SuffixedWord(new LiteralWord(word), "_", true));
    }

    private static Word _f(String word) {
        return new OptionalWord(new SuffixedWord(new LiteralWord(word), "_", false));
    }

    // frogger string word
    private static Word f2(String word) {
        return new OptionalWord(new LiteralWord(word));
    }

    @SuppressWarnings("unused")
    private static Word f(String... words) {
        return new OptionalWord(new SuffixedWord(new LiteralWord(words), "_", true));
    }

    private static Word _f(String... words) {
        return new OptionalWord(new SuffixedWord(new LiteralWord(words), "_", false));
    }

    /**
     * Find strings created via permutations of the provided words which have the desired linker hash.
     * @param linkerHash The desired linker hash.
     * @param words      The list of words to permute. (Each word can have multiple "permutation words", to allow for variations.)
     */
    @SuppressWarnings("unused")
    public static void findLinkerHash(int linkerHash, Word... words) {
        findLinkerHash(linkerHash, linkerHash, null, null, words);
    }

    /**
     * Find strings created via permutations of the provided words which have the desired linker hash.
     * @param minLinkerHash The minimum desired linker hash.
     * @param maxLinkerHash The maximum desired linker hash.
     * @param words         The list of words to permute. (Each word can have multiple "permutation words", to allow for variations.)
     */
    public static void findLinkerHash(int minLinkerHash, int maxLinkerHash, Word... words) {
        findLinkerHash(minLinkerHash, maxLinkerHash, null, null, words);
    }

    /**
     * Find strings created via permutations of the provided words which have the desired linker hash.
     * @param minLinkerHash The minimum desired linker hash.
     * @param maxLinkerHash The maximum desired linker hash.
     * @param prefix        A prefix to apply to the string. Can be null.
     * @param suffix        A suffix to apply to the string. Can be null.
     * @param words         The list of words to permute. (Each word can have multiple "permutation words", to allow for variations.)
     */
    public static void findLinkerHash(int minLinkerHash, int maxLinkerHash, String prefix, String suffix, Word... words) {
        findLinkerHash(minLinkerHash, maxLinkerHash, prefix, suffix, Arrays.asList(words));
    }

    /**
     * Find strings created via permutations of the provided words which have the desired linker hash.
     * @param minLinkerHash The minimum desired linker hash.
     * @param maxLinkerHash The maximum desired linker hash.
     * @param prefix        A prefix to apply to the string. Can be null.
     * @param suffix        A suffix to apply to the string. Can be null.
     * @param words         The list of words to permute. (Each word can have multiple "permutation words", to allow for variations.)
     */
    public static void findLinkerHash(int minLinkerHash, int maxLinkerHash, String prefix, String suffix, List<Word> words) {
        if (minLinkerHash < 0 || minLinkerHash >= FroggerHashUtil.LINKER_HASH_TABLE_SIZE)
            throw new IllegalArgumentException("The provided minimum linker hash was not in the expected range of a linker hash. (" + minLinkerHash + ")");
        if (maxLinkerHash < 0 || maxLinkerHash >= FroggerHashUtil.LINKER_HASH_TABLE_SIZE)
            throw new IllegalArgumentException("The provided maximum linker hash was not in the expected range of a linker hash. (" + maxLinkerHash + ")");
        if (words == null || words.isEmpty())
            throw new IllegalArgumentException("Cannot find linker hash when no words were specified.");

        // 1) Calculate number of permutations, and verify ok.
        BigInteger permutationCount = BigInteger.ONE;
        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            int wordCount = word.getPermutationWordCount();
            if (wordCount <= 0)
                throw new IllegalArgumentException("Word '" + word + "' had " + wordCount + " permutation words, but at least one is required!");

            permutationCount = permutationCount.multiply(BigInteger.valueOf(wordCount));
            //System.out.println("Word #" + (i + 1) + ": " + word.toString());
        }

        System.out.println("Testing " + permutationCount + " possible permutations.");

        // 2) Permute results
        long permuteStart = System.currentTimeMillis();
        int tempIndex;
        int[] wordIndices = new int[words.size()];
        StringBuilder builder = new StringBuilder();
        do {
            // a) Generate string.
            builder.setLength(0);
            if (prefix != null && !prefix.isEmpty())
                builder.append(prefix);
            for (int i = 0; i < words.size(); i++)
                builder.append(words.get(i).getPermutationWord(wordIndices[i]));
            if (suffix != null && !suffix.isEmpty())
                builder.append(suffix);

            // b) Check result against linker hash.
            String result = builder.toString();
            int linkerHash = FroggerHashUtil.getLinkerHash(result);
            if (linkerHash >= minLinkerHash && linkerHash <= maxLinkerHash && shouldShowWord(result, words, wordIndices))
                printResult(result); // Frogger global variables always start with a capital letter, (Although PSX SDK ones do not), so we can skip ones that don't.

            // c) Move on to the next permutation.
            tempIndex = wordIndices.length - 1;
            while (tempIndex >= 0 && ++wordIndices[tempIndex] >= words.get(tempIndex).getPermutationWordCount()) {
                wordIndices[tempIndex] = 0;
                tempIndex--; // Try again but with the next one.
            }
        } while (tempIndex >= 0);

        // Done.
        long permuteEnd = System.currentTimeMillis();
        System.out.println("Done permuting in " + (permuteEnd - permuteStart) + " ms.");
        System.out.println();
    }

    private static void printResult(String result) {
        System.out.print("Result '");
        System.out.print(result);
        System.out.print("', Linker Hash: ");
        System.out.print(FroggerHashUtil.getLinkerHash(result));
        System.out.print(", Assembler Hash: ");
        System.out.println(FroggerHashUtil.getAssemblerHash(result));
    }

    public static abstract class Word {
        /**
         * Gets the permutation word for a particular index.
         * @param index The index, assumed to be within the word range. Exceptions may be thrown otherwise.
         * @return word
         */
        public abstract String getPermutationWord(int index);

        /**
         * Gets the number of permutations this word includes.
         */
        public abstract int getPermutationWordCount();

        /**
         * Write the permutation words to the builder.
         * @param builder The builder to write to.
         */
        public void writePermutationWords(StringBuilder builder) {
            int wordCount = getPermutationWordCount();
            for (int i = 0; i < wordCount; i++) {
                if (i > 0)
                    builder.append(", ");

                // ' is an okay character to use this this hashing algorithm was used for symbol (variable) names.
                // Variable names can't contain the character ' so we don't need to worry about escaping.
                builder.append('\'').append(getPermutationWord(i)).append('\'');
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(getClass().getSimpleName());
            builder.append('[');

            // Write permutation words.
            int permutationWordCount = getPermutationWordCount();
            builder.append(permutationWordCount);
            if (permutationWordCount > 0) {
                builder.append(": ");
                writePermutationWords(builder);
            }

            return builder.append(']').toString();
        }
    }

    @Getter
    public static class LiteralWord extends Word {
        private final List<String> literals;

        public LiteralWord(String word) {
            this.literals = new ArrayList<>(1);
            this.literals.add(word);
        }

        public LiteralWord(String... words) {
            this.literals = new ArrayList<>(Arrays.asList(words));
        }

        @Override
        public String getPermutationWord(int index) {
            return this.literals.get(index);
        }

        @Override
        public int getPermutationWordCount() {
            return this.literals.size();
        }
    }

    /**
     * Represents a string which may or may not have the provided suffix.
     */
    public static class SuffixedWord extends Word {
        private final Word wrappedWord;
        private final String suffix;
        private final boolean includeUnmodified;

        public SuffixedWord(Word wrappedWord, String suffix, boolean includeUnmodified) {
            this.wrappedWord = wrappedWord;
            this.suffix = suffix;
            this.includeUnmodified = includeUnmodified;
        }

        @Override
        public String getPermutationWord(int index) {
            int wrappedCount = this.wrappedWord.getPermutationWordCount();
            if (index >= wrappedCount && this.includeUnmodified) {
                return this.wrappedWord.getPermutationWord(index - wrappedCount);
            } else {
                return this.wrappedWord.getPermutationWord(index) + this.suffix;
            }
        }

        @Override
        public int getPermutationWordCount() {
            return this.includeUnmodified ? this.wrappedWord.getPermutationWordCount() * 2 : this.wrappedWord.getPermutationWordCount();
        }
    }

    /**
     * Represents a word which may or may not be utilized.
     */
    public static class OptionalWord extends Word {
        private final Word wrappedWord;

        public OptionalWord(Word word) {
            this.wrappedWord = word;
        }

        @Override
        public String getPermutationWord(int index) {
            if (index > 0) {
                return this.wrappedWord.getPermutationWord(index - 1);
            } else {
                return "";
            }
        }

        @Override
        public int getPermutationWordCount() {
            return this.wrappedWord.getPermutationWordCount() + 1;
        }
    }
}