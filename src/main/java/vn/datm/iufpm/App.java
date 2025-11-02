package vn.datm.iufpm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import vn.datm.iufpm.db.UTDatabase;
import vn.datm.iufpm.lib.ISUCK;
import vn.datm.iufpm.lib.ITUFP;
import vn.datm.iufpm.lib.IUFPM;
import vn.datm.iufpm.lib.TUFP;

/** App */
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

  // @Option(name = "-i", usage = "input dataset")
  // private Path dataset;

  @Option(name = "-a", usage = "algorithm to benchmark")
  private Algorithm algorithm = Algorithm.ISUCK;

  @Option(name = "-v", usage = "verbose output")
  private boolean verbose = false;

  @Option(name = "-h", hidden = true, usage = "print help")
  private boolean help = false;

  @Argument(usage = "the path to the input uncertain dataset", metaVar = "PATH")
  private List<String> arguements = new ArrayList<>();

  /**
   * @param miner The IUFPM algorithm to time
   * @return The time it took to run {@code miner.mine()} in milliseconds
   */
  public long timeIUFPM(IUFPM miner) {
    long start = System.nanoTime();
    miner.mine();
    return TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
  }

  /**
   * @param factory The IUFPMFactory to create a IUFPM algorithm from.
   * @param k The top-K value to use for measurements
   * @param db The database to measure against
   * @return A list of times in milliseconds from each measurement
   */
  public LongList benchmarkIUFPM(IUFPMFactory factory, int k, UTDatabase db) {
    MutableLongList time = new LongArrayList(measurement);

    if (verbose) {
      System.out.println(
          String.format(
              "Benchmarking %s at k=%d against a database of size %d for %d warmup and %d"
                  + " measurement iterations:",
              factory.toString(), k, db.size(), warmnup, measurement));
    }

    for (int i = 0; i < measurement + warmnup; i++) {
      if (i < warmnup) {
        IUFPM miner = factory.create(k);
        miner.addDatabase(db);

        long t = timeIUFPM(miner);

        if (verbose) {
          System.out.println(String.format("Warmup iteration %d took: %d", i + 1, t));
        }
      } else {
        IUFPM miner = factory.create(k);
        miner.addDatabase(db);
        time.add(timeIUFPM(miner));

        if (verbose) {
          System.out.println(
              String.format("Measurement iteration %d took: %d", i - warmnup + 1, time.getLast()));
        }
      }

      System.gc();
    }

    return time;
  }

  /**
   * @param factory The IUFPMFactory to create a IUFPM algorithm from.
   * @param k The top-K value to use for measurements
   * @param dbs Each increment of a database to measure against
   * @return A list of times of length {@code measurement * dbs.size()} in milliseconds from each
   *     increment within a measurement
   */
  public LongList benchmarkIUFPM(IUFPMFactory factory, int k, List<UTDatabase> dbs) {
    MutableLongList time = new LongArrayList(measurement);

    if (verbose) {
      System.out.println(
          String.format(
              "Benchmarking %s at k=%d against %d databases for %d warmup and %d measurement"
                  + " iterations:",
              factory.toString(), k, dbs.size(), warmnup, measurement));
    }

    for (int i = 0; i < measurement + warmnup; i++) {
      if (i < warmnup) {
        IUFPM miner = factory.create(k);

        miner.addDatabase(dbs.get(0));

        long t = timeIUFPM(miner);

        if (verbose) {
          System.out.println(
              String.format(
                  "Warmnup iteration %d against a database of size %d took: %d",
                  i + 1, dbs.get(0).size(), t));
        }

        for (int j = 1; j < dbs.size(); j++) {
          miner.addDatabase(dbs.get(j));
        }

        t = timeIUFPM(miner);

        if (verbose) {
          System.out.println(
              String.format(
                  "Warmnup iteration %d against %d databases took: %d",
                  i + 1, dbs.size(), t));
        }
      } else {
        IUFPM miner = factory.create(k);

        miner.addDatabase(dbs.get(0));
        time.add(timeIUFPM(miner));

        if (verbose) {
          System.out.println(
              String.format(
                  "Measurement iteration %d against a database of size %d took: %d",
                  i - warmnup + 1, dbs.get(0).size(), time.getLast()));
        }

        for (int j = 1; j < dbs.size(); j++) {
          miner.addDatabase(dbs.get(j));
        }

        time.add(timeIUFPM(miner));

        if (verbose) {
          System.out.println(
              String.format(
                  "Measurement iteration %d against %d databases took: %d",
                  i - warmnup + 1, dbs.size(), time.getLast()));
        }
      }

      System.gc();
    }

    return time;
  }

  /**
   * @return An array of integers parsed from {@code -k}
   * @throws NumberFormatException {@code -k} was passed an illegal arguement
   */
  public IntList parseTopK() throws NumberFormatException {
    return FastList.newListWith(k.split(",")).collectInt(x -> Integer.parseInt(x));
  }

  /**
   * Handles CLI parsing and logics
   *
   * @param Arguements passed from {@link #main(String[])}
   */
  public void doMain(String[] args) {
    CmdLineParser parser = new CmdLineParser(this);

    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println("java -jar iufpmBenchmark.jar [options...] arguments...");
      parser.printUsage(System.err);
      return;
    }

    if (help) {
      System.out.println("java -jar iufpmBenchmark.jar [options...] arguments...");
      parser.printUsage(System.out);
      return;
    }

    IntList topKs;

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

    Path dataset = Paths.get(arguements.getFirst());

    try {
      if (Files.isRegularFile(dataset)) {
        UTDatabase db = UTDatabase.fromFile(dataset);

        topKs.each(
            (k) -> {
              LongList times = benchmarkIUFPM(factory, k, db);
              String row =
                  String.format(
                      "%s,%d,%.2f,%s",
                      factory.toString(), k, times.average(), times.makeString(" "));
              System.out.println(row);
            });
      } else if (Files.isDirectory(dataset)) {
        List<UTDatabase> dbs;

        try (Stream<Path> stream = Files.list(dataset)) {
          dbs =
              stream
                  .filter((f) -> Files.isRegularFile(f))
                  .map(
                      (f) -> {
                        try {
                          return UTDatabase.fromFile(f);
                        } catch (IOException e) {
                          e.printStackTrace();
                          System.err.println("Unable to access " + dataset);
                          return null;
                        }
                      })
                  .filter(f -> f != null)
                  .toList();
        }

        topKs.each(
            (k) -> {
              LongList times = benchmarkIUFPM(factory, k, dbs);
              String row =
                  String.format(
                      "%s,%d,%.2f,%s",
                      factory.toString(),
                      k,
                      ((double) times.sum()) / measurement,
                      times.makeString(" "));
              System.out.println(row);
            });
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Unable to access " + dataset);
    }
  }

  public static void main(String[] args) {
    new App().doMain(args);
  }
}
