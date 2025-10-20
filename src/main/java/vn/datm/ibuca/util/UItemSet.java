package vn.datm.ibuca.util;

import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;

public class UItemSet implements Comparable<UItemSet> {
  private final ImmutableIntSet ids;
  private double expectedSupport = 0;

  public UItemSet(int id, double probability) {
    ids = IntSets.immutable.with(id);
    expectedSupport = probability;
  }

  public UItemSet(ImmutableIntSet set, double probability) {
    ids = set;
    expectedSupport = probability;
  }

  // public UItemSet(int id1, int id2) {
  //   ids = ImmutableSet.of(id1, id2);
  // }

  // public UItemSet(Set<Integer> set, int id) {
  //   ids = new ImmutableSet.Builder<Integer>().addAll(set).add(id).build();
  // }

  public void addToExpectedSupport(double prob) {
    expectedSupport += prob;
  }

  public ImmutableIntSet getIds() {
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
