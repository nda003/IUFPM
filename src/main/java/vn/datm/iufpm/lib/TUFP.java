package vn.datm.iufpm.lib;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import vn.datm.iufpm.db.UItem;
import vn.datm.iufpm.db.UTDatabase;
import vn.datm.iufpm.util.CUPList;
import vn.datm.iufpm.util.TPPair;
import vn.datm.iufpm.util.UItemSet;
import vn.datm.iufpm.util.UPList;

/**
 * Top-K uncertain frequent pattern mining model from <a
 * href="https://doi.org/10.1007/s10489-019-01622-1">Le et al.</a>. This model have no incremental
 * capabilities and will construct new {@link CUPList} upon each {@link #mine()} call.
 *
 * @see Le, T., Vo, B., Huynh, VN. et al. Mining top-k frequent patterns from uncertain databases.
 *     Appl Intell 50, 1487–1497 (2020). https://doi.org/10.1007/s10489-019-01622-1
 */
public class TUFP extends IUFPM {
  /** Maps the transaction's id to its associated {@link UPList}. */
  protected MutableIntObjectMap<UPList> iupMap = new IntObjectHashMap<>();

  /**
   * Initialize this model with a top-K value.
   *
   * @param k The top-K value.
   */
  public TUFP(int k) {
    super(k);
  }

  @Override
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

  @Override
  public List<UItemSet> mine() {
    LimitedSortedItemSets pq =
        new LimitedSortedItemSets(
            k, Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed());

    pq.addAll(
        iupMap
            .keyValuesView()
            .collectIf(
                p -> p.getTwo().getExpectedSupport() >= minimumSupport,
                p -> new UItemSet(p.getOne(), p.getTwo().getExpectedSupport())));

    if (pq.size() >= k) {
      minimumSupport = Double.max(minimumSupport, pq.getLast().getExpectedSupport());
    }

    ImmutableIntList idToTraverse =
        IntLists.immutable.withAll(pq.toList().collect(x -> x.getIds().intIterator().next()));

    for (int i = 0; i < idToTraverse.size(); i++) {
      int id = idToTraverse.get(i);
      UPList iUPList = iupMap.get(id);

      if (iUPList.getExpectedSupport() >= minimumSupport) {
        List<SimpleImmutableEntry<CUPList, Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          int jd = idToTraverse.get(j);
          UPList jUPList = iupMap.get(jd);

          if (jUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
            CUPList cupList = constructCUPList(id, iUPList, jd, jUPList);

            if (cupList.getExpectedSupport() >= minimumSupport) {
              pq.add(cupList.getItemSet());
              patternToTraverse.add(new SimpleImmutableEntry<>(cupList, j));

              if (pq.size() >= k) {
                minimumSupport = pq.getLast().getExpectedSupport();
              }
            }
          }
        }

        if (!patternToTraverse.isEmpty()) {
          mine(pq, idToTraverse, patternToTraverse, i + 1);
        }
      }
    }

    return pq.toList();
  }

  private void mine(
      LimitedSortedItemSets pq,
      ImmutableIntList idToTraverse,
      List<SimpleImmutableEntry<CUPList, Integer>> patternToTraverse,
      int fromIndex) {
    for (SimpleImmutableEntry<CUPList, Integer> pattern : patternToTraverse) {
      List<SimpleImmutableEntry<CUPList, Integer>> nextPatternToTraverse = new ArrayList<>();

      for (int i = pattern.getValue() + 1; i < idToTraverse.size(); i++) {
        int id = idToTraverse.get(i);
        UPList iUPList = iupMap.get(id);

        if (pattern.getKey().getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
          CUPList nextCUPList = constructCUPList(pattern.getKey(), id, iUPList);

          if (nextCUPList.getExpectedSupport() >= minimumSupport) {
            pq.add(nextCUPList.getItemSet());
            nextPatternToTraverse.add(new SimpleImmutableEntry<>(nextCUPList, i));

            if (pq.size() >= k) {
              minimumSupport = pq.getLast().getExpectedSupport();
            }
          }
        }
      }

      if (!nextPatternToTraverse.isEmpty()) {
        mine(pq, idToTraverse, nextPatternToTraverse, fromIndex);
      }
    }
  }

  /**
   * Constructs a new {@link CUPList}.
   *
   * @param id1 The transaction id of the first parent.
   * @param l1 The first parent.
   * @param id2 The transaction id of the second parent.
   * @param l2 The second parent.
   * @return The newly constructed {@link CUPList}
   */
  private CUPList constructCUPList(int id1, UPList l1, int id2, UPList l2) {
    CUPList cList = new CUPList(id1, id2);
    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      int oIndex;

      oIndex = l2.getTransationIndex(pair.tid(), l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
      }

      if (minimumSupport > cList.getExpectedSupport()
          && minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    return cList;
  }

  /**
   * Constructs a new {@link CUPList}.
   *
   * @param l1 The first parent.
   * @param id The transaction id of the second parent.
   * @param l2 The second parent.
   * @return The newly constructed {@link CUPList}
   */
  private CUPList constructCUPList(CUPList l1, int id, UPList l2) {
    CUPList cList = new CUPList(l1.getIds(), id);
    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      int oIndex;

      oIndex = l2.getTransationIndex(pair.tid(), l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
      }

      if (minimumSupport > cList.getExpectedSupport()
          && minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    return cList;
  }
}
