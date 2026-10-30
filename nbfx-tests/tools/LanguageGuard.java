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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Enforces the W1 language split: the platform editor core has no branch on a concrete language.
 * Language modules provide decorators through the SyntaxDecoratorFactory SPI instead.
 *
 * <pre>java nbfx-tests/tools/LanguageGuard.java [repository-root]</pre>
 */
public final class LanguageGuard {

    private static final Pattern LANGUAGE_LITERAL = Pattern.compile(
            "\"(java|css|xml|fxml)\"", Pattern.CASE_INSENSITIVE);
    private static final Path CORE = Path.of(
            "nbfx-platform", "nbfx.platform.editor", "src");

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        Path core = root.resolve(CORE);
        if (!Files.isDirectory(core)) {
            System.out.println("[languages] no platform editor core at " + core);
            return;
        }
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(core)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(path -> check(root, path, violations));
        }
        if (!violations.isEmpty()) {
            violations.forEach(v -> System.out.println("VIOLATION " + v));
            System.out.println("[languages] " + violations.size() + " violation(s)");
            System.exit(1);
        }
        System.out.println("[languages] OK: no concrete-language branch in the editor core");
    }

    private static void check(Path root, Path file, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                if (LANGUAGE_LITERAL.matcher(lines.get(i)).find()) {
                    violations.add(root.relativize(file) + ":" + (i + 1) + " " + lines.get(i).trim());
                }
            }
        } catch (IOException ex) {
            violations.add(root.relativize(file) + ": " + ex.getMessage());
        }
    }
}
