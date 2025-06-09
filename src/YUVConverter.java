import java.awt.image.BufferedImage;


public class YUVConverter {
    // Convert RGB image to YUV components
    public static BufferedImage[] rgbToYUV(BufferedImage rgbImage) {
        int width = rgbImage.getWidth();
        int height = rgbImage.getHeight();
        BufferedImage yImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage uImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage vImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);


        //Loop over every pixel in the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = rgbImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;


                // RGB to YUV conversion
                int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int U = (int) (-0.147 * r - 0.289 * g + 0.436 * b + 128);
                int V = (int) (0.615 * r - 0.515 * g - 0.100 * b + 128);

                // Ensure Y, U, and V values are within the valid range
                Y = Math.max(0, Math.min(255, Y));
                U = Math.max(0, Math.min(255, U));
                V = Math.max(0, Math.min(255, V));

                // Set the Y, U, and V values in their respective images
                yImage.setRGB(x, y, (Y << 16) | (Y << 8) | Y);
                uImage.setRGB(x, y, (U << 16) | (U << 8) | U);
                vImage.setRGB(x, y, (V << 16) | (V << 8) | V);
            }
        }
        return new BufferedImage[]{yImage, uImage, vImage};
    }


    // Subsample U and V components to 50% width and height
    public static BufferedImage subsample(BufferedImage component) {
        int newWidth = component.getWidth() / 2;
        int newHeight = component.getHeight() / 2;
        BufferedImage subsampled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
        //Loop over the new image’s pixels
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                int value = (component.getRGB(x * 2, y * 2) >> 16) & 0xFF;
                subsampled.setRGB(x, y, (value << 16) | (value << 8) | value);
            }
        }
        return subsampled;
    }


    // Upsample U or V component back to original size
    public static BufferedImage upsample(BufferedImage component, int targetWidth, int targetHeight) {
        BufferedImage upsampled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        //Loop over each pixel in the upsampled image
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int srcX = Math.min(x / 2, component.getWidth() - 1);
                int srcY = Math.min(y / 2, component.getHeight() - 1);
                int value = (component.getRGB(srcX, srcY) >> 16) & 0xFF;
                upsampled.setRGB(x, y, (value << 16) | (value << 8) | value);
            }
        }
        return upsampled;
    }


    // Convert YUV back to RGB
    public static BufferedImage yuvToRGB(BufferedImage yImage, BufferedImage uImage, BufferedImage vImage) {
        int width = yImage.getWidth();
        int height = yImage.getHeight();
        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int Y = (yImage.getRGB(x, y) >> 16) & 0xFF;
                int U = (uImage.getRGB(x, y) >> 16) & 0xFF;
                int V = (vImage.getRGB(x, y) >> 16) & 0xFF;


                // YUV to RGB conversion
                int r = (int) (Y + 1.140 * (V - 128));
                int g = (int) (Y - 0.395 * (U - 128) - 0.581 * (V - 128));
                int b = (int) (Y + 2.032 * (U - 128));


                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));


                int rgb = (r << 16) | (g << 8) | b;
                rgbImage.setRGB(x, y, rgb);
            }
        }
        return rgbImage;
    }
}
