package net.highwayfrogs.editor.system;

import net.highwayfrogs.editor.Utils;

import java.util.Arrays;

/**
 * A replacement for List<Integer> which avoids auto-boxing.
 * Created by Kneesnap on 10/5/2018.
 */
public class IntList {
    private int[] array;
    private int size;

    public IntList() {
        this(8);
    }

    public IntList(int startSize) {
        this.array = new int[startSize];
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public boolean contains(int value) {
        return indexOf(value) != -1;
    }

    public boolean add(int value) {
        if (this.size == this.array.length - 1)
            resize(this.array.length * 2);

        this.array[this.size++] = value;
        return false;
    }

    private void resize(int newSize) {
        this.array = Arrays.copyOf(array, newSize);
    }

    public int remove(int index) {
        int val = get(index);
        System.arraycopy(array, index + 1, array, index, array.length - (index + 1));
        this.size--;
        return val;
    }

    public void clear() {
        this.size = 0;
    }

    public int get(int index) {
        Utils.verify(index >= 0 && this.size > index, "Out of range.");
        return array[index];
    }

    public int set(int index, int newValue) {
        int val = get(index);
        array[index] = newValue;
        return val;
    }

    public int indexOf(int value) {
        for (int i = 0; i < array.length; i++)
            if (array[i] == value)
                return i;
        return -1;
    }

    public int[] getArray() {
        return Arrays.copyOf(array, this.size);
    }
}
