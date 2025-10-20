package vn.datm.ibuca.util;

public record Segment(int start, int end) {
  @Override
  public final String toString() {
    return '(' + start + ", " + end + ')';
  }
}
