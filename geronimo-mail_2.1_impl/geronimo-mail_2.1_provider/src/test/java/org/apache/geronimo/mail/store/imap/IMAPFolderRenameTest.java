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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for IMAPFolder.renameTo against Apache James.
 *
 * renameTo used to refresh the folder status with a STATUS command issued
 * under the OLD mailbox name right after a successful server-side RENAME.
 * James answers that STATUS with a tagged NO ("Mailbox not found"), the
 * resulting CommandFailedException was swallowed and renameTo returned
 * false even though the mailbox had really been renamed.  In the Jakarta
 * Mail TCK this left the shared "test1" mailbox renamed for the remainder
 * of the run, making ~90 subsequent tests fail with CommandFailedException
 * when opening "test1" (SELECT/EXAMINE answered with "NO ... No such
 * mailbox.").
 */
public class IMAPFolderRenameTest extends AbstractProtocolTest {

    private Store connect() throws Exception {
        final Properties props = new Properties();
        props.setProperty("mail.imap.port", String.valueOf(imapConf.getListenerPort()));
        props.setProperty("mail.debug", "true");
        final Session session = Session.getInstance(props);
        final Store store = session.getStore("imap");
        store.connect("127.0.0.1", "serveruser", "serverpass");
        return store;
    }

    private void createMailboxWithMessage(final String name) throws Exception {
        server.createUserMailbox(name);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (InputStream in = IMAPFolderRenameTest.class.getResourceAsStream("/messages/simple.msg")) {
            final byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                bout.write(buf, 0, n);
            }
        }
        server.appendToUserMailbox(name, bout.toByteArray());
    }

    /**
     * A successful server-side RENAME must be reported as success by
     * renameTo, and the folder must remain usable under the new name.
     */
    @Test
    public void testRenameToReportsSuccess() throws Exception {
        start();
        createMailboxWithMessage("test1");

        final Store store = connect();
        try {
            final Folder test1 = store.getDefaultFolder().getFolder("test1");
            final Folder test2 = store.getDefaultFolder().getFolder("test2");

            assertTrue(test1.renameTo(test2), "renameTo must return true for a successful server-side RENAME");
            assertTrue(test2.exists(), "renamed mailbox must exist under the new name");
            assertFalse(test1.exists(), "renamed mailbox must no longer exist under the old name");
        } finally {
            store.close();
        }
    }

    /**
     * The TCK pattern (Folder/renameTo_Test): rename away, then rename back,
     * then open the folder again.  Before the fix the rename-back never
     * happened (renameTo wrongly returned false) and every subsequent open
     * of the folder failed with CommandFailedException.
     */
    @Test
    public void testOpenAfterRenameAndRenameBack() throws Exception {
        start();
        createMailboxWithMessage("test1");

        final Store store = connect();
        try {
            final Folder test1 = store.getDefaultFolder().getFolder("test1");
            final Folder test2 = store.getDefaultFolder().getFolder("test2");

            assertTrue(test1.renameTo(test2), "rename test1 -> test2 must succeed");
            assertTrue(store.getDefaultFolder().getFolder("test2").renameTo(test1),
                    "rename test2 -> test1 (rename back) must succeed");

            // a later session opens the folder, as the next TCK test would
            final Store store2 = connect();
            try {
                final Folder reopened = store2.getDefaultFolder().getFolder("test1");
                reopened.open(Folder.READ_ONLY);
                assertEquals(1, reopened.getMessageCount());
                reopened.close(false);
            } finally {
                store2.close();
            }
        } finally {
            store.close();
        }
    }
}
