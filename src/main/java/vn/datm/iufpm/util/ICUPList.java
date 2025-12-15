package vn.datm.iufpm.util;

import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

/**
 * Incremental conditional uncertain pattern list. This data structure has incremental capability by
 * storing the last mined indexes of the parent {@link UPList}s and only joining the {@link UPList}s
 * from the last indexes.
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
 * Therefore, using itemset (a, b) as an example, we have the {@link ICUPList}:
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
 *
 * The last indexes are stored as such:
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
public class ICUPList extends UPList {
  private final ImmutableIntSet firstParent;
  private int firstIndex = 0;

  private final int secondParent;
  private int secondIndex = 0;

  /**
   * Constructs a list with the specified ids.
   *
   * @param id1 The id of the first parent.
   * @param id2 The id of the second parent.
   */
  public ICUPList(int id1, int id2) {
    firstParent = IntSets.immutable.of(id1);
    secondParent = id2;
  }

  /**
   * Constructs a list with the specified ids and initial capacity.
   *
   * @param id1 The id of the first parent.
   * @param id2 The id of the second parent.
   * @param initialCapacity The initial capacity of the list.
   */
  public ICUPList(int id1, int id2, int initialCapacity) {
    firstParent = IntSets.immutable.of(id1);
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
  public ICUPList(ImmutableIntSet p, int id) {
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
  public ICUPList(ImmutableIntSet p, int id, int initialCapacity) {
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
}
