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

/**
 * An implementation of a FilterOutputStream that encodes the stream data
 * using the RFC 2047 "Q" encoding.  This is a variation of the
 * quoted-printable encoding used for encoded words:  a space is encoded
 * as an underscore and, depending on whether a word within a phrase or
 * unstructured text is encoded, a different set of characters needs to
 * be written encoded.
 */
public class QEncoderStream extends FilterOutputStream {

    // characters that must be encoded, in addition to the non-printable ones,
    // when encoding a word within a phrase (RFC 2047, section 5 (3)).
    private static final String WORD_SPECIALS = "=_?\"#$%&'(),.:;<>@[\\]^`{|}~";
    // characters that must be encoded, in addition to the non-printable ones,
    // when encoding unstructured text.
    private static final String TEXT_SPECIALS = "=_?";

    private static final byte[] HEX_CHARS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    // the specials set active for this stream.
    private final String specials;

    /**
     * Create a "Q" encoder stream that wraps the specified stream.
     *
     * @param out          The wrapped output stream.
     * @param encodingWord true if we are Q-encoding a word within a phrase.
     */
    public QEncoderStream(final OutputStream out, final boolean encodingWord) {
        super(out);
        this.specials = encodingWord ? WORD_SPECIALS : TEXT_SPECIALS;
    }

    @Override
    public void write(final int ch) throws IOException {
        final int c = ch & 0xff;
        if (c == ' ') {
            // spaces are encoded as underscores.
            out.write('_');
        } else if (c < 040 || c >= 0177 || specials.indexOf(c) >= 0) {
            // non-printable characters and the specials get written encoded.
            out.write('=');
            out.write(HEX_CHARS[c >> 4]);
            out.write(HEX_CHARS[c & 0x0f]);
        } else {
            // printable ASCII characters just get written unchanged.
            out.write(c);
        }
    }

    @Override
    public void write(final byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    @Override
    public void write(final byte[] data, final int offset, final int length) throws IOException {
        for (int i = 0; i < length; i++) {
            write(data[offset + i]);
        }
    }
}
