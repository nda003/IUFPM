package vn.datm.iufpm.util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

/**
 * Incrementally segmented uncertain pattern list. This data structure segments its list of
 * transations with each increments.
 *
 * <p>An increment is defined as the added database to which a transaction belongs to. For example,
 * we have an accumulated dynamic uncertain database:
 *
 * <table><thead>
 * <tr>
 * <th>Increment</th>
 * <th>Transaction ID</th>
 * <th>Items</th>
 * </tr></thead>
 * <tbody>
 * <tr>
 * <td rowspan="2">1</td>
 * <td>1</td>
 * <td>a: 0.5, b: 0.4</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>b: 0.4</td>
 * </tr>
 * <tr>
 * <td rowspan="2">2</td>
 * <td>3</td>
 * <td>a: 0.1</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>a: 0.3</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * Thus transaction 1 and 2 belongs to increment 1 and only 1, while transaction 3 and 4 belongs to
 * increment 2 and only 2. Using this one-to-many relation between increments and transactions, we
 * can modify the original ICUPList through segmentation using increment. By incrementally
 * segmenting the {@link ISUPList}, we can look up a transaction ID quickly by just scanning the
 * incremental segment that it belongs to, instead of scanning the whole list.
 *
 * <p>Using item a as an example, we have the {@link ISUPList}:
 *
 * <table><thead>
 * <tr>
 * <th>Increment</th>
 * <th>Index</th>
 * <th>Transaction ID</th>
 * <th>Probability</th>
 * </tr></thead>
 * <tbody>
 * <tr>
 * <td>1</td>
 * <td>0</td>
 * <td>1</td>
 * <td>0.5</td>
 * </tr>
 * <tr>
 * <td rowspan="2">2</td>
 * <td>1</td>
 * <td>3</td>
 * <td>0.1</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>4</td>
 * <td>0.3</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * The incremental segmentation is as such:
 *
 * <table><thead>
 * <tr>
 * <th>Increment</th>
 * <th>Start</th>
 * <th>End</th>
 * </tr></thead>
 * <tbody>
 * <tr>
 * <td>1</td>
 * <td>0</td>
 * <td>0</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>1</td>
 * <td>2</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * Start and end are inclusive.
 */
public class ISUPList {
  private static final int LINEAR_SEARCH_THRESHOLD = 32;

  protected LinkedHashMap<Integer, int[]> incSeg = new LinkedHashMap<>();
  protected int pairsPreviousSize = 0;

  private double maxSupport;
  private double expectedSupport;

  /**
   * We use 2 parallel primitive array lists for performance, with {@link #tidList} being
   * Transaction ID column, and {@link #probList} being the Probability column.
   */
  protected IntArrayList tidList;

  /**
   * We use 2 parallel primitive array lists for performance, with {@link #tidList} being
   * Transaction ID column, and {@link #probList} being the Probability column.
   */
  protected DoubleArrayList probList;

  protected ISUPList() {
    maxSupport = 0;
    expectedSupport = 0;
    tidList = new IntArrayList();
    probList = new DoubleArrayList();
  }

  /**
   * Constructs a list with the specified tid and probability.
   *
   * @param tid The transaction's id.
   * @param prob The probability of the transaction.
   */
  public ISUPList(int tid, double prob) {
    maxSupport = prob;
    expectedSupport = prob;
    tidList = IntArrayList.newListWith(tid);
    probList = DoubleArrayList.newListWith(prob);
  }

  /**
   * Add a transaction with a specified tid and support. Augement maximum support and expected
   * support.
   *
   * @param tid The transaction's id.
   * @param prob The support of the transaction.
   */
  public void addTransaction(int tid, double prob) {
    expectedSupport += prob;

    if (prob > maxSupport) {
      maxSupport = prob;
    }

    tidList.add(tid);
    probList.add(prob);
  }

  /**
   * Increases the capacity of this <tt>ISUPList</tt> instance, if necessary, to ensure that it can
   * hold at least the number of elements specified by the minimum capacity argument.
   *
   * @param minCapacity The desired minimum capacity.
   */
  public void ensureCapacity(int minCapacity) {
    tidList.ensureCapacity(minCapacity);
    probList.ensureCapacity(minCapacity);
  }

  /**
   * Returns the transaction's id at the specified position in this list.
   *
   * @param index The index of the transaction's id to return.
   * @return The transaction's id at the specified position in this list.
   */
  public int getTidAt(int index) {
    return tidList.get(index);
  }

  /**
   * Returns the probability at the specified position in this list.
   *
   * @param index The index of the probability to return.
   * @return The probability at the specified position in this list.
   */
  public double getProbAt(int index) {
    return probList.get(index);
  }

  /**
   * Returns the increment segments of this list.
   *
   * @return The increment segments of this list.
   */
  public LinkedHashMap<Integer, int[]> getIncrementSegments() {
    return incSeg;
  }

  /**
   * Returns {@code true} if the list contains the specified increment.
   *
   * @param increment The increment to check for.
   * @return {@code true} if the list contains the specified increment.
   */
  public boolean containIncrement(int increment) {
    return incSeg.containsKey(increment);
  }

  /**
   * Returns the segment at the specified increment.
   *
   * @param increment The increment to get the segment from.
   * @return The segment at the specified increment.
   */
  public int[] getSegmentAt(int increment) {
    return incSeg.get(increment);
  }

  /**
   * Returns the number of increments in this list.
   *
   * @return The number of increments in this list.
   */
  public int incrementSize() {
    return incSeg.size();
  }

  /**
   * Returns the segment of the last increment in this list.
   *
   * @return The segment of the last increment in this list.
   */
  public Map.Entry<Integer, int[]> getLastSegment() {
    return incSeg.lastEntry();
  }

  /**
   * Puts the increment into the list and segment it.
   *
   * @param increment The increment to put into the list.
   */
  public void putIncrement(int increment) {
    incSeg.put(increment, new int[] {pairsPreviousSize, tidList.size() - 1});
    pairsPreviousSize = tidList.size();
  }

  /**
   * Returns the maximum support of this list.
   *
   * @return The maximum support of this list.
   */
  public double getMaxSupport() {
    return maxSupport;
  }

  /**
   * Returns the expected support of this list.
   *
   * @return The expected support of this list.
   */
  public double getExpectedSupport() {
    return expectedSupport;
  }

  /**
   * Returns the number of transactions in this list.
   *
   * @return The number of transactions in this list.
   */
  public int size() {
    return tidList.size();
  }

  /**
   * Searches for the specified tid in the list.
   *
   * @param tid The tid to search for.
   * @param start The index to start searching from.
   * @param end The index to stop searching at.
   * @return The index of the tid, or -1 if not found.
   */
  public int search(int tid, int start, int end) {
    if (start > end) {
      return -1;
    }

    if (tidList.get(start) <= tid && tid <= tidList.get(end)) {
      if (tidList.get(start) == tid) {
        return start;
      }

      if (tidList.get(end) == tid) {
        return end;
      }

      if (end - start + 1 < LINEAR_SEARCH_THRESHOLD) {
        return linearSearch(tid, start, end);
      } else {
        return binarySearch(tid, start, end);
      }
    }

    return -1;
  }

  private int linearSearch(int tid, int start, int end) {
    for (int i = start; i <= end; i++) {
      if (tidList.get(i) == tid) {
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

      int midPairTID = tidList.get(mid);

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

  @Override
  public String toString() {
    StringBuilder sb =
        new StringBuilder(
            String.format(
                "(expectedSupport=%.2f, maxSupport=%.2f, %s)",
                expectedSupport, maxSupport, incSeg.toString()));

    return sb.toString();
  }
}
