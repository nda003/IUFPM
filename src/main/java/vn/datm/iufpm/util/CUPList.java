package vn.datm.iufpm.util;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;

public class CUPList {
  protected UItemSet itemSet;
  protected double maxSupport;
  protected List<TPPair> pairs = new ArrayList<>();

  protected CUPList() {}

  public CUPList(int id1, int id2) {
    itemSet = new UItemSet(id1, id2);
  }

  public CUPList(ImmutableIntSet p, int id) {
    itemSet = new UItemSet(p, id);
  }

  public boolean addTPPair(int tid, double prob) {
    itemSet.addToExpectedSupport(prob);

    if (prob > maxSupport) {
      maxSupport = prob;
    }

    return pairs.add(new TPPair(tid, prob));
  }

  public UItemSet getItemSet() {
    return itemSet;
  }

  public ImmutableIntSet getIds() {
    return itemSet.getIds();
  }

  public double getExpectedSupport() {
    return itemSet.getExpectedSupport();
  }

  public double getMaxSupport() {
    return maxSupport;
  }

  public List<TPPair> getTransactions() {
    return pairs;
  }

  public TPPair getTransationAt(int index) {
    return pairs.get(index);
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
          "(%s, expSup=%.2f, maxSup=%.2f, [])",
          itemSet.getIds().toString(), itemSet.getExpectedSupport(), maxSupport);
    }

    StringBuilder sb =
        new StringBuilder(
            String.format(
                    "(%s, expSup=%.2f, maxSup=%.2f, [",
                    itemSet.getIds().toString(), itemSet.getExpectedSupport(), maxSupport)
                + pairs.get(0).toString());

    for (int i = 1; i < pairs.size(); i++) {
      sb.append(", " + pairs.get(i).toString());
    }

    sb.append("])");

    return sb.toString();
  }
}
