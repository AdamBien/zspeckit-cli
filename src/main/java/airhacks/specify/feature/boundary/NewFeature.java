package airhacks.specify.feature.boundary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.json.JSONObject;

import airhacks.specify.feature.control.FeatureNaming;
import airhacks.specify.template.control.Templates;
import airhacks.specify.workspace.control.Workspace;

/// `new-feature` subcommand — port of `create-new-feature.sh`. Creates
/// `specs/NNN-slug/spec.md` (or a timestamped variant), persists the active feature
/// to `feature.json`, and emits `BRANCH_NAME`/`SPEC_FILE`/`FEATURE_NUM`.
public final class NewFeature {

    private static final int MAX_BRANCH_LENGTH = 244;
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public int run(String[] args) {
        var json = false;
        var dryRun = false;
        var allowExisting = false;
        var useTimestamp = false;
        String shortName = null;
        String number = null;
        var description = new ArrayList<String>();

        for (var i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--json" -> json = true;
                case "--dry-run" -> dryRun = true;
                case "--allow-existing-branch" -> allowExisting = true;
                case "--timestamp" -> useTimestamp = true;
                case "--short-name" -> {
                    if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                        System.err.println("Error: --short-name requires a value");
                        return 1;
                    }
                    shortName = args[++i];
                }
                case "--number" -> {
                    if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                        System.err.println("Error: --number requires a value");
                        return 1;
                    }
                    number = args[++i];
                }
                case "--help", "-h" -> {
                    printHelp();
                    return 0;
                }
                default -> description.add(args[i]);
            }
        }

        var featureDescription = String.join(" ", description).strip();
        if (featureDescription.isEmpty()) {
            System.err.println("Error: Feature description cannot be empty or contain only whitespace");
            return 1;
        }

        var suffix = shortName != null
                ? FeatureNaming.clean(shortName)
                : FeatureNaming.fromDescription(featureDescription);

        var repoRoot = Workspace.repoRoot();
        var specsDir = repoRoot.resolve("specs");

        if (useTimestamp && number != null) {
            System.err.println("[specify] Warning: --number is ignored when --timestamp is used");
            number = null;
        }

        String featureNum;
        if (useTimestamp) {
            featureNum = LocalDateTime.now().format(TIMESTAMP);
        } else {
            var resolved = number != null ? Integer.parseInt(number)
                    : FeatureNaming.highestFeatureNumber(specsDir) + 1;
            featureNum = "%03d".formatted(resolved);
        }

        var branchName = truncate(featureNum + "-" + suffix, featureNum);
        var featureDir = specsDir.resolve(branchName);
        var specFile = featureDir.resolve("spec.md");

        if (!dryRun) {
            if (Files.isDirectory(featureDir) && !allowExisting) {
                System.err.println("Error: Feature directory '" + featureDir
                        + "' already exists. Use a different name, --number, or --timestamp.");
                return 1;
            }
            createFeature(repoRoot, featureDir, specFile);
        }

        emit(json, dryRun, branchName, specFile, featureNum);
        return 0;
    }

    private static void createFeature(Path repoRoot, Path featureDir, Path specFile) {
        try {
            Files.createDirectories(featureDir);
            if (!Files.exists(specFile)) {
                var template = Templates.resolve("spec-template", repoRoot);
                if (template.isPresent()) {
                    Files.copy(template.get(), specFile);
                } else {
                    System.err.println("Warning: Spec template not found; created empty spec file");
                    Files.createFile(specFile);
                }
            }
            Workspace.persistFeatureDirectory(repoRoot, featureDir);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static String truncate(String branchName, String featureNum) {
        if (branchName.length() <= MAX_BRANCH_LENGTH) {
            return branchName;
        }
        var maxSuffix = MAX_BRANCH_LENGTH - (featureNum.length() + 1);
        var suffix = branchName.substring(featureNum.length() + 1);
        var truncated = suffix.substring(0, Math.min(maxSuffix, suffix.length())).replaceAll("-$", "");
        var result = featureNum + "-" + truncated;
        System.err.println("[specify] Warning: Branch name exceeded GitHub's 244-byte limit; truncated to " + result);
        return result;
    }

    private static void emit(boolean json, boolean dryRun, String branchName, Path specFile, String featureNum) {
        if (json) {
            var payload = new JSONObject()
                    .put("BRANCH_NAME", branchName)
                    .put("SPEC_FILE", specFile.toString())
                    .put("FEATURE_NUM", featureNum);
            if (dryRun) {
                payload.put("DRY_RUN", true);
            }
            IO.println(payload.toString());
        } else {
            IO.println("BRANCH_NAME: " + branchName);
            IO.println("SPEC_FILE: " + specFile);
            IO.println("FEATURE_NUM: " + featureNum);
        }
    }

    private static void printHelp() {
        IO.println("""
                Usage: new-feature [--json] [--dry-run] [--allow-existing-branch] \
                [--short-name <name>] [--number N] [--timestamp] <feature_description>""");
    }
}
