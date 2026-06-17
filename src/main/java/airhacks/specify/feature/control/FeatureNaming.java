package airhacks.specify.feature.control;

import java.io.IOException;
import java.io.UncheckedIOException;
import module java.base;

/// Branch/directory name derivation and sequential numbering — the Java port of the
/// shell helpers in `create-new-feature.sh` (`clean_branch_name`,
/// `generate_branch_name`, `get_highest_from_specs`).
public interface FeatureNaming {

    Set<String> STOP_WORDS = Set.of(
            "i", "a", "an", "the", "to", "for", "of", "in", "on", "at", "by", "with",
            "from", "is", "are", "was", "were", "be", "been", "being", "have", "has",
            "had", "do", "does", "did", "will", "would", "should", "could", "can",
            "may", "might", "must", "shall", "this", "that", "these", "those", "my",
            "your", "our", "their", "want", "need", "add", "get", "set");

    Pattern SEQUENTIAL = Pattern.compile("^[0-9]{3,}-.*");
    Pattern TIMESTAMP = Pattern.compile("^[0-9]{8}-[0-9]{6}-.*");
    Pattern LEADING_NUMBER = Pattern.compile("^([0-9]+).*");

    /// Lowercase, replace every non-alphanumeric run with a single hyphen, trim
    /// leading/trailing hyphens (mirrors `clean_branch_name`).
    static String clean(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /// Derive a 3-4 word slug from a free-text description, dropping stop words and
    /// short tokens (mirrors `generate_branch_name`, including its fallback).
    static String fromDescription(String description) {
        var meaningful = new ArrayList<String>();
        for (var word : description.toLowerCase().replaceAll("[^a-z0-9]", " ").split("\\s+")) {
            if (word.isEmpty() || STOP_WORDS.contains(word)) {
                continue;
            }
            var isAcronym = Pattern.compile("\\b" + Pattern.quote(word.toUpperCase()) + "\\b")
                    .matcher(description).find();
            if (word.length() >= 3 || isAcronym) {
                meaningful.add(word);
            }
        }
        if (meaningful.isEmpty()) {
            return Stream.of(clean(description).split("-"))
                    .filter(part -> !part.isEmpty())
                    .limit(3)
                    .reduce((left, right) -> left + "-" + right)
                    .orElse("");
        }
        var maxWords = meaningful.size() == 4 ? 4 : 3;
        return meaningful.stream().limit(maxWords).reduce((left, right) -> left + "-" + right).orElse("");
    }

    /// Highest sequential prefix among `specs/` subdirectories, ignoring timestamp
    /// directories (mirrors `get_highest_from_specs`).
    static int highestFeatureNumber(Path specsDir) {
        if (!Files.isDirectory(specsDir)) {
            return 0;
        }
        try (Stream<Path> children = Files.list(specsDir)) {
            return children.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> SEQUENTIAL.matcher(name).matches() && !TIMESTAMP.matcher(name).matches())
                    .mapToInt(FeatureNaming::leadingNumber)
                    .max()
                    .orElse(0);
        } catch (IOException listFailure) {
            throw new UncheckedIOException(listFailure);
        }
    }

    static int leadingNumber(String dirName) {
        var matcher = LEADING_NUMBER.matcher(dirName);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}
