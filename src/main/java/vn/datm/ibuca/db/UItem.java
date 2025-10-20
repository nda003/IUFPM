package vn.datm.ibuca.db;

public record UItem(int id, double prob) {
  // private final int id;
  // private final double probability;

  // public UItem(int id, double probability) {
  //   this.id = id;
  //   this.probability = probability;
  // }

  // public int getId() {
  //   return id;
  // }

  // public double getProbability() {
  //   return probability;
  // }

  // @Override
  // public boolean equals(Object obj) {
  //   UItem item = (UItem) obj;

  //   return item.getId() == this.getId();
  // }

  // @Override
  // public int hashCode() {
  //   return String.valueOf(this.getId()).hashCode();
  // }

  @Override
  public String toString() {
    return this.id() + ":" + this.prob();
  }
}
