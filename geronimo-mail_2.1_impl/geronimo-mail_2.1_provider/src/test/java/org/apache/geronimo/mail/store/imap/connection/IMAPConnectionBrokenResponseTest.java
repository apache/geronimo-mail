/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.mail.store.imap.connection;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;

import org.apache.geronimo.mail.util.ProtocolProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the broken-conn-containment defect: when
 * readResponse() fails (garbled data or unexpected EOF), the stream position
 * of the connection is undefined mid-response.  Such a connection used to
 * remain poolable, so one garbled exchange could poison a later, unrelated
 * command.  receiveResponse() must mark the connection closed (and shut the
 * server connection down) before propagating the failure, so the pool
 * discards it via the releaseClosedConnection path.
 */
public class IMAPConnectionBrokenResponseTest {

    private IMAPConnection connectionReading(final byte[] data) {
        final ProtocolProperties props = new ProtocolProperties(
                Session.getInstance(new Properties()), "imap", false, 143);
        final IMAPConnection connection = new IMAPConnection(props, null);
        // inject the response reader directly; the connection was never
        // actually connected, so there is no socket to clean up.
        connection.reader = new IMAPResponseStream(new ByteArrayInputStream(data));
        return connection;
    }

    @Test
    public void testGarbledResponseMarksConnectionClosed() throws Exception {
        // an untagged response whose payload cannot be parsed ("* )" is the
        // shape seen in the transient TCK flake)
        final IMAPConnection connection = connectionReading("* )\r\n".getBytes("ISO8859-1"));
        assertFalse(connection.isClosed(), "connection must start out poolable");

        assertThrows(MessagingException.class, connection::receiveResponse,
                "a garbled response must surface as a MessagingException");
        assertTrue(connection.isClosed(),
                "a connection that failed mid-response must not be poolable");
    }

    @Test
    public void testPrematureEndOfStreamMarksConnectionClosed() throws Exception {
        // EOF in the middle of a response line
        final IMAPConnection connection = connectionReading("* OK incomplete".getBytes("ISO8859-1"));

        assertThrows(MessagingException.class, connection::receiveResponse,
                "an EOF mid-response must surface as a MessagingException");
        assertTrue(connection.isClosed(),
                "a connection whose stream hit EOF mid-response must not be poolable");
    }

    @Test
    public void testCleanResponseLeavesConnectionPoolable() throws Exception {
        final IMAPConnection connection = connectionReading("a1 OK completed\r\n".getBytes("ISO8859-1"));

        final IMAPTaggedResponse response = connection.receiveResponse();
        assertTrue(response.isOK());
        assertFalse(connection.isClosed(),
                "a successfully parsed exchange must leave the connection poolable");
    }
}
