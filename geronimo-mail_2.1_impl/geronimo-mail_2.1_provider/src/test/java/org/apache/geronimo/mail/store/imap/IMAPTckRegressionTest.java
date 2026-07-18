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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.event.MessageChangedEvent;
import jakarta.mail.event.MessageChangedListener;

import org.apache.geronimo.mail.testserver.AbstractProtocolTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
     * Folder#create_Test/delete_Test/getType_Test: a top-level folder obtained
     * via the default folder has an UNDETERMINED separator; create() used to
     * append the raw '\0' separator to the mailbox name on the wire
     * ("CREATE topdog&AAA-"), so the folder was created under a mangled name
     * and create() reported failure.  The James hierarchy delimiter is '.',
     * not the '/' fallback, so the separator must be obtained from the server.
     */
    @Test
    public void testCreateDeleteGetTypeRoundTrip() throws Exception {
        start();

        final Store store = connect();
        try {
            final Folder folder = store.getDefaultFolder().getFolder("topdog");
            assertFalse(folder.exists(), "folder must not exist before creation");

            assertTrue(folder.create(Folder.HOLDS_FOLDERS), "create must succeed");
            assertTrue(folder.exists(), "created folder must exist under its own name");
            assertTrue((folder.getType() & Folder.HOLDS_FOLDERS) != 0,
                    "created folder must be able to hold folders");

            assertTrue(folder.delete(false), "delete must succeed");
            assertFalse(folder.exists(), "deleted folder must no longer exist");
        } finally {
            store.close();
        }
    }

    /**
     * Folder#list_Test/listSubscribed_Test: listing the default folder used to
     * issue LIST / "%" (the hard-coded root separator as reference), which
     * matches nothing on servers with a '.' hierarchy delimiter such as James.
     * The reference for the root folder must be the empty string.
     */
    @Test
    public void testDefaultFolderListIsNonEmpty() throws Exception {
        start();
        createMailboxWithMessage("test1");

        final Store store = connect();
        try {
            final Folder[] folders = store.getDefaultFolder().list("%");
            assertTrue(folders.length > 0, "default folder list(\"%\") must return at least INBOX");

            // also exercise the LSUB flavour of the same code path
            final Folder test1 = store.getDefaultFolder().getFolder("test1");
            test1.setSubscribed(true);
            final Folder[] subscribed = store.getDefaultFolder().listSubscribed("%");
            assertTrue(subscribed.length > 0, "default folder listSubscribed(\"%\") must find the subscribed folder");
        } finally {
            store.close();
        }
    }

    /**
     * event/FolderEvent#addMsgChangeList_Test: a MessageChangedListener must
     * fire when a flag is changed through a single message (Message.setFlags).
     * The untagged FETCH reply to the STORE command is consumed inside
     * IMAPConnection.setFlags, so the folder must notify the listeners itself.
     */
    @Test
    public void testMessageChangedListenerFiresOnSingleMessageSetFlag() throws Exception {
        start();
        createMailboxWithMessage("test1");

        final Store store = connect();
        try {
            final Folder folder = store.getDefaultFolder().getFolder("test1");
            folder.open(Folder.READ_WRITE);
            try {
                final CountDownLatch changed = new CountDownLatch(1);
                folder.addMessageChangedListener(new MessageChangedListener() {
                    public void messageChanged(final MessageChangedEvent e) {
                        if (e.getMessageChangeType() == MessageChangedEvent.FLAGS_CHANGED) {
                            changed.countDown();
                        }
                    }
                });

                final Message msg = folder.getMessage(1);
                msg.setFlag(Flags.Flag.ANSWERED, true);

                assertTrue(changed.await(10, TimeUnit.SECONDS),
                        "a MessageChangedEvent must be delivered for a single-message setFlag");
            } finally {
                folder.close(false);
            }
        } finally {
            store.close();
        }
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
