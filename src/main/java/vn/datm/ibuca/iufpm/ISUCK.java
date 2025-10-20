package vn.datm.ibuca.iufpm;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import vn.datm.ibuca.db.UItem;
import vn.datm.ibuca.db.UTDatabase;
import vn.datm.ibuca.util.ISCUPList;
import vn.datm.ibuca.util.ISUPList;
import vn.datm.ibuca.util.TPPair;
import vn.datm.ibuca.util.UItemSet;

public class ISUCK extends IUFPM {
  private int currentIncrement = 0;
  private MutableIntObjectMap<ISUPList> isupMap = new IntObjectHashMap<>();
  private Map<ImmutableIntSet, ISCUPList> iscupMap = new UnifiedMap<>();

  public ISUCK(int k) {
    super(k);
  }

  @Override
  public void addDatabase(UTDatabase db) {
    Set<Integer> changedIds = new UnifiedSet<>();

    for (ImmutableList<UItem> transation : db.getTransactions()) {
      for (UItem uItem : transation) {
        int id = uItem.id();

        if (!changedIds.contains(id)) {
          changedIds.add(id);
        }

        if (isupMap.containsKey(id)) {
          isupMap.get(id).addPair(currentTid, uItem.prob());
        } else {
          isupMap.put(id, new ISUPList(currentTid, uItem.prob()));
        }
      }

      currentTid++;
    }

    for (int id : changedIds) {
      isupMap.get(id).putIncrement(currentIncrement);
    }

    currentIncrement++;
  }

