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

import java.io.InputStream;
import java.io.OutputStream;

import jakarta.mail.util.LineInputStream;
import jakarta.mail.util.LineOutputStream;
import jakarta.mail.util.SharedByteArrayInputStream;
import jakarta.mail.util.StreamProvider;

/**
 * Default {@link StreamProvider} implementation backed by the encoder and
 * decoder streams shipped with the Geronimo Jakarta Mail spec bundle.
 */
public class MailStreamProvider implements StreamProvider {

    /**
     * Public no-argument constructor as required by the ServiceLoader mechanism.
     */
    public MailStreamProvider() {
    }

    @Override
    public InputStream inputBase64(final InputStream in) {
        return new Base64DecoderStream(in);
    }

    @Override
    public OutputStream outputBase64(final OutputStream out) {
        return new Base64EncoderStream(out);
    }

    @Override
    public InputStream inputBinary(final InputStream in) {
        // 'binary', '7bit' and '8bit' are pass-through encodings.
        return in;
    }

    @Override
    public OutputStream outputBinary(final OutputStream out) {
        // 'binary', '7bit' and '8bit' are pass-through encodings.
        return out;
    }

    @Override
    public OutputStream outputB(final OutputStream out) {
        // the "B" encoding is base64 without any line breaks inserted.
        return new Base64EncoderStream(out, Integer.MAX_VALUE);
    }

    @Override
    public InputStream inputQ(final InputStream in) {
        return new QDecoderStream(in);
    }

    @Override
    public OutputStream outputQ(final OutputStream out, final boolean encodingWord) {
        return new QEncoderStream(out, encodingWord);
    }

    @Override
    public LineInputStream inputLineStream(final InputStream in, final boolean allowutf8) {
        return new MailLineInputStream(in, allowutf8);
    }

    @Override
    public LineOutputStream outputLineStream(final OutputStream out, final boolean allowutf8) {
        return new MailLineOutputStream(out, allowutf8);
    }

    @Override
    public InputStream inputQP(final InputStream in) {
        return new QuotedPrintableDecoderStream(in);
    }

    @Override
    public OutputStream outputQP(final OutputStream out) {
        return new QuotedPrintableEncoderStream(out);
    }

    @Override
    public InputStream inputSharedByteArray(final byte[] buff) {
        return new SharedByteArrayInputStream(buff);
    }

    @Override
    public InputStream inputUU(final InputStream in) {
        return new UUDecoderStream(in);
    }

    @Override
    public OutputStream outputUU(final OutputStream out, final String filename) {
        if (filename == null) {
            return new UUEncoderStream(out);
        }
        return new UUEncoderStream(out, filename);
    }
}
