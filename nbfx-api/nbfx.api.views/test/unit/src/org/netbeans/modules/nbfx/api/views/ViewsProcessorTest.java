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
package org.netbeans.modules.nbfx.api.views;

import org.netbeans.modules.nbfx.api.layer.LayerPaths;
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
 * Verifies the view, menu and tool bar registration processors and the layer paths their readers
 * use.
 *
 * @since 1.0
 */
public class ViewsProcessorTest {

    private static final String SOURCE =
            "package demo;\n"
            + "import org.netbeans.modules.nbfx.api.views.*;\n"
            + "@ViewRegistration(id = \"demo.view\", displayName = \"Demo View\","
            + " location = ViewLocation.LEFT_BOTTOM, openAtStartup = true)\n"
            + "@MenuRegistration(id = \"Navigate\", position = 40)\n"
            + "@MenuRegistration(id = \"Inspect\", path = \"ContextMenus/Editor\", position = 100,"
            + " separatorBefore = true)\n"
            + "@ToolbarRegistration(id = \"Main\", displayName = \"Main\", position = 10)\n"
            + "public class DemoViews {}\n";

    private static final String PROCESSORS = String.join(",",
            "org.netbeans.modules.nbfx.api.views.processors.ViewRegistrationProcessor",
            "org.netbeans.modules.nbfx.api.views.processors.MenuRegistrationProcessor",
            "org.netbeans.modules.nbfx.api.views.processors.ToolbarRegistrationProcessor");

    @Test
    public void generatesLayerFragments() throws Exception {
        String xml = ProcessorTestSupport.compile(PROCESSORS,
                Map.of("demo/DemoViews.java", SOURCE));

        assertTrue(xml, xml.contains("name=\"demo.view.instance\""));
        assertTrue(xml, xml.contains("location\" stringvalue=\"LEFT_BOTTOM\""));
        assertTrue(xml, xml.contains("boolvalue=\"true\" name=\"openAtStartup\""));
        assertTrue(xml, xml.contains("<folder name=\"Navigate\">"));
        assertTrue(xml, xml.contains("<folder name=\"ContextMenus\">"));
        assertTrue(xml, xml.contains("<folder name=\"Editor\">"));
        assertTrue(xml, xml.contains("<folder name=\"Inspect\">"));
        assertTrue(xml, xml.contains("boolvalue=\"true\" name=\"separatorBefore\""));
        assertTrue(xml, xml.contains("<folder name=\"Main\">"));
        assertTrue(xml, xml.contains("displayName\" stringvalue=\"Main\""));

        assertLayerPath(xml, LayerPaths.VIEWS);
        assertLayerPath(xml, LayerPaths.MENUS);
        assertLayerPath(xml, LayerPaths.TOOLBARS);
        assertLayerPath(xml, LayerPaths.CONTEXT_MENUS);
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
            Path dir = Files.createTempDirectory("nbfx-views-processor-test");
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
