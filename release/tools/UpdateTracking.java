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
 * Rewrites a module's update_tracking file for files swapped after the build.
 *
 * The per-platform release zips (release/release.xml, release-bin) replace libs.javafx's jars, natives
 * and licence texts with another platform's. Its update_tracking file lists every file of the module
 * with its CRC-32, so it must list the swapped-in files instead. Entries under the given prefixes are
 * dropped and every file under the cluster dir is added with its CRC-32; all other entries are kept.
 * Pure Java, no dependencies; runs as a single-file source program:
 *
 *   java release/tools/UpdateTracking.java <tracking.xml> <cluster dir> <out.xml> <prefix>...
 *
 * e.g. prefixes modules/ext/ modules/lib/ legal/javafx/ with the staged nbfx-libs dir.
 */
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;

public final class UpdateTracking {

    private static final Pattern FILE = Pattern.compile("^(\\s*)<file crc=\"(\\d+)\" name=\"([^\"]+)\"/>\\s*$");

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("usage: UpdateTracking <tracking.xml> <cluster dir> <out.xml> <prefix>...");
            System.exit(2);
        }
        Path tracking = Path.of(args[0]);
        Path cluster = Path.of(args[1]);
        Path out = Path.of(args[2]);
        List<String> prefixes = List.of(args).subList(3, args.length);

        List<String> lines = Files.readAllLines(tracking, StandardCharsets.UTF_8);
        // name -> crc, sorted by name as the harness writes them
        TreeMap<String, String> files = new TreeMap<>();
        String indent = null;
        int first = -1;
        int last = -1;
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = FILE.matcher(lines.get(i));
            if (m.matches()) {
                if (first < 0) {
                    first = i;
                    indent = m.group(1);
                }
                last = i;
                if (prefixes.stream().noneMatch(m.group(3)::startsWith)) {
                    files.put(m.group(3), m.group(2));
                }
            }
        }
        if (first < 0) {
            throw new IOException(tracking + " lists no files");
        }
        try (Stream<Path> walk = Files.walk(cluster)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                String name = cluster.relativize(file).toString().replace('\\', '/');
                if (prefixes.stream().noneMatch(name::startsWith)) {
                    throw new IOException(name + " is not under any of " + prefixes);
                }
                files.put(name, Long.toString(crc(file)));
            }
        }

        List<String> result = new ArrayList<>(lines.subList(0, first));
        for (var e : files.entrySet()) {
            result.add(indent + "<file crc=\"" + e.getValue() + "\" name=\"" + e.getKey() + "\"/>");
        }
        result.addAll(lines.subList(last + 1, lines.size()));
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.write(out, result, StandardCharsets.UTF_8);
    }

    private static long crc(Path file) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[65536];
        try (InputStream in = Files.newInputStream(file)) {
            for (int n; (n = in.read(buffer)) > 0; ) {
                crc.update(buffer, 0, n);
            }
        }
        return crc.getValue();
    }
}
