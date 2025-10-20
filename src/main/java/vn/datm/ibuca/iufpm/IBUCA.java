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
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import vn.datm.ibuca.db.UItem;
import vn.datm.ibuca.db.UTDatabase;
import vn.datm.ibuca.util.SCUP;
import vn.datm.ibuca.util.SUP;
import vn.datm.ibuca.util.TPPair;
import vn.datm.ibuca.util.UItemSet;

public class IBUCA extends IUFPM {
  private int increment = 0;
  private List<TPPair> pairs = new FastList<>();
  private Map<Integer, SUP> supMap = new UnifiedMap<>();
  private Map<Set<Integer>, SCUP> scupMap = new UnifiedMap<>();

  public IBUCA(int k) {
    super(k);
  }

  @Override
  public void addDatabase(UTDatabase db) {
    Map<Integer, List<TPPair>> pairMap = new UnifiedMap<>();

    for (ArrayList<UItem> transation : db.getTransactions()) {
      for (UItem uItem : transation) {
        int id = uItem.getId();

        if (supMap.containsKey(id)) {
          supMap.get(id).accept(uItem.getProbability());
        } else {
          supMap.put(id, new SUP(uItem.getProbability()));
        }

        if (!pairMap.containsKey(id)) {
          pairMap.put(id, new FastList<>());
        }

        pairMap.get(id).add(new TPPair(currentTid, uItem.getProbability()));
      }

      currentTid++;
    }

    for (Map.Entry<Integer, List<TPPair>> entry : pairMap.entrySet()) {
      int startPos = pairs.isEmpty() ? 0 : pairs.size();
      pairs.addAll(entry.getValue());
      supMap.get(entry.getKey()).addPos(increment, startPos, pairs.size() - 1);
    }

    increment++;
  }

