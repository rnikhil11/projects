from utils import stratify_train_test_split
from bb import bagging, boosting, predict_example, predict_example_dt
from decision_tree import id3 as base_id3, compute_error
import numpy as np
from collections import Counter
import math
import matplotlib.pyplot as plt
from sklearn.metrics import f1_score


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


def discrete_boundaries(x_cont):
    num_features = len(x_cont[0])
    b = {}
    for i in range(num_features):
        max_a = max(x_cont[:, i])
        min_a = min(x_cont[:, i])
        b[i] = (min_a, max_a)
    return b


def discretize(x_cont, boundaries, n):
    num_features = len(x_cont[0])
    for x_j in x_cont:
        for i in range(num_features):
            x_j[i] = int(math.ceil(n*(x_j[i]-boundaries[i][0]) /
                                   (boundaries[i][1]-boundaries[i][0])))
    return x_cont


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
            false_counts[y_pred[i]] += 1
            fpr += 1
    for i in [1, 2, 3]:
        print(i)
        print(true_counts[i]/(true_counts[i]+false_counts[i]))
    return (tpr, fpr)


if __name__ == '__main__':

    f = open('fetal_health.csv')
    M = np.loadtxt(f, delimiter=',')
    y = M[:, -1]
    y = [int(y_i) for y_i in y]
    x = M[:, 0:-1]

    train_x_cont, test_x_cont, train_y, test_y = stratify_train_test_split(
        x, y,
        train_size=0.8
    )

    x_meta = discrete_boundaries(train_x_cont)
    train_x = discretize(train_x_cont, x_meta, 10)
    avps = generate_avps(train_x)

    test_x = discretize(test_x_cont, x_meta, 10)

    y_points_train_err = []
    y_points_test_err = []
    for i in range(10, 20):
        decision_tree = base_id3(
            train_x, train_y, attribute_value_pairs=avps.copy(), max_depth=i)
        y_pred = [predict_example_dt(x, decision_tree) for x in test_x]
        y_train_pred = [predict_example_dt(x, decision_tree) for x in train_x]

        train_err = compute_error(train_y, y_train_pred)
        tst_err = compute_error(test_y, y_pred)

        y_points_train_err.append(train_err*100)
        y_points_test_err.append(tst_err*100)
        print('Train Error = {0:4.2f}%.'.format(train_err * 100))

        print('Test Error = {0:4.2f}%.'.format(tst_err * 100))
        print()
    x_points = np.array(range(10, 20))
    fig, ax = plt.subplots(1, figsize=(8, 6))
    fig.suptitle('fetal-health', fontsize=20)

    ax.set_xlabel('Depth')
    ax.set_ylabel('Error %')
    ax.plot(x_points, y_points_train_err, color="blue", label="Training error")

    ax.plot(x_points, y_points_test_err, color="red", label="Test error")
    plt.legend(loc="upper right", frameon=False)

    plt.show()
    fig.savefig('fetal-health-base-dt.jpg')

    # Bagging
    for d in [14, 15]:
        print('depth: '+str(d))
        dt = base_id3(train_x, train_y,
                      attribute_value_pairs=avps.copy(), max_depth=d)
        print('Base decision tree:')
        print(f1_score(
            test_y, [predict_example_dt(x, dt) for x in test_x], average=None))

        for k in [10, 20]:
            print('Bagging:')
            bagger = bagging(train_x, train_y, max_depth=d, num_trees=k)

            print('bag_size: '+str(k))
            print(f1_score(
                test_y, [predict_example(x, bagger) for x in test_x], average=None))
            print()

            print('**********')

    for d in [10]:
        print('depth: '+str(d))

        dt = base_id3(train_x, train_y,
                      attribute_value_pairs=avps.copy(), max_depth=d)
        print('Base decision tree:')
        print(f1_score(test_y,
                       [predict_example_dt(x, dt) for x in test_x], average=None))

        for k in [20, 30]:
            print()
            print('Boosting:')

            booster = boosting(train_x, train_y, max_depth=d, num_stumps=k)
            print('bag_size: '+str(k))
            print(f1_score(test_y,
                           [predict_example(x, booster) for x in test_x], average=None))
            print('**********')
