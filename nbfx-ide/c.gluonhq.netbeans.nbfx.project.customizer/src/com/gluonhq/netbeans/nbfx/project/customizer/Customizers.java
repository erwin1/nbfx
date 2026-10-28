package com.gluonhq.netbeans.nbfx.project.customizer;

import com.gluonhq.netbeans.nbfx.project.ui.api.ProjectKinds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;

/**
 * Collects the {@link CustomizerPanel}s that apply to a project: those registered for the
 * project's kind (its {@link ProjectKinds#providerOf(Project) provider id}) plus the type-agnostic
 * ones, ordered by {@link CustomizerPanel#position()}.
 */
public final class Customizers {

    private Customizers() {
    }

    public static List<CustomizerPanel> panelsFor(Project project) {
        String kind = ProjectKinds.providerOf(project).id();
        List<CustomizerPanel> panels = new ArrayList<>();
        for (CustomizerPanel panel : Lookup.getDefault().lookupAll(CustomizerPanel.class)) {
            if (panel.appliesTo(project, kind)) {
                panels.add(panel);
            }
        }
        panels.sort(Comparator.comparingInt(CustomizerPanel::position));
        return panels;
    }
}
