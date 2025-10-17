package vn.datm.ituna.util;

public record Segment(int start, int end) {
  @Override
  public final String toString() {
    return '(' + start + ", " + end + ')';
  }
}
