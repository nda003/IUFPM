package vn.datm.ibuca.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class SUP {
  private LinkedHashMap<Integer, int[]> inSeg = LinkedHashMap.newLinkedHashMap(1);
  private double maxSupport;
  private double expectedSupport;
  private int size = 0;

  protected SUP() {
    maxSupport = 0;
    expectedSupport = 0;
  }

  public SUP(double support) {
    maxSupport = support;
    expectedSupport = support;
  }

  public LinkedHashMap<Integer, int[]> getIncrementSegments() {
    return inSeg;
  }

  public boolean containIncrement(int increment) {
    return inSeg.containsKey(increment);
  }

  public int[] getSegmentAt(int increment) {
    return inSeg.get(increment);
  }

  public int incrementSize() {
    return inSeg.size();
  }

  public Map.Entry<Integer, int[]> getLastSegment() {
    return inSeg.lastEntry();
  }

  public void addPos(int increment, int start, int end) {
    size += end - start + 1;
    inSeg.put(increment, new int[] {start, end});
  }

  public void accept(double support) {
    maxSupport = Math.max(support, maxSupport);
    expectedSupport += support;
  }

  public double getMaxSupport() {
    return maxSupport;
  }

  public double getExpectedSupport() {
    return expectedSupport;
  }

  public int size() {
    return size;
  }

  @Override
  public String toString() {
    StringBuilder sb =
        new StringBuilder(
            String.format(
                "(expectedSupport=%.2f, maxSupport=%.2f, %s)",
                expectedSupport, maxSupport, inSeg.toString()));

    return sb.toString();
  }
}
