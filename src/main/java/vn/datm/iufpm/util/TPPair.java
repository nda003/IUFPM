package vn.datm.iufpm.util;

/**
 * A pair of transaction id and probability.
 *
 * @param tid The transaction's id.
 * @param prob The probability of the transaction.
 */
public record TPPair(int tid, double prob) {
  @Override
  public String toString() {
    return "" + tid + "=" + prob;
  }
}
