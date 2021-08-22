import numpy as np
import math
from collections import Counter


def mse(y_true, y_pred):
    a = np.array([0.5, 1, 2])
    a = a.reshape(1, 3)

    return np.mean(a*(np.power(y_true-y_pred, 2)))


def mse_derivative(y_true, y_pred):
    a = np.array([0.5, 1, 2])
    a = a.reshape(1, 3)
    return 2*a*(y_pred-y_true)/y_true.size


def sigmoid(x):
    return 1/(1+np.exp(-x))


def sigmoid_der(x):
    return sigmoid(x) * (1-sigmoid(x))


def get_tpr_fpr(y_pred, y_true):
    y_pred = np.array(y_pred)
    y_true = np.array(y_true)
    print(y_pred.shape)
    print(y_true.shape)
    tpr = 0
    fpr = 0
    true_counts = {}
    true_counts[1] = 0
    true_counts[2] = 0
    true_counts[3] = 0
    false_counts = {}
    false_counts[1] = 0
    false_counts[2] = 0
    false_counts[3] = 0

    for i in range(len(y_pred)):
        if y_pred[i] == y_true[i]:
            tpr += 1
            true_counts[y_true[i]] += 1
        else:
            false_counts[y_true[i]] += 1
            fpr += 1
    for i in [1, 2, 3]:
        print(i)
        print(true_counts[i]/(true_counts[i]+false_counts[i]))
    return (tpr, fpr)


def stratify_train_test_split(x, y, train_size):
    num_samples = len(x)
    num_train = num_samples*train_size
    indices = {}

    y_counts = Counter(y)
    y = np.array(y)
    train_x = []
    train_y = []
    test_x = []
    test_y = []
    for y_i in y_counts.keys():
        fraction_i = y_counts[y_i]/len(y)
        indices[y_i] = np.where(y == y_i)[0]
        train_indices_i = np.random.choice(
            indices[y_i], int(num_train*fraction_i), replace=False)
        for idx in train_indices_i:
            train_x.append(x[idx])
            train_y.append(y[idx])
        test_indices_i = set(indices[y_i]).difference(set(train_indices_i))
        for idx in test_indices_i:
            test_x.append(x[idx])
            test_y.append(y[idx])
    train_x = np.array(train_x)
    train_y = np.array(train_y)
    test_x = np.array(test_x)
    test_y = np.array(test_y)
    return (train_x, test_x, train_y, test_y)
