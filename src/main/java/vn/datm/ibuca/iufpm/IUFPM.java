package vn.datm.ibuca.iufpm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import vn.datm.ibuca.db.UItem;
import vn.datm.ibuca.db.UTDatabase;
import vn.datm.ibuca.util.UItemSet;
import vn.datm.ibuca.util.UPList;

public abstract class IUFPM {
  protected class LimitedSortedItemSets {
    private List<UItemSet> sets;
    private final int maximumSize;
    private Comparator<UItemSet> comparator;

    public LimitedSortedItemSets(int maximumSize, Comparator<UItemSet> comparator) {
      this.sets = new ArrayList<>(maximumSize + 1);
      this.comparator = comparator;
      this.maximumSize = maximumSize;
    }

    public void addAll(Collection<UItemSet> c) {
      sets.addAll(c);
      Collections.sort(sets, comparator);

      if (sets.size() > maximumSize) {
        sets.subList(maximumSize, sets.size()).clear();
      }
    }

    public UItemSet getLast() {
      return sets.get(sets.size() - 1);
    }

    public void add(UItemSet set) {
      sets.add(set);
      Collections.sort(sets, comparator);

      if (sets.size() > maximumSize) {
        sets.remove(sets.size() - 1);
      }
    }

    public List<UItemSet> toList() {
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

  protected Map<Integer, UPList> iUPMap = new UnifiedMap<>();

  protected IUFPM(int k) {
    this.k = k;
  }

  public void addDatabase(UTDatabase db) {
    for (ArrayList<UItem> transation : db.getTransactions()) {
      for (UItem uItem : transation) {
        int id = uItem.getId();

        if (iUPMap.containsKey(id)) {
          iUPMap.get(id).addTPPair(currentTid, uItem.getProbability());
        } else {
          iUPMap.put(id, new UPList(id, currentTid, uItem.getProbability()));
        }
      }

      currentTid++;
    }
  }

  public abstract List<UItemSet> mine();
}
