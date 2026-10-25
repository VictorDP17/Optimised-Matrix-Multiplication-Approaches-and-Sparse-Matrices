package benchmark;

import control.CSRMatrix;
import control.MatrixGenerator;
import control.MatrixMultiplier;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Thread)
public class SparseBenchmarkRunner {
    @Param({"100", "200", "300", "400", "500"})
    public int MATRIX_SIZE;

    @Param({"0.90", "0.95", "0.99"})
    public double SPARSITY;

    private int[][] denseSparseA;
    private int[][] denseSparseB;
    private CSRMatrix csrA;
    private CSRMatrix csrB;

    @Setup(Level.Trial)
    public void setup() {
        denseSparseA = MatrixGenerator.generateSparseMatrix(MATRIX_SIZE, SPARSITY, 777);
        denseSparseB = MatrixGenerator.generateSparseMatrix(MATRIX_SIZE, SPARSITY, 778);
        csrA = CSRMatrix.fromDense(denseSparseA);
        csrB = CSRMatrix.fromDense(denseSparseB);
    }

    @Benchmark
    public void multiplyDenseBaselineOnSparseInputs(Blackhole blackhole) {
        blackhole.consume(MatrixMultiplier.multiply(denseSparseA, denseSparseB));
    }

    @Benchmark
    public void multiplyCSR(Blackhole blackhole) {
        blackhole.consume(CSRMatrix.multiply(csrA, csrB));
    }
}
