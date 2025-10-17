package vn.datm.ituna;

import com.google.common.collect.ImmutableList;
import com.google.common.math.StatsAccumulator;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.openjdk.jol.info.GraphLayout;
import vn.datm.ituna.db.UTDatabase;
import vn.datm.ituna.iufpm.IBUCA;
import vn.datm.ituna.iufpm.ITUFP;
import vn.datm.ituna.iufpm.IUFPM;

public class App {
  private static interface IUFPMFactory {
    public IUFPM create(int k);
  }

  private static class ITUFPFactory implements IUFPMFactory {
    public IUFPM create(int k) {
      return new ITUFP(k);
    }

    @Override
    public String toString() {
      return "ITUFP";
    }
  }

  private static class IBUCAFactory implements IUFPMFactory {
    public IUFPM create(int k) {
      return new IBUCA(k);
    }

    @Override
    public String toString() {
      return "IBUCA";
    }
  }

  public static long timeIUFPM(IUFPM miner) {
    long start = System.nanoTime();
    miner.mine();
    return TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
  }

  public static StatsAccumulator benchmarkIUFPM(
      IUFPMFactory factory, int k, UTDatabase db, int warmnups, int measurements) {
    StatsAccumulator stats = new StatsAccumulator();

    for (int i = 0; i < measurements + warmnups; i++) {
      if (i < warmnups) {
        IUFPM miner = factory.create(k);
        miner.addDatabase(db);
        miner.mine();
      } else {
        IUFPM miner = factory.create(k);
        miner.addDatabase(db);
        stats.add(timeIUFPM(miner));
        // System.out.println(GraphLayout.parseInstance(miner).totalSize() / Math.pow(1024, 2));
      }
    }

    return stats;
  }

  public static StatsAccumulator benchmarkIUFPM(
      IUFPMFactory factory,
      int k,
      UTDatabase dbs[],
      int sumFromDb,
      int warmnups,
      int measurements) {
    StatsAccumulator stats = new StatsAccumulator();

    for (int i = 0; i < measurements + warmnups; i++) {
      if (i < warmnups) {
        IUFPM miner = factory.create(k);
        for (UTDatabase db : dbs) {
          miner.addDatabase(db);
          timeIUFPM(miner);
        }
      } else {
        IUFPM miner = factory.create(k);
        long totalSum = 0;

        for (int j = 0; j < dbs.length; j++) {
          if (j < sumFromDb) {
            miner.addDatabase(dbs[j]);
            miner.mine();
          } else {
            miner.addDatabase(dbs[j]);
            totalSum += timeIUFPM(miner);
          }
        }
        stats.add(totalSum);
      }

      // System.out.println(GraphLayout.parseInstance(miner).totalSize() / Math.pow(1024, 2));
    }

    return stats;
  }

  public static void main(String[] args) {
    UTDatabase db = new UTDatabase();

    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      db.readInputStream(is);
    } catch (IOException e) {
      e.printStackTrace();
    }

    UTDatabase[] dbs = db.split(0.5f);
    StringBuilder sb = new StringBuilder();

    for (IUFPMFactory factory : ImmutableList.of(new ITUFPFactory(), new IBUCAFactory())) {
      StatsAccumulator stats = benchmarkIUFPM(factory, 100, db, 2, 50);
      String row =
          String.format(
              "%s,%.5f,%5f\n",
              factory.toString(), stats.mean(), stats.populationStandardDeviation());
      System.out.print(row);
      sb.append(row);
    }

    System.out.println(sb.toString());

    // System.out.println(stats1.getAverage());
    // System.out.println(stats2.getAverage());

    // IUFPM miner = new IBUM(100);
    // miner.addDatabase(dbs[0]);
    // miner.mine();
    // miner.addDatabase(dbs[1]);
    // List<UItemSet> topK1 = miner.mine();

    // IUFPM miner2 = new IBUM(100);
    // miner2.addDatabase(db);
    // List<UItemSet> topK2 = miner2.mine();

    // for (int i = 0; i < 31; i++) {
    //   // if (i == 0) {
    //   //   IUFPM miner = new IBUM(k);
    //   //   miner.addDatabase(db);
    //   //   testITUFP(miner);
    //   // }
    //   System.gc();
    //   IUFPM miner = new IBUM(k);
    //   miner.addDatabase(db);
    //   stats1.add(testITUFP(miner));
    //   System.out.println(ClassLayout.parseInstance(miner).instanceSize() << 10);
    // }

    // System.out.println(stats1.mean());
    // System.out.println(stats1.populationStandardDeviation());

    // for (int i = 0; i < 100; i++) {
    //   if (!topK1.get(i).getIds().equals(topK2.get(i).getIds())) {
    //     System.out.println(topK1.get(i));
    //     System.out.println(topK2.get(i));
    //     break;
    //   }
    // }
    // System.out.println(topK1);
    // System.out.println(topK2);

    // System.out.println(iTUNA.getTopK().length);
    // testITUNA(iTUNA, db);
    // iTUNA.debug();
    // System.out.println(iTUNA.getTopK().size());
  }
}
