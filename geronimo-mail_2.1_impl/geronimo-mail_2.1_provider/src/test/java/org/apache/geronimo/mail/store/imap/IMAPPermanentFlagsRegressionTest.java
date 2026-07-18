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
import jakarta.mail.Session;
import jakarta.mail.Store;

import org.apache.geronimo.mail.testserver.AbstractProtocolTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the respcode-bracket-parsing defect surfaced by the
 * Jakarta Mail TCK (Folder#getPermanentFlags_Test): the '* OK
 * [PERMANENTFLAGS ...]', '* OK [UIDVALIDITY ...]' etc. response codes sent
 * during SELECT collapsed into generic OK responses because '[' was not a
 * delimiter in the tokenizer's default atom mode, so getPermanentFlags()
 * always returned null and the open-time UID metadata was lost.
 */
public class IMAPPermanentFlagsRegressionTest extends AbstractProtocolTest {

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
        try (InputStream in = IMAPPermanentFlagsRegressionTest.class.getResourceAsStream(name)) {
            final byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                bout.write(buf, 0, n);
            }
        }
        return bout.toByteArray();
    }

    @Test
    public void testPermanentFlagsAndUidValidityAfterOpen() throws Exception {
        start();
        server.createUserMailbox("test1");
        server.appendToUserMailbox("test1", readMessageResource("/messages/simple.msg"));

        final Store store = connect();
        try {
            final IMAPFolder folder = (IMAPFolder) store.getDefaultFolder().getFolder("test1");
            folder.open(Folder.READ_WRITE);
            try {
                final Flags permanentFlags = folder.getPermanentFlags();
                assertNotNull(permanentFlags,
                        "getPermanentFlags() must reflect the PERMANENTFLAGS response code from SELECT");
                assertTrue(permanentFlags.contains(Flags.Flag.SEEN),
                        "the permanent flags advertised by James must include \\Seen");
                assertTrue(permanentFlags.contains(Flags.Flag.DELETED),
                        "the permanent flags advertised by James must include \\Deleted");

                assertTrue(folder.getUIDValidity() > 0,
                        "getUIDValidity() must be populated from the UIDVALIDITY response code");
            } finally {
                folder.close(false);
            }
        } finally {
            store.close();
        }
    }
}
