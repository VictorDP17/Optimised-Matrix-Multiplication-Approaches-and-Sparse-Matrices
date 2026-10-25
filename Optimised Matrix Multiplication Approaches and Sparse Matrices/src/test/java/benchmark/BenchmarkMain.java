package benchmark;

import control.CSRMatrix;
import control.MatrixGenerator;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.util.Statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.openjdk.jmh.annotations.Mode;
import java.util.concurrent.TimeUnit;

public class BenchmarkMain {
    private static final String SUMMARY_CSV = "results.csv";
    private static final String RAW_RESULTS_JSON = "jmh_raw.json";

    public static void main(String[] args) throws Exception {
        Options options = new OptionsBuilder()
                .include(".*(DenseBenchmarkRunner|SparseBenchmarkRunner).*\\..*")
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.MILLISECONDS)
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(1))
                .forks(1)
                .addProfiler(GCProfiler.class)
                .result(RAW_RESULTS_JSON)
                .resultFormat(ResultFormatType.JSON)
                .build();

        Collection<RunResult> runResults = new Runner(options).run();
        saveSummary(runResults);

        System.out.println("Summary saved to " + SUMMARY_CSV);
        System.out.println("JMH results saved to " + RAW_RESULTS_JSON);
    }

    private static void saveSummary(Collection<RunResult> results)throws IOException {
        Map<String, SparseMetadata> metadataCache = new HashMap<>();

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(SUMMARY_CSV), StandardCharsets.UTF_8)) {
            writeCsvHeader(writer);

            for (RunResult run : results) {
                writeSummaryRow(writer, run, metadataCache);
            }
        }
    }

    private static void writeCsvHeader(BufferedWriter writer) throws IOException {
        writer.write(
                "experiment,method,matrix_size,sparsity,block_size,numeric_type,"
                        + "warmup_iterations,measurement_iterations,threads,"
                        + "mean_time_ms,median_time_ms,variance_time_ms2,"
                        + "allocated_bytes_per_op,input_nnz_a,input_nnz_b,output_nnz"
        );
        writer.newLine();
    }

    private static void writeSummaryRow(
            BufferedWriter writer,
            RunResult run,
            Map<String, SparseMetadata> metadataCache
    ) throws IOException {

        String benchmarkName = run.getParams().getBenchmark();
        String methodName = benchmarkName.substring(
                benchmarkName.lastIndexOf('.') + 1
        );

        boolean sparse = benchmarkName.contains("SparseBenchmarkRunner");
        String experiment = sparse ? "sparse" : "dense";

        String method = normalizeMethod(methodName);
        int size = Integer.parseInt(run.getParams().getParam("MATRIX_SIZE"));

        String sparsity = sparse ? run.getParams().getParam("SPARSITY") : "";
        String blockSize = method.equals("blocked") ? "32" : "";

        Statistics stats = run.getPrimaryResult().getStatistics();
        double allocatedBytes = getAllocatedBytesPerOp(run);

        SparseMetadata metadata = sparse ? getSparseMetadata(size, sparsity, metadataCache) : null;
        int inputNnzA = metadata == null ? -1 : metadata.inputNnzA;
        int inputNnzB = metadata == null ? -1 : metadata.inputNnzB;
        int outputNnz = metadata == null ? -1 : metadata.outputNnz;

        writeCsvRow(
                writer,
                experiment,
                method,
                size,
                sparsity,
                blockSize,
                stats,
                allocatedBytes,
                inputNnzA,
                inputNnzB,
                outputNnz
        );
    }

    private static double getAllocatedBytesPerOp(RunResult run) {
        Result<?> memoryResult = run.getSecondaryResults()
                .get("gc.alloc.rate.norm");

        return memoryResult == null ? Double.NaN : memoryResult.getScore();
    }

    private static SparseMetadata getSparseMetadata(
            int size,
            String sparsity,
            Map<String, SparseMetadata> cache
    ) {
        String key = size + ":" + sparsity;

        return cache.computeIfAbsent(
                key,
                ignored -> calculateSparseMetadata(
                        size,
                        Double.parseDouble(sparsity)
                )
        );
    }

    private static void writeCsvRow(
            BufferedWriter writer,
            String experiment,
            String method,
            int size,
            String sparsity,
            String blockSize,
            Statistics stats,
            double allocatedBytes,
            int inputNnzA,
            int inputNnzB,
            int outputNnz
    ) throws IOException {

        writer.write(String.format(
                Locale.US,
                "%s,%s,%d,%s,%s,int,1,5,1,%.9f,%.9f,%.9f,%.3f,%s,%s,%s",
                experiment,
                method,
                size,
                sparsity,
                blockSize,
                stats.getMean(),
                stats.getPercentile(50),
                stats.getVariance(),
                allocatedBytes,
                csvNumber(inputNnzA),
                csvNumber(inputNnzB),
                csvNumber(outputNnz)
        ));
        writer.newLine();
    }

    private static String normalizeMethod(String method) {
        return switch (method) {
            case "multiplyBaseline" -> "baseline";
            case "multiplyLoopReordered" -> "loop_reordered";
            case "multiplyBlocked" -> "blocked";
            case "multiplyDenseBaselineOnSparseInputs" -> "dense_baseline";
            case "multiplyCSR" -> "csr";
            default -> throw new IllegalArgumentException("Unknown benchmark method: " + method);
        };
    }

    private static String csvNumber(int value) {
        return value < 0 ? "" : Integer.toString(value);
    }

    private static SparseMetadata calculateSparseMetadata(int size, double sparsity) {
        int[][] denseA = MatrixGenerator.generateSparseMatrix(size, sparsity, 777);
        int[][] denseB = MatrixGenerator.generateSparseMatrix(size, sparsity, 778);
        CSRMatrix csrA = CSRMatrix.fromDense(denseA);
        CSRMatrix csrB = CSRMatrix.fromDense(denseB);
        int outputNnz = CSRMatrix.multiply(csrA, csrB).nonZeroCount();

        return new SparseMetadata(csrA.nonZeroCount(), csrB.nonZeroCount(), outputNnz);
    }

    private static final class SparseMetadata {
        private final int inputNnzA;
        private final int inputNnzB;
        private final int outputNnz;

        private SparseMetadata(int inputNnzA, int inputNnzB, int outputNnz) {
            this.inputNnzA = inputNnzA;
            this.inputNnzB = inputNnzB;
            this.outputNnz = outputNnz;
        }
    }
}