  @Override
  public List<UItemSet> mine() {
    LimitedSortedItemSets pq =
        new LimitedSortedItemSets(
            k, Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed());

    pq.addAll(
        Collections2.transform(
            Collections2.filter(
                supMap.entrySet(), (x) -> x.getValue().getExpectedSupport() >= minimumSupport),
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
      SUP isup = supMap.get(idToTraverse.get(i));

      if (isup.getExpectedSupport() >= minimumSupport) {
        List<SimpleImmutableEntry<Set<Integer>, Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          SUP jsup = supMap.get(idToTraverse.get(j));

          if (jsup.getExpectedSupport() * isup.getMaxSupport() >= minimumSupport) {
            Set<Integer> pattern = ImmutableSet.of(idToTraverse.get(i), idToTraverse.get(j));

            if (scupMap.containsKey(pattern)) {
              reconstructSCUP(scupMap.get(pattern));

              if (scupMap.get(pattern).getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, scupMap.get(pattern).getExpectedSupport()));
                patternToTraverse.add(new SimpleImmutableEntry<>(pattern, j));

                if (pq.size() >= k) {
                  minimumSupport = pq.getLast().getExpectedSupport();
                }
              }
            } else {
              SCUP scup = constructSCUP(idToTraverse.get(i), isup, idToTraverse.get(j), jsup);
              scupMap.put(pattern, scup);

              if (scup.getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, scup.getExpectedSupport()));
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

      for (int i = pattern.getValue() + 1; i < idToTraverse.size(); i++) {
        Set<Integer> nextPattern =
            new ImmutableSet.Builder<Integer>()
                .addAll(pattern.getKey())
                .add(idToTraverse.get(i))
                .build();

        SUP sup = supMap.get(idToTraverse.get(i));

        if (scupMap.get(pattern.getKey()).getExpectedSupport() * sup.getMaxSupport()
            >= minimumSupport) {
          if (scupMap.containsKey(nextPattern)) {
            reconstructSCUP(scupMap.get(nextPattern));

            if (scupMap.get(nextPattern).getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, scupMap.get(nextPattern).getExpectedSupport()));
              nextPatternToTraverse.add(new SimpleImmutableEntry<>(nextPattern, i));

              if (pq.size() >= k) {
                minimumSupport = pq.getLast().getExpectedSupport();
              }
            }
          } else {
            SCUP nextSCUP =
                constructSCUP(
                    pattern.getKey(), scupMap.get(pattern.getKey()), idToTraverse.get(i), sup);
            scupMap.put(nextPattern, nextSCUP);

            if (nextSCUP.getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, nextSCUP.getExpectedSupport()));
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

  private SCUP constructSCUP(int id1, SUP s1, int id2, SUP s2) {
    SCUP scup = new SCUP(id1, id2);
    int segmentSize = 0;
    List<TPPair> bufferedPairs = new FastList<>();

    for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().entrySet()) {
      int inc = entry.getKey();
      int[] s1Segment = entry.getValue();

      if (!s2.containIncrement(inc)) {
        segmentSize += s1Segment[1] - s1Segment[0] + 1;
        continue;
      }

      int scupSegmentStart = bufferedPairs.size();

      int[] s2Segment = s2.getSegmentAt(inc);

      int s2Index = s2Segment[0];

      boolean ppf = false;

      for (int j = s1Segment[0]; j <= s1Segment[1]; j++) {
        TPPair pair = pairs.get(j);
        int oIndex = search(pair.tid(), s2Index, s2Segment[1]);

        if (oIndex > -1) {
          s2Index = oIndex + 1;
          double prob = pair.prob() * pairs.get(oIndex).prob();
          bufferedPairs.add(new TPPair(pair.tid(), prob));
          scup.accept(prob);
        }

        scup.setFirstIndex(j + 1);

        if (minimumSupport - scup.getExpectedSupport()
            > (s1.size() - (j - s1Segment[0] + segmentSize)) * s2.getMaxSupport()) {
          ppf = true;
          break;
        }
      }

      scup.addPos(inc, pairs.size() + scupSegmentStart, pairs.size() + bufferedPairs.size() - 1);

      scup.setSecondIndex(s2Index);

      if (ppf) {
        break;
      }

      segmentSize += s1Segment[1] - s1Segment[0] + 1;
    }

    pairs.addAll(bufferedPairs);

    return scup;
  }

  private SCUP constructSCUP(Set<Integer> p, SCUP s1, int id, SUP s2) {
    SCUP scup = new SCUP(p, id);
    int segmentSize = 0;
    int s2Index = -1;
    List<TPPair> bufferedPairs = new FastList<>();

    for (int[] inSeg : s1.getIncrementSegments()) {
      if (!s2.containIncrement(inSeg[0])) {
        segmentSize += inSeg[2] - inSeg[1] + 1;
        continue;
      }

      int scupSegmentStart = bufferedPairs.size();

      int[] s2Segment = s2.getSegmentAt(inSeg[0]);
      s2Index = Math.max(s2Index, s2Segment[0]);
      int s2End = s2Segment[1];

      boolean ppf = false;

      for (int j = inSeg[1]; j <= inSeg[2]; j++) {
        TPPair pair = pairs.get(j);
        int oIndex = search(pair.tid(), s2Index, s2End);

        if (oIndex > -1) {
          s2Index = oIndex + 1;
          double prob = pair.prob() * pairs.get(oIndex).prob();
          bufferedPairs.add(new TPPair(pair.tid(), prob));
          scup.accept(prob);
        }

        scup.setFirstIndex(j + 1);

        if (minimumSupport - scup.getExpectedSupport()
            > (s1.size() - (j - inSeg[1] + segmentSize)) * s2.getMaxSupport()) {
          ppf = true;
          break;
        }
      }

      scup.addPos(
          inSeg[0], pairs.size() + scupSegmentStart, pairs.size() + bufferedPairs.size() - 1);

      scup.setSecondIndex(s2Index);

      if (ppf) {
        break;
      }

      segmentSize += inSeg[2] - inSeg[1] + 1;
    }

    pairs.addAll(bufferedPairs);

    return scup;
  }

  private void reconstructSCUP(SCUP scup) {
    int[] lastSegment = scup.getLastSegment();
    int firstIndex = scup.getFirstIndex();
    int secondIndex = scup.getSecondIndex();

    if (scup.getFirstParent().size() == 1) {
      SUP s1 = supMap.get(scup.getFirstParent().iterator().next());
      SUP s2 = supMap.get(scup.getSecondParent());
      Map.Entry<Integer, int[]> s1LastSegment = s1.getLastSegment();
      Map.Entry<Integer, int[]> s2LastSegment = s2.getLastSegment();

      if (lastSegment[0] < s1LastSegment.getKey()
          && firstIndex < s1LastSegment.getValue()[1]
          && lastSegment[0] < s2LastSegment.getKey()
          && secondIndex < s2LastSegment.getValue()[1]) {

        int segmentSize = 0;
        int s2Index = secondIndex;
        List<TPPair> bufferedPairs = new FastList<>();

        for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().sequencedEntrySet()) {
          int inc = entry.getKey();
          int[] s1Segment = entry.getValue();

          if (inc < lastSegment[0] || !s2.containIncrement(inc)) {
            segmentSize += s1Segment[1] - s1Segment[0] + 1;
            continue;
          }

          int scupSegmentStart = bufferedPairs.size();

          int[] s2Segment = s2.getSegmentAt(entry.getKey());
          s2Index = Math.max(s2Segment[0], s2Index);
          int s2End = s2Segment[1];

          boolean ppf = false;

          for (int j = Math.max(s1Segment[0], firstIndex); j <= s1Segment[1]; j++) {
            TPPair pair = pairs.get(j);
            int oIndex = search(pair.tid(), s2Index, s2End);

            if (oIndex > -1) {
              s2Index = oIndex + 1;
              double prob = pair.prob() * pairs.get(oIndex).prob();
              bufferedPairs.add(new TPPair(pair.tid(), prob));
              scup.accept(prob);
            }

            scup.setFirstIndex(j + 1);

            if (minimumSupport - scup.getExpectedSupport()
                > (s1.size() - (j - s1Segment[0] + segmentSize)) * s2.getMaxSupport()) {
              ppf = true;
              break;
            }
          }

          scup.addPos(
              inc, pairs.size() + scupSegmentStart, pairs.size() + bufferedPairs.size() - 1);

          scup.setSecondIndex(s2Index);

          if (ppf) {
            break;
          }

          segmentSize += s1Segment[1] - s1Segment[0] + 1;
        }

        pairs.addAll(bufferedPairs);
      }
    } else {
      SCUP s1 = scupMap.get(scup.getFirstParent());
      reconstructSCUP(s1);

      SUP s2 = supMap.get(scup.getSecondParent());

      int[] s1LastSegment = s1.getLastSegment();
      Map.Entry<Integer, int[]> s2LastSegment = s2.getLastSegment();

      if (lastSegment[0] <= s1LastSegment[0]
          && firstIndex < s1LastSegment[2]
          && lastSegment[0] < s2LastSegment.getKey()
          && secondIndex < s2LastSegment.getValue()[1]) {

        int segmentSize = 0;
        int s2Index = secondIndex;
        List<TPPair> bufferedPairs = new FastList<>();

        for (int[] inSeg : s1.getIncrementSegments()) {
          if (inSeg[0] < lastSegment[0] || !s2.containIncrement(inSeg[0])) {
            segmentSize += inSeg[2] - inSeg[1] + 1;
            continue;
          }

          int scupSegmentStart = bufferedPairs.size();

          int[] s2Segment = s2.getSegmentAt(inSeg[0]);
          s2Index = Math.max(s2Segment[0], s2Index);

          boolean ppf = false;

          for (int j = Math.max(inSeg[1], firstIndex); j <= inSeg[2]; j++) {
            TPPair pair = pairs.get(j);
            int oIndex = search(pair.tid(), s2Index, s2Segment[1]);

            if (oIndex > -1) {
              s2Index = oIndex + 1;
              double prob = pair.prob() * pairs.get(oIndex).prob();
              bufferedPairs.add(new TPPair(pair.tid(), prob));
              scup.accept(prob);
            }

            scup.setFirstIndex(j + 1);

            if (minimumSupport - scup.getExpectedSupport()
                > (s1.size() - (j - inSeg[1] + segmentSize)) * s2.getMaxSupport()) {
              ppf = true;
              break;
            }
          }

          scup.addPos(
              inSeg[0], pairs.size() + scupSegmentStart, pairs.size() + bufferedPairs.size() - 1);

          scup.setSecondIndex(s2Index);

          if (ppf) {
            break;
          }

          segmentSize += inSeg[2] - inSeg[1] + 1;
        }

        pairs.addAll(bufferedPairs);
      }
    }
  }

  private int search(int tid, int start, int end) {
    if (start > end) {
      return -1;
    }

    if (pairs.get(start).tid() <= tid && tid <= pairs.get(end).tid()) {
      if (pairs.get(start).tid() == tid) {
        return start;
      }

      if (pairs.get(end).tid() == tid) {
        return end;
      }

      if (end - start + 1 < 32) {
        return linearSearch(tid, start, end);
      } else {
        return binarySearch(tid, start, end);
      }
    }

    return -1;
  }

  private int linearSearch(int tid, int start, int end) {
    for (int i = start; i <= end; i++) {
      if (pairs.get(i).tid() == tid) {
        return i;
      }
    }

    return -1;
  }

  private int binarySearch(int tid, int start, int end) {
    int left = start;
    int right = end;

    while (left <= right) {
      int mid = left + (right - left) / 2;

      int midPairTID = pairs.get(mid).tid();

      if (midPairTID > tid) {
        right = mid - 1;
      } else if (midPairTID < tid) {
        left = mid + 1;
      } else {
        return mid;
      }
    }

    return -1;
  }
}
