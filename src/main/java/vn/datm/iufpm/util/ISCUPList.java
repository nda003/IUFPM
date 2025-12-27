package vn.datm.iufpm.util;

import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

/**
 * Incrementally segmented conditional uncertain pattern list. This data structure has incremental
 * capability.
 *
 * <p>An increment is defined as the added database to which a transaction belongs to. For example:
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
 * can extend the original {@link ISUPList} through segmentation using increment. By incrementally
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
 * And with b, we have:
 *
 * <table><thead>
 * <tr>
 * <th>Increment</th>
 * <th>Index</th>
 * <th>Transaction ID</th>
 * <th>Items</th>
 * </tr></thead>
 * <tbody>
 * <tr>
 * <td rowspan="2">1</td>
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
 * Using itemset (a, b) as example:
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
 * <td>0.2</td>
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
 * </tbody>
 * </table>
 *
 * Start and end are inclusive.
 *
 * <p>The last indexes are stored as such:
 *
 * <table><thead>
 * <tr>
 * <th>Parent</th>
 * <th>Last index</th>
 * </tr></thead>
 * <tbody>
 * <tr>
 * <td>a</td>
 * <td>3</td>
 * </tr>
 * <tr>
 * <td>b</td>
 * <td>2</td>
 * </tr>
 * </tbody>
 * </table>
 */
public class ISCUPList extends ISUPList {
  /**
   * The first item or itemset that was used to mine the {@link ICUPList}. If an item was used to
   * mine, the first parent would be a set of size 1.
   */
  private final ImmutableIntSet firstParent;

  /**
   * The last index of the first parent's {@link UPList} or {@link CUPList} that was used to mine
   * the {@link ICUPList}. If the first parent's {@link UPList} or {@link CUPList} was changed due
   * to an increment in the database, the {@link ICUPList} is mined from this index of the {@link
   * UPList} or {@link CUPList} and will be updated accordingly once the incremental mining is done.
   */
  private int firstIndex = 0;

  /**
   * The second item that was used to mine the {@link ICUPList}. Due to how ITUFP mines and grows,
   * the second parent will always be an item.
   */
  private final int secondParent;

  /**
   * The last index of the second parent's {@link UPList} that was used to mine the {@link
   * ICUPList}. If the second parent's {@link UPList} was changed due to an increment in the
   * database, the {@link ICUPList} is mined from this index of the {@link UPList} and will be
   * updated accordingly once the incremental mining is done.
   */
  private int secondIndex = 0;

  /**
   * Constructs a list with the specified ids.
   *
   * @param id1 The id of the first parent.
   * @param id2 The id of the second parent.
   */
  public ISCUPList(int id1, int id2) {
    firstParent = IntSets.immutable.with(id1);
    secondParent = id2;
  }

  /**
   * Constructs a list with the specified ids and initial capacity.
   *
   * @param id1 The id of the first parent.
   * @param id2 The id of the second parent.
   * @param initialCapacity The initial capacity of the list.
   */
  public ISCUPList(int id1, int id2, int initialCapacity) {
    firstParent = IntSets.immutable.with(id1);
    secondParent = id2;
    tidList = new IntArrayList(initialCapacity);
    probList = new DoubleArrayList(initialCapacity);
  }

  /**
   * Constructs a list with the specified pattern and id.
   *
   * @param p The pattern of the first parent.
   * @param id The id of the second parent.
   */
  public ISCUPList(ImmutableIntSet p, int id) {
    firstParent = p;
    secondParent = id;
  }

  /**
   * Constructs a list with the specified pattern, id and initial capacity.
   *
   * @param p The pattern of the first parent.
   * @param id The id of the second parent.
   * @param initialCapacity The initial capacity of the list.
   */
  public ISCUPList(ImmutableIntSet p, int id, int initialCapacity) {
    firstParent = p;
    secondParent = id;
    tidList = new IntArrayList(initialCapacity);
    probList = new DoubleArrayList(initialCapacity);
  }

  /**
   * Returns the first parent of this list.
   *
   * @return The first parent of this list.
   */
  public ImmutableIntSet getFirstParent() {
    return firstParent;
  }

  /**
   * Returns the index of the first parent.
   *
   * @return The index of the first parent.
   */
  public int getFirstIndex() {
    return firstIndex;
  }

  /**
   * Sets the index of the first parent of this list.
   *
   * @param firstIndex The new index for the first parent.
   */
  public void setFirstIndex(int firstIndex) {
    this.firstIndex = firstIndex;
  }

  /**
   * Returns the second parent of this list.
   *
   * @return The second parent of this list.
   */
  public int getSecondParent() {
    return secondParent;
  }

  /**
   * Returns the index of the second parent.
   *
   * @return The index of the second parent.
   */
  public int getSecondIndex() {
    return secondIndex;
  }

  /**
   * Sets the index of the second parent of this list.
   *
   * @param firstIndex The new index for the second parent.
   */
  public void setSecondIndex(int secondIndex) {
    this.secondIndex = secondIndex;
  }

  /**
   * Puts the increment into the list and segment it. If the increment already exists, it will
   * update the end of the segment to the current size of the list.
   *
   * @param increment The increment to put into the list.
   */
  @Override
  public void putIncrement(int increment) {
    if (incSeg.containsKey(increment)) {
      incSeg.get(increment)[1] = tidList.size() - 1;
    } else {
      incSeg.put(increment, new int[] {pairsPreviousSize, tidList.size() - 1});
    }

    pairsPreviousSize = tidList.size();
  }
}
