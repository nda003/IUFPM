package vn.datm.ituna;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MinMaxPriorityQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ITUNA {
  private final int k;
  private MinMaxPriorityQueue<UItemSet> pq;

  private final Comparator<UItemSet> comparator =
      (UItemSet a, UItemSet b) -> -Double.compare(a.getExpectedSupport(), b.getExpectedSupport());
  private int currentTid = 0;
  private double minimumSupport = 0;

  private Map<Integer, IUPList> iUPMap = new HashMap<>();
  private Map<Set<Integer>, ICUPList> iCUPMap = new HashMap<>();

  public ITUNA(int k) {
    this.k = k;
    pq = MinMaxPriorityQueue.orderedBy(comparator).maximumSize(k).create();
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

  public UItemSet[] getTopK() {
    UItemSet[] buffer = pq.toArray(UItemSet[]::new);
    Arrays.sort(buffer, comparator);

    return buffer;
  }

  public void mine() {
    for (IUPList list : iUPMap.values()) {
      pq.offer(list.getItemSet());
    }

    if (pq.size() >= k) {
      minimumSupport = pq.peekLast().getExpectedSupport();
    }

    List<Integer> idToTraverse =
        pq.stream()
            .filter((a) -> a.size() == 1)
            .sorted(comparator)
            .map((a) -> a.getIds().toArray(Integer[]::new)[0])
            .collect(ImmutableList.toImmutableList());

    for (int i = 0; i < idToTraverse.size() - 1; i++) {
      IUPList iUPList = iUPMap.get(idToTraverse.get(i));

      if (iUPList.getExpectedSupport() > minimumSupport) {
        List<Set<Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          IUPList jUPList = iUPMap.get(idToTraverse.get(j));

          if (iUPList.getExpectedSupport() * jUPList.getMaxSupport() > minimumSupport) {
            Set<Integer> pattern = ImmutableSet.of(iUPList.getId(), jUPList.getId());

            if (iCUPMap.containsKey(pattern)) {
              reconstructICUPList(iCUPMap.get(pattern));

              if (iCUPMap.get(pattern).getExpectedSupport() > minimumSupport) {
                pq.offer(iCUPMap.get(pattern).getItemSet());
                minimumSupport = pq.peekLast().getExpectedSupport();
                patternToTraverse.add(iCUPMap.get(pattern).getItemSet().getIds());
              }
            } else {
              ICUPList iCUPList = constructICUPList(iUPList, jUPList);

              if (iCUPList.getExpectedSupport() > minimumSupport) {
                pq.offer(iCUPList.getItemSet());
                minimumSupport = pq.peekLast().getExpectedSupport();
                patternToTraverse.add(iCUPList.getItemSet().getIds());
              }
            }
          }
        }

        if (!patternToTraverse.isEmpty()) {
          mine(idToTraverse, patternToTraverse);
        }
      }
    }
  }

  private void mine(List<Integer> idToTraverse, List<Set<Integer>> patternToTraverse) {
    for (Set<Integer> pattern : patternToTraverse) {
      ICUPList iCUPList = iCUPMap.get(pattern);
      List<Set<Integer>> nextPatternToTraverse = new ArrayList<>();

      for (Integer id : idToTraverse) {
        if (!pattern.contains(id)) {
          IUPList iUPList = iUPMap.get(id);

          if (iCUPList.getExpectedSupport() * iUPList.getMaxSupport() > minimumSupport) {
            Set<Integer> nextPattern =
                new ImmutableSet.Builder<Integer>().addAll(pattern).add(id).build();

            if (iCUPMap.containsKey(nextPattern)) {
              reconstructICUPList(iCUPMap.get(pattern));

              if (iCUPMap.get(nextPattern).getExpectedSupport() > minimumSupport) {
                pq.offer(iCUPMap.get(nextPattern).getItemSet());
                minimumSupport = pq.peekLast().getExpectedSupport();
                nextPatternToTraverse.add(iCUPMap.get(nextPattern).getItemSet().getIds());
              }
            } else {
              ICUPList nextICUPList = constructICUPList(iCUPList, iUPList);

              if (nextICUPList.getExpectedSupport() > minimumSupport) {
                pq.offer(nextICUPList.getItemSet());
                minimumSupport = pq.peekLast().getExpectedSupport();
                nextPatternToTraverse.add(nextICUPList.getItemSet().getIds());
              }
            }
          }
        }
      }

      if (!nextPatternToTraverse.isEmpty()) {
        mine(idToTraverse, nextPatternToTraverse);
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

      cList.setLateIndex(l1.getIds(), i);

      if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setLateIndex(l2.getId(), l2.size() - 1);

    iCUPMap.put(cList.getItemSet().getIds(), cList);
    return cList;
  }

  private void reconstructICUPList(ICUPList cList) {
    List<Set<Integer>> parentPattern = cList.getParentPatterns();

    if (parentPattern.get(0).size() == 1) {
      IUPList l1 = iUPMap.get(parentPattern.get(0).toArray(Integer[]::new)[0]);
      IUPList l2 = iUPMap.get(parentPattern.get(1).toArray(Integer[]::new)[0]);

      if (l1.size() - 1 > cList.getLateIndex(l1.getId())
          && l2.size() - 1 > cList.getLateIndex(l2.getId())) {
        for (int i = cList.getLateIndex(l1.getId()); i < l1.size(); i++) {
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
      }
    } else {
      ICUPList l1 = iCUPMap.get(parentPattern.get(0));
      IUPList l2 = iUPMap.get(parentPattern.get(1).toArray(Integer[]::new)[0]);

      if (l1.size() - 1 > cList.getLateIndex(l1.getIds())
          && l2.size() - 1 > cList.getLateIndex(l2.getId())) {
        for (int i = cList.getLateIndex(l1.getIds()); i < l1.size(); i++) {
          TPPair pair = l1.getTransationAt(i);
          double oProb = l2.getTransationProbability(pair.tid());

          if (oProb > -1) {
            cList.addTPPair(pair.tid(), pair.prob() * oProb);
          }

          cList.setLateIndex(l1.getIds(), i);

          if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }
      }
    }
  }
}
