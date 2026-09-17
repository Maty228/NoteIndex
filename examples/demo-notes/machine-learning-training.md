# Machine Learning — Training Neural Networks

Training a neural network means optimizing its parameters to minimize a loss function.

## Gradient descent

Backpropagation computes gradients of the loss with respect to model parameters. An optimizer then updates those parameters.

Common optimizers include:

- stochastic gradient descent
- Adam
- AdamW

## Generalization

A model should perform well on unseen data rather than simply memorize the training set.

Regularization techniques include weight decay, dropout, data augmentation, and early stopping.

Validation data is used to monitor model performance during training.
