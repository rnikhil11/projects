import numpy as np
import math


def print_confusion_matrix(y_test, y_pred):
    truePositive = 0
    trueNegative = 0
    falseNegative = 0
    falsePositive = 0
    trues = 0
    falses = 0
    for i in range(len(y_test)):
        if y_test[i] == y_pred[i]:
            trues += 1
        else:
            falses += 1
    # print([[truePositive, falseNegative], [falsePositive, trueNegative]])
    print('Accuracy: '+str(trues/(len(y_test))))
    return


def predict_example_dt(x, tree):
    """
    Predicts the classification label for a single example x using tree by recursively descending the tree until
    a label/leaf node is reached.

    Returns the predicted label of x according to tree
    """

    test = list(tree.keys())[0]
    (attr, val, res) = test
    b = (x[attr-1] == val)
    if type(tree[(attr, val, b)]) != dict:
        return tree[(attr, val, b)]
    else:
        return predict_example_dt(x, tree[(attr, val, b)])


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


def weighted_entropy(y, w):
    """
    Compute the entropy of a vector y by considering the counts of the unique values (v1, ... vk), in z

    Returns the entropy of z: H(z) = p(z=v1) log2(p(z=v1)) + ... + p(z=vk) log2(p(z=vk))
    """

    h = 0
    indices_dict = partition(y)
    weights = np.array(w)
    s = sum(weights)

    for z in indices_dict.keys():
        weights_z = sum(list(map(lambda i: weights[i], indices_dict[z])))
        p = weights_z/s
        h += 0 if p == 0 else -p*math.log2(p)
    return h


def weighted_mutual_information(x: list, y: list, weights: list):

    mi_before_split = weighted_entropy(y, weights)

    mi_after_split = 0
    indices_dict = partition(x)
    s = sum(weights)
    for v in set(x):
        y_v = list(map(lambda i: y[i], indices_dict[v]))
        weights_v = list(map(lambda i: weights[i], indices_dict[v]))
        weighted_p = sum(weights_v)/s
        mi_after_split += weighted_p*weighted_entropy(y_v, weights_v)

    return mi_before_split-mi_after_split


def max_mutual_information_avp(attribute_value_pairs, x, y, weights):

    attributes = set(list(map(lambda avp: avp[0], attribute_value_pairs)))

    gain_dict = {}
    for a in attributes:
        gain_dict[a] = weighted_mutual_information(
            np.array(x)[:, a-1], y, weights)
    best_attribute = max(gain_dict, key=gain_dict.get)
    best_avp = next(
        filter(lambda avp: avp[0] == best_attribute, attribute_value_pairs))
    return best_avp


def id3(x, y, weights, attribute_value_pairs=None, depth=0, max_depth=5):

    if len(set(y)) == 1:
        return y[0]
    elif len(attribute_value_pairs) == 0 or depth == max_depth:
        return weighted_majority(y, weights)
    else:
        best_avp = max_mutual_information_avp(
            attribute_value_pairs, x, y, weights)
        true_set_indices = np.where(
            np.array(x)[:, best_avp[0]-1] == best_avp[1])[0]

        true_y = list(map(lambda i: y[i], true_set_indices))
        true_x = list(map(lambda i: x[i], true_set_indices))
        true_weights = list(map(lambda i: weights[i], true_set_indices))
        true_weights = np.array(true_weights)/sum(true_weights)

        false_set_indices = list(set(range(len(y))).difference(
            set(true_set_indices)))

        false_y = list(map(lambda i: y[i], false_set_indices))
        false_x = list(map(lambda i: x[i], false_set_indices))
        false_weights = list(map(lambda i: weights[i], false_set_indices))
        false_weights = np.array(false_weights)/sum(false_weights)

        attribute_value_pairs.remove(best_avp)
        rec1 = id3(true_x, true_y, true_weights, attribute_value_pairs=attribute_value_pairs.copy(), depth=depth+1,
                   max_depth=max_depth) if len(true_x) != 0 else weighted_majority(y, weights)
        rec2 = id3(false_x, false_y, false_weights, attribute_value_pairs=attribute_value_pairs.copy(), depth=depth+1,
                   max_depth=max_depth) if len(false_x) != 0 else weighted_majority(y, weights)

        return {(best_avp[0], best_avp[1], True): rec1,
                (best_avp[0], best_avp[1], False): rec2}


def weighted_majority(y: list, w: list):
    counts = {}
    for y_i in set(y):
        counts[y_i] = 0
    for i in range(len(y)):
        counts[y[i]] += w[i]
    return max(counts, key=counts.get)


def weighted_majority_boost(y: list, w: list):
    y = np.array(y)
    y_new = np.where(y == 0, -1, y)
    k = sum([y_new[i]*w[i] for i in range(len(y))])
    return 0 if k < 0 else 1


def bagging(x, y, max_depth, num_trees):
    trn_size = len(x)

    weights = [1/trn_size]*trn_size
    ensemble = []
    avps = generate_avps(x)

    for i in range(num_trees):
        indices = np.random.choice(range(trn_size), trn_size, replace=True)

        bag_x = np.array(list(map(lambda i: x[i], indices)))
        bag_y = np.array(list(map(lambda i: y[i], indices)))
        ensemble.append((1/num_trees, id3(bag_x, bag_y, weights,
                                          attribute_value_pairs=avps.copy(), max_depth=max_depth)))

    return ensemble


def compute_error(y_true, y_pred, weights):
    """
    Computes the average error between the true labels (y_true) and the predicted labels (y_pred)

    Returns the error =  sum(weights[i]*(y_true != y_pred))
    """

    errCount = 0
    n = len(y_true)
    for i in range(n):
        if y_true[i] != y_pred[i]:
            errCount += weights[i]
    return errCount/sum(weights)


def update_weights(y_true, y_pred, weights, alpha):
    n = len(y_true)
    new_weights = [0.1]*n
    c = math.exp(alpha)
    for i in range(n):
        new_weights[i] = weights[i] * \
            c if y_true[i] != y_pred[i] else weights[i]/c
    return new_weights


def boosting(xTrn, yTrn, max_depth, num_stumps):
    avps = generate_avps(xTrn)
    trn_size = len(yTrn)
    w = np.array([1/trn_size]*trn_size)
    ensemble = []

    for i in range(num_stumps):
        h_i = id3(
            xTrn, yTrn, w, attribute_value_pairs=avps.copy(), max_depth=max_depth)
        y_pred = [predict_example_dt(x_j, h_i) for x_j in xTrn]
        err = compute_error(yTrn, y_pred, w)

        alpha = math.log((1-err)/err) + math.log(2)
        w = update_weights(yTrn, y_pred, w, alpha)
        ensemble.append((alpha, h_i))
    return ensemble


def predict_example(x, h_ens):

    predicted_y = list([predict_example_dt(x, h_ens[i][1])
                        for i in range(len(h_ens))])
    weights = list([h_ens[i][0] for i in range(len(h_ens))])

    return weighted_majority(predicted_y, weights) if len(set(weights)) == 1 else weighted_majority_boost(predicted_y, weights)


def generate_avps(X):
    num_cols = len(X[0])
    avp_dict = {}
    for j in range(num_cols):
        avp_dict[j+1] = set()
    for i in range(len(X)):
        for j in range(num_cols):
            avp_dict[j+1].add(X[i][j])
    avps = []
    for x in avp_dict.keys():
        for y in avp_dict[x]:
            avps.append((x, y))

    return avps
