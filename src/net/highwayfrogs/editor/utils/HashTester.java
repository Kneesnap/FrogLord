package net.highwayfrogs.editor.utils;

import net.highwayfrogs.editor.gui.extra.hash.FroggerHashUtil;

import java.util.Scanner;

/**
 * Tests hashes from frogger.
 * Created by Kneesnap on 2/17/2022.
 */
public class HashTester {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        do {
            System.out.print("Please enter the string to hash: ");
            String str = scanner.nextLine();
            System.out.println("Linker    Hash: " + FroggerHashUtil.getLinkerHash(str) + "\t\t\tFull: " + FroggerHashUtil.getFullLinkerHash(str));
            System.out.println("Assembler Hash: " + FroggerHashUtil.getAssemblerHash(str) + "\t\t\tFull: " + FroggerHashUtil.getFullAssemblerHash(str));
        } while (true);
    }
}
