package vn.datm.iufpm.lib;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import vn.datm.iufpm.util.ICUPList;
import vn.datm.iufpm.util.UItemSet;
import vn.datm.iufpm.util.UPList;

/**
 * An interactive top-K uncertain frequent pattern mining model modified for the incremental mining.
 * This model reconstruct already mined {@link ICUPList} by only joining its parents from the last
 * indexes that were mined in the previous {@link #mine()} call.
 *
 * @see Razieh Davashi, ITUFP: A fast method for interactive mining of Top-K frequent patterns from
 *     uncertain data. Expert Systems with Applications, Volume 214, 2023, 119156, ISSN 0957-4174.
 *     https://doi.org/10.1016/j.eswa.2022.119156.
 */
public class ITUFP extends TUFP {

  /** Maps an itemset to its associated {@link ICUPList}. */
  private Map<ImmutableIntSet, ICUPList> icupMap = new HashMap<>();

  /**
   * Initialize this model with a top-K value.
   *
   * @param k The top-K value.
   */
  public ITUFP(int k) {
    super(k);
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
        List<Pair<ImmutableIntSet, Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          int jd = idToTraverse.get(j);
          UPList jUPList = iupMap.get(idToTraverse.get(j));

          if (jUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
            ImmutableIntSet pattern = IntSets.immutable.with(id, jd);

            if (icupMap.containsKey(pattern)) {
              reconstructICUPList(icupMap.get(pattern));

              if (icupMap.get(pattern).getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, icupMap.get(pattern).getExpectedSupport()));
                patternToTraverse.add(Tuples.pair(pattern, j));

                if (pq.size() >= k) {
                  minimumSupport = pq.getLast().getExpectedSupport();
                }
              }
            } else {
              ICUPList icupList = constructICUPList(id, iUPList, jd, jUPList);
              icupMap.put(pattern, icupList);

              if (icupList.getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, icupList.getExpectedSupport()));
                patternToTraverse.add(Tuples.pair(pattern, j));

                if (pq.size() >= k) {
                  minimumSupport = pq.getLast().getExpectedSupport();
                }
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

  /**
   * Recursively mines for frequent patterns.
   *
   * @param pq The priority queue of frequent patterns.
   * @param idToTraverse The list of transaction ids to traverse.
   * @param patternToTraverse The list of patterns to traverse.
   * @param fromIndex The index to start traversing from.
   */
  private void mine(
      LimitedSortedItemSets pq,
      ImmutableIntList idToTraverse,
      List<Pair<ImmutableIntSet, Integer>> patternToTraverse,
      int fromIndex) {
    for (Pair<ImmutableIntSet, Integer> pattern : patternToTraverse) {
      List<Pair<ImmutableIntSet, Integer>> nextPatternToTraverse = new FastList<>();

      ICUPList iCUPList = icupMap.get(pattern.getOne());

      for (int i = pattern.getTwo() + 1; i < idToTraverse.size(); i++) {
        int id = idToTraverse.get(i);
        UPList iUPList = iupMap.get(id);

        if (iCUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
          ImmutableIntSet nextPattern = pattern.getOne().newWith(idToTraverse.get(i));

          if (icupMap.containsKey(nextPattern)) {
            reconstructICUPList(icupMap.get(nextPattern));

            if (icupMap.get(nextPattern).getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, icupMap.get(nextPattern).getExpectedSupport()));
              nextPatternToTraverse.add(Tuples.pair(nextPattern, i));

              if (pq.size() >= k) {
                minimumSupport = pq.getLast().getExpectedSupport();
              }
            }
          } else {
            ICUPList nextICUPList = constructICUPList(pattern.getOne(), iCUPList, id, iUPList);
            icupMap.put(nextPattern, nextICUPList);

            if (nextICUPList.getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, nextICUPList.getExpectedSupport()));
              nextPatternToTraverse.add(Tuples.pair(nextPattern, i));

              if (pq.size() >= k) {
                minimumSupport = pq.getLast().getExpectedSupport();
              }
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
   * Constructs a new {@link ICUPList}.
   *
   * @param id1 The transaction id of the first parent.
   * @param l1 The first parent.
   * @param id2 The transaction id of the second parent.
   * @param l2 The second parent.
   * @return The newly constructed {@link ICUPList}.
   */
  private ICUPList constructICUPList(int id1, UPList l1, int id2, UPList l2) {
    ICUPList cList = new ICUPList(id1, id2);
    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      int tid = l1.getTidAt(i);
      int oIndex = l2.search(tid, l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTransaction(tid, l1.getProbAt(i) * l2.getProbAt(oIndex));
      }

      cList.setFirstIndex(i + 1);

      if (minimumSupport > cList.getExpectedSupport()
          && minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setSecondIndex(l2Index);

    return cList;
  }

  /**
   * Constructs a new {@link ICUPList}.
   *
   * @param p The pattern of the first parent.
   * @param l1 The first parent.
   * @param id The transaction id of the second parent.
   * @param l2 The second parent.
   * @return The newly constructed {@link ICUPList}.
   */
  private ICUPList constructICUPList(ImmutableIntSet p, ICUPList l1, int id, UPList l2) {
    ICUPList cList = new ICUPList(p, id);

    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      int tid = l1.getTidAt(i);
      int oIndex = l2.search(tid, l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTransaction(tid, l1.getProbAt(i) * l2.getProbAt(oIndex));
      }

      cList.setFirstIndex(i + 1);

      if (minimumSupport > cList.getExpectedSupport()
          && minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setSecondIndex(l2Index);

    return cList;
  }

  /**
   * Reconstruct the specified {@link ICUPList}.
   *
   * @param cList The {@link ICUPList} to be reconstructed.
   */
  private void reconstructICUPList(ICUPList cList) {
    if (cList.getFirstParent().size() == 1) {
      UPList l1 = iupMap.get(cList.getFirstParent().intIterator().next());
      UPList l2 = iupMap.get(cList.getSecondParent());

      if (l1.size() > cList.getFirstIndex() && l2.size() > cList.getSecondIndex()) {
        int l2Index = cList.getSecondIndex();

        for (int i = cList.getFirstIndex(); i < l1.size(); i++) {
          int tid = l1.getTidAt(i);
          int oIndex = l2.search(tid, l2Index);

          if (oIndex > -1) {
            l2Index = oIndex + 1;
            cList.addTransaction(tid, l1.getProbAt(i) * l2.getProbAt(oIndex));
          }

          cList.setFirstIndex(i + 1);

          if (minimumSupport > cList.getExpectedSupport()
              && minimumSupport - cList.getExpectedSupport()
                  > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }

        cList.setSecondIndex(l2Index);
      }
    } else {
      ICUPList l1 = icupMap.get(cList.getFirstParent());
      reconstructICUPList(l1);

      UPList l2 = iupMap.get(cList.getSecondParent());

      if (l1.size() > cList.getFirstIndex() && l2.size() > cList.getSecondIndex()) {
        int l2Index = cList.getSecondIndex();

        for (int i = cList.getFirstIndex(); i < l1.size(); i++) {
          int tid = l1.getTidAt(i);
          int oIndex = l2.search(tid, l2Index);

          if (oIndex > -1) {
            l2Index = oIndex + 1;
            cList.addTransaction(tid, l1.getProbAt(i) * l2.getProbAt(oIndex));
          }

          cList.setFirstIndex(i + 1);

          if (minimumSupport > cList.getExpectedSupport()
              && minimumSupport - cList.getExpectedSupport()
                  > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }

        cList.setSecondIndex(l2Index);
      }
    }
  }
}
