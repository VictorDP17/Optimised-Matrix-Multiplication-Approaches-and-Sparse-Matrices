import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]

COLUMNS = [
    "experiment",
    "method",
    "matrix_size",
    "sparsity",
    "block_size",
    "numeric_type",
    "warmup_iterations",
    "measurement_iterations",
    "threads",
    "mean_time_ms",
    "median_time_ms",
    "variance_time_ms2",
    "allocated_bytes_per_op",
    "input_nnz_a",
    "input_nnz_b",
    "output_nnz",
]
DENSE_METHODS = ["baseline", "loop_reordered", "blocked"]
SPARSE_METHODS = ["dense_baseline", "csr"]
METHOD_LABELS = {
    "baseline": "Baseline (i-j-k)",
    "loop_reordered": "Loop reordered (i-k-j)",
    "blocked": "Blocked (32)",
    "dense_baseline": "Dense baseline",
    "csr": "Sparse CSR",
}


class CsvManager:
    @staticmethod
    def find_csv(filename):
        path = ROOT / filename
        if not path.is_file():
            raise FileNotFoundError(
                f"{filename} was not found in project root: {ROOT}"
            )
        return path


    @staticmethod
    def read_results(path):
        with path.open(newline="", encoding="utf-8-sig") as file:
            return [
                {
                    "experiment": row["experiment"],
                    "method": row["method"],
                    "matrix_size": int(row["matrix_size"]),
                    "sparsity": float(row["sparsity"]) if row["sparsity"] else None,
                    "block_size": int(row["block_size"]) if row["block_size"] else None,
                    "numeric_type": row["numeric_type"],
                    "warmup_iterations": int(row["warmup_iterations"]),
                    "measurement_iterations": int(row["measurement_iterations"]),
                    "threads": int(row["threads"]),
                    "mean_time_ms": float(row["mean_time_ms"]),
                    "median_time_ms": float(row["median_time_ms"]),
                    "variance_time_ms2": float(row["variance_time_ms2"]),
                    "allocated_bytes_per_op": (
                        float(row["allocated_bytes_per_op"])
                        if row["allocated_bytes_per_op"] else None
                    ),
                    "input_nnz_a": int(row["input_nnz_a"]) if row["input_nnz_a"] else None,
                    "input_nnz_b": int(row["input_nnz_b"]) if row["input_nnz_b"] else None,
                    "output_nnz": int(row["output_nnz"]) if row["output_nnz"] else None,

                }
                for row in csv.DictReader(file)
            ]
