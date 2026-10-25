package benchmark;

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
public class DenseBenchmarkRunner {
    @Param({"100", "200", "300", "400", "500"})
    public int MATRIX_SIZE;

    private int[][] matrixA;
    private int[][] matrixB;

    @Setup(Level.Trial)
    public void setup() {
        matrixA = MatrixGenerator.generateMatrix(MATRIX_SIZE, 777);
        matrixB = MatrixGenerator.generateMatrix(MATRIX_SIZE, 778);
    }

    @Benchmark
    public void multiplyBaseline(Blackhole blackhole) {
        blackhole.consume(MatrixMultiplier.multiply(matrixA, matrixB));
    }

    @Benchmark
    public void multiplyLoopReordered(Blackhole blackhole) {
        blackhole.consume(MatrixMultiplier.multiplyLoopReordered(matrixA, matrixB));
    }

    @Benchmark
    public void multiplyBlocked(Blackhole blackhole) {
        blackhole.consume(MatrixMultiplier.multiplyBlocked(matrixA, matrixB, 32));
    }
}
