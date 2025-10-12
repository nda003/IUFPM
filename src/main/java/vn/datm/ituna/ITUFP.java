package vn.datm.ituna;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ITUFP {
  private class LimitedSortedItemSets {
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

  private final int k;

  private int currentTid = 0;
  private double minimumSupport = 0;

  private Map<Integer, IUPList> iUPMap = new HashMap<>();
  private Map<Set<Integer>, ICUPList> iCUPMap = new HashMap<>();

  public ITUFP(int k) {
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
    LimitedSortedItemSets pq =
        new LimitedSortedItemSets(
            k, Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed());

    pq.addAll(
        Collections2.transform(
            Collections2.filter(iUPMap.values(), (x) -> x.getExpectedSupport() >= minimumSupport),
            IUPList::getItemSet));

    if (pq.size() >= k) {
      minimumSupport = Double.max(minimumSupport, pq.getLast().getExpectedSupport());
    }

    List<Integer> idToTraverse =
        new ImmutableList.Builder<Integer>()
            .addAll(
                Collections2.transform(pq.toList(), (a) -> a.getIds().toArray(Integer[]::new)[0]))
            .build();

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
                  minimumSupport = pq.getLast().getExpectedSupport();
                }
              }
            } else {
              ICUPList iCUPList = constructICUPList(iUPList, jUPList);

              if (iCUPList.getExpectedSupport() >= minimumSupport) {
                pq.add(iCUPList.getItemSet());
                patternToTraverse.add(iCUPList.getItemSet().getIds());

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
                minimumSupport = pq.getLast().getExpectedSupport();
              }
            }
          } else {
            ICUPList nextICUPList = constructICUPList(iCUPList, iUPList);

            if (nextICUPList.getExpectedSupport() >= minimumSupport) {
              pq.add(nextICUPList.getItemSet());
              nextPatternToTraverse.add(nextICUPList.getItemSet().getIds());

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
      reconstructICUPList(l1);

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
