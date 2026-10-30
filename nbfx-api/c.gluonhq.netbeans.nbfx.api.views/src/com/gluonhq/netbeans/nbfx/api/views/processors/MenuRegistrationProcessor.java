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
package com.gluonhq.netbeans.nbfx.api.views.processors;

import com.gluonhq.netbeans.nbfx.api.views.MenuRegistration;
import com.gluonhq.netbeans.nbfx.api.views.MenuRegistrations;
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
 * Generates the layer entry for {@link MenuRegistration}: the {@code NbFx/<path>/<id>} folder
 * with its {@code position} (and optional display name and separator), so the window can build the
 * set and order of menus - and of the submenus of any surface - from the layer.
 *
 * @since 1.0
 */
@ServiceProvider(service = Processor.class)
public class MenuRegistrationProcessor extends LayerGeneratingProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                MenuRegistration.class.getCanonicalName(),
                MenuRegistrations.class.getCanonicalName());
    }

    @Override
    protected boolean handleProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
            throws LayerGenerationException {
        if (roundEnv.processingOver()) {
            return false;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(MenuRegistration.class)) {
            MenuRegistration registration = element.getAnnotation(MenuRegistration.class);
            if (registration != null) {
                register(element, registration);
            }
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(MenuRegistrations.class)) {
            MenuRegistrations registrations = element.getAnnotation(MenuRegistrations.class);
            if (registrations != null) {
                for (MenuRegistration registration : registrations.value()) {
                    register(element, registration);
                }
            }
        }
        return true;
    }

    private void register(Element element, MenuRegistration registration)
            throws LayerGenerationException {
        File folder = layer(element).folder("NbFx/" + registration.path() + "/" + registration.id());
        folder.intvalue("position", registration.position());
        if (!registration.displayName().isEmpty()) {
            folder.bundlevalue("displayName", registration.displayName());
        }
        if (registration.separatorBefore()) {
            folder.boolvalue("separatorBefore", true);
        }
        folder.write();
    }
}
