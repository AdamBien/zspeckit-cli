import airhacks.specify.workspace.control.Workspace;

void main() throws Exception {
    var repoRoot = Files.createTempDirectory("repo");
    var featureDir = repoRoot.resolve("specs").resolve("001-demo");

    // persist stores the feature directory relative to the repo root...
    Workspace.persistFeatureDirectory(repoRoot, featureDir);

    // ...and readFeatureDirectory reads exactly that relative path back
    var stored = Workspace.readFeatureDirectory(repoRoot);
    var expected = "specs" + File.separatorChar + "001-demo";
    if (stored.isEmpty() || !expected.equals(stored.get()))
        throw new AssertionError("roundtrip: expected '" + expected + "' but got " + stored);

    // a repo without feature.json reports no feature directory
    var empty = Workspace.readFeatureDirectory(Files.createTempDirectory("empty"));
    if (empty.isPresent())
        throw new AssertionError("expected empty Optional but got " + empty);
}
