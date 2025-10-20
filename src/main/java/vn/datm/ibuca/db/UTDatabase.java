package vn.datm.ibuca.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UTDatabase {
  private List<ArrayList<UItem>> transactions = new ArrayList<ArrayList<UItem>>();

  private UTDatabase() {}

  private UTDatabase(List<ArrayList<UItem>> transactions) {
    this.transactions = transactions;
  }

  public static UTDatabase fromFile(Path path) throws IOException {
    UTDatabase db = new UTDatabase();

    try (BufferedReader br = Files.newBufferedReader(path)) {
      String row;

      while ((row = br.readLine()) != null) {
        if (row.isEmpty()) continue;

        ArrayList<UItem> transaction = db.processTransaction(row);

        if (transaction != null) {
          db.transactions.add(transaction);
        }
      }
    }

    return db;
  }

  public static UTDatabase fromInputStream(InputStream is) throws IOException {
    UTDatabase db = new UTDatabase();

    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String row;

      while ((row = br.readLine()) != null) {
        if (row.isEmpty()) continue;

        ArrayList<UItem> transaction = db.processTransaction(row);

        if (transaction != null) {
          db.transactions.add(transaction);
        }
      }
    }

    return db;
  }


  public List<ArrayList<UItem>> getTransactions() {
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

    for (List<UItem> transaction : transactions) {
      sb.append(transaction.get(0).toString());

      for (int i = 1; i < transaction.size(); i++) {
        sb.append(" " + transaction.get(i).toString());
      }

      sb.append('\n');
    }

    return sb.toString();
  }

  private ArrayList<UItem> processTransaction(String row) {
    String[] splitRow = row.split(":");
    String[] ids = splitRow[0].split("\\s+");
    String[] probs = splitRow[1].split("\\s+");

    ArrayList<UItem> transaction = new ArrayList<>();

    if (ids.length == probs.length) {
      try {
        for (int i = 0; i < ids.length; i++) {
          int id = Integer.parseInt(ids[i]);
          double prob = Double.parseDouble(probs[i]);

          transaction.add(new UItem(id, prob));
        }
      } catch (NumberFormatException e) {
        System.err.println("Unable to process row line: " + row + '.');
        return null;
      }
    } else {
      System.err.println("Unable to process row line: " + row + '.');
      return null;
    }

    return transaction;
  }
}
