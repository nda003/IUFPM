package vn.datm.iufpm;

import java.io.BufferedWriter;
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
import vn.datm.iufpm.util.UItemSet;

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

  /** Factory for creating TUFP miners. */
  private class TUFPFactory implements IUFPMFactory {

    /**
     * @param k The top-K value to use for the miner.
     * @return A new TUFP miner.
     */
    @Override
    public IUFPM create(int k) {
      return new TUFP(k);
    }

    @Override
    public String toString() {
      return "TUFP";
    }
  }

  /** Factory for creating ITUFP miners. */
  private class ITUFPFactory implements IUFPMFactory {
    /**
     * @param k The top-K value to use for the miner.
     * @return A new ITUFP miner.
     */
    public IUFPM create(int k) {
      return new ITUFP(k);
    }

    @Override
    public String toString() {
      return "ITUFP";
    }
  }

  /** Factory for creating ISUCK miners. */
  private class ISUCKFactory implements IUFPMFactory {
    /**
     * @param k The top-K value to use for the miner.
     * @return A new ISUCK miner.
     */
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
      metaVar = "K,K,...")
  private String k = "100,200,300,400,500";

  @Option(name = "-m", usage = "number of measurement iterations")
  private int measurement = 10;

  @Option(name = "-w", usage = "number of warmnup iterations")
  private int warmnup = 2;

  // @Option(name = "-i", usage = "input dataset")
  // private Path dataset;

  @Option(name = "-a", usage = "algorithm to benchmark")
  private Algorithm algorithm = Algorithm.ISUCK;

  @Option(name = "-v", usage = "verbose output with top-K itemsets display")
  private boolean verbose = false;

  @Option(name = "-h", hidden = true, usage = "print help")
  private boolean help = false;

  @Option(name = "-o", usage = "the output csv file if provided", metaVar = "outfile")
  private String outfile;

  @Argument(usage = "the path to the input uncertain dataset", metaVar = "PATH")
  private List<String> arguements = new ArrayList<>();

  /**
   * @param miner The IUFPM algorithm to time
   * @return The time it took to run {@code miner.mine()} in milliseconds
   */
  public long timeIUFPM(IUFPM miner) {
    long start = System.nanoTime();
    List<UItemSet> topk = miner.mine();
    long ms = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);

    if (verbose) {
      System.out.println("The timed outputed top-K itemsets: " + topk);
    }

    return ms;
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
                  "Warmnup iteration %d against %d databases took: %d", i + 1, dbs.size(), t));
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
      System.err.println("java -jar iufpmBenchmark.jar [options...] PATH");
      parser.printUsage(System.err);
      return;
    }

    String bottomText =
"""

The csv headers are:

algorithm,topK,averageTime,times

If PATH is a singular file, the times column will be a list of integers
corresponding to how long it took the algorithm to mine the dataset during each
measurements in milliseconds.

If PATH is folder, each files will be processed in alphabetical order.
The selected algorithm will first mine the first ordered file, followed by
incrementing using the rest of the files without mining. After all the files
have been incremented, mining will then done. The times column will be a list of
integers corresponding to how long it took the algorithm to initially mine the
first file, followed by how long it took the algorithm to incrementally mine the
rest of the file during each measurements in milliseconds. For example, if we
measure ISUCK with K=10 three times and we get these results:

| Measurement | Initial mining | Incremental mining |
|-------------|----------------|--------------------|
| 1           | 30             | 300                |
| 2           | 40             | 400                |
| 3           | 35             | 350                |

The csv row will be thus:

ISUCK,10,385,30 300 40 400 35 350
""";

    if (help) {
      System.out.println("java -jar iufpmBenchmark.jar [options...] PATH");
      parser.printUsage(System.out);
      System.out.print(bottomText);
      return;
    }

    IntList topKs;

    try {
      topKs = parseTopK();
    } catch (NumberFormatException e) {
      e.printStackTrace();
      System.err.println("Unable to parse -k.");
      System.err.print(bottomText);
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
    StringBuilder sb = new StringBuilder();

    try {
      if (Files.isRegularFile(dataset)) {
        UTDatabase db = UTDatabase.fromFile(dataset);

        topKs.each(
            (k) -> {
              LongList times = benchmarkIUFPM(factory, k, db);
              String row =
                  String.format(
                      "%s,%d,%.2f,%s\n",
                      factory.toString(), k, times.average(), times.makeString(" "));
              System.out.print(row);

              if (outfile != null) {
                sb.append(row);
              }
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
                      "%s,%d,%.2f,%s\n",
                      factory.toString(),
                      k,
                      ((double) times.sum()) / measurement,
                      times.makeString(" "));
              System.out.print(row);

              if (outfile != null) {
                sb.append(row);
              }
            });
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("Unable to access " + dataset);
    }

    if (outfile != null) {
      try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outfile))) {
        writer.write(sb.toString());
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Unable to write to " + outfile);
      }
    }
  }

  /**
   * The main entry point of the application.
   *
   * @param args The command-line arguments.
   */
  public static void main(String[] args) {
    new App().doMain(args);
  }
}
