package vn.datm.ituna.iufpm;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.datm.ituna.db.UItem;
import vn.datm.ituna.db.UTDatabase;
import vn.datm.ituna.util.SCUP;
import vn.datm.ituna.util.SUP;
import vn.datm.ituna.util.TPPair;
import vn.datm.ituna.util.UItemSet;

public class IBUCA extends IUFPM {
  private int increment = 0;
  private ArrayList<TPPair> pairs = new ArrayList<>();
  private Map<Integer, SUP> supMap = new HashMap<>();
  private Map<Set<Integer>, SCUP> scupMap = new HashMap<>();

  public IBUCA(int k) {
    super(k);
  }

  @Override
  public void addDatabase(UTDatabase db) {
    Map<Integer, List<TPPair>> pairMap = new HashMap<>();

    for (ArrayList<UItem> transation : db.getTransactions()) {
      for (UItem uItem : transation) {
        int id = uItem.getId();

        if (supMap.containsKey(id)) {
          supMap.get(id).accept(uItem.getProbability());
        } else {
          supMap.put(id, new SUP(uItem.getProbability()));
        }

        if (!pairMap.containsKey(id)) {
          pairMap.put(id, new ArrayList<>());
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

    for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().sequencedEntrySet()) {
      if (!s2.containIncrement(entry.getKey())) {
        segmentSize += entry.getValue()[1] - entry.getValue()[0] + 1;
        continue;
      }

      int scupSegmentStart = pairs.size();

      int[] s2Segment = s2.getSegmentAt(entry.getKey());
      int s2Index = s2Segment[0];

      boolean ppf = false;

      for (int j = entry.getValue()[0]; j <= entry.getValue()[1]; j++) {
        TPPair pair = pairs.get(j);
        int oIndex = search(pair.tid(), s2Segment, s2Index);

        if (oIndex > -1) {
          s2Index = oIndex + 1;
          double prob = pair.prob() * pairs.get(oIndex).prob();
          pairs.add(new TPPair(pair.tid(), prob));
          scup.accept(prob);
        }

        scup.setFirstIndex(j + 1);

        if (minimumSupport - scup.getExpectedSupport()
            > (s1.size() - (j - entry.getValue()[0] + segmentSize)) * s2.getMaxSupport()) {
          ppf = true;
          break;
        }
      }

      scup.addPos(entry.getKey(), scupSegmentStart, pairs.size() - 1);
      scup.setSecondIndex(s2Index);

      if (ppf) {
        break;
      }

      segmentSize += entry.getValue()[1] - entry.getValue()[0] + 1;
    }

    return scup;
  }

  private SCUP constructSCUP(Set<Integer> p, SCUP s1, int id, SUP s2) {
    SCUP scup = new SCUP(p, id);
    int segmentSize = 0;
    int s2Index = -1;

    for (int[] inSeg : s1.getIncrementSegments()) {
      if (!s2.containIncrement(inSeg[0])) {
        segmentSize += inSeg[2] - inSeg[1] + 1;
        continue;
      }

      int scupSegmentStart = pairs.size();

      int[] s2Segment = s2.getSegmentAt(inSeg[0]);
      s2Index = Math.max(s2Index, s2Segment[0]);

      boolean ppf = false;

      for (int j = inSeg[1]; j <= inSeg[2]; j++) {
        TPPair pair = pairs.get(j);
        int oIndex = search(pair.tid(), s2Segment, s2Index);

        if (oIndex > -1) {
          s2Index = oIndex + 1;
          double prob = pair.prob() * pairs.get(oIndex).prob();
          pairs.add(new TPPair(pair.tid(), prob));
          scup.accept(prob);
        }

        scup.setFirstIndex(j + 1);

        if (minimumSupport - scup.getExpectedSupport()
            > (s1.size() - (j - inSeg[1] + segmentSize)) * s2.getMaxSupport()) {
          ppf = true;
          break;
        }
      }

      scup.addPos(inSeg[0], scupSegmentStart, pairs.size() - 1);
      scup.setSecondIndex(s2Index);

      if (ppf) {
        break;
      }

      segmentSize += inSeg[2] - inSeg[1] + 1;
    }

    return scup;
  }

  private void reconstructSCUP(SCUP scup) {
    int[] lastSegment = scup.getLastSegment();
    int firstIndex = scup.getFirstIndex();
    int secondIndex = scup.getSecondIndex();

    // if (new ImmutableSet.Builder<Integer>()
    //     .addAll(scup.getFirstParent())
    //     .add(scup.getSecondParent())
    //     .build()
    //     .equals(ImmutableSet.of(12, 90, 94))) {
    //   System.out.println(scupMap.get(ImmutableSet.of(12, 90, 94)));
    // }

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

        for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().sequencedEntrySet()) {
          if (entry.getKey() < lastSegment[0] || !s2.containIncrement(entry.getKey())) {
            segmentSize += entry.getValue()[1] - entry.getValue()[0] + 1;
            continue;
          }

          int scupSegmentStart = pairs.size();

          int[] s2Segment = s2.getSegmentAt(entry.getKey());
          s2Index = Math.max(s2Segment[0], s2Index);

          boolean ppf = false;

          for (int j = Math.max(entry.getValue()[0], firstIndex); j <= entry.getValue()[1]; j++) {
            TPPair pair = pairs.get(j);
            int oIndex = search(pair.tid(), s2Segment, s2Index);

            if (oIndex > -1) {
              s2Index = oIndex + 1;
              double prob = pair.prob() * pairs.get(oIndex).prob();
              pairs.add(new TPPair(pair.tid(), prob));
              scup.accept(prob);
            }

            scup.setFirstIndex(j + 1);

            if (minimumSupport - scup.getExpectedSupport()
                > (s1.size() - (j - entry.getValue()[0] + segmentSize)) * s2.getMaxSupport()) {
              ppf = true;
              break;
            }
          }

          scup.addPos(entry.getKey(), scupSegmentStart, pairs.size() - 1);
          scup.setSecondIndex(s2Index);

          if (ppf) {
            break;
          }

          segmentSize += entry.getValue()[1] - entry.getValue()[0] + 1;
        }
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

        for (int[] inSeg : s1.getIncrementSegments()) {
          if (inSeg[0] < lastSegment[0] || !s2.containIncrement(inSeg[0])) {
            segmentSize += inSeg[2] - inSeg[1] + 1;
            continue;
          }

          int scupSegmentStart = pairs.size();

          int[] s2Segment = s2.getSegmentAt(inSeg[0]);
          s2Index = Math.max(s2Segment[0], s2Index);

          boolean ppf = false;

          for (int j = Math.max(inSeg[1], firstIndex); j <= inSeg[2]; j++) {
            TPPair pair = pairs.get(j);
            int oIndex = search(pair.tid(), s2Segment, s2Index);

            if (oIndex > -1) {
              s2Index = oIndex + 1;
              double prob = pair.prob() * pairs.get(oIndex).prob();
              pairs.add(new TPPair(pair.tid(), prob));
              scup.accept(prob);
            }

            scup.setFirstIndex(j + 1);

            if (minimumSupport - scup.getExpectedSupport()
                > (s1.size() - (j - inSeg[1] + segmentSize)) * s2.getMaxSupport()) {
              ppf = true;
              break;
            }
          }

          scup.addPos(inSeg[0], scupSegmentStart, pairs.size() - 1);
          scup.setSecondIndex(s2Index);

          if (ppf) {
            break;
          }

          segmentSize += inSeg[2] - inSeg[1] + 1;
        }
      }
    }
  }

  private int search(int tid, int[] segment, int fromIndex) {
    if (fromIndex > segment[1]) {
      return -1;
    }

    if (pairs.get(fromIndex).tid() <= tid && tid <= pairs.get(segment[1]).tid()) {
      if (pairs.get(fromIndex).tid() == tid) {
        return fromIndex;
      }

      if (pairs.get(segment[1]).tid() == tid) {
        return segment[1];
      }

      if (segment[1] - fromIndex + 1 < 32) {
        return linearSearch(tid, segment, fromIndex);
      } else {
        return binarySearch(tid, segment, fromIndex);
      }
    }

    return -1;
  }

  private int linearSearch(int tid, int[] segment, int fromIndex) {
    for (int i = fromIndex; i <= segment[1]; i++) {
      if (pairs.get(i).tid() == tid) {
        return i;
      }
    }

    return -1;
  }

  private int binarySearch(int tid, int[] segment, int fromIndex) {
    int left = fromIndex;
    int right = segment[1];

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
