package vn.datm.ituna;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UTDatabase {
  private final List<ArrayList<UItem>> transactions = new ArrayList<ArrayList<UItem>>();

  public void loadFile(String path) throws IOException {
    try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {
      String row;

      while ((row = br.readLine()) != null) {
        if (row.isEmpty()) continue;

        ArrayList<UItem> transaction = processTransaction(row);

        if (transaction != null) {
          transactions.add(transaction);
        }
      }
    }
  }

  public void readInputStream(InputStream is) throws IOException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String row;

      while ((row = br.readLine()) != null) {
        if (row.isEmpty()) continue;

        ArrayList<UItem> transaction = processTransaction(row);

        if (transaction != null) {
          transactions.add(transaction);
        }
      }
    }
  }

  public void readInputStream(InputStream is, int nLines) throws IOException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      String row;
      int iLine = 0;

      while ((row = br.readLine()) != null && ++iLine < nLines) {
        if (row.isEmpty()) continue;

        ArrayList<UItem> transaction = processTransaction(row);

        if (transaction != null) {
          transactions.add(transaction);
        }
      }
    }
  }

  public List<ArrayList<UItem>> getTransactions() {
    return transactions;
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
}
