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

public class UTDatabase {
  private ImmutableList<ImmutableList<UItem>> transactions;

  private UTDatabase() {}

  private UTDatabase(ImmutableList<ImmutableList<UItem>> transactions) {
    this.transactions = transactions;
  }

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

  public ImmutableList<ImmutableList<UItem>> getTransactions() {
    return transactions;
  }

  public UTDatabase[] split(float splitFactor) {
    int splitAt = (int) (size() * splitFactor);

    return new UTDatabase[] {
      new UTDatabase(transactions.subList(0, splitAt)),
      new UTDatabase(transactions.subList(splitAt, size()))
    };
  }

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
