package vn.datm.iufpm.util;

import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public class ISCUPList extends ISUPList {
  private final ImmutableIntSet firstParent;
  private int firstIndex = 0;

  private final int secondParent;
  private int secondIndex = 0;

  public ISCUPList(int id1, int id2) {
    firstParent = IntSets.immutable.with(id1);
    secondParent = id2;
  }

  public ISCUPList(int id1, int id2, int initialCapacity) {
    firstParent = IntSets.immutable.with(id1);
    secondParent = id2;
    tidList = new IntArrayList(initialCapacity);
    probList = new DoubleArrayList(initialCapacity);
  }

  public ISCUPList(ImmutableIntSet p, int id) {
    firstParent = p;
    secondParent = id;
  }

  public ISCUPList(ImmutableIntSet p, int id, int initialCapacity) {
    firstParent = p;
    secondParent = id;
    tidList = new IntArrayList(initialCapacity);
    probList = new DoubleArrayList(initialCapacity);
  }

  public ImmutableIntSet getFirstParent() {
    return firstParent;
  }

  public int getFirstIndex() {
    return firstIndex;
  }

  public void setFirstIndex(int firstIndex) {
    this.firstIndex = firstIndex;
  }

  public int getSecondParent() {
    return secondParent;
  }

  public int getSecondIndex() {
    return secondIndex;
  }

  public void setSecondIndex(int secondIndex) {
    this.secondIndex = secondIndex;
  }

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
