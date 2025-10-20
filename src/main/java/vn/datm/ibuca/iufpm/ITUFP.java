package vn.datm.ibuca.iufpm;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import vn.datm.ibuca.util.ICUPList;
import vn.datm.ibuca.util.TPPair;
import vn.datm.ibuca.util.UItemSet;
import vn.datm.ibuca.util.UPList;

public class ITUFP extends IUFPM {
  private Map<ImmutableIntSet, ICUPList> icupMap = new UnifiedMap<>();

  public ITUFP(int k) {
    super(k);
  }

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
        List<SimpleImmutableEntry<ImmutableIntSet, Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          int jd = idToTraverse.get(j);
          UPList jUPList = iupMap.get(idToTraverse.get(j));

          if (jUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
            ImmutableIntSet pattern = IntSets.immutable.with(id, jd);

            if (icupMap.containsKey(pattern)) {
              reconstructICUPList(icupMap.get(pattern));

              if (icupMap.get(pattern).getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, icupMap.get(pattern).getExpectedSupport()));
                patternToTraverse.add(new SimpleImmutableEntry<>(pattern, j));

                if (pq.size() >= k) {
                  minimumSupport = pq.getLast().getExpectedSupport();
                }
              }
            } else {
              ICUPList icupList = constructICUPList(id, iUPList, jd, jUPList);
              icupMap.put(pattern, icupList);

              if (icupList.getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, icupList.getExpectedSupport()));
                patternToTraverse.add(new SimpleImmutableEntry<>(pattern, j));

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

  private void mine(
      LimitedSortedItemSets pq,
      ImmutableIntList idToTraverse,
      List<SimpleImmutableEntry<ImmutableIntSet, Integer>> patternToTraverse,
      int fromIndex) {
    for (SimpleImmutableEntry<ImmutableIntSet, Integer> pattern : patternToTraverse) {
      List<SimpleImmutableEntry<ImmutableIntSet, Integer>> nextPatternToTraverse =
          new ArrayList<>();

      ICUPList iCUPList = icupMap.get(pattern.getKey());

      for (int i = pattern.getValue() + 1; i < idToTraverse.size(); i++) {
        int id = idToTraverse.get(i);
        UPList iUPList = iupMap.get(id);

        if (iCUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
          ImmutableIntSet nextPattern = pattern.getKey().newWith(idToTraverse.get(i));

          if (icupMap.containsKey(nextPattern)) {
            reconstructICUPList(icupMap.get(nextPattern));

            if (icupMap.get(nextPattern).getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, icupMap.get(nextPattern).getExpectedSupport()));
              nextPatternToTraverse.add(new SimpleImmutableEntry<>(nextPattern, i));

              if (pq.size() >= k) {
                minimumSupport = pq.getLast().getExpectedSupport();
              }
            }
          } else {
            ICUPList nextICUPList = constructICUPList(pattern.getKey(), iCUPList, id, iUPList);
            icupMap.put(nextPattern, nextICUPList);

            if (nextICUPList.getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, nextICUPList.getExpectedSupport()));
              nextPatternToTraverse.add(new SimpleImmutableEntry<>(nextPattern, i));

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

  protected ICUPList constructICUPList(int id1, UPList l1, int id2, UPList l2) {
    ICUPList cList = new ICUPList(id1, id2);
    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      int oIndex;

      oIndex = l2.getTransationIndex(pair.tid(), l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
      }

      cList.setFirstIndex(i + 1);

      if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setSecondIndex(l2Index);

    return cList;
  }

  protected ICUPList constructICUPList(ImmutableIntSet p, ICUPList l1, int id, UPList l2) {
    ICUPList cList = new ICUPList(p, id);

    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      int oIndex;

      oIndex = l2.getTransationIndex(pair.tid(), l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
      }

      cList.setFirstIndex(i + 1);

      if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setSecondIndex(l2Index);

    return cList;
  }

  protected void reconstructICUPList(ICUPList cList) {
    if (cList.getFirstParent().size() == 1) {
      UPList l1 = iupMap.get(cList.getFirstParent().intIterator().next());
      UPList l2 = iupMap.get(cList.getSecondParent());

      if (l1.size() > cList.getFirstIndex() && l2.size() > cList.getSecondIndex()) {
        int l2Index = cList.getSecondIndex();

        for (int i = cList.getFirstIndex(); i < l1.size(); i++) {
          TPPair pair = l1.getTransationAt(i);
          int oIndex;

          oIndex = l2.getTransationIndex(pair.tid(), l2Index);

          if (oIndex > -1) {
            l2Index = oIndex + 1;
            cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
          }

          cList.setFirstIndex(i + 1);

          if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
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
          TPPair pair = l1.getTransationAt(i);
          int oIndex;

          oIndex = l2.getTransationIndex(pair.tid(), l2Index);

          if (oIndex > -1) {
            l2Index = oIndex + 1;
            cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
          }

          cList.setFirstIndex(i + 1);

          if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }

        cList.setSecondIndex(l2Index);
      }
    }
  }
}
