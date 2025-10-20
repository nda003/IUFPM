package vn.datm.ibuca;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import vn.datm.ibuca.db.UTDatabase;
import vn.datm.ibuca.iufpm.ISUCK;
import vn.datm.ibuca.iufpm.ITUFP;
import vn.datm.ibuca.iufpm.IUFPM;
import vn.datm.ibuca.iufpm.TUFP;
import vn.datm.ibuca.util.UItemSet;

public class App {
  private enum Algorithm {
    ISUCK,
    ITUFP,
    TUFP
  }

  private interface IUFPMFactory {
    public IUFPM create(int k);
  }

  private class TUFPFactory implements IUFPMFactory {

    @Override
    public IUFPM create(int k) {
      return new TUFP(k);
    }

    @Override
    public String toString() {
      return "TUFP";
    }
  }

  private class ITUFPFactory implements IUFPMFactory {
    public IUFPM create(int k) {
      return new ITUFP(k);
    }

    @Override
    public String toString() {
      return "ITUFP";
    }
  }

  private class ISUCKFactory implements IUFPMFactory {
    public IUFPM create(int k) {
      return new ISUCK(k);
    }

    @Override
    public String toString() {
      return "ISUCK";
    }
  }

  @Option(
      name = "-k",
      usage = "top-Ks used for benchmarking seperated by comma",
      metaVar = "N,N,...")
  private String k = "100,200,300,400,500";

  @Option(name = "-m", usage = "number of measurement iterations")
  private int measurement = 10;

  @Option(name = "-w", usage = "number of warmnup iterations")
  private int warmnup = 2;

  @Option(name = "-i", usage = "input dataset")
  private Path path;

  @Option(name = "-a", usage = "algorithm to benchmark")
  private Algorithm algorithm = Algorithm.ISUCK;

  @Option(name = "-h", hidden = true, usage = "print help")
  private boolean help;

  public long timeIUFPM(IUFPM miner) {
    long start = System.nanoTime();
    miner.mine();
    return TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
  }

  public LongList benchmarkIUFPM(IUFPMFactory factory, int k, UTDatabase db) {
    LongArrayList time = new LongArrayList(measurement);

    for (int i = 0; i < measurement + warmnup; i++) {
      if (i < warmnup) {
        IUFPM miner = factory.create(k);
        miner.addDatabase(db);
        miner.mine();
      } else {
        IUFPM miner = factory.create(k);
        miner.addDatabase(db);
        time.add(timeIUFPM(miner));
      }
    }

    return time;
  }

  public LongList benchmarkIUFPM(IUFPMFactory factory, int k, List<UTDatabase> dbs) {
    LongArrayList time = new LongArrayList(measurement);

    for (int i = 0; i < measurement + warmnup; i++) {
      if (i < warmnup) {
        IUFPM miner = factory.create(k);
        for (UTDatabase db : dbs) {
          miner.addDatabase(db);
          timeIUFPM(miner);
        }
      } else {
        IUFPM miner = factory.create(k);

        long totalRuntime = 0;

        for (UTDatabase db : dbs) {
          miner.addDatabase(db);
          totalRuntime += timeIUFPM(miner);
        }

        time.add(totalRuntime);
      }
    }

    return time;
  }

  public int[] parseTopK() throws NumberFormatException {
    return FastList.newListWith(k.split(",")).collectInt(x -> Integer.parseInt(x)).toArray();
  }

  public void doMain(String[] args) {
    CmdLineParser parser = new CmdLineParser(this);

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println("java -jar ibucaBenchmark.jar [options...] arguments...");
      parser.printUsage(System.err);
      return;
    }

    if (help) {
      System.out.println("java -jar ibucaBenchmark.jar [options...] arguments...");
      parser.printUsage(System.out);
      return;
    }

    int[] topKs;

    try {
      topKs = parseTopK();
    } catch (NumberFormatException e) {
      e.printStackTrace();
      System.err.println("Unable to parse -k.");

      return;
    }
    IUFPMFactory factory;

    switch (algorithm) {
      case TUFP:
        factory = new TUFPFactory();
        break;
      case ITUFP:
        factory = new ITUFPFactory();
        break;
      default:
        factory = new ISUCKFactory();
        break;
    }

    try {
      if (Files.isRegularFile(path)) {
        UTDatabase db = UTDatabase.fromFile(path);

        for (Integer k : topKs) {
          LongList times = benchmarkIUFPM(factory, k, db);
          String row =
              String.format(
                  "%s,%d,%.2f,%s", factory.toString(), k, times.average(), times.makeString(" "));
          System.out.println(row);
        }
      } else if (Files.isDirectory(path)) {
        List<UTDatabase> dbs;

        try (Stream<Path> stream = Files.list(path)) {
          dbs =
              stream
                  .filter((f) -> Files.isRegularFile(f))
                  .map(
                      (f) -> {
                        try {
                          return UTDatabase.fromFile(f);
                        } catch (IOException e) {
                          e.printStackTrace();
                          System.err.println("Unable to access " + path);
                          return null;
                        }
                      })
                  .filter(f -> f != null)
                  .toList();
        }

        for (Integer k : topKs) {
          LongList times = benchmarkIUFPM(factory, k, dbs);
          String row =
              String.format(
                  "%s,%d,%.2f,%s", factory.toString(), k, times.average(), times.makeString(" "));
          System.out.println(row);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Unable to access " + path);
    }
  }

  public static void main(String[] args) {
    // new App().doMain(args);

    try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
      UTDatabase db = UTDatabase.fromInputStream(is);

      UTDatabase[] dbs = db.split(0.8f);

      IUFPM miner = new ISUCK(100);
      miner.addDatabase(dbs[0]);
      miner.mine();
      miner.addDatabase(dbs[1]);
      List<UItemSet> topK1 = miner.mine();

      IUFPM miner2 = new ITUFP(100);
      miner2.addDatabase(db);
      List<UItemSet> topK2 = miner2.mine();

      System.out.println(topK1);
      System.out.println(topK2);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
