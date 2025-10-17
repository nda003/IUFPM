package vn.datm.ituna.util;

import java.util.ArrayList;
import java.util.List;

public class UPList {
  private static final int LINEAR_SEARCH_THRESHOLD = 32;

  // private int itemId;
  private double expectedSupport;
  private double maxSupport;
  private List<TPPair> pairs = new ArrayList<>();

  protected UPList() {
    expectedSupport = 0;
    maxSupport = 0;
  }

  public UPList(int itemId, int tid, double prob) {
    // this.itemId = itemId;
    expectedSupport = prob;
    maxSupport = prob;
    pairs.add(new TPPair(tid, prob));
  }

  public boolean addTPPair(int tid, double prob) {
    expectedSupport += prob;

    if (prob > maxSupport) {
      maxSupport = prob;
    }

    return pairs.add(new TPPair(tid, prob));
  }

  public TPPair getTransationAt(int index) {
    return pairs.get(index);
  }

  // public UItemSet getItemSet() {
  //   return new UItemSet(itemId, expectedSupport);
  // }

  // public int getId() {
  //   return itemId;
  // }

  public double getExpectedSupport() {
    return expectedSupport;
  }

  public double getMaxSupport() {
    return maxSupport;
  }

  public boolean isEmpty() {
    return pairs.size() == 0;
  }

  public int size() {
    return pairs.size();
  }

  public int getTransationIndex(int tid) {
    if (pairs.size() <= LINEAR_SEARCH_THRESHOLD) {
      return linearSearchTransationIndex(tid);
    } else {
      return binarySearchSearchTransationIndex(tid);
    }
  }

  public int getTransationIndex(int tid, int from) {
    if (from >= size()) {
      return -1;
    }

    if (pairs.get(from).tid() == tid) {
      return from;
    }

    if (pairs.size() - from <= LINEAR_SEARCH_THRESHOLD) {
      return linearSearchTransationIndex(tid, from);
    } else {
      return binarySearchSearchTransationIndex(tid, from);
    }
  }

  private int linearSearchTransationIndex(int tid) {
    for (int i = 0; i < pairs.size(); i++) {
      if (pairs.get(i).tid() == tid) {
        return i;
      }
    }

    return -1;
  }

  private int linearSearchTransationIndex(int tid, int from) {
    for (int i = from; i < pairs.size(); i++) {
      if (pairs.get(i).tid() == tid) {
        return i;
      }
    }

    return -1;
  }

  private int binarySearchSearchTransationIndex(int tid) {
    int left = 0;
    int right = pairs.size() - 1;

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

  private int binarySearchSearchTransationIndex(int tid, int from) {
    int left = from;
    int right = pairs.size() - 1;

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

  // @Override
  // public String toString() {
  //   if (isEmpty()) {
  //     return String.format(
  //         "([%d], expSup=%.2f, maxSup=%.2f, [])", itemId, expectedSupport, maxSupport);
  //   }

  //   StringBuilder sb =
  //       new StringBuilder(
  //           String.format("([%d], expSup=%.2f, maxSup=%.2f, [", itemId, expectedSupport, maxSupport)
  //               + pairs.get(0).toString());

  //   for (int i = 1; i < pairs.size(); i++) {
  //     sb.append(", " + pairs.get(i).toString());
  //   }

  //   sb.append("])");

  //   return sb.toString();
  // }
}
