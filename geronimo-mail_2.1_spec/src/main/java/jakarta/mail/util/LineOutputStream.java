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

import java.io.IOException;

/**
 * A writer that turns strings into CRLF-terminated byte sequences on an
 * underlying stream, as needed when generating RFC 822 header blocks and
 * similar protocol data.  Unless the implementation was configured to
 * permit UTF-8, the strings are expected to be pure US-ASCII.
 *
 * @since JavaMail 2.1
 */
public interface LineOutputStream {

    /**
     * Emits the given string followed by a CRLF terminator.
     *
     * @param s the line content to emit
     * @throws IOException if writing to the underlying stream fails
     */
    void writeln(String s) throws IOException;

    /**
     * Emits a bare CRLF terminator, producing an empty line.
     *
     * @throws IOException if writing to the underlying stream fails
     */
    void writeln() throws IOException;

    /**
     * Copies the given bytes to the underlying stream unchanged, without
     * appending a terminator.
     *
     * @param content the bytes to copy
     * @throws IOException if writing to the underlying stream fails
     */
    void write(byte[] content) throws IOException;
}
