package vn.datm.ituna.util;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class ICUPList extends UPList {
  private final Set<Integer> firstParent;
  private int firstIndex = 0;

  private final int secondParent;
  private int secondIndex = 0;

  public ICUPList(int id1, int id2) {
    firstParent = ImmutableSet.of(id1);
    secondParent = id2;
  }

  public ICUPList(Set<Integer> p, int id) {
    firstParent = p;
    secondParent = id;
  }

  public Set<Integer> getFirstParent() {
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
}
