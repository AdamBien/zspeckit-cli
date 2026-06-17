package airhacks.specify.prerequisites.boundary;

import module java.base;
import org.json.JSONArray;
import org.json.JSONObject;
import airhacks.specify.prerequisites.control.DesignDocuments;
import airhacks.specify.template.control.Templates;
import airhacks.specify.workspace.control.Workspace;
import airhacks.specify.workspace.entity.FeaturePaths;

/// `setup-tasks` subcommand — port of `setup-tasks.sh`. Validates that the plan and
/// spec exist, resolves the tasks template, and lists the available design documents.
public final class SetupTasks {

    public int run(String[] args) {
        var json = false;
        for (var arg : args) {
            switch (arg) {
                case "--json" -> json = true;
                case "--help", "-h" -> {
                    IO.println("Usage: setup-tasks [--json]");
                    return 0;
                }
                default -> {
                    System.err.println("ERROR: Unknown option '" + arg + "'");
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

        if (!Files.isRegularFile(paths.implPlan())) {
            System.err.println("ERROR: plan.md not found in " + paths.featureDir());
            return 1;
        }
        if (!Files.isRegularFile(paths.featureSpec())) {
            System.err.println("ERROR: spec.md not found in " + paths.featureDir());
            return 1;
        }

        var tasksTemplate = Templates.resolve("tasks-template", paths.repoRoot());
        if (tasksTemplate.isEmpty()) {
            System.err.println("ERROR: Could not resolve required tasks-template from the template override stack for "
                    + paths.repoRoot());
            return 1;
        }

        var docs = DesignDocuments.list(paths);

        if (json) {
            var payload = new JSONObject()
                    .put("FEATURE_DIR", paths.featureDir().toString())
                    .put("AVAILABLE_DOCS", new JSONArray(docs))
                    .put("TASKS_TEMPLATE", tasksTemplate.get().toString());
            IO.println(payload.toString());
        } else {
            IO.println("FEATURE_DIR: " + paths.featureDir());
            IO.println("TASKS_TEMPLATE: " + tasksTemplate.get());
            IO.println("AVAILABLE_DOCS:");
            CheckPrerequisites.printBullets(docs);
        }
        return 0;
    }
}
