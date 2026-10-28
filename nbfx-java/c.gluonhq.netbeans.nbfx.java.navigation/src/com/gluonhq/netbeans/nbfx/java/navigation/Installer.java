package com.gluonhq.netbeans.nbfx.java.navigation;

import java.util.logging.Logger;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    private static final Logger LOG = Logger.getLogger(Installer.class.getName());

    @Override
    public void restored() {
        LOG.info("nbfx-project-ui-java has been restored.");
    }
}

