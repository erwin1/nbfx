import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** One JDK/FX baseline: one javac.release, no javafx.web, one --add-exports location. */
public final class BaselineGuard {
    public static void main(String[] args) throws IOException {
        Path root = Paths.get(args.length == 0 ? System.getProperty("user.dir") : args[0]);
        List<String> errors = new ArrayList<>();
        Set<String> releases = new TreeSet<>();
        try (DirectoryStream<Path> clusters = Files.newDirectoryStream(root, "nbfx-*")) {
            for (Path cluster : clusters) {
                if (!Files.isDirectory(cluster)) {
                    continue;
                }
                try (DirectoryStream<Path> modules = Files.newDirectoryStream(cluster)) {
                    for (Path module : modules) {
                        if (!Files.isDirectory(module)) {
                            continue;
                        }
                        Path props = module.resolve("nbproject/project.properties");
                        if (Files.exists(props)) {
                            for (String line : Files.readAllLines(props)) {
                                if (line.startsWith("javac.release=")) releases.add(line.substring(14).trim());
                            }
                        }
                        try (Stream<Path> files = Files.exists(module) ? Files.walk(module) : Stream.empty()) {
                            for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)
                                    .filter(p -> !p.toString().contains("/build/") && !p.toString().contains("/dist/"))::iterator) {
                                String n = f.getFileName().toString();
                                if (n.equals("javafx.web.jar") || n.startsWith("javafx.web.")) {
                                    errors.add("javafx.web bundled: " + f);
                                }
                            }
                        }
                        if (Files.exists(module.resolve("src"))) {
                            try (Stream<Path> files = Files.walk(module.resolve("src"))) {
                                long n = files.filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".properties"))
                                        .filter(Files::isRegularFile)
                                        .filter(p -> { try { return Files.readString(p).contains("--add-exports"); } catch (IOException e) { return false; } })
                                        .count();
                                if (n > 0) releases.add("add-exports:" + module.getFileName());
                            }
                        }
                    }
                }
            }
        }
        Set<String> javac = releases.stream().filter(s -> s.matches("\\d+")).collect(Collectors.toSet());
        if (javac.size() != 1) errors.add("javac.release values: " + javac);
        long addExportsFiles = releases.stream().filter(s -> s.startsWith("add-exports:")).count();
        if (addExportsFiles > 1) errors.add("--add-exports appears in " + addExportsFiles + " modules: " + releases);
        if (!errors.isEmpty()) { errors.forEach(e -> System.out.println("[baseline] " + e)); System.exit(1); }
        System.out.println("[baseline] OK: javac.release " + javac + ", no javafx.web, one --add-exports location");
    }
}
