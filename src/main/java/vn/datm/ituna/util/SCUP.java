package vn.datm.ituna.util;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SCUP {
  private List<int[]> inSeg = new ArrayList<>();
  private double maxSupport;
  private double expectedSupport;
  private int size;

  private final Set<Integer> firstParent;
  private int firstIndex = 0;

  private final int secondParent;
  private int secondIndex = 0;

  public SCUP(int id1, int id2) {
    firstParent = ImmutableSet.of(id1);
    secondParent = id2;
  }

  public SCUP(Set<Integer> p, int id) {
    firstParent = p;
    secondParent = id;
  }

  public List<int[]> getIncrementSegments() {
    return inSeg;
  }

  public int[] getLastSegment() {
    return inSeg.get(inSeg.size() - 1);
  }

  public void accept(double support) {
    maxSupport = Math.max(support, maxSupport);
    expectedSupport += support;
  }

  public void addPos(int increment, int start, int end) {
    size += end - start + 1;
    inSeg.add(new int[] {increment, start, end});
  }

  public double getMaxSupport() {
    return maxSupport;
  }

  public double getExpectedSupport() {
    return expectedSupport;
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

  public int size() {
    return size;
  }

  @Override
  public String toString() {
    return String.format(
        "(expectedSupport=%.2f, maxSuppor%.2f, parents={%s:%d, %d:%d})",
        expectedSupport, maxSupport, firstParent.toString(), firstIndex, secondParent, secondIndex);
  }
}
