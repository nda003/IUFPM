package vn.datm.ituna;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ITUNA {
  private final int k;
  // private MinMaxPriorityQueue<UItemSet> pq;

  private int currentTid = 0;
  private double minimumSupport = 0;

  private Map<Integer, IUPList> iUPMap = new HashMap<>();
  private Map<Set<Integer>, ICUPList> iCUPMap = new HashMap<>();

  public ITUNA(int k) {
    this.k = k;
    // pq =
    //     MinMaxPriorityQueue.orderedBy(
    //             Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed())
    //         .maximumSize(k)
    //         .create();
  }

  public void addDatabase(UTDatabase db) {
    for (ArrayList<UItem> transation : db.getTransactions()) {
      for (UItem uItem : transation) {
        int id = uItem.getId();

        if (iUPMap.containsKey(id)) {
          iUPMap.get(id).addTPPair(currentTid, uItem.getProbability());
        } else {
          iUPMap.put(id, new IUPList(id, currentTid, uItem.getProbability()));
        }
      }

      currentTid++;
    }
  }

  // public void debug() {
  //   System.out.println(pq.getLast());
  //   pq.add(iCUPMap.get(ImmutableSet.of(90, 56)).getItemSet());
  //   System.out.println(pq.getLast());
  // }

  private List<UItemSet> toTopK(AbstractQueue<UItemSet> pq) {
    return pq.stream()
        .sorted(Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed())
        .toList();
  }

  public List<UItemSet> mine() {
    // if (pq.size() == 0) {
    //   for (IUPList list : iUPMap.values()) {
    //     pq.add(list.getItemSet());
    //   }
    // } else {
    //   pq =
    //       MinMaxPriorityQueue.orderedBy(
    //               Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed())
    //           .maximumSize(k)
    //           .create();

    //   // pq.clear();

    //   for (IUPList list : iUPMap.values()) {
    //     if (list.getExpectedSupport() >= minimumSupport) {
    //       pq.add(list.getItemSet());
    //     }
    //   }

    // pq.pollLast(); // DONOT REMOVE

    // pq =
    //     MinMaxPriorityQueue.orderedBy(
    //             Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed())
    //         .maximumSize(k)
    //         .create(pq);
    // }

    // MinMaxPriorityQueue<UItemSet> pq =
    //     MinMaxPriorityQueue.orderedBy(
    //             Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed())
    //         .maximumSize(k)
    //         .create();
    // TODO replace with a min-max heap
    LimitedSortedList<UItemSet> pq =
        new LimitedSortedList<>(
            k, Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed());

    // for (IUPList list : iUPMap.values()) {
    //   if (list.getExpectedSupport() >= minimumSupport) {
    //     pq.add(list.getItemSet());
    //   }
    // }
    pq.addAll(Collections2.transform(iUPMap.values(), IUPList::getItemSet));

    // UItemSet polledSet = pq.pollFirst();
    // pq.add(polledSet);
    // System.out.println(pq);
    // System.out.println(pq.getLast());

    minimumSupport = Double.max(minimumSupport, pq.getLast().getExpectedSupport());

    List<Integer> idToTraverse =
        pq.stream()
            .map((a) -> a.getIds().toArray(Integer[]::new)[0])
            .collect(ImmutableList.toImmutableList());

    for (int i = 0; i < idToTraverse.size(); i++) {
      IUPList iUPList = iUPMap.get(idToTraverse.get(i));

      if (iUPList.getExpectedSupport() >= minimumSupport) {
        List<Set<Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          IUPList jUPList = iUPMap.get(idToTraverse.get(j));

          if (iUPList.getExpectedSupport() * jUPList.getMaxSupport() >= minimumSupport) {
            Set<Integer> pattern = ImmutableSet.of(iUPList.getId(), jUPList.getId());

            if (iCUPMap.containsKey(pattern)) {
              reconstructICUPList(iCUPMap.get(pattern));

              if (iCUPMap.get(pattern).getExpectedSupport() >= minimumSupport) {
                pq.add(iCUPMap.get(pattern).getItemSet());
                minimumSupport = pq.getLast().getExpectedSupport();
                patternToTraverse.add(iCUPMap.get(pattern).getItemSet().getIds());
              }
            } else {
              ICUPList iCUPList = constructICUPList(iUPList, jUPList);

              if (iCUPList.getExpectedSupport() >= minimumSupport) {
                pq.add(iCUPList.getItemSet());
                minimumSupport = pq.getLast().getExpectedSupport();
                patternToTraverse.add(iCUPList.getItemSet().getIds());
              }
            }
          }
        }

        if (!patternToTraverse.isEmpty()) {
          mine(pq, idToTraverse, patternToTraverse, i + 1);
        }
      }
    }

    return pq.stream().toList();
  }

  private void mine(
      LimitedSortedList<UItemSet> pq,
      List<Integer> idToTraverse,
      List<Set<Integer>> patternToTraverse,
      int fromIndex) {
    Set<Set<Integer>> exploredPatterns = new HashSet<>();

    for (Set<Integer> pattern : patternToTraverse) {
      ICUPList iCUPList = iCUPMap.get(pattern);
      List<Set<Integer>> nextPatternToTraverse = new ArrayList<>();

      for (int i = fromIndex; i < idToTraverse.size(); i++) {
        if (pattern.contains(idToTraverse.get(i))) {
          continue;
        }

        Set<Integer> nextPattern =
            new ImmutableSet.Builder<Integer>().addAll(pattern).add(idToTraverse.get(i)).build();

        if (exploredPatterns.contains(nextPattern)) {
          continue;
        }

        IUPList iUPList = iUPMap.get(idToTraverse.get(i));
        exploredPatterns.add(nextPattern);

        if (iCUPList.getExpectedSupport() * iUPList.getMaxSupport() >= minimumSupport) {
          if (iCUPMap.containsKey(nextPattern)) {
            reconstructICUPList(iCUPMap.get(nextPattern));

            if (iCUPMap.get(nextPattern).getExpectedSupport() >= minimumSupport) {
              pq.add(iCUPMap.get(nextPattern).getItemSet());
              minimumSupport = pq.getLast().getExpectedSupport();
              nextPatternToTraverse.add(iCUPMap.get(nextPattern).getItemSet().getIds());
            }
          } else {
            ICUPList nextICUPList = constructICUPList(iCUPList, iUPList);

            if (nextICUPList.getExpectedSupport() >= minimumSupport) {
              pq.add(nextICUPList.getItemSet());
              minimumSupport = pq.getLast().getExpectedSupport();
              nextPatternToTraverse.add(nextICUPList.getItemSet().getIds());
            }
          }
        }
      }

      if (!nextPatternToTraverse.isEmpty()) {
        mine(pq, idToTraverse, nextPatternToTraverse, fromIndex);
      }
    }
  }

  private ICUPList constructICUPList(IUPList l1, IUPList l2) {
    ICUPList cList = new ICUPList(l1, l2);

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      double oProb = l2.getTransationProbability(pair.tid());

      if (oProb > -1) {
        cList.addTPPair(pair.tid(), pair.prob() * oProb);
      }

      cList.setLateIndex(l1.getId(), i);

      if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setLateIndex(l2.getId(), l2.size() - 1);

    iCUPMap.put(cList.getItemSet().getIds(), cList);
    return cList;
  }

  private ICUPList constructICUPList(ICUPList l1, IUPList l2) {
    ICUPList cList = new ICUPList(l1, l2);

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      double oProb = l2.getTransationProbability(pair.tid());

      if (oProb > -1) {
        cList.addTPPair(pair.tid(), pair.prob() * oProb);
      }

      cList.setLateIndex(l1.getIds(), i + 1);

      if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setLateIndex(l2.getId(), l2.size());

    iCUPMap.put(cList.getItemSet().getIds(), cList);
    return cList;
  }

  private void reconstructICUPList(ICUPList cList) {
    List<Set<Integer>> parentPattern = cList.getParentPatterns();

    if (parentPattern.get(0).size() == 1) {
      IUPList l1 = iUPMap.get(parentPattern.get(0).toArray(Integer[]::new)[0]);
      IUPList l2 = iUPMap.get(parentPattern.get(1).toArray(Integer[]::new)[0]);

      if (l1.size() > cList.getLateIndex(l1.getId())
          && l2.size() > cList.getLateIndex(l2.getId())) {
        for (int i = cList.getLateIndex(l1.getId()) + 1; i < l1.size(); i++) {
          TPPair pair = l1.getTransationAt(i);
          double oProb = l2.getTransationProbability(pair.tid());

          if (oProb > -1) {
            cList.addTPPair(pair.tid(), pair.prob() * oProb);
          }

          cList.setLateIndex(l1.getId(), i + 1);

          if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }

        cList.setLateIndex(l2.getId(), l2.size());
      }
    } else {
      ICUPList l1 = iCUPMap.get(parentPattern.get(0));
      IUPList l2 = iUPMap.get(parentPattern.get(1).toArray(Integer[]::new)[0]);

      if (l1.size() > cList.getLateIndex(l1.getIds())
          && l2.size() > cList.getLateIndex(l2.getId())) {
        for (int i = cList.getLateIndex(l1.getIds()) + 1; i < l1.size(); i++) {
          TPPair pair = l1.getTransationAt(i);
          double oProb = l2.getTransationProbability(pair.tid());

          if (oProb > -1) {
            cList.addTPPair(pair.tid(), pair.prob() * oProb);
          }

          cList.setLateIndex(l1.getIds(), i + 1);

          if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }

        cList.setLateIndex(l2.getId(), l2.size());
      }
    }
  }
}
