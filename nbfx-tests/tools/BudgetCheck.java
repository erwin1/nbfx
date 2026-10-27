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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares a fresh budget JSON (written by {@code NBFX_BUDGET}) with the committed baseline and fails
 * when a mark regresses beyond the tolerance. Pure Java, single-file source program:
 *
 * <pre>
 * java nbfx-tests/tools/BudgetCheck.java docs/w0-budget-baseline.json /tmp/nbfx-budget.json
 * </pre>
 *
 * The default tolerance is 20 % or 100 ms, whichever is larger. Marks present only in the current
 * run are ignored; marks missing from the current run are reported but do not fail (a subset run is
 * still useful).
 */
public final class BudgetCheck {

    private static final Pattern NUMBER = Pattern.compile("\"([A-Za-z0-9_]+)\"\\s*:\\s*(\\d+)");

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("usage: java BudgetCheck.java <baseline.json> <current.json> [tolerance] [absoluteMs]");
            System.exit(2);
        }
        double tolerance = args.length > 2 ? Double.parseDouble(args[2]) : 0.20;
        long absolute = args.length > 3 ? Long.parseLong(args[3]) : 100;
        Map<String, Long> baseline = read(Path.of(args[0]));
        Map<String, Long> current = read(Path.of(args[1]));
        boolean failed = false;
        for (Map.Entry<String, Long> mark : baseline.entrySet()) {
            Long now = current.get(mark.getKey());
            if (now == null) {
                System.out.println("missing " + mark.getKey() + " (baseline " + mark.getValue() + " ms)");
                continue;
            }
            long limit = Math.max(mark.getValue() + absolute,
                    (long) Math.ceil(mark.getValue() * (1 + tolerance)));
            if (now > limit) {
                System.out.println("REGRESSION " + mark.getKey() + ": " + mark.getValue()
                        + " -> " + now + " ms (limit " + limit + ")");
                failed = true;
            } else {
                System.out.println("ok " + mark.getKey() + ": " + mark.getValue() + " -> " + now + " ms");
            }
        }
        if (failed) {
            System.exit(1);
        }
    }

    private static Map<String, Long> read(Path file) throws IOException {
        Map<String, Long> marks = new LinkedHashMap<>();
        Matcher matcher = NUMBER.matcher(Files.readString(file));
        while (matcher.find()) {
            marks.put(matcher.group(1), Long.parseLong(matcher.group(2)));
        }
        return marks;
    }
}
