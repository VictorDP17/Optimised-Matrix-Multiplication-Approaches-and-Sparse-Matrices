package benchmark;

import control.CSRMatrix;
import control.MatrixGenerator;
import control.MatrixMultiplier;

import java.util.Arrays;

public class CorrectnessTest {
    public static void main(String[] args) {
        int[][] a = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        int[][] b = {
                {9, 8, 7},
                {6, 5, 4},
                {3, 2, 1}
        };
        int[][] expected = {
                {30, 24, 18},
                {84, 69, 54},
                {138, 114, 90}
        };

        assertMatrixEquals(expected, MatrixMultiplier.multiply(a, b), "baseline");
        assertMatrixEquals(expected, MatrixMultiplier.multiplyLoopReordered(a, b), "loop reordered");
        assertMatrixEquals(expected, MatrixMultiplier.multiplyBlocked(a, b, 2), "blocked");
        assertMatrixEquals(expected, CSRMatrix.multiply(CSRMatrix.fromDense(a), CSRMatrix.fromDense(b)).toDense(), "CSR");

        int[][] sparseA = MatrixGenerator.generateSparseMatrix(12, 0.8, 777);
        int[][] sparseB = MatrixGenerator.generateSparseMatrix(12, 0.8, 778);
        int[][] denseResult = MatrixMultiplier.multiply(sparseA, sparseB);
        int[][] csrResult = CSRMatrix.multiply(CSRMatrix.fromDense(sparseA), CSRMatrix.fromDense(sparseB)).toDense();
        assertMatrixEquals(denseResult, csrResult, "sparse input comparison");

        System.out.println("All Assignment correctness checks passed.");
    }

    private static void assertMatrixEquals(int[][] expected, int[][] actual, String testName) {
        if (!Arrays.deepEquals(expected, actual)) {
            throw new AssertionError("Correctness test failed: " + testName);
        }
    }
}
