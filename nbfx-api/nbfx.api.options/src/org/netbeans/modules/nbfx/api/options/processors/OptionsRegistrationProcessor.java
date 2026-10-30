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
package org.netbeans.modules.nbfx.api.options.processors;

import org.netbeans.modules.nbfx.api.layer.LayerPaths;
import org.netbeans.modules.nbfx.api.options.OptionsContainerRegistration;
import org.netbeans.modules.nbfx.api.options.OptionsRegistration;
import org.netbeans.modules.nbfx.api.options.OptionsSubRegistration;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.openide.filesystems.annotations.LayerBuilder.File;
import org.openide.filesystems.annotations.LayerGeneratingProcessor;
import org.openide.filesystems.annotations.LayerGenerationException;
import org.openide.util.lookup.ServiceProvider;

/**
 * Generates the layer entries for the JavaFX options registrations:
 * {@code NbFx/Options/<id>.instance} for a top-level category,
 * {@code NbFx/Options/<location>/<id>.instance} for a sub-panel, and a
 * {@code NbFx/Options/<id>} folder for a container category.
 *
 * @since 1.0
 */
@ServiceProvider(service = Processor.class)
public class OptionsRegistrationProcessor extends LayerGeneratingProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                OptionsRegistration.class.getCanonicalName(),
                OptionsSubRegistration.class.getCanonicalName(),
                OptionsContainerRegistration.class.getCanonicalName());
    }

    @Override
    protected boolean handleProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
            throws LayerGenerationException {
        if (roundEnv.processingOver()) {
            return false;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(OptionsRegistration.class)) {
            OptionsRegistration registration = element.getAnnotation(OptionsRegistration.class);
            if (registration == null) {
                continue;
            }
            File file = layer(element).instanceFile(LayerPaths.OPTIONS, registration.id(), (Class<?>) null);
            file.stringvalue("displayName", registration.categoryName());
            file.stringvalue("iconName", registration.iconName());
            file.stringvalue("keywords", registration.keywords());
            file.intvalue("position", registration.position());
            file.boolvalue("container", false);
            file.write();
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(OptionsSubRegistration.class)) {
            OptionsSubRegistration registration = element.getAnnotation(OptionsSubRegistration.class);
            if (registration == null) {
                continue;
            }
            File file = layer(element).instanceFile(
                    LayerPaths.OPTIONS + "/" + registration.location(), registration.id(), (Class<?>) null);
            file.stringvalue("displayName", registration.displayName());
            file.stringvalue("keywords", registration.keywords());
            file.intvalue("position", registration.position());
            file.boolvalue("sub", true);
            file.write();
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(OptionsContainerRegistration.class)) {
            OptionsContainerRegistration registration = element.getAnnotation(OptionsContainerRegistration.class);
            if (registration == null) {
                continue;
            }
            File folder = layer(element).folder(LayerPaths.OPTIONS + "/" + registration.id());
            folder.stringvalue("displayName", registration.categoryName());
            folder.stringvalue("iconName", registration.iconName());
            folder.stringvalue("keywords", registration.keywords());
            folder.intvalue("position", registration.position());
            folder.boolvalue("container", true);
            folder.write();
        }
        return true;
    }
}
