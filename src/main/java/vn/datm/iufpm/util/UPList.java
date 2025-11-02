package vn.datm.iufpm.util;

import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public class UPList {
  private static final int LINEAR_SEARCH_THRESHOLD = 32;

  private double expectedSupport;
  private double maxSupport;
  protected IntArrayList tidList;
  protected DoubleArrayList probList;

  protected UPList() {
    expectedSupport = 0;
    maxSupport = 0;
    tidList = new IntArrayList();
    probList = new DoubleArrayList();
  }

  public UPList(int tid, double prob) {
    expectedSupport = prob;
    maxSupport = prob;
    tidList = IntArrayList.newListWith(tid);
    probList = DoubleArrayList.newListWith(prob);
  }

  public void addTransaction(int tid, double prob) {
    expectedSupport += prob;

    if (prob > maxSupport) {
      maxSupport = prob;
    }

    tidList.add(tid);
    probList.add(prob);
  }

  public void ensureCapacity(int minCapacity) {
    tidList.ensureCapacity(minCapacity);
    probList.ensureCapacity(minCapacity);
  }

  public int getTidAt(int index) {
    return tidList.get(index);
  }

  public double getProbAt(int index) {
    return probList.get(index);
  }

  public double getExpectedSupport() {
    return expectedSupport;
  }

  public double getMaxSupport() {
    return maxSupport;
  }

  public boolean isEmpty() {
    return tidList.isEmpty();
  }

  public int size() {
    return tidList.size();
  }

  public int search(int tid, int from) {
    if (from >= tidList.size()) {
      return -1;
    }

    if (tidList.get(from) == tid) {
      return from;
    }

    if (tidList.size() - from <= LINEAR_SEARCH_THRESHOLD) {
      return linearSearchTransationIndex(tid, from);
    } else {
      return binarySearchSearchTransationIndex(tid, from);
    }
  }

  private int linearSearchTransationIndex(int tid, int from) {
    for (int i = from; i < tidList.size(); i++) {
      if (tidList.get(i) == tid) {
        return i;
      }
    }

    return -1;
  }

  private int binarySearchSearchTransationIndex(int tid, int from) {
    int left = from;
    int right = tidList.size() - 1;

    while (left <= right) {
      int mid = left + (right - left) / 2;
      int midTid = tidList.get(mid);

      if (midTid > tid) {
        right = mid - 1;
      } else if (midTid < tid) {
        left = mid + 1;
      } else {
        return mid;
      }
    }

    return -1;
  }

  // @Override
  // public String toString() {
  //   if (isEmpty()) {
  //     return String.format(
  //         "([%d], expSup=%.2f, maxSup=%.2f, [])", itemId, expectedSupport, maxSupport);
  //   }

  //   StringBuilder sb =
  //       new StringBuilder(
  //           String.format("([%d], expSup=%.2f, maxSup=%.2f, [", itemId, expectedSupport,
  // maxSupport)
  //               + pairs.get(0).toString());

  //   for (int i = 1; i < pairs.size(); i++) {
  //     sb.append(", " + pairs.get(i).toString());
  //   }

  //   sb.append("])");

  //   return sb.toString();
  // }
}
