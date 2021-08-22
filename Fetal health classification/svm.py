from matplotlib import pyplot as plt
from decision_tree import compute_error
from utils import stratify_train_test_split
import numpy as np
import pandas as pd
from sklearn.metrics import f1_score


class SVM:

    def __init__(self, learning_rate=0.001, lambda_param=0.001, n_iters=1000):
        self.lr = learning_rate
        self.lambda_param = lambda_param
        self.n_iters = n_iters
        self.w = None
        self.b = None

    def train(self, X, y):
        n_samples, n_features = X.shape

        y = np.array(y)
        y_new = np.where(y <= 0, -1, 1)

        self.w = np.zeros(n_features)
        self.b = 0

        for i in range(self.n_iters):
            for idx, x_i in enumerate(X):
                if y_new[idx] * (np.dot(x_i, self.w) - self.b) >= 1:
                    self.w -= self.lr * (2 * self.lambda_param * self.w)
                else:
                    self.w -= self.lr * \
                        (2 * self.lambda_param *
                         self.w - np.dot(x_i, y_new[idx]))
                    self.b -= self.lr * y_new[idx]

    def predict(self, X):
        approx = np.dot(X, self.w) - self.b
        return np.sign(approx)


class ThreeSVM:

    def __init__(self, learning_rate=0.001, lambda_param=0.001, n_iters=1000):
        self.lr = learning_rate
        self.lambda_param = lambda_param
        self.n_iters = n_iters
        self.w = None
        self.b = None
        self.c1 = None
        self.c2 = None
        self.c3 = None

    def train(self, X, y):
        dummy_y1 = []
        for i in range(len(y)):
            if y[i] != 1:
                dummy_y1.append(-1)
            else:
                dummy_y1.append(1)
        classifier1 = SVM(lambda_param=self.lambda_param)
        classifier1.train(X, dummy_y1)
        self.c1 = classifier1

        dummy_y2 = []
        for i in range(len(y)):
            if y[i] != 2:
                dummy_y2.append(-1)
            else:
                dummy_y2.append(1)
        classifier2 = SVM(lambda_param=self.lambda_param)
        classifier2.train(X, dummy_y2)
        self.c2 = classifier2

        dummy_y3 = []
        for i in range(len(y)):
            if y[i] != 3:
                dummy_y3.append(-1)
            else:
                dummy_y3.append(1)
        classifier3 = SVM(lambda_param=self.lambda_param)
        classifier3.train(X, dummy_y3)
        self.c3 = classifier3
        return

    def predict(self, x):
        pred1 = set()
        if self.c1.predict(x) >= 0:
            pred1.add(1)
        else:
            pred1.add(2)
            pred1.add(3)
        pred2 = set()
        if self.c2.predict(x) >= 0:
            pred2.add(2)
        else:
            pred2.add(3)
            pred2.add(1)
        pred3 = set()
        if self.c3.predict(x) >= 0:
            pred3.add(3)
        else:
            pred3.add(1)
            pred3.add(2)
        intersect = pred1.intersection(pred2).intersection(pred3)
        # predict suspect(2) if more than 2 classes
        return intersect.pop() if len(intersect) == 1 else 2


df = pd.read_csv('fetal_health2.csv')
xs = df.iloc[:, :-1]
ys = df['fetal_health']


x_train, x_test, y_train, y_test = stratify_train_test_split(
    xs.to_numpy(), ys.to_numpy(), train_size=0.8)
clf = ThreeSVM(lambda_param=0.003)
clf.train(x_train, y_train)
y_pred = [clf.predict(x_i) for x_i in x_test]
print(f1_score(y_test, y_pred, average=None))

# y_train_pred = [clf.predict(x_i) for x_i in x_train]

# train_err = compute_error(y_train, y_train_pred)
tst_err = compute_error(y_test, y_pred)
