package vn.datm.ituna;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

public class LimitedSortedList<E> extends AbstractList<E>
    implements RandomAccess, Cloneable, Serializable {
  private ArrayList<E> elements;
  private Comparator<? super E> comparator;
  private final int maximumSize;

  public LimitedSortedList(int maximumSize) {
    elements = new ArrayList<>(maximumSize + 1);
    this.maximumSize = maximumSize;
  }

  public LimitedSortedList(int maximumSize, Comparator<? super E> comparator) {
    elements = new ArrayList<>(maximumSize + 1);
    this.comparator = comparator;
    this.maximumSize = maximumSize;
  }

  public E getFirst() {
    return elements.get(0);
  }

  public E getLast() {
    return elements.get(elements.size() - 1);
  }

  public E pollFirst() {
    Collections.rotate(elements, 1);
    return pollLast();
  }

  public E pollLast() {
    E ret = getLast();
    elements.remove(size() - 1);
    return ret;
  }

  public boolean add(E e) {
    elements.add(e);
    Collections.sort(elements, comparator);

    if (size() > maximumSize) {
      elements.remove(size() - 1);
    }

    return true;
  }

  public boolean addAll(Collection<? extends E> c) {
    boolean ret = elements.addAll(c);
    Collections.sort(elements, comparator);

    if (elements.size() > maximumSize) {
      elements.subList(maximumSize, elements.size()).clear();
    }

    return ret;
  }

  public Object[] toArray() {
    return elements.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return elements.toArray(a);
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override
  public E get(int index) {
    return elements.get(index);
  }

  @Override
  public String toString() {
    return elements.toString();
  }
}
