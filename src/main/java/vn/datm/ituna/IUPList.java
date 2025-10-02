package vn.datm.ituna;

import java.util.ArrayList;
import java.util.List;

public class IUPList {
  private static final int LINEAR_SEARCH_THRESHOLD = 1024;

  private int itemId;
  private double expectedSupport;
  private double maxSupport;
  private List<TPPair> pairs = new ArrayList<>();

  public IUPList(int itemId, int tid, double prob) {
    this.itemId = itemId;
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

  public ICUPList join(IUPList oList) {
    ICUPList cList = new ICUPList(this, oList);

    for (TPPair pair : pairs) {
      double oProb = oList.getTransationProbability(pair.tid());

      if (oProb > -1) {
        cList.addTPPair(pair.tid(), pair.prob() * oProb);
      }
    }

    return cList;
  }

  public ICUPList join(IUPList oList, double minimumSupport) {
    ICUPList cList = new ICUPList(this, oList);

    for (int i = 0; i < pairs.size(); i++) {
      double oProb = oList.getTransationProbability(pairs.get(i).tid());

      if (oProb > -1) {
        cList.addTPPair(pairs.get(i).tid(), pairs.get(i).prob() * oProb);
      }

      if (minimumSupport - cList.getExpectedSupport()
          > (pairs.size() - i) * oList.getMaxSupport()) {
        break;
      }
    }

    return cList;
  }

  public TPPair getTransationAt(int index) {
    return pairs.get(index);
  }

  public UItemSet getItemSet() {
    return new UItemSet(itemId, expectedSupport);
  }

  public int getId() {
    return itemId;
  }

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

  @Override
  public String toString() {
    if (isEmpty()) {
      return String.format(
          "([%d], expSup=%.2f, maxSup=%.2f, [])", itemId, expectedSupport, maxSupport);
    }

    StringBuilder sb =
        new StringBuilder(
            String.format("([%d], expSup=%.2f, maxSup=%.2f, [", itemId, expectedSupport, maxSupport)
                + pairs.get(0).toString());

    for (int i = 1; i < pairs.size(); i++) {
      sb.append(", " + pairs.get(i).toString());
    }

    sb.append("])");

    return sb.toString();
  }

  public double getTransationProbability(int tid) {
    if (pairs.size() <= LINEAR_SEARCH_THRESHOLD) {
      return linearSearchTransationProbability(tid);
    } else {
      return binarySearchSearchTransationProbability(tid);
    }
  }

  private double linearSearchTransationProbability(int tid) {
    for (TPPair pair : pairs) {
      if (pair.tid() == tid) {
        return pair.prob();
      }
    }

    return -1;
  }

  private double binarySearchSearchTransationProbability(int tid) {
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
        return pairs.get(mid).prob();
      }
    }

    return -1;
  }
}
