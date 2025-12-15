package vn.datm.iufpm.util;

import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

/**
 * Conditional uncertain pattern list. This data structure has no incremental capability.
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
 * With item a, we have the {@link UPList}:
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
 *
 * And with b, we have:
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
 * <td>0.4</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>2</td>
 * <td>0.4</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * Therefore, using itemset (a, b) as an example, we have the {@link CUPList}:
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
 * <td>0.2</td>
 * </tr>
 * </tbody>
 * </table>
 */
public class CUPList {
  /** The itemset associated with this class. */
  private UItemSet itemSet;

  /** The maximum support for the itemset of this class. */
  private double maxSupport;

  /**
   * We use 2 parallel primitive array lists for performance, with {@link #tidList} being
   * Transaction ID column, and {@link #probList} being the Probability column.
   */
  private MutableIntList tidList = new IntArrayList();

  /**
   * We use 2 parallel primitive array lists for performance, with {@link #tidList} being
   * Transaction ID column, and {@link #probList} being the Probability column.
   */
  private MutableDoubleList probList = new DoubleArrayList();

  /**
   * Constructs a list with the specified ids.
   *
   * @param id1 The id of the first parent.
   * @param id2 The id of the second parent.
   */
  public CUPList(int id1, int id2) {
    itemSet = new UItemSet(id1, id2);
  }

  /**
   * Constructs a list with the specified pattern and id.
   *
   * @param p The pattern of the first parent.
   * @param id The id of the second parent.
   */
  public CUPList(ImmutableIntSet p, int id) {
    itemSet = new UItemSet(p, id);
  }

  /**
   * Add a transaction with a specified tid and support. Augement maximum support and expected
   * support.
   *
   * @param tid The transaction's id.
   * @param prob The support of the transaction.
   * @return {@code true}
   */
  public void addTransaction(int tid, double prob) {
    itemSet.addToExpectedSupport(prob);

    if (prob > maxSupport) {
      maxSupport = prob;
    }

    tidList.add(tid);
    probList.add(prob);
  }

  /**
   * Returns the itemset associated with this class.
   *
   * @return The itemset associated with this class.
   */
  public UItemSet getItemSet() {
    return itemSet;
  }

  /**
   * Returns the ids within the itemset associated with this class.
   *
   * @return The ids within the itemset associated with this class.
   */
  public ImmutableIntSet getIds() {
    return itemSet.getIds();
  }

  /**
   * Returns the expected support of the itemset associated with this class.
   *
   * @return The expected support of the itemset associated with this class.
   */
  public double getExpectedSupport() {
    return itemSet.getExpectedSupport();
  }

  /**
   * Returns the maximum support of the itemset associated with this class.
   *
   * @return The maximum support of the itemset associated with this class.
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
   * Returns the number of transactions in this class.
   *
   * @return The number of transactions in this class.
   */
  public int size() {
    return tidList.size();
  }
}
