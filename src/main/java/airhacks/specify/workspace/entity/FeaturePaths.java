package airhacks.specify.workspace.entity;

import module java.base;

/// Resolved locations for the active feature, mirroring the variables emitted by
/// `common.sh`'s `get_feature_paths`. `currentBranch` may be empty when no
/// `SPECIFY_FEATURE` is set — feature resolution relies on `feature.json` instead.
public record FeaturePaths(
        Path repoRoot,
        String currentBranch,
        Path featureDir,
        Path featureSpec,
        Path implPlan,
        Path tasks,
        Path research,
        Path dataModel,
        Path quickstart,
        Path contractsDir) {

    public static FeaturePaths of(Path repoRoot, String currentBranch, Path featureDir) {
        return new FeaturePaths(
                repoRoot,
                currentBranch,
                featureDir,
                featureDir.resolve("spec.md"),
                featureDir.resolve("plan.md"),
                featureDir.resolve("tasks.md"),
                featureDir.resolve("research.md"),
                featureDir.resolve("data-model.md"),
                featureDir.resolve("quickstart.md"),
                featureDir.resolve("contracts"));
    }
}
