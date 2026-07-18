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
 * A reader that splits an underlying byte stream into text lines, in the
 * way mail protocols and RFC 822 header blocks require.  Depending on how
 * the implementation was configured, line content is interpreted either
 * as US-ASCII or as UTF-8.
 *
 * @since JavaMail 2.1
 */
public interface LineInputStream {

    /**
     * Returns the next line from the underlying stream, or null once the
     * stream is exhausted.  Any of CR, LF or CR LF ends a line (the
     * malformed CR CR LF sequence produced by some agents is tolerated as
     * well), and the terminator itself is never part of the result.
     *
     * @return the next line, without its terminator, or null at end of data
     * @throws IOException if reading the underlying stream fails
     */
    String readLine() throws IOException;
}
