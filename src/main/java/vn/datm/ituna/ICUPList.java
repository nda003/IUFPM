package vn.datm.ituna;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ICUPList {
  private UItemSet itemSet;
  private double maxSupport;
  private List<TPPair> pairs = new ArrayList<>();

  private List<SimpleEntry<Set<Integer>, Integer>> lateIndexs;

  public ICUPList(IUPList l1, IUPList l2) {
    itemSet = new UItemSet(l1.getId(), l2.getId());
    lateIndexs = new ArrayList<>(2);
    lateIndexs.add(new SimpleEntry<>(ImmutableSet.of(l1.getId()), 0));
    lateIndexs.add(new SimpleEntry<>(ImmutableSet.of(l2.getId()), 0));
  }

  public ICUPList(ICUPList l1, IUPList l2) {
    itemSet = new UItemSet(l1.getIds(), l2.getId());
    lateIndexs = new ArrayList<>(2);
    lateIndexs.add(new SimpleEntry<>(l1.getIds(), 0));
    lateIndexs.add(new SimpleEntry<>(ImmutableSet.of(l2.getId()), 0));
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

  public Set<Integer> getIds() {
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

  public void setLateIndex(Set<Integer> pattern, int value) {
    for (SimpleEntry<Set<Integer>, Integer> entry : lateIndexs) {
      if (entry.getKey().equals(pattern)) {
        entry.setValue(value);
        return;
      }
    }
  }

  public TPPair getTransationAt(int index) {
    return pairs.get(index);
  }

  public void setLateIndex(int pattern, int value) {
    for (SimpleEntry<Set<Integer>, Integer> entry : lateIndexs) {
      if (entry.getKey().size() == 1 && entry.getKey().contains(pattern)) {
        entry.setValue(value);
        return;
      }
    }
  }

  public int getLateIndex(Set<Integer> pattern) {
    for (SimpleEntry<Set<Integer>, Integer> entry : lateIndexs) {
      if (entry.getKey().equals(pattern)) {
        return entry.getValue();
      }
    }

    return -1;
  }

  public int getLateIndex(int pattern) {
    for (SimpleEntry<Set<Integer>, Integer> entry : lateIndexs) {
      if (entry.getKey().size() == 1 && entry.getKey().contains(pattern)) {
        return entry.getValue();
      }
    }

    return -1;
  }

  public List<Set<Integer>> getParentPatterns() {
    return ImmutableList.of(lateIndexs.get(0).getKey(), lateIndexs.get(1).getKey());
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
          "(%s, expSup=%.2f, maxSup=%.2f, lateIndex=%s, [])",
          itemSet.getIds().toString(),
          itemSet.getExpectedSupport(),
          maxSupport,
          lateIndexs.toString());
    }

    StringBuilder sb =
        new StringBuilder(
            String.format(
                    "(%s, expSup=%.2f, maxSup=%.2f, lateIndex=%s, [",
                    itemSet.getIds().toString(),
                    itemSet.getExpectedSupport(),
                    maxSupport,
                    lateIndexs.toString())
                + pairs.get(0).toString());

    sb.append("])");

    return sb.toString();
  }
}
