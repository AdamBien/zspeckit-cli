package airhacks.specify.workspace.control;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.json.JSONObject;

import airhacks.specify.workspace.entity.FeaturePaths;

/// Repository and feature-state resolution — the Java port of the shared logic in
/// `common.sh`. All JSON I/O goes through the vendored `org.json` (zjson), replacing
/// the bash `jq` -> `python3` -> `grep/sed` cascade with a single robust path.
public final class Workspace {

    private Workspace() {
    }

    /// Walk upward from the current working directory looking for the `.specify`
    /// marker directory (mirrors `find_specify_root`/`get_repo_root`). Falls back to
    /// the current working directory when no marker is found.
    public static Path repoRoot() {
        var dir = Path.of("").toAbsolutePath();
        for (var current = dir; current != null; current = current.getParent()) {
            if (Files.isDirectory(current.resolve(".specify"))) {
                return current;
            }
        }
        return dir;
    }

    /// The active feature name from the `SPECIFY_FEATURE` environment variable, or
    /// empty when unset (resolution then relies on `feature.json`).
    public static String currentBranch() {
        return Optional.ofNullable(System.getenv("SPECIFY_FEATURE")).orElse("");
    }

    /// Read `.specify/feature.json`'s `feature_directory` value, or empty when the
    /// file is missing, unparseable, or lacks the key.
    public static Optional<String> readFeatureDirectory(Path repoRoot) {
        var featureJson = repoRoot.resolve(".specify/feature.json");
        if (!Files.isRegularFile(featureJson)) {
            return Optional.empty();
        }
        try {
            var json = new JSONObject(Files.readString(featureJson));
            var value = json.optString("feature_directory", "");
            return value.isBlank() ? Optional.empty() : Optional.of(value);
        } catch (IOException | RuntimeException parseFailure) {
            return Optional.empty();
        }
    }

    /// Persist `feature_directory` to `.specify/feature.json`, storing the path
    /// relative to the repo root. Skips the write when the stored value is unchanged.
    public static void persistFeatureDirectory(Path repoRoot, Path featureDir) {
        var relative = featureDir.isAbsolute() && featureDir.startsWith(repoRoot)
                ? repoRoot.relativize(featureDir).toString()
                : featureDir.toString();
        if (readFeatureDirectory(repoRoot).filter(relative::equals).isPresent()) {
            return;
        }
        try {
            var specifyDir = repoRoot.resolve(".specify");
            Files.createDirectories(specifyDir);
            var json = new JSONObject().put("feature_directory", relative);
            Files.writeString(specifyDir.resolve("feature.json"), json.toString());
        } catch (IOException writeFailure) {
            throw new UncheckedIOException(writeFailure);
        }
    }

    /// Resolve all feature paths, honoring `SPECIFY_FEATURE_DIRECTORY` first, then
    /// `feature.json` (mirrors `get_feature_paths`). Throws when no feature context
    /// can be determined.
    public static FeaturePaths featurePaths() {
        var repoRoot = repoRoot();
        var override = System.getenv("SPECIFY_FEATURE_DIRECTORY");
        if (override != null && !override.isBlank()) {
            var featureDir = absolutize(repoRoot, override);
            persistFeatureDirectory(repoRoot, featureDir);
            return FeaturePaths.of(repoRoot, currentBranch(), featureDir);
        }
        var stored = readFeatureDirectory(repoRoot)
                .orElseThrow(() -> new IllegalStateException(
                        "Feature directory not found. Set SPECIFY_FEATURE_DIRECTORY or run the specify command to create .specify/feature.json."));
        return FeaturePaths.of(repoRoot, currentBranch(), absolutize(repoRoot, stored));
    }

    private static Path absolutize(Path repoRoot, String value) {
        var path = Path.of(value);
        return path.isAbsolute() ? path : repoRoot.resolve(path);
    }
}
