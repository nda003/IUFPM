package vn.datm.ituna;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MinMaxPriorityQueue;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ITUNA extends IUFPM {
  private final int k;

  public ITUNA(int k) {
    this.k = k;
  }

  public List<UItemSet> mine() {
    MinMaxPriorityQueue<UItemSet> pq =
        MinMaxPriorityQueue.orderedBy(
                Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed())
            .maximumSize(k)
            .create();

    pq.addAll(
        Collections2.transform(
            Collections2.filter(iUPMap.values(), (x) -> x.getExpectedSupport() >= minimumSupport),
            IUPList::getItemSet));

    if (pq.size() >= k) {
      minimumSupport = Double.max(minimumSupport, pq.peekLast().getExpectedSupport());
    }

    List<Integer> idToTraverse =
        pq.stream()
            .sorted(pq.comparator())
            .map((a) -> a.getIds().toArray(Integer[]::new)[0])
            .collect(ImmutableList.toImmutableList());

    for (int i = 0; i < idToTraverse.size(); i++) {
      IUPList iUPList = iUPMap.get(idToTraverse.get(i));

      if (iUPList.getExpectedSupport() >= minimumSupport) {
        List<SimpleImmutableEntry<Set<Integer>, Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          IUPList jUPList = iUPMap.get(idToTraverse.get(j));

          if (jUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
            Set<Integer> pattern = ImmutableSet.of(iUPList.getId(), jUPList.getId());

            if (iCUPMap.containsKey(pattern)) {
              reconstructICUPList(iCUPMap.get(pattern));

              if (iCUPMap.get(pattern).getExpectedSupport() >= minimumSupport) {
                pq.add(iCUPMap.get(pattern).getItemSet());
                patternToTraverse.add(
                    new SimpleImmutableEntry<>(iCUPMap.get(pattern).getItemSet().getIds(), j));

                if (pq.size() >= k) {
                  minimumSupport = pq.peekLast().getExpectedSupport();
                }
              }
            } else {
              ICUPList iCUPList = constructICUPList(iUPList, jUPList);

              if (iCUPList.getExpectedSupport() >= minimumSupport) {
                pq.add(iCUPList.getItemSet());
                patternToTraverse.add(
                    new SimpleImmutableEntry<>(iCUPList.getItemSet().getIds(), j));

                if (pq.size() >= k) {
                  minimumSupport = pq.peekLast().getExpectedSupport();
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

    return pq.stream().sorted(pq.comparator()).toList();
  }

  private void mine(
      MinMaxPriorityQueue<UItemSet> pq,
      List<Integer> idToTraverse,
      List<SimpleImmutableEntry<Set<Integer>, Integer>> patternToTraverse,
      int fromIndex) {
    for (SimpleImmutableEntry<Set<Integer>, Integer> pattern : patternToTraverse) {
      ICUPList iCUPList = iCUPMap.get(pattern.getKey());
      List<SimpleImmutableEntry<Set<Integer>, Integer>> nextPatternToTraverse = new ArrayList<>();

      for (int i = pattern.getValue() + 1; i < idToTraverse.size(); i++) {
        Set<Integer> nextPattern =
            new ImmutableSet.Builder<Integer>()
                .addAll(pattern.getKey())
                .add(idToTraverse.get(i))
                .build();

        IUPList iUPList = iUPMap.get(idToTraverse.get(i));

        if (iCUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
          if (iCUPMap.containsKey(nextPattern)) {
            reconstructICUPList(iCUPMap.get(nextPattern));

            if (iCUPMap.get(nextPattern).getExpectedSupport() >= minimumSupport) {
              pq.add(iCUPMap.get(nextPattern).getItemSet());
              nextPatternToTraverse.add(
                  new SimpleImmutableEntry<>(iCUPMap.get(nextPattern).getItemSet().getIds(), i));

              if (pq.size() >= k) {
                minimumSupport = pq.peekLast().getExpectedSupport();
              }
            }
          } else {
            ICUPList nextICUPList = constructICUPList(iCUPList, iUPList);

            if (nextICUPList.getExpectedSupport() >= minimumSupport) {
              pq.add(nextICUPList.getItemSet());
              nextPatternToTraverse.add(
                  new SimpleImmutableEntry<>(nextICUPList.getItemSet().getIds(), i));

              if (pq.size() >= k) {
                minimumSupport = pq.peekLast().getExpectedSupport();
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
}
