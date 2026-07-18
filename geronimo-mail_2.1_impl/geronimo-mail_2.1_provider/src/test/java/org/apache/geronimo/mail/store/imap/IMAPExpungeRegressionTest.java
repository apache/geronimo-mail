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
package org.apache.geronimo.mail.store.imap;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;

import org.apache.geronimo.mail.testserver.AbstractProtocolTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the expunge-keyword-typo defect surfaced by the
 * Jakarta Mail TCK (exception#msgRemoveExp_Test):
 * IMAPConnection.expungeMailbox() extracted the untagged expunge responses
 * with the keyword "EXPUNGED", but the parser queues them under "EXPUNGE".
 * As a result Folder.expunge() always returned an empty array and the
 * unextracted responses were double-processed by the pending-response
 * handler.
 */
public class IMAPExpungeRegressionTest extends AbstractProtocolTest {

    protected Store connect() throws Exception {
        final Properties props = new Properties();
        props.setProperty("mail.imap.port", String.valueOf(imapConf.getListenerPort()));
        props.setProperty("mail.debug", "true");
        final Session session = Session.getInstance(props);
        final Store store = session.getStore("imap");
        store.connect("127.0.0.1", "serveruser", "serverpass");
        return store;
    }

    protected byte[] readMessageResource(final String name) throws Exception {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (InputStream in = IMAPExpungeRegressionTest.class.getResourceAsStream(name)) {
            final byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                bout.write(buf, 0, n);
            }
        }
        return bout.toByteArray();
    }

    @Test
    public void testExpungeReturnsExpungedMessages() throws Exception {
        start();
        server.createUserMailbox("test1");
        final byte[] message = readMessageResource("/messages/simple.msg");
        server.appendToUserMailbox("test1", message);
        server.appendToUserMailbox("test1", message);
        server.appendToUserMailbox("test1", message);

        final Store store = connect();
        try {
            final Folder folder = store.getDefaultFolder().getFolder("test1");
            folder.open(Folder.READ_WRITE);
            try {
                assertEquals(3, folder.getMessageCount(), "test setup must provide three messages");

                // flag two of the three messages for deletion
                folder.getMessage(1).setFlag(Flags.Flag.DELETED, true);
                folder.getMessage(3).setFlag(Flags.Flag.DELETED, true);

                final Message[] expunged = folder.expunge();
                assertNotNull(expunged, "expunge() must never return null");
                assertEquals(2, expunged.length,
                        "expunge() must return exactly the messages removed from the folder");
                for (final Message m : expunged) {
                    assertTrue(m.isExpunged(), "returned messages must be marked expunged");
                }

                // the message cache must have been updated exactly once per expunge
                assertEquals(1, folder.getMessageCount(),
                        "one message must remain after expunging two of three");
            } finally {
                folder.close(false);
            }
        } finally {
            store.close();
        }
    }
}
