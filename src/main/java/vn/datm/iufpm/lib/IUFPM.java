package vn.datm.iufpm.lib;

import java.util.Comparator;
import java.util.List;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;
import vn.datm.iufpm.db.UTDatabase;
import vn.datm.iufpm.util.UItemSet;

/** An abstract class for incremental top-K uncertain frequent pattern mining models. */
public abstract class IUFPM {
  /** LimitedSortedItemSets */
  protected class LimitedSortedItemSets {
    private MutableSortedSet<UItemSet> sets;
    private final int maximumSize;

    /**
     * Constructs a new, empty set sorted according to the specified comparator.
     *
     * @param maximumSize The maximum size for the itemsets
     * @param comparator The comparator to sort the itemsets
     */
    public LimitedSortedItemSets(int maximumSize, Comparator<UItemSet> comparator) {
      this.sets = new TreeSortedSet<>(comparator);
      this.maximumSize = maximumSize;
    }

    /**
     * Appends all of the elements in the specified iterable to the sets then reduce the sets to the
     * {@link #maximumSize}.
     *
     * @param c
     */
    public void addAll(Iterable<UItemSet> c) {
      sets.addAllIterable(c);

      if (sets.size() > maximumSize) {
        sets = sets.take(maximumSize);
      }
    }

    /**
     * Returns the last (highest) element currently in the sets.
     *
     * @return The last (highest) element currently in the sets.
     */
    public UItemSet getLast() {
      return sets.getLast();
    }

    /**
     * Adds the specified itemset and polls last (highest) element if already at {@link
     * #maximumSize}
     *
     * @param set The set to be added.
     */
    public void add(UItemSet set) {
      sets.add(set);

      if (sets.size() > maximumSize) {
        sets.remove(sets.last());
      }
    }

    /**
     * Returns the sets as a {@link MutableList}.
     *
     * @return The sets as a {@link MutableList}.
     */
    public MutableList<UItemSet> toList() {
      return sets.toList();
    }

    /**
     * Returns the number of itemset the sets.
     *
     * @return The number of itemset the sets.
     */
    public int size() {
      return sets.size();
    }

    @Override
    public String toString() {
      return sets.toString();
    }
  }

  /** The value of top-K. */
  protected final int k;

  /**
   * The current transation being procesed.
   *
   * @see {@link #addDatabase(UTDatabase)}
   */
  protected int currentTid = 0;

  /** The minimum support, recorded for pruning. */
  protected double minimumSupport = 0;

  /**
   * Initialize this model with a top-K value
   *
   * @param k The top-K value.
   */
  protected IUFPM(int k) {
    this.k = k;
  }

  /**
   * Increment this model using the specified {@link UTDatabase}, uses {@link #currentTid} to record
   * transactions.
   *
   * @param db The database to add to the model.
   */
  public abstract void addDatabase(UTDatabase db);

  /**
   * Mine the transactions recorded in this model.
   *
   * @return A list of {@link #k}-length {@link UItemSet} sorted in descending order according to their
   *     expected support.
   */
  public abstract List<UItemSet> mine();
}
