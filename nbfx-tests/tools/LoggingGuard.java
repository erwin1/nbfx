import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** No System.out / printStackTrace in production sources. */
public final class LoggingGuard {
    public static void main(String[] args) throws IOException {
        Path root = Paths.get(args.length == 0 ? System.getProperty("user.dir") : args[0]);
        List<String> errors = new ArrayList<>();
        try (DirectoryStream<Path> clusters = Files.newDirectoryStream(root, "nbfx-*")) {
            for (Path cluster : clusters) {
                if (!Files.isDirectory(cluster)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(cluster)) {
                    for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java"))
                            .filter(p -> p.toString().contains("/src/") && !p.toString().contains("/test/")
                                    && !p.toString().contains("/build/"))::iterator) {
                        // The in-application smoke intentionally writes its report to stdout.
                        if (f.getFileName().toString().equals("ScenarioRunner.java")) continue;
                        for (String line : Files.readAllLines(f)) {
                            String trimmed = line.trim();
                            if (trimmed.startsWith("*") || trimmed.startsWith("//") || line.contains("\"")) continue;
                            if (line.contains("System.out.")) errors.add(f + ": System.out");
                            if (line.contains(".printStackTrace(") && !line.contains("Exceptions.printStackTrace(")) {
                                errors.add(f + ": printStackTrace");
                            }
                        }
                    }
                }
            }
        }
        if (!errors.isEmpty()) { errors.forEach(e -> System.out.println("[logging] " + e)); System.exit(1); }
        System.out.println("[logging] OK: no System.out/printStackTrace in production sources");
    }
}
