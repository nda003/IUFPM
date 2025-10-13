package vn.datm.ituna;

import java.util.List;
import java.util.LongSummaryStatistics;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/** Hello world! */
public class App {
  public static long testITUFP(IUFPM miner, UTDatabase db) {
    miner.addDatabase(db);
    long start = System.nanoTime();
    miner.mine();
    // System.out.println(topK);
    return TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
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
    // LongSummaryStatistics stats1 = new LongSummaryStatistics();
    // LongSummaryStatistics stats2 = new LongSummaryStatistics();

    // for (int i = 0; i < 51; i++) {
    //   if (i == 0) {
    //     ITUNA iTUFP = new ITUNA(k);
    //     testITUFP(iTUFP, dbs[0]);
    //     testITUFP(iTUFP, dbs[1]);
    //   } else {
    //     ITUNA iTUFP = new ITUNA(k);
    //     stats1.accept(testITUFP(iTUFP, dbs[0]));
    //     stats2.accept(testITUFP(iTUFP, dbs[1]));
    //   }
    // }

    // System.out.println(stats1.getAverage());
    // System.out.println(stats2.getAverage());
    
    IUFPM miner = new ITUNA(k);
    miner.addDatabase(dbs[0]);
    miner.mine();
    miner.addDatabase(dbs[1]);
    List<UItemSet> topK1 = miner.mine();

    IUFPM miner2 = new ITUNA(k);
    miner2.addDatabase(db);
    List<UItemSet> topK2 = miner2.mine();

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
