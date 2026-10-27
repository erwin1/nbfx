package com.gluonhq.netbeans.nbfx.progress;

import static com.gluonhq.netbeans.nbfx.api.actions.ActionIds.SHOW_PROGRESS;

import com.gluonhq.netbeans.nbfx.annotations.ActionReference;

/** Places the View &#9656; Progress command in the View menu. */
@ActionReference(id = SHOW_PROGRESS, path = "Menus/View", position = 100)
final class ProgressMenuRegistrations {

    private ProgressMenuRegistrations() {
    }
}
