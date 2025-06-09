# Image Compression Using Vector Quantization

## Overview

This project implements an end‑to‑end Vector Quantization (VQ)‑based image‑compression pipeline for both RGB and YUV 4:2:0 colour spaces. The code:

trains 256‑vector codebooks with k‑means on 2 × 2 blocks,

compresses images by mapping each block to the nearest code‑vector,

reconstructs images and reports compression ratio and MSE.

# How It Works

Training – VectorQuantizer.generateCodebook() learns 256 centroids per component (R, G, B or Y, U, V) using 10 iterations of k‑means.
Compression –  Each 2 × 2 block is replaced by an 8‑bit index into the corresponding codebook.
Decompression –  Indices are mapped back to code‑vectors; YUV components are up‑sampled; RGB components are merged.
Metrics –  Compression ratio and pixelwise MSE are logged for every test image.
