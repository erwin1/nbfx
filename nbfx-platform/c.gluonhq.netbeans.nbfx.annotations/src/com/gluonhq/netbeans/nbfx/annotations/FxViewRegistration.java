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
package com.gluonhq.netbeans.nbfx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a JavaFX tool view (a dockable tab: a navigator, a search result, a usages window...).
 * <p>
 * The annotated class must be public, must have a public no-argument constructor and must implement
 * the nbfx view SPI. It is published in the generated layer under {@code NbFx/Views/<id>.instance}
 * together with its metadata.
 *
 * @since 1.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface FxViewRegistration {

    /**
     * The stable identifier of the view, used to persist and restore its tab. Defaults to the
     * annotated class's binary name, which matches the default view id; a view with several
     * instances must set a distinct id per instance.
     */
    String id() default "";

    /** The display name of the tab; may be a {@code #key} bundle reference. */
    String displayName();

    /** The name of the icon resource shown on the tab, or an empty string for none. */
    String iconName() default "";

    /** Where the view is docked the first time it is shown. */
    FxViewLocation location() default FxViewLocation.LEFT;

    /** The position of the view relative to the other views in the same location. */
    int position() default Integer.MAX_VALUE;

    /** Whether this view is a navigator docked at start-up. */
    boolean navigator() default false;

    /**
     * The id of the window area this view is docked into, mirroring the Swing window modes
     * ({@code *.wsmode}); an empty string derives the area from {@link #location()}.
     */
    String area() default "";

    /** Whether this view's tab is attached to its area when the window system starts up. */
    boolean openAtStartup() default false;

    /** How the view's tab and its state survive restarts, mirroring the Swing persistence types. */
    FxPersistenceType persistenceType() default FxPersistenceType.ALWAYS;

    /**
     * The persistence behaviour of a view's tab.
     *
     * @since 1.0
     */
    enum FxPersistenceType {
        /** The tab and its state are restored across every restart. */
        ALWAYS,
        /** The tab is never persisted; it closes with the session. */
        NEVER,
        /** The tab is persisted only while it is open when the session ends. */
        ONLY_OPENED
    }
}
