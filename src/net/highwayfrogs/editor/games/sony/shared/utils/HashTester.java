package net.highwayfrogs.editor.games.sony.shared.utils;

import net.highwayfrogs.editor.gui.extra.hash.FroggerHashUtil;

import java.util.Scanner;

/**
 * Tests hashes from frogger.
 * Created by Kneesnap on 2/17/2022.
 */
public class HashTester {

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        SCHashPermuter.findLinkerHashes();

        Scanner scanner = new Scanner(System.in);
        int targetHash = -1;
        while (true) {
            System.out.print("Please enter the string to hash: ");
            String line = scanner.nextLine();
            if (line.startsWith("!")) {
                try {
                    targetHash = Integer.parseInt(line.substring(1));
                    System.out.println("Updated target.");
                } catch (NumberFormatException ex) {
                    System.out.println("Couldn't read '" + line + "' as a number.");
                }
                continue;
            }

            print(line, targetHash);
        }
    }

    private static void print(String input, int targetHash) {
        System.out.print("Input '");
        System.out.print(input);
        System.out.print("', Linker Hash: ");
        System.out.print(FroggerHashUtil.getPsyQLinkerHash(input));
        System.out.print(", Assembler Hash: ");
        System.out.print(FroggerHashUtil.getPsyQAssemblerHash(input));
        if (targetHash >= 0) {
            System.out.print(", Target Offset: ");
            System.out.print(((FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE + targetHash) - FroggerHashUtil.getPsyQLinkerHash(input)) % FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE);
        }

        System.out.println();
    }
}