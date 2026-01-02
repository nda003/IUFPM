package vn.datm.iufpm.lib;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Triple;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.collections.impl.tuple.Tuples;
import vn.datm.iufpm.db.UItem;
import vn.datm.iufpm.db.UTDatabase;
import vn.datm.iufpm.util.ISCUPList;
import vn.datm.iufpm.util.ISUPList;
import vn.datm.iufpm.util.UItemSet;

/**
 * An incrementally segmented uncertain top-K frequent pattern mining model. This model differs from
 * {@link ITUFP} by recording at which increment were the transaction added to the model, thus
 * creating an initial lower bound and an upper bound from which to join and construct {@link
 * ISUPList} while remembering from which index were the {@link ISCUPList} were last mined for
 * reconstruction.
 */
public class ISUCK extends IUFPM {
  /**
   * The current increment to record the increment in which the transactions were added in.
   *
   * @see {@link #addDatabase(UTDatabase)}
   */
  private int currentIncrement = 0;

  /** Maps the transaction's id to its associated {@link ISUPList}. */
  private MutableIntObjectMap<ISUPList> isupMap = new IntObjectHashMap<>();

  /** Maps a itemset to its associated {@link ISCUPList}. */
  private Map<ImmutableIntSet, ISCUPList> iscupMap = new HashMap<>();

  /**
   * Initialize this model with a top-K value.
   *
   * @param k The top-K value.
   */
  public ISUCK(int k) {
    super(k);
  }

