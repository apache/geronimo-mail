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

import java.io.IOException;
import java.io.InputStream;

/**
 * An implementation of a FilterInputStream that decodes the stream data
 * using the RFC 2047 "Q" encoding.  This is the quoted-printable encoding
 * with the addition that an underscore character decodes to a space.
 */
public class QDecoderStream extends QuotedPrintableDecoderStream {

    /**
     * Stream constructor.
     *
     * @param in The InputStream this stream is filtering.
     */
    public QDecoderStream(final InputStream in) {
        super(in);
    }

    /**
     * Read a single byte from the stream, translating the "Q" encoding
     * underscore convention into a space character.
     *
     * @return The next decoded byte of the stream.  Returns -1 for an EOF condition.
     * @exception IOException
     */
    @Override
    public int read() throws IOException {
        final int ch = super.read();
        // in the "Q" encoding scheme, an un-encoded underscore represents a space.
        if (ch == '_') {
            return ' ';
        }
        return ch;
    }

    /**
     * Read a buffer of data from the input stream.
     *
     * @param buffer The target byte array the data is placed into.
     * @param offset The starting offset for the read data.
     * @param length How much data is requested.
     *
     * @return The number of bytes of data read.
     * @exception IOException
     */
    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        for (int i = 0; i < length; i++) {
            final int ch = read();
            if (ch == -1) {
                return i == 0 ? -1 : i;
            }
            buffer[offset + i] = (byte) ch;
        }
        return length;
    }
}
