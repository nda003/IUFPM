package vn.datm.iufpm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.datm.iufpm.db.UTDatabase;
import vn.datm.iufpm.lib.ISUCK;
import vn.datm.iufpm.lib.ITUFP;
import vn.datm.iufpm.lib.IUFPM;
import vn.datm.iufpm.lib.TUFP;
import vn.datm.iufpm.util.UItemSet;

public class AppTest {
  @Test
  public void testISUCKSplit() {
    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      int K = 50;

      UTDatabase db = UTDatabase.fromInputStream(is);

      UTDatabase[] dbs = db.split(0.8f);

      IUFPM miner = new ISUCK(K);
      miner.addDatabase(dbs[0]);
      miner.mine();
      miner.addDatabase(dbs[1]);
      List<UItemSet> topK1 = miner.mine();

      IUFPM miner2 = new ISUCK(K);
      miner2.addDatabase(db);
      List<UItemSet> topK2 = miner2.mine();

      for (int i = 0; i < K; i++) {
        if (!topK1.get(i).equals(topK2.get(i))) {
          assertTrue(
              false, "Results between ISUCK splits are inconsistent at " + topK1.get(i) + " and " + topK2.get(i) + '.');

          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    assertTrue(true, "Results between ISUCK splits are consistent.");
  }

  @Test
  public void testITUFPSplit() {
    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      int K = 50;

      UTDatabase db = UTDatabase.fromInputStream(is);

      UTDatabase[] dbs = db.split(0.8f);

      IUFPM miner = new ITUFP(K);
      miner.addDatabase(dbs[0]);
      miner.mine();
      miner.addDatabase(dbs[1]);
      List<UItemSet> topK1 = miner.mine();

      IUFPM miner2 = new ITUFP(K);
      miner2.addDatabase(db);
      List<UItemSet> topK2 = miner2.mine();

      for (int i = 0; i < K; i++) {
        if (!topK1.get(i).equals(topK2.get(i))) {
          assertTrue(
              false, "Results between ITUFP splits are inconsistent at " + topK1.get(i) + " and " + topK2.get(i) + '.');

          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    assertTrue(true, "Results between ITUFP splits were consistent.");
  }

  @Test
  public void testTUFPSplit() {
    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      int K = 50;

      UTDatabase db = UTDatabase.fromInputStream(is);

      UTDatabase[] dbs = db.split(0.8f);

      IUFPM miner = new TUFP(K);
      miner.addDatabase(dbs[0]);
      miner.mine();
      miner.addDatabase(dbs[1]);
      List<UItemSet> topK1 = miner.mine();

      IUFPM miner2 = new TUFP(K);
      miner2.addDatabase(db);
      List<UItemSet> topK2 = miner2.mine();

      for (int i = 0; i < K; i++) {
        if (!topK1.get(i).equals(topK2.get(i))) {
          assertTrue(
              false, "Results between TUFP splits are inconsistent at " + topK1.get(i) + " and " + topK2.get(i) + '.');

          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    assertTrue(true, "Results between TUFP splits were consistent.");
  }
}
