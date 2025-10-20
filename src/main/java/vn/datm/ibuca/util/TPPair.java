package vn.datm.ibuca.util;

public record TPPair(int tid, double prob) {
  @Override
  public String toString() {
    return "" + tid + "=" + prob;
  }
}
