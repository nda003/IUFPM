# ITUNA

A single-pass list-based **i**ncremental **t**op-k **u**ncertain frequent pattern mining **a**lgorithm.

The uncertain dataset `contextMushroom.txt` and `contextFoodmart.txt` in `src/main/resources` were taken from the [mushrooms and foodmart dataset](https://www.philippe-fournier-viger.com/spmf/index.php?link=datasets.php#v1) respectively. The probabilities were generated from a normal distribution with mean of 0.6 and a standard deviation of 0.2, clamped to a value between 0.01 and 1.
