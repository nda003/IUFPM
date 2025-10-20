package vn.datm.ibuca.iufpm;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import vn.datm.ibuca.util.ICUPList;
import vn.datm.ibuca.util.TPPair;
import vn.datm.ibuca.util.UItemSet;
import vn.datm.ibuca.util.UPList;

public class ITUFP extends IUFPM {
  protected Map<Set<Integer>, ICUPList> iCUPMap = new UnifiedMap<>();

  public ITUFP(int k) {
    super(k);
  }

  public List<UItemSet> mine() {
    LimitedSortedItemSets pq =
        new LimitedSortedItemSets(
            k, Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed());

    pq.addAll(
        Collections2.transform(
            Collections2.filter(
                iUPMap.entrySet(), (x) -> x.getValue().getExpectedSupport() >= minimumSupport),
            (x) -> new UItemSet(x.getKey(), x.getValue().getExpectedSupport())));

    if (pq.size() >= k) {
      minimumSupport = Double.max(minimumSupport, pq.getLast().getExpectedSupport());
    }

    List<Integer> idToTraverse =
        new ImmutableList.Builder<Integer>()
            .addAll(
                Collections2.transform(pq.toList(), (a) -> a.getIds().toArray(Integer[]::new)[0]))
            .build();

    for (int i = 0; i < idToTraverse.size(); i++) {
      int id = idToTraverse.get(i);
      UPList iUPList = iUPMap.get(id);

      if (iUPList.getExpectedSupport() >= minimumSupport) {
        List<SimpleImmutableEntry<Set<Integer>, Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          int jd = idToTraverse.get(j);
          UPList jUPList = iUPMap.get(idToTraverse.get(j));

          if (jUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
            Set<Integer> pattern = ImmutableSet.of(id, jd);

            if (iCUPMap.containsKey(pattern)) {
              reconstructICUPList(iCUPMap.get(pattern));

              if (iCUPMap.get(pattern).getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, iCUPMap.get(pattern).getExpectedSupport()));
                patternToTraverse.add(new SimpleImmutableEntry<>(pattern, j));

                if (pq.size() >= k) {
                  minimumSupport = pq.getLast().getExpectedSupport();
                }
              }
            } else {
              ICUPList iCUPList = constructICUPList(id, iUPList, jd, jUPList);

              if (iCUPList.getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, iCUPList.getExpectedSupport()));
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
      List<Integer> idToTraverse,
      List<SimpleImmutableEntry<Set<Integer>, Integer>> patternToTraverse,
      int fromIndex) {
    for (SimpleImmutableEntry<Set<Integer>, Integer> pattern : patternToTraverse) {
      List<SimpleImmutableEntry<Set<Integer>, Integer>> nextPatternToTraverse = new ArrayList<>();

      ICUPList iCUPList = iCUPMap.get(pattern.getKey());

      for (int i = pattern.getValue() + 1; i < idToTraverse.size(); i++) {
        int id = idToTraverse.get(i);
        UPList iUPList = iUPMap.get(id);

        if (iCUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
          Set<Integer> nextPattern =
              new ImmutableSet.Builder<Integer>()
                  .addAll(pattern.getKey())
                  .add(idToTraverse.get(i))
                  .build();

          if (iCUPMap.containsKey(nextPattern)) {
            reconstructICUPList(iCUPMap.get(nextPattern));

            if (iCUPMap.get(nextPattern).getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, iCUPMap.get(nextPattern).getExpectedSupport()));
              nextPatternToTraverse.add(new SimpleImmutableEntry<>(nextPattern, i));

              if (pq.size() >= k) {
                minimumSupport = pq.getLast().getExpectedSupport();
              }
            }
          } else {
            ICUPList nextICUPList = constructICUPList(pattern.getKey(), iCUPList, id, iUPList);

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

    iCUPMap.put(ImmutableSet.of(id1, id2), cList);
    return cList;
  }

  protected ICUPList constructICUPList(Set<Integer> p, ICUPList l1, int id, UPList l2) {
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

    iCUPMap.put(new ImmutableSet.Builder<Integer>().addAll(p).add(id).build(), cList);
    return cList;
  }

  protected void reconstructICUPList(ICUPList cList) {
    if (cList.getFirstParent().size() == 1) {
      UPList l1 = iUPMap.get(cList.getFirstParent().iterator().next());
      UPList l2 = iUPMap.get(cList.getSecondParent());

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
      ICUPList l1 = iCUPMap.get(cList.getFirstParent());
      reconstructICUPList(l1);

      UPList l2 = iUPMap.get(cList.getSecondParent());

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
