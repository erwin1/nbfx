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
 * Naming guard: exported packages must not expose Fx* or *Impl types.
 *
 * Public API names are capability nouns (PLAN §3.2); implementation names may
 * keep the Fx/Impl prefix only in non-exported packages (Appendix C.1).
 */
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class NamingGuard {

    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+);");
    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "(?m)^\\s*public\\s+(?:static\\s+|final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*"
            + "(?:class|interface|enum|record|@interface)\\s+(\\w+)");

    public static void main(String[] args) throws IOException {
        Path root = args.length == 0 ? Paths.get(System.getProperty("user.dir")) : Paths.get(args[0]);
        List<String> violations = new ArrayList<>();
        try (DirectoryStream<Path> clusters = Files.newDirectoryStream(root, "nbfx-*")) {
            for (Path cluster : clusters) {
                if (!Files.isDirectory(cluster)) {
                    continue;
                }
                try (DirectoryStream<Path> modules = Files.newDirectoryStream(cluster)) {
                    for (Path module : modules) {
                        Path project = module.resolve("nbproject/project.xml");
                        Path src = module.resolve("src");
                        if (!Files.exists(project) || !Files.isDirectory(src)) {
                            continue;
                        }
                        checkModule(module, project, src, violations);
                    }
                }
            }
        }
        if (violations.isEmpty()) {
            System.out.println("[naming] OK: no Fx*/Impl type in an exported package");
            return;
        }
        for (String violation : violations) {
            System.out.println("[naming] " + violation);
        }
        System.exit(1);
    }

    private static void checkModule(Path module, Path project, Path src, List<String> violations) throws IOException {
        String xml = Files.readString(project, StandardCharsets.UTF_8);
        List<String> subtrees = values(xml, "subpackages");
        List<String> packages = values(xml, "package");
        if (subtrees.isEmpty() && packages.isEmpty()) {
            return;
        }
        try (Stream<Path> files = Files.walk(src)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                Matcher pkg = PACKAGE.matcher(text);
                if (!pkg.find()) {
                    continue;
                }
                String name = pkg.group(1);
                boolean exported = packages.contains(name)
                        || subtrees.stream().anyMatch(name::startsWith);
                if (!exported) {
                    continue;
                }
                Matcher type = PUBLIC_TYPE.matcher(text);
                while (type.find()) {
                    String typeName = type.group(1);
                    if (typeName.startsWith("Fx") || typeName.endsWith("Impl")) {
                        violations.add(module.getFileName() + ": " + name + "." + typeName
                                + " (" + file + ")");
                    }
                }
            }
        }
    }

    private static List<String> values(String xml, String tag) {
        List<String> out = new ArrayList<>();
        Matcher matcher = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">").matcher(xml);
        while (matcher.find()) {
            out.add(matcher.group(1).trim());
        }
        return out;
    }
}
