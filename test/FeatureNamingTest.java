import airhacks.specify.feature.control.FeatureNaming;

void main() throws Exception {
    // clean: lowercase, collapse non-alphanumeric runs to a single hyphen, trim
    var cleaned = FeatureNaming.clean("  Add User-Auth!! ");
    if (!"add-user-auth".equals(cleaned))
        throw new AssertionError("clean: expected 'add-user-auth' but got '" + cleaned + "'");

    // fromDescription: drop stop words, keep up to three meaningful words
    var slug = FeatureNaming.fromDescription("I want to add user authentication flow");
    if (!"user-authentication-flow".equals(slug))
        throw new AssertionError("fromDescription: expected 'user-authentication-flow' but got '" + slug + "'");

    // highestFeatureNumber: max sequential prefix, ignoring timestamp directories
    var specs = Files.createTempDirectory("specs");
    Files.createDirectory(specs.resolve("001-alpha"));
    Files.createDirectory(specs.resolve("003-beta"));
    Files.createDirectory(specs.resolve("20240101-120000-gamma"));
    var highest = FeatureNaming.highestFeatureNumber(specs);
    if (highest != 3)
        throw new AssertionError("highestFeatureNumber: expected 3 but got " + highest);
}
