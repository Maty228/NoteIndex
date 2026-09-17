# Deep Learning — Diffusion Models

Diffusion models are generative models that learn to reverse a gradual noising process.

## Forward process

The forward diffusion process progressively adds Gaussian noise to training data until the original structure is largely destroyed.

## Reverse process

A neural network learns to predict how to remove noise step by step.

During generation, the model begins with random noise and repeatedly applies the learned denoising process.

## Why diffusion models matter

Diffusion models can produce high-quality images and have become important in modern generative AI.

Their main disadvantage is that generation often requires many iterative denoising steps, although newer sampling techniques can reduce this cost.
