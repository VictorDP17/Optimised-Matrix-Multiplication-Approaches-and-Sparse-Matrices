package control;

import java.util.Random;

public class MatrixGenerator {
    public static int[][] generateMatrix(int n, int seed) {
        Random random = new Random(seed);
        int[][] matrix = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = random.nextInt(100);
            }
        }
        return matrix;
    }

    public static int[][] generateSparseMatrix(int n, double sparsity, int seed) {
        if (sparsity < 0.0 || sparsity > 1.0) {
            throw new IllegalArgumentException("Sparsity must be between 0 and 1");
        }

        Random random = new Random(seed);
        int[][] matrix = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (random.nextDouble() > sparsity) {
                    matrix[i][j] = 1 + random.nextInt(9);
                }
            }
        }
        return matrix;
    }

    public static int[][] generateIdentityMatrix(int n) {
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            matrix[i][i] = 1;
        }
        return matrix;
    }

    public static int[][] generateZeroMatrix(int n) {
        return new int[n][n];
    }

    public static int countNonZero(int[][] matrix) {
        int count = 0;
        for (int[] row : matrix) {
            for (int value : row) {
                if (value != 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
