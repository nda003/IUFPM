package vn.datm.ituna;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import org.jheaps.DoubleEndedHeap;
import org.jheaps.array.MinMaxBinaryArrayDoubleEndedHeap;

public class EvictingMinMaxPriorityQueue<E> extends AbstractQueue<E> {
  private DoubleEndedHeap<E> heap;
  private int maximumSize;

  public EvictingMinMaxPriorityQueue(int maximumSize) {
    this.heap = new MinMaxBinaryArrayDoubleEndedHeap<>(maximumSize + 1);
    this.maximumSize = maximumSize;
  }

  public EvictingMinMaxPriorityQueue(int maximumSize, Comparator<E> comparator) {
    this.heap = new MinMaxBinaryArrayDoubleEndedHeap<>(comparator, maximumSize + 1);
    this.maximumSize = maximumSize;
  }

  public EvictingMinMaxPriorityQueue(E[] array, int maximumSize, Comparator<E> comparator) {
    this.heap = MinMaxBinaryArrayDoubleEndedHeap.heapify(array, comparator);

    for (int i = 0; i < heap.size() - maximumSize; i++) {
      heap.deleteMax();
    }

    this.maximumSize = maximumSize;
  }

  public boolean offerMin(E e) {
    heap.insert(e);

    if (heap.size() > maximumSize) {
      heap.deleteMin();
    }

    return true;
  }

  public boolean offerMax(E e) {
    heap.insert(e);

    if (heap.size() > maximumSize) {
      heap.deleteMax();
    }

    return true;
  }

  public E peekMin() {
    return heap.findMin();
  }

  public E peekMax() {
    return heap.findMax();
  }

  public E pollMin() {
    return heap.deleteMin();
  }

  public E pollMax() {
    return heap.deleteMax();
  }

  public void clear() {
    heap.clear();
  }

  public boolean isEmpty() {
    return heap.isEmpty();
  }

  @Override
  public boolean offer(E e) {
    return offerMax(e);
  }

  @Override
  public E poll() {
    return heap.deleteMax();
  }

  @Override
  public E peek() {
    return heap.findMax();
  }

  @Override
  public Iterator<E> iterator() {
    throw new UnsupportedOperationException("Unimplemented method 'iterator'");
  }

  @Override
  public int size() {
    return (int) heap.size();
  }

  @Override
  public String toString() {
    return heap.toString();
  }
}
