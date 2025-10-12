package vn.datm.ituna;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MinMaxPriorityQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ITUNA {
  private final int k;

  private int currentTid = 0;
  private double minimumSupport = 0;

  private Map<Integer, IUPList> iUPMap = new HashMap<>();
  private Map<Set<Integer>, ICUPList> iCUPMap = new HashMap<>();

  public ITUNA(int k) {
    this.k = k;
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
        List<Set<Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          IUPList jUPList = iUPMap.get(idToTraverse.get(j));

          if (iUPList.getExpectedSupport() * jUPList.getMaxSupport() >= minimumSupport) {
            Set<Integer> pattern = ImmutableSet.of(iUPList.getId(), jUPList.getId());

            if (iCUPMap.containsKey(pattern)) {
              reconstructICUPList(iCUPMap.get(pattern));

              if (iCUPMap.get(pattern).getExpectedSupport() >= minimumSupport) {
                pq.add(iCUPMap.get(pattern).getItemSet());
                patternToTraverse.add(iCUPMap.get(pattern).getItemSet().getIds());

                if (pq.size() >= k) {
                  minimumSupport = pq.peekLast().getExpectedSupport();
                }
              }
            } else {
              ICUPList iCUPList = constructICUPList(iUPList, jUPList);

              if (iCUPList.getExpectedSupport() >= minimumSupport) {
                pq.add(iCUPList.getItemSet());
                patternToTraverse.add(iCUPList.getItemSet().getIds());

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
              nextPatternToTraverse.add(iCUPMap.get(nextPattern).getItemSet().getIds());

              if (pq.size() >= k) {
                minimumSupport = pq.peekLast().getExpectedSupport();
              }
            }
          } else {
            ICUPList nextICUPList = constructICUPList(iCUPList, iUPList);

            if (nextICUPList.getExpectedSupport() >= minimumSupport) {
              pq.add(nextICUPList.getItemSet());
              nextPatternToTraverse.add(nextICUPList.getItemSet().getIds());

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

  private ICUPList constructICUPList(IUPList l1, IUPList l2) {
    ICUPList cList = new ICUPList(l1, l2);
    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      int oIndex;

      oIndex = l2.getTransationIndex(pair.tid(), l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
      }

      cList.setLateIndex(l1.getId(), i + 1);

      if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setLateIndex(l2.getId(), l2Index);

    iCUPMap.put(cList.getItemSet().getIds(), cList);
    return cList;
  }

  private ICUPList constructICUPList(ICUPList l1, IUPList l2) {
    ICUPList cList = new ICUPList(l1, l2);
    int l2Index = 0;

    for (int i = 0; i < l1.size(); i++) {
      TPPair pair = l1.getTransationAt(i);
      int oIndex;

      oIndex = l2.getTransationIndex(pair.tid(), l2Index);

      if (oIndex > -1) {
        l2Index = oIndex + 1;
        cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
      }

      cList.setLateIndex(l1.getIds(), i + 1);

      if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
        break;
      }
    }

    cList.setLateIndex(l2.getId(), l2Index);

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
        int l2Index = cList.getLateIndex(l2.getId());

        for (int i = cList.getLateIndex(l1.getId()); i < l1.size(); i++) {
          TPPair pair = l1.getTransationAt(i);
          int oIndex;

          oIndex = l2.getTransationIndex(pair.tid(), l2Index);

          if (oIndex > -1) {
            l2Index = oIndex + 1;
            cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
          }

          cList.setLateIndex(l1.getId(), i + 1);

          if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }

        cList.setLateIndex(l2.getId(), l2Index);
      }
    } else {
      ICUPList l1 = iCUPMap.get(parentPattern.get(0));
      reconstructICUPList(l1);

      IUPList l2 = iUPMap.get(parentPattern.get(1).toArray(Integer[]::new)[0]);

      if (l1.size() > cList.getLateIndex(l1.getIds())
          && l2.size() > cList.getLateIndex(l2.getId())) {
        int l2Index = cList.getLateIndex(l2.getId());

        for (int i = cList.getLateIndex(l1.getIds()); i < l1.size(); i++) {
          TPPair pair = l1.getTransationAt(i);
          int oIndex;

          oIndex = l2.getTransationIndex(pair.tid(), l2Index);

          if (oIndex > -1) {
            l2Index = oIndex + 1;
            cList.addTPPair(pair.tid(), pair.prob() * l2.getTransationAt(oIndex).prob());
          }

          cList.setLateIndex(l1.getIds(), i + 1);

          if (minimumSupport - cList.getExpectedSupport() > (l1.size() - i) * l2.getMaxSupport()) {
            break;
          }
        }

        cList.setLateIndex(l2.getId(), l2Index);
      }
    }
  }
}
