package vn.datm.iufpm.util;

import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

/**
 * Uncertain pattern list.
 *
 * <p>For example, we have an accumulated dynamic uncertain database:
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
 * Using item a as an example, we have the {@link UPList}:
 *
 * <table><thead>
 * <tr>
 * <th>Index</th>
 * <th>Transaction ID</th>
 * <th>Probability</th>
 * </tr></thead>
 * <tbody>
 * <tr>
 * <td>0</td>
 * <td>1</td>
 * <td>0.5</td>
 * </tr>
 * <tr>
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
 */
public class UPList {
  private static final int LINEAR_SEARCH_THRESHOLD = 32;

  private double expectedSupport;
  private double maxSupport;

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

  protected UPList() {
    expectedSupport = 0;
    maxSupport = 0;
    tidList = new IntArrayList();
    probList = new DoubleArrayList();
  }

  /**
   * Constructs a list with the specified tid and probability.
   *
   * @param tid The transaction's id.
   * @param prob The probability of the transaction.
   */
  public UPList(int tid, double prob) {
    expectedSupport = prob;
    maxSupport = prob;
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
   * Increases the capacity of this <tt>UPList</tt> instance, if necessary, to ensure that it can
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
   * Returns the expected support of this list.
   *
   * @return The expected support of this list.
   */
  public double getExpectedSupport() {
    return expectedSupport;
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
   * Returns {@code true} if this list contains no transactions.
   *
   * @return {@code true} if this list contains no transactions.
   */
  public boolean isEmpty() {
    return tidList.isEmpty();
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
   * @param from The index to start searching from.
   * @return The index of the tid, or -1 if not found.
   */
  public int search(int tid, int from) {
    if (from >= tidList.size()) {
      return -1;
    }

    if (tidList.get(from) == tid) {
      return from;
    }

    if (tidList.size() - from <= LINEAR_SEARCH_THRESHOLD) {
      return linearSearchTransationIndex(tid, from);
    } else {
      return binarySearchSearchTransationIndex(tid, from);
    }
  }

  private int linearSearchTransationIndex(int tid, int from) {
    for (int i = from; i < tidList.size(); i++) {
      if (tidList.get(i) == tid) {
        return i;
      }
    }

    return -1;
  }

  private int binarySearchSearchTransationIndex(int tid, int from) {
    int left = from;
    int right = tidList.size() - 1;

    while (left <= right) {
      int mid = left + (right - left) / 2;
      int midTid = tidList.get(mid);

      if (midTid > tid) {
        right = mid - 1;
      } else if (midTid < tid) {
        left = mid + 1;
      } else {
        return mid;
      }
    }

    return -1;
  }

  // @Override
  // public String toString() {
  //   if (isEmpty()) {
  //     return String.format(
  //         "([%d], expSup=%.2f, maxSup=%.2f, [])", itemId, expectedSupport, maxSupport);
  //   }

  //   StringBuilder sb =
  //       new StringBuilder(
  //           String.format("([%d], expSup=%.2f, maxSup=%.2f, [", itemId, expectedSupport,
  // maxSupport)
  //               + pairs.get(0).toString());

  //   for (int i = 1; i < pairs.size(); i++) {
  //     sb.append(", " + pairs.get(i).toString());
  //   }

  //   sb.append("])");

  //   return sb.toString();
  // }
}
