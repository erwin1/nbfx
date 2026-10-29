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
package com.gluonhq.netbeans.nbfx.editor.codearea;

import com.gluonhq.netbeans.nbfx.editor.processor.lex.BaseLexDecorationProcessor;
import com.gluonhq.netbeans.nbfx.editor.processor.lex.XmlLexDecorationProcessor;
import com.gluonhq.netbeans.nbfx.editor.processor.semantics.SourceContext;
import org.netbeans.api.xml.lexer.XMLTokenId;
import org.openide.filesystems.FileObject;

/**
 * A {@link BaseSyntaxDecorator} for XML/FXML sources. The syntax highlighting and the
 * caret-driven start/end tag matching (shown through the brace-match highlight) are done
 * by the lex-based defaults of the base class over an {@link XmlLexDecorationProcessor};
 * mark occurrences yield no highlights.
 */
public class XmlSyntaxDecorator extends BaseSyntaxDecorator {

    public XmlSyntaxDecorator(FileObject fo) {
        super(fo);
    }

    @Override
    protected BaseLexDecorationProcessor getLexProcessor(FileObject fo) {
        return new XmlLexDecorationProcessor(new SourceContext(fo, XMLTokenId.language()));
    }
}
