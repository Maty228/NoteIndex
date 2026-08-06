package cz.martim12.noteindex.importer.builtin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Produces search-oriented text from Markdown source.

 * This is intentionally not a Markdown renderer. It preserves
 * useful visible text while removing syntax, destinations and
 * presentation-only markers.
 */
final class MarkdownContentProcessor {

    private static final Pattern HTML_COMMENT =
            Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern FENCE =
            Pattern.compile("^\\s{0,3}(`{3,}|~{3,}).*$");
    private static final Pattern ATX_LEVEL_ONE_HEADING =
            Pattern.compile("^\\s{0,3}#\\s+(.+?)(?:\\s+#+)?\\s*$");
    private static final Pattern SETEXT_LEVEL_ONE_HEADING =
            Pattern.compile("^\\s{0,3}=+\\s*$");
    private static final Pattern SETEXT_UNDERLINE =
            Pattern.compile("^\\s{0,3}(?:=+|-+)\\s*$");
    private static final Pattern REFERENCE_DEFINITION =
            Pattern.compile("^\\s{0,3}\\[[^]]+]:\\s+\\S+.*$");
    private static final Pattern TABLE_SEPARATOR =
            Pattern.compile("^\\s*\\|?\\s*:?-{3,}:?\\s*(?:\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$");
    private static final Pattern THEMATIC_BREAK =
            Pattern.compile("^\\s{0,3}(?:(?:\\*\\s*){3,}|(?:-\\s*){3,}|(?:_\\s*){3,})$");
    private static final Pattern HTML_IMAGE_DOUBLE_QUOTED_ALT =
            Pattern.compile("(?i)<img\\b[^>]*\\balt\\s*=\\s*\"([^\"]*)\"[^>]*>");
    private static final Pattern HTML_IMAGE_SINGLE_QUOTED_ALT =
            Pattern.compile("(?i)<img\\b[^>]*\\balt\\s*=\\s*'([^']*)'[^>]*>");
    private static final Pattern OBSIDIAN_IMAGE =
            Pattern.compile(
                    "!\\[\\[([^|\\]]+)(?:\\|([^\\]]+))?\\]\\]"
            );

    private static final Pattern OBSIDIAN_LINK =
            Pattern.compile(
                    "\\[\\[([^|\\]]+)(?:\\|([^\\]]+))?\\]\\]"
            );
    private static final Pattern MARKDOWN_IMAGE =
            Pattern.compile("!\\[([^]]*)]\\([^\\n)]*\\)");
    private static final Pattern REFERENCE_IMAGE =
            Pattern.compile("!\\[([^]]*)]\\[[^]]*]");
    private static final Pattern MARKDOWN_LINK =
            Pattern.compile("\\[([^]]+)]\\([^\\n)]*\\)");
    private static final Pattern REFERENCE_LINK =
            Pattern.compile("\\[([^]]+)]\\[[^]]*]");
    private static final Pattern AUTOLINK =
            Pattern.compile("(?i)<(?:https?://|mailto:)[^>]+>");
    private static final Pattern HTML_TAG =
            Pattern.compile("</?[A-Za-z][^>]*>");
    private static final Pattern INLINE_CODE =
            Pattern.compile("(`+)(.+?)\\1");
    private static final Pattern STRONG_ASTERISK =
            Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern STRONG_UNDERSCORE =
            Pattern.compile("__(.+?)__");
    private static final Pattern EMPHASIS_ASTERISK =
            Pattern.compile("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)");
    private static final Pattern EMPHASIS_UNDERSCORE =
            Pattern.compile("(?<!_)_([^_\\n]+)_(?!_)");
    private static final Pattern STRIKETHROUGH =
            Pattern.compile("~~(.+?)~~");

    private MarkdownContentProcessor() {}

    static ProcessedMarkdown process(String markdown) {
        Objects.requireNonNull(markdown, "Markdown must not be null");

        String withoutComments = HTML_COMMENT.matcher(markdown).replaceAll(" ");

        List<String> lines = withoutComments.lines().toList();

        Optional<String> title = findTitle(lines);

        StringBuilder searchableContent = new StringBuilder();

        FenceTracker fenceTracker = new FenceTracker();

        for (String line : lines) {
            if (fenceTracker.consumeFence(line)){
                continue;
            }

            if (fenceTracker.isInsideFence()){
                appendLine(searchableContent, line);
                continue;
            }

            if (isDiscardedBlockLine(line)) {
                appendLine(searchableContent, "");
                continue;
            }

            String cleaned = cleanStructure(line);
            cleaned = cleanInlineMarkup(cleaned);

            /*
             * Any pipes still present are table separators. Obsidian links
             * and image captions have already been processed at this point.
             */
            cleaned = cleaned.replace('|', ' ');

            appendLine(searchableContent, cleaned);

        }

        return new ProcessedMarkdown(title, normalizeBlankLines(searchableContent.toString()));
    }

    private static Optional<String> findTitle(List<String> lines) {
        FenceTracker fenceTracker = new FenceTracker();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);

            if (fenceTracker.consumeFence(line)) {
                continue;
            }

            if (fenceTracker.isInsideFence()) {
                continue;
            }

            Matcher atxHeading = ATX_LEVEL_ONE_HEADING.matcher(line);

