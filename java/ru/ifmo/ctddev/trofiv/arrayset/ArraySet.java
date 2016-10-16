package ru.ifmo.ctddev.trofiv.arrayset;

import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private static final String IMMUTABLE_DATA_STRUCTURE = "Immutable data structure!";
    private static final String EMPTY_SET = "Empty set!";
    private static final IntUnaryOperator INCREMENT = operand -> operand + 1;
    private static final IntUnaryOperator DECREMENT = operand -> operand - 1;
    private final Object[] data;
    private final Comparator<? super E> comparator;

    @SuppressWarnings("ZeroLengthArrayAllocation")
    public ArraySet() {
        this.data = new Object[0];
        comparator = null;
    }

    public ArraySet(final Collection<? extends E> source) {
        this.data = source.stream().sorted().distinct().toArray();
        comparator = null;
    }

    private ArraySet(final E[] source, final int fromInclusive, final int toExclusive, final Comparator<? super E> comparator) {
        this.data = Arrays.copyOfRange(source, fromInclusive, toExclusive);
        this.comparator = comparator;
    }

    public ArraySet(final Collection<? extends E> source, final Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.data = source.stream()
                .collect(Collectors.toCollection(() -> new TreeSet<E>(comparator)))
                .stream()
                .toArray();
    }

    @Override
    @SuppressWarnings({"unchecked", "ReturnOfNull"})
    public E lower(final E key) {
        final int position = lowerPosition(key);
        return position < 0 ? null : (E) data[position];
    }

    private int lowerPosition(final E key) {
        final int position = binarySearch(key);
        if (position == 0) {
            return -1;
        } else if (position < 0) {
            final int insertionPoint = -position - 1;
            return insertionPoint == 0 ? -1 : insertionPoint - 1;
        } else {
            return position - 1;
        }
    }

    @SuppressWarnings({"unchecked", "SuspiciousArrayCast"})
    private int binarySearch(final E key) {
        return comparator == null
                ? Arrays.binarySearch(data, key)
                : Arrays.binarySearch((E[]) data, key, comparator);
    }

    @Override
    @SuppressWarnings({"unchecked", "ReturnOfNull"})
    public E floor(final E key) {
        final int position = floorPosition(key);
        return position < 0 ? null : (E) data[position];
    }

    private int floorPosition(final E key) {
        final int position = binarySearch(key);
        if (position < 0) {
            final int insertionPoint = -position - 1;
            return insertionPoint == 0 ? -1 : insertionPoint - 1;
        } else {
            return position;
        }
    }

    @Override
    @SuppressWarnings({"ReturnOfNull", "unchecked"})
    public E ceiling(final E key) {
        final int position = ceilingPosition(key);
        return position < 0 ? null : (E) data[position];
    }

    private int ceilingPosition(final E key) {
        final int position = binarySearch(key);
        if (position < 0) {
            final int insertionPoint = -position - 1;
            return insertionPoint == data.length ? -1 : insertionPoint;
        } else {
            return position;
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "ReturnOfNull"})
    public E higher(final E key) {
        final int position = higherPosition(key);
        return position < 0 ? null : (E) data[position];
    }

    private int higherPosition(final E key) {
        final int position = binarySearch(key);
        if (position == data.length - 1) {
            return -1;
        } else if (position < 0) {
            final int insertionPoint = -position - 1;
            return insertionPoint == data.length ? -1 : insertionPoint;
        } else {
            return position + 1;
        }
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException(IMMUTABLE_DATA_STRUCTURE);
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException(IMMUTABLE_DATA_STRUCTURE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(this, comparator == null
                ? (o1, o2) -> -((Comparable<? super E>) o1).compareTo(o2)
                : comparator.reversed());
    }

    @Override
    @SuppressWarnings("ReturnOfInnerClass")
    public Iterator<E> descendingIterator() {
        return new Itr(DECREMENT, -1);
    }

    @Override
    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
        final int fromPosition = fromPosition(fromElement, fromInclusive);
        final int toPosition = toPosition(toElement, toInclusive);
        return subSetByPositions(fromPosition, toPosition);
    }

    @Override
    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        final int fromPosition = 0;
        final int toPosition = toPosition(toElement, inclusive);
        return subSetByPositions(fromPosition, toPosition);
    }

    @Override
    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        final int fromPosition = fromPosition(fromElement, inclusive);
        final int toPosition = data.length - 1 < 0 ? 0 : data.length - 1;
        return subSetByPositions(fromPosition, toPosition);
    }

    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    private int fromPosition(final E key, final boolean inclusive) {
        return inclusive ? ceilingPosition(key) : higherPosition(key);
    }

    private int toPosition(final E key, final boolean inclusive) {
        return inclusive ? floorPosition(key) : lowerPosition(key);
    }

    @SuppressWarnings({"unchecked", "SuspiciousArrayCast"})
    private NavigableSet<E> subSetByPositions(final int fromElement, final int toElement) {
        return fromElement == -1 || toElement == -1 || toElement < fromElement
                ? new ArraySet<>()
                : new ArraySet<>((E[]) data, fromElement, toElement + 1, this.comparator);
    }

    @Override
    public SortedSet<E> headSet(final E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(final E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    @SuppressWarnings("ReturnOfInnerClass")
    public Iterator<E> iterator() {
        return new Itr(INCREMENT, data.length);
    }

    @Override
    public int size() {
        return data.length;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return binarySearch((E) o) >= 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsAll(final Collection<?> c) {
        for (Object o : c) {
            if (binarySearch((E) o) < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E first() {
        if (data.length == 0) {
            throw new NoSuchElementException(EMPTY_SET);
        } else {
            return (E) data[0];
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public E last() {
        if (data.length == 0) {
            throw new NoSuchElementException(EMPTY_SET);
        } else {
            return (E) data[data.length - 1];
        }
    }

    private class Itr implements Iterator<E> {
        private final IntUnaryOperator operator;
        private final int end;
        private int position;

        Itr(final IntUnaryOperator operator, final int end) {
            this.operator = operator;
            this.end = end;
        }

        @Override
        public boolean hasNext() {
            return position != end;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if (hasNext()) {
                final E result = (E) data[position];
                position = operator.applyAsInt(position);
                return result;
            } else {
                throw new NoSuchElementException("No more elements!");
            }
        }
    }
}