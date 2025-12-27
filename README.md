# IUFPM

A collection of incremental uncertain top-K frequent pattern mining algorithms. This repository includes [TUFP](https://doi.org/10.1007/s10489-019-01622-1), [ITUFP](https://doi.org/10.1016/j.eswa.2022.119156) modified to be incremental, and ISUCK (**I**ncrementally **S**egmented **U**n**C**ertain top-**K** frequent pattern mining algorithm). The compiled `iufpmBenchmark.jar` can be used to benchmark each algorithm.

## Datasets

The uncertain datasets such as `contextMushroom.txt` and `contextFoodmart.txt` in `src/main/resources` were taken from [SPMF's Public Datasets for Frequent Itemset mining](https://www.philippe-fournier-viger.com/spmf/index.php?link=datasets.php#v1). The probabilities were generated from a normal distribution with mean of 0.6 and a standard deviation of 0.2, clamped to a value between 0.01 and 1.

The dataset format follows `itemId itemId:itemProbability itemProbability`. For example:

```
10307 10311 12487:0.6609434160 0.3920031788 0.750090239
12559:0.788112943
12695 12703 18715:0.2097929623 0.3395640986 0.625568080
```

Other uncertain datasets taken SPMF are available [here](https://drive.google.com/drive/folders/1zSmc2lRaRyub-AU7pxyA0SFsRAUkdMXj?usp=sharing). SPMF datasets were converted into uncertain datasets using a normal distribution with the same mean and standard deviation as above.

Dataset folders with `Even` at the end means that the uncertain dataset were split into a file named `0` which is 10% of the original dataset and 45 other files with equal size to one another, each made up of 2% of the original, adding up to 90% of the original. This is to simulate an initial large dataset, followed by smaller even increments.

Dataset folders with `Uneven` at the means that the uncertain dataset were split into a file named `0` which is 10% of the original dataset, with the other 90% being random uneven split. The gibberish file name of these 90% are so that the order in which these files are processed would also be randomized. This is to simulate an initial large dataset, followed by random and smaller uneven increments.

## Compiling

This package requires Java 21 or higher. To compile and run the jar executable:

```bash
mvn clean package
java -jar target/iufpmBenchmark.jar -h
```

## Usage

```
java -jar iufpmBenchmark.jar [options...] PATH
 PATH                      : the path to the input uncertain dataset
 -a [ISUCK | ITUFP | TUFP] : algorithm to benchmark (default: ISUCK)
 -k K,K,...                : top-Ks used for benchmarking seperated by comma
                             (default: 100,200,300,400,500)
 -m N                      : number of measurement iterations (default: 10)
 -o outfile                : the output csv file if provided
 -v                        : verbose output with top-K itemsets display
                             (default: false)
 -w N                      : number of warmnup iterations (default: 2)

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

ISUCK,10,385.0,30 300 40 400 35 350
 ```
