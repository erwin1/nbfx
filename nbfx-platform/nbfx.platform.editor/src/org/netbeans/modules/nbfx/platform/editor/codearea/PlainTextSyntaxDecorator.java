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
package org.netbeans.modules.nbfx.platform.editor.codearea;

import org.netbeans.modules.nbfx.platform.editor.decoration.LineDecoration;
import org.netbeans.modules.nbfx.platform.editor.processor.lex.BaseLexDecorationProcessor;
import org.netbeans.modules.nbfx.platform.editor.processor.semantics.SourceContext;
import java.util.List;
import org.openide.filesystems.FileObject;

/**
 * Syntax decorator for files without a registered language: it adds no token, semantic or
 * caret decorations. Used when no {@link SyntaxDecoratorFactory} supports the file extension.
 */
public final class PlainTextSyntaxDecorator extends BaseSyntaxDecorator {

    public PlainTextSyntaxDecorator(FileObject file) {
        super(file);
    }

    @Override
    protected BaseLexDecorationProcessor getLexProcessor(FileObject file) {
        return new BaseLexDecorationProcessor(new SourceContext(file, null)) {
            @Override
            protected List<List<LineDecoration>> analyze() {
                return List.of();
            }
        };
    }
}
