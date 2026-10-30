package com.gluonhq.netbeans.nbfx.ide.usages.ui;

import com.gluonhq.netbeans.nbfx.api.elements.ElementIcons;
import com.gluonhq.netbeans.nbfx.api.elements.SourceElementKind;
import com.gluonhq.netbeans.nbfx.api.file.FileIconProvider;
import com.gluonhq.netbeans.nbfx.api.icons.ToolIcons;
import com.gluonhq.netbeans.nbfx.api.project.ProjectIconProvider;
import com.gluonhq.netbeans.nbfx.api.usages.EnclosingElement;
import com.gluonhq.netbeans.nbfx.api.usages.Usage;
import com.gluonhq.netbeans.nbfx.api.usages.UsageKind;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * The icons of the Usages view: the result markers copied from NetBeans' refactoring modules (in
 * {@code icons/}), the tool bar glyphs shared through {@link ToolIcons}, the element glyphs shared
 * through {@link ElementIcons}, and file icons from the registered {@link FileIconProvider}.
 */
final class UsagesIcons {

    static final double SIZE = 16;

    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private UsagesIcons() {
    }

    /** The (cached) image {@code icons/<name>.png}. */
    static Image image(String name) {
        return CACHE.computeIfAbsent(name, UsagesIcons::load);
    }

    /** A fixed-size view of the icon {@code icons/<name>.png}. */
    static ImageView view(String name) {
        ImageView view = new ImageView(image(name));
        view.setFitWidth(SIZE);
        view.setFitHeight(SIZE);
        view.setPreserveRatio(true);
        return view;
    }

    /**
     * The result marker of a usage: comment, import, a variable access, or a plain reference; the
     * category comes from the language provider, not from the file extension.
     */
    static String usageIconName(Usage usage) {
        return switch (usage.kind()) {
            case COMMENT -> "found_item_comment";
            case IMPORT -> "found_item_import";
            case WRITE -> "found_item_write";
            case READ_WRITE -> "found_item_readwrite";
            case READ, REFERENCE -> "found_item_read";
            case OTHER -> "found_item_orange";
        };
    }

    /** The element glyph of a type / method / field / variable, or {@code null} for kinds without one. */
    static ImageView elementIcon(EnclosingElement element) {
        if (element.kind() == SourceElementKind.OTHER) {
            return null;
        }
        return ElementIcons.iconViewFor(element.kind(), element.typeKind(), element.modifiers());
    }

    static ImageView packageIcon() {
        return ElementIcons.iconViewFor(SourceElementKind.PACKAGE, null, 0);
    }

    /**
     * The icon the Projects view shows for the project rooted at {@code projectRoot}, falling back
     * to its file icon when no navigator knows the project.
     */
    static Node projectIcon(FileObject projectRoot) {
        if (projectRoot == null) {
            return null;
        }
        ProjectIconProvider provider = Lookup.getDefault().lookup(ProjectIconProvider.class);
        Node icon = provider == null ? null : provider.createIcon(projectRoot);
        return icon != null ? icon : fileIcon(projectRoot);
    }

    /** The registered file icon of {@code file} (a folder icon for roots), or {@code null} without a provider. */
    static Node fileIcon(FileObject file) {
        if (file == null) {
            return null;
        }
        FileIconProvider provider = Lookup.getDefault().lookup(FileIconProvider.class);
        return provider == null ? null : provider.createIcon(file);
    }

    /** This module's markers from {@code icons/}, the tool bar glyphs from the shared {@link ToolIcons}. */
    private static Image load(String name) {
        URL local = UsagesIcons.class.getResource("icons/" + name + ".png");
        if (local != null) {
            return new Image(local.toExternalForm());
        }
        return Objects.requireNonNull(ToolIcons.image(name), () -> "Missing icon " + name);
    }
}
