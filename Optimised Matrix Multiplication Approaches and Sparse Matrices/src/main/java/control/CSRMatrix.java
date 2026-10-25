package control;

import java.util.Arrays;

/** Compressed Sparse Row (CSR) representation of a square integer matrix. */
public final class CSRMatrix {
    private final int size;
    private final int[] rowPointers;
    private final int[] columnIndices;
    private final int[] values;

    private CSRMatrix(int size, int[] rowPointers, int[] columnIndices, int[] values) {
        this.size = size;
        this.rowPointers = rowPointers;
        this.columnIndices = columnIndices;
        this.values = values;
    }

    public int size() {
        return size;
    }

    public int nonZeroCount() {
        return values.length;
    }

    public long estimatedStorageBytes() {
        return (long) rowPointers.length * Integer.BYTES
                + (long) columnIndices.length * Integer.BYTES
                + (long) values.length * Integer.BYTES;
    }

    public static CSRMatrix fromDense(int[][] matrix) {
        int n = matrix.length;
        int nnz = MatrixGenerator.countNonZero(matrix);
        int[] rowPointers = new int[n + 1];
        int[] columnIndices = new int[nnz];
        int[] values = new int[nnz];
        int index = 0;

        for (int i = 0; i < n; i++) {
            rowPointers[i] = index;
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] != 0) {
                    columnIndices[index] = j;
                    values[index] = matrix[i][j];
                    index++;
                }
            }
        }
        rowPointers[n] = index;
        return new CSRMatrix(n, rowPointers, columnIndices, values);
    }

    public int[][] toDense() {
        int[][] dense = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int p = rowPointers[i]; p < rowPointers[i + 1]; p++) {
                dense[i][columnIndices[p]] = values[p];
            }
        }
        return dense;
    }

    public static CSRMatrix multiply(CSRMatrix a, CSRMatrix b) {
        if (a.size != b.size) {
            throw new IllegalArgumentException("Matrix sizes must match");
        }

        int n = a.size;
        int[] resultRowPointers = new int[n + 1];
        int[] accumulator = new int[n];
        int[] marks = new int[n];
        int[] touchedColumns = new int[n];

        int initialCapacity = Math.max(16, Math.min(n * n, a.nonZeroCount() + b.nonZeroCount()));
        IntBuffer resultColumns = new IntBuffer(initialCapacity);
        IntBuffer resultValues = new IntBuffer(initialCapacity);

        for (int i = 0; i < n; i++) {
            int touchedCount = 0;
            int stamp = i + 1;

            for (int ap = a.rowPointers[i]; ap < a.rowPointers[i + 1]; ap++) {
                int k = a.columnIndices[ap];
                int valueA = a.values[ap];

                for (int bp = b.rowPointers[k]; bp < b.rowPointers[k + 1]; bp++) {
                    int j = b.columnIndices[bp];
                    if (marks[j] != stamp) {
                        marks[j] = stamp;
                        accumulator[j] = 0;
                        touchedColumns[touchedCount++] = j;
                    }
                    accumulator[j] += valueA * b.values[bp];
                }
            }

            Arrays.sort(touchedColumns, 0, touchedCount);
            for (int t = 0; t < touchedCount; t++) {
                int j = touchedColumns[t];
                if (accumulator[j] != 0) {
                    resultColumns.add(j);
                    resultValues.add(accumulator[j]);
                }
            }
            resultRowPointers[i + 1] = resultColumns.size();
        }
        return new CSRMatrix(n, resultRowPointers, resultColumns.toArray(), resultValues.toArray());
    }

    private static final class IntBuffer {
        private int[] data;
        private int size;

        private IntBuffer(int initialCapacity) {
            data = new int[Math.max(1, initialCapacity)];
        }

        private void add(int value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = value;
        }

        private int size() {
            return size;
        }

        private int[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
