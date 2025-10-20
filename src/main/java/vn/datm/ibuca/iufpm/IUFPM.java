package vn.datm.ibuca.iufpm;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import vn.datm.ibuca.db.UItem;
import vn.datm.ibuca.db.UTDatabase;
import vn.datm.ibuca.util.UItemSet;
import vn.datm.ibuca.util.UPList;

public abstract class IUFPM {
  protected class LimitedSortedItemSets {
    private MutableList<UItemSet> sets;
    private final int maximumSize;
    private Comparator<UItemSet> comparator;

    public LimitedSortedItemSets(int maximumSize, Comparator<UItemSet> comparator) {
      this.sets = new FastList<>(maximumSize + 1);
      this.comparator = comparator;
      this.maximumSize = maximumSize;
    }

    public void addAll(Iterable<UItemSet> c) {
      sets.addAllIterable(c);
      sets.sortThis(comparator);

      if (sets.size() > maximumSize) {
        sets.subList(maximumSize, sets.size()).clear();
      }
    }

    public UItemSet getLast() {
      return sets.getLast();
    }

    public void add(UItemSet set) {
      sets.add(set);
      Collections.sort(sets, comparator);

      if (sets.size() > maximumSize) {
        sets.remove(sets.size() - 1);
      }
    }

    public MutableList<UItemSet> toList() {
      return sets;
    }

    public int size() {
      return sets.size();
    }

    @Override
    public String toString() {
      return sets.toString();
    }
  }

  protected final int k;

  protected int currentTid = 0;
  protected double minimumSupport = 0;

  protected MutableIntObjectMap<UPList> iupMap = new IntObjectHashMap<>();

  protected IUFPM(int k) {
    this.k = k;
  }

  public void addDatabase(UTDatabase db) {
    for (ImmutableList<UItem> transation : db.getTransactions()) {
      for (UItem uItem : transation) {
        int id = uItem.id();

        if (iupMap.containsKey(id)) {
          iupMap.get(id).addTPPair(currentTid, uItem.prob());
        } else {
          iupMap.put(id, new UPList(id, currentTid, uItem.prob()));
        }
      }

      currentTid++;
    }
  }

  public abstract List<UItemSet> mine();
}
