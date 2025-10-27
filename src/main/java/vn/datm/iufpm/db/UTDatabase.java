package vn.datm.iufpm.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;

/** UTDatabase */
public class UTDatabase {
  /** The transactions recorded a list of lists of UItems */
  private final ImmutableList<ImmutableList<UItem>> transactions;

  private UTDatabase(ImmutableList<ImmutableList<UItem>> transactions) {
    this.transactions = transactions;
  }

  /**
   * Creates a UTDatabase from a file. The dataset format follows {@code itemId
   * itemId:itemProbability itemProbability}. For example:
   *
   * <pre>{@code
   * 10307 10311 12487:0.6609434160 0.3920031788 0.750090239
   * 12559:0.788112943
   * 12695 12703 18715:0.2097929623 0.3395640986 0.625568080
   * }</pre>
   *
   * @param path The path of the file to be parsed.
   * @return The UTDatabase created from the parsed file.
   * @throws IOException There was problem accessing the file.
   */
  public static UTDatabase fromFile(Path path) throws IOException {
    MutableList<ImmutableList<UItem>> bufferedTransations = new FastList<>();

    try (BufferedReader br = Files.newBufferedReader(path)) {
      String row;

      while ((row = br.readLine()) != null) {
        if (row.isEmpty()) continue;

        ImmutableList<UItem> transaction = processTransaction(row);

        if (transaction != null) {
          bufferedTransations.add(transaction);
        }
      }
    }

    return new UTDatabase(bufferedTransations.toImmutable());
  }

  /**
   * Creates a UTDatabase from an input stream. The dataset format follows {@code itemId
   * itemId:itemProbability itemProbability}. For example:
   *
   * <pre>{@code
   * 10307 10311 12487:0.6609434160 0.3920031788 0.750090239
   * 12559:0.788112943
   * 12695 12703 18715:0.2097929623 0.3395640986 0.625568080
   * }</pre>
   *
   * @param is The input stream to be parsed.
   * @return The UTDatabase created from the parsed input stream.
   * @throws IOException There was a problem when accessing the input stream.
   */
  public static UTDatabase fromInputStream(InputStream is) throws IOException {
    MutableList<ImmutableList<UItem>> bufferedTransations = new FastList<>();

    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String row;

      while ((row = br.readLine()) != null) {
        if (row.isEmpty()) continue;

        ImmutableList<UItem> transaction = processTransaction(row);

        if (transaction != null) {
          bufferedTransations.add(transaction);
        }
      }
    }

    return new UTDatabase(bufferedTransations.toImmutable());
  }

  /**
   * Returns an immutable list of lists of transations from this database.
   *
   * @return An immutable list of lists of transations from this database.
   */
  public ImmutableList<ImmutableList<UItem>> getTransactions() {
    return transactions;
  }

  /**
   * Splits this database into 2 according the split factor.
   *
   * @param splitFactor The factor to split this database.
   * @return Two databases with the first being this database splited at the split factor and the
   *     second being the rest of the database.
   */
  public UTDatabase[] split(float splitFactor) {
    int splitAt = (int) (size() * splitFactor);

    return new UTDatabase[] {
      new UTDatabase(transactions.subList(0, splitAt)),
      new UTDatabase(transactions.subList(splitAt, size()))
    };
  }

  /**
   * Returns the number of lists of transactions in this database.
   *
   * @return The number of lists of transactions in this database.
   */
  public int size() {
    return transactions.size();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (ImmutableList<UItem> transaction : transactions) {
      sb.append(transaction.makeString(" "));
      sb.append('\n');
    }

    return sb.toString();
  }

  /**
   * Converts a specified line of the input file or stream into a lists of transactions.
   *
   * @param row A line of the input file or stream
   * @return An immutable list of UItem to append to the database
   */
  private static ImmutableList<UItem> processTransaction(String row) {
    String[] splitRow = row.split(":");
    String[] ids = splitRow[0].split("\\s+");
    String[] probs = splitRow[1].split("\\s+");

    if (ids.length == probs.length) {
      try {
        ImmutableList<UItem> transactions =
            Lists.immutable
                .with(ids)
                .zip(Lists.immutable.with(probs))
                .collect(
                    p -> new UItem(Integer.parseInt(p.getOne()), Double.parseDouble(p.getTwo())));

        return transactions;
      } catch (NumberFormatException e) {
        System.err.println("Unable to process row line: " + row + '.');
        return null;
      }
    } else {
      System.err.println("Unable to process row line: " + row + '.');
      return null;
    }
  }
}
