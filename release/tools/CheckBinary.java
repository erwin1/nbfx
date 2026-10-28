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
 * Checks that the binary zip really serves every supported platform (release-verify).
 *
 * - the native libraries in nbfx-platform/modules/lib/ have the format and CPU of their place, read
 *   from the file headers: *.dll PE x64 and *.so ELF x86-64 in lib/, Mach-O arm64 in lib/aarch64/,
 *   Mach-O x86_64 in lib/x86_64/; each of the four is there and nothing else is;
 * - javafx.graphics.jar is exactly the union of the SDKs' javafx.graphics.jar given: every entry of
 *   every SDK jar, byte-identical (by CRC-32 and size), and no other entry. That covers the glass
 *   toolkit of each platform (com/sun/glass/ui/mac|win|gtk), which the check also names;
 * - bin/ holds the launchers bin/netbeans (executable) and bin/netbeans64.exe, and nothing else.
 *
 * Pure Java, no dependencies; runs as a single-file source program:
 *
 *   java release/tools/CheckBinary.java <zip> <root dir in the zip> <SDK javafx.graphics.jar>...
 */
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class CheckBinary {

    private enum Format { MACHO_ARM64, MACHO_X86_64, PE_X64, ELF_X86_64 }

    private static final List<String> GLASS = List.of("mac", "win", "gtk");

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: CheckBinary <zip> <root dir> <SDK javafx.graphics.jar>...");
            System.exit(2);
        }
        List<String> errors = new ArrayList<>();
        try (FileSystem zip = FileSystems.newFileSystem(URI.create("jar:" + Path.of(args[0]).toUri()),
                Map.of("enablePosixFileAttributes", "true"))) {
            Path root = zip.getPath("/", args[1]);
            Path lib = root.resolve("nbfx-platform/modules/lib");
            checkNatives(lib, Set.of("aarch64", "x86_64"), Map.of(".dll", Format.PE_X64, ".so", Format.ELF_X86_64), errors);
            checkNatives(lib.resolve("aarch64"), Set.of(), Map.of(".dylib", Format.MACHO_ARM64), errors);
            checkNatives(lib.resolve("x86_64"), Set.of(), Map.of(".dylib", Format.MACHO_X86_64), errors);
            List<Path> sdks = List.of(args).subList(2, args.length).stream().map(Path::of).toList();
            checkGraphics(root.resolve("nbfx-platform/modules/ext/javafx.graphics.jar"), sdks, errors);
            checkLaunchers(root.resolve("bin"), errors);
        }
        if (!errors.isEmpty()) {
            errors.forEach(e -> System.err.println(args[0] + ": " + e));
            System.exit(1);
        }
        System.out.println(args[0] + ": natives, javafx.graphics.jar and launchers OK for every platform");
    }

    /** Every file in @dir has one of the extensions of @expected and its format; each extension is there. */
    private static void checkNatives(Path dir, Set<String> subdirs, Map<String, Format> expected, List<String> errors) throws IOException {
        if (!Files.isDirectory(dir)) {
            errors.add(dir + " is missing");
            return;
        }
        Set<String> seen = new TreeSet<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString().replace("/", "");
                if (Files.isDirectory(file)) {
                    if (!subdirs.contains(name)) {
                        errors.add(file + " is not expected");
                    }
                    continue;
                }
                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                Format want = expected.get(ext);
                if (want == null) {
                    errors.add(file + " is not expected (" + dir + " holds " + expected.keySet() + ")");
                    continue;
                }
                seen.add(ext);
                Format actual = format(file);
                if (actual != want) {
                    errors.add(file + " is " + (actual == null ? "not a known native library" : actual) + ", expected " + want);
                }
            }
        }
        if (!seen.equals(expected.keySet())) {
            errors.add(dir + " holds " + seen + " libraries, expected " + new TreeSet<>(expected.keySet()));
        }
    }

    private static Format format(Path file) throws IOException {
        byte[] head = new byte[64];
        int n;
        try (InputStream in = Files.newInputStream(file)) {
            n = in.readNBytes(head, 0, head.length);
        }
        ByteBuffer b = ByteBuffer.wrap(head, 0, n).order(ByteOrder.LITTLE_ENDIAN);
        if (n >= 8 && b.getInt(0) == 0xfeedfacf) {           // 64-bit Mach-O, little endian
            return switch (b.getInt(4)) {
                case 0x0100000c -> Format.MACHO_ARM64;
                case 0x01000007 -> Format.MACHO_X86_64;
                default -> null;
            };
        }
        if (n >= 20 && b.getInt(0) == 0x464c457f && head[4] == 2 && head[5] == 1) {   // ELF64, little endian
            return b.getShort(18) == 0x3e ? Format.ELF_X86_64 : null;
        }
        if (n >= 64 && head[0] == 'M' && head[1] == 'Z') {
            int pe = b.getInt(0x3c);
            byte[] header = new byte[6];
            try (InputStream in = Files.newInputStream(file)) {
                in.skipNBytes(pe);
                if (in.readNBytes(header, 0, 6) == 6 && header[0] == 'P' && header[1] == 'E' && header[2] == 0 && header[3] == 0) {
                    int machine = (header[4] & 0xff) | (header[5] & 0xff) << 8;
                    return machine == 0x8664 ? Format.PE_X64 : null;
                }
            }
        }
        return null;
    }

    /** CRC-32 and size of an entry; directories are left out. */
    private record Content(long crc, long size) {}

    private static void checkGraphics(Path jar, List<Path> sdks, List<String> errors) throws IOException {
        if (!Files.isRegularFile(jar)) {
            errors.add(jar + " is missing");
            return;
        }
        Map<String, Content> merged = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(jar))) {
            byte[] buffer = new byte[65536];
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                if (e.isDirectory()) {
                    continue;
                }
                CRC32 crc = new CRC32();
                long size = 0;
                for (int n; (n = in.read(buffer)) > 0; size += n) {
                    crc.update(buffer, 0, n);
                }
                if (merged.put(e.getName(), new Content(crc.getValue(), size)) != null) {
                    errors.add(jar + " holds " + e.getName() + " twice");
                }
            }
        }
        Map<String, Content> union = new HashMap<>();
        for (Path sdk : sdks) {
            try (ZipFile z = new ZipFile(sdk.toFile())) {
                for (ZipEntry e : z.stream().filter(e -> !e.isDirectory()).toList()) {
                    Content c = new Content(e.getCrc(), e.getSize());
                    Content other = union.putIfAbsent(e.getName(), c);
                    if (other != null && !other.equals(c)) {
                        errors.add(e.getName() + " differs between the SDKs; the union is not well defined");
                    }
                }
            }
        }
        for (var e : union.entrySet()) {
            Content c = merged.get(e.getKey());
            if (c == null) {
                errors.add(jar + " lacks " + e.getKey());
            } else if (!c.equals(e.getValue())) {
                errors.add(jar + ": " + e.getKey() + " differs from the SDKs'");
            }
        }
        for (String name : merged.keySet()) {
            if (!union.containsKey(name)) {
                errors.add(jar + " holds " + name + ", which no SDK has");
            }
        }
        for (String glass : GLASS) {
            if (merged.keySet().stream().noneMatch(n -> n.startsWith("com/sun/glass/ui/" + glass + "/"))) {
                errors.add(jar + " lacks the " + glass + " glass toolkit");
            }
        }
    }

    private static void checkLaunchers(Path bin, List<String> errors) throws IOException {
        Set<String> names = new TreeSet<>();
        if (Files.isDirectory(bin)) {
            try (Stream<Path> files = Files.list(bin)) {
                files.forEach(f -> names.add(f.getFileName().toString().replace("/", "")));
            }
        }
        Set<String> expected = new TreeSet<>(Set.of("netbeans", "netbeans64.exe"));
        if (!names.equals(expected)) {
            errors.add(bin + " holds " + names + ", expected " + expected);
        }
        Path unix = bin.resolve("netbeans");
        if (Files.isRegularFile(unix) && !Files.getPosixFilePermissions(unix).contains(PosixFilePermission.OWNER_EXECUTE)) {
            errors.add(unix + " is not executable");
        }
    }
}