  @Override
  public List<UItemSet> mine() {
    LimitedSortedItemSets pq =
        new LimitedSortedItemSets(
            k, Comparator.comparingDouble(UItemSet::getExpectedSupport).reversed());

    pq.addAll(
        isupMap
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
      ISUPList isup = isupMap.get(idToTraverse.get(i));

      if (isup.getExpectedSupport() >= minimumSupport) {
        List<SimpleImmutableEntry<ImmutableIntSet, Integer>> patternToTraverse = new ArrayList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          ISUPList jsup = isupMap.get(idToTraverse.get(j));

          if (jsup.getExpectedSupport() * isup.getMaxSupport() >= minimumSupport) {
            ImmutableIntSet pattern =
                IntSets.immutable.with(idToTraverse.get(i), idToTraverse.get(j));

            if (iscupMap.containsKey(pattern)) {
              reconstructISCUPList(iscupMap.get(pattern));

              if (iscupMap.get(pattern).getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, iscupMap.get(pattern).getExpectedSupport()));
                patternToTraverse.add(new SimpleImmutableEntry<>(pattern, j));

                if (pq.size() >= k) {
                  minimumSupport = pq.getLast().getExpectedSupport();
                }
              }
            } else {
              ISCUPList scup =
                  constructISCUPList(idToTraverse.get(i), isup, idToTraverse.get(j), jsup);

              if (scup != null) {
                iscupMap.put(pattern, scup);
              } else {
                continue;
              }

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
      ImmutableIntList idToTraverse,
      List<SimpleImmutableEntry<ImmutableIntSet, Integer>> patternToTraverse,
      int fromIndex) {
    for (SimpleImmutableEntry<ImmutableIntSet, Integer> pattern : patternToTraverse) {
      List<SimpleImmutableEntry<ImmutableIntSet, Integer>> nextPatternToTraverse =
          new ArrayList<>();

      for (int i = pattern.getValue() + 1; i < idToTraverse.size(); i++) {
        ImmutableIntSet nextPattern = pattern.getKey().newWith(idToTraverse.get(i));

        ISUPList sup = isupMap.get(idToTraverse.get(i));

        if (iscupMap.get(pattern.getKey()).getExpectedSupport() * sup.getMaxSupport()
            >= minimumSupport) {
          if (iscupMap.containsKey(nextPattern)) {
            reconstructISCUPList(iscupMap.get(nextPattern));

            if (iscupMap.get(nextPattern).getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, iscupMap.get(nextPattern).getExpectedSupport()));
              nextPatternToTraverse.add(new SimpleImmutableEntry<>(nextPattern, i));

              if (pq.size() >= k) {
                minimumSupport = pq.getLast().getExpectedSupport();
              }
            }
          } else {
            ISCUPList nextSCUP =
                constructISCUPList(
                    pattern.getKey(), iscupMap.get(pattern.getKey()), idToTraverse.get(i), sup);

            if (nextSCUP != null) {
              iscupMap.put(nextPattern, nextSCUP);
            } else {
              continue;
            }

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

  private ISCUPList constructISCUPList(int id1, ISUPList s1, int id2, ISUPList s2) {
    ISCUPList scup = new ISCUPList(id1, id2);
    int segmentSize = 0;

    for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().entrySet()) {
      int inc = entry.getKey();
      int[] s1Segment = entry.getValue();

      if (!s2.containIncrement(inc)) {
        segmentSize += s1Segment[1] - s1Segment[0] + 1;
        continue;
      }

      int[] s2Segment = s2.getSegmentAt(inc);

      int s2Index = s2Segment[0];

      boolean ppf = false;

      for (int j = s1Segment[0]; j <= s1Segment[1]; j++) {
        TPPair pair = s1.getPairAt(j);
        int oIndex = s2.search(pair.tid(), s2Index, s2Segment[1]);

        if (oIndex > -1) {
          s2Index = oIndex + 1;
          scup.addPair(pair.tid(), pair.prob() * s2.getPairAt(oIndex).prob());
        }

        scup.setFirstIndex(j + 1);

        if (minimumSupport - scup.getExpectedSupport()
            > (s1.size() - (j - s1Segment[0] + segmentSize)) * s2.getMaxSupport()) {
          ppf = true;
          break;
        }
      }

      scup.putIncrement(inc);

      scup.setSecondIndex(s2Index);

      if (ppf) {
        break;
      }

      segmentSize += s1Segment[1] - s1Segment[0] + 1;
    }

    if (scup.getIncrementSegments().isEmpty()) {
      return null;
    }

    return scup;
  }

  private ISCUPList constructISCUPList(ImmutableIntSet p, ISCUPList s1, int id, ISUPList s2) {
    ISCUPList scup = new ISCUPList(p, id);
    int segmentSize = 0;

    for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().entrySet()) {
      int inc = entry.getKey();
      int[] s1Segment = entry.getValue();

      if (!s2.containIncrement(inc)) {
        segmentSize += s1Segment[1] - s1Segment[0] + 1;
        continue;
      }

      int[] s2Segment = s2.getSegmentAt(inc);

      int s2Index = s2Segment[0];

      boolean ppf = false;

      for (int j = s1Segment[0]; j <= s1Segment[1]; j++) {
        TPPair pair = s1.getPairAt(j);
        int oIndex = s2.search(pair.tid(), s2Index, s2Segment[1]);

        if (oIndex > -1) {
          s2Index = oIndex + 1;
          scup.addPair(pair.tid(), pair.prob() * s2.getPairAt(oIndex).prob());
        }

        scup.setFirstIndex(j + 1);

        if (minimumSupport - scup.getExpectedSupport()
            > (s1.size() - (j - s1Segment[0] + segmentSize)) * s2.getMaxSupport()) {
          ppf = true;
          break;
        }
      }

      scup.putIncrement(inc);

      scup.setSecondIndex(s2Index);

      if (ppf) {
        break;
      }

      segmentSize += s1Segment[1] - s1Segment[0] + 1;
    }

    if (scup.getIncrementSegments().isEmpty()) {
      return null;
    }

    return scup;
  }

  private void reconstructISCUPList(ISCUPList scup) {
    int lastInc = scup.getLastSegment().getKey();

    int firstIndex = scup.getFirstIndex();
    int secondIndex = scup.getSecondIndex();

    if (scup.getFirstParent().size() == 1) {
      ISUPList s1 = isupMap.get(scup.getFirstParent().intIterator().next());
      ISUPList s2 = isupMap.get(scup.getSecondParent());

      Map.Entry<Integer, int[]> s1LastSegment = s1.getLastSegment();
      Map.Entry<Integer, int[]> s2LastSegment = s2.getLastSegment();

      if (lastInc < s1LastSegment.getKey()
          && firstIndex < s1LastSegment.getValue()[1]
          && lastInc < s2LastSegment.getKey()
          && secondIndex < s2LastSegment.getValue()[1]) {

        int segmentSize = 0;
        int s2Index = secondIndex;

        for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().sequencedEntrySet()) {
          int inc = entry.getKey();
          int[] s1Segment = entry.getValue();

          if (inc < lastInc || !s2.containIncrement(inc)) {
            segmentSize += s1Segment[1] - s1Segment[0] + 1;
            continue;
          }

          int[] s2Segment = s2.getSegmentAt(entry.getKey());
          s2Index = Math.max(s2Segment[0], s2Index);
          int s2End = s2Segment[1];

          boolean ppf = false;

          for (int j = Math.max(s1Segment[0], firstIndex); j <= s1Segment[1]; j++) {
            TPPair pair = s1.getPairAt(j);
            int oIndex = s2.search(pair.tid(), s2Index, s2End);

            if (oIndex > -1) {
              s2Index = oIndex + 1;
              scup.addPair(pair.tid(), pair.prob() * s2.getPairAt(oIndex).prob());
            }

            scup.setFirstIndex(j + 1);

            if (minimumSupport - scup.getExpectedSupport()
                > (s1.size() - (j - s1Segment[0] + segmentSize)) * s2.getMaxSupport()) {
              ppf = true;
              break;
            }
          }

          scup.putIncrement(inc);

          scup.setSecondIndex(s2Index);

          if (ppf) {
            break;
          }

          segmentSize += s1Segment[1] - s1Segment[0] + 1;
        }
      }
    } else {
      ISCUPList s1 = iscupMap.get(scup.getFirstParent());
      reconstructISCUPList(s1);

      ISUPList s2 = isupMap.get(scup.getSecondParent());

      Map.Entry<Integer, int[]> s1LastSegment = s1.getLastSegment();
      Map.Entry<Integer, int[]> s2LastSegment = s2.getLastSegment();

      if (lastInc < s1LastSegment.getKey()
          && firstIndex < s1LastSegment.getValue()[1]
          && lastInc < s2LastSegment.getKey()
          && secondIndex < s2LastSegment.getValue()[1]) {

        int segmentSize = 0;
        int s2Index = secondIndex;

        for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().sequencedEntrySet()) {
          int inc = entry.getKey();
          int[] s1Segment = entry.getValue();

          if (inc < lastInc || !s2.containIncrement(inc)) {
            segmentSize += s1Segment[1] - s1Segment[0] + 1;
            continue;
          }

          int[] s2Segment = s2.getSegmentAt(entry.getKey());
          s2Index = Math.max(s2Segment[0], s2Index);
          int s2End = s2Segment[1];

          boolean ppf = false;

          for (int j = Math.max(s1Segment[0], firstIndex); j <= s1Segment[1]; j++) {
            TPPair pair = s1.getPairAt(j);
            int oIndex = s2.search(pair.tid(), s2Index, s2End);

            if (oIndex > -1) {
              s2Index = oIndex + 1;
              scup.addPair(pair.tid(), pair.prob() * s2.getPairAt(oIndex).prob());
            }

            scup.setFirstIndex(j + 1);

            if (minimumSupport - scup.getExpectedSupport()
                > (s1.size() - (j - s1Segment[0] + segmentSize)) * s2.getMaxSupport()) {
              ppf = true;
              break;
            }
          }

          scup.putIncrement(inc);

          scup.setSecondIndex(s2Index);

          if (ppf) {
            break;
          }

          segmentSize += s1Segment[1] - s1Segment[0] + 1;
        }
      }
    }
  }
}
