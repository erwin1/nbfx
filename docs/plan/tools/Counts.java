/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Regenerates docs/plan/figures.md from the source tree.
 *
 * The plan must state the tree, so its numbers must be generated, not hand-typed.
 * Pure Java, no dependencies; runs as a single-file source program:
 *
 *   java docs/plan/tools/Counts.java             # write figures.md
 *   java docs/plan/tools/Counts.java --check     # fail if figures.md is stale
 *   java docs/plan/tools/Counts.java --root=DIR  # use DIR as the repository root
 *
 * The repository root defaults to the current working directory.
 */
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Counts {

    private static final Pattern FX_DECL = Pattern.compile(
            "public (?:final )?(?:class|interface|@interface|enum|record) (Fx\\w+)");
    private static final Pattern CONTROL = Pattern.compile(
            "new (TreeTableView|TreeView|TableView|ListView)<");
    private static final Pattern VIEW = Pattern.compile(
            "TreeView|TreeTableView|TableView|ListView");
    private static final String JAVA_SOURCE = "org.netbeans.api.java.source";

    private static Path root;
    /** Tracked files in the repository and in the NetBeans submodule, or null when git is unavailable. */
    private static Set<Path> tracked;
    private static Set<Path> trackedNetbeans;

    /** Swing-typed upstream API/SPI modules (the Appendix H offender list). */
    private static final List<String> offenders = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        boolean check = false;
        root = Paths.get(System.getProperty("user.dir"));
        for (String arg : args) {
            if ("--check".equals(arg)) {
                check = true;
            } else if (arg.startsWith("--root=")) {
                root = Paths.get(arg.substring("--root=".length()));
            } else {
                System.err.println("usage: java Counts.java [--check] [--root=DIR]");
                System.exit(2);
            }
        }
        tracked = gitTracked(root);
        trackedNetbeans = Files.isDirectory(root.resolve("netbeans")) ? gitTracked(root.resolve("netbeans")) : null;
        Path out = root.resolve("docs/plan/figures.md");
        String text = render();
        if (check) {
            if (!read(out).equals(text)) {
                System.err.println("figures.md is stale; run: java docs/plan/tools/Counts.java");
                System.exit(1);
            }
            System.out.println("figures.md is up to date");
        } else {
            Files.createDirectories(out.getParent());
            Files.writeString(out, text, StandardCharsets.UTF_8);
            System.out.println("wrote " + root.relativize(out));
        }
    }

    private static String render() throws IOException {
        List<Path> mods = modules();
        List<Path> exporting = new ArrayList<>();
        List<Path> subpackages = new ArrayList<>();
        List<Path> arch = new ArrayList<>();
        List<Path> apichanges = new ArrayList<>();
        for (Path mod : mods) {
            String projectXml = read(mod.resolve("nbproject/project.xml"));
            if (projectXml.contains("<public-packages>")) {
                exporting.add(mod);
                if (projectXml.contains("<subpackages>")) {
                    subpackages.add(mod);
                }
            }
            if (Files.exists(mod.resolve("arch.xml"))) {
                arch.add(mod);
            }
            if (Files.exists(mod.resolve("apichanges.xml"))) {
                apichanges.add(mod);
            }
        }

        List<Path> src = new ArrayList<>();
        Set<String> specs = new TreeSet<>();
        Set<String> fxAll = new TreeSet<>();
        Set<String> fxApi = new TreeSet<>();
        int subscribe = 0;
        int disposeFiles = 0;
        int javaSourceFiles = 0;
        int semanticFiles = 0;
        int nbBundleFiles = 0;
        int controlFiles = 0;
        int viewFiles = 0;
        Set<String> javaSourceImporters = new TreeSet<>();

        for (Path mod : mods) {
            boolean isExporting = exporting.contains(mod);
            List<Path> modSrc = sources(mod);
            src.addAll(modSrc);
            for (String line : read(mod.resolve("manifest.mf")).split("\n")) {
                if (line.contains("OpenIDE-Module-Specification-Version")) {
                    specs.add(line.substring(line.indexOf(':') + 1).trim());
                }
            }
            boolean importer = false;
            for (Path file : modSrc) {
                String text = read(file);
                Matcher fx = FX_DECL.matcher(text);
                while (fx.find()) {
                    fxAll.add(fx.group(1));
                    if (isExporting) {
                        fxApi.add(fx.group(1));
                    }
                }
                if (CONTROL.matcher(text).find()) {
                    controlFiles++;
                }
                if (VIEW.matcher(text).find()) {
                    viewFiles++;
                }
                subscribe += occurrences(text, ".subscribe(");
                if (text.contains("dispose()")) {
                    disposeFiles++;
                }
                if (text.contains(JAVA_SOURCE)) {
                    javaSourceFiles++;
                    importer = true;
                }
                if (text.contains(JAVA_SOURCE) || text.contains("org.netbeans.api.java.")
                        || text.contains("com.sun.source") || text.contains("libs.javacapi")) {
                    semanticFiles++;
                }
                if (text.contains("NbBundle")) {
                    nbBundleFiles++;
                }
            }
            if (importer) {
                javaSourceImporters.add(mod.getFileName().toString());
            }
        }

        List<Path> testFiles = matching(root, "nbfx-*", "*", "test", "**/*.java");
        Set<String> testedModules = new TreeSet<>();
        for (Path test : testFiles) {
            Path dir = test;
            while (dir != null && !dir.getFileName().toString().equals("test")) {
                dir = dir.getParent();
            }
            if (dir != null && dir.getParent() != null) {
                testedModules.add(dir.getParent().getFileName().toString());
            }
        }
        List<Path> bundles = matching(root, "nbfx-*", "*", "src", "**/Bundle.properties");

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"nbfx modules", String.valueOf(mods.size())});
        rows.add(new String[]{"nbfx source files", String.valueOf(src.size())});
        rows.add(new String[]{"modules exporting packages (`<public-packages>`)", String.valueOf(exporting.size())});
        rows.add(new String[]{"of those, subtree exports (`<subpackages>`)", String.valueOf(subpackages.size())});
        rows.add(new String[]{"modules with `arch.xml`", String.valueOf(arch.size())});
        rows.add(new String[]{"modules with `apichanges.xml`", String.valueOf(apichanges.size())});
        rows.add(new String[]{"distinct `OpenIDE-Module-Specification-Version` values",
                specs.size() + ": " + String.join(", ", specs)});
        rows.add(new String[]{"public `Fx*` types (total)", String.valueOf(fxAll.size())});
        rows.add(new String[]{"public `Fx*` types in exporting modules", String.valueOf(fxApi.size())});
        rows.add(new String[]{"files constructing TreeView/TreeTableView/TableView/ListView", String.valueOf(controlFiles)});
        rows.add(new String[]{"files mentioning a TreeView/TreeTableView/TableView/ListView", String.valueOf(viewFiles)});
        rows.add(new String[]{"`.subscribe(` call sites", String.valueOf(subscribe)});
        rows.add(new String[]{"files mentioning `dispose()`", String.valueOf(disposeFiles)});
        rows.add(new String[]{"modules importing `org.netbeans.api.java.source`",
                javaSourceImporters.size() + ": " + String.join(", ", javaSourceImporters)});
        rows.add(new String[]{"files importing `org.netbeans.api.java.source`", String.valueOf(javaSourceFiles)});
        rows.add(new String[]{"files touching the Java semantic surface (`java.source`, `api.java.*`, `com.sun.source`, `libs.javacapi`)",
                String.valueOf(semanticFiles)});
        rows.add(new String[]{"files using `NbBundle`", String.valueOf(nbBundleFiles)});
        rows.add(new String[]{"`Bundle.properties` files", String.valueOf(bundles.size())});
        rows.add(new String[]{"test files", String.valueOf(testFiles.size())});
        rows.add(new String[]{"modules with tests", testedModules.size() + " of " + mods.size()});

        Path netbeans = root.resolve("netbeans");
        if (Files.isDirectory(netbeans)) {
            List<Path> upstream = upstreamModules(netbeans);
            int swing = 0;
            int total = 0;
            for (Path module : upstream) {
                for (Path file : sources(module)) {
                    total++;
                    String text = read(file);
                    if (text.contains("import javax.swing") || text.contains("import java.awt")) {
                        swing++;
                    }
                }
            }
            rows.add(new String[]{"upstream editor/Java modules (ide/editor*, java/java.source*, java.java.hints*)",
                    String.valueOf(upstream.size())});
            rows.add(new String[]{"of their source files, Swing/AWT-touching", swing + " of " + total});
            rows.add(new String[]{"upstream modules depending on `org.openide.nodes`",
                    String.valueOf(dependants(netbeans, "org.openide.nodes"))});
            rows.add(new String[]{"upstream modules depending on `org.openide.explorer`",
                    String.valueOf(dependants(netbeans, "org.openide.explorer"))});

            // Other language clusters: the second-implementation soundness test (Appendix I).
            for (String cluster : new String[]{"groovy", "php", "enterprise", "webcommon"}) {
                Path dir = netbeans.resolve(cluster);
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                int clusterModules = 0;
                int clusterJava = 0;
                int clusterSwing = 0;
                int clusterSwingFree = 0;
                try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
                    for (Path module : children) {
                        if (!Files.isDirectory(module.resolve("src"))) {
                            continue;
                        }
                        clusterModules++;
                        int moduleSwing = 0;
                        int moduleJava = 0;
                        for (Path file : javaFiles(module.resolve("src"))) {
                            clusterJava++;
                            moduleJava++;
                            String text = read(file);
                            if (text.contains("import javax.swing") || text.contains("import java.awt")) {
                                clusterSwing++;
                                moduleSwing++;
                            }
                        }
                        if (moduleSwing == 0) {
                            clusterSwingFree++;
                        }
                        String moduleName = module.getFileName().toString().toLowerCase();
                        if (moduleSwing > 0 && (moduleName.contains("api") || moduleName.contains("spi"))) {
                            offenders.add(cluster + "/" + module.getFileName() + " "
                                    + moduleSwing + "/" + moduleJava);
                        }
                    }
                }
                rows.add(new String[]{"upstream `" + cluster + "` cluster (modules / Swing-free modules / java files / Swing-touching)",
                        clusterModules + " / " + clusterSwingFree + " / " + clusterJava + " / " + clusterSwing});
            }
            if (!offenders.isEmpty()) {
                offenders.sort(String::compareTo);
                rows.add(new String[]{"upstream `api*`/`spi*` modules with Swing files (the offender list)",
                        offenders.size() + ": " + String.join(", ", offenders)});
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("<!-- generated by docs/plan/tools/Counts.java; do not edit by hand -->\n");
        out.append("# Figures (generated)\n\n");
        out.append("Every number the plan states is derived here from the tree. Regenerate with\n");
        out.append("`java docs/plan/tools/Counts.java`; `--check` fails when this file is stale.\n\n");
        out.append("| Figure | Value |\n|---|---|\n");
        for (String[] row : rows) {
            out.append("| ").append(row[0]).append(" | ").append(row[1]).append(" |\n");
        }
        out.append("\n");
        return out.toString();
    }

    /** Modules: nbfx-&lt;cluster&gt;/&lt;module&gt;/manifest.mf. */
    private static List<Path> modules() throws IOException {
        List<Path> mods = new ArrayList<>();
        try (DirectoryStream<Path> clusters = Files.newDirectoryStream(root, "nbfx-*")) {
            for (Path cluster : clusters) {
                if (!Files.isDirectory(cluster)) {
                    continue;
                }
                try (DirectoryStream<Path> children = Files.newDirectoryStream(cluster)) {
                    for (Path module : children) {
                        if (Files.exists(module.resolve("manifest.mf"))) {
                            mods.add(module);
                        }
                    }
                }
            }
        }
        mods.sort(Path::compareTo);
        return mods;
    }

    /** The upstream modules measured for the bridge matrix. */
    private static List<Path> upstreamModules(Path netbeans) throws IOException {
        List<Path> out = new ArrayList<>();
        for (String area : new String[]{"ide", "java"}) {
            Path dir = netbeans.resolve(area);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
                for (Path child : children) {
                    String name = child.getFileName().toString();
                    boolean match = "ide".equals(area)
                            ? name.startsWith("editor") || name.startsWith("spi.editor")
                            : name.startsWith("java.editor") || name.startsWith("java.source")
                              || name.startsWith("java.hints");
                    if (match && Files.isDirectory(child.resolve("src"))) {
                        out.add(child);
                    }
                }
            }
        }
        out.sort(Path::compareTo);
        return out;
    }

    private static List<Path> sources(Path module) throws IOException {
        Path src = module.resolve("src");
        return Files.isDirectory(src) ? javaFiles(src) : List.of();
    }

    private static List<Path> javaFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> p.getFileName().toString().endsWith(".java"))
                .filter(Files::isRegularFile)
                .filter(Counts::isTracked)
                .forEach(files::add);
        }
        files.sort(Path::compareTo);
        return files;
    }

    /** Files matching a glob of path segments below root, e.g. matching(root, "nbfx-*", "*", "test", "**&#47;*.java"). */
    private static int dependants(Path netbeans, String codeName) throws IOException {
        String marker = "<code-name-base>" + codeName + "</code-name-base>";
        int count = 0;
        try (DirectoryStream<Path> clusters = Files.newDirectoryStream(netbeans, "*")) {
            for (Path cluster : clusters) {
                if (!Files.isDirectory(cluster)) {
                    continue;
                }
                try (DirectoryStream<Path> modules = Files.newDirectoryStream(cluster)) {
                    for (Path module : modules) {
                        Path project = module.resolve("nbproject/project.xml");
                        if (Files.exists(project) && read(project).contains(marker)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    /** Files matching a glob of path segments below root, e.g. matching(root, "nbfx-*", "*", "test", "**&#47;*.java"). */
    private static List<Path> matching(Path base, String... segments) throws IOException {
        List<Path> current = new ArrayList<>(List.of(base));
        for (String segment : segments) {
            List<Path> next = new ArrayList<>();
            if (segment.contains("/") && segment.startsWith("**")) {
                String suffix = segment.substring(segment.indexOf('/') + 1);
                for (Path dir : current) {
                    if (!Files.isDirectory(dir)) {
                        continue;
                    }
                    try (Stream<Path> walk = Files.walk(dir)) {
                        walk.filter(p -> p.getFileName().toString().matches(globToRegex(suffix)))
                            .filter(Files::isRegularFile)
                            .filter(Counts::isTracked)
                            .forEach(next::add);
                    }
                }
            } else {
                for (Path dir : current) {
                    if (!Files.isDirectory(dir)) {
                        continue;
                    }
                    try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
                        for (Path child : children) {
                            if (child.getFileName().toString().matches(globToRegex(segment))) {
                                next.add(child);
                            }
                        }
                    }
                }
            }
            current = next;
        }
        current.sort(Path::compareTo);
        return current;
    }

    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> regex.append("[^/]*");
                case '?' -> regex.append("[^/]");
                case '.' -> regex.append("\\.");
                default -> regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return regex.toString();
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    /** True if {@code file} is tracked by git in its repository, or if git is unavailable. */
    private static boolean isTracked(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        Path netbeans = root.resolve("netbeans").toAbsolutePath().normalize();
        Set<Path> set = trackedNetbeans != null && absolute.startsWith(netbeans) ? trackedNetbeans : tracked;
        return set == null || set.isEmpty() || set.contains(absolute);
    }

    /** The files tracked by git in {@code repo}, or null if git cannot be used there. */
    private static Set<Path> gitTracked(Path repo) {
        if (!Files.exists(repo.resolve(".git"))) {
            return null;
        }
        try {
            Process process = new ProcessBuilder("git", "-C", repo.toString(), "ls-files", "-z")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            byte[] output = process.getInputStream().readAllBytes();
            if (process.waitFor() != 0) {
                return null;
            }
            Set<Path> files = new TreeSet<>();
            for (String relative : new String(output, StandardCharsets.UTF_8).split("\0")) {
                if (!relative.isEmpty()) {
                    files.add(repo.resolve(relative).toAbsolutePath().normalize());
                }
            }
            return files;
        } catch (IOException ex) {
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }
}
