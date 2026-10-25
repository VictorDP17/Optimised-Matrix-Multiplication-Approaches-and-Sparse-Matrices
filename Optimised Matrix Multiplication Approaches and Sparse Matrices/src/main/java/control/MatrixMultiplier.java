package control;

public class MatrixMultiplier {
    public static int[][] multiply(int[][] matrixA, int[][] matrixB) {
        int n = matrixA.length;
        int[][] matrixC = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    matrixC[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
        return matrixC;
    }

    public static int[][] multiplyLoopReordered(int[][] matrixA, int[][] matrixB) {
        int n = matrixA.length;
        int[][] matrixC = new int[n][n];

        for (int i = 0; i < n; i++) {
            int[] rowA = matrixA[i];
            int[] rowC = matrixC[i];

            for (int k = 0; k < n; k++) {
                int valueA = rowA[k];
                int[] rowB = matrixB[k];

                for (int j = 0; j < n; j++) {
                    rowC[j] += valueA * rowB[j];
                }
            }
        }
        return matrixC;
    }

    public static int[][] multiplyBlocked(int[][] matrixA, int[][] matrixB, int blockSize) {
        if (blockSize <= 0) {
            throw new IllegalArgumentException("Block size must be positive");
        }
        int n = matrixA.length;
        int[][] matrixC = new int[n][n];

        for (int ii = 0; ii < n; ii += blockSize) {
            int iEnd = Math.min(ii + blockSize, n);
            for (int kk = 0; kk < n; kk += blockSize) {
                int kEnd = Math.min(kk + blockSize, n);
                for (int jj = 0; jj < n; jj += blockSize) {
                    int jEnd = Math.min(jj + blockSize, n);
                    multiplyBlock(matrixA, matrixB, ii, iEnd, matrixC, kk, kEnd, jj, jEnd);
                }
            }
        }
        return matrixC;
    }

    private static void multiplyBlock(int[][] matrixA, int[][] matrixB, int ii, int iEnd, int[][] matrixC, int kk, int kEnd, int jj, int jEnd) {
        for (int i = ii; i < iEnd; i++) {
            int[] rowA = matrixA[i];
            int[] rowC = matrixC[i];

            for (int k = kk; k < kEnd; k++) {
                int valueA = rowA[k];
                int[] rowB = matrixB[k];

                for (int j = jj; j < jEnd; j++) {
                    rowC[j] += valueA * rowB[j];
                }
            }
        }
    }
}
