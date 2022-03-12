package net.highwayfrogs.editor.utils;

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
            System.out.println("Linker    Hash: " + getLinkerHash(str) + "\t\t\tFull: " + getFullLinkerHash(str));
            System.out.println("Assembler Hash: " + getAssemblerHash(str) + "\t\t\tFull: " + getFullAssemblerHash(str));
        } while (true);
    }

    public static int getFullLinkerHash(String input) {
        int hash = input.length();
        for (int i = 0; i < input.length(); i++)
            hash += input.charAt(i);
        return hash;
    }

    public static int getLinkerHash(String input) {
        return getFullLinkerHash(input) & 511;
    }

    public static int getFullAssemblerHash(String input) {
        int hash = 0;
        for (int i = 0; i < input.length(); i++)
            hash += input.charAt(i);
        return hash;
    }

    public static int getAssemblerHash(String input) {
        return getFullAssemblerHash(input) & 255;
    }
}
