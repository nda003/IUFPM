package vn.datm.ituna;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public class LimitedPriorityQueue<E> extends AbstractQueue<E> implements Serializable {
  private final int maximumSize;
  private PriorityQueue<E> pq;

  public LimitedPriorityQueue(int maximumSize) {
    this.maximumSize = maximumSize;
    pq = new PriorityQueue<>(maximumSize);
  }

  public LimitedPriorityQueue(int maximumSize, Comparator<? super E> comparator) {
    this.maximumSize = maximumSize;
    pq = new PriorityQueue<>(maximumSize, comparator);
  }

  public boolean add(E e) {
    return offer(e);
  }

  public void clear() {
    pq.clear();
  }

  public Comparator<? super E> comparator() {
    return pq.comparator();
  }

  public Object[] toArray() {
    return pq.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return pq.toArray(a);
  }

  public E peekLeast() {
    E min = null;

    for (E e : pq) {
      if (min == null || comparator().compare(e, min) > 0) {
        min = e;
      }
    }


    return min;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    for (E e : c) {
      offer(e);
    }

    return true;
  }

  @Override
  public boolean offer(E e) {
    boolean ret = pq.offer(e);

    // if (pq.size() > maximumSize) {
    //   pq.poll();
    // }

    return ret;
  }

  @Override
  public E poll() {
    return pq.poll();
  }

  @Override
  public E peek() {
    return pq.peek();
  }

  @Override
  public Iterator<E> iterator() {
    return pq.iterator();
  }

  @Override
  public int size() {
    return pq.size();
  }
}
