import math

import matplotlib.pyplot as plt

from control.csv_manager import (
    ROOT,
    DENSE_METHODS,
    SPARSE_METHODS,
    METHOD_LABELS,
    CsvManager,
)

RESULTS_CSV = "results.csv"
RESULTS_DIR = ROOT / "results"
PLOTS_DIR = RESULTS_DIR / "plots"


def main():
    rows = CsvManager.read_results(CsvManager.find_csv(RESULTS_CSV))
    PLOTS_DIR.mkdir(parents=True, exist_ok=True)

    create_dense_plots(rows)
    create_sparse_plots(rows)

    print(f"Plots saved to: {PLOTS_DIR}")
    print("Finished process.")


def create_dense_plots(rows):
    dense_rows = [row for row in rows if row["experiment"] == "dense"]
    if not dense_rows:
        return

    plot_time_comparison(
        dense_rows,
        DENSE_METHODS,
        "Dense multiplication — execution time",
        PLOTS_DIR / "dense_time_comparison.png",
    )
    plot_allocated_memory(
        dense_rows,
        DENSE_METHODS,
        "Dense multiplication — allocated memory",
        PLOTS_DIR / "dense_allocated_memory.png",
    )


def create_sparse_plots(rows):
    sparse_rows = [row for row in rows if row["experiment"] == "sparse"]
    sparsity_levels = sorted({row["sparsity"] for row in sparse_rows if row["sparsity"] is not None})

    for sparsity in sparsity_levels:
        level_rows = [
            row for row in sparse_rows
            if row["sparsity"] is not None and math.isclose(row["sparsity"], sparsity)
        ]
        suffix = format_sparsity(sparsity)

        plot_time_comparison(
            level_rows,
            SPARSE_METHODS,
            f"Sparse multiplication — {sparsity * 100:g}% sparsity",
            PLOTS_DIR / f"sparse_time_{suffix}.png",
        )
        plot_allocated_memory(
            level_rows,
            SPARSE_METHODS,
            f"Sparse multiplication — allocated memory ({sparsity * 100:g}% sparsity)",
            PLOTS_DIR / f"sparse_allocated_memory_{suffix}.png",
        )
        plot_sparse_density(
            level_rows,
            sparsity,
            PLOTS_DIR / f"sparse_density_{suffix}.png",
        )


def get_method_rows(rows, method):
    return sorted(
        (row for row in rows if row["method"] == method),
        key=lambda row: row["matrix_size"],
    )


def plot_time_comparison(rows, methods, title, output):
    fig, ax = plt.subplots(figsize=(9, 5.5))
    plotted = False

    for method in methods:
        method_rows = get_method_rows(rows, method)
        if not method_rows:
            continue

        sizes = [row["matrix_size"] for row in method_rows]
        means = [row["mean_time_ms"] for row in method_rows]
        medians = [row["median_time_ms"] for row in method_rows]
        sd = [math.sqrt(max(0.0, row["variance_time_ms2"])) for row in method_rows]

        lower_sd = [min(deviation, mean * 0.99) for mean, deviation in zip(means, sd)]
        mean_plot = ax.errorbar(
            sizes,
            means,
            yerr=[lower_sd, sd],
            marker="o",
            linestyle="-",
            capsize=4,
            label=f"{METHOD_LABELS.get(method, method)} mean ± SD",
        )
        color = mean_plot.lines[0].get_color()
        ax.plot(
            sizes,
            medians,
            marker="x",
            linestyle="--",
            color=color,
            label=f"{METHOD_LABELS.get(method, method)} median",
        )
        plotted = True

    if not plotted:
        plt.close(fig)
        return

    sizes = sorted({row["matrix_size"] for row in rows})
    ax.set_title(title)
    ax.set_xlabel("Matrix size (n × n)")
    ax.set_ylabel("Execution time (ms, logarithmic scale)")
    ax.set_xticks(sizes)
    ax.set_yscale("log")
    ax.grid(True, which="both", alpha=0.3)
    ax.legend()
    fig.tight_layout()
    fig.savefig(output, dpi=200)
    plt.close(fig)
    print(f"Created plot: {output}")


def plot_allocated_memory(rows, methods, title, output):
    fig, ax = plt.subplots(figsize=(9, 5.5))
    plotted = False

    for method in methods:
        method_rows = [
            row for row in get_method_rows(rows, method)
            if row["allocated_bytes_per_op"] is not None
            and math.isfinite(row["allocated_bytes_per_op"])
            and row["allocated_bytes_per_op"] > 0
        ]
        if not method_rows:
            continue

        sizes = [row["matrix_size"] for row in method_rows]
        memory = [row["allocated_bytes_per_op"] for row in method_rows]
        ax.plot(
            sizes,
            memory,
            marker="o",
            linestyle="-",
            label=METHOD_LABELS.get(method, method),
        )
        plotted = True

    if not plotted:
        plt.close(fig)
        return

    sizes = sorted({row["matrix_size"] for row in rows})
    ax.set_title(title)
    ax.set_xlabel("Matrix size (n × n)")
    ax.set_ylabel("Allocated memory (bytes/op, logarithmic scale)")
    ax.set_xticks(sizes)
    ax.set_yscale("log")
    ax.grid(True, which="both", alpha=0.3)
    ax.legend()
    fig.tight_layout()
    fig.savefig(output, dpi=200)
    plt.close(fig)
    print(f"Created plot: {output}")


def plot_sparse_density(rows, sparsity, output):
    metadata_rows = sorted(
        (row for row in rows if row["method"] == "csr"),
        key=lambda row: row["matrix_size"],
    )
    if not metadata_rows:
        metadata_rows = sorted(rows, key=lambda row: row["matrix_size"])

    by_size = {}
    for row in metadata_rows:
        by_size[row["matrix_size"]] = row
    metadata_rows = list(by_size.values())

    if not metadata_rows or any(
        row["input_nnz_a"] is None or row["input_nnz_b"] is None or row["output_nnz"] is None
        for row in metadata_rows
    ):
        return

    sizes = [row["matrix_size"] for row in metadata_rows]
    density_a = [100.0 * row["input_nnz_a"] / (row["matrix_size"] ** 2) for row in metadata_rows]
    density_b = [100.0 * row["input_nnz_b"] / (row["matrix_size"] ** 2) for row in metadata_rows]
    density_output = [100.0 * row["output_nnz"] / (row["matrix_size"] ** 2) for row in metadata_rows]

    fig, ax = plt.subplots(figsize=(9, 5.5))
    ax.plot(sizes, density_a, marker="o", label="Input A non-zero density")
    ax.plot(sizes, density_b, marker="s", label="Input B non-zero density")
    ax.plot(sizes, density_output, marker="^", label="Output non-zero density")
    ax.set_title(f"Sparse multiplication — actual density at {sparsity * 100:g}% sparsity")
    ax.set_xlabel("Matrix size (n × n)")
    ax.set_ylabel("Non-zero entries (%)")
    ax.set_xticks(sizes)
    ax.set_ylim(bottom=0)
    ax.grid(True, alpha=0.3)
    ax.legend()
    fig.tight_layout()
    fig.savefig(output, dpi=200)
    plt.close(fig)
    print(f"Created plot: {output}")


def format_sparsity(sparsity):
    return f"{sparsity * 100:g}".replace(".", "p") + "pct"


if __name__ == "__main__":
    main()
