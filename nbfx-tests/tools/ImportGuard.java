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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Enforces the Java semantic boundary: {@code org.netbeans.api.java.source},
 * {@code org.netbeans.modules.java.source} and {@code org.netbeans.libs.javacapi} may be imported only
 * by modules whose {@code nbproject/nbfx-policy.properties} allows the family in {@code imports.allow}
 * (Appendix A.5 / B.5 of the plan). Pure Java, single-file source program:
 *
 * <pre>
 * java nbfx-tests/tools/ImportGuard.java [repository-root]
 * </pre>
 *
 * Exits 1 on a violation, 0 when every importer is covered by its module policy.
 */
public final class ImportGuard {

    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+)\\s*;");
    private static final Pattern ALLOW = Pattern.compile("^\\s*imports\\.allow\\s*=\\s*(.*)$");
    private static final String[][] FAMILIES = {
        {"org.netbeans.api.java.source", "java.source"},
        {"org.netbeans.modules.java.source", "java.source"},
        {"org.netbeans.libs.javacapi", "libs.javacapi"},
    };

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        int violations = 0;
        for (Path file : sources(root).toList()) {
            Path module = moduleOf(root, file);
            Set<String> allowed = allowed(module);
            List<String> lines = Files.readAllLines(file);
            for (int line = 0; line < lines.size(); line++) {
                Matcher matcher = IMPORT.matcher(lines.get(line));
                if (!matcher.find()) {
                    continue;
                }
                String imported = matcher.group(1);
                String family = familyOf(imported);
                if (family != null && !allowed.contains(family)) {
                    System.out.println("VIOLATION " + root.relativize(file) + ":" + (line + 1)
                            + " imports " + imported + " (" + family + " not allowed)");
                    violations++;
                }
            }
        }
        if (violations > 0) {
            System.out.println("[java-source] " + violations + " violation(s)");
            System.exit(1);
        }
        System.out.println("[java-source] OK");
    }

    private static Stream<Path> sources(Path root) throws IOException {
        return Files.walk(root)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .filter(path -> {
                    Path relative = root.relativize(path);
                    return relative.getNameCount() > 3
                            && relative.getName(0).toString().startsWith("nbfx-")
                            && "src".equals(relative.getName(2).toString());
                });
    }

    private static Path moduleOf(Path root, Path file) {
        Path relative = root.relativize(file);
        return root.resolve(relative.getName(0)).resolve(relative.getName(1));
    }

    private static String familyOf(String imported) {
        for (String[] family : FAMILIES) {
            if (imported.equals(family[0]) || imported.startsWith(family[0] + ".")) {
                return family[1];
            }
        }
        return null;
    }

    private static Set<String> allowed(Path module) throws IOException {
        Set<String> families = new HashSet<>();
        Path policy = module.resolve("nbproject/nbfx-policy.properties");
        if (!Files.isRegularFile(policy)) {
            return families;
        }
        for (String line : Files.readAllLines(policy)) {
            Matcher matcher = ALLOW.matcher(line);
            if (matcher.find()) {
                for (String family : matcher.group(1).split("[,\\s]+")) {
                    if (!family.isBlank()) {
                        families.add(family.trim());
                    }
                }
            }
        }
        return families;
    }
}
