package airhacks.specify.prerequisites.control;

import module java.base;

import airhacks.specify.workspace.entity.FeaturePaths;

/// Optional design documents present in the feature directory, in the order
/// `check-prerequisites.sh` reports them. Shared by the `check-prerequisites` and
/// `setup-tasks` boundaries.
public interface DesignDocuments {

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
