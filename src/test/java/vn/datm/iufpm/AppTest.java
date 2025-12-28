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

/** Unit test for simple App. */
public class AppTest {
  static final int K = 100;

  @Test
  public void testISUCKSplit() {
    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
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
              false,
              "Results between ISUCK splits are inconsistent at "
                  + topK1.get(i)
                  + " and "
                  + topK2.get(i)
                  + '.');

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
              false,
              "Results between ITUFP splits are inconsistent at "
                  + topK1.get(i)
                  + " and "
                  + topK2.get(i)
                  + '.');
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
              false,
              "Results between TUFP splits are inconsistent at "
                  + topK1.get(i)
                  + " and "
                  + topK2.get(i)
                  + '.');

          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    assertTrue(true, "Results between TUFP splits were consistent.");
  }

  public void compareITUFPAndISUCK() {
    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      UTDatabase db = UTDatabase.fromInputStream(is);

      UTDatabase[] dbs = db.split(0.8f);

      IUFPM isuck = new ISUCK(K);
      isuck.addDatabase(dbs[0]);
      isuck.mine();
      isuck.addDatabase(dbs[1]);
      List<UItemSet> topK1 = isuck.mine();

      IUFPM itufp = new ITUFP(K);
      itufp.addDatabase(dbs[0]);
      itufp.mine();
      itufp.addDatabase(dbs[1]);
      List<UItemSet> topK2 = itufp.mine();

      for (int i = 0; i < K; i++) {
        if (!topK1.get(i).equals(topK2.get(i))) {
          assertTrue(
              false,
              "Inconsistent results between ISUCK and ITUFP at "
                  + topK1.get(i)
                  + " and "
                  + topK2.get(i)
                  + '.');

          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    assertTrue(true, "Results between ISUCK and ITUFP are consistent.");
  }

  public void compareTUFPAndISUCK() {
    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      UTDatabase db = UTDatabase.fromInputStream(is);

      UTDatabase[] dbs = db.split(0.8f);

      IUFPM isuck = new ISUCK(K);
      isuck.addDatabase(dbs[0]);
      isuck.mine();
      isuck.addDatabase(dbs[1]);
      List<UItemSet> topK1 = isuck.mine();

      IUFPM tufp = new TUFP(K);
      tufp.addDatabase(db);
      List<UItemSet> topK2 = tufp.mine();

      for (int i = 0; i < K; i++) {
        if (!topK1.get(i).equals(topK2.get(i))) {
          assertTrue(
              false,
              "Inconsistent results between ISUCK and ITUFP at "
                  + topK1.get(i)
                  + " and "
                  + topK2.get(i)
                  + '.');

          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    assertTrue(true, "Results between ISUCK and ITUFP are consistent.");
  }

  public void compareTUFPAndITUFP() {
    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      UTDatabase db = UTDatabase.fromInputStream(is);

      UTDatabase[] dbs = db.split(0.8f);

      IUFPM itufp = new ITUFP(K);
      itufp.addDatabase(dbs[0]);
      itufp.mine();
      itufp.addDatabase(dbs[1]);
      List<UItemSet> topK1 = itufp.mine();

      IUFPM tufp = new TUFP(K);
      tufp.addDatabase(db);
      List<UItemSet> topK2 = tufp.mine();

      for (int i = 0; i < K; i++) {
        if (!topK1.get(i).equals(topK2.get(i))) {
          assertTrue(
              false,
              "Inconsistent results between ISUCK and ITUFP at "
                  + topK1.get(i)
                  + " and "
                  + topK2.get(i)
                  + '.');

          return;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    assertTrue(true, "Results between ISUCK and ITUFP are consistent.");
  }
}
