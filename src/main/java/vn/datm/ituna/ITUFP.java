package vn.datm.ituna;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ITUFP extends IUFPM {
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

  public ITUFP(int k) {
    this.k = k;
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
}
