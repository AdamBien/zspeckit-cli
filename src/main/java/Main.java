import airhacks.specify.feature.boundary.NewFeature;
import airhacks.specify.prerequisites.boundary.CheckPrerequisites;
import airhacks.specify.prerequisites.boundary.SetupPlan;
import airhacks.specify.prerequisites.boundary.SetupTasks;

/// Single executable entry point replacing the five `.specify/scripts/bash` scripts.
/// Dispatches on the first argument to the matching subcommand; remaining arguments
/// keep the exact flag/`--json` contract the original scripts exposed.
void main(String[] args) {
    var version = "2026-06-17.2";
    if (args.length == 0) {
        printUsage(version);
        System.exit(1);
    }

    var rest = java.util.Arrays.copyOfRange(args, 1, args.length);
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
