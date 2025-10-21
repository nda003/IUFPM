# IBUCA

A single-pass **i**ncremental *b*uffered top-k **u**n**c**ertain frequent pattern mining **a**lgorithm.

## Datasets

The uncertain datasets such as `contextMushroom.txt` and `contextFoodmart.txt` in `src/main/resources` were taken from [SPMF's Public Datasets for Frequent Itemset mining](https://www.philippe-fournier-viger.com/spmf/index.php?link=datasets.php#v1). The probabilities were generated from a normal distribution with mean of 0.6 and a standard deviation of 0.2, clamped to a value between 0.01 and 1.

## Usage

This repository requires Java 21 or higher. To run the program, execute the followings:

```bash
mvn clean package
java -jar target/benchmarks.jar
```
