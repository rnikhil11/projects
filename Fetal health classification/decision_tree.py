# decision_tree.py
# ---------
# Licensing Information:  You are free to use or extend these projects for
# personal and educational purposes provided that (1) you do not distribute
# or publish solutions, (2) you retain this notice, and (3) you provide clear
# attribution to UT Dallas, including a link to http://cs.utdallas.edu.
#
# This file is part of Programming Assignment 1 for CS6375: Machine Learning.
# Gautam Kunapuli (gautam.kunapuli@utdallas.edu)
# Sriraam Natarajan (sriraam.natarajan@utdallas.edu),
#
#

import numpy as np
from collections import Counter
import math
import matplotlib.pyplot as plt


def partition(x):
    """
    Partition the column vector x into subsets indexed by its unique values (v1, ... vk)

    Returns a dictionary of the form
    { v1: indices of x == v1,
      v2: indices of x == v2,
      ...
      vk: indices of x == vk }, where [v1, ... vk] are all the unique values in the vector z.
    """

    x_unique_values = set(x)
    d = {}
    for v in x_unique_values:
        d[v] = np.where(np.array(x) == v)[0]
    return d


def entropy(y):
    """
    Compute the entropy of a vector y by considering the counts of the unique values (v1, ... vk), in z

    Returns the entropy of z: H(z) = p(z=v1) log2(p(z=v1)) + ... + p(z=vk) log2(p(z=vk))
    """
    h = 0
    for z in set(y):
        p = Counter(y).get(z)/len(y)
        h += 0 if p == 0 else -p*math.log2(p)
    return h


def mutual_information(x: list, y: list):
    """
    Compute the mutual information between a data column (x) and the labels (y). The data column is a single attribute
    over all the examples (n x 1). Mutual information is the difference between the entropy BEFORE the split set, and
    the weighted-average entropy of EACH possible split.

    Returns the mutual information: I(x, y) = H(y) - H(y | x)
    """
    mi_before_split = entropy(y)

    mi_after_split = 0
    indices_dict = partition(x)
    for v in set(x):
        y_v = list(map(lambda i: y[i], indices_dict[v]))
        p = Counter(x).get(v)/len(x)
        mi_after_split += p*entropy(y_v)

    return mi_before_split-mi_after_split


def max_mutual_information_avp(attribute_value_pairs, x, y):

    attributes = set(map(lambda avp: avp[0], attribute_value_pairs))

    gain_dict = {}
    for a in attributes:
        gain_dict[a] = mutual_information(np.array(x)[:, a-1], y)
    best_attribute = max(gain_dict, key=gain_dict.get)
    best_avp = next(
        filter(lambda avp: avp[0] == best_attribute, attribute_value_pairs))
    return best_avp


def id3(x, y, attribute_value_pairs=None, depth=0, max_depth=5):

    if len(set(y)) == 1:
        return y[0]
    elif len(attribute_value_pairs) == 0 or depth == max_depth:
        return Counter(y).most_common(1)[0][0]
    else:
        best_avp = max_mutual_information_avp(attribute_value_pairs, x, y)
        true_set_indices = np.where(
            np.array(x)[:, best_avp[0]-1] == best_avp[1])[0]

        true_y = list(map(lambda i: y[i], true_set_indices))
        true_x = list(map(lambda i: x[i], true_set_indices))
        false_set_indices = list(set(range(len(y))).difference(
            set(true_set_indices)))

        false_y = list(map(lambda i: y[i], false_set_indices))
        false_x = list(map(lambda i: x[i], false_set_indices))

        attribute_value_pairs.remove(best_avp)
        rec1 = id3(true_x, true_y, attribute_value_pairs=attribute_value_pairs, depth=depth+1,
                   max_depth=max_depth) if len(true_x) != 0 else Counter(y).most_common(1)[0][0]
        rec2 = id3(false_x, false_y, attribute_value_pairs=attribute_value_pairs, depth=depth+1,
                   max_depth=max_depth) if len(false_x) != 0 else Counter(y).most_common(1)[0][0]

        return {(best_avp[0], best_avp[1], True): rec1,
                (best_avp[0], best_avp[1], False): rec2}


def predict_example(x, tree):

    test = list(tree.keys())[0]
    (attr, val, res) = test
    b = (x[attr-1] == val)
    if type(tree[(attr, val, b)]) != dict:
        return tree[(attr, val, b)]
    else:
        return predict_example(x, tree[(attr, val, b)])


def compute_error(y_true, y_pred):
    """
    Computes the average error between the true labels (y_true) and the predicted labels (y_pred)

    Returns the error = (1/n) * sum(y_true != y_pred)
    """
    errCount = 0
    n = len(y_true)
    for i in range(n):
        errCount += 1 if y_true[i] != y_pred[i] else 0
    return errCount/n


def print_confusion_matrix(y_test, y_pred):
    truePositive = 0
    trueNegative = 0
    falseNegative = 0
    falsePositive = 0

    for i in range(len(y_test)):
        if y_test[i] == 0:
            if y_pred[i] == 0:
                truePositive += 1
            else:
                falseNegative += 1
        else:
            if y_pred[i] == 0:
                falsePositive += 1
            else:
                trueNegative += 1
    print([[truePositive, falseNegative], [falsePositive, trueNegative]])
    return
