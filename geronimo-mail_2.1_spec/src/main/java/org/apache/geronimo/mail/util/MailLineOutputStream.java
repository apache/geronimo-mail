/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.mail.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import jakarta.mail.util.LineOutputStream;

/**
 * Default implementation of {@link jakarta.mail.util.LineOutputStream} that
 * writes out strings as a sequence of bytes terminated by a CRLF sequence.
 */
public class MailLineOutputStream extends FilterOutputStream implements LineOutputStream {

    private static final byte[] CRLF = { (byte) '\r', (byte) '\n' };

    // do we encode strings as UTF-8 (true) or by truncating each char to 8 bits (false)?
    private final boolean allowutf8;

    public MailLineOutputStream(final OutputStream out) {
        this(out, false);
    }

    public MailLineOutputStream(final OutputStream out, final boolean allowutf8) {
        super(out);
        this.allowutf8 = allowutf8;
    }

    @Override
    public void writeln(final String s) throws IOException {
        write(getBytes(s));
        writeln();
    }

    @Override
    public void writeln() throws IOException {
        out.write(CRLF);
    }

    @Override
    public void write(final byte[] content) throws IOException {
        out.write(content);
    }

    private byte[] getBytes(final String s) {
        if (s == null || s.length() == 0) {
            return new byte[0];
        }
        if (allowutf8) {
            return s.getBytes(StandardCharsets.UTF_8);
        }
        // the string is expected to contain only US-ASCII characters, so just
        // take the low-order 8 bits of each character.
        final char[] chars = s.toCharArray();
        final byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }
}
