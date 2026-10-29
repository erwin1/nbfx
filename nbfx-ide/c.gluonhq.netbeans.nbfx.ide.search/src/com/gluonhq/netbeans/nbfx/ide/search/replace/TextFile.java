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
package com.gluonhq.netbeans.nbfx.ide.search.replace;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;
import org.netbeans.api.queries.FileEncodingQuery;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;

/**
 * A text file read for editing: its text with {@code '\n'} line separators - the offsets of the
 * search - and what is needed to write it back as it was: its charset, its line separator and
 * whether it started with a byte order mark.
 *
 * @param text          the content with every line break as {@code '\n'}
 * @param charset       the encoding it was read with, from the {@link FileEncodingQuery}
 * @param lineSeparator the separator of its first line break; {@code "\n"} when it has none
 * @param bom           whether it started with U+FEFF
 */
record TextFile(String text, Charset charset, String lineSeparator, boolean bom) {

    TextFile {
        Objects.requireNonNull(text);
        Objects.requireNonNull(charset);
        Objects.requireNonNull(lineSeparator);
    }

    /**
     * Reads {@code file}, strictly: bytes that are not valid in its charset are an error, as a
     * lenient decode could not be written back unchanged.
     *
     * @throws CharacterCodingException for bytes the charset cannot decode
     * @throws IOException              when the file cannot be read
     */
    static TextFile read(FileObject file) throws IOException {
        byte[] bytes = file.asBytes();
        Charset charset = FileEncodingQuery.getEncoding(file);
        String decoded = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
        boolean bom = !decoded.isEmpty() && decoded.charAt(0) == '﻿';
        if (bom) {
            decoded = decoded.substring(1);
        }
        return new TextFile(normalize(decoded), charset, separatorOf(decoded), bom);
    }

    /** Writes {@code content} - with {@code '\n'} line breaks - to {@code file} as this file was encoded. */
    void write(FileObject file, String content) throws IOException {
        String out = lineSeparator.equals("\n") ? content : content.replace("\n", lineSeparator);
        if (bom) {
            out = "﻿" + out;
        }
        byte[] bytes = out.getBytes(charset);
        FileLock lock = file.lock();
        try (OutputStream stream = file.getOutputStream(lock)) {
            stream.write(bytes);
        } finally {
            lock.releaseLock();
        }
    }

    /** The separator of the first line break in {@code text}: {@code \r\n}, {@code \r} or {@code \n}. */
    static String separatorOf(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                return "\n";
            }
            if (c == '\r') {
                return i + 1 < text.length() && text.charAt(i + 1) == '\n' ? "\r\n" : "\r";
            }
        }
        return "\n";
    }

    /** {@code \r\n} and {@code \r} become {@code \n}, as the search reads files. */
    static String normalize(String text) {
        if (text.indexOf('\r') < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                sb.append('\n');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
