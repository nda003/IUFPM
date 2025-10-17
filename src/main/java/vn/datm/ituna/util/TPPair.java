package vn.datm.ituna.util;

public record TPPair(int tid, double prob) {
  @Override
  public String toString() {
    return "" + tid + "=" + prob;
  }
}
