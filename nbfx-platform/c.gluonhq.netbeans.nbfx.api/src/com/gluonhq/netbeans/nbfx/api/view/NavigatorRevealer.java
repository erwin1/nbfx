/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
