package vn.datm.ibuca;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.math.StatsAccumulator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import vn.datm.ibuca.db.UTDatabase;
import vn.datm.ibuca.iufpm.IBUCA;
import vn.datm.ibuca.iufpm.ITUFP;
import vn.datm.ibuca.iufpm.IUFPM;
import vn.datm.ibuca.iufpm.TUFP;

public class App {
  private enum Algorithm {
    IBUCA,
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

  private class IBUCAFactory implements IUFPMFactory {
    public IUFPM create(int k) {
      return new IBUCA(k);
    }

    @Override
    public String toString() {
      return "IBUCA";
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
  private Algorithm algorithm = Algorithm.IBUCA;

  @Option(name = "-h", hidden = true, usage = "print help")
  private boolean help;

  public long timeIUFPM(IUFPM miner) {
    long start = System.nanoTime();
    miner.mine();
    return System.nanoTime() - start;
  }

  public StatsAccumulator benchmarkIUFPM(IUFPMFactory factory, int k, UTDatabase db) {
    StatsAccumulator stats = new StatsAccumulator();

    for (int i = 0; i < measurement + warmnup; i++) {
      if (i < warmnup) {
        IUFPM miner = factory.create(k);
        miner.addDatabase(db);
        miner.mine();
      } else {
        IUFPM miner = factory.create(k);
        miner.addDatabase(db);
        stats.add(timeIUFPM(miner));
      }
    }

    return stats;
  }

  public StatsAccumulator benchmarkIUFPM(IUFPMFactory factory, int k, List<UTDatabase> dbs) {
    StatsAccumulator stats = new StatsAccumulator();

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

        stats.add(totalRuntime);
      }
    }

    return stats;
  }

  public Integer[] parseTopK() throws NumberFormatException {
    return Collections2.transform(List.of(k.split(",")), (x) -> Integer.parseInt(x))
        .toArray(Integer[]::new);
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

    Integer[] topKs;

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
        factory = new IBUCAFactory();
        break;
    }

    try {
      if (Files.isRegularFile(path)) {
        UTDatabase db = UTDatabase.fromFile(path);

        for (Integer k : topKs) {
          StatsAccumulator stats = benchmarkIUFPM(factory, k, db);
          String row =
              String.format(
                  "%s,%d,%.5f,%5f",
                  factory.toString(), k, stats.mean(), stats.populationStandardDeviation());
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
                  .collect(ImmutableList.toImmutableList());
        }

        for (Integer k : topKs) {
          StatsAccumulator stats = benchmarkIUFPM(factory, k, dbs);
          String row =
              String.format(
                  "%s,%d,%d,%d",
                  factory.toString(),
                  k,
                  TimeUnit.MILLISECONDS.convert((long) stats.mean(), TimeUnit.NANOSECONDS),
                  TimeUnit.MILLISECONDS.convert(
                      (long) stats.populationStandardDeviation(), TimeUnit.NANOSECONDS));
          System.out.println(row);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Unable to access " + path);
    }
  }

  public static void main(String[] args) {
    new App().doMain(args);

    // try (InputStream is = App.class.getResourceAsStream("/contextMushroom.txt")) {
    //   UTDatabase db = UTDatabase.fromInputStream(is);

    //   UTDatabase[] dbs = db.split(0.8f);

    //   IUFPM miner = new IBUCA(100);
    //   miner.addDatabase(dbs[0]);
    //   miner.mine();
    //   miner.addDatabase(dbs[1]);
    //   List<UItemSet> topK1 = miner.mine();

    //   IUFPM miner2 = new ITUFP(100);
    //   miner2.addDatabase(db);
    //   List<UItemSet> topK2 = miner2.mine();

    //   System.out.println(topK1);
    //   System.out.println(topK2);
    // } catch (IOException e) {
    //   e.printStackTrace();
    // }
  }
}
