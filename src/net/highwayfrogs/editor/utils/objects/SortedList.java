package net.highwayfrogs.editor.utils.objects;

import java.util.*;

/**
 * Represents a list which automatically sorts itself when objects are added.
 * Created by Kneesnap on 9/23/2023.
 */
public class SortedList<TElement> implements List<TElement> {
    private final Comparator<TElement> comparator;
    private final List<TElement> wrappedList = new ArrayList<>();

    public SortedList(Comparator<TElement> comparator) {
        if (comparator == null)
            throw new NullPointerException("comparator");

        this.comparator = comparator;
    }

    /**
     * Re-sort the current list to ensure proper ordering.
     */
    public void update() {
        this.wrappedList.sort(this.comparator);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object value) {
        return Collections.binarySearch(this.wrappedList, (TElement) value, this.comparator) >= 0;
    }

    @Override
    public boolean add(TElement value) {
        int searchResult = Collections.binarySearch(this.wrappedList, value, this.comparator);
        if (searchResult >= 0)
            return false; // Value is already here.

        int insertionPoint = -(searchResult + 1);
        this.wrappedList.add(insertionPoint, value);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object value) {
        // Try binary search. If it fails, we'll do a normal.
        int searchResult = Collections.binarySearch(this.wrappedList, (TElement) value, this.comparator);
        if (searchResult >= 0 && Objects.equals(this.wrappedList.get(searchResult), value)) {
            this.wrappedList.remove(searchResult);
            return true;
        }

        // Do a slower but thorough removal if we can't find it.
        if (!this.wrappedList.remove(value))
            throw new RuntimeException("Failed to remove the value from the sorted list, but it was seen in the set. This suggests something has gone wrong internally.");

        return true;
    }

    @Override
    public boolean containsAll(Collection<?> values) {
        if (values == null || values.isEmpty())
            return true;

        for (Object value : values)
            if (!contains(value))
                return false;

        return true;
    }

    @Override
    @SuppressWarnings("ConstantConditions") // The warning is just flat out wrong.
    public boolean addAll(Collection<? extends TElement> values) {
        if (values == null || values.isEmpty())
            return false;

        boolean addedAny = false;
        for (TElement value : values)
            if (this.add(value))
                addedAny = true;

        return addedAny;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int indexOf(Object value) {
        int searchResult = Collections.binarySearch(this.wrappedList, (TElement) value, this.comparator);
        return (searchResult >= 0) ? searchResult : -1;
    }

    @Override
    public int lastIndexOf(Object value) {
        return indexOf(value); // There can be no duplicates, so the behavior of these two functions match.
    }

    @Override
    public int size() {
        return this.wrappedList.size();
    }

    @Override
    public boolean isEmpty() {
        return this.wrappedList.isEmpty();
    }

    @Override
    public Iterator<TElement> iterator() {
        return this.wrappedList.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.wrappedList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] arr) {
        return this.wrappedList.toArray(arr);
    }

    @Override
    public boolean addAll(int index, Collection<? extends TElement> c) {
        throw new UnsupportedOperationException("Cannot add elements to a specific index in a sorted list.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.wrappedList.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.wrappedList.retainAll(c);
    }

    @Override
    public void clear() {
        this.wrappedList.clear();
    }

    @Override
    public TElement get(int index) {
        return this.wrappedList.get(index);
    }

    @Override
    public TElement set(int index, TElement element) {
        throw new UnsupportedOperationException("Cannot set elements at a specific index in a sorted list.");
    }

    @Override
    public void add(int index, TElement element) {
        throw new UnsupportedOperationException("Cannot add elements to a specific index in a sorted list.");
    }

    @Override
    public TElement remove(int index) {
        return this.wrappedList.remove(index);
    }

    @Override
    public ListIterator<TElement> listIterator() {
        return this.wrappedList.listIterator();
    }

    @Override
    public ListIterator<TElement> listIterator(int index) {
        return this.wrappedList.listIterator(index);
    }

    @Override
    public List<TElement> subList(int fromIndex, int toIndex) {
        return this.wrappedList.subList(fromIndex, toIndex);
    }
}