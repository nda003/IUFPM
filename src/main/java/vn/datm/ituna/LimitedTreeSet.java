package vn.datm.ituna;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

/** LimitedTreeSet */
public class LimitedTreeSet<E> extends AbstractSet<E>
    implements NavigableSet<E>, Cloneable, Serializable {

  private final int maximumSize;
  private final TreeSet<E> ts;

  public LimitedTreeSet(int maximumSize) {
    this.maximumSize = maximumSize;
    ts = new TreeSet<>();
  }

  public LimitedTreeSet(int maximumSize, Comparator<? super E> comparator) {
    this.maximumSize = maximumSize;
    ts = new TreeSet<>(comparator);
  }

  public boolean add(E e) {
    boolean ret = ts.add(e);

    if (ts.size() > maximumSize) {
        ts.pollLast();
    }

    return ret;
  }

  public boolean contains(Object o) {
    return ts.contains(o);
  }

  public void clear() {
    ts.clear();
  }

  @Override
  public Comparator<? super E> comparator() {
    return ts.comparator();
  }

  @Override
  public E first() {
    return ts.first();
  }

  @Override
  public E last() {
    return ts.last();
  }

  @Override
  public int size() {
    return ts.size();
  }

  @Override
  public E lower(E e) {
    return ts.lower(e);
  }

  @Override
  public E floor(E e) {
    return ts.floor(e);
  }

  @Override
  public E ceiling(E e) {
    return ts.ceiling(e);
  }

  @Override
  public E higher(E e) {
    return ts.higher(e);
  }

  @Override
  public E pollFirst() {
    return ts.pollFirst();
  }

  @Override
  public E pollLast() {
    return ts.pollLast();
  }

  @Override
  public Iterator<E> iterator() {
    return ts.iterator();
  }

  @Override
  public NavigableSet<E> descendingSet() {
    return ts.descendingSet();
  }

  @Override
  public Iterator<E> descendingIterator() {
    return ts.descendingIterator();
  }

  @Override
  public NavigableSet<E> subSet(
      E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
    return ts.subSet(fromElement, fromInclusive, toElement, toInclusive);
  }

  @Override
  public NavigableSet<E> headSet(E toElement, boolean inclusive) {
    return ts.headSet(toElement, inclusive);
  }

  @Override
  public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
    return ts.tailSet(fromElement, inclusive);
  }

  @Override
  public SortedSet<E> subSet(E fromElement, E toElement) {
    return ts.subSet(fromElement, toElement);
  }

  @Override
  public SortedSet<E> headSet(E toElement) {
    return ts.headSet(toElement);
  }

  @Override
  public SortedSet<E> tailSet(E fromElement) {
    return ts.tailSet(fromElement);
  }
}
