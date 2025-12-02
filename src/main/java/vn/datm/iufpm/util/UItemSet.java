package vn.datm.iufpm.util;

import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;

/**
 * An uncertain itemset.
 */
public class UItemSet implements Comparable<UItemSet> {
  private final ImmutableIntSet ids;
  private double expectedSupport = 0;

  /**
   * Constructs an itemset with the specified id and probability.
   *
   * @param id The id of the itemset.
   * @param probability The probability of the itemset.
   */
  public UItemSet(int id, double probability) {
    ids = IntSets.immutable.with(id);
    expectedSupport = probability;
  }

  /**
   * Constructs an itemset with the specified set and probability.
   *
   * @param set The set of the itemset.
   * @param probability The probability of the itemset.
   */
  public UItemSet(ImmutableIntSet set, double probability) {
    ids = set;
    expectedSupport = probability;
  }

  /**
   * Adds the specified probability to the expected support.
   *
   * @param prob The probability to add.
   */
  public void addToExpectedSupport(double prob) {
    expectedSupport += prob;
  }

  /**
   * Returns the ids of this itemset.
   *
   * @return The ids of this itemset.
   */
  public ImmutableIntSet getIds() {
    return ids;
  }

  /**
   * Returns the expected support of this itemset.
   *
   * @return The expected support of this itemset.
   */
  public double getExpectedSupport() {
    return expectedSupport;
  }

  /**
   * Returns the number of items in this itemset.
   *
   * @return The number of items in this itemset.
   */
  public int size() {
    return ids.size();
  }

  /**
   * Returns {@code true} if the specified object is equal to this itemset.
   *
   * @param obj The object to compare to.
   * @return {@code true} if the specified object is equal to this itemset.
   */
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

  /**
   * Compares this itemset to the specified itemset.
   *
   * @param o The itemset to compare to.
   * @return A negative integer, zero, or a positive integer as this itemset is less than, equal
   *     to, or greater than the specified itemset.
   */
  @Override
  public int compareTo(UItemSet o) {
    return Double.compare(this.expectedSupport, o.getExpectedSupport());
  }
}
