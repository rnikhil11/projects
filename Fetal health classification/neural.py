import numpy as np


class FCLayer():
    def __init__(self, input_size, output_size):
        self.weights = np.random.rand(input_size, output_size) - 0.5
        self.bias = np.random.rand(1, output_size) - 0.5

    def fwd_prop(self, input_data):
        self.input = input_data
        self.output = np.dot(self.input, self.weights) + self.bias
        return self.output

    def back_prop(self, output_error, learning_rate):
        d_error_d_x = np.dot(output_error, self.weights.T)
        d_error_d_weights = np.dot(self.input.T, output_error)

        self.weights -= learning_rate * d_error_d_weights
        self.bias -= learning_rate * output_error
        return d_error_d_x


class ActivationLayer():
    def __init__(self, activation, activation_der):
        self.activation = activation
        self.activation_der = activation_der

    def fwd_prop(self, input_data):
        self.input = input_data
        self.output = self.activation(self.input)
        return self.output

    def back_prop(self, output_error, learning_rate):
        return self.activation_der(self.input) * output_error


class NeuralNet:
    def __init__(self, layers, loss, loss_der):
        self.layers = layers
        self.loss = loss
        self.loss_der = loss_der
        self.weights = None

    def predict(self, input_data):
        num_test = len(input_data)
        result = []

        for i in range(num_test):
            output = input_data[i]
            for layer in self.layers:
                output = layer.fwd_prop(output)
            output = np.argmax(output) + 1
            result.append(output)

        return result

    def train(self, x_train, y_train, num_iterations=30, learning_rate=0.1):
        num_samples = len(x_train)
        for i in range(num_iterations):
            err = 0
            for j in range(num_samples):
                output = x_train[j]
                for layer in self.layers:
                    output = layer.fwd_prop(output)

                err += self.loss(y_train[j], output)
                error = self.loss_der(y_train[j], output)
                for layer in self.layers[::-1]:
                    error = layer.back_prop(error, learning_rate)

            err /= num_samples
            print('iteration %d/%d   error=%f' % (i+1, num_iterations, err))
