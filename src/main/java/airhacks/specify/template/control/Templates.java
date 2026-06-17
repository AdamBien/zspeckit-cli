package airhacks.specify.template.control;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.json.JSONObject;

/// Path-only template resolution, porting `common.sh`'s `resolve_template`. Search
/// order: project overrides -> installed presets (by `.registry` priority) ->
/// extensions -> core templates.
///
/// The YAML-based composition stack (`resolve_template_content`) is intentionally
/// not ported: no command here uses it, and `org.json` does not parse YAML. Add a
/// minimal YAML reader if preset composition is ever required.
public final class Templates {

    private Templates() {
    }

    public static Optional<Path> resolve(String templateName, Path repoRoot) {
        var base = repoRoot.resolve(".specify/templates");

        var override = base.resolve("overrides/" + templateName + ".md");
        if (Files.isRegularFile(override)) {
            return Optional.of(override);
        }

        var fromPreset = resolveFromPresets(templateName, repoRoot.resolve(".specify/presets"));
        if (fromPreset.isPresent()) {
            return fromPreset;
        }

        var fromExtension = resolveFromExtensions(templateName, repoRoot.resolve(".specify/extensions"));
        if (fromExtension.isPresent()) {
            return fromExtension;
        }

        var core = base.resolve(templateName + ".md");
        return Files.isRegularFile(core) ? Optional.of(core) : Optional.empty();
    }

    private static Optional<Path> resolveFromPresets(String templateName, Path presetsDir) {
        if (!Files.isDirectory(presetsDir)) {
            return Optional.empty();
        }
        for (var presetId : presetIdsByPriority(presetsDir)) {
            var candidate = presetsDir.resolve(presetId).resolve("templates/" + templateName + ".md");
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /// Preset ids ordered by `.registry` priority (lower number wins), skipping
    /// disabled entries. Falls back to alphabetical directory order when the
    /// registry is missing or unparseable.
    private static List<String> presetIdsByPriority(Path presetsDir) {
        var registry = presetsDir.resolve(".registry");
        if (Files.isRegularFile(registry)) {
            try {
                var presets = new JSONObject(Files.readString(registry)).optJSONObject("presets");
                if (presets != null) {
                    return presets.keySet().stream()
                            .map(id -> new JSONObject().put("id", id).put("meta", presets.optJSONObject(id)))
                            .filter(entry -> entry.optJSONObject("meta") == null
                                    || entry.optJSONObject("meta").optBoolean("enabled", true))
                            .sorted(Comparator.comparingInt(entry -> entry.optJSONObject("meta") == null
                                    ? 10
                                    : entry.optJSONObject("meta").optInt("priority", 10)))
                            .map(entry -> entry.getString("id"))
                            .toList();
                }
            } catch (IOException | RuntimeException ignored) {
                // fall through to alphabetical scan
            }
        }
        return childDirectoryNames(presetsDir);
    }

    private static Optional<Path> resolveFromExtensions(String templateName, Path extensionsDir) {
        if (!Files.isDirectory(extensionsDir)) {
            return Optional.empty();
        }
        for (var extId : childDirectoryNames(extensionsDir)) {
            if (extId.startsWith(".")) {
                continue;
            }
            var candidate = extensionsDir.resolve(extId).resolve("templates/" + templateName + ".md");
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static List<String> childDirectoryNames(Path dir) {
        try (Stream<Path> children = Files.list(dir)) {
            return children.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } catch (IOException listFailure) {
            throw new UncheckedIOException(listFailure);
        }
    }
}
