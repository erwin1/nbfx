package com.gluonhq.netbeans.nbfx.api.view;

import org.openide.filesystems.FileObject;

/**
 * Reveals a file in the navigator view that shows its project, bringing that view forward. A
 * targeted service replacing the navigator SPI's reveal method: the search-results UI asks the
 * registered revealer to show a hit instead of locating the navigator itself.
 *
 * @since 1.0
 */
public interface NavigatorRevealer {

    /**
     * Reveals {@code file} in the navigator view that contains it, showing and focusing that view.
     *
     * @param file the file to reveal and select
     * @return {@code true} when a navigator handled the request
     */
    boolean reveal(FileObject file);
}
