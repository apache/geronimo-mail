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

import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;

import org.apache.geronimo.mail.testserver.AbstractProtocolTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression tests for a set of IMAP provider defects surfaced by the
 * Jakarta Mail TCK when run against Apache James.
 */
public class IMAPTckRegressionTest extends AbstractProtocolTest {

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
        try (InputStream in = IMAPTckRegressionTest.class.getResourceAsStream(name)) {
            final byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                bout.write(buf, 0, n);
            }
        }
        return bout.toByteArray();
    }

    protected void createMailboxWithMessage(final String name) throws Exception {
        server.createUserMailbox(name);
        server.appendToUserMailbox(name, readMessageResource("/messages/simple.msg"));
    }

    /**
     * MimeMessage#getContentLanguage_Test: a message whose BODYSTRUCTURE
     * carries no language information used to trigger a NullPointerException;
     * the spec requires a null return instead.
     */
    @Test
    public void testGetContentLanguageWithoutLanguagesReturnsNull() throws Exception {
        start();
        createMailboxWithMessage("test1");

        final Store store = connect();
        try {
            final Folder folder = store.getDefaultFolder().getFolder("test1");
            folder.open(Folder.READ_ONLY);
            try {
                final jakarta.mail.internet.MimeMessage msg =
                        (jakarta.mail.internet.MimeMessage) folder.getMessage(1);
                assertNull(msg.getContentLanguage(),
                        "getContentLanguage must return null when the message has no Content-Language");
            } finally {
                folder.close(false);
            }
        } finally {
            store.close();
        }
    }
}
