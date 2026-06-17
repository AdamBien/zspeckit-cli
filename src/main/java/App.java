import airhacks.specify.feature.boundary.NewFeature;
import airhacks.specify.prerequisites.boundary.CheckPrerequisites;
import airhacks.specify.prerequisites.boundary.SetupPlan;
import airhacks.specify.prerequisites.boundary.SetupTasks;

/// Single executable entry point replacing the five `.specify/scripts/bash` scripts.
/// Dispatches on the first argument to the matching subcommand; remaining arguments
/// keep the exact flag/`--json` contract the original scripts exposed.
void main(String[] args) {
    var version = version();
    if (args.length == 0) {
        printUsage(version);
        System.exit(1);
    }

    var rest = Arrays.copyOfRange(args, 1, args.length);
    var exitCode = switch (args[0]) {
        case "new-feature", "create-new-feature" -> new NewFeature().run(rest);
        case "check-prerequisites" -> new CheckPrerequisites().run(rest);
        case "setup-plan" -> new SetupPlan().run(rest);
        case "setup-tasks" -> new SetupTasks().run(rest);
        case "--version", "-v" -> { IO.println(version); yield 0; }
        case "--help", "-h" -> { printUsage(version); yield 0; }
        default -> {
            System.err.println("ERROR: Unknown subcommand '" + args[0] + "'");
            printUsage(version);
            yield 1;
        }
    };
    System.exit(exitCode);
}

/// Resolve the version from the bundled `version.txt` classpath resource (the path
/// in the packaged JAR), falling back to the project-root file when running from
/// source. `version.txt` is maintained by `zbump` and is the single source of truth.
String version() {
    try (var resource = NewFeature.class.getResourceAsStream("/version.txt")) {
        if (resource != null) {
            return new String(resource.readAllBytes()).strip();
        }
    } catch (IOException _) {
        // fall through to the filesystem lookup
    }
    try {
        return Files.readString(Path.of("version.txt")).strip();
    } catch (IOException _) {
        return "unknown";
    }
}

void printUsage(String version) {
    IO.println("""
            specify-cli %s
            Usage: specify <subcommand> [options]

            Subcommands:
              new-feature           Create a feature directory and spec.md
              check-prerequisites   Validate the feature scaffold / report paths
              setup-plan            Seed plan.md from the plan template
              setup-tasks           Resolve the tasks template and list docs

            Run 'specify <subcommand> --help' for subcommand options.""".formatted(version));
}
