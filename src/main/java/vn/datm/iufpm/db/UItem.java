package vn.datm.iufpm.db;

/**
 * UItem
 * @param id The id of the item
 * @param prob The probablity of the item
 */
public record UItem(int id, double prob) {
  @Override
  public String toString() {
    return this.id() + ":" + this.prob();
  }
}
