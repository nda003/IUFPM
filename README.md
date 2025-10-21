# IUFPM

A collection of incremental uncertain top-K frequent pattern mining algorithms. This repository includes [TUFP](https://doi.org/10.1007/s10489-019-01622-1), [ITUFP](https://doi.org/10.1016/j.eswa.2022.119156) modified to be incremental, and ISUCK (**I**ncrementally **S**egmented **U**n**C**ertain top-**K** frequent pattern mining algorithm). The compiled `iufpmBenchmark.jar` can be used to benchmark each algorithm.

## Datasets

The uncertain datasets such as `contextMushroom.txt` and `contextFoodmart.txt` in `src/main/resources` were taken from [SPMF's Public Datasets for Frequent Itemset mining](https://www.philippe-fournier-viger.com/spmf/index.php?link=datasets.php#v1). The probabilities were generated from a normal distribution with mean of 0.6 and a standard deviation of 0.2, clamped to a value between 0.01 and 1. The dataset format follows `itemId itemId:itemProbability itemProbability`. For example:

```
10307 10311 12487:0.6609434160 0.3920031788 0.750090239
12559:0.788112943
12695 12703 18715:0.2097929623 0.3395640986 0.625568080
```

## Compiling

This package requires Java 21 or higher. To compile and run the jar executable:

```bash
mvn clean package
java -jar target/iufpmBenchmark.jar -h
```

## Usage

```bash
java -jar iufpmBenchmark.jar [options...] arguments...
 -a [ISUCK | ITUFP | TUFP] : algorithm to benchmark (default: ISUCK)
 -i PATH                   : input dataset
 -k N,N,...                : top-Ks used for benchmarking seperated by comma
                             (default: 100,200,300,400,500)
 -m N                      : number of measurement iterations (default: 10)
 -w N                      : number of warmnup iterations (default: 2)
```
