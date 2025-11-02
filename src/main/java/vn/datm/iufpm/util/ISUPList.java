package vn.datm.iufpm.util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public class ISUPList {
  private static final int LINEAR_SEARCH_THRESHOLD = 32;

  protected LinkedHashMap<Integer, int[]> incSeg = new LinkedHashMap<>();
  protected IntArrayList tidList;
  protected DoubleArrayList probList;
  protected int pairsPreviousSize = 0;

  private double maxSupport;
  private double expectedSupport;

  protected ISUPList() {
    maxSupport = 0;
    expectedSupport = 0;
    tidList = new IntArrayList();
    probList = new DoubleArrayList();
  }

  public ISUPList(int tid, double prob) {
    maxSupport = prob;
    expectedSupport = prob;
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

  public LinkedHashMap<Integer, int[]> getIncrementSegments() {
    return incSeg;
  }

  public boolean containIncrement(int increment) {
    return incSeg.containsKey(increment);
  }

  public int[] getSegmentAt(int increment) {
    return incSeg.get(increment);
  }

  public int incrementSize() {
    return incSeg.size();
  }

  public Map.Entry<Integer, int[]> getLastSegment() {
    return incSeg.lastEntry();
  }

  public void putIncrement(int increment) {
    incSeg.put(increment, new int[] {pairsPreviousSize, tidList.size() - 1});
    pairsPreviousSize = tidList.size();
  }

  public double getMaxSupport() {
    return maxSupport;
  }

  public double getExpectedSupport() {
    return expectedSupport;
  }

  public int size() {
    return tidList.size();
  }

  public int search(int tid, int start, int end) {
    if (start > end) {
      return -1;
    }

    if (tidList.get(start) <= tid && tid <= tidList.get(end)) {
      if (tidList.get(start) == tid) {
        return start;
      }

      if (tidList.get(end) == tid) {
        return end;
      }

      if (end - start + 1 < LINEAR_SEARCH_THRESHOLD) {
        return linearSearch(tid, start, end);
      } else {
        return binarySearch(tid, start, end);
      }
    }

    return -1;
  }

  private int linearSearch(int tid, int start, int end) {
    for (int i = start; i <= end; i++) {
      if (tidList.get(i) == tid) {
        return i;
      }
    }

    return -1;
  }

  private int binarySearch(int tid, int start, int end) {
    int left = start;
    int right = end;

    while (left <= right) {
      int mid = left + (right - left) / 2;

      int midPairTID = tidList.get(mid);

      if (midPairTID > tid) {
        right = mid - 1;
      } else if (midPairTID < tid) {
        left = mid + 1;
      } else {
        return mid;
      }
    }

    return -1;
  }

  @Override
  public String toString() {
    StringBuilder sb =
        new StringBuilder(
            String.format(
                "(expectedSupport=%.2f, maxSupport=%.2f, %s)",
                expectedSupport, maxSupport, incSeg.toString()));

    return sb.toString();
  }
}