  /**
   * Increment this model using the specified {@link UTDatabase}, uses {@link #currentTid} to record
   * transactions and {@link #currentIncrement} to record its associated increment.
   *
   * @param db The database to add to the model.
   */
  @Override
  public void addDatabase(UTDatabase db) {
    MutableIntSet changedIds = new IntHashSet();

    for (ImmutableList<UItem> transation : db.getTransactions()) {
      for (UItem uItem : transation) {
        int id = uItem.id();

        if (!changedIds.contains(id)) {
          changedIds.add(id);
        }

        if (isupMap.containsKey(id)) {
          isupMap.get(id).addTransaction(currentTid, uItem.prob());
        } else {
          isupMap.put(id, new ISUPList(currentTid, uItem.prob()));
        }
      }

      currentTid++;
    }

    changedIds.each((id) -> isupMap.get(id).putIncrement(currentIncrement));
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
        List<Pair<ImmutableIntSet, Integer>> patternToTraverse = new FastList<>();

        for (int j = i + 1; j < idToTraverse.size(); j++) {
          ISUPList jsup = isupMap.get(idToTraverse.get(j));

          if (jsup.getExpectedSupport() * isup.getMaxSupport() >= minimumSupport) {
            ImmutableIntSet pattern =
                IntSets.immutable.with(idToTraverse.get(i), idToTraverse.get(j));

            if (iscupMap.containsKey(pattern)) {
              reconstructISCUPList(iscupMap.get(pattern));

              if (iscupMap.get(pattern).getExpectedSupport() >= minimumSupport) {
                pq.add(new UItemSet(pattern, iscupMap.get(pattern).getExpectedSupport()));
                patternToTraverse.add(Tuples.pair(pattern, j));

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
                patternToTraverse.add(Tuples.pair(pattern, j));

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

  /**
   * Recursively mines for frequent patterns.
   *
   * @param pq The priority queue of frequent patterns.
   * @param idToTraverse The list of transaction ids to traverse.
   * @param patternToTraverse The list of patterns to traverse.
   * @param fromIndex The index to start traversing from.
   */
  private void mine(
      LimitedSortedItemSets pq,
      ImmutableIntList idToTraverse,
      List<Pair<ImmutableIntSet, Integer>> patternToTraverse,
      int fromIndex) {
    for (Pair<ImmutableIntSet, Integer> pattern : patternToTraverse) {
      List<Pair<ImmutableIntSet, Integer>> nextPatternToTraverse = new FastList<>();

      for (int i = pattern.getTwo() + 1; i < idToTraverse.size(); i++) {
        ImmutableIntSet nextPattern = pattern.getOne().newWith(idToTraverse.get(i));

        ISUPList sup = isupMap.get(idToTraverse.get(i));

        if (iscupMap.get(pattern.getOne()).getExpectedSupport() * sup.getMaxSupport()
            >= minimumSupport) {
          if (iscupMap.containsKey(nextPattern)) {
            reconstructISCUPList(iscupMap.get(nextPattern));

            if (iscupMap.get(nextPattern).getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, iscupMap.get(nextPattern).getExpectedSupport()));
              nextPatternToTraverse.add(Tuples.pair(nextPattern, i));

              if (pq.size() >= k) {
                minimumSupport = pq.getLast().getExpectedSupport();
              }
            }
          } else {
            ISCUPList nextSCUP =
                constructISCUPList(
                    pattern.getOne(), iscupMap.get(pattern.getOne()), idToTraverse.get(i), sup);

            if (nextSCUP != null) {
              iscupMap.put(nextPattern, nextSCUP);
            } else {
              continue;
            }

            if (nextSCUP.getExpectedSupport() >= minimumSupport) {
              pq.add(new UItemSet(nextPattern, nextSCUP.getExpectedSupport()));
              nextPatternToTraverse.add(Tuples.pair(nextPattern, i));

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

  /**
   * Constructs a new {@link ISCUPList}.
   *
   * @param id1 The transaction id of the first parent.
   * @param s1 The first parent.
   * @param id2 The transaction id of the second parent.
   * @param s2 The second parent.
   * @return The newly constructed {@link ISCUPList}.
   */
  private ISCUPList constructISCUPList(int id1, ISUPList s1, int id2, ISUPList s2) {
    ISCUPList scup = new ISCUPList(id1, id2);

    List<Triple<Integer, int[], int[]>> sharedIncrementSegments = new FastList<>();
    int potentialSize = 0;

    for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().entrySet()) {
      int inc = entry.getKey();

      int[] s2Segment = s2.getSegmentAt(inc);

      if (s2Segment == null) {
        continue;
      }

      int[] s1Segment = entry.getValue();

      sharedIncrementSegments.add(Tuples.triple(inc, s1Segment, s2Segment));
      potentialSize += s1Segment[1] - s1Segment[0] + 1;
    }

    if (sharedIncrementSegments.isEmpty()) {
      return null;
    }

    int processedTransactions = 0;

    for (Triple<Integer, int[], int[]> triple : sharedIncrementSegments) {
      int increment = triple.getOne();

      int[] s1Segment = triple.getTwo();
      int[] s2Segment = triple.getThree();

      int s2Index = s2Segment[0];

      boolean ppf = false;

      for (int i = s1Segment[0]; i <= s1Segment[1]; i++) {
        int tid = s1.getTidAt(i);
        int oIndex = s2.search(tid, s2Index, s2Segment[1]);

        if (oIndex > -1) {
          s2Index = oIndex + 1;
          scup.addTransaction(tid, s1.getProbAt(i) * s2.getProbAt(oIndex));
        }

        scup.setFirstIndex(i + 1);
        processedTransactions++;

        if (minimumSupport > scup.getExpectedSupport()
            && minimumSupport - scup.getExpectedSupport()
                > (potentialSize - processedTransactions) * s2.getMaxSupport()) {
          ppf = true;
          break;
        }
      }

      scup.putIncrement(increment);

      scup.setSecondIndex(s2Index);

      if (ppf) {
        break;
      }
    }

    // if (scup.getIncrementSegments().isEmpty()) {
    //   return null;
    // }

    return scup;
  }

  /**
   * Constructs a new {@link ISCUPList}.
   *
   * @param p The pattern of the first parent.
   * @param s1 The first parent.
   * @param id The transaction id of the second parent.
   * @param s2 The second parent.
   * @return The newly constructed {@link ISCUPList}.
   */
  private ISCUPList constructISCUPList(ImmutableIntSet p, ISCUPList s1, int id, ISUPList s2) {
    ISCUPList scup = new ISCUPList(p, id);

    List<Triple<Integer, int[], int[]>> sharedIncrementSegments = new FastList<>();
    int potentialSize = 0;

    for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().entrySet()) {
      int inc = entry.getKey();

      int[] s2Segment = s2.getSegmentAt(inc);

      if (s2Segment == null) {
        continue;
      }

      int[] s1Segment = entry.getValue();

      sharedIncrementSegments.add(Tuples.triple(inc, s1Segment, s2Segment));
      potentialSize += s1Segment[1] - s1Segment[0] + 1;
    }

    if (sharedIncrementSegments.isEmpty()) {
      return null;
    }

    int processedTransaction = 0;

    for (Triple<Integer, int[], int[]> triple : sharedIncrementSegments) {
      int inc = triple.getOne();

      int[] s1Segment = triple.getTwo();
      int[] s2Segment = triple.getThree();

      int s2Index = s2Segment[0];

      boolean ppf = false;

      for (int i = s1Segment[0]; i <= s1Segment[1]; i++) {
        int tid = s1.getTidAt(i);
        int oIndex = s2.search(tid, s2Index, s2Segment[1]);

        if (oIndex > -1) {
          s2Index = oIndex + 1;
          scup.addTransaction(tid, s1.getProbAt(i) * s2.getProbAt(oIndex));
        }

        scup.setFirstIndex(i + 1);
        processedTransaction++;

        if (minimumSupport > scup.getExpectedSupport()
            && minimumSupport - scup.getExpectedSupport()
                > (potentialSize - processedTransaction) * s2.getMaxSupport()) {
          ppf = true;
          break;
        }
      }

      scup.putIncrement(inc);

      scup.setSecondIndex(s2Index);

      if (ppf) {
        break;
      }
    }

    return scup;
  }

  /**
   * Reconstruct the specified {@link ISCUPList}.
   *
   * @param scup The {@link ISCUPList} to be reconstructed.
   */
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

        List<Triple<Integer, int[], int[]>> sharedIncrementSegments = new FastList<>();
        int potentialSize = 0;

        for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().entrySet()) {
          int inc = entry.getKey();

          if (inc < lastInc) {
            continue;
          }

          int[] s2Segment = s2.getSegmentAt(inc);

          if (s2Segment == null) {
            continue;
          }

          int[] s1Segment = entry.getValue();

          if (inc == lastInc) {
            if (firstIndex < s1Segment[1] && secondIndex < s2Segment[1]) {
              sharedIncrementSegments.add(
                  Tuples.triple(
                      inc,
                      new int[] {firstIndex, s1Segment[1]},
                      new int[] {secondIndex, s2Segment[1]}));
              potentialSize += s1Segment[1] - firstIndex + 1;
            }
          } else {
            sharedIncrementSegments.add(Tuples.triple(inc, s1Segment, s2Segment));
            potentialSize += s1Segment[1] - s1Segment[0] + 1;
          }
        }

        int processedTransaction = 0;

        for (Triple<Integer, int[], int[]> triple : sharedIncrementSegments) {
          int inc = triple.getOne();
          int[] s1Segment = triple.getTwo();
          int[] s2Segment = triple.getThree();

          boolean ppf = false;

          int s2Index = s2Segment[0];

          for (int i = s1Segment[0]; i <= s1Segment[1]; i++) {
            int tid = s1.getTidAt(i);
            int oIndex = s2.search(tid, s2Index, s2Segment[1]);

            if (oIndex > -1) {
              s2Index = oIndex + 1;
              scup.addTransaction(tid, s1.getProbAt(i) * s2.getProbAt(oIndex));
            }

            scup.setFirstIndex(i + 1);
            processedTransaction++;

            if (minimumSupport > scup.getExpectedSupport()
                && minimumSupport - scup.getExpectedSupport()
                    > (potentialSize - processedTransaction) * s2.getMaxSupport()) {
              ppf = true;
              break;
            }
          }

          scup.putIncrement(inc);

          scup.setSecondIndex(s2Index);

          if (ppf) {
            break;
          }
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

        List<Triple<Integer, int[], int[]>> sharedIncrementSegments = new FastList<>();
        int potentialSize = 0;

        for (Map.Entry<Integer, int[]> entry : s1.getIncrementSegments().entrySet()) {
          int inc = entry.getKey();

          if (inc < lastInc) {
            continue;
          }

          int[] s2Segment = s2.getSegmentAt(inc);

          if (s2Segment == null) {
            continue;
          }

          int[] s1Segment = entry.getValue();

          if (inc == lastInc) {
            if (firstIndex < s1Segment[1] && secondIndex < s2Segment[1]) {
              sharedIncrementSegments.add(
                  Tuples.triple(
                      inc,
                      new int[] {firstIndex, s1Segment[1]},
                      new int[] {secondIndex, s2Segment[1]}));
              potentialSize += s1Segment[1] - firstIndex + 1;
            }
          } else {
            sharedIncrementSegments.add(Tuples.triple(inc, s1Segment, s2Segment));
            potentialSize += s1Segment[1] - s1Segment[0] + 1;
          }
        }

        int processedTransactions = 0;

        for (Triple<Integer, int[], int[]> triple : sharedIncrementSegments) {
          int inc = triple.getOne();
          int[] s1Segment = triple.getTwo();
          int[] s2Segment = triple.getThree();

          boolean ppf = false;

          int s2Index = s2Segment[0];

          for (int i = s1Segment[0]; i <= s1Segment[1]; i++) {
            int tid = s1.getTidAt(i);
            int oIndex = s2.search(tid, s2Index, s2Segment[1]);

            if (oIndex > -1) {
              s2Index = oIndex + 1;
              scup.addTransaction(tid, s1.getProbAt(i) * s2.getProbAt(oIndex));
            }

            scup.setFirstIndex(i + 1);
            processedTransactions++;

            if (minimumSupport > scup.getExpectedSupport()
                && minimumSupport - scup.getExpectedSupport()
                    > (potentialSize - processedTransactions) * s2.getMaxSupport()) {
              ppf = true;
              break;
            }
          }

          scup.putIncrement(inc);

          scup.setSecondIndex(s2Index);

          if (ppf) {
            break;
          }
        }
      }
    }
  }
}
