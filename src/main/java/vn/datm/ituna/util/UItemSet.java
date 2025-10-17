package vn.datm.ituna.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;

public class UItemSet implements Comparable<UItemSet> {
  private final Set<Integer> ids;
  private double expectedSupport = 0;

  public UItemSet(int id, double probability) {
    ids = ImmutableSet.of(id);
    expectedSupport = probability;
  }

  public UItemSet(Set<Integer> set, double probability) {
    ids = set;
    expectedSupport = probability;
  }

  public UItemSet(int id1, int id2) {
    ids = ImmutableSet.of(id1, id2);
  }

  public UItemSet(Set<Integer> set, int id) {
    ids = new ImmutableSet.Builder<Integer>().addAll(set).add(id).build();
  }

  public UItemSet(Set<Integer> ids1, Set<Integer> ids2) {
    ids = Sets.union(ids1, ids2).immutableCopy();
  }

  public void addToExpectedSupport(double prob) {
    expectedSupport += prob;
  }

  public Set<Integer> getIds() {
    return ids;
  }

  public double getExpectedSupport() {
    return expectedSupport;
  }

  public int size() {
    return ids.size();
  }

  @Override
  public boolean equals(Object obj) {
    UItemSet other = (UItemSet) obj;
    return ids.equals(other.getIds());
  }

  @Override
  public String toString() {
    return ids.toString() + ':' + expectedSupport;
  }

  @Override
  public int hashCode() {
    return ids.hashCode();
  }

  @Override
  public int compareTo(UItemSet o) {
    return Double.compare(this.expectedSupport, o.getExpectedSupport());
  }
}
