package airhacks.specify.prerequisites.boundary;

import module java.base;
import org.json.JSONObject;
import airhacks.specify.template.control.Templates;
import airhacks.specify.workspace.control.Workspace;
import airhacks.specify.workspace.entity.FeaturePaths;

/// `setup-plan` subcommand — port of `setup-plan.sh`. Ensures the feature directory
/// exists, seeds `plan.md` from the plan template, and reports the resolved paths.
public final class SetupPlan {

    public int run(String[] args) {
        var json = false;
        for (var arg : args) {
            switch (arg) {
                case "--json" -> json = true;
                case "--help", "-h" -> {
                    IO.println("Usage: setup-plan [--json]");
                    return 0;
                }
                default -> {
                    // create-new-feature parity: unknown args are ignored here
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

        try {
            Files.createDirectories(paths.featureDir());
            if (Files.exists(paths.implPlan())) {
                System.err.println("Plan already exists at " + paths.implPlan() + ", skipping template copy");
            } else {
                var template = Templates.resolve("plan-template", paths.repoRoot());
                if (template.isPresent()) {
                    Files.copy(template.get(), paths.implPlan());
                    System.err.println("Copied plan template to " + paths.implPlan());
                } else {
                    System.err.println("Warning: Plan template not found");
                    Files.createFile(paths.implPlan());
                }
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }

        if (json) {
            var payload = new JSONObject()
                    .put("FEATURE_SPEC", paths.featureSpec().toString())
                    .put("IMPL_PLAN", paths.implPlan().toString())
                    .put("SPECS_DIR", paths.featureDir().toString())
                    .put("BRANCH", paths.currentBranch());
            IO.println(payload.toString());
        } else {
            IO.println("FEATURE_SPEC: " + paths.featureSpec());
            IO.println("IMPL_PLAN: " + paths.implPlan());
            IO.println("SPECS_DIR: " + paths.featureDir());
            IO.println("BRANCH: " + paths.currentBranch());
        }
        return 0;
    }
}
