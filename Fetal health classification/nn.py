import numpy as np
import pandas as pd

from neural import NeuralNet, FCLayer, ActivationLayer
from utils import mse, mse_derivative, sigmoid_der, sigmoid, stratify_train_test_split
from sklearn.preprocessing import LabelEncoder
from keras.utils import np_utils
from sklearn.metrics import f1_score

df = pd.read_csv('fetal_health2.csv')
xs = df.iloc[:, :-1]
ys = df['fetal_health']
x_train, x_test, y_train, y_test = stratify_train_test_split(
    xs.to_numpy(), ys.to_numpy(), train_size=0.8)

encoder = LabelEncoder()
encoder.fit(y_train)
encoded_Y_train = encoder.transform(y_train)
y_train = np_utils.to_categorical(encoded_Y_train)


y_train = y_train.reshape(len(y_train), 1, 3)
x_train = x_train.reshape(len(x_train), 1, 21)
x_test = x_test.reshape(len(x_test), 1, 21)

layers = [FCLayer(21, 21), ActivationLayer(sigmoid, sigmoid_der), FCLayer(21, 21), ActivationLayer(
    sigmoid, sigmoid_der), FCLayer(21, 3), ActivationLayer(sigmoid, sigmoid_der)]
net = NeuralNet(loss=mse, loss_der=mse_derivative, layers=layers)
net.train(x_train, y_train, num_iterations=100, learning_rate=0.01)


out = np.array(net.predict(x_test))

print(f1_score(y_test, out, average=None))
