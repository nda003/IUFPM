package vn.datm.ibuca.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.impl.list.mutable.FastList;

public class ISUPList {
  private static final int LINEAR_SEARCH_THRESHOLD = 32;

  protected LinkedHashMap<Integer, int[]> incSeg = new LinkedHashMap<>();
  protected List<TPPair> pairs = new FastList<>();
  protected int pairsPreviousSize = 0;

  private double maxSupport;
  private double expectedSupport;

  protected ISUPList() {
    maxSupport = 0;
    expectedSupport = 0;
  }

  public ISUPList(double support) {
    maxSupport = support;
    expectedSupport = support;
  }

  public ISUPList(int tid, double prob) {
    maxSupport = prob;
    expectedSupport = prob;
    pairs.add(new TPPair(tid, prob));
  }

  public boolean addPair(int tid, double prob) {
    maxSupport = Math.max(maxSupport, prob);
    expectedSupport += prob;
    return pairs.add(new TPPair(tid, prob));
  }

  public TPPair getPairAt(int index) {
    return pairs.get(index);
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
    incSeg.put(increment, new int[] {pairsPreviousSize, pairs.size() - 1});
    pairsPreviousSize = pairs.size();
  }

  // public void accept(double support) {
  //   maxSupport = Math.max(support, maxSupport);
  //   expectedSupport += support;
  // }

  public double getMaxSupport() {
    return maxSupport;
  }

  public double getExpectedSupport() {
    return expectedSupport;
  }

  public int size() {
    return pairs.size();
  }

  public int search(int tid, int start, int end) {
    if (start > end) {
      return -1;
    }

    if (pairs.get(start).tid() <= tid && tid <= pairs.get(end).tid()) {
      if (pairs.get(start).tid() == tid) {
        return start;
      }

      if (pairs.get(end).tid() == tid) {
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
      if (pairs.get(i).tid() == tid) {
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

      int midPairTID = pairs.get(mid).tid();

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
