import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {
    // Load images from a directory
    public static List<BufferedImage> loadImages(String directoryPath) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IOException("Directory not found: " + directoryPath);
        }
        // Get all .jpg files in the directory
        File[] files = dir.listFiles((_, name) -> name.endsWith(".jpg"));
        if (files == null) {
            throw new IOException("No images found in: " + directoryPath);
        }
        for (File file : files) {
            images.add(ImageIO.read(file));
        }
        return images;
    }

    // Extract 2x2 pixel blocks for a specific RGB component
    public static List<int[]> extractBlocks(BufferedImage image, char component) {
        List<int[]> blocks = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height - 1; y += 2) {
            for (int x = 0; x < width - 1; x += 2) {
                int[] block = new int[4];//store 2X2 Block
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        int rgb = image.getRGB(x + j, y + i);// Get RGB value of pixel
                        int value;
                        switch (component) {
                            case 'R': value = (rgb >> 16) & 0xFF; break;// Extract red component
                            case 'G': value = (rgb >> 8) & 0xFF; break;
                            case 'B': value = rgb & 0xFF; break;
                            default: throw new IllegalArgumentException("Invalid component: " + component);
                        }
                        block[i * 2 + j] = value;
                    }
                }
                blocks.add(block);
            }
        }
        return blocks;
    }

    // Reconstruct image component from codebook indices
    public static BufferedImage reconstructComponent(int[][] indices, int[][] codebook, int width, int height) {
        BufferedImage component = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int index = 0;//Counter to keep track of which block we are on
        for (int y = 0; y < height - 1; y += 2) {// Loop through each 2x2 block in the image
            for (int x = 0; x < width - 1; x += 2) {
                if (index < indices.length) {
                    int[] codeVector = codebook[indices[index][0]];
                    for (int i = 0; i < 2; i++) {
                        for (int j = 0; j < 2; j++) {
                            if (x + j < width && y + i < height) {
                                int value = codeVector[i * 2 + j];
                                
                                component.setRGB(x + j, y + i, (value << 16) | (value << 8) | value);
                            }
                        }
                    }
                    index++;
                }
            }
        }
        return component;
    }

    // Combine RGB components into a color image
    public static BufferedImage combineRGB(BufferedImage r, BufferedImage g, BufferedImage b) {
        int width = r.getWidth();
        int height = r.getHeight();
        BufferedImage colorImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
       //Loop through every pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = (r.getRGB(x, y) >> 16) & 0xFF;
                int green = (g.getRGB(x, y) >> 8) & 0xFF;
                int blue = b.getRGB(x, y) & 0xFF;
                int rgb = (red << 16) | (green << 8) | blue;
                colorImage.setRGB(x, y, rgb);
            }
        }
        return colorImage;
    }

    // Save image to file
    public static void saveImage(BufferedImage image, String path) throws IOException {
        File outputFile = new File(path);
        outputFile.getParentFile().mkdirs(); 
        ImageIO.write(image, "png", outputFile);
    }
}