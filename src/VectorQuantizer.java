import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VectorQuantizer {
    // Generate codebook using k-means clustering
    public static int[][] generateCodebook(List<int[]> blocks, int codebookSize) {
        
        int[][] codebook = new int[codebookSize][4];
        Random rand = new Random();

        // Initialize centroids randomly
        for (int i = 0; i < codebookSize; i++) {
            codebook[i] = blocks.get(rand.nextInt(blocks.size())).clone();
        }

        // K-means clustering 
        for (int iter = 0; iter < 10; iter++) {
            //Initializes a list of codebookSize clusters to group blocks that belong to each centroid
            List<List<int[]>> clusters = new ArrayList<>();
            for (int i = 0; i < codebookSize; i++) {
                clusters.add(new ArrayList<>());
            }

            // Assign blocks to nearest centroid
            for (int[] block : blocks) {
                int nearest = findNearestCentroid(block, codebook);
                clusters.get(nearest).add(block);
            }

            // Update centroids
            for (int i = 0; i < codebookSize; i++) {
                if (!clusters.get(i).isEmpty()) {
                    int[] newCentroid = new int[4];
                    for (int[] block : clusters.get(i)) {
                        for (int j = 0; j < 4; j++) {
                            newCentroid[j] += block[j];
                        }
                    }
                    for (int j = 0; j < 4; j++) {
                        newCentroid[j] /= clusters.get(i).size();
                    }
                    codebook[i] = newCentroid;
                }
            }
        }
        return codebook;
    }

    // Find nearest codebook vector
    private static int findNearestCentroid(int[] block, int[][] codebook) {
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < codebook.length; i++) {
            double dist = 0;
            for (int j = 0; j < 4; j++) {
                dist += Math.pow(block[j] - codebook[i][j], 2);
            }
            if (dist < minDist) {
                minDist = dist;
                nearest = i;
            }
        }
        return nearest;
    }

    // Compress image component to codebook indices
    public static int[][] compressComponent(List<int[]> blocks, int[][] codebook) {
        int[][] indices = new int[blocks.size()][1];
        for (int i = 0; i < blocks.size(); i++) {
            indices[i][0] = findNearestCentroid(blocks.get(i), codebook);
        }
        return indices;
    }
}