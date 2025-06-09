import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String[] categories = {"Nature", "Faces", "Animals"};
        List<BufferedImage> trainingImages = new ArrayList<>();
        List<BufferedImage> testImages = new ArrayList<>();
        for (String category : categories) {
            List<BufferedImage> images = ImageProcessor.loadImages("images/" + category);
            if (images.size() != 15) {
                System.err.println("Warning: Expected 15 images in " + category + ", found " + images.size());
            }
            //Add first 10 images to training, last 5 to testing
            for (int i = 0; i < images.size(); i++) {
                if (i < 10) {
                    trainingImages.add(images.get(i));
                } else {
                    testImages.add(images.get(i));
                }
            }
        }
        //extract color blocks from training images
        List<int[]> redBlocks = new ArrayList<>();
        List<int[]> greenBlocks = new ArrayList<>();
        List<int[]> blueBlocks = new ArrayList<>();
        for (BufferedImage img : trainingImages) {
            redBlocks.addAll(ImageProcessor.extractBlocks(img, 'R'));
            greenBlocks.addAll(ImageProcessor.extractBlocks(img, 'G'));
            blueBlocks.addAll(ImageProcessor.extractBlocks(img, 'B'));
        }

        int[][] redCodebook = VectorQuantizer.generateCodebook(redBlocks, 256);
        int[][] greenCodebook = VectorQuantizer.generateCodebook(greenBlocks, 256);
        int[][] blueCodebook = VectorQuantizer.generateCodebook(blueBlocks, 256);
        //Iterate through each test image, get its dimensions
        for (int i = 0; i < testImages.size(); i++) {
            BufferedImage original = testImages.get(i);
            int width = original.getWidth();
            int height = original.getHeight();

            // RGB Compression
            List<int[]> testRedBlocks = ImageProcessor.extractBlocks(original, 'R');
            List<int[]> testGreenBlocks = ImageProcessor.extractBlocks(original, 'G');
            List<int[]> testBlueBlocks = ImageProcessor.extractBlocks(original, 'B');
            //Compress each color component using the codebook generated from training images
            int[][] redIndices = VectorQuantizer.compressComponent(testRedBlocks, redCodebook);
            int[][] greenIndices = VectorQuantizer.compressComponent(testGreenBlocks, greenCodebook);
            int[][] blueIndices = VectorQuantizer.compressComponent(testBlueBlocks, blueCodebook);

            BufferedImage redComponent = ImageProcessor.reconstructComponent(redIndices, redCodebook, width, height);
            BufferedImage greenComponent = ImageProcessor.reconstructComponent(greenIndices, greenCodebook, width, height);
            BufferedImage blueComponent = ImageProcessor.reconstructComponent(blueIndices, blueCodebook, width, height);

            BufferedImage reconstructed = ImageProcessor.combineRGB(redComponent, greenComponent, blueComponent);
            ImageProcessor.saveImage(reconstructed, "output/reconstructed_" + i + ".png");

            // RGB Compression Ratio
            long originalSize = width * height * 3L * 8;
            long rgbCompressedSize = (long) Math.ceil((double) (width * height) / 4.0) * 8; // Single index per block
            double rgbCompressionRatio = (double) originalSize / rgbCompressedSize;
            System.out.printf("Image %d RGB Compression Ratio: %.2f\n", i, rgbCompressionRatio);
            System.out.println("Image " + i + ": width=" + width + ", height=" + height + ", RGB compressedSize=" + rgbCompressedSize);

            // RGB MSE
            double mse = calculateMSE(original, reconstructed);
            System.out.printf("Image %d RGB MSE: %.2f\n", i, mse);

            // YUV Compression
            try {
                BufferedImage[] yuv = YUVConverter.rgbToYUV(original);
                BufferedImage yComponent = yuv[0];
                BufferedImage uComponent = YUVConverter.subsample(yuv[1]);
                BufferedImage vComponent = YUVConverter.subsample(yuv[2]);
                //prepare YUV components for compression
                List<int[]> yBlocks = new ArrayList<>();
                List<int[]> uBlocks = new ArrayList<>();
                List<int[]> vBlocks = new ArrayList<>();
                for (BufferedImage img : trainingImages) {
                    BufferedImage[] yuvTrain = YUVConverter.rgbToYUV(img);
                    yBlocks.addAll(ImageProcessor.extractBlocks(yuvTrain[0], 'R'));
                    uBlocks.addAll(ImageProcessor.extractBlocks(YUVConverter.subsample(yuvTrain[1]), 'R'));
                    vBlocks.addAll(ImageProcessor.extractBlocks(YUVConverter.subsample(yuvTrain[2]), 'R'));
                }

                int[][] yCodebook = VectorQuantizer.generateCodebook(yBlocks, 256);
                int[][] uCodebook = VectorQuantizer.generateCodebook(uBlocks, 256);
                int[][] vCodebook = VectorQuantizer.generateCodebook(vBlocks, 256);
                 //Extract blocks from test image YUV
                List<int[]> testYBlocks = ImageProcessor.extractBlocks(yComponent, 'R');
                List<int[]> testUBlocks = ImageProcessor.extractBlocks(uComponent, 'R');
                List<int[]> testVBlocks = ImageProcessor.extractBlocks(vComponent, 'R');

                int[][] yIndices = VectorQuantizer.compressComponent(testYBlocks, yCodebook);
                int[][] uIndices = VectorQuantizer.compressComponent(testUBlocks, uCodebook);
                int[][] vIndices = VectorQuantizer.compressComponent(testVBlocks, vCodebook);

                BufferedImage reconY = ImageProcessor.reconstructComponent(yIndices, yCodebook, width, height);
                BufferedImage reconU = ImageProcessor.reconstructComponent(uIndices, uCodebook, uComponent.getWidth(), uComponent.getHeight());
                BufferedImage reconV = ImageProcessor.reconstructComponent(vIndices, vCodebook, vComponent.getWidth(), vComponent.getHeight());

                BufferedImage upsampledU = YUVConverter.upsample(reconU, width, height);
                BufferedImage upsampledV = YUVConverter.upsample(reconV, width, height);

                BufferedImage reconstructedYUV = YUVConverter.yuvToRGB(reconY, upsampledU, upsampledV);
                ImageProcessor.saveImage(reconstructedYUV, "output/reconstructed_yuv_" + i + ".png");

                // YUV Compression Ratio
                long yBlockCount = (long) Math.ceil((double) (width * height) / 4.0);
                long uvBlockCount = (long) Math.ceil((double) (width / 2 * height / 2) / 4.0);
                long yuvCompressedSize = yBlockCount * 8 + 2 * uvBlockCount * 8; // Standard 4:2:0
                double yuvCompressionRatio = (double) originalSize / yuvCompressedSize;
                System.out.printf("Image %d YUV Compression Ratio: %.2f\n", i, yuvCompressionRatio);
                System.out.println("Image " + i + ": width=" + width + ", height=" + height + ", Y blocks=" + yBlockCount +
                                  ", U/V blocks=" + uvBlockCount + ", YUV compressedSize=" + yuvCompressedSize);

                // Experimental YUV Compression Ratio
                long yuvCompressedSizeExp = yBlockCount * 8 + uvBlockCount * 8; // Reduced U/V contribution
                double yuvCompressionRatioExp = (double) originalSize / yuvCompressedSizeExp;
                System.out.printf("Image %d YUV Experimental Compression Ratio: %.2f\n", i, yuvCompressionRatioExp);

                // YUV MSE
                mse = calculateMSE(original, reconstructedYUV);
                System.out.printf("Image %d YUV MSE: %.2f\n", i, mse);
            } catch (Exception e) {
                System.err.println("Error processing YUV for Image " + i + ": " + e.getMessage());
            }
        }
    }

    private static double calculateMSE(BufferedImage original, BufferedImage reconstructed) {
        int width = original.getWidth();
        int height = original.getHeight();
        double mse = 0;
        //Loop over each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //Extract R, G, B channels
                int origRGB = original.getRGB(x, y);
                int reconRGB = reconstructed.getRGB(x, y);
                int r1 = (origRGB >> 16) & 0xFF;
                int g1 = (origRGB >> 8) & 0xFF;
                int b1 = origRGB & 0xFF;
                int r2 = (reconRGB >> 16) & 0xFF;
                int g2 = (reconRGB >> 8) & 0xFF;
                int b2 = reconRGB & 0xFF;
                mse += Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2);
            }
        }
        return mse / (width * height * 3);
    }
}