/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Adds the plain Apache-2.0 licence header to every file that lacks one.
 *
 * Scans the files git knows about (tracked, plus untracked files that are not ignored), skips the
 * patterns of release/rat-excludes.txt and every file that already has an Apache licence header, and
 * puts the header in the comment style of the file type. `ant rat` is the check; this is the fix.
 * Pure Java, no dependencies; runs as a single-file source program (or `ant headers`):
 *
 *   java release/tools/Headers.java             # add the missing headers
 *   java release/tools/Headers.java --dry-run   # only list the files that would change
 *   java release/tools/Headers.java --root=DIR  # use DIR as the repository root
 *
 * The repository root defaults to the current working directory. Files are read and written as
 * ISO-8859-1, which round-trips every byte, so the encoding of a file is never changed.
 */
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Headers {

    private static final List<String> LICENSE = List.of(
            "Licensed under the Apache License, Version 2.0 (the \"License\");",
            "you may not use this file except in compliance with the License.",
            "You may obtain a copy of the License at",
            "",
            "    http://www.apache.org/licenses/LICENSE-2.0",
            "",
            "Unless required by applicable law or agreed to in writing, software",
            "distributed under the License is distributed on an \"AS IS\" BASIS,",
            "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.",
            "See the License for the specific language governing permissions and",
            "limitations under the License.");

    /** Either the plain header (this tool) or the ASF header (files derived from Apache NetBeans). */
    private static final Pattern HAS_HEADER = Pattern.compile(
            "Licensed under the Apache License|Licensed to the Apache Software Foundation");
    private static final Pattern XML_DECL = Pattern.compile("\\A<\\?xml[^>]*\\?>\\n");
    private static final Pattern FRONT_MATTER = Pattern.compile("\\A---\\n.*?\\n---\\n", Pattern.DOTALL);

    /** Files Rat's standard exclusions skip as well. */
    private static final Set<String> SKIPPED = Set.of(".gitignore", ".gitmodules", ".DS_Store");

    private static final Function<List<String>, String> C = lines -> block("/*", " * ", " */", lines);
    private static final Function<List<String>, String> XML = lines -> "<!--\n\n" + indent("    ", lines) + "\n-->\n";
    private static final Function<List<String>, String> HASH = lines -> indent("# ", lines) + "\n";

    /** Comment style by file extension (or by file name for dot-files). */
    private static final Map<String, Function<List<String>, String>> STYLES = Map.ofEntries(
            Map.entry("java", C), Map.entry("css", C), Map.entry("js", C),
            Map.entry("xml", XML), Map.entry("fxml", XML), Map.entry("xsl", XML),
            Map.entry("html", XML), Map.entry("md", XML),
            Map.entry("properties", HASH), Map.entry("fragment", HASH), Map.entry("gitattributes", HASH),
            Map.entry("yml", HASH), Map.entry("yaml", HASH));

    private static Path root;

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean dryRun = false;
        root = Paths.get(System.getProperty("user.dir"));
        for (String arg : args) {
            if ("--dry-run".equals(arg)) {
                dryRun = true;
            } else if (arg.startsWith("--root=")) {
                root = Paths.get(arg.substring("--root=".length()));
            } else {
                System.err.println("usage: java Headers.java [--dry-run] [--root=DIR]");
                System.exit(2);
            }
        }
        List<PathMatcher> excludes = excludes(root.resolve("release/rat-excludes.txt"));
        int added = 0;
        List<String> unknown = new ArrayList<>();
        for (String rel : gitFiles()) {
            Path file = root.resolve(rel);
            if (!Files.isRegularFile(file) || Files.isSymbolicLink(file) || excluded(excludes, rel)
                    || SKIPPED.contains(rel.substring(rel.lastIndexOf('/') + 1))) {
                continue;
            }
            String text = Files.readString(file, StandardCharsets.ISO_8859_1);
            // binaries (icons, jars, natives) carry no header; Rat skips them too
            if (text.substring(0, Math.min(text.length(), 8192)).indexOf('\0') >= 0) {
                continue;
            }
            if (HAS_HEADER.matcher(text.substring(0, Math.min(text.length(), 2048))).find()) {
                continue;
            }
            Function<List<String>, String> style = STYLES.get(extension(rel));
            if (style == null) {
                unknown.add(rel);
                continue;
            }
            if (!dryRun) {
                Files.writeString(file, withHeader(text, style, "md".equals(extension(rel))), StandardCharsets.ISO_8859_1);
            }
            System.out.println((dryRun ? "would add header: " : "added header: ") + rel);
            added++;
        }
        for (String rel : unknown) {
            System.out.println("no header style for " + rel + ": add one by hand or exclude it in release/rat-excludes.txt");
        }
        System.out.println(added + " file(s) " + (dryRun ? "without a header" : "updated"));
    }

    /** Puts the header first, after an XML declaration or Markdown front matter if there is one. */
    private static String withHeader(String text, Function<List<String>, String> style, boolean markdown) {
        String prefix = "";
        Matcher m = (markdown ? FRONT_MATTER : XML_DECL).matcher(text);
        if (style == XML && m.find()) {
            prefix = m.group();
            text = text.substring(m.end());
        }
        return prefix + style.apply(LICENSE) + text.replaceFirst("\\A\\n+", "");
    }

    private static String block(String open, String prefix, String close, List<String> lines) {
        return open + "\n" + indent(prefix, lines) + close + "\n";
    }

    private static String indent(String prefix, List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append((prefix + line).stripTrailing()).append('\n');
        }
        return sb.toString();
    }

    private static String extension(String rel) {
        String name = rel.substring(rel.lastIndexOf('/') + 1);
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /** Tracked files plus untracked, non-ignored ones; the netbeans submodule is a single gitlink entry. */
    private static Set<String> gitFiles() throws IOException, InterruptedException {
        Process git = new ProcessBuilder("git", "ls-files", "-z", "--cached", "--others", "--exclude-standard")
                .directory(root.toFile())
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        String out;
        try (InputStream in = git.getInputStream()) {
            out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (git.waitFor() != 0) {
            throw new IOException("git ls-files failed in " + root);
        }
        Set<String> files = new LinkedHashSet<>();
        for (String rel : out.split("\0")) {
            if (!rel.isEmpty()) {
                files.add(rel);
            }
        }
        return files;
    }

    /** The Ant-style patterns of rat-excludes.txt as glob matchers. */
    private static List<PathMatcher> excludes(Path file) throws IOException {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String pattern = line.strip();
            if (pattern.isEmpty() || pattern.startsWith("#")) {
                continue;
            }
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
            // Ant's **/ also matches zero directories; a glob needs at least one
            if (pattern.startsWith("**/")) {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern.substring(3)));
            }
        }
        return matchers;
    }

    private static boolean excluded(List<PathMatcher> excludes, String rel) {
        Path path = Paths.get(rel);
        return excludes.stream().anyMatch(m -> m.matches(path));
    }
}
