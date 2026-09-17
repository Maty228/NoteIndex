# Deep Learning — Convolutional Neural Networks

Convolutional neural networks are designed to process data with spatial structure, especially images.

## Convolution

A convolutional layer applies learned filters across an input image. Each filter detects local patterns such as edges, textures, or more complex structures.

Important concepts include:

- kernels
- stride
- padding
- feature maps
- receptive fields

## ResNet

**ResNet** introduced residual connections that allow very deep neural networks to train more effectively.

Instead of learning a direct mapping, a residual block learns a residual function:

`y = F(x) + x`

Residual connections improve gradient flow and reduce degradation in deep architectures.

## Applications

CNNs are widely used for image classification, object detection, segmentation, and visual feature extraction.
