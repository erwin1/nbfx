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
package com.gluonhq.netbeans.nbfx.api.options;

import com.gluonhq.netbeans.nbfx.api.layer.LayerPaths;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Verifies the options registration processor and the layer path its reader uses.
 *
 * @since 1.0
 */
public class OptionsRegistrationProcessorTest {

    private static final String PACKAGE_INFO =
            "@OptionsContainerRegistration(id = \"demo.container\", categoryName = \"Container\","
            + " position = 30)\n"
            + "package demo;\n"
            + "import com.gluonhq.netbeans.nbfx.api.options.OptionsContainerRegistration;\n";

    private static final String SOURCE =
            "package demo;\n"
            + "import com.gluonhq.netbeans.nbfx.api.options.*;\n"
            + "@OptionsRegistration(id = \"demo.options\", categoryName = \"Demo\","
            + " iconName = \"demo.png\", keywords = \"demo,test\", position = 10)\n"
            + "public class DemoOptions {\n"
            + "    @OptionsSubRegistration(id = \"demo.sub\", location = \"demo.options\","
            + " displayName = \"Sub\", keywords = \"sub\", position = 20)\n"
            + "    public static class Sub {}\n"
            + "}\n";

    @Test
    public void generatesLayerFragments() throws Exception {
        String xml = ProcessorTestSupport.compile(
                "com.gluonhq.netbeans.nbfx.api.options.processors.OptionsRegistrationProcessor",
                Map.of(
                        "demo/package-info.java", PACKAGE_INFO,
                        "demo/DemoOptions.java", SOURCE));

        assertTrue(xml, xml.contains("name=\"demo.options.instance\""));
        assertTrue(xml, xml.contains("displayName\" stringvalue=\"Demo\""));
        assertTrue(xml, xml.contains("name=\"demo.sub.instance\""));
        assertTrue(xml, xml.contains("boolvalue=\"true\" name=\"sub\""));
        assertTrue(xml, xml.contains("name=\"demo.container\""));
        assertTrue(xml, xml.contains("boolvalue=\"true\" name=\"container\""));
        assertLayerPath(xml, LayerPaths.OPTIONS);
    }

    private static void assertLayerPath(String xml, String path) {
        for (String segment : path.split("/")) {
            assertTrue("layer path " + path + " missing segment " + segment + " in:\n" + xml,
                    xml.contains("name=\"" + segment + "\""));
        }
    }

    private static final class ProcessorTestSupport {

        private ProcessorTestSupport() {
        }

        static String compile(String processors, Map<String, String> sources) throws Exception {
            Path dir = Files.createTempDirectory("nbfx-options-processor-test");
            List<Path> files = new ArrayList<>();
            for (Map.Entry<String, String> source : sources.entrySet()) {
                Path file = dir.resolve(source.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, source.getValue());
                files.add(file);
            }
            Path out = dir.resolve("out");
            Files.createDirectories(out);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            assertNotNull("no system Java compiler", compiler);
            String classpath = System.getProperty("java.class.path");
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StringWriter output = new StringWriter();
            boolean success;
            try (StandardJavaFileManager fileManager =
                    compiler.getStandardFileManager(diagnostics, null, null)) {
                Iterable<? extends JavaFileObject> units =
                        fileManager.getJavaFileObjectsFromPaths(files);
                success = Boolean.TRUE.equals(compiler.getTask(output, fileManager, diagnostics,
                        List.of("-classpath", classpath, "-processorpath", classpath,
                                "-processor", processors, "-d", out.toString()),
                        null, units).call());
            }
            assertTrue("compilation failed: " + output + diagnostics.getDiagnostics(), success);
            Path layer = out.resolve("META-INF/generated-layer.xml");
            assertTrue("generated layer missing", Files.exists(layer));
            return Files.readString(layer);
        }
    }
}
