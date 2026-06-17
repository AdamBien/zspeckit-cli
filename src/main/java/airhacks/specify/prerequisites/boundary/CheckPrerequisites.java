package airhacks.specify.prerequisites.boundary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import airhacks.specify.workspace.control.Workspace;
import airhacks.specify.workspace.entity.FeaturePaths;

/// `check-prerequisites` subcommand — port of `check-prerequisites.sh`. Validates the
/// feature scaffold (or just reports paths) and lists the available design documents.
public final class CheckPrerequisites {

    public int run(String[] args) {
        var json = false;
        var requireTasks = false;
        var includeTasks = false;
        var pathsOnly = false;

        for (var arg : args) {
            switch (arg) {
                case "--json" -> json = true;
                case "--require-tasks" -> requireTasks = true;
                case "--include-tasks" -> includeTasks = true;
                case "--paths-only" -> pathsOnly = true;
                case "--help", "-h" -> {
                    IO.println("Usage: check-prerequisites [--json] [--require-tasks] [--include-tasks] [--paths-only]");
                    return 0;
                }
                default -> {
                    System.err.println("ERROR: Unknown option '" + arg + "'. Use --help for usage information.");
                    return 1;
                }
            }
        }

        FeaturePaths paths;
        try {
            paths = Workspace.featurePaths();
        } catch (RuntimeException failure) {
            System.err.println("ERROR: " + failure.getMessage());
            return 1;
        }

        if (pathsOnly) {
            emitPaths(json, paths);
            return 0;
        }

        if (!Files.isDirectory(paths.featureDir())) {
            System.err.println("ERROR: Feature directory not found: " + paths.featureDir());
            return 1;
        }
        if (!Files.isRegularFile(paths.implPlan())) {
            System.err.println("ERROR: plan.md not found in " + paths.featureDir());
            return 1;
        }
        if (requireTasks && !Files.isRegularFile(paths.tasks())) {
            System.err.println("ERROR: tasks.md not found in " + paths.featureDir());
            return 1;
        }

        var docs = Documents.list(paths);
        if (includeTasks && Files.isRegularFile(paths.tasks())) {
            docs.add("tasks.md");
        }

        if (json) {
            var payload = new JSONObject()
                    .put("FEATURE_DIR", paths.featureDir().toString())
                    .put("AVAILABLE_DOCS", new JSONArray(docs));
            IO.println(payload.toString());
        } else {
            IO.println("FEATURE_DIR:" + paths.featureDir());
            IO.println("AVAILABLE_DOCS:");
            docs.forEach(doc -> IO.println("  ✓ " + doc));
        }
        return 0;
    }

    static void emitPaths(boolean json, FeaturePaths paths) {
        if (json) {
            var payload = new JSONObject()
                    .put("REPO_ROOT", paths.repoRoot().toString())
                    .put("BRANCH", paths.currentBranch())
                    .put("FEATURE_DIR", paths.featureDir().toString())
                    .put("FEATURE_SPEC", paths.featureSpec().toString())
                    .put("IMPL_PLAN", paths.implPlan().toString())
                    .put("TASKS", paths.tasks().toString());
            IO.println(payload.toString());
        } else {
            IO.println("REPO_ROOT: " + paths.repoRoot());
            IO.println("BRANCH: " + paths.currentBranch());
            IO.println("FEATURE_DIR: " + paths.featureDir());
            IO.println("FEATURE_SPEC: " + paths.featureSpec());
            IO.println("IMPL_PLAN: " + paths.implPlan());
            IO.println("TASKS: " + paths.tasks());
        }
    }

    /// Optional design documents present in the feature directory, in the order
    /// `check-prerequisites.sh` reports them.
    interface Documents {

        static List<String> list(FeaturePaths paths) {
            var docs = new ArrayList<String>();
            if (Files.isRegularFile(paths.research())) {
                docs.add("research.md");
            }
            if (Files.isRegularFile(paths.dataModel())) {
                docs.add("data-model.md");
            }
            if (Files.isDirectory(paths.contractsDir()) && hasEntries(paths.contractsDir())) {
                docs.add("contracts/");
            }
            if (Files.isRegularFile(paths.quickstart())) {
                docs.add("quickstart.md");
            }
            return docs;
        }

        static boolean hasEntries(Path dir) {
            try (Stream<Path> entries = Files.list(dir)) {
                return entries.findAny().isPresent();
            } catch (IOException listFailure) {
                throw new UncheckedIOException(listFailure);
            }
        }
    }
}
