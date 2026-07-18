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

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;

import jakarta.mail.util.LineInputStream;

/**
 * Default implementation of {@link jakarta.mail.util.LineInputStream} that
 * reads lines terminated by CR, NL, CR-NL (or the common error case CR-CR-NL)
 * from an input stream.  The terminator is not part of the returned string.
 */
public class MailLineInputStream extends FilterInputStream implements LineInputStream {

    // do we decode the raw bytes as UTF-8 (true) or as an 8-bit charset (false)?
    private final boolean allowutf8;

    public MailLineInputStream(final InputStream in) {
        this(in, false);
    }

    public MailLineInputStream(final InputStream in, final boolean allowutf8) {
        // we need pushback capability to handle a lone CR line terminator followed
        // by real data.
        super(in instanceof PushbackInputStream ? in : new PushbackInputStream(in, 2));
        this.allowutf8 = allowutf8;
    }

    /**
     * Read a line terminated by a CR, NL or CR-NL sequence (a common error
     * case, CR-CR-NL, also terminates the line).  The line terminator is not
     * returned as part of the string.
     *
     * @return the next line from the stream, or null if no data is available.
     * @exception IOException for input errors.
     */
    @Override
    public String readLine() throws IOException {
        final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(128);
        final PushbackInputStream in = (PushbackInputStream) this.in;

        int ch = in.read();
        // no data at all available?  this is the end-of-data marker.
        if (ch == -1) {
            return null;
        }

        while (ch != -1) {
            // a new-line character is always an unconditional terminator.
            if (ch == '\n') {
                break;
            }
            if (ch == '\r') {
                // check what follows the CR.  A NL is consumed as part of the
                // terminator, anything else is pushed back.
                int next = in.read();
                boolean twoCRs = false;
                if (next == '\r') {
                    // the common error case of a CR-CR-NL sequence.
                    twoCRs = true;
                    next = in.read();
                }
                if (next != '\n') {
                    if (next != -1) {
                        in.unread(next);
                    }
                    if (twoCRs) {
                        in.unread('\r');
                    }
                }
                break;
            }
            lineBuffer.write(ch);
            ch = in.read();
        }

        final byte[] bytes = lineBuffer.toByteArray();
        return new String(bytes, 0, bytes.length, allowutf8 ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1);
    }
}