            if (atxHeading.matches()) {
                String title = cleanInlineMarkup(atxHeading.group(1));

                if (!title.isBlank()) {
                    return Optional.of(title);
                }
            }

            if (!line.isBlank() && index + 1 < lines.size() && SETEXT_LEVEL_ONE_HEADING.matcher(lines.get(index + 1)).matches()) {
                String title = cleanInlineMarkup(line);

                if (!title.isBlank()) {
                    return Optional.of(title);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isDiscardedBlockLine(String line) {
        return SETEXT_UNDERLINE.matcher(line).matches() || REFERENCE_DEFINITION.matcher(line).matches() || TABLE_SEPARATOR.matcher(line).matches() || THEMATIC_BREAK.matcher(line).matches();
    }

    private static String cleanStructure(String line) {
        String cleaned = line;

        cleaned = cleaned.replaceFirst("^\\s{0,3}#{1,6}\\s+", "");

        cleaned = cleaned.replaceFirst("\\s+#+\\s*$", "");

        cleaned = cleaned.replaceFirst("^(?:\\s{0,3}>\\s?)+", "");

        cleaned = cleaned.replaceFirst("^\\s*(?:[-+*]|\\d+[.)])\\s+", "");

        cleaned = cleaned.replaceFirst("^\\s*\\[[ xX]]\\s+", "");

        return cleaned;
    }

    private static String cleanInlineMarkup(String text) {
        String cleaned = text;

        cleaned = HTML_IMAGE_DOUBLE_QUOTED_ALT.matcher(cleaned).replaceAll("$1");

        cleaned = HTML_IMAGE_SINGLE_QUOTED_ALT.matcher(cleaned).replaceAll("$1");

        cleaned = replaceObsidianImages(cleaned);

        cleaned = MARKDOWN_IMAGE.matcher(cleaned).replaceAll("$1");

        cleaned = REFERENCE_IMAGE.matcher(cleaned).replaceAll("$1");

        cleaned = replaceObsidianLinks(cleaned);

        cleaned = MARKDOWN_LINK.matcher(cleaned).replaceAll("$1");

        cleaned = REFERENCE_LINK.matcher(cleaned).replaceAll("$1");

        cleaned = INLINE_CODE.matcher(cleaned).replaceAll("$2");

        /*
         * Two passes allow simple nested emphasis such as:
         * **an _important_ term**
         */
        for (int pass = 0; pass < 2; pass++) {
            cleaned = STRONG_ASTERISK.matcher(cleaned).replaceAll("$1");

            cleaned = STRONG_UNDERSCORE.matcher(cleaned).replaceAll("$1");

            cleaned = EMPHASIS_ASTERISK.matcher(cleaned).replaceAll("$1");

            cleaned = EMPHASIS_UNDERSCORE.matcher(cleaned).replaceAll("$1");

            cleaned = STRIKETHROUGH.matcher(cleaned).replaceAll("$1");
        }

        cleaned = AUTOLINK.matcher(cleaned).replaceAll(" ");

        cleaned = HTML_TAG.matcher(cleaned).replaceAll(" ");

        /*
         * Preserve the expression itself while removing common
         * Markdown/KaTeX delimiters.
         */
        cleaned = cleaned
                .replace("$$", " ")
                .replace("\\[", " ")
                .replace("\\]", " ")
                .replace("\\(", " ")
                .replace("\\)", " ")
                .replace('$', ' ');

        cleaned = decodeCommonHtmlEntities(cleaned);

        return cleaned.replaceAll("[\\t ]+", " ").strip();
    }

    private static String replaceObsidianImages(String text) {
        Matcher matcher = OBSIDIAN_IMAGE.matcher(text);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String caption = matcher.group(2);

            /*
             * Keep an explicit caption. A raw image filename is not
             * useful searchable prose.
             */
            String replacement = caption == null ? "" : caption;

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceObsidianLinks(String text) {
        Matcher matcher = OBSIDIAN_LINK.matcher(text);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String page = matcher.group(1);
            String label = matcher.group(2);

            String replacement = label == null ? page : label;

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String decodeCommonHtmlEntities(String text) {
        return text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static void appendLine(StringBuilder output, String line) {
        output.append(line.strip()).append("\n");
    }

    private static String normalizeBlankLines(String text) {
        return text.replaceAll("(?m)[\\t ]+$", "").replaceAll("\\n{3,}", "\n\n").strip();
    }

    record ProcessedMarkdown(Optional<String> title, String searchableContent) {
        ProcessedMarkdown {
            Objects.requireNonNull(title, "Processed title must not be null");
            searchableContent = Objects.requireNonNull(searchableContent, "Searchable content must not be null");
        }
    }

    private static final class FenceTracker {
        private boolean insideFence;
        private char markerCharacter;
        private int markerLength;

        boolean consumeFence(String line) {
            Matcher matcher = FENCE.matcher(line);

            if (!matcher.matches()) {
                return false;
            }

            String marker = matcher.group(1);

            if (!insideFence) {
                insideFence = true;
                markerCharacter = marker.charAt(0);
                markerLength = marker.length();
                return true;
            }

            if (marker.charAt(0) == markerCharacter
                    && marker.length() >= markerLength) {
                insideFence = false;
                return true;
            }

            /*
             * A different fence marker inside a code block is ordinary
             * code content and must not be discarded.
             */
            return false;
        }

        boolean isInsideFence() {
            return insideFence;
        }
    }
}
