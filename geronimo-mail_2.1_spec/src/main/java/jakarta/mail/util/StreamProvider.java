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

package jakarta.mail.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

/**
 * Factory for the transfer-encoding and line-oriented streams that a
 * Jakarta Mail implementation needs.  Implementations of this interface
 * are discovered through the service-loader mechanism; application code
 * usually obtains an instance from {@link jakarta.mail.Session#getStreamProvider}
 * rather than performing the lookup itself.
 *
 * @since JavaMail 2.1
 */
public interface StreamProvider {

    /**
     * The content-transfer-encoding names understood by the Mail API,
     * each paired with its wire label.
     *
     * @since JavaMail 2.1
     */
    enum EncoderTypes {

        BASE_64("base64"),
        B_ENCODER("b"),
        Q_ENCODER("q"),
        BINARY_ENCODER("binary"),
        BIT7_ENCODER("7bit"),
        BIT8_ENCODER("8bit"),
        QUOTED_PRINTABLE_ENCODER("quoted-printable"),
        UU_ENCODER("uuencode"),
        X_UU_ENCODER("x-uuencode"),
        X_UUE("x-uue");

        private final String encoder;

        EncoderTypes(final String encoder) {
            this.encoder = encoder;
        }

        public String getEncoder() {
            return encoder;
        }
    }

    /**
     * Wraps a stream carrying base64 data so that reads deliver the
     * decoded bytes.
     *
     * @param in the stream carrying base64 data
     * @return a decoding stream
     */
    InputStream inputBase64(InputStream in);

    /**
     * Wraps a stream so that bytes written to it are emitted in
     * base64 form.
     *
     * @param out the target stream for the encoded data
     * @return an encoding stream
     */
    OutputStream outputBase64(OutputStream out);

    /**
     * Returns a pass-through reader for the identity encodings
     * ("binary", "7bit" and "8bit"), which need no transformation.
     *
     * @param in the stream to read from
     * @return a stream delivering the data unchanged
     */
    InputStream inputBinary(InputStream in);

    /**
     * Returns a pass-through writer for the identity encodings
     * ("binary", "7bit" and "8bit"), which need no transformation.
     *
     * @param out the stream to write to
     * @return a stream passing the data through unchanged
     */
    OutputStream outputBinary(OutputStream out);

    /**
     * Wraps a stream so that written bytes are emitted using the
     * RFC 2047 "B" encoding (base64 for encoded words).
     *
     * @param out the target stream for the encoded data
     * @return an encoding stream
     */
    OutputStream outputB(OutputStream out);

    /**
     * Wraps a stream carrying RFC 2047 "Q"-encoded data so that reads
     * deliver the decoded bytes.
     *
     * @param in the stream carrying the encoded data
     * @return a decoding stream
     */
    InputStream inputQ(InputStream in);

    /**
     * Wraps a stream so that written bytes are emitted using the
     * RFC 2047 "Q" encoding.
     *
     * @param out          the target stream for the encoded data
     * @param encodingWord whether the data forms a word in a phrase,
     *                     which tightens the set of characters that may
     *                     appear unencoded
     * @return an encoding stream
     */
    OutputStream outputQ(OutputStream out, boolean encodingWord);

    /**
     * Returns a reader that splits the underlying stream into lines,
     * as needed for mail protocol and header parsing.
     *
     * @param in        the stream to read from
     * @param allowutf8 whether line content may be interpreted as UTF-8
     *                  instead of pure US-ASCII
     * @return the line-oriented reader
     */
    LineInputStream inputLineStream(InputStream in, boolean allowutf8);

    /**
     * Returns a writer that emits strings as CRLF-terminated lines,
     * as needed for generating mail protocol data and headers.
     *
     * @param out       the stream to write to
     * @param allowutf8 whether line content may be written as UTF-8
     *                  instead of pure US-ASCII
     * @return the line-oriented writer
     */
    LineOutputStream outputLineStream(OutputStream out, boolean allowutf8);

    /**
     * Wraps a stream carrying quoted-printable data so that reads
     * deliver the decoded bytes.
     *
     * @param in the stream carrying the encoded data
     * @return a decoding stream
     */
    InputStream inputQP(InputStream in);

    /**
     * Wraps a stream so that written bytes are emitted in
     * quoted-printable form.
     *
     * @param out the target stream for the encoded data
     * @return an encoding stream
     */
    OutputStream outputQP(OutputStream out);

    /**
     * Returns a stream over the given byte array whose underlying data
     * may be shared by several concurrent readers.
     *
     * @param buff the bytes to read
     * @return a stream over the shared data
     */
    InputStream inputSharedByteArray(byte[] buff);

    /**
     * Wraps a stream carrying uuencoded data (any of the "uuencode",
     * "x-uuencode" or "x-uue" labels) so that reads deliver the decoded
     * bytes.
     *
     * @param in the stream carrying the encoded data
     * @return a decoding stream
     */
    InputStream inputUU(InputStream in);

    /**
     * Wraps a stream so that written bytes are emitted in uuencoded
     * form (the "uuencode", "x-uuencode" and "x-uue" labels).
     *
     * @param out      the target stream for the encoded data
     * @param filename an optional name to record in the encoded
     *                 preamble, may be null
     * @return an encoding stream
     */
    OutputStream outputUU(OutputStream out, String filename);

    /**
     * Locates and returns a {@link StreamProvider} implementation.  The
     * lookup honors a system property naming the implementation class,
     * then the {@link ServiceLoader} mechanism, and finally falls back
     * to the provider bundled with this specification jar.  Callers are
     * encouraged to reuse the returned instance instead of repeating
     * the lookup.
     *
     * @return the stream provider implementation
     */
    static StreamProvider provider() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new PrivilegedAction<StreamProvider>() {
                public StreamProvider run() {
                    return FactoryFinder.find(StreamProvider.class);
                }
            });
        } else {
            return FactoryFinder.find(StreamProvider.class);
        }
    }
}
