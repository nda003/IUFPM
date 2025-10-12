package vn.datm.ituna;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Hello world! */
public class App {
  public static void testITUNA(ITUNA iTUNA, UTDatabase db) {
    iTUNA.addDatabase(db);
    long start = System.nanoTime();
    List<UItemSet> topK = iTUNA.mine();
    System.out.println(
        TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));
    System.out.println(topK);
  }

  public static void main(String[] args) {
    UTDatabase db = new UTDatabase();

    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      db.readInputStream(is);
    } catch (IOException e) {
      e.printStackTrace();
    }

    int k = 100;

    UTDatabase[] dbs = db.split(0.8f);

    ITUFP iTUFP = new ITUFP(k);
    // testITUFP(ITUFP, dbs[0]);
    // testITUFP(ITUFP, dbs[1]);
    iTUFP.addDatabase(dbs[0]);
    iTUFP.mine();
    iTUFP.addDatabase(dbs[1]);
    List<UItemSet> topK1 = iTUFP.mine();

    ITUFP iTUFP2 = new ITUFP(k);
    iTUFP2.addDatabase(db);
    List<UItemSet> topK2 = iTUFP2.mine();

    for (int i = 0; i < k; i++) {
      if (!topK1.get(i).getIds().equals(topK2.get(i).getIds())) {
        System.out.println(topK1.get(i));
        System.out.println(topK2.get(i));
        break;
      }
    }
    System.out.println(topK1);
    System.out.println(topK2);

    // System.out.println(iTUNA.getTopK().length);
    // testITUNA(iTUNA, db);
    // iTUNA.debug();
    // System.out.println(iTUNA.getTopK().size());
  }
}
